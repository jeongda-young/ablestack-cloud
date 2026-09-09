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

import com.cloud.dr.DrSiteHealthCheckVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DrSiteHealthCheckVO.class)
public class DrSiteHealthCheckResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the DR site health check ID")
    private String id;

    @SerializedName("siteid")
    @Param(description = "the DR site ID")
    private String siteId;

    @SerializedName("sitename")
    @Param(description = "the DR site name at check time")
    private String siteName;

    @SerializedName("sitetype")
    @Param(description = "the DR site type at check time")
    private String siteType;

    @SerializedName("hypervisortype")
    @Param(description = "the hypervisor type at check time")
    private String hypervisorType;

    @SerializedName("endpoint")
    @Param(description = "the endpoint checked")
    private String endpoint;

    @SerializedName("credentialid")
    @Param(description = "the credential row ID used by the check")
    private Long credentialId;

    @SerializedName("credentialstate")
    @Param(description = "the credential state at check time")
    private String credentialState;

    @SerializedName("triggertype")
    @Param(description = "the health check trigger type")
    private String triggerType;

    @SerializedName("healthstate")
    @Param(description = "the health check state")
    private String healthState;

    @SerializedName("reasoncode")
    @Param(description = "the health check reason code")
    private String reasonCode;

    @SerializedName("message")
    @Param(description = "the health check message")
    private String message;

    @SerializedName("latencyms")
    @Param(description = "the health check latency in milliseconds")
    private Long latencyMs;

    @SerializedName("checkedat")
    @Param(description = "the check time")
    private Date checkedAt;

    @SerializedName("managementserverid")
    @Param(description = "the management server ID that recorded the check")
    private Long managementServerId;

    @SerializedName("healthcheckjobid")
    @Param(description = "the async job ID associated with the check")
    private String jobId;

    @SerializedName("details")
    @Param(description = "the health check detail JSON without secrets")
    private String detailsJson;

    @SerializedName("created")
    @Param(description = "the creation date")
    private Date created;

    public void setId(String id) {
        this.id = id;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
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

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    public void setCredentialState(String credentialState) {
        this.credentialState = credentialState;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public void setHealthState(String healthState) {
        this.healthState = healthState;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public void setCheckedAt(Date checkedAt) {
        this.checkedAt = checkedAt;
    }

    public void setManagementServerId(Long managementServerId) {
        this.managementServerId = managementServerId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
