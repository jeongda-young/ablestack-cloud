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

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class GuestNetworkAddressResponse extends BaseResponse {
    @SerializedName("family")
    @Param(description = "Address family: IPv4 or IPv6")
    private String family;

    @SerializedName("address")
    @Param(description = "Guest-observed IP address")
    private String address;

    @SerializedName("prefix")
    @Param(description = "Network prefix length")
    private Integer prefix;

    @SerializedName("scope")
    @Param(description = "Address scope such as global, private, link-local, or loopback")
    private String scope;

    @SerializedName("role")
    @Param(description = "QGA-observed address role: PRIMARY, SECONDARY, or UNKNOWN")
    private String role;

    @SerializedName("rolesource")
    @Param(description = "Source used to determine the QGA address role")
    private String roleSource;

    @SerializedName("representative")
    @Param(description = "Whether this is the QGA-selected representative address")
    private boolean representative;

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPrefix() {
        return prefix;
    }

    public void setPrefix(Integer prefix) {
        this.prefix = prefix;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRoleSource() {
        return roleSource;
    }

    public void setRoleSource(String roleSource) {
        this.roleSource = roleSource;
    }

    public boolean isRepresentative() {
        return representative;
    }

    public void setRepresentative(boolean representative) {
        this.representative = representative;
    }
}
