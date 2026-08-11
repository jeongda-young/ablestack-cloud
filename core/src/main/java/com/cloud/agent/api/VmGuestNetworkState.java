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
package com.cloud.agent.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VmGuestNetworkState {
    public static final int CURRENT_SCHEMA_VERSION = 3;

    private int schemaVersion;
    private String vmName;
    private String status;
    private long observedAt;
    private boolean agentConnected;
    private String agentVersion;
    private String collectorBuildId;
    private Long collectorHostId;
    private String capabilityHash;
    private VmGuestToolsInfo guestTools;
    private Map<String, Boolean> capabilities;
    private Map<String, VmGuestNetworkSectionStatus> sectionStatuses;
    private List<VmGuestNetworkInterface> interfaces;
    private VmGuestDnsState dns;
    private List<VmGuestRoute> routes;

    public VmGuestNetworkState() {
        schemaVersion = CURRENT_SCHEMA_VERSION;
        capabilities = new LinkedHashMap<>();
        sectionStatuses = new LinkedHashMap<>();
        interfaces = new ArrayList<>();
        dns = new VmGuestDnsState();
        routes = new ArrayList<>();
        guestTools = new VmGuestToolsInfo();
    }

    public VmGuestNetworkState(String vmName) {
        this();
        this.vmName = vmName;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(long observedAt) {
        this.observedAt = observedAt;
    }

    public boolean isAgentConnected() {
        return agentConnected;
    }

    public void setAgentConnected(boolean agentConnected) {
        this.agentConnected = agentConnected;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public String getCollectorBuildId() {
        return collectorBuildId;
    }

    public void setCollectorBuildId(String collectorBuildId) {
        this.collectorBuildId = collectorBuildId;
    }

    public Long getCollectorHostId() {
        return collectorHostId;
    }

    public void setCollectorHostId(Long collectorHostId) {
        this.collectorHostId = collectorHostId;
    }

    public String getCapabilityHash() {
        return capabilityHash;
    }

    public void setCapabilityHash(String capabilityHash) {
        this.capabilityHash = capabilityHash;
    }

    public VmGuestToolsInfo getGuestTools() {
        return guestTools;
    }

    public void setGuestTools(VmGuestToolsInfo guestTools) {
        this.guestTools = guestTools;
    }

    public Map<String, Boolean> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Map<String, Boolean> capabilities) {
        this.capabilities = capabilities;
    }

    public void putCapability(String name, boolean enabled) {
        capabilities.put(name, enabled);
    }

    public Map<String, VmGuestNetworkSectionStatus> getSectionStatuses() {
        return sectionStatuses;
    }

    public void setSectionStatuses(Map<String, VmGuestNetworkSectionStatus> sectionStatuses) {
        this.sectionStatuses = sectionStatuses;
    }

    public void putSectionStatus(String section, VmGuestNetworkSectionStatus sectionStatus) {
        sectionStatuses.put(section, sectionStatus);
    }

    public List<VmGuestNetworkInterface> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<VmGuestNetworkInterface> interfaces) {
        this.interfaces = interfaces;
    }

    public VmGuestDnsState getDns() {
        return dns;
    }

    public void setDns(VmGuestDnsState dns) {
        this.dns = dns;
    }

    public List<VmGuestRoute> getRoutes() {
        return routes;
    }

    public void setRoutes(List<VmGuestRoute> routes) {
        this.routes = routes;
    }
}
