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

import java.util.ArrayList;
import java.util.List;

public class FtctlDrCapabilitiesCommand extends Command {
    private String planUuid;
    private String runUuid;
    private List<String> requiredActions = new ArrayList<String>();
    private List<String> requiredCliCommands = new ArrayList<String>();
    private List<String> requiredFeatures = new ArrayList<String>();

    public FtctlDrCapabilitiesCommand() {
    }

    public FtctlDrCapabilitiesCommand(String planUuid, String runUuid) {
        this.planUuid = planUuid;
        this.runUuid = runUuid;
    }

    public String getPlanUuid() {
        return planUuid;
    }

    public void setPlanUuid(String planUuid) {
        this.planUuid = planUuid;
    }

    public String getRunUuid() {
        return runUuid;
    }

    public void setRunUuid(String runUuid) {
        this.runUuid = runUuid;
    }

    public List<String> getRequiredActions() {
        return requiredActions;
    }

    public void setRequiredActions(List<String> requiredActions) {
        this.requiredActions = requiredActions == null ? new ArrayList<String>() : requiredActions;
    }

    public List<String> getRequiredCliCommands() {
        return requiredCliCommands;
    }

    public void setRequiredCliCommands(List<String> requiredCliCommands) {
        this.requiredCliCommands = requiredCliCommands == null ? new ArrayList<String>() : requiredCliCommands;
    }

    public List<String> getRequiredFeatures() { return requiredFeatures; }
    public void setRequiredFeatures(List<String> values) {
        requiredFeatures = values == null ? new ArrayList<String>() : values;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
