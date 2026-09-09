// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package com.cloud.dr.dao;

import java.util.List;

import com.cloud.dr.DrGroupRunVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrGroupRunDaoImpl extends GenericDaoBase<DrGroupRunVO, Long> implements DrGroupRunDao {
    private final SearchBuilder<DrGroupRunVO> byGroup;
    private final SearchBuilder<DrGroupRunVO> recoverable;

    public DrGroupRunDaoImpl() {
        byGroup = createSearchBuilder();
        byGroup.and("groupUuid", byGroup.entity().getGroupUuid(), SearchCriteria.Op.EQ);
        byGroup.done();
        recoverable = createSearchBuilder();
        recoverable.and("state", recoverable.entity().getState(), SearchCriteria.Op.IN);
        recoverable.done();
    }

    @Override public List<DrGroupRunVO> listByGroupUuid(String groupUuid) {
        SearchCriteria<DrGroupRunVO> sc = byGroup.create();
        sc.setParameters("groupUuid", groupUuid);
        return listBy(sc, new Filter(DrGroupRunVO.class, "created", false, 0L, 100L));
    }

    @Override public List<DrGroupRunVO> listRecoverable() {
        SearchCriteria<DrGroupRunVO> sc = recoverable.create();
        sc.setParameters("state", "QUEUED", "RUNNING");
        return listBy(sc);
    }
}
