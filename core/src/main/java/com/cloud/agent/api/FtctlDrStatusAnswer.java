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

public class FtctlDrStatusAnswer extends Answer {

    private String planUuid;
    private String runUuid;
    private String statusScope;
    private String ftctlResult;
    private String state;
    private String step;
    private Integer progress;
    private String lastSourceCheckpointAt;
    private String lastTargetDurableAt;
    private Integer targetReadyRpoSeconds;
    private Integer targetRpoSeconds;
    private String schedulerNextRunAt;
    private Integer schedulerExecutionBudgetSeconds;
    private Integer schedulerCycleWallDurationSeconds;
    private Boolean targetMaterialized;
    private Boolean targetVmPresent;
    private Boolean targetStoragePresent;
    private Boolean targetNetworkPresent;
    private Boolean restorePointPresent;
    private Long eventsOffset;
    private String errorCode;
    private String errorMessage;
    private String failedComponent;
    private String dataCommitState;
    private Boolean dataCopied;
    private Boolean metadataCommitted;
    private Boolean targetDurable;
    private String cycleRetryMode;
    private Integer exitCode;
    private String output;
    private String statusJson;
    private String sourceDiskMapPath;
    private String targetDiskMapPath;
    private String diskMapRole;
    private Integer targetDiskCount;
    private Integer targetDiskInvalidCount;
    private String workerState;
    private Integer workerPid;
    private Long workerStartTicks;
    private Boolean workerPidAlive;
    private String workerStartedAt;
    private String workerUpdatedAt;
    private Integer workerExitCode;
    private Integer driverExitCode;
    private String failurePhase;
    private String terminalSource;
    private Integer terminalVersion;
    private Boolean terminalPublicationPending;
    private String terminalPublicationPendingSince;
    private String baselineFileState;
    private String sourceDiskProbeState;
    private Integer sourceDiskCount;
    private String targetWriterProbeState;
    private String operationIntent;
    private String requestedMode;
    private String effectiveMode;
    private String modeDecisionCode;
    private Boolean initialSeedRequired;
    private Long estimatedVirtualBytes;
    private Boolean retryable;
    private Integer retryAfterSeconds;
    private Boolean transitionReady;
    private Integer transitionSchemaVersion;
    private String transitionContractVersion;
    private String transitionOperation;
    private String transitionExpectedAuthority;
    private String transitionActiveSide;
    private Long transitionExpectedGeneration;
    private Long transitionAuthorityGeneration;
    private String transitionTargetPowerState;
    private String transitionSourceFenceState;
    private String transitionSourcePowerState;
    private Long transitionCheckedAtEpochMs;
    private Long currentCheckpointSequence;
    private String currentCheckpointCycleType;
    private String currentCheckpointRequestedMode;
    private String currentCheckpointEffectiveMode;
    private String currentCheckpointModeDecisionCode;
    private Boolean currentCheckpointAutomaticReseed;
    private Integer currentCheckpointInvalidBaselineDiskCount;
    private String currentCheckpointRef;
    private String currentCheckpointState;
    private Long latestCompletedCheckpointSequence;
    private Long latestCompletedCycleSequence;
    private String latestCompletedCheckpointCycleType;
    private String latestCompletedCheckpointRef;
    private String latestCompletedCheckpointState;
    private String latestCompletedProducerRunUuid;
    private String latestCompletedSourceCheckpointAt;
    private String latestCompletedTargetDurableAt;
    private Integer latestCompletedTargetReadyRpoSeconds;
    private String latestCompletedManifestPath;
    private String latestCompletedCheckpointPath;
    private String latestCompletedRequestedMode;
    private String latestCompletedEffectiveMode;
    private String latestCompletedModeDecisionCode;
    private String latestCompletedReseedReason;
    private Boolean latestCompletedAutomaticReseed;
    private Integer latestCompletedInvalidBaselineDiskCount;
    private Boolean latestCompletedIncrementalVerified;
    private Boolean latestCompletedMetricsEstimated;
    private Long latestCompletedVirtualBytes;
    private Long latestCompletedChangedBytes;
    private Long latestCompletedSourceReadBytes;
    private Long latestCompletedTargetWrittenBytes;
    private Long latestCompletedTransferPayloadBytes;
    private Long latestCompletedChangedExtentCount;
    private Long latestCompletedDurationMs;
    private Long latestCompletedThroughputBps;
    private Long latestCompletedBaselineGeneration;
    private String latestCompletedCycleToken;
    private String latestCompletedNbdTeardownState;
    private Long latestCompletedNbdTeardownStartedAtEpochMs;
    private Long latestCompletedNbdTeardownCompletedAtEpochMs;
    private Long latestCompletedNbdTeardownDurationMs;
    private Integer latestCompletedNbdSourceDeviceCount;
    private Integer latestCompletedNbdTargetDeviceCount;
    private Integer latestCompletedNbdQuarantinedDeviceCount;
    private String latestCompletedNbdTeardownErrorCode;
    private String latestCompletedNbdTeardownErrorMessage;
    private Integer controlProtocolVersion;
    private Long controlGeneration;
    private Long controlAckGeneration;
    private String controlState;
    private String cycleState;
    private String transitionState;
    private String checkpointLeaseState;
    private String guestPreparationState;
    private String guestFamily;
    private String testSessionState;
    private String testArtifactsState;
    private Integer testArtifactCount;
    private String testCleanupState;
    private Boolean cleanupRequired;
    private String guestPreparationManifestPath;
    private String manifestSchemaVersion;
    private String manifestSha256;
    private Long guestPreparationCheckpointSequence;
    private String testDomainName;
    private String testDomainState;
    private String testBootValidationMode;
    private Long runtimeGeneration;
    private Boolean schedulerPidAlive;
    private String schedulerDesiredState;
    private String schedulerServiceUnit;
    private String schedulerUnitActiveState;
    private String schedulerUnitSubState;
    private Long schedulerUnitMainPid;
    private String schedulerCgroup;
    private String schedulerRecoveryState;
    private String schedulerRecoveryTrigger;
    private String schedulerRecoveredAt;
    private String nbdTeardownState;
    private Integer nbdQuarantinedDeviceCount;
    private String nbdTeardownErrorCode;
    private String nbdTeardownErrorMessage;
    private String schedulerSessionUuid;
    private Long schedulerLeaseEpoch;
    private Long authoritySequence;
    private Long planCycleSequence;
    private Long resumeBaselineCheckpointSequence;
    private Long minimumCompletedCheckpointSequence;
    private Boolean immediateCyclePending;
    private String immediateCycleOwnerRun;
    private String schedulerHealth;
    private String replicationActivity;
    private String protectionState;
    private String activeWorkerRunUuid;
    private Long activeWorkerPid;
    private Long activeWorkerStartTicks;
    private String workerHeartbeatAt;
    private String controlRequestRunUuid;
    private Boolean ownerMatched;
    private String baselineState;
    private String reseedReason;
    private Integer consecutiveAutomaticReseedCount;
    private Integer cycleContractVersion;
    private String cycleEvidenceState;
    private String cycleEvidenceCode;
    private String cycleEvidenceMessage;
    private String replicationDirection;
    private String providerPair;
    private Integer routeContractVersion;
    private Integer reverseEvidenceContractVersion;
    private String reverseEvidenceState;
    private String reverseEvidenceRunUuid;
    private Long reverseBaselineGeneration;
    private String reverseBaselineState;
    private String reverseTrackerState;
    private String reverseWriterState;
    private Boolean reverseTargetWritten;
    private Boolean reverseWriteVerified;
    private String reverseGuestCompatibilityState;
    private String workerIdentityState;
    private String workerLivenessState;
    private String workerLaunchNonce;
    private Long workerGeneration;
    private String transferActivityState;
    private Long transferPayloadBytes;
    private Integer transferProgressSchemaVersion;
    private Long transferCycleSequence;
    private Long transferSampleSequence;
    private String transferPhase;
    private String transferMode;
    private Long transferBytesTotal;
    private Long transferBytesProcessed;
    private Long transferSourceReadBytes;
    private Long transferTargetWrittenBytes;
    private Long transferVerifiedBytes;
    private Double transferPercent;
    private Long transferThroughputBps;
    private Long transferEtaSeconds;
    private Integer transferCurrentDiskIndex;
    private Integer transferDiskCount;
    private Boolean transferProgressEstimated;
    private Long transferProgressSampleEpochMs;
    private Boolean transferProgressStale;
    private Integer ownedProcessCount;
    private Boolean reconciliationRequired;
    private Boolean runtimeEndpointsDrained;
    private Boolean terminalAuthoritative;
    private FtctlDrCycleSnapshot currentCycle;
    private FtctlDrCycleSnapshot latestCompletedCycle;

