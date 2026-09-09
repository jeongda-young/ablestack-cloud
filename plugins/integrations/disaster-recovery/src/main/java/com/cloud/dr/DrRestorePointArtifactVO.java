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
@Table(name = "dr_restore_point_artifact")
public class DrRestorePointArtifactVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "restore_point_id")
    private long restorePointId;

    @Column(name = "artifact_type")
    private String artifactType;

    @Column(name = "artifact_ref")
    private String artifactRef;

    @Column(name = "storage_pool_id")
    private Long storagePoolId;

    @Column(name = "datastore_ref")
    private String datastoreRef;

    @Column(name = "format")
    private String format;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "parent_artifact_id")
    private Long parentArtifactId;

    @Column(name = "state")
    private String state;

    @Column(name = "details_json", length = 65535)
    private String detailsJson;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    protected DrRestorePointArtifactVO() {
    }

    public DrRestorePointArtifactVO(long restorePointId, String artifactType, String artifactRef) {
        this.restorePointId = restorePointId;
        this.artifactType = artifactType;
        this.artifactRef = artifactRef;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public long getRestorePointId() {
        return restorePointId;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public String getArtifactRef() {
        return artifactRef;
    }

    public Long getStoragePoolId() {
        return storagePoolId;
    }

    public String getDatastoreRef() {
        return datastoreRef;
    }

    public String getFormat() {
        return format;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getChecksum() {
        return checksum;
    }

    public Long getParentArtifactId() {
        return parentArtifactId;
    }

    public String getState() {
        return state;
    }

    public String getDetailsJson() {
        return detailsJson;
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

    public void setStoragePoolId(Long storagePoolId) {
        this.storagePoolId = storagePoolId;
    }

    public void setDatastoreRef(String datastoreRef) {
        this.datastoreRef = datastoreRef;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setParentArtifactId(Long parentArtifactId) {
        this.parentArtifactId = parentArtifactId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public void markUpdated() {
        updated = new Date();
    }

    public void markRemoved() {
        removed = new Date();
        markUpdated();
    }
}
