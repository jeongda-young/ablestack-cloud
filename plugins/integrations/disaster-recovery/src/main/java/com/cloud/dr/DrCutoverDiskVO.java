// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.dr;

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
@Table(name = "dr_cutover_disk")
public class DrCutoverDiskVO implements InternalIdentity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private long id;
    @Column(name = "session_id") private long sessionId;
    @Column(name = "disk_index") private int diskIndex;
    private String provider;
    @Column(name = "checkpoint_ref") private String checkpointRef;
    @Column(name = "writable_ref") private String writableRef;
    @Column(name = "rollback_ref") private String rollbackRef;
    private String state;
    @Column(name = "target_volume_id") private Long targetVolumeId;
    @Column(name = "target_volume_uuid") private String targetVolumeUuid;
    @Column(name = "checkpoint_sequence") private Long checkpointSequence;
    @Column(name = "manifest_sha256") private String manifestSha256;
    @Column(name = "details_json", length = 16777215) private String detailsJson;
    @Temporal(TemporalType.TIMESTAMP) private Date created = new Date();
    @Temporal(TemporalType.TIMESTAMP) private Date updated = new Date();
    @Temporal(TemporalType.TIMESTAMP) private Date removed;
    protected DrCutoverDiskVO() { }
    public DrCutoverDiskVO(long sessionId, int diskIndex) { this.sessionId = sessionId; this.diskIndex = diskIndex; }
    @Override public long getId() { return id; }
    public long getSessionId() { return sessionId; }
    public int getDiskIndex() { return diskIndex; }
    public String getProvider() { return provider; }
    public String getCheckpointRef() { return checkpointRef; }
    public String getWritableRef() { return writableRef; }
    public String getRollbackRef() { return rollbackRef; }
    public String getState() { return state; }
    public Long getTargetVolumeId() { return targetVolumeId; }
    public String getTargetVolumeUuid() { return targetVolumeUuid; }
    public Long getCheckpointSequence() { return checkpointSequence; }
    public String getManifestSha256() { return manifestSha256; }
    public Date getRemoved() { return removed; }
    public void setProvider(String value) { provider = value; }
    public void setCheckpointRef(String value) { checkpointRef = value; }
    public void setWritableRef(String value) { writableRef = value; }
    public void setRollbackRef(String value) { rollbackRef = value; }
    public void setState(String value) { state = value; }
    public void setTargetVolumeId(Long value) { targetVolumeId = value; }
    public void setTargetVolumeUuid(String value) { targetVolumeUuid = value; }
    public void setCheckpointSequence(Long value) { checkpointSequence = value; }
    public void setManifestSha256(String value) { manifestSha256 = value; }
    public void setDetailsJson(String value) { detailsJson = value; }
    public void markUpdated() { updated = new Date(); }
    public void setRemoved(Date value) { removed = value; }
}
