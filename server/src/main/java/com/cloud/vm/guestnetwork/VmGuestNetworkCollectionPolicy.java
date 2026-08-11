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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VmGuestNetworkCollectionPolicy {
    private final Map<Long, VmSchedule> schedules = new ConcurrentHashMap<>();

    public boolean isInterfaceDue(long vmId, long now) {
        return now >= schedule(vmId).nextInterfaceAt;
    }

    public boolean isDnsDue(long vmId, long now) {
        return now >= schedule(vmId).nextDnsAt;
    }

    public boolean isRouteDue(long vmId, long now) {
        return now >= schedule(vmId).nextRouteAt;
    }

    public boolean hasCachedEnabledInterfaceCapability(long vmId, long now) {
        VmSchedule schedule = schedule(vmId);
        return Boolean.TRUE.equals(schedule.interfaceCapabilityEnabled)
                && now < schedule.capabilityExpiresAt;
    }

    public void recordSuccess(long vmId, long now, long interfaceIntervalSeconds,
            long dnsIntervalSeconds, long routeIntervalSeconds, int jitterPercent) {
        recordInterfaceSuccess(vmId, now, interfaceIntervalSeconds, jitterPercent);
        recordDnsSuccess(vmId, now, dnsIntervalSeconds, jitterPercent);
        recordRouteSuccess(vmId, now, routeIntervalSeconds, jitterPercent);
    }

    public void recordInterfaceSuccess(long vmId, long now,
            long intervalSeconds, int jitterPercent) {
        VmSchedule schedule = schedule(vmId);
        schedule.interfaceFailures = 0;
        schedule.nextInterfaceAt = nextTime(vmId, now, intervalSeconds, jitterPercent, 11);
    }

    public void recordRouteSuccess(long vmId, long now,
            long intervalSeconds, int jitterPercent) {
        VmSchedule schedule = schedule(vmId);
        schedule.routeFailures = 0;
        schedule.nextRouteAt = nextTime(vmId, now, intervalSeconds, jitterPercent, 37);
    }

    public void recordDnsSuccess(long vmId, long now,
            long intervalSeconds, int jitterPercent) {
        VmSchedule schedule = schedule(vmId);
        schedule.dnsFailures = 0;
        schedule.nextDnsAt = nextTime(vmId, now, intervalSeconds, jitterPercent, 23);
    }

    public void recordFailure(long vmId, long now, long baseIntervalSeconds,
            long maxBackoffSeconds, int jitterPercent) {
        recordInterfaceFailure(vmId, now, baseIntervalSeconds, maxBackoffSeconds, jitterPercent);
    }

    public void recordInterfaceFailure(long vmId, long now, long baseIntervalSeconds,
            long maxBackoffSeconds, int jitterPercent) {
        VmSchedule schedule = schedule(vmId);
        schedule.interfaceFailures = Math.min(30, schedule.interfaceFailures + 1);
        long multiplier = 1L << Math.min(20, schedule.interfaceFailures - 1);
        long backoff = Math.min(maxBackoffSeconds, Math.max(1L, baseIntervalSeconds) * multiplier);
        schedule.nextInterfaceAt = nextTime(vmId, now, backoff, jitterPercent, 53);
    }

    public void recordRouteFailure(long vmId, long now, long baseIntervalSeconds,
            long maxBackoffSeconds, int jitterPercent) {
        VmSchedule schedule = schedule(vmId);
        schedule.routeFailures = Math.min(30, schedule.routeFailures + 1);
        long multiplier = 1L << Math.min(20, schedule.routeFailures - 1);
        long backoff = Math.min(maxBackoffSeconds, Math.max(1L, baseIntervalSeconds) * multiplier);
        schedule.nextRouteAt = nextTime(vmId, now, backoff, jitterPercent, 71);
    }

    public void recordDnsFailure(long vmId, long now, long baseIntervalSeconds,
            long maxBackoffSeconds, int jitterPercent) {
        VmSchedule schedule = schedule(vmId);
        schedule.dnsFailures = Math.min(30, schedule.dnsFailures + 1);
        long multiplier = 1L << Math.min(20, schedule.dnsFailures - 1);
        long backoff = Math.min(maxBackoffSeconds, Math.max(1L, baseIntervalSeconds) * multiplier);
        schedule.nextDnsAt = nextTime(vmId, now, backoff, jitterPercent, 89);
    }

    public void recordInterfaceCapability(long vmId, boolean enabled, long now, long ttlSeconds) {
        VmSchedule schedule = schedule(vmId);
        schedule.interfaceCapabilityEnabled = enabled;
        schedule.capabilityExpiresAt = now + Math.max(1L, ttlSeconds) * 1000L;
    }

    public void remove(long vmId) {
        schedules.remove(vmId);
    }

    public Set<Long> trackedVmIds() {
        return new HashSet<>(schedules.keySet());
    }

    long getNextInterfaceAt(long vmId) {
        return schedule(vmId).nextInterfaceAt;
    }

    long getNextDnsAt(long vmId) {
        return schedule(vmId).nextDnsAt;
    }

    long getNextRouteAt(long vmId) {
        return schedule(vmId).nextRouteAt;
    }

    private long nextTime(long vmId, long now, long intervalSeconds, int jitterPercent, int salt) {
        long intervalMillis = Math.max(1L, intervalSeconds) * 1000L;
        int boundedPercent = Math.min(50, Math.max(0, jitterPercent));
        if (boundedPercent == 0) {
            return now + intervalMillis;
        }
        long range = intervalMillis * boundedPercent / 100L;
        long hash = mix(vmId ^ salt);
        long offset = Math.floorMod(hash, range * 2L + 1L) - range;
        return now + intervalMillis + offset;
    }

    private long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        return value ^ value >>> 33;
    }

    private VmSchedule schedule(long vmId) {
        return schedules.computeIfAbsent(vmId, ignored -> new VmSchedule());
    }

    private static final class VmSchedule {
        private volatile long nextInterfaceAt;
        private volatile long nextDnsAt;
        private volatile long nextRouteAt;
        private volatile int interfaceFailures;
        private volatile int routeFailures;
        private volatile int dnsFailures;
        private volatile Boolean interfaceCapabilityEnabled;
        private volatile long capabilityExpiresAt;
    }
}
