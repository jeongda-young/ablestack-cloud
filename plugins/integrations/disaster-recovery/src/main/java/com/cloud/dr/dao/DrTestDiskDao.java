// Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
package com.cloud.dr.dao;

import java.util.List;

import com.cloud.dr.DrTestDiskVO;
import com.cloud.utils.db.GenericDao;

public interface DrTestDiskDao extends GenericDao<DrTestDiskVO, Long> {
    List<DrTestDiskVO> listActiveBySessionId(long sessionId);
}
