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

import com.cloud.dr.DrPlanVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrPlanDaoImpl extends GenericDaoBase<DrPlanVO, Long> implements DrPlanDao {

    private final SearchBuilder<DrPlanVO> activeSearch;
    private final SearchBuilder<DrPlanVO> activeBySourceVmSearch;
    private final SearchBuilder<DrPlanVO> activeBySourceSiteExternalRefSearch;
    private final SearchBuilder<DrPlanVO> activeByEngineBindingSearch;
    private final SearchBuilder<DrPlanVO> activeByStateSearch;
    private final SearchBuilder<DrPlanVO> activeFilteredSearch;

    public DrPlanDaoImpl() {
        activeSearch = createSearchBuilder();
        activeSearch.and("removed", activeSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeSearch.done();

        activeBySourceVmSearch = createSearchBuilder();
        activeBySourceVmSearch.and("sourceVmId", activeBySourceVmSearch.entity().getSourceVmId(), SearchCriteria.Op.EQ);
        activeBySourceVmSearch.and("removed", activeBySourceVmSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeBySourceVmSearch.done();

        activeBySourceSiteExternalRefSearch = createSearchBuilder();
        activeBySourceSiteExternalRefSearch.and("sourceSiteId", activeBySourceSiteExternalRefSearch.entity().getSourceSiteId(), SearchCriteria.Op.EQ);
        activeBySourceSiteExternalRefSearch.and("sourceExternalRef", activeBySourceSiteExternalRefSearch.entity().getSourceExternalRef(), SearchCriteria.Op.EQ);
        activeBySourceSiteExternalRefSearch.and("removed", activeBySourceSiteExternalRefSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeBySourceSiteExternalRefSearch.done();

        activeByEngineBindingSearch = createSearchBuilder();
        activeByEngineBindingSearch.and("engineBindingType", activeByEngineBindingSearch.entity().getEngineBindingType(), SearchCriteria.Op.EQ);
        activeByEngineBindingSearch.and("engineBindingId", activeByEngineBindingSearch.entity().getEngineBindingId(), SearchCriteria.Op.EQ);
        activeByEngineBindingSearch.and("removed", activeByEngineBindingSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByEngineBindingSearch.done();

        activeByStateSearch = createSearchBuilder();
        activeByStateSearch.and("state", activeByStateSearch.entity().getState(), SearchCriteria.Op.EQ);
        activeByStateSearch.and("removed", activeByStateSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByStateSearch.done();

        activeFilteredSearch = createSearchBuilder();
        activeFilteredSearch.and("id", activeFilteredSearch.entity().getId(), SearchCriteria.Op.EQ);
        activeFilteredSearch.and("state", activeFilteredSearch.entity().getState(), SearchCriteria.Op.EQ);
        activeFilteredSearch.and("sourceSiteId", activeFilteredSearch.entity().getSourceSiteId(), SearchCriteria.Op.EQ);
        activeFilteredSearch.and("targetSiteId", activeFilteredSearch.entity().getTargetSiteId(), SearchCriteria.Op.EQ);
        activeFilteredSearch.and("direction", activeFilteredSearch.entity().getDirection(), SearchCriteria.Op.EQ);
        activeFilteredSearch.and("engineType", activeFilteredSearch.entity().getEngineType(), SearchCriteria.Op.EQ);
        activeFilteredSearch.and("removed", activeFilteredSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeFilteredSearch.done();
    }

    @Override
    public DrPlanVO findActiveBySourceVmId(long sourceVmId) {
        SearchCriteria<DrPlanVO> sc = activeBySourceVmSearch.create();
        sc.setParameters("sourceVmId", sourceVmId);
        return findOneBy(sc);
    }

    @Override
    public List<DrPlanVO> listActiveBySourceVmId(long sourceVmId) {
        SearchCriteria<DrPlanVO> sc = activeBySourceVmSearch.create();
        sc.setParameters("sourceVmId", sourceVmId);
        return listBy(sc);
    }

    @Override
    public DrPlanVO findActiveBySourceSiteAndExternalRef(long sourceSiteId, String sourceExternalRef) {
        SearchCriteria<DrPlanVO> sc = activeBySourceSiteExternalRefSearch.create();
        sc.setParameters("sourceSiteId", sourceSiteId);
        sc.setParameters("sourceExternalRef", sourceExternalRef);
        return findOneBy(sc);
    }

    @Override
    public DrPlanVO findActiveByEngineBinding(String engineBindingType, long engineBindingId) {
        SearchCriteria<DrPlanVO> sc = activeByEngineBindingSearch.create();
        sc.setParameters("engineBindingType", engineBindingType);
        sc.setParameters("engineBindingId", engineBindingId);
        return findOneBy(sc);
    }

    @Override
    public List<DrPlanVO> listActive() {
        return listBy(activeSearch.create());
    }

    @Override
    public List<DrPlanVO> listActiveByState(String state) {
        SearchCriteria<DrPlanVO> sc = activeByStateSearch.create();
        sc.setParameters("state", state);
        return listBy(sc);
    }

    @Override
    public Pair<List<DrPlanVO>, Integer> searchActive(Long id, String keyword, String state, Long sourceSiteId,
            Long targetSiteId, String direction, String engineType, Long offset, Long limit) {
        SearchCriteria<DrPlanVO> sc = activeFilteredSearch.create();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (state != null) {
            sc.setParameters("state", state);
        }
        if (sourceSiteId != null) {
            sc.setParameters("sourceSiteId", sourceSiteId);
        }
        if (targetSiteId != null) {
            sc.setParameters("targetSiteId", targetSiteId);
        }
        if (direction != null) {
            sc.setParameters("direction", direction);
        }
        if (engineType != null) {
            sc.setParameters("engineType", engineType);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String value = "%" + keyword.trim() + "%";
            SearchCriteria<DrPlanVO> keywordCriteria = createSearchCriteria();
            keywordCriteria.addOr("name", SearchCriteria.Op.LIKE, value);
            keywordCriteria.addOr("description", SearchCriteria.Op.LIKE, value);
            keywordCriteria.addOr("uuid", SearchCriteria.Op.LIKE, value);
            sc.addAnd("name", SearchCriteria.Op.SC, keywordCriteria);
        }
        Filter filter = new Filter(DrPlanVO.class, "created", false,
                offset != null ? offset : 0L, limit != null ? limit : 20L);
        return searchAndCount(sc, filter);
    }

    @Override
    public long countActiveBySiteId(long siteId) {
        long count = 0L;
        for (DrPlanVO plan : listActive()) {
            if (plan.getSourceSiteId() == siteId || plan.getTargetSiteId() == siteId) {
                count++;
            }
        }
        return count;
    }
}
