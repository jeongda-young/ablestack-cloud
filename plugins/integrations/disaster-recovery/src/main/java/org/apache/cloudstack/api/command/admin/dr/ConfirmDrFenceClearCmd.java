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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.dr.DrRunResponse;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.cluster.DisasterRecoveryClusterEventTypes;
import com.google.gson.JsonObject;

@APICommand(name = ConfirmDrFenceClearCmd.APINAME, description = "Confirm DR fence clear", responseObject = DrRunResponse.class,
        responseView = ResponseObject.ResponseView.Full, authorized = {RoleType.Admin})
public class ConfirmDrFenceClearCmd extends AbstractDrPlanActionCmd {
    public static final String APINAME = "confirmDrFenceClear";

    @Parameter(name = "remotemoldapiurl", type = CommandType.STRING,
            description = "the remote Mold API URL used only for DR remote site lifecycle calls")
    private String remoteMoldApiUrl;

    @Parameter(name = "remotemoldapikey", type = CommandType.STRING,
            description = "the remote Mold API key used only for this DR remote site fence confirmation")
    private String remoteMoldApiKey;

    @Parameter(name = "remotemoldsecretkey", type = CommandType.STRING,
            description = "the remote Mold secret key used only for this DR remote site fence confirmation")
    private String remoteMoldSecretKey;

    @Override
    protected String getRunType() {
        return "FENCE_CONFIRM";
    }

    @Override
    protected void validateActionAllowed() {
        DrPlanVO plan = drPlanService.getPlan(getPlanId());
        if (plan != null && (DrConstants.ENGINE_TYPE_FTCTL_DR.equalsIgnoreCase(plan.getEngineType())
                || DrConstants.ENGINE_BINDING_TYPE_FTCTL_DR.equalsIgnoreCase(plan.getEngineBindingType()))) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    DrConstants.ERROR_FENCE_CLEAR_INTERNAL_ONLY
                            + ": source isolation is validated inside failback or reprotect");
        }
        super.validateActionAllowed();
    }

    @Override
    protected void addRequestProperties(JsonObject request) {
        addProperty(request, "remoteMoldApiUrl", remoteMoldApiUrl);
        addProperty(request, "remoteMoldApiKey", remoteMoldApiKey);
        addProperty(request, "remoteMoldSecretKey", remoteMoldSecretKey);
    }

    @Override
    protected String getApiName() {
        return APINAME;
    }

    @Override
    public String getEventType() {
        return DisasterRecoveryClusterEventTypes.EVENT_DR_FENCE_CONFIRM;
    }

    @Override
    public String getEventDescription() {
        return "Confirming DR fence clear for plan " + getPlanId();
    }
}
