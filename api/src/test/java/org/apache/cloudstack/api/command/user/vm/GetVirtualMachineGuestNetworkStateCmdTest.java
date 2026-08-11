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
package org.apache.cloudstack.api.command.user.vm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.response.GuestNetworkDnsConfigResponse;
import org.apache.cloudstack.api.response.GuestNetworkDnsResponse;
import org.apache.cloudstack.api.response.GuestNetworkDnsServerResponse;
import org.apache.cloudstack.api.response.GuestNetworkRouteResponse;
import org.apache.cloudstack.api.response.GuestNetworkStateResponse;
import org.apache.cloudstack.api.response.GuestNetworkSummaryResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.vm.guestnetwork.VmGuestNetworkApiService;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.gson.Gson;

public class GetVirtualMachineGuestNetworkStateCmdTest {

    @Test
    public void testExecuteReturnsPersistedStateResponse() {
        VmGuestNetworkApiService service = mock(VmGuestNetworkApiService.class);
        GuestNetworkStateResponse response = new GuestNetworkStateResponse();
        when(service.getState(41L)).thenReturn(response);
        GetVirtualMachineGuestNetworkStateCmd cmd = new GetVirtualMachineGuestNetworkStateCmd();
        ReflectionTestUtils.setField(cmd, "guestNetworkApiService", service);
        ReflectionTestUtils.setField(cmd, "virtualMachineId", 41L);

        cmd.execute();

        assertSame(response, cmd.getResponseObject());
        assertEquals("guestnetworkstate", response.getObjectName());
        verify(service).getState(41L);
    }

    @Test
    public void testVirtualMachineParameterUsesListEntryAccess() throws NoSuchFieldException {
        Field field = GetVirtualMachineGuestNetworkStateCmd.class.getDeclaredField("virtualMachineId");
        ACL acl = field.getAnnotation(ACL.class);

        assertEquals(AccessType.ListEntry, acl.accessType());
    }

    @Test
    public void testVmListSummarySerializesBothAddressFamilies() {
        GuestNetworkSummaryResponse summary = new GuestNetworkSummaryResponse();
        summary.setStatus("OK");
        summary.setIpv4Addresses(Arrays.asList("10.10.22.10/24"));
        summary.setIpv6Addresses(Arrays.asList("2001:db8::10/64"));
        UserVmResponse response = new UserVmResponse();
        response.setGuestNetwork(summary);

        String json = new Gson().toJson(response);

        assertTrue(json.contains("\"guestnetwork\""));
        assertTrue(json.contains("\"ipv4addresses\":[\"10.10.22.10/24\"]"));
        assertTrue(json.contains("\"ipv6addresses\":[\"2001:db8::10/64\"]"));
    }

    @Test
    public void testDetailResponseSerializesNormalizedDefaultRoute() {
        GuestNetworkRouteResponse route = new GuestNetworkRouteResponse();
        route.setFamily("IPv6");
        route.setDestination("::");
        route.setPrefix(0);
        route.setGateway("fe80::1");
        route.setInterfaceName("eth0");
        route.setDefaultRoute(true);
        GuestNetworkStateResponse response = new GuestNetworkStateResponse();
        response.setRoutes(Arrays.asList(route));

        String json = new Gson().toJson(response);

        assertTrue(json.contains("\"routes\""));
        assertTrue(json.contains("\"destination\":\"::\""));
        assertTrue(json.contains("\"prefix\":0"));
        assertTrue(json.contains("\"default\":true"));
    }

    @Test
    public void testDetailResponseSerializesDnsStubAndUpstreamKnowledge() {
        GuestNetworkDnsServerResponse server = new GuestNetworkDnsServerResponse();
        server.setAddress("127.0.0.53");
        server.setFamily("IPv4");
        server.setLocalStub(true);
        GuestNetworkDnsConfigResponse config = new GuestNetworkDnsConfigResponse();
        config.setGlobal(true);
        config.setSource("resolv.conf");
        config.setServers(Arrays.asList(server));
        GuestNetworkDnsResponse dns = new GuestNetworkDnsResponse();
        dns.setSource("resolv.conf");
        dns.setUpstreamServersKnown(false);
        dns.setServers(Arrays.asList("127.0.0.53"));
        dns.setConfigurations(Arrays.asList(config));
        GuestNetworkStateResponse response = new GuestNetworkStateResponse();
        response.setDns(dns);

        String json = new Gson().toJson(response);

        assertTrue(json.contains("\"dns\""));
        assertTrue(json.contains("\"localstub\":true"));
        assertTrue(json.contains("\"upstreamserversknown\":false"));
        assertTrue(json.contains("\"source\":\"resolv.conf\""));
    }
}
