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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.cloud.agent.api.VmGuestDnsState;
import com.cloud.agent.api.VmGuestIpAddress;
import com.cloud.agent.api.VmGuestNetworkInterface;
import com.cloud.agent.api.VmGuestRoute;
import com.cloud.agent.api.VmGuestToolsInfo;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class QemuGuestToolsSnapshot {
    public static final int SUPPORTED_SCHEMA_VERSION = 1;

    private int schemaVersion;
    private Tool tool;
    private Profile profile;
    private Map<String, Section> sections;
    private List<HelperInterface> interfaces;
    private List<HelperRoute> routes;
    private HelperDns dns;

    public static QemuGuestToolsSnapshot parse(String json) {
        try {
            QemuGuestToolsSnapshot snapshot = new Gson().fromJson(json, QemuGuestToolsSnapshot.class);
            if (snapshot == null || snapshot.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
                throw new IllegalArgumentException("Unsupported guest tools helper schema");
            }
            return snapshot;
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("Invalid guest tools helper JSON", e);
        }
    }

    public VmGuestToolsInfo toInfo() {
        VmGuestToolsInfo info = new VmGuestToolsInfo();
        info.setInstalled(true);
        info.setVersion(tool == null ? null : tool.version);
        info.setHelperSchemaVersion(schemaVersion);
        info.setQgaPolicyMode("FULL");
        info.setReadinessStatus(profile == null ? "UNKNOWN" : profile.status);
        info.setProfileVersion(profile == null ? null : String.valueOf(profile.version));
        return info;
    }

    public Section section(String name) {
        return sections == null ? null : sections.get(name);
    }

    public void enrichAddressRoles(List<VmGuestNetworkInterface> target) {
        if (interfaces == null || target == null) {
            return;
        }
        VmGuestIpAddress representative = null;
        for (HelperInterface sourceInterface : interfaces) {
            for (HelperAddress sourceAddress : safe(sourceInterface.addresses)) {
                for (VmGuestNetworkInterface targetInterface : target) {
                    if (sourceInterface.name != null
                            && !sourceInterface.name.equalsIgnoreCase(targetInterface.getName())) {
                        continue;
                    }
                    for (VmGuestIpAddress targetAddress : safeAddresses(targetInterface)) {
                        if (sourceAddress.address != null
                                && sourceAddress.address.equalsIgnoreCase(targetAddress.getAddress())) {
                            targetAddress.setRole(sourceAddress.secondary ? "SECONDARY" : "PRIMARY");
                            targetAddress.setRoleSource("ABLESTACK_GUEST_TOOLS");
                            if (sourceAddress.primary && representative == null
                                    && "ipv4".equalsIgnoreCase(targetAddress.getFamily())) {
                                representative = targetAddress;
                            }
                        }
                    }
                }
            }
        }
        if (representative == null) {
            for (VmGuestNetworkInterface networkInterface : target) {
                for (VmGuestIpAddress address : safeAddresses(networkInterface)) {
                    if ("PRIMARY".equals(address.getRole())) {
                        representative = address;
                        break;
                    }
                }
                if (representative != null) {
                    break;
                }
            }
        }
        for (VmGuestNetworkInterface networkInterface : target) {
            for (VmGuestIpAddress address : safeAddresses(networkInterface)) {
                address.setRepresentative(address == representative);
            }
        }
    }

    public List<VmGuestRoute> toRoutes() {
        List<VmGuestRoute> result = new ArrayList<>();
        for (HelperRoute source : safe(routes)) {
            VmGuestRoute route = new VmGuestRoute();
            route.setFamily(source.family);
            String destination = source.destination;
            if ("default".equals(destination)) {
                route.setDefaultRoute(true);
                route.setDestination("IPv6".equalsIgnoreCase(source.family) ? "::" : "0.0.0.0");
                route.setPrefix(0);
            } else if (destination != null && destination.contains("/")) {
                int slash = destination.lastIndexOf('/');
                route.setDestination(destination.substring(0, slash));
                try {
                    route.setPrefix(Integer.valueOf(destination.substring(slash + 1)));
                } catch (NumberFormatException ignored) {
                    route.setDestination(destination);
                }
            } else {
                route.setDestination(destination);
            }
            route.setGateway(source.gateway);
            route.setInterfaceName(source.device);
            route.setMetric(source.metric);
            route.setTable(source.table);
            route.setProtocol(source.protocol);
            route.setScope(source.scope);
            result.add(route);
        }
        return result;
    }

    public VmGuestDnsState toDns() {
        VmGuestDnsState result = new VmGuestDnsState();
        if (dns != null) {
            result.setServers(new ArrayList<>(safe(dns.servers)));
            result.setSearchDomains(new ArrayList<>(safe(dns.searchDomains)));
            result.setSource(dns.source);
            result.setUpstreamServersKnown(!safe(dns.servers).isEmpty());
        }
        return result;
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private static List<VmGuestIpAddress> safeAddresses(VmGuestNetworkInterface value) {
        return value.getAddresses() == null ? Collections.emptyList() : value.getAddresses();
    }

    private static class Tool {
        private String version;
    }

    private static class Profile {
        private int version;
        private String status;
    }

    public static class Section {
        private String status;
        private String source;
        private String errorCode;

        public String getStatus() {
            return status;
        }

        public String getSource() {
            return source;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    private static class HelperInterface {
        private String name;
        private List<HelperAddress> addresses;
    }

    private static class HelperAddress {
        private String address;
        private boolean primary;
        private boolean secondary;
    }

    private static class HelperRoute {
        private String family;
        private String destination;
        private String gateway;
        private String device;
        private Integer metric;
        private String table;
        private String protocol;
        private String scope;
    }

    private static class HelperDns {
        private List<String> servers;
        private List<String> searchDomains;
        private String source;
    }
}
