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

public class FtctlSyncProfileCommand extends Command {

    private String vmName;
    private String mode;
    private String peerUri;
    private String profileName;
    private String backendMode;
    private String provisioningBackend;
    private String provisioningState;
    private String targetStorageScope;
    private String targetStoragePoolId;
    private String targetStoragePoolName;
    private String targetStoragePoolPath;
    private String targetStoragePoolType;
    private String diskMap;
    private String secondaryVmName;
    private String fencingPolicy;
    private String secondaryTargetDir;
    private String remoteNbdExportAddr;
    private String xcoloProxyEndpoint;
    private String xcoloNbdEndpoint;
    private String xcoloMigrateUri;
    private String xcoloMirrorPort;
    private String xcoloComparePort;
    private String xcoloCompareLocalPort;
    private String xcoloCompareOutPort;
    private String xcoloControlPort;
    private String fencingIpmiPrimaryHost;
    private String fencingIpmiPrimaryPort;
    private String fencingIpmiPrimaryUser;
    private String fencingIpmiPrimaryPassword;
    private String fencingIpmiPrimaryInterface;
    private String fencingIpmiSecondaryHost;
    private String fencingIpmiSecondaryPort;
    private String fencingIpmiSecondaryUser;
    private String fencingIpmiSecondaryPassword;
    private String fencingIpmiSecondaryInterface;
    private String secondarySshKeyFile;

    public FtctlSyncProfileCommand() {
    }

    public FtctlSyncProfileCommand(String vmName, String mode, String peerUri) {
        this.vmName = vmName;
        this.mode = mode;
        this.peerUri = peerUri;
    }

    public String getVmName() {
        return vmName;
    }

    public String getMode() {
        return mode;
    }

