// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
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
@Table(name = "dr_target_resource_claim")
public class DrTargetResourceClaimVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "plan_id")
    private long planId;

    @Column(name = "replica_id")
    private long replicaId;

    @Column(name = "replica_disk_id")
    private Long replicaDiskId;

    @Column(name = "claim_run_id")
    private Long claimRunId;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "resource_uuid")
    private String resourceUuid;

    @Column(name = "resource_locator_hash")
    private String resourceLocatorHash;

    @Column(name = "ownership_generation")
    private long ownershipGeneration;

    @Column(name = "claim_state")
    private String claimState;

    @Column(name = "active_resource_key")
    private String activeResourceKey;

    @Column(name = "active_role_key")
    private String activeRoleKey;

    @Column(name = "manifest_sha256")
    private String manifestSha256;

    @Column(name = "created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updated = new Date();

    @Column(name = "released")
    @Temporal(TemporalType.TIMESTAMP)
    private Date released;

    protected DrTargetResourceClaimVO() {
    }

    public DrTargetResourceClaimVO(long planId, long replicaId, Long replicaDiskId, Long claimRunId,
            String resourceType, Long resourceId, String resourceUuid, String resourceLocatorHash,
            long ownershipGeneration, String activeResourceKey, String activeRoleKey) {
        this.planId = planId;
        this.replicaId = replicaId;
        this.replicaDiskId = replicaDiskId;
        this.claimRunId = claimRunId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.resourceUuid = resourceUuid;
        this.resourceLocatorHash = resourceLocatorHash;
        this.ownershipGeneration = ownershipGeneration;
        this.activeResourceKey = activeResourceKey;
        this.activeRoleKey = activeRoleKey;
        this.claimState = "CLAIMED";
    }

    @Override
    public long getId() { return id; }
    public String getUuid() { return uuid; }
    public long getPlanId() { return planId; }
    public long getReplicaId() { return replicaId; }
    public Long getReplicaDiskId() { return replicaDiskId; }
    public Long getClaimRunId() { return claimRunId; }
    public String getResourceType() { return resourceType; }
    public Long getResourceId() { return resourceId; }
    public String getResourceUuid() { return resourceUuid; }
    public String getResourceLocatorHash() { return resourceLocatorHash; }
    public long getOwnershipGeneration() { return ownershipGeneration; }
    public String getClaimState() { return claimState; }
    public String getActiveResourceKey() { return activeResourceKey; }
    public String getActiveRoleKey() { return activeRoleKey; }
    public String getManifestSha256() { return manifestSha256; }
    public Date getCreated() { return created; }
    public Date getUpdated() { return updated; }
    public Date getReleased() { return released; }

    public void setClaimState(String claimState) { this.claimState = claimState; this.updated = new Date(); }
    public void setManifestSha256(String manifestSha256) { this.manifestSha256 = manifestSha256; this.updated = new Date(); }
    public void release() {
        this.claimState = "RELEASED";
        this.activeResourceKey = null;
        this.activeRoleKey = null;
        this.released = new Date();
        this.updated = this.released;
    }
}
