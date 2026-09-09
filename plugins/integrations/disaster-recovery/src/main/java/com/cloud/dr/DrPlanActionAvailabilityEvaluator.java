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
import java.util.LinkedHashMap;
import java.util.Map;

class DrPlanActionAvailabilityEvaluator {
    static final String ACTIVE_RUN = "DR_ACTION_ACTIVE_RUN";
    static final String PLAN_DISABLED = "DR_ACTION_PLAN_DISABLED";
    static final String ENGINE_UNAVAILABLE = "DR_ACTION_ENGINE_UNAVAILABLE";
    static final String SOURCE_AUTHORITY_REQUIRED = "DR_ACTION_SOURCE_AUTHORITY_REQUIRED";
    static final String TARGET_AUTHORITY_REQUIRED = "DR_ACTION_TARGET_AUTHORITY_REQUIRED";
    static final String TARGET_NOT_READY = "DR_ACTION_TARGET_NOT_READY";
    static final String CUTOVER_NOT_READY = DrConstants.ACTION_REASON_CUTOVER_NOT_READY;
    static final String SCHEDULER_RUNNING = "DR_ACTION_SCHEDULER_RUNNING";
    static final String SCHEDULER_PAUSED = "DR_ACTION_SCHEDULER_PAUSED";
    static final String RECOVERY_REQUIRED = "DR_ACTION_RECOVERY_REQUIRED";
    static final String TEST_SESSION_ACTIVE = "DR_ACTION_TEST_SESSION_ACTIVE";
    static final String TEST_SESSION_NOT_ACTIVE = "DR_ACTION_TEST_SESSION_NOT_ACTIVE";
    static final String FENCE_CONFIRM_REQUIRED = "DR_ACTION_FENCE_CONFIRM_REQUIRED";
    static final String COMMITTED_TARGET_REQUIRED = "DR_ACTION_COMMITTED_TARGET_REQUIRED";
    static final String PROTECTION_RELEASE_REQUIRED = "DR_ACTION_PROTECTION_RELEASE_REQUIRED";
    static final String RELEASE_NOT_READY = "DR_ACTION_RELEASE_NOT_READY";
    static final String TRANSITION_IN_PROGRESS = "DR_ACTION_TRANSITION_IN_PROGRESS";
    static final String NOT_ELIGIBLE = "DR_ACTION_NOT_ELIGIBLE";
    static final String RUNTIME_RECONCILIATION_REQUIRED = "DR_ACTION_RUNTIME_RECONCILIATION_REQUIRED";

    Map<String, DrActionAvailability> evaluate(Map<String, Boolean> eligibility,
            DrPlanActionAvailabilityContext context) {
        Map<String, DrActionAvailability> result = new LinkedHashMap<String, DrActionAvailability>();
        add(result, eligibility, context, "update", true);
        add(result, eligibility, context, "delete", true);
        add(result, eligibility, context, "sync",
                context.sourceAuthority && !context.v2kPlan && !context.recoverSyncRequired);
        add(result, eligibility, context, "recoverSync",
                context.ftctlDrPlan && context.recoverSyncRequired);
        add(result, eligibility, context, "pauseSync",
                context.ftctlDrPlan && context.sourceAuthority && context.syncPausable && !context.syncPaused);
        add(result, eligibility, context, "resumeSync",
                context.ftctlDrPlan && context.sourceAuthority && context.syncPaused);
        add(result, eligibility, context, "testFailover",
                context.ftctlDrPlan && context.sourceAuthority && !context.testRunning);
        add(result, eligibility, context, "stopTestFailover",
                context.ftctlDrPlan && context.testRunning);
        add(result, eligibility, context, "failover",
                context.sourceAuthority && !context.testRunning);
        add(result, eligibility, context, "confirmFenceClear",
                context.legacyFtctlPlan && context.targetAuthority);
        add(result, eligibility, context, "failback",
                (context.ftctlDrPlan || context.legacyFtctlPlan) && context.targetAuthority);
        add(result, eligibility, context, "reprotect",
                (context.ftctlDrPlan || context.legacyFtctlPlan)
                        && context.failedOver && context.targetAuthority);
        add(result, eligibility, context, "adoptReplica", context.legacyFtctlPlan);
        add(result, eligibility, context, "releaseProtection",
                context.ftctlDrPlan && (context.runtimeResources
                        || context.protectedPlanState || context.ftctlReleaseReady));
        add(result, eligibility, context, "cancelRun", context.activeRun);
        return result;
    }

