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

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dr.DrPlanVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

/**
 * Primitive-only result for asynchronous plan mutations. Full plan state is
 * intentionally read through getDrPlan after the async job completes.
 */
@EntityReference(value = DrPlanVO.class)
public class DrPlanMutationResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the DR plan ID")
    private String id;

    @SerializedName("name")
    @Param(description = "the DR plan name")
    private String name;

    @SerializedName("state")
    @Param(description = "the persisted DR plan state at mutation completion")
    private String state;

    @SerializedName("initialrunid")
    @Param(description = "the initial DR run ID when one was requested")
    private String initialRunId;

    @SerializedName("operation")
    @Param(description = "the completed plan mutation")
    private String operation;

    public DrPlanMutationResponse() {
        setObjectName("drplanmutation");
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setInitialRunId(String initialRunId) {
        this.initialRunId = initialRunId;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
