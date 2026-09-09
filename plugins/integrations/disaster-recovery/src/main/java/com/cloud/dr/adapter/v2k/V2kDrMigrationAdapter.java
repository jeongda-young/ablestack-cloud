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
package com.cloud.dr.adapter.v2k;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.apache.cloudstack.vm.ImportVmTask;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrReplicaVO;
import com.cloud.dr.DrRunStepVO;
import com.cloud.dr.DrRunVO;
import com.cloud.dr.DrSiteVO;
import com.cloud.dr.adapter.DrAdapterResult;
import com.cloud.dr.adapter.DrExecutionContext;
import com.cloud.dr.adapter.DrReplicationEngine;
import com.cloud.dr.dao.DrPlanDao;
import com.cloud.dr.dao.DrReplicaDao;
import com.cloud.dr.dao.DrRunStepDao;
import com.cloud.dr.dao.DrSiteDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.ImportVMTaskVO;
import com.cloud.vm.dao.ImportVMTaskDao;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class V2kDrMigrationAdapter extends ManagerBase implements DrReplicationEngine {
    private static final Logger LOGGER = LogManager.getLogger(V2kDrMigrationAdapter.class);
    private static final Gson GSON = new Gson();

    private static final String STEP_V2K_PHASE1 = "v2k-phase1";
    private static final String STEP_V2K_PHASE2 = "v2k-phase2";

    @Inject
    private DrSiteDao drSiteDao;
    @Inject
    private DrPlanDao drPlanDao;
    @Inject
    private DrReplicaDao drReplicaDao;
    @Inject
    private DrRunStepDao drRunStepDao;
    @Inject
    private ImportVMTaskDao importVMTaskDao;

    @Override
    public String getEngineType() {
        return DrConstants.ENGINE_TYPE_V2K;
    }

    @Override
    public String getEngineBindingType() {
        return DrConstants.ENGINE_BINDING_TYPE_V2K;
    }

    @Override
    public DrAdapterResult validatePlan(DrPlanVO plan) {
        DrAdapterResult siteValidation = validateSites(plan);
        if (!siteValidation.isSuccess()) {
            return siteValidation;
        }

        JsonObject details = basePlanDetails(plan);
        details.addProperty("validationOnly", true);
        details.addProperty("taskBindingRequiredForRun", true);
        details.addProperty("phase1Action", "TRACK_EXISTING_IMPORT_VM_TASK");
        details.addProperty("phase2Action", "TRACK_EXISTING_IMPORT_VM_TASK");
        return DrAdapterResult.success("V2K DR plan direction and sites are valid", GSON.toJson(details));
    }

    @Override
    public DrAdapterResult execute(DrExecutionContext context) {
        if (context == null || context.getRun() == null || context.getPlan() == null) {
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_ACTION_FAILED, "DR run context is required", null);
        }

        DrAdapterResult validation = validateSites(context.getPlan());
        if (!validation.isSuccess()) {
            return validation;
        }

        String runType = StringUtils.upperCase(context.getRun().getRunType(), Locale.ROOT);
        if (StringUtils.equals(runType, DrConstants.RUN_TYPE_SYNC)) {
            if (requestBoolean(context.getRun(), "dryRun", false)) {
                return validatePlan(context.getPlan());
            }
            return trackPhase1(context);
        }
        if (StringUtils.equals(runType, DrConstants.RUN_TYPE_FAILOVER)) {
            return trackPhase2(context);
        }
        if (StringUtils.equalsAny(runType, DrConstants.RUN_TYPE_TEST_FAILOVER, DrConstants.RUN_TYPE_TEST_CLEANUP,
                DrConstants.RUN_TYPE_FAILBACK, DrConstants.RUN_TYPE_REPROTECT, DrConstants.RUN_TYPE_ADOPT)) {
            return unsupported(context, "V2K DR adapter does not support " + runType + " yet");
        }
        return unsupported(context, "DR run type " + context.getRun().getRunType() + " is not supported by V2K adapter");
    }

    private DrAdapterResult trackPhase1(DrExecutionContext context) {
        ImportVMTaskVO task = resolveTask(context);
        if (task == null) {
            String message = "V2K DR sync requires an existing import VM task binding";
            JsonObject details = baseRunDetails(context);
            details.addProperty("requiredBinding", "engineBindingId or importVmTaskId/importVmTaskUuid");
            recordStep(context, STEP_V2K_PHASE1, 11, DrConstants.STEP_STATE_FAILED, 100, details,
                    DrConstants.ERROR_V2K_TASK_NOT_FOUND, message);
            return DrAdapterResult.failure(DrConstants.ERROR_V2K_TASK_NOT_FOUND, message, GSON.toJson(details));
        }

        DrAdapterResult taskValidation = validateV2kTask(context, task, STEP_V2K_PHASE1);
        if (!taskValidation.isSuccess()) {
            return taskValidation;
        }

        JsonObject details = taskDetails(context, task);
        if (isTaskFailed(task)) {
            String message = "V2K phase1 task is in failed state";
            recordStep(context, STEP_V2K_PHASE1, 11, DrConstants.STEP_STATE_FAILED, 100, details,
                    DrConstants.ERROR_ENGINE_ACTION_FAILED, message);
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_ACTION_FAILED, message, GSON.toJson(details));
        }
        if (!isPhase1Completed(task)) {
            String message = "V2K phase1 is not completed yet";
            details.addProperty("requiredV2kStep", ImportVmTask.V2KStep.Phase1_Completed.name());
            recordStep(context, STEP_V2K_PHASE1, 11, DrConstants.STEP_STATE_FAILED, 50, details,
                    DrConstants.ERROR_ENGINE_BUSY, message);
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_BUSY, message, GSON.toJson(details));
        }

        DrReplicaVO replica = upsertReplica(context, task, DrConstants.REPLICA_STATE_PHASE1_READY, "SOURCE");
        applyPhase1Ready(context, task, replica);
        details.addProperty("replicaId", replica.getId());
        details.addProperty("replicaUuid", replica.getUuid());
        details.addProperty("replicaState", replica.getState());
        recordStep(context, STEP_V2K_PHASE1, 11, DrConstants.STEP_STATE_SUCCEEDED, 100, details, null, null);
        return DrAdapterResult.success("V2K phase1 is ready for DR failover", GSON.toJson(details));
    }

    private DrAdapterResult trackPhase2(DrExecutionContext context) {
        ImportVMTaskVO task = resolveTask(context);
        if (task == null) {
            String message = "V2K DR failover requires an existing import VM task binding";
            JsonObject details = baseRunDetails(context);
            details.addProperty("requiredBinding", "engineBindingId or importVmTaskId/importVmTaskUuid");
            recordStep(context, STEP_V2K_PHASE2, 12, DrConstants.STEP_STATE_FAILED, 100, details,
                    DrConstants.ERROR_V2K_TASK_NOT_FOUND, message);
            return DrAdapterResult.failure(DrConstants.ERROR_V2K_TASK_NOT_FOUND, message, GSON.toJson(details));
        }

        DrAdapterResult taskValidation = validateV2kTask(context, task, STEP_V2K_PHASE2);
        if (!taskValidation.isSuccess()) {
            return taskValidation;
        }

        JsonObject details = taskDetails(context, task);
        if (isTaskFailed(task)) {
            String message = "V2K phase2 task is in failed state";
            recordStep(context, STEP_V2K_PHASE2, 12, DrConstants.STEP_STATE_FAILED, 100, details,
                    DrConstants.ERROR_ENGINE_ACTION_FAILED, message);
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_ACTION_FAILED, message, GSON.toJson(details));
        }
        if (!isPhase1Completed(task)) {
            String message = "V2K phase1 must be completed before DR failover";
            details.addProperty("requiredV2kStep", ImportVmTask.V2KStep.Phase1_Completed.name());
            recordStep(context, STEP_V2K_PHASE2, 12, DrConstants.STEP_STATE_FAILED, 100, details,
                    DrConstants.ERROR_V2K_PHASE1_REQUIRED, message);
            return DrAdapterResult.failure(DrConstants.ERROR_V2K_PHASE1_REQUIRED, message, GSON.toJson(details));
        }
        if (isPhase2InProgress(task)) {
            String message = "V2K phase2 is still running";
            details.addProperty("requiredV2kStep", ImportVmTask.V2KStep.Phase2_Completed.name());
            recordStep(context, STEP_V2K_PHASE2, 12, DrConstants.STEP_STATE_FAILED, 75, details,
                    DrConstants.ERROR_ENGINE_BUSY, message);
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_BUSY, message, GSON.toJson(details));
        }
        if (!isPhase2Completed(task)) {
            String message = "V2K phase2 must be executed through the existing V2K import task action before DR failover can complete";
            details.addProperty("requiredV2kStep", ImportVmTask.V2KStep.Phase2_Completed.name());
            details.addProperty("phase2StartPath", "importUnmanagedInstanceForAblestackV2K split=phase2 taskaction=phase2");
            recordStep(context, STEP_V2K_PHASE2, 12, DrConstants.STEP_STATE_FAILED, 100, details,
                    DrConstants.ERROR_V2K_PHASE2_REQUIRED, message);
            return DrAdapterResult.failure(DrConstants.ERROR_V2K_PHASE2_REQUIRED, message, GSON.toJson(details));
        }

        DrReplicaVO replica = upsertReplica(context, task, DrConstants.REPLICA_STATE_FAILED_OVER, "TARGET");
        applyPhase2Completed(context, task, replica);
        details.addProperty("replicaId", replica.getId());
        details.addProperty("replicaUuid", replica.getUuid());
        details.addProperty("replicaState", replica.getState());
        recordStep(context, STEP_V2K_PHASE2, 12, DrConstants.STEP_STATE_SUCCEEDED, 100, details, null, null);
        return DrAdapterResult.success("V2K phase2 cutover is complete", GSON.toJson(details));
    }

    private DrAdapterResult validateV2kTask(DrExecutionContext context, ImportVMTaskVO task, String stepName) {
        JsonObject details = taskDetails(context, task);
        if (StringUtils.equalsIgnoreCase(task.getMigrationTool(), ImportVmTask.MigrationTool.AblestackN2K.getValue())) {
            String message = "Import VM task is an N2K task, not a V2K task";
            recordStep(context, stepName, 10, DrConstants.STEP_STATE_FAILED, 100, details,
                    DrConstants.ERROR_ENGINE_UNSUPPORTED, message);
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_UNSUPPORTED, message, GSON.toJson(details));
        }
        if (StringUtils.isNotBlank(task.getSourceProvider())
                && !StringUtils.equalsIgnoreCase(task.getSourceProvider(), ImportVmTask.SourceProvider.VMware.getValue())) {
            String message = "V2K DR requires a VMware source import task";
            recordStep(context, stepName, 10, DrConstants.STEP_STATE_FAILED, 100, details,
                    DrConstants.ERROR_ENGINE_UNSUPPORTED, message);
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_UNSUPPORTED, message, GSON.toJson(details));
        }
        if (StringUtils.isNotBlank(task.getTargetProvider())
                && !StringUtils.equalsIgnoreCase(task.getTargetProvider(), ImportVmTask.TargetProvider.KVM.getValue())
                && !StringUtils.equalsIgnoreCase(task.getTargetProvider(), ImportVmTask.TargetProvider.Cloud.getValue())) {
            String message = "V2K DR requires a KVM or Cloud target import task";
            recordStep(context, stepName, 10, DrConstants.STEP_STATE_FAILED, 100, details,
                    DrConstants.ERROR_ENGINE_UNSUPPORTED, message);
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_UNSUPPORTED, message, GSON.toJson(details));
        }
        return DrAdapterResult.success("V2K import VM task is compatible", GSON.toJson(details));
    }

    private DrAdapterResult validateSites(DrPlanVO plan) {
        if (plan == null) {
            return DrAdapterResult.failure(DrConstants.ERROR_PLAN_NOT_FOUND, "DR plan is required", null);
        }
        JsonObject details = basePlanDetails(plan);
        if (!StringUtils.equalsIgnoreCase(DrConstants.DIRECTION_VMWARE_TO_KVM, plan.getDirection())) {
            String message = "V2K adapter supports only " + DrConstants.DIRECTION_VMWARE_TO_KVM + " plans";
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_UNSUPPORTED, message, GSON.toJson(details));
        }

        DrSiteVO sourceSite = drSiteDao.findById(plan.getSourceSiteId());
        if (sourceSite == null || sourceSite.getRemoved() != null) {
            String message = "Source DR site was not found for plan " + plan.getId();
            return DrAdapterResult.failure(DrConstants.ERROR_SITE_NOT_FOUND, message, GSON.toJson(details));
        }
        if (!StringUtils.equalsIgnoreCase(DrConstants.HYPERVISOR_TYPE_VMWARE, sourceSite.getHypervisorType())) {
            String message = "Source site must use VMware hypervisor type for V2K plans";
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_UNSUPPORTED, message, GSON.toJson(details));
        }

        DrSiteVO targetSite = drSiteDao.findById(plan.getTargetSiteId());
        if (targetSite == null || targetSite.getRemoved() != null) {
            String message = "Target DR site was not found for plan " + plan.getId();
            return DrAdapterResult.failure(DrConstants.ERROR_SITE_NOT_FOUND, message, GSON.toJson(details));
        }
        if (!StringUtils.equalsIgnoreCase(DrConstants.HYPERVISOR_TYPE_KVM, targetSite.getHypervisorType())) {
            String message = "Target site must use KVM hypervisor type for V2K plans";
            return DrAdapterResult.failure(DrConstants.ERROR_TARGET_MAPPING_INVALID, message, GSON.toJson(details));
        }
        details.addProperty("sourceSiteName", sourceSite.getName());
        details.addProperty("targetSiteName", targetSite.getName());
        return DrAdapterResult.success("V2K sites are valid", GSON.toJson(details));
    }

    private ImportVMTaskVO resolveTask(DrExecutionContext context) {
        DrPlanVO plan = context.getPlan();
        if (plan.getEngineBindingId() != null) {
            ImportVMTaskVO task = importVMTaskDao.findById(plan.getEngineBindingId());
            if (task != null) {
                return task;
            }
        }

        String taskRef = firstString(requestJson(context.getRun()), "importVmTaskId", "importVmTaskUuid", "taskUuid");
        if (StringUtils.isBlank(taskRef)) {
            taskRef = firstString(mappingJson(plan), "importVmTaskId", "importVmTaskUuid", "taskUuid");
        }
        return findTaskByReference(taskRef);
    }

    private ImportVMTaskVO findTaskByReference(String taskRef) {
        String trimmed = StringUtils.trimToNull(taskRef);
        if (trimmed == null) {
            return null;
        }
        try {
            return importVMTaskDao.findById(Long.valueOf(trimmed));
        } catch (NumberFormatException e) {
            return importVMTaskDao.findByUuid(trimmed);
        }
    }

    private DrReplicaVO upsertReplica(DrExecutionContext context, ImportVMTaskVO task, String state, String activeSide) {
        DrPlanVO plan = context.getPlan();
        List<DrReplicaVO> replicas = drReplicaDao.listActiveByPlanId(plan.getId());
        if (replicas == null) {
            replicas = Collections.emptyList();
        }
        DrReplicaVO replica = replicas.isEmpty() ? null : replicas.get(0);
        boolean created = false;
        if (replica == null) {
            replica = new DrReplicaVO(plan.getId(), plan.getTargetSiteId());
            created = true;
        }

        replica.setTargetVmId(task.getVmId());
        replica.setTargetVmName(StringUtils.defaultIfBlank(task.getTargetVMName(), task.getDisplayName()));
        replica.setTargetExternalRef("v2k-import-task://" + task.getUuid());
        replica.setState(state);
        replica.setHypervisorType(DrConstants.HYPERVISOR_TYPE_KVM);
        replica.setActiveSide(activeSide);
        replica.setPowerState(StringUtils.equals(activeSide, "TARGET") ? "UNKNOWN" : DrConstants.REPLICA_POWER_STATE_POWERED_OFF);
        JsonObject runtime = taskDetails(context, task);
        runtime.addProperty("ownershipMarker", "MoldCrossHypervisorDR");
        runtime.addProperty("createdBy", "MoldCrossHypervisorDR");
        runtime.addProperty("targetReady", isPhase1Completed(task));
        replica.setRuntimeStateJson(GSON.toJson(runtime));

        if (created) {
            return drReplicaDao.persist(replica);
        }
        replica.markUpdated();
        drReplicaDao.update(replica.getId(), replica);
        return replica;
    }

    private void applyPhase1Ready(DrExecutionContext context, ImportVMTaskVO task, DrReplicaVO replica) {
        DrPlanVO plan = context.getPlan();
        if (!StringUtils.equals(DrConstants.PLAN_STATE_FAILED_OVER, plan.getState())) {
            plan.setState(DrConstants.PLAN_STATE_PHASE1_READY);
        }
        plan.setTargetReadyAt(new Date());
        plan.setTargetReadyRpoSeconds(null);
        plan.setLastRunId(context.getRun().getId());
        plan.setLastErrorCode(null);
        plan.setLastErrorMessage(null);
        plan.markUpdated();
        drPlanDao.update(plan.getId(), plan);
    }

    private void applyPhase2Completed(DrExecutionContext context, ImportVMTaskVO task, DrReplicaVO replica) {
        DrPlanVO plan = context.getPlan();
        plan.setState(DrConstants.PLAN_STATE_FAILED_OVER);
        plan.setLastRunId(context.getRun().getId());
        plan.setLastErrorCode(null);
        plan.setLastErrorMessage(null);
        plan.markUpdated();
        drPlanDao.update(plan.getId(), plan);
    }

    private boolean isTaskFailed(ImportVMTaskVO task) {
        return task.getState() == ImportVmTask.TaskState.Failed
                || task.getState() == ImportVmTask.TaskState.Cancelled
                || task.getState() == ImportVmTask.TaskState.Cancelling
                || containsAny(task.getMigrationState(), "fail", "error", "abort", "cancel")
                || containsAny(task.getErrorCode(), "fail", "error", "abort", "cancel");
    }

    private boolean isPhase1Completed(ImportVMTaskVO task) {
        return StringUtils.equals(task.getV2kStep(), ImportVmTask.V2KStep.Phase1_Completed.name())
                || isPhase2InProgress(task)
                || isPhase2Completed(task)
                || StringUtils.equalsIgnoreCase(task.getCurrentPhase(), ImportVmTask.MigrationPhase.Phase2.getValue())
                || StringUtils.equalsIgnoreCase(task.getCurrentPhase(), ImportVmTask.MigrationPhase.Completed.getValue())
                || (StringUtils.equalsIgnoreCase(task.getCurrentPhase(), ImportVmTask.MigrationPhase.Phase1.getValue())
                    && StringUtils.equalsIgnoreCase(task.getMigrationState(), ImportVmTask.MigrationState.Completed.getValue()))
                || containsAny(task.getMigrationStep(), ImportVmTask.V2KStep.Phase1_Completed.name(), "phase1_done");
    }

    private boolean isPhase2InProgress(ImportVMTaskVO task) {
        return StringUtils.equals(task.getV2kStep(), ImportVmTask.V2KStep.Phase2_In_Progress.name())
                || (StringUtils.equalsIgnoreCase(task.getCurrentPhase(), ImportVmTask.MigrationPhase.Phase2.getValue())
                    && StringUtils.equalsIgnoreCase(task.getMigrationState(), ImportVmTask.MigrationState.Running.getValue()));
    }

    private boolean isPhase2Completed(ImportVMTaskVO task) {
        return StringUtils.equals(task.getV2kStep(), ImportVmTask.V2KStep.Phase2_Completed.name())
                || StringUtils.equals(task.getV2kStep(), ImportVmTask.V2KStep.Completed.name())
                || task.getState() == ImportVmTask.TaskState.Completed
                || StringUtils.equalsIgnoreCase(task.getCurrentPhase(), ImportVmTask.MigrationPhase.Completed.getValue())
                || (StringUtils.equalsIgnoreCase(task.getCurrentPhase(), ImportVmTask.MigrationPhase.Phase2.getValue())
                    && StringUtils.equalsIgnoreCase(task.getMigrationState(), ImportVmTask.MigrationState.Completed.getValue()))
                || containsAny(task.getMigrationStep(), ImportVmTask.V2KStep.Phase2_Completed.name(), "phase2_done");
    }

    private boolean containsAny(String source, String... tokens) {
        if (StringUtils.isBlank(source)) {
            return false;
        }
        for (String token : tokens) {
            if (StringUtils.containsIgnoreCase(source, token)) {
                return true;
            }
        }
        return false;
    }

    private void recordStep(DrExecutionContext context, String stepName, int stepOrder, String state, Integer progress,
            JsonObject details, String errorCode, String errorMessage) {
        if (drRunStepDao == null || context == null || context.getRun() == null) {
            return;
        }
        DrRunStepVO step = new DrRunStepVO(context.getRun().getId(), stepName, stepOrder);
        step.setState(state);
        step.setProgress(progress);
        step.setStarted(new Date());
        if (!StringUtils.equals(state, DrConstants.STEP_STATE_RUNNING)) {
            step.setCompleted(new Date());
        }
        if (details != null) {
            step.setDetailsJson(GSON.toJson(details));
        }
        step.setErrorCode(errorCode);
        step.setErrorMessage(errorMessage);
        drRunStepDao.persist(step);
    }

    private DrAdapterResult unsupported(DrExecutionContext context, String message) {
        JsonObject details = baseRunDetails(context);
        return DrAdapterResult.failure(DrConstants.ERROR_ACTION_UNSUPPORTED, message, GSON.toJson(details));
    }

    private JsonObject taskDetails(DrExecutionContext context, ImportVMTaskVO task) {
        JsonObject details = baseRunDetails(context);
        details.addProperty("importVmTaskId", task.getId());
        details.addProperty("importVmTaskUuid", task.getUuid());
        details.addProperty("displayName", task.getDisplayName());
        details.addProperty("sourceVmName", task.getSourceVMName());
        details.addProperty("targetVmName", task.getTargetVMName());
        details.addProperty("sourceProvider", task.getSourceProvider());
        details.addProperty("targetProvider", task.getTargetProvider());
        details.addProperty("migrationTool", task.getMigrationTool());
        details.addProperty("v2kStep", task.getV2kStep());
        details.addProperty("currentPhase", task.getCurrentPhase());
        details.addProperty("migrationState", task.getMigrationState());
        details.addProperty("migrationStep", task.getMigrationStep());
        details.addProperty("workdir", task.getWorkdir());
        details.addProperty("targetStoragePoolId", task.getTargetStoragePoolId());
        details.addProperty("v2kTargetStoragePoolId", task.getV2kTargetStoragePoolId());
        details.addProperty("targetFormat", task.getTargetFormat());
        details.addProperty("targetStorageType", task.getTargetStorageType());
        if (task.getVmId() != null) {
            details.addProperty("targetVmId", task.getVmId());
        }
        if (task.getState() != null) {
            details.addProperty("taskState", task.getState().name());
        }
        details.addProperty("phase1Completed", isPhase1Completed(task));
        details.addProperty("phase2Completed", isPhase2Completed(task));
        if (StringUtils.isNotBlank(task.getStatusJson())) {
            details.addProperty("statusJson", task.getStatusJson());
        }
        if (StringUtils.isNotBlank(task.getDescription())) {
            details.addProperty("description", task.getDescription());
        }
        if (StringUtils.isNotBlank(task.getErrorCode())) {
            details.addProperty("taskErrorCode", task.getErrorCode());
        }
        return details;
    }

    private JsonObject baseRunDetails(DrExecutionContext context) {
        JsonObject details = new JsonObject();
        if (context != null && context.getPlan() != null) {
            details.addProperty("planId", context.getPlan().getId());
            details.addProperty("planUuid", context.getPlan().getUuid());
            details.addProperty("direction", context.getPlan().getDirection());
        }
        if (context != null && context.getRun() != null) {
            details.addProperty("runId", context.getRun().getId());
            details.addProperty("runType", context.getRun().getRunType());
        }
        details.addProperty("engineType", getEngineType());
        details.addProperty("engineBindingType", getEngineBindingType());
        return details;
    }

    private JsonObject basePlanDetails(DrPlanVO plan) {
        JsonObject details = new JsonObject();
        if (plan != null) {
            details.addProperty("planId", plan.getId());
            details.addProperty("planUuid", plan.getUuid());
            details.addProperty("direction", plan.getDirection());
            details.addProperty("engineType", plan.getEngineType());
            details.addProperty("engineBindingType", plan.getEngineBindingType());
            details.addProperty("engineBindingId", plan.getEngineBindingId());
        }
        details.addProperty("engineAdapter", "V2K");
        return details;
    }

    private boolean requestBoolean(DrRunVO run, String key, boolean defaultValue) {
        JsonElement value = requestJson(run).get(key);
        return value != null && !value.isJsonNull() ? value.getAsBoolean() : defaultValue;
    }

    private JsonObject requestJson(DrRunVO run) {
        if (run == null || StringUtils.isBlank(run.getRequestJson())) {
            return new JsonObject();
        }
        return parseJsonObject(run.getRequestJson(), "run request", run.getId());
    }

    private JsonObject mappingJson(DrPlanVO plan) {
        if (plan == null || StringUtils.isBlank(plan.getMappingJson())) {
            return new JsonObject();
        }
        return parseJsonObject(plan.getMappingJson(), "plan mapping", plan.getId());
    }

    private JsonObject parseJsonObject(String json, String label, long ownerId) {
        try {
            JsonElement parsed = new JsonParser().parse(json);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException e) {
            LOGGER.warn("Ignoring invalid V2K DR {} JSON for {}: {}", label, ownerId, e.getMessage());
            return new JsonObject();
        }
    }

    private String firstString(JsonObject root, String... fieldNames) {
        if (root == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonElement value = root.get(fieldName);
            if (value != null && !value.isJsonNull() && value.isJsonPrimitive()) {
                String stringValue = StringUtils.trimToNull(value.getAsString());
                if (stringValue != null) {
                    return stringValue;
                }
            }
        }
        return null;
    }
}
