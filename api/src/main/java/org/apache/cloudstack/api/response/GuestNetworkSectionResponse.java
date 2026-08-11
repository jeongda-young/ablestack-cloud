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

import java.util.Date;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class GuestNetworkSectionResponse extends BaseResponse {
    @SerializedName("name")
    @Param(description = "Section name")
    private String name;

    @SerializedName("status")
    @Param(description = "Section collection status")
    private String status;

    @SerializedName("details")
    @Param(description = "Optional section status details")
    private String details;

    @SerializedName("truncated")
    @Param(description = "True when the section was truncated")
    private boolean truncated;

    @SerializedName("originalcount")
    @Param(description = "Original item count before truncation")
    private Integer originalCount;

    @SerializedName("source")
    @Param(description = "Collection source used for the section")
    private String source;

    @SerializedName("errorcode")
    @Param(description = "Structured section error code")
    private String errorCode;

    @SerializedName("observed")
    @Param(description = "Last section collection attempt")
    private Date observed;

    @SerializedName("lastsuccess")
    @Param(description = "Last successful section collection")
    private Date lastSuccess;

    @SerializedName("nextdue")
    @Param(description = "Next scheduled section collection")
    private Date nextDue;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public Integer getOriginalCount() {
        return originalCount;
    }

    public void setOriginalCount(Integer originalCount) {
        this.originalCount = originalCount;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public Date getObserved() { return observed; }
    public void setObserved(Date observed) { this.observed = observed; }
    public Date getLastSuccess() { return lastSuccess; }
    public void setLastSuccess(Date lastSuccess) { this.lastSuccess = lastSuccess; }
    public Date getNextDue() { return nextDue; }
    public void setNextDue(Date nextDue) { this.nextDue = nextDue; }
}
