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
@Table(name = "dr_replica_disk")
public class DrReplicaDiskVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "replica_id")
    private long replicaId;

    @Column(name = "source_volume_id")
    private Long sourceVolumeId;

    @Column(name = "target_volume_id")
    private Long targetVolumeId;

    @Column(name = "source_disk_ref")
    private String sourceDiskRef;

    @Column(name = "target_disk_ref")
    private String targetDiskRef;

    @Column(name = "disk_label")
    private String diskLabel;

    @Column(name = "format")
    private String format;

    @Column(name = "state")
    private String state;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "details_json", length = 65535)
    private String detailsJson;

    @Column(name = "target_claim_id")
    private Long targetClaimId;

    @Column(name = "artifact_uuid")
    private String artifactUuid;

    @Column(name = "locator_hash")
    private String locatorHash;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    protected DrReplicaDiskVO() {
    }

    public DrReplicaDiskVO(long replicaId, String diskLabel) {
        this.replicaId = replicaId;
        this.diskLabel = diskLabel;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public long getReplicaId() {
        return replicaId;
    }

    public Long getSourceVolumeId() {
        return sourceVolumeId;
    }

    public Long getTargetVolumeId() {
        return targetVolumeId;
    }

    public String getSourceDiskRef() {
        return sourceDiskRef;
    }

    public String getTargetDiskRef() {
        return targetDiskRef;
    }

    public String getDiskLabel() {
        return diskLabel;
    }

    public String getFormat() {
        return format;
    }

    public String getState() {
        return state;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public Long getTargetClaimId() { return targetClaimId; }
    public String getArtifactUuid() { return artifactUuid; }
    public String getLocatorHash() { return locatorHash; }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setSourceVolumeId(Long sourceVolumeId) {
        this.sourceVolumeId = sourceVolumeId;
    }

    public void setTargetVolumeId(Long targetVolumeId) {
        this.targetVolumeId = targetVolumeId;
    }

    public void setSourceDiskRef(String sourceDiskRef) {
        this.sourceDiskRef = sourceDiskRef;
    }

    public void setTargetDiskRef(String targetDiskRef) {
        this.targetDiskRef = targetDiskRef;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public void setTargetClaimId(Long targetClaimId) { this.targetClaimId = targetClaimId; }
    public void setArtifactUuid(String artifactUuid) { this.artifactUuid = artifactUuid; }
    public void setLocatorHash(String locatorHash) { this.locatorHash = locatorHash; }

    public void markUpdated() {
        updated = new Date();
    }

    public void markRemoved() {
        removed = new Date();
        markUpdated();
    }
}
