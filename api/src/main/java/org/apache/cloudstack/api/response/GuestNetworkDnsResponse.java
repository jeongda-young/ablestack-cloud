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

public class GuestNetworkDnsResponse {
    @SerializedName("source")
    @Param(description = "Selected DNS configuration source")
    private String source;

    @SerializedName("upstreamserversknown")
    @Param(description = "Whether at least one non-local upstream DNS server is known")
    private boolean upstreamServersKnown;

    @SerializedName("servers")
    @Param(description = "Flattened unique DNS server addresses")
    private List<String> servers = new ArrayList<>();

    @SerializedName("searchdomains")
    @Param(description = "Flattened unique search domains")
    private List<String> searchDomains = new ArrayList<>();

    @SerializedName("configurations")
    @Param(description = "Global and per-interface DNS configuration")
    private List<GuestNetworkDnsConfigResponse> configurations = new ArrayList<>();

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isUpstreamServersKnown() {
        return upstreamServersKnown;
    }

    public void setUpstreamServersKnown(boolean upstreamServersKnown) {
        this.upstreamServersKnown = upstreamServersKnown;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public List<String> getSearchDomains() {
        return searchDomains;
    }

    public void setSearchDomains(List<String> searchDomains) {
        this.searchDomains = searchDomains;
    }

    public List<GuestNetworkDnsConfigResponse> getConfigurations() {
        return configurations;
    }

    public void setConfigurations(List<GuestNetworkDnsConfigResponse> configurations) {
        this.configurations = configurations;
    }
}
