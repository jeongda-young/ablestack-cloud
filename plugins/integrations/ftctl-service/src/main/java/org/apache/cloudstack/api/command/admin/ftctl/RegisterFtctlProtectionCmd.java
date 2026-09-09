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
package org.apache.cloudstack.api.command.admin.ftctl;

import com.cloud.ftctl.FtctlService;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlProtectionResponse;

import javax.inject.Inject;

@APICommand(name = RegisterFtctlProtectionCmd.APINAME,
        description = "Registers FTCTL protection settings for a virtual machine",
        responseObject = FtctlProtectionResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        authorized = {RoleType.Admin})
public class RegisterFtctlProtectionCmd extends BaseAsyncCmd {
    public static final String APINAME = "registerFtctlProtection";

    @Inject
    private FtctlService ftctlService;

    @Parameter(name = "virtualmachineid", type = CommandType.UUID, entityType = UserVmResponse.class,
            required = true, description = "the virtual machine ID")
    private Long virtualMachineId;

    @Parameter(name = "mode", type = CommandType.STRING, required = true, description = "the FTCTL protection mode")
    private String mode;

    @Parameter(name = "backendmode", type = CommandType.STRING, description = "the FTCTL backend mode")
    private String backendMode;

    @Parameter(name = "targetstoragescope", type = CommandType.STRING, description = "the FTCTL target storage scope")
    private String targetStorageScope;

    @Parameter(name = "targetstoragepoolid", type = CommandType.UUID, entityType = StoragePoolResponse.class,
            description = "the target primary storage pool ID for FTCTL HA/DR/FT protection")
    private Long targetStoragePoolId;

    @Parameter(name = "drpeersitetype", type = CommandType.STRING,
            description = "the DR peer site management type: local-mold or remote-mold")
    private String drPeerSiteType;

    @Parameter(name = "remotemoldapiurl", type = CommandType.STRING,
            description = "the remote Mold API URL used only for DR remote site lookup")
    private String remoteMoldApiUrl;

    @Parameter(name = "remotemoldapikey", type = CommandType.STRING,
            description = "the remote Mold API key used only by Cloud backend for DR remote site lookup")
    private String remoteMoldApiKey;

    @Parameter(name = "remotemoldsecretkey", type = CommandType.STRING,
            description = "the remote Mold secret key used only by Cloud backend for DR remote site lookup")
    private String remoteMoldSecretKey;

    @Parameter(name = "remotepeerhostuuid", type = CommandType.STRING,
            description = "the remote Mold peer host UUID for DR")
    private String remotePeerHostUuid;

    @Parameter(name = "remotepeerhostname", type = CommandType.STRING,
            description = "the remote Mold peer host name for DR")
    private String remotePeerHostName;

    @Parameter(name = "remotepeerhostaddress", type = CommandType.STRING,
            description = "the remote Mold peer host management address for DR")
    private String remotePeerHostAddress;

    @Parameter(name = "remotepeerhostblockcopyaddress", type = CommandType.STRING,
            description = "the remote Mold peer host blockcopy/data-transfer address for DR")
    private String remotePeerHostBlockcopyAddress;

    @Parameter(name = "remotepeersshuser", type = CommandType.STRING,
            description = "the SSH user used by the source host to reach the remote peer host for DR")
    private String remotePeerSshUser;

    @Parameter(name = "remotepeersshport", type = CommandType.STRING,
            description = "the SSH port used by the source host to reach the remote peer host for DR")
    private String remotePeerSshPort;

    @Parameter(name = "remotepeersshoverride", type = CommandType.BOOLEAN,
            description = "whether remote peer SSH connection values were explicitly edited by the user")
    private Boolean remotePeerSshOverride;

    @Parameter(name = "remotepeersshautosetup", type = CommandType.BOOLEAN,
            description = "whether Cloud pre-prepared source-to-remote SSH access before DR registration")
    private Boolean remotePeerSshAutoSetup;

    @Parameter(name = "remotepeerlibvirturi", type = CommandType.STRING,
            description = "the resolved remote peer libvirt URI for DR")
    private String remotePeerLibvirtUri;

    @Parameter(name = "remotetargetstoragepooluuid", type = CommandType.STRING,
            description = "the remote Mold target storage pool UUID for DR")
    private String remoteTargetStoragePoolUuid;

    @Parameter(name = "remotetargetstoragepoolname", type = CommandType.STRING,
            description = "the remote Mold target storage pool name for DR")
    private String remoteTargetStoragePoolName;

    @Parameter(name = "remotetargetstoragepoolpath", type = CommandType.STRING,
            description = "the remote Mold target storage pool path for DR")
    private String remoteTargetStoragePoolPath;

    @Parameter(name = "remotetargetstoragepooltype", type = CommandType.STRING,
            description = "the remote Mold target storage pool type for DR")
    private String remoteTargetStoragePoolType;

    @Parameter(name = "provisioningbackend", type = CommandType.STRING,
            description = "the FTCTL standby VM and volume provisioning backend")
    private String provisioningBackend;

    @Parameter(name = "fencingpolicy", type = CommandType.STRING, description = "the FTCTL fencing policy")
    private String fencingPolicy;

