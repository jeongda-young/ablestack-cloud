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
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.apache.cloudstack.api.response.GuestNetworkStateResponse;
import org.apache.cloudstack.api.response.GuestNetworkSummaryResponse;
import org.apache.cloudstack.context.CallContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VmGuestNetworkStateVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VmGuestNetworkStateDao;

@RunWith(MockitoJUnitRunner.class)
public class VmGuestNetworkApiServiceImplTest {
    @Mock
    private VmGuestNetworkStateDao stateDao;
    @Mock
    private UserVmDao userVmDao;
    @Mock
    private AccountManager accountManager;

    private VmGuestNetworkApiServiceImpl service;

    @Before
    public void setUp() {
        service = new VmGuestNetworkApiServiceImpl();
        ReflectionTestUtils.setField(service, "stateDao", stateDao);
        ReflectionTestUtils.setField(service, "userVmDao", userVmDao);
        ReflectionTestUtils.setField(service, "accountManager", accountManager);
    }

    @Test
    public void testGetStateChecksAccessAndMapsPersistedSnapshot() {
        UserVmVO vm = mock(UserVmVO.class);
        Account caller = mock(Account.class);
        CallContext callContext = mock(CallContext.class);
        VmGuestNetworkStateVO snapshot = snapshot(41L, "OK");
        when(vm.getUuid()).thenReturn("vm-uuid");
        when(userVmDao.findById(41L)).thenReturn(vm);
        when(stateDao.findByVmId(41L)).thenReturn(snapshot);
        when(callContext.getCallingAccount()).thenReturn(caller);

        try (MockedStatic<CallContext> context = Mockito.mockStatic(CallContext.class)) {
            context.when(CallContext::current).thenReturn(callContext);
            GuestNetworkStateResponse response = service.getState(41L);

            assertEquals("vm-uuid", response.getVirtualMachineId());
            assertEquals("OK", response.getStatus());
            assertEquals(1, response.getInterfaces().size());
            assertEquals(2, response.getInterfaces().get(0).getAddresses().size());
            assertEquals("IPv4", response.getInterfaces().get(0).getAddresses().get(0).getFamily());
            assertEquals("PRIMARY", response.getInterfaces().get(0).getAddresses().get(0).getRole());
            assertEquals(true, response.getInterfaces().get(0).getAddresses().get(0).isRepresentative());
            assertEquals("IPv6", response.getInterfaces().get(0).getAddresses().get(1).getFamily());
            assertEquals(1, response.getRoutes().size());
            assertEquals("IPv4", response.getRoutes().get(0).getFamily());
            assertEquals(Integer.valueOf(0), response.getRoutes().get(0).getPrefix());
            assertEquals("10.10.22.1", response.getRoutes().get(0).getGateway());
            assertEquals(true, response.getRoutes().get(0).isDefaultRoute());
            assertEquals("resolvectl", response.getDns().getSource());
            assertEquals(true, response.getDns().isUpstreamServersKnown());
            assertEquals(Collections.singletonList("10.10.22.1"),
                    response.getDns().getServers());
            assertEquals(1, response.getDns().getConfigurations().size());
            assertEquals("eth0", response.getDns().getConfigurations().get(0).getInterfaceName());
            assertEquals("IPv4",
                    response.getDns().getConfigurations().get(0).getServers().get(0).getFamily());
            assertEquals(false,
                    response.getDns().getConfigurations().get(0).getServers().get(0).isLocalStub());
            verify(accountManager).checkAccess(eq(caller), any(), eq(true), eq(vm));
            verify(stateDao).findByVmId(41L);
        }
    }

    @Test
    public void testListSummariesUsesOneBatchReadAndIncludesAllIpv4AndIpv6() {
        VmGuestNetworkStateVO snapshot = snapshot(41L, "STALE");
        when(stateDao.listByVmIds(Arrays.asList(41L, 42L)))
                .thenReturn(Collections.singletonList(snapshot));

        Map<Long, GuestNetworkSummaryResponse> responses =
                service.listSummaries(Arrays.asList(41L, 42L));

        assertEquals(Arrays.asList("10.10.22.10/24"), responses.get(41L).getIpv4Addresses());
        assertEquals(Arrays.asList("2001:db8::10/64"), responses.get(41L).getIpv6Addresses());
        assertEquals("STALE", responses.get(41L).getStatus());
        assertEquals(null, responses.get(41L).getRepresentativeAddress());
        assertEquals("NOT_COLLECTED", responses.get(42L).getStatus());
        verify(stateDao).listByVmIds(Arrays.asList(41L, 42L));
    }

