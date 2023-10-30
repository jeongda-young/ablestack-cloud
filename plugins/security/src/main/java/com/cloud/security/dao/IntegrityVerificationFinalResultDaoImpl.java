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

package com.cloud.security.dao;

import com.cloud.security.IntegrityVerificationFinalResultVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IntegrityVerificationFinalResultDaoImpl extends GenericDaoBase<IntegrityVerificationFinalResultVO, Long> implements IntegrityVerificationFinalResultDao {

    protected SearchBuilder<IntegrityVerificationFinalResultVO> IntegrityVerificationFinalResultsSearchBuilder;

    protected IntegrityVerificationFinalResultDaoImpl() {
        super();
        IntegrityVerificationFinalResultsSearchBuilder = createSearchBuilder();
        IntegrityVerificationFinalResultsSearchBuilder.and("msHostId", IntegrityVerificationFinalResultsSearchBuilder.entity().getMsHostId(), SearchCriteria.Op.EQ);
        IntegrityVerificationFinalResultsSearchBuilder.done();
    }

    @Override
    public List<IntegrityVerificationFinalResultVO> getIntegrityVerificationFinalResults(long msHostId) {
        SearchCriteria<IntegrityVerificationFinalResultVO> sc = IntegrityVerificationFinalResultsSearchBuilder.create();
        sc.setParameters("msHostId", msHostId);
        return listBy(sc);
    }

    @Override
    public IntegrityVerificationFinalResultVO getIntegrityVerificationFinalResult(long msHostId) {
        SearchCriteria<IntegrityVerificationFinalResultVO> sc = IntegrityVerificationFinalResultsSearchBuilder.create();
        sc.setParameters("msHostId", msHostId);
        List<IntegrityVerificationFinalResultVO> verifications = listBy(sc);
        return verifications.isEmpty() ? null : verifications.get(0);
    }

    @Override
    public List<IntegrityVerificationFinalResultVO> listAllByIntegrityVerificationFinalResult(long id) {
        SearchCriteria<IntegrityVerificationFinalResultVO> sc = IntegrityVerificationFinalResultsSearchBuilder.create();
        sc.setParameters("id", id);
        return this.listBy(sc);
    }

    @Override
    public List<IntegrityVerificationFinalResultVO> listByIntegrityVerificationFinalResult(long id) {
        SearchCriteria<IntegrityVerificationFinalResultVO> sc = IntegrityVerificationFinalResultsSearchBuilder.create();
        sc.setParameters("id", id);
        return this.listBy(sc);
    }

}
