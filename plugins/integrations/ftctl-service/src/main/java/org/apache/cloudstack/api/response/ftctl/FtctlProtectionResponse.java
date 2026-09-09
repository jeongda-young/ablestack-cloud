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
package org.apache.cloudstack.api.response.ftctl;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

import java.util.List;

public class FtctlProtectionResponse extends BaseResponse {

    @SerializedName("virtualmachineid")
    @Param(description = "the virtual machine ID")
    private Long virtualMachineId;

    @SerializedName("protectionrole")
    @Param(description = "the FTCTL protection role of the requested VM")
    private String protectionRole;

    @SerializedName("primaryvirtualmachineid")
    @Param(description = "the FTCTL primary virtual machine ID")
    private Long primaryVirtualMachineId;

    @SerializedName("primaryvirtualmachinename")
    @Param(description = "the FTCTL primary virtual machine name")
    private String primaryVirtualMachineName;

    @SerializedName("primaryvirtualmachineuuid")
    @Param(description = "the FTCTL primary virtual machine UUID")
    private String primaryVirtualMachineUuid;

    @SerializedName("primaryvirtualmachinestate")
    @Param(description = "the FTCTL primary virtual machine state from Cloud DB")
    private String primaryVirtualMachineState;

    @SerializedName("primaryvirtualmachinehostid")
    @Param(description = "the FTCTL primary virtual machine host ID from Cloud DB")
    private Long primaryVirtualMachineHostId;

    @SerializedName("primaryvirtualmachinehostname")
    @Param(description = "the FTCTL primary virtual machine host name from Cloud DB")
    private String primaryVirtualMachineHostName;

    @SerializedName("secondaryvirtualmachineid")
    @Param(description = "the FTCTL secondary virtual machine ID")
    private Long secondaryVirtualMachineId;

    @SerializedName("secondaryvirtualmachineuuid")
    @Param(description = "the FTCTL secondary virtual machine UUID")
    private String secondaryVirtualMachineUuid;

    @SerializedName("secondaryvirtualmachinedisplayname")
    @Param(description = "the FTCTL secondary virtual machine display name")
    private String secondaryVirtualMachineDisplayName;

    @SerializedName("secondaryvirtualmachinestate")
    @Param(description = "the FTCTL secondary virtual machine state from Cloud DB")
    private String secondaryVirtualMachineState;

    @SerializedName("secondaryvirtualmachinehostid")
    @Param(description = "the FTCTL secondary virtual machine host ID from Cloud DB")
    private Long secondaryVirtualMachineHostId;

    @SerializedName("secondaryvirtualmachinehostname")
    @Param(description = "the FTCTL secondary virtual machine host name from Cloud DB")
    private String secondaryVirtualMachineHostName;

    @SerializedName("enabled")
    @Param(description = "whether FTCTL protection is enabled")
    private String enabled;

    @SerializedName("mode")
    @Param(description = "the FTCTL protection mode")
    private String mode;

    @SerializedName("backendmode")
    @Param(description = "the FTCTL backend mode")
    private String backendMode;

    @SerializedName("provisioningbackend")
    @Param(description = "the FTCTL standby VM and volume provisioning backend")
    private String provisioningBackend;

    @SerializedName("provisioningstate")
    @Param(description = "the FTCTL standby VM and volume provisioning state")
    private String provisioningState;

    @SerializedName("targetstoragescope")
    @Param(description = "the FTCTL target storage scope")
    private String targetStorageScope;

    @SerializedName("targetstoragepoolid")
    @Param(description = "the FTCTL target primary storage pool ID")
    private String targetStoragePoolId;

    @SerializedName("targetstoragepoolname")
    @Param(description = "the FTCTL target primary storage pool name")
    private String targetStoragePoolName;

    @SerializedName("drpeersitetype")
    @Param(description = "the FTCTL DR peer site type")
    private String drPeerSiteType;

    @SerializedName("remotemoldapiurl")
    @Param(description = "the remote Mold API URL for DR remote site lifecycle calls")
    private String remoteMoldApiUrl;

    @SerializedName("fencingpolicy")
    @Param(description = "the FTCTL fencing policy")
    private String fencingPolicy;

    @SerializedName("peerhostid")
    @Param(description = "the FTCTL peer host ID")
    private String peerHostId;

    @SerializedName("peerhostname")
    @Param(description = "the FTCTL peer host name")
    private String peerHostName;

    @SerializedName("secondaryvmname")
    @Param(description = "the FTCTL secondary VM name")
    private String secondaryVmName;

    @SerializedName("secondarytargetdir")
    @Param(description = "the FTCTL secondary target directory")
    private String secondaryTargetDir;

    @SerializedName("secondarytargetdisk")
    @Param(description = "the FTCTL secondary target disk path summary")
    private String secondaryTargetDisk;

    @SerializedName("diskmap")
    @Param(description = "the FTCTL disk map used by the sync profile")
    private String diskMap;

