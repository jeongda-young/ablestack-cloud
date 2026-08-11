// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
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
@Table(name = "vm_guest_network_section_state")
public class VmGuestNetworkSectionStateVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "vm_id", nullable = false, updatable = false)
    private long vmId;

    @Column(name = "section", nullable = false, updatable = false)
    private String section;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "source")
    private String source;

    @Column(name = "observed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date observedAt;

    @Column(name = "last_success_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSuccessAt;

    @Column(name = "next_due_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date nextDueAt;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "payload_hash")
    private String payloadHash;

    @Column(name = "payload", length = 16777215)
    private String payload;

    @Column(name = "lease_owner")
    private String leaseOwner;

    @Column(name = "lease_until")
    @Temporal(TemporalType.TIMESTAMP)
    private Date leaseUntil;

    @Column(name = "created", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "updated", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updated;

    protected VmGuestNetworkSectionStateVO() {
    }

    public VmGuestNetworkSectionStateVO(long vmId, String section, Date now) {
        this.vmId = vmId;
        this.section = section;
        this.status = "NOT_COLLECTED";
        this.nextDueAt = now;
        this.created = now;
        this.updated = now;
    }

    public long getId() { return id; }
    public long getVmId() { return vmId; }
    public String getSection() { return section; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Date getObservedAt() { return observedAt; }
    public void setObservedAt(Date observedAt) { this.observedAt = observedAt; }
    public Date getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(Date lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }
    public Date getNextDueAt() { return nextDueAt; }
    public void setNextDueAt(Date nextDueAt) { this.nextDueAt = nextDueAt; }
    public int getFailureCount() { return failureCount; }
    public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getPayloadHash() { return payloadHash; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getLeaseOwner() { return leaseOwner; }
    public void setLeaseOwner(String leaseOwner) { this.leaseOwner = leaseOwner; }
    public Date getLeaseUntil() { return leaseUntil; }
    public void setLeaseUntil(Date leaseUntil) { this.leaseUntil = leaseUntil; }
    public Date getCreated() { return created; }
    public Date getUpdated() { return updated; }
    public void setUpdated(Date updated) { this.updated = updated; }
}
