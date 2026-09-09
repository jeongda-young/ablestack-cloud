// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.dr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

final class DrFailbackDataEvidence {
    private final DrFailbackSessionVO session;
    private final List<String> missingFields;

    private DrFailbackDataEvidence(DrFailbackSessionVO session, List<String> missingFields) {
        this.session = session;
        this.missingFields = missingFields;
    }

    static DrFailbackDataEvidence from(DrFailbackSessionVO session) {
        List<String> missing = new ArrayList<String>();
        if (session == null) {
            missing.add("session");
            return new DrFailbackDataEvidence(null, missing);
        }
        require(session.getReplicationDirection(), "replication_direction", missing);
        require(session.getProviderPair(), "provider_pair", missing);
        if (session.getBaselineGeneration() == null) missing.add("baseline_generation");
        require(session.getBaselineState(), "baseline_state", missing);
        require(session.getTrackerState(), "tracker_state", missing);
        require(session.getWriterState(), "writer_state", missing);
        if (session.getTargetWritten() == null) missing.add("target_written");
        if (session.getWriteVerified() == null) missing.add("write_verified");
        require(session.getGuestCompatibilityState(), "reverse_guest_compatibility_state", missing);
        return new DrFailbackDataEvidence(session, missing);
    }

    private static void require(String value, String field, List<String> missing) {
        if (StringUtils.isBlank(value)) {
            missing.add(field);
        }
    }

    boolean isComplete() {
        return missingFields.isEmpty();
    }

    List<String> getMissingFields() {
        return Collections.unmodifiableList(missingFields);
    }

    DrFailbackSessionVO getSession() {
        return session;
    }
}
