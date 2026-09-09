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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;

public class DrSourceVmHardware {
    public static final String FINGERPRINT_CONTRACT_VERSION = "2";

    private String sourceVmRef;
    private String sourceHostUuid;
    private String sourceHostName;
    private String instanceName;
    private String firmware;
    private String uefiMode;
    private Boolean secureBootEnabled;
    private String guestId;
    private Integer cpuCount;
    private Long memoryMiB;
    private String rootDiskController;
    private String dataDiskController;
    private Map<String, String> vmDetails;
    private Date observedAt;
    private String inventorySource;
    private String fingerprint;
    private String operationBlockerCode;
    private String operationBlockerMessage;
    private String errorCode;
    private String message;

    public static DrSourceVmHardware unavailable(String sourceVmRef, String errorCode, String message) {
        DrSourceVmHardware hardware = new DrSourceVmHardware();
        hardware.sourceVmRef = sourceVmRef;
        hardware.errorCode = errorCode;
        hardware.message = message;
        hardware.observedAt = new Date();
        return hardware;
    }

    public boolean isComplete() {
        return StringUtils.isNotBlank(firmware) && secureBootEnabled != null;
    }

    public void seal() {
        observedAt = observedAt != null ? observedAt : new Date();
        fingerprint = stableFingerprint(canonicalJson());
    }

    public JsonObject toJsonObject() {
        JsonObject object = canonicalJson();
        if (observedAt != null) {
            object.addProperty("observedAtEpochMs", observedAt.getTime());
        }
        if (StringUtils.isNotBlank(inventorySource)) {
            object.addProperty("inventorySource", inventorySource);
        }
        if (StringUtils.isNotBlank(fingerprint)) {
            object.addProperty("fingerprint", fingerprint);
            object.addProperty("fingerprintVersion", FINGERPRINT_CONTRACT_VERSION);
        }
        if (StringUtils.isNotBlank(operationBlockerCode)) {
            object.addProperty("operationBlockerCode", operationBlockerCode);
        }
        if (StringUtils.isNotBlank(operationBlockerMessage)) {
            object.addProperty("operationBlockerMessage", operationBlockerMessage);
        }
        if (StringUtils.isNotBlank(errorCode)) {
            object.addProperty("errorCode", errorCode);
        }
        if (StringUtils.isNotBlank(message)) {
            object.addProperty("message", message);
        }
        return object;
    }

