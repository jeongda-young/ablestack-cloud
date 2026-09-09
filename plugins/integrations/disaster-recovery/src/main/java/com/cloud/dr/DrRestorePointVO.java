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
@Table(name = "dr_restore_point")
public class DrRestorePointVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "plan_id")
    private long planId;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "checkpoint_sequence")
    private Long checkpointSequence;

    @Column(name = "checkpoint_cycle_type")
    private String checkpointCycleType;

    @Column(name = "checkpoint_ref_hash")
    private byte[] checkpointRefHash;

    @Column(name = "source_snapshot_ref")
    private String sourceSnapshotRef;

    @Column(name = "source_created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date sourceCreated;

    @Column(name = "target_ready_at")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date targetReadyAt;

    @Column(name = "source_rpo_seconds")
    private Integer sourceRpoSeconds;

    @Column(name = "target_ready_rpo_seconds")
    private Integer targetReadyRpoSeconds;

    @Column(name = "consistency_level")
    private String consistencyLevel;

    @Column(name = "restore_point_type")
    private String restorePointType;

    @Column(name = "state")
    private String state;

    @Column(name = "effective_mode")
    private String effectiveMode;

    @Column(name = "requested_mode")
    private String requestedMode;

    @Column(name = "automatic_reseed")
    private Boolean automaticReseed;

    @Column(name = "mode_decision_code")
    private String modeDecisionCode;

    @Column(name = "reseed_reason")
    private String reseedReason;

    @Column(name = "invalid_baseline_disk_count")
    private Integer invalidBaselineDiskCount;

    @Column(name = "incremental_verified")
    private Boolean incrementalVerified;

    @Column(name = "metrics_estimated")
    private Boolean metricsEstimated;

    @Column(name = "virtual_bytes")
    private Long virtualBytes;

    @Column(name = "changed_bytes")
    private Long changedBytes;

    @Column(name = "source_read_bytes")
    private Long sourceReadBytes;

    @Column(name = "target_written_bytes")
    private Long targetWrittenBytes;

    @Column(name = "transfer_payload_bytes")
    private Long transferPayloadBytes;

    @Column(name = "changed_extent_count")
    private Long changedExtentCount;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "throughput_bps")
    private Long throughputBps;

    @Column(name = "baseline_generation")
    private Long baselineGeneration;

    @Column(name = "cycle_token")
    private String cycleToken;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    protected DrRestorePointVO() {
    }

    public DrRestorePointVO(long planId, String restorePointType) {
        this.planId = planId;
        this.restorePointType = restorePointType;
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

    public Long getRunId() {
        return runId;
    }

    public Long getCheckpointSequence() {
        return checkpointSequence;
    }

    public String getCheckpointCycleType() {
        return checkpointCycleType;
    }

    public byte[] getCheckpointRefHash() {
        return checkpointRefHash;
    }

    public String getSourceSnapshotRef() {
        return sourceSnapshotRef;
    }

    public Date getSourceCreated() {
        return sourceCreated;
    }

    public Date getTargetReadyAt() {
        return targetReadyAt;
    }

    public Integer getSourceRpoSeconds() {
        return sourceRpoSeconds;
    }

    public Integer getTargetReadyRpoSeconds() {
        return targetReadyRpoSeconds;
    }

    public String getConsistencyLevel() {
        return consistencyLevel;
    }

    public String getRestorePointType() {
        return restorePointType;
    }

    public String getState() {
        return state;
    }

    public String getEffectiveMode() { return effectiveMode; }
    public String getRequestedMode() { return requestedMode; }
    public Boolean getAutomaticReseed() { return automaticReseed; }
    public String getModeDecisionCode() { return modeDecisionCode; }
    public String getReseedReason() { return reseedReason; }
    public Integer getInvalidBaselineDiskCount() { return invalidBaselineDiskCount; }
    public Boolean getIncrementalVerified() { return incrementalVerified; }
    public Boolean getMetricsEstimated() { return metricsEstimated; }
    public Long getVirtualBytes() { return virtualBytes; }
    public Long getChangedBytes() { return changedBytes; }
    public Long getSourceReadBytes() { return sourceReadBytes; }
    public Long getTargetWrittenBytes() { return targetWrittenBytes; }
    public Long getTransferPayloadBytes() { return transferPayloadBytes; }
    public Long getChangedExtentCount() { return changedExtentCount; }
    public Long getDurationMs() { return durationMs; }
    public Long getThroughputBps() { return throughputBps; }
    public Long getBaselineGeneration() { return baselineGeneration; }
    public String getCycleToken() { return cycleToken; }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setSourceSnapshotRef(String sourceSnapshotRef) {
        this.sourceSnapshotRef = sourceSnapshotRef;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public void setCheckpointSequence(Long checkpointSequence) {
        this.checkpointSequence = checkpointSequence;
    }

    public void setCheckpointCycleType(String checkpointCycleType) {
        this.checkpointCycleType = checkpointCycleType;
    }

    public void setCheckpointRefHash(byte[] checkpointRefHash) {
        this.checkpointRefHash = checkpointRefHash;
    }

    public void setSourceCreated(Date sourceCreated) {
        this.sourceCreated = sourceCreated;
    }

    public void setTargetReadyAt(Date targetReadyAt) {
        this.targetReadyAt = targetReadyAt;
    }

    public void setSourceRpoSeconds(Integer sourceRpoSeconds) {
        this.sourceRpoSeconds = sourceRpoSeconds;
    }

    public void setTargetReadyRpoSeconds(Integer targetReadyRpoSeconds) {
        this.targetReadyRpoSeconds = targetReadyRpoSeconds;
    }

    public void setConsistencyLevel(String consistencyLevel) {
        this.consistencyLevel = consistencyLevel;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setEffectiveMode(String effectiveMode) { this.effectiveMode = effectiveMode; }
    public void setRequestedMode(String requestedMode) { this.requestedMode = requestedMode; }
    public void setAutomaticReseed(Boolean automaticReseed) { this.automaticReseed = automaticReseed; }
    public void setModeDecisionCode(String modeDecisionCode) { this.modeDecisionCode = modeDecisionCode; }
    public void setReseedReason(String reseedReason) { this.reseedReason = reseedReason; }
    public void setInvalidBaselineDiskCount(Integer invalidBaselineDiskCount) { this.invalidBaselineDiskCount = invalidBaselineDiskCount; }
    public void setIncrementalVerified(Boolean incrementalVerified) { this.incrementalVerified = incrementalVerified; }
    public void setMetricsEstimated(Boolean metricsEstimated) { this.metricsEstimated = metricsEstimated; }
    public void setVirtualBytes(Long virtualBytes) { this.virtualBytes = virtualBytes; }
    public void setChangedBytes(Long changedBytes) { this.changedBytes = changedBytes; }
    public void setSourceReadBytes(Long sourceReadBytes) { this.sourceReadBytes = sourceReadBytes; }
    public void setTargetWrittenBytes(Long targetWrittenBytes) { this.targetWrittenBytes = targetWrittenBytes; }
    public void setTransferPayloadBytes(Long transferPayloadBytes) { this.transferPayloadBytes = transferPayloadBytes; }
    public void setChangedExtentCount(Long changedExtentCount) { this.changedExtentCount = changedExtentCount; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public void setThroughputBps(Long throughputBps) { this.throughputBps = throughputBps; }
    public void setBaselineGeneration(Long baselineGeneration) { this.baselineGeneration = baselineGeneration; }
    public void setCycleToken(String cycleToken) { this.cycleToken = cycleToken; }

    public void markUpdated() {
        updated = new Date();
    }

    public void markRemoved() {
        checkpointRefHash = null;
        removed = new Date();
        markUpdated();
    }
}
