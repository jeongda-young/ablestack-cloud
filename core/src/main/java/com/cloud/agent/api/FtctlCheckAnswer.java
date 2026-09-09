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

public class FtctlCheckAnswer extends Answer {

    private String ftctlResult;
    private String inventoryResult;
    private String vmName;
    private Integer primaryRc;
    private Integer peerRc;
    private Boolean peerDomainExpected;
    private String standbyDomainState;
    private String provisioningBackend;

    public FtctlCheckAnswer(Command command, boolean success, String details) {
        super(command, success, details);
    }

    public FtctlCheckAnswer(Command command, boolean success, String details, String ftctlResult,
                            String inventoryResult, String vmName, Integer primaryRc, Integer peerRc) {
        super(command, success, details);
        this.ftctlResult = ftctlResult;
        this.inventoryResult = inventoryResult;
        this.vmName = vmName;
        this.primaryRc = primaryRc;
        this.peerRc = peerRc;
    }

    public FtctlCheckAnswer(Command command, boolean success, String details, String ftctlResult,
                            String inventoryResult, String vmName, Integer primaryRc, Integer peerRc,
                            Boolean peerDomainExpected, String standbyDomainState, String provisioningBackend) {
        this(command, success, details, ftctlResult, inventoryResult, vmName, primaryRc, peerRc);
        this.peerDomainExpected = peerDomainExpected;
        this.standbyDomainState = standbyDomainState;
        this.provisioningBackend = provisioningBackend;
    }

    public String getFtctlResult() {
        return ftctlResult;
    }

    public String getInventoryResult() {
        return inventoryResult;
    }

    public String getVmName() {
        return vmName;
    }

    public Integer getPrimaryRc() {
        return primaryRc;
    }

    public Integer getPeerRc() {
        return peerRc;
    }

    public Boolean getPeerDomainExpected() {
        return peerDomainExpected;
    }

    public String getStandbyDomainState() {
        return standbyDomainState;
    }

    public String getProvisioningBackend() {
        return provisioningBackend;
    }
}
