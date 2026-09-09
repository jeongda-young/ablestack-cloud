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
package com.cloud.dr.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrEventVO;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrReplicaDiskVO;
import com.cloud.dr.DrReplicaVO;
import com.cloud.dr.DrRunVO;
import com.cloud.dr.DrWorkerPlacementService;
import com.cloud.dr.DrWorkerRole;
import com.cloud.dr.dao.DrEventDao;
import com.cloud.dr.dao.DrPlanDao;
import com.cloud.dr.dao.DrReplicaDao;
import com.cloud.dr.dao.DrReplicaDiskDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DrProtectionOrchestratorImpl extends ManagerBase implements DrProtectionOrchestrator {
    @Inject
    private DrPlanDao drPlanDao;
    @Inject
    private DrReplicaDao drReplicaDao;
    @Inject
    private DrReplicaDiskDao drReplicaDiskDao;
    @Inject
    private DrEventDao drEventDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private DrWorkerPlacementService drWorkerPlacementService;

    @Override
    public DrPlanVO prepareSyncRun(final DrPlanVO plan, final DrRunVO run) {
        if (plan == null || !isFtctlDrPlan(plan)) {
            return plan;
        }
        return Transaction.execute(new TransactionCallback<DrPlanVO>() {
            @Override
            public DrPlanVO doInTransaction(TransactionStatus status) {
                DrPlanVO latestPlan = drPlanDao.findById(plan.getId());
                if (latestPlan == null || latestPlan.getRemoved() != null) {
                    throw new InvalidParameterValueException(DrConstants.ERROR_PLAN_NOT_FOUND + ": " + plan.getId());
                }
                materializeWorkerBindings(latestPlan);
                DrReplicaVO replica = materializeReplica(latestPlan, run);
                List<DiskMapping> diskMappings = parseDiskMappings(latestPlan.getMappingJson());
                if (diskMappings.isEmpty()) {
                    markReplicaError(latestPlan, replica, "disk mapping is required before FTCTL_DR protection sync");
                    throw new InvalidParameterValueException(DrConstants.ERROR_TARGET_MAPPING_INVALID
                            + ": disk mapping is required before FTCTL_DR protection sync");
                }
                validateDiskMappings(latestPlan, diskMappings, replica);
                materializeReplicaDisks(replica, diskMappings);

                latestPlan.markUpdated();
                drPlanDao.update(latestPlan.getId(), latestPlan);
                recordEvent(latestPlan, run, DrConstants.EVENT_PROTECTION_PREPARED, DrConstants.EVENT_SEVERITY_INFO,
                        "DR protection resources prepared; waiting for FTCTL_DR engine acceptance", buildReadinessDetails(latestPlan, replica, diskMappings));
                return drPlanDao.findById(latestPlan.getId());
            }
        });
    }

    private void materializeWorkerBindings(DrPlanVO plan) {
        Long coordinatorHostId = drWorkerPlacementService != null
                ? drWorkerPlacementService.resolveWorkerHostId(plan, DrWorkerRole.COORDINATOR) : null;
        if (coordinatorHostId == null) {
            throw new InvalidParameterValueException(DrConstants.ERROR_WORKER_BINDING_INVALID
                    + ": no eligible KVM worker is currently available");
        }
        requireHost(coordinatorHostId, "coordinator");
        if (StringUtils.isBlank(plan.getActiveSide())) {
            plan.setActiveSide("SOURCE");
        }
    }

    private boolean isRemoteKvmSource(DrPlanVO plan) {
        return plan != null
                && StringUtils.equalsIgnoreCase(plan.getDirection(), DrConstants.DIRECTION_KVM_TO_KVM)
                && plan.getSourceVmId() == null
                && StringUtils.isNotBlank(plan.getSourceExternalRef());
    }

    private void requireHost(Long hostId, String role) {
        if (hostId == null) {
            return;
        }
        if (hostDao == null || hostDao.findById(hostId) == null) {
            throw new InvalidParameterValueException(DrConstants.ERROR_WORKER_BINDING_INVALID
                    + ": " + role + " worker host was not found: " + hostId);
        }
    }

    private DrReplicaVO materializeReplica(DrPlanVO plan, DrRunVO run) {
        List<DrReplicaVO> replicas = drReplicaDao.listActiveByPlanId(plan.getId());
        DrReplicaVO replica = replicas != null && !replicas.isEmpty() ? replicas.get(0) : null;
        if (replica == null) {
            replica = new DrReplicaVO(plan.getId(), plan.getTargetSiteId());
            replica.setActiveSide("TARGET");
            replica = drReplicaDao.persist(replica);
        }
        JsonObject mapping = parseObject(plan.getMappingJson());
        Long targetVmId = firstLong(mapping, "targetVmId", "targetvmid", "replicaVmId", "targetInstanceId");
        String targetExternalRef = firstString(mapping, "targetExternalRef", "targetVmRef", "targetVmdkVmRef", "targetUuid");
        String targetVmName = firstString(mapping, "targetVmName", "replicaName", "targetName");
        JsonObject target = objectAt(mapping, "target");
        if (targetVmId == null) {
            targetVmId = firstLong(target, "vmId", "id");
        }
        if (StringUtils.isBlank(targetExternalRef)) {
            targetExternalRef = firstString(target, "externalRef", "uuid", "vmRef", "vmId");
        }
        if (StringUtils.isBlank(targetVmName)) {
            targetVmName = firstString(target, "name", "vmName");
        }
        replica.setTargetVmId(targetVmId);
        replica.setTargetExternalRef(targetExternalRef);
        replica.setTargetVmName(targetVmName);
        replica.setHypervisorType(targetHypervisor(plan));
        replica.setPowerState(DrConstants.REPLICA_POWER_STATE_POWERED_OFF);
        replica.setState(DrConstants.REPLICA_STATE_SKELETON_READY);
        replica.setRuntimeStateJson(buildReplicaRuntimeState(plan, run, "READY"));
        replica.markUpdated();
        drReplicaDao.update(replica.getId(), replica);
        return replica;
    }

    private void validateDiskMappings(DrPlanVO plan, List<DiskMapping> diskMappings, DrReplicaVO replica) {
        List<String> missing = new ArrayList<String>();
        for (DiskMapping mapping : diskMappings) {
            if (StringUtils.isBlank(mapping.sourceRef) && mapping.sourceVolumeId == null) {
                missing.add(mapping.label + ":source");
            }
            if (StringUtils.isBlank(mapping.targetRef) && mapping.targetVolumeId == null) {
                missing.add(mapping.label + ":target");
            }
        }
        if (!missing.isEmpty()) {
            markReplicaError(plan, replica, DrConstants.ERROR_TARGET_MAPPING_INVALID,
                    "disk mapping is missing required refs: " + StringUtils.join(missing, ","));
            throw new InvalidParameterValueException(DrConstants.ERROR_TARGET_MAPPING_INVALID
                    + ": disk mapping is missing required refs: " + StringUtils.join(missing, ","));
        }
        if (StringUtils.equalsIgnoreCase(DrConstants.DIRECTION_VMWARE_TO_KVM, plan.getDirection())) {
            List<String> unresolved = new ArrayList<String>();
            for (DiskMapping mapping : diskMappings) {
                if (mapping.sizeBytes == null || mapping.sizeBytes <= 0) {
                    unresolved.add(mapping.label);
                }
            }
            if (!unresolved.isEmpty()) {
                String message = "source disk size is unresolved for target preparation: " + StringUtils.join(unresolved, ",");
                markReplicaError(plan, replica, DrConstants.ERROR_TARGET_DISK_SIZE_UNRESOLVED, message);
                throw new InvalidParameterValueException(DrConstants.ERROR_TARGET_DISK_SIZE_UNRESOLVED + ": " + message);
            }
        }
    }

    private void materializeReplicaDisks(DrReplicaVO replica, List<DiskMapping> diskMappings) {
        List<DrReplicaDiskVO> existing = drReplicaDiskDao.listActiveByReplicaId(replica.getId());
        for (DiskMapping mapping : diskMappings) {
            DrReplicaDiskVO disk = findExistingDisk(existing, mapping);
            if (disk == null) {
                disk = new DrReplicaDiskVO(replica.getId(), mapping.label);
                disk = drReplicaDiskDao.persist(disk);
            }
            disk.setSourceVolumeId(mapping.sourceVolumeId);
            disk.setTargetVolumeId(mapping.targetVolumeId);
            disk.setSourceDiskRef(mapping.sourceRef);
            disk.setTargetDiskRef(mapping.targetRef);
            disk.setFormat(mapping.format);
            disk.setSizeBytes(mapping.sizeBytes);
            disk.setState(DrConstants.REPLICA_STATE_SKELETON_READY);
            disk.setDetailsJson(mapping.raw.toString());
            disk.markUpdated();
            drReplicaDiskDao.update(disk.getId(), disk);
        }
    }

    private DrReplicaDiskVO findExistingDisk(List<DrReplicaDiskVO> existing, DiskMapping mapping) {
        if (existing == null || existing.isEmpty()) {
            return null;
        }
        for (DrReplicaDiskVO disk : existing) {
            if (StringUtils.isNotBlank(mapping.label) && StringUtils.equals(mapping.label, disk.getDiskLabel())) {
                return disk;
            }
            if (StringUtils.isNotBlank(mapping.sourceRef) && StringUtils.equals(mapping.sourceRef, disk.getSourceDiskRef())) {
                return disk;
            }
            if (mapping.sourceVolumeId != null && mapping.sourceVolumeId.equals(disk.getSourceVolumeId())) {
                return disk;
            }
        }
        return null;
    }

    private List<DiskMapping> parseDiskMappings(String mappingJson) {
        JsonObject mapping = parseObject(mappingJson);
        JsonArray disks = firstArray(mapping, "disks", "diskMappings", "volumes", "volumeMappings");
        List<DiskMapping> result = new ArrayList<DiskMapping>();
        for (int i = 0; i < disks.size(); i++) {
            JsonElement item = disks.get(i);
            if (item == null || !item.isJsonObject()) {
                continue;
            }
            result.add(DiskMapping.from(item.getAsJsonObject(), i));
        }
        return result;
    }

    private void markReplicaError(DrPlanVO plan, DrReplicaVO replica, String message) {
        markReplicaError(plan, replica, DrConstants.ERROR_TARGET_MAPPING_INVALID, message);
    }

    private void markReplicaError(DrPlanVO plan, DrReplicaVO replica, String errorCode, String message) {
        if (replica != null) {
            replica.setState(DrConstants.REPLICA_STATE_ERROR);
            replica.setRuntimeStateJson(buildErrorRuntimeState(plan, errorCode, message));
            replica.markUpdated();
            drReplicaDao.update(replica.getId(), replica);
        }
        plan.setState(DrConstants.PLAN_STATE_ERROR);
        plan.setLastErrorCode(errorCode);
        plan.setLastErrorMessage(message);
        plan.markUpdated();
        drPlanDao.update(plan.getId(), plan);
        recordEvent(plan, null, DrConstants.EVENT_PROTECTION_PREPARED, DrConstants.EVENT_SEVERITY_ERROR, message, buildErrorRuntimeState(plan, errorCode, message));
    }

    private void recordEvent(DrPlanVO plan, DrRunVO run, String type, String severity, String message, String detailsJson) {
        DrEventVO event = new DrEventVO(type, severity, DrConstants.EVENT_SOURCE_CLOUD);
        event.setPlanId(plan.getId());
        if (run != null) {
            event.setRunId(run.getId());
        }
        event.setMessage(message);
        event.setDetailsJson(detailsJson);
        drEventDao.persist(event);
    }

    private String buildReplicaRuntimeState(DrPlanVO plan, DrRunVO run, String readiness) {
        JsonObject details = new JsonObject();
        details.addProperty("readiness", readiness);
        details.addProperty("planUuid", plan.getUuid());
        details.addProperty("runUuid", run != null ? run.getUuid() : null);
        details.addProperty("direction", plan.getDirection());
        details.addProperty("sourceProvider", sourceProvider(plan));
        details.addProperty("targetProvider", targetProvider(plan));
        details.addProperty("coordinatorWorkerHostId", plan.getCoordinatorWorkerHostId());
        details.addProperty("sourceWorkerHostId", plan.getSourceWorkerHostId());
        details.addProperty("targetWorkerHostId", plan.getTargetWorkerHostId());
        details.addProperty("preparedForInitialSync", true);
        return details.toString();
    }

    private String buildReadinessDetails(DrPlanVO plan, DrReplicaVO replica, List<DiskMapping> diskMappings) {
        JsonObject details = JsonParser.parseString(buildReplicaRuntimeState(plan, null, "READY")).getAsJsonObject();
        details.addProperty("replicaId", replica != null ? replica.getId() : null);
        details.addProperty("diskCount", diskMappings != null ? diskMappings.size() : 0);
        return details.toString();
    }

    private String buildErrorRuntimeState(DrPlanVO plan, String message) {
        return buildErrorRuntimeState(plan, DrConstants.ERROR_TARGET_MAPPING_INVALID, message);
    }

    private String buildErrorRuntimeState(DrPlanVO plan, String errorCode, String message) {
        JsonObject details = new JsonObject();
        details.addProperty("readiness", "ERROR");
        details.addProperty("planUuid", plan.getUuid());
        details.addProperty("direction", plan.getDirection());
        details.addProperty("errorCode", errorCode);
        details.addProperty("message", message);
        return details.toString();
    }

    private boolean isFtctlDrPlan(DrPlanVO plan) {
        return StringUtils.equalsIgnoreCase(DrConstants.ENGINE_TYPE_FTCTL_DR, plan.getEngineType())
                || StringUtils.equalsIgnoreCase(DrConstants.ENGINE_BINDING_TYPE_FTCTL_DR, plan.getEngineBindingType());
    }

    private boolean isSourceAbleStack(DrPlanVO plan) {
        return StringUtils.equals(sourceProvider(plan), "ABLESTACK");
    }

    private boolean isTargetAbleStack(DrPlanVO plan) {
        return StringUtils.equals(targetProvider(plan), "ABLESTACK");
    }

    private String sourceProvider(DrPlanVO plan) {
        return StringUtils.startsWith(StringUtils.upperCase(plan.getDirection(), Locale.ROOT), "VMWARE") ? "VMWARE" : "ABLESTACK";
    }

    private String targetProvider(DrPlanVO plan) {
        return StringUtils.endsWith(StringUtils.upperCase(plan.getDirection(), Locale.ROOT), "VMWARE") ? "VMWARE" : "ABLESTACK";
    }

    private String targetHypervisor(DrPlanVO plan) {
        return StringUtils.equals(targetProvider(plan), "VMWARE") ? DrConstants.HYPERVISOR_TYPE_VMWARE : DrConstants.HYPERVISOR_TYPE_KVM;
    }

    private Long firstNonNull(Long first, Long second, Long third) {
        if (first != null) {
            return first;
        }
        if (second != null) {
            return second;
        }
        return third;
    }

    private JsonObject parseObject(String json) {
        if (StringUtils.isBlank(json)) {
            return new JsonObject();
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException e) {
            return new JsonObject();
        }
    }

    private JsonObject objectAt(JsonObject object, String key) {
        JsonElement element = object != null ? object.get(key) : null;
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private JsonArray firstArray(JsonObject object, String... names) {
        if (object != null) {
            for (String name : names) {
                JsonElement element = object.get(name);
                if (element != null && element.isJsonArray()) {
                    return element.getAsJsonArray();
                }
            }
        }
        return new JsonArray();
    }

    private static String firstString(JsonObject object, String... names) {
        if (object == null) {
            return null;
        }
        for (String name : names) {
            JsonElement element = object.get(name);
            if (element != null && !element.isJsonNull() && element.isJsonPrimitive()) {
                String value = element.getAsString();
                if (StringUtils.isNotBlank(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private static Long firstLong(JsonObject object, String... names) {
        if (object == null) {
            return null;
        }
        for (String name : names) {
            JsonElement element = object.get(name);
            if (element != null && !element.isJsonNull() && element.isJsonPrimitive()) {
                try {
                    return element.getAsLong();
                } catch (RuntimeException ignored) {
                }
            }
        }
        return null;
    }

    private static final class DiskMapping {
        private final String label;
        private final Long sourceVolumeId;
        private final Long targetVolumeId;
        private final String sourceRef;
        private final String targetRef;
        private final String format;
        private final Long sizeBytes;
        private final JsonObject raw;

        private DiskMapping(String label, Long sourceVolumeId, Long targetVolumeId, String sourceRef, String targetRef,
                String format, Long sizeBytes, JsonObject raw) {
            this.label = label;
            this.sourceVolumeId = sourceVolumeId;
            this.targetVolumeId = targetVolumeId;
            this.sourceRef = sourceRef;
            this.targetRef = targetRef;
            this.format = format;
            this.sizeBytes = sizeBytes;
            this.raw = raw;
        }

        private static DiskMapping from(JsonObject object, int index) {
            JsonObject source = objectAtStatic(object, "source");
            JsonObject target = objectAtStatic(object, "target");
            String label = StringUtils.defaultIfBlank(firstString(object, "device", "diskLabel", "label", "targetDevice"), "disk" + index);
            String sourceRef = firstNonBlank(
                    firstString(object, "sourcePath", "sourceDiskRef", "sourceVmdkPath", "sourceDisk", "source"),
                    firstString(source, "path", "diskRef", "vmdkPath", "ref", "uuid"));
            String targetRef = firstNonBlank(
                    firstString(object, "targetPath", "targetDiskRef", "targetVmdkPath", "targetDisk", "destination", "dest", "target"),
                    firstString(target, "path", "diskRef", "vmdkPath", "ref", "uuid"));
            String format = firstNonBlank(firstString(object, "targetFormat", "format"), firstString(target, "format"), firstString(object, "sourceFormat"));
            Long sizeBytes = firstNonNullLong(firstLong(object, "sizeBytes", "virtualSize", "capacityBytes", "bytesTotal"),
                    firstLong(source, "sizeBytes", "virtualSize", "capacityBytes", "bytesTotal"),
                    firstLong(target, "sizeBytes", "virtualSize", "capacityBytes", "bytesTotal"));
            return new DiskMapping(label, firstLong(object, "sourceVolumeId", "sourcevolumeid"),
                    firstLong(object, "targetVolumeId", "targetvolumeid"), sourceRef, targetRef, format, sizeBytes, object.deepCopy());
        }

        private static JsonObject objectAtStatic(JsonObject object, String key) {
            JsonElement element = object != null ? object.get(key) : null;
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        }

        private static String firstNonBlank(String... values) {
            if (values == null) {
                return null;
            }
            for (String value : values) {
                if (StringUtils.isNotBlank(value)) {
                    return value;
                }
            }
            return null;
        }

        private static Long firstNonNullLong(Long first, Long second, Long third) {
            if (first != null) {
                return first;
            }
            if (second != null) {
                return second;
            }
            return third;
        }
    }
}
