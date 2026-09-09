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

import java.util.List;

public class FtctlDrReplicaResourcesResponse extends BaseResponse {

    @SerializedName("remotevirtualmachineid")
    @Param(description = "the remote replica VM UUID")
    private String remoteVirtualMachineId;

    @SerializedName("remotevirtualmachinename")
    @Param(description = "the remote replica VM display name")
    private String remoteVirtualMachineName;

    @SerializedName("remotevirtualmachineinstancename")
    @Param(description = "the remote replica VM hypervisor instance name")
    private String remoteVirtualMachineInstanceName;

    @SerializedName("remotevirtualmachinestate")
    @Param(description = "the remote replica VM state from the remote Mold")
    private String remoteVirtualMachineState;

    @SerializedName("remotevirtualmachinehostid")
    @Param(description = "the remote replica VM target host ID from the remote Mold")
    private String remoteVirtualMachineHostId;

    @SerializedName("remotevirtualmachinehostname")
    @Param(description = "the remote replica VM target host name from the remote Mold")
    private String remoteVirtualMachineHostName;

    @SerializedName("remotevirtualmachinestateupdated")
    @Param(description = "the timestamp when the remote replica VM state snapshot was created")
    private String remoteVirtualMachineStateUpdated;

    @SerializedName("diskmap")
    @Param(description = "the explicit source target to remote Cloud-managed disk map")
    private String diskMap;

    @SerializedName("volume")
    @Param(description = "remote replica volumes", responseObject = FtctlDrReplicaVolumeResponse.class)
    private List<FtctlDrReplicaVolumeResponse> volumes;

    public void setRemoteVirtualMachineId(String remoteVirtualMachineId) {
        this.remoteVirtualMachineId = remoteVirtualMachineId;
    }

    public void setRemoteVirtualMachineName(String remoteVirtualMachineName) {
        this.remoteVirtualMachineName = remoteVirtualMachineName;
    }

    public void setRemoteVirtualMachineInstanceName(String remoteVirtualMachineInstanceName) {
        this.remoteVirtualMachineInstanceName = remoteVirtualMachineInstanceName;
    }

    public void setRemoteVirtualMachineState(String remoteVirtualMachineState) {
        this.remoteVirtualMachineState = remoteVirtualMachineState;
    }

    public void setRemoteVirtualMachineHostId(String remoteVirtualMachineHostId) {
        this.remoteVirtualMachineHostId = remoteVirtualMachineHostId;
    }

    public void setRemoteVirtualMachineHostName(String remoteVirtualMachineHostName) {
        this.remoteVirtualMachineHostName = remoteVirtualMachineHostName;
    }

    public void setRemoteVirtualMachineStateUpdated(String remoteVirtualMachineStateUpdated) {
        this.remoteVirtualMachineStateUpdated = remoteVirtualMachineStateUpdated;
    }

    public void setDiskMap(String diskMap) {
        this.diskMap = diskMap;
    }

    public void setVolumes(List<FtctlDrReplicaVolumeResponse> volumes) {
        this.volumes = volumes;
    }
}
