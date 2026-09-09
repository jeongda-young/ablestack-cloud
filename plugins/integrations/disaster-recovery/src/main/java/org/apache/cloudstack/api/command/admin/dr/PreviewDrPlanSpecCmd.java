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
package org.apache.cloudstack.api.command.admin.dr;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.dr.DrPlanSpecPreviewResponse;
import org.apache.cloudstack.api.response.dr.DrSiteResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.dr.DrPlanGeneratedSpec;
import com.cloud.dr.DrPlanGuidedSpec;
import com.cloud.dr.DrPlanGuidedSpecBuilder;
import com.cloud.dr.DrPlanReadiness;
import com.cloud.dr.DrPlanReadinessValidator;
import com.cloud.dr.DrPlanVO;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@APICommand(name = PreviewDrPlanSpecCmd.APINAME,
        description = "Preview generated Cross Hypervisor DR plan JSON from guided inputs",
        responseObject = DrPlanSpecPreviewResponse.class,
        authorized = {RoleType.Admin})
public class PreviewDrPlanSpecCmd extends BaseCmd {
    public static final String APINAME = "previewDrPlanSpec";

    @Inject
    private DrPlanReadinessValidator drPlanReadinessValidator;
    @Inject
    private DrPlanGuidedSpecBuilder drPlanGuidedSpecBuilder;

    @Parameter(name = "name", type = CommandType.STRING, description = "the DR plan name")
    private String name;

    @Parameter(name = "sourcesiteid", type = CommandType.UUID, entityType = DrSiteResponse.class, required = true,
            description = "the source DR site ID")
    private Long sourceSiteId;

    @Parameter(name = "targetsiteid", type = CommandType.UUID, entityType = DrSiteResponse.class, required = true,
            description = "the target DR site ID")
    private Long targetSiteId;

    @Parameter(name = "sourcevmid", type = CommandType.UUID, entityType = UserVmResponse.class,
            description = "the source virtual machine ID")
    private Long sourceVmId;

    @Parameter(name = "sourceexternalref", type = CommandType.STRING, description = "the source external reference")
    private String sourceExternalRef;

    @Parameter(name = "direction", type = CommandType.STRING, required = true, description = "the DR direction")
    private String direction;

    @Parameter(name = "rposeconds", type = CommandType.INTEGER, description = "the target RPO in seconds")
    private Integer rpoSeconds;

    @Parameter(name = "rtoseconds", type = CommandType.INTEGER, description = "the target RTO in seconds")
    private Integer rtoSeconds;

    @Parameter(name = "sourceworkerhostid", type = CommandType.UUID, entityType = HostResponse.class,
            description = "the source FTCTL_DR worker host ID")
    private Long sourceWorkerHostId;

    @Parameter(name = "targetworkerhostid", type = CommandType.UUID, entityType = HostResponse.class,
            description = "the target FTCTL_DR worker host ID")
    private Long targetWorkerHostId;

    @Parameter(name = "coordinatorworkerhostid", type = CommandType.UUID, entityType = HostResponse.class,
            description = "the coordinator FTCTL_DR worker host ID")
    private Long coordinatorWorkerHostId;

    @Parameter(name = "targetvmname", type = CommandType.STRING, description = "the recovery target VM name")
    private String targetVmName;

    @Parameter(name = "targetzoneid", type = CommandType.STRING, description = "the recovery target Cloud zone reference")
    private String targetZoneId;

    @Parameter(name = "targetstorageref", type = CommandType.STRING, description = "the default recovery target storage reference; disk mappings override it per disk")
    private String targetStorageRef;

    @Parameter(name = "targetcomputeref", type = CommandType.STRING, description = "the recovery target compute reference")
    private String targetComputeRef;

    @Parameter(name = "targetcpunumber", type = CommandType.INTEGER, description = "the recovery target CPU core count for dynamic compute offerings")
    private Integer targetCpuNumber;

    @Parameter(name = "targetcpuspeed", type = CommandType.INTEGER, description = "the recovery target CPU speed in MHz for dynamic compute offerings")
    private Integer targetCpuSpeed;

    @Parameter(name = "targetmemory", type = CommandType.INTEGER, description = "the recovery target memory in MiB for dynamic compute offerings")
    private Integer targetMemory;

