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
package com.cloud.dr;

import java.util.List;

public interface DrSiteService {
    DrSiteVO createSite(DrSiteVO site);

    DrSiteVO createSite(DrSiteVO site, DrSiteCredentialInput credentialInput);

    DrSiteVO updateSite(long siteId, DrSiteVO update);

    DrSiteVO updateSite(long siteId, DrSiteVO update, DrSiteCredentialInput credentialInput, boolean clearCredential);

    DrSiteVO getSite(long siteId);

    List<DrSiteVO> listSites();

    boolean deleteSite(long siteId);

    DrSiteVO checkSite(long siteId);

    DrSiteVO checkSite(long siteId, boolean persistStatus);

    DrSiteVO checkSite(long siteId, boolean persistStatus, String triggerType, String jobId);
}
