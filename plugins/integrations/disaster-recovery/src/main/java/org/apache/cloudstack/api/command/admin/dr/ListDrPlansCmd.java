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
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.dr.DrPlanResponse;
import org.apache.cloudstack.api.response.dr.DrSiteResponse;

import com.cloud.dr.DrPlanService;
import com.cloud.dr.DrPlanSearchCriteria;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.response.DrResponseGenerator;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = ListDrPlansCmd.APINAME,
        description = "List Cross Hypervisor DR plans",
        responseObject = DrPlanResponse.class,
        authorized = {RoleType.Admin})
public class ListDrPlansCmd extends BaseListCmd {
    public static final String APINAME = "listDrPlans";

    @Inject
    private DrPlanService drPlanService;
    @Inject
    private DrResponseGenerator drResponseGenerator;

    @Parameter(name = "keyword", type = CommandType.STRING, description = "the keyword filter")
    private String keyword;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DrPlanResponse.class,
            description = "the exact DR plan ID")
    private Long id;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "the DR plan state")
    private String state;

    @Parameter(name = "sourcesiteid", type = CommandType.UUID, entityType = DrSiteResponse.class,
            description = "the source DR site ID")
    private Long sourceSiteId;

    @Parameter(name = "targetsiteid", type = CommandType.UUID, entityType = DrSiteResponse.class,
            description = "the target DR site ID")
    private Long targetSiteId;

    @Parameter(name = "direction", type = CommandType.STRING, description = "the DR direction")
    private String direction;

    @Parameter(name = "enginetype", type = CommandType.STRING, description = "the DR engine type")
    private String engineType;

    public String getKeyword() {
        return keyword;
    }

    @Override
    public void execute() {
        Pair<List<DrPlanVO>, Integer> plans = drPlanService.searchPlans(new DrPlanSearchCriteria(id, keyword, state,
                sourceSiteId, targetSiteId, direction, engineType, getStartIndex(), getPageSizeVal()));
        List<DrPlanResponse> responses = new ArrayList<DrPlanResponse>();
        for (DrPlanVO plan : plans.first()) {
            responses.add(drResponseGenerator.createPlanResponse(plan,
                    drPlanService.getDatabaseActionEvaluation(plan.getId())));
        }
        ListResponse<DrPlanResponse> response = new ListResponse<DrPlanResponse>();
        response.setResponses(responses, plans.second());
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
