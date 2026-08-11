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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VmGuestNetworkCollectionPolicyTest {

    @Test
    public void testSectionCadenceAndDeterministicJitter() {
        VmGuestNetworkCollectionPolicy first = new VmGuestNetworkCollectionPolicy();
        VmGuestNetworkCollectionPolicy second = new VmGuestNetworkCollectionPolicy();
        long now = 100000L;

        first.recordSuccess(42L, now, 120L, 600L, 900L, 20);
        second.recordSuccess(42L, now, 120L, 600L, 900L, 20);

        assertTrue(first.getNextInterfaceAt(42L) < first.getNextDnsAt(42L));
        assertTrue(first.getNextDnsAt(42L) < first.getNextRouteAt(42L));
        assertTrue(first.getNextInterfaceAt(42L) == second.getNextInterfaceAt(42L));
        assertFalse(first.isInterfaceDue(42L, first.getNextInterfaceAt(42L) - 1L));
        assertTrue(first.isInterfaceDue(42L, first.getNextInterfaceAt(42L)));
    }

    @Test
    public void testFailureBackoffIncreasesAndIsCapped() {
        VmGuestNetworkCollectionPolicy policy = new VmGuestNetworkCollectionPolicy();
        long now = 100000L;

        policy.recordFailure(1L, now, 120L, 300L, 0);
        long firstDelay = policy.getNextInterfaceAt(1L) - now;
        policy.recordFailure(1L, now, 120L, 300L, 0);
        long secondDelay = policy.getNextInterfaceAt(1L) - now;
        policy.recordFailure(1L, now, 120L, 300L, 0);
        long thirdDelay = policy.getNextInterfaceAt(1L) - now;

        assertTrue(secondDelay > firstDelay);
        assertTrue(thirdDelay == 300000L);
    }

    @Test
    public void testCapabilityCacheExpires() {
        VmGuestNetworkCollectionPolicy policy = new VmGuestNetworkCollectionPolicy();
        long now = 100000L;

        policy.recordInterfaceCapability(1L, true, now, 60L);

        assertTrue(policy.hasCachedEnabledInterfaceCapability(1L, now + 59999L));
        assertFalse(policy.hasCachedEnabledInterfaceCapability(1L, now + 60000L));
    }

    @Test
    public void testInterfaceAndRouteSchedulesAdvanceIndependently() {
        VmGuestNetworkCollectionPolicy policy = new VmGuestNetworkCollectionPolicy();
        long now = 100000L;

        policy.recordInterfaceSuccess(1L, now, 120L, 0);

        assertFalse(policy.isInterfaceDue(1L, now + 1L));
        assertTrue(policy.isRouteDue(1L, now + 1L));

        policy.recordRouteSuccess(1L, now, 600L, 0);

        assertFalse(policy.isRouteDue(1L, now + 1L));
        assertTrue(policy.isInterfaceDue(1L, now + 120000L));
        assertFalse(policy.isRouteDue(1L, now + 120000L));
    }

    @Test
    public void testDnsScheduleAndBackoffAdvanceIndependently() {
        VmGuestNetworkCollectionPolicy policy = new VmGuestNetworkCollectionPolicy();
        long now = 100000L;

        policy.recordInterfaceSuccess(1L, now, 120L, 0);
        policy.recordRouteSuccess(1L, now, 600L, 0);

        assertTrue(policy.isDnsDue(1L, now + 1L));
        policy.recordDnsSuccess(1L, now, 600L, 0);
        assertFalse(policy.isDnsDue(1L, now + 1L));
        assertTrue(policy.isInterfaceDue(1L, now + 120000L));
        assertFalse(policy.isDnsDue(1L, now + 120000L));

        policy.recordDnsFailure(1L, now, 600L, 1800L, 0);
        long firstRetry = policy.getNextDnsAt(1L);
        policy.recordDnsFailure(1L, now, 600L, 1800L, 0);
        assertTrue(policy.getNextDnsAt(1L) > firstRetry);
        assertFalse(policy.isRouteDue(1L, now + 599999L));
    }
}
