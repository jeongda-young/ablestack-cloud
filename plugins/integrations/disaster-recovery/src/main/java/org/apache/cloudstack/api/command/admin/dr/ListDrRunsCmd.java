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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.dr.DrPlanResponse;
import org.apache.cloudstack.api.response.dr.DrRunResponse;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.DrRunService;
import com.cloud.dr.DrRunVO;
import com.cloud.dr.response.DrResponseGenerator;
import com.cloud.user.Account;

@APICommand(name = ListDrRunsCmd.APINAME, description = "List DR runs", responseObject = DrRunResponse.class,
        responseView = ResponseObject.ResponseView.Full, authorized = {RoleType.Admin})
public class ListDrRunsCmd extends BaseListCmd {
    public static final String APINAME = "listDrRuns";

    @Inject
    private DrRunService drRunService;
    @Inject
    private DrResponseGenerator drResponseGenerator;

    @Parameter(name = "planid", type = CommandType.UUID, entityType = DrPlanResponse.class, required = true, description = "the DR plan ID")
    private Long planId;

    @Parameter(name = "idempotencykey", type = CommandType.STRING,
            description = "optional idempotency key used to recover an accepted DR run")
    private String idempotencyKey;

    @Override
    public void execute() {
        List<DrRunResponse> responses = new ArrayList<DrRunResponse>();
        List<DrRunVO> runs = new ArrayList<DrRunVO>();
        if (StringUtils.isNotBlank(idempotencyKey)) {
            DrRunVO run = drRunService.findRunByIdempotencyKey(planId, idempotencyKey);
            if (run != null) {
                runs.add(run);
            }
        } else {
            runs.addAll(drRunService.listRuns(planId));
        }
        for (DrRunVO run : runs) {
            responses.add(drResponseGenerator.createRunResponse(run, drRunService.listRunSteps(run.getId()), false));
        }
        ListResponse<DrRunResponse> response = new ListResponse<DrRunResponse>();
        response.setResponses(responses);
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
