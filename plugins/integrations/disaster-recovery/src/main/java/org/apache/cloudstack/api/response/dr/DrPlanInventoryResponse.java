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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DrPlanInventoryResponse extends BaseResponse {
    @SerializedName("sourcesiteid")
    @Param(description = "the source DR site ID")
    private String sourceSiteId;

    @SerializedName("targetsiteid")
    @Param(description = "the target DR site ID")
    private String targetSiteId;

    @SerializedName("direction")
    @Param(description = "the inferred DR direction")
    private String direction;

    @SerializedName("healthstate")
    @Param(description = "the inventory discovery state")
    private String healthState;

    @SerializedName("reasoncode")
    @Param(description = "the inventory discovery reason code")
    private String reasonCode;

    @SerializedName("message")
    @Param(description = "the inventory discovery message")
    private String message;

    @SerializedName("latencyms")
    @Param(description = "the inventory discovery latency in milliseconds")
    private Long latencyMs;

    @SerializedName("checkedat")
    @Param(description = "the inventory discovery time")
    private Date checkedAt;

    @SerializedName("targetzone")
    @Param(description = "the target site zone option")
    private DrInventoryOptionResponse targetZone;

    @SerializedName("sourcehardware")
    @Param(description = "the selected source VM hardware discovered by the backend")
    private Map<String, String> sourceHardware;

    @SerializedName("sourceworkloads")
    @Param(description = "the source workload options")
    private List<DrInventoryOptionResponse> sourceWorkloads;

    @SerializedName("sourcedisks")
    @Param(description = "the selected source workload disk options")
    private List<DrInventoryOptionResponse> sourceDisks;

    @SerializedName("sourcenics")
    @Param(description = "the selected source workload NIC options")
    private List<DrInventoryOptionResponse> sourceNics;

    @SerializedName("sourceworkerhosts")
    @Param(description = "the source worker host options")
    private List<DrInventoryOptionResponse> sourceWorkerHosts;

    @SerializedName("targetworkerhosts")
    @Param(description = "the target worker host options")
    private List<DrInventoryOptionResponse> targetWorkerHosts;

    @SerializedName("coordinatorworkerhosts")
    @Param(description = "the coordinator worker host options")
    private List<DrInventoryOptionResponse> coordinatorWorkerHosts;

    @SerializedName("targetstorageoptions")
    @Param(description = "the target storage options")
    private List<DrInventoryOptionResponse> targetStorageOptions;

    @SerializedName("targetcomputeoptions")
    @Param(description = "the target compute options")
    private List<DrInventoryOptionResponse> targetComputeOptions;

    @SerializedName("targetserviceofferings")
    @Param(description = "the target service offering options")
    private List<DrInventoryOptionResponse> targetServiceOfferings;

    @SerializedName("targetdiskofferings")
    @Param(description = "the target disk offering options")
    private List<DrInventoryOptionResponse> targetDiskOfferings;

    @SerializedName("targetnetworkoptions")
    @Param(description = "the target network options")
    private List<DrInventoryOptionResponse> targetNetworkOptions;

    @SerializedName("targetfolderoptions")
    @Param(description = "the target folder options")
    private List<DrInventoryOptionResponse> targetFolderOptions;

    @SerializedName("blockingreasons")
    @Param(description = "the inventory blocking reasons")
    private List<String> blockingReasons;

    @SerializedName("warnings")
    @Param(description = "the inventory warnings")
    private List<String> warnings;

    public void setSourceSiteId(String sourceSiteId) {
        this.sourceSiteId = sourceSiteId;
    }

    public void setTargetSiteId(String targetSiteId) {
        this.targetSiteId = targetSiteId;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public void setHealthState(String healthState) {
        this.healthState = healthState;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public void setCheckedAt(Date checkedAt) {
        this.checkedAt = checkedAt;
    }

    public void setTargetZone(DrInventoryOptionResponse targetZone) {
        this.targetZone = targetZone;
    }

    public void setSourceHardware(Map<String, String> sourceHardware) {
        this.sourceHardware = sourceHardware;
    }

    public void setSourceWorkloads(List<DrInventoryOptionResponse> sourceWorkloads) {
        this.sourceWorkloads = sourceWorkloads;
    }

    public void setSourceDisks(List<DrInventoryOptionResponse> sourceDisks) {
        this.sourceDisks = sourceDisks;
    }

    public void setSourceNics(List<DrInventoryOptionResponse> sourceNics) {
        this.sourceNics = sourceNics;
    }

    public void setSourceWorkerHosts(List<DrInventoryOptionResponse> sourceWorkerHosts) {
        this.sourceWorkerHosts = sourceWorkerHosts;
    }

    public void setTargetWorkerHosts(List<DrInventoryOptionResponse> targetWorkerHosts) {
        this.targetWorkerHosts = targetWorkerHosts;
    }

    public void setCoordinatorWorkerHosts(List<DrInventoryOptionResponse> coordinatorWorkerHosts) {
        this.coordinatorWorkerHosts = coordinatorWorkerHosts;
    }

    public void setTargetStorageOptions(List<DrInventoryOptionResponse> targetStorageOptions) {
        this.targetStorageOptions = targetStorageOptions;
    }

    public void setTargetComputeOptions(List<DrInventoryOptionResponse> targetComputeOptions) {
        this.targetComputeOptions = targetComputeOptions;
    }

    public void setTargetServiceOfferings(List<DrInventoryOptionResponse> targetServiceOfferings) {
        this.targetServiceOfferings = targetServiceOfferings;
    }

    public void setTargetDiskOfferings(List<DrInventoryOptionResponse> targetDiskOfferings) {
        this.targetDiskOfferings = targetDiskOfferings;
    }

    public void setTargetNetworkOptions(List<DrInventoryOptionResponse> targetNetworkOptions) {
        this.targetNetworkOptions = targetNetworkOptions;
    }

    public void setTargetFolderOptions(List<DrInventoryOptionResponse> targetFolderOptions) {
        this.targetFolderOptions = targetFolderOptions;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
