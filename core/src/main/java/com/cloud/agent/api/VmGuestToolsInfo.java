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

public class VmGuestToolsInfo {
    private boolean installed;
    private String version;
    private int helperSchemaVersion;
    private String qgaPolicyMode;
    private String readinessStatus;
    private String profileVersion;
    private String errorCode;

    public boolean isInstalled() {
        return installed;
    }

    public void setInstalled(boolean installed) {
        this.installed = installed;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getHelperSchemaVersion() {
        return helperSchemaVersion;
    }

    public void setHelperSchemaVersion(int helperSchemaVersion) {
        this.helperSchemaVersion = helperSchemaVersion;
    }

    public String getQgaPolicyMode() {
        return qgaPolicyMode;
    }

    public void setQgaPolicyMode(String qgaPolicyMode) {
        this.qgaPolicyMode = qgaPolicyMode;
    }

    public String getReadinessStatus() {
        return readinessStatus;
    }

    public void setReadinessStatus(String readinessStatus) {
        this.readinessStatus = readinessStatus;
    }

    public String getProfileVersion() {
        return profileVersion;
    }

    public void setProfileVersion(String profileVersion) {
        this.profileVersion = profileVersion;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
