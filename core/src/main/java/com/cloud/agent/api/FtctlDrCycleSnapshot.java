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
package com.cloud.agent.api;

public class FtctlDrCycleSnapshot {

    private String planUuid;
    private String runUuid;
    private Long sequence;
    private String cycleToken;
    private String state;
    private String requestedMode;
    private String effectiveMode;
    private Long baselineGeneration;
    private Boolean incrementalVerified;
    private Boolean metricsEstimated;
    private Long virtualBytes;
    private Long changedBytes;
    private Long sourceReadBytes;
    private Long targetWrittenBytes;
    private Long transferPayloadBytes;
    private Long changedExtentCount;
    private Long durationMs;
    private Long throughputBps;
    private String sourceCheckpointAt;
    private String targetDurableAt;
    private String nbdTeardownState;
    private Long nbdTeardownStartedAtEpochMs;
    private Long nbdTeardownCompletedAtEpochMs;
    private Long nbdTeardownDurationMs;
    private Integer nbdSourceDeviceCount;
    private Integer nbdTargetDeviceCount;
    private Integer nbdQuarantinedDeviceCount;
    private String nbdTeardownErrorCode;
    private String nbdTeardownErrorMessage;

    public String getPlanUuid() { return planUuid; }
    public void setPlanUuid(String value) { planUuid = value; }
    public String getRunUuid() { return runUuid; }
    public void setRunUuid(String value) { runUuid = value; }
    public Long getSequence() { return sequence; }
    public void setSequence(Long value) { sequence = value; }
    public String getCycleToken() { return cycleToken; }
    public void setCycleToken(String value) { cycleToken = value; }
    public String getState() { return state; }
    public void setState(String value) { state = value; }
    public String getRequestedMode() { return requestedMode; }
    public void setRequestedMode(String value) { requestedMode = value; }
    public String getEffectiveMode() { return effectiveMode; }
    public void setEffectiveMode(String value) { effectiveMode = value; }
    public Long getBaselineGeneration() { return baselineGeneration; }
    public void setBaselineGeneration(Long value) { baselineGeneration = value; }
    public Boolean getIncrementalVerified() { return incrementalVerified; }
    public void setIncrementalVerified(Boolean value) { incrementalVerified = value; }
    public Boolean getMetricsEstimated() { return metricsEstimated; }
    public void setMetricsEstimated(Boolean value) { metricsEstimated = value; }
    public Long getVirtualBytes() { return virtualBytes; }
    public void setVirtualBytes(Long value) { virtualBytes = value; }
    public Long getChangedBytes() { return changedBytes; }
    public void setChangedBytes(Long value) { changedBytes = value; }
    public Long getSourceReadBytes() { return sourceReadBytes; }
    public void setSourceReadBytes(Long value) { sourceReadBytes = value; }
    public Long getTargetWrittenBytes() { return targetWrittenBytes; }
    public void setTargetWrittenBytes(Long value) { targetWrittenBytes = value; }
    public Long getTransferPayloadBytes() { return transferPayloadBytes; }
    public void setTransferPayloadBytes(Long value) { transferPayloadBytes = value; }
    public Long getChangedExtentCount() { return changedExtentCount; }
    public void setChangedExtentCount(Long value) { changedExtentCount = value; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long value) { durationMs = value; }
    public Long getThroughputBps() { return throughputBps; }
    public void setThroughputBps(Long value) { throughputBps = value; }
    public String getSourceCheckpointAt() { return sourceCheckpointAt; }
    public void setSourceCheckpointAt(String value) { sourceCheckpointAt = value; }
    public String getTargetDurableAt() { return targetDurableAt; }
    public void setTargetDurableAt(String value) { targetDurableAt = value; }
    public String getNbdTeardownState() { return nbdTeardownState; }
    public void setNbdTeardownState(String value) { nbdTeardownState = value; }
    public Long getNbdTeardownStartedAtEpochMs() { return nbdTeardownStartedAtEpochMs; }
    public void setNbdTeardownStartedAtEpochMs(Long value) { nbdTeardownStartedAtEpochMs = value; }
    public Long getNbdTeardownCompletedAtEpochMs() { return nbdTeardownCompletedAtEpochMs; }
    public void setNbdTeardownCompletedAtEpochMs(Long value) { nbdTeardownCompletedAtEpochMs = value; }
    public Long getNbdTeardownDurationMs() { return nbdTeardownDurationMs; }
    public void setNbdTeardownDurationMs(Long value) { nbdTeardownDurationMs = value; }
    public Integer getNbdSourceDeviceCount() { return nbdSourceDeviceCount; }
    public void setNbdSourceDeviceCount(Integer value) { nbdSourceDeviceCount = value; }
    public Integer getNbdTargetDeviceCount() { return nbdTargetDeviceCount; }
    public void setNbdTargetDeviceCount(Integer value) { nbdTargetDeviceCount = value; }
    public Integer getNbdQuarantinedDeviceCount() { return nbdQuarantinedDeviceCount; }
    public void setNbdQuarantinedDeviceCount(Integer value) { nbdQuarantinedDeviceCount = value; }
    public String getNbdTeardownErrorCode() { return nbdTeardownErrorCode; }
    public void setNbdTeardownErrorCode(String value) { nbdTeardownErrorCode = value; }
    public String getNbdTeardownErrorMessage() { return nbdTeardownErrorMessage; }
    public void setNbdTeardownErrorMessage(String value) { nbdTeardownErrorMessage = value; }
}
