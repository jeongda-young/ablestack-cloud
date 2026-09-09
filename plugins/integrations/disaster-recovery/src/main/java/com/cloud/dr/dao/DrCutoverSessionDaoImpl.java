// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
package com.cloud.dr.dao;

import java.util.List;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrCutoverSessionVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrCutoverSessionDaoImpl extends GenericDaoBase<DrCutoverSessionVO, Long> implements DrCutoverSessionDao {
    private final SearchBuilder<DrCutoverSessionVO> activeByRun;
    private final SearchBuilder<DrCutoverSessionVO> activeByPlan;
    public DrCutoverSessionDaoImpl() {
        activeByRun = createSearchBuilder();
        activeByRun.and("runId", activeByRun.entity().getRunId(), SearchCriteria.Op.EQ);
        activeByRun.and("removed", activeByRun.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByRun.done();
        activeByPlan = createSearchBuilder();
        activeByPlan.and("planId", activeByPlan.entity().getPlanId(), SearchCriteria.Op.EQ);
        activeByPlan.and("removed", activeByPlan.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByPlan.done();
    }
    @Override public DrCutoverSessionVO findActiveByRunId(long runId) {
        SearchCriteria<DrCutoverSessionVO> sc = activeByRun.create(); sc.setParameters("runId", runId); return findOneBy(sc);
    }

    @Override
    public DrCutoverSessionVO findLatestActiveByPlanId(long planId) {
        return findCurrentAuthorityByPlanId(planId);
    }

    @Override
    public DrCutoverSessionVO findCurrentAuthorityByPlanId(long planId) {
        SearchCriteria<DrCutoverSessionVO> sc = activeByPlan.create();
        sc.setParameters("planId", planId);
        List<DrCutoverSessionVO> sessions = listBy(sc);
        DrCutoverSessionVO latest = null;
        for (DrCutoverSessionVO session : sessions) {
            if (session.getAuthorityEndedAt() == null && !isTerminalAuthorityState(session.getState())
                    && (latest == null || session.getId() > latest.getId())) {
                latest = session;
            }
        }
        return latest;
    }

    @Override
    public DrCutoverSessionVO findCommittedTargetAuthorityByPlanId(long planId) {
        DrCutoverSessionVO authority = findCurrentAuthorityByPlanId(planId);
        if (authority == null || !DrConstants.PLAN_STATE_FAILED_OVER.equalsIgnoreCase(authority.getState())
                || !"ACKNOWLEDGED".equalsIgnoreCase(authority.getEngineAckState())
                || authority.getCloudAuthorityGeneration() == null) {
            return null;
        }
        return authority;
    }

    @Override
    public DrCutoverSessionVO findLatestByPlanId(long planId) {
        List<DrCutoverSessionVO> sessions = listHistoryByPlanId(planId);
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    @Override
    public List<DrCutoverSessionVO> listHistoryByPlanId(long planId) {
        SearchCriteria<DrCutoverSessionVO> sc = activeByPlan.create();
        sc.setParameters("planId", planId);
        List<DrCutoverSessionVO> sessions = listBy(sc);
        sessions.sort((left, right) -> Long.compare(right.getId(), left.getId()));
        return sessions;
    }

    private boolean isTerminalAuthorityState(String state) {
        return DrConstants.CUTOVER_STATE_FAILED_BACK.equalsIgnoreCase(state)
                || DrConstants.CUTOVER_STATE_SUPERSEDED.equalsIgnoreCase(state)
                || "ABORTED".equalsIgnoreCase(state);
    }
}
