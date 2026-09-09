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
package com.cloud.ftctl.dao;

import java.util.List;

import com.cloud.ftctl.FtctlProtectionVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class FtctlProtectionDaoImpl extends GenericDaoBase<FtctlProtectionVO, Long> implements FtctlProtectionDao {

    private final SearchBuilder<FtctlProtectionVO> activeByPrimaryVmSearch;
    private final SearchBuilder<FtctlProtectionVO> activeBySecondaryVmSearch;
    private final SearchBuilder<FtctlProtectionVO> activeSearch;
    private final SearchBuilder<FtctlProtectionVO> activeByPeerHostSearch;
    private final SearchBuilder<FtctlProtectionVO> activeByProtectionStateSearch;

    public FtctlProtectionDaoImpl() {
        activeByPrimaryVmSearch = createSearchBuilder();
        activeByPrimaryVmSearch.and("primaryVmId", activeByPrimaryVmSearch.entity().getPrimaryVmId(), SearchCriteria.Op.EQ);
        activeByPrimaryVmSearch.and("removed", activeByPrimaryVmSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByPrimaryVmSearch.done();

        activeBySecondaryVmSearch = createSearchBuilder();
        activeBySecondaryVmSearch.and("secondaryVmId", activeBySecondaryVmSearch.entity().getSecondaryVmId(), SearchCriteria.Op.EQ);
        activeBySecondaryVmSearch.and("removed", activeBySecondaryVmSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeBySecondaryVmSearch.done();

        activeSearch = createSearchBuilder();
        activeSearch.and("removed", activeSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeSearch.done();

        activeByPeerHostSearch = createSearchBuilder();
        activeByPeerHostSearch.and("peerHostId", activeByPeerHostSearch.entity().getPeerHostId(), SearchCriteria.Op.EQ);
        activeByPeerHostSearch.and("removed", activeByPeerHostSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByPeerHostSearch.done();

        activeByProtectionStateSearch = createSearchBuilder();
        activeByProtectionStateSearch.and("protectionState", activeByProtectionStateSearch.entity().getProtectionState(), SearchCriteria.Op.EQ);
        activeByProtectionStateSearch.and("removed", activeByProtectionStateSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByProtectionStateSearch.done();
    }

    @Override
    public FtctlProtectionVO findActiveByPrimaryVmId(long primaryVmId) {
        SearchCriteria<FtctlProtectionVO> sc = activeByPrimaryVmSearch.create();
        sc.setParameters("primaryVmId", primaryVmId);
        return findOneBy(sc);
    }

    @Override
    public FtctlProtectionVO findActiveBySecondaryVmId(long secondaryVmId) {
        SearchCriteria<FtctlProtectionVO> sc = activeBySecondaryVmSearch.create();
        sc.setParameters("secondaryVmId", secondaryVmId);
        return findOneBy(sc);
    }

    @Override
    public List<FtctlProtectionVO> listActive() {
        return listBy(activeSearch.create());
    }

    @Override
    public List<FtctlProtectionVO> listActiveByPeerHostId(long peerHostId) {
        SearchCriteria<FtctlProtectionVO> sc = activeByPeerHostSearch.create();
        sc.setParameters("peerHostId", peerHostId);
        return listBy(sc);
    }

    @Override
    public List<FtctlProtectionVO> listActiveByProtectionState(String protectionState) {
        SearchCriteria<FtctlProtectionVO> sc = activeByProtectionStateSearch.create();
        sc.setParameters("protectionState", protectionState);
        return listBy(sc);
    }
}
