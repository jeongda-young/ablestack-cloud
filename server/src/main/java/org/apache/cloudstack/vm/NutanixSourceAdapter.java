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
package org.apache.cloudstack.vm;

import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NutanixSourceAdapter implements MigrationSourceAdapter {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final String API_AUTO = "auto";
    private static final String API_V4 = "v4";
    private static final String API_V3 = "v3";
    private static final String API_V2 = "v2";

    @Override
    public ImportVmTask.SourceProvider getSourceProvider() {
        return ImportVmTask.SourceProvider.Nutanix;
    }

    @Override
    public void validate(AblestackVmMigrationRequest request) {
        if (!ImportVmTask.SourceProvider.Nutanix.equals(request.getSourceProvider())) {
            throw new CloudRuntimeException(String.format("Nutanix source adapter cannot handle source provider %s",
                    request.getSourceProvider()));
        }
    }

    public NutanixVmInventory listVms(String endpoint, String username, String password, String sourceApi,
                                      boolean insecure) {
        String normalizedEndpoint = normalizeEndpoint(endpoint);
        List<String> errors = new ArrayList<>();
        for (String api : getApiOrder(sourceApi)) {
            try {
                JsonObject payload = fetchVmList(normalizedEndpoint, username, password, api, insecure);
                ReferenceNameMaps referenceNames = fetchReferenceNameMaps(normalizedEndpoint, username, password, api, insecure);
                List<UnmanagedInstanceTO> instances = parseVmList(api, payload, referenceNames);
                return new NutanixVmInventory(api, instances);
            } catch (RuntimeException e) {
                errors.add(String.format("%s: %s", api, e.getMessage()));
            }
        }
        throw new CloudRuntimeException("Unable to list Nutanix VMs using Prism APIs: " + String.join("; ", errors));
    }

    private JsonObject fetchVmList(String endpoint, String username, String password, String sourceApi,
                                   boolean insecure) {
        if (API_V4.equals(sourceApi)) {
            return executeJsonRequest("GET", endpoint, "/api/vmm/v4.0/ahv/config/vms?$limit=100",
                    username, password, insecure, null);
        }
        if (API_V3.equals(sourceApi)) {
            return executeJsonRequest("POST", endpoint, "/api/nutanix/v3/vms/list", username, password,
                    insecure, "{\"kind\":\"vm\",\"length\":100}");
        }
        if (API_V2.equals(sourceApi)) {
            return executeJsonRequest("GET", endpoint, "/PrismGateway/services/rest/v2.0/vms",
                    username, password, insecure, null);
        }
        throw new CloudRuntimeException("Unsupported Nutanix source API: " + sourceApi);
    }

    private ReferenceNameMaps fetchReferenceNameMaps(String endpoint, String username, String password, String sourceApi,
                                                     boolean insecure) {
        Map<String, String> clusterNames = new HashMap<>();
        Map<String, String> hostNames = new HashMap<>();
        addReferenceNames(clusterNames, fetchReferenceList(endpoint, username, password, sourceApi, insecure, true));
        addReferenceNames(hostNames, fetchReferenceList(endpoint, username, password, sourceApi, insecure, false));
        return new ReferenceNameMaps(clusterNames, hostNames);
    }

    private JsonObject fetchReferenceList(String endpoint, String username, String password, String sourceApi,
                                          boolean insecure, boolean cluster) {
        try {
            if (API_V4.equals(sourceApi)) {
                String path = cluster ? "/api/clustermgmt/v4.0/config/clusters?$limit=500" :
                        "/api/clustermgmt/v4.0/config/hosts?$limit=500";
                return executeJsonRequest("GET", endpoint, path, username, password, insecure, null);
            }
            if (API_V3.equals(sourceApi)) {
                String kind = cluster ? "cluster" : "host";
                return executeJsonRequest("POST", endpoint, String.format("/api/nutanix/v3/%ss/list", kind),
                        username, password, insecure, String.format("{\"kind\":\"%s\",\"length\":500}", kind));
            }
            if (API_V2.equals(sourceApi)) {
                String path = cluster ? "/PrismGateway/services/rest/v2.0/cluster" :
                        "/PrismGateway/services/rest/v2.0/hosts";
                return executeJsonRequest("GET", endpoint, path, username, password, insecure, null);
            }
        } catch (RuntimeException e) {
            // VM listing can still succeed without these optional display-name maps.
        }
        return null;
    }

    private JsonObject executeJsonRequest(String method, String endpoint, String path, String username,
                                          String password, boolean insecure, String body) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpoint + path);
            connection = (HttpURLConnection) url.openConnection();
            if (insecure && connection instanceof HttpsURLConnection) {
                configureInsecureTls((HttpsURLConnection) connection);
            }
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            if (StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password)) {
                String token = StringUtils.defaultString(username) + ":" + StringUtils.defaultString(password);
                connection.setRequestProperty("Authorization", "Basic " +
                        Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8)));
            }
            if (StringUtils.isNotBlank(body)) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int responseCode = connection.getResponseCode();
            String responseBody = readResponseBody(responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (responseCode < 200 || responseCode >= 300) {
                throw new CloudRuntimeException(String.format("HTTP %s returned %s", responseCode,
                        StringUtils.abbreviate(StringUtils.defaultString(responseBody), 512)));
            }
            JsonElement jsonElement = new JsonParser().parse(responseBody);
            if (!jsonElement.isJsonObject()) {
                throw new CloudRuntimeException("Response is not a JSON object");
            }
            return jsonElement.getAsJsonObject();
        } catch (IOException | GeneralSecurityException e) {
            throw new CloudRuntimeException("Nutanix Prism request failed: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private List<UnmanagedInstanceTO> parseVmList(String sourceApi, JsonObject payload, ReferenceNameMaps referenceNames) {
        JsonArray vmArray = extractVmArray(payload);
        List<UnmanagedInstanceTO> instances = new ArrayList<>();
        for (JsonElement vmElement : vmArray) {
            if (vmElement == null || !vmElement.isJsonObject()) {
                continue;
            }
            UnmanagedInstanceTO instance = parseVm(sourceApi, vmElement.getAsJsonObject(), instances.size());
            if (StringUtils.isNotBlank(instance.getName())) {
                instance.setClusterName(resolveDisplayName(instance.getClusterName(), referenceNames.clusterNames));
                instance.setHostName(resolveDisplayName(instance.getHostName(), referenceNames.hostNames));
                instances.add(instance);
            }
        }
        return instances;
    }

    private UnmanagedInstanceTO parseVm(String sourceApi, JsonObject vm, int position) {
        UnmanagedInstanceTO instance = new UnmanagedInstanceTO();
        instance.setName(firstString(vm, "name", "spec.name", "status.name", "vm_name"));
        instance.setInternalCSName(firstString(vm, "extId", "uuid", "metadata.uuid", "status.uuid", "vm_uuid"));
        instance.setHypervisorType("Nutanix");
        instance.setHostHypervisorVersion(sourceApi);
        instance.setPowerState(parsePowerState(firstString(vm, "powerState", "power_state",
                "status.resources.power_state", "resources.power_state")));
        instance.setClusterName(firstString(vm, "cluster.name", "clusterName", "cluster_name", "cluster.extId",
                "cluster.uuid", "cluster_uuid", "status.cluster_reference.name", "status.cluster_reference.uuid",
                "status.resources.cluster_reference.name", "status.resources.cluster_reference.uuid",
                "status.resources.cluster_uuid", "resources.cluster_reference.name",
                "resources.cluster_reference.uuid", "resources.cluster_uuid"));
        instance.setHostName(firstString(vm, "host.name", "hostName", "host_name", "host.extId", "host.uuid",
                "host_uuid", "status.host_reference.name", "status.host_reference.uuid",
                "status.resources.host_reference.name", "status.resources.host_reference.uuid",
                "status.resources.host_uuid", "resources.host_reference.name", "resources.host_reference.uuid",
                "resources.host_uuid"));
        Integer sockets = firstInteger(vm, "numSockets", "num_sockets", "status.resources.num_sockets",
                "resources.num_sockets");
        Integer coresPerSocket = firstInteger(vm, "numCoresPerSocket", "num_vcpus_per_socket",
                "status.resources.num_vcpus_per_socket", "resources.num_vcpus_per_socket");
        if (sockets != null && coresPerSocket != null) {
            instance.setCpuCores(sockets * coresPerSocket);
            instance.setCpuCoresPerSocket(coresPerSocket);
        } else {
            instance.setCpuCores(firstInteger(vm, "numVcpus", "num_vcpus", "status.resources.num_vcpus",
                    "resources.num_vcpus"));
        }
        instance.setMemory(firstMemoryMiB(vm));
        instance.setOperatingSystem(firstString(vm, "guestOsName", "guestOperatingSystem", "guest_os_name",
                "guest_os_type", "guest_os_id", "guestTools.guestOsVersion", "guestTools.guestOsType",
                "status.resources.guest_os_name", "status.resources.guest_os_id",
                "status.resources.guest_tools.guest_os_version",
                "status.resources.guest_tools.nutanix_guest_tools.guest_os_version",
                "resources.guest_os_name", "resources.guest_os_id", "resources.guest_tools.guest_os_version",
                "resources.guest_tools.nutanix_guest_tools.guest_os_version"));
        instance.setDisks(parseDisks(vm));
        instance.setNics(parseNics(vm));
        if (StringUtils.isBlank(instance.getName())) {
            instance.setName(String.format("nutanix-vm-%d", position));
        }
        return instance;
    }

    private List<UnmanagedInstanceTO.Disk> parseDisks(JsonObject vm) {
        JsonArray diskArray = firstArray(vm, "disks", "diskList", "disk_list", "vm_disk_info",
                "status.resources.disk_list", "resources.disk_list");
        if (diskArray == null) {
            return Collections.emptyList();
        }
        List<UnmanagedInstanceTO.Disk> disks = new ArrayList<>();
        for (JsonElement diskElement : diskArray) {
            if (diskElement == null || !diskElement.isJsonObject()) {
                continue;
            }
            JsonObject diskObject = diskElement.getAsJsonObject();
            UnmanagedInstanceTO.Disk disk = new UnmanagedInstanceTO.Disk();
            int index = disks.size();
            if (isCdromDisk(diskObject)) {
                continue;
            }
            Integer deviceIndex = firstInteger(diskObject, "deviceIndex", "device_index",
                    "disk_address.device_index", "device_properties.disk_address.device_index");
            int diskPosition = deviceIndex != null ? deviceIndex : index;
            disk.setDiskId(StringUtils.defaultIfBlank(firstString(diskObject, "uuid", "extId",
                    "vmdisk_uuid", "disk_address.vmdisk_uuid", "device_properties.disk_address.vmdisk_uuid"),
                    String.valueOf(diskPosition)));
            disk.setLabel(StringUtils.defaultIfBlank(firstString(diskObject, "label", "device_properties.disk_label"),
                    String.format("disk-%d", diskPosition)));
            disk.setCapacity(firstCapacityBytes(diskObject));
            disk.setController(firstString(diskObject, "adapterType", "adapter_type", "disk_address.adapter_type",
                    "device_properties.disk_address.adapter_type"));
            disk.setControllerUnit(deviceIndex);
            disk.setPosition(diskPosition);
            disk.setDatastoreName(firstString(diskObject, "storageContainerName", "storage_container_name",
                    "storage_container_uuid", "storage_config.storage_container_reference.name",
                    "storage_config.storage_container_reference.uuid"));
            disk.setDatastoreType("Nutanix");
            disks.add(disk);
        }
        return disks;
    }

    private List<UnmanagedInstanceTO.Nic> parseNics(JsonObject vm) {
        JsonArray nicArray = firstArray(vm, "nics", "nicList", "nic_list", "vm_nics",
                "status.resources.nic_list", "resources.nic_list");
        if (nicArray == null) {
            return Collections.emptyList();
        }
        List<UnmanagedInstanceTO.Nic> nics = new ArrayList<>();
        for (JsonElement nicElement : nicArray) {
            if (nicElement == null || !nicElement.isJsonObject()) {
                continue;
            }
            JsonObject nicObject = nicElement.getAsJsonObject();
            UnmanagedInstanceTO.Nic nic = new UnmanagedInstanceTO.Nic();
            int index = nics.size();
            nic.setNicId(StringUtils.defaultIfBlank(firstString(nicObject, "uuid", "extId"), String.valueOf(index)));
            nic.setMacAddress(firstString(nicObject, "macAddress", "mac_address"));
            nic.setNetwork(firstString(nicObject, "networkName", "network_name", "subnetName", "subnet_name"));
            nic.setAdapterType(firstString(nicObject, "adapterType", "adapter_type"));
            nic.setPciSlot(firstString(nicObject, "pciSlot", "pci_slot"));
            nics.add(nic);
        }
        return nics;
    }

    private JsonArray extractVmArray(JsonObject payload) {
        JsonArray vmArray = firstArray(payload, "data", "entities", "vms");
        if (vmArray == null) {
            throw new CloudRuntimeException("Nutanix VM list response does not contain a VM array");
        }
        return vmArray;
    }

    private UnmanagedInstanceTO.PowerState parsePowerState(String powerState) {
        String normalizedPowerState = StringUtils.lowerCase(StringUtils.defaultString(powerState));
        if (StringUtils.contains(normalizedPowerState, "on")) {
            return UnmanagedInstanceTO.PowerState.PowerOn;
        }
        if (StringUtils.contains(normalizedPowerState, "off")) {
            return UnmanagedInstanceTO.PowerState.PowerOff;
        }
        return UnmanagedInstanceTO.PowerState.PowerUnknown;
    }

    private Integer firstMemoryMiB(JsonObject object) {
        Long bytes = firstLong(object, "memorySizeBytes", "memory_size_bytes");
        if (bytes != null) {
            return (int) (bytes / 1024 / 1024);
        }
        return firstInteger(object, "memorySizeMiB", "memory_size_mib", "memory_mb",
                "status.resources.memory_size_mib", "resources.memory_size_mib", "resources.memory_mb");
    }

    private JsonArray firstArray(JsonObject object, String... paths) {
        for (String path : paths) {
            JsonElement element = getPath(object, path);
            if (element != null && element.isJsonArray()) {
                return element.getAsJsonArray();
            }
        }
        return null;
    }

    private String firstString(JsonObject object, String... paths) {
        for (String path : paths) {
            JsonElement element = getPath(object, path);
            if (element != null && element.isJsonPrimitive()) {
                String value = element.getAsString();
                if (StringUtils.isNotBlank(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private Integer firstInteger(JsonObject object, String... paths) {
        Long value = firstLong(object, paths);
        return value != null ? value.intValue() : null;
    }

    private Long firstLong(JsonObject object, String... paths) {
        for (String path : paths) {
            JsonElement element = getPath(object, path);
            if (element != null && element.isJsonPrimitive()) {
                try {
                    return element.getAsLong();
                } catch (NumberFormatException ignored) {
                    // Try the next compatible field.
                }
            }
        }
        return null;
    }

    private Long firstCapacityBytes(JsonObject diskObject) {
        Long bytes = firstLong(diskObject, "diskSizeBytes", "disk_size_bytes", "vm_disk_size_bytes",
                "vm_disk_size", "sizeBytes", "size_bytes", "size", "capacityBytes", "capacity_bytes", "capacity");
        if (bytes != null) {
            return bytes;
        }
        Long mib = firstLong(diskObject, "diskSizeMiB", "disk_size_mib", "disk_size_mb",
                "vm_disk_size_mib", "vm_disk_size_mb", "capacityMiB", "capacity_mib");
        return mib != null ? mib * 1024L * 1024L : null;
    }

    private boolean isCdromDisk(JsonObject diskObject) {
        Boolean isCdrom = firstBoolean(diskObject, "isCdrom", "is_cdrom", "device_properties.is_cdrom");
        if (Boolean.TRUE.equals(isCdrom)) {
            return true;
        }
        String deviceType = firstString(diskObject, "deviceType", "device_type", "device_properties.device_type");
        return StringUtils.containsIgnoreCase(deviceType, "cdrom");
    }

    private Boolean firstBoolean(JsonObject object, String... paths) {
        for (String path : paths) {
            JsonElement element = getPath(object, path);
            if (element != null && element.isJsonPrimitive()) {
                try {
                    return element.getAsBoolean();
                } catch (ClassCastException | IllegalStateException ignored) {
                    // Try the next compatible field.
                }
            }
        }
        return null;
    }

    private void addReferenceNames(Map<String, String> namesById, JsonObject payload) {
        if (payload == null) {
            return;
        }
        JsonArray references = firstArray(payload, "data", "entities", "hosts", "clusters");
        if (references == null && hasReferenceIdentity(payload)) {
            references = new JsonArray();
            references.add(payload);
        }
        if (references == null) {
            return;
        }
        for (JsonElement referenceElement : references) {
            if (referenceElement == null || !referenceElement.isJsonObject()) {
                continue;
            }
            JsonObject reference = referenceElement.getAsJsonObject();
            String id = firstString(reference, "extId", "uuid", "metadata.uuid", "status.uuid",
                    "status.resources.uuid", "cluster_uuid", "host_uuid");
            String name = firstDisplayName(reference);
            if (StringUtils.isNotBlank(id) && StringUtils.isNotBlank(name)) {
                namesById.put(id, name);
            }
        }
    }

    private boolean hasReferenceIdentity(JsonObject object) {
        return StringUtils.isNotBlank(firstString(object, "extId", "uuid", "metadata.uuid", "status.uuid",
                "cluster_uuid", "host_uuid"));
    }

    private String firstDisplayName(JsonObject object) {
        String name = firstString(object, "name", "spec.name", "status.name", "hostname", "hostName",
                "status.resources.name", "status.resources.host_name", "status.resources.hypervisor_server_name");
        return isUuidLike(name) ? null : name;
    }

    private String resolveDisplayName(String value, Map<String, String> namesById) {
        if (StringUtils.isBlank(value)) {
            return value;
        }
        String mappedName = namesById.get(value);
        if (StringUtils.isNotBlank(mappedName)) {
            return mappedName;
        }
        return isUuidLike(value) ? null : value;
    }

    private boolean isUuidLike(String value) {
        return StringUtils.isNotBlank(value) &&
                value.matches("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    }

    private JsonElement getPath(JsonObject object, String path) {
        JsonElement current = object;
        for (String part : StringUtils.split(path, '.')) {
            if (current == null || !current.isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject().get(part);
        }
        return current;
    }

    private List<String> getApiOrder(String sourceApi) {
        String normalizedSourceApi = StringUtils.lowerCase(StringUtils.defaultIfBlank(sourceApi, API_AUTO));
        if (API_AUTO.equals(normalizedSourceApi)) {
            return Arrays.asList(API_V4, API_V3, API_V2);
        }
        if (Arrays.asList(API_V4, API_V3, API_V2).contains(normalizedSourceApi)) {
            return Collections.singletonList(normalizedSourceApi);
        }
        throw new CloudRuntimeException(String.format("Unsupported Nutanix source API %s", sourceApi));
    }

    private String normalizeEndpoint(String endpoint) {
        if (StringUtils.isBlank(endpoint)) {
            throw new CloudRuntimeException("Nutanix Prism endpoint is required");
        }
        String normalizedEndpoint = StringUtils.trim(endpoint);
        if (!StringUtils.startsWithIgnoreCase(normalizedEndpoint, "http://") &&
                !StringUtils.startsWithIgnoreCase(normalizedEndpoint, "https://")) {
            normalizedEndpoint = "https://" + normalizedEndpoint;
        }
        try {
            URI uri = new URI(normalizedEndpoint);
            if (uri.getHost() != null && uri.getPort() < 0 && "https".equalsIgnoreCase(uri.getScheme())) {
                uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), 9440, uri.getPath(),
                        uri.getQuery(), uri.getFragment());
            }
            return StringUtils.removeEnd(uri.toString(), "/");
        } catch (URISyntaxException | IllegalArgumentException e) {
            throw new CloudRuntimeException("Invalid Nutanix Prism endpoint: " + endpoint, e);
        }
    }

    private void configureInsecureTls(HttpsURLConnection connection) throws GeneralSecurityException {
        TrustManager[] trustManagers = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new java.security.SecureRandom());
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        HostnameVerifier hostnameVerifier = (hostname, session) -> true;
        connection.setHostnameVerifier(hostnameVerifier);
    }

    private String readResponseBody(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    public static class NutanixVmInventory {
        private final String sourceApi;
        private final List<UnmanagedInstanceTO> instances;

        public NutanixVmInventory(String sourceApi, List<UnmanagedInstanceTO> instances) {
            this.sourceApi = sourceApi;
            this.instances = CollectionUtils.isNotEmpty(instances) ? instances : Collections.emptyList();
        }

        public String getSourceApi() {
            return sourceApi;
        }

        public List<UnmanagedInstanceTO> getInstances() {
            return instances;
        }
    }

    private static class ReferenceNameMaps {
        private final Map<String, String> clusterNames;
        private final Map<String, String> hostNames;

        private ReferenceNameMaps(Map<String, String> clusterNames, Map<String, String> hostNames) {
            this.clusterNames = clusterNames != null ? clusterNames : Collections.emptyMap();
            this.hostNames = hostNames != null ? hostNames : Collections.emptyMap();
        }
    }
}
