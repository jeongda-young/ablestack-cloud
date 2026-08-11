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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.cloud.agent.api.VmGuestIpAddress;
import com.cloud.agent.api.VmGuestNetworkInterface;

public class QemuGuestAddressRoleFallbackTest {
    private final QemuGuestNetworkStateParser parser = new QemuGuestNetworkStateParser();
    private final QemuGuestAddressRoleFallback fallback = new QemuGuestAddressRoleFallback();
    private final QemuGuestOsFamilyResolver osFamilyResolver =
            new QemuGuestOsFamilyResolver();

    @Test
    public void testLinuxSecondaryFlagsSelectActualPreflightPrimary() throws Exception {
        List<VmGuestNetworkInterface> interfaces = parser.parseInterfaces(
                readFixture("guest-network-get-interfaces-primary-secondary.json"),
                Collections.emptyMap());
        RecordingExecutor executor = new RecordingExecutor();
        executor.addCompleted(901L,
                readFixture("guest-exec-linux-address-primary-secondary.json"));

        String source = fallback.collect(executor, resolve("debian"), interfaces, 1, 65536);

        assertEquals("guest-exec-linux-ip-address", source);
        List<VmGuestIpAddress> addresses = interfaces.get(1).getAddresses();
        assertEquals("PRIMARY", addresses.get(0).getRole());
        assertTrue(addresses.get(0).isRepresentative());
        assertEquals("10.10.254.230", addresses.get(0).getAddress());
        assertEquals("SECONDARY", addresses.get(1).getRole());
        assertEquals("SECONDARY", addresses.get(2).getRole());
        assertEquals("SECONDARY", addresses.get(3).getRole());
        assertFalse(addresses.get(3).isRepresentative());
        assertTrue(executor.commands.get(0).contains("\"path\":\"/usr/sbin/ip\""));
        assertTrue(executor.commands.get(0).contains(
                "\"arg\":[\"-j\",\"address\",\"show\"]"));
    }

    @Test
    public void testWindowsSkipAsSourceAndDefaultRouteSelectPrimary() throws Exception {
        String response = "{\"return\":[{\"name\":\"Ethernet 2\","
                + "\"ip-addresses\":["
                + "{\"ip-address-type\":\"ipv4\",\"ip-address\":\"172.16.10.20\","
                + "\"prefix\":24},"
                + "{\"ip-address-type\":\"ipv4\",\"ip-address\":\"172.16.10.21\","
                + "\"prefix\":24}]}]}";
        List<VmGuestNetworkInterface> interfaces =
                parser.parseInterfaces(response, Collections.emptyMap());
        RecordingExecutor executor = new RecordingExecutor();
        executor.addCompleted(902L,
                readFixture("guest-exec-windows-address-primary-secondary.json"));

        String source = fallback.collect(
                executor, resolve("mswindows"), interfaces, 1, 65536);

        assertEquals("guest-exec-windows-get-net-ip-address", source);
        assertEquals("PRIMARY", interfaces.get(0).getAddresses().get(0).getRole());
        assertTrue(interfaces.get(0).getAddresses().get(0).isRepresentative());
        assertEquals("SECONDARY", interfaces.get(0).getAddresses().get(1).getRole());
        assertTrue(executor.commands.get(0).contains("Get-NetIPAddress"));
    }

    @Test
    public void testSingleAddressAvoidsGuestExecAndBecomesRepresentative() throws Exception {
        String response = "{\"return\":[{\"name\":\"eth0\","
                + "\"ip-addresses\":[{\"ip-address-type\":\"ipv4\","
                + "\"ip-address\":\"192.0.2.50\",\"prefix\":24}]}]}";
        List<VmGuestNetworkInterface> interfaces =
                parser.parseInterfaces(response, Collections.emptyMap());

        assertTrue(fallback.markSingleAddress(interfaces));
        assertFalse(fallback.requiresResolution(interfaces));
        assertEquals("PRIMARY", interfaces.get(0).getAddresses().get(0).getRole());
        assertEquals("QGA_SINGLE_ADDRESS",
                interfaces.get(0).getAddresses().get(0).getRoleSource());
        assertTrue(interfaces.get(0).getAddresses().get(0).isRepresentative());
    }

    @Test
    public void testGenericShellInvocationIsRejected() {
        try {
            fallback.buildGuestExec("/bin/sh",
                    java.util.Arrays.asList("-c", "ip -j address show"));
            fail("Generic shell command must not be accepted");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("allowlisted"));
        }
    }

    private QemuGuestOsFamilyResolution resolve(String osId) {
        return osFamilyResolver.resolve(new QemuGuestOsInfo(osId, null, null, null));
    }

    private String readFixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/qga/" + name)) {
            if (input == null) {
                throw new IOException("Missing fixture: " + name);
            }
            return IOUtils.toString(input, StandardCharsets.UTF_8);
        }
    }

    private static final class RecordingExecutor
            implements QemuGuestAddressRoleFallback.AgentCommandExecutor {
        private final Deque<String> responses = new ArrayDeque<>();
        private final List<String> commands = new ArrayList<>();

        void addCompleted(long pid, String stdout) {
            responses.add("{\"return\":{\"pid\":" + pid + "}}");
            responses.add("{\"return\":{\"exited\":true,\"exitcode\":0,\"out-data\":\""
                    + Base64.getEncoder().encodeToString(stdout.getBytes(StandardCharsets.UTF_8))
                    + "\"}}");
        }

        @Override
        public String execute(String command, int timeoutSeconds) {
            commands.add(command);
            return responses.removeFirst();
        }
    }
}
