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
package org.apache.cloudstack.api.response.dr;

import java.util.Date;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dr.DrRestorePointVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DrRestorePointVO.class)
public class DrRestorePointResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the DR restore point ID")
    private String id;

    @SerializedName("planid")
    @Param(description = "the DR plan ID")
    private Long planId;

    @SerializedName("runid")
    @Param(description = "the DR run ID that produced the synchronization checkpoint")
    private Long runId;

    @SerializedName("checkpointsequence")
    @Param(description = "the monotonically increasing synchronization checkpoint sequence")
    private Long checkpointSequence;

    @SerializedName("checkpointcycletype")
    @Param(description = "the synchronization cycle type")
    private String checkpointCycleType;

    @SerializedName("checkpointref")
    @Param(description = "the opaque FTCTL synchronization checkpoint reference")
    private String checkpointRef;

    @SerializedName("sourcesnapshotref")
    @Param(description = "the source snapshot reference")
    private String sourceSnapshotRef;

    @SerializedName("sourcecreated")
    @Param(description = "the source creation time")
    private Date sourceCreated;

    @SerializedName("targetreadyat")
    @Param(description = "the target-ready time")
    private Date targetReadyAt;

    @SerializedName("sourcerposeconds")
    @Param(description = "the source RPO in seconds")
    private Integer sourceRpoSeconds;

    @SerializedName("targetreadyrposeconds")
    @Param(description = "the target-ready RPO in seconds")
    private Integer targetReadyRpoSeconds;

    @SerializedName("consistencylevel")
    @Param(description = "the consistency level")
    private String consistencyLevel;

    @SerializedName("restorepointtype")
    @Param(description = "the restore point type")
    private String restorePointType;

    @SerializedName("state")
    @Param(description = "the restore point state")
    private String state;

    @SerializedName("effectivemode")
    @Param(description = "the effective replication mode used by the completed cycle")
    private String effectiveMode;

    @SerializedName("requestedmode")
    @Param(description = "the replication mode requested for the completed cycle")
    private String requestedMode;

    @SerializedName("automaticreseed")
    @Param(description = "whether FTCTL automatically promoted an incremental request to a full reseed")
    private Boolean automaticReseed;

    @SerializedName("modedecisioncode")
    @Param(description = "the stable FTCTL mode-decision reason code")
    private String modeDecisionCode;

    @SerializedName("reseedreason")
    @Param(description = "the stable reason that required a full reseed")
    private String reseedReason;

    @SerializedName("invalidbaselinediskcount")
    @Param(description = "the number of disks that failed baseline validation")
    private Integer invalidBaselineDiskCount;

    @SerializedName("incrementalverified")
    @Param(description = "whether CBT incremental transfer was verified for the completed cycle")
    private Boolean incrementalVerified;

    @SerializedName("metricsestimated")
    @Param(description = "whether byte metrics are estimates instead of measured transfer counters")
    private Boolean metricsEstimated;

    @SerializedName("virtualbytes") @Param(description = "the protected virtual disk bytes") private Long virtualBytes;
    @SerializedName("changedbytes") @Param(description = "the CBT changed bytes") private Long changedBytes;
    @SerializedName("sourcereadbytes") @Param(description = "the bytes read from the source") private Long sourceReadBytes;
    @SerializedName("targetwrittenbytes") @Param(description = "the bytes written to the target") private Long targetWrittenBytes;
    @SerializedName("transferpayloadbytes") @Param(description = "the transfer payload bytes") private Long transferPayloadBytes;
    @SerializedName("changedextentcount") @Param(description = "the number of changed CBT extents") private Long changedExtentCount;
    @SerializedName("durationms") @Param(description = "the transfer duration in milliseconds") private Long durationMs;
    @SerializedName("throughputbps") @Param(description = "the measured source throughput in bytes per second") private Long throughputBps;
    @SerializedName("baselinegeneration") @Param(description = "the committed CBT baseline generation") private Long baselineGeneration;
    @SerializedName("cycletoken") @Param(description = "the idempotent engine cycle token") private String cycleToken;

    @SerializedName("created")
    @Param(description = "the creation time")
    private Date created;

    public void setId(String id) {
        this.id = id;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
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

    public void setCheckpointRef(String checkpointRef) {
        this.checkpointRef = checkpointRef;
    }

    public void setSourceSnapshotRef(String sourceSnapshotRef) {
        this.sourceSnapshotRef = sourceSnapshotRef;
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

    public void setRestorePointType(String restorePointType) {
        this.restorePointType = restorePointType;
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

    public void setCreated(Date created) {
        this.created = created;
    }
}
