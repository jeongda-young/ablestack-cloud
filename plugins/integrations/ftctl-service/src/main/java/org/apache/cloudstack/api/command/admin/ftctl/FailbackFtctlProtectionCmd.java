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

@APICommand(name = FailbackFtctlProtectionCmd.APINAME,
        description = "Triggers FTCTL failback for a virtual machine",
        responseObject = FtctlActionResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        authorized = {RoleType.Admin})
public class FailbackFtctlProtectionCmd extends AbstractFtctlVmActionCmd {
    public static final String APINAME = "failbackFtctlProtection";

    @Parameter(name = "failbacktargetmoldtype", type = CommandType.STRING,
            description = "DR failback target Mold type: current, original-primary, or new")
    private String failbackTargetMoldType;

    @Parameter(name = "remotemoldapiurl", type = CommandType.STRING,
            description = "one-time remote Mold API URL used for DR remote Mold failback continuation")
    private String remoteMoldApiUrl;

    @Parameter(name = "remotemoldapikey", type = CommandType.STRING,
            description = "one-time remote Mold API key used for DR remote Mold failback continuation")
    private String remoteMoldApiKey;

    @Parameter(name = "remotemoldsecretkey", type = CommandType.STRING,
            description = "one-time remote Mold secret key used for DR remote Mold failback continuation")
    private String remoteMoldSecretKey;

    @Parameter(name = "targetmoldapiurl", type = CommandType.STRING,
            description = "one-time target Mold API URL for DR failback target selection")
    private String targetMoldApiUrl;

    @Parameter(name = "targetmoldapikey", type = CommandType.STRING,
            description = "one-time target Mold API key for DR failback target selection")
    private String targetMoldApiKey;

    @Parameter(name = "targetmoldsecretkey", type = CommandType.STRING,
            description = "one-time target Mold secret key for DR failback target selection")
    private String targetMoldSecretKey;

    @Override
    protected FtctlActionCommand.Action getAction() {
        return FtctlActionCommand.Action.FAILBACK;
    }

    @Override
    protected boolean useForce() {
        return true;
    }

    @Override
    public void execute() throws ServerApiException {
        FtctlActionResponse response = ftctlService.failbackFtctlProtection(getVirtualMachineId(), useForce(),
                failbackTargetMoldType, remoteMoldApiUrl, remoteMoldApiKey, remoteMoldSecretKey,
                targetMoldApiUrl, targetMoldApiKey, targetMoldSecretKey);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
