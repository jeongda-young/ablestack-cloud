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

import com.cloud.security.IntegrityVerificationFinalResult;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import javax.persistence.Column;
import java.util.Date;
@EntityReference(value = {IntegrityVerificationFinalResult.class})
public class GetIntegrityVerificationFinalResultListResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the id of the integrity verification final result")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @SerializedName(ApiConstants.MANAGEMENT_SERVER_ID)
    @Param(description = "the id of the management server")
    private String msHostId;

    @SerializedName(ApiConstants.RESULT)
    @Param(description = "result of the integrity verification")
    private boolean verificationFinalResult;

    @SerializedName(ApiConstants.LAST_UPDATED)
    @Param(description = "the date this integrity verification was updated")
    private Date lastUpdated;

    @SerializedName(ApiConstants.INTEGRITY_VERIFICATIONS_FAILED_LIST)
    @Param(description = "the verification failed list")
    private String verificationFailedList;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "the type of the integrity verification")
    private String type;

    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getMsHostId() {
        return msHostId;
    }

    public boolean getVerificationFinalResult() { return verificationFinalResult; }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public String getVerificationFailedList() {
        return verificationFailedList;
    }

    public String getType() {
        return type;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setMsHostId(String msHostId) {
        this.msHostId = msHostId;
    }

    public void setVerificationFinalResult(boolean verificationFinalResult) {
        this.verificationFinalResult = verificationFinalResult;
    }

    public void setVerificationDate(Date verificationDate) {
        this.lastUpdated = verificationDate;
    }

    public void setVerificationFailedList(String verificationFailedList) {
        this.verificationFailedList = verificationFailedList;
    }

    public void setType(String type) {
        this.type = type;
    }
}