    public String getReplicationDirection() { return replicationDirection; }
    public void setReplicationDirection(String value) { replicationDirection = value; }
    public String getProviderPair() { return providerPair; }
    public void setProviderPair(String value) { providerPair = value; }
    public Integer getRouteContractVersion() { return routeContractVersion; }
    public void setRouteContractVersion(Integer value) { routeContractVersion = value; }
    public Integer getReverseEvidenceContractVersion() { return reverseEvidenceContractVersion; }
    public void setReverseEvidenceContractVersion(Integer value) { reverseEvidenceContractVersion = value; }
    public String getReverseEvidenceState() { return reverseEvidenceState; }
    public void setReverseEvidenceState(String value) { reverseEvidenceState = value; }
    public String getReverseEvidenceRunUuid() { return reverseEvidenceRunUuid; }
    public void setReverseEvidenceRunUuid(String value) { reverseEvidenceRunUuid = value; }
    public Long getReverseBaselineGeneration() { return reverseBaselineGeneration; }
    public void setReverseBaselineGeneration(Long value) { reverseBaselineGeneration = value; }
    public String getReverseBaselineState() { return reverseBaselineState; }
    public void setReverseBaselineState(String value) { reverseBaselineState = value; }
    public String getReverseTrackerState() { return reverseTrackerState; }
    public void setReverseTrackerState(String value) { reverseTrackerState = value; }
    public String getReverseWriterState() { return reverseWriterState; }
    public void setReverseWriterState(String value) { reverseWriterState = value; }
    public Boolean getReverseTargetWritten() { return reverseTargetWritten; }
    public void setReverseTargetWritten(Boolean value) { reverseTargetWritten = value; }
    public Boolean getReverseWriteVerified() { return reverseWriteVerified; }
    public void setReverseWriteVerified(Boolean value) { reverseWriteVerified = value; }
    public String getReverseGuestCompatibilityState() { return reverseGuestCompatibilityState; }
    public void setReverseGuestCompatibilityState(String value) { reverseGuestCompatibilityState = value; }
    public String getWorkerIdentityState() { return workerIdentityState; }
    public void setWorkerIdentityState(String value) { workerIdentityState = value; }
    public String getWorkerLivenessState() { return workerLivenessState; }
    public void setWorkerLivenessState(String value) { workerLivenessState = value; }
    public String getWorkerLaunchNonce() { return workerLaunchNonce; }
    public void setWorkerLaunchNonce(String value) { workerLaunchNonce = value; }
    public Long getWorkerGeneration() { return workerGeneration; }
    public void setWorkerGeneration(Long value) { workerGeneration = value; }
    public String getTransferActivityState() { return transferActivityState; }
    public void setTransferActivityState(String value) { transferActivityState = value; }
    public Long getTransferPayloadBytes() { return transferPayloadBytes; }
    public void setTransferPayloadBytes(Long value) { transferPayloadBytes = value; }
    public Integer getTransferProgressSchemaVersion() { return transferProgressSchemaVersion; }
    public void setTransferProgressSchemaVersion(Integer value) { transferProgressSchemaVersion = value; }
    public Long getTransferCycleSequence() { return transferCycleSequence; }
    public void setTransferCycleSequence(Long value) { transferCycleSequence = value; }
    public Long getTransferSampleSequence() { return transferSampleSequence; }
    public void setTransferSampleSequence(Long value) { transferSampleSequence = value; }
    public String getTransferPhase() { return transferPhase; }
    public void setTransferPhase(String value) { transferPhase = value; }
    public String getTransferMode() { return transferMode; }
    public void setTransferMode(String value) { transferMode = value; }
    public Long getTransferBytesTotal() { return transferBytesTotal; }
    public void setTransferBytesTotal(Long value) { transferBytesTotal = value; }
    public Long getTransferBytesProcessed() { return transferBytesProcessed; }
    public void setTransferBytesProcessed(Long value) { transferBytesProcessed = value; }
    public Long getTransferSourceReadBytes() { return transferSourceReadBytes; }
    public void setTransferSourceReadBytes(Long value) { transferSourceReadBytes = value; }
    public Long getTransferTargetWrittenBytes() { return transferTargetWrittenBytes; }
    public void setTransferTargetWrittenBytes(Long value) { transferTargetWrittenBytes = value; }
    public Long getTransferVerifiedBytes() { return transferVerifiedBytes; }
    public void setTransferVerifiedBytes(Long value) { transferVerifiedBytes = value; }
    public Double getTransferPercent() { return transferPercent; }
    public void setTransferPercent(Double value) { transferPercent = value; }
    public Long getTransferThroughputBps() { return transferThroughputBps; }
    public void setTransferThroughputBps(Long value) { transferThroughputBps = value; }
    public Long getTransferEtaSeconds() { return transferEtaSeconds; }
    public void setTransferEtaSeconds(Long value) { transferEtaSeconds = value; }
    public Integer getTransferCurrentDiskIndex() { return transferCurrentDiskIndex; }
    public void setTransferCurrentDiskIndex(Integer value) { transferCurrentDiskIndex = value; }
    public Integer getTransferDiskCount() { return transferDiskCount; }
    public void setTransferDiskCount(Integer value) { transferDiskCount = value; }
    public Boolean getTransferProgressEstimated() { return transferProgressEstimated; }
    public void setTransferProgressEstimated(Boolean value) { transferProgressEstimated = value; }
    public Long getTransferProgressSampleEpochMs() { return transferProgressSampleEpochMs; }
    public void setTransferProgressSampleEpochMs(Long value) { transferProgressSampleEpochMs = value; }
    public Boolean getTransferProgressStale() { return transferProgressStale; }
    public void setTransferProgressStale(Boolean value) { transferProgressStale = value; }
    public Integer getOwnedProcessCount() { return ownedProcessCount; }
    public void setOwnedProcessCount(Integer value) { ownedProcessCount = value; }
    public Boolean getReconciliationRequired() { return reconciliationRequired; }
    public void setReconciliationRequired(Boolean value) { reconciliationRequired = value; }
    public Boolean getRuntimeEndpointsDrained() { return runtimeEndpointsDrained; }
    public void setRuntimeEndpointsDrained(Boolean value) { runtimeEndpointsDrained = value; }
    public Boolean getTerminalAuthoritative() { return terminalAuthoritative; }
    public void setTerminalAuthoritative(Boolean value) { terminalAuthoritative = value; }

