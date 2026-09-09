// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package org.apache.cloudstack.api.response.dr;

import java.util.List;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DrProtectionGroupPreflightResponse extends BaseResponse {
    @SerializedName("action") @Param(description = "the normalized group action") private String action;
    @SerializedName("ready") @Param(description = "whether every selected plan is eligible") private boolean ready;
    @SerializedName("plans") @Param(description = "ordered per-plan preflight results")
    private List<DrProtectionGroupPlanPreflightResponse> plans;

    public void setAction(String value) { action = value; }
    public void setReady(boolean value) { ready = value; }
    public void setPlans(List<DrProtectionGroupPlanPreflightResponse> value) { plans = value; }
}
