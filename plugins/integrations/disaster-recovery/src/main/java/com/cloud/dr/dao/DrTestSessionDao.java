// Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
package com.cloud.dr.dao;

import java.util.List;

import com.cloud.dr.DrTestSessionVO;
import com.cloud.utils.db.GenericDao;

public interface DrTestSessionDao extends GenericDao<DrTestSessionVO, Long> {
    DrTestSessionVO findActiveByRunId(long runId);
    DrTestSessionVO findActiveByPlanId(long planId);
    DrTestSessionVO findByRunIdIncludingRemoved(long runId);
    List<DrTestSessionVO> listActiveByTargetVmId(long targetVmId);
    boolean restoreSoftClosedForMaterialization(DrTestSessionVO session);
}
