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

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.lang3.StringUtils;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dr.DrConstants;
import com.cloud.dr.DrResolvedSiteCredential;
import com.cloud.dr.DrSiteCredentialService;
import com.cloud.dr.DrSiteVO;
import com.cloud.dr.dao.DrSiteDao;
import com.cloud.dr.inventory.DrMoldInventoryClient.InventoryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.service.dao.ServiceOfferingDetailsDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.utils.component.ManagerBase;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;

public class DrPlanInventoryServiceImpl extends ManagerBase implements DrPlanInventoryService {
    @Inject
    private DrSiteDao drSiteDao;
    @Inject
    private DrSiteCredentialService drSiteCredentialService;
    @Inject
    private DrMoldInventoryClient drMoldInventoryClient;
    @Inject
    private DrVmwareInventoryClient drVmwareInventoryClient;
    @Inject
    private DrSourceHardwareInventoryService drSourceHardwareInventoryService;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private ResourceManager resourceManager;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    private DataCenterDao dataCenterDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;
    @Inject
    private ServiceOfferingDetailsDao serviceOfferingDetailsDao;
    @Inject
    private DiskOfferingDao diskOfferingDao;
    @Inject
    private NetworkDao networkDao;

    @Override
    public DrPlanInventoryResult discover(DrPlanInventoryRequest request) {
        long started = System.currentTimeMillis();
        Date checkedAt = new Date();
        DrPlanInventoryResult result = new DrPlanInventoryResult();
        result.setCheckedAt(checkedAt);
        DrResolvedSiteCredential resolvedCredential = null;
        try {
            if (request == null || request.getSourceSiteId() == null || request.getTargetSiteId() == null) {
                throw new InvalidParameterValueException("source and target DR sites are required");
            }
            DrSiteVO sourceSite = requireSite(request.getSourceSiteId(), "source");
            DrSiteVO targetSite = requireSite(request.getTargetSiteId(), "target");
            result.setSourceSiteId(sourceSite.getUuid());
            result.setTargetSiteId(targetSite.getUuid());
            result.setDirection(resolveDirection(sourceSite, targetSite));
            if (request.includePlacement()) {
                populateGuidedTargetOptions(result, sourceSite, targetSite);
            }
            resolvedCredential = drSiteCredentialService != null ? drSiteCredentialService.resolveCredential(sourceSite) : null;
            if (resolvedCredential == null || !resolvedCredential.hasSecrets()) {
                return complete(result, DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_CREDENTIAL_MISSING,
                        "source site credential is required to discover source workloads", started);
            }

            String credentialType = resolvedCredential.getCredential().getCredentialType();
            List<DrInventoryOption> workloads;
            if (StringUtils.equalsIgnoreCase(DrConstants.CREDENTIAL_TYPE_MOLD_API, credentialType)) {
                workloads = drMoldInventoryClient.listVirtualMachines(resolvedCredential, request.getKeyword(),
                        sourceSite.getZoneExternalId(), sourceSite.getZoneId());
            } else if (StringUtils.equalsIgnoreCase(DrConstants.CREDENTIAL_TYPE_VCENTER, credentialType)) {
                workloads = drVmwareInventoryClient.listVirtualMachines(resolvedCredential, request.getKeyword());
            } else {
                return complete(result, DrConstants.HEALTH_UNKNOWN, DrConstants.HEALTH_REASON_UNSUPPORTED_SITE_TYPE,
                        "source site credential type does not support workload discovery", started);
            }
            classifyLocalCloudReferences(workloads);
            result.setSourceWorkloads(workloads);
            if (StringUtils.equalsIgnoreCase(DrConstants.CREDENTIAL_TYPE_MOLD_API, credentialType)
                    && StringUtils.isNotBlank(request.getSourceExternalRef())) {
                if (request.includeDisks()) {
                    result.setSourceDisks(drMoldInventoryClient.listVirtualMachineDisks(resolvedCredential,
                            request.getSourceExternalRef()));
                    if (result.getSourceDisks().isEmpty()) {
                        result.addBlockingReason("SOURCE_DISK_INVENTORY_REQUIRED");
                    }
                }
                if (request.includeNetworks()) {
                    result.setSourceNics(drMoldInventoryClient.listVirtualMachineNics(resolvedCredential,
                            request.getSourceExternalRef()));
                }
                Map<String, String> sourceHardware = drMoldInventoryClient.getVirtualMachineHardware(
                        resolvedCredential, request.getSourceExternalRef());
                result.setSourceHardware(sourceHardware);
                applySourceHardwareDetails(workloads, request.getSourceExternalRef(), sourceHardware);
            }
            if (StringUtils.equalsIgnoreCase(DrConstants.CREDENTIAL_TYPE_VCENTER, credentialType)
                    && StringUtils.isNotBlank(request.getSourceExternalRef())) {
                if (request.includeDisks()) {
                    result.setSourceDisks(drVmwareInventoryClient.listVirtualMachineDisks(resolvedCredential, request.getSourceExternalRef()));
                    if (result.getSourceDisks().isEmpty()) {
                        result.addBlockingReason("SOURCE_DISK_INVENTORY_REQUIRED");
                    }
                }
                if (request.includeNetworks()) {
                    result.setSourceNics(drVmwareInventoryClient.listVirtualMachineNics(resolvedCredential, request.getSourceExternalRef()));
                }
                DrSourceVmHardware sourceHardware = drSourceHardwareInventoryService.resolve(sourceSite,
                        request.getSourceExternalRef(), firstWorkerHostId(result.getCoordinatorWorkerHosts()));
                result.setSourceHardware(sourceHardware.toDetails());
                applySourceHardwareDetails(workloads, request.getSourceExternalRef(), sourceHardware);
                if (!sourceHardware.isComplete()) {
                    result.addBlockingReason(StringUtils.defaultIfBlank(sourceHardware.getErrorCode(),
                            "SOURCE_HARDWARE_INVENTORY_REQUIRED"));
                }
            }
            return complete(result, DrConstants.HEALTH_CONNECTED, DrConstants.HEALTH_REASON_MOLD_API_OK,
                    "source workloads were discovered", started);
        } catch (InventoryException e) {
            String reason = e.getResponseCode() == 401 || e.getResponseCode() == 403
                    ? DrConstants.HEALTH_REASON_CREDENTIAL_INVALID
                    : (e.getResponseCode() > 0 ? DrConstants.HEALTH_REASON_ENDPOINT_HTTP_ERROR : DrConstants.HEALTH_REASON_ENDPOINT_UNREACHABLE);
            return complete(result, DrConstants.HEALTH_DISCONNECTED, reason, e.getMessage(), started);
        } finally {
            if (resolvedCredential != null) {
                resolvedCredential.close();
            }
        }
    }

