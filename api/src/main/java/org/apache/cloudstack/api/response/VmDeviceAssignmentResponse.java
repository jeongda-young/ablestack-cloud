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
public class VmDeviceAssignmentResponse extends BaseResponse {

    @SerializedName(ApiConstants.HOST_ID)
    @Param(description = "Host ID where the device resides")
    private Long hostId;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "Host name where the device resides")
    private String hostName;

    @SerializedName(ApiConstants.HOSTDEVICES_NAME)
    @Param(description = "Assigned device name")
    private String deviceName;

    @SerializedName(ApiConstants.HOSTDEVICES_DETAIL)
    @Param(description = "Assigned device detail text")
    private String deviceDetail;

    @SerializedName(ApiConstants.HOSTDEVICES_TYPE)
    @Param(description = "Assigned device type")
    private String deviceType;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "VM ID that owns the device")
    private String virtualMachineId;

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceDetail(String deviceDetail) {
        this.deviceDetail = deviceDetail;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }
}

