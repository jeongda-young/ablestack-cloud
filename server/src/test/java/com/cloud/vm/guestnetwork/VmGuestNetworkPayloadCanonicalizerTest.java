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

import java.util.Arrays;

import org.junit.Test;

import com.cloud.agent.api.VmGuestIpAddress;
import com.cloud.agent.api.VmGuestNetworkInterface;
import com.cloud.agent.api.VmGuestNetworkState;
import com.cloud.vm.guestnetwork.VmGuestNetworkPayloadCanonicalizer.CanonicalPayload;

public class VmGuestNetworkPayloadCanonicalizerTest {
    private final VmGuestNetworkPayloadCanonicalizer canonicalizer = new VmGuestNetworkPayloadCanonicalizer();

    @Test
    public void testCanonicalPayloadIgnoresObservationTimeAndArrayOrder() {
        VmGuestNetworkState first = stateWithInterfaces(false);
        first.setObservedAt(100L);
        VmGuestNetworkState second = stateWithInterfaces(true);
        second.setObservedAt(200L);

        CanonicalPayload firstPayload = canonicalizer.canonicalize(first);
        CanonicalPayload secondPayload = canonicalizer.canonicalize(second);

        assertEquals(firstPayload.getHash(), secondPayload.getHash());
        assertEquals(firstPayload.getPayload(), secondPayload.getPayload());
        assertFalse(firstPayload.getPayload().contains("observedAt"));
    }

    private VmGuestNetworkState stateWithInterfaces(boolean reverse) {
        VmGuestNetworkInterface eth0 = networkInterface("eth0", "10.10.22.10");
        VmGuestNetworkInterface eth1 = networkInterface("eth1", "2001:db8::10");
        VmGuestNetworkState state = new VmGuestNetworkState("vm-1");
        state.setStatus("OK");
        state.setInterfaces(reverse ? Arrays.asList(eth1, eth0) : Arrays.asList(eth0, eth1));
        return state;
    }

    private VmGuestNetworkInterface networkInterface(String name, String address) {
        VmGuestNetworkInterface networkInterface = new VmGuestNetworkInterface();
        networkInterface.setName(name);
        networkInterface.setAddresses(Arrays.asList(new VmGuestIpAddress(
                address.contains(":") ? "ipv6" : "ipv4", address, 24, "private")));
        return networkInterface;
    }
}