    private Long firstWorkerHostId(List<DrInventoryOption> workers) {
        if (workers == null) {
            return null;
        }
        for (DrInventoryOption worker : workers) {
            if (worker == null || StringUtils.isBlank(worker.getLocalId())) {
                continue;
            }
            try {
                return Long.valueOf(worker.getLocalId());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private void applySourceHardwareDetails(List<DrInventoryOption> workloads, String sourceExternalRef,
            DrSourceVmHardware hardware) {
        applySourceHardwareDetails(workloads, sourceExternalRef, hardware != null ? hardware.toDetails() : null);
    }

    private void applySourceHardwareDetails(List<DrInventoryOption> workloads, String sourceExternalRef,
            Map<String, String> hardware) {
        if (workloads == null || hardware == null) {
            return;
        }
        for (DrInventoryOption workload : workloads) {
            if (workload != null && StringUtils.equals(sourceExternalRef, workload.getExternalRef())) {
                for (Map.Entry<String, String> entry : hardware.entrySet()) {
                    workload.putDetail(entry.getKey(), entry.getValue());
                }
                return;
            }
        }
    }

    private DrSiteVO requireSite(long siteId, String role) {
        DrSiteVO site = drSiteDao.findById(siteId);
        if (site == null || site.getRemoved() != null) {
            throw new InvalidParameterValueException(DrConstants.ERROR_SITE_NOT_FOUND + ": " + role + " site " + siteId);
        }
        return site;
    }

    private String resolveDirection(DrSiteVO sourceSite, DrSiteVO targetSite) {
        boolean sourceVmware = isVmware(sourceSite);
        boolean targetVmware = isVmware(targetSite);
        if (sourceVmware && targetVmware) {
            return DrConstants.DIRECTION_VMWARE_TO_VMWARE;
        }
        if (sourceVmware) {
            return DrConstants.DIRECTION_VMWARE_TO_KVM;
        }
        if (targetVmware) {
            return DrConstants.DIRECTION_KVM_TO_VMWARE;
        }
        return DrConstants.DIRECTION_KVM_TO_KVM;
    }

    private boolean isVmware(DrSiteVO site) {
        return StringUtils.equalsIgnoreCase(DrConstants.HYPERVISOR_TYPE_VMWARE, site.getHypervisorType())
                || StringUtils.equalsIgnoreCase("VMWARE", site.getHypervisorType());
    }

    private boolean isKvm(DrSiteVO site) {
        return StringUtils.equalsIgnoreCase(DrConstants.HYPERVISOR_TYPE_KVM, site.getHypervisorType())
                || StringUtils.equalsIgnoreCase("KVM", site.getHypervisorType());
    }

    private void populateGuidedTargetOptions(DrPlanInventoryResult result, DrSiteVO sourceSite, DrSiteVO targetSite) {
        ResolvedTargetZone targetZone = resolveTargetZone(targetSite);
        if (targetZone.getOption() != null) {
            result.setTargetZone(targetZone.getOption());
        }
        if (targetZone.getWarning() != null) {
            result.addWarning(targetZone.getWarning());
        }
        if (targetZone.getBlockingReason() != null) {
            result.addBlockingReason(targetZone.getBlockingReason());
        }
        List<DrInventoryOption> sourceWorkerHosts = listWorkerHosts(sourceSite, sourceSite.getZoneId(), "SOURCE_WORKER_HOST");
        List<DrInventoryOption> targetWorkerHosts = listWorkerHosts(targetSite, targetZone.getZoneId(), "TARGET_WORKER_HOST");
        result.setSourceWorkerHosts(sourceWorkerHosts);
        result.setTargetWorkerHosts(targetWorkerHosts);
        result.setCoordinatorWorkerHosts(mergeOptions(sourceWorkerHosts, targetWorkerHosts));
        result.setTargetStorageOptions(listTargetStorageOptions(targetSite, targetZone.getZoneId()));
        if (isKvm(targetSite)) {
            List<DrInventoryOption> serviceOfferings = listTargetServiceOfferingOptions(targetZone.getZoneId());
            result.setTargetServiceOfferings(serviceOfferings);
            result.setTargetComputeOptions(serviceOfferings);
            result.setTargetDiskOfferings(listTargetDiskOfferingOptions(targetZone.getZoneId()));
        } else {
            result.setTargetComputeOptions(listTargetComputeOptions(targetSite, targetWorkerHosts));
        }
        result.setTargetNetworkOptions(listTargetNetworkOptions(targetSite, targetZone.getZoneId()));
        result.setTargetFolderOptions(listTargetFolderOptions(targetSite));
    }

    private List<DrInventoryOption> listWorkerHosts(DrSiteVO site, String type) {
        return listWorkerHosts(site, site != null ? site.getZoneId() : null, type);
    }

    private List<DrInventoryOption> listWorkerHosts(DrSiteVO site, Long zoneId, String type) {
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        if (!isKvm(site) || zoneId == null || resourceManager == null) {
            return options;
        }
        List<HostVO> hosts = resourceManager.listAllUpAndEnabledHostsInOneZoneByHypervisor(Hypervisor.HypervisorType.KVM, zoneId);
        if (hosts == null) {
            return options;
        }
        for (HostVO host : hosts) {
            if (host == null || StringUtils.isBlank(host.getUuid())) {
                continue;
            }
            DrInventoryOption option = new DrInventoryOption();
            option.setType(type);
            option.setId(host.getUuid());
            option.setValue(host.getUuid());
            option.setExternalId(host.getUuid());
            option.setLocalId(String.valueOf(host.getId()));
            option.setReferenceType("CLOUD_HOST_ID");
            option.setName(StringUtils.defaultIfBlank(host.getName(), host.getUuid()));
            option.setDescription(host.getPrivateIpAddress());
            option.setState(host.getStatus() != null ? host.getStatus().toString() : null);
            option.setHypervisorType(host.getHypervisorType() != null ? host.getHypervisorType().toString() : null);
            option.setSelectable(true);
            option.putDetail("hostId", String.valueOf(host.getId()));
            option.putDetail("zoneId", String.valueOf(host.getDataCenterId()));
            putDetailIfNotBlank(option, "speed", host.getSpeed());
            putDetailIfNotBlank(option, "cpuCount", host.getCpus());
            putDetailIfNotBlank(option, "memoryBytes", host.getTotalMemory());
            if (host.getClusterId() != null) {
                option.putDetail("clusterId", String.valueOf(host.getClusterId()));
            }
            options.add(option);
        }
        return options;
    }

    private List<DrInventoryOption> listTargetStorageOptions(DrSiteVO targetSite) {
        return listTargetStorageOptions(targetSite, targetSite != null ? targetSite.getZoneId() : null);
    }

    private List<DrInventoryOption> listTargetStorageOptions(DrSiteVO targetSite, Long zoneId) {
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        if (!isKvm(targetSite) || zoneId == null || primaryDataStoreDao == null) {
            return options;
        }
        List<StoragePoolVO> pools = primaryDataStoreDao.listByDataCenterId(zoneId);
        if (pools == null) {
            return options;
        }
        for (StoragePoolVO pool : pools) {
            if (pool == null || pool.getRemoved() != null || pool.getStatus() != StoragePoolStatus.Up || StringUtils.isBlank(pool.getUuid())) {
                continue;
            }
            DrInventoryOption option = new DrInventoryOption();
            option.setType("TARGET_STORAGE");
            option.setId(pool.getUuid());
            option.setValue(pool.getUuid());
            option.setExternalId(pool.getUuid());
            option.setLocalId(String.valueOf(pool.getId()));
            option.setReferenceType("CLOUD_STORAGE_POOL_ID");
            option.setName(StringUtils.defaultIfBlank(pool.getName(), pool.getUuid()));
            option.setDescription(pool.getPoolType() != null ? pool.getPoolType().toString() : null);
            option.setState(pool.getStatus() != null ? pool.getStatus().toString() : null);
            option.setSelectable(true);
            option.putDetail("poolId", String.valueOf(pool.getId()));
            option.putDetail("zoneId", String.valueOf(pool.getDataCenterId()));
            option.putDetail("poolType", pool.getPoolType() != null ? pool.getPoolType().toString() : null);
            option.putDetail("path", pool.getPath());
            option.putDetail("hostAddress", pool.getHostAddress());
            option.putDetail("krbdPath", pool.getKrbdPath());
            if (pool.getClusterId() != null) {
                option.putDetail("clusterId", String.valueOf(pool.getClusterId()));
            }
            if (pool.getScope() != null) {
                option.putDetail("scope", pool.getScope().toString());
            }
            options.add(option);
        }
        return options;
    }

    private List<DrInventoryOption> listTargetComputeOptions(DrSiteVO targetSite, List<DrInventoryOption> targetWorkerHosts) {
        if (isKvm(targetSite)) {
            return targetWorkerHosts != null ? targetWorkerHosts : new ArrayList<DrInventoryOption>();
        }
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        if (isVmware(targetSite) && StringUtils.isNotBlank(targetSite.getVmwareDatacenterExternalId())) {
            DrInventoryOption option = new DrInventoryOption();
            option.setType("TARGET_COMPUTE");
            option.setId(targetSite.getVmwareDatacenterExternalId());
            option.setValue(targetSite.getVmwareDatacenterExternalId());
            option.setExternalId(targetSite.getVmwareDatacenterExternalId());
            option.setLocalId(targetSite.getVmwareDatacenterId() != null ? String.valueOf(targetSite.getVmwareDatacenterId()) : null);
            option.setReferenceType("VMWARE_DATACENTER");
            option.setName(StringUtils.defaultIfBlank(targetSite.getVmwareDatacenterName(), targetSite.getVmwareDatacenterExternalId()));
            option.setSelectable(true);
            options.add(option);
        }
        return options;
    }

    private List<DrInventoryOption> listTargetNetworkOptions(DrSiteVO targetSite) {
        return listTargetNetworkOptions(targetSite, targetSite != null ? targetSite.getZoneId() : null);
    }

    private ResolvedTargetZone resolveTargetZone(DrSiteVO targetSite) {
        if (!isKvm(targetSite)) {
            return ResolvedTargetZone.notRequired();
        }
        if (targetSite.getZoneId() != null) {
            return ResolvedTargetZone.of(targetSite.getZoneId(), targetSite.getZoneName(), targetSite.getZoneExternalId(), false);
        }
        if (dataCenterDao != null) {
            List<DataCenterVO> enabledZones = dataCenterDao.listEnabledZones();
            if (enabledZones != null && enabledZones.size() == 1) {
                DataCenterVO zone = enabledZones.get(0);
                return ResolvedTargetZone.of(zone.getId(), zone.getName(), zone.getUuid(), true);
            }
        }
        return ResolvedTargetZone.blocked("TARGET_SITE_ZONE_REQUIRED");
    }

    private List<DrInventoryOption> listTargetServiceOfferingOptions(Long zoneId) {
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        if (zoneId == null || serviceOfferingDao == null) {
            return options;
        }
        List<ServiceOfferingVO> offerings = serviceOfferingDao.listAll();
        if (offerings == null) {
            return options;
        }
        for (ServiceOfferingVO offering : offerings) {
            if (offering == null || offering.getRemoved() != null || offering.isSystemUse()
                    || !StringUtils.equalsIgnoreCase(String.valueOf(offering.getState()), "Active")
                    || StringUtils.isBlank(offering.getUuid())) {
                continue;
            }
            DrInventoryOption option = new DrInventoryOption();
            option.setType("TARGET_SERVICE_OFFERING");
            option.setId(offering.getUuid());
            option.setValue(offering.getUuid());
            option.setExternalId(offering.getUuid());
            option.setLocalId(String.valueOf(offering.getId()));
            option.setReferenceType("CLOUD_SERVICE_OFFERING_ID");
            option.setName(StringUtils.defaultIfBlank(offering.getName(), offering.getUuid()));
            option.setDescription(offering.getDisplayText());
            option.setState(offering.getState() != null ? offering.getState().toString() : null);
            option.setSelectable(true);
            option.putDetail("serviceOfferingId", String.valueOf(offering.getId()));
            option.putDetail("zoneId", String.valueOf(zoneId));
            option.putDetail("customized", String.valueOf(offering.isCustomized()));
            option.putDetail("dynamic", String.valueOf(offering.isDynamic()));
            option.putDetail("requiresCpuNumber", String.valueOf(offering.getCpu() == null));
            option.putDetail("requiresCpuSpeed", String.valueOf(offering.getSpeed() == null));
            option.putDetail("requiresMemory", String.valueOf(offering.getRamSize() == null));
            putDetailIfNotBlank(option, "cpu", offering.getCpu());
            putDetailIfNotBlank(option, "memoryMb", offering.getRamSize());
            putDetailIfNotBlank(option, "speed", offering.getSpeed());
            putDetailIfNotBlank(option, ApiConstants.MIN_CPU_NUMBER, offeringDetail(offering, ApiConstants.MIN_CPU_NUMBER));
            putDetailIfNotBlank(option, ApiConstants.MAX_CPU_NUMBER, offeringDetail(offering, ApiConstants.MAX_CPU_NUMBER));
            putDetailIfNotBlank(option, ApiConstants.MIN_MEMORY, offeringDetail(offering, ApiConstants.MIN_MEMORY));
            putDetailIfNotBlank(option, ApiConstants.MAX_MEMORY, offeringDetail(offering, ApiConstants.MAX_MEMORY));
            options.add(option);
        }
        return options;
    }

    private List<DrInventoryOption> listTargetDiskOfferingOptions(Long zoneId) {
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        if (zoneId == null || diskOfferingDao == null) {
            return options;
        }
        List<DiskOfferingVO> offerings = diskOfferingDao.listAllActiveAndNonComputeDiskOfferings();
        if (offerings == null) {
            return options;
        }
        for (DiskOfferingVO offering : offerings) {
            if (offering == null || offering.getRemoved() != null || offering.isComputeOnly()
                    || !StringUtils.equalsIgnoreCase(String.valueOf(offering.getState()), "Active")
                    || StringUtils.isBlank(offering.getUuid())) {
                continue;
            }
            DrInventoryOption option = new DrInventoryOption();
            option.setType("TARGET_DISK_OFFERING");
            option.setId(offering.getUuid());
            option.setValue(offering.getUuid());
            option.setExternalId(offering.getUuid());
            option.setLocalId(String.valueOf(offering.getId()));
            option.setReferenceType("CLOUD_DISK_OFFERING_ID");
            option.setName(StringUtils.defaultIfBlank(offering.getName(), offering.getUuid()));
            option.setDescription(offering.getDisplayText());
            option.setState(offering.getState() != null ? offering.getState().toString() : null);
            option.setSelectable(true);
            option.putDetail("diskOfferingId", String.valueOf(offering.getId()));
            option.putDetail("zoneId", String.valueOf(zoneId));
            option.putDetail("diskSizeBytes", String.valueOf(offering.getDiskSize()));
            option.putDetail("provisioningType", offering.getProvisioningType() != null ? offering.getProvisioningType().toString() : "");
            option.putDetail("customized", String.valueOf(offering.isCustomized()));
            options.add(option);
        }
        return options;
    }

    private List<DrInventoryOption> listTargetNetworkOptions(DrSiteVO targetSite, Long zoneId) {
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        if (!isKvm(targetSite) || zoneId == null || networkDao == null) {
            return options;
        }
        List<NetworkVO> networks = networkDao.listByZone(zoneId);
        if (networks == null) {
            return options;
        }
        for (NetworkVO network : networks) {
            if (network == null || network.getRemoved() != null || StringUtils.isBlank(network.getUuid())
                    || network.getTrafficType() != TrafficType.Guest
                    || network.getState() == Network.State.Destroy
                    || network.getState() == Network.State.Shutdown) {
                continue;
            }
            DrInventoryOption option = new DrInventoryOption();
            option.setType("TARGET_NETWORK");
            option.setId(network.getUuid());
            option.setValue(network.getUuid());
            option.setExternalId(network.getUuid());
            option.setLocalId(String.valueOf(network.getId()));
            option.setReferenceType("CLOUD_NETWORK_ID");
            option.setName(StringUtils.defaultIfBlank(network.getName(), network.getUuid()));
            option.setDescription(network.getDisplayText());
            option.setState(network.getState() != null ? network.getState().toString() : null);
            option.setSelectable(true);
            option.putDetail("networkId", String.valueOf(network.getId()));
            option.putDetail("zoneId", String.valueOf(network.getDataCenterId()));
            option.putDetail("guestType", network.getGuestType() != null ? network.getGuestType().toString() : "");
            option.putDetail("trafficType", network.getTrafficType() != null ? network.getTrafficType().toString() : "");
            option.putDetail("cidr", StringUtils.defaultString(network.getCidr()));
            option.putDetail("networkOfferingId", String.valueOf(network.getNetworkOfferingId()));
            options.add(option);
        }
        return options;
    }

    private List<DrInventoryOption> listTargetFolderOptions(DrSiteVO targetSite) {
        return new ArrayList<DrInventoryOption>();
    }

    private List<DrInventoryOption> mergeOptions(List<DrInventoryOption> first, List<DrInventoryOption> second) {
        Map<String, DrInventoryOption> optionsByValue = new LinkedHashMap<String, DrInventoryOption>();
        addOptions(optionsByValue, first);
        addOptions(optionsByValue, second);
        return new ArrayList<DrInventoryOption>(optionsByValue.values());
    }

    private void addOptions(Map<String, DrInventoryOption> optionsByValue, List<DrInventoryOption> options) {
        if (options == null) {
            return;
        }
        for (DrInventoryOption option : options) {
            if (option != null && StringUtils.isNotBlank(option.getValue())) {
                optionsByValue.put(option.getValue(), option);
            }
        }
    }

    private String offeringDetail(ServiceOfferingVO offering, String key) {
        return offering != null && serviceOfferingDetailsDao != null && StringUtils.isNotBlank(key)
                ? serviceOfferingDetailsDao.getDetail(offering.getId(), key)
                : null;
    }

    private void putDetailIfNotBlank(DrInventoryOption option, String key, Object value) {
        String text = value != null ? StringUtils.trimToNull(String.valueOf(value)) : null;
        if (option != null && StringUtils.isNotBlank(key) && StringUtils.isNotBlank(text)
                && !StringUtils.equalsIgnoreCase(text, "null")) {
            option.putDetail(key, text);
        }
    }

    private void classifyLocalCloudReferences(List<DrInventoryOption> workloads) {
        if (workloads == null || userVmDao == null) {
            return;
        }
        for (DrInventoryOption option : workloads) {
            String uuid = StringUtils.trimToNull(option.getExternalId());
            if (uuid == null) {
                continue;
            }
            UserVmVO localVm = userVmDao.findByUuid(uuid);
            if (localVm == null || localVm.getRemoved() != null) {
                continue;
            }
            option.setReferenceType("CLOUD_VM_ID");
            option.setSourceVmId(localVm.getId());
            option.setExternalRef(null);
            option.setValue(uuid);
            option.putDetail("localVmId", String.valueOf(localVm.getId()));
            if (StringUtils.isBlank(option.getHypervisorType()) && localVm.getHypervisorType() != null) {
                option.setHypervisorType(localVm.getHypervisorType().toString());
            }
            if (StringUtils.isBlank(option.getState()) && localVm.getState() != null) {
                option.setState(localVm.getState().toString());
            }
        }
    }

    private static class ResolvedTargetZone {
        private final Long zoneId;
        private final DrInventoryOption option;
        private final String blockingReason;
        private final String warning;

        private ResolvedTargetZone(Long zoneId, DrInventoryOption option, String blockingReason, String warning) {
            this.zoneId = zoneId;
            this.option = option;
            this.blockingReason = blockingReason;
            this.warning = warning;
        }

        static ResolvedTargetZone notRequired() {
            return new ResolvedTargetZone(null, null, null, null);
        }

        static ResolvedTargetZone blocked(String blockingReason) {
            return new ResolvedTargetZone(null, null, blockingReason, null);
        }

        static ResolvedTargetZone of(Long zoneId, String name, String externalId, boolean inferred) {
            DrInventoryOption option = new DrInventoryOption();
            option.setType("TARGET_ZONE");
            option.setId(String.valueOf(zoneId));
            option.setValue(String.valueOf(zoneId));
            option.setExternalId(externalId);
            option.setLocalId(String.valueOf(zoneId));
            option.setReferenceType("CLOUD_ZONE_ID");
            option.setName(StringUtils.defaultIfBlank(name, String.valueOf(zoneId)));
            option.setSelectable(true);
            option.putDetail("zoneId", String.valueOf(zoneId));
            option.putDetail("inferred", String.valueOf(inferred));
            return new ResolvedTargetZone(zoneId, option, null, inferred ? "TARGET_SITE_ZONE_INFERRED" : null);
        }

        Long getZoneId() {
            return zoneId;
        }

        DrInventoryOption getOption() {
            return option;
        }

        String getBlockingReason() {
            return blockingReason;
        }

        String getWarning() {
            return warning;
        }
    }

    private DrPlanInventoryResult complete(DrPlanInventoryResult result, String state, String reasonCode, String message, long started) {
        result.setHealthState(state);
        result.setReasonCode(reasonCode);
        result.setMessage(message);
        result.setLatencyMs(System.currentTimeMillis() - started);
        if (result.getCheckedAt() == null) {
            result.setCheckedAt(new Date());
        }
        return result;
    }
}
