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
import com.cloud.dr.DrSiteCredentialInput;
import com.cloud.dr.DrSiteVO;
import com.cloud.dr.cluster.DisasterRecoveryClusterEventTypes;
import com.cloud.dr.response.DrResponseGenerator;

@APICommand(name = UpdateDrSiteCmd.APINAME,
        description = "Update a Cross Hypervisor DR site",
        responseObject = DrSiteResponse.class,
        authorized = {RoleType.Admin})
public class UpdateDrSiteCmd extends BaseAsyncCmd {
    public static final String APINAME = "updateDrSite";

    @Inject
    private DrSiteService drSiteService;
    @Inject
    private DrResponseGenerator drResponseGenerator;

    @Parameter(name = "id", type = CommandType.UUID, entityType = DrSiteResponse.class, required = true, description = "the DR site ID")
    private Long id;

    @Parameter(name = "name", type = CommandType.STRING, description = "the DR site name")
    private String name;

    @Parameter(name = "description", type = CommandType.STRING, description = "the DR site description")
    private String description;

    @Parameter(name = "sitetype", type = CommandType.STRING, description = "the DR site type")
    private String siteType;

    @Parameter(name = "hypervisortype", type = CommandType.STRING, description = "the hypervisor type")
    private String hypervisorType;

    @Parameter(name = "endpoint", type = CommandType.STRING, description = "the DR endpoint URL or address")
    private String endpoint;

    @Parameter(name = "credentialref", type = CommandType.STRING, description = "the credential reference")
    private String credentialRef;

    @Parameter(name = "credentialtype", type = CommandType.STRING, description = "the credential type. Supported values: MOLD_API, VCENTER")
    private String credentialType;

    @Parameter(name = "moldapiurl", type = CommandType.STRING, description = "the Mold API URL for the DR site")
    private String moldApiUrl;

    @Parameter(name = "moldapikey", type = CommandType.STRING, description = "the Mold API key for the DR site")
    private String moldApiKey;

    @Parameter(name = "moldsecretkey", type = CommandType.STRING, description = "the Mold secret key for the DR site")
    private String moldSecretKey;

    @Parameter(name = "vcenterurl", type = CommandType.STRING, description = "the vCenter URL for the DR site")
    private String vCenterUrl;

    @Parameter(name = "vcenterusername", type = CommandType.STRING, description = "the vCenter username for the DR site")
    private String vCenterUsername;

    @Parameter(name = "vcenterpassword", type = CommandType.STRING, description = "the vCenter password for the DR site")
    private String vCenterPassword;

    @Parameter(name = "tlsverify", type = CommandType.BOOLEAN, description = "true to verify TLS certificates for the DR site endpoint")
    private Boolean tlsVerify;

    @Parameter(name = "clearcredential", type = CommandType.BOOLEAN, description = "true to remove stored DR site credentials")
    private Boolean clearCredential;

    @Parameter(name = "zoneid", type = CommandType.LONG, description = "the local CloudStack zone ID")
    private Long zoneId;

    @Parameter(name = "zoneexternalid", type = CommandType.STRING, description = "the remote site zone external ID")
    private String zoneExternalId;

    @Parameter(name = "zonename", type = CommandType.STRING, description = "the remote site zone display name")
    private String zoneName;

    @Parameter(name = "vmwaredcid", type = CommandType.LONG, description = "the VMware datacenter ID")
    private Long vmwareDatacenterId;

    @Parameter(name = "vmwaredcexternalid", type = CommandType.STRING, description = "the remote site VMware datacenter external ID")
    private String vmwareDatacenterExternalId;

    @Parameter(name = "vmwaredcname", type = CommandType.STRING, description = "the remote site VMware datacenter display name")
    private String vmwareDatacenterName;

    @Parameter(name = "state", type = CommandType.STRING, description = "the site state")
    private String state;

    @Parameter(name = "healthstate", type = CommandType.STRING, description = "the site health state")
    private String healthState;

    @Parameter(name = "capabilityjson", type = CommandType.STRING, description = "the site capability JSON")
    private String capabilitiesJson;

    @Override
    public void execute() throws ServerApiException {
        try {
            DrSiteVO update = new DrSiteVO(name, siteType, hypervisorType);
            update.setDescription(description);
            update.setEndpoint(endpoint);
            update.setCredentialRef(credentialRef);
            update.setZoneId(zoneId);
            update.setZoneExternalId(zoneExternalId);
            update.setZoneName(zoneName);
            update.setVmwareDatacenterId(vmwareDatacenterId);
            update.setVmwareDatacenterExternalId(vmwareDatacenterExternalId);
            update.setVmwareDatacenterName(vmwareDatacenterName);
            update.setState(state);
            update.setHealthState(healthState);
            update.setCapabilitiesJson(capabilitiesJson);
            DrSiteResponse response = drResponseGenerator.createSiteResponse(
                    drSiteService.updateSite(id, update, buildCredentialInput(), Boolean.TRUE.equals(clearCredential)));
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
        return DisasterRecoveryClusterEventTypes.EVENT_DR_SITE_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating DR site " + id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.DisasterRecoveryCluster;
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    private DrSiteCredentialInput buildCredentialInput() {
        DrSiteCredentialInput input = new DrSiteCredentialInput();
        input.setCredentialType(credentialType);
        input.setTlsVerify(tlsVerify);
        if (vCenterUrl != null || vCenterUsername != null || vCenterPassword != null) {
            input.setEndpoint(vCenterUrl);
            input.setPrincipal(vCenterUsername);
            input.setPassword(vCenterPassword);
        } else {
            input.setEndpoint(moldApiUrl);
            input.setApiKey(moldApiKey);
            input.setSecretKey(moldSecretKey);
        }
        return input.hasCredentialData() ? input : null;
    }
}
