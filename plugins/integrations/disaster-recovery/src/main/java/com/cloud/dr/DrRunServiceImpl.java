// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.dr;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FtctlDrCancelAnswer;
import com.cloud.agent.api.FtctlDrCancelCommand;
import com.cloud.dr.adapter.ftctl.DrRemoteAgentClient;
import com.cloud.dr.dao.DrRunDao;
import com.cloud.dr.dao.DrRunStepDao;
import com.cloud.dr.dao.DrPlanDao;
import com.cloud.dr.orchestrator.DrOrchestrator;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ManagerBase;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DrRunServiceImpl extends ManagerBase implements DrRunService {
    @Inject
    private DrRunDao drRunDao;
    @Inject
    private DrRunStepDao drRunStepDao;
    @Inject
    private DrOrchestrator drOrchestrator;
    @Inject
    private DrPlanDao drPlanDao;
    @Inject
    private AgentManager agentManager;
    @Inject
    private DrFailbackLifecycleService drFailbackLifecycleService;
    @Inject
    private DrProjectionService drProjectionService;
    @Inject
    private DrRemoteAgentClient drRemoteAgentClient;
    @Inject
    private DrWorkerPlacementService drWorkerPlacementService;

    private enum CancelDispatchOutcome {
        CANCELED,
        ALREADY_TERMINAL,
        PENDING
    }

    @Override
    public DrRunVO startRun(long planId, String runType, String idempotencyKey, Long requestedByUserId, Long asyncJobId) {
        return startRun(planId, runType, idempotencyKey, requestedByUserId, asyncJobId, null);
    }

    @Override
    public DrRunVO startRun(long planId, String runType, String idempotencyKey, Long requestedByUserId, Long asyncJobId, String requestJson) {
        DrRunVO run = drOrchestrator.createRun(planId, runType, idempotencyKey, requestedByUserId, asyncJobId, requestJson);
        return drOrchestrator.executeRun(run.getId());
    }

    @Override
    public DrRunVO getRun(long runId) {
        return requireRun(runId);
    }

    @Override
    public List<DrRunVO> listRuns(long planId) {
        return drRunDao.listByPlanId(planId);
    }

    @Override
    public DrRunVO findRunByIdempotencyKey(long planId, String idempotencyKey) {
        if (StringUtils.isBlank(idempotencyKey)) {
            return null;
        }
        return drRunDao.findByPlanIdAndIdempotencyKey(planId, idempotencyKey);
    }

    @Override
    public List<DrRunStepVO> listRunSteps(long runId) {
        requireRun(runId);
        return drRunStepDao.listActiveByRunId(runId);
    }

    @Override
    public DrRunVO cancelRun(long runId) {
        DrRunVO run = requireRun(runId);
        if (run.getCompleted() != null) {
            return run;
        }
        if (StringUtils.equals(DrConstants.RUN_STATE_QUEUED, run.getState())) {
            run.setState(DrConstants.RUN_STATE_CANCELED);
            run.setCompleted(new Date());
        } else {
            run.setState(DrConstants.RUN_STATE_CANCEL_REQUESTED);
        }
        run.markUpdated();
        drRunDao.update(runId, run);
        drOrchestrator.recordEvent(run.getPlanId(), run.getId(), DrConstants.EVENT_RUN_CANCELED, DrConstants.EVENT_SEVERITY_WARN,
                DrConstants.EVENT_SOURCE_CLOUD, "DR run cancel requested", null);
        if (StringUtils.equals(DrConstants.RUN_STATE_CANCEL_REQUESTED, run.getState())) {
            CancelDispatchOutcome cancelOutcome = cancelFtctlRun(run);
            if (cancelOutcome == CancelDispatchOutcome.PENDING) {
                return drRunDao.findById(runId);
            }
            if (cancelOutcome == CancelDispatchOutcome.ALREADY_TERMINAL) {
                restoreRunForTerminalProjection(run);
                if (drProjectionService != null) {
                    drProjectionService.refreshPlanProjection(run.getPlanId(), true);
                }
                return drRunDao.findById(runId);
            }
            DrPlanVO plan = drPlanDao.findById(run.getPlanId());
            if (StringUtils.equalsIgnoreCase(run.getRunType(), DrConstants.RUN_TYPE_FAILBACK)
                    && drFailbackLifecycleService != null
                    && !drFailbackLifecycleService.cancelAndRestoreTargetAuthority(plan, run)) {
                return drRunDao.findById(runId);
            }
            // The original executor context may no longer exist after a management
            // server restart. Requeue the cancellation so the durable run is
            // terminalized instead of remaining an active-run blocker forever.
            drOrchestrator.executeRun(runId);
        }
        return drRunDao.findById(runId);
    }

    private CancelDispatchOutcome cancelFtctlRun(DrRunVO run) {
        DrPlanVO plan = drPlanDao != null ? drPlanDao.findById(run.getPlanId()) : null;
        if (plan == null || !StringUtils.equalsAnyIgnoreCase(DrConstants.ENGINE_TYPE_FTCTL_DR,
                plan.getEngineType(), plan.getEngineBindingType())) {
            return CancelDispatchOutcome.CANCELED;
        }
        Long coordinatorHostId = drWorkerPlacementService != null
                ? drWorkerPlacementService.resolveWorkerHostId(plan, run, DrWorkerRole.COORDINATOR) : null;
        boolean remoteSourceDispatch = dispatchesCancelOnRemoteSource(plan, run);
        if (remoteSourceDispatch && drRemoteAgentClient == null) {
            recordCancelDispatchPending(run, "FTCTL_DR cancel requires the remote source Agent client");
            return CancelDispatchOutcome.PENDING;
        }
        if (!remoteSourceDispatch && (coordinatorHostId == null || agentManager == null)) {
            recordCancelDispatchPending(run, "FTCTL_DR cancel requires an available coordinator Agent");
            return CancelDispatchOutcome.PENDING;
        }
        FtctlDrCancelCommand command = new FtctlDrCancelCommand(plan.getUuid(), run.getUuid());
        Answer rawAnswer;
        try {
            rawAnswer = remoteSourceDispatch
                    ? drRemoteAgentClient.cancelSourceRun(plan, run.getUuid())
                    : agentManager.easySend(coordinatorHostId, command);
        } catch (RuntimeException e) {
            recordCancelDispatchPending(run, StringUtils.defaultIfBlank(e.getMessage(),
                    "FTCTL_DR cancel dispatch failed"));
            return CancelDispatchOutcome.PENDING;
        }
        if (!(rawAnswer instanceof FtctlDrCancelAnswer)) {
            recordCancelDispatchPending(run, "FTCTL_DR cancel returned no typed Agent answer");
            return CancelDispatchOutcome.PENDING;
        }
        FtctlDrCancelAnswer answer = (FtctlDrCancelAnswer) rawAnswer;
        boolean identityMatches = StringUtils.equals(plan.getUuid(), answer.getPlanUuid())
                && StringUtils.equals(run.getUuid(), answer.getRunUuid());
        if (answer.getResult() && Boolean.TRUE.equals(answer.getAccepted()) && identityMatches
                && StringUtils.equalsIgnoreCase(answer.getFtctlResult(), "already_terminal")
                && hasAuthoritativeExistingTerminal(answer.getOutput())) {
            return CancelDispatchOutcome.ALREADY_TERMINAL;
        }
        if (!answer.getResult() || !Boolean.TRUE.equals(answer.getAccepted()) || !identityMatches
                || !hasAuthoritativeCancelTerminal(answer.getOutput())) {
            recordCancelDispatchPending(run, StringUtils.defaultIfBlank(answer.getDetails(),
                    "FTCTL_DR cancel did not return authoritative drained terminal evidence"));
            return CancelDispatchOutcome.PENDING;
        }
        return CancelDispatchOutcome.CANCELED;
    }

    private boolean dispatchesCancelOnRemoteSource(DrPlanVO plan, DrRunVO run) {
        return drRemoteAgentClient != null && drRemoteAgentClient.isRemoteKvmSource(plan)
                && StringUtils.equalsIgnoreCase(plan.getActiveSide(), "SOURCE")
                && StringUtils.equalsAnyIgnoreCase(run.getRunType(),
                        DrConstants.RUN_TYPE_SYNC, DrConstants.RUN_TYPE_RECOVER_SYNC,
                        DrConstants.RUN_TYPE_PAUSE_SYNC, DrConstants.RUN_TYPE_RESUME_SYNC,
                        DrConstants.RUN_TYPE_FAILOVER, DrConstants.RUN_TYPE_RELEASE);
    }

    private boolean hasAuthoritativeCancelTerminal(String output) {
        if (StringUtils.isBlank(output)) {
            return false;
        }
        try {
            JsonObject payload = JsonParser.parseString(output).getAsJsonObject();
            return payload.has("state") && StringUtils.equals("CANCELED", payload.get("state").getAsString())
                    && payload.has("terminal_authoritative") && payload.get("terminal_authoritative").getAsBoolean()
                    && payload.has("runtime_endpoints_drained") && payload.get("runtime_endpoints_drained").getAsBoolean()
                    && payload.has("transfer_activity_state")
                    && StringUtils.equals("CANCELED", payload.get("transfer_activity_state").getAsString());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean hasAuthoritativeExistingTerminal(String output) {
        if (StringUtils.isBlank(output)) {
            return false;
        }
        try {
            JsonObject payload = JsonParser.parseString(output).getAsJsonObject();
            String state = payload.has("state") ? payload.get("state").getAsString() : null;
            return payload.has("terminal_authoritative") && payload.get("terminal_authoritative").getAsBoolean()
                    && StringUtils.equalsAnyIgnoreCase(state, "READY", "FAILED", "ERROR", "FAILED_OVER",
                            "RELEASED", "UNPROTECTED", "CANCELED");
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void restoreRunForTerminalProjection(DrRunVO run) {
        run.setState(DrConstants.RUN_STATE_RUNNING);
        run.setCurrentStepName("runtime-terminal-reconciliation");
        run.setProjectionState("reconciling");
        run.setRetryable(true);
        run.setRetryAfterSeconds(2);
        run.setNextRetryAt(new Date(System.currentTimeMillis() + 2000L));
        run.setErrorCode(null);
        run.setErrorMessage(null);
        run.markUpdated();
        drRunDao.update(run.getId(), run);
        drOrchestrator.recordEvent(run.getPlanId(), run.getId(), DrConstants.EVENT_PROJECTION_REFRESH,
                DrConstants.EVENT_SEVERITY_INFO, DrConstants.EVENT_SOURCE_CLOUD,
                "Cancellation arrived after authoritative engine terminal; reconciling original result", null);
    }

    private void recordCancelDispatchPending(DrRunVO run, String message) {
        drOrchestrator.recordEvent(run.getPlanId(), run.getId(), DrConstants.EVENT_RUN_CANCELED,
                DrConstants.EVENT_SEVERITY_WARN, DrConstants.EVENT_SOURCE_CLOUD,
                StringUtils.defaultIfBlank(message, "FTCTL_DR cancel dispatch is pending"), null);
    }

    private DrRunVO requireRun(long runId) {
        DrRunVO run = drRunDao.findById(runId);
        if (run == null || run.getRemoved() != null) {
            throw new InvalidParameterValueException(DrConstants.ERROR_RUN_NOT_FOUND + ": " + runId);
        }
        return run;
    }
}
