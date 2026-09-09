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

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrRunVO;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Immutable routing policy for a Failover Run. Planned failover may contact
 * the source for a final delta; disaster failover must be executable from the
 * controller/target site using only the last durable checkpoint.
 */
public final class DrFailoverExecutionPolicy {

    public static final String MODE_PLANNED = "planned";
    public static final String MODE_DISASTER = "disaster";
    public static final String SCOPE_REMOTE_SOURCE = "REMOTE_SOURCE";
    public static final String SCOPE_TARGET_DISASTER = "TARGET_DISASTER";

    private DrFailoverExecutionPolicy() {
    }

    public static String mode(DrRunVO run) {
        if (run == null || !StringUtils.equalsIgnoreCase(run.getRunType(), DrConstants.RUN_TYPE_FAILOVER)) {
            return MODE_PLANNED;
        }
        try {
            JsonObject request = JsonParser.parseString(StringUtils.defaultIfBlank(run.getRequestJson(), "{}"))
                    .getAsJsonObject();
            String mode = request.has("mode") && !request.get("mode").isJsonNull()
                    ? request.get("mode").getAsString() : MODE_PLANNED;
            return StringUtils.equalsIgnoreCase(mode, MODE_DISASTER) ? MODE_DISASTER : MODE_PLANNED;
        } catch (RuntimeException ignored) {
            return MODE_PLANNED;
        }
    }

    public static boolean isDisaster(DrRunVO run) {
        return StringUtils.equals(mode(run), MODE_DISASTER);
    }

    public static boolean usesRemoteSource(DrPlanVO plan, DrRunVO run, String actionName,
            boolean remoteSourceAvailable) {
        if (!remoteSourceAvailable || plan == null
                || !StringUtils.equalsIgnoreCase(plan.getActiveSide(), DrConstants.AUTHORITY_SIDE_SOURCE)) {
            return false;
        }
        String action = StringUtils.upperCase(StringUtils.defaultString(actionName), Locale.ROOT);
        if (StringUtils.equals(action, DrConstants.RUN_TYPE_FAILOVER) && isDisaster(run)) {
            return false;
        }
        return StringUtils.equalsAny(action, DrConstants.RUN_TYPE_SYNC, DrConstants.RUN_TYPE_RECOVER_SYNC,
                DrConstants.RUN_TYPE_FAILOVER, DrConstants.RUN_TYPE_PAUSE_SYNC,
                DrConstants.RUN_TYPE_RESUME_SYNC, DrConstants.RUN_TYPE_RELEASE);
    }

    public static String schedulerTransitionScope(DrPlanVO plan, DrRunVO run, boolean remoteKvmPlan) {
        if (!remoteKvmPlan) {
            return null;
        }
        return isDisaster(run) ? SCOPE_TARGET_DISASTER : SCOPE_REMOTE_SOURCE;
    }
}
