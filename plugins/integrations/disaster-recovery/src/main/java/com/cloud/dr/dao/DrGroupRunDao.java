// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package com.cloud.dr.dao;

import java.util.List;

import com.cloud.dr.DrGroupRunVO;
import com.cloud.utils.db.GenericDao;

public interface DrGroupRunDao extends GenericDao<DrGroupRunVO, Long> {
    List<DrGroupRunVO> listByGroupUuid(String groupUuid);
    List<DrGroupRunVO> listRecoverable();
}
