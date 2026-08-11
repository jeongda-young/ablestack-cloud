// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
package com.cloud.vm.guestnetwork;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.agent.api.VmGuestNetworkState;
import com.cloud.vm.VmGuestNetworkSectionStateVO;

public interface VmGuestNetworkScheduleService {
    Map<Long, DueWork> findDueWork(Collection<Long> vmIds, Date now);
    void claim(Collection<Long> vmIds, String leaseOwner, Date now, int leaseSeconds);
    Set<String> getClaimedSections(long vmId, String leaseOwner, Date now);
    void complete(long vmId, VmGuestNetworkState state, Date observedAt,
            long interfaceIntervalSeconds, long routeIntervalSeconds,
            long dnsIntervalSeconds, int jitterPercent, long maxBackoffSeconds);
    void fail(long vmId, String errorCode, String errorMessage, Date observedAt,
            long interfaceIntervalSeconds, long routeIntervalSeconds,
            long dnsIntervalSeconds, int jitterPercent, long maxBackoffSeconds);
    boolean requestRefresh(long vmId, Set<String> sections, Date now, int cooldownSeconds);
    void invalidateFailedSections(long vmId, Date now);
    List<VmGuestNetworkSectionStateVO> listByVmId(long vmId);

    final class DueWork {
        private final Set<String> sections;
        private final Date oldestDueAt;

        public DueWork(Set<String> sections, Date oldestDueAt) {
            this.sections = sections;
            this.oldestDueAt = oldestDueAt;
        }

        public Set<String> getSections() {
            return sections;
        }

        public Date getOldestDueAt() {
            return oldestDueAt;
        }
    }
}
