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
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlActionResponse;

import javax.inject.Inject;

@APICommand(name = InstallFtctlDrRemoteSshKeyCmd.APINAME,
        description = "Installs an FTCTL DR SSH public key on a local KVM host",
        responseObject = FtctlActionResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        authorized = {RoleType.Admin})
public class InstallFtctlDrRemoteSshKeyCmd extends BaseCmd {
    public static final String APINAME = "installFtctlDrRemoteSshKey";

    @Inject
    private FtctlService ftctlService;

    @Parameter(name = "hostid", type = CommandType.UUID, entityType = HostResponse.class,
            required = true, description = "the host ID where the SSH key must be installed")
    private Long hostId;

    @Parameter(name = "profile", type = CommandType.STRING, required = true,
            description = "the FTCTL DR key profile")
    private String profile;

    @Parameter(name = "publickey", type = CommandType.STRING, required = true, length = 4096,
            description = "the SSH public key to install")
    private String publicKey;

    @Parameter(name = "keycomment", type = CommandType.STRING,
            description = "the SSH authorized_keys comment used for later cleanup")
    private String keyComment;

    @Parameter(name = "sshuser", type = CommandType.STRING,
            description = "the remote host SSH user")
    private String sshUser;

    @Parameter(name = "applyfirewall", type = CommandType.BOOLEAN,
            description = "whether to apply the FTCTL firewalld service on the host")
    private Boolean applyFirewall;

    public Long getHostId() {
        return hostId;
    }

    public String getProfile() {
        return profile;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getKeyComment() {
        return keyComment;
    }

    public String getSshUser() {
        return sshUser;
    }

    public Boolean getApplyFirewall() {
        return applyFirewall;
    }

    @Override
    public void execute() throws ServerApiException {
        FtctlActionResponse response = ftctlService.installFtctlDrRemoteSshKey(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Host;
    }

    @Override
    public Long getApiResourceId() {
        return getHostId();
    }
}
