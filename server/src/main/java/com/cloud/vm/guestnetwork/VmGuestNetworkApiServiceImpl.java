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
package com.cloud.vm.guestnetwork;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.response.GuestNetworkAddressResponse;
import org.apache.cloudstack.api.response.GuestNetworkCollectorResponse;
import org.apache.cloudstack.api.response.GuestNetworkDnsConfigResponse;
import org.apache.cloudstack.api.response.GuestNetworkDnsDomainResponse;
import org.apache.cloudstack.api.response.GuestNetworkDnsResponse;
import org.apache.cloudstack.api.response.GuestNetworkDnsServerResponse;
import org.apache.cloudstack.api.response.GuestNetworkInterfaceResponse;
import org.apache.cloudstack.api.response.GuestNetworkRouteResponse;
import org.apache.cloudstack.api.response.GuestNetworkRefreshResponse;
import org.apache.cloudstack.api.response.GuestNetworkSectionResponse;
import org.apache.cloudstack.api.response.GuestNetworkStateResponse;
import org.apache.cloudstack.api.response.GuestNetworkSummaryResponse;
import org.apache.cloudstack.api.response.GuestToolsResponse;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.guestnetwork.VmGuestNetworkApiService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.VmGuestDnsConfig;
import com.cloud.agent.api.VmGuestDnsDomain;
import com.cloud.agent.api.VmGuestDnsServer;
import com.cloud.agent.api.VmGuestDnsState;
import com.cloud.agent.api.VmGuestIpAddress;
import com.cloud.agent.api.VmGuestNetworkInterface;
import com.cloud.agent.api.VmGuestNetworkSectionStatus;
import com.cloud.agent.api.VmGuestNetworkState;
import com.cloud.agent.api.VmGuestRoute;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.serializer.GsonHelper;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmGuestNetworkStateVO;
import com.cloud.vm.VmGuestNetworkSectionStateVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VmGuestNetworkStateDao;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

@Component
public class VmGuestNetworkApiServiceImpl implements VmGuestNetworkApiService {
    private static final String NOT_COLLECTED = "NOT_COLLECTED";

    @Inject
    private VmGuestNetworkStateDao stateDao;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private AccountManager accountManager;
    @Inject
    private VmGuestNetworkScheduleService scheduleService;

    private final Gson gson = GsonHelper.getGson();

