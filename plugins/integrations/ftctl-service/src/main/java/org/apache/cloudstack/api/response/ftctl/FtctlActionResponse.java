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

public class FtctlActionResponse extends BaseResponse {

    @SerializedName("virtualmachineid")
    @Param(description = "the virtual machine ID")
    private Long virtualMachineId;

    @SerializedName("vmname")
    @Param(description = "the virtual machine name")
    private String vmName;

    @SerializedName("action")
    @Param(description = "the FTCTL action")
    private String action;

    @SerializedName("result")
    @Param(description = "the FTCTL action result")
    private String result;

    @SerializedName("exitcode")
    @Param(description = "the FTCTL action exit code")
    private Integer exitCode;

    @SerializedName("output")
    @Param(description = "the FTCTL action output")
    private String output;

    @SerializedName("mode")
    @Param(description = "the current FTCTL mode after action execution")
    private String mode;

    @SerializedName("protectionstate")
    @Param(description = "the current FTCTL protection state after action execution")
    private String protectionState;

    @SerializedName("transportstate")
    @Param(description = "the current FTCTL transport state after action execution")
    private String transportState;

    @SerializedName("activeside")
    @Param(description = "the current FTCTL active side after action execution")
    private String activeSide;

    @SerializedName("adminstate")
    @Param(description = "the current FTCTL admin state after action execution")
    private String adminState;

    @SerializedName("fencingstate")
    @Param(description = "the current FTCTL fencing state after action execution")
    private String fencingState;

    @SerializedName("lasterror")
    @Param(description = "the current FTCTL error after action execution")
    private String lastError;

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setExitCode(Integer exitCode) {
        this.exitCode = exitCode;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setProtectionState(String protectionState) {
        this.protectionState = protectionState;
    }

    public void setTransportState(String transportState) {
        this.transportState = transportState;
    }

    public void setActiveSide(String activeSide) {
        this.activeSide = activeSide;
    }

    public void setAdminState(String adminState) {
        this.adminState = adminState;
    }

    public void setFencingState(String fencingState) {
        this.fencingState = fencingState;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getResult() {
        return result;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getOutput() {
        return output;
    }
}
