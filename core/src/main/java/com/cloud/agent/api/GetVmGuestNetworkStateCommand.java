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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.agent.api.LogLevel.Log4jLevel;

@LogLevel(Log4jLevel.Trace)
public class GetVmGuestNetworkStateCommand extends Command {
    public static final int DEFAULT_TIMEOUT_SECONDS = 3;
    public static final int DEFAULT_MAX_EXEC_OUTPUT_BYTES = 1024 * 1024;

    private List<String> vmNames;
    private Map<String, Map<String, String>> cloudNicIdsByVmAndMac;
    private Set<String> vmNamesWithCachedInterfaceCapability;
    private Set<String> vmNamesRequiringInterfaces;
    private Set<String> vmNamesRequiringRoutes;
    private Set<String> vmNamesRequiringDns;
    private boolean execFallbackEnabled;
    private int maxExecOutputBytes;
    private int timeoutSeconds;
    private boolean preferGuestToolsHelper;
    private Set<String> vmNamesRequiringReadiness;
    private Long collectorHostId;

    protected GetVmGuestNetworkStateCommand() {
        vmNames = new ArrayList<>();
        cloudNicIdsByVmAndMac = new LinkedHashMap<>();
        vmNamesWithCachedInterfaceCapability = new LinkedHashSet<>();
        vmNamesRequiringInterfaces = new LinkedHashSet<>();
        vmNamesRequiringRoutes = new LinkedHashSet<>();
        vmNamesRequiringDns = new LinkedHashSet<>();
        maxExecOutputBytes = DEFAULT_MAX_EXEC_OUTPUT_BYTES;
        timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
        vmNamesRequiringReadiness = new LinkedHashSet<>();
    }

    public GetVmGuestNetworkStateCommand(List<String> vmNames,
            Map<String, Map<String, String>> cloudNicIdsByVmAndMac) {
        this(vmNames, cloudNicIdsByVmAndMac, DEFAULT_TIMEOUT_SECONDS);
    }

    public GetVmGuestNetworkStateCommand(List<String> vmNames,
            Map<String, Map<String, String>> cloudNicIdsByVmAndMac,
            int timeoutSeconds) {
        this(vmNames, cloudNicIdsByVmAndMac, timeoutSeconds, null);
    }

    public GetVmGuestNetworkStateCommand(List<String> vmNames,
            Map<String, Map<String, String>> cloudNicIdsByVmAndMac,
            int timeoutSeconds,
            Set<String> vmNamesWithCachedInterfaceCapability) {
        this(vmNames, cloudNicIdsByVmAndMac, timeoutSeconds, vmNamesWithCachedInterfaceCapability,
                vmNames == null ? null : new LinkedHashSet<>(vmNames), null,
                null, false, DEFAULT_MAX_EXEC_OUTPUT_BYTES);
    }

    public GetVmGuestNetworkStateCommand(List<String> vmNames,
            Map<String, Map<String, String>> cloudNicIdsByVmAndMac,
            int timeoutSeconds,
            Set<String> vmNamesWithCachedInterfaceCapability,
            Set<String> vmNamesRequiringInterfaces,
            Set<String> vmNamesRequiringRoutes,
            boolean execFallbackEnabled,
            int maxExecOutputBytes) {
        this(vmNames, cloudNicIdsByVmAndMac, timeoutSeconds,
                vmNamesWithCachedInterfaceCapability, vmNamesRequiringInterfaces,
                vmNamesRequiringRoutes, null, execFallbackEnabled, maxExecOutputBytes);
    }

