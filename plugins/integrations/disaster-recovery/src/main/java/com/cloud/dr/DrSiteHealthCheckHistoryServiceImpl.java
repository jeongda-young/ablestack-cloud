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

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.cloudstack.utils.identity.ManagementServerNode;

import com.cloud.dr.dao.DrSiteHealthCheckDao;
import com.cloud.dr.health.DrSiteHealthCheckResult;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Filter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class DrSiteHealthCheckHistoryServiceImpl extends ManagerBase implements DrSiteHealthCheckHistoryService {
    private static final Gson GSON = new Gson();
    private static final int MAX_MESSAGE_LENGTH = 4096;

    @Inject
    private DrSiteHealthCheckDao drSiteHealthCheckDao;

    @Override
    public DrSiteHealthCheckVO record(DrSiteVO site, DrSiteCredentialVO credential, DrSiteHealthCheckResult result, String triggerType, String jobId) {
        if (site == null || result == null || StringUtils.isBlank(triggerType)) {
            return null;
        }
        DrSiteHealthCheckVO history = new DrSiteHealthCheckVO(site.getId(), StringUtils.upperCase(triggerType), result.getHealthState());
        history.setSiteUuid(site.getUuid());
        history.setSiteName(site.getName());
        history.setSiteType(site.getSiteType());
        history.setHypervisorType(site.getHypervisorType());
        history.setEndpoint(resolveEndpoint(site, credential));
        history.setCredentialId(credential != null && credential.getId() > 0 ? credential.getId() : site.getCredentialId());
        history.setCredentialState(credential != null ? credential.getState() : null);
        history.setReasonCode(result.getReasonCode());
        history.setMessage(limit(result.getMessage(), MAX_MESSAGE_LENGTH));
        history.setLatencyMs(result.getLatencyMs());
        history.setCheckedAt(result.getCheckedAt() == null ? new Date() : result.getCheckedAt());
        history.setManagementServerId(ManagementServerNode.getManagementServerId());
        history.setJobId(StringUtils.trimToNull(jobId));
        history.setDetailsJson(buildDetailsJson(result));
        return drSiteHealthCheckDao.persist(history);
    }

    @Override
    public Pair<List<DrSiteHealthCheckVO>, Integer> list(long siteId, String healthState, String triggerType, Date startDate, Date endDate, Filter filter) {
        return drSiteHealthCheckDao.searchBySite(siteId, healthState, triggerType, startDate, endDate, filter);
    }

    @Override
    public int cleanupOlderThan(Date checkedBefore, long batchSize) {
        return drSiteHealthCheckDao.expungeOlderThan(checkedBefore, batchSize);
    }

    private String resolveEndpoint(DrSiteVO site, DrSiteCredentialVO credential) {
        if (credential != null && StringUtils.isNotBlank(credential.getEndpoint())) {
            return credential.getEndpoint();
        }
        return site != null ? site.getEndpoint() : null;
    }

    private String buildDetailsJson(DrSiteHealthCheckResult result) {
        JsonObject details = new JsonObject();
        details.addProperty("credentialValidated", result.isCredentialValidated());
        if (result.getReasonCode() != null) {
            details.addProperty("reasonCode", result.getReasonCode());
        }
        if (result.getLatencyMs() != null) {
            details.addProperty("latencyMs", result.getLatencyMs());
        }
        mergeRedactedDetails(details, result.getDetails());
        return GSON.toJson(details);
    }

    private void mergeRedactedDetails(JsonObject target, JsonObject details) {
        if (target == null || details == null) {
            return;
        }
        for (String key : details.keySet()) {
            if (!isSafeHealthDetailKey(key)) {
                continue;
            }
            target.add(key, details.get(key));
        }
    }

    private boolean isSafeHealthDetailKey(String key) {
        if (StringUtils.isBlank(key)) {
            return false;
        }
        String lowerKey = StringUtils.lowerCase(key);
        return !StringUtils.contains(lowerKey, "secret")
                && !StringUtils.contains(lowerKey, "password")
                && !StringUtils.contains(lowerKey, "token")
                && !StringUtils.contains(lowerKey, "authorization")
                && !StringUtils.contains(lowerKey, "apikey");
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
