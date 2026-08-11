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

public class GuestNetworkRouteResponse extends BaseResponse {
    @SerializedName("family")
    @Param(description = "Route address family: IPv4 or IPv6")
    private String family;

    @SerializedName("destination")
    @Param(description = "Route destination address")
    private String destination;

    @SerializedName("prefix")
    @Param(description = "Route destination prefix length")
    private Integer prefix;

    @SerializedName("gateway")
    @Param(description = "Route gateway or next hop")
    private String gateway;

    @SerializedName("interfacename")
    @Param(description = "Guest interface used by the route")
    private String interfaceName;

    @SerializedName("metric")
    @Param(description = "Route metric")
    private Integer metric;

    @SerializedName("table")
    @Param(description = "Route table")
    private String table;

    @SerializedName("protocol")
    @Param(description = "Route protocol")
    private String protocol;

    @SerializedName("scope")
    @Param(description = "Route scope")
    private String scope;

    @SerializedName("default")
    @Param(description = "True when this is a default route")
    private boolean defaultRoute;

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getPrefix() {
        return prefix;
    }

    public void setPrefix(Integer prefix) {
        this.prefix = prefix;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public Integer getMetric() {
        return metric;
    }

    public void setMetric(Integer metric) {
        this.metric = metric;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isDefaultRoute() {
        return defaultRoute;
    }

    public void setDefaultRoute(boolean defaultRoute) {
        this.defaultRoute = defaultRoute;
    }
}
