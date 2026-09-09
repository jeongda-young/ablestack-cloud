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
@Table(name = "dr_replica")
public class DrReplicaVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "plan_id")
    private long planId;

    @Column(name = "target_site_id")
    private long targetSiteId;

    @Column(name = "target_vm_id")
    private Long targetVmId;

    @Column(name = "target_external_ref")
    private String targetExternalRef;

    @Column(name = "target_vm_name")
    private String targetVmName;

    @Column(name = "state")
    private String state;

    @Column(name = "power_state")
    private String powerState;

    @Column(name = "hypervisor_type")
    private String hypervisorType;

    @Column(name = "active_side")
    private String activeSide;

    @Column(name = "runtime_state_json", length = 65535)
    private String runtimeStateJson;

    @Column(name = "ownership_generation")
    private Long ownershipGeneration = 1L;

    @Column(name = "ownership_state")
    private String ownershipState;

    @Column(name = "materialization_digest")
    private String materializationDigest;

    @Column(name = "power_state_observed_at")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date powerStateObservedAt;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    protected DrReplicaVO() {
    }

    public DrReplicaVO(long planId, long targetSiteId) {
        this.planId = planId;
        this.targetSiteId = targetSiteId;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public long getPlanId() {
        return planId;
    }

    public long getTargetSiteId() {
        return targetSiteId;
    }

    public Long getTargetVmId() {
        return targetVmId;
    }

    public String getTargetExternalRef() {
        return targetExternalRef;
    }

    public String getTargetVmName() {
        return targetVmName;
    }

    public String getState() {
        return state;
    }

    public String getPowerState() {
        return powerState;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public String getActiveSide() {
        return activeSide;
    }

    public String getRuntimeStateJson() {
        return runtimeStateJson;
    }

    public Long getOwnershipGeneration() { return ownershipGeneration; }
    public String getOwnershipState() { return ownershipState; }
    public String getMaterializationDigest() { return materializationDigest; }
    public Date getPowerStateObservedAt() { return powerStateObservedAt; }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setTargetVmId(Long targetVmId) {
        this.targetVmId = targetVmId;
    }

    public void setTargetExternalRef(String targetExternalRef) {
        this.targetExternalRef = targetExternalRef;
    }

    public void setTargetVmName(String targetVmName) {
        this.targetVmName = targetVmName;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setPowerState(String powerState) {
        this.powerState = powerState;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public void setActiveSide(String activeSide) {
        this.activeSide = activeSide;
    }

    public void setRuntimeStateJson(String runtimeStateJson) {
        this.runtimeStateJson = runtimeStateJson;
    }

    public void setOwnershipGeneration(Long ownershipGeneration) { this.ownershipGeneration = ownershipGeneration; }
    public void setOwnershipState(String ownershipState) { this.ownershipState = ownershipState; }
    public void setMaterializationDigest(String materializationDigest) { this.materializationDigest = materializationDigest; }
    public void setPowerStateObservedAt(Date powerStateObservedAt) { this.powerStateObservedAt = powerStateObservedAt; }

    public void markUpdated() {
        updated = new Date();
    }

    public void markRemoved() {
        removed = new Date();
        markUpdated();
    }
}
