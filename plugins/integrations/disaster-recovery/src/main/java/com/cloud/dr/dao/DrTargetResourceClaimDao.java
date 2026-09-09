// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.
package com.cloud.dr.dao;

import java.util.List;

import com.cloud.dr.DrTargetResourceClaimVO;
import com.cloud.utils.db.GenericDao;

public interface DrTargetResourceClaimDao extends GenericDao<DrTargetResourceClaimVO, Long> {
    DrTargetResourceClaimVO findActiveByResourceKey(String activeResourceKey);
    DrTargetResourceClaimVO findActiveByRoleKey(String activeRoleKey);
    List<DrTargetResourceClaimVO> listActiveByPlanId(long planId);
}
