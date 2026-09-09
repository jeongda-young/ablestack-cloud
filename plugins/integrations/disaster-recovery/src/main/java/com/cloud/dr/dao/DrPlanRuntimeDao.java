// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0.
package com.cloud.dr.dao;

import com.cloud.dr.DrPlanRuntimeVO;
import com.cloud.utils.db.GenericDao;

public interface DrPlanRuntimeDao extends GenericDao<DrPlanRuntimeVO, Long> {
    DrPlanRuntimeVO findByPlanId(long planId);
    int removeByPlanId(long planId);
}
