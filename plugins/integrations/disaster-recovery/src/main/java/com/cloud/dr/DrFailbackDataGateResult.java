// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.dr;

public final class DrFailbackDataGateResult {
    private final boolean ready;
    private final String errorCode;
    private final String message;

    private DrFailbackDataGateResult(boolean ready, String errorCode, String message) {
        this.ready = ready;
        this.errorCode = errorCode;
        this.message = message;
    }

    public static DrFailbackDataGateResult ready() {
        return new DrFailbackDataGateResult(true, null, null);
    }

    public static DrFailbackDataGateResult blocked(String errorCode, String message) {
        return new DrFailbackDataGateResult(false, errorCode, message);
    }

    public boolean isReady() { return ready; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
}
