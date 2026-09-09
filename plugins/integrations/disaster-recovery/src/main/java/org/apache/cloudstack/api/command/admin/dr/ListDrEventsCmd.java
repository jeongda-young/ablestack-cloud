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
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.dr.DrEventResponse;
import org.apache.cloudstack.api.response.dr.DrPlanResponse;
import org.apache.cloudstack.api.response.dr.DrRunResponse;

import com.cloud.dr.DrEventVO;
import com.cloud.dr.DrProjectionService;
import com.cloud.dr.response.DrResponseGenerator;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = ListDrEventsCmd.APINAME, description = "List DR events", responseObject = DrEventResponse.class, authorized = {RoleType.Admin})
public class ListDrEventsCmd extends BaseListCmd {
    public static final String APINAME = "listDrEvents";

    @Inject
    private DrProjectionService drProjectionService;
    @Inject
    private DrResponseGenerator drResponseGenerator;

    @Parameter(name = "planid", type = CommandType.UUID, entityType = DrPlanResponse.class, description = "the DR plan ID")
    private Long planId;

    @Parameter(name = "runid", type = CommandType.UUID, entityType = DrRunResponse.class, description = "the DR run ID")
    private Long runId;

    @Override
    public void execute() {
        if (planId == null && runId == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "planid or runid is required");
        }
        long startIndex = getStartIndex() != null ? getStartIndex() : 0L;
        long pageSize = getPageSizeVal() != null ? Math.min(getPageSizeVal(), 100L) : 20L;
        Pair<List<DrEventVO>, Integer> result = runId != null
                ? drProjectionService.listRunEvents(runId, startIndex, pageSize)
                : drProjectionService.listPlanEvents(planId, startIndex, pageSize);
        List<DrEventResponse> responses = new ArrayList<DrEventResponse>();
        for (DrEventVO event : result.first()) {
            responses.add(drResponseGenerator.createEventResponse(event));
        }
        ListResponse<DrEventResponse> response = new ListResponse<DrEventResponse>();
        response.setResponses(responses, result.second());
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
