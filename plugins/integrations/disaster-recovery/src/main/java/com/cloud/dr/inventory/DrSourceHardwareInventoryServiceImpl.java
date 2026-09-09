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
package com.cloud.dr.inventory;

import java.util.Map;
import java.util.LinkedHashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FtctlDrSourceHardwareAnswer;
import com.cloud.agent.api.FtctlDrSourceHardwareCommand;
import com.cloud.dr.DrConstants;
import com.cloud.dr.DrPlanReadinessValidator;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrResolvedSiteCredential;
import com.cloud.dr.DrSiteCredentialService;
import com.cloud.dr.DrSiteVO;
import com.cloud.dr.DrVmDetailReplicationPolicy;
import com.cloud.dr.DrWorkerPlacementService;
import com.cloud.dr.DrWorkerRole;
import com.cloud.dr.dao.DrSiteDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class DrSourceHardwareInventoryServiceImpl extends ManagerBase implements DrSourceHardwareInventoryService {
    @Inject
    private DrSiteDao drSiteDao;
    @Inject
    private DrSiteCredentialService drSiteCredentialService;
    @Inject
    private AgentManager agentManager;
    @Inject
    private HostDao hostDao;
    @Inject
    private DrMoldInventoryClient drMoldInventoryClient;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private VMInstanceDetailsDao vmInstanceDetailsDao;
    @Inject
    private DrWorkerPlacementService drWorkerPlacementService;

    @Override
    public DrSourceVmHardware resolve(DrPlanVO plan) {
        if (plan == null) {
            return null;
        }
        if (StringUtils.equalsIgnoreCase(plan.getDirection(), DrConstants.DIRECTION_KVM_TO_KVM)) {
            if (plan.getSourceVmId() != null) {
                return resolveLocalKvmSource(plan);
            }
            if (StringUtils.isNotBlank(plan.getSourceExternalRef())) {
                return resolveRemoteMoldSource(plan);
            }
            return DrSourceVmHardware.unavailable(null,
                    DrPlanReadinessValidator.REASON_SOURCE_HARDWARE_INVENTORY_REQUIRED,
                    "ABLESTACK source VM reference is required");
        }
        if (!StringUtils.startsWithIgnoreCase(plan.getDirection(), "VMWARE_")) {
            return null;
        }
        if (StringUtils.isBlank(plan.getSourceExternalRef())) {
            return DrSourceVmHardware.unavailable(null,
                    DrPlanReadinessValidator.REASON_SOURCE_HARDWARE_INVENTORY_REQUIRED,
                    "VMware source VM reference is required");
        }
        DrSiteVO site = drSiteDao != null ? drSiteDao.findById(plan.getSourceSiteId()) : null;
        if (site == null || site.getRemoved() != null) {
            return DrSourceVmHardware.unavailable(plan.getSourceExternalRef(), DrConstants.ERROR_SITE_NOT_FOUND,
                    "VMware source site was not found");
        }
        Long workerHostId = drWorkerPlacementService != null
                ? drWorkerPlacementService.resolveWorkerHostId(plan, DrWorkerRole.VDDK_DATA_PLANE) : null;
        return resolve(site, plan.getSourceExternalRef(), workerHostId);
    }

    private DrSourceVmHardware resolveRemoteMoldSource(DrPlanVO plan) {
        DrSiteVO site = drSiteDao != null ? drSiteDao.findById(plan.getSourceSiteId()) : null;
        if (site == null || site.getRemoved() != null) {
            return DrSourceVmHardware.unavailable(plan.getSourceExternalRef(), DrConstants.ERROR_SITE_NOT_FOUND,
                    "ABLESTACK source site was not found");
        }
        DrResolvedSiteCredential credential = null;
        try {
            credential = drSiteCredentialService.resolveCredential(site);
            if (credential == null || !credential.hasSecrets()
                    || !StringUtils.equalsIgnoreCase(credential.getCredential().getCredentialType(), DrConstants.CREDENTIAL_TYPE_MOLD_API)) {
                return DrSourceVmHardware.unavailable(plan.getSourceExternalRef(),
                        DrPlanReadinessValidator.REASON_SOURCE_HARDWARE_INVENTORY_REQUIRED,
                        "ABLESTACK source site Mold API credential is required");
            }
            Map<String, String> values = drMoldInventoryClient.getVirtualMachineHardware(credential, plan.getSourceExternalRef());
            DrSourceVmHardware hardware = new DrSourceVmHardware();
            hardware.setSourceVmRef(plan.getSourceExternalRef());
            hardware.setSourceHostUuid(values.get("sourceHostUuid"));
            hardware.setSourceHostName(values.get("sourceHostName"));
            hardware.setInstanceName(values.get("instanceName"));
            String uefiBootMode = firstNonBlank(values.get("UEFI"), values.get("uefi"));
            hardware.setUefiMode(uefiBootMode);
            hardware.setFirmware(StringUtils.isNotBlank(uefiBootMode) ? "UEFI" : "BIOS");
            hardware.setSecureBootEnabled(StringUtils.equalsIgnoreCase(uefiBootMode, "SECURE"));
            hardware.setGuestId(firstNonBlank(values.get("guestOsName"), values.get("guestOsId")));
            hardware.setCpuCount(parseInteger(values.get("cpuCount")));
            hardware.setMemoryMiB(parseLong(values.get("memoryMiB")));
            Map<String, String> sourceDetails = extractVmDetails(values);
            hardware.setVmDetails(stableKvmDetails(sourceDetails));
            applyKvmOperationBlocker(hardware, sourceDetails);
            hardware.setInventorySource("MOLD_API");
            hardware.seal();
            return hardware;
        } catch (RuntimeException e) {
            return DrSourceVmHardware.unavailable(plan.getSourceExternalRef(),
                    DrPlanReadinessValidator.REASON_SOURCE_HARDWARE_INVENTORY_REQUIRED,
                    StringUtils.defaultIfBlank(e.getMessage(), "ABLESTACK source hardware inventory failed"));
        } finally {
            if (credential != null) {
                credential.close();
            }
        }
    }

    private DrSourceVmHardware resolveLocalKvmSource(DrPlanVO plan) {
        UserVmVO vm = userVmDao != null ? userVmDao.findById(plan.getSourceVmId()) : null;
        if (vm == null || vm.getRemoved() != null) {
            return DrSourceVmHardware.unavailable(plan.getSourceExternalRef(), DrConstants.ERROR_SITE_NOT_FOUND,
                    "ABLESTACK source VM was not found");
        }
        Map<String, String> details = vmInstanceDetailsDao != null
                ? vmInstanceDetailsDao.listDetailsKeyPairs(vm.getId()) : null;
        String uefiBootMode = details != null ? firstNonBlank(details.get("UEFI"), details.get("uefi")) : null;
        DrSourceVmHardware hardware = new DrSourceVmHardware();
        hardware.setSourceVmRef(StringUtils.defaultIfBlank(plan.getSourceExternalRef(), vm.getUuid()));
        hardware.setInstanceName(vm.getInstanceName());
        HostVO host = vm.getHostId() != null && hostDao != null ? hostDao.findById(vm.getHostId()) : null;
        if (host != null) {
            hardware.setSourceHostUuid(host.getUuid());
            hardware.setSourceHostName(host.getName());
        }
        hardware.setFirmware(StringUtils.isNotBlank(uefiBootMode) ? "UEFI" : "BIOS");
        hardware.setUefiMode(uefiBootMode);
        hardware.setSecureBootEnabled(StringUtils.equalsIgnoreCase(uefiBootMode, "SECURE"));
        hardware.setVmDetails(stableKvmDetails(details));
        applyKvmOperationBlocker(hardware, details);
        hardware.setInventorySource("LOCAL_MOLD_VM_DETAILS");
        hardware.seal();
        return hardware;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private Map<String, String> extractVmDetails(Map<String, String> values) {
        Map<String, String> details = new LinkedHashMap<String, String>();
        if (values == null) {
            return details;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (StringUtils.startsWith(entry.getKey(), DrMoldInventoryClient.SOURCE_VM_DETAIL_PREFIX)) {
                details.put(StringUtils.removeStart(entry.getKey(), DrMoldInventoryClient.SOURCE_VM_DETAIL_PREFIX),
                        entry.getValue());
            }
        }
        return details;
    }

    private Map<String, String> stableKvmDetails(Map<String, String> details) {
        return DrVmDetailReplicationPolicy.copyableSourceDetails(DrConstants.DIRECTION_KVM_TO_KVM, details);
    }

    private void applyKvmOperationBlocker(DrSourceVmHardware hardware, Map<String, String> details) {
        if (hardware == null || details == null) {
            return;
        }
        String cloneState = firstNonBlank(details.get("clone.fast.status"),
                details.get("clone.fast.flatten.status"));
        if (!StringUtils.equalsAnyIgnoreCase(cloneState, "pending", "running")) {
            return;
        }
        hardware.setOperationBlockerCode(DrConstants.ERROR_SOURCE_CLONE_FLATTEN_ACTIVE);
        hardware.setOperationBlockerMessage("Source VM SharedMountPoint clone flatten is "
                + StringUtils.lowerCase(cloneState) + "; wait for dependent clone flatten completion before DR cutover");
    }

    private Integer parseInteger(String value) {
        try { return StringUtils.isBlank(value) ? null : Integer.valueOf(value); } catch (NumberFormatException e) { return null; }
    }

    private Long parseLong(String value) {
        try { return StringUtils.isBlank(value) ? null : Long.valueOf(value); } catch (NumberFormatException e) { return null; }
    }

    @Override
    public DrSourceVmHardware resolve(DrSiteVO sourceSite, String sourceVmRef, Long workerHostId) {
        if (sourceSite == null || sourceSite.getRemoved() != null) {
            return DrSourceVmHardware.unavailable(sourceVmRef, DrConstants.ERROR_SITE_NOT_FOUND,
                    "VMware source site was not found");
        }
        if (workerHostId == null) {
            return DrSourceVmHardware.unavailable(sourceVmRef,
                    DrPlanReadinessValidator.REASON_SOURCE_HARDWARE_INVENTORY_REQUIRED,
                    "A KVM worker host is required for vCenter hardware inventory");
        }
        HostVO worker = hostDao != null ? hostDao.findById(workerHostId) : null;
        if (worker == null || worker.getStatus() != Status.Up) {
            return DrSourceVmHardware.unavailable(sourceVmRef,
                    DrPlanReadinessValidator.REASON_SOURCE_HARDWARE_INVENTORY_REQUIRED,
                    "The selected vCenter inventory worker host is unavailable");
        }
        DrResolvedSiteCredential credential = null;
        try {
            credential = drSiteCredentialService.resolveCredential(sourceSite);
            if (credential == null || !credential.hasSecrets()) {
                return DrSourceVmHardware.unavailable(sourceVmRef,
                        DrPlanReadinessValidator.REASON_SOURCE_HARDWARE_INVENTORY_REQUIRED,
                        "VMware source site credential is required");
            }
            String password = secret(credential.getSecretPayload(), "password");
            FtctlDrSourceHardwareCommand command = new FtctlDrSourceHardwareCommand(
                    credential.getCredential().getEndpoint(), credential.getCredential().getPrincipal(), password,
                    credential.getCredential().getTlsVerify(), sourceVmRef);
            Answer rawAnswer = agentManager.send(workerHostId, command);
            if (!(rawAnswer instanceof FtctlDrSourceHardwareAnswer) || !rawAnswer.getResult()) {
                return DrSourceVmHardware.unavailable(sourceVmRef,
                        DrPlanReadinessValidator.REASON_SOURCE_HARDWARE_INVENTORY_REQUIRED,
                        rawAnswer != null ? rawAnswer.getDetails() : "vCenter hardware inventory returned no answer");
            }
            FtctlDrSourceHardwareAnswer answer = (FtctlDrSourceHardwareAnswer) rawAnswer;
            DrSourceVmHardware hardware = new DrSourceVmHardware();
            hardware.setSourceVmRef(sourceVmRef);
            hardware.setFirmware(answer.getFirmware());
            hardware.setSecureBootEnabled(answer.getSecureBoot());
            hardware.setGuestId(answer.getGuestId());
            hardware.setCpuCount(answer.getCpuCount());
            hardware.setMemoryMiB(answer.getMemoryMiB());
            hardware.setRootDiskController(answer.getRootDiskController());
            hardware.setDataDiskController(answer.getDataDiskController());
            hardware.setInventorySource(answer.getInventorySource());
            hardware.seal();
            return hardware;
        } catch (AgentUnavailableException | OperationTimedoutException | RuntimeException e) {
            return DrSourceVmHardware.unavailable(sourceVmRef,
                    DrPlanReadinessValidator.REASON_SOURCE_HARDWARE_INVENTORY_REQUIRED,
                    StringUtils.defaultIfBlank(e.getMessage(), "VMware source hardware inventory failed"));
        } finally {
            if (credential != null) {
                credential.close();
            }
        }
    }

    private Long firstNonNull(Long... values) {
        if (values != null) {
            for (Long value : values) {
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private String secret(JsonObject payload, String key) {
        if (payload == null || !payload.has(key)) {
            return null;
        }
        JsonElement value = payload.get(key);
        return value != null && !value.isJsonNull() ? StringUtils.trimToNull(value.getAsString()) : null;
    }
}
