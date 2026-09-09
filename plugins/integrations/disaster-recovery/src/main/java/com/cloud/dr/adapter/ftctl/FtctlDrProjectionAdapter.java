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
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrEventVO;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrReplicaDiskVO;
import com.cloud.dr.DrReplicaVO;
import com.cloud.dr.adapter.DrAdapterResult;
import com.cloud.dr.adapter.DrProjectionAdapter;
import com.cloud.dr.dao.DrEventDao;
import com.cloud.dr.dao.DrPlanDao;
import com.cloud.dr.dao.DrReplicaDao;
import com.cloud.dr.dao.DrReplicaDiskDao;
import com.cloud.ftctl.FtctlProtectionVO;
import com.cloud.ftctl.FtctlProtectionVolumeVO;
import com.cloud.ftctl.dao.FtctlProtectionDao;
import com.cloud.ftctl.dao.FtctlProtectionVolumeDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class FtctlDrProjectionAdapter extends ManagerBase implements DrProjectionAdapter {
    private static final Gson GSON = new Gson();
    private static final String DETAIL_PREFIX_FTCTL = "ftctl.";
    private static final String PROJECTION_VERSION = "1";

    @Inject
    private FtctlProtectionDao ftctlProtectionDao;
    @Inject
    private FtctlProtectionVolumeDao ftctlProtectionVolumeDao;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private VMInstanceDetailsDao vmInstanceDetailsDao;
    @Inject
    private DrPlanDao drPlanDao;
    @Inject
    private DrReplicaDao drReplicaDao;
    @Inject
    private DrReplicaDiskDao drReplicaDiskDao;
    @Inject
    private DrEventDao drEventDao;

    @Override
    public String getEngineType() {
        return DrConstants.ENGINE_TYPE_FTCTL;
    }

    @Override
    public String getEngineBindingType() {
        return DrConstants.ENGINE_BINDING_TYPE_FTCTL;
    }

    @Override
    public DrAdapterResult refreshPlanProjection(DrPlanVO plan) {
        FtctlProtectionVO protection = resolveProtection(plan);
        if (protection == null) {
            String message = "Active FTCTL protection was not found for DR plan " + plan.getId();
            markPlanError(plan, DrConstants.ERROR_FTCTL_PROTECTION_NOT_FOUND, message);
            return DrAdapterResult.failure(DrConstants.ERROR_FTCTL_PROTECTION_NOT_FOUND, message, null);
        }

        List<FtctlProtectionVolumeVO> protectionVolumes = ftctlProtectionVolumeDao.listActiveByProtectionId(protection.getId());
        VMInstanceVO primaryVm = vmInstanceDao.findById(protection.getPrimaryVmId());
        VMInstanceVO secondaryVm = protection.getSecondaryVmId() != null ? vmInstanceDao.findById(protection.getSecondaryVmId()) : findSecondaryVm(protection);

        updatePlanProjection(plan, protection, secondaryVm);
        DrReplicaVO replica = upsertReplica(plan, protection, secondaryVm, protectionVolumes);
        if (replica != null) {
            upsertReplicaDisks(replica, protectionVolumes);
        }

        String detailsJson = buildProjectionDetails(protection, protectionVolumes, primaryVm, secondaryVm);
        persistProjectionEventIfChanged(plan.getId(), detailsJson);
        return DrAdapterResult.success("FTCTL projection refreshed", detailsJson);
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

    private VMInstanceVO findSecondaryVm(FtctlProtectionVO protection) {
        if (StringUtils.isBlank(protection.getSecondaryVmName())) {
            return null;
        }
        VMInstanceVO vm = vmInstanceDao.findVMByInstanceName(protection.getSecondaryVmName());
        return vm != null ? vm : vmInstanceDao.findVMByHostName(protection.getSecondaryVmName());
    }

    private void updatePlanProjection(DrPlanVO plan, FtctlProtectionVO protection, VMInstanceVO secondaryVm) {
        boolean changed = false;
        if (plan.getSourceVmId() == null || plan.getSourceVmId() != protection.getPrimaryVmId()) {
            plan.setSourceVmId(protection.getPrimaryVmId());
            changed = true;
        }
        if (!DrConstants.ENGINE_TYPE_FTCTL.equalsIgnoreCase(plan.getEngineType())) {
            plan.setEngineType(DrConstants.ENGINE_TYPE_FTCTL);
            changed = true;
        }
        if (!DrConstants.ENGINE_BINDING_TYPE_FTCTL.equalsIgnoreCase(plan.getEngineBindingType())) {
            plan.setEngineBindingType(DrConstants.ENGINE_BINDING_TYPE_FTCTL);
            changed = true;
        }
        if (plan.getEngineBindingId() == null || plan.getEngineBindingId() != protection.getId()) {
            plan.setEngineBindingId(protection.getId());
            changed = true;
        }
        String state = mapPlanState(protection);
        if (!StringUtils.equals(plan.getState(), state)) {
            plan.setState(state);
            changed = true;
        }
        if (StringUtils.isNotBlank(protection.getAdminState()) && !StringUtils.equals(plan.getAdminState(), protection.getAdminState())) {
            plan.setAdminState(protection.getAdminState());
            changed = true;
        }
        if (secondaryVm != null && plan.getTargetReadyAt() == null) {
            plan.setTargetReadyAt(protection.getUpdated() != null ? protection.getUpdated() : protection.getCreated());
            changed = true;
        }
        if (StringUtils.isNotBlank(protection.getLastError())) {
            if (!StringUtils.equals(plan.getLastErrorCode(), "FTCTL_LAST_ERROR")) {
                plan.setLastErrorCode("FTCTL_LAST_ERROR");
                changed = true;
            }
            if (!StringUtils.equals(plan.getLastErrorMessage(), protection.getLastError())) {
                plan.setLastErrorMessage(protection.getLastError());
                changed = true;
            }
        } else {
            if (plan.getLastErrorCode() != null || plan.getLastErrorMessage() != null) {
                plan.setLastErrorCode(null);
                plan.setLastErrorMessage(null);
                changed = true;
            }
        }
        if (changed) {
            plan.markUpdated();
            drPlanDao.update(plan.getId(), plan);
        }
    }

    private void markPlanError(DrPlanVO plan, String errorCode, String message) {
        plan.setState(DrConstants.PLAN_STATE_ERROR);
        plan.setLastErrorCode(errorCode);
        plan.setLastErrorMessage(message);
        plan.markUpdated();
        drPlanDao.update(plan.getId(), plan);
    }

    private DrReplicaVO upsertReplica(DrPlanVO plan, FtctlProtectionVO protection, VMInstanceVO secondaryVm,
            List<FtctlProtectionVolumeVO> protectionVolumes) {
        Long secondaryVmId = secondaryVm != null ? secondaryVm.getId() : protection.getSecondaryVmId();
        DrReplicaVO replica = secondaryVmId != null ? drReplicaDao.findActiveByTargetVmId(secondaryVmId) : firstReplica(plan.getId());
        if (replica == null) {
            replica = new DrReplicaVO(plan.getId(), plan.getTargetSiteId());
        }

        replica.setTargetVmId(secondaryVmId);
        replica.setTargetExternalRef(resolveSecondaryExternalRef(protection, secondaryVm));
        replica.setTargetVmName(resolveSecondaryVmName(protection, secondaryVm));
        replica.setState(mapReplicaState(protection));
        replica.setPowerState(secondaryVm != null && secondaryVm.getPowerState() != null ? secondaryVm.getPowerState().toString() : null);
        replica.setHypervisorType(secondaryVm != null && secondaryVm.getHypervisorType() != null ? secondaryVm.getHypervisorType().toString() : "KVM");
        replica.setActiveSide(protection.getActiveSide());
        replica.setRuntimeStateJson(buildRuntimeStateJson(protection, protectionVolumes, secondaryVm));
        replica.markUpdated();

        if (replica.getId() == 0) {
            return drReplicaDao.persist(replica);
        }
        drReplicaDao.update(replica.getId(), replica);
        return drReplicaDao.findById(replica.getId());
    }

    private DrReplicaVO firstReplica(long planId) {
        List<DrReplicaVO> replicas = drReplicaDao.listActiveByPlanId(planId);
        return replicas.isEmpty() ? null : replicas.get(0);
    }

    private void upsertReplicaDisks(DrReplicaVO replica, List<FtctlProtectionVolumeVO> protectionVolumes) {
        for (FtctlProtectionVolumeVO protectionVolume : protectionVolumes) {
            DrReplicaDiskVO disk = drReplicaDiskDao.findActiveByReplicaIdAndSourceVolumeId(replica.getId(), protectionVolume.getPrimaryVolumeId());
            if (disk == null) {
                disk = new DrReplicaDiskVO(replica.getId(), resolveDiskLabel(protectionVolume));
            }
            disk.setSourceVolumeId(protectionVolume.getPrimaryVolumeId());
            disk.setTargetVolumeId(protectionVolume.getSecondaryVolumeId());
            disk.setSourceDiskRef(protectionVolume.getPrimaryDiskPath());
            disk.setTargetDiskRef(protectionVolume.getSecondaryDiskPath());
            disk.setFormat(inferDiskFormat(protectionVolume.getSecondaryDiskPath()));
            disk.setState(normalizeState(protectionVolume.getReplicationState(), "UNKNOWN"));
            disk.setDetailsJson(buildReplicaDiskDetails(protectionVolume));
            disk.markUpdated();
            if (disk.getId() == 0) {
                drReplicaDiskDao.persist(disk);
            } else {
                drReplicaDiskDao.update(disk.getId(), disk);
            }
        }
    }

    private String resolveSecondaryExternalRef(FtctlProtectionVO protection, VMInstanceVO secondaryVm) {
        if (secondaryVm != null) {
            return secondaryVm.getUuid();
        }
        if (protection.getSecondaryVmId() != null) {
            return String.valueOf(protection.getSecondaryVmId());
        }
        if (StringUtils.isNotBlank(protection.getSecondaryVmName())) {
            return protection.getSecondaryVmName();
        }
        return "ftctl-protection:" + protection.getId();
    }

    private String resolveSecondaryVmName(FtctlProtectionVO protection, VMInstanceVO secondaryVm) {
        if (secondaryVm != null) {
            return StringUtils.defaultIfBlank(secondaryVm.getInstanceName(), secondaryVm.getHostName());
        }
        return protection.getSecondaryVmName();
    }

    private String mapPlanState(FtctlProtectionVO protection) {
        String state = StringUtils.defaultString(protection.getProtectionState()).toLowerCase(Locale.ROOT);
        String provisioningState = StringUtils.defaultString(protection.getProvisioningState()).toLowerCase(Locale.ROOT);
        if (state.contains("error") || provisioningState.contains("failed")) {
            return DrConstants.PLAN_STATE_ERROR;
        }
        if (StringUtils.equals(state, "failed_over")) {
            return DrConstants.PLAN_STATE_FAILED_OVER;
        }
        return DrConstants.PLAN_STATE_READY;
    }

    private String mapReplicaState(FtctlProtectionVO protection) {
        String state = StringUtils.defaultIfBlank(protection.getProtectionState(), protection.getProvisioningState());
        if (StringUtils.isBlank(state)) {
            return DrConstants.REPLICA_STATE_NEW;
        }
        if (StringUtils.containsIgnoreCase(state, "error") || StringUtils.containsIgnoreCase(state, "failed")) {
            return DrConstants.REPLICA_STATE_ERROR;
        }
        return normalizeState(state, DrConstants.REPLICA_STATE_READY);
    }

    private String normalizeState(String state, String defaultState) {
        if (StringUtils.isBlank(state)) {
            return defaultState;
        }
        return state.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private String resolveDiskLabel(FtctlProtectionVolumeVO protectionVolume) {
        return StringUtils.defaultIfBlank(protectionVolume.getDiskLabel(), "volume-" + protectionVolume.getPrimaryVolumeId());
    }

    private String inferDiskFormat(String diskPath) {
        if (StringUtils.isBlank(diskPath)) {
            return null;
        }
        String value = diskPath.toLowerCase(Locale.ROOT);
        if (value.startsWith("rbd:") || value.startsWith("rbd/") || value.contains("/rbd/")) {
            return "rbd";
        }
        if (value.endsWith(".qcow2") || value.contains(".qcow2")) {
            return "qcow2";
        }
        return null;
    }

    private String buildRuntimeStateJson(FtctlProtectionVO protection, List<FtctlProtectionVolumeVO> volumes, VMInstanceVO secondaryVm) {
        JsonObject root = baseProtectionJson(protection);
        root.addProperty("standbyState", protection.getProvisioningState());
        if (secondaryVm != null) {
            root.addProperty("secondaryVmUuid", secondaryVm.getUuid());
            root.addProperty("secondaryVmState", secondaryVm.getState() != null ? secondaryVm.getState().toString() : null);
        }
        root.add("secondaryVmDetails", safeFtctlDetails(secondaryVm != null ? secondaryVm.getId() : protection.getSecondaryVmId()));
        root.add("volumeMappings", buildVolumeMappings(volumes));
        return GSON.toJson(root);
    }

    private String buildProjectionDetails(FtctlProtectionVO protection, List<FtctlProtectionVolumeVO> volumes, VMInstanceVO primaryVm, VMInstanceVO secondaryVm) {
        JsonObject root = baseProtectionJson(protection);
        root.addProperty("projectionVersion", PROJECTION_VERSION);
        if (primaryVm != null) {
            root.addProperty("primaryVmUuid", primaryVm.getUuid());
            root.addProperty("primaryVmName", primaryVm.getInstanceName());
            root.addProperty("primaryVmState", primaryVm.getState() != null ? primaryVm.getState().toString() : null);
        }
        if (secondaryVm != null) {
            root.addProperty("secondaryVmUuid", secondaryVm.getUuid());
            root.addProperty("secondaryVmName", secondaryVm.getInstanceName());
            root.addProperty("secondaryVmState", secondaryVm.getState() != null ? secondaryVm.getState().toString() : null);
        }
        root.add("primaryVmDetails", safeFtctlDetails(protection.getPrimaryVmId()));
        root.add("secondaryVmDetails", safeFtctlDetails(secondaryVm != null ? secondaryVm.getId() : protection.getSecondaryVmId()));
        root.add("volumeMappings", buildVolumeMappings(volumes));
        return GSON.toJson(root);
    }

    private JsonObject baseProtectionJson(FtctlProtectionVO protection) {
        JsonObject root = new JsonObject();
        root.addProperty("ftctlProtectionId", protection.getId());
        root.addProperty("ftctlProtectionUuid", protection.getUuid());
        root.addProperty("primaryVmId", protection.getPrimaryVmId());
        if (protection.getSecondaryVmId() != null) {
            root.addProperty("secondaryVmId", protection.getSecondaryVmId());
        }
        root.addProperty("secondaryVmName", protection.getSecondaryVmName());
        if (protection.getPeerHostId() != null) {
            root.addProperty("peerHostId", protection.getPeerHostId());
        }
        if (protection.getTargetStoragePoolId() != null) {
            root.addProperty("targetStoragePoolId", protection.getTargetStoragePoolId());
        }
        root.addProperty("mode", protection.getMode());
        root.addProperty("backendMode", protection.getBackendMode());
        root.addProperty("provisioningBackend", protection.getProvisioningBackend());
        root.addProperty("provisioningState", protection.getProvisioningState());
        root.addProperty("protectionState", protection.getProtectionState());
        root.addProperty("transportState", protection.getTransportState());
        root.addProperty("activeSide", protection.getActiveSide());
        root.addProperty("fencingPolicy", protection.getFencingPolicy());
        root.addProperty("fencingState", protection.getFencingState());
        root.addProperty("xcoloPortAllocationMode", protection.getXcoloPortAllocationMode());
        if (protection.getXcoloPortSlot() != null) {
            root.addProperty("xcoloPortSlot", protection.getXcoloPortSlot());
        }
        root.addProperty("lastError", protection.getLastError());
        return root;
    }

    private JsonArray buildVolumeMappings(List<FtctlProtectionVolumeVO> volumes) {
        JsonArray mappings = new JsonArray();
        for (FtctlProtectionVolumeVO volume : volumes) {
            JsonObject item = new JsonObject();
            item.addProperty("ftctlProtectionVolumeId", volume.getId());
            item.addProperty("primaryVolumeId", volume.getPrimaryVolumeId());
            if (volume.getSecondaryVolumeId() != null) {
                item.addProperty("secondaryVolumeId", volume.getSecondaryVolumeId());
            }
            item.addProperty("primaryDiskPath", volume.getPrimaryDiskPath());
            item.addProperty("secondaryDiskPath", volume.getSecondaryDiskPath());
            item.addProperty("diskLabel", volume.getDiskLabel());
            item.addProperty("replicationState", volume.getReplicationState());
            mappings.add(item);
        }
        return mappings;
    }

    private String buildReplicaDiskDetails(FtctlProtectionVolumeVO volume) {
        JsonObject root = new JsonObject();
        root.addProperty("ftctlProtectionVolumeId", volume.getId());
        root.addProperty("primaryDiskPath", volume.getPrimaryDiskPath());
        root.addProperty("secondaryDiskPath", volume.getSecondaryDiskPath());
        root.addProperty("replicationState", volume.getReplicationState());
        return GSON.toJson(root);
    }

    private JsonObject safeFtctlDetails(Long vmId) {
        JsonObject details = new JsonObject();
        if (vmId == null) {
            return details;
        }
        Map<String, String> keyPairs = vmInstanceDetailsDao.listDetailsKeyPairs(vmId);
        if (keyPairs == null) {
            return details;
        }
        for (Map.Entry<String, String> entry : keyPairs.entrySet()) {
            if (StringUtils.startsWith(entry.getKey(), DETAIL_PREFIX_FTCTL) && !isSensitiveKey(entry.getKey())) {
                details.addProperty(entry.getKey(), entry.getValue());
            }
        }
        return details;
    }

    private boolean isSensitiveKey(String key) {
        return StringUtils.containsIgnoreCase(key, "secret")
                || StringUtils.containsIgnoreCase(key, "password")
                || StringUtils.containsIgnoreCase(key, "token")
                || StringUtils.containsIgnoreCase(key, "credential")
                || StringUtils.containsIgnoreCase(key, "private.key");
    }

    private void persistProjectionEventIfChanged(long planId, String detailsJson) {
        DrEventVO latest = drEventDao.findLatestByPlanIdAndEventType(planId, DrConstants.EVENT_PROJECTION_REFRESH);
        if (latest != null && StringUtils.equals(latest.getDetailsJson(), detailsJson)) {
            return;
        }
        DrEventVO event = new DrEventVO(DrConstants.EVENT_PROJECTION_REFRESH, DrConstants.EVENT_SEVERITY_INFO, DrConstants.EVENT_SOURCE_FTCTL);
        event.setPlanId(planId);
        event.setMessage("FTCTL projection refreshed");
        event.setDetailsJson(detailsJson);
        drEventDao.persist(event);
    }
}
