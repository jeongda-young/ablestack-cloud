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

public class FtctlRemotePreflightCommand extends Command {

    private String vmName;
    private String mode;
    private String peerUri;
    private String secondaryTargetDir;
    private String remoteNbdExportAddr;
    private String secondarySshKeyFile;

    public FtctlRemotePreflightCommand() {
    }

    public FtctlRemotePreflightCommand(String vmName, String mode, String peerUri) {
        this.vmName = vmName;
        this.mode = mode;
        this.peerUri = peerUri;
    }

    public String getVmName() {
        return vmName;
    }

    public String getMode() {
        return mode;
    }

    public String getPeerUri() {
        return peerUri;
    }

    public String getSecondaryTargetDir() {
        return secondaryTargetDir;
    }

    public void setSecondaryTargetDir(String secondaryTargetDir) {
        this.secondaryTargetDir = secondaryTargetDir;
    }

    public String getRemoteNbdExportAddr() {
        return remoteNbdExportAddr;
    }

    public void setRemoteNbdExportAddr(String remoteNbdExportAddr) {
        this.remoteNbdExportAddr = remoteNbdExportAddr;
    }

    public String getSecondarySshKeyFile() {
        return secondarySshKeyFile;
    }

    public void setSecondarySshKeyFile(String secondarySshKeyFile) {
        this.secondarySshKeyFile = secondarySshKeyFile;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
