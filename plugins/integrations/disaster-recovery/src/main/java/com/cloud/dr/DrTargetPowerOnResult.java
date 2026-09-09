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

import java.util.Date;

public class DrTargetPowerOnResult {
    private final long targetVmId;
    private final String targetVmUuid;
    private final String powerState;
    private final String bootValidationState;
    private final Date powerOnAt;
    private final Date bootValidatedAt;
    private final boolean alreadyRunning;

    public DrTargetPowerOnResult(long targetVmId, String targetVmUuid, String powerState,
            String bootValidationState, Date powerOnAt, Date bootValidatedAt, boolean alreadyRunning) {
        this.targetVmId = targetVmId;
        this.targetVmUuid = targetVmUuid;
        this.powerState = powerState;
        this.bootValidationState = bootValidationState;
        this.powerOnAt = powerOnAt;
        this.bootValidatedAt = bootValidatedAt;
        this.alreadyRunning = alreadyRunning;
    }

    public long getTargetVmId() { return targetVmId; }
    public String getTargetVmUuid() { return targetVmUuid; }
    public String getPowerState() { return powerState; }
    public String getBootValidationState() { return bootValidationState; }
    public Date getPowerOnAt() { return powerOnAt; }
    public Date getBootValidatedAt() { return bootValidatedAt; }
    public boolean isAlreadyRunning() { return alreadyRunning; }
    public boolean isReady() {
        return "POWERED_ON".equals(powerState)
                && ("POWER_STATE_VALIDATED".equals(bootValidationState)
                        || "GUEST_HEARTBEAT_VALIDATED".equals(bootValidationState));
    }
}
