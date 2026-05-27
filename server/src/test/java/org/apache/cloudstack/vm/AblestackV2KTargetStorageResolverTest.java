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

import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public class AblestackV2KTargetStorageResolverTest {

    private static final Type TARGET_MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    private AblestackV2KTargetStorageResolver resolver;
    private Gson gson;

    @Before
    public void setUp() {
        gson = new Gson();
        resolver = new AblestackV2KTargetStorageResolver(gson);
    }

    @Test
    public void resolveRbdStorageBuildsRawCloudPlanWithPerDiskTargetMap() {
        PrimaryDataStoreTO rbdStore = mockPrimaryStore(Storage.StoragePoolType.RBD, "/rbd", "rbd-uuid", "rbd-store");
        AblestackV2KTargetStoragePlan plan = resolver.resolve("vm name/with spaces", sourceInstanceWithDisks(2),
                rbdStore, new Date(0L));

        Assert.assertEquals("ablestack-cloud", plan.getTargetProvider());
        Assert.assertEquals("cloud-rbd", plan.getTargetProfile());
        Assert.assertEquals("rbd", plan.getTargetStorage());
        Assert.assertEquals("raw", plan.getTargetFormat());
        Assert.assertEquals("/var/lib/libvirt/images/vm_name_with_spaces", plan.getDestinationPath());
        Assert.assertEquals("rbd", plan.getStorageRoot());
        Assert.assertEquals(Storage.StoragePoolType.RBD.name(), plan.getPoolType());

        Map<String, String> targetMap = gson.fromJson(plan.getTargetMapJson(), TARGET_MAP_TYPE);
        Assert.assertEquals(2, targetMap.size());
        Assert.assertTrue(targetMap.get("scsi0:0").startsWith("rbd:rbd/vm_name_with_spaces-disk0-"));
        Assert.assertTrue(targetMap.get("scsi0:1").startsWith("rbd:rbd/vm_name_with_spaces-disk1-"));
    }


    @Test
    public void resolveRbdStorageUsesSourceDiskIdsWhenPresent() {
        PrimaryDataStoreTO rbdStore = mockPrimaryStore(Storage.StoragePoolType.RBD, "/rbd", "rbd-uuid", "rbd-store");
        UnmanagedInstanceTO instance = sourceInstanceWithDisks(2);
        instance.getDisks().get(0).setDiskId("nutanix-disk-0");
        instance.getDisks().get(1).setDiskId("nutanix-disk-1");

        AblestackV2KTargetStoragePlan plan = resolver.resolve("rhel", instance, rbdStore, new Date(0L));

        Map<String, String> targetMap = gson.fromJson(plan.getTargetMapJson(), TARGET_MAP_TYPE);
        Assert.assertTrue(targetMap.get("nutanix-disk-0").startsWith("rbd:rbd/rhel-disk0-"));
        Assert.assertTrue(targetMap.get("nutanix-disk-1").startsWith("rbd:rbd/rhel-disk1-"));
    }

    @Test
    public void resolveSharedMountPointStorageBuildsQcow2FilePlan() {
        PrimaryDataStoreTO sharedMountPoint = mockPrimaryStore(Storage.StoragePoolType.SharedMountPoint,
                "/mnt/glue-gfs", "smp-uuid", "shared-mount");

        AblestackV2KTargetStoragePlan plan = resolver.resolve("rhel", sourceInstanceWithDisks(1),
                sharedMountPoint, new Date(0L));

        Assert.assertEquals("cloud-filesystem", plan.getTargetProfile());
        Assert.assertEquals("file", plan.getTargetStorage());
        Assert.assertEquals("qcow2", plan.getTargetFormat());
        Assert.assertEquals("/mnt/glue-gfs", plan.getDestinationPath());
        Assert.assertNull(plan.getTargetMapJson());
        Assert.assertEquals("/mnt/glue-gfs", plan.getStorageRoot());
    }

    @Test
    public void networkFilesystemKeepsDestinationPathUnsetForMountResolution() {
        PrimaryDataStoreTO nfsStore = mockPrimaryStore(Storage.StoragePoolType.NetworkFilesystem,
                "nfs://10.0.0.10/export", "nfs-uuid", "nfs-store");

        AblestackV2KTargetStoragePlan plan = resolver.resolve("rhel", sourceInstanceWithDisks(1),
                nfsStore, new Date(0L));

        Assert.assertEquals("file", plan.getTargetStorage());
        Assert.assertEquals("qcow2", plan.getTargetFormat());
        Assert.assertNull(plan.getDestinationPath());
        Assert.assertEquals("nfs://10.0.0.10/export", plan.getStorageRoot());
    }

    @Test
    public void blockStorageIsReportedButRejectedForCloudRunnablePlan() {
        PrimaryDataStoreTO lvmStore = mockPrimaryStore(Storage.StoragePoolType.LVM, "/dev/vg", "lvm-uuid", "lvm-store");

        Assert.assertEquals("block", resolver.getTargetStorage(lvmStore));
        Assert.assertEquals("raw", resolver.getTargetFormat(lvmStore));
        Assert.assertFalse(resolver.canCreateRunnablePlan(Storage.StoragePoolType.LVM));
        Assert.assertThrows(CloudRuntimeException.class, () ->
                resolver.resolve("rhel", sourceInstanceWithDisks(1), lvmStore, new Date(0L)));
    }

    private PrimaryDataStoreTO mockPrimaryStore(Storage.StoragePoolType poolType, String path, String uuid, String name) {
        PrimaryDataStoreTO store = Mockito.mock(PrimaryDataStoreTO.class);
        Mockito.when(store.getPoolType()).thenReturn(poolType);
        Mockito.when(store.getPath()).thenReturn(path);
        Mockito.when(store.getUrl()).thenReturn(path);
        Mockito.when(store.getUuid()).thenReturn(uuid);
        Mockito.when(store.getName()).thenReturn(name);
        return store;
    }

    private UnmanagedInstanceTO sourceInstanceWithDisks(int count) {
        UnmanagedInstanceTO instance = new UnmanagedInstanceTO();
        UnmanagedInstanceTO.Disk[] disks = new UnmanagedInstanceTO.Disk[count];
        for (int index = 0; index < count; index++) {
            UnmanagedInstanceTO.Disk disk = new UnmanagedInstanceTO.Disk();
            disk.setLabel("disk-" + index);
            disk.setCapacity(1024L);
            disks[index] = disk;
        }
        instance.setDisks(Arrays.asList(disks));
        return instance;
    }
}
