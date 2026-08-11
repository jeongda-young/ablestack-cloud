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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cloud.agent.api.VmGuestDnsConfig;
import com.cloud.agent.api.VmGuestDnsDomain;
import com.cloud.agent.api.VmGuestDnsServer;
import com.cloud.agent.api.VmGuestDnsState;
import com.cloud.utils.net.NetUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class QemuGuestDnsParser {
    public static final int MAX_DNS_SERVERS = 64;
    public static final int MAX_DNS_DOMAINS = 64;
    private static final Pattern LINK_HEADER = Pattern.compile("^Link\\s+\\d+\\s+\\((.+)\\)$");

    public DnsParseResult parseResolvectl(String output) {
        ParseAccumulator accumulator = new ParseAccumulator("resolvectl");
        VmGuestDnsConfig current = null;
        String continuation = null;
        for (String rawLine : lines(output)) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continuation = null;
                continue;
            }
            Matcher link = LINK_HEADER.matcher(line);
            if ("Global".equals(line)) {
                current = newConfig(null, true, "resolvectl");
                accumulator.configurations.add(current);
                continuation = null;
                continue;
            }
            if (link.matches()) {
                current = newConfig(link.group(1), false, "resolvectl");
                accumulator.configurations.add(current);
                continuation = null;
                continue;
            }
            if (current == null) {
                continue;
            }
            int separator = line.indexOf(':');
            if (separator > 0) {
                String label = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if ("DNS Servers".equals(label) || "Current DNS Server".equals(label)) {
                    addServers(accumulator, current, value, null);
                    continuation = "servers";
                } else if ("DNS Domain".equals(label) || "DNS Domains".equals(label)) {
                    addDomains(accumulator, current, value);
                    continuation = "domains";
                } else if ("servers".equals(continuation)) {
                    addServers(accumulator, current, line, null);
                } else if ("domains".equals(continuation)) {
                    addDomains(accumulator, current, line);
                } else {
                    continuation = null;
                }
            } else if ("servers".equals(continuation)) {
                addServers(accumulator, current, line, null);
            } else if ("domains".equals(continuation)) {
                addDomains(accumulator, current, line);
            }
        }
        return accumulator.finish();
    }

    public DnsParseResult parseNmcli(String output) {
        ParseAccumulator accumulator = new ParseAccumulator("nmcli");
        VmGuestDnsConfig current = null;
        for (String rawLine : lines(output)) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                current = null;
                continue;
            }
            int separator = firstUnescapedColon(line);
            if (separator < 1) {
                continue;
            }
            String key = line.substring(0, separator);
            String value = unescapeNmcli(line.substring(separator + 1).trim());
            if ("GENERAL.DEVICE".equals(key)) {
                if (!value.isEmpty() && !"--".equals(value)) {
                    current = newConfig(value, false, "nmcli");
                    accumulator.configurations.add(current);
                }
            } else if (current != null && (key.startsWith("IP4.DNS") || key.startsWith("IP6.DNS"))) {
                addServers(accumulator, current, value, key.startsWith("IP6") ? "ipv6" : "ipv4");
            } else if (current != null && (key.startsWith("IP4.DOMAIN") || key.startsWith("IP6.DOMAIN"))) {
                addDomains(accumulator, current, value.replace(',', ' '));
            }
        }
        return accumulator.finish();
    }

    public DnsParseResult parseResolvConf(String output) {
        ParseAccumulator accumulator = new ParseAccumulator("resolv.conf");
        VmGuestDnsConfig config = newConfig(null, true, "resolv.conf");
        accumulator.configurations.add(config);
        for (String rawLine : lines(output)) {
            String line = rawLine;
            int comment = line.indexOf('#');
            if (comment >= 0) {
                line = line.substring(0, comment);
            }
            line = line.trim();
            if (line.startsWith("nameserver ")) {
                addServers(accumulator, config, line.substring("nameserver ".length()).trim(), null);
            } else if (line.startsWith("search ")) {
                addDomains(accumulator, config, line.substring("search ".length()).trim());
            } else if (line.startsWith("domain ")) {
                addDomains(accumulator, config, line.substring("domain ".length()).trim());
            }
        }
        return accumulator.finish();
    }

    public DnsParseResult parseWindows(String output) {
        JsonElement root = new JsonParser().parse(output);
        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("Windows DNS output must be a JSON object");
        }
        ParseAccumulator accumulator = new ParseAccumulator("windows-dns-client");
        Map<String, VmGuestDnsConfig> byInterface = new LinkedHashMap<>();
        for (JsonObject serverRecord : objects(root.getAsJsonObject().get("servers"))) {
            String interfaceName = readString(serverRecord, "InterfaceAlias");
            VmGuestDnsConfig config = byInterface.computeIfAbsent(interfaceName,
                    name -> newConfig(name, name == null, "windows-dns-client"));
            String family = normalizeFamily(readString(serverRecord, "AddressFamily"), null);
            for (String server : strings(serverRecord.get("ServerAddresses"))) {
                addServers(accumulator, config, server, family);
            }
        }
        for (JsonObject clientRecord : objects(root.getAsJsonObject().get("clients"))) {
            String interfaceName = readString(clientRecord, "InterfaceAlias");
            VmGuestDnsConfig config = byInterface.computeIfAbsent(interfaceName,
                    name -> newConfig(name, name == null, "windows-dns-client"));
            addDomains(accumulator, config, readString(clientRecord, "ConnectionSpecificSuffix"));
            for (String domain : strings(clientRecord.get("ConnectionSpecificSuffixSearchList"))) {
                addDomains(accumulator, config, domain);
            }
        }
        accumulator.configurations.addAll(byInterface.values());
        for (JsonObject globalRecord : objects(root.getAsJsonObject().get("global"))) {
            VmGuestDnsConfig config = newConfig(null, true, "windows-dns-client");
            for (String domain : strings(globalRecord.get("SuffixSearchList"))) {
                addDomains(accumulator, config, domain);
            }
            accumulator.configurations.add(config);
        }
        return accumulator.finish();
    }

    private VmGuestDnsConfig newConfig(String interfaceName, boolean global, String source) {
        VmGuestDnsConfig config = new VmGuestDnsConfig();
        config.setInterfaceName(interfaceName);
        config.setGlobal(global);
        config.setSource(source);
        return config;
    }

    private void addServers(ParseAccumulator accumulator, VmGuestDnsConfig config,
            String values, String forcedFamily) {
        if (values == null) {
            return;
        }
        for (String value : values.trim().split("\\s+")) {
            String address = value.trim();
            String family = normalizeFamily(forcedFamily, address);
            if (family == null || containsServer(config, address)) {
                continue;
            }
            accumulator.originalCount++;
            if (accumulator.serverCount >= MAX_DNS_SERVERS) {
                accumulator.truncated = true;
                continue;
            }
            config.getServers().add(new VmGuestDnsServer(address, family, isLocalStub(address)));
            accumulator.serverCount++;
        }
    }

    private void addDomains(ParseAccumulator accumulator, VmGuestDnsConfig config, String values) {
        if (values == null) {
            return;
        }
        for (String value : values.trim().split("\\s+")) {
            String rawDomain = value.trim();
            if (rawDomain.isEmpty() || "--".equals(rawDomain)) {
                continue;
            }
            boolean routingOnly = rawDomain.startsWith("~");
            String domain = routingOnly ? rawDomain.substring(1) : rawDomain;
            if (domain.isEmpty() || containsDomain(config, domain, routingOnly)) {
                continue;
            }
            accumulator.originalCount++;
            if (accumulator.domainCount >= MAX_DNS_DOMAINS) {
                accumulator.truncated = true;
                continue;
            }
            config.getDomains().add(new VmGuestDnsDomain(domain, routingOnly));
            accumulator.domainCount++;
        }
    }

    private boolean containsServer(VmGuestDnsConfig config, String address) {
        return config.getServers().stream().anyMatch(server -> address.equals(server.getAddress()));
    }

    private boolean containsDomain(VmGuestDnsConfig config, String domain, boolean routingOnly) {
        return config.getDomains().stream().anyMatch(existing ->
                domain.equals(existing.getDomain()) && routingOnly == existing.isRoutingOnly());
    }

    private String normalizeFamily(String family, String address) {
        if (family != null) {
            String normalized = family.toLowerCase(Locale.ROOT);
            if ("ipv4".equals(normalized) || "2".equals(normalized)) {
                return "ipv4";
            }
            if ("ipv6".equals(normalized) || "23".equals(normalized)) {
                return "ipv6";
            }
        }
        String addressWithoutZone = address;
        if (addressWithoutZone != null && addressWithoutZone.contains("%")) {
            addressWithoutZone = addressWithoutZone.substring(0, addressWithoutZone.indexOf('%'));
        }
        if (NetUtils.isValidIp4(addressWithoutZone)) {
            return "ipv4";
        }
        if (NetUtils.isValidIp6(addressWithoutZone)) {
            return "ipv6";
        }
        return null;
    }

    private boolean isLocalStub(String address) {
        String addressWithoutZone = address;
        if (addressWithoutZone != null && addressWithoutZone.contains("%")) {
            addressWithoutZone = addressWithoutZone.substring(0, addressWithoutZone.indexOf('%'));
        }
        try {
            return InetAddress.getByName(addressWithoutZone).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private int firstUnescapedColon(String value) {
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == ':' && !escaped) {
                return index;
            }
            if (character == '\\' && !escaped) {
                escaped = true;
            } else {
                escaped = false;
            }
        }
        return -1;
    }

    private String unescapeNmcli(String value) {
        return value.replace("\\:", ":").replace("\\\\", "\\");
    }

    private List<String> lines(String output) {
        String normalized = output == null ? "" : output.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = new ArrayList<>();
        for (String line : normalized.split("\n")) {
            lines.add(line);
        }
        return lines;
    }

    private List<JsonObject> objects(JsonElement element) {
        List<JsonObject> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return result;
        }
        if (element.isJsonObject()) {
            result.add(element.getAsJsonObject());
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (child.isJsonObject()) {
                    result.add(child.getAsJsonObject());
                }
            }
        }
        return result;
    }

    private List<String> strings(JsonElement element) {
        List<String> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return result;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                if (!child.isJsonNull()) {
                    result.add(child.getAsString());
                }
            }
        } else {
            result.add(element.getAsString());
        }
        return result;
    }

    private String readString(JsonObject object, String member) {
        JsonElement element = object.get(member);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private static final class ParseAccumulator {
        private final String source;
        private final List<VmGuestDnsConfig> configurations = new ArrayList<>();
        private int serverCount;
        private int domainCount;
        private int originalCount;
        private boolean truncated;

        private ParseAccumulator(String source) {
            this.source = source;
        }

        private DnsParseResult finish() {
            configurations.removeIf(config ->
                    config.getServers().isEmpty() && config.getDomains().isEmpty());
            VmGuestDnsState state = new VmGuestDnsState();
            state.setSource(source);
            state.setConfigurations(configurations);
            Set<String> servers = new LinkedHashSet<>();
            Set<String> searchDomains = new LinkedHashSet<>();
            boolean upstreamKnown = false;
            for (VmGuestDnsConfig config : configurations) {
                for (VmGuestDnsServer server : config.getServers()) {
                    servers.add(server.getAddress());
                    upstreamKnown |= !server.isLocalStub();
                }
                for (VmGuestDnsDomain domain : config.getDomains()) {
                    if (!domain.isRoutingOnly()) {
                        searchDomains.add(domain.getDomain());
                    }
                }
            }
            state.setServers(new ArrayList<>(servers));
            state.setSearchDomains(new ArrayList<>(searchDomains));
            state.setUpstreamServersKnown(upstreamKnown);
            return new DnsParseResult(state, truncated, originalCount);
        }
    }

    public static final class DnsParseResult {
        private final VmGuestDnsState state;
        private final boolean truncated;
        private final int originalCount;

        DnsParseResult(VmGuestDnsState state, boolean truncated, int originalCount) {
            this.state = state;
            this.truncated = truncated;
            this.originalCount = originalCount;
        }

        public VmGuestDnsState getState() {
            return state;
        }

        public boolean isTruncated() {
            return truncated;
        }

        public int getOriginalCount() {
            return originalCount;
        }

        public boolean isEmpty() {
            return state.getConfigurations().isEmpty();
        }
    }
}
