// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.hypervisor.kvm.resource;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.apache.cloudstack.utils.qemu.QemuCommand;

import com.cloud.agent.api.VmGuestRoute;
import com.cloud.hypervisor.kvm.resource.QemuGuestNetworkStateParser.RouteParseResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QemuGuestRouteFallback {
    static final String LINUX_IP_SBIN = "/usr/sbin/ip";
    static final String LINUX_IP_BIN = "/usr/bin/ip";
    static final String WINDOWS_POWERSHELL =
            "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
    static final String WINDOWS_ROUTE_SCRIPT = "Get-NetRoute | Select-Object "
            + "AddressFamily,DestinationPrefix,NextHop,InterfaceAlias,RouteMetric,PolicyStore,Protocol "
            + "| ConvertTo-Json -Compress";
    private static final long POLL_INTERVAL_MILLIS = 50L;

    private final QemuGuestNetworkStateParser parser;

    public QemuGuestRouteFallback(QemuGuestNetworkStateParser parser) {
        this.parser = parser;
    }

    public FallbackResult collect(AgentCommandExecutor executor, QemuGuestOsFamilyResolution os,
            int timeoutSeconds, int maxOutputBytes) throws Exception {
        if (os != null) {
            switch (os.getFamily()) {
                case LINUX:
                    return collectLinux(executor, timeoutSeconds, maxOutputBytes);
                case WINDOWS:
                    return collectWindows(executor, timeoutSeconds, maxOutputBytes);
                case UNSUPPORTED:
                default:
                    break;
            }
        }
        throw new UnsupportedOperationException("Unsupported guest OS for route fallback: "
                + (os == null ? "id=-, kernel-name=-, name=-, pretty-name=-" : os.describe()));
    }

    private FallbackResult collectLinux(AgentCommandExecutor executor,
            int timeoutSeconds, int maxOutputBytes) throws Exception {
        String executable = LINUX_IP_SBIN;
        String ipv4;
        try {
            ipv4 = execute(executor, executable,
                    Arrays.asList("-j", "-4", "route", "show", "table", "all"),
                    timeoutSeconds, maxOutputBytes);
        } catch (GuestExecException e) {
            if (!isExecutableMissing(e.getMessage())) {
                throw e;
            }
            executable = LINUX_IP_BIN;
            ipv4 = execute(executor, executable,
                    Arrays.asList("-j", "-4", "route", "show", "table", "all"),
                    timeoutSeconds, maxOutputBytes);
        }
        String ipv6 = execute(executor, executable,
                Arrays.asList("-j", "-6", "route", "show", "table", "all"),
                timeoutSeconds, maxOutputBytes);
        RouteParseResult ipv4Routes = parser.parseLinuxRoutes(ipv4, "ipv4");
        RouteParseResult ipv6Routes = parser.parseLinuxRoutes(ipv6, "ipv6");
        List<VmGuestRoute> routes = new ArrayList<>(ipv4Routes.getRoutes());
        routes.addAll(ipv6Routes.getRoutes());
        int originalCount = ipv4Routes.getOriginalCount() + ipv6Routes.getOriginalCount();
        boolean truncated = ipv4Routes.isTruncated() || ipv6Routes.isTruncated()
                || routes.size() > QemuGuestNetworkStateParser.MAX_ROUTES;
        if (routes.size() > QemuGuestNetworkStateParser.MAX_ROUTES) {
            routes = new ArrayList<>(
                    routes.subList(0, QemuGuestNetworkStateParser.MAX_ROUTES));
        }
        return new FallbackResult(routes, truncated, originalCount,
                "guest-exec-linux-ip");
    }

    private boolean isExecutableMissing(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("no such file")
                || normalized.contains("not found")
                || normalized.contains("does not contain a pid");
    }

    private FallbackResult collectWindows(AgentCommandExecutor executor,
            int timeoutSeconds, int maxOutputBytes) throws Exception {
        String output = execute(executor, WINDOWS_POWERSHELL,
                Arrays.asList("-NoProfile", "-NonInteractive", "-Command", WINDOWS_ROUTE_SCRIPT),
                timeoutSeconds, maxOutputBytes);
        RouteParseResult result = parser.parseWindowsRoutes(output);
        return new FallbackResult(result.getRoutes(), result.isTruncated(),
                result.getOriginalCount(), "guest-exec-windows-get-net-route");
    }

    private String execute(AgentCommandExecutor executor, String path, List<String> arguments,
            int timeoutSeconds, int maxOutputBytes) throws Exception {
        String launchResponse = executor.execute(buildGuestExec(path, arguments), timeoutSeconds);
        long pid = parsePid(launchResponse);
        int attempts = Math.max(2, timeoutSeconds * 20);
        for (int attempt = 0; attempt < attempts; attempt++) {
            String statusResponse = executor.execute(buildGuestExecStatus(pid), timeoutSeconds);
            JsonObject status = parseReturnObject(statusResponse);
            if (status.has("exited") && status.get("exited").getAsBoolean()) {
                int exitCode = status.has("exitcode") ? status.get("exitcode").getAsInt() : -1;
                String stdout = decode(status.get("out-data"), maxOutputBytes);
                String stderr = decode(status.get("err-data"), maxOutputBytes);
                if (exitCode != 0) {
                    throw new GuestExecException("Allowlisted guest-exec failed with exit code "
                            + exitCode + ": " + stderr);
                }
                return stdout;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                try {
                    executor.execute(buildGuestExecStatus(pid), timeoutSeconds);
                } catch (Exception cleanupError) {
                    e.addSuppressed(cleanupError);
                }
                throw new GuestExecException("Allowlisted guest-exec was interrupted", e);
            }
        }
        GuestExecException timeout = new GuestExecException("Allowlisted guest-exec timed out");
        try {
            executor.execute(buildGuestExecStatus(pid), timeoutSeconds);
        } catch (Exception cleanupError) {
            timeout.addSuppressed(cleanupError);
        }
        throw timeout;
    }

    String buildGuestExec(String path, List<String> arguments) {
        if (!isAllowed(path, arguments)) {
            throw new IllegalArgumentException("guest-exec invocation is not allowlisted");
        }
        JsonObject request = new JsonObject();
        request.addProperty("execute", QemuCommand.AGENT_EXEC);
        JsonObject params = new JsonObject();
        params.addProperty("path", path);
        JsonArray args = new JsonArray();
        arguments.forEach(args::add);
        params.add("arg", args);
        params.addProperty("capture-output", true);
        request.add("arguments", params);
        return request.toString();
    }

    String buildGuestExecStatus(long pid) {
        JsonObject request = new JsonObject();
        request.addProperty("execute", QemuCommand.AGENT_EXEC_STATUS);
        JsonObject params = new JsonObject();
        params.addProperty("pid", pid);
        request.add("arguments", params);
        return request.toString();
    }

    private boolean isAllowed(String path, List<String> arguments) {
        if ((LINUX_IP_SBIN.equals(path) || LINUX_IP_BIN.equals(path)) && arguments.size() == 6) {
            return "-j".equals(arguments.get(0))
                    && ("-4".equals(arguments.get(1)) || "-6".equals(arguments.get(1)))
                    && Arrays.asList("route", "show", "table", "all")
                            .equals(arguments.subList(2, arguments.size()));
        }
        return WINDOWS_POWERSHELL.equals(path)
                && Arrays.asList("-NoProfile", "-NonInteractive", "-Command", WINDOWS_ROUTE_SCRIPT)
                        .equals(arguments);
    }

    private long parsePid(String response) {
        JsonObject result = parseReturnObject(response);
        if (!result.has("pid")) {
            throw new GuestExecException("guest-exec response does not contain a pid");
        }
        return result.get("pid").getAsLong();
    }

    private JsonObject parseReturnObject(String json) {
        JsonElement root = new JsonParser().parse(json);
        if (!root.isJsonObject()) {
            throw new GuestExecException("QGA guest-exec response must be an object");
        }
        JsonObject response = root.getAsJsonObject();
        JsonElement result = response.get("return");
        if (result == null || !result.isJsonObject()) {
            JsonElement error = response.get("error");
            if (error != null && error.isJsonObject()) {
                JsonElement description = error.getAsJsonObject().get("desc");
                if (description != null && !description.isJsonNull()) {
                    throw new GuestExecException(description.getAsString());
                }
            }
            throw new GuestExecException("QGA guest-exec response does not contain an object result");
        }
        return result.getAsJsonObject();
    }

    private String decode(JsonElement encoded, int maxOutputBytes) {
        if (encoded == null || encoded.isJsonNull()) {
            return "";
        }
        String value = encoded.getAsString();
        if (value.length() > (long) maxOutputBytes * 2L) {
            throw new GuestExecException("guest-exec encoded output exceeds limit");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new GuestExecException("guest-exec output is not valid base64", e);
        }
        if (decoded.length > maxOutputBytes) {
            throw new GuestExecException("guest-exec output exceeds limit");
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(decoded)).toString();
        } catch (CharacterCodingException e) {
            throw new GuestExecException("guest-exec output is not valid UTF-8", e);
        }
    }

    @FunctionalInterface
    public interface AgentCommandExecutor {
        String execute(String command, int timeoutSeconds) throws Exception;
    }

    public static final class FallbackResult {
        private final List<VmGuestRoute> routes;
        private final boolean truncated;
        private final int originalCount;
        private final String source;

        FallbackResult(List<VmGuestRoute> routes, boolean truncated, int originalCount, String source) {
            this.routes = routes;
            this.truncated = truncated;
            this.originalCount = originalCount;
            this.source = source;
        }

        public List<VmGuestRoute> getRoutes() {
            return routes;
        }

        public boolean isTruncated() {
            return truncated;
        }

        public int getOriginalCount() {
            return originalCount;
        }

        public String getSource() {
            return source;
        }
    }

    static class GuestExecException extends RuntimeException {
        GuestExecException(String message) {
            super(message);
        }

        GuestExecException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
