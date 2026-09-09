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

import com.cloud.dr.DrCutoverDiskVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrCutoverDiskDaoImpl extends GenericDaoBase<DrCutoverDiskVO, Long> implements DrCutoverDiskDao {
    private final SearchBuilder<DrCutoverDiskVO> activeBySession;
    public DrCutoverDiskDaoImpl() {
        activeBySession = createSearchBuilder();
        activeBySession.and("sessionId", activeBySession.entity().getSessionId(), SearchCriteria.Op.EQ);
        activeBySession.and("removed", activeBySession.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeBySession.done();
    }
    @Override public List<DrCutoverDiskVO> listActiveBySessionId(long sessionId) {
        SearchCriteria<DrCutoverDiskVO> sc = activeBySession.create(); sc.setParameters("sessionId", sessionId); return listBy(sc);
    }
}
