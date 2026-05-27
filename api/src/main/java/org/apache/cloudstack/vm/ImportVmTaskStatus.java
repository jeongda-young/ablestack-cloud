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
package org.apache.cloudstack.vm;

public class ImportVmTaskStatus {

    private final String currentPhase;
    private final String migrationState;
    private final String migrationStep;
    private final String workdir;
    private final String syncPhysical;
    private final String displayStep;
    private final String syncProgressLabel;
    private final Long syncDoneBytes;
    private final Long syncTotalBytes;
    private final Integer syncPercent;
    private final Long syncCumulativeDoneBytes;
    private final Long syncCumulativeKnownBytes;
    private final Integer syncCumulativePercent;

    public ImportVmTaskStatus(String currentPhase, String migrationState, String migrationStep, String workdir, String syncPhysical) {
        this(currentPhase, migrationState, migrationStep, workdir, syncPhysical, null, null, null, null, null, null, null, null);
    }

    public ImportVmTaskStatus(String currentPhase, String migrationState, String migrationStep, String workdir, String syncPhysical,
                              String displayStep, String syncProgressLabel, Long syncDoneBytes, Long syncTotalBytes, Integer syncPercent,
                              Long syncCumulativeDoneBytes, Long syncCumulativeKnownBytes, Integer syncCumulativePercent) {
        this.currentPhase = currentPhase;
        this.migrationState = migrationState;
        this.migrationStep = migrationStep;
        this.workdir = workdir;
        this.syncPhysical = syncPhysical;
        this.displayStep = displayStep;
        this.syncProgressLabel = syncProgressLabel;
        this.syncDoneBytes = syncDoneBytes;
        this.syncTotalBytes = syncTotalBytes;
        this.syncPercent = syncPercent;
        this.syncCumulativeDoneBytes = syncCumulativeDoneBytes;
        this.syncCumulativeKnownBytes = syncCumulativeKnownBytes;
        this.syncCumulativePercent = syncCumulativePercent;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public String getMigrationState() {
        return migrationState;
    }

    public String getMigrationStep() {
        return migrationStep;
    }

    public String getWorkdir() {
        return workdir;
    }

    public String getSyncPhysical() {
        return syncPhysical;
    }

    public String getDisplayStep() {
        return displayStep;
    }

    public String getSyncProgressLabel() {
        return syncProgressLabel;
    }

    public Long getSyncDoneBytes() {
        return syncDoneBytes;
    }

    public Long getSyncTotalBytes() {
        return syncTotalBytes;
    }

    public Integer getSyncPercent() {
        return syncPercent;
    }

    public Long getSyncCumulativeDoneBytes() {
        return syncCumulativeDoneBytes;
    }

    public Long getSyncCumulativeKnownBytes() {
        return syncCumulativeKnownBytes;
    }

    public Integer getSyncCumulativePercent() {
        return syncCumulativePercent;
    }
}
