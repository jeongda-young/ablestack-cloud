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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.FtctlActionAnswer;
import com.cloud.agent.api.FtctlActionCommand;
import com.cloud.agent.api.FtctlEventsAnswer;
import com.cloud.agent.api.FtctlEventsCommand;
import com.cloud.agent.api.FtctlStatusAnswer;
import com.cloud.agent.api.FtctlStatusCommand;
import com.cloud.agent.api.FtctlSyncAnswer;
import com.cloud.agent.api.FtctlSyncClusterCommand;
import com.cloud.agent.api.FtctlSyncProfileCommand;
import com.cloud.event.dao.EventDao;
import com.cloud.ftctl.dao.FtctlProtectionDao;
import com.cloud.ftctl.dao.FtctlProtectionVolumeDao;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.ScopeType;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.api.command.admin.ftctl.GetFtctlEventsCmd;
import org.apache.cloudstack.api.command.admin.ftctl.GetFtctlProtectionCmd;
import org.apache.cloudstack.api.command.admin.ftctl.RegisterFtctlProtectionCmd;
import org.apache.cloudstack.api.response.ftctl.FtctlActionResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlEventResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlEventsResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlProtectionResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class FtctlServiceAgentIntegrationTest {

    @Mock
    private VMInstanceDetailsDao vmInstanceDetailsDao;
    @Mock
    private UserVmDao userVmDao;
    @Mock
    private AgentManager agentManager;
    @Mock
    private HostDetailsDao hostDetailsDao;
    @Mock
    private HostDao hostDao;
    @Mock
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Mock
    private FtctlProtectionProvisioningService ftctlProtectionProvisioningService;
    @Mock
    private FtctlProtectionDao ftctlProtectionDao;
    @Mock
    private FtctlProtectionVolumeDao ftctlProtectionVolumeDao;
    @Mock
    private VolumeDao volumeDao;
    @Mock
    private EventDao eventDao;

    @InjectMocks
    private FtctlServiceImpl ftctlService;

    private UserVmVO userVm;
    private final Map<String, String> vmDetails = new HashMap<>();
    private final Map<String, String> hostDetails = new HashMap<>();
    private FtctlAgentIntegrationTestHelper agentHelper;

    @Before
    public void setup() throws Exception {
        userVm = Mockito.mock(UserVmVO.class);
        Mockito.when(userVm.getId()).thenReturn(101L);
        Mockito.when(userVm.getUuid()).thenReturn("vm-uuid");
        Mockito.when(userVm.getInstanceName()).thenReturn("vm-name");
        Mockito.when(userVm.getHostId()).thenReturn(201L);
        Mockito.when(userVm.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.lenient().when(userVm.getDataCenterId()).thenReturn(401L);
        Mockito.when(userVmDao.findById(101L)).thenReturn(userVm);

        Mockito.lenient().doAnswer(invocation -> {
            Long vmId = invocation.getArgument(0);
            String key = invocation.getArgument(1);
            String value = invocation.getArgument(2);
            vmDetails.put(vmId + ":" + key, value);
            return null;
        }).when(vmInstanceDetailsDao).addDetail(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean());

        Mockito.lenient().when(vmInstanceDetailsDao.findDetail(Mockito.anyLong(), Mockito.anyString())).thenAnswer(invocation -> {
            Long vmId = invocation.getArgument(0);
            String key = invocation.getArgument(1);
            String value = vmDetails.get(vmId + ":" + key);
            if (value == null) {
                return null;
            }
            VMInstanceDetailVO detail = Mockito.mock(VMInstanceDetailVO.class);
            Mockito.when(detail.getValue()).thenReturn(value);
            return detail;
        });

        Mockito.lenient().when(hostDetailsDao.findDetail(Mockito.anyLong(), Mockito.anyString())).thenAnswer(invocation -> {
            Long hostId = invocation.getArgument(0);
            String key = invocation.getArgument(1);
            String value = hostDetails.get(hostId + ":" + key);
            if (value == null) {
                return null;
            }
            DetailVO detail = Mockito.mock(DetailVO.class);
            Mockito.when(detail.getValue()).thenReturn(value);
            return detail;
        });

        Mockito.lenient().when(ftctlProtectionProvisioningService.prepareProtection(Mockito.any())).thenAnswer(invocation -> {
            FtctlProtectionProvisioningRequest request = invocation.getArgument(0);
            return new FtctlProtectionProvisioningContext(
                    FtctlProtectionProvisioningService.BACKEND_LIBVIRT_MANAGED,
                    FtctlProtectionProvisioningService.STATE_READY,
                    request.getSecondaryVmName(),
                    null);
        });

        agentHelper = new FtctlAgentIntegrationTestHelper();
        agentHelper.bind(agentManager);
    }

    @Test
    public void testRegisterFtctlProtectionServiceAgentIntegrationFlow() throws Exception {
        RegisterFtctlProtectionCmd cmd = buildRegisterCmd();
        HostVO localHost = mockHost(201L, 301L, "10.0.0.11");
        HostVO peerHost = mockHost(202L, 301L, "10.0.0.12");
        Mockito.when(hostDao.findById(201L)).thenReturn(localHost);
        Mockito.when(hostDao.findById(202L)).thenReturn(peerHost);
        hostDetails.put("201:ftctl.management.ip", "192.168.0.11");
        hostDetails.put("202:ftctl.libvirt.uri", "qemu+ssh://peer-ft/system");

        agentHelper.onCommand(FtctlSyncClusterCommand.class, (hostId, command) -> {
            Assert.assertEquals(Long.valueOf(201L), hostId);
            Assert.assertEquals("cluster-301", command.getClusterName());
            Assert.assertEquals("201", command.getLocalHostId());
            Assert.assertEquals("202", command.getPeerHostId());
            Assert.assertEquals("qemu+ssh://peer-ft/system", command.getPeerLibvirtUri());
            return new FtctlSyncAnswer(command, true, "OK", "ok", 0, "cluster-synced");
        });
        agentHelper.onCommand(FtctlSyncProfileCommand.class, (hostId, command) -> {
            Assert.assertEquals(Long.valueOf(201L), hostId);
            Assert.assertEquals("vm-name", command.getVmName());
            Assert.assertEquals("dr", command.getMode());
            Assert.assertEquals("vm-uuid", command.getProfileName());
            Assert.assertEquals("remote-nbd", command.getBackendMode());
            Assert.assertEquals("libvirt-managed", command.getProvisioningBackend());
            Assert.assertEquals("Ready", command.getProvisioningState());
            Assert.assertEquals("secondary-local", command.getTargetStorageScope());
            Assert.assertEquals("pool-uuid", command.getTargetStoragePoolId());
            Assert.assertEquals("pool-name", command.getTargetStoragePoolName());
            Assert.assertEquals("manual-block", command.getFencingPolicy());
            Assert.assertEquals("10.0.0.12:10809", command.getRemoteNbdExportAddr());
            return new FtctlSyncAnswer(command, true, "OK", "ok", 0, "profile-synced");
        });
        agentHelper.onCommand(FtctlActionCommand.class, (hostId, command) -> {
            Assert.assertEquals(Long.valueOf(201L), hostId);
            Assert.assertEquals(FtctlActionCommand.Action.PROTECT_START, command.getAction());
            Assert.assertEquals("vm-name", command.getVmName());
            Assert.assertEquals("dr", command.getMode());
            Assert.assertEquals("qemu+ssh://peer-ft/system", command.getPeerUri());
            Assert.assertEquals("remote-nbd", command.getContextParam("ftctl.backend.mode"));
            Assert.assertEquals("libvirt-managed", command.getContextParam("ftctl.provisioning.backend"));
            Assert.assertEquals("secondary-local", command.getContextParam("ftctl.target.storage.scope"));
            Assert.assertEquals("manual-block", command.getContextParam("ftctl.fencing.policy"));
            return new FtctlActionAnswer(command, true, "OK", FtctlActionCommand.Action.PROTECT_START, "accepted", 0, "protection job accepted");
        });
        agentHelper.onCommand(FtctlStatusCommand.class, (hostId, command) -> {
            Assert.assertEquals(Long.valueOf(201L), hostId);
            Assert.assertEquals("vm-name", command.getVmName());
            return new FtctlStatusAnswer(command, true, "OK", "ok", "vm-name",
                    "dr", "protected", "replicating", "primary", "running", "clear", "",
                    "2026-04-19T00:20:00+09:00", 0, 0);
        });

        FtctlProtectionResponse response = ftctlService.registerFtctlProtection(cmd);

        agentHelper.assertSequence(
                FtctlSyncClusterCommand.class,
                FtctlSyncProfileCommand.class,
                FtctlActionCommand.class,
                FtctlStatusCommand.class
        );
        List<FtctlAgentIntegrationTestHelper.DispatchRecord> records = agentHelper.getDispatchRecords();
        Assert.assertEquals(4, records.size());
        for (FtctlAgentIntegrationTestHelper.DispatchRecord record : records) {
            Assert.assertEquals(Long.valueOf(201L), record.getHostId());
        }

        Assert.assertEquals(Long.valueOf(101L), getFieldValue(response, "virtualMachineId"));
        Assert.assertEquals("true", getFieldValue(response, "enabled"));
        Assert.assertEquals("dr", getFieldValue(response, "mode"));
        Assert.assertEquals("remote-nbd", getFieldValue(response, "backendMode"));
        Assert.assertEquals("libvirt-managed", getFieldValue(response, "provisioningBackend"));
        Assert.assertEquals("Ready", getFieldValue(response, "provisioningState"));
        Assert.assertEquals("pool-uuid", getFieldValue(response, "targetStoragePoolId"));
        Assert.assertEquals("pool-name", getFieldValue(response, "targetStoragePoolName"));
        Assert.assertEquals("202", getFieldValue(response, "peerHostId"));
        Assert.assertEquals("host-202", getFieldValue(response, "peerHostName"));
        Assert.assertEquals("protected", getFieldValue(response, "protectionState"));
        Assert.assertEquals("replicating", getFieldValue(response, "transportState"));
        Assert.assertEquals("primary", getFieldValue(response, "activeSide"));
        Assert.assertEquals("running", getFieldValue(response, "adminState"));
        Assert.assertEquals("clear", getFieldValue(response, "fencingState"));

        Mockito.verify(vmInstanceDetailsDao).addDetail(101L, "ftctl.enabled", "true", true);
        Mockito.verify(vmInstanceDetailsDao).addDetail(101L, "ftctl.backend.mode", "remote-nbd", true);
        Mockito.verify(vmInstanceDetailsDao).addDetail(101L, "ftctl.provisioning.backend", "libvirt-managed", true);
        Mockito.verify(vmInstanceDetailsDao).addDetail(101L, "ftctl.provisioning.state", "Ready", true);
        Mockito.verify(vmInstanceDetailsDao).addDetail(101L, "ftctl.target.storage.pool.id", "pool-uuid", true);
        Mockito.verify(vmInstanceDetailsDao).addDetail(101L, "ftctl.target.storage.pool.name", "pool-name", true);
        Mockito.verify(vmInstanceDetailsDao).addDetail(101L, "ftctl.peer.host.id", "202", true);
        Mockito.verify(vmInstanceDetailsDao).addDetail(101L, "ftctl.last.protection.state", "protected", true);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testRegisterFtctlProtectionStopsAfterClusterSyncFailure() throws Exception {
        RegisterFtctlProtectionCmd cmd = buildRegisterCmd();
        HostVO localHost = mockHost(201L, 301L, "10.0.0.11");
        HostVO peerHost = mockHost(202L, 301L, "10.0.0.12");
        Mockito.when(hostDao.findById(201L)).thenReturn(localHost);
        Mockito.when(hostDao.findById(202L)).thenReturn(peerHost);

        agentHelper.onCommand(FtctlSyncClusterCommand.class, (hostId, command) ->
                new FtctlSyncAnswer(command, false, "cluster failed", "error", 1, "cluster failed"));

        try {
            ftctlService.registerFtctlProtection(cmd);
        } finally {
            agentHelper.assertSequence(FtctlSyncClusterCommand.class);
        }
    }

    @Test
    public void testExecuteFtctlActionServiceAgentIntegrationFlow() throws Exception {
        FtctlActionCommand.Action action = FtctlActionCommand.Action.FAILOVER;
        agentHelper.onCommand(FtctlActionCommand.class, (hostId, command) -> {
            Assert.assertEquals(Long.valueOf(201L), hostId);
            Assert.assertEquals(action, command.getAction());
            Assert.assertEquals("vm-name", command.getVmName());
            Assert.assertTrue(command.isForce());
            return new FtctlActionAnswer(command, true, "OK", action, "ok", 0, "failover complete");
        });
        agentHelper.onCommand(FtctlStatusCommand.class, (hostId, command) -> {
            Assert.assertEquals(Long.valueOf(201L), hostId);
            Assert.assertEquals("vm-name", command.getVmName());
            return new FtctlStatusAnswer(command, true, "OK", "ok", "vm-name",
                    "dr", "protected", "replicating", "secondary", "running", "clear", "",
                    "2026-04-19T00:40:00+09:00", 0, 1);
        });

        FtctlActionResponse response = ftctlService.executeFtctlAction(101L, action, false);

        agentHelper.assertSequence(FtctlActionCommand.class, FtctlStatusCommand.class);
        Assert.assertEquals(Long.valueOf(101L), getFieldValue(response, "virtualMachineId"));
        Assert.assertEquals("vm-name", getFieldValue(response, "vmName"));
        Assert.assertEquals("FAILOVER", getFieldValue(response, "action"));
        Assert.assertEquals("ok", getFieldValue(response, "result"));
        Assert.assertEquals(Integer.valueOf(0), getFieldValue(response, "exitCode"));
        Assert.assertEquals("failover complete", getFieldValue(response, "output"));
        Assert.assertEquals("secondary", getFieldValue(response, "activeSide"));
        Assert.assertEquals("running", getFieldValue(response, "adminState"));

        Mockito.verify(vmInstanceDetailsDao).addDetail(101L, "ftctl.last.active.side", "secondary", true);
        Mockito.verify(vmInstanceDetailsDao).addDetail(101L, "ftctl.last.protection.state", "protected", true);
    }

    @Test
    public void testGetFtctlProtectionReadsCachedRuntimeState() throws Exception {
        vmDetails.put("101:ftctl.enabled", "true");
        vmDetails.put("101:ftctl.mode", "dr");
        vmDetails.put("101:ftctl.backend.mode", "remote-nbd");
        vmDetails.put("101:ftctl.peer.host.id", "202");
        vmDetails.put("101:ftctl.last.protection.state", "protected");
        vmDetails.put("101:ftctl.last.transport.state", "replicating");
        vmDetails.put("101:ftctl.last.active.side", "primary");
        vmDetails.put("101:ftctl.last.admin.state", "running");
        vmDetails.put("101:ftctl.last.fencing.state", "clear");

        GetFtctlProtectionCmd cmd = new GetFtctlProtectionCmd();
        setField(cmd, "virtualMachineId", 101L);

        FtctlProtectionResponse response = ftctlService.getFtctlProtection(cmd);

        agentHelper.assertSequence();
        Assert.assertEquals(Long.valueOf(101L), getFieldValue(response, "virtualMachineId"));
        Assert.assertEquals("true", getFieldValue(response, "enabled"));
        Assert.assertEquals("dr", getFieldValue(response, "mode"));
        Assert.assertEquals("remote-nbd", getFieldValue(response, "backendMode"));
        Assert.assertEquals("202", getFieldValue(response, "peerHostId"));
        Assert.assertEquals("protected", getFieldValue(response, "protectionState"));
        Assert.assertEquals("replicating", getFieldValue(response, "transportState"));
        Assert.assertEquals("primary", getFieldValue(response, "activeSide"));
        Assert.assertEquals("running", getFieldValue(response, "adminState"));
        Assert.assertEquals("clear", getFieldValue(response, "fencingState"));
    }

    @Test
    public void testGetFtctlEventsReadsQemuRuntimeEvents() throws Exception {
        GetFtctlEventsCmd cmd = new GetFtctlEventsCmd();
        setField(cmd, "virtualMachineId", 101L);
        setField(cmd, "limit", 5);

        agentHelper.onCommand(FtctlEventsCommand.class, (hostId, command) -> {
            Assert.assertEquals(Long.valueOf(201L), hostId);
            Assert.assertEquals("vm-name", command.getVmName());
            Assert.assertEquals(Integer.valueOf(5), command.getLimit());
            String itemsJson = "[" +
                    "{\"ts\":\"2026-05-10T00:00:01+09:00\",\"vm\":\"vm-name\",\"stage\":\"runtime\",\"event\":\"failover.start\",\"result\":\"ok\",\"details\":{\"operator\":\"admin\"}}," +
                    "{\"ts\":\"2026-05-10T00:00:02+09:00\",\"vm\":\"vm-name\",\"stage\":\"blockcopy\",\"event\":\"reverse_sync.progress\",\"result\":\"ok\",\"details\":{\"percent\":80.0,\"copied_bytes\":80,\"total_bytes\":100,\"ready\":false}}" +
                    "]";
            return new FtctlEventsAnswer(command, true, "OK", "ok", "vm-name", 2, itemsJson);
        });

        FtctlEventsResponse response = ftctlService.getFtctlEvents(cmd);

        agentHelper.assertSequence(FtctlEventsCommand.class);
        Assert.assertEquals(Long.valueOf(101L), response.getVirtualMachineId());
        Assert.assertEquals("vm-name", response.getVmName());
        Assert.assertEquals("ok", response.getResult());
        Assert.assertEquals(Integer.valueOf(2), response.getCount());
        Assert.assertEquals(Double.valueOf(80.0), response.getSyncProgressPercent());
        Assert.assertEquals("reverse", response.getSyncDirection());

        List<FtctlEventResponse> events = response.getEvents();
        Assert.assertEquals(2, events.size());
        Assert.assertEquals("runtime", events.get(0).getStage());
        Assert.assertEquals("failover.start", events.get(0).getEvent());
        Assert.assertEquals("ok", events.get(0).getResult());
        Assert.assertTrue(events.get(0).getDetails().contains("operator"));
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to set field " + fieldName, e);
        }
    }

    private Object getFieldValue(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read field " + fieldName, e);
        }
    }

    private RegisterFtctlProtectionCmd buildRegisterCmd() {
        RegisterFtctlProtectionCmd cmd = new RegisterFtctlProtectionCmd();
        setField(cmd, "virtualMachineId", 101L);
        setField(cmd, "mode", "dr");
        setField(cmd, "backendMode", "remote-nbd");
        setField(cmd, "targetStorageScope", "host");
        setField(cmd, "targetStoragePoolId", 501L);
        setField(cmd, "provisioningBackend", FtctlProtectionProvisioningService.BACKEND_LIBVIRT_MANAGED);
        setField(cmd, "fencingPolicy", "manual-block");
        setField(cmd, "peerHostId", 202L);
        setField(cmd, "secondaryVmName", "vm-name-secondary");
        setField(cmd, "secondaryTargetDir", "/data/secondary");
        setField(cmd, "remoteNbdExportAddr", "10.0.0.12:10809");
        StoragePoolVO storagePool = mockStoragePool();
        Mockito.lenient().when(primaryDataStoreDao.findById(501L)).thenReturn(storagePool);
        return cmd;
    }

    private StoragePoolVO mockStoragePool() {
        StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(storagePool.getUuid()).thenReturn("pool-uuid");
        Mockito.when(storagePool.getName()).thenReturn("pool-name");
        Mockito.when(storagePool.getDataCenterId()).thenReturn(401L);
        Mockito.when(storagePool.getScope()).thenReturn(ScopeType.HOST);
        return storagePool;
    }

    private HostVO mockHost(Long id, Long clusterId, String privateIp) {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(id);
        Mockito.when(host.getName()).thenReturn(String.format("host-%s", id));
        Mockito.when(host.getClusterId()).thenReturn(clusterId);
        Mockito.when(host.getPrivateIpAddress()).thenReturn(privateIp);
        Mockito.when(host.getType()).thenReturn(Host.Type.Routing);
        Mockito.when(host.getHypervisorType()).thenReturn(HypervisorType.KVM);
        return host;
    }
}
