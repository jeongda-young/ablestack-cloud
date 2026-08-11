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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.VmGuestNetworkSectionStatus;
import com.cloud.agent.api.VmGuestNetworkState;
import com.cloud.vm.VmGuestNetworkSectionStateVO;
import com.cloud.vm.dao.VmGuestNetworkSectionStateDao;

@RunWith(MockitoJUnitRunner.class)
public class VmGuestNetworkScheduleServiceImplTest {
    private static final long VM_ID = 42L;
    private static final Date NOW = new Date(1_000_000L);

    @Mock
    private VmGuestNetworkSectionStateDao sectionDao;

    @InjectMocks
    private VmGuestNetworkScheduleServiceImpl service;

    private List<VmGuestNetworkSectionStateVO> rows;

    @Before
    public void setUp() {
        rows = Arrays.asList(
                row(VmGuestNetworkScheduleServiceImpl.INTERFACES),
                row(VmGuestNetworkScheduleServiceImpl.ROUTES),
                row(VmGuestNetworkScheduleServiceImpl.DNS),
                row(VmGuestNetworkScheduleServiceImpl.READINESS));
        when(sectionDao.listByVmId(VM_ID)).thenReturn(rows);
        when(sectionDao.listByVmIds(Collections.singleton(VM_ID))).thenReturn(rows);
    }

    @Test
    public void testSuccessfulSectionsReceiveIndependentNextDueTimes() {
        VmGuestNetworkState state = new VmGuestNetworkState();
        Map<String, VmGuestNetworkSectionStatus> statuses = new LinkedHashMap<>();
        rows.forEach(row -> statuses.put(row.getSection(),
                sectionStatus("OK", "QGA_STANDARD")));
        state.setSectionStatuses(statuses);

        service.complete(VM_ID, state, NOW, 60L, 120L, 300L, 0, 3600L);

        assertEquals(new Date(NOW.getTime() + 60_000L), rows.get(0).getNextDueAt());
        assertEquals(new Date(NOW.getTime() + 120_000L), rows.get(1).getNextDueAt());
        assertEquals(new Date(NOW.getTime() + 300_000L), rows.get(2).getNextDueAt());
        assertEquals(new Date(NOW.getTime() + 120_000L), rows.get(3).getNextDueAt());
        rows.forEach(row -> {
            assertEquals(0, row.getFailureCount());
            assertEquals(NOW, row.getLastSuccessAt());
            assertNull(row.getLeaseOwner());
        });
    }

    @Test
    public void testFailureBackoffIsBoundedAndKeepsLastGoodPayload() {
        VmGuestNetworkSectionStateVO row = rows.get(0);
        row.setPayload("last-good");
        row.setPayloadHash("last-good-hash");
        row.setLeaseOwner("collector-a");
        row.setLeaseUntil(new Date(NOW.getTime() + 30_000L));

        service.fail(VM_ID, "AGENT_UNAVAILABLE", "host timeout", NOW,
                60L, 120L, 300L, 0, 90L);

        assertEquals("UNAVAILABLE", row.getStatus());
        assertEquals(1, row.getFailureCount());
        assertEquals(new Date(NOW.getTime() + 60_000L), row.getNextDueAt());
        assertEquals("last-good", row.getPayload());
        assertEquals("last-good-hash", row.getPayloadHash());
        assertNull(row.getLeaseOwner());

        row.setLeaseOwner("collector-a");
        row.setLeaseUntil(new Date(NOW.getTime() + 30_000L));
        service.fail(VM_ID, "AGENT_UNAVAILABLE", "host timeout", NOW,
                60L, 120L, 300L, 0, 90L);
        assertEquals(2, row.getFailureCount());
        assertEquals(new Date(NOW.getTime() + 90_000L), row.getNextDueAt());
    }

    @Test
    public void testManualRefreshIsQueuedOnceWithinCooldown() {
        Date future = new Date(NOW.getTime() + 300_000L);
        rows.forEach(row -> {
            row.setNextDueAt(future);
            row.setUpdated(new Date(NOW.getTime() - 60_000L));
        });

        assertTrue(service.requestRefresh(VM_ID,
                Collections.singleton(VmGuestNetworkScheduleServiceImpl.INTERFACES),
                NOW, 30));
        assertEquals(NOW, rows.get(0).getNextDueAt());
        assertFalse(service.requestRefresh(VM_ID,
                Collections.singleton(VmGuestNetworkScheduleServiceImpl.INTERFACES),
                new Date(NOW.getTime() + 5_000L), 30));
    }

    private VmGuestNetworkSectionStateVO row(String section) {
        VmGuestNetworkSectionStateVO row =
                new VmGuestNetworkSectionStateVO(VM_ID, section, NOW);
        row.setLeaseOwner("collector-a");
        row.setLeaseUntil(new Date(NOW.getTime() + 30_000L));
        return row;
    }

    private VmGuestNetworkSectionStatus sectionStatus(String status, String source) {
        VmGuestNetworkSectionStatus result = new VmGuestNetworkSectionStatus();
        result.setStatus(status);
        result.setSource(source);
        return result;
    }
}
