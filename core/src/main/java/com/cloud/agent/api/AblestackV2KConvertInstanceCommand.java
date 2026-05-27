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

import com.cloud.agent.api.to.DataStoreTO;

public class AblestackV2KConvertInstanceCommand extends Command {

    private String vmName;
    private String vcenter;
    private String username;
    private String password;
    private DataStoreTO targetStorageLocation;
    private String splitMode;
    private String workdir;
    private String targetFormat;
    private String targetStorage;
    private String targetMapJson;
    private String targetDestinationPath;
    private String targetProfile;
    private String targetProvider;
    private String cloudEndpoint;
    private String cloudApiKey;
    private String cloudSecretKey;
    private String cloudZoneId;
    private String cloudServiceOfferingId;
    private String cloudNetworkIds;
    private String cloudStorageId;
    private String cloudDiskOfferingId;
    private String cloudHostId;
    private String cloudAccount;
    private String cloudDomainId;
    private String cloudProjectId;
    private String cloudName;
    private String cloudDisplayName;
    private String cloudCpuSpeed;
    private boolean resume;

    public AblestackV2KConvertInstanceCommand() {
    }

    public AblestackV2KConvertInstanceCommand(String vmName, String vcenter, String username, String password,
                                              DataStoreTO targetStorageLocation, String splitMode) {
        this.vmName = vmName;
        this.vcenter = vcenter;
        this.username = username;
        this.password = password;
        this.targetStorageLocation = targetStorageLocation;
        this.splitMode = splitMode;
    }

    public String getVmName() {
        return vmName;
    }

    public String getVcenter() {
        return vcenter;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public DataStoreTO getTargetStorageLocation() {
        return targetStorageLocation;
    }

    public String getSplitMode() {
        return splitMode;
    }

    public String getWorkdir() {
        return workdir;
    }

    public void setWorkdir(String workdir) {
        this.workdir = workdir;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public String getTargetStorage() {
        return targetStorage;
    }

    public void setTargetStorage(String targetStorage) {
        this.targetStorage = targetStorage;
    }

    public String getTargetMapJson() {
        return targetMapJson;
    }

    public void setTargetMapJson(String targetMapJson) {
        this.targetMapJson = targetMapJson;
    }

    public String getTargetDestinationPath() {
        return targetDestinationPath;
    }

    public void setTargetDestinationPath(String targetDestinationPath) {
        this.targetDestinationPath = targetDestinationPath;
    }

    public String getTargetProfile() {
        return targetProfile;
    }

    public void setTargetProfile(String targetProfile) {
        this.targetProfile = targetProfile;
    }

    public String getTargetProvider() {
        return targetProvider;
    }

    public void setTargetProvider(String targetProvider) {
        this.targetProvider = targetProvider;
    }

    public String getCloudEndpoint() {
        return cloudEndpoint;
    }

    public void setCloudEndpoint(String cloudEndpoint) {
        this.cloudEndpoint = cloudEndpoint;
    }

    public String getCloudApiKey() {
        return cloudApiKey;
    }

    public void setCloudApiKey(String cloudApiKey) {
        this.cloudApiKey = cloudApiKey;
    }

    public String getCloudSecretKey() {
        return cloudSecretKey;
    }

    public void setCloudSecretKey(String cloudSecretKey) {
        this.cloudSecretKey = cloudSecretKey;
    }

    public String getCloudZoneId() {
        return cloudZoneId;
    }

    public void setCloudZoneId(String cloudZoneId) {
        this.cloudZoneId = cloudZoneId;
    }

    public String getCloudServiceOfferingId() {
        return cloudServiceOfferingId;
    }

    public void setCloudServiceOfferingId(String cloudServiceOfferingId) {
        this.cloudServiceOfferingId = cloudServiceOfferingId;
    }

    public String getCloudNetworkIds() {
        return cloudNetworkIds;
    }

    public void setCloudNetworkIds(String cloudNetworkIds) {
        this.cloudNetworkIds = cloudNetworkIds;
    }

    public String getCloudStorageId() {
        return cloudStorageId;
    }

    public void setCloudStorageId(String cloudStorageId) {
        this.cloudStorageId = cloudStorageId;
    }

    public String getCloudDiskOfferingId() {
        return cloudDiskOfferingId;
    }

    public void setCloudDiskOfferingId(String cloudDiskOfferingId) {
        this.cloudDiskOfferingId = cloudDiskOfferingId;
    }

    public String getCloudHostId() {
        return cloudHostId;
    }

    public void setCloudHostId(String cloudHostId) {
        this.cloudHostId = cloudHostId;
    }

    public String getCloudAccount() {
        return cloudAccount;
    }

    public void setCloudAccount(String cloudAccount) {
        this.cloudAccount = cloudAccount;
    }

    public String getCloudDomainId() {
        return cloudDomainId;
    }

    public void setCloudDomainId(String cloudDomainId) {
        this.cloudDomainId = cloudDomainId;
    }

    public String getCloudProjectId() {
        return cloudProjectId;
    }

    public void setCloudProjectId(String cloudProjectId) {
        this.cloudProjectId = cloudProjectId;
    }

    public String getCloudName() {
        return cloudName;
    }

    public void setCloudName(String cloudName) {
        this.cloudName = cloudName;
    }

    public String getCloudDisplayName() {
        return cloudDisplayName;
    }

    public void setCloudDisplayName(String cloudDisplayName) {
        this.cloudDisplayName = cloudDisplayName;
    }

    public String getCloudCpuSpeed() {
        return cloudCpuSpeed;
    }

    public void setCloudCpuSpeed(String cloudCpuSpeed) {
        this.cloudCpuSpeed = cloudCpuSpeed;
    }

    public boolean isResume() {
        return resume;
    }

    public void setResume(boolean resume) {
        this.resume = resume;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
