// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.dr.dao;

import java.util.List;

import com.cloud.dr.DrPlanVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

public interface DrPlanDao extends GenericDao<DrPlanVO, Long> {
    DrPlanVO findActiveBySourceVmId(long sourceVmId);

    List<DrPlanVO> listActiveBySourceVmId(long sourceVmId);

    DrPlanVO findActiveBySourceSiteAndExternalRef(long sourceSiteId, String sourceExternalRef);

    DrPlanVO findActiveByEngineBinding(String engineBindingType, long engineBindingId);

    List<DrPlanVO> listActive();

    List<DrPlanVO> listActiveByState(String state);

    Pair<List<DrPlanVO>, Integer> searchActive(Long id, String keyword, String state, Long sourceSiteId,
            Long targetSiteId, String direction, String engineType, Long offset, Long limit);

    long countActiveBySiteId(long siteId);
}
