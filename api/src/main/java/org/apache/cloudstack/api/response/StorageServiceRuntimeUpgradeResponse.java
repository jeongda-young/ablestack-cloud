// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

package org.apache.cloudstack.api.response;

import java.util.Date;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.storage.dataservice.StorageServiceRuntimeUpgrade;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = StorageServiceRuntimeUpgrade.class)
public class StorageServiceRuntimeUpgradeResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description = "runtime upgrade ID") private String id;
    @SerializedName("instanceid") @Param(description = "Storage Service instance ID") private String instanceId;
    @SerializedName("bundleid") @Param(description = "target runtime bundle ID") private String bundleId;
    @SerializedName("bundleversion") @Param(description = "target runtime version") private String bundleVersion;
    @SerializedName("state") @Param(description = "upgrade state") private String state;
    @SerializedName("phase") @Param(description = "upgrade phase") private String phase;
    @SerializedName("progress") @Param(description = "upgrade progress percent") private Integer progress;
    @SerializedName("transactionid") @Param(description = "guest transaction ID") private String transactionId;
    @SerializedName("preflightjson") @Param(description = "preflight result JSON") private String preflightJson;
    @SerializedName("verificationjson") @Param(description = "activation verification JSON") private String verificationJson;
    @SerializedName("rollbackresultjson") @Param(description = "rollback result JSON") private String rollbackResultJson;
    @SerializedName("errorcode") @Param(description = "structured error code") private String errorCode;
    @SerializedName("errormessage") @Param(description = "bounded error message") private String errorMessage;
    @SerializedName("started") @Param(description = "start time") private Date started;
    @SerializedName("completed") @Param(description = "completion time") private Date completed;

    public void setId(final String value) { id = value; }
    public void setInstanceId(final String value) { instanceId = value; }
    public void setBundleId(final String value) { bundleId = value; }
    public void setBundleVersion(final String value) { bundleVersion = value; }
    public void setState(final String value) { state = value; }
    public void setPhase(final String value) { phase = value; }
    public void setProgress(final Integer value) { progress = value; }
    public void setTransactionId(final String value) { transactionId = value; }
    public void setPreflightJson(final String value) { preflightJson = value; }
    public void setVerificationJson(final String value) { verificationJson = value; }
    public void setRollbackResultJson(final String value) { rollbackResultJson = value; }
    public void setErrorCode(final String value) { errorCode = value; }
    public void setErrorMessage(final String value) { errorMessage = value; }
    public void setStarted(final Date value) { started = value; }
    public void setCompleted(final Date value) { completed = value; }
}
