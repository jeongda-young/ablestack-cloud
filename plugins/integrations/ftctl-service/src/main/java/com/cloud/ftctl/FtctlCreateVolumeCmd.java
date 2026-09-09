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

import org.apache.cloudstack.api.command.admin.volume.CreateVolumeCmdByAdmin;

public class FtctlCreateVolumeCmd extends CreateVolumeCmdByAdmin {

    private final long entityOwnerId;
    private final String accountName;
    private final Long domainId;
    private final Long diskOfferingId;
    private final String volumeName;
    private final Long size;
    private final Long minIops;
    private final Long maxIops;
    private final Long zoneId;
    private final Long storageId;
    private final Boolean displayVolume;

    public FtctlCreateVolumeCmd(long entityOwnerId, String accountName, Long domainId, Long diskOfferingId,
                                String volumeName, Long size, Long minIops, Long maxIops, Long zoneId, Long storageId, Boolean displayVolume) {
        this.entityOwnerId = entityOwnerId;
        this.accountName = accountName;
        this.domainId = domainId;
        this.diskOfferingId = diskOfferingId;
        this.volumeName = volumeName;
        this.size = size;
        this.minIops = minIops;
        this.maxIops = maxIops;
        this.zoneId = zoneId;
        this.storageId = storageId;
        this.displayVolume = displayVolume;
    }

    @Override
    public long getEntityOwnerId() {
        return entityOwnerId;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public Long getDomainId() {
        return domainId;
    }

    @Override
    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    @Override
    public String getVolumeName() {
        return volumeName;
    }

    @Override
    public Long getSize() {
        return size;
    }

    @Override
    public Long getMinIops() {
        return minIops;
    }

    @Override
    public Long getMaxIops() {
        return maxIops;
    }

    @Override
    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public Long getStorageId() {
        return storageId;
    }

    @Override
    public Boolean getDisplayVolume() {
        return displayVolume;
    }

    @Override
    public boolean isDisplay() {
        return Boolean.TRUE.equals(displayVolume);
    }
}
