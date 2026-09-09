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

public class FtctlDrStatusCommand extends Command {

    public static final int TRANSITION_PREFLIGHT_SCHEMA_VERSION = 2;
    public static final String TRANSITION_PREFLIGHT_CONTRACT_VERSION = "dr-transition-preflight-v2";

    public enum StatusScope {
        PLAN_AUTHORITY,
        OPERATION,
        TRANSITION_PREFLIGHT
    }

    private String planUuid;
    private String runUuid;
    private String sourceVmUuid;
    private Long eventsOffset;
    private StatusScope statusScope;
    private String transitionOperation;
    private String expectedAuthoritySide;
    private Long expectedAuthorityGeneration;

    public FtctlDrStatusCommand() {
    }

    public FtctlDrStatusCommand(String planUuid, String runUuid) {
        this(planUuid, runUuid, runUuid == null || runUuid.isEmpty()
                ? StatusScope.PLAN_AUTHORITY : StatusScope.OPERATION);
    }

    public FtctlDrStatusCommand(String planUuid, String runUuid, StatusScope statusScope) {
        this.planUuid = planUuid;
        this.runUuid = runUuid;
        this.statusScope = statusScope;
    }

    public String getPlanUuid() {
        return planUuid;
    }

    public String getRunUuid() {
        return runUuid;
    }

    public String getSourceVmUuid() {
        return sourceVmUuid;
    }

    public void setSourceVmUuid(String sourceVmUuid) {
        this.sourceVmUuid = sourceVmUuid;
    }

    public Long getEventsOffset() {
        return eventsOffset;
    }

    public StatusScope getStatusScope() {
        return statusScope != null ? statusScope
                : (runUuid == null || runUuid.isEmpty() ? StatusScope.PLAN_AUTHORITY : StatusScope.OPERATION);
    }

    public void setEventsOffset(Long eventsOffset) {
        this.eventsOffset = eventsOffset;
    }

    public String getTransitionOperation() {
        return transitionOperation;
    }

    public void setTransitionOperation(String transitionOperation) {
        this.transitionOperation = transitionOperation;
    }

    public String getExpectedAuthoritySide() {
        return expectedAuthoritySide;
    }

    public void setExpectedAuthoritySide(String expectedAuthoritySide) {
        this.expectedAuthoritySide = expectedAuthoritySide;
    }

    public Long getExpectedAuthorityGeneration() {
        return expectedAuthorityGeneration;
    }

    public void setExpectedAuthorityGeneration(Long expectedAuthorityGeneration) {
        this.expectedAuthorityGeneration = expectedAuthorityGeneration;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
