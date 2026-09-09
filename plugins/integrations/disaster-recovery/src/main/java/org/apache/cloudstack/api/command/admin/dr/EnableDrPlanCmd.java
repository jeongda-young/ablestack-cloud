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
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.dr.DrPlanResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.dr.DrPlanService;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.cluster.DisasterRecoveryClusterEventTypes;
import com.cloud.dr.response.DrResponseGenerator;

@APICommand(name = EnableDrPlanCmd.APINAME, description = "Enable a DR plan", responseObject = DrPlanResponse.class, authorized = {RoleType.Admin})
public class EnableDrPlanCmd extends BaseAsyncCmd {
    public static final String APINAME = "enableDrPlan";

    @Inject
    private DrPlanService drPlanService;
    @Inject
    private DrResponseGenerator drResponseGenerator;

    @Parameter(name = "id", type = CommandType.UUID, entityType = DrPlanResponse.class, required = true, description = "the DR plan ID")
    private Long id;

    @Override
    public void execute() throws ServerApiException {
        try {
            DrPlanVO plan = drPlanService.enablePlan(id);
            DrPlanResponse response = drResponseGenerator.createPlanResponse(plan, drPlanService.getActionEvaluation(plan.getId()));
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
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public String getEventType() {
        return DisasterRecoveryClusterEventTypes.EVENT_DR_PLAN_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Enabling DR plan " + id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.DisasterRecoveryCluster;
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }
}