    public String getPeerUri() {
        return peerUri;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getBackendMode() {
        return backendMode;
    }

    public void setBackendMode(String backendMode) {
        this.backendMode = backendMode;
    }

    public String getProvisioningBackend() {
        return provisioningBackend;
    }

    public void setProvisioningBackend(String provisioningBackend) {
        this.provisioningBackend = provisioningBackend;
    }

    public String getProvisioningState() {
        return provisioningState;
    }

    public void setProvisioningState(String provisioningState) {
        this.provisioningState = provisioningState;
    }

    public String getTargetStorageScope() {
        return targetStorageScope;
    }

    public void setTargetStorageScope(String targetStorageScope) {
        this.targetStorageScope = targetStorageScope;
    }

    public String getTargetStoragePoolId() {
        return targetStoragePoolId;
    }

    public void setTargetStoragePoolId(String targetStoragePoolId) {
        this.targetStoragePoolId = targetStoragePoolId;
    }

    public String getTargetStoragePoolName() {
        return targetStoragePoolName;
    }

    public void setTargetStoragePoolName(String targetStoragePoolName) {
        this.targetStoragePoolName = targetStoragePoolName;
    }

    public String getTargetStoragePoolPath() {
        return targetStoragePoolPath;
    }

    public void setTargetStoragePoolPath(String targetStoragePoolPath) {
        this.targetStoragePoolPath = targetStoragePoolPath;
    }

    public String getTargetStoragePoolType() {
        return targetStoragePoolType;
    }

    public void setTargetStoragePoolType(String targetStoragePoolType) {
        this.targetStoragePoolType = targetStoragePoolType;
    }

    public String getDiskMap() {
        return diskMap;
    }

    public void setDiskMap(String diskMap) {
        this.diskMap = diskMap;
    }

    public String getSecondaryVmName() {
        return secondaryVmName;
    }

    public void setSecondaryVmName(String secondaryVmName) {
        this.secondaryVmName = secondaryVmName;
    }

    public String getFencingPolicy() {
        return fencingPolicy;
    }

    public void setFencingPolicy(String fencingPolicy) {
        this.fencingPolicy = fencingPolicy;
    }

    public String getSecondaryTargetDir() {
        return secondaryTargetDir;
    }

    public void setSecondaryTargetDir(String secondaryTargetDir) {
        this.secondaryTargetDir = secondaryTargetDir;
    }

    public String getRemoteNbdExportAddr() {
        return remoteNbdExportAddr;
    }

    public void setRemoteNbdExportAddr(String remoteNbdExportAddr) {
        this.remoteNbdExportAddr = remoteNbdExportAddr;
    }

    public String getXcoloProxyEndpoint() {
        return xcoloProxyEndpoint;
    }

    public void setXcoloProxyEndpoint(String xcoloProxyEndpoint) {
        this.xcoloProxyEndpoint = xcoloProxyEndpoint;
    }

    public String getXcoloNbdEndpoint() {
        return xcoloNbdEndpoint;
    }

    public void setXcoloNbdEndpoint(String xcoloNbdEndpoint) {
        this.xcoloNbdEndpoint = xcoloNbdEndpoint;
    }

    public String getXcoloMigrateUri() {
        return xcoloMigrateUri;
    }

    public void setXcoloMigrateUri(String xcoloMigrateUri) {
        this.xcoloMigrateUri = xcoloMigrateUri;
    }

    public String getXcoloMirrorPort() {
        return xcoloMirrorPort;
    }

    public void setXcoloMirrorPort(String xcoloMirrorPort) {
        this.xcoloMirrorPort = xcoloMirrorPort;
    }

    public String getXcoloComparePort() {
        return xcoloComparePort;
    }

    public void setXcoloComparePort(String xcoloComparePort) {
        this.xcoloComparePort = xcoloComparePort;
    }

    public String getXcoloCompareLocalPort() {
        return xcoloCompareLocalPort;
    }

    public void setXcoloCompareLocalPort(String xcoloCompareLocalPort) {
        this.xcoloCompareLocalPort = xcoloCompareLocalPort;
    }

    public String getXcoloCompareOutPort() {
        return xcoloCompareOutPort;
    }

    public void setXcoloCompareOutPort(String xcoloCompareOutPort) {
        this.xcoloCompareOutPort = xcoloCompareOutPort;
    }

    public String getXcoloControlPort() {
        return xcoloControlPort;
    }

    public void setXcoloControlPort(String xcoloControlPort) {
        this.xcoloControlPort = xcoloControlPort;
    }

    public String getFencingIpmiPrimaryHost() {
        return fencingIpmiPrimaryHost;
    }

    public void setFencingIpmiPrimaryHost(String fencingIpmiPrimaryHost) {
        this.fencingIpmiPrimaryHost = fencingIpmiPrimaryHost;
    }

    public String getFencingIpmiPrimaryPort() {
        return fencingIpmiPrimaryPort;
    }

    public void setFencingIpmiPrimaryPort(String fencingIpmiPrimaryPort) {
        this.fencingIpmiPrimaryPort = fencingIpmiPrimaryPort;
    }

    public String getFencingIpmiPrimaryUser() {
        return fencingIpmiPrimaryUser;
    }

    public void setFencingIpmiPrimaryUser(String fencingIpmiPrimaryUser) {
        this.fencingIpmiPrimaryUser = fencingIpmiPrimaryUser;
    }

    public String getFencingIpmiPrimaryPassword() {
        return fencingIpmiPrimaryPassword;
    }

    public void setFencingIpmiPrimaryPassword(String fencingIpmiPrimaryPassword) {
        this.fencingIpmiPrimaryPassword = fencingIpmiPrimaryPassword;
    }

    public String getFencingIpmiPrimaryInterface() {
        return fencingIpmiPrimaryInterface;
    }

    public void setFencingIpmiPrimaryInterface(String fencingIpmiPrimaryInterface) {
        this.fencingIpmiPrimaryInterface = fencingIpmiPrimaryInterface;
    }

    public String getFencingIpmiSecondaryHost() {
        return fencingIpmiSecondaryHost;
    }

    public void setFencingIpmiSecondaryHost(String fencingIpmiSecondaryHost) {
        this.fencingIpmiSecondaryHost = fencingIpmiSecondaryHost;
    }

    public String getFencingIpmiSecondaryPort() {
        return fencingIpmiSecondaryPort;
    }

    public void setFencingIpmiSecondaryPort(String fencingIpmiSecondaryPort) {
        this.fencingIpmiSecondaryPort = fencingIpmiSecondaryPort;
    }

    public String getFencingIpmiSecondaryUser() {
        return fencingIpmiSecondaryUser;
    }

    public void setFencingIpmiSecondaryUser(String fencingIpmiSecondaryUser) {
        this.fencingIpmiSecondaryUser = fencingIpmiSecondaryUser;
    }

    public String getFencingIpmiSecondaryPassword() {
        return fencingIpmiSecondaryPassword;
    }

    public void setFencingIpmiSecondaryPassword(String fencingIpmiSecondaryPassword) {
        this.fencingIpmiSecondaryPassword = fencingIpmiSecondaryPassword;
    }

    public String getFencingIpmiSecondaryInterface() {
        return fencingIpmiSecondaryInterface;
    }

    public void setFencingIpmiSecondaryInterface(String fencingIpmiSecondaryInterface) {
        this.fencingIpmiSecondaryInterface = fencingIpmiSecondaryInterface;
    }

    public String getSecondarySshKeyFile() {
        return secondarySshKeyFile;
    }

    public void setSecondarySshKeyFile(String secondarySshKeyFile) {
        this.secondarySshKeyFile = secondarySshKeyFile;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
