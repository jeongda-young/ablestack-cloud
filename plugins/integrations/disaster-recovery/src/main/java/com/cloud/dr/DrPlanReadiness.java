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

import java.util.ArrayList;
import java.util.List;

public class DrPlanReadiness {
    public static final String STATE_CONFIG_INCOMPLETE = "CONFIG_INCOMPLETE";
    public static final String STATE_EXECUTION_READY = "EXECUTION_READY";
    public static final String STATE_ENGINE_ACCEPTED = "ENGINE_ACCEPTED";
    public static final String STATE_TARGET_MATERIALIZING = "TARGET_MATERIALIZING";
    public static final String STATE_TARGET_READY = "TARGET_READY";
    public static final String STATE_DEGRADED = "DEGRADED";
    public static final String STATE_RUNTIME_ACTIVE = "RUNTIME_ACTIVE";
    public static final String STATE_RELEASE_READY = "RELEASE_READY";

    private String state = STATE_CONFIG_INCOMPLETE;
    private boolean executionReady;
    private boolean releaseReady;
    private boolean engineAccepted;
    private boolean targetMaterialized;
    private boolean targetVmPresent;
    private boolean targetStoragePresent;
    private boolean targetNetworkPresent;
    private boolean restorePointPresent;
    private boolean durableCheckpointPresent;
    private String reasonCode;
    private String message;
    private List<String> blockingReasons = new ArrayList<String>();
    private List<String> warnings = new ArrayList<String>();

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isExecutionReady() {
        return executionReady;
    }

    public void setExecutionReady(boolean executionReady) {
        this.executionReady = executionReady;
    }

    public boolean isReleaseReady() {
        return releaseReady;
    }

    public void setReleaseReady(boolean releaseReady) {
        this.releaseReady = releaseReady;
    }

    public boolean isEngineAccepted() {
        return engineAccepted;
    }

    public void setEngineAccepted(boolean engineAccepted) {
        this.engineAccepted = engineAccepted;
    }

    public boolean isTargetMaterialized() {
        return targetMaterialized;
    }

    public void setTargetMaterialized(boolean targetMaterialized) {
        this.targetMaterialized = targetMaterialized;
    }

    public boolean isTargetVmPresent() {
        return targetVmPresent;
    }

    public void setTargetVmPresent(boolean targetVmPresent) {
        this.targetVmPresent = targetVmPresent;
    }

    public boolean isTargetStoragePresent() {
        return targetStoragePresent;
    }

    public void setTargetStoragePresent(boolean targetStoragePresent) {
        this.targetStoragePresent = targetStoragePresent;
    }

    public boolean isTargetNetworkPresent() {
        return targetNetworkPresent;
    }

    public void setTargetNetworkPresent(boolean targetNetworkPresent) {
        this.targetNetworkPresent = targetNetworkPresent;
    }

    public boolean isRestorePointPresent() {
        return restorePointPresent;
    }

    public void setRestorePointPresent(boolean restorePointPresent) {
        this.restorePointPresent = restorePointPresent;
    }

    public boolean isDurableCheckpointPresent() {
        return durableCheckpointPresent;
    }

    public void setDurableCheckpointPresent(boolean durableCheckpointPresent) {
        this.durableCheckpointPresent = durableCheckpointPresent;
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

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public void addBlockingReason(String reason) {
        if (reason != null && !blockingReasons.contains(reason)) {
            blockingReasons.add(reason);
        }
    }

    public void addWarning(String warning) {
        if (warning != null && !warnings.contains(warning)) {
            warnings.add(warning);
        }
    }
}