    private void add(Map<String, DrActionAvailability> result, Map<String, Boolean> eligibility,
            DrPlanActionAvailabilityContext context, String action, boolean applicable) {
        boolean enabled = Boolean.TRUE.equals(eligibility.get(action));
        String reasonCode = applicable && !enabled ? reasonCode(action, context) : null;
        Map<String, String> reasonArgs = reasonCode != null
                ? context.capabilityReasonArgs.getOrDefault(action, Collections.emptyMap())
                : Collections.emptyMap();
        result.put(action, new DrActionAvailability(applicable, applicable && enabled, reasonCode,
                reasonArgs));
    }

    private String reasonCode(String action, DrPlanActionAvailabilityContext context) {
        if (context.lifecycleTransition) {
            return TRANSITION_IN_PROGRESS;
        }
        if (context.runtimeReconciliationRequired && !"cancelRun".equals(action)) {
            return RUNTIME_RECONCILIATION_REQUIRED;
        }
        if (context.capabilityBlockingReasons.containsKey(action)) {
            return context.capabilityBlockingReasons.get(action);
        }
        if (context.activeRun && !"cancelRun".equals(action)) {
            return ACTIVE_RUN;
        }
        if (!context.planEnabled && !"update".equals(action) && !"delete".equals(action)) {
            return PLAN_DISABLED;
        }
        if (!context.hasEngine && !"update".equals(action) && !"delete".equals(action)) {
            return ENGINE_UNAVAILABLE;
        }
        if (context.nbdRecoveryRequired && !"recoverSync".equals(action)) {
            return RECOVERY_REQUIRED;
        }
        if ("delete".equals(action) && (context.runtimeResources || context.protectedPlanState)) {
            return PROTECTION_RELEASE_REQUIRED;
        }
        if ("pauseSync".equals(action) && context.syncPaused) {
            return SCHEDULER_PAUSED;
        }
        if ("resumeSync".equals(action) && !context.syncPaused) {
            return SCHEDULER_RUNNING;
        }
        if ("testFailover".equals(action) && context.testRunning) {
            return TEST_SESSION_ACTIVE;
        }
        if ("stopTestFailover".equals(action) && !context.testRunning) {
            return TEST_SESSION_NOT_ACTIVE;
        }
        if (requiresSourceAuthority(action) && !context.sourceAuthority) {
            return SOURCE_AUTHORITY_REQUIRED;
        }
        if (requiresTargetAuthority(action) && !context.targetAuthority) {
            return TARGET_AUTHORITY_REQUIRED;
        }
        if (requiresTargetReady(action) && !context.targetReady) {
            return TARGET_NOT_READY;
        }
        if (requiresTargetReady(action) && !context.normalCutoverReady) {
            return CUTOVER_NOT_READY;
        }
        if ("confirmFenceClear".equals(action) && !context.failedOver) {
            return FENCE_CONFIRM_REQUIRED;
        }
        if ("reprotect".equals(action) && !context.committedTargetAuthority) {
            return COMMITTED_TARGET_REQUIRED;
        }
        if ("releaseProtection".equals(action) && !context.ftctlReleaseReady) {
            return RELEASE_NOT_READY;
        }
        if (isFtctlControlAction(action) && !context.ftctlControlReady) {
            return NOT_ELIGIBLE;
        }
        return NOT_ELIGIBLE;
    }

    private boolean requiresSourceAuthority(String action) {
        return "sync".equals(action) || "recoverSync".equals(action)
                || "pauseSync".equals(action) || "resumeSync".equals(action)
                || "testFailover".equals(action) || "failover".equals(action);
    }

    private boolean requiresTargetAuthority(String action) {
        return "confirmFenceClear".equals(action) || "failback".equals(action)
                || "reprotect".equals(action);
    }

    private boolean requiresTargetReady(String action) {
        return "testFailover".equals(action) || "failover".equals(action);
    }

    private boolean isFtctlControlAction(String action) {
        return "pauseSync".equals(action) || "resumeSync".equals(action)
                || "testFailover".equals(action) || "stopTestFailover".equals(action)
                || "failover".equals(action) || "failback".equals(action)
                || "reprotect".equals(action) || "releaseProtection".equals(action);
    }
}
