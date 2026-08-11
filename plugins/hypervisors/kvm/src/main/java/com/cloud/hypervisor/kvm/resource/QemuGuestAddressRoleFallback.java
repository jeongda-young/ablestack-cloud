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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.cloudstack.utils.qemu.QemuCommand;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.VmGuestIpAddress;
import com.cloud.agent.api.VmGuestNetworkInterface;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Enriches the portable QGA interface response with OS-specific primary and
 * secondary address semantics. It is intentionally invoked only for VMs with
 * more than one eligible address.
 */
public class QemuGuestAddressRoleFallback {
    static final String ROLE_PRIMARY = "PRIMARY";
    static final String ROLE_SECONDARY = "SECONDARY";
    static final String ROLE_UNKNOWN = "UNKNOWN";
    static final String SOURCE_LINUX_FLAGS = "QGA_LINUX_ADDRESS_FLAGS";
    static final String SOURCE_WINDOWS_SKIP_AS_SOURCE = "QGA_WINDOWS_SKIP_AS_SOURCE";
    static final String SOURCE_SINGLE_ADDRESS = "QGA_SINGLE_ADDRESS";
    static final String LINUX_IP_SBIN = "/usr/sbin/ip";
    static final String LINUX_IP_BIN = "/usr/bin/ip";
    static final String WINDOWS_POWERSHELL =
            "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
    static final String WINDOWS_ADDRESS_SCRIPT =
            "$v4=(Get-NetRoute -DestinationPrefix '0.0.0.0/0' -ErrorAction SilentlyContinue "
            + "| Sort-Object RouteMetric | Select-Object -First 1).InterfaceIndex;"
            + "$v6=(Get-NetRoute -DestinationPrefix '::/0' -ErrorAction SilentlyContinue "
            + "| Sort-Object RouteMetric | Select-Object -First 1).InterfaceIndex;"
            + "Get-NetIPAddress | Select-Object InterfaceAlias,InterfaceIndex,IPAddress,"
            + "PrefixLength,AddressFamily,SkipAsSource,AddressState,"
            + "@{Name='DefaultRoute';Expression={($_.AddressFamily -eq 'IPv4' -and "
            + "$_.InterfaceIndex -eq $v4) -or ($_.AddressFamily -eq 'IPv6' -and "
            + "$_.InterfaceIndex -eq $v6)}} | ConvertTo-Json -Compress";
    private static final List<String> LINUX_ADDRESS_ARGS =
            Collections.unmodifiableList(Arrays.asList("-j", "address", "show"));
    private static final List<String> WINDOWS_ADDRESS_ARGS = Collections.unmodifiableList(
            Arrays.asList("-NoProfile", "-NonInteractive", "-Command", WINDOWS_ADDRESS_SCRIPT));
    private static final long POLL_INTERVAL_MILLIS = 50L;

    public boolean markSingleAddress(List<VmGuestNetworkInterface> interfaces) {
        List<VmGuestIpAddress> eligible = eligibleAddresses(interfaces);
        if (eligible.size() != 1) {
            return false;
        }
        VmGuestIpAddress address = eligible.get(0);
        address.setRole(ROLE_PRIMARY);
        address.setRoleSource(SOURCE_SINGLE_ADDRESS);
        address.setRepresentative(true);
        return true;
    }

    public boolean requiresResolution(List<VmGuestNetworkInterface> interfaces) {
        return eligibleAddresses(interfaces).size() > 1;
    }

    public String collect(AgentCommandExecutor executor, QemuGuestOsFamilyResolution os,
            List<VmGuestNetworkInterface> interfaces, int timeoutSeconds,
            int maxOutputBytes) throws Exception {
        if (os == null) {
            throw new UnsupportedOperationException("Guest OS is unavailable");
        }
        switch (os.getFamily()) {
            case LINUX:
                return collectLinux(executor, interfaces, timeoutSeconds, maxOutputBytes);
            case WINDOWS:
                return collectWindows(executor, interfaces, timeoutSeconds, maxOutputBytes);
            case UNSUPPORTED:
            default:
                throw new UnsupportedOperationException(
                        "Unsupported guest OS for address role collection: " + os.describe());
        }
    }

