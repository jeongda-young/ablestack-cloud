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
package org.apache.cloudstack.api.command.admin.dr;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.dr.DrSiteAgentCommandResponse;

import com.cloud.dr.DrSiteAgentBrokerService;
import com.cloud.user.Account;

@APICommand(name = ExecuteDrSiteAgentCommandCmd.APINAME,
        description = "Executes an allow-listed FTCTL DR command on a site-local Agent worker",
        responseObject = DrSiteAgentCommandResponse.class, authorized = {RoleType.Admin})
public class ExecuteDrSiteAgentCommandCmd extends BaseCmd {
    public static final String APINAME = "executeDrSiteAgentCommand";

    @Inject private DrSiteAgentBrokerService brokerService;

    @Parameter(name = "commandtype", type = CommandType.STRING, required = true,
            description = "ACTION, STATUS, CAPABILITIES, CANCEL, or REVERSE_PREFLIGHT")
    private String commandType;
    @Parameter(name = "commandjson", type = CommandType.STRING, required = true, length = 65535,
            description = "the serialized allow-listed FTCTL Agent command")
    private String commandJson;
    @Parameter(name = "workerhostuuid", type = CommandType.STRING,
            description = "deprecated worker hint; the site broker selects an eligible Agent automatically")
    private String workerHostUuid;

    @Override
    public void execute() throws ServerApiException {
        try {
            DrSiteAgentCommandResponse response = brokerService.execute(commandType, commandJson, workerHostUuid);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (RuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
