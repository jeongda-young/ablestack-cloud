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
import java.util.Deque;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.cloud.hypervisor.kvm.resource.QemuGuestDnsParser.DnsParseResult;

public class QemuGuestDnsFallbackTest {
    private final QemuGuestDnsFallback fallback =
            new QemuGuestDnsFallback(new QemuGuestDnsParser());
    private final QemuGuestOsFamilyResolver osFamilyResolver =
            new QemuGuestOsFamilyResolver();

    @Test
    public void testLinuxPrefersAllowlistedResolvectl() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        executor.addCompleted(501L, readFixture("guest-exec-linux-dns-resolvectl.txt"));

        DnsParseResult result = fallback.collect(executor, resolve("debian"), 1, 65536);

        assertEquals("resolvectl", result.getState().getSource());
        assertTrue(result.getState().isUpstreamServersKnown());
        assertEquals(2, executor.commands.size());
        assertTrue(executor.commands.get(0).contains("\"path\":\"/usr/bin/resolvectl\""));
    }

    @Test
    public void testLinuxFallsBackToNmcliThenResolvConf() throws Exception {
        RecordingExecutor nmcliExecutor = new RecordingExecutor();
        nmcliExecutor.addMissing();
        nmcliExecutor.addMissing();
        nmcliExecutor.addCompleted(502L, readFixture("guest-exec-linux-dns-nmcli.txt"));

        DnsParseResult nmcli = fallback.collect(nmcliExecutor, resolve("ubuntu"), 1, 65536);

        assertEquals("nmcli", nmcli.getState().getSource());
        assertTrue(nmcliExecutor.commands.get(2).contains("\"path\":\"/usr/bin/nmcli\""));

        RecordingExecutor resolvConfExecutor = new RecordingExecutor();
        for (int index = 0; index < 4; index++) {
            resolvConfExecutor.addMissing();
        }
        resolvConfExecutor.addCompleted(503L, readFixture("guest-exec-linux-dns-resolv-conf.txt"));

        DnsParseResult resolvConf = fallback.collect(
                resolvConfExecutor, resolve("rocky"), 1, 65536);

        assertEquals("resolv.conf", resolvConf.getState().getSource());
        assertFalse(resolvConf.getState().isUpstreamServersKnown());
        assertTrue(resolvConfExecutor.commands.get(4).contains("\"path\":\"/usr/bin/cat\""));
        assertTrue(resolvConfExecutor.commands.get(4).contains("\"arg\":[\"/etc/resolv.conf\"]"));
    }

    @Test
    public void testWindowsUsesFixedDnsClientProjection() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        executor.addCompleted(504L, readFixture("guest-exec-windows-dns.json"));

        DnsParseResult result = fallback.collect(executor, resolve("windows"), 1, 65536);

        assertEquals("windows-dns-client", result.getState().getSource());
        assertTrue(executor.commands.get(0).contains("Get-DnsClientServerAddress"));
        assertTrue(executor.commands.get(0).contains("Get-DnsClient"));
        assertTrue(executor.commands.get(0).contains("Get-DnsClientGlobalSetting"));
        assertTrue(executor.commands.get(0).contains("ConvertTo-Json"));
    }

    @Test
    public void testNonAllowlistedDnsCommandIsRejected() {
        try {
            fallback.buildGuestExec("/bin/sh", java.util.Arrays.asList("-c", "cat /etc/resolv.conf"));
            fail("Generic shell command must not be accepted");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("allowlisted"));
        }
    }

    @Test
    public void testDnsOutputLimitIsEnforcedForEverySource() {
        RecordingExecutor executor = new RecordingExecutor();
        executor.addCompleted(505L, "0123456789");
        executor.addCompleted(506L, "0123456789");
        executor.addCompleted(507L, "0123456789");

        try {
            fallback.collect(executor, resolve("centos"), 1, 4);
            fail("Oversized DNS output must fail");
        } catch (Exception e) {
            assertTrue(e.getSuppressed().length > 0);
            assertTrue(e.getSuppressed()[0].getMessage().contains("output"));
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
            implements QemuGuestDnsFallback.AgentCommandExecutor {
        private final Deque<String> responses = new ArrayDeque<>();
        private final List<String> commands = new ArrayList<>();

        void addCompleted(long pid, String stdout) {
            responses.add("{\"return\":{\"pid\":" + pid + "}}");
            responses.add("{\"return\":{\"exited\":true,\"exitcode\":0,\"out-data\":\""
                    + Base64.getEncoder().encodeToString(stdout.getBytes(StandardCharsets.UTF_8))
                    + "\"}}");
        }

        void addMissing() {
            responses.add("{\"error\":{\"desc\":\"No such file or directory\"}}");
        }

        @Override
        public String execute(String command, int timeoutSeconds) {
            commands.add(command);
            return responses.removeFirst();
        }
    }
}
