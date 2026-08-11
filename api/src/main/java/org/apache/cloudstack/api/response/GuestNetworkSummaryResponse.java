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
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class GuestNetworkSummaryResponse extends BaseResponse {
    @SerializedName("status")
    @Param(description = "Latest guest network collection status")
    private String status;

    @SerializedName("observed")
    @Param(description = "Last collection attempt time")
    private Date observed;

    @SerializedName("lastsuccess")
    @Param(description = "Last successful collection time")
    private Date lastSuccess;

    @SerializedName("ipv4addresses")
    @Param(description = "All guest-observed IPv4 addresses with prefix")
    private List<String> ipv4Addresses = new ArrayList<>();

    @SerializedName("ipv6addresses")
    @Param(description = "All guest-observed IPv6 addresses with prefix")
    private List<String> ipv6Addresses = new ArrayList<>();

    @SerializedName("interfacecount")
    @Param(description = "Number of observed guest interfaces")
    private int interfaceCount;

    @SerializedName("representativeaddress")
    @Param(description = "QGA-selected representative guest address without prefix")
    private String representativeAddress;

    @SerializedName("representativeprefix")
    @Param(description = "Network prefix length of the QGA-selected representative address")
    private Integer representativePrefix;

    @SerializedName("representativefamily")
    @Param(description = "Address family of the QGA-selected representative address")
    private String representativeFamily;

    @SerializedName("representativesource")
    @Param(description = "Source used by QGA collection to select the representative address")
    private String representativeSource;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getObserved() {
        return observed;
    }

    public void setObserved(Date observed) {
        this.observed = observed;
    }

    public Date getLastSuccess() {
        return lastSuccess;
    }

    public void setLastSuccess(Date lastSuccess) {
        this.lastSuccess = lastSuccess;
    }

    public List<String> getIpv4Addresses() {
        return ipv4Addresses;
    }

    public void setIpv4Addresses(List<String> ipv4Addresses) {
        this.ipv4Addresses = ipv4Addresses;
    }

    public List<String> getIpv6Addresses() {
        return ipv6Addresses;
    }

    public void setIpv6Addresses(List<String> ipv6Addresses) {
        this.ipv6Addresses = ipv6Addresses;
    }

    public int getInterfaceCount() {
        return interfaceCount;
    }

    public void setInterfaceCount(int interfaceCount) {
        this.interfaceCount = interfaceCount;
    }

    public String getRepresentativeAddress() {
        return representativeAddress;
    }

    public void setRepresentativeAddress(String representativeAddress) {
        this.representativeAddress = representativeAddress;
    }

    public Integer getRepresentativePrefix() {
        return representativePrefix;
    }

    public void setRepresentativePrefix(Integer representativePrefix) {
        this.representativePrefix = representativePrefix;
    }

    public String getRepresentativeFamily() {
        return representativeFamily;
    }

    public void setRepresentativeFamily(String representativeFamily) {
        this.representativeFamily = representativeFamily;
    }

    public String getRepresentativeSource() {
        return representativeSource;
    }

    public void setRepresentativeSource(String representativeSource) {
        this.representativeSource = representativeSource;
    }
}
