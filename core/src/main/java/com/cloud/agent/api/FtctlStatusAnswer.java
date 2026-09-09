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

public class FtctlStatusAnswer extends Answer {

    private String ftctlResult;
    private String vmName;
    private String mode;
    private String protectionState;
    private String transportState;
    private String activeSide;
    private String adminState;
    private String fencingState;
    private String lastError;
    private String lastReconcileTs;
    private Integer rearmCount;
    private Integer failoverCount;
    private Double syncProgressPercent;
    private Long syncCopiedBytes;
    private Long syncTotalBytes;
    private Boolean syncReady;
    private String syncDirection;
    private String syncUpdated;
    private String syncProgressJson;

    public FtctlStatusAnswer(Command command, boolean success, String details) {
        super(command, success, details);
    }

    public FtctlStatusAnswer(Command command, boolean success, String details, String ftctlResult, String vmName,
                             String mode, String protectionState, String transportState, String activeSide,
                             String adminState, String fencingState, String lastError, String lastReconcileTs,
                             Integer rearmCount, Integer failoverCount) {
        this(command, success, details, ftctlResult, vmName, mode, protectionState, transportState, activeSide,
                adminState, fencingState, lastError, lastReconcileTs, rearmCount, failoverCount,
                null, null, null, null, null, null, null);
    }

    public FtctlStatusAnswer(Command command, boolean success, String details, String ftctlResult, String vmName,
                             String mode, String protectionState, String transportState, String activeSide,
                             String adminState, String fencingState, String lastError, String lastReconcileTs,
                             Integer rearmCount, Integer failoverCount, Double syncProgressPercent,
                             Long syncCopiedBytes, Long syncTotalBytes, Boolean syncReady, String syncDirection,
                             String syncUpdated, String syncProgressJson) {
        super(command, success, details);
        this.ftctlResult = ftctlResult;
        this.vmName = vmName;
        this.mode = mode;
        this.protectionState = protectionState;
        this.transportState = transportState;
        this.activeSide = activeSide;
        this.adminState = adminState;
        this.fencingState = fencingState;
        this.lastError = lastError;
        this.lastReconcileTs = lastReconcileTs;
        this.rearmCount = rearmCount;
        this.failoverCount = failoverCount;
        this.syncProgressPercent = syncProgressPercent;
        this.syncCopiedBytes = syncCopiedBytes;
        this.syncTotalBytes = syncTotalBytes;
        this.syncReady = syncReady;
        this.syncDirection = syncDirection;
        this.syncUpdated = syncUpdated;
        this.syncProgressJson = syncProgressJson;
    }

    public String getFtctlResult() {
        return ftctlResult;
    }

    public String getVmName() {
        return vmName;
    }

    public String getMode() {
        return mode;
    }

    public String getProtectionState() {
        return protectionState;
    }

    public String getTransportState() {
        return transportState;
    }

    public String getActiveSide() {
        return activeSide;
    }

    public String getAdminState() {
        return adminState;
    }

    public String getFencingState() {
        return fencingState;
    }

    public String getLastError() {
        return lastError;
    }

    public String getLastReconcileTs() {
        return lastReconcileTs;
    }

    public Integer getRearmCount() {
        return rearmCount;
    }

    public Integer getFailoverCount() {
        return failoverCount;
    }

    public Double getSyncProgressPercent() {
        return syncProgressPercent;
    }

    public Long getSyncCopiedBytes() {
        return syncCopiedBytes;
    }

    public Long getSyncTotalBytes() {
        return syncTotalBytes;
    }

    public Boolean getSyncReady() {
        return syncReady;
    }

    public String getSyncDirection() {
        return syncDirection;
    }

    public String getSyncUpdated() {
        return syncUpdated;
    }

    public String getSyncProgressJson() {
        return syncProgressJson;
    }
}
