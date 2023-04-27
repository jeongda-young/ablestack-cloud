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
package com.cloud.ssv.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloud.ssv.SSVVmMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;


@Component
public class SSVVmMapDaoImpl extends GenericDaoBase<SSVVmMapVO, Long> implements SSVVmMapDao {

    private final SearchBuilder<SSVVmMapVO> ssvIdSearch;
    private final SearchBuilder<SSVVmMapVO> ssvIdAndVmType;
    private final SearchBuilder<SSVVmMapVO> ssvIdAndNotVmType;

    public SSVVmMapDaoImpl() {
        ssvIdSearch = createSearchBuilder();
        ssvIdSearch.and("desktopClusterId", ssvIdSearch.entity().getSsvId(), SearchCriteria.Op.EQ);
        ssvIdSearch.done();

        ssvIdAndVmType = createSearchBuilder();
        ssvIdAndVmType.and("desktopClusterId", ssvIdAndVmType.entity().getSsvId(), SearchCriteria.Op.EQ);
        ssvIdAndVmType.and("type", ssvIdAndVmType.entity().getType(), SearchCriteria.Op.EQ);
        ssvIdAndVmType.done();

        ssvIdAndNotVmType = createSearchBuilder();
        ssvIdAndNotVmType.and("desktopClusterId", ssvIdAndNotVmType.entity().getSsvId(), SearchCriteria.Op.EQ);
        ssvIdAndNotVmType.and("type", ssvIdAndNotVmType.entity().getType(), SearchCriteria.Op.NEQ);
        ssvIdAndNotVmType.done();

    }

    @Override
    public List<SSVVmMapVO> listBySSVId(long desktopClusterId) {
        SearchCriteria<SSVVmMapVO> sc = ssvIdSearch.create();
        sc.setParameters("desktopClusterId", desktopClusterId);
        return listBy(sc, null);
    }

    @Override
    public List<SSVVmMapVO> listBySSVIdAndVmType(long desktopClusterId, String type) {
        SearchCriteria<SSVVmMapVO> sc = ssvIdAndVmType.create();
        sc.setParameters("desktopClusterId", desktopClusterId);
        sc.setParameters("type", type);
        return listBy(sc);
    }

    @Override
    public List<SSVVmMapVO> listBySSVIdAndNotVmType(long desktopClusterId, String type) {
        SearchCriteria<SSVVmMapVO> sc = ssvIdAndNotVmType.create();
        sc.setParameters("desktopClusterId", desktopClusterId);
        sc.setParameters("type", type);
        return listBy(sc);
    }
}