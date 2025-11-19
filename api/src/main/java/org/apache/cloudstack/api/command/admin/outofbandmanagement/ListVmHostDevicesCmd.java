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
package org.apache.cloudstack.api.command.admin.outofbandmanagement;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ListVmHostDevicesResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "listVmHostDevices", description = "List host devices allocated to a specific VM",
        since = "4.20.0.0", responseObject = ListVmHostDevicesResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin})
public class ListVmHostDevicesCmd extends BaseListCmd {

    private static final String LISTVMHOSTDEVICES = "listvmhostdevices";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID,
            entityType = UserVmResponse.class, description = "VM ID to query", required = true)
    private Long virtualMachineId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public static String getResultObjectName() {
        return LISTVMHOSTDEVICES;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public void execute() {
        ListResponse<ListVmHostDevicesResponse> response = _mgr.listVmHostDevices(this);
        response.setResponseName(getCommandName());
        response.setObjectName(getCommandName());
        this.setResponseObject(response);
    }
}

