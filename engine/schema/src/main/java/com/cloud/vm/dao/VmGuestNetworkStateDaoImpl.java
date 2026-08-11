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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.vm.VmGuestNetworkStateVO;

@Component
public class VmGuestNetworkStateDaoImpl extends GenericDaoBase<VmGuestNetworkStateVO, Long>
        implements VmGuestNetworkStateDao {
    private SearchBuilder<VmGuestNetworkStateVO> vmIdSearch;
    private SearchBuilder<VmGuestNetworkStateVO> vmIdsSearch;

    @PostConstruct
    protected void init() {
        vmIdSearch = createSearchBuilder();
        vmIdSearch.and("vmId", vmIdSearch.entity().getVmId(), Op.EQ);
        vmIdSearch.done();
        vmIdsSearch = createSearchBuilder();
        vmIdsSearch.and("vmIds", vmIdsSearch.entity().getVmId(), Op.IN);
        vmIdsSearch.done();
    }

    @Override
    public VmGuestNetworkStateVO findByVmId(long vmId) {
        return findOneBy(vmIdCriteria(vmId));
    }

    @Override
    public List<VmGuestNetworkStateVO> listByVmIds(Collection<Long> vmIds) {
        if (vmIds == null || vmIds.isEmpty()) {
            return Collections.emptyList();
        }
        SearchCriteria<VmGuestNetworkStateVO> criteria = vmIdsSearch.create();
        criteria.setParameters("vmIds", vmIds.toArray());
        return listBy(criteria);
    }

    @Override
    public boolean updateSnapshot(VmGuestNetworkStateVO state) {
        return update(state.getId(), state);
    }

    @Override
    public boolean updateMetadata(VmGuestNetworkStateVO state) {
        UpdateBuilder builder = getUpdateBuilder(state);
        builder.set(state, "schemaVersion", state.getSchemaVersion());
        builder.set(state, "status", state.getStatus());
        builder.set(state, "qgaVersion", state.getQgaVersion());
        builder.set(state, "collectorBuildId", state.getCollectorBuildId());
        builder.set(state, "collectorHostId", state.getCollectorHostId());
        builder.set(state, "capabilityHash", state.getCapabilityHash());
        builder.set(state, "guestToolsVersion", state.getGuestToolsVersion());
        builder.set(state, "qgaPolicyMode", state.getQgaPolicyMode());
        builder.set(state, "readinessStatus", state.getReadinessStatus());
        builder.set(state, "readinessCheckedAt", state.getReadinessCheckedAt());
        builder.set(state, "observedAt", state.getObservedAt());
        builder.set(state, "lastSuccessAt", state.getLastSuccessAt());
        builder.set(state, "errorCode", state.getErrorCode());
        builder.set(state, "errorMessage", state.getErrorMessage());
        builder.set(state, "updated", state.getUpdated());
        return update(state, vmIdCriteria(state.getVmId())) > 0;
    }

    @Override
    public boolean removeByVmId(long vmId) {
        return expunge(vmIdCriteria(vmId)) > 0;
    }

    private SearchCriteria<VmGuestNetworkStateVO> vmIdCriteria(long vmId) {
        SearchCriteria<VmGuestNetworkStateVO> criteria = vmIdSearch.create();
        criteria.setParameters("vmId", vmId);
        return criteria;
    }
}
