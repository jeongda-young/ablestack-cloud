// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.response.ftctl;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

public class FtctlRemoteMoldNetworkResponse extends BaseResponse {

    @SerializedName("id")
    @Param(description = "the remote Mold network ID")
    private String id;

    @SerializedName("name")
    @Param(description = "the remote Mold network name")
    private String name;

    @SerializedName("displaytext")
    @Param(description = "the remote Mold network display text")
    private String displayText;

    @SerializedName("zoneid")
    @Param(description = "the remote Mold network zone ID")
    private String zoneId;

    @SerializedName("zonename")
    @Param(description = "the remote Mold network zone name")
    private String zoneName;

    @SerializedName("type")
    @Param(description = "the remote Mold network guest type")
    private String type;

    @SerializedName("traffictype")
    @Param(description = "the remote Mold network traffic type")
    private String trafficType;

    @SerializedName("state")
    @Param(description = "the remote Mold network state")
    private String state;

    @SerializedName("cidr")
    @Param(description = "the remote Mold network CIDR")
    private String cidr;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }
}
