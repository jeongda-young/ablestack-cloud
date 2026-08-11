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
package com.cloud.vm.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VmGuestNetworkStateVO;

@RunWith(MockitoJUnitRunner.class)
public class VmGuestNetworkStateDaoImplTest {
    @Spy
    private VmGuestNetworkStateDaoImpl dao;

    private SearchBuilder<VmGuestNetworkStateVO> searchBuilder;
    private SearchCriteria<VmGuestNetworkStateVO> searchCriteria;

    @Before
    public void setUp() {
        searchBuilder = mock(SearchBuilder.class);
        searchCriteria = mock(SearchCriteria.class);
        VmGuestNetworkStateVO searchEntity = mock(VmGuestNetworkStateVO.class);
        when(searchBuilder.entity()).thenReturn(searchEntity);
        when(searchBuilder.and(anyString(), any(), any(SearchCriteria.Op.class))).thenReturn(searchBuilder);
        when(searchBuilder.create()).thenReturn(searchCriteria);
        doReturn(searchBuilder).when(dao).createSearchBuilder();
        dao.init();
    }

    @Test
    public void testRemoveByVmIdUsesVmScopedCriteria() {
        doReturn(1).when(dao).expunge(searchCriteria);

        assertTrue(dao.removeByVmId(42L));

        verify(searchCriteria).setParameters("vmId", 42L);
        verify(dao).expunge(searchCriteria);
    }

    @Test
    public void testUpdateSnapshotUsesStateIdentifier() {
        VmGuestNetworkStateVO state = mock(VmGuestNetworkStateVO.class);
        when(state.getId()).thenReturn(7L);
        doReturn(true).when(dao).update(7L, state);

        assertTrue(dao.updateSnapshot(state));

        verify(dao).update(7L, state);
    }

    @Test
    public void testListByVmIdsUsesSingleInQuery() {
        VmGuestNetworkStateVO first = mock(VmGuestNetworkStateVO.class);
        VmGuestNetworkStateVO second = mock(VmGuestNetworkStateVO.class);
        List<VmGuestNetworkStateVO> states = Arrays.asList(first, second);
        doReturn(states).when(dao).listBy(searchCriteria);

        assertEquals(states, dao.listByVmIds(Arrays.asList(41L, 42L)));

        verify(searchCriteria).setParameters("vmIds", new Object[] {41L, 42L});
        verify(dao).listBy(searchCriteria);
        assertTrue(dao.listByVmIds(Collections.emptyList()).isEmpty());
    }
}
