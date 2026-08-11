// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
package org.apache.cloudstack.api.response;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class GuestNetworkCollectorResponse extends BaseResponse {
    @SerializedName("buildid")
    @Param(description = "Collector build identifier")
    private String buildId;

    @SerializedName("hostid")
    @Param(description = "Database ID of the collector host")
    private Long hostId;

    @SerializedName("capabilityhash")
    @Param(description = "SHA-256 of enabled QGA capabilities")
    private String capabilityHash;

    public String getBuildId() { return buildId; }
    public void setBuildId(String buildId) { this.buildId = buildId; }
    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }
    public String getCapabilityHash() { return capabilityHash; }
    public void setCapabilityHash(String capabilityHash) { this.capabilityHash = capabilityHash; }
}
