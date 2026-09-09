// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.dr;

public class DrCurrentAuthorityProjection {
    private final String authoritySide;
    private final String authorityPhase;
    private final Long authoritySequence;
    private final boolean consistent;
    private final String inconsistencyCode;
    private final String inconsistencyMessage;
    private final DrCutoverSessionVO currentCutoverSession;
    private final String transitionType;
    private final String transitionState;
    private final String transitionRunUuid;
    private final Long requiredCheckpointSequence;

    public DrCurrentAuthorityProjection(String authoritySide, String authorityPhase, Long authoritySequence,
            boolean consistent, String inconsistencyCode, String inconsistencyMessage,
            DrCutoverSessionVO currentCutoverSession) {
        this(authoritySide, authorityPhase, authoritySequence, consistent, inconsistencyCode,
                inconsistencyMessage, currentCutoverSession, null, null, null, null);
    }

    public DrCurrentAuthorityProjection(String authoritySide, String authorityPhase, Long authoritySequence,
            boolean consistent, String inconsistencyCode, String inconsistencyMessage,
            DrCutoverSessionVO currentCutoverSession, String transitionType, String transitionState,
            String transitionRunUuid, Long requiredCheckpointSequence) {
        this.authoritySide = authoritySide;
        this.authorityPhase = authorityPhase;
        this.authoritySequence = authoritySequence;
        this.consistent = consistent;
        this.inconsistencyCode = inconsistencyCode;
        this.inconsistencyMessage = inconsistencyMessage;
        this.currentCutoverSession = currentCutoverSession;
        this.transitionType = transitionType;
        this.transitionState = transitionState;
        this.transitionRunUuid = transitionRunUuid;
        this.requiredCheckpointSequence = requiredCheckpointSequence;
    }

    public String getAuthoritySide() { return authoritySide; }
    public String getAuthorityPhase() { return authorityPhase; }
    public Long getAuthoritySequence() { return authoritySequence; }
    public boolean isConsistent() { return consistent; }
    public String getInconsistencyCode() { return inconsistencyCode; }
    public String getInconsistencyMessage() { return inconsistencyMessage; }
    public DrCutoverSessionVO getCurrentCutoverSession() { return currentCutoverSession; }
    public String getTransitionType() { return transitionType; }
    public String getTransitionState() { return transitionState; }
    public String getTransitionRunUuid() { return transitionRunUuid; }
    public Long getRequiredCheckpointSequence() { return requiredCheckpointSequence; }
}
