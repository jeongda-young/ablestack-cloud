// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.dr;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;

final class DrFailbackCommitEnvelope {
    static final String CONTRACT_VERSION = "DR_FAILBACK_COMMIT_V1";
    private static final Gson GSON = new Gson();

    private DrFailbackCommitEnvelope() {
    }

    static String sha256(DrPlanVO plan, DrRunVO run, DrFailbackSessionVO session,
            String targetPowerState, String sourcePowerState, String bootValidationState) {
        Map<String, Object> fields = new TreeMap<>();
        fields.put("authorityGeneration", session.getAuthorityGeneration());
        fields.put("baselineGeneration", session.getBaselineGeneration());
        fields.put("bootValidationState", bootValidationState);
        fields.put("checkpointSequence", session.getCheckpointSequence());
        fields.put("commitAttemptId", session.getCommitAttemptId());
        fields.put("contractVersion", CONTRACT_VERSION);
        fields.put("evidenceRunUuid", run.getUuid());
        fields.put("failbackSessionId", session.getEngineSessionId());
        fields.put("planUuid", plan.getUuid());
        fields.put("runUuid", run.getUuid());
        fields.put("sourcePowerState", sourcePowerState);
        fields.put("targetPowerState", targetPowerState);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(GSON.toJson(fields).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("SHA-256 is unavailable", e);
        }
    }
}
