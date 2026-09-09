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
package com.cloud.dr.dao;

import java.util.List;

import com.cloud.dr.DrReplicaDiskVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrReplicaDiskDaoImpl extends GenericDaoBase<DrReplicaDiskVO, Long> implements DrReplicaDiskDao {

    private final SearchBuilder<DrReplicaDiskVO> activeByReplicaSearch;
    private final SearchBuilder<DrReplicaDiskVO> activeByReplicaAndSourceVolumeSearch;
    private final SearchBuilder<DrReplicaDiskVO> byTargetVolumeSearch;

    public DrReplicaDiskDaoImpl() {
        activeByReplicaSearch = createSearchBuilder();
        activeByReplicaSearch.and("replicaId", activeByReplicaSearch.entity().getReplicaId(), SearchCriteria.Op.EQ);
        activeByReplicaSearch.and("removed", activeByReplicaSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByReplicaSearch.done();

        activeByReplicaAndSourceVolumeSearch = createSearchBuilder();
        activeByReplicaAndSourceVolumeSearch.and("replicaId", activeByReplicaAndSourceVolumeSearch.entity().getReplicaId(), SearchCriteria.Op.EQ);
        activeByReplicaAndSourceVolumeSearch.and("sourceVolumeId", activeByReplicaAndSourceVolumeSearch.entity().getSourceVolumeId(), SearchCriteria.Op.EQ);
        activeByReplicaAndSourceVolumeSearch.and("removed", activeByReplicaAndSourceVolumeSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByReplicaAndSourceVolumeSearch.done();

        byTargetVolumeSearch = createSearchBuilder();
        byTargetVolumeSearch.and("targetVolumeId", byTargetVolumeSearch.entity().getTargetVolumeId(), SearchCriteria.Op.EQ);
        byTargetVolumeSearch.done();
    }

    @Override
    public List<DrReplicaDiskVO> listActiveByReplicaId(long replicaId) {
        SearchCriteria<DrReplicaDiskVO> sc = activeByReplicaSearch.create();
        sc.setParameters("replicaId", replicaId);
        return listBy(sc);
    }

    @Override
    public DrReplicaDiskVO findActiveByReplicaIdAndSourceVolumeId(long replicaId, long sourceVolumeId) {
        SearchCriteria<DrReplicaDiskVO> sc = activeByReplicaAndSourceVolumeSearch.create();
        sc.setParameters("replicaId", replicaId);
        sc.setParameters("sourceVolumeId", sourceVolumeId);
        return findOneBy(sc);
    }

    @Override
    public List<DrReplicaDiskVO> listByTargetVolumeId(long targetVolumeId) {
        SearchCriteria<DrReplicaDiskVO> sc = byTargetVolumeSearch.create();
        sc.setParameters("targetVolumeId", targetVolumeId);
        return listBy(sc);
    }
}
