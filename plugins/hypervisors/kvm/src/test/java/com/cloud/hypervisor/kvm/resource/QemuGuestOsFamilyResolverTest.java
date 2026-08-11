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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class QemuGuestOsFamilyResolverTest {
    private final QemuGuestNetworkStateParser parser = new QemuGuestNetworkStateParser();
    private final QemuGuestOsFamilyResolver resolver = new QemuGuestOsFamilyResolver();

    @Test
    public void testActualDebianFixtureWithoutKernelNameResolvesLinux() throws IOException {
        QemuGuestOsFamilyResolution result = resolver.resolve(
                parser.parseOsInfo(readFixture("guest-get-osinfo-debian.json")));

        assertEquals(QemuGuestOsFamily.LINUX, result.getFamily());
        assertEquals("id:debian", result.getSource());
    }

    @Test
    public void testUbuntuFixtureWithoutLinuxDisplayTokenResolvesLinux() throws IOException {
        QemuGuestOsFamilyResolution result = resolver.resolve(
                parser.parseOsInfo(readFixture("guest-get-osinfo-ubuntu.json")));

        assertEquals(QemuGuestOsFamily.LINUX, result.getFamily());
        assertEquals("id:ubuntu", result.getSource());
    }

    @Test
    public void testObservedLinuxDistributionIdsResolveLinux() {
        assertResolution("rocky", QemuGuestOsFamily.LINUX, "id:rocky");
        assertResolution("centos", QemuGuestOsFamily.LINUX, "id:centos");
        assertResolution("rhel", QemuGuestOsFamily.LINUX, "id:rhel");
        assertResolution("almalinux", QemuGuestOsFamily.LINUX, "id:almalinux");
    }

    @Test
    public void testWindowsIdResolvesWindows() {
        assertResolution("mswindows", QemuGuestOsFamily.WINDOWS, "id:mswindows");
    }

    @Test
    public void testKernelAndPrettyNameProvideBoundedLinuxFallback() {
        QemuGuestOsFamilyResolution kernel = resolver.resolve(
                new QemuGuestOsInfo("custom", "Linux", "Custom", "Custom"));
        QemuGuestOsFamilyResolution prettyName = resolver.resolve(
                new QemuGuestOsInfo("custom", null, "Custom", "Example Linux 1"));

        assertEquals(QemuGuestOsFamily.LINUX, kernel.getFamily());
        assertEquals("kernel-name:linux", kernel.getSource());
        assertEquals(QemuGuestOsFamily.LINUX, prettyName.getFamily());
        assertEquals("pretty-name:linux", prettyName.getSource());
    }

    @Test
    public void testUnknownAndNonTokenLinuxTextFailClosed() {
        QemuGuestOsFamilyResolution unknown = resolver.resolve(
                new QemuGuestOsInfo("freebsd", null, "FreeBSD", "FreeBSD 14"));
        QemuGuestOsFamilyResolution embedded = resolver.resolve(
                new QemuGuestOsInfo("custom", null, "NotlinuxOS", "NotlinuxOS"));

        assertEquals(QemuGuestOsFamily.UNSUPPORTED, unknown.getFamily());
        assertEquals("unsupported", unknown.getSource());
        assertEquals(QemuGuestOsFamily.UNSUPPORTED, embedded.getFamily());
        assertEquals("unsupported", embedded.getSource());
    }

    private void assertResolution(String id, QemuGuestOsFamily family, String source) {
        QemuGuestOsFamilyResolution result = resolver.resolve(
                new QemuGuestOsInfo(id, null, null, null));
        assertEquals(family, result.getFamily());
        assertEquals(source, result.getSource());
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
