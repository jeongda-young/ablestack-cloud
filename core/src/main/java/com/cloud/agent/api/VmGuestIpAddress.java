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

public class VmGuestIpAddress {
    private String family;
    private String address;
    private Integer prefix;
    private String scope;
    private String role;
    private String roleSource;
    private boolean representative;

    public VmGuestIpAddress() {
    }

    public VmGuestIpAddress(String family, String address, Integer prefix, String scope) {
        this.family = family;
        this.address = address;
        this.prefix = prefix;
        this.scope = scope;
    }

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
