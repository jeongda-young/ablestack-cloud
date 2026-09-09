// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.agent.api;

public class FtctlDrReversePreflightCommand extends Command {
    private String planUuid;
    private String profileJson;
    private String operationIntent;
    private String requestedMode;

    public FtctlDrReversePreflightCommand() {
    }

    public FtctlDrReversePreflightCommand(String planUuid, String profileJson,
            String operationIntent, String requestedMode) {
        this.planUuid = planUuid;
        this.profileJson = profileJson;
        this.operationIntent = operationIntent;
        this.requestedMode = requestedMode;
    }

    public String getPlanUuid() { return planUuid; }
    public String getProfileJson() { return profileJson; }
    public String getOperationIntent() { return operationIntent; }
    public String getRequestedMode() { return requestedMode; }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
