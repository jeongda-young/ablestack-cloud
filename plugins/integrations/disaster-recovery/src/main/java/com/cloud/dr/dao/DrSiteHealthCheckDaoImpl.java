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

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.DrSiteHealthCheckVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrSiteHealthCheckDaoImpl extends GenericDaoBase<DrSiteHealthCheckVO, Long> implements DrSiteHealthCheckDao {

    private final SearchBuilder<DrSiteHealthCheckVO> historySearch;
    private final SearchBuilder<DrSiteHealthCheckVO> cleanupSearch;

    public DrSiteHealthCheckDaoImpl() {
        historySearch = createSearchBuilder();
        historySearch.and("siteId", historySearch.entity().getSiteId(), SearchCriteria.Op.EQ);
        historySearch.and("healthState", historySearch.entity().getHealthState(), SearchCriteria.Op.EQ);
        historySearch.and("triggerType", historySearch.entity().getTriggerType(), SearchCriteria.Op.EQ);
        historySearch.and("startDate", historySearch.entity().getCheckedAt(), SearchCriteria.Op.GTEQ);
        historySearch.and("endDate", historySearch.entity().getCheckedAt(), SearchCriteria.Op.LTEQ);
        historySearch.done();

        cleanupSearch = createSearchBuilder();
        cleanupSearch.and("checkedBefore", cleanupSearch.entity().getCheckedAt(), SearchCriteria.Op.LT);
        cleanupSearch.done();
    }

    @Override
    public Pair<List<DrSiteHealthCheckVO>, Integer> searchBySite(long siteId, String healthState, String triggerType, Date startDate, Date endDate, Filter filter) {
        SearchCriteria<DrSiteHealthCheckVO> sc = historySearch.create();
        sc.setParameters("siteId", siteId);
        if (StringUtils.isNotBlank(healthState)) {
            sc.setParameters("healthState", StringUtils.upperCase(healthState));
        }
        if (StringUtils.isNotBlank(triggerType)) {
            sc.setParameters("triggerType", StringUtils.upperCase(triggerType));
        }
        if (startDate != null) {
            sc.setParameters("startDate", startDate);
        }
        if (endDate != null) {
            sc.setParameters("endDate", endDate);
        }
        Filter effectiveFilter = filter != null ? filter : new Filter(DrSiteHealthCheckVO.class, "checkedAt", false, null, null);
        return searchAndCount(sc, effectiveFilter);
    }

    @Override
    public int expungeOlderThan(Date checkedBefore, long batchSize) {
        if (checkedBefore == null) {
            return 0;
        }
        SearchCriteria<DrSiteHealthCheckVO> sc = cleanupSearch.create();
        sc.setParameters("checkedBefore", checkedBefore);
        return batchExpunge(sc, batchSize);
    }
}
