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
package com.cloud.vm;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "vm_guest_network_state")
public class VmGuestNetworkStateVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "vm_id", updatable = false, nullable = false)
    private long vmId;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "qga_version")
    private String qgaVersion;

    @Column(name = "collector_build_id")
    private String collectorBuildId;

    @Column(name = "collector_host_id")
    private Long collectorHostId;

    @Column(name = "capability_hash")
    private String capabilityHash;

    @Column(name = "guest_tools_version")
    private String guestToolsVersion;

    @Column(name = "qga_policy_mode")
    private String qgaPolicyMode;

    @Column(name = "readiness_status")
    private String readinessStatus;

    @Column(name = "readiness_checked_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date readinessCheckedAt;

    @Column(name = "observed_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date observedAt;

    @Column(name = "last_success_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSuccessAt;

    @Column(name = "payload_hash")
    private String payloadHash;

    @Column(name = "payload", length = 16777215)
    private String payload;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created", updatable = false, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "updated", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updated;

    protected VmGuestNetworkStateVO() {
    }

    public VmGuestNetworkStateVO(long vmId, Date now) {
        this.vmId = vmId;
        this.schemaVersion = 1;
        this.observedAt = now;
        this.created = now;
        this.updated = now;
    }

    public long getId() {
        return id;
    }

    public long getVmId() {
        return vmId;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQgaVersion() {
        return qgaVersion;
    }

    public void setQgaVersion(String qgaVersion) {
        this.qgaVersion = qgaVersion;
    }

    public String getCollectorBuildId() {
        return collectorBuildId;
    }

    public void setCollectorBuildId(String collectorBuildId) {
        this.collectorBuildId = collectorBuildId;
    }

    public Long getCollectorHostId() {
        return collectorHostId;
    }

    public void setCollectorHostId(Long collectorHostId) {
        this.collectorHostId = collectorHostId;
    }

    public String getCapabilityHash() {
        return capabilityHash;
    }

    public void setCapabilityHash(String capabilityHash) {
        this.capabilityHash = capabilityHash;
    }

    public String getGuestToolsVersion() {
        return guestToolsVersion;
    }

    public void setGuestToolsVersion(String guestToolsVersion) {
        this.guestToolsVersion = guestToolsVersion;
    }

    public String getQgaPolicyMode() {
        return qgaPolicyMode;
    }

    public void setQgaPolicyMode(String qgaPolicyMode) {
        this.qgaPolicyMode = qgaPolicyMode;
    }

    public String getReadinessStatus() {
        return readinessStatus;
    }

    public void setReadinessStatus(String readinessStatus) {
        this.readinessStatus = readinessStatus;
    }

    public Date getReadinessCheckedAt() {
        return readinessCheckedAt;
    }

    public void setReadinessCheckedAt(Date readinessCheckedAt) {
        this.readinessCheckedAt = readinessCheckedAt;
    }

    public Date getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Date observedAt) {
        this.observedAt = observedAt;
    }

    public Date getLastSuccessAt() {
        return lastSuccessAt;
    }

    public void setLastSuccessAt(Date lastSuccessAt) {
        this.lastSuccessAt = lastSuccessAt;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }
}
