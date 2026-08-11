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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class QemuGuestOsFamilyResolver {
    private static final Set<String> LINUX_IDS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("almalinux", "alpine", "amzn", "arch", "archlinux", "centos",
                    "clear-linux-os", "coreos", "debian", "fedora", "flatcar", "gentoo",
                    "kali", "linux", "linuxmint", "mariner", "ol", "opensuse",
                    "opensuse-leap", "opensuse-tumbleweed", "photon", "rhel", "rocky",
                    "sled", "sles", "ubuntu")));
    private static final Set<String> WINDOWS_IDS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("mswindows", "windows", "win32")));
    private static final Pattern LINUX_TOKEN =
            Pattern.compile("(^|[^a-z0-9])linux([^a-z0-9]|$)");

    public QemuGuestOsFamilyResolution resolve(QemuGuestOsInfo osInfo) {
        if (osInfo == null) {
            return unsupported(null);
        }

        String id = normalize(osInfo.getId());
        if (WINDOWS_IDS.contains(id)) {
            return resolution(QemuGuestOsFamily.WINDOWS, "id:" + id, osInfo);
        }
        if (LINUX_IDS.contains(id)) {
            return resolution(QemuGuestOsFamily.LINUX, "id:" + id, osInfo);
        }

        String kernelName = normalize(osInfo.getKernelName());
        if ("linux".equals(kernelName)) {
            return resolution(QemuGuestOsFamily.LINUX, "kernel-name:linux", osInfo);
        }
        if (containsLinuxToken(osInfo.getName())) {
            return resolution(QemuGuestOsFamily.LINUX, "name:linux", osInfo);
        }
        if (containsLinuxToken(osInfo.getPrettyName())) {
            return resolution(QemuGuestOsFamily.LINUX, "pretty-name:linux", osInfo);
        }
        return unsupported(osInfo);
    }

    private QemuGuestOsFamilyResolution unsupported(QemuGuestOsInfo osInfo) {
        return resolution(QemuGuestOsFamily.UNSUPPORTED, "unsupported", osInfo);
    }

    private QemuGuestOsFamilyResolution resolution(QemuGuestOsFamily family, String source,
            QemuGuestOsInfo osInfo) {
        return new QemuGuestOsFamilyResolution(family, source, osInfo);
    }

    private boolean containsLinuxToken(String value) {
        String normalized = normalize(value);
        return normalized != null && LINUX_TOKEN.matcher(normalized).find();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