    public Map<String, String> toDetails() {
        Map<String, String> details = new LinkedHashMap<String, String>();
        JsonObject json = toJsonObject();
        for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
            if (entry.getValue() != null && entry.getValue().isJsonPrimitive()) {
                details.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return details;
    }

    private JsonObject canonicalJson() {
        JsonObject object = new JsonObject();
        add(object, "sourceVmRef", sourceVmRef);
        add(object, "sourceHostUuid", sourceHostUuid);
        add(object, "sourceHostName", sourceHostName);
        add(object, "instanceName", instanceName);
        add(object, "firmware", firmware);
        add(object, "UEFI", uefiMode);
        if (secureBootEnabled != null) {
            object.addProperty("secureBoot", secureBootEnabled);
        }
        add(object, "guestId", guestId);
        if (cpuCount != null) {
            object.addProperty("cpuCount", cpuCount);
        }
        if (memoryMiB != null) {
            object.addProperty("memoryMiB", memoryMiB);
        }
        add(object, "rootDiskController", rootDiskController);
        add(object, "dataDiskController", dataDiskController);
        if (vmDetails != null) {
            JsonObject details = new JsonObject();
            for (Map.Entry<String, String> entry : new TreeMap<String, String>(vmDetails).entrySet()) {
                if (StringUtils.isNotBlank(entry.getKey()) && entry.getValue() != null) {
                    details.addProperty(entry.getKey(), entry.getValue());
                }
            }
            object.add("vmDetails", details);
        }
        return object;
    }

    public static String stableFingerprint(JsonObject hardware) {
        return "sha256:" + sha256(fingerprintJson(hardware).toString());
    }

    private static JsonObject fingerprintJson(JsonObject hardware) {
        JsonObject object = new JsonObject();
        copy(hardware, object, "sourceVmRef");
        copy(hardware, object, "firmware");
        copy(hardware, object, "UEFI");
        copy(hardware, object, "secureBoot");
        copy(hardware, object, "guestId");
        copy(hardware, object, "cpuCount");
        copy(hardware, object, "memoryMiB");
        copy(hardware, object, "rootDiskController");
        copy(hardware, object, "dataDiskController");
        if (hardware != null && hardware.has("vmDetails") && hardware.get("vmDetails").isJsonObject()) {
            JsonObject details = new JsonObject();
            TreeMap<String, com.google.gson.JsonElement> sorted = new TreeMap<String, com.google.gson.JsonElement>();
            for (Map.Entry<String, com.google.gson.JsonElement> entry
                    : hardware.getAsJsonObject("vmDetails").entrySet()) {
                sorted.put(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, com.google.gson.JsonElement> entry : sorted.entrySet()) {
                if (isStableFingerprintDetail(entry.getKey()) && entry.getValue() != null) {
                    details.add(entry.getKey(), entry.getValue().deepCopy());
                }
            }
            object.add("vmDetails", details);
        }
        return object;
    }

    private static void copy(JsonObject source, JsonObject target, String key) {
        if (source != null && source.has(key) && !source.get(key).isJsonNull()) {
            target.add(key, source.get(key).deepCopy());
        }
    }

    private static boolean isStableFingerprintDetail(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        String normalized = StringUtils.lowerCase(key);
        return !StringUtils.startsWithAny(normalized,
                "message.", "clone.", "ftctl.", "dr.", "ha.", "host.", "runtime.", "last.");
    }

    private static void add(JsonObject object, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            object.addProperty(key, value);
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                int valueByte = item & 0xff;
                hex.append(Character.forDigit(valueByte >>> 4, 16));
                hex.append(Character.forDigit(valueByte & 0x0f, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public String getSourceVmRef() { return sourceVmRef; }
    public void setSourceVmRef(String sourceVmRef) { this.sourceVmRef = sourceVmRef; }
    public void setSourceHostUuid(String sourceHostUuid) { this.sourceHostUuid = sourceHostUuid; }
    public void setSourceHostName(String sourceHostName) { this.sourceHostName = sourceHostName; }
    public void setInstanceName(String instanceName) { this.instanceName = instanceName; }
    public String getFirmware() { return firmware; }
    public void setFirmware(String firmware) { this.firmware = firmware; }
    public String getUefiMode() { return uefiMode; }
    public void setUefiMode(String uefiMode) { this.uefiMode = uefiMode; }
    public Boolean getSecureBootEnabled() { return secureBootEnabled; }
    public void setSecureBootEnabled(Boolean secureBootEnabled) { this.secureBootEnabled = secureBootEnabled; }
    public String getGuestId() { return guestId; }
    public void setGuestId(String guestId) { this.guestId = guestId; }
    public Integer getCpuCount() { return cpuCount; }
    public void setCpuCount(Integer cpuCount) { this.cpuCount = cpuCount; }
    public Long getMemoryMiB() { return memoryMiB; }
    public void setMemoryMiB(Long memoryMiB) { this.memoryMiB = memoryMiB; }
    public String getRootDiskController() { return rootDiskController; }
    public void setRootDiskController(String rootDiskController) { this.rootDiskController = rootDiskController; }
    public String getDataDiskController() { return dataDiskController; }
    public void setDataDiskController(String dataDiskController) { this.dataDiskController = dataDiskController; }
    public Map<String, String> getVmDetails() { return vmDetails == null ? new LinkedHashMap<String, String>()
            : new LinkedHashMap<String, String>(vmDetails); }
    public void setVmDetails(Map<String, String> vmDetails) {
        this.vmDetails = vmDetails == null ? new LinkedHashMap<String, String>()
                : new LinkedHashMap<String, String>(vmDetails);
    }
    public Date getObservedAt() { return observedAt; }
    public void setObservedAt(Date observedAt) { this.observedAt = observedAt; }
    public String getInventorySource() { return inventorySource; }
    public void setInventorySource(String inventorySource) { this.inventorySource = inventorySource; }
    public String getFingerprint() { return fingerprint; }
    public String getOperationBlockerCode() { return operationBlockerCode; }
    public void setOperationBlockerCode(String operationBlockerCode) { this.operationBlockerCode = operationBlockerCode; }
    public String getOperationBlockerMessage() { return operationBlockerMessage; }
    public void setOperationBlockerMessage(String operationBlockerMessage) { this.operationBlockerMessage = operationBlockerMessage; }
    public boolean hasOperationBlocker() { return StringUtils.isNotBlank(operationBlockerCode); }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
}
