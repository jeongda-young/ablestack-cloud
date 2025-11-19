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
package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.cloud.vm.VirtualMachine;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

@EntityReference(value = VirtualMachine.class)
public class ListVmHostDevicesResponse extends BaseResponse {

    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "Host ID where the device is allocated")
    private Long hostId;

    @SerializedName(ApiConstants.HOSTDEVICES_NAME)
    @Param(description = "Host device name")
    private String hostDevicesName;

    @SerializedName(ApiConstants.HOSTDEVICES_TEXT)
    @Param(description = "Host device detail text")
    private String hostDevicesText;

    @SerializedName(ApiConstants.HOSTDEVICES_TYPE)
    @Param(description = "Host device type classification")
    private String hostDevicesType;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "VM ID that owns the device")
    private String virtualMachineId;

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public void setHostDevicesName(String hostDevicesName) {
        this.hostDevicesName = hostDevicesName;
    }

    public void setHostDevicesText(String hostDevicesText) {
        this.hostDevicesText = hostDevicesText;
    }

    public void setHostDevicesType(String hostDevicesType) {
        this.hostDevicesType = hostDevicesType;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }
}

