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
package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.Date;
import java.util.List;

public class ImportVMTaskResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of importing task")
    private String id;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the Zone ID")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the Zone name")
    private String zoneName;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account name")
    private String accountName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the ID of account")
    private String accountId;

    @SerializedName(ApiConstants.CLUSTER_ID)
    @Param(description = "the cluster ID used by the import task")
    private String clusterId;

    @SerializedName(ApiConstants.SERVICE_OFFERING_ID)
    @Param(description = "the service offering ID used by the import task")
    private String serviceOfferingId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "the ID of the imported VM (after task is completed)")
    private String virtualMachineId;

    @SerializedName(ApiConstants.DISPLAY_NAME)
    @Param(description = "the display name of the importing VM")
    private String displayName;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the importing VM task")
    private String state;

    @SerializedName(ApiConstants.VCENTER)
    @Param(description = "the vcenter name of the importing VM task")
    private String vcenter;

    @SerializedName(ApiConstants.DATACENTER_NAME)
    @Param(description = "the datacenter name of the importing VM task")
    private String datacenterName;

    @SerializedName("sourcevmname")
    @Param(description = "the source VM name")
    private String sourceVMName;

    @SerializedName("migrationtool")
    @Param(description = "the migration tool used by the import task")
    private String migrationTool;

    @SerializedName("sourceprovider")
    @Param(description = "the source provider used by the import task")
    private String sourceProvider;

    @SerializedName("targetprovider")
    @Param(description = "the target provider used by the import task")
    private String targetProvider;

    @SerializedName("targetprofile")
    @Param(description = "the target profile selected for the import task")
    private String targetProfile;

    @SerializedName("targetvmname")
    @Param(description = "the target VM name selected for the import task")
    private String targetVMName;

    @SerializedName("step")
    @Param(description = "the current step on the importing VM task")
    private String step;

    @SerializedName("v2kstep")
    @Param(description = "the current ablestack-v2k step on the importing VM task")
    private String v2kStep;

    @SerializedName("stepduration")
    @Param(description = "the duration of the current step")
    private String stepDuration;

    @SerializedName(ApiConstants.DURATION)
    @Param(description = "the total task duration")
    private String duration;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the current step description on the importing VM task")
    private String description;

    @SerializedName(ApiConstants.CONVERT_INSTANCE_HOST_ID)
    @Param(description = "the ID of the host on which the instance is being converted")
    private String convertInstanceHostId;

    @SerializedName("convertinstancehostname")
    @Param(description = "the name of the host on which the instance is being converted")
    private String convertInstanceHostName;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the create date of the importing task")
    private Date created;

    @SerializedName(ApiConstants.LAST_UPDATED)
    @Param(description = "the last updated date of the importing task")
    private Date lastUpdated;

    @SerializedName("phase")
    @Param(description = "the current PHASE from ablestack-v2k status")
    private String phase;

    @SerializedName("currentphase")
    @Param(description = "the normalized current migration phase")
    private String currentPhase;

    @SerializedName("migrationstate")
    @Param(description = "the current STATE from ablestack-v2k status")
    private String migrationState;

    @SerializedName("migrationstep")
    @Param(description = "the current STEP from ablestack-v2k status")
    private String migrationStep;

    @SerializedName("syncphysical")
    @Param(description = "the current SYNC(Physical) from ablestack-v2k status")
    private String syncPhysical;

    @SerializedName("displaystep")
    @Param(description = "the normalized display step for the import task")
    private String displayStep;

    @SerializedName("syncprogresslabel")
    @Param(description = "the current sync progress label")
    private String syncProgressLabel;

    @SerializedName("syncdonebytes")
    @Param(description = "the bytes transferred in the current sync step")
    private Long syncDoneBytes;

    @SerializedName("synctotalbytes")
    @Param(description = "the total bytes for the current sync step")
    private Long syncTotalBytes;

    @SerializedName("syncpercent")
    @Param(description = "the percent completed in the current sync step")
    private Integer syncPercent;

    @SerializedName("synccumulativedonebytes")
    @Param(description = "the cumulative transferred bytes from base sync through the current sync step")
    private Long syncCumulativeDoneBytes;

    @SerializedName("synccumulativeknownbytes")
    @Param(description = "the cumulative known bytes from base sync through the current sync step")
    private Long syncCumulativeKnownBytes;

    @SerializedName("synccumulativepercent")
    @Param(description = "the cumulative sync percent from base sync through the current sync step")
    private Integer syncCumulativePercent;

    @SerializedName("workdir")
    @Param(description = "the current WORKDIR from ablestack-v2k status")
    private String workdir;

    @SerializedName("credentialstate")
    @Param(description = "the source credential state for the import task")
    private String credentialState;

    @SerializedName("availableactions")
    @Param(description = "the currently available task actions")
    private List<String> availableActions;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getServiceOfferingId() {
        return serviceOfferingId;
    }

    public void setServiceOfferingId(String serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public String getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
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

    public String getDatacenterName() {
        return datacenterName;
    }

    public void setDatacenterName(String datacenterName) {
        this.datacenterName = datacenterName;
    }

    public String getSourceVMName() {
        return sourceVMName;
    }

    public void setSourceVMName(String sourceVMName) {
        this.sourceVMName = sourceVMName;
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

    public String getTargetVMName() {
        return targetVMName;
    }

    public void setTargetVMName(String targetVMName) {
        this.targetVMName = targetVMName;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getV2kStep() {
        return v2kStep;
    }

    public void setV2kStep(String v2kStep) {
        this.v2kStep = v2kStep;
    }

    public String getStepDuration() {
        return stepDuration;
    }

    public void setStepDuration(String stepDuration) {
        this.stepDuration = stepDuration;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConvertInstanceHostId() {
        return convertInstanceHostId;
    }

    public void setConvertInstanceHostId(String convertInstanceHostId) {
        this.convertInstanceHostId = convertInstanceHostId;
    }

    public String getConvertInstanceHostName() {
        return convertInstanceHostName;
    }

    public void setConvertInstanceHostName(String convertInstanceHostName) {
        this.convertInstanceHostName = convertInstanceHostName;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
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

    public String getSyncPhysical() {
        return syncPhysical;
    }

    public void setSyncPhysical(String syncPhysical) {
        this.syncPhysical = syncPhysical;
    }

    public String getDisplayStep() {
        return displayStep;
    }

    public void setDisplayStep(String displayStep) {
        this.displayStep = displayStep;
    }

    public String getSyncProgressLabel() {
        return syncProgressLabel;
    }

    public void setSyncProgressLabel(String syncProgressLabel) {
        this.syncProgressLabel = syncProgressLabel;
    }

    public Long getSyncDoneBytes() {
        return syncDoneBytes;
    }

    public void setSyncDoneBytes(Long syncDoneBytes) {
        this.syncDoneBytes = syncDoneBytes;
    }

    public Long getSyncTotalBytes() {
        return syncTotalBytes;
    }

    public void setSyncTotalBytes(Long syncTotalBytes) {
        this.syncTotalBytes = syncTotalBytes;
    }

    public Integer getSyncPercent() {
        return syncPercent;
    }

    public void setSyncPercent(Integer syncPercent) {
        this.syncPercent = syncPercent;
    }

    public Long getSyncCumulativeDoneBytes() {
        return syncCumulativeDoneBytes;
    }

    public void setSyncCumulativeDoneBytes(Long syncCumulativeDoneBytes) {
        this.syncCumulativeDoneBytes = syncCumulativeDoneBytes;
    }

    public Long getSyncCumulativeKnownBytes() {
        return syncCumulativeKnownBytes;
    }

    public void setSyncCumulativeKnownBytes(Long syncCumulativeKnownBytes) {
        this.syncCumulativeKnownBytes = syncCumulativeKnownBytes;
    }

    public Integer getSyncCumulativePercent() {
        return syncCumulativePercent;
    }

    public void setSyncCumulativePercent(Integer syncCumulativePercent) {
        this.syncCumulativePercent = syncCumulativePercent;
    }

    public String getWorkdir() {
        return workdir;
    }

    public void setWorkdir(String workdir) {
        this.workdir = workdir;
    }

    public String getCredentialState() {
        return credentialState;
    }

    public void setCredentialState(String credentialState) {
        this.credentialState = credentialState;
    }

    public List<String> getAvailableActions() {
        return availableActions;
    }

    public void setAvailableActions(List<String> availableActions) {
        this.availableActions = availableActions;
    }
}
