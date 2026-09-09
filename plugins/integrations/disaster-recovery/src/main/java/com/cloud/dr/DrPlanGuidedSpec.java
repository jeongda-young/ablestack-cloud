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

import org.apache.commons.lang3.StringUtils;

public class DrPlanGuidedSpec {
    private Boolean guidedPlan;
    private String targetVmName;
    private String targetZoneId;
    private String targetStorageRef;
    private String targetComputeRef;
    private Integer targetCpuNumber;
    private Integer targetCpuSpeed;
    private Integer targetMemory;
    private String targetBootType;
    private String targetBootMode;
    private String targetRootDiskController;
    private String targetDataDiskController;
    private Boolean targetIoThreadsEnabled;
    private String targetIoPolicy;
    private String targetNetworkRef;
    private String targetFolderPath;
    private String diskMappingsJson;
    private String consistencyMode;
    private String testNetworkMode;
    private String testBootValidationMode;
    private Integer testBootTimeoutSeconds;
    private Boolean failoverPowerOn;
    private Integer syncIntervalSeconds;
    private Integer retentionCount;
    private Integer bandwidthLimitMbps;
    private Integer retryCount;

    public Boolean getGuidedPlan() {
        return guidedPlan;
    }

    public void setGuidedPlan(Boolean guidedPlan) {
        this.guidedPlan = guidedPlan;
    }

    public String getTargetVmName() {
        return targetVmName;
    }

    public void setTargetVmName(String targetVmName) {
        this.targetVmName = targetVmName;
    }

    public String getTargetZoneId() {
        return targetZoneId;
    }

    public void setTargetZoneId(String targetZoneId) {
        this.targetZoneId = targetZoneId;
    }

    public String getTargetStorageRef() {
        return targetStorageRef;
    }

    public void setTargetStorageRef(String targetStorageRef) {
        this.targetStorageRef = targetStorageRef;
    }

    public String getTargetComputeRef() {
        return targetComputeRef;
    }

    public void setTargetComputeRef(String targetComputeRef) {
        this.targetComputeRef = targetComputeRef;
    }

    public Integer getTargetCpuNumber() {
        return targetCpuNumber;
    }

    public void setTargetCpuNumber(Integer targetCpuNumber) {
        this.targetCpuNumber = targetCpuNumber;
    }

    public Integer getTargetCpuSpeed() {
        return targetCpuSpeed;
    }

    public void setTargetCpuSpeed(Integer targetCpuSpeed) {
        this.targetCpuSpeed = targetCpuSpeed;
    }

    public Integer getTargetMemory() {
        return targetMemory;
    }

    public void setTargetMemory(Integer targetMemory) {
        this.targetMemory = targetMemory;
    }

    public String getTargetBootType() {
        return targetBootType;
    }

    public void setTargetBootType(String targetBootType) {
        this.targetBootType = targetBootType;
    }

    public String getTargetBootMode() {
        return targetBootMode;
    }

    public void setTargetBootMode(String targetBootMode) {
        this.targetBootMode = targetBootMode;
    }

    public String getTargetRootDiskController() {
        return targetRootDiskController;
    }

    public void setTargetRootDiskController(String targetRootDiskController) {
        this.targetRootDiskController = targetRootDiskController;
    }

    public String getTargetDataDiskController() {
        return targetDataDiskController;
    }

    public void setTargetDataDiskController(String targetDataDiskController) {
        this.targetDataDiskController = targetDataDiskController;
    }

    public Boolean getTargetIoThreadsEnabled() {
        return targetIoThreadsEnabled;
    }

    public void setTargetIoThreadsEnabled(Boolean targetIoThreadsEnabled) {
        this.targetIoThreadsEnabled = targetIoThreadsEnabled;
    }

    public String getTargetIoPolicy() {
        return targetIoPolicy;
    }

    public void setTargetIoPolicy(String targetIoPolicy) {
        this.targetIoPolicy = targetIoPolicy;
    }

    public String getTargetNetworkRef() {
        return targetNetworkRef;
    }

    public void setTargetNetworkRef(String targetNetworkRef) {
        this.targetNetworkRef = targetNetworkRef;
    }

    public String getTargetFolderPath() {
        return targetFolderPath;
    }

    public void setTargetFolderPath(String targetFolderPath) {
        this.targetFolderPath = targetFolderPath;
    }

    public String getDiskMappingsJson() {
        return diskMappingsJson;
    }

    public void setDiskMappingsJson(String diskMappingsJson) {
        this.diskMappingsJson = diskMappingsJson;
    }

    public String getConsistencyMode() {
        return consistencyMode;
    }

    public void setConsistencyMode(String consistencyMode) {
        this.consistencyMode = consistencyMode;
    }

    public String getTestNetworkMode() {
        return testNetworkMode;
    }

    public void setTestNetworkMode(String testNetworkMode) {
        this.testNetworkMode = testNetworkMode;
    }

    public String getTestBootValidationMode() {
        return testBootValidationMode;
    }

    public void setTestBootValidationMode(String testBootValidationMode) {
        this.testBootValidationMode = testBootValidationMode;
    }

    public Integer getTestBootTimeoutSeconds() {
        return testBootTimeoutSeconds;
    }

    public void setTestBootTimeoutSeconds(Integer testBootTimeoutSeconds) {
        this.testBootTimeoutSeconds = testBootTimeoutSeconds;
    }

    public Boolean getFailoverPowerOn() {
        return failoverPowerOn;
    }

    public void setFailoverPowerOn(Boolean failoverPowerOn) {
        this.failoverPowerOn = failoverPowerOn;
    }

    public Integer getSyncIntervalSeconds() {
        return syncIntervalSeconds;
    }

    public void setSyncIntervalSeconds(Integer syncIntervalSeconds) {
        this.syncIntervalSeconds = syncIntervalSeconds;
    }

    public Integer getRetentionCount() {
        return retentionCount;
    }

    public void setRetentionCount(Integer retentionCount) {
        this.retentionCount = retentionCount;
    }

    public Integer getBandwidthLimitMbps() {
        return bandwidthLimitMbps;
    }

    public void setBandwidthLimitMbps(Integer bandwidthLimitMbps) {
        this.bandwidthLimitMbps = bandwidthLimitMbps;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public boolean shouldApply() {
        return Boolean.TRUE.equals(guidedPlan)
                || StringUtils.isNotBlank(targetVmName)
                || StringUtils.isNotBlank(targetZoneId)
                || StringUtils.isNotBlank(targetStorageRef)
                || StringUtils.isNotBlank(targetComputeRef)
                || targetCpuNumber != null
                || targetCpuSpeed != null
                || targetMemory != null
                || StringUtils.isNotBlank(targetBootType)
                || StringUtils.isNotBlank(targetBootMode)
                || StringUtils.isNotBlank(targetRootDiskController)
                || StringUtils.isNotBlank(targetDataDiskController)
                || targetIoThreadsEnabled != null
                || StringUtils.isNotBlank(targetIoPolicy)
                || StringUtils.isNotBlank(targetNetworkRef)
                || StringUtils.isNotBlank(targetFolderPath)
                || StringUtils.isNotBlank(diskMappingsJson)
                || StringUtils.isNotBlank(consistencyMode)
                || StringUtils.isNotBlank(testNetworkMode)
                || StringUtils.isNotBlank(testBootValidationMode)
                || testBootTimeoutSeconds != null
                || failoverPowerOn != null
                || syncIntervalSeconds != null
                || retentionCount != null
                || bandwidthLimitMbps != null
                || retryCount != null;
    }
}