    @Parameter(name = "targetboottype", type = CommandType.STRING, description = "the recovery target boot type, BIOS or UEFI")
    private String targetBootType;

    @Parameter(name = "targetbootmode", type = CommandType.STRING, description = "the recovery target boot mode, LEGACY or SECURE")
    private String targetBootMode;

    @Parameter(name = "targetrootdiskcontroller", type = CommandType.STRING, description = "the recovery target root disk controller")
    private String targetRootDiskController;

    @Parameter(name = "targetdatadiskcontroller", type = CommandType.STRING, description = "the recovery target data disk controller")
    private String targetDataDiskController;

    @Parameter(name = "targetiothreadsenabled", type = CommandType.BOOLEAN, description = "whether to enable KVM IOThreads on the recovery target VM")
    private Boolean targetIoThreadsEnabled;

    @Parameter(name = "targetiopolicy", type = CommandType.STRING, description = "the recovery target KVM IO policy; defaults to io_uring")
    private String targetIoPolicy;

    @Parameter(name = "targetnetworkref", type = CommandType.STRING, description = "the recovery target network reference")
    private String targetNetworkRef;

    @Parameter(name = "targetfolderpath", type = CommandType.STRING, description = "the VMware target folder path")
    private String targetFolderPath;

    @Parameter(name = "diskmappingsjson", type = CommandType.STRING, length = 65535, description = "the compact guided disk mapping JSON array; per-disk target storage is authoritative")
    private String diskMappingsJson;

    @Parameter(name = "consistencymode", type = CommandType.STRING, description = "the consistency mode")
    private String consistencyMode;

    @Parameter(name = "testnetworkmode", type = CommandType.STRING, description = "the test failover network mode")
    private String testNetworkMode;

    @Parameter(name = "testbootvalidationmode", type = CommandType.STRING, description = "test failover boot validation mode: POWER_STATE_ONLY or QGA_REQUIRED")
    private String testBootValidationMode;

    @Parameter(name = "testboottimeoutseconds", type = CommandType.INTEGER, description = "test failover boot validation timeout in seconds")
    private Integer testBootTimeoutSeconds;

    @Parameter(name = "failoverpoweron", type = CommandType.BOOLEAN, description = "whether failover should power on the target VM")
    private Boolean failoverPowerOn;

    @Parameter(name = "syncintervalseconds", type = CommandType.INTEGER, description = "the continuous sync interval in seconds")
    private Integer syncIntervalSeconds;

    @Parameter(name = "retentioncount", type = CommandType.INTEGER, description = "the restore point retention count")
    private Integer retentionCount;

    @Parameter(name = "bandwidthlimitmbps", type = CommandType.INTEGER, description = "the optional bandwidth limit in Mbps")
    private Integer bandwidthLimitMbps;

    @Parameter(name = "retrycount", type = CommandType.INTEGER, description = "the retry count")
    private Integer retryCount;

