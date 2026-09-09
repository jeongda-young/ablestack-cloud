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
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dr.DrEventVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DrEventVO.class)
public class DrEventResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the DR event ID")
    private String id;

    @SerializedName("planid")
    @Param(description = "the DR plan ID")
    private Long planId;

    @SerializedName("runid")
    @Param(description = "the DR run ID")
    private Long runId;

    @SerializedName("eventtype")
    @Param(description = "the event type")
    private String eventType;

    @SerializedName("severity")
    @Param(description = "the event severity")
    private String severity;

    @SerializedName("source")
    @Param(description = "the event source")
    private String source;

    @SerializedName("message")
    @Param(description = "the event message")
    private String message;

    @SerializedName("details")
    @Param(description = "the event details JSON")
    private String detailsJson;

    @SerializedName("created")
    @Param(description = "the event creation time")
    private Date created;

    public void setId(String id) {
        this.id = id;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
