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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.Date;

public class GetIntegrityVerificationFinalResultResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "result of the integrity verification")
    private long id;
    @SerializedName(ApiConstants.RESULT)
    @Param(description = "result of the integrity verification")
    private boolean verificationFinalResult;

    @SerializedName(ApiConstants.LAST_UPDATED)
    @Param(description = "the date this mshost was updated")
    private Date lastUpdated;

    public long getId() {
        return id;
    }

    public boolean getVerificationFinalResult() { return verificationFinalResult; }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setVerificationFinalResult(boolean verificationFinalResult) {
        this.verificationFinalResult = verificationFinalResult;
    }

    public void setVerificationDate(Date verificationDate) {
        this.lastUpdated = verificationDate;
    }
}
