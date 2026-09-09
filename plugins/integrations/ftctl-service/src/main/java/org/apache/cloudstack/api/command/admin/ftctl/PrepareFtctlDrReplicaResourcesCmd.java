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
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ftctl.FtctlDrReplicaResourcesResponse;

import javax.inject.Inject;

@APICommand(name = PrepareFtctlDrReplicaResourcesCmd.APINAME,
        description = "Creates Cloud-managed remote replica VM and volumes for FTCTL DR",
        responseObject = FtctlDrReplicaResourcesResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        authorized = {RoleType.Admin})
public class PrepareFtctlDrReplicaResourcesCmd extends BaseCmd {
    public static final String APINAME = "prepareFtctlDrReplicaResources";

    @Inject
    private FtctlService ftctlService;

    @Parameter(name = "sourcevirtualmachineid", type = CommandType.STRING, required = true,
            description = "the source-site VM UUID")
    private String sourceVirtualMachineId;

    @Parameter(name = "sourcevirtualmachinename", type = CommandType.STRING, required = true,
            description = "the source-site VM display name")
    private String sourceVirtualMachineName;

    @Parameter(name = "sourcevirtualmachineinstancename", type = CommandType.STRING,
            description = "the source-site VM hypervisor instance name")
    private String sourceVirtualMachineInstanceName;

    @Parameter(name = "sourcemoldapiurl", type = CommandType.STRING,
            description = "non-secret source Mold API URL display hint for future DR recovery")
    private String sourceMoldApiUrl;

    @Parameter(name = "secondaryvmname", type = CommandType.STRING, required = true,
            description = "the remote replica VM name")
    private String secondaryVmName;

    @Parameter(name = "remotepeerhostuuid", type = CommandType.STRING, required = true,
            description = "the remote Mold target KVM host UUID")
    private String remotePeerHostUuid;

    @Parameter(name = "remotetargetstoragepooluuid", type = CommandType.STRING, required = true,
            description = "the remote Mold target primary storage pool UUID")
    private String remoteTargetStoragePoolUuid;

    @Parameter(name = "sourcehypervisor", type = CommandType.STRING,
            description = "the source VM hypervisor type")
    private String sourceHypervisor;

    @Parameter(name = "sourcevmdetails", type = CommandType.STRING, length = 65535,
            description = "JSON object containing source VM details to copy")
    private String sourceVmDetails;

    @Parameter(name = "sourcevolumes", type = CommandType.STRING, required = true, length = 65535,
            description = "JSON array containing source volume specifications")
    private String sourceVolumes;

    @Parameter(name = "networkids", type = CommandType.STRING, required = true,
            description = "comma-separated remote network IDs or UUIDs used for replica VM creation")
    private String networkIds;

    public String getSourceVirtualMachineId() {
        return sourceVirtualMachineId;
    }

    public String getSourceVirtualMachineName() {
        return sourceVirtualMachineName;
    }

    public String getSourceVirtualMachineInstanceName() {
        return sourceVirtualMachineInstanceName;
    }

    public String getSourceMoldApiUrl() {
        return sourceMoldApiUrl;
    }

    public String getSecondaryVmName() {
        return secondaryVmName;
    }

    public String getRemotePeerHostUuid() {
        return remotePeerHostUuid;
    }

    public String getRemoteTargetStoragePoolUuid() {
        return remoteTargetStoragePoolUuid;
    }

    public String getSourceHypervisor() {
        return sourceHypervisor;
    }

    public String getSourceVmDetails() {
        return sourceVmDetails;
    }

    public String getSourceVolumes() {
        return sourceVolumes;
    }

    public String getNetworkIds() {
        return networkIds;
    }

    @Override
    public void execute() throws ServerApiException {
        FtctlDrReplicaResourcesResponse response = ftctlService.prepareFtctlDrReplicaResources(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
