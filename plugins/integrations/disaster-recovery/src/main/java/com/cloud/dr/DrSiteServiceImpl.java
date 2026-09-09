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

import com.cloud.dr.health.DrSiteHealthCheckResult;
import com.cloud.dr.health.DrSiteHealthCheckService;
import com.cloud.dr.dao.DrPlanDao;
import com.cloud.dr.dao.DrSiteDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DrSiteServiceImpl extends ManagerBase implements DrSiteService {
    private static final Gson GSON = new Gson();
    private static final String CAPABILITY_HEALTH_CHECK = "healthCheck";

    @Inject
    private DrSiteDao drSiteDao;
    @Inject
    private DrPlanDao drPlanDao;
    @Inject
    private DrSiteCredentialService drSiteCredentialService;
    @Inject
    private DrSiteHealthCheckService drSiteHealthCheckService;
    @Inject
    private DrSiteHealthCheckHistoryService drSiteHealthCheckHistoryService;

    @Override
    public DrSiteVO createSite(DrSiteVO site) {
        return createSite(site, null);
    }

    @Override
    public DrSiteVO createSite(DrSiteVO site, DrSiteCredentialInput credentialInput) {
        validateSite(site);
        applyCredentialEndpointDefault(site, credentialInput);
        DrSiteVO existing = drSiteDao.findActiveByName(site.getName());
        if (existing != null) {
            throw new InvalidParameterValueException(DrConstants.ERROR_DUPLICATE_SITE + ": site name already exists");
        }

        if (StringUtils.isBlank(site.getState())) {
            site.setState(DrConstants.ADMIN_STATE_ENABLED);
        }
        if (StringUtils.isBlank(site.getHealthState())) {
            site.setHealthState(DrConstants.HEALTH_UNKNOWN);
        }
        DrSiteVO persisted = drSiteDao.persist(site);
        if (credentialInput != null && credentialInput.hasCredentialData()) {
            drSiteCredentialService.upsertCredential(persisted, credentialInput);
            return drSiteDao.findById(persisted.getId());
        }
        return persisted;
    }

    @Override
    public DrSiteVO updateSite(long siteId, DrSiteVO update) {
        return updateSite(siteId, update, null, false);
    }

    @Override
    public DrSiteVO updateSite(long siteId, DrSiteVO update, DrSiteCredentialInput credentialInput, boolean clearCredential) {
        DrSiteVO site = requireSite(siteId);
        if (update == null) {
            return site;
        }
        applyCredentialEndpointDefault(update, credentialInput);
        if (StringUtils.isNotBlank(update.getName()) && !StringUtils.equals(site.getName(), update.getName())) {
            DrSiteVO existing = drSiteDao.findActiveByName(update.getName());
            if (existing != null && existing.getId() != siteId) {
                throw new InvalidParameterValueException(DrConstants.ERROR_DUPLICATE_SITE + ": site name already exists");
            }
            site.setName(update.getName());
        }
        if (update.getDescription() != null) {
            site.setDescription(update.getDescription());
        }
        if (StringUtils.isNotBlank(update.getSiteType())) {
            site.setSiteType(update.getSiteType());
        }
        if (StringUtils.isNotBlank(update.getHypervisorType())) {
            site.setHypervisorType(update.getHypervisorType());
        }
        if (update.getEndpoint() != null) {
            site.setEndpoint(update.getEndpoint());
        }
        if (update.getCredentialRef() != null) {
            site.setCredentialRef(update.getCredentialRef());
        }
        if (update.getZoneId() != null) {
            site.setZoneId(update.getZoneId());
        }
        if (update.getZoneExternalId() != null) {
            site.setZoneExternalId(StringUtils.trimToNull(update.getZoneExternalId()));
        }
        if (update.getZoneName() != null) {
            site.setZoneName(StringUtils.trimToNull(update.getZoneName()));
        }
        if (update.getVmwareDatacenterId() != null) {
            site.setVmwareDatacenterId(update.getVmwareDatacenterId());
        }
        if (update.getVmwareDatacenterExternalId() != null) {
            site.setVmwareDatacenterExternalId(StringUtils.trimToNull(update.getVmwareDatacenterExternalId()));
        }
        if (update.getVmwareDatacenterName() != null) {
            site.setVmwareDatacenterName(StringUtils.trimToNull(update.getVmwareDatacenterName()));
        }
        if (StringUtils.isNotBlank(update.getState())) {
            site.setState(update.getState());
        }
        if (StringUtils.isNotBlank(update.getHealthState())) {
            site.setHealthState(update.getHealthState());
        }
        if (update.getCapabilitiesJson() != null) {
            site.setCapabilitiesJson(update.getCapabilitiesJson());
        }
        site.markUpdated();
        drSiteDao.update(siteId, site);
        site = drSiteDao.findById(siteId);
        if (clearCredential) {
            drSiteCredentialService.clearCredential(site);
        } else if (credentialInput != null && credentialInput.hasCredentialData()) {
            drSiteCredentialService.upsertCredential(site, credentialInput);
        }
        return drSiteDao.findById(siteId);
    }

    @Override
    public DrSiteVO getSite(long siteId) {
        return requireSite(siteId);
    }

    @Override
    public List<DrSiteVO> listSites() {
        return drSiteDao.listActive();
    }

    @Override
    public boolean deleteSite(final long siteId) {
        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                DrSiteVO site = requireSite(siteId);
                long activePlanCount = drPlanDao.countActiveBySiteId(siteId);
                if (activePlanCount > 0L) {
                    throw new InvalidParameterValueException(DrConstants.ERROR_ACTIVE_PLAN_EXISTS
                            + ": " + activePlanCount + " active DR plan(s) refer to site " + siteId);
                }
                site.setCredentialId(null);
                site.setCredentialRef(null);
                site.markUpdated();
                if (!drSiteDao.update(siteId, site)) {
                    throw new CloudRuntimeException("Failed to clear DR site credential reference " + siteId);
                }
                drSiteCredentialService.clearCredentialsForDeletedSite(siteId);
                if (!drSiteDao.remove(siteId)) {
                    throw new CloudRuntimeException("Failed to delete DR site " + siteId);
                }
                DrSiteVO removedSite = drSiteDao.findByIdIncludingRemoved(siteId);
                if (removedSite == null || removedSite.getRemoved() == null) {
                    throw new CloudRuntimeException("DR site soft delete was not persisted " + siteId);
                }
                return true;
            }
        });
    }

    @Override
    public DrSiteVO checkSite(long siteId) {
        return checkSite(siteId, true, DrConstants.HEALTH_TRIGGER_MANUAL, null);
    }

    @Override
    public DrSiteVO checkSite(long siteId, boolean persistStatus) {
        return checkSite(siteId, persistStatus, DrConstants.HEALTH_TRIGGER_MANUAL, null);
    }

    @Override
    public DrSiteVO checkSite(final long siteId, final boolean persistStatus, final String triggerType, final String jobId) {
        DrSiteVO site = requireSite(siteId);
        DrSiteHealthCheckResult result = drSiteHealthCheckService != null
                ? drSiteHealthCheckService.checkSite(site, persistStatus)
                : new DrSiteHealthCheckResult(DrConstants.HEALTH_UNKNOWN, DrConstants.HEALTH_REASON_UNSUPPORTED_SITE_TYPE,
                        "DR site health check service is not available", 0L, new Date(), false);
        applyHealthCheckResult(site, result);
        if (!persistStatus) {
            return site;
        }
        final DrSiteHealthCheckResult checkedResult = result;
        Transaction.execute(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction(TransactionStatus status) {
                DrSiteVO activeSite = requireSite(siteId);
                applyHealthCheckResult(activeSite, checkedResult);
                drSiteDao.update(siteId, activeSite);
                if (drSiteHealthCheckHistoryService != null) {
                    DrSiteCredentialVO credential = drSiteCredentialService != null ? drSiteCredentialService.findLatestCredential(siteId) : null;
                    drSiteHealthCheckHistoryService.record(activeSite, credential, checkedResult,
                            StringUtils.defaultIfBlank(triggerType, DrConstants.HEALTH_TRIGGER_MANUAL), jobId);
                }
                return null;
            }
        });
        return drSiteDao.findById(siteId);
    }

    private DrSiteVO requireSite(long siteId) {
        DrSiteVO site = drSiteDao.findById(siteId);
        if (site == null || site.getRemoved() != null) {
            throw new InvalidParameterValueException(DrConstants.ERROR_SITE_NOT_FOUND + ": " + siteId);
        }
        return site;
    }

    private void validateSite(DrSiteVO site) {
        if (site == null) {
            throw new InvalidParameterValueException("DR site is required");
        }
        if (StringUtils.isBlank(site.getName())) {
            throw new InvalidParameterValueException("DR site name is required");
        }
        if (StringUtils.isBlank(site.getSiteType())) {
            throw new InvalidParameterValueException("DR site type is required");
        }
        if (StringUtils.isBlank(site.getHypervisorType())) {
            throw new InvalidParameterValueException("DR site hypervisor type is required");
        }
    }

    private void applyCredentialEndpointDefault(DrSiteVO site, DrSiteCredentialInput credentialInput) {
        if (site == null || credentialInput == null || StringUtils.isNotBlank(site.getEndpoint())
                || StringUtils.isBlank(credentialInput.getEndpoint())) {
            return;
        }
        site.setEndpoint(credentialInput.getEndpoint());
    }

    private void applyHealthCheckResult(DrSiteVO site, DrSiteHealthCheckResult result) {
        if (site == null || result == null) {
            return;
        }
        site.setHealthState(result.getHealthState());
        site.setLastChecked(result.getCheckedAt());
        site.setCapabilitiesJson(mergeHealthCheckResult(site.getCapabilitiesJson(), result));
        site.markUpdated();
    }

    private String mergeHealthCheckResult(String capabilitiesJson, DrSiteHealthCheckResult result) {
        JsonObject root = parseJsonObject(capabilitiesJson);
        JsonObject health = new JsonObject();
        health.addProperty("state", result.getHealthState());
        health.addProperty("reasonCode", result.getReasonCode());
        health.addProperty("message", result.getMessage());
        if (result.getLatencyMs() != null) {
            health.addProperty("latencyMs", result.getLatencyMs());
        }
        if (result.getCheckedAt() != null) {
            health.addProperty("checkedAtEpochMs", result.getCheckedAt().getTime());
        }
        mergeRedactedDetails(health, result.getDetails());
        root.add(CAPABILITY_HEALTH_CHECK, health);
        return GSON.toJson(root);
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

    private JsonObject parseJsonObject(String json) {
        if (StringUtils.isBlank(json)) {
            return new JsonObject();
        }
        try {
            JsonElement parsed = JsonParser.parseString(json);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException e) {
            return new JsonObject();
        }
    }
}
