// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
package com.cloud.hypervisor.kvm.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.cloud.agent.api.VmGuestIpAddress;
import com.cloud.agent.api.VmGuestNetworkInterface;

public class QemuGuestToolsSnapshotTest {
    @Test
    public void testHelperEnrichesQgaAddressesAndMapsRoutesAndDns() {
        String json = "{"
                + "\"schemaVersion\":1,"
                + "\"tool\":{\"version\":\"0.9.3-1\"},"
                + "\"profile\":{\"version\":1,\"status\":\"READY\"},"
                + "\"sections\":{\"addresses\":{\"status\":\"OK\",\"source\":\"linux-ip-json\"},"
                + "\"routes\":{\"status\":\"OK\",\"source\":\"linux-ip-json\"},"
                + "\"dns\":{\"status\":\"OK\",\"source\":\"resolv-conf\"}},"
                + "\"interfaces\":[{\"name\":\"eth0\",\"addresses\":["
                + "{\"address\":\"10.10.22.201\",\"primary\":true,\"secondary\":false},"
                + "{\"address\":\"10.10.22.202\",\"primary\":false,\"secondary\":true}]}],"
                + "\"routes\":[{\"family\":\"IPv4\",\"destination\":\"default\","
                + "\"gateway\":\"10.10.22.1\",\"device\":\"eth0\",\"metric\":100}],"
                + "\"dns\":{\"servers\":[\"10.10.1.10\"],"
                + "\"searchDomains\":[\"example.test\"],\"source\":\"/etc/resolv.conf\"}}";
        QemuGuestToolsSnapshot snapshot = QemuGuestToolsSnapshot.parse(json);
        VmGuestNetworkInterface networkInterface = new VmGuestNetworkInterface();
        networkInterface.setName("eth0");
        VmGuestIpAddress primary = address("10.10.22.201");
        VmGuestIpAddress secondary = address("10.10.22.202");
        networkInterface.setAddresses(Arrays.asList(primary, secondary));

        snapshot.enrichAddressRoles(Arrays.asList(networkInterface));

        assertEquals("PRIMARY", primary.getRole());
        assertTrue(primary.isRepresentative());
        assertEquals("SECONDARY", secondary.getRole());
        assertEquals("10.10.22.1", snapshot.toRoutes().get(0).getGateway());
        assertEquals("10.10.1.10", snapshot.toDns().getServers().get(0));
        assertEquals("READY", snapshot.toInfo().getReadinessStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectsUnsupportedHelperSchema() {
        QemuGuestToolsSnapshot.parse("{\"schemaVersion\":2}");
    }

    private VmGuestIpAddress address(String value) {
        VmGuestIpAddress address = new VmGuestIpAddress();
        address.setAddress(value);
        address.setFamily("ipv4");
        address.setScope("global");
        return address;
    }
}
