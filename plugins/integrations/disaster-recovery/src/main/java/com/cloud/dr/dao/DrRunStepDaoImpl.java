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

import com.cloud.dr.DrRunStepVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrRunStepDaoImpl extends GenericDaoBase<DrRunStepVO, Long> implements DrRunStepDao {

    private final SearchBuilder<DrRunStepVO> activeByRunSearch;
    private final SearchBuilder<DrRunStepVO> activeByRunAndOrderSearch;
    private final SearchBuilder<DrRunStepVO> activeByRunAndNameSearch;

    public DrRunStepDaoImpl() {
        activeByRunSearch = createSearchBuilder();
        activeByRunSearch.and("runId", activeByRunSearch.entity().getRunId(), SearchCriteria.Op.EQ);
        activeByRunSearch.and("removed", activeByRunSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByRunSearch.done();

        activeByRunAndOrderSearch = createSearchBuilder();
        activeByRunAndOrderSearch.and("runId", activeByRunAndOrderSearch.entity().getRunId(), SearchCriteria.Op.EQ);
        activeByRunAndOrderSearch.and("stepOrder", activeByRunAndOrderSearch.entity().getStepOrder(), SearchCriteria.Op.EQ);
        activeByRunAndOrderSearch.and("removed", activeByRunAndOrderSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByRunAndOrderSearch.done();

        activeByRunAndNameSearch = createSearchBuilder();
        activeByRunAndNameSearch.and("runId", activeByRunAndNameSearch.entity().getRunId(), SearchCriteria.Op.EQ);
        activeByRunAndNameSearch.and("stepName", activeByRunAndNameSearch.entity().getStepName(), SearchCriteria.Op.EQ);
        activeByRunAndNameSearch.and("removed", activeByRunAndNameSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByRunAndNameSearch.done();
    }

    @Override
    public List<DrRunStepVO> listActiveByRunId(long runId) {
        SearchCriteria<DrRunStepVO> sc = activeByRunSearch.create();
        sc.setParameters("runId", runId);
        return listBy(sc, new Filter(DrRunStepVO.class, "stepOrder", true, null, null));
    }

    @Override
    public DrRunStepVO findActiveByRunIdAndStepOrder(long runId, int stepOrder) {
        SearchCriteria<DrRunStepVO> sc = activeByRunAndOrderSearch.create();
        sc.setParameters("runId", runId);
        sc.setParameters("stepOrder", stepOrder);
        return findOneBy(sc);
    }

    @Override
    public DrRunStepVO findActiveByRunIdAndStepName(long runId, String stepName) {
        SearchCriteria<DrRunStepVO> sc = activeByRunAndNameSearch.create();
        sc.setParameters("runId", runId);
        sc.setParameters("stepName", stepName);
        return findOneBy(sc);
    }
}
