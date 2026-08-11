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
package com.cloud.api.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.api.response.GuestNetworkSummaryResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.vm.guestnetwork.VmGuestNetworkApiService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.query.vo.UserVmJoinVO;

public class QueryManagerGuestNetworkTest {

    @Test
    public void testGuestNetworkSummariesAreLoadedInOneBatch() {
        QueryManagerImpl manager = new QueryManagerImpl();
        VmGuestNetworkApiService service = mock(VmGuestNetworkApiService.class);
        ReflectionTestUtils.setField(manager, "vmGuestNetworkApiService", service);

        UserVmJoinVO firstRow = row(41L, "vm-41");
        UserVmJoinVO secondRow = row(42L, "vm-42");
        UserVmResponse firstResponse = response("vm-41");
        UserVmResponse secondResponse = response("vm-42");
        GuestNetworkSummaryResponse firstSummary = new GuestNetworkSummaryResponse();
        GuestNetworkSummaryResponse secondSummary = new GuestNetworkSummaryResponse();
        Map<Long, GuestNetworkSummaryResponse> summaries = new HashMap<>();
        summaries.put(41L, firstSummary);
        summaries.put(42L, secondSummary);
        when(service.listSummaries(anyCollection())).thenReturn(summaries);

        ReflectionTestUtils.invokeMethod(manager, "attachGuestNetworkSummaries",
                Arrays.asList(firstRow, secondRow), Arrays.asList(firstResponse, secondResponse));

        ArgumentCaptor<Collection> ids = ArgumentCaptor.forClass(Collection.class);
        verify(service).listSummaries(ids.capture());
        assertEquals(2, ids.getValue().size());
        assertSame(firstSummary, firstResponse.getGuestNetwork());
        assertSame(secondSummary, secondResponse.getGuestNetwork());
    }

    private UserVmJoinVO row(long id, String uuid) {
        UserVmJoinVO row = mock(UserVmJoinVO.class);
        when(row.getId()).thenReturn(id);
        when(row.getUuid()).thenReturn(uuid);
        return row;
    }

    private UserVmResponse response(String id) {
        UserVmResponse response = new UserVmResponse();
        response.setId(id);
        return response;
    }
}
