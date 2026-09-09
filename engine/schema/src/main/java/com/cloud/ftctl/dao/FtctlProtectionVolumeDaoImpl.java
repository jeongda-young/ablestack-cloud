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

import com.cloud.ftctl.FtctlProtectionVolumeVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class FtctlProtectionVolumeDaoImpl extends GenericDaoBase<FtctlProtectionVolumeVO, Long> implements FtctlProtectionVolumeDao {

    private final SearchBuilder<FtctlProtectionVolumeVO> activeByProtectionSearch;
    private final SearchBuilder<FtctlProtectionVolumeVO> activeByProtectionAndPrimaryVolumeSearch;

    public FtctlProtectionVolumeDaoImpl() {
        activeByProtectionSearch = createSearchBuilder();
        activeByProtectionSearch.and("protectionId", activeByProtectionSearch.entity().getProtectionId(), SearchCriteria.Op.EQ);
        activeByProtectionSearch.and("removed", activeByProtectionSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByProtectionSearch.done();

        activeByProtectionAndPrimaryVolumeSearch = createSearchBuilder();
        activeByProtectionAndPrimaryVolumeSearch.and("protectionId", activeByProtectionAndPrimaryVolumeSearch.entity().getProtectionId(), SearchCriteria.Op.EQ);
        activeByProtectionAndPrimaryVolumeSearch.and("primaryVolumeId", activeByProtectionAndPrimaryVolumeSearch.entity().getPrimaryVolumeId(), SearchCriteria.Op.EQ);
        activeByProtectionAndPrimaryVolumeSearch.and("removed", activeByProtectionAndPrimaryVolumeSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByProtectionAndPrimaryVolumeSearch.done();
    }

    @Override
    public List<FtctlProtectionVolumeVO> listActiveByProtectionId(long protectionId) {
        SearchCriteria<FtctlProtectionVolumeVO> sc = activeByProtectionSearch.create();
        sc.setParameters("protectionId", protectionId);
        return listBy(sc);
    }

    @Override
    public FtctlProtectionVolumeVO findActiveByProtectionIdAndPrimaryVolumeId(long protectionId, long primaryVolumeId) {
        SearchCriteria<FtctlProtectionVolumeVO> sc = activeByProtectionAndPrimaryVolumeSearch.create();
        sc.setParameters("protectionId", protectionId);
        sc.setParameters("primaryVolumeId", primaryVolumeId);
        return findOneBy(sc);
    }
}