    private String collectLinux(AgentCommandExecutor executor,
            List<VmGuestNetworkInterface> interfaces, int timeoutSeconds,
            int maxOutputBytes) throws Exception {
        String output;
        try {
            output = execute(executor, LINUX_IP_SBIN, LINUX_ADDRESS_ARGS,
                    timeoutSeconds, maxOutputBytes);
        } catch (GuestExecException e) {
            if (!isExecutableMissing(e.getMessage())) {
                throw e;
            }
            output = execute(executor, LINUX_IP_BIN, LINUX_ADDRESS_ARGS,
                    timeoutSeconds, maxOutputBytes);
        }
        JsonElement root = new JsonParser().parse(output);
        if (!root.isJsonArray()) {
            throw new GuestExecException("Linux address output must be a JSON array");
        }
        for (JsonElement element : root.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject sourceInterface = element.getAsJsonObject();
            String interfaceName = readString(sourceInterface, "ifname");
            JsonElement addressInfo = sourceInterface.get("addr_info");
            if (addressInfo == null || !addressInfo.isJsonArray()) {
                continue;
            }
            for (JsonElement addressElement : addressInfo.getAsJsonArray()) {
                if (!addressElement.isJsonObject()) {
                    continue;
                }
                JsonObject sourceAddress = addressElement.getAsJsonObject();
                String address = readString(sourceAddress, "local");
                Boolean secondary = readBoolean(sourceAddress, "secondary");
                if (StringUtils.isBlank(address)) {
                    continue;
                }
                applyRole(interfaces, interfaceName, address,
                        Boolean.TRUE.equals(secondary) ? ROLE_SECONDARY : ROLE_PRIMARY,
                        SOURCE_LINUX_FLAGS, false);
            }
        }
        selectRepresentative(interfaces);
        return "guest-exec-linux-ip-address";
    }

