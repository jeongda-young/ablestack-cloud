// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
package com.cloud.hypervisor.kvm.resource;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.utils.qemu.QemuCommand;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Executes only enum-owned, fixed guest commands. API or UI strings never
 * become an executable path or argument.
 */
public class BoundedQgaGuestExec {
    private static final long POLL_INTERVAL_MILLIS = 50L;

    public enum Operation {
        ABLESTACK_NETWORK_SNAPSHOT(
                "/usr/libexec/ablestack-qemu-exec-tools/guest-network-snapshot",
                Arrays.asList("--schema", "1", "--sections", "addresses,routes,dns"));

        private final String path;
        private final List<String> arguments;

        Operation(String path, List<String> arguments) {
            this.path = path;
            this.arguments = Collections.unmodifiableList(arguments);
        }
    }

    @FunctionalInterface
    public interface AgentCommandExecutor {
        String execute(String command, int timeoutSeconds) throws Exception;
    }

    public String execute(AgentCommandExecutor executor, Operation operation,
            int timeoutSeconds, int maxOutputBytes) throws Exception {
        JsonObject launch = new JsonObject();
        launch.addProperty("execute", QemuCommand.AGENT_EXEC);
        JsonObject parameters = new JsonObject();
        parameters.addProperty("path", operation.path);
        JsonArray arguments = new JsonArray();
        operation.arguments.forEach(arguments::add);
        parameters.add("arg", arguments);
        parameters.addProperty("capture-output", true);
        launch.add("arguments", parameters);

        long pid = returnObject(executeAgentCommand(executor, launch.toString(), timeoutSeconds))
                .get("pid").getAsLong();
        int attempts = Math.max(2, timeoutSeconds * 20);
        for (int attempt = 0; attempt < attempts; attempt++) {
            JsonObject statusRequest = new JsonObject();
            statusRequest.addProperty("execute", QemuCommand.AGENT_EXEC_STATUS);
            JsonObject statusParameters = new JsonObject();
            statusParameters.addProperty("pid", pid);
            statusRequest.add("arguments", statusParameters);
            JsonObject status = returnObject(
                    executeAgentCommand(executor, statusRequest.toString(), timeoutSeconds));
            if (status.has("exited") && status.get("exited").getAsBoolean()) {
                int exitCode = status.has("exitcode") ? status.get("exitcode").getAsInt() : -1;
                String stdout = decode(status.get("out-data"), maxOutputBytes);
                String stderr = decode(status.get("err-data"), maxOutputBytes);
                if (exitCode != 0) {
                    throw new GuestExecFailure(classify(stderr),
                            "Guest helper exited with " + exitCode + ": " + stderr);
                }
                return stdout;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GuestExecFailure("EXEC_INTERRUPTED", "Guest helper was interrupted", e);
            }
        }
        throw new GuestExecFailure("EXEC_TIMEOUT", "Guest helper timed out");
    }

    private String executeAgentCommand(AgentCommandExecutor executor, String command,
            int timeoutSeconds) {
        try {
            return executor.execute(command, timeoutSeconds);
        } catch (GuestExecFailure e) {
            throw e;
        } catch (Exception e) {
            String details = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new GuestExecFailure(classify(details), details, e);
        }
    }

    private JsonObject returnObject(String json) {
        JsonElement parsed = new JsonParser().parse(json);
        if (!parsed.isJsonObject()) {
            throw new GuestExecFailure("INVALID_JSON", "QGA response is not an object");
        }
        JsonObject response = parsed.getAsJsonObject();
        if (response.has("return") && response.get("return").isJsonObject()) {
            return response.getAsJsonObject("return");
        }
        String details = response.has("error") ? response.get("error").toString()
                : "QGA response has no object result";
        throw new GuestExecFailure(classify(details), details);
    }

    private String decode(JsonElement encoded, int maxBytes) {
        if (encoded == null || encoded.isJsonNull()) {
            return "";
        }
        String value = encoded.getAsString();
        if (value.length() > (long) maxBytes * 2L) {
            throw new GuestExecFailure("OUTPUT_LIMIT_EXCEEDED",
                    "Encoded helper output exceeds limit");
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(value);
            if (bytes.length > maxBytes) {
                throw new GuestExecFailure("OUTPUT_LIMIT_EXCEEDED",
                        "Decoded helper output exceeds limit");
            }
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (GuestExecFailure e) {
            throw e;
        } catch (Exception e) {
            throw new GuestExecFailure("INVALID_UTF8",
                    "Helper output is not valid base64 UTF-8", e);
        }
    }

    private String classify(String details) {
        String normalized = details == null ? "" : details.toLowerCase();
        if (normalized.contains("permission denied")) {
            return "EXEC_PERMISSION_DENIED";
        }
        if (normalized.contains("no such file") || normalized.contains("not found")) {
            return "HELPER_NOT_INSTALLED";
        }
        return "EXEC_EXIT_NONZERO";
    }

    public static class GuestExecFailure extends RuntimeException {
        private final String errorCode;

        public GuestExecFailure(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public GuestExecFailure(String errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
