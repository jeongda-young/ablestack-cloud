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
package com.cloud.ftctl;

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
@Table(name = "ftctl_protection")
public class FtctlProtectionVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "primary_vm_id")
    private long primaryVmId;

    @Column(name = "secondary_vm_id")
    private Long secondaryVmId;

    @Column(name = "secondary_vm_name")
    private String secondaryVmName;

    @Column(name = "peer_host_id")
    private Long peerHostId;

    @Column(name = "target_storage_pool_id")
    private Long targetStoragePoolId;

    @Column(name = "mode")
    private String mode;

    @Column(name = "backend_mode")
    private String backendMode;

    @Column(name = "provisioning_backend")
    private String provisioningBackend;

    @Column(name = "fencing_policy")
    private String fencingPolicy;

    @Column(name = "xcolo_port_allocation_mode")
    private String xcoloPortAllocationMode;

    @Column(name = "xcolo_port_slot")
    private Integer xcoloPortSlot;

    @Column(name = "admin_state")
    private String adminState;

    @Column(name = "provisioning_state")
    private String provisioningState;

    @Column(name = "protection_state")
    private String protectionState;

    @Column(name = "transport_state")
    private String transportState;

    @Column(name = "active_side")
    private String activeSide;

    @Column(name = "fencing_state")
    private String fencingState;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    protected FtctlProtectionVO() {
    }

    public FtctlProtectionVO(long primaryVmId) {
        this.primaryVmId = primaryVmId;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public long getPrimaryVmId() {
        return primaryVmId;
    }

    public Long getSecondaryVmId() {
        return secondaryVmId;
    }

    public String getSecondaryVmName() {
        return secondaryVmName;
    }

    public Long getPeerHostId() {
        return peerHostId;
    }

    public Long getTargetStoragePoolId() {
        return targetStoragePoolId;
    }

    public String getMode() {
        return mode;
    }

    public String getBackendMode() {
        return backendMode;
    }

    public String getProvisioningBackend() {
        return provisioningBackend;
    }

    public String getFencingPolicy() {
        return fencingPolicy;
    }

    public String getXcoloPortAllocationMode() {
        return xcoloPortAllocationMode;
    }

    public Integer getXcoloPortSlot() {
        return xcoloPortSlot;
    }

    public String getAdminState() {
        return adminState;
    }

    public String getProvisioningState() {
        return provisioningState;
    }

    public String getProtectionState() {
        return protectionState;
    }

    public String getTransportState() {
        return transportState;
    }

    public String getActiveSide() {
        return activeSide;
    }

    public String getFencingState() {
        return fencingState;
    }

    public String getLastError() {
        return lastError;
    }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setSecondaryVmId(Long secondaryVmId) {
        this.secondaryVmId = secondaryVmId;
    }

    public void setSecondaryVmName(String secondaryVmName) {
        this.secondaryVmName = secondaryVmName;
    }

    public void setPeerHostId(Long peerHostId) {
        this.peerHostId = peerHostId;
    }

    public void setTargetStoragePoolId(Long targetStoragePoolId) {
        this.targetStoragePoolId = targetStoragePoolId;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setBackendMode(String backendMode) {
        this.backendMode = backendMode;
    }

    public void setProvisioningBackend(String provisioningBackend) {
        this.provisioningBackend = provisioningBackend;
    }

    public void setFencingPolicy(String fencingPolicy) {
        this.fencingPolicy = fencingPolicy;
    }

    public void setXcoloPortAllocationMode(String xcoloPortAllocationMode) {
        this.xcoloPortAllocationMode = xcoloPortAllocationMode;
    }

    public void setXcoloPortSlot(Integer xcoloPortSlot) {
        this.xcoloPortSlot = xcoloPortSlot;
    }

    public void setAdminState(String adminState) {
        this.adminState = adminState;
    }

    public void setProvisioningState(String provisioningState) {
        this.provisioningState = provisioningState;
    }

    public void setProtectionState(String protectionState) {
        this.protectionState = protectionState;
    }

    public void setTransportState(String transportState) {
        this.transportState = transportState;
    }

    public void setActiveSide(String activeSide) {
        this.activeSide = activeSide;
    }

    public void setFencingState(String fencingState) {
        this.fencingState = fencingState;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public void markUpdated() {
        updated = new Date();
    }

    public void markRemoved() {
        removed = new Date();
        markUpdated();
    }
}
