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
package com.cloud.dr;

import java.util.Collections;
import java.util.Map;

class DrPlanActionAvailabilityContext {
    boolean planEnabled;
    boolean activeRun;
    boolean runtimeResources;
    boolean protectedPlanState;
    boolean hasEngine;
    boolean sourceAuthority;
    boolean targetAuthority;
    boolean v2kPlan;
    boolean legacyFtctlPlan;
    boolean ftctlDrPlan;
    boolean syncPausable;
    boolean syncPaused;
    boolean recoverSyncRequired;
    boolean testRunning;
    boolean targetReady;
    boolean normalCutoverReady;
    boolean failedOver;
    boolean committedTargetAuthority;
    boolean ftctlControlReady;
    boolean ftctlReleaseReady;
    boolean lifecycleTransition;
    boolean nbdRecoveryRequired;
    boolean runtimeReconciliationRequired;
    Map<String, String> capabilityBlockingReasons = Collections.emptyMap();
    Map<String, Map<String, String>> capabilityReasonArgs = Collections.emptyMap();
}