    @Parameter(name = "peerhostid", type = CommandType.UUID, entityType = HostResponse.class,
            description = "the FTCTL peer host ID")
    private Long peerHostId;

    @Parameter(name = "secondaryvmname", type = CommandType.STRING, description = "the FTCTL secondary VM name")
    private String secondaryVmName;

    @Parameter(name = "networkids", type = CommandType.STRING,
            description = "comma-separated target network IDs or UUIDs used for Cloud-managed DR standby VM creation")
    private String networkIds;

    @Parameter(name = "secondarytargetdir", type = CommandType.STRING, description = "the FTCTL secondary target directory for remote-nbd")
    private String secondaryTargetDir;

    @Parameter(name = "remotenbdexportaddr", type = CommandType.STRING, description = "the FTCTL remote NBD export address")
    private String remoteNbdExportAddr;

    @Parameter(name = "xcoloproxyendpoint", type = CommandType.STRING, description = "the FTCTL x-colo proxy endpoint")
    private String xcoloProxyEndpoint;

    @Parameter(name = "xcolonbdendpoint", type = CommandType.STRING, description = "the FTCTL x-colo NBD endpoint")
    private String xcoloNbdEndpoint;

    @Parameter(name = "xcolomigrateuri", type = CommandType.STRING, description = "the FTCTL x-colo migrate URI")
    private String xcoloMigrateUri;

    @Parameter(name = "xcoloportallocationmode", type = CommandType.STRING,
            description = "the FTCTL x-colo port allocation mode: auto or manual")
    private String xcoloPortAllocationMode;

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public String getMode() {
        return mode;
    }

    public String getBackendMode() {
        return backendMode;
    }

    public String getTargetStorageScope() {
        return targetStorageScope;
    }

    public Long getTargetStoragePoolId() {
        return targetStoragePoolId;
    }

    public String getDrPeerSiteType() {
        return drPeerSiteType;
    }

    public String getRemoteMoldApiUrl() {
        return remoteMoldApiUrl;
    }

    public String getRemoteMoldApiKey() {
        return remoteMoldApiKey;
    }

    public String getRemoteMoldSecretKey() {
        return remoteMoldSecretKey;
    }

    public String getRemotePeerHostUuid() {
        return remotePeerHostUuid;
    }

    public String getRemotePeerHostName() {
        return remotePeerHostName;
    }

    public String getRemotePeerHostAddress() {
        return remotePeerHostAddress;
    }

    public String getRemotePeerHostBlockcopyAddress() {
        return remotePeerHostBlockcopyAddress;
    }

    public String getRemotePeerSshUser() {
        return remotePeerSshUser;
    }

    public String getRemotePeerSshPort() {
        return remotePeerSshPort;
    }

    public Boolean getRemotePeerSshOverride() {
        return remotePeerSshOverride;
    }

    public Boolean getRemotePeerSshAutoSetup() {
        return remotePeerSshAutoSetup;
    }

    public String getRemotePeerLibvirtUri() {
        return remotePeerLibvirtUri;
    }

    public String getRemoteTargetStoragePoolUuid() {
        return remoteTargetStoragePoolUuid;
    }

    public String getRemoteTargetStoragePoolName() {
        return remoteTargetStoragePoolName;
    }

    public String getRemoteTargetStoragePoolPath() {
        return remoteTargetStoragePoolPath;
    }

    public String getRemoteTargetStoragePoolType() {
        return remoteTargetStoragePoolType;
    }

    public String getProvisioningBackend() {
        return provisioningBackend;
    }

    public String getFencingPolicy() {
        return fencingPolicy;
    }

    public Long getPeerHostId() {
        return peerHostId;
    }

    public String getSecondaryVmName() {
        return secondaryVmName;
    }

    public String getNetworkIds() {
        return networkIds;
    }

    public String getSecondaryTargetDir() {
        return secondaryTargetDir;
    }

    public String getRemoteNbdExportAddr() {
        return remoteNbdExportAddr;
    }

    public String getXcoloProxyEndpoint() {
        return xcoloProxyEndpoint;
    }

    public String getXcoloNbdEndpoint() {
        return xcoloNbdEndpoint;
    }

    public String getXcoloMigrateUri() {
        return xcoloMigrateUri;
    }

    public String getXcoloPortAllocationMode() {
        return xcoloPortAllocationMode;
    }

    @Override
    public void execute() throws ServerApiException {
        FtctlProtectionResponse response = ftctlService.registerFtctlProtection(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _entityMgr.findById(UserVm.class, getVirtualMachineId());
        return vm != null ? vm.getAccountId() : Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_FTCTL_PROTECTION_REGISTER;
    }

    @Override
    public String getEventDescription() {
        UserVm vm = _entityMgr.findById(UserVm.class, getVirtualMachineId());
        String identifier = vm != null ? vm.getUuid() : String.valueOf(getVirtualMachineId());
        return String.format("Registering FTCTL protection for VM %s", identifier);
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }

    @Override
    public Long getApiResourceId() {
        return getVirtualMachineId();
    }
}
