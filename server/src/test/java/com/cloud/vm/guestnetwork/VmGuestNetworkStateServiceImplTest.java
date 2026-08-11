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
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.api.VmGuestDnsConfig;
import com.cloud.agent.api.VmGuestDnsServer;
import com.cloud.agent.api.VmGuestIpAddress;
import com.cloud.agent.api.VmGuestNetworkInterface;
import com.cloud.agent.api.VmGuestNetworkSectionStatus;
import com.cloud.agent.api.VmGuestNetworkState;
import com.cloud.agent.api.VmGuestRoute;
import com.cloud.serializer.GsonHelper;
import com.cloud.vm.VmGuestNetworkStateVO;
import com.cloud.vm.dao.VmGuestNetworkStateDao;
import com.cloud.vm.guestnetwork.VmGuestNetworkPayloadCanonicalizer.CanonicalPayload;
import com.cloud.vm.guestnetwork.VmGuestNetworkStateService.PersistResult;

@RunWith(MockitoJUnitRunner.class)
public class VmGuestNetworkStateServiceImplTest {
    @Mock
    private VmGuestNetworkStateDao stateDao;

    private VmGuestNetworkPayloadCanonicalizer canonicalizer;
    private VmGuestNetworkStateServiceImpl service;

    @Before
    public void setUp() {
        canonicalizer = new VmGuestNetworkPayloadCanonicalizer();
        service = new VmGuestNetworkStateServiceImpl(canonicalizer);
        ReflectionTestUtils.setField(service, "stateDao", stateDao);
    }

    @Test
    public void testUnchangedPayloadUsesMetadataOnlyUpdate() {
        VmGuestNetworkState state = populatedState();
        CanonicalPayload payload = canonicalizer.canonicalize(state);
        VmGuestNetworkStateVO existing = existingState(payload);
        when(stateDao.findByVmId(1L)).thenReturn(existing);

        PersistResult result = service.persistSuccess(1L, state, new Date(2000L));

        assertEquals(PersistResult.METADATA_ONLY, result);
        verify(stateDao).updateMetadata(existing);
        verify(stateDao, never()).updateSnapshot(any());
        assertEquals(payload.getPayload(), existing.getPayload());
    }

    @Test
    public void testChangedAndEmptyPayloadReplacesPreviousSnapshot() {
        VmGuestNetworkState populated = populatedState();
        VmGuestNetworkStateVO existing = existingState(canonicalizer.canonicalize(populated));
        VmGuestNetworkState empty = new VmGuestNetworkState("vm-1");
        empty.setStatus("OK");
        when(stateDao.findByVmId(1L)).thenReturn(existing);

        PersistResult result = service.persistSuccess(1L, empty, new Date(3000L));

        assertEquals(PersistResult.PAYLOAD_UPDATED, result);
        verify(stateDao).updateSnapshot(existing);
        assertEquals(Collections.emptyList(), empty.getInterfaces());
    }

    @Test
    public void testFailurePreservesLastPayloadAndMarksStale() {
        CanonicalPayload payload = canonicalizer.canonicalize(populatedState());
        VmGuestNetworkStateVO existing = existingState(payload);
        when(stateDao.findByVmId(1L)).thenReturn(existing);

        service.persistFailure(1L, null, "QGA_TIMEOUT", "timed out", new Date(4000L));

        assertEquals("STALE", existing.getStatus());
        assertEquals(payload.getPayload(), existing.getPayload());
        assertEquals("QGA_TIMEOUT", existing.getErrorCode());
        verify(stateDao).updateMetadata(existing);
        verify(stateDao, never()).updateSnapshot(any());
    }

    @Test
    public void testStoppedStatePreservesSnapshot() {
        CanonicalPayload payload = canonicalizer.canonicalize(populatedState());
        VmGuestNetworkStateVO existing = existingState(payload);
        when(stateDao.findByVmId(1L)).thenReturn(existing);

        PersistResult result = service.markStopped(1L, new Date(5000L));

        assertEquals(PersistResult.METADATA_ONLY, result);
        assertEquals("STOPPED", existing.getStatus());
        assertEquals(payload.getPayload(), existing.getPayload());
        assertNull(existing.getErrorCode());
        verify(stateDao).updateMetadata(existing);
    }

    @Test
    public void testRouteOnlyUpdatePreservesNotDueInterfaces() {
        VmGuestNetworkState previous = populatedState();
        previous.putSectionStatus("interfaces", new VmGuestNetworkSectionStatus("OK"));
        previous.putSectionStatus("routes", new VmGuestNetworkSectionStatus("UNSUPPORTED"));
        VmGuestNetworkStateVO existing = existingState(canonicalizer.canonicalize(previous));
        when(stateDao.findByVmId(1L)).thenReturn(existing);

        VmGuestRoute route = new VmGuestRoute();
        route.setFamily("ipv4");
        route.setDestination("0.0.0.0");
        route.setPrefix(0);
        route.setGateway("10.10.22.1");
        route.setDefaultRoute(true);
        VmGuestNetworkState update = new VmGuestNetworkState("vm-1");
        update.setStatus("OK");
        update.putSectionStatus("interfaces", new VmGuestNetworkSectionStatus("NOT_DUE"));
        update.putSectionStatus("routes", new VmGuestNetworkSectionStatus("OK"));
        update.setRoutes(Collections.singletonList(route));

        service.persistSuccess(1L, update, new Date(6000L));

        assertEquals(1, update.getInterfaces().size());
        assertEquals("10.10.22.10", update.getInterfaces().get(0).getAddresses().get(0).getAddress());
        assertEquals(1, update.getRoutes().size());
        assertEquals("OK", update.getStatus());
        verify(stateDao).updateSnapshot(existing);
    }