    private String collectWindows(AgentCommandExecutor executor,
            List<VmGuestNetworkInterface> interfaces, int timeoutSeconds,
            int maxOutputBytes) throws Exception {
        String output = execute(executor, WINDOWS_POWERSHELL, WINDOWS_ADDRESS_ARGS,
                timeoutSeconds, maxOutputBytes);
        JsonElement root = new JsonParser().parse(output);
        JsonArray addresses = new JsonArray();
        if (root.isJsonArray()) {
            root.getAsJsonArray().forEach(addresses::add);
        } else if (root.isJsonObject()) {
            addresses.add(root);
        } else {
            throw new GuestExecException("Windows address output must be a JSON object or array");
        }
        for (JsonElement element : addresses) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject source = element.getAsJsonObject();
            String address = readAnyString(source, "IPAddress", "ipaddress");
            String interfaceName = readAnyString(source, "InterfaceAlias", "interfacealias");
            Boolean skipAsSource = readAnyBoolean(source, "SkipAsSource", "skipassource");
            Boolean defaultRoute = readAnyBoolean(source, "DefaultRoute", "defaultroute");
            if (StringUtils.isBlank(address)) {
                continue;
            }
            applyRole(interfaces, interfaceName, address,
                    Boolean.TRUE.equals(skipAsSource) ? ROLE_SECONDARY : ROLE_PRIMARY,
                    SOURCE_WINDOWS_SKIP_AS_SOURCE, Boolean.TRUE.equals(defaultRoute));
        }
        selectRepresentative(interfaces);
        return "guest-exec-windows-get-net-ip-address";
    }

    private void selectRepresentative(List<VmGuestNetworkInterface> interfaces) {
        List<VmGuestIpAddress> preferred = new ArrayList<>();
        List<VmGuestIpAddress> primary = new ArrayList<>();
        for (VmGuestNetworkInterface networkInterface : safeInterfaces(interfaces)) {
            for (VmGuestIpAddress address : safeAddresses(networkInterface)) {
                if (!isEligible(networkInterface, address)
                        || !ROLE_PRIMARY.equals(address.getRole())) {
                    continue;
                }
                primary.add(address);
                if (address.isRepresentative()) {
                    preferred.add(address);
                }
            }
        }
        VmGuestIpAddress representative = selectByFamily(preferred);
        if (representative == null) {
            List<VmGuestIpAddress> ipv4 = filterFamily(primary, "ipv4");
            List<VmGuestIpAddress> ipv6 = filterFamily(primary, "ipv6");
            if (ipv4.size() == 1) {
                representative = ipv4.get(0);
            } else if (ipv4.isEmpty() && ipv6.size() == 1) {
                representative = ipv6.get(0);
            }
        }
        for (VmGuestIpAddress address : primary) {
            address.setRepresentative(address == representative);
        }
    }

    private VmGuestIpAddress selectByFamily(List<VmGuestIpAddress> addresses) {
        List<VmGuestIpAddress> ipv4 = filterFamily(addresses, "ipv4");
        if (ipv4.size() == 1) {
            return ipv4.get(0);
        }
        List<VmGuestIpAddress> ipv6 = filterFamily(addresses, "ipv6");
        return ipv4.isEmpty() && ipv6.size() == 1 ? ipv6.get(0) : null;
    }

    private List<VmGuestIpAddress> filterFamily(List<VmGuestIpAddress> addresses, String family) {
        List<VmGuestIpAddress> result = new ArrayList<>();
        for (VmGuestIpAddress address : addresses) {
            if (family.equalsIgnoreCase(address.getFamily())) {
                result.add(address);
            }
        }
        return result;
    }

    private void applyRole(List<VmGuestNetworkInterface> interfaces, String interfaceName,
            String addressValue, String role, String source, boolean representativeCandidate) {
        for (VmGuestNetworkInterface networkInterface : safeInterfaces(interfaces)) {
            if (StringUtils.isNotBlank(interfaceName)
                    && !interfaceName.equalsIgnoreCase(networkInterface.getName())) {
                continue;
            }
            for (VmGuestIpAddress address : safeAddresses(networkInterface)) {
                if (addressValue.equalsIgnoreCase(address.getAddress())) {
                    address.setRole(role);
                    address.setRoleSource(source);
                    address.setRepresentative(representativeCandidate && ROLE_PRIMARY.equals(role));
                    return;
                }
            }
        }
    }

    private List<VmGuestIpAddress> eligibleAddresses(List<VmGuestNetworkInterface> interfaces) {
        List<VmGuestIpAddress> eligible = new ArrayList<>();
        for (VmGuestNetworkInterface networkInterface : safeInterfaces(interfaces)) {
            for (VmGuestIpAddress address : safeAddresses(networkInterface)) {
                if (isEligible(networkInterface, address)) {
                    eligible.add(address);
                }
            }
        }
        return eligible;
    }

    private boolean isEligible(VmGuestNetworkInterface networkInterface, VmGuestIpAddress address) {
        if (networkInterface.isLoopback() || address == null
                || StringUtils.isBlank(address.getAddress())) {
            return false;
        }
        String scope = StringUtils.defaultString(address.getScope()).toLowerCase(Locale.ROOT);
        return !"loopback".equals(scope)
                && !"link-local".equals(scope)
                && !"multicast".equals(scope);
    }

    private List<VmGuestNetworkInterface> safeInterfaces(
            List<VmGuestNetworkInterface> interfaces) {
        return interfaces == null ? Collections.emptyList() : interfaces;
    }

    private List<VmGuestIpAddress> safeAddresses(VmGuestNetworkInterface networkInterface) {
        return networkInterface.getAddresses() == null
                ? Collections.emptyList() : networkInterface.getAddresses();
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
                    throw new GuestExecException(
                            "Allowlisted address command failed with exit code "
                                    + exitCode + ": " + stderr);
                }
                return stdout;
            }
            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GuestExecException("Allowlisted address command was interrupted", e);
            }
        }
        throw new GuestExecException("Allowlisted address command timed out");
    }

    String buildGuestExec(String path, List<String> arguments) {
        if (!isAllowed(path, arguments)) {
            throw new IllegalArgumentException(
                    "Address role guest-exec invocation is not allowlisted");
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
        if ((LINUX_IP_SBIN.equals(path) || LINUX_IP_BIN.equals(path))
                && LINUX_ADDRESS_ARGS.equals(arguments)) {
            return true;
        }
        return WINDOWS_POWERSHELL.equals(path) && WINDOWS_ADDRESS_ARGS.equals(arguments);
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
            throw new GuestExecException(
                    "QGA guest-exec response does not contain an object result");
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

    private String readString(JsonObject source, String name) {
        JsonElement value = source.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private String readAnyString(JsonObject source, String... names) {
        for (String name : names) {
            String value = readString(source, name);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private Boolean readBoolean(JsonObject source, String name) {
        JsonElement value = source.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsBoolean();
    }

    private Boolean readAnyBoolean(JsonObject source, String... names) {
        for (String name : names) {
            Boolean value = readBoolean(source, name);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @FunctionalInterface
    public interface AgentCommandExecutor {
        String execute(String command, int timeoutSeconds) throws Exception;
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
