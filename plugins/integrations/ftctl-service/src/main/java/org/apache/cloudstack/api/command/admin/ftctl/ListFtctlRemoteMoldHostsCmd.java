// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.admin.ftctl;

import com.cloud.ftctl.FtctlService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ftctl.FtctlRemoteMoldHostsResponse;

import javax.inject.Inject;

@APICommand(name = ListFtctlRemoteMoldHostsCmd.APINAME,
        description = "Lists remote Mold KVM routing hosts for FTCTL DR",
        responseObject = FtctlRemoteMoldHostsResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        authorized = {RoleType.Admin})
public class ListFtctlRemoteMoldHostsCmd extends AbstractFtctlRemoteMoldCmd {
    public static final String APINAME = "listFtctlRemoteMoldHosts";

    @Inject
    private FtctlService ftctlService;

    @Parameter(name = "zoneid", type = CommandType.STRING, description = "optional remote Mold zone ID filter")
    private String zoneId;

    @Parameter(name = "clusterid", type = CommandType.STRING, description = "optional remote Mold cluster ID filter")
    private String clusterId;

    public String getZoneId() {
        return zoneId;
    }

    public String getClusterId() {
        return clusterId;
    }

    @Override
    public void execute() throws ServerApiException {
        FtctlRemoteMoldHostsResponse response = ftctlService.listFtctlRemoteMoldHosts(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
