// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.dr;

import org.apache.commons.lang3.StringUtils;

public final class DrFailbackSessionState {
    private DrFailbackSessionState() {
    }

    public static boolean isTerminalRunFailureWithoutEngineArtifacts(DrFailbackSessionVO session,
            DrRunVO run) {
        if (session == null || run == null || run.getCompleted() == null
                || !StringUtils.equals(run.getRunType(), DrConstants.RUN_TYPE_FAILBACK)
                || !StringUtils.equalsAny(run.getState(), DrConstants.RUN_STATE_FAILED,
                        DrConstants.RUN_STATE_CANCELED)) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(session.getState(), "REQUESTED")
                && StringUtils.equalsIgnoreCase(session.getAcceptanceState(), "SUBMITTED")
                && StringUtils.equalsAnyIgnoreCase(session.getEngineAckState(), null, "", "PENDING")
                && StringUtils.equalsAnyIgnoreCase(session.getCommitOutcome(), null, "", "PENDING")
                && StringUtils.equalsAnyIgnoreCase(session.getRollbackState(), null, "", "NONE")
                && session.getCheckpointSequence() == null
                && session.getAuthorityGeneration() == null
                && session.getDataReadyAt() == null
                && session.getTargetStoppedAt() == null
                && session.getSourcePoweredOnAt() == null
                && session.getCommitRequestedAt() == null
                && session.getRollbackRequestedAt() == null
                && !Boolean.TRUE.equals(session.getWorkerPidAlive());
    }
}
