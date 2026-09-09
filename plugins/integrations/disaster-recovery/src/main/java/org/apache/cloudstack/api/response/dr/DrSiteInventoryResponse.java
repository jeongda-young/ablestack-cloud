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
package org.apache.cloudstack.api.response.dr;

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DrSiteInventoryResponse extends BaseResponse {
    @SerializedName("siteid")
    @Param(description = "the DR site ID")
    private String siteId;

    @SerializedName("sitetype")
    @Param(description = "the DR site type")
    private String siteType;

    @SerializedName("healthstate")
    @Param(description = "the inventory discovery state")
    private String healthState;

    @SerializedName("reasoncode")
    @Param(description = "the inventory discovery reason code")
    private String reasonCode;

    @SerializedName("message")
    @Param(description = "the inventory discovery message")
    private String message;

    @SerializedName("latencyms")
    @Param(description = "the inventory discovery latency in milliseconds")
    private Long latencyMs;

    @SerializedName("checkedat")
    @Param(description = "the inventory discovery time")
    private Date checkedAt;

    @SerializedName("zones")
    @Param(description = "the Zone options")
    private List<DrInventoryOptionResponse> zones;

    @SerializedName("vmwaredatacenters")
    @Param(description = "the VMware datacenter options")
    private List<DrInventoryOptionResponse> vmwareDatacenters;

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public void setHealthState(String healthState) {
        this.healthState = healthState;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public void setCheckedAt(Date checkedAt) {
        this.checkedAt = checkedAt;
    }

    public void setZones(List<DrInventoryOptionResponse> zones) {
        this.zones = zones;
    }

    public void setVmwareDatacenters(List<DrInventoryOptionResponse> vmwareDatacenters) {
        this.vmwareDatacenters = vmwareDatacenters;
    }
}
