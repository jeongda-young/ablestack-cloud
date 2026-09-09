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

import com.cloud.dr.DrSiteVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrSiteDaoImpl extends GenericDaoBase<DrSiteVO, Long> implements DrSiteDao {

    private final SearchBuilder<DrSiteVO> activeSearch;
    private final SearchBuilder<DrSiteVO> activeByNameSearch;

    public DrSiteDaoImpl() {
        activeSearch = createSearchBuilder();
        activeSearch.and("removed", activeSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeSearch.done();

        activeByNameSearch = createSearchBuilder();
        activeByNameSearch.and("name", activeByNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        activeByNameSearch.and("removed", activeByNameSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        activeByNameSearch.done();
    }

    @Override
    public DrSiteVO findActiveByName(String name) {
        SearchCriteria<DrSiteVO> sc = activeByNameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    public List<DrSiteVO> listActive() {
        return listBy(activeSearch.create());
    }
}
