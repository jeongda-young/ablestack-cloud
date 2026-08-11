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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.apache.cloudstack.utils.qemu.QemuCommand;

import com.cloud.hypervisor.kvm.resource.QemuGuestDnsParser.DnsParseResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QemuGuestDnsFallback {
    static final String RESOLVECTL_USR = "/usr/bin/resolvectl";
    static final String RESOLVECTL_BIN = "/bin/resolvectl";
    static final String NMCLI_USR = "/usr/bin/nmcli";
    static final String NMCLI_BIN = "/bin/nmcli";
    static final String CAT_USR = "/usr/bin/cat";
    static final String CAT_BIN = "/bin/cat";
    static final String RESOLV_CONF = "/etc/resolv.conf";
    static final String WINDOWS_POWERSHELL =
            "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
    static final String WINDOWS_DNS_SCRIPT =
            "$servers=Get-DnsClientServerAddress | Select-Object "
            + "InterfaceAlias,AddressFamily,ServerAddresses;"
            + "$clients=Get-DnsClient | Select-Object InterfaceAlias,"
            + "ConnectionSpecificSuffix,ConnectionSpecificSuffixSearchList;"
            + "$global=Get-DnsClientGlobalSetting | Select-Object SuffixSearchList;"
            + "[pscustomobject]@{servers=$servers;clients=$clients;global=$global} "
            + "| ConvertTo-Json -Depth 4 -Compress";
    private static final List<String> RESOLVECTL_ARGS = Arrays.asList("status", "--no-pager");
    private static final List<String> NMCLI_ARGS = Arrays.asList("-t", "-f",
            "GENERAL.DEVICE,IP4.DNS,IP6.DNS,IP4.DOMAIN,IP6.DOMAIN", "device", "show");
    private static final List<String> CAT_ARGS = Arrays.asList(RESOLV_CONF);
    private static final List<String> WINDOWS_ARGS = Arrays.asList(
            "-NoProfile", "-NonInteractive", "-Command", WINDOWS_DNS_SCRIPT);
    private static final long POLL_INTERVAL_MILLIS = 50L;

    private final QemuGuestDnsParser parser;

    public QemuGuestDnsFallback(QemuGuestDnsParser parser) {
        this.parser = parser;
    }

    public DnsParseResult collect(AgentCommandExecutor executor, QemuGuestOsFamilyResolution os,
            int timeoutSeconds, int maxOutputBytes) throws Exception {
        if (os != null) {
            switch (os.getFamily()) {
                case LINUX:
                    return collectLinux(executor, timeoutSeconds, maxOutputBytes);
                case WINDOWS:
                    String output = execute(executor, WINDOWS_POWERSHELL, WINDOWS_ARGS,
                            timeoutSeconds, maxOutputBytes);
                    return parser.parseWindows(output);
                case UNSUPPORTED:
                default:
                    break;
            }
        }
        throw new UnsupportedOperationException("Unsupported guest OS for DNS fallback: "
                + (os == null ? "id=-, kernel-name=-, name=-, pretty-name=-" : os.describe()));
    }

    private DnsParseResult collectLinux(AgentCommandExecutor executor,
            int timeoutSeconds, int maxOutputBytes) throws Exception {
        Exception lastFailure = null;
        for (DnsSource source : DnsSource.values()) {
            try {
                DnsParseResult result = collectLinuxSource(
                        executor, source, timeoutSeconds, maxOutputBytes);
                if (!result.isEmpty()) {
                    return result;
                }
            } catch (Exception e) {
                lastFailure = e;
            }
        }
        GuestExecException unavailable =
                new GuestExecException("No supported Linux DNS source returned usable data");
        if (lastFailure != null) {
            unavailable.addSuppressed(lastFailure);
        }
        throw unavailable;
    }

    private DnsParseResult collectLinuxSource(AgentCommandExecutor executor, DnsSource source,
            int timeoutSeconds, int maxOutputBytes) throws Exception {
        String primaryPath;
        String secondaryPath;
        List<String> arguments;
        switch (source) {
            case RESOLVECTL:
                primaryPath = RESOLVECTL_USR;
                secondaryPath = RESOLVECTL_BIN;
                arguments = RESOLVECTL_ARGS;
                break;
            case NMCLI:
                primaryPath = NMCLI_USR;
                secondaryPath = NMCLI_BIN;
                arguments = NMCLI_ARGS;
                break;
            case RESOLV_CONF:
                primaryPath = CAT_USR;
                secondaryPath = CAT_BIN;
                arguments = CAT_ARGS;
                break;
            default:
                throw new IllegalArgumentException("Unsupported DNS source");
        }
        String output;
        try {
            output = execute(executor, primaryPath, arguments, timeoutSeconds, maxOutputBytes);
        } catch (GuestExecException e) {
            if (!isExecutableMissing(e.getMessage())) {
                throw e;
            }
            output = execute(executor, secondaryPath, arguments, timeoutSeconds, maxOutputBytes);
        }
        return parseLinux(source, output);
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

    private DnsParseResult parseLinux(DnsSource source, String output) {
        switch (source) {
            case RESOLVECTL:
                return parser.parseResolvectl(output);
            case NMCLI:
                return parser.parseNmcli(output);
            case RESOLV_CONF:
                return parser.parseResolvConf(output);
            default:
                throw new IllegalArgumentException("Unsupported DNS source");
        }
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
                    throw new GuestExecException("Allowlisted DNS command failed with exit code "
                            + exitCode + ": " + stderr);
                }
                return stdout;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                performFinalStatus(executor, pid, timeoutSeconds, e);
                throw new GuestExecException("Allowlisted DNS command was interrupted", e);
            }
        }
        GuestExecException timeout = new GuestExecException("Allowlisted DNS command timed out");
        performFinalStatus(executor, pid, timeoutSeconds, timeout);
        throw timeout;
    }

    private void performFinalStatus(AgentCommandExecutor executor, long pid,
            int timeoutSeconds, Exception failure) {
        try {
            executor.execute(buildGuestExecStatus(pid), timeoutSeconds);
        } catch (Exception cleanupError) {
            failure.addSuppressed(cleanupError);
        }
    }

    String buildGuestExec(String path, List<String> arguments) {
        if (!isAllowed(path, arguments)) {
            throw new IllegalArgumentException("DNS guest-exec invocation is not allowlisted");
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
        if ((RESOLVECTL_USR.equals(path) || RESOLVECTL_BIN.equals(path))
                && RESOLVECTL_ARGS.equals(arguments)) {
            return true;
        }
        if ((NMCLI_USR.equals(path) || NMCLI_BIN.equals(path)) && NMCLI_ARGS.equals(arguments)) {
            return true;
        }
        if ((CAT_USR.equals(path) || CAT_BIN.equals(path)) && CAT_ARGS.equals(arguments)) {
            return true;
        }
        return WINDOWS_POWERSHELL.equals(path) && WINDOWS_ARGS.equals(arguments);
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
            throw new GuestExecException("DNS command encoded output exceeds limit");
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new GuestExecException("DNS command output is not valid base64", e);
        }
        if (decoded.length > maxOutputBytes) {
            throw new GuestExecException("DNS command output exceeds limit");
        }
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(decoded)).toString();
        } catch (CharacterCodingException e) {
            throw new GuestExecException("DNS command output is not valid UTF-8", e);
        }
    }

    @FunctionalInterface
    public interface AgentCommandExecutor {
        String execute(String command, int timeoutSeconds) throws Exception;
    }

    private enum DnsSource {
        RESOLVECTL,
        NMCLI,
        RESOLV_CONF
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
