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

public class FtctlCheckCommand extends Command {

    private String vmName;
    private String secondaryVmName;
    private String activeSide;
    private String provisioningBackend;

    public FtctlCheckCommand() {
    }

    public FtctlCheckCommand(String vmName) {
        this.vmName = vmName;
    }

    public FtctlCheckCommand(String vmName, String secondaryVmName, String activeSide, String provisioningBackend) {
        this.vmName = vmName;
        this.secondaryVmName = secondaryVmName;
        this.activeSide = activeSide;
        this.provisioningBackend = provisioningBackend;
    }

    public String getVmName() {
        return vmName;
    }

    public String getSecondaryVmName() {
        return secondaryVmName;
    }

    public String getActiveSide() {
        return activeSide;
    }

    public String getProvisioningBackend() {
        return provisioningBackend;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
