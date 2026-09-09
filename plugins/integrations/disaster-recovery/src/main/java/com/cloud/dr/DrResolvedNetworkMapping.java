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
package com.cloud.dr;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;

public class DrResolvedNetworkMapping {
    private String networkId;
    private String networkLocalId;
    private String role;
    private String name;

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getNetworkLocalId() {
        return networkLocalId;
    }

    public void setNetworkLocalId(String networkLocalId) {
        this.networkLocalId = networkLocalId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonObject toJsonObject() {
        JsonObject network = new JsonObject();
        addString(network, "networkId", networkId);
        addString(network, "networkLocalId", networkLocalId);
        addString(network, "role", role);
        addString(network, "name", name);
        return network;
    }

    private void addString(JsonObject object, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            object.addProperty(key, StringUtils.trim(value));
        }
    }
}
