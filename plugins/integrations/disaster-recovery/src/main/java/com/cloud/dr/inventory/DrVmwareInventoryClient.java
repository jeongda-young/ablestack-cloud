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
package com.cloud.dr.inventory;

import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.DrResolvedSiteCredential;
import com.cloud.dr.DrSiteCredentialVO;
import com.cloud.dr.health.DrSiteProbeSupport;
import com.cloud.dr.inventory.DrMoldInventoryClient.InventoryException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DrVmwareInventoryClient {
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;

    public List<DrInventoryOption> listVirtualMachines(DrResolvedSiteCredential credential, String keyword) {
        try {
            DrSiteCredentialVO credentialVo = credential.getCredential();
            String endpoint = StringUtils.defaultString(credentialVo.getEndpoint());
            String rootEndpoint = DrSiteProbeSupport.normalizeRootEndpoint(endpoint, "https");
            String principal = StringUtils.trimToNull(credentialVo.getPrincipal());
            String password = getSecret(credential.getSecretPayload(), "password");
            if (StringUtils.isAnyBlank(rootEndpoint, principal, password)) {
                throw new InventoryException(0, "vCenter URL, username, and password are required");
            }
            String sessionId = openSession(rootEndpoint, principal, password, credentialVo.getTlsVerify());
            return toVirtualMachineOptions(fetchVmArray(rootEndpoint, sessionId, credentialVo.getTlsVerify()), keyword);
        } catch (InventoryException e) {
            throw e;
        } catch (Exception e) {
            throw new InventoryException(0, "vCenter VM inventory request failed: " + e.getClass().getSimpleName());
        }
    }

    public List<DrInventoryOption> listVirtualMachineDisks(DrResolvedSiteCredential credential, String vmRef) {
        return listVirtualMachineHardware(credential, vmRef, "disk");
    }

    public List<DrInventoryOption> listVirtualMachineNics(DrResolvedSiteCredential credential, String vmRef) {
        return listVirtualMachineHardware(credential, vmRef, "ethernet");
    }

    public String getVirtualMachinePowerState(DrResolvedSiteCredential credential, String vmRef) {
        try {
            SessionContext context = sessionContext(credential);
            return fetchPowerState(context.rootEndpoint, context.sessionId, context.tlsVerify, vmRef);
        } catch (InventoryException e) {
            throw e;
        } catch (Exception e) {
            throw new InventoryException(0, "vCenter VM power-state request failed: " + e.getClass().getSimpleName());
        }
    }

    public String ensureVirtualMachinePowerState(DrResolvedSiteCredential credential, String vmRef, boolean poweredOn) {
        try {
            SessionContext context = sessionContext(credential);
            String expected = poweredOn ? "POWERED_ON" : "POWERED_OFF";
            if (StringUtils.equals(expected, fetchPowerState(context.rootEndpoint, context.sessionId, context.tlsVerify, vmRef))) {
                return expected;
            }
            invokePowerAction(context.rootEndpoint, context.sessionId, context.tlsVerify, vmRef,
                    poweredOn ? "start" : "stop");
            for (int attempt = 0; attempt < 30; attempt++) {
                if (StringUtils.equals(expected, fetchPowerState(context.rootEndpoint, context.sessionId, context.tlsVerify, vmRef))) {
                    return expected;
                }
                Thread.sleep(2000L);
            }
            throw new InventoryException(0, "vCenter VM did not reach " + expected + ": " + vmRef);
        } catch (InventoryException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InventoryException(0, "vCenter VM power wait was interrupted");
        } catch (Exception e) {
            throw new InventoryException(0, "vCenter VM power action failed: " + e.getClass().getSimpleName());
        }
    }

    public String validateVirtualMachineGuestBoot(DrResolvedSiteCredential credential, String vmRef) {
        try {
            SessionContext context = sessionContext(credential);
            InventoryException last = null;
            for (int attempt = 0; attempt < 60; attempt++) {
                for (String prefix : new String[] {"/rest/vcenter/vm/", "/api/vcenter/vm/"}) {
                    try {
                        JsonObject identity = fetchVmObject(DrSiteProbeSupport.appendPath(context.rootEndpoint,
                                prefix + encodePath(vmRef) + "/guest/identity"),
                                "vmware-api-session-id", context.sessionId, context.tlsVerify);
                        String family = firstString(identity, "family", "guest_family", "guestFamily");
                        String name = firstString(identity, "name", "host_name", "hostName");
                        String fullName = firstString(identity, "full_name", "fullName");
                        if (StringUtils.isNotBlank(family) || StringUtils.isNotBlank(name)
                                || StringUtils.isNotBlank(fullName)) {
                            return "GUEST_HEARTBEAT_VALIDATED";
                        }
                    } catch (InventoryException e) {
                        last = e;
                    }
                }
                Thread.sleep(2000L);
            }
            throw last != null ? last : new InventoryException(0,
                    "vCenter guest identity did not become available for " + vmRef);
        } catch (InventoryException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InventoryException(0, "vCenter guest boot validation was interrupted");
        } catch (Exception e) {
            throw new InventoryException(0,
                    "vCenter guest boot validation failed: " + e.getClass().getSimpleName());
        }
    }

    private List<DrInventoryOption> listVirtualMachineHardware(DrResolvedSiteCredential credential, String vmRef, String hardwareType) {
        if (StringUtils.isBlank(vmRef)) {
            return new ArrayList<DrInventoryOption>();
        }
        try {
            DrSiteCredentialVO credentialVo = credential.getCredential();
            String endpoint = StringUtils.defaultString(credentialVo.getEndpoint());
            String rootEndpoint = DrSiteProbeSupport.normalizeRootEndpoint(endpoint, "https");
            String principal = StringUtils.trimToNull(credentialVo.getPrincipal());
            String password = getSecret(credential.getSecretPayload(), "password");
            if (StringUtils.isAnyBlank(rootEndpoint, principal, password)) {
                throw new InventoryException(0, "vCenter URL, username, and password are required");
            }
            String sessionId = openSession(rootEndpoint, principal, password, credentialVo.getTlsVerify());
            JsonArray items = fetchVmHardwareArray(rootEndpoint, sessionId, credentialVo.getTlsVerify(), vmRef, hardwareType);
            return "disk".equals(hardwareType) ? toDiskOptions(items, rootEndpoint, sessionId, credentialVo.getTlsVerify(), vmRef) : toNicOptions(items, vmRef);
        } catch (InventoryException e) {
            throw e;
        } catch (Exception e) {
            throw new InventoryException(0, "vCenter VM " + hardwareType + " inventory request failed: " + e.getClass().getSimpleName());
        }
    }

    private String openSession(String rootEndpoint, String principal, String password, Boolean tlsVerify) throws Exception {
        InventoryException fallback = null;
        try {
            return openRestSession(DrSiteProbeSupport.appendPath(rootEndpoint, "/rest/com/vmware/cis/session"), principal, password, tlsVerify);
        } catch (InventoryException e) {
            fallback = e;
        }
        try {
            return openRestSession(DrSiteProbeSupport.appendPath(rootEndpoint, "/api/session"), principal, password, tlsVerify);
        } catch (InventoryException e) {
            throw fallback != null ? fallback : e;
        }
    }

    private SessionContext sessionContext(DrResolvedSiteCredential credential) throws Exception {
        DrSiteCredentialVO credentialVo = credential.getCredential();
        String rootEndpoint = DrSiteProbeSupport.normalizeRootEndpoint(
                StringUtils.defaultString(credentialVo.getEndpoint()), "https");
        String principal = StringUtils.trimToNull(credentialVo.getPrincipal());
        String password = getSecret(credential.getSecretPayload(), "password");
        if (StringUtils.isAnyBlank(rootEndpoint, principal, password)) {
            throw new InventoryException(0, "vCenter URL, username, and password are required");
        }
        return new SessionContext(rootEndpoint, openSession(rootEndpoint, principal, password,
                credentialVo.getTlsVerify()), credentialVo.getTlsVerify());
    }

    private String fetchPowerState(String rootEndpoint, String sessionId, Boolean tlsVerify, String vmRef) throws Exception {
        InventoryException fallback = null;
        for (String prefix : new String[] {"/rest/vcenter/vm/", "/api/vcenter/vm/"}) {
            try {
                JsonObject response = fetchVmObject(DrSiteProbeSupport.appendPath(rootEndpoint,
                        prefix + encodePath(vmRef) + "/power"), "vmware-api-session-id", sessionId, tlsVerify);
                return normalizePowerState(firstString(response, "state", "power_state", "powerState"));
            } catch (InventoryException e) {
                fallback = e;
            }
        }
        throw fallback != null ? fallback : new InventoryException(0, "vCenter power API unavailable");
    }

    private void invokePowerAction(String rootEndpoint, String sessionId, Boolean tlsVerify,
            String vmRef, String action) throws Exception {
        InventoryException fallback = null;
        String encodedVmRef = encodePath(vmRef);
        String[] actionUrls = {
                DrSiteProbeSupport.appendPath(rootEndpoint,
                        "/rest/vcenter/vm/" + encodedVmRef + "/power/" + action),
                DrSiteProbeSupport.appendPath(rootEndpoint,
                        "/api/vcenter/vm/" + encodedVmRef + "/power?action=" + action)
        };
        for (String url : actionUrls) {
            try {
                HttpURLConnection connection = DrSiteProbeSupport.openConnection(url, "POST", tlsVerify,
                        CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("vmware-api-session-id", sessionId);
                int responseCode = connection.getResponseCode();
                DrSiteProbeSupport.readBody(connection);
                if (responseCode >= 200 && responseCode < 300) {
                    return;
                }
                fallback = new InventoryException(responseCode,
                        "vCenter power action returned HTTP " + responseCode);
            } catch (InventoryException e) {
                fallback = e;
            }
        }
        throw fallback != null ? fallback : new InventoryException(0, "vCenter power action unavailable");
    }

    private String encodePath(String value) {
        return URLEncoder.encode(StringUtils.defaultString(value), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private String normalizePowerState(String state) {
        return StringUtils.equalsAnyIgnoreCase(state, "POWERED_ON", "poweredOn")
                ? "POWERED_ON" : StringUtils.equalsAnyIgnoreCase(state, "POWERED_OFF", "poweredOff")
                ? "POWERED_OFF" : StringUtils.upperCase(StringUtils.defaultIfBlank(state, "UNKNOWN"), Locale.ROOT);
    }

    private static class SessionContext {
        private final String rootEndpoint;
        private final String sessionId;
        private final Boolean tlsVerify;

        private SessionContext(String rootEndpoint, String sessionId, Boolean tlsVerify) {
            this.rootEndpoint = rootEndpoint;
            this.sessionId = sessionId;
            this.tlsVerify = tlsVerify;
        }
    }

    private String openRestSession(String url, String principal, String password, Boolean tlsVerify) throws Exception {
        HttpURLConnection connection = DrSiteProbeSupport.openConnection(url, "POST", tlsVerify, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", DrSiteProbeSupport.basicAuth(principal, password));
        int responseCode = connection.getResponseCode();
        String body = DrSiteProbeSupport.readBody(connection);
        if (responseCode == 401 || responseCode == 403) {
            throw new InventoryException(responseCode, "vCenter authentication failed with HTTP " + responseCode);
        }
        if (responseCode < 200 || responseCode >= 300) {
            throw new InventoryException(responseCode, "vCenter session API returned HTTP " + responseCode);
        }
        try {
            JsonElement parsed = JsonParser.parseString(StringUtils.defaultString(body, "\"\""));
            if (parsed != null && parsed.isJsonObject()) {
                String value = firstString(parsed.getAsJsonObject(), "value", "session_id", "sessionId");
                if (StringUtils.isNotBlank(value)) {
                    return value;
                }
            }
            if (parsed != null && parsed.isJsonPrimitive()) {
                return parsed.getAsString();
            }
        } catch (RuntimeException ignored) {
            String token = StringUtils.trimToNull(body);
            if (token != null) {
                return token;
            }
        }
        throw new InventoryException(responseCode, "vCenter session API did not return a session ID");
    }

    private JsonArray fetchVmArray(String rootEndpoint, String sessionId, Boolean tlsVerify) throws Exception {
        InventoryException fallback = null;
        try {
            return fetchVmArray(DrSiteProbeSupport.appendPath(rootEndpoint, "/rest/vcenter/vm"), "vmware-api-session-id", sessionId, tlsVerify);
        } catch (InventoryException e) {
            fallback = e;
        }
        try {
            return fetchVmArray(DrSiteProbeSupport.appendPath(rootEndpoint, "/api/vcenter/vm"), "vmware-api-session-id", sessionId, tlsVerify);
        } catch (InventoryException e) {
            throw fallback != null ? fallback : e;
        }
    }

    private JsonArray fetchVmArray(String url, String sessionHeader, String sessionId, Boolean tlsVerify) throws Exception {
        HttpURLConnection connection = DrSiteProbeSupport.openConnection(url, "GET", tlsVerify, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty(sessionHeader, sessionId);
        int responseCode = connection.getResponseCode();
        String body = DrSiteProbeSupport.readBody(connection);
        if (responseCode < 200 || responseCode >= 300) {
            throw new InventoryException(responseCode, "vCenter VM API returned HTTP " + responseCode);
        }
        JsonElement parsed = JsonParser.parseString(StringUtils.defaultString(body, "{}"));
        if (parsed != null && parsed.isJsonObject()) {
            JsonElement value = getElementIgnoreCase(parsed.getAsJsonObject(), "value");
            if (value != null && value.isJsonArray()) {
                return value.getAsJsonArray();
            }
        }
        return new JsonArray();
    }

    private JsonObject fetchVmObject(String url, String sessionHeader, String sessionId, Boolean tlsVerify) throws Exception {
        HttpURLConnection connection = DrSiteProbeSupport.openConnection(url, "GET", tlsVerify, CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty(sessionHeader, sessionId);
        int responseCode = connection.getResponseCode();
        String body = DrSiteProbeSupport.readBody(connection);
        if (responseCode < 200 || responseCode >= 300) {
            throw new InventoryException(responseCode, "vCenter VM API returned HTTP " + responseCode);
        }
        JsonElement parsed = JsonParser.parseString(StringUtils.defaultString(body, "{}"));
        if (parsed != null && parsed.isJsonObject()) {
            JsonObject object = parsed.getAsJsonObject();
            JsonElement value = getElementIgnoreCase(object, "value");
            if (value != null && value.isJsonObject()) {
                return value.getAsJsonObject();
            }
            return object;
        }
        return new JsonObject();
    }

    private JsonArray fetchVmHardwareArray(String rootEndpoint, String sessionId, Boolean tlsVerify, String vmRef, String hardwareType) throws Exception {
        InventoryException fallback = null;
        try {
            return fetchVmArray(DrSiteProbeSupport.appendPath(rootEndpoint, "/rest/vcenter/vm/" + vmRef + "/hardware/" + hardwareType),
                    "vmware-api-session-id", sessionId, tlsVerify);
        } catch (InventoryException e) {
            fallback = e;
        }
        try {
            return fetchVmArray(DrSiteProbeSupport.appendPath(rootEndpoint, "/api/vcenter/vm/" + vmRef + "/hardware/" + hardwareType),
                    "vmware-api-session-id", sessionId, tlsVerify);
        } catch (InventoryException e) {
            throw fallback != null ? fallback : e;
        }
    }

    private JsonObject fetchVmDiskDetail(String rootEndpoint, String sessionId, Boolean tlsVerify, String vmRef, String diskRef) throws Exception {
        InventoryException fallback = null;
        try {
            return fetchVmObject(DrSiteProbeSupport.appendPath(rootEndpoint, "/rest/vcenter/vm/" + vmRef + "/hardware/disk/" + diskRef),
                    "vmware-api-session-id", sessionId, tlsVerify);
        } catch (InventoryException e) {
            fallback = e;
        }
        try {
            return fetchVmObject(DrSiteProbeSupport.appendPath(rootEndpoint, "/api/vcenter/vm/" + vmRef + "/hardware/disk/" + diskRef),
                    "vmware-api-session-id", sessionId, tlsVerify);
        } catch (InventoryException e) {
            throw fallback != null ? fallback : e;
        }
    }

    private List<DrInventoryOption> toVirtualMachineOptions(JsonArray items, String keyword) {
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        String normalizedKeyword = StringUtils.lowerCase(StringUtils.trimToEmpty(keyword));
        if (items == null) {
            return options;
        }
        for (JsonElement item : items) {
            if (item == null || !item.isJsonObject()) {
                continue;
            }
            JsonObject object = item.getAsJsonObject();
            String vmRef = firstString(object, "vm", "vmid", "id");
            String name = firstString(object, "name");
            if (StringUtils.isNotBlank(normalizedKeyword) && !StringUtils.contains(StringUtils.lowerCase(StringUtils.defaultString(name) + " " + StringUtils.defaultString(vmRef)), normalizedKeyword)) {
                continue;
            }
            DrInventoryOption option = new DrInventoryOption();
            option.setType("SOURCE_WORKLOAD");
            option.setId(vmRef);
            option.setName(StringUtils.defaultIfBlank(name, vmRef));
            option.setDescription(firstString(object, "path", "folder"));
            option.setValue(vmRef);
            option.setExternalId(vmRef);
            option.setExternalRef(vmRef);
            option.setReferenceType("EXTERNAL_REF");
            option.setState(firstString(object, "power_state", "powerState"));
            option.setHypervisorType("VMWARE");
            option.setSelectable(StringUtils.isNotBlank(vmRef));
            putDetailIfNotBlank(option, "vmRef", vmRef);
            putDetailIfNotBlank(option, "name", name);
            putDetailIfNotBlank(option, "powerState", option.getState());
            putDetailIfNotBlank(option, "cpuCount", firstString(object, "cpu_count", "cpuCount"));
            putDetailIfNotBlank(option, "memoryMiB", firstString(object, "memory_size_MiB", "memorySizeMiB"));
            options.add(option);
        }
        return options;
    }

    private List<DrInventoryOption> toDiskOptions(JsonArray items, String rootEndpoint, String sessionId, Boolean tlsVerify, String vmRef) {
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        if (items == null) {
            return options;
        }
        int index = 0;
        for (JsonElement item : items) {
            if (item == null || !item.isJsonObject()) {
                continue;
            }
            JsonObject object = item.getAsJsonObject();
            String diskRef = firstString(object, "disk", "disk_id", "diskId", "id", "key");
            JsonObject detail = fetchVmDiskDetailQuietly(rootEndpoint, sessionId, tlsVerify, vmRef, diskRef);
            JsonObject backingObject = objectAt(detail, "backing");
            String label = firstNonBlank(firstString(detail, "label", "name"), firstString(object, "label", "name"));
            String backing = firstNonBlank(firstString(backingObject, "vmdk_file", "vmdkFile", "file", "path"),
                    firstString(detail, "backing", "file", "vmdk_file", "vmdkFile", "path"),
                    firstString(object, "backing", "file", "vmdk_file", "vmdkFile", "path"));
            String capacity = positiveLongString(firstNonBlank(firstString(detail, "capacity", "capacityBytes", "capacity_bytes", "sizeBytes", "size_bytes"),
                    firstString(object, "capacity", "capacityBytes", "capacity_bytes", "sizeBytes", "size_bytes")));
            String diskType = firstNonBlank(firstString(detail, "type"), firstString(object, "type"));
            String controllerBus = firstNonBlank(firstString(detail, "bus", "controller_bus", "controllerBus", "controllerBusNumber"),
                    firstString(object, "bus", "controller_bus", "controllerBus", "controllerBusNumber"));
            String unitNumber = firstNonBlank(firstString(detail, "unit", "unit_number", "unitNumber"),
                    firstString(object, "unit", "unit_number", "unitNumber"));
            String cbtDiskId = inferVmwareCbtDiskId(controllerBus, unitNumber);
            DrInventoryOption option = new DrInventoryOption();
            option.setType("SOURCE_DISK");
            option.setId(StringUtils.defaultIfBlank(diskRef, String.valueOf(index)));
            option.setValue(StringUtils.defaultIfBlank(diskRef, String.valueOf(index)));
            option.setExternalId(diskRef);
            option.setReferenceType("VMWARE_DISK_REF");
            option.setName(StringUtils.defaultIfBlank(label, "Disk " + (index + 1)));
            option.setDescription(backing);
            option.setSelectable(true);
            option.putDetail("vmRef", vmRef);
            putDetailIfNotBlank(option, "diskRef", diskRef);
            putDetailIfNotBlank(option, "label", label);
            putDetailIfNotBlank(option, "path", backing);
            putDetailIfNotBlank(option, "vmdkFile", backing);
            putDetailIfNotBlank(option, "capacityBytes", capacity);
            putDetailIfNotBlank(option, "sizeBytes", capacity);
            putDetailIfNotBlank(option, "backingType", firstString(backingObject, "type"));
            putDetailIfNotBlank(option, "type", diskType);
            putDetailIfNotBlank(option, "controllerType", diskType);
            putDetailIfNotBlank(option, "controllerBus", controllerBus);
            putDetailIfNotBlank(option, "controllerBusNumber", controllerBus);
            putDetailIfNotBlank(option, "unit", unitNumber);
            putDetailIfNotBlank(option, "unitNumber", unitNumber);
            putDetailIfNotBlank(option, "deviceKey", diskRef);
            putDetailIfNotBlank(option, "cbtDiskId", cbtDiskId);
            options.add(option);
            index++;
        }
        return options;
    }

    private List<DrInventoryOption> toNicOptions(JsonArray items, String vmRef) {
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        if (items == null) {
            return options;
        }
        int index = 0;
        for (JsonElement item : items) {
            if (item == null || !item.isJsonObject()) {
                continue;
            }
            JsonObject object = item.getAsJsonObject();
            String nicRef = firstString(object, "nic", "ethernet", "id", "key");
            String label = firstString(object, "label", "name");
            String backing = firstString(object, "backing", "network", "network_name", "networkName");
            DrInventoryOption option = new DrInventoryOption();
            option.setType("SOURCE_NIC");
            option.setId(StringUtils.defaultIfBlank(nicRef, String.valueOf(index)));
            option.setValue(StringUtils.defaultIfBlank(nicRef, String.valueOf(index)));
            option.setExternalId(nicRef);
            option.setReferenceType("VMWARE_NIC_REF");
            option.setName(StringUtils.defaultIfBlank(label, "NIC " + (index + 1)));
            option.setDescription(backing);
            option.setSelectable(true);
            option.putDetail("vmRef", vmRef);
            putDetailIfNotBlank(option, "nicRef", nicRef);
            putDetailIfNotBlank(option, "label", label);
            putDetailIfNotBlank(option, "network", backing);
            putDetailIfNotBlank(option, "macAddress", firstString(object, "mac_address", "macAddress", "mac"));
            putDetailIfNotBlank(option, "state", firstString(object, "state"));
            options.add(option);
            index++;
        }
        return options;
    }

    private void putDetailIfNotBlank(DrInventoryOption option, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            option.putDetail(key, value);
        }
    }

    private JsonObject fetchVmDiskDetailQuietly(String rootEndpoint, String sessionId, Boolean tlsVerify, String vmRef, String diskRef) {
        if (StringUtils.isAnyBlank(vmRef, diskRef)) {
            return null;
        }
        try {
            return fetchVmDiskDetail(rootEndpoint, sessionId, tlsVerify, vmRef, diskRef);
        } catch (Exception ignored) {
            return null;
        }
    }

    private JsonObject objectAt(JsonObject object, String key) {
        JsonElement element = object != null ? getElementIgnoreCase(object, key) : null;
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = StringUtils.trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private String positiveLongString(String value) {
        String trimmed = StringUtils.trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            long parsed = Long.parseLong(trimmed);
            return parsed > 0L ? String.valueOf(parsed) : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String inferVmwareCbtDiskId(String controllerBus, String unitNumber) {
        Long bus = positiveOrZeroLong(controllerBus);
        Long unit = positiveOrZeroLong(unitNumber);
        if (bus == null || unit == null) {
            return null;
        }
        return "scsi" + bus + ":" + unit;
    }

    private Long positiveOrZeroLong(String value) {
        String trimmed = StringUtils.trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            long parsed = Long.parseLong(trimmed);
            return parsed >= 0L ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String getSecret(JsonObject secret, String key) {
        JsonElement element = secret != null ? getElementIgnoreCase(secret, key) : null;
        return element != null && !element.isJsonNull() ? StringUtils.trimToNull(element.getAsString()) : null;
    }

    private JsonElement getElementIgnoreCase(JsonObject object, String key) {
        if (object == null || StringUtils.isBlank(key)) {
            return null;
        }
        if (object.has(key)) {
            return object.get(key);
        }
        String lowerKey = key.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (StringUtils.equals(entry.getKey().toLowerCase(Locale.ROOT), lowerKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String firstString(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement element = getElementIgnoreCase(object, key);
            if (element != null && !element.isJsonNull()) {
                String value = null;
                if (element.isJsonPrimitive()) {
                    value = StringUtils.trimToNull(element.getAsString());
                } else if (element.isJsonObject()) {
                    value = firstString(element.getAsJsonObject(), "file", "vmdk_file", "vmdkFile", "path", "network", "network_name", "networkName", "label", "name", "type");
                }
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }
}