    @Test
    public void testFreshSummaryPublishesOnlyQgaRepresentativeAddress() {
        GuestNetworkSummaryResponse response = service.toSummaryResponse(snapshot(41L, "OK"));

        assertEquals("10.10.22.10", response.getRepresentativeAddress());
        assertEquals(Integer.valueOf(24), response.getRepresentativePrefix());
        assertEquals("IPv4", response.getRepresentativeFamily());
        assertEquals("QGA_LINUX_ADDRESS_FLAGS", response.getRepresentativeSource());
    }

    @Test
    public void testMalformedPayloadReturnsMetadataWithoutFailingRequest() {
        VmGuestNetworkStateVO snapshot = snapshot(41L, "PARTIAL");
        snapshot.setPayload("{malformed");

        GuestNetworkSummaryResponse response = service.toSummaryResponse(snapshot);

        assertEquals("PARTIAL", response.getStatus());
        assertEquals(0, response.getInterfaceCount());
        assertEquals(Collections.emptyList(), response.getIpv4Addresses());
        assertEquals(Collections.emptyList(), response.getIpv6Addresses());
    }

    @Test
    public void testReadServiceHasNoAgentManagerDependency() {
        for (Field field : VmGuestNetworkApiServiceImpl.class.getDeclaredFields()) {
            assertFalse("Read API must not contact the Agent", AgentManager.class.isAssignableFrom(field.getType()));
        }
    }

    private VmGuestNetworkStateVO snapshot(long vmId, String status) {
        Date observed = new Date(1000L);
        VmGuestNetworkStateVO snapshot = new VmGuestNetworkStateVO(vmId, observed);
        snapshot.setStatus(status);
        snapshot.setLastSuccessAt(observed);
        snapshot.setPayload("{"
                + "\"schemaVersion\":2,"
                + "\"interfaces\":[{"
                + "\"name\":\"eth0\","
                + "\"hardwareAddress\":\"52:54:00:12:34:56\","
                + "\"cloudNicId\":\"nic-uuid\","
                + "\"loopback\":false,"
                + "\"addresses\":["
                + "{\"family\":\"IPv4\",\"address\":\"10.10.22.10\",\"prefix\":24,"
                + "\"scope\":\"private\",\"role\":\"PRIMARY\","
                + "\"roleSource\":\"QGA_LINUX_ADDRESS_FLAGS\",\"representative\":true},"
                + "{\"family\":\"IPv6\",\"address\":\"2001:db8::10\",\"prefix\":64,\"scope\":\"global\"}"
                + "]}],"
                + "\"routes\":[{\"family\":\"ipv4\",\"destination\":\"0.0.0.0\","
                + "\"prefix\":0,\"gateway\":\"10.10.22.1\",\"interfaceName\":\"eth0\","
                + "\"metric\":100,\"table\":\"main\",\"protocol\":\"dhcp\","
                + "\"scope\":\"global\",\"defaultRoute\":true}],"
                + "\"dns\":{\"source\":\"resolvectl\",\"upstreamServersKnown\":true,"
                + "\"servers\":[\"10.10.22.1\"],\"searchDomains\":[\"example.internal\"],"
                + "\"configurations\":[{\"interfaceName\":\"eth0\",\"source\":\"resolvectl\","
                + "\"global\":false,\"servers\":[{\"address\":\"10.10.22.1\","
                + "\"family\":\"ipv4\",\"localStub\":false}],"
                + "\"domains\":[{\"domain\":\"example.internal\",\"routingOnly\":false}]}]},"
                + "\"sectionStatuses\":{\"interfaces\":{\"status\":\"OK\"},"
                + "\"dns\":{\"status\":\"OK\"}}"
                + "}");
        return snapshot;
    }
}
