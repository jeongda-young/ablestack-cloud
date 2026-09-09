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

import java.util.ArrayList;
import java.util.List;

public class DrResolvedTargetPlacement {
    private Long zoneId;
    private Long workerHostId;
    private String targetVmName;
    private String storageRef;
    private String storageLocalId;
    private String storagePath;
    private String storagePoolType;
    private String storageHostAddress;
    private String krbdPath;
    private String serviceOfferingId;
    private String serviceOfferingLocalId;
    private Integer targetCpuNumber;
    private Integer targetCpuSpeed;
    private Integer targetMemory;
    private DrResolvedTargetHardware targetHardware;
    private List<DrResolvedNetworkMapping> networks = new ArrayList<DrResolvedNetworkMapping>();
    private List<DrResolvedDiskMapping> disks = new ArrayList<DrResolvedDiskMapping>();
    private List<String> blockingReasons = new ArrayList<String>();
    private List<String> warnings = new ArrayList<String>();

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public Long getWorkerHostId() {
        return workerHostId;
    }

    public void setWorkerHostId(Long workerHostId) {
        this.workerHostId = workerHostId;
    }

    public String getTargetVmName() {
        return targetVmName;
    }

    public void setTargetVmName(String targetVmName) {
        this.targetVmName = targetVmName;
    }

    public String getStorageRef() {
        return storageRef;
    }

    public void setStorageRef(String storageRef) {
        this.storageRef = storageRef;
    }

    public String getStorageLocalId() {
        return storageLocalId;
    }

    public void setStorageLocalId(String storageLocalId) {
        this.storageLocalId = storageLocalId;
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

    public String getServiceOfferingId() {
        return serviceOfferingId;
    }

    public void setServiceOfferingId(String serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public String getServiceOfferingLocalId() {
        return serviceOfferingLocalId;
    }

    public void setServiceOfferingLocalId(String serviceOfferingLocalId) {
        this.serviceOfferingLocalId = serviceOfferingLocalId;
    }

    public Integer getTargetCpuNumber() {
        return targetCpuNumber;
    }

    public void setTargetCpuNumber(Integer targetCpuNumber) {
        this.targetCpuNumber = targetCpuNumber;
    }

    public Integer getTargetCpuSpeed() {
        return targetCpuSpeed;
    }

    public void setTargetCpuSpeed(Integer targetCpuSpeed) {
        this.targetCpuSpeed = targetCpuSpeed;
    }

    public Integer getTargetMemory() {
        return targetMemory;
    }

    public void setTargetMemory(Integer targetMemory) {
        this.targetMemory = targetMemory;
    }

    public DrResolvedTargetHardware getTargetHardware() {
        return targetHardware;
    }

    public void setTargetHardware(DrResolvedTargetHardware targetHardware) {
        this.targetHardware = targetHardware;
    }

    public List<DrResolvedNetworkMapping> getNetworks() {
        return networks;
    }

    public List<DrResolvedDiskMapping> getDisks() {
        return disks;
    }

    public List<String> getBlockingReasons() {
        return blockingReasons;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void addNetwork(DrResolvedNetworkMapping network) {
        if (network != null) {
            networks.add(network);
        }
    }

    public void addDisk(DrResolvedDiskMapping disk) {
        if (disk != null) {
            disks.add(disk);
        }
    }

    public void addBlockingReason(String reason) {
        if (reason != null && !blockingReasons.contains(reason)) {
            blockingReasons.add(reason);
        }
    }

    public void addWarning(String warning) {
        if (warning != null && !warnings.contains(warning)) {
            warnings.add(warning);
        }
    }
}
