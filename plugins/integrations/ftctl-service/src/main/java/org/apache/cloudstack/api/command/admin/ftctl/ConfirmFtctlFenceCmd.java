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

import com.cloud.agent.api.FtctlActionCommand;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ftctl.FtctlActionResponse;

@APICommand(name = ConfirmFtctlFenceCmd.APINAME,
        description = "Confirms FTCTL fence completion for a virtual machine",
        responseObject = FtctlActionResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        authorized = {RoleType.Admin})
public class ConfirmFtctlFenceCmd extends AbstractFtctlVmActionCmd {
    public static final String APINAME = "confirmFtctlFence";

    @Parameter(name = "remotemoldapiurl", type = CommandType.STRING,
            description = "the remote Mold API URL used only for DR remote site lifecycle calls")
    private String remoteMoldApiUrl;

    @Parameter(name = "remotemoldapikey", type = CommandType.STRING,
            description = "the remote Mold API key used only for this DR remote site fence confirmation")
    private String remoteMoldApiKey;

    @Parameter(name = "remotemoldsecretkey", type = CommandType.STRING,
            description = "the remote Mold secret key used only for this DR remote site fence confirmation")
    private String remoteMoldSecretKey;

    @Override
    protected FtctlActionCommand.Action getAction() {
        return FtctlActionCommand.Action.FENCE_CONFIRM;
    }

    @Override
    public void execute() throws ServerApiException {
        FtctlActionResponse response = ftctlService.confirmFtctlFence(getVirtualMachineId(), remoteMoldApiUrl,
                remoteMoldApiKey, remoteMoldSecretKey);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
