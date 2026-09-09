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
package org.apache.cloudstack.api.response.dr;

import java.util.Date;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dr.DrSiteVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DrSiteVO.class)
public class DrSiteResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the DR site ID")
    private String id;

    @SerializedName("name")
    @Param(description = "the DR site name")
    private String name;

    @SerializedName("description")
    @Param(description = "the DR site description")
    private String description;

    @SerializedName("sitetype")
    @Param(description = "the DR site type")
    private String siteType;

    @SerializedName("hypervisortype")
    @Param(description = "the hypervisor type")
    private String hypervisorType;

    @SerializedName("endpoint")
    @Param(description = "the endpoint URL or address")
    private String endpoint;

    @SerializedName("credentialref")
    @Param(description = "the masked credential reference")
    private String credentialRef;

    @SerializedName("credentialconfigured")
    @Param(description = "true if the DR site has stored credentials")
    private Boolean credentialConfigured;

    @SerializedName("credentialtype")
    @Param(description = "the stored credential type")
    private String credentialType;

    @SerializedName("credentialendpoint")
    @Param(description = "the credential endpoint")
    private String credentialEndpoint;

    @SerializedName("credentialprincipal")
    @Param(description = "the credential principal")
    private String credentialPrincipal;

    @SerializedName("credentialstate")
    @Param(description = "the credential state")
    private String credentialState;

    @SerializedName("credentiallastvalidated")
    @Param(description = "the credential last validation time")
    private Date credentialLastValidated;

    @SerializedName("activeplancount")
    @Param(description = "the active DR plan count that refers to this site")
    private Long activePlanCount;

    @SerializedName("zoneid")
    @Param(description = "the local CloudStack zone ID")
    private Long zoneId;

    @SerializedName("zoneexternalid")
    @Param(description = "the remote site zone external ID")
    private String zoneExternalId;

    @SerializedName("zonename")
    @Param(description = "the remote site zone display name")
    private String zoneName;

    @SerializedName("vmwaredcid")
    @Param(description = "the VMware datacenter ID")
    private Long vmwareDatacenterId;

    @SerializedName("vmwaredcexternalid")
    @Param(description = "the remote site VMware datacenter external ID")
    private String vmwareDatacenterExternalId;

    @SerializedName("vmwaredcname")
    @Param(description = "the remote site VMware datacenter display name")
    private String vmwareDatacenterName;

    @SerializedName("state")
    @Param(description = "the administrative state")
    private String state;

    @SerializedName("healthstate")
    @Param(description = "the site health state")
    private String healthState;

    @SerializedName("healthreasoncode")
    @Param(description = "the site health reason code")
    private String healthReasonCode;

    @SerializedName("healthmessage")
    @Param(description = "the site health message")
    private String healthMessage;

    @SerializedName("healthlatencyms")
    @Param(description = "the last site health check latency in milliseconds")
    private Long healthLatencyMs;

    @SerializedName("capabilities")
    @Param(description = "the site capabilities JSON")
    private String capabilitiesJson;

    @SerializedName("lastchecked")
    @Param(description = "the last check time")
    private Date lastChecked;

    @SerializedName("created")
    @Param(description = "the creation date")
    private Date created;

    @SerializedName("removed")
    @Param(description = "the removal date")
    private Date removed;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setCredentialRef(String credentialRef) {
        this.credentialRef = credentialRef;
    }

    public void setCredentialConfigured(Boolean credentialConfigured) {
        this.credentialConfigured = credentialConfigured;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public void setCredentialEndpoint(String credentialEndpoint) {
        this.credentialEndpoint = credentialEndpoint;
    }

    public void setCredentialPrincipal(String credentialPrincipal) {
        this.credentialPrincipal = credentialPrincipal;
    }

    public void setCredentialState(String credentialState) {
        this.credentialState = credentialState;
    }

    public void setCredentialLastValidated(Date credentialLastValidated) {
        this.credentialLastValidated = credentialLastValidated;
    }

    public void setActivePlanCount(Long activePlanCount) {
        this.activePlanCount = activePlanCount;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneExternalId(String zoneExternalId) {
        this.zoneExternalId = zoneExternalId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setVmwareDatacenterId(Long vmwareDatacenterId) {
        this.vmwareDatacenterId = vmwareDatacenterId;
    }

    public void setVmwareDatacenterExternalId(String vmwareDatacenterExternalId) {
        this.vmwareDatacenterExternalId = vmwareDatacenterExternalId;
    }

    public void setVmwareDatacenterName(String vmwareDatacenterName) {
        this.vmwareDatacenterName = vmwareDatacenterName;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setHealthState(String healthState) {
        this.healthState = healthState;
    }

    public void setHealthReasonCode(String healthReasonCode) {
        this.healthReasonCode = healthReasonCode;
    }

    public void setHealthMessage(String healthMessage) {
        this.healthMessage = healthMessage;
    }

    public void setHealthLatencyMs(Long healthLatencyMs) {
        this.healthLatencyMs = healthLatencyMs;
    }

    public void setCapabilitiesJson(String capabilitiesJson) {
        this.capabilitiesJson = capabilitiesJson;
    }

    public void setLastChecked(Date lastChecked) {
        this.lastChecked = lastChecked;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
}
