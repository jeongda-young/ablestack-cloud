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
@Table(name = "ftctl_protection_volume")
public class FtctlProtectionVolumeVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "protection_id")
    private long protectionId;

    @Column(name = "primary_volume_id")
    private long primaryVolumeId;

    @Column(name = "secondary_volume_id")
    private Long secondaryVolumeId;

    @Column(name = "primary_disk_path")
    private String primaryDiskPath;

    @Column(name = "secondary_disk_path")
    private String secondaryDiskPath;

    @Column(name = "disk_label")
    private String diskLabel;

    @Column(name = "replication_state")
    private String replicationState;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    protected FtctlProtectionVolumeVO() {
    }

    public FtctlProtectionVolumeVO(long protectionId, long primaryVolumeId) {
        this.protectionId = protectionId;
        this.primaryVolumeId = primaryVolumeId;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getProtectionId() {
        return protectionId;
    }

    public long getPrimaryVolumeId() {
        return primaryVolumeId;
    }

    public Long getSecondaryVolumeId() {
        return secondaryVolumeId;
    }

    public String getPrimaryDiskPath() {
        return primaryDiskPath;
    }

    public String getSecondaryDiskPath() {
        return secondaryDiskPath;
    }

    public String getDiskLabel() {
        return diskLabel;
    }

    public String getReplicationState() {
        return replicationState;
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

    public void setSecondaryVolumeId(Long secondaryVolumeId) {
        this.secondaryVolumeId = secondaryVolumeId;
    }

    public void setPrimaryDiskPath(String primaryDiskPath) {
        this.primaryDiskPath = primaryDiskPath;
    }

    public void setSecondaryDiskPath(String secondaryDiskPath) {
        this.secondaryDiskPath = secondaryDiskPath;
    }

    public void setDiskLabel(String diskLabel) {
        this.diskLabel = diskLabel;
    }

    public void setReplicationState(String replicationState) {
        this.replicationState = replicationState;
    }

    public void markUpdated() {
        updated = new Date();
    }

    public void markRemoved() {
        removed = new Date();
        markUpdated();
    }
}
