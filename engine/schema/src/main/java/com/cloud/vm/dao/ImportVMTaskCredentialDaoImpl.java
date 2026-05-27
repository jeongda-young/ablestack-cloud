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
package com.cloud.vm.dao;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.ImportVMTaskCredentialVO;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class ImportVMTaskCredentialDaoImpl extends GenericDaoBase<ImportVMTaskCredentialVO, Long> implements ImportVMTaskCredentialDao {

    private SearchBuilder<ImportVMTaskCredentialVO> ActiveTaskSearch;

    @PostConstruct
    void init() {
        ActiveTaskSearch = createSearchBuilder();
        ActiveTaskSearch.and("taskId", ActiveTaskSearch.entity().getTaskId(), SearchCriteria.Op.EQ);
        ActiveTaskSearch.and("removed", ActiveTaskSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        ActiveTaskSearch.done();
    }

    @Override
    public ImportVMTaskCredentialVO findLatestByTaskId(long taskId) {
        SearchCriteria<ImportVMTaskCredentialVO> sc = ActiveTaskSearch.create();
        sc.setParameters("taskId", taskId);
        Filter filter = new Filter(ImportVMTaskCredentialVO.class, "created", false, 0L, 1L);
        List<ImportVMTaskCredentialVO> credentials = listBy(sc, filter);
        return credentials.isEmpty() ? null : credentials.get(0);
    }

    @Override
    public List<ImportVMTaskCredentialVO> listByTaskId(long taskId) {
        SearchCriteria<ImportVMTaskCredentialVO> sc = ActiveTaskSearch.create();
        sc.setParameters("taskId", taskId);
        Filter filter = new Filter(ImportVMTaskCredentialVO.class, "created", false, null, null);
        return listBy(sc, filter);
    }
}