    @Test
    public void testFailedRouteSectionRetainsLastSuccessfulRoutesAsStale() {
        VmGuestNetworkState previous = populatedState();
        VmGuestRoute previousRoute = new VmGuestRoute();
        previousRoute.setFamily("ipv6");
        previousRoute.setDestination("::");
        previousRoute.setPrefix(0);
        previousRoute.setGateway("fe80::1");
        previousRoute.setDefaultRoute(true);
        previous.setRoutes(Collections.singletonList(previousRoute));
        previous.putSectionStatus("interfaces", new VmGuestNetworkSectionStatus("OK"));
        previous.putSectionStatus("routes", new VmGuestNetworkSectionStatus("OK"));
        VmGuestNetworkStateVO existing = existingState(canonicalizer.canonicalize(previous));
        VmGuestNetworkState restored = GsonHelper.getGson().fromJson(
                existing.getPayload(), VmGuestNetworkState.class);
        assertEquals(existing.getPayload(), 1, restored.getRoutes().size());
        when(stateDao.findByVmId(1L)).thenReturn(existing);

        VmGuestNetworkState update = new VmGuestNetworkState("vm-1");
        update.setStatus("UNSUPPORTED");
        update.putSectionStatus("interfaces", new VmGuestNetworkSectionStatus("NOT_DUE"));
        update.putSectionStatus("routes",
                new VmGuestNetworkSectionStatus("UNSUPPORTED", "fallback disabled"));

        service.persistSuccess(1L, update, new Date(7000L));

        assertEquals(1, update.getRoutes().size());
        assertEquals("fe80::1", update.getRoutes().get(0).getGateway());
        assertEquals("STALE", update.getSectionStatuses().get("routes").getStatus());
        assertEquals("PARTIAL", update.getStatus());
        verify(stateDao).updateSnapshot(existing);
    }

    @Test
    public void testFailedDnsSectionRetainsLastSuccessfulDnsAsStale() {
        VmGuestNetworkState previous = populatedState();
        VmGuestDnsConfig config = new VmGuestDnsConfig();
        config.setInterfaceName("eth0");
        config.setSource("resolvectl");
        config.setServers(Collections.singletonList(
                new VmGuestDnsServer("10.10.22.1", "ipv4", false)));
        previous.getDns().setSource("resolvectl");
        previous.getDns().setServers(Collections.singletonList("10.10.22.1"));
        previous.getDns().setConfigurations(Collections.singletonList(config));
        previous.putSectionStatus("dns", new VmGuestNetworkSectionStatus("OK"));
        VmGuestNetworkStateVO existing = existingState(canonicalizer.canonicalize(previous));
        when(stateDao.findByVmId(1L)).thenReturn(existing);

        VmGuestNetworkState update = new VmGuestNetworkState("vm-1");
        update.setStatus("UNAVAILABLE");
        update.putSectionStatus("interfaces", new VmGuestNetworkSectionStatus("NOT_DUE"));
        update.putSectionStatus("routes", new VmGuestNetworkSectionStatus("NOT_DUE"));
        update.putSectionStatus("dns",
                new VmGuestNetworkSectionStatus("UNAVAILABLE", "resolvectl timed out"));

        service.persistSuccess(1L, update, new Date(8000L));

        assertEquals("10.10.22.1", update.getDns().getServers().get(0));
        assertEquals("STALE", update.getSectionStatuses().get("dns").getStatus());
        assertEquals("STALE", update.getStatus());
        verify(stateDao).updateSnapshot(existing);
    }

    private VmGuestNetworkState populatedState() {
        VmGuestNetworkInterface networkInterface = new VmGuestNetworkInterface();
        networkInterface.setName("eth0");
        networkInterface.setAddresses(Collections.singletonList(
                new VmGuestIpAddress("ipv4", "10.10.22.10", 24, "private")));
        VmGuestNetworkState state = new VmGuestNetworkState("vm-1");
        state.setStatus("OK");
        state.setAgentVersion("8.2.0");
        state.setInterfaces(Collections.singletonList(networkInterface));
        return state;
    }

    private VmGuestNetworkStateVO existingState(CanonicalPayload payload) {
        VmGuestNetworkStateVO state = new VmGuestNetworkStateVO(1L, new Date(1000L));
        state.setStatus("OK");
        state.setPayload(payload.getPayload());
        state.setPayloadHash(payload.getHash());
        state.setLastSuccessAt(new Date(1000L));
        return state;
    }
}
