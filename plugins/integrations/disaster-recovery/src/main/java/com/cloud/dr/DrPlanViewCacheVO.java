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
@Table(name = "dr_plan_view_cache")
public class DrPlanViewCacheVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "plan_id")
    private long planId;

    @Column(name = "snapshot_version")
    private int snapshotVersion;

    @Column(name = "snapshot_json", length = 16777215, columnDefinition = "MEDIUMTEXT")
    private String snapshotJson;

    @Column(name = "projection_state")
    private String projectionState;

    @Column(name = "last_error", length = 4096)
    private String lastError;

    @Column(name = "last_refresh_error_code")
    private String lastRefreshErrorCode;

    @Column(name = "last_refresh_error_message", length = 4096)
    private String lastRefreshErrorMessage;

    @Column(name = "last_refresh_failed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastRefreshFailedAt;

    @Column(name = "generated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date generated;

    @Column(name = "created")
    @Temporal(TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "updated")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updated;

    protected DrPlanViewCacheVO() {
    }

    public DrPlanViewCacheVO(long planId) {
        this.planId = planId;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getPlanId() {
        return planId;
    }

    public int getSnapshotVersion() {
        return snapshotVersion;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public String getProjectionState() {
        return projectionState;
    }

    public String getLastError() {
        return lastError;
    }

    public Date getGenerated() {
        return generated;
    }

    public String getLastRefreshErrorCode() { return lastRefreshErrorCode; }

    public String getLastRefreshErrorMessage() { return lastRefreshErrorMessage; }

    public Date getLastRefreshFailedAt() { return lastRefreshFailedAt; }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void updateSnapshot(int version, String json, String state, String error) {
        setSnapshotVersion(version);
        setSnapshotJson(json);
        setProjectionState(state);
        setLastError(error);
        setLastRefreshErrorCode(null);
        setLastRefreshErrorMessage(null);
        setLastRefreshFailedAt(null);
        Date now = new Date();
        setGenerated(now);
        setUpdated(now);
    }

    public void setSnapshotVersion(int snapshotVersion) {
        this.snapshotVersion = snapshotVersion;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public void setProjectionState(String projectionState) {
        this.projectionState = projectionState;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public void setLastRefreshErrorCode(String lastRefreshErrorCode) { this.lastRefreshErrorCode = lastRefreshErrorCode; }

    public void setLastRefreshErrorMessage(String lastRefreshErrorMessage) { this.lastRefreshErrorMessage = lastRefreshErrorMessage; }

    public void setLastRefreshFailedAt(Date lastRefreshFailedAt) { this.lastRefreshFailedAt = lastRefreshFailedAt; }

    public void markRefreshFailed(String errorCode, String message) {
        setProjectionState("DEGRADED");
        setLastError(message);
        setLastRefreshErrorCode(errorCode);
        setLastRefreshErrorMessage(message);
        Date now = new Date();
        setLastRefreshFailedAt(now);
        setUpdated(now);
    }

    public void setGenerated(Date generated) {
        this.generated = generated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }
}
