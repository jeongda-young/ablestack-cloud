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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.cloud.hypervisor.kvm.resource.QemuGuestRouteFallback.FallbackResult;

public class QemuGuestRouteFallbackTest {
    private final QemuGuestRouteFallback fallback =
            new QemuGuestRouteFallback(new QemuGuestNetworkStateParser());
    private final QemuGuestOsFamilyResolver osFamilyResolver =
            new QemuGuestOsFamilyResolver();

    @Test
    public void testLinuxFallbackUsesOnlyAbsoluteAllowlistedIpCommands() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        executor.addCompleted(101L, readFixture("guest-exec-linux-route-v4.json"));
        executor.addCompleted(102L, readFixture("guest-exec-linux-route-v6.json"));

        FallbackResult result = fallback.collect(executor, resolve("debian"), 1, 65536);

        assertEquals(4, result.getRoutes().size());
        assertEquals("guest-exec-linux-ip", result.getSource());
        assertTrue(executor.commands.get(0).contains("\"path\":\"/usr/sbin/ip\""));
        assertTrue(executor.commands.get(0).contains("\"arg\":[\"-j\",\"-4\",\"route\",\"show\",\"table\",\"all\"]"));
        assertTrue(executor.commands.get(2).contains("\"arg\":[\"-j\",\"-6\",\"route\",\"show\",\"table\",\"all\"]"));
    }

    @Test
    public void testWindowsFallbackUsesFixedPowerShellProjection() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        executor.addCompleted(201L, readFixture("guest-exec-windows-route.json"));

        FallbackResult result = fallback.collect(executor, resolve("mswindows"), 1, 65536);

        assertEquals(3, result.getRoutes().size());
        assertEquals("guest-exec-windows-get-net-route", result.getSource());
        assertTrue(executor.commands.get(0).contains("WindowsPowerShell"));
        assertTrue(executor.commands.get(0).contains("Get-NetRoute"));
        assertTrue(executor.commands.get(0).contains("ConvertTo-Json -Compress"));
    }

    @Test
    public void testLinuxFallbackUsesUsrBinIpWhenUsrSbinIsMissing() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        executor.addResponse("{\"error\":{\"desc\":\"No such file or directory\"}}");
        executor.addCompleted(151L, readFixture("guest-exec-linux-route-v4.json"));
        executor.addCompleted(152L, readFixture("guest-exec-linux-route-v6.json"));

        FallbackResult result = fallback.collect(executor, resolve("ubuntu"), 1, 65536);

        assertEquals(4, result.getRoutes().size());
        assertTrue(executor.commands.get(1).contains("\"path\":\"/usr/bin/ip\""));
        assertTrue(executor.commands.get(3).contains("\"path\":\"/usr/bin/ip\""));
    }

    @Test
    public void testGenericShellInvocationIsRejected() {
        try {
            fallback.buildGuestExec("/bin/sh", java.util.Arrays.asList("-c", "ip route"));
            fail("Generic shell command must not be accepted");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("allowlisted"));
        }
    }

    @Test
    public void testOutputLimitIsEnforcedBeforeParsing() {
        RecordingExecutor executor = new RecordingExecutor();
        executor.addCompleted(301L, "0123456789");

        try {
            fallback.collect(executor, resolve("rocky"), 1, 4);
            fail("Oversized output must fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("output"));
        }
    }

    @Test
    public void testLinuxFallbackAppliesGlobalRouteLimitAcrossFamilies() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        StringBuilder ipv4 = new StringBuilder("[");
        StringBuilder ipv6 = new StringBuilder("[");
        int routesPerFamily = QemuGuestNetworkStateParser.MAX_ROUTES / 2 + 1;
        for (int index = 0; index < routesPerFamily; index++) {
            if (index > 0) {
                ipv4.append(',');
                ipv6.append(',');
            }
            ipv4.append("{\"dst\":\"10.0.0.0/24\",\"dev\":\"eth0\"}");
            ipv6.append("{\"dst\":\"2001:db8::/64\",\"dev\":\"eth0\"}");
        }
        ipv4.append(']');
        ipv6.append(']');
        executor.addCompleted(351L, ipv4.toString());
        executor.addCompleted(352L, ipv6.toString());

        FallbackResult result = fallback.collect(executor, resolve("centos"), 1, 1024 * 1024);

        assertEquals(QemuGuestNetworkStateParser.MAX_ROUTES, result.getRoutes().size());
        assertEquals(routesPerFamily * 2, result.getOriginalCount());
        assertTrue(result.isTruncated());
    }

    @Test
    public void testTimeoutPerformsFinalStatusCleanup() {
        final int[] calls = {0};
        QemuGuestRouteFallback.AgentCommandExecutor executor = (command, timeout) -> {
            calls[0]++;
            if (command.contains("\"execute\":\"guest-exec\"")) {
                return "{\"return\":{\"pid\":401}}";
            }
            return "{\"return\":{\"exited\":false}}";
        };

        try {
            fallback.collect(executor, resolve("debian"), 1, 65536);
            fail("Timed out process must fail");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("timed out"));
            assertEquals(22, calls[0]);
        }
    }

    private String readFixture(String name) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/qga/" + name)) {
            if (input == null) {
                throw new IOException("Missing fixture: " + name);
            }
            return IOUtils.toString(input, StandardCharsets.UTF_8);
        }
    }

    private QemuGuestOsFamilyResolution resolve(String osId) {
        return osFamilyResolver.resolve(new QemuGuestOsInfo(osId, null, null, null));
    }

    private static final class RecordingExecutor
            implements QemuGuestRouteFallback.AgentCommandExecutor {
        private final Deque<String> responses = new ArrayDeque<>();
        private final List<String> commands = new ArrayList<>();

        void addCompleted(long pid, String stdout) {
            responses.add("{\"return\":{\"pid\":" + pid + "}}");
            responses.add("{\"return\":{\"exited\":true,\"exitcode\":0,\"out-data\":\""
                    + Base64.getEncoder().encodeToString(stdout.getBytes(StandardCharsets.UTF_8))
                    + "\"}}");
        }

        void addResponse(String response) {
            responses.add(response);
        }

        @Override
        public String execute(String command, int timeoutSeconds) {
            commands.add(command);
            return responses.removeFirst();
        }
    }
}
