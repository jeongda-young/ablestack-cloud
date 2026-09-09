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
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dr.DrPlanVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DrPlanVO.class)
public class DrPlanResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the DR plan ID")
    private String id;

    @SerializedName("name")
    @Param(description = "the DR plan name")
    private String name;

    @SerializedName("description")
    @Param(description = "the DR plan description")
    private String description;

    @SerializedName("sourcesiteid")
    @Param(description = "the source site ID")
    private String sourceSiteId;

    @SerializedName("targetsiteid")
    @Param(description = "the target site ID")
    private String targetSiteId;

    @SerializedName("sourcevmid")
    @Param(description = "the source virtual machine ID")
    private Long sourceVmId;

    @SerializedName("sourceexternalref")
    @Param(description = "the source external reference")
    private String sourceExternalRef;

    @SerializedName("direction")
    @Param(description = "the DR direction")
    private String direction;

    @SerializedName("enginetype")
    @Param(description = "the replication engine type")
    private String engineType;

    @SerializedName("enginebindingtype")
    @Param(description = "the engine binding type")
    private String engineBindingType;

    @SerializedName("enginebindingid")
    @Param(description = "the engine binding ID")
    private Long engineBindingId;

    @SerializedName("state")
    @Param(description = "the DR plan state")
    private String state;

    @SerializedName("effectivestate")
    @Param(description = "the effective DR plan state after runtime projection and readiness evaluation")
    private String effectiveState;

    @SerializedName("protectionstate")
    @Param(description = "the current persisted replication authority state")
    private String protectionState;

    @SerializedName("freshnessstate")
    @Param(description = "the current RPO freshness state")
    private String freshnessState;

    @SerializedName("projectionintegritystate")
    @Param(description = "the completed-cycle projection integrity state")
    private String projectionIntegrityState;

    @SerializedName("projectionintegritycode")
    @Param(description = "the completed-cycle projection integrity reason code")
    private String projectionIntegrityCode;

    @SerializedName("projectionintegritysequence")
    @Param(description = "the completed-cycle sequence evaluated for projection integrity")
    private Long projectionIntegritySequence;

    @SerializedName("schedulerstate")
    @Param(description = "the current FTCTL scheduler state")
    private String schedulerState;

    @SerializedName("schedulerdesiredstate")
    @Param(description = "the Cloud desired state for the Plan scheduler")
    private String schedulerDesiredState;

    @SerializedName("schedulerserviceunit")
    @Param(description = "the Plan-scoped systemd scheduler unit")
    private String schedulerServiceUnit;

    @SerializedName("schedulerunitactivestate")
    @Param(description = "the systemd active state for the Plan scheduler")
    private String schedulerUnitActiveState;

    @SerializedName("schedulerunitsubstate")
    @Param(description = "the systemd sub-state for the Plan scheduler")
    private String schedulerUnitSubState;

    @SerializedName("schedulerrecoverystate")
    @Param(description = "the scheduler recovery state")
    private String schedulerRecoveryState;

    @SerializedName("schedulerrecoverytrigger")
    @Param(description = "the scheduler recovery trigger")
    private String schedulerRecoveryTrigger;

    @SerializedName("schedulerpidalive")
    @Param(description = "true when the scheduler process ownership is verified")
    private Boolean schedulerPidAlive;

    @SerializedName("schedulersessionuuid")
    @Param(description = "the Plan-scoped FTCTL scheduler session UUID")
    private String schedulerSessionUuid;

    @SerializedName("schedulerleaseepoch")
    @Param(description = "the monotonic scheduler lease epoch")
    private Long schedulerLeaseEpoch;

    @SerializedName("authoritysequence")
    @Param(description = "the monotonic scheduler authority sequence")
    private Long authoritySequence;

    @SerializedName("schedulerhealth")
    @Param(description = "the scheduler ownership and heartbeat health")
    private String schedulerHealth;

    @SerializedName("schedulernextrunat")
    @Param(description = "the next scheduler start time calculated from the durable RPO deadline")
    private Date schedulerNextRunAt;

    @SerializedName("schedulerexecutionbudgetseconds")
    @Param(description = "the P95 transfer execution budget reserved before the RPO deadline")
    private Integer schedulerExecutionBudgetSeconds;

    @SerializedName("schedulercyclewalldurationseconds")
    @Param(description = "the most recent scheduler cycle wall-clock duration")
    private Integer schedulerCycleWallDurationSeconds;

    @SerializedName("replicationactivity")
    @Param(description = "the current replication activity independent of protection state")
    private String replicationActivity;

    @SerializedName("activeworkerrunuuid")
    @Param(description = "the run UUID that owns the active scheduler worker")
    private String activeWorkerRunUuid;

    @SerializedName("workerheartbeatat")
    @Param(description = "the last verified active scheduler heartbeat time")
    private Date workerHeartbeatAt;

    @SerializedName("workeridentitystate")
    @Param(description = "the FTCTL worker identity verification state")
    private String workerIdentityState;

    @SerializedName("workerlivenessstate")
    @Param(description = "the FTCTL worker liveness state")
    private String workerLivenessState;

    @SerializedName("transferactivitystate")
    @Param(description = "the live transfer activity state")
    private String transferActivityState;

    @SerializedName("transferpayloadbytes")
    @Param(description = "the cumulative payload bytes reported by the active transfer")
    private Long transferPayloadBytes;

    @SerializedName("transferprogressschemaversion") @Param(description = "the live transfer progress schema version") private Integer transferProgressSchemaVersion;
    @SerializedName("transfercyclesequence") @Param(description = "the live transfer cycle sequence") private Long transferCycleSequence;
    @SerializedName("transfersamplesequence") @Param(description = "the live transfer sample sequence") private Long transferSampleSequence;
    @SerializedName("transferphase") @Param(description = "the live transfer phase") private String transferPhase;
    @SerializedName("transfermode") @Param(description = "the live transfer mode") private String transferMode;
    @SerializedName("transferbytestotal") @Param(description = "the total bytes in the active transfer") private Long transferBytesTotal;
    @SerializedName("transferbytesprocessed") @Param(description = "the bytes processed by the active transfer") private Long transferBytesProcessed;
    @SerializedName("transfersourcereadbytes") @Param(description = "the source bytes read by the active transfer") private Long transferSourceReadBytes;
    @SerializedName("transfertargetwrittenbytes") @Param(description = "the target bytes written by the active transfer") private Long transferTargetWrittenBytes;
    @SerializedName("transferverifiedbytes") @Param(description = "the bytes verified by the active transfer") private Long transferVerifiedBytes;
    @SerializedName("transferpercent") @Param(description = "the active transfer completion percentage") private Double transferPercent;
    @SerializedName("transferthroughputbps") @Param(description = "the active transfer throughput in bytes per second") private Long transferThroughputBps;
    @SerializedName("transferetaseconds") @Param(description = "the active transfer estimated remaining seconds") private Long transferEtaSeconds;
    @SerializedName("transfercurrentdiskindex") @Param(description = "the zero-based active transfer disk index") private Integer transferCurrentDiskIndex;
    @SerializedName("transferdiskcount") @Param(description = "the active transfer disk count") private Integer transferDiskCount;
    @SerializedName("transferprogressestimated") @Param(description = "whether the active transfer progress is estimated") private Boolean transferProgressEstimated;
    @SerializedName("transferprogresssampledat") @Param(description = "the live transfer progress sample time") private Date transferProgressSampledAt;
    @SerializedName("transferprogressstale") @Param(description = "whether the live transfer progress sample is stale") private Boolean transferProgressStale;

    @SerializedName("reconciliationstate")
    @Param(description = "the runtime reconciliation state")
    private String reconciliationState;

    @SerializedName("ownermatched")
    @Param(description = "true when the status owner matches the active lease")
    private Boolean ownerMatched;

    @SerializedName("normalcutoverready")
    @Param(description = "true when the canonical Plan authority permits test failover or failover")
    private Boolean normalCutoverReady;

    @SerializedName("normalcutoverreason")
    @Param(description = "typed reason why normal cutover is currently blocked")
    private String normalCutoverReason;

    @SerializedName("authoritygeneration")
    @Param(description = "the accepted FTCTL runtime generation")
    private Long authorityGeneration;

    @SerializedName("currentcyclesequence")
    @Param(description = "the current replication cycle sequence")
    private Long currentCycleSequence;

    @SerializedName("currentcyclestate")
    @Param(description = "the current replication cycle state")
    private String currentCycleState;

    @SerializedName("currentcyclemode")
    @Param(description = "the current replication cycle mode")
    private String currentCycleMode;

    @SerializedName("baselinestate")
    @Param(description = "the committed CBT baseline state")
    private String baselineState;

    @SerializedName("reseedreason")
    @Param(description = "the automatic full reseed reason")
    private String reseedReason;

    @SerializedName("rpoageseconds")
    @Param(description = "the age of the latest durable target copy")
    private Long rpoAgeSeconds;

    @SerializedName("rpooverdue")
    @Param(description = "true when the latest durable target copy is outside RPO")
    private Boolean rpoOverdue;

    @SerializedName("rpoevaluationmode")
    @Param(description = "the authority-aware RPO evaluation mode")
    private String rpoEvaluationMode;

    @SerializedName("displayrposeconds")
    @Param(description = "the authority-aware RPO value to display")
    private Long displayRpoSeconds;

    @SerializedName("rpoasof")
    @Param(description = "the reference time used for the displayed RPO")
    private Date rpoAsOf;

    @SerializedName("rpostatus")
    @Param(description = "the authority-aware RPO target status")
    private String rpoStatus;

    @SerializedName("currentseverity")
    @Param(description = "the severity of the current DR condition, excluding historical failures")
    private String currentSeverity;

    @SerializedName("adminstate")
    @Param(description = "the DR plan administrative state")
    private String adminState;

    @SerializedName("activeside")
    @Param(description = "the active DR side")
    private String activeSide;

    @SerializedName("operatingside")
    @Param(description = "the side currently authorized to serve workloads")
    private String operatingSide;

    @SerializedName("protectionphase")
    @Param(description = "the current protection or cutover phase")
    private String protectionPhase;

    @SerializedName("authorityside")
    @Param(description = "the canonical side that currently owns workload authority")
    private String authoritySide;

    @SerializedName("authorityphase")
    @Param(description = "the canonical authority lifecycle phase")
    private String authorityPhase;

    @SerializedName("authorityconsistent")
    @Param(description = "true when Plan, cutover session, and runtime authority evidence agree")
    private Boolean authorityConsistent;

    @SerializedName("authorityinconsistencycode")
    @Param(description = "typed authority projection inconsistency code")
    private String authorityInconsistencyCode;

    @SerializedName("authorityinconsistencymessage")
    @Param(description = "operator-readable authority projection inconsistency message")
    private String authorityInconsistencyMessage;

    @SerializedName("authoritytransitiontype")
    @Param(description = "the recognized authority transition type")
    private String authorityTransitionType;

    @SerializedName("authoritytransitionstate")
    @Param(description = "the recognized authority transition state")
    private String authorityTransitionState;

    @SerializedName("authoritytransitionrunid")
    @Param(description = "the DR run ID that owns the current authority transition")
    private String authorityTransitionRunId;

    @SerializedName("requiredcheckpointsequence")
    @Param(description = "the minimum completed checkpoint sequence required to finish the transition")
    private Long requiredCheckpointSequence;

    @SerializedName("currentcutoversessionid")
    @Param(description = "the current authority-bearing cutover session ID, absent for source authority")
    private String currentCutoverSessionId;

    @SerializedName("cutoversessionstate")
    @Param(description = "the persisted actual failover session state")
    private String cutoverSessionState;

    @SerializedName("cloudpromotionstate")
    @Param(description = "the Cloud-owned target promotion state")
    private String cloudPromotionState;

    @SerializedName("cutovertargetpowerstate")
    @Param(description = "the target VM power state recorded by Cloud during cutover")
    private String cutoverTargetPowerState;

    @SerializedName("cutoverbootvalidationstate")
    @Param(description = "the target VM boot validation state recorded during cutover")
    private String cutoverBootValidationState;

    @SerializedName("engineackstate")
    @Param(description = "the FTCTL acknowledgement state for Cloud promotion")
    private String engineAckState;

    @SerializedName("cutovercommitstate")
    @Param(description = "the durable FTCTL cutover commit state")
    private String cutoverCommitState;

    @SerializedName("cutoverauthoritygeneration")
    @Param(description = "the monotonic Cloud cutover authority generation")
    private Long cutoverAuthorityGeneration;

    @SerializedName("cutovercompletedat")
    @Param(description = "the time at which Cloud and FTCTL completed authority convergence")
    private Date cutoverCompletedAt;

    @SerializedName("rposeconds")
    @Param(description = "the target RPO in seconds")
    private Integer rpoSeconds;

    @SerializedName("rtoseconds")
    @Param(description = "the target RTO in seconds")
    private Integer rtoSeconds;

    @SerializedName("schedulejson")
    @Param(description = "the sync schedule JSON")
    private String scheduleJson;

    @SerializedName("policyjson")
    @Param(description = "the plan policy JSON")
    private String policyJson;

    @SerializedName("mappingjson")
    @Param(description = "the plan mapping JSON")
    private String mappingJson;

    @SerializedName("quiescepolicyjson")
    @Param(description = "the quiesce policy JSON")
    private String quiescePolicyJson;

    @SerializedName("sourceworkerhostid")
    @Param(description = "the source worker host ID")
    private Long sourceWorkerHostId;

    @SerializedName("sourceworkerhostuuid")
    @Param(description = "the remote source worker host UUID")
    private String sourceWorkerHostUuid;

    @SerializedName("sourceworkerhostname")
    @Param(description = "the remote source worker host name")
    private String sourceWorkerHostName;

    @SerializedName("targetworkerhostid")
    @Param(description = "the target worker host ID")
    private Long targetWorkerHostId;

    @SerializedName("coordinatorworkerhostid")
    @Param(description = "the coordinator worker host ID")
    private Long coordinatorWorkerHostId;

    @SerializedName("protectiongroupuuid") @Param(description = "the protection group UUID") private String protectionGroupUuid;
    @SerializedName("protectiongroupname") @Param(description = "the protection group name") private String protectionGroupName;
    @SerializedName("protectiongrouporder") @Param(description = "the ordered position in the protection group") private Integer protectionGroupOrder;
    @SerializedName("protectiongroupmaxparallel") @Param(description = "the group maximum concurrency") private Integer protectionGroupMaxParallel;
    @SerializedName("protectiongroupquiescerequired") @Param(description = "whether group quiesce is required") private Boolean protectionGroupQuiesceRequired;

    @SerializedName("lastsourcecheckpointat")
    @Param(description = "the latest source checkpoint time")
    private Date lastSourceCheckpointAt;

    @SerializedName("lasttargetdurableat")
    @Param(description = "the latest target durable checkpoint time")
    private Date lastTargetDurableAt;

    @SerializedName("targetreadyat")
    @Param(description = "the latest target-ready time")
    private Date targetReadyAt;

    @SerializedName("targetreadyrposeconds")
    @Param(description = "the latest target-ready RPO in seconds")
    private Integer targetReadyRpoSeconds;

    @SerializedName("lastrunid")
    @Param(description = "the last DR run ID")
    private Long lastRunId;

    @SerializedName("lastrun")
    @Param(description = "the latest DR run summary")
    private DrRunResponse lastRun;

    @SerializedName("runtimeprojectionstate")
    @Param(description = "the latest runtime projection state")
    private String runtimeProjectionState;

    @SerializedName("runtimeprojectionmessage")
    @Param(description = "the latest runtime projection message")
    private String runtimeProjectionMessage;

    @SerializedName("runtimestate")
    @Param(description = "the latest FTCTL runtime state")
    private String runtimeState;

    @SerializedName("runtimestep")
    @Param(description = "the latest FTCTL runtime step")
    private String runtimeStep;

    @SerializedName("runtimeerrorcode")
    @Param(description = "the latest FTCTL runtime error code")
    private String runtimeErrorCode;

    @SerializedName("runtimecontrolprotocolversion")
    @Param(description = "the FTCTL DR control protocol version")
    private Integer runtimeControlProtocolVersion;

    @SerializedName("runtimecontrolgeneration")
    @Param(description = "the latest FTCTL DR control request generation")
    private Long runtimeControlGeneration;

    @SerializedName("runtimecontrolackgeneration")
    @Param(description = "the latest FTCTL DR acknowledged control generation")
    private Long runtimeControlAckGeneration;

    @SerializedName("runtimecontrolstate")
    @Param(description = "the latest FTCTL DR scheduler control state")
    private String runtimeControlState;

    @SerializedName("runtimecyclestate")
    @Param(description = "the latest FTCTL DR replication cycle state")
    private String runtimeCycleState;

    @SerializedName("runtimetransitionstate")
    @Param(description = "the latest FTCTL DR transition coordination state")
    private String runtimeTransitionState;

    @SerializedName("runtimecheckpointleasestate")
    @Param(description = "the latest FTCTL DR checkpoint lease state")
    private String runtimeCheckpointLeaseState;

    @SerializedName("runtimecontrolready")
    @Param(description = "true when the FTCTL DR control protocol is ready for coordinated actions")
    private Boolean runtimeControlReady;

    @SerializedName("targetmaterializationstate")
    @Param(description = "the derived target materialization state")
    private String targetMaterializationState;

    @SerializedName("targetmaterializationmessage")
    @Param(description = "the derived target materialization message")
    private String targetMaterializationMessage;

    @SerializedName("targetownershipstate")
    @Param(description = "the authoritative target resource ownership state")
    private String targetOwnershipState;

    @SerializedName("targetownershipgeneration")
    @Param(description = "the monotonic target ownership generation")
    private Long targetOwnershipGeneration;

    @SerializedName("targetmaterializationdigest")
    @Param(description = "the accepted target materialization manifest SHA-256")
    private String targetMaterializationDigest;

    @SerializedName("targetpowerstateobservedat")
    @Param(description = "the time at which target power state was last observed")
    private Date targetPowerStateObservedAt;

    @SerializedName("initialsyncinprogress")
    @Param(description = "true if the initial DR sync is actively transferring data before target VM materialization")
    private Boolean initialSyncInProgress;

    @SerializedName("sourcediskmappath")
    @Param(description = "the latest FTCTL source disk map path")
    private String sourceDiskMapPath;

    @SerializedName("targetdiskmappath")
    @Param(description = "the latest FTCTL target disk map path")
    private String targetDiskMapPath;

    @SerializedName("diskmaprole")
    @Param(description = "the active FTCTL disk map role")
    private String diskMapRole;

    @SerializedName("targetdiskcount")
    @Param(description = "the target disk count reported by FTCTL")
    private Integer targetDiskCount;

    @SerializedName("targetdiskinvalidcount")
    @Param(description = "the invalid target disk count reported by FTCTL")
    private Integer targetDiskInvalidCount;

    @SerializedName("runtimecbtenabled")
    @Param(description = "whether VMware CBT is enabled according to the latest FTCTL preflight status")
    private Boolean runtimeCbtEnabled;

    @SerializedName("runtimecbtlifecyclestate")
    @Param(description = "the evidence-backed VMware CBT lifecycle state")
    private String runtimeCbtLifecycleState;

    @SerializedName("runtimecbtvmconfigsignal")
    @Param(description = "the non-authoritative VMware VM-level CBT configuration signal")
    private String runtimeCbtVmConfigSignal;

    @SerializedName("runtimecbtdiskid")
    @Param(description = "the VMware CBT disk ID resolved by the latest FTCTL preflight status")
    private String runtimeCbtDiskId;

    @SerializedName("runtimecbtmessage")
    @Param(description = "the VMware CBT preflight message reported by FTCTL")
    private String runtimeCbtMessage;

    @SerializedName("runtimecbtgovcbin")
    @Param(description = "the govc binary resolved by the latest FTCTL VMware preflight status")
    private String runtimeCbtGovcBin;

    @SerializedName("runtimecbtcheckedatepochms")
    @Param(description = "the VMware CBT preflight check timestamp in epoch milliseconds")
    private Long runtimeCbtCheckedAtEpochMs;

    @SerializedName("runtimesourceopenready")
    @Param(description = "whether VMware source-open preflight succeeded according to the latest FTCTL status")
    private Boolean runtimeSourceOpenReady;

    @SerializedName("runtimesourceopenerrorcode")
    @Param(description = "the VMware source-open preflight error code reported by FTCTL")
    private String runtimeSourceOpenErrorCode;

    @SerializedName("runtimesourceopenmessage")
    @Param(description = "the VMware source-open preflight message reported by FTCTL")
    private String runtimeSourceOpenMessage;

    @SerializedName("runtimesourcesnapshotready")
    @Param(description = "whether VMware source snapshot preflight succeeded according to the latest FTCTL status")
    private Boolean runtimeSourceSnapshotReady;

    @SerializedName("runtimesourcesnapshoterrorcode")
    @Param(description = "the VMware source snapshot preflight error code reported by FTCTL")
    private String runtimeSourceSnapshotErrorCode;

    @SerializedName("runtimesourcesnapshotmessage")
    @Param(description = "the VMware source snapshot preflight message reported by FTCTL")
    private String runtimeSourceSnapshotMessage;

    @SerializedName("runtimesourcesnapshotname")
    @Param(description = "the VMware source snapshot name used by FTCTL")
    private String runtimeSourceSnapshotName;

    @SerializedName("runtimesourcesnapshotrefpresent")
    @Param(description = "whether FTCTL resolved the VMware source snapshot managed object reference")
    private Boolean runtimeSourceSnapshotRefPresent;

    @SerializedName("runtimesourcesnapshotlifecyclestate")
    @Param(description = "the VMware source snapshot lifecycle state")
    private String runtimeSourceSnapshotLifecycleState;

    @SerializedName("runtimesourcesnapshotcleanuprequired")
    @Param(description = "whether VMware source snapshot cleanup is required")
    private Boolean runtimeSourceSnapshotCleanupRequired;

    @SerializedName("runtimesourcesnapshotlastref")
    @Param(description = "the last VMware source snapshot reference retained for audit")
    private String runtimeSourceSnapshotLastRef;

    @SerializedName("runtimesourcesnapshotcleanedatepochms")
    @Param(description = "the VMware source snapshot cleanup time in epoch milliseconds")
    private Long runtimeSourceSnapshotCleanedAtEpochMs;

    @SerializedName("lasterrorcode")
    @Param(description = "the last error code")
    private String lastErrorCode;

    @SerializedName("lasterrormessage")
    @Param(description = "the last error message")
    private String lastErrorMessage;

    @SerializedName("failedcomponent")
    @Param(description = "the runtime component that reported the latest failure")
    private String failedComponent;

    @SerializedName("datacommitstate")
    @Param(description = "the current replication cycle data commit state")
    private String dataCommitState;

    @SerializedName("datacopied")
    @Param(description = "whether disk data was copied for the current cycle")
    private Boolean dataCopied;

    @SerializedName("metadatacommitted")
    @Param(description = "whether cycle metadata was committed")
    private Boolean metadataCommitted;

    @SerializedName("targetdurable")
    @Param(description = "whether the current cycle is durable on the target")
    private Boolean targetDurable;

    @SerializedName("cycleretrymode")
    @Param(description = "the safe retry mode for the current replication cycle")
    private String cycleRetryMode;

    @SerializedName("actioneligibility")
    @Param(description = "the backend calculated action eligibility map")
    private Map<String, Boolean> actionEligibility;

    @SerializedName("actionavailability")
    @Param(description = "the backend calculated typed action availability map")
    private Map<String, DrActionAvailabilityResponse> actionAvailability;

    @SerializedName("readinessstate")
    @Param(description = "the backend calculated DR plan readiness state")
    private String readinessState;

    @SerializedName("readinessreasoncode")
    @Param(description = "the backend calculated DR plan readiness reason code")
    private String readinessReasonCode;

    @SerializedName("readinessmessage")
    @Param(description = "the backend calculated DR plan readiness message")
    private String readinessMessage;

    @SerializedName("executionready")
    @Param(description = "true if the plan can start a protection sync")
    private Boolean executionReady;

    @SerializedName("releaseready")
    @Param(description = "true if the plan has runtime resources that can be released")
    private Boolean releaseReady;

    @SerializedName("engineaccepted")
    @Param(description = "true if the DR engine has accepted the plan or runtime resources exist")
    private Boolean engineAccepted;

    @SerializedName("targetmaterialized")
    @Param(description = "true if the target VM/storage/network and restore point are materialized")
    private Boolean targetMaterialized;

    @SerializedName("targetvmpresent")
    @Param(description = "true if the target VM reference exists")
    private Boolean targetVmPresent;

    @SerializedName("targetstoragepresent")
    @Param(description = "true if the target storage/checkpoint evidence exists")
    private Boolean targetStoragePresent;

    @SerializedName("targetnetworkpresent")
    @Param(description = "true if the target network evidence exists")
    private Boolean targetNetworkPresent;

    @SerializedName("restorepointpresent")
    @Param(description = "true if a target-ready restore point exists")
    private Boolean restorePointPresent;

    @SerializedName("durablecheckpointpresent")
    @Param(description = "true if a durable target checkpoint exists")
    private Boolean durableCheckpointPresent;

    @SerializedName("readinessblockingreasons")
    @Param(description = "blocking readiness reason codes")
    private List<String> readinessBlockingReasons;

    @SerializedName("readinesswarnings")
    @Param(description = "non-blocking readiness warnings")
    private List<String> readinessWarnings;

    @SerializedName("created")
    @Param(description = "the creation date")
    private Date created;

    @SerializedName("removed")
    @Param(description = "the removal date")
    private Date removed;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSourceSiteId(String sourceSiteId) {
        this.sourceSiteId = sourceSiteId;
    }

    public void setTargetSiteId(String targetSiteId) {
        this.targetSiteId = targetSiteId;
    }

    public void setSourceVmId(Long sourceVmId) {
        this.sourceVmId = sourceVmId;
    }

    public void setSourceExternalRef(String sourceExternalRef) {
        this.sourceExternalRef = sourceExternalRef;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public void setEngineBindingType(String engineBindingType) {
        this.engineBindingType = engineBindingType;
    }

    public void setEngineBindingId(Long engineBindingId) {
        this.engineBindingId = engineBindingId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setEffectiveState(String effectiveState) {
        this.effectiveState = effectiveState;
    }

    public void setProtectionState(String value) { protectionState = value; }
    public void setFreshnessState(String value) { freshnessState = value; }
    public void setProjectionIntegrityState(String value) { projectionIntegrityState = value; }
    public void setProjectionIntegrityCode(String value) { projectionIntegrityCode = value; }
    public void setProjectionIntegritySequence(Long value) { projectionIntegritySequence = value; }
    public void setSchedulerState(String value) { schedulerState = value; }
    public void setSchedulerDesiredState(String value) { schedulerDesiredState = value; }
    public void setSchedulerServiceUnit(String value) { schedulerServiceUnit = value; }
    public void setSchedulerUnitActiveState(String value) { schedulerUnitActiveState = value; }
    public void setSchedulerUnitSubState(String value) { schedulerUnitSubState = value; }
    public void setSchedulerRecoveryState(String value) { schedulerRecoveryState = value; }
    public void setSchedulerRecoveryTrigger(String value) { schedulerRecoveryTrigger = value; }
    public void setSchedulerPidAlive(Boolean value) { schedulerPidAlive = value; }
    public void setSchedulerSessionUuid(String value) { schedulerSessionUuid = value; }
    public void setSchedulerLeaseEpoch(Long value) { schedulerLeaseEpoch = value; }
    public void setAuthoritySequence(Long value) { authoritySequence = value; }
    public void setSchedulerHealth(String value) { schedulerHealth = value; }
    public void setSchedulerNextRunAt(Date value) { schedulerNextRunAt = value; }
    public void setSchedulerExecutionBudgetSeconds(Integer value) { schedulerExecutionBudgetSeconds = value; }
    public void setSchedulerCycleWallDurationSeconds(Integer value) { schedulerCycleWallDurationSeconds = value; }
    public void setReplicationActivity(String value) { replicationActivity = value; }
    public void setActiveWorkerRunUuid(String value) { activeWorkerRunUuid = value; }
    public void setWorkerHeartbeatAt(Date value) { workerHeartbeatAt = value; }
    public void setWorkerIdentityState(String value) { workerIdentityState = value; }
    public void setWorkerLivenessState(String value) { workerLivenessState = value; }
    public void setTransferActivityState(String value) { transferActivityState = value; }
    public void setTransferPayloadBytes(Long value) { transferPayloadBytes = value; }
    public void setTransferProgressSchemaVersion(Integer value) { transferProgressSchemaVersion = value; }
    public void setTransferCycleSequence(Long value) { transferCycleSequence = value; }
    public void setTransferSampleSequence(Long value) { transferSampleSequence = value; }
    public void setTransferPhase(String value) { transferPhase = value; }
    public void setTransferMode(String value) { transferMode = value; }
    public void setTransferBytesTotal(Long value) { transferBytesTotal = value; }
    public void setTransferBytesProcessed(Long value) { transferBytesProcessed = value; }
    public void setTransferSourceReadBytes(Long value) { transferSourceReadBytes = value; }
    public void setTransferTargetWrittenBytes(Long value) { transferTargetWrittenBytes = value; }
    public void setTransferVerifiedBytes(Long value) { transferVerifiedBytes = value; }
    public void setTransferPercent(Double value) { transferPercent = value; }
    public void setTransferThroughputBps(Long value) { transferThroughputBps = value; }
    public void setTransferEtaSeconds(Long value) { transferEtaSeconds = value; }
    public void setTransferCurrentDiskIndex(Integer value) { transferCurrentDiskIndex = value; }
    public void setTransferDiskCount(Integer value) { transferDiskCount = value; }
    public void setTransferProgressEstimated(Boolean value) { transferProgressEstimated = value; }
    public void setTransferProgressSampledAt(Date value) { transferProgressSampledAt = value; }
    public void setTransferProgressStale(Boolean value) { transferProgressStale = value; }
    public void setReconciliationState(String value) { reconciliationState = value; }
    public void setOwnerMatched(Boolean value) { ownerMatched = value; }
    public void setNormalCutoverReady(Boolean value) { normalCutoverReady = value; }
    public void setNormalCutoverReason(String value) { normalCutoverReason = value; }
    public void setAuthorityGeneration(Long value) { authorityGeneration = value; }
    public void setCurrentCycleSequence(Long value) { currentCycleSequence = value; }
    public void setCurrentCycleState(String value) { currentCycleState = value; }
    public void setCurrentCycleMode(String value) { currentCycleMode = value; }
    public void setBaselineState(String value) { baselineState = value; }
    public void setReseedReason(String value) { reseedReason = value; }
    public void setRpoAgeSeconds(Long value) { rpoAgeSeconds = value; }
    public void setRpoOverdue(Boolean value) { rpoOverdue = value; }
    public void setRpoEvaluationMode(String value) { rpoEvaluationMode = value; }
    public void setDisplayRpoSeconds(Long value) { displayRpoSeconds = value; }
    public void setRpoAsOf(Date value) { rpoAsOf = value; }
    public void setRpoStatus(String value) { rpoStatus = value; }
    public void setCurrentSeverity(String value) { currentSeverity = value; }

    public void setAdminState(String adminState) {
        this.adminState = adminState;
    }

    public void setActiveSide(String activeSide) {
        this.activeSide = activeSide;
    }

    public void setOperatingSide(String value) { operatingSide = value; }
    public void setProtectionPhase(String value) { protectionPhase = value; }
    public void setAuthoritySide(String value) { authoritySide = value; }
    public void setAuthorityPhase(String value) { authorityPhase = value; }
    public void setAuthorityConsistent(Boolean value) { authorityConsistent = value; }
    public void setAuthorityInconsistencyCode(String value) { authorityInconsistencyCode = value; }
    public void setAuthorityInconsistencyMessage(String value) { authorityInconsistencyMessage = value; }
    public void setAuthorityTransitionType(String value) { authorityTransitionType = value; }
    public void setAuthorityTransitionState(String value) { authorityTransitionState = value; }
    public void setAuthorityTransitionRunId(String value) { authorityTransitionRunId = value; }
    public void setRequiredCheckpointSequence(Long value) { requiredCheckpointSequence = value; }
    public void setCurrentCutoverSessionId(String value) { currentCutoverSessionId = value; }
    public void setCutoverSessionState(String value) { cutoverSessionState = value; }
    public void setCloudPromotionState(String value) { cloudPromotionState = value; }
    public void setCutoverTargetPowerState(String value) { cutoverTargetPowerState = value; }
    public void setCutoverBootValidationState(String value) { cutoverBootValidationState = value; }
    public void setEngineAckState(String value) { engineAckState = value; }
    public void setCutoverCommitState(String value) { cutoverCommitState = value; }
    public void setCutoverAuthorityGeneration(Long value) { cutoverAuthorityGeneration = value; }
    public void setCutoverCompletedAt(Date value) { cutoverCompletedAt = value; }

    public void setRpoSeconds(Integer rpoSeconds) {
        this.rpoSeconds = rpoSeconds;
    }

    public void setRtoSeconds(Integer rtoSeconds) {
        this.rtoSeconds = rtoSeconds;
    }

    public void setScheduleJson(String scheduleJson) {
        this.scheduleJson = scheduleJson;
    }

    public void setPolicyJson(String policyJson) {
        this.policyJson = policyJson;
    }

    public void setMappingJson(String mappingJson) {
        this.mappingJson = mappingJson;
    }

    public void setQuiescePolicyJson(String quiescePolicyJson) {
        this.quiescePolicyJson = quiescePolicyJson;
    }

    public void setSourceWorkerHostId(Long sourceWorkerHostId) {
        this.sourceWorkerHostId = sourceWorkerHostId;
    }

    public void setSourceWorkerHostUuid(String sourceWorkerHostUuid) {
        this.sourceWorkerHostUuid = sourceWorkerHostUuid;
    }

    public void setSourceWorkerHostName(String sourceWorkerHostName) {
        this.sourceWorkerHostName = sourceWorkerHostName;
    }

    public void setTargetWorkerHostId(Long targetWorkerHostId) {
        this.targetWorkerHostId = targetWorkerHostId;
    }

    public void setCoordinatorWorkerHostId(Long coordinatorWorkerHostId) {
        this.coordinatorWorkerHostId = coordinatorWorkerHostId;
    }

    public void setProtectionGroupUuid(String value) { protectionGroupUuid = value; }
    public void setProtectionGroupName(String value) { protectionGroupName = value; }
    public void setProtectionGroupOrder(Integer value) { protectionGroupOrder = value; }
    public void setProtectionGroupMaxParallel(Integer value) { protectionGroupMaxParallel = value; }
    public void setProtectionGroupQuiesceRequired(Boolean value) { protectionGroupQuiesceRequired = value; }

    public void setLastSourceCheckpointAt(Date lastSourceCheckpointAt) {
        this.lastSourceCheckpointAt = lastSourceCheckpointAt;
    }

    public void setLastTargetDurableAt(Date lastTargetDurableAt) {
        this.lastTargetDurableAt = lastTargetDurableAt;
    }

    public void setTargetReadyAt(Date targetReadyAt) {
        this.targetReadyAt = targetReadyAt;
    }

    public void setTargetReadyRpoSeconds(Integer targetReadyRpoSeconds) {
        this.targetReadyRpoSeconds = targetReadyRpoSeconds;
    }

    public void setLastRunId(Long lastRunId) {
        this.lastRunId = lastRunId;
    }

    public void setLastRun(DrRunResponse lastRun) {
        this.lastRun = lastRun;
    }

    public void setRuntimeProjectionState(String runtimeProjectionState) {
        this.runtimeProjectionState = runtimeProjectionState;
    }

    public void setRuntimeProjectionMessage(String runtimeProjectionMessage) {
        this.runtimeProjectionMessage = runtimeProjectionMessage;
    }

    public void setRuntimeState(String runtimeState) {
        this.runtimeState = runtimeState;
    }

    public void setRuntimeStep(String runtimeStep) {
        this.runtimeStep = runtimeStep;
    }

    public void setRuntimeErrorCode(String runtimeErrorCode) {
        this.runtimeErrorCode = runtimeErrorCode;
    }

    public void setRuntimeControlProtocolVersion(Integer runtimeControlProtocolVersion) {
        this.runtimeControlProtocolVersion = runtimeControlProtocolVersion;
    }

    public void setRuntimeControlGeneration(Long runtimeControlGeneration) {
        this.runtimeControlGeneration = runtimeControlGeneration;
    }

    public void setRuntimeControlAckGeneration(Long runtimeControlAckGeneration) {
        this.runtimeControlAckGeneration = runtimeControlAckGeneration;
    }

    public void setRuntimeControlState(String runtimeControlState) {
        this.runtimeControlState = runtimeControlState;
    }

    public void setRuntimeCycleState(String runtimeCycleState) {
        this.runtimeCycleState = runtimeCycleState;
    }

    public void setRuntimeTransitionState(String runtimeTransitionState) {
        this.runtimeTransitionState = runtimeTransitionState;
    }

    public void setRuntimeCheckpointLeaseState(String runtimeCheckpointLeaseState) {
        this.runtimeCheckpointLeaseState = runtimeCheckpointLeaseState;
    }

    public void setRuntimeControlReady(Boolean runtimeControlReady) {
        this.runtimeControlReady = runtimeControlReady;
    }

    public void setTargetMaterializationState(String targetMaterializationState) {
        this.targetMaterializationState = targetMaterializationState;
    }

    public void setTargetMaterializationMessage(String targetMaterializationMessage) {
        this.targetMaterializationMessage = targetMaterializationMessage;
    }

    public void setTargetOwnershipState(String targetOwnershipState) { this.targetOwnershipState = targetOwnershipState; }
    public void setTargetOwnershipGeneration(Long targetOwnershipGeneration) { this.targetOwnershipGeneration = targetOwnershipGeneration; }
    public void setTargetMaterializationDigest(String targetMaterializationDigest) { this.targetMaterializationDigest = targetMaterializationDigest; }
    public void setTargetPowerStateObservedAt(Date targetPowerStateObservedAt) { this.targetPowerStateObservedAt = targetPowerStateObservedAt; }

    public void setInitialSyncInProgress(Boolean initialSyncInProgress) {
        this.initialSyncInProgress = initialSyncInProgress;
    }

    public void setSourceDiskMapPath(String sourceDiskMapPath) {
        this.sourceDiskMapPath = sourceDiskMapPath;
    }

    public void setTargetDiskMapPath(String targetDiskMapPath) {
        this.targetDiskMapPath = targetDiskMapPath;
    }

    public void setDiskMapRole(String diskMapRole) {
        this.diskMapRole = diskMapRole;
    }

    public void setTargetDiskCount(Integer targetDiskCount) {
        this.targetDiskCount = targetDiskCount;
    }

    public void setTargetDiskInvalidCount(Integer targetDiskInvalidCount) {
        this.targetDiskInvalidCount = targetDiskInvalidCount;
    }

    public void setRuntimeCbtEnabled(Boolean runtimeCbtEnabled) {
        this.runtimeCbtEnabled = runtimeCbtEnabled;
    }

    public void setRuntimeCbtLifecycleState(String runtimeCbtLifecycleState) {
        this.runtimeCbtLifecycleState = runtimeCbtLifecycleState;
    }

    public void setRuntimeCbtVmConfigSignal(String runtimeCbtVmConfigSignal) {
        this.runtimeCbtVmConfigSignal = runtimeCbtVmConfigSignal;
    }

    public void setRuntimeCbtDiskId(String runtimeCbtDiskId) {
        this.runtimeCbtDiskId = runtimeCbtDiskId;
    }

    public void setRuntimeCbtMessage(String runtimeCbtMessage) {
        this.runtimeCbtMessage = runtimeCbtMessage;
    }

    public void setRuntimeCbtGovcBin(String runtimeCbtGovcBin) {
        this.runtimeCbtGovcBin = runtimeCbtGovcBin;
    }

    public void setRuntimeCbtCheckedAtEpochMs(Long runtimeCbtCheckedAtEpochMs) {
        this.runtimeCbtCheckedAtEpochMs = runtimeCbtCheckedAtEpochMs;
    }

    public void setRuntimeSourceOpenReady(Boolean runtimeSourceOpenReady) {
        this.runtimeSourceOpenReady = runtimeSourceOpenReady;
    }

    public void setRuntimeSourceOpenErrorCode(String runtimeSourceOpenErrorCode) {
        this.runtimeSourceOpenErrorCode = runtimeSourceOpenErrorCode;
    }

    public void setRuntimeSourceOpenMessage(String runtimeSourceOpenMessage) {
        this.runtimeSourceOpenMessage = runtimeSourceOpenMessage;
    }

    public void setRuntimeSourceSnapshotReady(Boolean runtimeSourceSnapshotReady) {
        this.runtimeSourceSnapshotReady = runtimeSourceSnapshotReady;
    }

    public void setRuntimeSourceSnapshotErrorCode(String runtimeSourceSnapshotErrorCode) {
        this.runtimeSourceSnapshotErrorCode = runtimeSourceSnapshotErrorCode;
    }

    public void setRuntimeSourceSnapshotMessage(String runtimeSourceSnapshotMessage) {
        this.runtimeSourceSnapshotMessage = runtimeSourceSnapshotMessage;
    }

    public void setRuntimeSourceSnapshotName(String runtimeSourceSnapshotName) {
        this.runtimeSourceSnapshotName = runtimeSourceSnapshotName;
    }

    public void setRuntimeSourceSnapshotRefPresent(Boolean runtimeSourceSnapshotRefPresent) {
        this.runtimeSourceSnapshotRefPresent = runtimeSourceSnapshotRefPresent;
    }

    public void setRuntimeSourceSnapshotLifecycleState(String value) { this.runtimeSourceSnapshotLifecycleState = value; }
    public void setRuntimeSourceSnapshotCleanupRequired(Boolean value) { this.runtimeSourceSnapshotCleanupRequired = value; }
    public void setRuntimeSourceSnapshotLastRef(String value) { this.runtimeSourceSnapshotLastRef = value; }
    public void setRuntimeSourceSnapshotCleanedAtEpochMs(Long value) { this.runtimeSourceSnapshotCleanedAtEpochMs = value; }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public void setFailedComponent(String failedComponent) {
        this.failedComponent = failedComponent;
    }

    public void setDataCommitState(String dataCommitState) {
        this.dataCommitState = dataCommitState;
    }

    public void setDataCopied(Boolean dataCopied) {
        this.dataCopied = dataCopied;
    }

    public void setMetadataCommitted(Boolean metadataCommitted) {
        this.metadataCommitted = metadataCommitted;
    }

    public void setTargetDurable(Boolean targetDurable) {
        this.targetDurable = targetDurable;
    }

    public void setCycleRetryMode(String cycleRetryMode) {
        this.cycleRetryMode = cycleRetryMode;
    }

    public void setActionEligibility(Map<String, Boolean> actionEligibility) {
        this.actionEligibility = actionEligibility;
    }

    public void setActionAvailability(Map<String, DrActionAvailabilityResponse> actionAvailability) {
        this.actionAvailability = actionAvailability;
    }

    public void setReadinessState(String readinessState) {
        this.readinessState = readinessState;
    }

    public void setReadinessReasonCode(String readinessReasonCode) {
        this.readinessReasonCode = readinessReasonCode;
    }

    public void setReadinessMessage(String readinessMessage) {
        this.readinessMessage = readinessMessage;
    }

    public void setExecutionReady(Boolean executionReady) {
        this.executionReady = executionReady;
    }

    public void setReleaseReady(Boolean releaseReady) {
        this.releaseReady = releaseReady;
    }

    public void setEngineAccepted(Boolean engineAccepted) {
        this.engineAccepted = engineAccepted;
    }

    public void setTargetMaterialized(Boolean targetMaterialized) {
        this.targetMaterialized = targetMaterialized;
    }

    public void setTargetVmPresent(Boolean targetVmPresent) {
        this.targetVmPresent = targetVmPresent;
    }

    public void setTargetStoragePresent(Boolean targetStoragePresent) {
        this.targetStoragePresent = targetStoragePresent;
    }

    public void setTargetNetworkPresent(Boolean targetNetworkPresent) {
        this.targetNetworkPresent = targetNetworkPresent;
    }

    public void setRestorePointPresent(Boolean restorePointPresent) {
        this.restorePointPresent = restorePointPresent;
    }

    public void setDurableCheckpointPresent(Boolean durableCheckpointPresent) {
        this.durableCheckpointPresent = durableCheckpointPresent;
    }

    public void setReadinessBlockingReasons(List<String> readinessBlockingReasons) {
        this.readinessBlockingReasons = readinessBlockingReasons;
    }

    public void setReadinessWarnings(List<String> readinessWarnings) {
        this.readinessWarnings = readinessWarnings;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }
}
