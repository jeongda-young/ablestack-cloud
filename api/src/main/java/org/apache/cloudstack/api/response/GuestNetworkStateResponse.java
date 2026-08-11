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

public class GuestNetworkStateResponse extends BaseResponse {
    @SerializedName("virtualmachineid")
    @Param(description = "The ID of the Instance")
    private String virtualMachineId;

    @SerializedName("status")
    @Param(description = "Latest guest network collection status")
    private String status;

    @SerializedName("schemaversion")
    @Param(description = "Guest network payload schema version")
    private int schemaVersion;

    @SerializedName("qgaversion")
    @Param(description = "Last observed QEMU guest agent version")
    private String qgaVersion;

    @SerializedName("observed")
    @Param(description = "Last collection attempt time")
    private Date observed;

    @SerializedName("lastsuccess")
    @Param(description = "Last successful collection time")
    private Date lastSuccess;

    @SerializedName("errorcode")
    @Param(description = "Structured collection error code")
    private String errorCode;

    @SerializedName("errormessage")
    @Param(description = "Bounded collection error message")
    private String errorMessage;

    @SerializedName("interfaces")
    @Param(description = "Guest-observed interfaces and all IPv4/IPv6 addresses")
    private List<GuestNetworkInterfaceResponse> interfaces = new ArrayList<>();

    @SerializedName("sections")
    @Param(description = "Per-section collection status")
    private List<GuestNetworkSectionResponse> sections = new ArrayList<>();

    @SerializedName("routes")
    @Param(description = "Guest-observed IPv4 and IPv6 routes")
    private List<GuestNetworkRouteResponse> routes = new ArrayList<>();

    @SerializedName("dns")
    @Param(description = "Guest-observed DNS configuration")
    private GuestNetworkDnsResponse dns = new GuestNetworkDnsResponse();

    @SerializedName("collector")
    @Param(description = "Collector build and host metadata")
    private GuestNetworkCollectorResponse collector;

    @SerializedName("guesttools")
    @Param(description = "ABLESTACK guest tools readiness")
    private GuestToolsResponse guestTools;

    public String getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getQgaVersion() {
        return qgaVersion;
    }

    public void setQgaVersion(String qgaVersion) {
        this.qgaVersion = qgaVersion;
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

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<GuestNetworkInterfaceResponse> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<GuestNetworkInterfaceResponse> interfaces) {
        this.interfaces = interfaces;
    }

    public List<GuestNetworkSectionResponse> getSections() {
        return sections;
    }

    public void setSections(List<GuestNetworkSectionResponse> sections) {
        this.sections = sections;
    }

    public List<GuestNetworkRouteResponse> getRoutes() {
        return routes;
    }

    public void setRoutes(List<GuestNetworkRouteResponse> routes) {
        this.routes = routes;
    }

    public GuestNetworkDnsResponse getDns() {
        return dns;
    }

    public void setDns(GuestNetworkDnsResponse dns) {
        this.dns = dns;
    }

    public GuestNetworkCollectorResponse getCollector() {
        return collector;
    }

    public void setCollector(GuestNetworkCollectorResponse collector) {
        this.collector = collector;
    }

    public GuestToolsResponse getGuestTools() {
        return guestTools;
    }

    public void setGuestTools(GuestToolsResponse guestTools) {
        this.guestTools = guestTools;
    }
}
