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
package com.cloud.vm.guestnetwork;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.GetVmGuestNetworkStateAnswer;
import com.cloud.agent.api.GetVmGuestNetworkStateCommand;
import com.cloud.agent.api.VmGuestNetworkSectionStatus;
import com.cloud.agent.api.VmGuestNetworkState;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.db.GlobalLock;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class VmGuestNetworkCollectorTest {
    private static final long HOST_ID = 5L;

    @Mock
    private AgentManager agentManager;
    @Mock
    private VMInstanceDao vmInstanceDao;
    @Mock
    private NicDao nicDao;
    @Mock
    private VmGuestNetworkStateService stateService;

    private TestCollector collector;

    @Before
    public void setUp() {
        collector = new TestCollector(new VmGuestNetworkCollectionPolicy());
        ReflectionTestUtils.setField(collector, "agentManager", agentManager);
        ReflectionTestUtils.setField(collector, "vmInstanceDao", vmInstanceDao);
        ReflectionTestUtils.setField(collector, "nicDao", nicDao);
        ReflectionTestUtils.setField(collector, "stateService", stateService);
    }

    @Test
    public void testDisabledStartCreatesIdleSchedulerAndMakesNoCalls() {
        collector.enabled = false;

        assertTrue(collector.start());
        collector.runCycle();

        assertNotNull(ReflectionTestUtils.getField(collector, "scheduler"));
        assertNotNull(ReflectionTestUtils.getField(collector, "collectionExecutor"));
        verifyNoInteractions(agentManager, vmInstanceDao, nicDao, stateService);
        assertTrue(collector.stop());
    }

    @Test
    public void testContendedGlobalLockSkipsVmAndAgentWork() {
        GlobalLock scanLock = org.mockito.Mockito.mock(GlobalLock.class);
        when(scanLock.lock(1)).thenReturn(false);
        collector.scanLock = scanLock;

        collector.runCycle();

        verify(scanLock).releaseRef();
        verifyNoInteractions(agentManager, vmInstanceDao, nicDao, stateService);
    }

    @Test
    public void testSuccessUsesVmScopedNicMapAndCapabilityCache() {
        VMInstanceVO vm = vm(1L, "vm-one", HypervisorType.KVM);
        NicVO nic = org.mockito.Mockito.mock(NicVO.class);
        when(nic.getMacAddress()).thenReturn("52-54-00-AA-BB-01");
        when(nic.getUuid()).thenReturn("nic-uuid");
        when(nicDao.listByVmId(1L)).thenReturn(Collections.singletonList(nic));
        when(agentManager.easySend(eq(HOST_ID), any(GetVmGuestNetworkStateCommand.class)))
                .thenAnswer(invocation -> successfulAnswer(
                        invocation.getArgument(1), "vm-one", true));

        collector.collectBatch(HOST_ID, Collections.singletonList(vm));
        collector.collectBatch(HOST_ID, Collections.singletonList(vm));

        ArgumentCaptor<GetVmGuestNetworkStateCommand> captor =
                ArgumentCaptor.forClass(GetVmGuestNetworkStateCommand.class);
        verify(agentManager, times(2)).easySend(eq(HOST_ID), captor.capture());
        List<GetVmGuestNetworkStateCommand> commands = captor.getAllValues();
        assertEquals("nic-uuid", commands.get(0).getCloudNicIdsForVm("vm-one")
                .get("52:54:00:aa:bb:01"));
        assertTrue(commands.get(0).shouldCollectInterfaces("vm-one"));
        assertTrue(commands.get(0).shouldCollectRoutes("vm-one"));
        assertTrue(commands.get(0).shouldCollectDns("vm-one"));
        assertTrue(commands.get(1).hasCachedInterfaceCapability("vm-one"));
        assertEquals(false, commands.get(0).isExecFallbackEnabled());
        verify(stateService, times(2)).persistSuccess(eq(1L), any(), any());
        verify(stateService, never()).persistFailure(eq(1L), any(), any(), any(), any());
    }

    @Test
    public void testHostCycleLimitBoundsVmCommandBatch() {
        VMInstanceVO first = vm(1L, "vm-one", HypervisorType.KVM);
        VMInstanceVO second = vm(2L, "vm-two", HypervisorType.KVM);
        VMInstanceVO third = vm(3L, "vm-three", HypervisorType.KVM);
        collector.maxConcurrentVms = 2;
        collector.maxVmsPerCycle = 2;
        when(nicDao.listByVmId(any(Long.class))).thenReturn(Collections.emptyList());
        when(agentManager.easySend(eq(HOST_ID), any(GetVmGuestNetworkStateCommand.class)))
                .thenAnswer(invocation -> successfulAnswerForCommand(invocation.getArgument(1)));

        collector.collectHost(HOST_ID, Arrays.asList(first, second, third));

        ArgumentCaptor<GetVmGuestNetworkStateCommand> captor =
                ArgumentCaptor.forClass(GetVmGuestNetworkStateCommand.class);
        verify(agentManager).easySend(eq(HOST_ID), captor.capture());
        assertEquals(Arrays.asList("vm-one", "vm-two"), captor.getValue().getVmNames());
        verify(stateService, times(2)).persistSuccess(any(Long.class), any(), any());
    }

    @Test
    public void testHostCycleLimitRotatesAcrossDueVms() {
        VMInstanceVO first = vm(1L, "vm-one", HypervisorType.KVM);
        VMInstanceVO second = vm(2L, "vm-two", HypervisorType.KVM);
        VMInstanceVO third = vm(3L, "vm-three", HypervisorType.KVM);
        collector.maxVmsPerCycle = 1;
        when(nicDao.listByVmId(any(Long.class))).thenReturn(Collections.emptyList());
        when(agentManager.easySend(eq(HOST_ID), any(GetVmGuestNetworkStateCommand.class)))
                .thenAnswer(invocation -> successfulAnswerForCommand(invocation.getArgument(1)));

        collector.collectHost(HOST_ID, Arrays.asList(first, second, third));
        collector.collectHost(HOST_ID, Arrays.asList(first, second, third));
        collector.collectHost(HOST_ID, Arrays.asList(first, second, third));

        ArgumentCaptor<GetVmGuestNetworkStateCommand> captor =
                ArgumentCaptor.forClass(GetVmGuestNetworkStateCommand.class);
        verify(agentManager, times(3)).easySend(eq(HOST_ID), captor.capture());
        assertEquals(Collections.singletonList("vm-one"), captor.getAllValues().get(0).getVmNames());
        assertEquals(Collections.singletonList("vm-two"), captor.getAllValues().get(1).getVmNames());
        assertEquals(Collections.singletonList("vm-three"), captor.getAllValues().get(2).getVmNames());
    }

    @Test
    public void testLargeFleetCollectionRemainsBoundedByHostCycleAndBatchLimits() {
        List<VMInstanceVO> vms = new ArrayList<>();
        for (int index = 1; index <= 1000; index++) {
            vms.add(vm(index, "vm-" + index, HypervisorType.KVM));
        }
        collector.maxConcurrentVms = 10;
        collector.maxVmsPerCycle = 50;
        when(nicDao.listByVmId(any(Long.class))).thenReturn(Collections.emptyList());
        when(agentManager.easySend(eq(HOST_ID), any(GetVmGuestNetworkStateCommand.class)))
                .thenAnswer(invocation -> successfulAnswerForCommand(invocation.getArgument(1)));

        collector.collectHost(HOST_ID, vms);

        ArgumentCaptor<GetVmGuestNetworkStateCommand> captor =
                ArgumentCaptor.forClass(GetVmGuestNetworkStateCommand.class);
        verify(agentManager, times(5)).easySend(eq(HOST_ID), captor.capture());
        assertEquals(5, captor.getAllValues().size());
        assertTrue(captor.getAllValues().stream()
                .allMatch(command -> command.getVmNames().size() == 10));
        assertEquals("vm-1", captor.getAllValues().get(0).getVmNames().get(0));
        assertEquals("vm-50", captor.getAllValues().get(4).getVmNames().get(9));
        verify(stateService, times(50)).persistSuccess(any(Long.class), any(), any());
    }

    @Test
    public void testOneVmPersistenceFailureDoesNotSkipOtherVm() {
        VMInstanceVO first = vm(1L, "vm-one", HypervisorType.KVM);
        VMInstanceVO second = vm(2L, "vm-two", HypervisorType.KVM);
        when(nicDao.listByVmId(any(Long.class))).thenReturn(Collections.emptyList());
        when(agentManager.easySend(eq(HOST_ID), any(GetVmGuestNetworkStateCommand.class)))
                .thenAnswer(invocation -> successfulAnswerForCommand(invocation.getArgument(1)));
        doThrow(new IllegalArgumentException("payload too large"))
                .when(stateService).persistSuccess(eq(1L), any(), any());

        collector.collectBatch(HOST_ID, Arrays.asList(first, second));

        verify(stateService).persistSuccess(eq(1L), any(), any());
        verify(stateService).persistFailure(eq(1L), any(), eq("PERSISTENCE_FAILED"),
                eq("payload too large"), any());
        verify(stateService).persistSuccess(eq(2L), any(), any());
    }

    @Test
    public void testSelectionExcludesNonKvmVm() {
        VMInstanceVO kvm = vm(1L, "vm-one", HypervisorType.KVM);
        VMInstanceVO vmware = vm(2L, "vm-two", HypervisorType.VMware);
        when(vmInstanceDao.listByTypeAndState(VirtualMachine.Type.User, VirtualMachine.State.Running))
                .thenReturn(Arrays.asList(kvm, vmware));

        Map<Long, List<VMInstanceVO>> selected = collector.selectDueVmsByHost(1000L);

        assertEquals(Collections.singletonList(kvm), selected.get(HOST_ID));
    }

    @Test
    public void testSelectionHonorsHostAndZoneScope() {
        VMInstanceVO inScope = vm(1L, "vm-one", HypervisorType.KVM);
        collector.hostIdScope = Long.toString(HOST_ID);
        collector.zoneIdScope = "1";
        when(vmInstanceDao.listByTypeAndState(VirtualMachine.Type.User, VirtualMachine.State.Running))
                .thenReturn(Collections.singletonList(inScope));

        assertEquals(Collections.singletonList(inScope),
                collector.selectDueVmsByHost(1000L).get(HOST_ID));

        collector.hostIdScope = "invalid";
        assertTrue(collector.selectDueVmsByHost(1000L).isEmpty());

        collector.hostIdScope = Long.toString(HOST_ID);
        collector.zoneIdScope = "2";
        assertTrue(collector.selectDueVmsByHost(1000L).isEmpty());
    }

    @Test
    public void testRouteOnlyCollectionSkipsNicDaoAndInterfaceRequest() {
        VMInstanceVO vm = vm(1L, "vm-one", HypervisorType.KVM);
        collector.markInterfaceNotDue(1L);
        collector.markDnsNotDue(1L);
        when(agentManager.easySend(eq(HOST_ID), any(GetVmGuestNetworkStateCommand.class)))
                .thenAnswer(invocation -> successfulAnswer(
                        invocation.getArgument(1), "vm-one", false));

        collector.collectBatch(HOST_ID, Collections.singletonList(vm));

        ArgumentCaptor<GetVmGuestNetworkStateCommand> captor =
                ArgumentCaptor.forClass(GetVmGuestNetworkStateCommand.class);
        verify(agentManager).easySend(eq(HOST_ID), captor.capture());
        assertEquals(false, captor.getValue().shouldCollectInterfaces("vm-one"));
        assertTrue(captor.getValue().shouldCollectRoutes("vm-one"));
        assertEquals(false, captor.getValue().shouldCollectDns("vm-one"));
        verifyNoInteractions(nicDao);
    }

    @Test
    public void testDnsOnlyCollectionSkipsNicDaoInterfaceAndRouteRequests() {
        VMInstanceVO vm = vm(1L, "vm-one", HypervisorType.KVM);
        collector.markInterfaceNotDue(1L);
        collector.markRouteNotDue(1L);
        when(agentManager.easySend(eq(HOST_ID), any(GetVmGuestNetworkStateCommand.class)))
                .thenAnswer(invocation -> successfulAnswer(
                        invocation.getArgument(1), "vm-one", false));

        collector.collectBatch(HOST_ID, Collections.singletonList(vm));

        ArgumentCaptor<GetVmGuestNetworkStateCommand> captor =
                ArgumentCaptor.forClass(GetVmGuestNetworkStateCommand.class);
        verify(agentManager).easySend(eq(HOST_ID), captor.capture());
        assertEquals(false, captor.getValue().shouldCollectInterfaces("vm-one"));
        assertEquals(false, captor.getValue().shouldCollectRoutes("vm-one"));
        assertTrue(captor.getValue().shouldCollectDns("vm-one"));
        verifyNoInteractions(nicDao);
    }

    @Test
    public void testPersistenceMergeDoesNotExtendNotDueSectionBackoff() {
        VMInstanceVO vm = vm(1L, "vm-one", HypervisorType.KVM);
        collector.markRouteNotDue(1L);
        collector.markDnsNotDue(1L);
        long nextRouteAt = collector.getNextRouteAt(1L);
        long nextDnsAt = collector.getNextDnsAt(1L);
        when(agentManager.easySend(eq(HOST_ID), any(GetVmGuestNetworkStateCommand.class)))
                .thenAnswer(invocation -> {
                    VmGuestNetworkState state = successState("vm-one", true);
                    state.putSectionStatus("interfaces", new VmGuestNetworkSectionStatus("OK"));
                    state.putSectionStatus("routes", new VmGuestNetworkSectionStatus("NOT_DUE"));
                    state.putSectionStatus("dns", new VmGuestNetworkSectionStatus("NOT_DUE"));
                    return new GetVmGuestNetworkStateAnswer(invocation.getArgument(1),
                            Collections.singletonMap("vm-one", state), Collections.emptyMap());
                });
        doAnswer(invocation -> {
            VmGuestNetworkState persisted = invocation.getArgument(1);
            persisted.putSectionStatus("routes", new VmGuestNetworkSectionStatus("UNSUPPORTED"));
            persisted.putSectionStatus("dns", new VmGuestNetworkSectionStatus("UNSUPPORTED"));
            return null;
        }).when(stateService).persistSuccess(eq(1L), any(), any());

        collector.collectBatch(HOST_ID, Collections.singletonList(vm));

        assertEquals(nextRouteAt, collector.getNextRouteAt(1L));
        assertEquals(nextDnsAt, collector.getNextDnsAt(1L));
    }

    @Test
    public void testStructuredUnavailableStateUsesSectionAwarePersistence() {
        VMInstanceVO vm = vm(1L, "vm-one", HypervisorType.KVM);
        when(nicDao.listByVmId(1L)).thenReturn(Collections.emptyList());
        when(agentManager.easySend(eq(HOST_ID), any(GetVmGuestNetworkStateCommand.class)))
                .thenAnswer(invocation -> {
                    VmGuestNetworkState state = new VmGuestNetworkState("vm-one");
                    state.setStatus("UNAVAILABLE");
                    state.setObservedAt(System.currentTimeMillis());
                    state.putSectionStatus("interfaces",
                            new VmGuestNetworkSectionStatus("NOT_DUE"));
                    state.putSectionStatus("routes",
                            new VmGuestNetworkSectionStatus("UNAVAILABLE", "ip denied"));
                    state.putSectionStatus("dns",
                            new VmGuestNetworkSectionStatus("NOT_DUE"));
                    return new GetVmGuestNetworkStateAnswer(invocation.getArgument(1),
                            Collections.singletonMap("vm-one", state),
                            Collections.singletonMap("vm-one", "route collection failed"));
                });

        collector.collectBatch(HOST_ID, Collections.singletonList(vm));

        verify(stateService).persistSuccess(eq(1L), any(), any());
        verify(stateService, never()).persistFailure(eq(1L), any(), any(), any(), any());
    }

    @Test
    public void testMissingStateStillUsesGlobalFailurePersistence() {
        VMInstanceVO vm = vm(1L, "vm-one", HypervisorType.KVM);
        when(nicDao.listByVmId(1L)).thenReturn(Collections.emptyList());
        when(agentManager.easySend(eq(HOST_ID), any(GetVmGuestNetworkStateCommand.class)))
                .thenAnswer(invocation -> new GetVmGuestNetworkStateAnswer(
                        invocation.getArgument(1), Collections.emptyMap(),
                        Collections.singletonMap("vm-one", "transport failed")));

        collector.collectBatch(HOST_ID, Collections.singletonList(vm));

        verify(stateService).persistFailure(eq(1L), eq(null), eq("COLLECTION_FAILED"),
                eq("transport failed"), any());
        verify(stateService, never()).persistSuccess(eq(1L), any(), any());
    }

    private GetVmGuestNetworkStateAnswer successfulAnswer(
            GetVmGuestNetworkStateCommand command, String vmName, boolean includeCapability) {
        VmGuestNetworkState state = successState(vmName, includeCapability);
        return new GetVmGuestNetworkStateAnswer(command,
                Collections.singletonMap(vmName, state), Collections.emptyMap());
    }

    private GetVmGuestNetworkStateAnswer successfulAnswerForCommand(
            GetVmGuestNetworkStateCommand command) {
        Map<String, VmGuestNetworkState> states = new LinkedHashMap<>();
        command.getVmNames().forEach(vmName -> states.put(vmName, successState(vmName, true)));
        return new GetVmGuestNetworkStateAnswer(command, states, Collections.emptyMap());
    }

    private VmGuestNetworkState successState(String vmName, boolean includeCapability) {
        VmGuestNetworkState state = new VmGuestNetworkState(vmName);
        state.setStatus("OK");
        state.setObservedAt(System.currentTimeMillis());
        if (includeCapability) {
            state.putCapability("guest-network-get-interfaces", true);
        }
        return state;
    }

    private VMInstanceVO vm(long id, String instanceName, HypervisorType hypervisorType) {
        VMInstanceVO vm = org.mockito.Mockito.mock(VMInstanceVO.class);
        when(vm.getId()).thenReturn(id);
        when(vm.getInstanceName()).thenReturn(instanceName);
        when(vm.getHypervisorType()).thenReturn(hypervisorType);
        when(vm.getHostId()).thenReturn(HOST_ID);
        when(vm.getDataCenterId()).thenReturn(1L);
        return vm;
    }

    private static final class TestCollector extends VmGuestNetworkCollector {
        private final VmGuestNetworkCollectionPolicy policy;
        private boolean enabled = true;
        private int maxConcurrentVms = 1;
        private int maxVmsPerCycle = 50;
        private GlobalLock scanLock;
        private String hostIdScope = "";
        private String zoneIdScope = "";

        TestCollector(VmGuestNetworkCollectionPolicy policy) {
            super(policy);
            this.policy = policy;
        }

        void markInterfaceNotDue(long vmId) {
            policy.recordInterfaceSuccess(vmId, System.currentTimeMillis(), 120, 0);
        }

        void markRouteNotDue(long vmId) {
            policy.recordRouteSuccess(vmId, System.currentTimeMillis(), 600, 0);
        }

        void markDnsNotDue(long vmId) {
            policy.recordDnsSuccess(vmId, System.currentTimeMillis(), 600, 0);
        }

        long getNextRouteAt(long vmId) {
            return policy.getNextRouteAt(vmId);
        }

        long getNextDnsAt(long vmId) {
            return policy.getNextDnsAt(vmId);
        }

        @Override
        protected boolean isEnabled() {
            return enabled;
        }

        @Override
        protected String getHostIdScope() {
            return hostIdScope;
        }

        @Override
        protected String getZoneIdScope() {
            return zoneIdScope;
        }

        @Override
        protected int getInterfaceInterval() {
            return 120;
        }

        @Override
        protected int getDnsInterval() {
            return 600;
        }

        @Override
        protected int getRouteInterval() {
            return 600;
        }

        @Override
        protected int getJitterPercent() {
            return 0;
        }

        @Override
        protected int getMaxConcurrentVmsPerHost() {
            return maxConcurrentVms;
        }

        @Override
        protected int getMaxVmsPerHostCycle() {
            return maxVmsPerCycle;
        }

        @Override
        protected int getCapabilityCacheTtl() {
            return 600;
        }

        @Override
        protected GlobalLock getCollectorGlobalLock() {
            return scanLock == null ? super.getCollectorGlobalLock() : scanLock;
        }
    }
}
