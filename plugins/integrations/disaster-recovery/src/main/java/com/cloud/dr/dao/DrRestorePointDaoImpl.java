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

import com.cloud.dr.DrRestorePointVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrRestorePointDaoImpl extends GenericDaoBase<DrRestorePointVO, Long> implements DrRestorePointDao {

    private final SearchBuilder<DrRestorePointVO> activeByPlanSearch;
    private final SearchBuilder<DrRestorePointVO> latestTargetReadySearch;
    private final SearchBuilder<DrRestorePointVO> sourceSnapshotRefSearch;
    private final SearchBuilder<DrRestorePointVO> checkpointRefHashSearch;

    public DrRestorePointDaoImpl() {
        activeByPlanSearch = createSearchBuilder();
        activeByPlanSearch.and("planId", activeByPlanSearch.entity().getPlanId(), SearchCriteria.Op.EQ);
        activeByPlanSearch.and("removed", activeByPlanSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByPlanSearch.done();

        latestTargetReadySearch = createSearchBuilder();
        latestTargetReadySearch.and("planId", latestTargetReadySearch.entity().getPlanId(), SearchCriteria.Op.EQ);
        latestTargetReadySearch.and("targetReadyAt", latestTargetReadySearch.entity().getTargetReadyAt(), SearchCriteria.Op.NNULL);
        latestTargetReadySearch.and("removed", latestTargetReadySearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        latestTargetReadySearch.done();

        sourceSnapshotRefSearch = createSearchBuilder();
        sourceSnapshotRefSearch.and("planId", sourceSnapshotRefSearch.entity().getPlanId(), SearchCriteria.Op.EQ);
        sourceSnapshotRefSearch.and("sourceSnapshotRef", sourceSnapshotRefSearch.entity().getSourceSnapshotRef(), SearchCriteria.Op.EQ);
        sourceSnapshotRefSearch.and("removed", sourceSnapshotRefSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        sourceSnapshotRefSearch.done();

        checkpointRefHashSearch = createSearchBuilder();
        checkpointRefHashSearch.and("planId", checkpointRefHashSearch.entity().getPlanId(), SearchCriteria.Op.EQ);
        checkpointRefHashSearch.and("checkpointRefHash", checkpointRefHashSearch.entity().getCheckpointRefHash(), SearchCriteria.Op.EQ);
        checkpointRefHashSearch.and("removed", checkpointRefHashSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        checkpointRefHashSearch.done();
    }

    @Override
    public List<DrRestorePointVO> listActiveByPlanId(long planId) {
        SearchCriteria<DrRestorePointVO> sc = activeByPlanSearch.create();
        sc.setParameters("planId", planId);
        return listBy(sc, new Filter(DrRestorePointVO.class, "targetReadyAt", false, null, null));
    }

    @Override
    public List<DrRestorePointVO> listActiveByPlanId(long planId, long startIndex, long pageSize) {
        SearchCriteria<DrRestorePointVO> sc = activeByPlanSearch.create();
        sc.setParameters("planId", planId);
        return listBy(sc, new Filter(DrRestorePointVO.class, "targetReadyAt", false, startIndex, pageSize));
    }

    @Override
    public long countActiveByPlanId(long planId) {
        SearchCriteria<DrRestorePointVO> sc = activeByPlanSearch.create();
        sc.setParameters("planId", planId);
        return getCount(sc);
    }

    @Override
    public DrRestorePointVO findLatestTargetReadyByPlanId(long planId) {
        SearchCriteria<DrRestorePointVO> sc = latestTargetReadySearch.create();
        sc.setParameters("planId", planId);
        List<DrRestorePointVO> restorePoints = listBy(sc, new Filter(DrRestorePointVO.class, "targetReadyAt", false, 0L, 1L));
        return restorePoints.isEmpty() ? null : restorePoints.get(0);
    }

    @Override
    public DrRestorePointVO findByPlanIdAndSourceSnapshotRef(long planId, String sourceSnapshotRef) {
        SearchCriteria<DrRestorePointVO> sc = sourceSnapshotRefSearch.create();
        sc.setParameters("planId", planId);
        sc.setParameters("sourceSnapshotRef", sourceSnapshotRef);
        return findOneBy(sc);
    }

    @Override
    public DrRestorePointVO findByPlanIdAndCheckpointRefHash(long planId, byte[] checkpointRefHash) {
        SearchCriteria<DrRestorePointVO> sc = checkpointRefHashSearch.create();
        sc.setParameters("planId", planId);
        sc.setParameters("checkpointRefHash", checkpointRefHash);
        return findOneBy(sc);
    }
}