    public GetVmGuestNetworkStateCommand(List<String> vmNames,
            Map<String, Map<String, String>> cloudNicIdsByVmAndMac,
            int timeoutSeconds,
            Set<String> vmNamesWithCachedInterfaceCapability,
            Set<String> vmNamesRequiringInterfaces,
            Set<String> vmNamesRequiringRoutes,
            Set<String> vmNamesRequiringDns,
            boolean execFallbackEnabled,
            int maxExecOutputBytes) {
        this.vmNames = vmNames == null ? new ArrayList<>() : new ArrayList<>(vmNames);
        this.cloudNicIdsByVmAndMac = copyCloudNicIds(cloudNicIdsByVmAndMac);
        this.timeoutSeconds = Math.max(1, timeoutSeconds);
        this.vmNamesWithCachedInterfaceCapability = vmNamesWithCachedInterfaceCapability == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(vmNamesWithCachedInterfaceCapability);
        this.vmNamesRequiringInterfaces = vmNamesRequiringInterfaces == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(vmNamesRequiringInterfaces);
        this.vmNamesRequiringRoutes = vmNamesRequiringRoutes == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(vmNamesRequiringRoutes);
        this.vmNamesRequiringDns = vmNamesRequiringDns == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(vmNamesRequiringDns);
        this.execFallbackEnabled = execFallbackEnabled;
        this.maxExecOutputBytes = Math.max(1024, maxExecOutputBytes);
        this.vmNamesRequiringReadiness = new LinkedHashSet<>();
    }

    private Map<String, Map<String, String>> copyCloudNicIds(
            Map<String, Map<String, String>> source) {
        Map<String, Map<String, String>> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        source.forEach((vmName, nicIds) -> copy.put(vmName,
                nicIds == null ? new LinkedHashMap<>() : new LinkedHashMap<>(nicIds)));
        return copy;
    }

    public List<String> getVmNames() {
        return vmNames;
    }

    public Map<String, String> getCloudNicIdsForVm(String vmName) {
        if (cloudNicIdsByVmAndMac == null || !cloudNicIdsByVmAndMac.containsKey(vmName)) {
            return new LinkedHashMap<>();
        }
        return cloudNicIdsByVmAndMac.get(vmName);
    }

    public Map<String, Map<String, String>> getCloudNicIdsByVmAndMac() {
        return cloudNicIdsByVmAndMac;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public boolean hasCachedInterfaceCapability(String vmName) {
        return vmNamesWithCachedInterfaceCapability != null
                && vmNamesWithCachedInterfaceCapability.contains(vmName);
    }

    public Set<String> getVmNamesWithCachedInterfaceCapability() {
        return vmNamesWithCachedInterfaceCapability;
    }

    public boolean shouldCollectInterfaces(String vmName) {
        return vmNamesRequiringInterfaces != null && vmNamesRequiringInterfaces.contains(vmName);
    }

    public boolean shouldCollectRoutes(String vmName) {
        return vmNamesRequiringRoutes != null && vmNamesRequiringRoutes.contains(vmName);
    }

    public Set<String> getVmNamesRequiringInterfaces() {
        return vmNamesRequiringInterfaces;
    }

    public Set<String> getVmNamesRequiringRoutes() {
        return vmNamesRequiringRoutes;
    }

    public boolean shouldCollectDns(String vmName) {
        return vmNamesRequiringDns != null && vmNamesRequiringDns.contains(vmName);
    }

    public Set<String> getVmNamesRequiringDns() {
        return vmNamesRequiringDns;
    }

    public boolean isExecFallbackEnabled() {
        return execFallbackEnabled;
    }

    public int getMaxExecOutputBytes() {
        return maxExecOutputBytes;
    }

    public boolean isPreferGuestToolsHelper() {
        return preferGuestToolsHelper;
    }

    public void setPreferGuestToolsHelper(boolean preferGuestToolsHelper) {
        this.preferGuestToolsHelper = preferGuestToolsHelper;
    }

    public boolean shouldCollectReadiness(String vmName) {
        return vmNamesRequiringReadiness != null && vmNamesRequiringReadiness.contains(vmName);
    }

    public Set<String> getVmNamesRequiringReadiness() {
        return vmNamesRequiringReadiness;
    }

    public void setVmNamesRequiringReadiness(Set<String> vmNamesRequiringReadiness) {
        this.vmNamesRequiringReadiness = vmNamesRequiringReadiness == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(vmNamesRequiringReadiness);
    }

    public Long getCollectorHostId() {
        return collectorHostId;
    }

    public void setCollectorHostId(Long collectorHostId) {
        this.collectorHostId = collectorHostId;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