    @Override
    public GuestNetworkStateResponse getState(long vmId) {
        UserVmVO vm = userVmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find Instance with ID " + vmId);
        }
        Account caller = CallContext.current().getCallingAccount();
        accountManager.checkAccess(caller, null, true, vm);
        return toStateResponse(vm.getUuid(), stateDao.findByVmId(vmId));
    }

    @Override
    public Map<Long, GuestNetworkSummaryResponse> listSummaries(Collection<Long> vmIds) {
        if (vmIds == null || vmIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, VmGuestNetworkStateVO> states = stateDao.listByVmIds(vmIds).stream()
                .collect(Collectors.toMap(VmGuestNetworkStateVO::getVmId, Function.identity()));
        Map<Long, GuestNetworkSummaryResponse> summaries = new LinkedHashMap<>();
        new LinkedHashSet<>(vmIds).forEach(vmId -> summaries.put(vmId, toSummaryResponse(states.get(vmId))));
        return summaries;
    }

    GuestNetworkStateResponse toStateResponse(String vmUuid, VmGuestNetworkStateVO snapshot) {
        GuestNetworkStateResponse response = new GuestNetworkStateResponse();
        response.setVirtualMachineId(vmUuid);
        if (snapshot == null) {
            response.setStatus(NOT_COLLECTED);
            response.setSchemaVersion(VmGuestNetworkState.CURRENT_SCHEMA_VERSION);
            return response;
        }
        response.setStatus(snapshot.getStatus());
        response.setSchemaVersion(snapshot.getSchemaVersion());
        response.setQgaVersion(snapshot.getQgaVersion());
        response.setObserved(snapshot.getObservedAt());
        response.setLastSuccess(snapshot.getLastSuccessAt());
        response.setErrorCode(snapshot.getErrorCode());
        response.setErrorMessage(snapshot.getErrorMessage());
        GuestNetworkCollectorResponse collector = new GuestNetworkCollectorResponse();
        collector.setBuildId(snapshot.getCollectorBuildId());
        collector.setHostId(snapshot.getCollectorHostId());
        collector.setCapabilityHash(snapshot.getCapabilityHash());
        response.setCollector(collector);
        GuestToolsResponse guestTools = new GuestToolsResponse();
        guestTools.setInstalled(StringUtils.isNotBlank(snapshot.getGuestToolsVersion()));
        guestTools.setVersion(snapshot.getGuestToolsVersion());
        guestTools.setQgaPolicyMode(snapshot.getQgaPolicyMode());
        guestTools.setReadinessStatus(snapshot.getReadinessStatus());
        guestTools.setChecked(snapshot.getReadinessCheckedAt());
        response.setGuestTools(guestTools);

        VmGuestNetworkState state = parsePayload(snapshot.getPayload());
        response.setInterfaces(toInterfaceResponses(state.getInterfaces()));
        response.setSections(toSectionResponses(state.getSectionStatuses(),
                scheduleService == null ? Collections.emptyList()
                        : scheduleService.listByVmId(snapshot.getVmId())));
        response.setRoutes(toRouteResponses(state.getRoutes()));
        response.setDns(toDnsResponse(state.getDns()));
        return response;
    }

    GuestNetworkSummaryResponse toSummaryResponse(VmGuestNetworkStateVO snapshot) {
        GuestNetworkSummaryResponse response = new GuestNetworkSummaryResponse();
        if (snapshot == null) {
            response.setStatus(NOT_COLLECTED);
            return response;
        }
        response.setStatus(snapshot.getStatus());
        response.setObserved(snapshot.getObservedAt());
        response.setLastSuccess(snapshot.getLastSuccessAt());

        VmGuestNetworkState state = parsePayload(snapshot.getPayload());
        List<VmGuestNetworkInterface> interfaces = state.getInterfaces() == null
                ? Collections.emptyList()
                : state.getInterfaces();
        response.setInterfaceCount(interfaces.size());
        Set<String> ipv4 = new LinkedHashSet<>();
        Set<String> ipv6 = new LinkedHashSet<>();
        VmGuestIpAddress representative = null;
        for (VmGuestNetworkInterface networkInterface : interfaces) {
            if (networkInterface.getAddresses() == null) {
                continue;
            }
            for (VmGuestIpAddress address : networkInterface.getAddresses()) {
                if (address == null || StringUtils.isBlank(address.getAddress())) {
                    continue;
                }
                String value = formatAddress(address);
                if ("IPv4".equalsIgnoreCase(address.getFamily())) {
                    ipv4.add(value);
                } else if ("IPv6".equalsIgnoreCase(address.getFamily())) {
                    ipv6.add(value);
                }
                if (address.isRepresentative()) {
                    representative = address;
                }
            }
        }
        response.setIpv4Addresses(new ArrayList<>(ipv4));
        response.setIpv6Addresses(new ArrayList<>(ipv6));
        if (representative != null && isInterfaceSnapshotFresh(state, snapshot.getStatus())) {
            response.setRepresentativeAddress(representative.getAddress());
            response.setRepresentativePrefix(representative.getPrefix());
            response.setRepresentativeFamily(normalizeAddressFamily(representative.getFamily()));
            response.setRepresentativeSource(representative.getRoleSource());
        }
        return response;
    }

    private boolean isInterfaceSnapshotFresh(VmGuestNetworkState state, String snapshotStatus) {
        if ("STALE".equals(snapshotStatus) || "STOPPED".equals(snapshotStatus)
                || "UNAVAILABLE".equals(snapshotStatus)
                || "UNSUPPORTED".equals(snapshotStatus)) {
            return false;
        }
        if (state.getSectionStatuses() == null
                || state.getSectionStatuses().get("interfaces") == null) {
            return true;
        }
        String status = state.getSectionStatuses().get("interfaces").getStatus();
        return "OK".equals(status) || "PARTIAL".equals(status) || "EMPTY".equals(status);
    }

    private VmGuestNetworkState parsePayload(String payload) {
        if (StringUtils.isBlank(payload)) {
            return new VmGuestNetworkState();
        }
        try {
            VmGuestNetworkState state = gson.fromJson(payload, VmGuestNetworkState.class);
            return state == null ? new VmGuestNetworkState() : state;
        } catch (JsonParseException e) {
            return new VmGuestNetworkState();
        }
    }

    private List<GuestNetworkInterfaceResponse> toInterfaceResponses(
            List<VmGuestNetworkInterface> interfaces) {
        List<GuestNetworkInterfaceResponse> responses = new ArrayList<>();
        if (interfaces == null) {
            return responses;
        }
        for (VmGuestNetworkInterface networkInterface : interfaces) {
            GuestNetworkInterfaceResponse response = new GuestNetworkInterfaceResponse();
            response.setName(networkInterface.getName());
            response.setHardwareAddress(networkInterface.getHardwareAddress());
            response.setCloudNicId(networkInterface.getCloudNicId());
            response.setLoopback(networkInterface.isLoopback());
            List<GuestNetworkAddressResponse> addresses = new ArrayList<>();
            if (networkInterface.getAddresses() != null) {
                networkInterface.getAddresses().forEach(address -> addresses.add(toAddressResponse(address)));
            }
            response.setAddresses(addresses);
            responses.add(response);
        }
        return responses;
    }

    private GuestNetworkAddressResponse toAddressResponse(VmGuestIpAddress address) {
        GuestNetworkAddressResponse response = new GuestNetworkAddressResponse();
        if ("ipv4".equalsIgnoreCase(address.getFamily())) {
            response.setFamily("IPv4");
        } else if ("ipv6".equalsIgnoreCase(address.getFamily())) {
            response.setFamily("IPv6");
        } else {
            response.setFamily(address.getFamily());
        }
        response.setAddress(address.getAddress());
        response.setPrefix(address.getPrefix());
        response.setScope(address.getScope());
        response.setRole(StringUtils.defaultIfBlank(address.getRole(), "UNKNOWN"));
        response.setRoleSource(address.getRoleSource());
        response.setRepresentative(address.isRepresentative());
        return response;
    }

    private String normalizeAddressFamily(String family) {
        if ("ipv4".equalsIgnoreCase(family)) {
            return "IPv4";
        }
        if ("ipv6".equalsIgnoreCase(family)) {
            return "IPv6";
        }
        return family;
    }

    private List<GuestNetworkSectionResponse> toSectionResponses(
            Map<String, VmGuestNetworkSectionStatus> sectionStatuses,
            List<VmGuestNetworkSectionStateVO> persistedSections) {
        List<GuestNetworkSectionResponse> responses = new ArrayList<>();
        Map<String, VmGuestNetworkSectionStateVO> persisted = new LinkedHashMap<>();
        if (persistedSections != null) {
            persistedSections.forEach(row -> persisted.put(row.getSection(), row));
        }
        Set<String> names = new LinkedHashSet<>(persisted.keySet());
        if (sectionStatuses != null) {
            names.addAll(sectionStatuses.keySet());
        }
        names.forEach(name -> {
            VmGuestNetworkSectionStatus section =
                    sectionStatuses == null ? null : sectionStatuses.get(name);
            VmGuestNetworkSectionStateVO row = persisted.get(name);
            GuestNetworkSectionResponse response = new GuestNetworkSectionResponse();
            response.setName(name);
            response.setStatus(row == null
                    ? section == null ? NOT_COLLECTED : section.getStatus() : row.getStatus());
            response.setDetails(row != null && StringUtils.isNotBlank(row.getErrorMessage())
                    ? row.getErrorMessage() : section == null ? null : section.getDetails());
            response.setTruncated(section != null && section.isTruncated());
            response.setOriginalCount(section == null ? null : section.getOriginalCount());
            response.setSource(row == null
                    ? section == null ? null : section.getSource() : row.getSource());
            response.setErrorCode(row == null
                    ? section == null ? null : section.getErrorCode() : row.getErrorCode());
            response.setObserved(row == null ? null : row.getObservedAt());
            response.setLastSuccess(row == null ? null : row.getLastSuccessAt());
            response.setNextDue(row == null ? null : row.getNextDueAt());
            responses.add(response);
        });
        return responses;
    }

    @Override
    public GuestNetworkRefreshResponse requestRefresh(long vmId, Set<String> sections) {
        UserVmVO vm = userVmDao.findById(vmId);
        if (vm == null) {
            throw new InvalidParameterValueException("Unable to find Instance with ID " + vmId);
        }
        Account caller = CallContext.current().getCallingAccount();
        accountManager.checkAccess(caller, AccessType.OperateEntry, true, vm);
        Set<String> allowed = new LinkedHashSet<>(
                java.util.Arrays.asList("interfaces", "routes", "dns", "readiness"));
        Set<String> requested = sections == null || sections.isEmpty()
                ? allowed : new LinkedHashSet<>(sections);
        if (!allowed.containsAll(requested)) {
            throw new InvalidParameterValueException(
                    "Guest network sections must be interfaces, routes, dns, or readiness");
        }
        boolean accepted = scheduleService.requestRefresh(
                vmId, requested, new Date(), 30);
        GuestNetworkRefreshResponse response = new GuestNetworkRefreshResponse();
        response.setAccepted(accepted);
        response.setRequestedSections(new ArrayList<>(requested));
        return response;
    }

    private List<GuestNetworkRouteResponse> toRouteResponses(List<VmGuestRoute> routes) {
        List<GuestNetworkRouteResponse> responses = new ArrayList<>();
        if (routes == null) {
            return responses;
        }
        for (VmGuestRoute route : routes) {
            GuestNetworkRouteResponse response = new GuestNetworkRouteResponse();
            if ("ipv4".equalsIgnoreCase(route.getFamily())) {
                response.setFamily("IPv4");
            } else if ("ipv6".equalsIgnoreCase(route.getFamily())) {
                response.setFamily("IPv6");
            } else {
                response.setFamily(route.getFamily());
            }
            response.setDestination(route.getDestination());
            response.setPrefix(route.getPrefix());
            response.setGateway(route.getGateway());
            response.setInterfaceName(route.getInterfaceName());
            response.setMetric(route.getMetric());
            response.setTable(route.getTable());
            response.setProtocol(route.getProtocol());
            response.setScope(route.getScope());
            response.setDefaultRoute(route.isDefaultRoute());
            responses.add(response);
        }
        return responses;
    }

    private GuestNetworkDnsResponse toDnsResponse(VmGuestDnsState dns) {
        GuestNetworkDnsResponse response = new GuestNetworkDnsResponse();
        if (dns == null) {
            return response;
        }
        response.setSource(dns.getSource());
        response.setUpstreamServersKnown(dns.isUpstreamServersKnown());
        response.setServers(dns.getServers() == null
                ? new ArrayList<>() : new ArrayList<>(dns.getServers()));
        response.setSearchDomains(dns.getSearchDomains() == null
                ? new ArrayList<>() : new ArrayList<>(dns.getSearchDomains()));
        List<GuestNetworkDnsConfigResponse> configurations = new ArrayList<>();
        if (dns.getConfigurations() != null) {
            for (VmGuestDnsConfig config : dns.getConfigurations()) {
                GuestNetworkDnsConfigResponse configResponse = new GuestNetworkDnsConfigResponse();
                configResponse.setInterfaceName(config.getInterfaceName());
                configResponse.setSource(config.getSource());
                configResponse.setGlobal(config.isGlobal());
                List<GuestNetworkDnsServerResponse> servers = new ArrayList<>();
                if (config.getServers() != null) {
                    config.getServers().forEach(server -> servers.add(toDnsServerResponse(server)));
                }
                configResponse.setServers(servers);
                List<GuestNetworkDnsDomainResponse> domains = new ArrayList<>();
                if (config.getDomains() != null) {
                    config.getDomains().forEach(domain -> domains.add(toDnsDomainResponse(domain)));
                }
                configResponse.setDomains(domains);
                configurations.add(configResponse);
            }
        }
        response.setConfigurations(configurations);
        return response;
    }

    private GuestNetworkDnsServerResponse toDnsServerResponse(VmGuestDnsServer server) {
        GuestNetworkDnsServerResponse response = new GuestNetworkDnsServerResponse();
        response.setAddress(server.getAddress());
        if ("ipv4".equalsIgnoreCase(server.getFamily())) {
            response.setFamily("IPv4");
        } else if ("ipv6".equalsIgnoreCase(server.getFamily())) {
            response.setFamily("IPv6");
        } else {
            response.setFamily(server.getFamily());
        }
        response.setLocalStub(server.isLocalStub());
        return response;
    }

    private GuestNetworkDnsDomainResponse toDnsDomainResponse(VmGuestDnsDomain domain) {
        GuestNetworkDnsDomainResponse response = new GuestNetworkDnsDomainResponse();
        response.setDomain(domain.getDomain());
        response.setRoutingOnly(domain.isRoutingOnly());
        return response;
    }

    private String formatAddress(VmGuestIpAddress address) {
        return address.getPrefix() == null
                ? address.getAddress()
                : address.getAddress() + "/" + address.getPrefix();
    }
}
