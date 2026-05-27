//
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
//
package com.cloud.vm;

import org.apache.cloudstack.vm.ImportVmTask;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "import_vm_task")
public class ImportVMTaskVO implements ImportVmTask {

    public ImportVMTaskVO(long zoneId, long accountId, long userId, String displayName,
                          String vcenter, String datacenter, String sourceVMName, long convertHostId, long importHostId) {
        this.zoneId = zoneId;
        this.accountId = accountId;
        this.userId = userId;
        this.displayName = displayName;
        this.vcenter = vcenter;
        this.datacenter = datacenter;
        this.sourceVMName = sourceVMName;
        this.step = Step.Prepare;
        this.uuid = UUID.randomUUID().toString();
        this.convertHostId = convertHostId;
        this.importHostId = importHostId;
        this.migrationTool = ImportVmTask.MigrationTool.Legacy.getValue();
        this.sourceProvider = ImportVmTask.SourceProvider.VMware.getValue();
        this.targetProvider = ImportVmTask.TargetProvider.KVM.getValue();
        this.sourceEndpoint = vcenter;
        this.sourceRef = sourceVMName;
        this.targetVMName = displayName;
    }

    public ImportVMTaskVO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "vm_id")
    private Long vmId;
    @Column(name = "display_name")
    private String displayName;

    @Column(name = "vcenter")
    private String vcenter;

    @Column(name = "vcenter_id")
    private Long vcenterId;

    @Column(name = "vcenter_username")
    private String vcenterUsername;

    @Column(name = "vcenter_password")
    private String vcenterPassword;

    @Column(name = "datacenter")
    private String datacenter;

    @Column(name = "source_vm_name")
    private String sourceVMName;

    @Column(name = "convert_host_id")
    private long convertHostId;

    @Column(name = "import_host_id")
    private long importHostId;

    @Column(name = "step")
    private Step step;

    @Column(name = "v2k_step")
    private String v2kStep;

    @Column(name = "cluster_id")
    private Long clusterId;

    @Column(name = "service_offering_id")
    private Long serviceOfferingId;

    @Column(name = "v2k_target_storage_pool_id")
    private Long v2kTargetStoragePoolId;

    @Column(name = "source_cluster_name")
    private String sourceClusterName;

    @Column(name = "source_host_name")
    private String sourceHostName;

    @Column(name = "service_offering_details")
    private String serviceOfferingDetails;

    @Column(name = "nic_network_map")
    private String nicNetworkMap;

    @Column(name = "migration_tool")
    private String migrationTool;

    @Column(name = "source_provider")
    private String sourceProvider;

    @Column(name = "target_provider")
    private String targetProvider;

    @Column(name = "target_profile")
    private String targetProfile;

    @Column(name = "target_storage_pool_id")
    private Long targetStoragePoolId;

    @Column(name = "target_format")
    private String targetFormat;

    @Column(name = "target_storage_type")
    private String targetStorageType;

    @Column(name = "target_vm_name")
    private String targetVMName;

    @Column(name = "source_endpoint")
    private String sourceEndpoint;

    @Column(name = "source_ref")
    private String sourceRef;

    @Lob
    @Column(name = "source_inventory_json")
    private String sourceInventoryJson;

    @Lob
    @Column(name = "source_context_json")
    private String sourceContextJson;

    @Column(name = "source_credential_id")
    private Long sourceCredentialId;

    @Lob
    @Column(name = "target_context_json")
    private String targetContextJson;

    @Column(name = "workdir")
    private String workdir;

    @Column(name = "split_mode")
    private String splitMode;

    @Column(name = "current_phase")
    private String currentPhase;

    @Column(name = "migration_state")
    private String migrationState;

    @Column(name = "migration_step")
    private String migrationStep;

    @Column(name = "cutover_policy")
    private String cutoverPolicy;

    @Column(name = "status_json")
    private String statusJson;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "state")
    private TaskState state;

    @Column(name = "description")
    private String description;

    @Column(name = "duration")
    private Long duration;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getVcenter() {
        return vcenter;
    }

    public void setVcenter(String vcenter) {
        this.vcenter = vcenter;
    }

    public Long getVcenterId() {
        return vcenterId;
    }

    public void setVcenterId(Long vcenterId) {
        this.vcenterId = vcenterId;
    }

    public String getVcenterUsername() {
        return vcenterUsername;
    }

    public void setVcenterUsername(String vcenterUsername) {
        this.vcenterUsername = vcenterUsername;
    }

    public String getVcenterPassword() {
        return vcenterPassword;
    }

    public void setVcenterPassword(String vcenterPassword) {
        this.vcenterPassword = vcenterPassword;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public String getSourceVMName() {
        return sourceVMName;
    }

    public void setSourceVMName(String sourceVMName) {
        this.sourceVMName = sourceVMName;
    }

    public long getConvertHostId() {
        return convertHostId;
    }

    public void setConvertHostId(long convertHostId) {
        this.convertHostId = convertHostId;
    }

    public long getImportHostId() {
        return importHostId;
    }

    public void setImportHostId(long importHostId) {
        this.importHostId = importHostId;
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public String getV2kStep() {
        return v2kStep;
    }

    public void setV2kStep(String v2kStep) {
        this.v2kStep = v2kStep;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public void setServiceOfferingId(Long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public Long getV2kTargetStoragePoolId() {
        return v2kTargetStoragePoolId;
    }

    public void setV2kTargetStoragePoolId(Long v2kTargetStoragePoolId) {
        this.v2kTargetStoragePoolId = v2kTargetStoragePoolId;
    }

    public String getSourceClusterName() {
        return sourceClusterName;
    }

    public void setSourceClusterName(String sourceClusterName) {
        this.sourceClusterName = sourceClusterName;
    }

    public String getSourceHostName() {
        return sourceHostName;
    }

    public void setSourceHostName(String sourceHostName) {
        this.sourceHostName = sourceHostName;
    }

    public String getServiceOfferingDetails() {
        return serviceOfferingDetails;
    }

    public void setServiceOfferingDetails(String serviceOfferingDetails) {
        this.serviceOfferingDetails = serviceOfferingDetails;
    }

    public String getNicNetworkMap() {
        return nicNetworkMap;
    }

    public void setNicNetworkMap(String nicNetworkMap) {
        this.nicNetworkMap = nicNetworkMap;
    }

    public String getMigrationTool() {
        return migrationTool;
    }

    public void setMigrationTool(String migrationTool) {
        this.migrationTool = migrationTool;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public void setSourceProvider(String sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    public String getTargetProvider() {
        return targetProvider;
    }

    public void setTargetProvider(String targetProvider) {
        this.targetProvider = targetProvider;
    }

    public String getTargetProfile() {
        return targetProfile;
    }

    public void setTargetProfile(String targetProfile) {
        this.targetProfile = targetProfile;
    }

    public Long getTargetStoragePoolId() {
        return targetStoragePoolId;
    }

    public void setTargetStoragePoolId(Long targetStoragePoolId) {
        this.targetStoragePoolId = targetStoragePoolId;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public String getTargetStorageType() {
        return targetStorageType;
    }

    public void setTargetStorageType(String targetStorageType) {
        this.targetStorageType = targetStorageType;
    }

    public String getTargetVMName() {
        return targetVMName;
    }

    public void setTargetVMName(String targetVMName) {
        this.targetVMName = targetVMName;
    }

    public String getSourceEndpoint() {
        return sourceEndpoint;
    }

    public void setSourceEndpoint(String sourceEndpoint) {
        this.sourceEndpoint = sourceEndpoint;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getSourceInventoryJson() {
        return sourceInventoryJson;
    }

    public void setSourceInventoryJson(String sourceInventoryJson) {
        this.sourceInventoryJson = sourceInventoryJson;
    }

    public String getSourceContextJson() {
        return sourceContextJson;
    }

    public void setSourceContextJson(String sourceContextJson) {
        this.sourceContextJson = sourceContextJson;
    }

    public Long getSourceCredentialId() {
        return sourceCredentialId;
    }

    public void setSourceCredentialId(Long sourceCredentialId) {
        this.sourceCredentialId = sourceCredentialId;
    }

    public String getTargetContextJson() {
        return targetContextJson;
    }

    public void setTargetContextJson(String targetContextJson) {
        this.targetContextJson = targetContextJson;
    }

    public String getWorkdir() {
        return workdir;
    }

    public void setWorkdir(String workdir) {
        this.workdir = workdir;
    }

    public String getSplitMode() {
        return splitMode;
    }

    public void setSplitMode(String splitMode) {
        this.splitMode = splitMode;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public String getMigrationState() {
        return migrationState;
    }

    public void setMigrationState(String migrationState) {
        this.migrationState = migrationState;
    }

    public String getMigrationStep() {
        return migrationStep;
    }

    public void setMigrationStep(String migrationStep) {
        this.migrationStep = migrationStep;
    }

    public String getCutoverPolicy() {
        return cutoverPolicy;
    }

    public void setCutoverPolicy(String cutoverPolicy) {
        this.cutoverPolicy = cutoverPolicy;
    }

    public String getStatusJson() {
        return statusJson;
    }

    public void setStatusJson(String statusJson) {
        this.statusJson = statusJson;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public TaskState getState() {
        return state;
    }

    public void setState(TaskState state) {
        this.state = state;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
}
