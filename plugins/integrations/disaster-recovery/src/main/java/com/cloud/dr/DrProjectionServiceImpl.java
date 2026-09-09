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
package com.cloud.dr;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.adapter.DrAdapterRegistry;
import com.cloud.dr.adapter.DrAdapterResult;
import com.cloud.dr.adapter.DrProjectionAdapter;
import com.cloud.dr.dao.DrEventDao;
import com.cloud.dr.dao.DrPlanDao;
import com.cloud.dr.dao.DrReplicaDao;
import com.cloud.dr.dao.DrRestorePointDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;

public class DrProjectionServiceImpl extends ManagerBase implements DrProjectionService {
    @Inject
    private DrPlanDao drPlanDao;
    @Inject
    private DrEventDao drEventDao;
    @Inject
    private DrReplicaDao drReplicaDao;
    @Inject
    private DrRestorePointDao drRestorePointDao;
    @Inject
    private DrAdapterRegistry drAdapterRegistry;

    @Override
    public DrPlanVO refreshPlanProjection(long planId, boolean bestEffort) {
        DrPlanVO plan = requirePlan(planId);
        String engineType = StringUtils.defaultIfBlank(plan.getEngineType(), plan.getEngineBindingType());
        String engineBindingType = StringUtils.defaultIfBlank(plan.getEngineBindingType(), plan.getEngineType());
        DrProjectionAdapter projectionAdapter = drAdapterRegistry.getProjectionAdapter(engineType, engineBindingType);
        if (projectionAdapter != null) {
            DrAdapterResult result = projectionAdapter.refreshPlanProjection(plan);
            if (result != null && !result.isSuccess() && !bestEffort) {
                throw new InvalidParameterValueException(result.getErrorCode() + ": " + result.getMessage());
            }
        } else if (!bestEffort) {
            throw new InvalidParameterValueException(DrConstants.ERROR_ENGINE_UNAVAILABLE + ": projection adapter is unavailable");
        }
        return drPlanDao.findById(planId);
    }

    @Override
    public boolean projectTerminalActionResult(long planId, DrRunVO run, String detailsJson) {
        DrPlanVO plan = requirePlan(planId);
        String engineType = StringUtils.defaultIfBlank(plan.getEngineType(), plan.getEngineBindingType());
        String engineBindingType = StringUtils.defaultIfBlank(plan.getEngineBindingType(), plan.getEngineType());
        DrProjectionAdapter projectionAdapter = drAdapterRegistry.getProjectionAdapter(engineType, engineBindingType);
        if (projectionAdapter == null) {
            return false;
        }
        DrAdapterResult result = projectionAdapter.projectTerminalActionResult(plan, run, detailsJson);
        return result != null && result.isSuccess();
    }

    @Override
    public List<DrReplicaVO> listReplicas(long planId) {
        requirePlan(planId);
        return drReplicaDao.listActiveByPlanId(planId);
    }

    @Override
    public List<DrRestorePointVO> listRestorePoints(long planId) {
        requirePlan(planId);
        return drRestorePointDao.listActiveByPlanId(planId);
    }

    @Override
    public List<DrRestorePointVO> listRestorePoints(long planId, long startIndex, long pageSize) {
        requirePlan(planId);
        return drRestorePointDao.listActiveByPlanId(planId, startIndex, pageSize);
    }

    @Override
    public long countRestorePoints(long planId) {
        requirePlan(planId);
        return drRestorePointDao.countActiveByPlanId(planId);
    }

    @Override
    public List<DrEventVO> listPlanEvents(long planId) {
        requirePlan(planId);
        return drEventDao.listRecentByPlanId(planId, 20, false);
    }

    @Override
    public List<DrEventVO> listRunEvents(long runId) {
        return drEventDao.listRecentByRunId(runId, 20);
    }

    @Override
    public Pair<List<DrEventVO>, Integer> listPlanEvents(long planId, long startIndex, long pageSize) {
        requirePlan(planId);
        return drEventDao.searchRecentByPlanId(planId, startIndex, pageSize, false);
    }

    @Override
    public Pair<List<DrEventVO>, Integer> listRunEvents(long runId, long startIndex, long pageSize) {
        return drEventDao.searchRecentByRunId(runId, startIndex, pageSize);
    }

    private DrPlanVO requirePlan(long planId) {
        DrPlanVO plan = drPlanDao.findById(planId);
        if (plan == null || plan.getRemoved() != null) {
            throw new InvalidParameterValueException(DrConstants.ERROR_PLAN_NOT_FOUND + ": " + planId);
        }
        return plan;
    }
}
