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

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.storage.Storage;
import com.cloud.utils.DateUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class AblestackV2KTargetStorageResolver {

    private static final String TARGET_PROVIDER_CLOUD = "ablestack-cloud";
    private static final String TARGET_DISK_TIMESTAMP_FORMAT = "yyyyMMddHHmm";

    private static final List<Storage.StoragePoolType> FILE_POOL_TYPES = Arrays.asList(
            Storage.StoragePoolType.Filesystem,
            Storage.StoragePoolType.NetworkFilesystem,
            Storage.StoragePoolType.SharedMountPoint
    );

    private static final List<Storage.StoragePoolType> BLOCK_POOL_TYPES = Arrays.asList(
            Storage.StoragePoolType.Iscsi,
            Storage.StoragePoolType.IscsiLUN,
            Storage.StoragePoolType.LVM,
            Storage.StoragePoolType.CLVM,
            Storage.StoragePoolType.PowerFlex,
            Storage.StoragePoolType.Linstor,
            Storage.StoragePoolType.StorPool,
            Storage.StoragePoolType.FiberChannel
    );

    private final Gson gson;

    public AblestackV2KTargetStorageResolver(Gson gson) {
        this.gson = gson;
    }

    public AblestackV2KTargetStoragePlan resolve(String vmName, UnmanagedInstanceTO sourceVmwareInstance,
                                                 DataStoreTO targetStorageLocation, Date importTaskCreatedTime) {
        PrimaryDataStoreTO primaryDataStoreTO = getPrimaryDataStoreTO(targetStorageLocation);
        Storage.StoragePoolType poolType = primaryDataStoreTO.getPoolType();
        if (Storage.StoragePoolType.RBD.equals(poolType)) {
            return buildRbdPlan(vmName, sourceVmwareInstance, primaryDataStoreTO, importTaskCreatedTime);
        }
        if (FILE_POOL_TYPES.contains(poolType)) {
            return buildFilePlan(primaryDataStoreTO);
        }
        if (BLOCK_POOL_TYPES.contains(poolType)) {
            throw new CloudRuntimeException(String.format("Primary storage pool type %s resolves to the ablestack-v2k block/raw target, " +
                    "but Cloud VM import does not yet have a safe per-disk block device reservation map. Select an RBD or file-backed " +
                    "primary storage pool for ablestack-v2k Cloud import.", poolType));
        }
        throw new CloudRuntimeException(String.format("Unsupported primary storage pool type %s for ablestack-v2k Cloud import", poolType));
    }

    public String getTargetFormat(DataStoreTO targetStorageLocation) {
        Storage.StoragePoolType poolType = getPrimaryDataStoreTO(targetStorageLocation).getPoolType();
        if (Storage.StoragePoolType.RBD.equals(poolType) || BLOCK_POOL_TYPES.contains(poolType)) {
            return "raw";
        }
        if (FILE_POOL_TYPES.contains(poolType)) {
            return "qcow2";
        }
        throw new CloudRuntimeException(String.format("Unsupported primary storage pool type %s for ablestack-v2k Cloud import", poolType));
    }

    public String getTargetStorage(DataStoreTO targetStorageLocation) {
        Storage.StoragePoolType poolType = getPrimaryDataStoreTO(targetStorageLocation).getPoolType();
        if (Storage.StoragePoolType.RBD.equals(poolType)) {
            return "rbd";
        }
        if (FILE_POOL_TYPES.contains(poolType)) {
            return "file";
        }
        if (BLOCK_POOL_TYPES.contains(poolType)) {
            return "block";
        }
        throw new CloudRuntimeException(String.format("Unsupported primary storage pool type %s for ablestack-v2k Cloud import", poolType));
    }

    public String buildTargetMapJson(String vmName, UnmanagedInstanceTO sourceVmwareInstance,
                                     DataStoreTO targetStorageLocation, Date importTaskCreatedTime) {
        Storage.StoragePoolType poolType = getPrimaryDataStoreTO(targetStorageLocation).getPoolType();
        if (!Storage.StoragePoolType.RBD.equals(poolType)) {
            return null;
        }
        return buildRbdTargetMapJson(vmName, sourceVmwareInstance, getRbdPoolName(targetStorageLocation), importTaskCreatedTime);
    }

    public boolean canCreateRunnablePlan(Storage.StoragePoolType poolType) {
        return Storage.StoragePoolType.RBD.equals(poolType) || FILE_POOL_TYPES.contains(poolType);
    }

    private AblestackV2KTargetStoragePlan buildRbdPlan(String vmName, UnmanagedInstanceTO sourceVmwareInstance,
                                                       PrimaryDataStoreTO primaryDataStoreTO, Date importTaskCreatedTime) {
        String rbdPoolName = getRbdPoolName(primaryDataStoreTO);
        String targetMapJson = buildRbdTargetMapJson(vmName, sourceVmwareInstance, rbdPoolName, importTaskCreatedTime);
        String destinationPath = "/var/lib/libvirt/images" + File.separator + sanitizeName(vmName);
        return new AblestackV2KTargetStoragePlan(TARGET_PROVIDER_CLOUD, "cloud-rbd", "rbd", "raw",
                destinationPath, targetMapJson, rbdPoolName, primaryDataStoreTO.getPoolType().name());
    }

    private AblestackV2KTargetStoragePlan buildFilePlan(PrimaryDataStoreTO primaryDataStoreTO) {
        String storageRoot = getFileStorageRoot(primaryDataStoreTO);
        String destinationPath = Storage.StoragePoolType.NetworkFilesystem.equals(primaryDataStoreTO.getPoolType()) ? null : storageRoot;
        return new AblestackV2KTargetStoragePlan(TARGET_PROVIDER_CLOUD, "cloud-filesystem", "file", "qcow2",
                destinationPath, null, storageRoot, primaryDataStoreTO.getPoolType().name());
    }

    private String buildRbdTargetMapJson(String vmName, UnmanagedInstanceTO sourceVmwareInstance, String rbdPoolName,
                                         Date importTaskCreatedTime) {
        if (sourceVmwareInstance == null || CollectionUtils.isEmpty(sourceVmwareInstance.getDisks())) {
            throw new CloudRuntimeException(String.format("Unable to build ablestack-v2k target disk mapping for VM %s without source disk information",
                    vmName));
        }
        String safeVmName = sanitizeName(vmName);
        String timestampSuffix = getTargetDiskTimestamp(importTaskCreatedTime, vmName);
        Map<String, String> targetMap = new LinkedHashMap<>();
        for (int index = 0; index < sourceVmwareInstance.getDisks().size(); index++) {
            UnmanagedInstanceTO.Disk disk = sourceVmwareInstance.getDisks().get(index);
            String sourceDiskKey = StringUtils.defaultIfBlank(disk.getDiskId(), String.format("scsi0:%d", index));
            targetMap.put(sourceDiskKey,
                    String.format("rbd:%s/%s-disk%d-%s", rbdPoolName, safeVmName, index, timestampSuffix));
        }
        return gson.toJson(targetMap);
    }

    private String getTargetDiskTimestamp(Date importTaskCreatedTime, String vmName) {
        if (importTaskCreatedTime == null) {
            throw new CloudRuntimeException(String.format("Import VM task created timestamp is null while building ablestack-v2k target map for VM %s",
                    vmName));
        }
        return DateUtil.getDateDisplayString(TimeZone.getDefault(), importTaskCreatedTime, TARGET_DISK_TIMESTAMP_FORMAT);
    }

    private PrimaryDataStoreTO getPrimaryDataStoreTO(DataStoreTO targetStorageLocation) {
        if (!(targetStorageLocation instanceof PrimaryDataStoreTO)) {
            throw new CloudRuntimeException("Ablestack-v2k Cloud import requires a primary datastore target");
        }
        return (PrimaryDataStoreTO) targetStorageLocation;
    }

    private String getRbdPoolName(DataStoreTO targetStorageLocation) {
        PrimaryDataStoreTO primaryDataStoreTO = getPrimaryDataStoreTO(targetStorageLocation);
        String poolName = StringUtils.defaultIfBlank(primaryDataStoreTO.getPath(), primaryDataStoreTO.getUuid());
        return StringUtils.removeStart(poolName, File.separator);
    }

    private String getFileStorageRoot(PrimaryDataStoreTO primaryDataStoreTO) {
        String storageRoot = StringUtils.defaultIfBlank(primaryDataStoreTO.getPath(), primaryDataStoreTO.getUrl());
        storageRoot = StringUtils.defaultIfBlank(storageRoot, primaryDataStoreTO.getUuid());
        if (StringUtils.isBlank(storageRoot)) {
            throw new CloudRuntimeException(String.format("Unable to resolve file storage path for primary storage pool %s",
                    primaryDataStoreTO.getName()));
        }
        return storageRoot;
    }

    private String sanitizeName(String name) {
        String safeName = StringUtils.defaultIfBlank(name, "vm").trim();
        safeName = safeName.replaceAll("[/\\\\]+", "_");
        safeName = safeName.replaceAll("\\s+", "_");
        safeName = safeName.replaceAll("[^A-Za-z0-9_.-]", "_");
        safeName = safeName.replaceAll("_+", "_");
        safeName = safeName.replaceAll("^\\.+", "");
        return StringUtils.defaultIfBlank(safeName, "vm");
    }
}
