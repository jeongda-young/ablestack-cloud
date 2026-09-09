// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.dr;

import java.util.Date;

public class DrFailbackPreflightStage {
    public static final String STATE_READY = "READY";
    public static final String STATE_BLOCKED = "BLOCKED";
    public static final String STATE_NOT_RUN = "NOT_RUN";

    private final String code;
    private final String state;
    private final String errorCode;
    private final String message;
    private final String observedBy;
    private final Date observedAt;

    private DrFailbackPreflightStage(String code, String state, String errorCode,
            String message, String observedBy, Date observedAt) {
        this.code = code;
        this.state = state;
        this.errorCode = errorCode;
        this.message = message;
        this.observedBy = observedBy;
        this.observedAt = observedAt;
    }

    public static DrFailbackPreflightStage ready(String code, String observedBy, String message) {
        return new DrFailbackPreflightStage(code, STATE_READY, null, message, observedBy, new Date());
    }

    public static DrFailbackPreflightStage blocked(String code, String errorCode,
            String message, String observedBy) {
        return new DrFailbackPreflightStage(code, STATE_BLOCKED, errorCode, message,
                observedBy, new Date());
    }

    public static DrFailbackPreflightStage notRun(String code, String message) {
        return new DrFailbackPreflightStage(code, STATE_NOT_RUN, null, message, null, null);
    }

    public String getCode() { return code; }
    public String getState() { return state; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public String getObservedBy() { return observedBy; }
    public Date getObservedAt() { return observedAt; }
}
