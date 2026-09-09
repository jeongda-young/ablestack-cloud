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
import org.apache.cloudstack.api.response.dr.DrSiteInventoryResponse;
import org.apache.cloudstack.api.response.dr.DrSiteResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrSiteCredentialInput;
import com.cloud.dr.cluster.DisasterRecoveryClusterEventTypes;
import com.cloud.dr.inventory.DrSiteInventoryRequest;
import com.cloud.dr.inventory.DrSiteInventoryResult;
import com.cloud.dr.inventory.DrSiteInventoryService;
import com.cloud.dr.response.DrResponseGenerator;

@APICommand(name = DiscoverDrSiteInventoryCmd.APINAME,
        description = "Discover inventory options for a Cross Hypervisor DR site",
        responseObject = DrSiteInventoryResponse.class,
        authorized = {RoleType.Admin})
public class DiscoverDrSiteInventoryCmd extends BaseAsyncCmd {
    public static final String APINAME = "discoverDrSiteInventory";

    @Inject
    private DrSiteInventoryService drSiteInventoryService;
    @Inject
    private DrResponseGenerator drResponseGenerator;

    @Parameter(name = "id", type = CommandType.UUID, entityType = DrSiteResponse.class, description = "the existing DR site ID")
    private Long id;

    @Parameter(name = "sitetype", type = CommandType.STRING, description = "the DR site type")
    private String siteType;

    @Parameter(name = "moldapiurl", type = CommandType.STRING, description = "the Mold API URL for transient discovery")
    private String moldApiUrl;

    @Parameter(name = "moldapikey", type = CommandType.STRING, description = "the Mold API key for transient discovery")
    private String moldApiKey;

    @Parameter(name = "moldsecretkey", type = CommandType.STRING, description = "the Mold secret key for transient discovery")
    private String moldSecretKey;

    @Parameter(name = "tlsverify", type = CommandType.BOOLEAN, description = "true to verify TLS certificates")
    private Boolean tlsVerify;

    @Parameter(name = "zoneid", type = CommandType.LONG, description = "the selected zone ID for VMware datacenter discovery")
    private Long zoneId;

    @Parameter(name = "zoneexternalid", type = CommandType.STRING, description = "the selected remote zone external ID for VMware datacenter discovery")
    private String zoneExternalId;

    @Parameter(name = "includezones", type = CommandType.BOOLEAN, description = "true to include Zone options")
    private Boolean includeZones;

    @Parameter(name = "includevmwaredcs", type = CommandType.BOOLEAN, description = "true to include VMware datacenter options")
    private Boolean includeVmwareDatacenters;

    @Override
    public void execute() throws ServerApiException {
        try {
            DrSiteInventoryResult result = drSiteInventoryService.discover(buildRequest());
            DrSiteInventoryResponse response = drResponseGenerator.createSiteInventoryResponse(result);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (RuntimeException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private DrSiteInventoryRequest buildRequest() {
        DrSiteInventoryRequest request = new DrSiteInventoryRequest();
        request.setSiteId(id);
        request.setSiteType(siteType);
        request.setZoneId(zoneId);
        request.setZoneExternalId(zoneExternalId);
        request.setIncludeZones(includeZones == null || includeZones);
        request.setIncludeVmwareDatacenters(includeVmwareDatacenters != null ? includeVmwareDatacenters : "MOLD_VMWARE".equalsIgnoreCase(siteType));
        request.setCredentialInput(buildCredentialInput());
        return request;
    }

    private DrSiteCredentialInput buildCredentialInput() {
        DrSiteCredentialInput input = new DrSiteCredentialInput();
        input.setCredentialType(DrConstants.CREDENTIAL_TYPE_MOLD_API);
        input.setEndpoint(moldApiUrl);
        input.setApiKey(moldApiKey);
        input.setSecretKey(moldSecretKey);
        input.setTlsVerify(tlsVerify);
        return input.hasCredentialData() ? input : null;
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
        return DisasterRecoveryClusterEventTypes.EVENT_DR_SITE_INVENTORY;
    }

    @Override
    public String getEventDescription() {
        return "Discovering DR site inventory";
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.DisasterRecoveryCluster;
    }
}
