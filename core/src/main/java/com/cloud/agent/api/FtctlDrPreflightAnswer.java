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

public class FtctlDrPreflightAnswer extends Answer {

    private String planUuid;
    private String ftctlResult;
    private Boolean capable;
    private String errorCode;
    private Integer exitCode;
    private String output;
    private String statusJson;

    public FtctlDrPreflightAnswer(Command command, boolean success, String details) {
        super(command, success, details);
    }

    public FtctlDrPreflightAnswer(Command command, boolean success, String details, String planUuid, String ftctlResult,
            Boolean capable, String errorCode, Integer exitCode, String output, String statusJson) {
        super(command, success, details);
        this.planUuid = planUuid;
        this.ftctlResult = ftctlResult;
        this.capable = capable;
        this.errorCode = errorCode;
        this.exitCode = exitCode;
        this.output = output;
        this.statusJson = statusJson;
    }

    public String getPlanUuid() {
        return planUuid;
    }

    public String getFtctlResult() {
        return ftctlResult;
    }

    public Boolean getCapable() {
        return capable;
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
