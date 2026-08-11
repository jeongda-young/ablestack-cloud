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
package com.cloud.hypervisor.kvm.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.cloud.agent.api.VmGuestDnsConfig;
import com.cloud.hypervisor.kvm.resource.QemuGuestDnsParser.DnsParseResult;

public class QemuGuestDnsParserTest {
    private final QemuGuestDnsParser parser = new QemuGuestDnsParser();

    @Test
    public void testResolvectlPreservesPerLinkServersAndRoutingDomains() throws Exception {
        DnsParseResult result = parser.parseResolvectl(
                readFixture("guest-exec-linux-dns-resolvectl.txt"));

        assertEquals("resolvectl", result.getState().getSource());
        assertEquals(2, result.getState().getConfigurations().size());
        assertEquals(3, result.getState().getServers().size());
        assertEquals(2, result.getState().getSearchDomains().size());
        assertTrue(result.getState().isUpstreamServersKnown());
        VmGuestDnsConfig ethernet = result.getState().getConfigurations().get(0);
        assertEquals("eth0", ethernet.getInterfaceName());
        assertEquals("ipv6", ethernet.getServers().get(1).getFamily());
        assertTrue(ethernet.getDomains().get(1).isRoutingOnly());
        assertEquals(".", ethernet.getDomains().get(1).getDomain());
        assertTrue(result.getState().getConfigurations().get(1).getServers().get(0).isLocalStub());
    }

    @Test
    public void testNmcliParsesEscapedIpv6AndPerDeviceDns() throws Exception {
        DnsParseResult result = parser.parseNmcli(readFixture("guest-exec-linux-dns-nmcli.txt"));

        assertEquals(2, result.getState().getConfigurations().size());
        assertEquals("2001:db8::53",
                result.getState().getConfigurations().get(0).getServers().get(1).getAddress());
        assertEquals("IPv6 family is normalized internally", "ipv6",
                result.getState().getConfigurations().get(0).getServers().get(1).getFamily());
        assertTrue(result.getState().getConfigurations().get(1).getServers().get(0).isLocalStub());
    }

    @Test
    public void testResolvConfDoesNotInferUpstreamBehindLocalStub() throws Exception {
        DnsParseResult result = parser.parseResolvConf(
                readFixture("guest-exec-linux-dns-resolv-conf.txt"));

        assertEquals(1, result.getState().getServers().size());
        assertTrue(result.getState().getConfigurations().get(0).getServers().get(0).isLocalStub());
        assertFalse(result.getState().isUpstreamServersKnown());
        assertEquals(2, result.getState().getSearchDomains().size());
    }

    @Test
    public void testWindowsDnsClientParsesIpv4Ipv6SuffixesAndLocalStub() throws Exception {
        DnsParseResult result = parser.parseWindows(readFixture("guest-exec-windows-dns.json"));

        assertEquals("windows-dns-client", result.getState().getSource());
        assertEquals(2, result.getState().getConfigurations().size());
        assertEquals(3, result.getState().getServers().size());
        assertEquals(3, result.getState().getSearchDomains().size());
        assertTrue(result.getState().getConfigurations().get(0).getServers().get(1).isLocalStub());
        assertTrue(result.getState().getConfigurations().get(1).isGlobal());
        assertTrue(result.getState().isUpstreamServersKnown());
    }

    @Test
    public void testDnsServerLimitIsBounded() {
        StringBuilder resolvConf = new StringBuilder();
        for (int index = 0; index <= QemuGuestDnsParser.MAX_DNS_SERVERS; index++) {
            int thirdOctet = index / 250;
            int fourthOctet = index % 250 + 1;
            resolvConf.append("nameserver 10.0.")
                    .append(thirdOctet).append('.').append(fourthOctet).append('\n');
        }

        DnsParseResult result = parser.parseResolvConf(resolvConf.toString());

        assertEquals(QemuGuestDnsParser.MAX_DNS_SERVERS, result.getState().getServers().size());
        assertTrue(result.isTruncated());
        assertEquals(QemuGuestDnsParser.MAX_DNS_SERVERS + 1, result.getOriginalCount());
    }

    private String readFixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/qga/" + name)) {
            if (input == null) {
                throw new IOException("Missing fixture: " + name);
            }
            return IOUtils.toString(input, StandardCharsets.UTF_8);
        }
    }
}
