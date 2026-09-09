// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package org.apache.cloudstack.api.response.dr;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DrProtectionGroupResponse extends BaseResponse {
    @SerializedName("groupuuid") @Param(description = "the protection group UUID") private String groupUuid;
    @SerializedName("groupname") @Param(description = "the protection group name") private String groupName;
    @SerializedName("plancount") @Param(description = "the number of plans in the group") private int planCount;

    public void setGroupUuid(String value) { groupUuid = value; }
    public void setGroupName(String value) { groupName = value; }
    public void setPlanCount(int value) { planCount = value; }
}
