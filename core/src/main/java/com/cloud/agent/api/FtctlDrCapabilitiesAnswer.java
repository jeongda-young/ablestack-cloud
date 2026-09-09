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

public class FtctlDrCapabilitiesAnswer extends Answer {
    private String planUuid;
    private String runUuid;
    private List<String> supportedActions = new ArrayList<String>();
    private List<String> supportedCliCommands = new ArrayList<String>();
    private List<String> missingActions = new ArrayList<String>();
    private List<String> missingCliCommands = new ArrayList<String>();
    private List<String> supportedFeatures = new ArrayList<String>();
    private List<String> missingFeatures = new ArrayList<String>();
    private List<String> reprotectAuthorityContractVersions = new ArrayList<String>();
    private String ftctlVersion;
    private String runtimeSchemaVersion;
    private String actionContractVersion;
    private String actionCommandCodeSource;
    private String wrapperCodeSource;
    private String capabilitiesJson;

    public FtctlDrCapabilitiesAnswer(Command command, boolean success, String details) {
        super(command, success, details);
    }

    public FtctlDrCapabilitiesAnswer(Command command, boolean success, String details, String planUuid, String runUuid,
            List<String> supportedActions, List<String> supportedCliCommands, List<String> missingActions,
            List<String> missingCliCommands, String ftctlVersion, String runtimeSchemaVersion, String capabilitiesJson) {
        super(command, success, details);
        this.planUuid = planUuid;
        this.runUuid = runUuid;
        this.supportedActions = supportedActions == null ? new ArrayList<String>() : supportedActions;
        this.supportedCliCommands = supportedCliCommands == null ? new ArrayList<String>() : supportedCliCommands;
        this.missingActions = missingActions == null ? new ArrayList<String>() : missingActions;
        this.missingCliCommands = missingCliCommands == null ? new ArrayList<String>() : missingCliCommands;
        this.ftctlVersion = ftctlVersion;
        this.runtimeSchemaVersion = runtimeSchemaVersion;
        this.capabilitiesJson = capabilitiesJson;
    }

    public String getPlanUuid() {
        return planUuid;
    }

    public String getRunUuid() {
        return runUuid;
    }

    public List<String> getSupportedActions() {
        return supportedActions;
    }

    public void setSupportedActions(List<String> values) {
        supportedActions = values == null ? new ArrayList<String>() : values;
    }

    public List<String> getSupportedCliCommands() {
        return supportedCliCommands;
    }

    public void setSupportedCliCommands(List<String> values) {
        supportedCliCommands = values == null ? new ArrayList<String>() : values;
    }

    public List<String> getMissingActions() {
        return missingActions;
    }

    public List<String> getMissingCliCommands() {
        return missingCliCommands;
    }

    public List<String> getSupportedFeatures() {
        return supportedFeatures;
    }

    public void setSupportedFeatures(List<String> supportedFeatures) {
        this.supportedFeatures = supportedFeatures == null ? new ArrayList<String>() : supportedFeatures;
    }

    public List<String> getMissingFeatures() { return missingFeatures; }
    public void setMissingFeatures(List<String> values) {
        missingFeatures = values == null ? new ArrayList<String>() : values;
    }

    public List<String> getReprotectAuthorityContractVersions() {
        return reprotectAuthorityContractVersions;
    }

    public void setReprotectAuthorityContractVersions(List<String> values) {
        reprotectAuthorityContractVersions = values == null ? new ArrayList<String>() : values;
    }

    public String getFtctlVersion() {
        return ftctlVersion;
    }

    public String getRuntimeSchemaVersion() {
        return runtimeSchemaVersion;
    }

    public String getActionContractVersion() {
        return actionContractVersion;
    }

    public void setActionContractVersion(String actionContractVersion) {
        this.actionContractVersion = actionContractVersion;
    }

    public String getActionCommandCodeSource() {
        return actionCommandCodeSource;
    }

    public void setActionCommandCodeSource(String actionCommandCodeSource) {
        this.actionCommandCodeSource = actionCommandCodeSource;
    }

    public String getWrapperCodeSource() {
        return wrapperCodeSource;
    }

    public void setWrapperCodeSource(String wrapperCodeSource) {
        this.wrapperCodeSource = wrapperCodeSource;
    }

    public String getCapabilitiesJson() {
        return capabilitiesJson;
    }
}
