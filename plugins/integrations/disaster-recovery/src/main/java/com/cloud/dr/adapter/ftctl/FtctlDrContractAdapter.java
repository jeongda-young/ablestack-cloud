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
package com.cloud.dr.adapter.ftctl;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.adapter.DrAdapterResult;
import com.cloud.dr.adapter.DrExecutionContext;
import com.cloud.dr.adapter.DrReplicationEngine;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class FtctlDrContractAdapter implements DrReplicationEngine {
    private static final Gson GSON = new Gson();

    @Override
    public String getEngineType() {
        return DrConstants.ENGINE_TYPE_FTCTL_DR;
    }

    @Override
    public String getEngineBindingType() {
        return DrConstants.ENGINE_BINDING_TYPE_FTCTL_DR;
    }

    @Override
    public DrAdapterResult validatePlan(DrPlanVO plan) {
        if (plan == null) {
            return DrAdapterResult.failure(DrConstants.ERROR_PLAN_NOT_FOUND, "FTCTL_DR plan is required", null);
        }
        String direction = StringUtils.upperCase(plan.getDirection());
        if (!StringUtils.equalsAny(direction, DrConstants.DIRECTION_KVM_TO_KVM, DrConstants.DIRECTION_KVM_TO_VMWARE,
                DrConstants.DIRECTION_VMWARE_TO_VMWARE, DrConstants.DIRECTION_VMWARE_TO_KVM)) {
            return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_UNSUPPORTED,
                    "FTCTL_DR does not support direction " + plan.getDirection(), validationDetails(plan));
        }
        return DrAdapterResult.success("FTCTL_DR plan contract is valid", validationDetails(plan));
    }

    @Override
    public DrAdapterResult execute(DrExecutionContext context) {
        JsonObject details = new JsonObject();
        DrPlanVO plan = context.getPlan();
        details.addProperty("engineType", getEngineType());
        details.addProperty("engineBindingType", getEngineBindingType());
        details.addProperty("direction", plan != null ? plan.getDirection() : null);
        details.addProperty("runType", context.getRun() != null ? context.getRun().getRunType() : null);
        details.addProperty("contractReady", true);
        details.addProperty("runtimeDispatchReady", false);
        details.addProperty("nextImplementationStage", "cloud-agent-ftctl-dr-dispatch");
        return DrAdapterResult.failure(DrConstants.ERROR_ENGINE_UNAVAILABLE,
                "FTCTL_DR runtime dispatch is not implemented in this stage yet", GSON.toJson(details));
    }

    private String validationDetails(DrPlanVO plan) {
        JsonObject details = new JsonObject();
        details.addProperty("engineType", getEngineType());
        details.addProperty("engineBindingType", getEngineBindingType());
        if (plan != null) {
            details.addProperty("planId", plan.getId());
            details.addProperty("direction", plan.getDirection());
            details.addProperty("sourceWorkerHostId", plan.getSourceWorkerHostId());
            details.addProperty("targetWorkerHostId", plan.getTargetWorkerHostId());
            details.addProperty("coordinatorWorkerHostId", plan.getCoordinatorWorkerHostId());
        }
        details.addProperty("supportsContinuousReplication", true);
        details.addProperty("supportsTestFailover", true);
        details.addProperty("supportsFailback", true);
        return GSON.toJson(details);
    }
}
