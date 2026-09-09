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

public class FtctlDrReplicaVolumeResponse extends BaseResponse {

    @SerializedName("id")
    @Param(description = "the remote replica volume UUID")
    private String id;

    @SerializedName("name")
    @Param(description = "the remote replica volume name")
    private String name;

    @SerializedName("path")
    @Param(description = "the remote Cloud-managed disk path used by FTCTL")
    private String path;

    @SerializedName("state")
    @Param(description = "the remote replica volume state")
    private String state;

    @SerializedName("disklabel")
    @Param(description = "the FTCTL disk label")
    private String diskLabel;

    @SerializedName("sourcevolumeid")
    @Param(description = "the source-site primary volume ID or UUID")
    private String sourceVolumeId;

    @SerializedName("sourcedisktarget")
    @Param(description = "the source VM KVM disk target")
    private String sourceDiskTarget;

    @SerializedName("deviceid")
    @Param(description = "the source volume device ID")
    private Long deviceId;

    @SerializedName("type")
    @Param(description = "the source volume type")
    private String type;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setDiskLabel(String diskLabel) {
        this.diskLabel = diskLabel;
    }

    public void setSourceVolumeId(String sourceVolumeId) {
        this.sourceVolumeId = sourceVolumeId;
    }

    public void setSourceDiskTarget(String sourceDiskTarget) {
        this.sourceDiskTarget = sourceDiskTarget;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public void setType(String type) {
        this.type = type;
    }
}
