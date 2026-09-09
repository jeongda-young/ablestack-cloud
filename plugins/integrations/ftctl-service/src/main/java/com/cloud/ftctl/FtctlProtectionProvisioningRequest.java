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
package com.cloud.ftctl;

import com.cloud.vm.UserVmVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

public class FtctlProtectionProvisioningRequest {

    private final UserVmVO primaryVm;
    private final Long peerHostId;
    private final StoragePoolVO targetStoragePool;
    private final String mode;
    private final String backendMode;
    private final String provisioningBackend;
    private final String fencingPolicy;
    private final String secondaryVmName;
    private final String networkIds;

    public FtctlProtectionProvisioningRequest(UserVmVO primaryVm, Long peerHostId, StoragePoolVO targetStoragePool,
                                               String mode, String backendMode, String provisioningBackend,
                                               String fencingPolicy, String secondaryVmName) {
        this(primaryVm, peerHostId, targetStoragePool, mode, backendMode, provisioningBackend, fencingPolicy, secondaryVmName, null);
    }

    public FtctlProtectionProvisioningRequest(UserVmVO primaryVm, Long peerHostId, StoragePoolVO targetStoragePool,
                                               String mode, String backendMode, String provisioningBackend,
                                               String fencingPolicy, String secondaryVmName, String networkIds) {
        this.primaryVm = primaryVm;
        this.peerHostId = peerHostId;
        this.targetStoragePool = targetStoragePool;
        this.mode = mode;
        this.backendMode = backendMode;
        this.provisioningBackend = provisioningBackend;
        this.fencingPolicy = fencingPolicy;
        this.secondaryVmName = secondaryVmName;
        this.networkIds = StringUtils.trimToNull(networkIds);
    }

    public UserVmVO getPrimaryVm() {
        return primaryVm;
    }

    public Long getPeerHostId() {
        return peerHostId;
    }

    public StoragePoolVO getTargetStoragePool() {
        return targetStoragePool;
    }

    public String getMode() {
        return mode;
    }

    public String getBackendMode() {
        return backendMode;
    }

    public String getProvisioningBackend() {
        return provisioningBackend;
    }

    public String getFencingPolicy() {
        return fencingPolicy;
    }

    public String getSecondaryVmName() {
        return secondaryVmName;
    }

    public String getNetworkIds() {
        return networkIds;
    }
}
