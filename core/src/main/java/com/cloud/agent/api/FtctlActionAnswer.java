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

public class FtctlActionAnswer extends Answer {

    private FtctlActionCommand.Action action;
    private String ftctlResult;
    private Integer exitCode;
    private String output;

    public FtctlActionAnswer(Command command, boolean success, String details) {
        super(command, success, details);
    }

    public FtctlActionAnswer(Command command, boolean success, String details, FtctlActionCommand.Action action,
                             String ftctlResult, Integer exitCode, String output) {
        super(command, success, details);
        this.action = action;
        this.ftctlResult = ftctlResult;
        this.exitCode = exitCode;
        this.output = output;
    }

    public FtctlActionCommand.Action getAction() {
        return action;
    }

    public String getFtctlResult() {
        return ftctlResult;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getOutput() {
        return output;
    }
}
