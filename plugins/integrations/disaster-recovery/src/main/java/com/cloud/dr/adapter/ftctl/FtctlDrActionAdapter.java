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
package com.cloud.dr.adapter.ftctl;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.apache.cloudstack.api.response.ftctl.FtctlActionResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.FtctlActionCommand;
import com.cloud.dr.DrConstants;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrReplicaVO;
import com.cloud.dr.DrRunVO;
import com.cloud.dr.adapter.DrAdapterResult;
import com.cloud.dr.adapter.DrExecutionContext;
import com.cloud.dr.adapter.DrFencingAdapter;
import com.cloud.dr.adapter.DrReplicationEngine;
import com.cloud.dr.dao.DrReplicaDao;
import com.cloud.ftctl.FtctlProtectionVO;
import com.cloud.ftctl.FtctlService;
import com.cloud.ftctl.dao.FtctlProtectionDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FtctlDrActionAdapter extends ManagerBase implements DrReplicationEngine, DrFencingAdapter {
    private static final Logger LOGGER = LogManager.getLogger(FtctlDrActionAdapter.class);
    private static final Gson GSON = new Gson();

    @Inject
    private FtctlService ftctlService;
    @Inject
    private FtctlProtectionDao ftctlProtectionDao;
    @Inject
    private DrReplicaDao drReplicaDao;

    @Override
    public String getEngineType() {
        return DrConstants.ENGINE_TYPE_FTCTL;
    }

    @Override
    public String getEngineBindingType() {
        return DrConstants.ENGINE_BINDING_TYPE_FTCTL;
    }

    @Override
    public DrAdapterResult validatePlan(DrPlanVO plan) {
        FtctlProtectionVO protection = resolveProtection(plan);
        if (protection == null) {
            String message = "Active FTCTL protection was not found for DR plan " + plan.getId();
            return DrAdapterResult.failure(DrConstants.ERROR_FTCTL_PROTECTION_NOT_FOUND, message, buildPlanDetails(plan, null));
        }
        return DrAdapterResult.success("FTCTL protection is available", buildPlanDetails(plan, protection));
    }

    @Override
    public DrAdapterResult execute(DrExecutionContext context) {
        if (requestBoolean(context.getRun(), "dryRun", false)) {
            return validatePlan(context.getPlan());
        }

        String runType = StringUtils.upperCase(context.getRun().getRunType(), Locale.ROOT);
        try {
            if (StringUtils.equals(runType, DrConstants.RUN_TYPE_SYNC)) {
                return executeFtctlVmAction(context, FtctlActionCommand.Action.PROTECT_START, false);
            }
            if (StringUtils.equalsAny(runType, DrConstants.RUN_TYPE_TEST_FAILOVER, DrConstants.RUN_TYPE_TEST_CLEANUP)) {
                return unsupported(context, "FTCTL does not expose a host engine action for " + runType + " yet");
            }
            if (StringUtils.equals(runType, DrConstants.RUN_TYPE_FAILOVER)) {
                return executeFtctlVmAction(context, FtctlActionCommand.Action.FAILOVER, true);
            }
            if (StringUtils.equals(runType, DrConstants.RUN_TYPE_FAILBACK)) {
                return executeFailback(context);
            }
            if (StringUtils.equals(runType, DrConstants.RUN_TYPE_REPROTECT)) {
                return executeFtctlVmAction(context, FtctlActionCommand.Action.FAILBACK_REPROTECT, true);
            }
            if (StringUtils.equals(runType, DrConstants.RUN_TYPE_ADOPT)) {
                return executeAdopt(context);
            }
            if (StringUtils.equals(runType, DrConstants.RUN_TYPE_FENCE_CONFIRM)) {
                return confirmFenceClear(context);
            }
            return unsupported(context, "DR run type " + context.getRun().getRunType() + " is not supported by the FTCTL action adapter yet");
        } catch (CloudRuntimeException e) {
            return failureFromException(context, e);
        } catch (RuntimeException e) {
            return failureFromException(context, e);
        }
    }

    @Override
    public DrAdapterResult confirmFenceClear(DrExecutionContext context) {
        try {
            Long primaryVmId = resolvePrimaryVmId(context);
            if (primaryVmId == null) {
                return protectionNotFound(context.getPlan());
            }
            DrRunVO run = context.getRun();
            FtctlActionResponse response = ftctlService.confirmFtctlFence(primaryVmId, requestString(run, "remoteMoldApiUrl"),
                    requestString(run, "remoteMoldApiKey"), requestString(run, "remoteMoldSecretKey"));
            return toAdapterResult(context, FtctlActionCommand.Action.FENCE_CONFIRM.name(), response);
        } catch (CloudRuntimeException e) {
            return failureFromException(context, e);
        } catch (RuntimeException e) {
            return failureFromException(context, e);
        }
    }

    private DrAdapterResult executeFtctlVmAction(DrExecutionContext context, FtctlActionCommand.Action action, boolean defaultForce) {
        Long primaryVmId = resolvePrimaryVmId(context);
        if (primaryVmId == null) {
            return protectionNotFound(context.getPlan());
        }
        boolean force = requestBoolean(context.getRun(), "force", defaultForce);
        FtctlActionResponse response = ftctlService.executeFtctlAction(primaryVmId, action, force);
        return toAdapterResult(context, action.name(), response);
    }

    private DrAdapterResult executeFailback(DrExecutionContext context) {
        Long primaryVmId = resolvePrimaryVmId(context);
        if (primaryVmId == null) {
            return protectionNotFound(context.getPlan());
        }
        DrRunVO run = context.getRun();
        FtctlActionResponse response = ftctlService.failbackFtctlProtection(primaryVmId, requestBoolean(run, "force", true),
                requestString(run, "failbackTargetMoldType"),
                requestString(run, "remoteMoldApiUrl"), requestString(run, "remoteMoldApiKey"), requestString(run, "remoteMoldSecretKey"),
                requestString(run, "targetMoldApiUrl"), requestString(run, "targetMoldApiKey"), requestString(run, "targetMoldSecretKey"));
        return toAdapterResult(context, FtctlActionCommand.Action.FAILBACK.name(), response);
    }

    private DrAdapterResult executeAdopt(DrExecutionContext context) {
        Long targetVmId = resolveAdoptTargetVmId(context);
        if (targetVmId == null) {
            String message = "Unable to resolve target replica VM for DR plan " + context.getPlan().getId();
            return DrAdapterResult.failure(DrConstants.ERROR_FTCTL_PROTECTION_NOT_FOUND, message,
                    buildPlanDetails(context.getPlan(), resolveProtection(context.getPlan())));
        }
        boolean cleanupTransport = requestBoolean(context.getRun(), "cleanupTransport", true);
        FtctlActionResponse response = ftctlService.adoptFtctlDrReplica(targetVmId, cleanupTransport);
        return toAdapterResult(context, DrConstants.RUN_TYPE_ADOPT, response);
    }

    private DrAdapterResult unsupported(DrExecutionContext context, String message) {
        JsonObject details = new JsonObject();
        details.addProperty("runType", context.getRun().getRunType());
        details.addProperty("planId", context.getPlan().getId());
        return DrAdapterResult.failure(DrConstants.ERROR_ACTION_UNSUPPORTED, message, GSON.toJson(details));
    }

    private DrAdapterResult toAdapterResult(DrExecutionContext context, String action, FtctlActionResponse response) {
        JsonObject details = new JsonObject();
        details.addProperty("runType", context.getRun().getRunType());
        details.addProperty("action", action);
        details.add("ftctlResponse", response == null ? null : GSON.toJsonTree(response));

        if (response == null) {
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_ACTION_FAILED, "FTCTL action returned no response", GSON.toJson(details));
        }

        String result = StringUtils.trimToEmpty(response.getResult()).toLowerCase(Locale.ROOT);
        Integer exitCode = response.getExitCode();
        if (StringUtils.equals(result, "locked")) {
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_BUSY, "FTCTL action is locked by another process", GSON.toJson(details));
        }
        if (StringUtils.equalsAny(result, "fail", "failed", "error") || (exitCode != null && exitCode != 0)) {
            String message = StringUtils.defaultIfBlank(response.getOutput(), "FTCTL action failed");
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_ACTION_FAILED, message, GSON.toJson(details));
        }
        if (StringUtils.equalsAny(result, "ok", "success", "accepted", "delegated", "warn") || Integer.valueOf(0).equals(exitCode)) {
            return DrAdapterResult.success("FTCTL action " + action + " completed", GSON.toJson(details));
        }
        return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_ACTION_FAILED,
                "FTCTL action returned unsupported result: " + StringUtils.defaultIfBlank(response.getResult(), "unknown"), GSON.toJson(details));
    }

    private DrAdapterResult failureFromException(DrExecutionContext context, RuntimeException e) {
        String message = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
        String errorCode = isLockFailure(message) ? DrConstants.ERROR_ENGINE_BUSY : DrConstants.ERROR_ENGINE_ACTION_FAILED;
        JsonObject details = new JsonObject();
        details.addProperty("runType", context.getRun().getRunType());
        details.addProperty("exception", e.getClass().getName());
        details.addProperty("message", message);
        LOGGER.warn("FTCTL DR action failed for run {}: {}", context.getRun().getId(), message);
        return DrAdapterResult.failure(errorCode, message, GSON.toJson(details));
    }

    private boolean isLockFailure(String message) {
        return StringUtils.containsIgnoreCase(message, "locked")
                || StringUtils.containsIgnoreCase(message, "another ftctl process")
                || StringUtils.containsIgnoreCase(message, "remained locked");
    }

    private Long resolvePrimaryVmId(DrExecutionContext context) {
        FtctlProtectionVO protection = resolveProtection(context.getPlan());
        if (protection != null) {
            return protection.getPrimaryVmId();
        }
        return context.getPlan().getSourceVmId();
    }

    private Long resolveAdoptTargetVmId(DrExecutionContext context) {
        Long replicaId = requestLong(context.getRun(), "replicaId");
        if (replicaId != null) {
            DrReplicaVO replica = drReplicaDao.findById(replicaId);
            if (replica != null && replica.getRemoved() == null && replica.getPlanId() == context.getPlan().getId()) {
                return replica.getTargetVmId();
            }
        }

        List<DrReplicaVO> replicas = drReplicaDao.listActiveByPlanId(context.getPlan().getId());
        for (DrReplicaVO replica : replicas) {
            if (replica.getTargetVmId() != null) {
                return replica.getTargetVmId();
            }
        }

        FtctlProtectionVO protection = resolveProtection(context.getPlan());
        return protection != null ? protection.getSecondaryVmId() : null;
    }

    private FtctlProtectionVO resolveProtection(DrPlanVO plan) {
        if (plan.getEngineBindingId() != null) {
            FtctlProtectionVO protection = ftctlProtectionDao.findById(plan.getEngineBindingId());
            return protection != null && protection.getRemoved() == null ? protection : null;
        }
        if (plan.getSourceVmId() != null) {
            return ftctlProtectionDao.findActiveByPrimaryVmId(plan.getSourceVmId());
        }
        return null;
    }

    private DrAdapterResult protectionNotFound(DrPlanVO plan) {
        String message = "Active FTCTL protection was not found for DR plan " + plan.getId();
        return DrAdapterResult.failure(DrConstants.ERROR_FTCTL_PROTECTION_NOT_FOUND, message, buildPlanDetails(plan, null));
    }

    private String buildPlanDetails(DrPlanVO plan, FtctlProtectionVO protection) {
        JsonObject details = new JsonObject();
        details.addProperty("planId", plan.getId());
        if (plan.getSourceVmId() != null) {
            details.addProperty("sourceVmId", plan.getSourceVmId());
        }
        if (plan.getEngineBindingId() != null) {
            details.addProperty("engineBindingId", plan.getEngineBindingId());
        }
        if (protection != null) {
            details.addProperty("ftctlProtectionId", protection.getId());
            details.addProperty("primaryVmId", protection.getPrimaryVmId());
            if (protection.getSecondaryVmId() != null) {
                details.addProperty("secondaryVmId", protection.getSecondaryVmId());
            }
            details.addProperty("protectionState", protection.getProtectionState());
            details.addProperty("transportState", protection.getTransportState());
            details.addProperty("activeSide", protection.getActiveSide());
        }
        return GSON.toJson(details);
    }

    private String requestString(DrRunVO run, String key) {
        JsonObject request = requestJson(run);
        JsonElement value = request.get(key);
        return value != null && !value.isJsonNull() ? value.getAsString() : null;
    }

    private Long requestLong(DrRunVO run, String key) {
        JsonObject request = requestJson(run);
        JsonElement value = request.get(key);
        return value != null && !value.isJsonNull() ? value.getAsLong() : null;
    }

    private boolean requestBoolean(DrRunVO run, String key, boolean defaultValue) {
        JsonObject request = requestJson(run);
        JsonElement value = request.get(key);
        return value != null && !value.isJsonNull() ? value.getAsBoolean() : defaultValue;
    }

    private JsonObject requestJson(DrRunVO run) {
        if (run == null || StringUtils.isBlank(run.getRequestJson())) {
            return new JsonObject();
        }
        try {
            JsonElement parsed = new JsonParser().parse(run.getRequestJson());
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException e) {
            LOGGER.warn("Ignoring invalid DR run request JSON for run {}: {}", run.getId(), e.getMessage());
            return new JsonObject();
        }
    }
}
