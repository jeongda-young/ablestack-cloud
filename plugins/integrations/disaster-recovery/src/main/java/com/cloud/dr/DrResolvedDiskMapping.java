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
package com.cloud.dr;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;

public class DrResolvedDiskMapping {
    private String label;
    private String sourceRef;
    private String sourcePath;
    private String sourceLabel;
    private String device;
    private String sourceCbtDiskId;
    private String sourceDiskKey;
    private String sourceController;
    private String controllerBusNumber;
    private String unitNumber;
    private String capacityBytes;
    private Boolean boot;
    private String targetRef;
    private String targetName;
    private String targetStorageRef;
    private String targetStorageLocalId;
    private String targetDiskOfferingId;
    private String targetDiskOfferingLocalId;
    private String targetType;
    private String targetFormat;
    private String targetCacheMode;
    private String storagePath;
    private String storagePoolType;
    private String storageHostAddress;
    private String krbdPath;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel = sourceLabel;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getSourceCbtDiskId() {
        return sourceCbtDiskId;
    }

    public void setSourceCbtDiskId(String sourceCbtDiskId) {
        this.sourceCbtDiskId = sourceCbtDiskId;
    }

    public String getSourceDiskKey() {
        return sourceDiskKey;
    }

    public void setSourceDiskKey(String sourceDiskKey) {
        this.sourceDiskKey = sourceDiskKey;
    }

    public String getSourceController() {
        return sourceController;
    }

    public void setSourceController(String sourceController) {
        this.sourceController = sourceController;
    }

    public String getControllerBusNumber() {
        return controllerBusNumber;
    }

    public void setControllerBusNumber(String controllerBusNumber) {
        this.controllerBusNumber = controllerBusNumber;
    }

    public String getUnitNumber() {
        return unitNumber;
    }

    public void setUnitNumber(String unitNumber) {
        this.unitNumber = unitNumber;
    }

    public String getCapacityBytes() {
        return capacityBytes;
    }

    public void setCapacityBytes(String capacityBytes) {
        this.capacityBytes = capacityBytes;
    }

    public Boolean getBoot() {
        return boot;
    }

    public void setBoot(Boolean boot) {
        this.boot = boot;
    }

    public String getTargetRef() {
        return targetRef;
    }

    public void setTargetRef(String targetRef) {
        this.targetRef = targetRef;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetStorageRef() {
        return targetStorageRef;
    }

    public void setTargetStorageRef(String targetStorageRef) {
        this.targetStorageRef = targetStorageRef;
    }

    public String getTargetStorageLocalId() {
        return targetStorageLocalId;
    }

    public void setTargetStorageLocalId(String targetStorageLocalId) {
        this.targetStorageLocalId = targetStorageLocalId;
    }

    public String getTargetDiskOfferingId() {
        return targetDiskOfferingId;
    }

    public void setTargetDiskOfferingId(String targetDiskOfferingId) {
        this.targetDiskOfferingId = targetDiskOfferingId;
    }

    public String getTargetDiskOfferingLocalId() {
        return targetDiskOfferingLocalId;
    }

    public void setTargetDiskOfferingLocalId(String targetDiskOfferingLocalId) {
        this.targetDiskOfferingLocalId = targetDiskOfferingLocalId;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public String getTargetCacheMode() {
        return targetCacheMode;
    }

    public void setTargetCacheMode(String targetCacheMode) {
        this.targetCacheMode = targetCacheMode;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getStoragePoolType() {
        return storagePoolType;
    }

    public void setStoragePoolType(String storagePoolType) {
        this.storagePoolType = storagePoolType;
    }

    public String getStorageHostAddress() {
        return storageHostAddress;
    }

    public void setStorageHostAddress(String storageHostAddress) {
        this.storageHostAddress = storageHostAddress;
    }

    public String getKrbdPath() {
        return krbdPath;
    }

    public void setKrbdPath(String krbdPath) {
        this.krbdPath = krbdPath;
    }

    public JsonObject toJsonObject() {
        JsonObject disk = new JsonObject();
        addString(disk, "label", label);
        addString(disk, "device", StringUtils.defaultIfBlank(sourceCbtDiskId, device));
        addString(disk, "cbtDiskId", sourceCbtDiskId);
        addString(disk, "sourceDiskKey", sourceDiskKey);
        addString(disk, "sourceController", sourceController);
        addString(disk, "controllerBusNumber", controllerBusNumber);
        addString(disk, "unitNumber", unitNumber);
        addString(disk, "sourceRef", sourceRef);
        addString(disk, "sourcePath", sourcePath);
        addString(disk, "sizeBytes", capacityBytes);
        addString(disk, "capacityBytes", capacityBytes);
        addString(disk, "targetRef", targetRef);
        addString(disk, "targetStorageRef", targetStorageRef);
        addString(disk, "targetStorageLocalId", targetStorageLocalId);
        addString(disk, "targetDiskOfferingId", targetDiskOfferingId);
        addString(disk, "targetDiskOfferingLocalId", targetDiskOfferingLocalId);
        JsonObject source = new JsonObject();
        addString(source, "diskRef", sourceRef);
        addString(source, "device", StringUtils.defaultIfBlank(sourceCbtDiskId, device));
        addString(source, "cbtDiskId", sourceCbtDiskId);
        addString(source, "deviceKey", sourceDiskKey);
        addString(source, "controllerType", sourceController);
        addString(source, "controllerBusNumber", controllerBusNumber);
        addString(source, "unitNumber", unitNumber);
        addString(source, "path", sourcePath);
        addString(source, "vmdkPath", sourcePath);
        addString(source, "label", sourceLabel);
        addString(source, "capacityBytes", capacityBytes);
        if (boot != null) {
            source.addProperty("boot", boot);
        }
        if (!source.entrySet().isEmpty()) {
            disk.add("source", source);
        }
        JsonObject target = new JsonObject();
        addString(target, "name", targetName);
        addString(target, "diskRef", targetRef);
        addString(target, "storageRef", targetStorageRef);
        addString(target, "storagePoolId", targetStorageLocalId);
        addString(target, "diskOfferingId", targetDiskOfferingId);
        addString(target, "diskOfferingLocalId", targetDiskOfferingLocalId);
        addString(target, "type", targetType);
        addString(target, "targetType", targetType);
        addString(target, "format", targetFormat);
        addString(target, "cacheMode", targetCacheMode);
        addString(target, "capacityBytes", capacityBytes);
        addString(target, "sizeBytes", capacityBytes);
        addString(target, "storagePath", storagePath);
        addString(target, "storagePoolType", storagePoolType);
        addString(target, "storageHostAddress", storageHostAddress);
        addString(target, "krbdPath", krbdPath);
        if (!target.entrySet().isEmpty()) {
            disk.add("target", target);
        }
        return disk;
    }

    private void addString(JsonObject object, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            object.addProperty(key, StringUtils.trim(value));
        }
    }
}
