// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
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
import com.cloud.vm.VmGuestNetworkSectionStateVO;

@Component
public class VmGuestNetworkSectionStateDaoImpl
        extends GenericDaoBase<VmGuestNetworkSectionStateVO, Long>
        implements VmGuestNetworkSectionStateDao {
    private SearchBuilder<VmGuestNetworkSectionStateVO> vmSearch;
    private SearchBuilder<VmGuestNetworkSectionStateVO> vmIdsSearch;
    private SearchBuilder<VmGuestNetworkSectionStateVO> vmSectionSearch;

    @PostConstruct
    protected void init() {
        vmSearch = createSearchBuilder();
        vmSearch.and("vmId", vmSearch.entity().getVmId(), Op.EQ);
        vmSearch.done();
        vmIdsSearch = createSearchBuilder();
        vmIdsSearch.and("vmIds", vmIdsSearch.entity().getVmId(), Op.IN);
        vmIdsSearch.done();
        vmSectionSearch = createSearchBuilder();
        vmSectionSearch.and("vmId", vmSectionSearch.entity().getVmId(), Op.EQ);
        vmSectionSearch.and("section", vmSectionSearch.entity().getSection(), Op.EQ);
        vmSectionSearch.done();
    }

    @Override
    public List<VmGuestNetworkSectionStateVO> listByVmId(long vmId) {
        SearchCriteria<VmGuestNetworkSectionStateVO> criteria = vmSearch.create();
        criteria.setParameters("vmId", vmId);
        return listBy(criteria);
    }

    @Override
    public List<VmGuestNetworkSectionStateVO> listByVmIds(Collection<Long> vmIds) {
        if (vmIds == null || vmIds.isEmpty()) {
            return Collections.emptyList();
        }
        SearchCriteria<VmGuestNetworkSectionStateVO> criteria = vmIdsSearch.create();
        criteria.setParameters("vmIds", vmIds.toArray());
        return listBy(criteria);
    }

    @Override
    public VmGuestNetworkSectionStateVO findByVmIdAndSection(long vmId, String section) {
        SearchCriteria<VmGuestNetworkSectionStateVO> criteria = vmSectionSearch.create();
        criteria.setParameters("vmId", vmId);
        criteria.setParameters("section", section);
        return findOneBy(criteria);
    }
}
