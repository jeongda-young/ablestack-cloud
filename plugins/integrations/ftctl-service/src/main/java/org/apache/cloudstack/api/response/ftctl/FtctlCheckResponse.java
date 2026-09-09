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
package org.apache.cloudstack.api.response.ftctl;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

public class FtctlCheckResponse extends BaseResponse {

    @SerializedName("virtualmachineid")
    @Param(description = "the virtual machine ID")
    private Long virtualMachineId;

    @SerializedName("vmname")
    @Param(description = "the virtual machine name")
    private String vmName;

    @SerializedName("result")
    @Param(description = "the FTCTL check result")
    private String result;

    @SerializedName("inventoryresult")
    @Param(description = "the FTCTL inventory result")
    private String inventoryResult;

    @SerializedName("primaryrc")
    @Param(description = "the primary inventory return code")
    private Integer primaryRc;

    @SerializedName("primarydomainstate")
    @Param(description = "the interpreted primary domain state")
    private String primaryDomainState;

    @SerializedName("peerrc")
    @Param(description = "the peer inventory return code")
    private Integer peerRc;

    @SerializedName("peerdomainexpected")
    @Param(description = "whether the peer libvirt domain is expected to exist for the current protection state")
    private Boolean peerDomainExpected;

    @SerializedName("standbydomainstate")
    @Param(description = "the interpreted standby domain state")
    private String standbyDomainState;

    @SerializedName("provisioningbackend")
    @Param(description = "the FTCTL standby VM and volume provisioning backend")
    private String provisioningBackend;

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setInventoryResult(String inventoryResult) {
        this.inventoryResult = inventoryResult;
    }

    public void setPrimaryRc(Integer primaryRc) {
        this.primaryRc = primaryRc;
    }

    public void setPrimaryDomainState(String primaryDomainState) {
        this.primaryDomainState = primaryDomainState;
    }

    public void setPeerRc(Integer peerRc) {
        this.peerRc = peerRc;
    }

    public void setPeerDomainExpected(Boolean peerDomainExpected) {
        this.peerDomainExpected = peerDomainExpected;
    }

    public void setStandbyDomainState(String standbyDomainState) {
        this.standbyDomainState = standbyDomainState;
    }

    public void setProvisioningBackend(String provisioningBackend) {
        this.provisioningBackend = provisioningBackend;
    }
}
