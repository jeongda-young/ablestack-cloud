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
package com.cloud.dr;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "dr_site_health_check")
public class DrSiteHealthCheckVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "site_id")
    private long siteId;

    @Column(name = "site_uuid")
    private String siteUuid;

    @Column(name = "site_name")
    private String siteName;

    @Column(name = "site_type")
    private String siteType;

    @Column(name = "hypervisor_type")
    private String hypervisorType;

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "credential_id")
    private Long credentialId;

    @Column(name = "credential_state")
    private String credentialState;

    @Column(name = "trigger_type")
    private String triggerType;

    @Column(name = "health_state")
    private String healthState;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "message", length = 65535)
    private String message;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "checked_at")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date checkedAt;

    @Column(name = "management_server_id")
    private Long managementServerId;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "details_json", length = 65535)
    private String detailsJson;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = new Date();

    protected DrSiteHealthCheckVO() {
    }

    public DrSiteHealthCheckVO(long siteId, String triggerType, String healthState) {
        this.siteId = siteId;
        this.triggerType = triggerType;
        this.healthState = healthState;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public long getSiteId() {
        return siteId;
    }

    public String getSiteUuid() {
        return siteUuid;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getSiteType() {
        return siteType;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public String getCredentialState() {
        return credentialState;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public String getHealthState() {
        return healthState;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getMessage() {
        return message;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public Date getCheckedAt() {
        return checkedAt;
    }

    public Long getManagementServerId() {
        return managementServerId;
    }

    public String getJobId() {
        return jobId;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public Date getCreated() {
        return created;
    }

    public void setSiteUuid(String siteUuid) {
        this.siteUuid = siteUuid;
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
}
