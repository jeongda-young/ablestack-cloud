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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class GuestNetworkRefreshResponse extends BaseResponse {
    @SerializedName("accepted")
    @Param(description = "True when the refresh request was accepted")
    private boolean accepted;
    @SerializedName("requestedsections")
    @Param(description = "Sections requested for recollection")
    private List<String> requestedSections = new ArrayList<>();

    public boolean isAccepted() { return accepted; }
    public void setAccepted(boolean accepted) { this.accepted = accepted; }
    public List<String> getRequestedSections() { return requestedSections; }
    public void setRequestedSections(List<String> requestedSections) {
        this.requestedSections = requestedSections;
    }
}
