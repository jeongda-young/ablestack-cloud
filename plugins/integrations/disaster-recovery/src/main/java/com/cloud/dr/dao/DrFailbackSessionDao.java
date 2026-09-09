// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.dr.dao;

import java.util.Date;
import java.util.List;

import com.cloud.dr.DrFailbackSessionVO;
import com.cloud.utils.db.GenericDao;

public interface DrFailbackSessionDao extends GenericDao<DrFailbackSessionVO, Long> {
    DrFailbackSessionVO findActiveByRunId(long runId);
    DrFailbackSessionVO findLatestActiveByPlanId(long planId);
    List<DrFailbackSessionVO> listReconcileCandidates(Date probeBefore, int limit);
    void clearFailureMetadata(long sessionId);
}
