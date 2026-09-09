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
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.dr.DrRunResponse;
import org.apache.cloudstack.api.response.dr.DrRunStepResponse;

import com.cloud.dr.DrRunService;
import com.cloud.dr.response.DrResponseGenerator;
import com.cloud.user.Account;

@APICommand(name = ListDrRunStepsCmd.APINAME, description = "List DR run steps", responseObject = DrRunStepResponse.class, authorized = {RoleType.Admin})
public class ListDrRunStepsCmd extends BaseListCmd {
    public static final String APINAME = "listDrRunSteps";

    @Inject
    private DrRunService drRunService;
    @Inject
    private DrResponseGenerator drResponseGenerator;

    @Parameter(name = "runid", type = CommandType.UUID, entityType = DrRunResponse.class, required = true, description = "the DR run ID")
    private Long runId;

    @Override
    public void execute() {
        drRunService.getRun(runId);
        ListResponse<DrRunStepResponse> response = new ListResponse<DrRunStepResponse>();
        response.setResponses(drResponseGenerator.createRunStepResponses(drRunService.listRunSteps(runId)));
        response.setResponseName(getCommandName());
        setResponseObject(response);
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
