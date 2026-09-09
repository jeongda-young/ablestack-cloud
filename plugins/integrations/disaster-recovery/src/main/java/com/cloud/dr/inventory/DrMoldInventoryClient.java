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

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.DrResolvedSiteCredential;
import com.cloud.dr.DrSiteCredentialVO;
import com.cloud.dr.health.DrSiteProbeSupport;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DrMoldInventoryClient {
    static final String SOURCE_VM_DETAIL_PREFIX = "vmDetail.";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final String COMMAND_LIST_ZONES = "listZones";
    private static final String COMMAND_LIST_VMWARE_DCS = "listVmwareDcs";
    private static final String COMMAND_LIST_VMS = "listVirtualMachines";
    private static final String COMMAND_LIST_VOLUMES = "listVolumes";
    private static final String COMMAND_LIST_NICS = "listNics";
    private static final String COMMAND_LIST_STORAGE_POOLS = "listStoragePools";
    private static final String COMMAND_START_VM = "startVirtualMachine";
    private static final String COMMAND_STOP_VM = "stopVirtualMachine";
    private static final String COMMAND_QUERY_ASYNC_JOB = "queryAsyncJobResult";
    private static final String COMMAND_EXECUTE_DR_SITE_AGENT = "executeFtctlDrSiteAgentCommand";
    private static final String COMMAND_PREPARE_REMOTE_SSH = "prepareFtctlDrRemoteSshAccess";

    public List<DrInventoryOption> listZones(DrResolvedSiteCredential credential) {
        JsonObject response = execute(credential, COMMAND_LIST_ZONES, null);
        JsonObject payload = getObjectIgnoreCase(response, "listzonesresponse");
        return toOptions(getArrayIgnoreCase(payload, "zone"), "ZONE");
    }

    public List<DrInventoryOption> listVmwareDatacenters(DrResolvedSiteCredential credential, String zoneExternalId, Long zoneId) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        if (StringUtils.isNotBlank(zoneExternalId)) {
            params.put("zoneid", StringUtils.trim(zoneExternalId));
        } else if (zoneId != null) {
            params.put("zoneid", String.valueOf(zoneId));
        }
        JsonObject response = execute(credential, COMMAND_LIST_VMWARE_DCS, params);
        JsonObject payload = getObjectIgnoreCase(response, "listvmwaredcsresponse");
        JsonArray items = getArrayIgnoreCase(payload, "VMwareDC");
        if (items == null || items.size() == 0) {
            items = getArrayIgnoreCase(payload, "vmwaredc");
        }
        return toOptions(items, "VMWARE_DATACENTER");
    }

    public List<DrInventoryOption> listVirtualMachines(DrResolvedSiteCredential credential, String keyword, String zoneExternalId, Long zoneId) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("listall", "true");
        params.put("details", "all");
        if (StringUtils.isNotBlank(keyword)) {
            params.put("keyword", StringUtils.trim(keyword));
        }
        if (StringUtils.isNotBlank(zoneExternalId)) {
            params.put("zoneid", StringUtils.trim(zoneExternalId));
        } else if (zoneId != null) {
            params.put("zoneid", String.valueOf(zoneId));
        }
        JsonObject response = execute(credential, COMMAND_LIST_VMS, params);
        JsonObject payload = getObjectIgnoreCase(response, "listvirtualmachinesresponse");
        return toVirtualMachineOptions(getArrayIgnoreCase(payload, "virtualmachine"));
    }

    public List<DrInventoryOption> listVirtualMachineDisks(DrResolvedSiteCredential credential, String vmRef) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("virtualmachineid", StringUtils.trim(vmRef));
        params.put("listall", "true");
        JsonObject response = execute(credential, COMMAND_LIST_VOLUMES, params);
        JsonObject payload = getObjectIgnoreCase(response, "listvolumesresponse");
        JsonArray volumes = getArrayIgnoreCase(payload, "volume");
        Map<String, JsonObject> pools = new LinkedHashMap<String, JsonObject>();
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        for (JsonElement item : volumes) {
            if (item == null || !item.isJsonObject()) {
                continue;
            }
            JsonObject volume = item.getAsJsonObject();
            String storageId = firstString(volume, "storageid");
            JsonObject pool = StringUtils.isNotBlank(storageId)
                    ? pools.computeIfAbsent(storageId, id -> getStoragePool(credential, id)) : new JsonObject();
            String poolType = firstString(pool, "type");
            String poolPath = firstString(pool, "path");
            String volumePath = firstString(volume, "path", "id");
            String sourcePath = canonicalStoragePath(poolType, poolPath, volumePath);
            String deviceId = firstString(volume, "deviceid");

            DrInventoryOption option = new DrInventoryOption();
            option.setType("SOURCE_DISK");
            option.setId(firstString(volume, "id"));
            option.setExternalId(firstString(volume, "id"));
            option.setExternalRef(sourcePath);
            option.setValue(StringUtils.defaultIfBlank(sourcePath, option.getExternalId()));
            option.setReferenceType("MOLD_VOLUME_ID");
            option.setName(firstString(volume, "name", "id"));
            option.setDescription(sourcePath);
            option.setState(firstString(volume, "state"));
            option.setHypervisorType(firstString(volume, "hypervisor"));
            option.setSelectable(StringUtils.isNotBlank(sourcePath));
            putDetailIfNotBlank(option, "diskRef", firstString(volume, "id"));
            putDetailIfNotBlank(option, "path", sourcePath);
            putDetailIfNotBlank(option, "sourcePath", sourcePath);
            putDetailIfNotBlank(option, "volumePath", volumePath);
            putDetailIfNotBlank(option, "storageId", storageId);
            putDetailIfNotBlank(option, "storagePoolPath", poolPath);
            putDetailIfNotBlank(option, "storagePoolType", poolType);
            putDetailIfNotBlank(option, "sizeBytes", firstString(volume, "size", "virtualsize"));
            putDetailIfNotBlank(option, "capacityBytes", firstString(volume, "size", "virtualsize"));
            putDetailIfNotBlank(option, "physicalSizeBytes", firstString(volume, "physicalsize"));
            putDetailIfNotBlank(option, "deviceId", deviceId);
            putDetailIfNotBlank(option, "deviceKey", deviceId);
            putDetailIfNotBlank(option, "diskTarget", diskTarget(deviceId));
            putDetailIfNotBlank(option, "volumeType", firstString(volume, "type"));
            putDetailIfNotBlank(option, "diskOfferingId", firstString(volume, "diskofferingid"));
            putDetailIfNotBlank(option, "diskOfferingName", firstString(volume, "diskofferingname"));
            putDetailIfNotBlank(option, "cacheMode", firstString(volume, "cachemode"));
            putDetailIfNotBlank(option, "format", resolveVolumeFormat(volume, poolType, volumePath));
            options.add(option);
        }
        return options;
    }

    public List<DrInventoryOption> listVirtualMachineNics(DrResolvedSiteCredential credential, String vmRef) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("virtualmachineid", StringUtils.trim(vmRef));
        JsonObject response = execute(credential, COMMAND_LIST_NICS, params);
        JsonObject payload = getObjectIgnoreCase(response, "listnicsresponse");
        JsonArray nics = getArrayIgnoreCase(payload, "nic");
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        for (JsonElement item : nics) {
            if (item == null || !item.isJsonObject()) {
                continue;
            }
            JsonObject nic = item.getAsJsonObject();
            DrInventoryOption option = new DrInventoryOption();
            option.setType("SOURCE_NIC");
            option.setId(firstString(nic, "id"));
            option.setExternalId(firstString(nic, "id"));
            option.setExternalRef(firstString(nic, "networkid"));
            option.setValue(firstString(nic, "networkid", "id"));
            option.setReferenceType("MOLD_NETWORK_ID");
            option.setName(firstString(nic, "networkname", "id"));
            option.setDescription(firstString(nic, "ipaddress", "macaddress"));
            option.setSelectable(StringUtils.isNotBlank(option.getValue()));
            putDetailIfNotBlank(option, "networkId", firstString(nic, "networkid"));
            putDetailIfNotBlank(option, "networkName", firstString(nic, "networkname"));
            putDetailIfNotBlank(option, "deviceId", firstString(nic, "deviceid"));
            putDetailIfNotBlank(option, "macAddress", firstString(nic, "macaddress"));
            putDetailIfNotBlank(option, "ipAddress", firstString(nic, "ipaddress"));
            putDetailIfNotBlank(option, "trafficType", firstString(nic, "traffictype"));
            putDetailIfNotBlank(option, "networkType", firstString(nic, "type"));
            options.add(option);
        }
        return options;
    }

    public Map<String, String> getVirtualMachineHardware(DrResolvedSiteCredential credential, String vmRef) {
        JsonObject vm = getVirtualMachine(credential, vmRef, "all");
        return extractVirtualMachineHardware(vm);
    }

    Map<String, String> extractVirtualMachineHardware(JsonObject vm) {
        JsonObject details = getObjectIgnoreCase(vm, "details");
        Map<String, String> hardware = new LinkedHashMap<String, String>();
        for (Map.Entry<String, JsonElement> entry : details.entrySet()) {
            JsonElement value = entry.getValue();
            if (StringUtils.isNotBlank(entry.getKey()) && value != null && value.isJsonPrimitive()) {
                hardware.put(SOURCE_VM_DETAIL_PREFIX + entry.getKey(), value.getAsString());
            }
        }
        putIfNotBlank(hardware, "cpuCount", firstString(vm, "cpunumber"));
        putIfNotBlank(hardware, "cpuSpeed", firstString(vm, "cpuspeed"));
        putIfNotBlank(hardware, "memoryMiB", firstString(vm, "memory"));
        putIfNotBlank(hardware, "guestOsId", firstString(vm, "guestosid", "ostypeid"));
        putIfNotBlank(hardware, "guestOsName", firstString(vm, "osdisplayname"));
        putIfNotBlank(hardware, "serviceOfferingId", firstString(vm, "serviceofferingid"));
        putIfNotBlank(hardware, "serviceOfferingName", firstString(vm, "serviceofferingname"));
        String hypervisorType = firstString(vm, "hypervisor");
        String uefiBootMode = firstString(details, "UEFI", "uefi");
        String bootType = StringUtils.isNotBlank(uefiBootMode) ? "UEFI" : firstString(vm, "boottype");
        String bootMode = StringUtils.isNotBlank(uefiBootMode) ? uefiBootMode : firstString(vm, "bootmode");
        if (StringUtils.equalsIgnoreCase(hypervisorType, "KVM")) {
            bootType = StringUtils.isNotBlank(uefiBootMode) ? "UEFI" : "BIOS";
            bootMode = StringUtils.isNotBlank(uefiBootMode) ? uefiBootMode : "LEGACY";
        }
        String secureBoot = firstString(details, "secureboot", "secureBoot");
        putIfNotBlank(hardware, "firmware", bootType);
        putIfNotBlank(hardware, "bootType", bootType);
        putIfNotBlank(hardware, "bootMode", bootMode);
        putIfNotBlank(hardware, "secureBoot", StringUtils.defaultIfBlank(secureBoot,
                String.valueOf(StringUtils.equalsIgnoreCase(bootMode, "SECURE"))));
        putIfNotBlank(hardware, "UEFI", uefiBootMode);
        putIfNotBlank(hardware, "uefi", uefiBootMode);
        putIfNotBlank(hardware, "tpmVersion", firstString(details, "tpmversion", "tpmVersion"));
        putIfNotBlank(hardware, "sourceHostUuid", firstString(vm, "hostid"));
        putIfNotBlank(hardware, "sourceHostName", firstString(vm, "hostname"));
        putIfNotBlank(hardware, "instanceName", firstString(vm, "instancename"));
        putIfNotBlank(hardware, "hypervisorType", hypervisorType);
        return hardware;
    }

    String resolveVolumeFormat(JsonObject volume, String poolType, String volumePath) {
        String explicitFormat = firstString(volume, "format", "diskformat");
        if (StringUtils.isNotBlank(explicitFormat)) {
            return StringUtils.lowerCase(explicitFormat, Locale.ROOT);
        }
        if (StringUtils.equalsIgnoreCase(poolType, "RBD")) {
            return "raw";
        }
        if (StringUtils.equalsAnyIgnoreCase(poolType, "SharedMountPoint", "Filesystem", "NetworkFilesystem", "NFS")) {
            return "qcow2";
        }
        String normalizedPath = StringUtils.lowerCase(StringUtils.defaultString(volumePath), Locale.ROOT);
        if (StringUtils.endsWithAny(normalizedPath, ".qcow2", ".qcow")) {
            return "qcow2";
        }
        return "raw";
    }

    public String getVirtualMachinePowerState(DrResolvedSiteCredential credential, String vmRef) {
        return normalizePowerState(firstString(getVirtualMachine(credential, vmRef, "min"), "state"));
    }

    private JsonObject getVirtualMachine(DrResolvedSiteCredential credential, String vmRef, String details) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("id", vmRef);
        params.put("listall", "true");
        params.put("details", StringUtils.defaultIfBlank(details, "min"));
        JsonObject response = execute(credential, COMMAND_LIST_VMS, params);
        JsonObject payload = getObjectIgnoreCase(response, "listvirtualmachinesresponse");
        JsonArray items = getArrayIgnoreCase(payload, "virtualmachine");
        if (items.size() == 0 || !items.get(0).isJsonObject()) {
            throw new InventoryException(404, "Mold VM was not found: " + vmRef);
        }
        return items.get(0).getAsJsonObject();
    }

    public String ensureVirtualMachinePowerState(DrResolvedSiteCredential credential, String vmRef, boolean poweredOn) {
        String expected = poweredOn ? "POWERED_ON" : "POWERED_OFF";
        if (StringUtils.equals(expected, getVirtualMachinePowerState(credential, vmRef))) {
            return expected;
        }
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("id", vmRef);
        if (!poweredOn) {
            params.put("forced", "true");
        }
        JsonObject actionResponse = execute(credential, poweredOn ? COMMAND_START_VM : COMMAND_STOP_VM, params);
        waitForAsyncJob(credential, actionResponse, poweredOn ? COMMAND_START_VM : COMMAND_STOP_VM);
        for (int attempt = 0; attempt < 30; attempt++) {
            if (StringUtils.equals(expected, getVirtualMachinePowerState(credential, vmRef))) {
                return expected;
            }
            sleep(2000L);
        }
        throw new InventoryException(0, "Mold VM did not reach " + expected + ": " + vmRef);
    }

    private void waitForAsyncJob(DrResolvedSiteCredential credential, JsonObject actionResponse, String command) {
        JsonObject actionPayload = getObjectIgnoreCase(actionResponse, StringUtils.lowerCase(command) + "response");
        String jobId = firstString(actionPayload, "jobid");
        if (StringUtils.isBlank(jobId)) {
            return;
        }
        for (int attempt = 0; attempt < 30; attempt++) {
            Map<String, String> params = new LinkedHashMap<String, String>();
            params.put("jobid", jobId);
            JsonObject response = execute(credential, COMMAND_QUERY_ASYNC_JOB, params);
            JsonObject payload = getObjectIgnoreCase(response, "queryasyncjobresultresponse");
            int status = intValue(payload, "jobstatus");
            if (status == 1) {
                return;
            }
            if (status == 2) {
                int resultCode = intValue(payload, "jobresultcode");
                throw new InventoryException(resultCode,
                        StringUtils.defaultIfBlank(asyncJobError(payload), "Mold " + command + " job failed"));
            }
            sleep(2000L);
        }
        throw new InventoryException(0, "Mold " + command + " job did not complete: " + jobId);
    }

    String asyncJobError(JsonObject payload) {
        if (payload == null) {
            return null;
        }
        JsonElement result = getElementIgnoreCase(payload, "jobresult");
        if (result != null && result.isJsonObject()) {
            String message = firstString(result.getAsJsonObject(), "errortext", "errorText", "message", "displaytext");
            if (StringUtils.isNotBlank(message)) {
                return message;
            }
        }
        return firstString(payload, "errortext", "errorText", "message");
    }

    private int intValue(JsonObject object, String key) {
        JsonElement value = getElementIgnoreCase(object, key);
        try {
            return value != null && !value.isJsonNull() ? value.getAsInt() : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    public JsonObject executeSiteAgentCommand(DrResolvedSiteCredential credential, String commandType,
            String commandJson, String workerHostUuid) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("commandtype", StringUtils.trim(commandType));
        params.put("commandjson", commandJson);
        if (StringUtils.isNotBlank(workerHostUuid)) {
            params.put("workerhostuuid", StringUtils.trim(workerHostUuid));
        }
        JsonObject response = execute(credential, COMMAND_EXECUTE_DR_SITE_AGENT, params, true);
        return extractSiteAgentCommandResponse(response);
    }

    JsonObject extractSiteAgentCommandResponse(JsonObject response) {
        JsonObject payload = getObjectIgnoreCase(response, "executeftctldrsiteagentcommandresponse");
        JsonObject typedPayload = getObjectIgnoreCase(payload, "ftctldrsiteagentcommand");
        return typedPayload.entrySet().isEmpty() ? payload : typedPayload;
    }

    public JsonObject prepareRemoteSshAccess(DrResolvedSiteCredential sourceCredential,
            DrResolvedSiteCredential targetCredential, String sourceVmUuid, String targetHostUuid,
            String targetHostAddress, String targetDirectory, String remoteNbdAddress) {
        JsonObject targetSecret = targetCredential != null ? targetCredential.getSecretPayload() : null;
        DrSiteCredentialVO targetCredentialVo = targetCredential != null ? targetCredential.getCredential() : null;
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("virtualmachineid", StringUtils.trim(sourceVmUuid));
        params.put("remotepeerhostuuid", StringUtils.trim(targetHostUuid));
        params.put("remotepeerhostaddress", StringUtils.trim(targetHostAddress));
        params.put("remotepeersshuser", "root");
        params.put("remotepeersshport", "22");
        params.put("remotepeerlibvirturi", "qemu+ssh://root@" + StringUtils.trim(targetHostAddress) + "/system");
        params.put("secondarytargetdir", StringUtils.defaultIfBlank(targetDirectory, "/dev/rbd"));
        params.put("remotenbdexportaddr", StringUtils.defaultIfBlank(remoteNbdAddress, targetHostAddress));
        params.put("remotemoldapiurl", targetCredentialVo != null ? targetCredentialVo.getEndpoint() : null);
        params.put("remotemoldapikey", getSecret(targetSecret, "apiKey"));
        params.put("remotemoldsecretkey", getSecret(targetSecret, "secretKey"));
        JsonObject response = execute(sourceCredential, COMMAND_PREPARE_REMOTE_SSH, params, true);
        return getObjectIgnoreCase(response, "prepareftctldrremotesshaccessresponse");
    }

    private JsonObject execute(DrResolvedSiteCredential credential, String command, Map<String, String> additionalParams) {
        return execute(credential, command, additionalParams, false);
    }

    private JsonObject execute(DrResolvedSiteCredential credential, String command,
            Map<String, String> additionalParams, boolean post) {
        try {
            DrSiteCredentialVO credentialVo = credential.getCredential();
            String endpoint = StringUtils.trimToNull(credentialVo.getEndpoint());
            String apiEndpoint = normalizeMoldApiEndpoint(endpoint);
            JsonObject secret = credential.getSecretPayload();
            String apiKey = getSecret(secret, "apiKey");
            String secretKey = getSecret(secret, "secretKey");
            if (StringUtils.isAnyBlank(apiEndpoint, apiKey, secretKey)) {
                throw new InventoryException(0, "Mold API endpoint, API key, and secret key are required");
            }

            Map<String, String> params = new LinkedHashMap<String, String>();
            params.put("command", command);
            params.put("response", "json");
            params.put("apiKey", apiKey);
            if (additionalParams != null) {
                params.putAll(additionalParams);
            }
            String query = DrSiteProbeSupport.buildQuery(params);
            String signature = DrSiteProbeSupport.signCloudStackRequest(params, secretKey);
            String signedQuery = query + "&signature=" + signature;
            String requestUrl = post ? apiEndpoint : apiEndpoint + "?" + signedQuery;
            HttpURLConnection connection = DrSiteProbeSupport.openConnection(requestUrl, post ? "POST" : "GET",
                    credentialVo.getTlsVerify(), CONNECT_TIMEOUT_MS, post ? 45000 : READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            if (post) {
                byte[] requestBody = signedQuery.getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                connection.setFixedLengthStreamingMode(requestBody.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(requestBody);
                }
            }
            int responseCode = connection.getResponseCode();
            String body = DrSiteProbeSupport.readBody(connection);
            if (responseCode < 200 || responseCode >= 300) {
                throw new InventoryException(responseCode, "Mold API returned HTTP " + responseCode);
            }
            JsonElement parsed = JsonParser.parseString(StringUtils.defaultString(body, "{}"));
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (InventoryException e) {
            throw e;
        } catch (Exception e) {
            throw new InventoryException(0, "Mold API inventory request failed: " + e.getClass().getSimpleName());
        }
    }

    private String normalizeMoldApiEndpoint(String endpoint) throws Exception {
        String normalized = DrSiteProbeSupport.normalizeEndpoint(endpoint, "http");
        String lower = StringUtils.lowerCase(normalized);
        if (StringUtils.contains(lower, "/client/api")) {
            return normalized;
        }
        return DrSiteProbeSupport.appendPath(normalized, "/client/api");
    }

    private List<DrInventoryOption> toOptions(JsonArray items, String type) {
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        if (items == null) {
            return options;
        }
        for (JsonElement item : items) {
            if (item == null || !item.isJsonObject()) {
                continue;
            }
            JsonObject object = item.getAsJsonObject();
            DrInventoryOption option = new DrInventoryOption();
            option.setType(type);
            String externalId = firstString(object, "id", "uuid", "externalid");
            String localId = firstNumericString(object, "internalid", "dbid", "dbId", "databaseid");
            option.setId(StringUtils.defaultIfBlank(externalId, firstString(object, "name")));
            option.setName(firstString(object, "name", "displayname", "description", "id"));
            option.setDescription(firstString(object, "description", "displaytext", "vcenter", "path"));
            option.setValue(StringUtils.defaultIfBlank(externalId, localId));
            option.setExternalId(externalId);
            option.setLocalId(localId);
            option.setSelectable(StringUtils.isNotBlank(option.getValue()));
            if (StringUtils.isNotBlank(externalId)) {
                option.putDetail("externalId", externalId);
            }
            if (StringUtils.isNotBlank(localId)) {
                option.putDetail("localId", localId);
            }
            String vcenter = firstString(object, "vcenter", "vcentername", "host");
            if (StringUtils.isNotBlank(vcenter)) {
                option.putDetail("vcenter", vcenter);
            }
            options.add(option);
        }
        return options;
    }

    private List<DrInventoryOption> toVirtualMachineOptions(JsonArray items) {
        List<DrInventoryOption> options = new ArrayList<DrInventoryOption>();
        if (items == null) {
            return options;
        }
        for (JsonElement item : items) {
            if (item == null || !item.isJsonObject()) {
                continue;
            }
            JsonObject object = item.getAsJsonObject();
            String externalId = firstString(object, "id", "uuid");
            String instanceName = firstString(object, "instancename");
            String name = firstString(object, "displayname", "name", "id");
            DrInventoryOption option = new DrInventoryOption();
            option.setType("SOURCE_WORKLOAD");
            option.setId(StringUtils.defaultIfBlank(externalId, instanceName));
            option.setName(name);
            option.setDescription(firstString(object, "displaytext", "zonename", "host"));
            option.setValue(StringUtils.defaultIfBlank(externalId, instanceName));
            option.setExternalId(externalId);
            option.setExternalRef(StringUtils.defaultIfBlank(externalId, instanceName));
            option.setReferenceType("EXTERNAL_REF");
            option.setState(firstString(object, "state"));
            option.setHypervisorType(firstString(object, "hypervisor", "hypervisortype"));
            option.setSelectable(StringUtils.isNotBlank(option.getValue()));
            putDetailIfNotBlank(option, "externalId", externalId);
            putDetailIfNotBlank(option, "instanceName", instanceName);
            putDetailIfNotBlank(option, "name", firstString(object, "name"));
            putDetailIfNotBlank(option, "displayName", firstString(object, "displayname"));
            putDetailIfNotBlank(option, "zoneName", firstString(object, "zonename"));
            putDetailIfNotBlank(option, "hostName", firstString(object, "hostname", "host"));
            putDetailIfNotBlank(option, "hostUuid", firstString(object, "hostid"));
            putDetailIfNotBlank(option, "zoneId", firstString(object, "zoneid"));
            putDetailIfNotBlank(option, "cpuCount", firstString(object, "cpunumber"));
            putDetailIfNotBlank(option, "cpuSpeed", firstString(object, "cpuspeed"));
            putDetailIfNotBlank(option, "memoryMiB", firstString(object, "memory"));
            putDetailIfNotBlank(option, "guestOsId", firstString(object, "guestosid", "ostypeid"));
            putDetailIfNotBlank(option, "guestOsName", firstString(object, "osdisplayname"));
            putDetailIfNotBlank(option, "serviceOfferingId", firstString(object, "serviceofferingid"));
            putDetailIfNotBlank(option, "serviceOfferingName", firstString(object, "serviceofferingname"));
            Map<String, String> hardware = extractVirtualMachineHardware(object);
            putDetailIfNotBlank(option, "bootType", hardware.get("bootType"));
            putDetailIfNotBlank(option, "bootMode", hardware.get("bootMode"));
            putDetailIfNotBlank(option, "uefi", hardware.get("uefi"));
            putDetailIfNotBlank(option, "secureBoot", hardware.get("secureBoot"));
            putDetailIfNotBlank(option, "tpmVersion", hardware.get("tpmVersion"));
            putDetailIfNotBlank(option, "account", firstString(object, "account"));
            putDetailIfNotBlank(option, "domain", firstString(object, "domain"));
            options.add(option);
        }
        return options;
    }

    private JsonObject getStoragePool(DrResolvedSiteCredential credential, String storageId) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("id", storageId);
        JsonObject response = execute(credential, COMMAND_LIST_STORAGE_POOLS, params);
        JsonObject payload = getObjectIgnoreCase(response, "liststoragepoolsresponse");
        JsonArray pools = getArrayIgnoreCase(payload, "storagepool");
        return pools.size() > 0 && pools.get(0).isJsonObject() ? pools.get(0).getAsJsonObject() : new JsonObject();
    }

    private String canonicalStoragePath(String poolType, String poolPath, String volumePath) {
        if (StringUtils.isBlank(volumePath)) {
            return null;
        }
        if (StringUtils.containsIgnoreCase(poolType, "RBD")) {
            String pool = StringUtils.defaultIfBlank(StringUtils.trim(poolPath), "rbd");
            return "rbd:" + StringUtils.removeEnd(pool, "/") + "/" + StringUtils.removeStart(volumePath, "/");
        }
        if (StringUtils.startsWith(volumePath, "/")) {
            return volumePath;
        }
        return StringUtils.isNotBlank(poolPath)
                ? StringUtils.removeEnd(poolPath, "/") + "/" + volumePath : volumePath;
    }

    private String diskTarget(String deviceId) {
        if (StringUtils.isNumeric(deviceId)) {
            int index = Integer.parseInt(deviceId);
            if (index >= 0 && index < 26) {
                return "sd" + (char) ('a' + index);
            }
        }
        return null;
    }

    private void putIfNotBlank(Map<String, String> values, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            values.put(key, value);
        }
    }

    private void putDetailIfNotBlank(DrInventoryOption option, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            option.putDetail(key, value);
        }
    }

    private JsonObject getObjectIgnoreCase(JsonObject object, String key) {
        JsonElement element = getElementIgnoreCase(object, key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private JsonArray getArrayIgnoreCase(JsonObject object, String key) {
        JsonElement element = getElementIgnoreCase(object, key);
        if (element == null) {
            return new JsonArray();
        }
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        JsonArray array = new JsonArray();
        if (element.isJsonObject()) {
            array.add(element);
        }
        return array;
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
                String value = StringUtils.trimToNull(element.getAsString());
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private String firstNumericString(JsonObject object, String... keys) {
        for (String key : keys) {
            String value = firstString(object, key);
            if (StringUtils.isNotBlank(value) && StringUtils.isNumeric(value)) {
                return value;
            }
        }
        return null;
    }

    private String getSecret(JsonObject secret, String key) {
        JsonElement element = secret != null ? getElementIgnoreCase(secret, key) : null;
        return element != null && !element.isJsonNull() ? StringUtils.trimToNull(element.getAsString()) : null;
    }

    private String normalizePowerState(String state) {
        return StringUtils.equalsAnyIgnoreCase(state, "Running", "Starting", "Migrating")
                ? "POWERED_ON" : StringUtils.equalsAnyIgnoreCase(state, "Stopped", "Stopping", "Destroyed", "Expunging")
                ? "POWERED_OFF" : StringUtils.upperCase(StringUtils.defaultIfBlank(state, "UNKNOWN"), Locale.ROOT);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InventoryException(0, "Mold VM power wait was interrupted");
        }
    }

    public static class InventoryException extends RuntimeException {
        private final int responseCode;

        public InventoryException(int responseCode, String message) {
            super(message);
            this.responseCode = responseCode;
        }

        public int getResponseCode() {
            return responseCode;
        }
    }
}
