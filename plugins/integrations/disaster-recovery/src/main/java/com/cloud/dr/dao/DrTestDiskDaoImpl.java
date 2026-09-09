// Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
package com.cloud.dr.dao;

import java.util.List;

import com.cloud.dr.DrTestDiskVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrTestDiskDaoImpl extends GenericDaoBase<DrTestDiskVO, Long> implements DrTestDiskDao {
    private final SearchBuilder<DrTestDiskVO> activeBySession;

    public DrTestDiskDaoImpl() {
        activeBySession = createSearchBuilder();
        activeBySession.and("sessionId", activeBySession.entity().getSessionId(), SearchCriteria.Op.EQ);
        activeBySession.and("removed", activeBySession.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeBySession.done();
    }

    @Override public List<DrTestDiskVO> listActiveBySessionId(long sessionId) {
        SearchCriteria<DrTestDiskVO> sc = activeBySession.create();
        sc.setParameters("sessionId", sessionId);
        return listBy(sc);
    }
}
