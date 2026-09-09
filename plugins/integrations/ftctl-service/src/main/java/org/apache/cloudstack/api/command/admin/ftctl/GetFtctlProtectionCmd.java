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
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlProtectionResponse;

import javax.inject.Inject;

@APICommand(name = GetFtctlProtectionCmd.APINAME,
        description = "Gets FTCTL protection settings for a virtual machine",
        responseObject = FtctlProtectionResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        authorized = {RoleType.Admin})
public class GetFtctlProtectionCmd extends BaseCmd {
    public static final String APINAME = "getFtctlProtection";

    @Inject
    private FtctlService ftctlService;

    @Parameter(name = "virtualmachineid", type = CommandType.UUID, entityType = UserVmResponse.class,
            required = true, description = "the virtual machine ID")
    private Long virtualMachineId;

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    @Override
    public void execute() throws ServerApiException {
        FtctlProtectionResponse response = ftctlService.getFtctlProtection(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
