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

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrRunVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrRunDaoImpl extends GenericDaoBase<DrRunVO, Long> implements DrRunDao {

    private final SearchBuilder<DrRunVO> activeByPlanSearch;
    private final SearchBuilder<DrRunVO> byPlanAndIdempotencySearch;
    private final SearchBuilder<DrRunVO> byPlanSearch;
    private final SearchBuilder<DrRunVO> byUuidSearch;
    private final SearchBuilder<DrRunVO> acceptedTestFailoverSearch;

    public DrRunDaoImpl() {
        activeByPlanSearch = createSearchBuilder();
        activeByPlanSearch.and("planId", activeByPlanSearch.entity().getPlanId(), SearchCriteria.Op.EQ);
        activeByPlanSearch.and("completed", activeByPlanSearch.entity().getCompleted(), SearchCriteria.Op.NULL);
        activeByPlanSearch.and("removed", activeByPlanSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByPlanSearch.done();

        byPlanAndIdempotencySearch = createSearchBuilder();
        byPlanAndIdempotencySearch.and("planId", byPlanAndIdempotencySearch.entity().getPlanId(), SearchCriteria.Op.EQ);
        byPlanAndIdempotencySearch.and("idempotencyKey", byPlanAndIdempotencySearch.entity().getIdempotencyKey(), SearchCriteria.Op.EQ);
        byPlanAndIdempotencySearch.and("removed", byPlanAndIdempotencySearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        byPlanAndIdempotencySearch.done();

        byPlanSearch = createSearchBuilder();
        byPlanSearch.and("planId", byPlanSearch.entity().getPlanId(), SearchCriteria.Op.EQ);
        byPlanSearch.and("removed", byPlanSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        byPlanSearch.done();

        byUuidSearch = createSearchBuilder();
        byUuidSearch.and("uuid", byUuidSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        byUuidSearch.and("removed", byUuidSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        byUuidSearch.done();

        acceptedTestFailoverSearch = createSearchBuilder();
        acceptedTestFailoverSearch.and("state", acceptedTestFailoverSearch.entity().getState(), SearchCriteria.Op.EQ);
        acceptedTestFailoverSearch.and("runType", acceptedTestFailoverSearch.entity().getRunType(), SearchCriteria.Op.EQ);
        acceptedTestFailoverSearch.and("completed", acceptedTestFailoverSearch.entity().getCompleted(), SearchCriteria.Op.NULL);
        acceptedTestFailoverSearch.and("removed", acceptedTestFailoverSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        acceptedTestFailoverSearch.done();
    }

    @Override
    public DrRunVO findActiveByPlanId(long planId) {
        SearchCriteria<DrRunVO> sc = activeByPlanSearch.create();
        sc.setParameters("planId", planId);
        return findOneBy(sc);
    }

    @Override
    public DrRunVO findByPlanIdAndIdempotencyKey(long planId, String idempotencyKey) {
        SearchCriteria<DrRunVO> sc = byPlanAndIdempotencySearch.create();
        sc.setParameters("planId", planId);
        sc.setParameters("idempotencyKey", idempotencyKey);
        return findOneBy(sc);
    }

    @Override
    public List<DrRunVO> listByPlanId(long planId) {
        SearchCriteria<DrRunVO> sc = byPlanSearch.create();
        sc.setParameters("planId", planId);
        return listBy(sc, new Filter(DrRunVO.class, "created", false, null, null));
    }

    @Override
    public DrRunVO findLatestByPlanId(long planId) {
        SearchCriteria<DrRunVO> sc = byPlanSearch.create();
        sc.setParameters("planId", planId);
        List<DrRunVO> runs = listBy(sc, new Filter(DrRunVO.class, "created", false, 0L, 1L));
        return runs != null && !runs.isEmpty() ? runs.get(0) : null;
    }

    @Override
    public DrRunVO findByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        SearchCriteria<DrRunVO> sc = byUuidSearch.create();
        sc.setParameters("uuid", uuid);
        return findOneBy(sc);
    }

    @Override
    public DrRunVO findLatestProtectionProducerByPlanId(long planId) {
        List<DrRunVO> runs = listByPlanId(planId);
        if (runs == null) {
            return null;
        }
        return runs.stream()
                .filter(run -> StringUtils.equalsAnyIgnoreCase(run.getRunType(),
                        DrConstants.RUN_TYPE_SYNC, DrConstants.RUN_TYPE_REPROTECT))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<DrRunVO> listAcceptedTestFailoverRuns() {
        SearchCriteria<DrRunVO> sc = acceptedTestFailoverSearch.create();
        sc.setParameters("state", DrConstants.RUN_STATE_ACCEPTED);
        sc.setParameters("runType", DrConstants.RUN_TYPE_TEST_FAILOVER);
        return listBy(sc);
    }
}
