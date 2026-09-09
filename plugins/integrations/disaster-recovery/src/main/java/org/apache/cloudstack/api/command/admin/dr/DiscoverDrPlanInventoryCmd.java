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
import org.apache.cloudstack.api.response.dr.DrPlanInventoryResponse;
import org.apache.cloudstack.api.response.dr.DrSiteResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.dr.cluster.DisasterRecoveryClusterEventTypes;
import com.cloud.dr.inventory.DrPlanInventoryRequest;
import com.cloud.dr.inventory.DrPlanInventoryResult;
import com.cloud.dr.inventory.DrPlanInventoryService;
import com.cloud.dr.response.DrResponseGenerator;

@APICommand(name = DiscoverDrPlanInventoryCmd.APINAME,
        description = "Discover source workload options for a Cross Hypervisor DR plan",
        responseObject = DrPlanInventoryResponse.class,
        authorized = {RoleType.Admin})
public class DiscoverDrPlanInventoryCmd extends BaseAsyncCmd {
    public static final String APINAME = "discoverDrPlanInventory";

    @Inject
    private DrPlanInventoryService drPlanInventoryService;
    @Inject
    private DrResponseGenerator drResponseGenerator;

    @Parameter(name = "sourcesiteid", type = CommandType.UUID, entityType = DrSiteResponse.class, required = true,
            description = "the source DR site ID")
    private Long sourceSiteId;

    @Parameter(name = "targetsiteid", type = CommandType.UUID, entityType = DrSiteResponse.class, required = true,
            description = "the target DR site ID")
    private Long targetSiteId;

    @Parameter(name = "sourcevmid", type = CommandType.UUID, entityType = UserVmResponse.class,
            description = "the selected local source VM ID")
    private Long sourceVmId;

    @Parameter(name = "sourceexternalref", type = CommandType.STRING,
            description = "the selected remote source workload reference")
    private String sourceExternalRef;

    @Parameter(name = "keyword", type = CommandType.STRING, description = "the optional source workload search keyword")
    private String keyword;

    @Parameter(name = "includeplacement", type = CommandType.BOOLEAN,
            description = "whether to include target placement inventory")
    private Boolean includePlacement;

    @Parameter(name = "includedisks", type = CommandType.BOOLEAN,
            description = "whether to include selected source workload disk inventory")
    private Boolean includeDisks;

    @Parameter(name = "includenetworks", type = CommandType.BOOLEAN,
            description = "whether to include source NIC and target network inventory")
    private Boolean includeNetworks;

    @Override
    public void execute() throws ServerApiException {
        try {
            DrPlanInventoryResult result = drPlanInventoryService.discover(buildRequest());
            DrPlanInventoryResponse response = drResponseGenerator.createPlanInventoryResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (RuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private DrPlanInventoryRequest buildRequest() {
        DrPlanInventoryRequest request = new DrPlanInventoryRequest();
        request.setSourceSiteId(sourceSiteId);
        request.setTargetSiteId(targetSiteId);
        request.setSourceVmId(sourceVmId);
        request.setSourceExternalRef(sourceExternalRef);
        request.setKeyword(keyword);
        request.setIncludePlacement(includePlacement);
        request.setIncludeDisks(includeDisks);
        request.setIncludeNetworks(includeNetworks);
        return request;
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
        return DisasterRecoveryClusterEventTypes.EVENT_DR_PLAN_INVENTORY;
    }

    @Override
    public String getEventDescription() {
        return "Discovering DR plan source workload inventory";
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.DisasterRecoveryCluster;
    }
}
