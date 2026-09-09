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
package com.cloud.dr.health;

import java.util.Date;

import com.google.gson.JsonObject;

public class DrSiteHealthCheckResult {
    private final String healthState;
    private final String reasonCode;
    private final String message;
    private final Long latencyMs;
    private final Date checkedAt;
    private final boolean credentialValidated;
    private final JsonObject details;

    public DrSiteHealthCheckResult(String healthState, String reasonCode, String message, Long latencyMs, Date checkedAt, boolean credentialValidated) {
        this(healthState, reasonCode, message, latencyMs, checkedAt, credentialValidated, null);
    }

    public DrSiteHealthCheckResult(String healthState, String reasonCode, String message, Long latencyMs, Date checkedAt,
            boolean credentialValidated, JsonObject details) {
        this.healthState = healthState;
        this.reasonCode = reasonCode;
        this.message = message;
        this.latencyMs = latencyMs;
        this.checkedAt = checkedAt == null ? new Date() : checkedAt;
        this.credentialValidated = credentialValidated;
        this.details = details == null ? new JsonObject() : details.deepCopy();
    }

    public String getHealthState() {
        return healthState;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getMessage() {
        return message;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public Date getCheckedAt() {
        return checkedAt;
    }

    public boolean isCredentialValidated() {
        return credentialValidated;
    }

    public JsonObject getDetails() {
        return details.deepCopy();
    }
}
