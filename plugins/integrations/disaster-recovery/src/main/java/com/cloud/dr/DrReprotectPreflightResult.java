// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0.
package com.cloud.dr;

public class DrReprotectPreflightResult {
    private final boolean ready;
    private final String errorCode;
    private final String message;
    private final DrReprotectAuthoritySpec authoritySpec;

    private DrReprotectPreflightResult(boolean ready, String errorCode, String message,
            DrReprotectAuthoritySpec authoritySpec) {
        this.ready = ready;
        this.errorCode = errorCode;
        this.message = message;
        this.authoritySpec = authoritySpec;
    }

    public static DrReprotectPreflightResult success(DrReprotectAuthoritySpec authoritySpec) {
        return new DrReprotectPreflightResult(true, null, null, authoritySpec);
    }

    public static DrReprotectPreflightResult failure(String errorCode, String message) {
        return new DrReprotectPreflightResult(false, errorCode, message, null);
    }

    public boolean isReady() { return ready; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public DrReprotectAuthoritySpec getAuthoritySpec() { return authoritySpec; }
}
