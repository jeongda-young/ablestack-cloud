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

public class FtctlDrActionAnswer extends Answer {

    private FtctlDrActionCommand.Action action;
    private String planUuid;
    private String runUuid;
    private String ftctlResult;
    private Boolean accepted;
    private String state;
    private String step;
    private Integer progress;
    private String externalJobRef;
    private Long eventsOffset;
    private String errorCode;
    private Integer exitCode;
    private String output;
    private String statusJson;

    public FtctlDrActionAnswer(Command command, boolean success, String details) {
        super(command, success, details);
    }

    public FtctlDrActionAnswer(Command command, boolean success, String details, FtctlDrActionCommand.Action action,
            String planUuid, String runUuid, String ftctlResult, Boolean accepted, String state, String step,
            Integer progress, String externalJobRef, Long eventsOffset, String errorCode, Integer exitCode,
            String output, String statusJson) {
        super(command, success, details);
        this.action = action;
        this.planUuid = planUuid;
        this.runUuid = runUuid;
        this.ftctlResult = ftctlResult;
        this.accepted = accepted;
        this.state = state;
        this.step = step;
        this.progress = progress;
        this.externalJobRef = externalJobRef;
        this.eventsOffset = eventsOffset;
        this.errorCode = errorCode;
        this.exitCode = exitCode;
        this.output = output;
        this.statusJson = statusJson;
    }

    public FtctlDrActionCommand.Action getAction() {
        return action;
    }

    public String getPlanUuid() {
        return planUuid;
    }

    public String getRunUuid() {
        return runUuid;
    }

    public String getFtctlResult() {
        return ftctlResult;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public String getState() {
        return state;
    }

    public String getStep() {
        return step;
    }

    public Integer getProgress() {
        return progress;
    }

    public String getExternalJobRef() {
        return externalJobRef;
    }

    public Long getEventsOffset() {
        return eventsOffset;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getOutput() {
        return output;
    }

    public String getStatusJson() {
        return statusJson;
    }
}