    @Override
    public void execute() {
        DrPlanVO plan = new DrPlanVO(name, sourceSiteId, targetSiteId, direction);
        plan.setSourceVmId(sourceVmId);
        plan.setSourceExternalRef(sourceExternalRef);
        plan.setRpoSeconds(rpoSeconds);
        plan.setRtoSeconds(rtoSeconds);
        plan.setSourceWorkerHostId(sourceWorkerHostId);
        plan.setTargetWorkerHostId(targetWorkerHostId);
        plan.setCoordinatorWorkerHostId(coordinatorWorkerHostId);
        DrPlanGeneratedSpec generated = guidedSpecBuilder().build(plan, buildGuidedSpec());
        plan.setMappingJson(generated.getMappingJson());
        plan.setScheduleJson(generated.getScheduleJson());
        plan.setPolicyJson(generated.getPolicyJson());
        plan.setQuiescePolicyJson(generated.getQuiescePolicyJson());
        DrPlanReadiness readiness = drPlanReadinessValidator != null ? drPlanReadinessValidator.validate(plan) : null;
        DrPlanSpecPreviewResponse response = new DrPlanSpecPreviewResponse();
        response.setObjectName("drplanspecpreview");
        response.setMappingJson(generated.getMappingJson());
        response.setScheduleJson(generated.getScheduleJson());
        response.setPolicyJson(generated.getPolicyJson());
        response.setQuiescePolicyJson(generated.getQuiescePolicyJson());
        populateHardwarePreview(response, generated.getMappingJson());
        response.setWarnings(generated.getWarnings());
        if (readiness != null) {
            response.setReadinessState(readiness.getState());
            response.setExecutionReady(readiness.isExecutionReady());
            response.setReleaseReady(readiness.isReleaseReady());
            response.setBlockingReasons(mergeBlockingReasons(generated, readiness));
        } else {
            response.setBlockingReasons(generated.getBlockingReasons());
        }
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    private void populateHardwarePreview(DrPlanSpecPreviewResponse response, String mappingJson) {
        try {
            JsonObject mapping = JsonParser.parseString(mappingJson).getAsJsonObject();
            JsonObject source = objectAt(mapping, "source");
            JsonObject target = objectAt(mapping, "target");
            JsonObject sourceHardware = objectAt(source, "hardware");
            JsonObject targetHardware = objectAt(target, "hardware");
            response.setSourceHardwareJson(sourceHardware.entrySet().isEmpty() ? null : sourceHardware.toString());
            response.setResolvedTargetHardwareJson(targetHardware.entrySet().isEmpty() ? null : targetHardware.toString());
        } catch (RuntimeException ignored) {
            response.setSourceHardwareJson(null);
            response.setResolvedTargetHardwareJson(null);
        }
    }

    private JsonObject objectAt(JsonObject object, String key) {
        JsonElement element = object != null ? object.get(key) : null;
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private DrPlanGuidedSpec buildGuidedSpec() {
        DrPlanGuidedSpec spec = new DrPlanGuidedSpec();
        spec.setGuidedPlan(true);
        spec.setTargetVmName(targetVmName);
        spec.setTargetZoneId(targetZoneId);
        spec.setTargetStorageRef(targetStorageRef);
        spec.setTargetComputeRef(targetComputeRef);
        spec.setTargetCpuNumber(targetCpuNumber);
        spec.setTargetCpuSpeed(targetCpuSpeed);
        spec.setTargetMemory(targetMemory);
        spec.setTargetBootType(targetBootType);
        spec.setTargetBootMode(targetBootMode);
        spec.setTargetRootDiskController(targetRootDiskController);
        spec.setTargetDataDiskController(targetDataDiskController);
        spec.setTargetIoThreadsEnabled(targetIoThreadsEnabled);
        spec.setTargetIoPolicy(targetIoPolicy);
        spec.setTargetNetworkRef(targetNetworkRef);
        spec.setTargetFolderPath(targetFolderPath);
        spec.setDiskMappingsJson(diskMappingsJson);
        spec.setConsistencyMode(consistencyMode);
        spec.setTestNetworkMode(testNetworkMode);
        spec.setTestBootValidationMode(testBootValidationMode);
        spec.setTestBootTimeoutSeconds(testBootTimeoutSeconds);
        spec.setFailoverPowerOn(failoverPowerOn);
        spec.setSyncIntervalSeconds(syncIntervalSeconds);
        spec.setRetentionCount(retentionCount);
        spec.setBandwidthLimitMbps(bandwidthLimitMbps);
        spec.setRetryCount(retryCount);
        return spec;
    }

    private DrPlanGuidedSpecBuilder guidedSpecBuilder() {
        return drPlanGuidedSpecBuilder != null ? drPlanGuidedSpecBuilder : new DrPlanGuidedSpecBuilder();
    }

    private List<String> mergeBlockingReasons(DrPlanGeneratedSpec generated, DrPlanReadiness readiness) {
        List<String> reasons = new ArrayList<String>();
        addReasons(reasons, generated.getBlockingReasons());
        addReasons(reasons, readiness.getBlockingReasons());
        return reasons;
    }

    private void addReasons(List<String> target, List<String> source) {
        if (target == null || source == null) {
            return;
        }
        for (String reason : source) {
            if (reason != null && !target.contains(reason)) {
                target.add(reason);
            }
        }
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccountId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.DisasterRecoveryCluster;
    }
}
