// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package org.apache.cloudstack.api.response.dr;

import java.util.Date;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dr.DrGroupRunVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(DrGroupRunVO.class)
public class DrGroupRunResponse extends BaseResponse {
    @SerializedName("id") @Param(description = "the group run UUID") private String id;
    @SerializedName("groupuuid") @Param(description = "the protection group UUID") private String groupUuid;
    @SerializedName("groupname") @Param(description = "the protection group name") private String groupName;
    @SerializedName("action") @Param(description = "the group action") private String action;
    @SerializedName("state") @Param(description = "the group run state") private String state;
    @SerializedName("maxparallel") @Param(description = "the maximum concurrent plan count") private int maxParallel;
    @SerializedName("quiescerequired") @Param(description = "whether application quiesce is required") private boolean quiesceRequired;
    @SerializedName("totalcount") @Param(description = "the total plan count") private int totalCount;
    @SerializedName("succeededcount") @Param(description = "the succeeded plan count") private int succeededCount;
    @SerializedName("failedcount") @Param(description = "the failed plan count") private int failedCount;
    @SerializedName("progressjson") @Param(description = "the aggregate per-plan progress") private String progressJson;
    @SerializedName("created") @Param(description = "the creation time") private Date created;
    @SerializedName("completed") @Param(description = "the completion time") private Date completed;

    public void setId(String value) { id = value; }
    public void setGroupUuid(String value) { groupUuid = value; }
    public void setGroupName(String value) { groupName = value; }
    public void setAction(String value) { action = value; }
    public void setState(String value) { state = value; }
    public void setMaxParallel(int value) { maxParallel = value; }
    public void setQuiesceRequired(boolean value) { quiesceRequired = value; }
    public void setTotalCount(int value) { totalCount = value; }
    public void setSucceededCount(int value) { succeededCount = value; }
    public void setFailedCount(int value) { failedCount = value; }
    public void setProgressJson(String value) { progressJson = value; }
    public void setCreated(Date value) { created = value; }
    public void setCompleted(Date value) { completed = value; }
}
