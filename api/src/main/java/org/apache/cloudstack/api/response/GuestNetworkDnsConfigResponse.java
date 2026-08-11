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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class GuestNetworkDnsConfigResponse {
    @SerializedName("interfacename")
    @Param(description = "Guest interface name, or null for global DNS")
    private String interfaceName;

    @SerializedName("source")
    @Param(description = "DNS configuration source")
    private String source;

    @SerializedName("global")
    @Param(description = "Whether this is a global DNS configuration")
    private boolean global;

    @SerializedName("servers")
    @Param(description = "DNS servers for this scope")
    private List<GuestNetworkDnsServerResponse> servers = new ArrayList<>();

    @SerializedName("domains")
    @Param(description = "Search and routing domains for this scope")
    private List<GuestNetworkDnsDomainResponse> domains = new ArrayList<>();

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }

    public List<GuestNetworkDnsServerResponse> getServers() {
        return servers;
    }

    public void setServers(List<GuestNetworkDnsServerResponse> servers) {
        this.servers = servers;
    }

    public List<GuestNetworkDnsDomainResponse> getDomains() {
        return domains;
    }

    public void setDomains(List<GuestNetworkDnsDomainResponse> domains) {
        this.domains = domains;
    }
}
