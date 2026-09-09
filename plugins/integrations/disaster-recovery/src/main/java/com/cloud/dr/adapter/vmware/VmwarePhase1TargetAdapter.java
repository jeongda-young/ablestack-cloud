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
package com.cloud.dr.adapter.vmware;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrReplicaVO;
import com.cloud.dr.DrRunVO;
import com.cloud.dr.DrSiteCredentialService;
import com.cloud.dr.DrSiteVO;
import com.cloud.dr.adapter.DrAdapterResult;
import com.cloud.dr.adapter.DrExecutionContext;
import com.cloud.dr.adapter.DrReplicationEngine;
import com.cloud.dr.dao.DrPlanDao;
import com.cloud.dr.dao.DrReplicaDao;
import com.cloud.dr.dao.DrSiteDao;
import com.cloud.utils.component.ManagerBase;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class VmwarePhase1TargetAdapter extends ManagerBase implements DrReplicationEngine {
    private static final Logger LOGGER = LogManager.getLogger(VmwarePhase1TargetAdapter.class);
    private static final Gson GSON = new Gson();

    @Inject
    private DrSiteDao drSiteDao;
    @Inject
    private DrReplicaDao drReplicaDao;
    @Inject
    private DrPlanDao drPlanDao;
    @Inject
    private DrSiteCredentialService drSiteCredentialService;

    @Override
    public String getEngineType() {
        return DrConstants.ENGINE_TYPE_VMWARE_PHASE1;
    }

    @Override
    public String getEngineBindingType() {
        return DrConstants.ENGINE_BINDING_TYPE_VMWARE_PHASE1;
    }

    @Override
    public DrAdapterResult validatePlan(DrPlanVO plan) {
        VmwarePhase1Spec spec;
        try {
            spec = buildSpec(plan);
        } catch (IllegalArgumentException e) {
            return DrAdapterResult.failure(DrConstants.ERROR_TARGET_MAPPING_INVALID, e.getMessage(), invalidPlanDetails(plan, e.getMessage()));
        }

        DrAdapterResult siteResult = validateSites(plan, spec);
        if (siteResult != null && !siteResult.isSuccess()) {
            return siteResult;
        }

        JsonObject details = spec.toJson();
        details.addProperty("validationOnly", true);
        details.addProperty("vcenterOperation", "NOT_STARTED");
        return DrAdapterResult.success("VMware Phase 1 target mapping is valid", GSON.toJson(details));
    }

    @Override
    public DrAdapterResult execute(DrExecutionContext context) {
        if (context == null || context.getRun() == null) {
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_ACTION_FAILED, "DR run context is required", null);
        }

        String runType = StringUtils.upperCase(context.getRun().getRunType(), Locale.ROOT);
        if (StringUtils.equals(runType, DrConstants.RUN_TYPE_SYNC)) {
            if (requestBoolean(context.getRun(), "dryRun", false)) {
                return validatePlan(context.getPlan());
            }
            return ensureSkeletonRecord(context);
        }

        if (StringUtils.equalsAny(runType, DrConstants.RUN_TYPE_TEST_FAILOVER, DrConstants.RUN_TYPE_FAILOVER)) {
            return targetNotReady(context, "VMware Phase 1 has only a skeleton replica. TARGET_READY restore point is required before " + runType);
        }
        if (StringUtils.equalsAny(runType, DrConstants.RUN_TYPE_FAILBACK, DrConstants.RUN_TYPE_REPROTECT, DrConstants.RUN_TYPE_ADOPT)) {
            return unsupported(context, "VMware Phase 1 does not support " + runType + " yet");
        }
        return unsupported(context, "DR run type " + context.getRun().getRunType() + " is not supported by VMware Phase 1");
    }

    private DrAdapterResult ensureSkeletonRecord(DrExecutionContext context) {
        DrPlanVO plan = context.getPlan();
        VmwarePhase1Spec spec;
        try {
            spec = buildSpec(plan);
        } catch (IllegalArgumentException e) {
            return DrAdapterResult.failure(DrConstants.ERROR_TARGET_MAPPING_INVALID, e.getMessage(), invalidPlanDetails(plan, e.getMessage()));
        }

        DrAdapterResult validation = validateSites(plan, spec);
        if (validation != null && !validation.isSuccess()) {
            return validation;
        }

        DrReplicaVO replica;
        try {
            replica = resolveOwnedReplica(plan, spec);
        } catch (IllegalStateException e) {
            return DrAdapterResult.failure(DrConstants.ERROR_TARGET_OWNERSHIP_CONFLICT, e.getMessage(), spec.toJsonString());
        }

        boolean created = false;
        if (replica == null) {
            replica = new DrReplicaVO(plan.getId(), plan.getTargetSiteId());
            created = true;
        }

        populateSkeleton(replica, spec, context);
        if (created) {
            replica = drReplicaDao.persist(replica);
        } else {
            replica.markUpdated();
            drReplicaDao.update(replica.getId(), replica);
        }

        plan.setState(DrConstants.PLAN_STATE_ENABLED);
        plan.setLastRunId(context.getRun().getId());
        plan.setLastErrorCode(null);
        plan.setLastErrorMessage(null);
        plan.setTargetReadyAt(null);
        plan.setTargetReadyRpoSeconds(null);
        plan.markUpdated();
        drPlanDao.update(plan.getId(), plan);

        JsonObject details = spec.toJson();
        details.addProperty("phase", "VMWARE_TARGET_PHASE1");
        details.addProperty("adapterAction", "ENSURE_REPLICA_SKELETON_RECORD");
        details.addProperty("created", created);
        details.addProperty("replicaId", replica.getId());
        details.addProperty("replicaUuid", replica.getUuid());
        details.addProperty("replicaState", replica.getState());
        details.addProperty("powerState", replica.getPowerState());
        details.addProperty("vcenterOperation", "NOT_STARTED");
        details.addProperty("targetReady", false);
        return DrAdapterResult.success("VMware target skeleton record is ready", GSON.toJson(details));
    }

    private DrAdapterResult validateSites(DrPlanVO plan, VmwarePhase1Spec spec) {
        if (plan == null) {
            return DrAdapterResult.failure(DrConstants.ERROR_PLAN_NOT_FOUND, "DR plan is required", null);
        }
        if (!StringUtils.equalsIgnoreCase(DrConstants.DIRECTION_KVM_TO_VMWARE, plan.getDirection())) {
            String message = "VMware Phase 1 currently supports only " + DrConstants.DIRECTION_KVM_TO_VMWARE + " plans";
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_UNSUPPORTED, message, spec.toJsonString());
        }

        DrSiteVO sourceSite = drSiteDao.findById(plan.getSourceSiteId());
        if (sourceSite != null && !StringUtils.equalsIgnoreCase(DrConstants.HYPERVISOR_TYPE_KVM, sourceSite.getHypervisorType())) {
            String message = "Source site must be KVM for VMware Phase 1 KVM_TO_VMWARE plans";
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_UNSUPPORTED, message, spec.toJsonString());
        }

        DrSiteVO targetSite = drSiteDao.findById(plan.getTargetSiteId());
        if (targetSite == null || targetSite.getRemoved() != null) {
            String message = "Target DR site was not found for plan " + plan.getId();
            return DrAdapterResult.failure(DrConstants.ERROR_SITE_NOT_FOUND, message, spec.toJsonString());
        }
        if (!StringUtils.equalsIgnoreCase(DrConstants.HYPERVISOR_TYPE_VMWARE, targetSite.getHypervisorType())) {
            String message = "Target DR site must use VMware hypervisor type";
            return DrAdapterResult.failure(DrConstants.ERROR_TARGET_MAPPING_INVALID, message, spec.toJsonString());
        }
        if (targetSite.getVmwareDatacenterId() == null && StringUtils.isBlank(targetSite.getEndpoint())) {
            String message = "VMware target site requires a vCenter endpoint or vmwareDatacenterId";
            return DrAdapterResult.failure(DrConstants.ERROR_TARGET_UNAVAILABLE, message, spec.toJsonString());
        }
        if (targetSite.getVmwareDatacenterId() == null && StringUtils.isNotBlank(targetSite.getEndpoint())
                && !hasUsableCredential(targetSite)) {
            String message = "VMware target site endpoint requires stored vCenter credentials";
            return DrAdapterResult.failure(DrConstants.ERROR_CREDENTIAL_INVALID, message, spec.toJsonString());
        }
        spec.targetSiteName = targetSite.getName();
        spec.vmwareDatacenterId = targetSite.getVmwareDatacenterId();
        spec.endpoint = targetSite.getEndpoint();
        return null;
    }

    private boolean hasUsableCredential(DrSiteVO site) {
        return drSiteCredentialService != null ? drSiteCredentialService.hasUsableCredential(site) : StringUtils.isNotBlank(site.getCredentialRef());
    }

    private VmwarePhase1Spec buildSpec(DrPlanVO plan) {
        if (plan == null) {
            throw new IllegalArgumentException("DR plan is required");
        }
        JsonObject mapping = parseMapping(plan);
        VmwarePhase1Spec spec = new VmwarePhase1Spec();
        spec.planId = plan.getId();
        spec.planUuid = plan.getUuid();
        spec.sourceVmId = plan.getSourceVmId();
        spec.sourceExternalRef = plan.getSourceExternalRef();
        spec.targetVmName = StringUtils.defaultIfBlank(firstString(mapping, "targetVmName", "targetName"),
                StringUtils.defaultIfBlank(plan.getName(), "dr-plan-" + plan.getId()) + "-vmware-standby");
        spec.targetDatastoreRef = firstString(mapping, "targetDatastoreRef", "targetDatastore", "datastoreRef", "datastore");
        spec.targetFolderPath = firstNestedString(mapping, new String[] {"placement", "storage", "storageMapping"},
                "targetFolderPath", "folderPath", "folder");
        spec.targetComputeRef = firstNestedString(mapping, new String[] {"placement", "compute", "computeMapping"},
                "resourcePoolRef", "targetResourcePoolRef", "clusterRef", "targetClusterRef", "targetComputeRef");
        spec.targetNetworkRef = targetNetworkRef(mapping);
        spec.networkConnectMode = StringUtils.defaultIfBlank(targetNetworkPolicy(mapping, "connectMode"), "DISCONNECTED");
        spec.macPolicy = StringUtils.defaultIfBlank(targetNetworkPolicy(mapping, "macPolicy"), "GENERATE");
        spec.targetExternalRef = "vmware-phase1://site/" + plan.getTargetSiteId() + "/plan/" + plan.getUuid() + "/vm/" + spec.targetVmName;
        validateSpec(spec);
        return spec;
    }

    private JsonObject parseMapping(DrPlanVO plan) {
        if (StringUtils.isBlank(plan.getMappingJson())) {
            throw new IllegalArgumentException("VMware Phase 1 requires mapping_json");
        }
        try {
            JsonElement parsed = new JsonParser().parse(plan.getMappingJson());
            if (parsed == null || !parsed.isJsonObject()) {
                throw new IllegalArgumentException("VMware Phase 1 mapping_json must be a JSON object");
            }
            return parsed.getAsJsonObject();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("VMware Phase 1 mapping_json is invalid: " + e.getMessage());
        }
    }

    private void validateSpec(VmwarePhase1Spec spec) {
        if (StringUtils.isBlank(spec.targetDatastoreRef)) {
            throw new IllegalArgumentException("VMware Phase 1 mapping requires targetDatastoreRef");
        }
        if (StringUtils.isBlank(spec.targetComputeRef)) {
            throw new IllegalArgumentException("VMware Phase 1 mapping requires resourcePoolRef or clusterRef");
        }
        if (StringUtils.isBlank(spec.targetFolderPath)) {
            throw new IllegalArgumentException("VMware Phase 1 mapping requires targetFolderPath");
        }
        if (StringUtils.isBlank(spec.targetNetworkRef)) {
            throw new IllegalArgumentException("VMware Phase 1 mapping requires targetNetworkRef");
        }
        if (StringUtils.equalsIgnoreCase("PRODUCTION_ON_FAILOVER", spec.networkConnectMode)) {
            throw new IllegalArgumentException("VMware Phase 1 skeleton must not connect directly to production network");
        }
    }

    private DrReplicaVO resolveOwnedReplica(DrPlanVO plan, VmwarePhase1Spec spec) {
        List<DrReplicaVO> replicas = drReplicaDao.listActiveByPlanId(plan.getId());
        if (replicas == null) {
            replicas = Collections.emptyList();
        }
        for (DrReplicaVO replica : replicas) {
            if (StringUtils.equals(replica.getTargetExternalRef(), spec.targetExternalRef)
                    || StringUtils.equals(replica.getTargetVmName(), spec.targetVmName)) {
                return replica;
            }
            if (StringUtils.isNotBlank(replica.getTargetExternalRef()) || StringUtils.isNotBlank(replica.getTargetVmName())) {
                throw new IllegalStateException("Active DR replica belongs to another target resource for plan " + plan.getId());
            }
        }
        return null;
    }

    private void populateSkeleton(DrReplicaVO replica, VmwarePhase1Spec spec, DrExecutionContext context) {
        replica.setTargetExternalRef(spec.targetExternalRef);
        replica.setTargetVmName(spec.targetVmName);
        replica.setState(DrConstants.REPLICA_STATE_SKELETON_READY);
        replica.setPowerState(DrConstants.REPLICA_POWER_STATE_POWERED_OFF);
        replica.setHypervisorType(DrConstants.HYPERVISOR_TYPE_VMWARE);
        replica.setActiveSide("SOURCE");
        JsonObject runtime = spec.toJson();
        runtime.addProperty("phase", "VMWARE_TARGET_PHASE1");
        runtime.addProperty("targetReady", false);
        runtime.addProperty("runId", context.getRun().getId());
        runtime.addProperty("ownershipMarker", "MoldCrossHypervisorDR");
        runtime.addProperty("createdBy", "MoldCrossHypervisorDR");
        runtime.addProperty("vcenterOperation", "NOT_STARTED");
        runtime.addProperty("materializationState", "NOT_STARTED");
        replica.setRuntimeStateJson(GSON.toJson(runtime));
    }

    private DrAdapterResult targetNotReady(DrExecutionContext context, String message) {
        JsonObject details = baseRunDetails(context);
        details.addProperty("requiredState", "TARGET_READY");
        details.addProperty("currentPhase", "SKELETON_READY");
        return DrAdapterResult.failure(DrConstants.ERROR_TARGET_NOT_READY, message, GSON.toJson(details));
    }

    private DrAdapterResult unsupported(DrExecutionContext context, String message) {
        JsonObject details = baseRunDetails(context);
        return DrAdapterResult.failure(DrConstants.ERROR_ACTION_UNSUPPORTED, message, GSON.toJson(details));
    }

    private JsonObject baseRunDetails(DrExecutionContext context) {
        JsonObject details = new JsonObject();
        if (context != null && context.getPlan() != null) {
            details.addProperty("planId", context.getPlan().getId());
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

    private String invalidPlanDetails(DrPlanVO plan, String message) {
        JsonObject details = new JsonObject();
        if (plan != null) {
            details.addProperty("planId", plan.getId());
            details.addProperty("direction", plan.getDirection());
            details.addProperty("engineType", plan.getEngineType());
            details.addProperty("engineBindingType", plan.getEngineBindingType());
        }
        details.addProperty("message", message);
        return GSON.toJson(details);
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
            LOGGER.warn("Ignoring invalid VMware Phase 1 DR run request JSON for run {}: {}", run.getId(), e.getMessage());
            return new JsonObject();
        }
    }

    private String targetNetworkRef(JsonObject mapping) {
        String direct = firstString(mapping, "targetNetworkRef", "targetPortgroupRef", "targetPortgroup");
        if (StringUtils.isNotBlank(direct)) {
            return direct;
        }
        String nested = firstNestedString(mapping, new String[] {"network", "networkMapping"},
                "targetNetworkRef", "targetPortgroupRef", "targetPortgroup");
        if (StringUtils.isNotBlank(nested)) {
            return nested;
        }
        JsonArray mappings = firstArray(mapping, "networkMappings", "networks");
        if (mappings != null && mappings.size() > 0 && mappings.get(0).isJsonObject()) {
            return firstString(mappings.get(0).getAsJsonObject(), "targetNetworkRef", "targetPortgroupRef", "targetPortgroup");
        }
        return null;
    }

    private String targetNetworkPolicy(JsonObject mapping, String field) {
        String direct = firstString(mapping, field);
        if (StringUtils.isNotBlank(direct)) {
            return direct;
        }
        String nested = firstNestedString(mapping, new String[] {"network", "networkMapping"}, field);
        if (StringUtils.isNotBlank(nested)) {
            return nested;
        }
        JsonArray mappings = firstArray(mapping, "networkMappings", "networks");
        if (mappings != null && mappings.size() > 0 && mappings.get(0).isJsonObject()) {
            return firstString(mappings.get(0).getAsJsonObject(), field);
        }
        return null;
    }

    private String firstNestedString(JsonObject root, String[] objectNames, String... fieldNames) {
        String direct = firstString(root, fieldNames);
        if (StringUtils.isNotBlank(direct)) {
            return direct;
        }
        for (String objectName : objectNames) {
            JsonObject nested = firstObject(root, objectName);
            String value = firstString(nested, fieldNames);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private JsonObject firstObject(JsonObject root, String fieldName) {
        if (root == null || StringUtils.isBlank(fieldName)) {
            return null;
        }
        JsonElement value = root.get(fieldName);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private JsonArray firstArray(JsonObject root, String... fieldNames) {
        if (root == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonElement value = root.get(fieldName);
            if (value != null && value.isJsonArray()) {
                return value.getAsJsonArray();
            }
        }
        return null;
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

    private static final class VmwarePhase1Spec {
        private long planId;
        private String planUuid;
        private Long sourceVmId;
        private String sourceExternalRef;
        private String targetSiteName;
        private Long vmwareDatacenterId;
        private String endpoint;
        private String targetVmName;
        private String targetExternalRef;
        private String targetDatastoreRef;
        private String targetFolderPath;
        private String targetComputeRef;
        private String targetNetworkRef;
        private String networkConnectMode;
        private String macPolicy;

        private JsonObject toJson() {
            JsonObject root = new JsonObject();
            root.addProperty("planId", planId);
            root.addProperty("planUuid", planUuid);
            if (sourceVmId != null) {
                root.addProperty("sourceVmId", sourceVmId);
            }
            root.addProperty("sourceExternalRef", sourceExternalRef);
            root.addProperty("targetSiteName", targetSiteName);
            if (vmwareDatacenterId != null) {
                root.addProperty("vmwareDatacenterId", vmwareDatacenterId);
            }
            root.addProperty("endpoint", endpoint);
            root.addProperty("targetVmName", targetVmName);
            root.addProperty("targetExternalRef", targetExternalRef);
            root.addProperty("targetDatastoreRef", targetDatastoreRef);
            root.addProperty("targetFolderPath", targetFolderPath);
            root.addProperty("targetComputeRef", targetComputeRef);
            root.addProperty("targetNetworkRef", targetNetworkRef);
            root.addProperty("networkConnectMode", networkConnectMode);
            root.addProperty("macPolicy", macPolicy);
            return root;
        }

        private String toJsonString() {
            return GSON.toJson(toJson());
        }
    }
}