    public FtctlDrStatusAnswer(Command command, boolean success, String details) {
        super(command, success, details);
    }

    public FtctlDrStatusAnswer(Command command, boolean success, String details, String planUuid, String runUuid,
            String ftctlResult, String state, String step, Integer progress, String lastSourceCheckpointAt,
            String lastTargetDurableAt, Integer targetReadyRpoSeconds, Long eventsOffset, String errorCode,
            Integer exitCode, String output, String statusJson) {
        this(command, success, details, planUuid, runUuid, ftctlResult, state, step, progress, lastSourceCheckpointAt,
                lastTargetDurableAt, targetReadyRpoSeconds, null, null, null, null, null, eventsOffset, errorCode,
                exitCode, output, statusJson);
    }

    public FtctlDrStatusAnswer(Command command, boolean success, String details, String planUuid, String runUuid,
            String ftctlResult, String state, String step, Integer progress, String lastSourceCheckpointAt,
            String lastTargetDurableAt, Integer targetReadyRpoSeconds, Boolean targetMaterialized,
            Boolean targetVmPresent, Boolean targetStoragePresent, Boolean targetNetworkPresent,
            Boolean restorePointPresent, Long eventsOffset, String errorCode, Integer exitCode, String output,
            String statusJson) {
        super(command, success, details);
        this.planUuid = planUuid;
        this.runUuid = runUuid;
        this.ftctlResult = ftctlResult;
        this.state = state;
        this.step = step;
        this.progress = progress;
        this.lastSourceCheckpointAt = lastSourceCheckpointAt;
        this.lastTargetDurableAt = lastTargetDurableAt;
        this.targetReadyRpoSeconds = targetReadyRpoSeconds;
        this.targetMaterialized = targetMaterialized;
        this.targetVmPresent = targetVmPresent;
        this.targetStoragePresent = targetStoragePresent;
        this.targetNetworkPresent = targetNetworkPresent;
        this.restorePointPresent = restorePointPresent;
        this.eventsOffset = eventsOffset;
        this.errorCode = errorCode;
        this.exitCode = exitCode;
        this.output = output;
        this.statusJson = statusJson;
    }

    public String getPlanUuid() {
        return planUuid;
    }

    public String getRunUuid() {
        return runUuid;
    }

    public String getStatusScope() {
        return statusScope;
    }

    public void setStatusScope(String statusScope) {
        this.statusScope = statusScope;
    }

    public String getFtctlResult() {
        return ftctlResult;
    }

    public String getState() {
        return state;
    }

    public String getStep() {
        return step;
    }

    public Integer getProgress() {
        return progress;
    }

    public String getLastSourceCheckpointAt() {
        return lastSourceCheckpointAt;
    }

    public String getLastTargetDurableAt() {
        return lastTargetDurableAt;
    }

    public Integer getTargetReadyRpoSeconds() {
        return targetReadyRpoSeconds;
    }

    public Integer getTargetRpoSeconds() {
        return targetRpoSeconds;
    }

    public void setTargetRpoSeconds(Integer targetRpoSeconds) {
        this.targetRpoSeconds = targetRpoSeconds;
    }

    public String getSchedulerNextRunAt() {
        return schedulerNextRunAt;
    }

    public void setSchedulerNextRunAt(String schedulerNextRunAt) {
        this.schedulerNextRunAt = schedulerNextRunAt;
    }

    public Integer getSchedulerExecutionBudgetSeconds() {
        return schedulerExecutionBudgetSeconds;
    }

    public void setSchedulerExecutionBudgetSeconds(Integer schedulerExecutionBudgetSeconds) {
        this.schedulerExecutionBudgetSeconds = schedulerExecutionBudgetSeconds;
    }

    public Integer getSchedulerCycleWallDurationSeconds() {
        return schedulerCycleWallDurationSeconds;
    }

    public void setSchedulerCycleWallDurationSeconds(Integer schedulerCycleWallDurationSeconds) {
        this.schedulerCycleWallDurationSeconds = schedulerCycleWallDurationSeconds;
    }

