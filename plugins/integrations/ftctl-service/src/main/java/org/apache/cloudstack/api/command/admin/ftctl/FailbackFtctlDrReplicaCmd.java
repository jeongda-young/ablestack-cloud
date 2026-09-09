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

@APICommand(name = FailbackFtctlDrReplicaCmd.APINAME,
        description = "Delegates FTCTL DR failback from a running remote replica VM to the source Mold controller",
        responseObject = FtctlActionResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        authorized = {RoleType.Admin})
public class FailbackFtctlDrReplicaCmd extends AbstractFtctlVmActionCmd {
    public static final String APINAME = "failbackFtctlDrReplica";

    @Parameter(name = "targetmoldapiurl", type = CommandType.STRING, required = true,
            description = "one-time source or failback target Mold API URL")
    private String targetMoldApiUrl;

    @Parameter(name = "targetmoldapikey", type = CommandType.STRING, required = true,
            description = "one-time source or failback target Mold API key")
    private String targetMoldApiKey;

    @Parameter(name = "targetmoldsecretkey", type = CommandType.STRING, required = true,
            description = "one-time source or failback target Mold secret key")
    private String targetMoldSecretKey;

    @Parameter(name = "replicamoldapiurl", type = CommandType.STRING, required = true,
            description = "one-time current replica Mold API URL passed to the source controller for replica cutback")
    private String replicaMoldApiUrl;

    @Parameter(name = "replicamoldapikey", type = CommandType.STRING, required = true,
            description = "one-time current replica Mold API key passed to the source controller for replica cutback")
    private String replicaMoldApiKey;

    @Parameter(name = "replicamoldsecretkey", type = CommandType.STRING, required = true,
            description = "one-time current replica Mold secret key passed to the source controller for replica cutback")
    private String replicaMoldSecretKey;

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
        FtctlActionResponse response = ftctlService.failbackFtctlDrReplica(getVirtualMachineId(),
                targetMoldApiUrl, targetMoldApiKey, targetMoldSecretKey,
                replicaMoldApiUrl, replicaMoldApiKey, replicaMoldSecretKey);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
