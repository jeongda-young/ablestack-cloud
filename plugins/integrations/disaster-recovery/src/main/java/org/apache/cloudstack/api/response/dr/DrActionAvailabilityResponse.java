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

import java.util.Map;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DrActionAvailabilityResponse extends BaseResponse {
    @SerializedName("applicable")
    @Param(description = "true when the action is relevant to the current DR plan state")
    private Boolean applicable;

    @SerializedName("enabled")
    @Param(description = "true when the action can be requested immediately")
    private Boolean enabled;

    @SerializedName("reasoncode")
    @Param(description = "stable reason code when an applicable action is disabled")
    private String reasonCode;

    @SerializedName("reasonargs")
    @Param(description = "non-sensitive reason message arguments")
    private Map<String, String> reasonArgs;

    public void setApplicable(Boolean applicable) {
        this.applicable = applicable;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public void setReasonArgs(Map<String, String> reasonArgs) {
        this.reasonArgs = reasonArgs;
    }
}
