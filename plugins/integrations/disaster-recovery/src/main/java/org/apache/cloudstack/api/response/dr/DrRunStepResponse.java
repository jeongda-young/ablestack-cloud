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

import com.cloud.dr.DrRunStepVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DrRunStepVO.class)
public class DrRunStepResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the DR run step ID")
    private String id;

    @SerializedName("runid")
    @Param(description = "the DR run ID")
    private Long runId;

    @SerializedName("stepname")
    @Param(description = "the step name")
    private String stepName;

    @SerializedName("steporder")
    @Param(description = "the step order")
    private Integer stepOrder;

    @SerializedName("state")
    @Param(description = "the step state")
    private String state;

    @SerializedName("progress")
    @Param(description = "the step progress percentage")
    private Integer progress;

    @SerializedName("details")
    @Param(description = "the step details JSON")
    private String detailsJson;

    @SerializedName("errorcode")
    @Param(description = "the step error code")
    private String errorCode;

    @SerializedName("errormessage")
    @Param(description = "the step error message")
    private String errorMessage;

    @SerializedName("started")
    @Param(description = "the step start time")
    private Date started;

    @SerializedName("completed")
    @Param(description = "the step completion time")
    private Date completed;

    public void setId(String id) {
        this.id = id;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public void setCompleted(Date completed) {
        this.completed = completed;
    }
}
