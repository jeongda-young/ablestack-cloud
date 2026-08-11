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
package com.cloud.agent.api;

import java.util.ArrayList;
import java.util.List;

public class VmGuestNetworkInterface {
    private String name;
    private String hardwareAddress;
    private String normalizedMacAddress;
    private String cloudNicId;
    private boolean loopback;
    private List<VmGuestIpAddress> addresses;

    public VmGuestNetworkInterface() {
        addresses = new ArrayList<>();
    }

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

    public String getNormalizedMacAddress() {
        return normalizedMacAddress;
    }

    public void setNormalizedMacAddress(String normalizedMacAddress) {
        this.normalizedMacAddress = normalizedMacAddress;
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

    public List<VmGuestIpAddress> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<VmGuestIpAddress> addresses) {
        this.addresses = addresses;
    }
}
