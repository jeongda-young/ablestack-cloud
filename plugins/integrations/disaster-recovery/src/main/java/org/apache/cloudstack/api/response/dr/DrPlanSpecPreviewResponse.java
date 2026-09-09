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

import java.util.List;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DrPlanSpecPreviewResponse extends BaseResponse {
    @SerializedName("mappingjson")
    @Param(description = "generated DR plan mapping JSON")
    private String mappingJson;

    @SerializedName("schedulejson")
    @Param(description = "generated DR plan schedule JSON")
    private String scheduleJson;

    @SerializedName("policyjson")
    @Param(description = "generated DR plan policy JSON")
    private String policyJson;

    @SerializedName("quiescepolicyjson")
    @Param(description = "generated DR plan quiesce policy JSON")
    private String quiescePolicyJson;

    @SerializedName("sourcehardwarejson")
    @Param(description = "backend-discovered source VM hardware JSON")
    private String sourceHardwareJson;

    @SerializedName("resolvedtargethardwarejson")
    @Param(description = "backend-resolved target VM hardware JSON")
    private String resolvedTargetHardwareJson;

    @SerializedName("warnings")
    @Param(description = "non-blocking warnings from guided spec generation")
    private List<String> warnings;

    @SerializedName("readinessstate")
    @Param(description = "the backend calculated DR plan readiness state")
    private String readinessState;

    @SerializedName("executionready")
    @Param(description = "true if the generated plan spec can start a protection sync")
    private Boolean executionReady;

    @SerializedName("releaseready")
    @Param(description = "true if the generated plan spec has releaseable runtime resources")
    private Boolean releaseReady;

    @SerializedName("blockingreasons")
    @Param(description = "blocking readiness reason codes")
    private List<String> blockingReasons;

    public void setMappingJson(String mappingJson) {
        this.mappingJson = mappingJson;
    }

    public void setScheduleJson(String scheduleJson) {
        this.scheduleJson = scheduleJson;
    }

    public void setPolicyJson(String policyJson) {
        this.policyJson = policyJson;
    }

    public void setQuiescePolicyJson(String quiescePolicyJson) {
        this.quiescePolicyJson = quiescePolicyJson;
    }

    public void setSourceHardwareJson(String sourceHardwareJson) {
        this.sourceHardwareJson = sourceHardwareJson;
    }

    public void setResolvedTargetHardwareJson(String resolvedTargetHardwareJson) {
        this.resolvedTargetHardwareJson = resolvedTargetHardwareJson;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public void setReadinessState(String readinessState) {
        this.readinessState = readinessState;
    }

    public void setExecutionReady(Boolean executionReady) {
        this.executionReady = executionReady;
    }

    public void setReleaseReady(Boolean releaseReady) {
        this.releaseReady = releaseReady;
    }

    public void setBlockingReasons(List<String> blockingReasons) {
        this.blockingReasons = blockingReasons;
    }
}
