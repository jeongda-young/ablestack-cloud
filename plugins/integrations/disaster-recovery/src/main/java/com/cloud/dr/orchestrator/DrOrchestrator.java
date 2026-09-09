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
package com.cloud.dr.orchestrator;

import com.cloud.dr.DrEventVO;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrRunStepVO;
import com.cloud.dr.DrRunVO;

public interface DrOrchestrator {
    DrRunVO createRun(long planId, String runType, String idempotencyKey, Long requestedByUserId, Long asyncJobId);

    DrRunVO createRun(long planId, String runType, String idempotencyKey, Long requestedByUserId, Long asyncJobId, String requestJson);

    DrRunVO executeRun(long runId);

    DrPlanVO transitionPlan(long planId, String state, String errorCode, String errorMessage);

    DrRunStepVO recordStep(long runId, String stepName, int stepOrder, String state, Integer progress, String detailsJson);

    DrEventVO recordEvent(Long planId, Long runId, String eventType, String severity, String source, String message, String detailsJson);

    DrRunVO handleFailure(long runId, String errorCode, String errorMessage);
}
