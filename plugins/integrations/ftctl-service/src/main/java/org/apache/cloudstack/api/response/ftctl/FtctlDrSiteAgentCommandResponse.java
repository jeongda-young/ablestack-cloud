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
package org.apache.cloudstack.api.response.ftctl;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class FtctlDrSiteAgentCommandResponse extends BaseResponse {
    @SerializedName("commandtype") @Param(description = "the allow-listed FTCTL DR command type")
    private String commandType;
    @SerializedName("workerhostuuid") @Param(description = "the site-local worker host UUID")
    private String workerHostUuid;
    @SerializedName("result") @Param(description = "whether the Agent command succeeded")
    private boolean result;
    @SerializedName("details") @Param(description = "the Agent answer details")
    private String details;
    @SerializedName("answerclass") @Param(description = "the typed Agent answer class")
    private String answerClass;
    @SerializedName("answerjson") @Param(description = "the typed Agent answer JSON")
    private String answerJson;

    public void setCommandType(String value) { commandType = value; }
    public void setWorkerHostUuid(String value) { workerHostUuid = value; }
    public void setResult(boolean value) { result = value; }
    public void setDetails(String value) { details = value; }
    public void setAnswerClass(String value) { answerClass = value; }
    public void setAnswerJson(String value) { answerJson = value; }

    public String getCommandType() { return commandType; }
    public String getWorkerHostUuid() { return workerHostUuid; }
    public boolean getResult() { return result; }
    public String getDetails() { return details; }
    public String getAnswerClass() { return answerClass; }
    public String getAnswerJson() { return answerJson; }
}
