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
package com.cloud.dr.adapter;

public class DrAdapterResult {
    private final boolean success;
    private final String errorCode;
    private final String message;
    private final String detailsJson;
    private final boolean terminal;
    private final String externalJobRef;
    private final boolean retryable;
    private final Integer retryAfterSeconds;

    private DrAdapterResult(boolean success, String errorCode, String message, String detailsJson, boolean terminal,
            String externalJobRef, boolean retryable, Integer retryAfterSeconds) {
        this.success = success;
        this.errorCode = errorCode;
        this.message = message;
        this.detailsJson = detailsJson;
        this.terminal = terminal;
        this.externalJobRef = externalJobRef;
        this.retryable = retryable;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static DrAdapterResult success(String message, String detailsJson) {
        return new DrAdapterResult(true, null, message, detailsJson, true, null, false, null);
    }

    public static DrAdapterResult accepted(String message, String detailsJson, String externalJobRef) {
        return new DrAdapterResult(true, null, message, detailsJson, false, externalJobRef, false, null);
    }

    public static DrAdapterResult failure(String errorCode, String message, String detailsJson) {
        return new DrAdapterResult(false, errorCode, message, detailsJson, true, null, false, null);
    }

    public static DrAdapterResult retryable(String errorCode, String message, String detailsJson, Integer retryAfterSeconds) {
        return new DrAdapterResult(false, errorCode, message, detailsJson, false, null, true, retryAfterSeconds);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public String getExternalJobRef() {
        return externalJobRef;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
