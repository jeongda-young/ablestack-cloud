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
package org.apache.cloudstack.api.response.dr;

import java.util.Date;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.dr.DrPlanViewCacheVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DrProtectionViewResponse extends BaseResponse {
    @SerializedName("planid")
    @Param(description = "the DR plan internal ID")
    private long planId;

    @SerializedName("snapshotversion")
    @Param(description = "the protection view snapshot schema version")
    private int snapshotVersion;

    @SerializedName("snapshot")
    @Param(description = "the cached DR protection view JSON")
    private String snapshot;

    @SerializedName("projectionstate")
    @Param(description = "the cache projection state")
    private String projectionState;

    @SerializedName("lasterror")
    @Param(description = "the last projection error, if any")
    private String lastError;

    @SerializedName("lastrefresherrorcode")
    @Param(description = "the most recent protection-view refresh error code")
    private String lastRefreshErrorCode;

    @SerializedName("lastrefresherrormessage")
    @Param(description = "the most recent protection-view refresh error message")
    private String lastRefreshErrorMessage;

    @SerializedName("lastrefreshfailed")
    @Param(description = "the most recent protection-view refresh failure time")
    private Date lastRefreshFailed;

    @SerializedName("generated")
    @Param(description = "the snapshot generation time")
    private Date generated;

    public static DrProtectionViewResponse from(DrPlanViewCacheVO cache) {
        DrProtectionViewResponse response = new DrProtectionViewResponse();
        response.setObjectName("drprotectionview");
        response.planId = cache.getPlanId();
        response.snapshotVersion = cache.getSnapshotVersion();
        response.snapshot = cache.getSnapshotJson();
        response.projectionState = cache.getProjectionState();
        response.lastError = cache.getLastError();
        response.lastRefreshErrorCode = cache.getLastRefreshErrorCode();
        response.lastRefreshErrorMessage = cache.getLastRefreshErrorMessage();
        response.lastRefreshFailed = cache.getLastRefreshFailedAt();
        response.generated = cache.getGenerated();
        return response;
    }
}
