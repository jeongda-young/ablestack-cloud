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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class GuestNetworkInterfaceResponse extends BaseResponse {
    @SerializedName("name")
    @Param(description = "Guest interface name")
    private String name;

    @SerializedName("hardwareaddress")
    @Param(description = "Guest-reported hardware address")
    private String hardwareAddress;

    @SerializedName("cloudnicid")
    @Param(description = "Cloud NIC ID matched by normalized MAC address")
    private String cloudNicId;

    @SerializedName("loopback")
    @Param(description = "True when this is a loopback interface")
    private boolean loopback;

    @SerializedName("addresses")
    @Param(description = "All IPv4 and IPv6 addresses observed on the interface")
    private List<GuestNetworkAddressResponse> addresses = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHardwareAddress() {
        return hardwareAddress;
    }

    public void setHardwareAddress(String hardwareAddress) {
        this.hardwareAddress = hardwareAddress;
    }

    public String getCloudNicId() {
        return cloudNicId;
    }

    public void setCloudNicId(String cloudNicId) {
        this.cloudNicId = cloudNicId;
    }

    public boolean isLoopback() {
        return loopback;
    }

    public void setLoopback(boolean loopback) {
        this.loopback = loopback;
    }

    public List<GuestNetworkAddressResponse> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<GuestNetworkAddressResponse> addresses) {
        this.addresses = addresses;
    }
}
