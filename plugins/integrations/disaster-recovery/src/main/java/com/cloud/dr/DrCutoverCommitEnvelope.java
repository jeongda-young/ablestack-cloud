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

public final class DrCutoverCommitEnvelope {
    public static final String CONTRACT_VERSION = "DR_CUTOVER_COMMIT_V2";
    private static final Gson GSON = new Gson();

    private DrCutoverCommitEnvelope() {
    }

    public static String sha256(DrPlanVO plan, DrRunVO run, DrCutoverSessionVO session,
            String engineSessionId, long targetVmId, String targetExternalRef,
            String targetPowerState, String bootValidationState,
            String sourceFenceState, String sourcePowerState) {
        Map<String, Object> fields = new TreeMap<>();
        fields.put("authorityGeneration", session.getCloudAuthorityGeneration());
        fields.put("bootValidationState", bootValidationState);
        fields.put("checkpointSequence", session.getCheckpointSequence());
        fields.put("cloudCutoverSessionUuid", session.getUuid());
        fields.put("commitAttemptId", session.getCommitAttemptId());
        fields.put("contractVersion", CONTRACT_VERSION);
        fields.put("engineSessionId", engineSessionId);
        fields.put("manifestSha256", session.getManifestSha256());
        fields.put("planUuid", plan.getUuid());
        fields.put("runUuid", run.getUuid());
        fields.put("sourceFenceState", sourceFenceState);
        fields.put("sourcePowerState", sourcePowerState);
        fields.put("targetExternalRef", targetExternalRef);
        fields.put("targetPowerState", targetPowerState);
        fields.put("targetVmId", targetVmId);
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
