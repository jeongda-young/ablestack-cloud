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

public class ImportVMTaskEventResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the import VM task event")
    private String id;

    @SerializedName(ApiConstants.IMPORT_VM_TASK_ID)
    @Param(description = "the import VM task ID")
    private String importVmTaskId;

    @SerializedName("eventtype")
    @Param(description = "the import VM task event type")
    private String eventType;

    @SerializedName("phase")
    @Param(description = "the migration phase at event time")
    private String phase;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the migration state at event time")
    private String state;

    @SerializedName("step")
    @Param(description = "the migration step at event time")
    private String step;

    @SerializedName("message")
    @Param(description = "the event message")
    private String message;

    @SerializedName("payload")
    @Param(description = "the event payload without secrets")
    private String payload;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the event create date")
    private Date created;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getImportVmTaskId() {
        return importVmTaskId;
    }

    public void setImportVmTaskId(String importVmTaskId) {
        this.importVmTaskId = importVmTaskId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