    public Boolean getTargetMaterialized() {
        return targetMaterialized;
    }

    public Boolean getTargetVmPresent() {
        return targetVmPresent;
    }

    public Boolean getTargetStoragePresent() {
        return targetStoragePresent;
    }

    public Boolean getTargetNetworkPresent() {
        return targetNetworkPresent;
    }

    public Boolean getRestorePointPresent() {
        return restorePointPresent;
    }

    public Long getEventsOffset() {
        return eventsOffset;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getFailedComponent() {
        return failedComponent;
    }

    public void setFailedComponent(String failedComponent) {
        this.failedComponent = failedComponent;
    }

    public String getDataCommitState() {
        return dataCommitState;
    }

    public void setDataCommitState(String dataCommitState) {
        this.dataCommitState = dataCommitState;
    }

    public Boolean getDataCopied() {
        return dataCopied;
    }

    public void setDataCopied(Boolean dataCopied) {
        this.dataCopied = dataCopied;
    }

    public Boolean getMetadataCommitted() {
        return metadataCommitted;
    }

    public void setMetadataCommitted(Boolean metadataCommitted) {
        this.metadataCommitted = metadataCommitted;
    }

    public Boolean getTargetDurable() {
        return targetDurable;
    }

    public void setTargetDurable(Boolean targetDurable) {
        this.targetDurable = targetDurable;
    }

    public String getCycleRetryMode() {
        return cycleRetryMode;
    }

    public void setCycleRetryMode(String cycleRetryMode) {
        this.cycleRetryMode = cycleRetryMode;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getOutput() {
        return output;
    }

    public String getStatusJson() {
        return statusJson;
    }

    public String getSourceDiskMapPath() {
        return sourceDiskMapPath;
    }

    public void setSourceDiskMapPath(String sourceDiskMapPath) {
        this.sourceDiskMapPath = sourceDiskMapPath;
    }

    public String getTargetDiskMapPath() {
        return targetDiskMapPath;
    }

    public void setTargetDiskMapPath(String targetDiskMapPath) {
        this.targetDiskMapPath = targetDiskMapPath;
    }

    public String getDiskMapRole() {
        return diskMapRole;
    }

    public void setDiskMapRole(String diskMapRole) {
        this.diskMapRole = diskMapRole;
    }

    public Integer getTargetDiskCount() {
        return targetDiskCount;
    }

    public void setTargetDiskCount(Integer targetDiskCount) {
        this.targetDiskCount = targetDiskCount;
    }

    public Integer getTargetDiskInvalidCount() {
        return targetDiskInvalidCount;
    }

    public void setTargetDiskInvalidCount(Integer targetDiskInvalidCount) {
        this.targetDiskInvalidCount = targetDiskInvalidCount;
    }

    public String getWorkerState() {
        return workerState;
    }

    public void setWorkerState(String workerState) {
        this.workerState = workerState;
    }

    public Integer getWorkerPid() {
        return workerPid;
    }

    public void setWorkerPid(Integer workerPid) {
        this.workerPid = workerPid;
    }

    public Long getWorkerStartTicks() { return workerStartTicks; }
    public void setWorkerStartTicks(Long value) { workerStartTicks = value; }
    public Boolean getWorkerPidAlive() { return workerPidAlive; }
    public void setWorkerPidAlive(Boolean value) { workerPidAlive = value; }

    public String getWorkerStartedAt() {
        return workerStartedAt;
    }

    public void setWorkerStartedAt(String workerStartedAt) {
        this.workerStartedAt = workerStartedAt;
    }

    public String getWorkerUpdatedAt() {
        return workerUpdatedAt;
    }

    public void setWorkerUpdatedAt(String workerUpdatedAt) {
        this.workerUpdatedAt = workerUpdatedAt;
    }

    public Integer getWorkerExitCode() {
        return workerExitCode;
    }

    public void setWorkerExitCode(Integer workerExitCode) {
        this.workerExitCode = workerExitCode;
    }

    public Integer getDriverExitCode() { return driverExitCode; }
    public void setDriverExitCode(Integer value) { driverExitCode = value; }
    public String getFailurePhase() { return failurePhase; }
    public void setFailurePhase(String value) { failurePhase = value; }
    public String getTerminalSource() { return terminalSource; }
    public void setTerminalSource(String value) { terminalSource = value; }
    public Integer getTerminalVersion() { return terminalVersion; }
    public void setTerminalVersion(Integer value) { terminalVersion = value; }
    public Boolean getTerminalPublicationPending() { return terminalPublicationPending; }
    public void setTerminalPublicationPending(Boolean value) { terminalPublicationPending = value; }
    public String getTerminalPublicationPendingSince() { return terminalPublicationPendingSince; }
    public void setTerminalPublicationPendingSince(String value) { terminalPublicationPendingSince = value; }
    public String getBaselineFileState() { return baselineFileState; }
    public void setBaselineFileState(String value) { baselineFileState = value; }
    public String getSourceDiskProbeState() { return sourceDiskProbeState; }
    public void setSourceDiskProbeState(String value) { sourceDiskProbeState = value; }
    public Integer getSourceDiskCount() { return sourceDiskCount; }
    public void setSourceDiskCount(Integer value) { sourceDiskCount = value; }
    public String getTargetWriterProbeState() { return targetWriterProbeState; }
    public void setTargetWriterProbeState(String value) { targetWriterProbeState = value; }
    public String getOperationIntent() { return operationIntent; }
    public void setOperationIntent(String value) { operationIntent = value; }
    public String getRequestedMode() { return requestedMode; }
    public void setRequestedMode(String value) { requestedMode = value; }
    public String getEffectiveMode() { return effectiveMode; }
    public void setEffectiveMode(String value) { effectiveMode = value; }
    public String getModeDecisionCode() { return modeDecisionCode; }
    public void setModeDecisionCode(String value) { modeDecisionCode = value; }
    public Boolean getInitialSeedRequired() { return initialSeedRequired; }
    public void setInitialSeedRequired(Boolean value) { initialSeedRequired = value; }
    public Long getEstimatedVirtualBytes() { return estimatedVirtualBytes; }
    public void setEstimatedVirtualBytes(Long value) { estimatedVirtualBytes = value; }

    public Boolean getRetryable() {
        return retryable;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    public Boolean getTransitionReady() { return transitionReady; }
    public void setTransitionReady(Boolean value) { transitionReady = value; }
    public Integer getTransitionSchemaVersion() { return transitionSchemaVersion; }
    public void setTransitionSchemaVersion(Integer value) { transitionSchemaVersion = value; }
    public String getTransitionContractVersion() { return transitionContractVersion; }
    public void setTransitionContractVersion(String value) { transitionContractVersion = value; }
    public String getTransitionOperation() { return transitionOperation; }
    public void setTransitionOperation(String value) { transitionOperation = value; }
    public String getTransitionExpectedAuthority() { return transitionExpectedAuthority; }
    public void setTransitionExpectedAuthority(String value) { transitionExpectedAuthority = value; }
    public String getTransitionActiveSide() { return transitionActiveSide; }
    public void setTransitionActiveSide(String value) { transitionActiveSide = value; }
    public Long getTransitionExpectedGeneration() { return transitionExpectedGeneration; }
    public void setTransitionExpectedGeneration(Long value) { transitionExpectedGeneration = value; }
    public Long getTransitionAuthorityGeneration() { return transitionAuthorityGeneration; }
    public void setTransitionAuthorityGeneration(Long value) { transitionAuthorityGeneration = value; }
    public String getTransitionTargetPowerState() { return transitionTargetPowerState; }
    public void setTransitionTargetPowerState(String value) { transitionTargetPowerState = value; }
    public String getTransitionSourceFenceState() { return transitionSourceFenceState; }
    public void setTransitionSourceFenceState(String value) { transitionSourceFenceState = value; }
    public String getTransitionSourcePowerState() { return transitionSourcePowerState; }
    public void setTransitionSourcePowerState(String value) { transitionSourcePowerState = value; }
    public Long getTransitionCheckedAtEpochMs() { return transitionCheckedAtEpochMs; }
    public void setTransitionCheckedAtEpochMs(Long value) { transitionCheckedAtEpochMs = value; }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public void setRetryAfterSeconds(Integer retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getCurrentCheckpointSequence() {
        return currentCheckpointSequence;
    }

    public void setCurrentCheckpointSequence(Long currentCheckpointSequence) {
        this.currentCheckpointSequence = currentCheckpointSequence;
    }

    public String getCurrentCheckpointCycleType() {
        return currentCheckpointCycleType;
    }

    public void setCurrentCheckpointCycleType(String currentCheckpointCycleType) {
        this.currentCheckpointCycleType = currentCheckpointCycleType;
    }

    public String getCurrentCheckpointRequestedMode() { return currentCheckpointRequestedMode; }
    public void setCurrentCheckpointRequestedMode(String value) { currentCheckpointRequestedMode = value; }
    public String getCurrentCheckpointEffectiveMode() { return currentCheckpointEffectiveMode; }
    public void setCurrentCheckpointEffectiveMode(String value) { currentCheckpointEffectiveMode = value; }
    public String getCurrentCheckpointModeDecisionCode() { return currentCheckpointModeDecisionCode; }
    public void setCurrentCheckpointModeDecisionCode(String value) { currentCheckpointModeDecisionCode = value; }
    public Boolean getCurrentCheckpointAutomaticReseed() { return currentCheckpointAutomaticReseed; }
    public void setCurrentCheckpointAutomaticReseed(Boolean value) { currentCheckpointAutomaticReseed = value; }
    public Integer getCurrentCheckpointInvalidBaselineDiskCount() { return currentCheckpointInvalidBaselineDiskCount; }
    public void setCurrentCheckpointInvalidBaselineDiskCount(Integer value) { currentCheckpointInvalidBaselineDiskCount = value; }

    public String getCurrentCheckpointRef() {
        return currentCheckpointRef;
    }

    public void setCurrentCheckpointRef(String currentCheckpointRef) {
        this.currentCheckpointRef = currentCheckpointRef;
    }

    public String getCurrentCheckpointState() {
        return currentCheckpointState;
    }

    public void setCurrentCheckpointState(String currentCheckpointState) {
        this.currentCheckpointState = currentCheckpointState;
    }

    public Long getLatestCompletedCheckpointSequence() {
        return latestCompletedCheckpointSequence;
    }

    public void setLatestCompletedCheckpointSequence(Long latestCompletedCheckpointSequence) {
        this.latestCompletedCheckpointSequence = latestCompletedCheckpointSequence;
    }

    public Long getLatestCompletedCycleSequence() {
        return latestCompletedCycleSequence;
    }

    public void setLatestCompletedCycleSequence(Long latestCompletedCycleSequence) {
        this.latestCompletedCycleSequence = latestCompletedCycleSequence;
    }

    public String getLatestCompletedCheckpointCycleType() {
        return latestCompletedCheckpointCycleType;
    }

    public void setLatestCompletedCheckpointCycleType(String latestCompletedCheckpointCycleType) {
        this.latestCompletedCheckpointCycleType = latestCompletedCheckpointCycleType;
    }

    public String getLatestCompletedCheckpointRef() {
        return latestCompletedCheckpointRef;
    }

    public void setLatestCompletedCheckpointRef(String latestCompletedCheckpointRef) {
        this.latestCompletedCheckpointRef = latestCompletedCheckpointRef;
    }

    public String getLatestCompletedCheckpointState() {
        return latestCompletedCheckpointState;
    }

    public void setLatestCompletedCheckpointState(String latestCompletedCheckpointState) {
        this.latestCompletedCheckpointState = latestCompletedCheckpointState;
    }

    public String getLatestCompletedProducerRunUuid() {
        return latestCompletedProducerRunUuid;
    }

    public void setLatestCompletedProducerRunUuid(String latestCompletedProducerRunUuid) {
        this.latestCompletedProducerRunUuid = latestCompletedProducerRunUuid;
    }

    public String getLatestCompletedSourceCheckpointAt() {
        return latestCompletedSourceCheckpointAt;
    }

    public void setLatestCompletedSourceCheckpointAt(String latestCompletedSourceCheckpointAt) {
        this.latestCompletedSourceCheckpointAt = latestCompletedSourceCheckpointAt;
    }

    public String getLatestCompletedTargetDurableAt() {
        return latestCompletedTargetDurableAt;
    }

    public void setLatestCompletedTargetDurableAt(String latestCompletedTargetDurableAt) {
        this.latestCompletedTargetDurableAt = latestCompletedTargetDurableAt;
    }

    public Integer getLatestCompletedTargetReadyRpoSeconds() {
        return latestCompletedTargetReadyRpoSeconds;
    }

    public void setLatestCompletedTargetReadyRpoSeconds(Integer latestCompletedTargetReadyRpoSeconds) {
        this.latestCompletedTargetReadyRpoSeconds = latestCompletedTargetReadyRpoSeconds;
    }

    public String getLatestCompletedManifestPath() {
        return latestCompletedManifestPath;
    }

    public void setLatestCompletedManifestPath(String latestCompletedManifestPath) {
        this.latestCompletedManifestPath = latestCompletedManifestPath;
    }

    public String getLatestCompletedCheckpointPath() {
        return latestCompletedCheckpointPath;
    }

    public void setLatestCompletedCheckpointPath(String latestCompletedCheckpointPath) {
        this.latestCompletedCheckpointPath = latestCompletedCheckpointPath;
    }

    public String getLatestCompletedRequestedMode() { return latestCompletedRequestedMode; }
    public void setLatestCompletedRequestedMode(String value) { latestCompletedRequestedMode = value; }

    public String getLatestCompletedEffectiveMode() {
        return latestCompletedEffectiveMode;
    }

    public void setLatestCompletedEffectiveMode(String latestCompletedEffectiveMode) {
        this.latestCompletedEffectiveMode = latestCompletedEffectiveMode;
    }

    public String getLatestCompletedModeDecisionCode() { return latestCompletedModeDecisionCode; }
    public void setLatestCompletedModeDecisionCode(String value) { latestCompletedModeDecisionCode = value; }
    public String getLatestCompletedReseedReason() { return latestCompletedReseedReason; }
    public void setLatestCompletedReseedReason(String value) { latestCompletedReseedReason = value; }
    public Boolean getLatestCompletedAutomaticReseed() { return latestCompletedAutomaticReseed; }
    public void setLatestCompletedAutomaticReseed(Boolean value) { latestCompletedAutomaticReseed = value; }
    public Integer getLatestCompletedInvalidBaselineDiskCount() { return latestCompletedInvalidBaselineDiskCount; }
    public void setLatestCompletedInvalidBaselineDiskCount(Integer value) { latestCompletedInvalidBaselineDiskCount = value; }

    public Boolean getLatestCompletedIncrementalVerified() {
        return latestCompletedIncrementalVerified;
    }

    public void setLatestCompletedIncrementalVerified(Boolean latestCompletedIncrementalVerified) {
        this.latestCompletedIncrementalVerified = latestCompletedIncrementalVerified;
    }

    public Boolean getLatestCompletedMetricsEstimated() {
        return latestCompletedMetricsEstimated;
    }

    public void setLatestCompletedMetricsEstimated(Boolean latestCompletedMetricsEstimated) {
        this.latestCompletedMetricsEstimated = latestCompletedMetricsEstimated;
    }

    public Long getLatestCompletedVirtualBytes() {
        return latestCompletedVirtualBytes;
    }

    public void setLatestCompletedVirtualBytes(Long latestCompletedVirtualBytes) {
        this.latestCompletedVirtualBytes = latestCompletedVirtualBytes;
    }

    public Long getLatestCompletedChangedBytes() {
        return latestCompletedChangedBytes;
    }

    public void setLatestCompletedChangedBytes(Long latestCompletedChangedBytes) {
        this.latestCompletedChangedBytes = latestCompletedChangedBytes;
    }

    public Long getLatestCompletedSourceReadBytes() {
        return latestCompletedSourceReadBytes;
    }

    public void setLatestCompletedSourceReadBytes(Long latestCompletedSourceReadBytes) {
        this.latestCompletedSourceReadBytes = latestCompletedSourceReadBytes;
    }

    public Long getLatestCompletedTargetWrittenBytes() {
        return latestCompletedTargetWrittenBytes;
    }

    public void setLatestCompletedTargetWrittenBytes(Long latestCompletedTargetWrittenBytes) {
        this.latestCompletedTargetWrittenBytes = latestCompletedTargetWrittenBytes;
    }

    public Long getLatestCompletedTransferPayloadBytes() {
        return latestCompletedTransferPayloadBytes;
    }

    public void setLatestCompletedTransferPayloadBytes(Long latestCompletedTransferPayloadBytes) {
        this.latestCompletedTransferPayloadBytes = latestCompletedTransferPayloadBytes;
    }

    public Long getLatestCompletedChangedExtentCount() {
        return latestCompletedChangedExtentCount;
    }

    public void setLatestCompletedChangedExtentCount(Long latestCompletedChangedExtentCount) {
        this.latestCompletedChangedExtentCount = latestCompletedChangedExtentCount;
    }

    public Long getLatestCompletedDurationMs() {
        return latestCompletedDurationMs;
    }

    public void setLatestCompletedDurationMs(Long latestCompletedDurationMs) {
        this.latestCompletedDurationMs = latestCompletedDurationMs;
    }

    public Long getLatestCompletedThroughputBps() {
        return latestCompletedThroughputBps;
    }

    public void setLatestCompletedThroughputBps(Long latestCompletedThroughputBps) {
        this.latestCompletedThroughputBps = latestCompletedThroughputBps;
    }

    public Long getLatestCompletedBaselineGeneration() {
        return latestCompletedBaselineGeneration;
    }

    public void setLatestCompletedBaselineGeneration(Long latestCompletedBaselineGeneration) {
        this.latestCompletedBaselineGeneration = latestCompletedBaselineGeneration;
    }

    public String getLatestCompletedCycleToken() {
        return latestCompletedCycleToken;
    }

    public void setLatestCompletedCycleToken(String latestCompletedCycleToken) {
        this.latestCompletedCycleToken = latestCompletedCycleToken;
    }

    public String getLatestCompletedNbdTeardownState() { return latestCompletedNbdTeardownState; }
    public void setLatestCompletedNbdTeardownState(String value) { latestCompletedNbdTeardownState = value; }
    public Long getLatestCompletedNbdTeardownStartedAtEpochMs() { return latestCompletedNbdTeardownStartedAtEpochMs; }
    public void setLatestCompletedNbdTeardownStartedAtEpochMs(Long value) { latestCompletedNbdTeardownStartedAtEpochMs = value; }
    public Long getLatestCompletedNbdTeardownCompletedAtEpochMs() { return latestCompletedNbdTeardownCompletedAtEpochMs; }
    public void setLatestCompletedNbdTeardownCompletedAtEpochMs(Long value) { latestCompletedNbdTeardownCompletedAtEpochMs = value; }
    public Long getLatestCompletedNbdTeardownDurationMs() { return latestCompletedNbdTeardownDurationMs; }
    public void setLatestCompletedNbdTeardownDurationMs(Long value) { latestCompletedNbdTeardownDurationMs = value; }
    public Integer getLatestCompletedNbdSourceDeviceCount() { return latestCompletedNbdSourceDeviceCount; }
    public void setLatestCompletedNbdSourceDeviceCount(Integer value) { latestCompletedNbdSourceDeviceCount = value; }
    public Integer getLatestCompletedNbdTargetDeviceCount() { return latestCompletedNbdTargetDeviceCount; }
    public void setLatestCompletedNbdTargetDeviceCount(Integer value) { latestCompletedNbdTargetDeviceCount = value; }
    public Integer getLatestCompletedNbdQuarantinedDeviceCount() { return latestCompletedNbdQuarantinedDeviceCount; }
    public void setLatestCompletedNbdQuarantinedDeviceCount(Integer value) { latestCompletedNbdQuarantinedDeviceCount = value; }
    public String getLatestCompletedNbdTeardownErrorCode() { return latestCompletedNbdTeardownErrorCode; }
    public void setLatestCompletedNbdTeardownErrorCode(String value) { latestCompletedNbdTeardownErrorCode = value; }
    public String getLatestCompletedNbdTeardownErrorMessage() { return latestCompletedNbdTeardownErrorMessage; }
    public void setLatestCompletedNbdTeardownErrorMessage(String value) { latestCompletedNbdTeardownErrorMessage = value; }

    public Integer getCycleContractVersion() { return cycleContractVersion; }
    public void setCycleContractVersion(Integer value) { cycleContractVersion = value; }
    public String getCycleEvidenceState() { return cycleEvidenceState; }
    public void setCycleEvidenceState(String value) { cycleEvidenceState = value; }
    public String getCycleEvidenceCode() { return cycleEvidenceCode; }
    public void setCycleEvidenceCode(String value) { cycleEvidenceCode = value; }
    public String getCycleEvidenceMessage() { return cycleEvidenceMessage; }
    public void setCycleEvidenceMessage(String value) { cycleEvidenceMessage = value; }
    public FtctlDrCycleSnapshot getCurrentCycle() { return currentCycle; }
    public void setCurrentCycle(FtctlDrCycleSnapshot value) { currentCycle = value; }
    public FtctlDrCycleSnapshot getLatestCompletedCycle() { return latestCompletedCycle; }
    public void setLatestCompletedCycle(FtctlDrCycleSnapshot value) { latestCompletedCycle = value; }

    public Integer getControlProtocolVersion() {
        return controlProtocolVersion;
    }

    public void setControlProtocolVersion(Integer controlProtocolVersion) {
        this.controlProtocolVersion = controlProtocolVersion;
    }

    public Long getControlGeneration() {
        return controlGeneration;
    }

    public void setControlGeneration(Long controlGeneration) {
        this.controlGeneration = controlGeneration;
    }

    public Long getControlAckGeneration() {
        return controlAckGeneration;
    }

    public void setControlAckGeneration(Long controlAckGeneration) {
        this.controlAckGeneration = controlAckGeneration;
    }

    public String getControlState() {
        return controlState;
    }

    public void setControlState(String controlState) {
        this.controlState = controlState;
    }

    public String getCycleState() {
        return cycleState;
    }

    public void setCycleState(String cycleState) {
        this.cycleState = cycleState;
    }

    public String getTransitionState() {
        return transitionState;
    }

    public void setTransitionState(String transitionState) {
        this.transitionState = transitionState;
    }

    public String getCheckpointLeaseState() {
        return checkpointLeaseState;
    }

    public void setCheckpointLeaseState(String checkpointLeaseState) {
        this.checkpointLeaseState = checkpointLeaseState;
    }

    public String getGuestPreparationState() {
        return guestPreparationState;
    }

    public void setGuestPreparationState(String guestPreparationState) {
        this.guestPreparationState = guestPreparationState;
    }

    public String getGuestFamily() {
        return guestFamily;
    }

    public void setGuestFamily(String guestFamily) {
        this.guestFamily = guestFamily;
    }

    public String getTestSessionState() {
        return testSessionState;
    }

    public void setTestSessionState(String testSessionState) {
        this.testSessionState = testSessionState;
    }

    public String getTestArtifactsState() {
        return testArtifactsState;
    }

    public void setTestArtifactsState(String testArtifactsState) {
        this.testArtifactsState = testArtifactsState;
    }

    public Integer getTestArtifactCount() {
        return testArtifactCount;
    }

    public void setTestArtifactCount(Integer testArtifactCount) {
        this.testArtifactCount = testArtifactCount;
    }

    public String getTestCleanupState() {
        return testCleanupState;
    }

    public void setTestCleanupState(String testCleanupState) {
        this.testCleanupState = testCleanupState;
    }

    public Boolean getCleanupRequired() {
        return cleanupRequired;
    }

    public void setCleanupRequired(Boolean cleanupRequired) {
        this.cleanupRequired = cleanupRequired;
    }

    public String getGuestPreparationManifestPath() {
        return guestPreparationManifestPath;
    }

    public void setGuestPreparationManifestPath(String guestPreparationManifestPath) {
        this.guestPreparationManifestPath = guestPreparationManifestPath;
    }

    public String getManifestSchemaVersion() {
        return manifestSchemaVersion;
    }

    public void setManifestSchemaVersion(String manifestSchemaVersion) {
        this.manifestSchemaVersion = manifestSchemaVersion;
    }

    public String getManifestSha256() {
        return manifestSha256;
    }

    public void setManifestSha256(String manifestSha256) {
        this.manifestSha256 = manifestSha256;
    }

    public Long getGuestPreparationCheckpointSequence() {
        return guestPreparationCheckpointSequence;
    }

    public void setGuestPreparationCheckpointSequence(Long guestPreparationCheckpointSequence) {
        this.guestPreparationCheckpointSequence = guestPreparationCheckpointSequence;
    }

    public String getTestDomainName() {
        return testDomainName;
    }

    public void setTestDomainName(String testDomainName) {
        this.testDomainName = testDomainName;
    }

    public String getTestDomainState() {
        return testDomainState;
    }

    public void setTestDomainState(String testDomainState) {
        this.testDomainState = testDomainState;
    }

    public String getTestBootValidationMode() {
        return testBootValidationMode;
    }

    public void setTestBootValidationMode(String testBootValidationMode) {
        this.testBootValidationMode = testBootValidationMode;
    }

    public Long getRuntimeGeneration() {
        return runtimeGeneration;
    }

    public void setRuntimeGeneration(Long runtimeGeneration) {
        this.runtimeGeneration = runtimeGeneration;
    }

    public Boolean getSchedulerPidAlive() {
        return schedulerPidAlive;
    }

    public void setSchedulerPidAlive(Boolean schedulerPidAlive) {
        this.schedulerPidAlive = schedulerPidAlive;
    }

    public String getSchedulerDesiredState() { return schedulerDesiredState; }
    public void setSchedulerDesiredState(String value) { schedulerDesiredState = value; }
    public String getSchedulerServiceUnit() { return schedulerServiceUnit; }
    public void setSchedulerServiceUnit(String value) { schedulerServiceUnit = value; }
    public String getSchedulerUnitActiveState() { return schedulerUnitActiveState; }
    public void setSchedulerUnitActiveState(String value) { schedulerUnitActiveState = value; }
    public String getSchedulerUnitSubState() { return schedulerUnitSubState; }
    public void setSchedulerUnitSubState(String value) { schedulerUnitSubState = value; }
    public Long getSchedulerUnitMainPid() { return schedulerUnitMainPid; }
    public void setSchedulerUnitMainPid(Long value) { schedulerUnitMainPid = value; }
    public String getSchedulerCgroup() { return schedulerCgroup; }
    public void setSchedulerCgroup(String value) { schedulerCgroup = value; }
    public String getSchedulerRecoveryState() { return schedulerRecoveryState; }
    public void setSchedulerRecoveryState(String value) { schedulerRecoveryState = value; }
    public String getSchedulerRecoveryTrigger() { return schedulerRecoveryTrigger; }
    public void setSchedulerRecoveryTrigger(String value) { schedulerRecoveryTrigger = value; }
    public String getSchedulerRecoveredAt() { return schedulerRecoveredAt; }
    public void setSchedulerRecoveredAt(String value) { schedulerRecoveredAt = value; }
    public String getNbdTeardownState() { return nbdTeardownState; }
    public void setNbdTeardownState(String value) { nbdTeardownState = value; }
    public Integer getNbdQuarantinedDeviceCount() { return nbdQuarantinedDeviceCount; }
    public void setNbdQuarantinedDeviceCount(Integer value) { nbdQuarantinedDeviceCount = value; }
    public String getNbdTeardownErrorCode() { return nbdTeardownErrorCode; }
    public void setNbdTeardownErrorCode(String value) { nbdTeardownErrorCode = value; }
    public String getNbdTeardownErrorMessage() { return nbdTeardownErrorMessage; }
    public void setNbdTeardownErrorMessage(String value) { nbdTeardownErrorMessage = value; }

    public String getSchedulerSessionUuid() { return schedulerSessionUuid; }
    public void setSchedulerSessionUuid(String value) { schedulerSessionUuid = value; }
    public Long getSchedulerLeaseEpoch() { return schedulerLeaseEpoch; }
    public void setSchedulerLeaseEpoch(Long value) { schedulerLeaseEpoch = value; }
    public Long getAuthoritySequence() { return authoritySequence; }
    public void setAuthoritySequence(Long value) { authoritySequence = value; }
    public Long getPlanCycleSequence() { return planCycleSequence; }
    public void setPlanCycleSequence(Long value) { planCycleSequence = value; }
    public Long getResumeBaselineCheckpointSequence() { return resumeBaselineCheckpointSequence; }
    public void setResumeBaselineCheckpointSequence(Long value) { resumeBaselineCheckpointSequence = value; }
    public Long getMinimumCompletedCheckpointSequence() { return minimumCompletedCheckpointSequence; }
    public void setMinimumCompletedCheckpointSequence(Long value) { minimumCompletedCheckpointSequence = value; }
    public Boolean getImmediateCyclePending() { return immediateCyclePending; }
    public void setImmediateCyclePending(Boolean value) { immediateCyclePending = value; }
    public String getImmediateCycleOwnerRun() { return immediateCycleOwnerRun; }
    public void setImmediateCycleOwnerRun(String value) { immediateCycleOwnerRun = value; }
    public String getSchedulerHealth() { return schedulerHealth; }
    public void setSchedulerHealth(String value) { schedulerHealth = value; }
    public String getReplicationActivity() { return replicationActivity; }
    public void setReplicationActivity(String value) { replicationActivity = value; }
    public String getProtectionState() { return protectionState; }
    public void setProtectionState(String value) { protectionState = value; }
    public String getActiveWorkerRunUuid() { return activeWorkerRunUuid; }
    public void setActiveWorkerRunUuid(String value) { activeWorkerRunUuid = value; }
    public Long getActiveWorkerPid() { return activeWorkerPid; }
    public void setActiveWorkerPid(Long value) { activeWorkerPid = value; }
    public Long getActiveWorkerStartTicks() { return activeWorkerStartTicks; }
    public void setActiveWorkerStartTicks(Long value) { activeWorkerStartTicks = value; }
    public String getWorkerHeartbeatAt() { return workerHeartbeatAt; }
    public void setWorkerHeartbeatAt(String value) { workerHeartbeatAt = value; }
    public String getControlRequestRunUuid() { return controlRequestRunUuid; }
    public void setControlRequestRunUuid(String value) { controlRequestRunUuid = value; }
    public Boolean getOwnerMatched() { return ownerMatched; }
    public void setOwnerMatched(Boolean value) { ownerMatched = value; }

    public String getBaselineState() {
        return baselineState;
    }

    public void setBaselineState(String baselineState) {
        this.baselineState = baselineState;
    }

    public String getReseedReason() {
        return reseedReason;
    }

    public void setReseedReason(String reseedReason) {
        this.reseedReason = reseedReason;
    }

    public Integer getConsecutiveAutomaticReseedCount() { return consecutiveAutomaticReseedCount; }
    public void setConsecutiveAutomaticReseedCount(Integer value) { consecutiveAutomaticReseedCount = value; }
}
