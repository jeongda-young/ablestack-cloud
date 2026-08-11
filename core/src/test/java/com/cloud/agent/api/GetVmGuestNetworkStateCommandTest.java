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
package com.cloud.agent.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.google.gson.Gson;

public class GetVmGuestNetworkStateCommandTest {

    @Test
    public void testCommandRoundTripPreservesVmScopedNicMapAndTimeout() {
        Map<String, String> firstVmNics = new LinkedHashMap<>();
        firstVmNics.put("52-54-00-AA-BB-01", "nic-1");
        Map<String, Map<String, String>> nicIds = new LinkedHashMap<>();
        nicIds.put("vm-1", firstVmNics);

        GetVmGuestNetworkStateCommand command = new GetVmGuestNetworkStateCommand(
                Arrays.asList("vm-1", "vm-2"), nicIds, 4, Collections.singleton("vm-1"));
        GetVmGuestNetworkStateCommand decoded = new Gson().fromJson(
                new Gson().toJson(command), GetVmGuestNetworkStateCommand.class);

        assertEquals(Arrays.asList("vm-1", "vm-2"), decoded.getVmNames());
        assertEquals("nic-1", decoded.getCloudNicIdsForVm("vm-1").get("52-54-00-AA-BB-01"));
        assertEquals(4, decoded.getTimeoutSeconds());
        assertTrue(decoded.hasCachedInterfaceCapability("vm-1"));
        assertFalse(decoded.hasCachedInterfaceCapability("vm-2"));
        assertTrue(decoded.shouldCollectInterfaces("vm-1"));
        assertFalse(decoded.shouldCollectRoutes("vm-1"));
        assertFalse(decoded.shouldCollectDns("vm-1"));
        assertFalse(decoded.executeInSequence());
    }

    @Test
    public void testCommandPreservesSectionRequestsAndFallbackLimits() {
        GetVmGuestNetworkStateCommand command = new GetVmGuestNetworkStateCommand(
                Arrays.asList("vm-1", "vm-2"), Collections.emptyMap(), 5,
                Collections.emptySet(), Collections.singleton("vm-1"),
                Collections.singleton("vm-2"), Collections.singleton("vm-1"),
                true, 65536);
        GetVmGuestNetworkStateCommand decoded = new Gson().fromJson(
                new Gson().toJson(command), GetVmGuestNetworkStateCommand.class);

        assertTrue(decoded.shouldCollectInterfaces("vm-1"));
        assertFalse(decoded.shouldCollectInterfaces("vm-2"));
        assertFalse(decoded.shouldCollectRoutes("vm-1"));
        assertTrue(decoded.shouldCollectRoutes("vm-2"));
        assertTrue(decoded.shouldCollectDns("vm-1"));
        assertFalse(decoded.shouldCollectDns("vm-2"));
        assertTrue(decoded.isExecFallbackEnabled());
        assertEquals(65536, decoded.getMaxExecOutputBytes());
    }

    @Test
    public void testCommandDefensivelyCopiesInputMaps() {
        Map<String, String> vmNics = new LinkedHashMap<>();
        vmNics.put("52:54:00:aa:bb:01", "nic-1");
        Map<String, Map<String, String>> nicIds = new LinkedHashMap<>();
        nicIds.put("vm-1", vmNics);

        GetVmGuestNetworkStateCommand command = new GetVmGuestNetworkStateCommand(
                Arrays.asList("vm-1"), nicIds);
        vmNics.put("52:54:00:aa:bb:02", "nic-2");

        assertEquals(1, command.getCloudNicIdsForVm("vm-1").size());
        assertEquals(GetVmGuestNetworkStateCommand.DEFAULT_TIMEOUT_SECONDS, command.getTimeoutSeconds());
    }
}
