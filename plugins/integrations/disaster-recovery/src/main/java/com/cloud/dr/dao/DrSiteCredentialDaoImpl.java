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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrSiteCredentialVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@DB
public class DrSiteCredentialDaoImpl extends GenericDaoBase<DrSiteCredentialVO, Long> implements DrSiteCredentialDao {

    private final SearchBuilder<DrSiteCredentialVO> siteSearch;
    private final SearchBuilder<DrSiteCredentialVO> configuredBySiteSearch;
    private final SearchBuilder<DrSiteCredentialVO> configuredByIdAndSiteSearch;

    public DrSiteCredentialDaoImpl() {
        siteSearch = createSearchBuilder();
        siteSearch.and("siteId", siteSearch.entity().getSiteId(), SearchCriteria.Op.EQ);
        siteSearch.done();

        configuredBySiteSearch = createSearchBuilder();
        configuredBySiteSearch.and("siteId", configuredBySiteSearch.entity().getSiteId(), SearchCriteria.Op.EQ);
        configuredBySiteSearch.and("state", configuredBySiteSearch.entity().getState(), SearchCriteria.Op.EQ);
        configuredBySiteSearch.and("removed", configuredBySiteSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        configuredBySiteSearch.done();

        configuredByIdAndSiteSearch = createSearchBuilder();
        configuredByIdAndSiteSearch.and("id", configuredByIdAndSiteSearch.entity().getId(), SearchCriteria.Op.EQ);
        configuredByIdAndSiteSearch.and("siteId", configuredByIdAndSiteSearch.entity().getSiteId(), SearchCriteria.Op.EQ);
        configuredByIdAndSiteSearch.and("state", configuredByIdAndSiteSearch.entity().getState(), SearchCriteria.Op.EQ);
        configuredByIdAndSiteSearch.and("removed", configuredByIdAndSiteSearch.entity().getRemoved(), SearchCriteria.Op.NULL);
        configuredByIdAndSiteSearch.done();
    }

    @Override
    public DrSiteCredentialVO findActiveBySiteId(long siteId) {
        return findConfiguredBySiteId(siteId);
    }

    @Override
    public DrSiteCredentialVO findConfiguredBySiteId(long siteId) {
        SearchCriteria<DrSiteCredentialVO> sc = configuredBySiteSearch.create();
        sc.setParameters("siteId", siteId);
        sc.setParameters("state", DrConstants.CREDENTIAL_STATE_CONFIGURED);
        return findOneBy(sc);
    }

    @Override
    public DrSiteCredentialVO findConfiguredByIdAndSiteId(long credentialId, long siteId) {
        SearchCriteria<DrSiteCredentialVO> sc = configuredByIdAndSiteSearch.create();
        sc.setParameters("id", credentialId);
        sc.setParameters("siteId", siteId);
        sc.setParameters("state", DrConstants.CREDENTIAL_STATE_CONFIGURED);
        return findOneBy(sc);
    }

    @Override
    public DrSiteCredentialVO findLatestBySiteId(long siteId) {
        List<DrSiteCredentialVO> credentials = listBySiteId(siteId);
        if (credentials.isEmpty()) {
            return null;
        }
        credentials.sort(Comparator.comparingLong(DrSiteCredentialVO::getId).reversed());
        return credentials.get(0);
    }

    @Override
    public List<DrSiteCredentialVO> listBySiteId(long siteId) {
        SearchCriteria<DrSiteCredentialVO> sc = siteSearch.create();
        sc.setParameters("siteId", siteId);
        List<DrSiteCredentialVO> credentials = listBy(sc);
        return credentials == null ? new ArrayList<DrSiteCredentialVO>() : credentials;
    }
}
