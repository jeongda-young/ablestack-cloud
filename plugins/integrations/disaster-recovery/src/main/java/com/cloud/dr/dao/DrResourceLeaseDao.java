// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package com.cloud.dr.dao;

import java.util.Date;
import java.util.List;

import com.cloud.dr.DrResourceLeaseVO;
import com.cloud.utils.db.GenericDao;

public interface DrResourceLeaseDao extends GenericDao<DrResourceLeaseVO, Long> {
    List<DrResourceLeaseVO> listActiveByResourceKey(String resourceKey, Date now);
    DrResourceLeaseVO findActiveByRunId(long runId, Date now);
    DrResourceLeaseVO findByRunId(long runId);
}