    @SerializedName("secondaryvolumes")
    @Param(description = "the FTCTL secondary volume list", responseObject = FtctlProtectionVolumeResponse.class)
    private List<FtctlProtectionVolumeResponse> secondaryVolumes;

    @SerializedName("remotenbdexportaddr")
    @Param(description = "the FTCTL remote NBD export address")
    private String remoteNbdExportAddr;

    @SerializedName("xcoloproxyendpoint")
    @Param(description = "the FTCTL x-colo proxy endpoint")
    private String xcoloProxyEndpoint;

    @SerializedName("xcolonbdendpoint")
    @Param(description = "the FTCTL x-colo NBD endpoint")
    private String xcoloNbdEndpoint;

    @SerializedName("xcolomigrateuri")
    @Param(description = "the FTCTL x-colo migrate URI")
    private String xcoloMigrateUri;

    @SerializedName("ftmachinetype")
    @Param(description = "the configured KVM machine type used to determine FT compatibility")
    private String ftMachineType;

    @SerializedName("ftmachinecompatible")
    @Param(description = "whether the VM has an FT-compatible KVM machine type contract")
    private Boolean ftMachineCompatible;

    @SerializedName("ftmachinecompatibilitymessage")
    @Param(description = "the FT machine type compatibility message")
    private String ftMachineCompatibilityMessage;

    @SerializedName("protectionstate")
    @Param(description = "the last known FTCTL protection state")
    private String protectionState;

    @SerializedName("transportstate")
    @Param(description = "the last known FTCTL transport state")
    private String transportState;

    @SerializedName("activeside")
    @Param(description = "the last known FTCTL active side")
    private String activeSide;

    @SerializedName("adminstate")
    @Param(description = "the current FTCTL admin state")
    private String adminState;

    @SerializedName("fencingstate")
    @Param(description = "the current FTCTL fencing state")
    private String fencingState;

    @SerializedName("fencingresult")
    @Param(description = "the current FTCTL fencing result classification")
    private String fencingResult;

    @SerializedName("fencingreason")
    @Param(description = "the current FTCTL fencing result reason")
    private String fencingReason;

    @SerializedName("lasterror")
    @Param(description = "the last known FTCTL error")
    private String lastError;

    @SerializedName("syncprogresspercent")
    @Param(description = "the current FTCTL block copy progress percentage")
    private Double syncProgressPercent;

    @SerializedName("synccopiedbytes")
    @Param(description = "the current FTCTL block copy copied bytes")
    private Long syncCopiedBytes;

    @SerializedName("synctotalbytes")
    @Param(description = "the current FTCTL block copy total bytes")
    private Long syncTotalBytes;

    @SerializedName("syncready")
    @Param(description = "whether the current FTCTL block copy is ready")
    private Boolean syncReady;

    @SerializedName("syncdirection")
    @Param(description = "the current FTCTL block copy direction")
    private String syncDirection;

    @SerializedName("syncupdated")
    @Param(description = "the current FTCTL block copy progress update timestamp")
    private String syncUpdated;

    @SerializedName("syncprogressjson")
    @Param(description = "the current FTCTL block copy progress details as JSON")
    private String syncProgressJson;

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public void setProtectionRole(String protectionRole) {
        this.protectionRole = protectionRole;
    }

    public void setPrimaryVirtualMachineId(Long primaryVirtualMachineId) {
        this.primaryVirtualMachineId = primaryVirtualMachineId;
    }

    public void setPrimaryVirtualMachineName(String primaryVirtualMachineName) {
        this.primaryVirtualMachineName = primaryVirtualMachineName;
    }

    public void setPrimaryVirtualMachineUuid(String primaryVirtualMachineUuid) {
        this.primaryVirtualMachineUuid = primaryVirtualMachineUuid;
    }

    public void setPrimaryVirtualMachineState(String primaryVirtualMachineState) {
        this.primaryVirtualMachineState = primaryVirtualMachineState;
    }

    public void setPrimaryVirtualMachineHostId(Long primaryVirtualMachineHostId) {
        this.primaryVirtualMachineHostId = primaryVirtualMachineHostId;
    }

    public void setPrimaryVirtualMachineHostName(String primaryVirtualMachineHostName) {
        this.primaryVirtualMachineHostName = primaryVirtualMachineHostName;
    }

    public void setSecondaryVirtualMachineId(Long secondaryVirtualMachineId) {
        this.secondaryVirtualMachineId = secondaryVirtualMachineId;
    }

    public void setSecondaryVirtualMachineUuid(String secondaryVirtualMachineUuid) {
        this.secondaryVirtualMachineUuid = secondaryVirtualMachineUuid;
    }

    public void setSecondaryVirtualMachineDisplayName(String secondaryVirtualMachineDisplayName) {
        this.secondaryVirtualMachineDisplayName = secondaryVirtualMachineDisplayName;
    }

