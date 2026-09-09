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
import org.apache.cloudstack.api.response.dr.DrRunResponse;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrTargetMaterializationService;
import com.cloud.dr.cluster.DisasterRecoveryClusterEventTypes;
import com.google.gson.JsonObject;

import javax.inject.Inject;

@APICommand(name = ReleaseDrProtectionCmd.APINAME, description = "Release DR protection", responseObject = DrRunResponse.class,
        responseView = ResponseObject.ResponseView.Full, authorized = {RoleType.Admin})
public class ReleaseDrProtectionCmd extends AbstractDrPlanActionCmd {
    public static final String APINAME = "releaseDrProtection";

    @Override
    protected String getRunType() {
        return DrConstants.RUN_TYPE_RELEASE;
    }

    @Override
    protected String getApiName() {
        return APINAME;
    }

    @Override
    public String getEventType() {
        return DisasterRecoveryClusterEventTypes.EVENT_DR_PLAN_RELEASE;
    }

    @Override
    public String getEventDescription() {
        return "Releasing DR protection for plan " + getPlanId();
    }

    @Inject
    private DrTargetMaterializationService drTargetMaterializationService;

    @Parameter(name = "resourcedisposition", type = CommandType.STRING,
            description = "resource disposition: RETAIN_OPERATIONAL_VM (default) or DELETE_STANDBY_REPLICA")
    private String resourceDisposition;

    public String getResourceDisposition() {
        return resourceDisposition != null ? resourceDisposition
                : DrConstants.RELEASE_DISPOSITION_RETAIN_OPERATIONAL_VM;
    }

    @Override
    protected void validateActionAllowed() {
        super.validateActionAllowed();
        drTargetMaterializationService.validateReleaseDisposition(getPlanId(), getResourceDisposition());
    }

    @Override
    protected void addRequestProperties(JsonObject request) {
        addProperty(request, "resourceDisposition", getResourceDisposition());
    }
}
