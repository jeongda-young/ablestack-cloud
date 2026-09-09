// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package org.apache.cloudstack.api.response.dr;

import java.util.Map;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DrProtectionGroupPlanPreflightResponse extends BaseResponse {
    @SerializedName("planid") @Param(description = "the DR plan UUID") private String planId;
    @SerializedName("planname") @Param(description = "the DR plan name") private String planName;
    @SerializedName("planstate") @Param(description = "the DR plan state") private String planState;
    @SerializedName("adminstate") @Param(description = "the DR plan administrative state") private String adminState;
    @SerializedName("eligible") @Param(description = "whether this plan can execute the group action") private boolean eligible;
    @SerializedName("reasoncode") @Param(description = "stable reason code when execution is blocked") private String reasonCode;
    @SerializedName("reasonargs") @Param(description = "non-sensitive reason arguments") private Map<String, String> reasonArgs;

    public void setPlanId(String value) { planId = value; }
    public void setPlanName(String value) { planName = value; }
    public void setPlanState(String value) { planState = value; }
    public void setAdminState(String value) { adminState = value; }
    public void setEligible(boolean value) { eligible = value; }
    public void setReasonCode(String value) { reasonCode = value; }
    public void setReasonArgs(Map<String, String> value) { reasonArgs = value; }
}
