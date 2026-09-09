// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0.
package com.cloud.dr.dao;

import com.cloud.dr.DrPlanRuntimeVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrPlanRuntimeDaoImpl extends GenericDaoBase<DrPlanRuntimeVO, Long> implements DrPlanRuntimeDao {
    private final SearchBuilder<DrPlanRuntimeVO> byPlanSearch;

    public DrPlanRuntimeDaoImpl() {
        byPlanSearch = createSearchBuilder();
        byPlanSearch.and("planId", byPlanSearch.entity().getPlanId(), SearchCriteria.Op.EQ);
        byPlanSearch.done();
    }

    @Override
    public DrPlanRuntimeVO findByPlanId(long planId) {
        SearchCriteria<DrPlanRuntimeVO> sc = byPlanSearch.create();
        sc.setParameters("planId", planId);
        return findOneBy(sc);
    }

    @Override
    public int removeByPlanId(long planId) {
        SearchCriteria<DrPlanRuntimeVO> sc = byPlanSearch.create();
        sc.setParameters("planId", planId);
        return remove(sc);
    }
}
