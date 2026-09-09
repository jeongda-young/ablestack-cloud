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
import org.apache.cloudstack.api.response.dr.DrSiteResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.dr.DrSiteService;
import com.cloud.dr.cluster.DisasterRecoveryClusterEventTypes;
import com.cloud.dr.response.DrResponseGenerator;

@APICommand(name = CheckDrSiteCmd.APINAME,
        description = "Check a Cross Hypervisor DR site",
        responseObject = DrSiteResponse.class,
        authorized = {RoleType.Admin})
public class CheckDrSiteCmd extends BaseAsyncCmd {
    public static final String APINAME = "checkDrSite";

    @Inject
    private DrSiteService drSiteService;
    @Inject
    private DrResponseGenerator drResponseGenerator;

    @Parameter(name = "id", type = CommandType.UUID, entityType = DrSiteResponse.class, required = true, description = "the DR site ID")
    private Long id;

    @Parameter(name = "persiststatus", type = CommandType.BOOLEAN, description = "whether to persist endpoint status")
    private Boolean persistStatus;

    public Boolean getPersistStatus() {
        return persistStatus;
    }

    @Override
    public void execute() throws ServerApiException {
        try {
            boolean persist = persistStatus == null || Boolean.TRUE.equals(persistStatus);
            DrSiteResponse response = drResponseGenerator.createSiteResponse(drSiteService.checkSite(id, persist));
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
        return DisasterRecoveryClusterEventTypes.EVENT_DR_SITE_CHECK;
    }

    @Override
    public String getEventDescription() {
        return "Checking DR site " + id;
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
