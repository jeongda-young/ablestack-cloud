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

public class FtctlProtectionVolumeResponse extends BaseResponse {

    @SerializedName("id")
    @Param(description = "the secondary volume UUID")
    private String id;

    @SerializedName("name")
    @Param(description = "the secondary volume name")
    private String name;

    @SerializedName("path")
    @Param(description = "the secondary volume storage path")
    private String path;

    @SerializedName("state")
    @Param(description = "the secondary volume state from Cloud")
    private String state;

    @SerializedName("disklabel")
    @Param(description = "the FTCTL disk label")
    private String diskLabel;

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
}
