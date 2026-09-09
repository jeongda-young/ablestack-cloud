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
package com.cloud.dr.inventory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class DrPlanInventoryResult {
    private String sourceSiteId;
    private String targetSiteId;
    private String direction;
    private String healthState;
    private String reasonCode;
    private String message;
    private Long latencyMs;
    private Date checkedAt;
    private DrInventoryOption targetZone;
    private Map<String, String> sourceHardware = new LinkedHashMap<String, String>();
    private List<DrInventoryOption> sourceWorkloads = new ArrayList<DrInventoryOption>();
    private List<DrInventoryOption> sourceDisks = new ArrayList<DrInventoryOption>();
    private List<DrInventoryOption> sourceNics = new ArrayList<DrInventoryOption>();
    private List<DrInventoryOption> sourceWorkerHosts = new ArrayList<DrInventoryOption>();
    private List<DrInventoryOption> targetWorkerHosts = new ArrayList<DrInventoryOption>();
    private List<DrInventoryOption> coordinatorWorkerHosts = new ArrayList<DrInventoryOption>();
    private List<DrInventoryOption> targetStorageOptions = new ArrayList<DrInventoryOption>();
    private List<DrInventoryOption> targetComputeOptions = new ArrayList<DrInventoryOption>();
    private List<DrInventoryOption> targetServiceOfferings = new ArrayList<DrInventoryOption>();
    private List<DrInventoryOption> targetDiskOfferings = new ArrayList<DrInventoryOption>();
    private List<DrInventoryOption> targetNetworkOptions = new ArrayList<DrInventoryOption>();
    private List<DrInventoryOption> targetFolderOptions = new ArrayList<DrInventoryOption>();
    private List<String> blockingReasons = new ArrayList<String>();
    private List<String> warnings = new ArrayList<String>();

    public String getSourceSiteId() {
        return sourceSiteId;
    }

    public void setSourceSiteId(String sourceSiteId) {
        this.sourceSiteId = sourceSiteId;
    }

    public String getTargetSiteId() {
        return targetSiteId;
    }

    public void setTargetSiteId(String targetSiteId) {
        this.targetSiteId = targetSiteId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getHealthState() {
        return healthState;
    }

    public void setHealthState(String healthState) {
        this.healthState = healthState;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Date getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Date checkedAt) {
        this.checkedAt = checkedAt;
    }

    public DrInventoryOption getTargetZone() {
        return targetZone;
    }

    public void setTargetZone(DrInventoryOption targetZone) {
        this.targetZone = targetZone;
    }

    public Map<String, String> getSourceHardware() {
        return sourceHardware;
    }

    public void setSourceHardware(Map<String, String> sourceHardware) {
        this.sourceHardware = sourceHardware == null ? new LinkedHashMap<String, String>() : sourceHardware;
    }

    public List<DrInventoryOption> getSourceWorkloads() {
        return sourceWorkloads;
    }

    public void setSourceWorkloads(List<DrInventoryOption> sourceWorkloads) {
        this.sourceWorkloads = sourceWorkloads;
    }

    public List<DrInventoryOption> getSourceDisks() {
        return sourceDisks;
    }

    public void setSourceDisks(List<DrInventoryOption> sourceDisks) {
        this.sourceDisks = sourceDisks;
    }

    public List<DrInventoryOption> getSourceNics() {
        return sourceNics;
    }

    public void setSourceNics(List<DrInventoryOption> sourceNics) {
        this.sourceNics = sourceNics;
    }

    public List<DrInventoryOption> getSourceWorkerHosts() {
        return sourceWorkerHosts;
    }

    public void setSourceWorkerHosts(List<DrInventoryOption> sourceWorkerHosts) {
        this.sourceWorkerHosts = sourceWorkerHosts;
    }

    public List<DrInventoryOption> getTargetWorkerHosts() {
        return targetWorkerHosts;
    }

    public void setTargetWorkerHosts(List<DrInventoryOption> targetWorkerHosts) {
        this.targetWorkerHosts = targetWorkerHosts;
    }

    public List<DrInventoryOption> getCoordinatorWorkerHosts() {
        return coordinatorWorkerHosts;
    }

    public void setCoordinatorWorkerHosts(List<DrInventoryOption> coordinatorWorkerHosts) {
        this.coordinatorWorkerHosts = coordinatorWorkerHosts;
    }

    public List<DrInventoryOption> getTargetStorageOptions() {
        return targetStorageOptions;
    }

    public void setTargetStorageOptions(List<DrInventoryOption> targetStorageOptions) {
        this.targetStorageOptions = targetStorageOptions;
    }

    public List<DrInventoryOption> getTargetComputeOptions() {
        return targetComputeOptions;
    }

    public void setTargetComputeOptions(List<DrInventoryOption> targetComputeOptions) {
        this.targetComputeOptions = targetComputeOptions;
    }

    public List<DrInventoryOption> getTargetServiceOfferings() {
        return targetServiceOfferings;
    }

    public void setTargetServiceOfferings(List<DrInventoryOption> targetServiceOfferings) {
        this.targetServiceOfferings = targetServiceOfferings;
    }

    public List<DrInventoryOption> getTargetDiskOfferings() {
        return targetDiskOfferings;
    }

    public void setTargetDiskOfferings(List<DrInventoryOption> targetDiskOfferings) {
        this.targetDiskOfferings = targetDiskOfferings;
    }

    public List<DrInventoryOption> getTargetNetworkOptions() {
        return targetNetworkOptions;
    }

    public void setTargetNetworkOptions(List<DrInventoryOption> targetNetworkOptions) {
        this.targetNetworkOptions = targetNetworkOptions;
    }

    public List<DrInventoryOption> getTargetFolderOptions() {
        return targetFolderOptions;
    }

    public void setTargetFolderOptions(List<DrInventoryOption> targetFolderOptions) {
        this.targetFolderOptions = targetFolderOptions;
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons;
    }

    public void addBlockingReason(String blockingReason) {
        this.blockingReasons.add(blockingReason);
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