    public void setSecondaryVirtualMachineState(String secondaryVirtualMachineState) {
        this.secondaryVirtualMachineState = secondaryVirtualMachineState;
    }

    public void setSecondaryVirtualMachineHostId(Long secondaryVirtualMachineHostId) {
        this.secondaryVirtualMachineHostId = secondaryVirtualMachineHostId;
    }

    public void setSecondaryVirtualMachineHostName(String secondaryVirtualMachineHostName) {
        this.secondaryVirtualMachineHostName = secondaryVirtualMachineHostName;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setBackendMode(String backendMode) {
        this.backendMode = backendMode;
    }

    public void setProvisioningBackend(String provisioningBackend) {
        this.provisioningBackend = provisioningBackend;
    }

    public void setProvisioningState(String provisioningState) {
        this.provisioningState = provisioningState;
    }

    public void setTargetStorageScope(String targetStorageScope) {
        this.targetStorageScope = targetStorageScope;
    }

    public void setTargetStoragePoolId(String targetStoragePoolId) {
        this.targetStoragePoolId = targetStoragePoolId;
    }

    public void setTargetStoragePoolName(String targetStoragePoolName) {
        this.targetStoragePoolName = targetStoragePoolName;
    }

    public void setDrPeerSiteType(String drPeerSiteType) {
        this.drPeerSiteType = drPeerSiteType;
    }

    public void setRemoteMoldApiUrl(String remoteMoldApiUrl) {
        this.remoteMoldApiUrl = remoteMoldApiUrl;
    }

    public void setFencingPolicy(String fencingPolicy) {
        this.fencingPolicy = fencingPolicy;
    }

    public void setPeerHostId(String peerHostId) {
        this.peerHostId = peerHostId;
    }

    public void setPeerHostName(String peerHostName) {
        this.peerHostName = peerHostName;
    }

    public void setSecondaryVmName(String secondaryVmName) {
        this.secondaryVmName = secondaryVmName;
    }

    public void setSecondaryTargetDir(String secondaryTargetDir) {
        this.secondaryTargetDir = secondaryTargetDir;
    }

    public void setSecondaryTargetDisk(String secondaryTargetDisk) {
        this.secondaryTargetDisk = secondaryTargetDisk;
    }

    public void setDiskMap(String diskMap) {
        this.diskMap = diskMap;
    }

    public void setSecondaryVolumes(List<FtctlProtectionVolumeResponse> secondaryVolumes) {
        this.secondaryVolumes = secondaryVolumes;
    }

    public void setRemoteNbdExportAddr(String remoteNbdExportAddr) {
        this.remoteNbdExportAddr = remoteNbdExportAddr;
    }

    public void setXcoloProxyEndpoint(String xcoloProxyEndpoint) {
        this.xcoloProxyEndpoint = xcoloProxyEndpoint;
    }

    public void setXcoloNbdEndpoint(String xcoloNbdEndpoint) {
        this.xcoloNbdEndpoint = xcoloNbdEndpoint;
    }

    public void setXcoloMigrateUri(String xcoloMigrateUri) {
        this.xcoloMigrateUri = xcoloMigrateUri;
    }

    public void setFtMachineType(String ftMachineType) {
        this.ftMachineType = ftMachineType;
    }

    public void setFtMachineCompatible(Boolean ftMachineCompatible) {
        this.ftMachineCompatible = ftMachineCompatible;
    }

    public void setFtMachineCompatibilityMessage(String ftMachineCompatibilityMessage) {
        this.ftMachineCompatibilityMessage = ftMachineCompatibilityMessage;
    }

    public void setProtectionState(String protectionState) {
        this.protectionState = protectionState;
    }

    public void setTransportState(String transportState) {
        this.transportState = transportState;
    }

    public void setActiveSide(String activeSide) {
        this.activeSide = activeSide;
    }

    public void setAdminState(String adminState) {
        this.adminState = adminState;
    }

    public void setFencingState(String fencingState) {
        this.fencingState = fencingState;
    }

    public void setFencingResult(String fencingResult) {
        this.fencingResult = fencingResult;
    }

    public void setFencingReason(String fencingReason) {
        this.fencingReason = fencingReason;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public void setSyncProgressPercent(Double syncProgressPercent) {
        this.syncProgressPercent = syncProgressPercent;
    }

    public void setSyncCopiedBytes(Long syncCopiedBytes) {
        this.syncCopiedBytes = syncCopiedBytes;
    }

    public void setSyncTotalBytes(Long syncTotalBytes) {
        this.syncTotalBytes = syncTotalBytes;
    }

    public void setSyncReady(Boolean syncReady) {
        this.syncReady = syncReady;
    }

    public void setSyncDirection(String syncDirection) {
        this.syncDirection = syncDirection;
    }

    public void setSyncUpdated(String syncUpdated) {
        this.syncUpdated = syncUpdated;
    }

    public void setSyncProgressJson(String syncProgressJson) {
        this.syncProgressJson = syncProgressJson;
    }
}
