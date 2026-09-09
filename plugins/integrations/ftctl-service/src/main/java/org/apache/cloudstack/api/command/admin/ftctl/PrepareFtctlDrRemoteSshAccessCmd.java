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
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlActionResponse;

import javax.inject.Inject;

@APICommand(name = PrepareFtctlDrRemoteSshAccessCmd.APINAME,
        description = "Prepares source and remote host SSH access for FTCTL DR remote Mold",
        responseObject = FtctlActionResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        authorized = {RoleType.Admin})
public class PrepareFtctlDrRemoteSshAccessCmd extends AbstractFtctlRemoteMoldCmd {
    public static final String APINAME = "prepareFtctlDrRemoteSshAccess";

    @Inject
    private FtctlService ftctlService;

    @Parameter(name = "virtualmachineid", type = CommandType.UUID, entityType = UserVmResponse.class,
            required = true, description = "the virtual machine ID")
    private Long virtualMachineId;

    @Parameter(name = "remotepeerhostuuid", type = CommandType.STRING, required = true,
            description = "the remote Mold peer host UUID for DR")
    private String remotePeerHostUuid;

    @Parameter(name = "remotepeerhostaddress", type = CommandType.STRING, required = true,
            description = "the remote Mold peer host management address for DR")
    private String remotePeerHostAddress;

    @Parameter(name = "remotepeersshuser", type = CommandType.STRING,
            description = "the SSH user used by qemu+ssh")
    private String remotePeerSshUser;

    @Parameter(name = "remotepeersshport", type = CommandType.STRING,
            description = "the SSH port used by qemu+ssh")
    private String remotePeerSshPort;

    @Parameter(name = "remotepeerlibvirturi", type = CommandType.STRING,
            description = "the resolved remote peer libvirt URI")
    private String remotePeerLibvirtUri;

    @Parameter(name = "secondarytargetdir", type = CommandType.STRING,
            description = "the FTCTL secondary target directory for remote-nbd")
    private String secondaryTargetDir;

    @Parameter(name = "remotenbdexportaddr", type = CommandType.STRING,
            description = "the FTCTL remote NBD export address")
    private String remoteNbdExportAddr;

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public String getRemotePeerHostUuid() {
        return remotePeerHostUuid;
    }

    public String getRemotePeerHostAddress() {
        return remotePeerHostAddress;
    }

    public String getRemotePeerSshUser() {
        return remotePeerSshUser;
    }

    public String getRemotePeerSshPort() {
        return remotePeerSshPort;
    }

    public String getRemotePeerLibvirtUri() {
        return remotePeerLibvirtUri;
    }

    public String getSecondaryTargetDir() {
        return secondaryTargetDir;
    }

    public String getRemoteNbdExportAddr() {
        return remoteNbdExportAddr;
    }

    @Override
    public void execute() throws ServerApiException {
        FtctlActionResponse response = ftctlService.prepareFtctlDrRemoteSshAccess(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
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
