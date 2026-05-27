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

public class AblestackN2KStatusAnswer extends Answer {

    private String phase;
    private String migrationState;
    private String migrationStep;
    private String syncPhysical;
    private String workdir;
    private String statusJson;
    private String targetProvider;
    private String cloudVmId;
    private String displayStep;
    private String syncProgressLabel;
    private Long syncDoneBytes;
    private Long syncTotalBytes;
    private Integer syncPercent;
    private Long syncCumulativeDoneBytes;
    private Long syncCumulativeKnownBytes;
    private Integer syncCumulativePercent;

    public AblestackN2KStatusAnswer(Command command, boolean success, String details) {
        super(command, success, details);
    }

    public AblestackN2KStatusAnswer(Command command, boolean success, String details,
                                    String phase, String migrationState, String migrationStep,
                                    String syncPhysical, String workdir, String statusJson) {
        this(command, success, details, phase, migrationState, migrationStep, syncPhysical, workdir, statusJson, null, null);
    }

    public AblestackN2KStatusAnswer(Command command, boolean success, String details,
                                    String phase, String migrationState, String migrationStep,
                                    String syncPhysical, String workdir, String statusJson,
                                    String targetProvider, String cloudVmId) {
        super(command, success, details);
        this.phase = phase;
        this.migrationState = migrationState;
        this.migrationStep = migrationStep;
        this.syncPhysical = syncPhysical;
        this.workdir = workdir;
        this.statusJson = statusJson;
        this.targetProvider = targetProvider;
        this.cloudVmId = cloudVmId;
    }

    public String getPhase() {
        return phase;
    }

    public String getMigrationState() {
        return migrationState;
    }

    public String getMigrationStep() {
        return migrationStep;
    }

    public String getSyncPhysical() {
        return syncPhysical;
    }

    public String getWorkdir() {
        return workdir;
    }

    public String getStatusJson() {
        return statusJson;
    }

    public String getTargetProvider() {
        return targetProvider;
    }

    public String getCloudVmId() {
        return cloudVmId;
    }

    public String getDisplayStep() {
        return displayStep;
    }

    public void setDisplayStep(String displayStep) {
        this.displayStep = displayStep;
    }

    public String getSyncProgressLabel() {
        return syncProgressLabel;
    }

    public void setSyncProgressLabel(String syncProgressLabel) {
        this.syncProgressLabel = syncProgressLabel;
    }

    public Long getSyncDoneBytes() {
        return syncDoneBytes;
    }

    public void setSyncDoneBytes(Long syncDoneBytes) {
        this.syncDoneBytes = syncDoneBytes;
    }

    public Long getSyncTotalBytes() {
        return syncTotalBytes;
    }

    public void setSyncTotalBytes(Long syncTotalBytes) {
        this.syncTotalBytes = syncTotalBytes;
    }

    public Integer getSyncPercent() {
        return syncPercent;
    }

    public void setSyncPercent(Integer syncPercent) {
        this.syncPercent = syncPercent;
    }

    public Long getSyncCumulativeDoneBytes() {
        return syncCumulativeDoneBytes;
    }

    public void setSyncCumulativeDoneBytes(Long syncCumulativeDoneBytes) {
        this.syncCumulativeDoneBytes = syncCumulativeDoneBytes;
    }

    public Long getSyncCumulativeKnownBytes() {
        return syncCumulativeKnownBytes;
    }

    public void setSyncCumulativeKnownBytes(Long syncCumulativeKnownBytes) {
        this.syncCumulativeKnownBytes = syncCumulativeKnownBytes;
    }

    public Integer getSyncCumulativePercent() {
        return syncCumulativePercent;
    }

    public void setSyncCumulativePercent(Integer syncCumulativePercent) {
        this.syncCumulativePercent = syncCumulativePercent;
    }
}
