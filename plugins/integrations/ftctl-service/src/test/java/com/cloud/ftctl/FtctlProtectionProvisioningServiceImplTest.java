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

import com.cloud.ftctl.dao.FtctlProtectionDao;
import com.cloud.ftctl.dao.FtctlProtectionVolumeDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.command.admin.ftctl.PrepareFtctlDrReplicaResourcesCmd;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class FtctlProtectionProvisioningServiceImplTest {

    @Mock
    private FtctlProtectionDao ftctlProtectionDao;
    @Mock
    private FtctlProtectionVolumeDao ftctlProtectionVolumeDao;
    @Mock
    private VolumeDao volumeDao;
    @Mock
    private VolumeApiService volumeApiService;
    @Mock
    private UserVmService userVmService;
    @Mock
    private UserVmDao userVmDao;
    @Mock
    private VMInstanceDao vmInstanceDao;
    @Mock
    private NicDao nicDao;
    @Mock
    private NetworkDao networkDao;
    @Mock
    private AccountDao accountDao;
    @Mock
    private DiskOfferingDao diskOfferingDao;
    @Mock
    private ServiceOfferingDao serviceOfferingDao;
    @Mock
    private HostDao hostDao;
    @Mock
    private PrimaryDataStoreDao primaryDataStoreDao;

    @InjectMocks
    private FtctlProtectionProvisioningServiceImpl service;

    @Test
    public void startCreatesFtctlHiddenOfferingsWhenMissing() {
        DiskOfferingVO persistedRootDiskOffering = mockDiskOffering(901L);
        DiskOfferingVO persistedDataDiskOffering = mockDiskOffering(902L);
        ServiceOfferingVO persistedComputeOffering = mockServiceOffering(801L);

        Mockito.when(diskOfferingDao.findByUniqueName("ABLESTACK.FTCTL.RootDisk.Custom")).thenReturn(null);
        Mockito.when(diskOfferingDao.findByUniqueName("ABLESTACK.FTCTL.DataDisk.Custom")).thenReturn(null);
        Mockito.when(diskOfferingDao.persistDefaultDiskOffering(Mockito.any(DiskOfferingVO.class)))
                .thenReturn(persistedRootDiskOffering)
                .thenReturn(persistedDataDiskOffering);
        Mockito.when(serviceOfferingDao.findByName("ABLESTACK.FTCTL.Compute.Custom")).thenReturn(null);
        Mockito.when(serviceOfferingDao.persistDeafultServiceOffering(Mockito.any(ServiceOfferingVO.class))).thenReturn(persistedComputeOffering);

        Assert.assertTrue(service.start());

        ArgumentCaptor<DiskOfferingVO> diskOfferingCaptor = ArgumentCaptor.forClass(DiskOfferingVO.class);
        Mockito.verify(diskOfferingDao, Mockito.times(2)).persistDefaultDiskOffering(diskOfferingCaptor.capture());
        List<DiskOfferingVO> diskOfferings = diskOfferingCaptor.getAllValues();
        Assert.assertEquals("ABLESTACK.FTCTL.RootDisk.Custom", diskOfferings.get(0).getUniqueName());
        Assert.assertEquals("ABLESTACK.FTCTL.DataDisk.Custom", diskOfferings.get(1).getUniqueName());
        Assert.assertFalse(diskOfferings.get(0).getDisplayOffering());
        Assert.assertFalse(diskOfferings.get(1).getDisplayOffering());
        Assert.assertTrue(diskOfferings.get(0).isCustomized());
        Assert.assertTrue(diskOfferings.get(1).isCustomized());
        Assert.assertTrue(diskOfferings.get(0).isCustomizedIops());
        Assert.assertTrue(diskOfferings.get(1).isCustomizedIops());

        ArgumentCaptor<ServiceOfferingVO> serviceOfferingCaptor = ArgumentCaptor.forClass(ServiceOfferingVO.class);
        Mockito.verify(serviceOfferingDao).persistDeafultServiceOffering(serviceOfferingCaptor.capture());
        ServiceOfferingVO serviceOffering = serviceOfferingCaptor.getValue();
        Assert.assertEquals("ABLESTACK.FTCTL.Compute.Custom", serviceOffering.getUniqueName());
        Assert.assertEquals(Long.valueOf(901L), serviceOffering.getDiskOfferingId());
        Assert.assertTrue(serviceOffering.isSystemUse());
        Assert.assertTrue(serviceOffering.isCustomized());
    }

    @Test
    public void prepareProtectionPersistsReadyRecordForLibvirtManagedBackend() {
        UserVmVO primaryVm = mockPrimaryVm();
        StoragePoolVO targetStoragePool = mockTargetStoragePool();
        FtctlProtectionProvisioningRequest request = new FtctlProtectionProvisioningRequest(
                primaryVm, 202L, targetStoragePool, "ha", "shared-blockcopy",
                FtctlProtectionProvisioningService.BACKEND_LIBVIRT_MANAGED, "manual-block", "vm-secondary");

        Mockito.when(ftctlProtectionDao.findActiveByPrimaryVmId(101L)).thenReturn(null);
        Mockito.when(ftctlProtectionDao.persist(Mockito.any(FtctlProtectionVO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FtctlProtectionProvisioningContext context = service.prepareProtection(request);

        Assert.assertEquals(FtctlProtectionProvisioningService.BACKEND_LIBVIRT_MANAGED, context.getProvisioningBackend());
        Assert.assertEquals(FtctlProtectionProvisioningService.STATE_READY, context.getProvisioningState());
        Assert.assertEquals("vm-secondary", context.getSecondaryVmName());

        ArgumentCaptor<FtctlProtectionVO> captor = ArgumentCaptor.forClass(FtctlProtectionVO.class);
        Mockito.verify(ftctlProtectionDao).persist(captor.capture());
        FtctlProtectionVO protection = captor.getValue();
        Assert.assertEquals(101L, protection.getPrimaryVmId());
        Assert.assertEquals(Long.valueOf(202L), protection.getPeerHostId());
        Assert.assertEquals(Long.valueOf(501L), protection.getTargetStoragePoolId());
        Assert.assertEquals("ha", protection.getMode());
        Assert.assertEquals("shared-blockcopy", protection.getBackendMode());
        Assert.assertEquals("manual-block", protection.getFencingPolicy());
        Assert.assertEquals(FtctlProtectionProvisioningService.STATE_READY, protection.getProvisioningState());
    }

    @Test
    public void prepareProtectionPersistsPrimaryVolumeRecordsForLibvirtManagedBackend() {
        UserVmVO primaryVm = mockPrimaryVm();
        StoragePoolVO targetStoragePool = mockTargetStoragePool();
        VolumeVO rootVolume = mockVolume(301L, Volume.Type.ROOT, 0L, "rbd/root-disk");
        FtctlProtectionProvisioningRequest request = new FtctlProtectionProvisioningRequest(
                primaryVm, 202L, targetStoragePool, "ha", "shared-blockcopy",
                FtctlProtectionProvisioningService.BACKEND_LIBVIRT_MANAGED, "manual-block", "vm-secondary");

        Mockito.when(ftctlProtectionDao.findActiveByPrimaryVmId(101L)).thenReturn(null);
        Mockito.when(ftctlProtectionDao.persist(Mockito.any(FtctlProtectionVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(volumeDao.findByInstance(101L)).thenReturn(Collections.singletonList(rootVolume));
        Mockito.when(ftctlProtectionVolumeDao.findActiveByProtectionIdAndPrimaryVolumeId(0L, 301L)).thenReturn(null);

        service.prepareProtection(request);

        ArgumentCaptor<FtctlProtectionVolumeVO> captor = ArgumentCaptor.forClass(FtctlProtectionVolumeVO.class);
        Mockito.verify(ftctlProtectionVolumeDao).persist(captor.capture());
        FtctlProtectionVolumeVO protectionVolume = captor.getValue();
        Assert.assertEquals(0L, protectionVolume.getProtectionId());
        Assert.assertEquals(301L, protectionVolume.getPrimaryVolumeId());
        Assert.assertEquals("rbd/root-disk", protectionVolume.getPrimaryDiskPath());
        Assert.assertEquals("root-0", protectionVolume.getDiskLabel());
    }

    @Test
    public void prepareProtectionAllocatesStandbyVmForCloudManagedBackendAndBlocksUntilDiskPathsAreReady() throws Exception {
        UserVmVO primaryVm = mockPrimaryVm();
        Mockito.when(primaryVm.getDetails()).thenReturn(Map.of(
                "rootDiskController", "scsi",
                "dataDiskController", "scsi",
                "UEFI", "SECURE",
                "iothreads", "true",
                "nameonhypervisor", "primary-hypervisor-name"));
        AccountVO owner = mockAccount();
        NicVO nic = mockNic(701L);
        ServiceOfferingVO primaryOffering = mockPrimaryServiceOffering();
        ServiceOfferingVO ftctlComputeOffering = mockServiceOffering(801L);
        DiskOfferingVO ftctlRootDiskOffering = mockDiskOffering(901L);
        DiskOfferingVO ftctlDataDiskOffering = mockDiskOffering(902L);
        StoragePoolVO targetStoragePool = mockTargetStoragePool();
        VolumeVO primaryRootVolume = mockVolume(301L, Volume.Type.ROOT, 0L, "rbd/primary-root");
        Mockito.when(primaryRootVolume.getSize()).thenReturn(10L * 1024L * 1024L * 1024L);
        Mockito.when(primaryRootVolume.getMinIops()).thenReturn(0L);
        Mockito.when(primaryRootVolume.getMaxIops()).thenReturn(0L);
        VolumeVO primaryDataVolume = mockVolume(302L, Volume.Type.DATADISK, 1L, "rbd/primary-data");
        Mockito.when(primaryDataVolume.getSize()).thenReturn(20L * 1024L * 1024L * 1024L);
        Mockito.when(primaryDataVolume.getMinIops()).thenReturn(100L);
        Mockito.when(primaryDataVolume.getMaxIops()).thenReturn(1000L);
        VolumeVO standbyRootVolume = mockVolume(501L, Volume.Type.DATADISK, 0L, "rbd/standby-root");
        Mockito.when(standbyRootVolume.getVolumeType()).thenReturn(Volume.Type.DATADISK, Volume.Type.ROOT);
        Mockito.when(standbyRootVolume.getUuid()).thenReturn("standby-root-uuid");
        VolumeVO standbyDataVolume = mockVolume(502L, Volume.Type.DATADISK, 1L, "rbd/standby-data");
        Mockito.when(standbyDataVolume.getUuid()).thenReturn("standby-data-uuid");
        UserVm createdVm = mockCreatedUserVm(401L, "standby-created-uuid");
        UserVmVO standbyVm = mockStandbyVm();
        FtctlProtectionProvisioningRequest request = new FtctlProtectionProvisioningRequest(
                primaryVm, 202L, targetStoragePool, "ha", "shared-blockcopy",
                FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED, "manual-block", "vm-secondary");

        Mockito.when(ftctlProtectionDao.findActiveByPrimaryVmId(101L)).thenReturn(null);
        Mockito.when(ftctlProtectionDao.persist(Mockito.any(FtctlProtectionVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(diskOfferingDao.findByUniqueName("ABLESTACK.FTCTL.RootDisk.Custom")).thenReturn(ftctlRootDiskOffering);
        Mockito.when(diskOfferingDao.findByUniqueName("ABLESTACK.FTCTL.DataDisk.Custom")).thenReturn(ftctlDataDiskOffering);
        Mockito.when(serviceOfferingDao.findByName("ABLESTACK.FTCTL.Compute.Custom")).thenReturn(ftctlComputeOffering);
        Mockito.when(serviceOfferingDao.findById(301L)).thenReturn(primaryOffering);
        Mockito.when(ftctlProtectionVolumeDao.findActiveByProtectionIdAndPrimaryVolumeId(0L, 301L)).thenReturn(null);
        Mockito.when(ftctlProtectionVolumeDao.findActiveByProtectionIdAndPrimaryVolumeId(0L, 302L)).thenReturn(null);
        Mockito.when(volumeApiService.allocVolume(Mockito.any(FtctlCreateVolumeCmd.class))).thenReturn(standbyRootVolume).thenReturn(standbyDataVolume);
        Mockito.when(volumeApiService.createVolume(Mockito.any(FtctlCreateVolumeCmd.class))).thenReturn(standbyRootVolume).thenReturn(standbyDataVolume);
        Mockito.when(vmInstanceDao.findVMByHostNameInZone("vm-secondary", 401L)).thenReturn(null);
        Mockito.when(accountDao.findById(11L)).thenReturn(owner);
        Mockito.when(nicDao.listByVmIdOrderByDeviceId(101L)).thenReturn(Collections.singletonList(nic));
        Mockito.when(userVmService.createVirtualMachineVolume(Mockito.any(FtctlStandbyDeployVMVolumeCmd.class))).thenReturn(createdVm);
        Mockito.when(userVmDao.findById(401L)).thenReturn(standbyVm);
        Mockito.when(volumeDao.findByInstance(101L)).thenReturn(List.of(primaryRootVolume, primaryDataVolume));
        Mockito.when(volumeDao.findById(501L)).thenReturn(standbyRootVolume);
        Mockito.when(volumeDao.findById(502L)).thenReturn(standbyDataVolume);

        try {
            service.prepareProtection(request);
            Assert.fail("Cloud-managed provisioning should be blocked until standby volume disk paths are ready");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("standby VM"));
            Assert.assertTrue(e.getMessage().contains("disk paths are not ready"));
        }

        ArgumentCaptor<FtctlCreateVolumeCmd> createVolumeCaptor = ArgumentCaptor.forClass(FtctlCreateVolumeCmd.class);
        Mockito.verify(volumeApiService, Mockito.times(2)).allocVolume(createVolumeCaptor.capture());
        List<FtctlCreateVolumeCmd> createVolumeCommands = createVolumeCaptor.getAllValues();
        FtctlCreateVolumeCmd createRootVolumeCmd = createVolumeCommands.get(0);
        Assert.assertEquals(11L, createRootVolumeCmd.getEntityOwnerId());
        Assert.assertEquals("admin", createRootVolumeCmd.getAccountName());
        Assert.assertEquals(Long.valueOf(1L), createRootVolumeCmd.getDomainId());
        Assert.assertEquals(Long.valueOf(901L), createRootVolumeCmd.getDiskOfferingId());
        Assert.assertEquals("vm-secondary-root", createRootVolumeCmd.getVolumeName());
        Assert.assertEquals(Long.valueOf(10L), createRootVolumeCmd.getSize());
        Assert.assertNull(createRootVolumeCmd.getMinIops());
        Assert.assertNull(createRootVolumeCmd.getMaxIops());
        Assert.assertEquals(Long.valueOf(401L), createRootVolumeCmd.getZoneId());
        Assert.assertEquals(Long.valueOf(501L), createRootVolumeCmd.getStorageId());
        Assert.assertTrue(createRootVolumeCmd.isDisplay());
        FtctlCreateVolumeCmd createDataVolumeCmd = createVolumeCommands.get(1);
        Assert.assertEquals(Long.valueOf(902L), createDataVolumeCmd.getDiskOfferingId());
        Assert.assertEquals("vm-secondary-data-1", createDataVolumeCmd.getVolumeName());
        Assert.assertEquals(Long.valueOf(20L), createDataVolumeCmd.getSize());
        Assert.assertEquals(Long.valueOf(100L), createDataVolumeCmd.getMinIops());
        Assert.assertEquals(Long.valueOf(1000L), createDataVolumeCmd.getMaxIops());
        Assert.assertEquals(Long.valueOf(501L), createDataVolumeCmd.getStorageId());
        Assert.assertTrue(createDataVolumeCmd.isDisplay());

        Mockito.verify(standbyRootVolume).setVolumeType(Volume.Type.ROOT);
        Mockito.verify(standbyRootVolume).setDeviceId(0L);
        Mockito.verify(volumeDao).update(501L, standbyRootVolume);

        ArgumentCaptor<FtctlStandbyDeployVMVolumeCmd> deployCaptor = ArgumentCaptor.forClass(FtctlStandbyDeployVMVolumeCmd.class);
        Mockito.verify(userVmService).createVirtualMachineVolume(deployCaptor.capture());
        FtctlStandbyDeployVMVolumeCmd deployCmd = deployCaptor.getValue();
        Assert.assertEquals(11L, deployCmd.getEntityOwnerId());
        Assert.assertEquals("admin", deployCmd.getAccountName());
        Assert.assertEquals(Long.valueOf(1L), deployCmd.getDomainId());
        Assert.assertEquals(Long.valueOf(401L), deployCmd.getZoneId());
        Assert.assertEquals(Long.valueOf(801L), deployCmd.getServiceOfferingId());
        Assert.assertEquals("vm-secondary", deployCmd.getName());
        Assert.assertEquals("vm-secondary", deployCmd.getDisplayName());
        Assert.assertEquals(List.of(701L), deployCmd.getNetworkIds());
        Assert.assertEquals(Long.valueOf(202L), deployCmd.getHostId());
        Assert.assertEquals(HypervisorType.KVM, deployCmd.getHypervisor());
        Assert.assertEquals(Long.valueOf(501L), deployCmd.getVolumeId());
        Assert.assertFalse(deployCmd.getStartVm());
        Assert.assertTrue(deployCmd.isDisplayVm());
        Assert.assertTrue(deployCmd.isDisplay());
        Assert.assertEquals("2", deployCmd.getDetails().get("cpuNumber"));
        Assert.assertEquals("1000", deployCmd.getDetails().get("cpuSpeed"));
        Assert.assertEquals("4096", deployCmd.getDetails().get("memory"));
        Assert.assertEquals("10", deployCmd.getDetails().get("rootdisksize"));
        Assert.assertEquals("scsi", deployCmd.getDetails().get("rootDiskController"));
        Assert.assertEquals("scsi", deployCmd.getDetails().get("dataDiskController"));
        Assert.assertEquals("SECURE", deployCmd.getDetails().get("UEFI"));
        Assert.assertEquals("true", deployCmd.getDetails().get("iothreads"));
        Assert.assertEquals("pc-i440fx-9.2", deployCmd.getDetails().get(VmDetailConstants.KVM_GUEST_OS_MACHINE_TYPE));
        Assert.assertFalse(deployCmd.getDetails().containsKey("nameonhypervisor"));
        Mockito.verify(volumeDao).attachVolume(502L, 401L, 1L);

        ArgumentCaptor<FtctlProtectionVO> persistCaptor = ArgumentCaptor.forClass(FtctlProtectionVO.class);
        Mockito.verify(ftctlProtectionDao).persist(persistCaptor.capture());
        FtctlProtectionVO protection = persistCaptor.getValue();
        Assert.assertEquals(FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED, protection.getProvisioningBackend());
        Assert.assertEquals(FtctlProtectionProvisioningService.STATE_STANDBY_ALLOCATED, protection.getProvisioningState());
        Assert.assertEquals(Long.valueOf(401L), protection.getSecondaryVmId());
        Assert.assertTrue(protection.getLastError().contains("disk paths are not ready"));
    }

    @Test
    public void prepareProtectionReturnsReadyContextWhenAllStandbyDiskPathsAreAvailable() throws Exception {
        UserVmVO primaryVm = mockPrimaryVm();
        Mockito.when(primaryVm.getDetails()).thenReturn(Map.of(
                "rootDiskController", "scsi",
                "dataDiskController", "scsi"));
        AccountVO owner = mockAccount();
        ServiceOfferingVO primaryOffering = mockPrimaryServiceOffering();
        ServiceOfferingVO ftctlComputeOffering = mockServiceOffering(801L);
        DiskOfferingVO ftctlRootDiskOffering = mockDiskOffering(901L);
        DiskOfferingVO ftctlDataDiskOffering = mockDiskOffering(902L);
        StoragePoolVO targetStoragePool = mockTargetStoragePool();
        VolumeVO primaryRootVolume = mockVolume(301L, Volume.Type.ROOT, 0L, "rbd/primary-root");
        Mockito.when(primaryRootVolume.getSize()).thenReturn(10L * 1024L * 1024L * 1024L);
        VolumeVO primaryDataVolume = mockVolume(302L, Volume.Type.DATADISK, 1L, "rbd/primary-data");
        Mockito.when(primaryDataVolume.getSize()).thenReturn(20L * 1024L * 1024L * 1024L);
        VolumeVO standbyRootVolume = mockVolume(501L, Volume.Type.ROOT, 0L, "rbd/standby-root");
        Mockito.when(standbyRootVolume.getUuid()).thenReturn("standby-root-uuid");
        VolumeVO standbyDataVolume = mockVolume(502L, Volume.Type.DATADISK, 1L, "rbd/standby-data");
        Mockito.when(standbyDataVolume.getUuid()).thenReturn("standby-data-uuid");
        UserVm createdVm = mockCreatedUserVm(401L, "standby-created-uuid");
        UserVmVO standbyVm = mockStandbyVm();
        FtctlProtectionVolumeVO rootProtectionVolume = new FtctlProtectionVolumeVO(0L, 301L);
        FtctlProtectionVolumeVO dataProtectionVolume = new FtctlProtectionVolumeVO(0L, 302L);
        FtctlProtectionProvisioningRequest request = new FtctlProtectionProvisioningRequest(
                primaryVm, 202L, targetStoragePool, "ha", "shared-blockcopy",
                FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED, "manual-block", "vm-secondary");

        Mockito.when(ftctlProtectionDao.findActiveByPrimaryVmId(101L)).thenReturn(null);
        Mockito.when(ftctlProtectionDao.persist(Mockito.any(FtctlProtectionVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(diskOfferingDao.findByUniqueName("ABLESTACK.FTCTL.RootDisk.Custom")).thenReturn(ftctlRootDiskOffering);
        Mockito.when(diskOfferingDao.findByUniqueName("ABLESTACK.FTCTL.DataDisk.Custom")).thenReturn(ftctlDataDiskOffering);
        Mockito.when(serviceOfferingDao.findByName("ABLESTACK.FTCTL.Compute.Custom")).thenReturn(ftctlComputeOffering);
        Mockito.when(serviceOfferingDao.findById(301L)).thenReturn(primaryOffering);
        Mockito.when(ftctlProtectionVolumeDao.findActiveByProtectionIdAndPrimaryVolumeId(0L, 301L))
                .thenReturn(null)
                .thenReturn(rootProtectionVolume)
                .thenReturn(rootProtectionVolume);
        Mockito.when(ftctlProtectionVolumeDao.findActiveByProtectionIdAndPrimaryVolumeId(0L, 302L))
                .thenReturn(null)
                .thenReturn(dataProtectionVolume)
                .thenReturn(dataProtectionVolume);
        Mockito.when(volumeApiService.allocVolume(Mockito.any(FtctlCreateVolumeCmd.class))).thenReturn(standbyRootVolume).thenReturn(standbyDataVolume);
        Mockito.when(volumeApiService.createVolume(Mockito.any(FtctlCreateVolumeCmd.class))).thenReturn(standbyRootVolume).thenReturn(standbyDataVolume);
        Mockito.when(vmInstanceDao.findVMByHostNameInZone("vm-secondary", 401L)).thenReturn(null);
        Mockito.when(accountDao.findById(11L)).thenReturn(owner);
        NicVO nic = mockNic(701L);
        Mockito.when(nicDao.listByVmIdOrderByDeviceId(101L)).thenReturn(Collections.singletonList(nic));
        Mockito.when(userVmService.createVirtualMachineVolume(Mockito.any(FtctlStandbyDeployVMVolumeCmd.class))).thenReturn(createdVm);
        Mockito.when(userVmDao.findById(401L)).thenReturn(standbyVm);
        Mockito.when(volumeDao.findByInstance(Mockito.anyLong())).thenAnswer(invocation -> {
            Long vmId = invocation.getArgument(0);
            if (vmId == 101L) {
                return List.of(primaryRootVolume, primaryDataVolume);
            }
            return Collections.emptyList();
        });
        Mockito.when(volumeDao.findById(501L)).thenReturn(standbyRootVolume);
        Mockito.when(volumeDao.findById(502L)).thenReturn(standbyDataVolume);
        Mockito.when(ftctlProtectionVolumeDao.listActiveByProtectionId(0L)).thenReturn(List.of(rootProtectionVolume, dataProtectionVolume));

        FtctlProtectionProvisioningContext context = service.prepareProtection(request);

        Assert.assertEquals(FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED, context.getProvisioningBackend());
        Assert.assertEquals(FtctlProtectionProvisioningService.STATE_READY, context.getProvisioningState());
        Assert.assertEquals("i-2-401-VM", context.getSecondaryVmName());
        Assert.assertEquals("sda=/dev/rbd/rbd/standby-root;sdb=/dev/rbd/rbd/standby-data", context.getDiskMap());
    }

    @Test
    public void prepareProtectionBuildsAbsoluteDiskMapForFilesystemStorage() throws Exception {
        UserVmVO primaryVm = mockPrimaryVm();
        Mockito.when(primaryVm.getDetails()).thenReturn(Map.of(
                "rootDiskController", "scsi",
                "dataDiskController", "scsi"));
        AccountVO owner = mockAccount();
        ServiceOfferingVO primaryOffering = mockPrimaryServiceOffering();
        ServiceOfferingVO ftctlComputeOffering = mockServiceOffering(801L);
        DiskOfferingVO ftctlRootDiskOffering = mockDiskOffering(901L);
        DiskOfferingVO ftctlDataDiskOffering = mockDiskOffering(902L);
        StoragePoolVO targetStoragePool = mockLocalTargetStoragePool();
        VolumeVO primaryRootVolume = mockVolume(301L, Volume.Type.ROOT, 0L, "rbd/primary-root");
        Mockito.when(primaryRootVolume.getSize()).thenReturn(10L * 1024L * 1024L * 1024L);
        VolumeVO primaryDataVolume = mockVolume(302L, Volume.Type.DATADISK, 1L, "rbd/primary-data");
        Mockito.when(primaryDataVolume.getSize()).thenReturn(20L * 1024L * 1024L * 1024L);
        VolumeVO standbyRootVolume = mockVolume(501L, Volume.Type.ROOT, 0L, "standby-root-file");
        Mockito.when(standbyRootVolume.getUuid()).thenReturn("standby-root-uuid");
        VolumeVO standbyDataVolume = mockVolume(502L, Volume.Type.DATADISK, 1L, "standby-data-file");
        Mockito.when(standbyDataVolume.getUuid()).thenReturn("standby-data-uuid");
        UserVm createdVm = mockCreatedUserVm(401L, "standby-created-uuid");
        UserVmVO standbyVm = mockStandbyVm();
        FtctlProtectionVolumeVO rootProtectionVolume = new FtctlProtectionVolumeVO(0L, 301L);
        FtctlProtectionVolumeVO dataProtectionVolume = new FtctlProtectionVolumeVO(0L, 302L);
        FtctlProtectionProvisioningRequest request = new FtctlProtectionProvisioningRequest(
                primaryVm, 202L, targetStoragePool, "ha", "remote-nbd",
                FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED, "manual-block", "vm-secondary");

        Mockito.when(ftctlProtectionDao.findActiveByPrimaryVmId(101L)).thenReturn(null);
        Mockito.when(ftctlProtectionDao.persist(Mockito.any(FtctlProtectionVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(diskOfferingDao.findByUniqueName("ABLESTACK.FTCTL.RootDisk.Custom")).thenReturn(ftctlRootDiskOffering);
        Mockito.when(diskOfferingDao.findByUniqueName("ABLESTACK.FTCTL.DataDisk.Custom")).thenReturn(ftctlDataDiskOffering);
        Mockito.when(serviceOfferingDao.findByName("ABLESTACK.FTCTL.Compute.Custom")).thenReturn(ftctlComputeOffering);
        Mockito.when(serviceOfferingDao.findById(301L)).thenReturn(primaryOffering);
        Mockito.when(ftctlProtectionVolumeDao.findActiveByProtectionIdAndPrimaryVolumeId(0L, 301L))
                .thenReturn(null)
                .thenReturn(rootProtectionVolume)
                .thenReturn(rootProtectionVolume);
        Mockito.when(ftctlProtectionVolumeDao.findActiveByProtectionIdAndPrimaryVolumeId(0L, 302L))
                .thenReturn(null)
                .thenReturn(dataProtectionVolume)
                .thenReturn(dataProtectionVolume);
        Mockito.when(volumeApiService.allocVolume(Mockito.any(FtctlCreateVolumeCmd.class))).thenReturn(standbyRootVolume).thenReturn(standbyDataVolume);
        Mockito.when(volumeApiService.createVolume(Mockito.any(FtctlCreateVolumeCmd.class))).thenReturn(standbyRootVolume).thenReturn(standbyDataVolume);
        Mockito.when(vmInstanceDao.findVMByHostNameInZone("vm-secondary", 401L)).thenReturn(null);
        Mockito.when(accountDao.findById(11L)).thenReturn(owner);
        NicVO nic = mockNic(701L);
        Mockito.when(nicDao.listByVmIdOrderByDeviceId(101L)).thenReturn(Collections.singletonList(nic));
        Mockito.when(userVmService.createVirtualMachineVolume(Mockito.any(FtctlStandbyDeployVMVolumeCmd.class))).thenReturn(createdVm);
        Mockito.when(userVmDao.findById(401L)).thenReturn(standbyVm);
        Mockito.when(volumeDao.findByInstance(Mockito.anyLong())).thenAnswer(invocation -> {
            Long vmId = invocation.getArgument(0);
            if (vmId == 101L) {
                return List.of(primaryRootVolume, primaryDataVolume);
            }
            return Collections.emptyList();
        });
        Mockito.when(volumeDao.findById(501L)).thenReturn(standbyRootVolume);
        Mockito.when(volumeDao.findById(502L)).thenReturn(standbyDataVolume);
        Mockito.when(ftctlProtectionVolumeDao.listActiveByProtectionId(0L)).thenReturn(List.of(rootProtectionVolume, dataProtectionVolume));

        FtctlProtectionProvisioningContext context = service.prepareProtection(request);

        Assert.assertEquals("sda=/var/lib/libvirt/images/standby-root-file;sdb=/var/lib/libvirt/images/standby-data-file", context.getDiskMap());
    }

    @Test
    public void prepareDrReplicaResourcesSelectsSafeReplicaNameWhenRequestedNameIsOccupied() throws Exception {
        PrepareFtctlDrReplicaResourcesCmd cmd = Mockito.mock(PrepareFtctlDrReplicaResourcesCmd.class);
        StoragePoolVO targetStoragePool = mockTargetStoragePool();
        HostVO targetHost = Mockito.mock(HostVO.class);
        AccountVO owner = mockAccount();
        ServiceOfferingVO ftctlComputeOffering = mockServiceOffering(801L);
        DiskOfferingVO ftctlRootDiskOffering = mockDiskOffering(901L);
        DiskOfferingVO ftctlDataDiskOffering = mockDiskOffering(902L);
        VolumeVO replicaRootVolume = mockVolume(601L, Volume.Type.ROOT, 0L, "replica-root");
        VolumeVO replicaDataVolume = mockVolume(602L, Volume.Type.DATADISK, 1L, "replica-data");
        UserVm createdVm = mockCreatedUserVm(701L, "replica-vm-uuid");
        UserVmVO replicaVm = Mockito.mock(UserVmVO.class);
        VMInstanceVO conflictingVm = Mockito.mock(VMInstanceVO.class);
        UserVmVO conflictingUserVm = Mockito.mock(UserVmVO.class);
        NetworkVO network = Mockito.mock(NetworkVO.class);

        Mockito.when(cmd.getRemoteTargetStoragePoolUuid()).thenReturn("target-pool-uuid");
        Mockito.when(cmd.getRemotePeerHostUuid()).thenReturn("target-host-uuid");
        Mockito.when(cmd.getSourceVirtualMachineId()).thenReturn("source-vm-uuid");
        Mockito.when(cmd.getSourceVirtualMachineName()).thenReturn("vm-primary");
        Mockito.when(cmd.getSecondaryVmName()).thenReturn("vm-primary");
        Mockito.when(cmd.getSourceHypervisor()).thenReturn("KVM");
        Mockito.when(cmd.getSourceVmDetails()).thenReturn("{}");
        Mockito.when(cmd.getNetworkIds()).thenReturn("701");
        Mockito.when(cmd.getSourceVolumes()).thenReturn("[{\"sourcevolumeid\":\"301\",\"sourcedisktarget\":\"sda\",\"disklabel\":\"root-0\",\"type\":\"ROOT\",\"deviceid\":0,\"size\":10737418240,\"miniops\":0,\"maxiops\":0},{\"sourcevolumeid\":\"302\",\"sourcedisktarget\":\"sdb\",\"disklabel\":\"data-1\",\"type\":\"DATADISK\",\"deviceid\":1,\"size\":21474836480,\"miniops\":0,\"maxiops\":0}]");

        Mockito.when(primaryDataStoreDao.findByUuid("target-pool-uuid")).thenReturn(targetStoragePool);
        Mockito.when(hostDao.findByUuid("target-host-uuid")).thenReturn(targetHost);
        Mockito.when(targetHost.getId()).thenReturn(202L);
        Mockito.when(targetHost.getName()).thenReturn("ablecube22-1");
        Mockito.when(accountDao.findById(Mockito.anyLong())).thenReturn(owner);
        Mockito.when(diskOfferingDao.findByUniqueName("ABLESTACK.FTCTL.RootDisk.Custom")).thenReturn(ftctlRootDiskOffering);
        Mockito.when(diskOfferingDao.findByUniqueName("ABLESTACK.FTCTL.DataDisk.Custom")).thenReturn(ftctlDataDiskOffering);
        Mockito.when(serviceOfferingDao.findByName("ABLESTACK.FTCTL.Compute.Custom")).thenReturn(ftctlComputeOffering);
        Mockito.when(vmInstanceDao.findVMByHostNameInZone("vm-primary", 401L)).thenReturn(conflictingVm);
        Mockito.when(vmInstanceDao.findVMByHostNameInZone("vm-primary-replica", 401L)).thenReturn(null);
        Mockito.when(conflictingVm.getId()).thenReturn(900L);
        Mockito.when(userVmDao.findById(900L)).thenReturn(conflictingUserVm);
        Mockito.when(conflictingUserVm.getDetails()).thenReturn(Collections.emptyMap());
        Mockito.when(volumeApiService.allocVolume(Mockito.any(FtctlCreateVolumeCmd.class))).thenReturn(replicaRootVolume).thenReturn(replicaDataVolume);
        Mockito.when(volumeApiService.createVolume(Mockito.any(FtctlCreateVolumeCmd.class))).thenReturn(replicaRootVolume).thenReturn(replicaDataVolume);
        Mockito.when(userVmService.createVirtualMachineVolume(Mockito.any(FtctlStandbyDeployVMVolumeCmd.class))).thenReturn(createdVm);
        Mockito.when(userVmDao.findById(701L)).thenReturn(replicaVm);
        Mockito.when(replicaVm.getId()).thenReturn(701L);
        Mockito.when(replicaVm.getUuid()).thenReturn("replica-vm-uuid");
        Mockito.when(replicaVm.getDisplayName()).thenReturn("vm-primary-replica");
        Mockito.when(replicaVm.getInstanceName()).thenReturn("i-2-701-VM");
        Mockito.when(volumeDao.findByInstance(701L)).thenReturn(Collections.emptyList());
        Mockito.when(volumeDao.findById(601L)).thenReturn(replicaRootVolume);
        Mockito.when(volumeDao.findById(602L)).thenReturn(replicaDataVolume);
        Mockito.when(networkDao.findById(701L)).thenReturn(network);
        Mockito.when(network.getDataCenterId()).thenReturn(401L);

        service.prepareDrReplicaResources(cmd);

        ArgumentCaptor<FtctlCreateVolumeCmd> volumeCaptor = ArgumentCaptor.forClass(FtctlCreateVolumeCmd.class);
        Mockito.verify(volumeApiService, Mockito.times(2)).allocVolume(volumeCaptor.capture());
        Assert.assertEquals("vm-primary-replica-root", volumeCaptor.getAllValues().get(0).getVolumeName());
        Assert.assertNull(volumeCaptor.getAllValues().get(0).getMinIops());
        Assert.assertNull(volumeCaptor.getAllValues().get(0).getMaxIops());
        Assert.assertEquals("vm-primary-replica-data-1", volumeCaptor.getAllValues().get(1).getVolumeName());
        Assert.assertNull(volumeCaptor.getAllValues().get(1).getMinIops());
        Assert.assertNull(volumeCaptor.getAllValues().get(1).getMaxIops());

        ArgumentCaptor<FtctlStandbyDeployVMVolumeCmd> deployCaptor = ArgumentCaptor.forClass(FtctlStandbyDeployVMVolumeCmd.class);
        Mockito.verify(userVmService).createVirtualMachineVolume(deployCaptor.capture());
        Assert.assertEquals("vm-primary-replica", deployCaptor.getValue().getName());
        Assert.assertEquals("vm-primary-replica", deployCaptor.getValue().getDisplayName());
        Assert.assertEquals("pc-i440fx-9.2", deployCaptor.getValue().getDetails().get(VmDetailConstants.KVM_GUEST_OS_MACHINE_TYPE));
    }

    private UserVmVO mockPrimaryVm() {
        UserVmVO primaryVm = Mockito.mock(UserVmVO.class);
        Mockito.lenient().when(primaryVm.getId()).thenReturn(101L);
        Mockito.lenient().when(primaryVm.getUuid()).thenReturn("primary-uuid");
        Mockito.lenient().when(primaryVm.getAccountId()).thenReturn(11L);
        Mockito.lenient().when(primaryVm.getDomainId()).thenReturn(1L);
        Mockito.lenient().when(primaryVm.getDataCenterId()).thenReturn(401L);
        Mockito.lenient().when(primaryVm.getServiceOfferingId()).thenReturn(301L);
        Mockito.lenient().when(primaryVm.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.lenient().when(primaryVm.getHostName()).thenReturn("vm-primary");
        return primaryVm;
    }

    private StoragePoolVO mockTargetStoragePool() {
        StoragePoolVO targetStoragePool = Mockito.mock(StoragePoolVO.class);
        Mockito.lenient().when(targetStoragePool.getId()).thenReturn(501L);
        Mockito.lenient().when(targetStoragePool.getDataCenterId()).thenReturn(401L);
        Mockito.lenient().when(targetStoragePool.getPoolType()).thenReturn(Storage.StoragePoolType.RBD);
        Mockito.lenient().when(targetStoragePool.getPath()).thenReturn("rbd");
        return targetStoragePool;
    }

    private StoragePoolVO mockLocalTargetStoragePool() {
        StoragePoolVO targetStoragePool = Mockito.mock(StoragePoolVO.class);
        Mockito.lenient().when(targetStoragePool.getId()).thenReturn(501L);
        Mockito.lenient().when(targetStoragePool.getDataCenterId()).thenReturn(401L);
        Mockito.lenient().when(targetStoragePool.getPoolType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.lenient().when(targetStoragePool.getPath()).thenReturn("/var/lib/libvirt/images");
        return targetStoragePool;
    }

    private VolumeVO mockVolume(long id, Volume.Type volumeType, Long deviceId, String path) {
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getId()).thenReturn(id);
        Mockito.when(volume.getVolumeType()).thenReturn(volumeType);
        Mockito.when(volume.getDeviceId()).thenReturn(deviceId);
        Mockito.when(volume.getPath()).thenReturn(path);
        Mockito.lenient().when(volume.getUuid()).thenReturn(String.format("volume-%s", id));
        return volume;
    }

    private AccountVO mockAccount() {
        AccountVO account = Mockito.mock(AccountVO.class);
        Mockito.when(account.getId()).thenReturn(11L);
        Mockito.when(account.getAccountName()).thenReturn("admin");
        Mockito.when(account.getDomainId()).thenReturn(1L);
        return account;
    }

    private NicVO mockNic(long networkId) {
        NicVO nic = Mockito.mock(NicVO.class);
        Mockito.when(nic.getNetworkId()).thenReturn(networkId);
        return nic;
    }

    private UserVm mockCreatedUserVm(long id, String uuid) {
        UserVm vm = Mockito.mock(UserVm.class);
        Mockito.when(vm.getId()).thenReturn(id);
        Mockito.lenient().when(vm.getUuid()).thenReturn(uuid);
        return vm;
    }

    private UserVmVO mockStandbyVm() {
        UserVmVO standbyVm = Mockito.mock(UserVmVO.class);
        Mockito.when(standbyVm.getId()).thenReturn(401L);
        Mockito.when(standbyVm.getUuid()).thenReturn("standby-uuid");
        Mockito.when(standbyVm.getInstanceName()).thenReturn("i-2-401-VM");
        Mockito.when(standbyVm.getDisplayName()).thenReturn("vm-secondary");
        return standbyVm;
    }

    private DiskOfferingVO mockDiskOffering(long id) {
        DiskOfferingVO diskOffering = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(diskOffering.getId()).thenReturn(id);
        return diskOffering;
    }

    private ServiceOfferingVO mockServiceOffering(long id) {
        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOffering.getId()).thenReturn(id);
        return serviceOffering;
    }

    private ServiceOfferingVO mockPrimaryServiceOffering() {
        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOffering.getCpu()).thenReturn(2);
        Mockito.when(serviceOffering.getSpeed()).thenReturn(1000);
        Mockito.when(serviceOffering.getRamSize()).thenReturn(4096);
        return serviceOffering;
    }
}
