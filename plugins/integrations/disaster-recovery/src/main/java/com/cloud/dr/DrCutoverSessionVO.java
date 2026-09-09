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
@Table(name = "dr_cutover_session")
public class DrCutoverSessionVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String uuid = UUID.randomUUID().toString();
    @Column(name = "plan_id") private long planId;
    @Column(name = "run_id") private long runId;
    private String mode;
    @Column(name = "checkpoint_sequence") private Long checkpointSequence;
    private String state;
    @Column(name = "guest_os_family") private String guestOsFamily;
    @Column(name = "guest_preparation_state") private String guestPreparationState;
    @Column(name = "virtio_state") private String virtioState;
    @Column(name = "secure_boot_state") private String secureBootState;
    @Column(name = "domain_name") private String domainName;
    @Column(name = "boot_validation_state") private String bootValidationState;
    @Column(name = "source_fence_state") private String sourceFenceState;
    @Column(name = "source_power_state") private String sourcePowerState;
    @Column(name = "manifest_schema_version") private String manifestSchemaVersion;
    @Column(name = "manifest_sha256") private String manifestSha256;
    @Column(name = "target_disk_count") private Integer targetDiskCount;
    @Column(name = "scheduler_recovery_state") private String schedulerRecoveryState;
    @Column(name = "cloud_promotion_state") private String cloudPromotionState;
    @Column(name = "target_power_state") private String targetPowerState;
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "target_power_on_at") private Date targetPowerOnAt;
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "boot_validated_at") private Date bootValidatedAt;
    @Column(name = "engine_ack_state") private String engineAckState;
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "engine_ack_at") private Date engineAckAt;
    @Column(name = "cloud_authority_generation") private Long cloudAuthorityGeneration;
    @Column(name = "commit_contract_version") private String commitContractVersion;
    @Column(name = "engine_session_id") private String engineSessionId;
    @Column(name = "commit_attempt_id") private String commitAttemptId;
    @Column(name = "commit_envelope_sha256") private String commitEnvelopeSha256;
    @Column(name = "commit_state") private String commitState;
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "completed_at") private Date completedAt;
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "authority_ended_at") private Date authorityEndedAt;
    @Column(name = "authority_ended_by_run_id") private Long authorityEndedByRunId;
    @Column(name = "cleanup_required") private boolean cleanupRequired;
    @Column(name = "details_json", length = 16777215) private String detailsJson;
    @Column(name = "error_code") private String errorCode;
    @Column(name = "error_message") private String errorMessage;
    @Temporal(TemporalType.TIMESTAMP) private Date created = new Date();
    @Temporal(TemporalType.TIMESTAMP) private Date updated = new Date();
    @Temporal(TemporalType.TIMESTAMP) private Date removed;

    protected DrCutoverSessionVO() { }
    public DrCutoverSessionVO(long planId, long runId, String mode, String state) {
        this.planId = planId; this.runId = runId; this.mode = mode; this.state = state;
    }
    @Override public long getId() { return id; }
    public String getUuid() { return uuid; }
    public long getPlanId() { return planId; }
    public long getRunId() { return runId; }
    public String getMode() { return mode; }
    public Long getCheckpointSequence() { return checkpointSequence; }
    public String getState() { return state; }
    public String getGuestOsFamily() { return guestOsFamily; }
    public String getGuestPreparationState() { return guestPreparationState; }
    public String getVirtioState() { return virtioState; }
    public String getSecureBootState() { return secureBootState; }
    public String getDomainName() { return domainName; }
    public String getBootValidationState() { return bootValidationState; }
    public String getSourceFenceState() { return sourceFenceState; }
    public String getSourcePowerState() { return sourcePowerState; }
    public String getManifestSchemaVersion() { return manifestSchemaVersion; }
    public String getManifestSha256() { return manifestSha256; }
    public Integer getTargetDiskCount() { return targetDiskCount; }
    public String getSchedulerRecoveryState() { return schedulerRecoveryState; }
    public String getCloudPromotionState() { return cloudPromotionState; }
    public String getTargetPowerState() { return targetPowerState; }
    public Date getTargetPowerOnAt() { return targetPowerOnAt; }
    public Date getBootValidatedAt() { return bootValidatedAt; }
    public String getEngineAckState() { return engineAckState; }
    public Date getEngineAckAt() { return engineAckAt; }
    public Long getCloudAuthorityGeneration() { return cloudAuthorityGeneration; }
    public String getCommitContractVersion() { return commitContractVersion; }
    public String getEngineSessionId() { return engineSessionId; }
    public String getCommitAttemptId() { return commitAttemptId; }
    public String getCommitEnvelopeSha256() { return commitEnvelopeSha256; }
    public String getCommitState() { return commitState; }
    public Date getCompletedAt() { return completedAt; }
    public Date getAuthorityEndedAt() { return authorityEndedAt; }
    public Long getAuthorityEndedByRunId() { return authorityEndedByRunId; }
    public boolean isCleanupRequired() { return cleanupRequired; }
    public String getDetailsJson() { return detailsJson; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Date getCreated() { return created; }
    public Date getUpdated() { return updated; }
    public Date getRemoved() { return removed; }
    public void setCheckpointSequence(Long value) { checkpointSequence = value; }
    public void setState(String value) { state = value; }
    public void setGuestOsFamily(String value) { guestOsFamily = value; }
    public void setGuestPreparationState(String value) { guestPreparationState = value; }
    public void setVirtioState(String value) { virtioState = value; }
    public void setSecureBootState(String value) { secureBootState = value; }
    public void setDomainName(String value) { domainName = value; }
    public void setBootValidationState(String value) { bootValidationState = value; }
    public void setSourceFenceState(String value) { sourceFenceState = value; }
    public void setSourcePowerState(String value) { sourcePowerState = value; }
    public void setManifestSchemaVersion(String value) { manifestSchemaVersion = value; }
    public void setManifestSha256(String value) { manifestSha256 = value; }
    public void setTargetDiskCount(Integer value) { targetDiskCount = value; }
    public void setSchedulerRecoveryState(String value) { schedulerRecoveryState = value; }
    public void setCloudPromotionState(String value) { cloudPromotionState = value; }
    public void setTargetPowerState(String value) { targetPowerState = value; }
    public void setTargetPowerOnAt(Date value) { targetPowerOnAt = value; }
    public void setBootValidatedAt(Date value) { bootValidatedAt = value; }
    public void setEngineAckState(String value) { engineAckState = value; }
    public void setEngineAckAt(Date value) { engineAckAt = value; }
    public void setCloudAuthorityGeneration(Long value) { cloudAuthorityGeneration = value; }
    public void setCommitContractVersion(String value) { commitContractVersion = value; }
    public void setEngineSessionId(String value) { engineSessionId = value; }
    public void setCommitAttemptId(String value) { commitAttemptId = value; }
    public void setCommitEnvelopeSha256(String value) { commitEnvelopeSha256 = value; }
    public void setCommitState(String value) { commitState = value; }
    public void setCompletedAt(Date value) { completedAt = value; }
    public void setAuthorityEndedAt(Date value) { authorityEndedAt = value; }
    public void setAuthorityEndedByRunId(Long value) { authorityEndedByRunId = value; }
    public void setCleanupRequired(boolean value) { cleanupRequired = value; }
    public void setDetailsJson(String value) { detailsJson = value; }
    public void setErrorCode(String value) { errorCode = value; }
    public void setErrorMessage(String value) { errorMessage = value; }
    public void markUpdated() { updated = new Date(); }
    public void setRemoved(Date value) { removed = value; }
}
