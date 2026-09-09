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
package com.cloud.dr.inventory;

import java.util.Date;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrResolvedSiteCredential;
import com.cloud.dr.DrSiteCredentialInput;
import com.cloud.dr.DrSiteCredentialService;
import com.cloud.dr.DrSiteCredentialVO;
import com.cloud.dr.DrSiteVO;
import com.cloud.dr.dao.DrSiteDao;
import com.cloud.dr.inventory.DrMoldInventoryClient.InventoryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ManagerBase;
import com.google.gson.Gson;

public class DrSiteInventoryServiceImpl extends ManagerBase implements DrSiteInventoryService {
    private static final Gson GSON = new Gson();

    @Inject
    private DrSiteDao drSiteDao;
    @Inject
    private DrSiteCredentialService drSiteCredentialService;
    @Inject
    private DrMoldInventoryClient drMoldInventoryClient;

    @Override
    public DrSiteInventoryResult discover(DrSiteInventoryRequest request) {
        long started = System.currentTimeMillis();
        Date checkedAt = new Date();
        DrSiteInventoryResult result = new DrSiteInventoryResult();
        result.setCheckedAt(checkedAt);

        DrSiteVO site = null;
        DrResolvedSiteCredential resolvedCredential = null;
        try {
            if (request == null) {
                throw new InvalidParameterValueException("DR site inventory request is required");
            }
            if (request.getSiteId() != null) {
                site = requireSite(request.getSiteId());
                result.setSiteId(site.getUuid());
                result.setSiteType(site.getSiteType());
                resolvedCredential = drSiteCredentialService != null ? drSiteCredentialService.resolveCredential(site) : null;
            } else {
                result.setSiteType(request.getSiteType());
                site = transientSite(request);
                resolvedCredential = transientCredential(request.getCredentialInput());
            }

            if (!isMoldSite(site.getSiteType())) {
                return complete(result, DrConstants.HEALTH_UNKNOWN, DrConstants.HEALTH_REASON_UNSUPPORTED_SITE_TYPE,
                        "Mold inventory is not applicable to this DR site type", started);
            }
            if (resolvedCredential == null || !resolvedCredential.hasSecrets()) {
                return complete(result, DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_CREDENTIAL_MISSING,
                        "Mold API endpoint, API key, and secret key are required", started);
            }

            if (request.isIncludeZones()) {
                result.setZones(drMoldInventoryClient.listZones(resolvedCredential));
            }
            if (request.isIncludeVmwareDatacenters()) {
                String zoneExternalId = StringUtils.defaultIfBlank(request.getZoneExternalId(), site != null ? site.getZoneExternalId() : null);
                Long zoneId = request.getZoneId() != null ? request.getZoneId() : (site != null ? site.getZoneId() : null);
                result.setVmwareDatacenters(drMoldInventoryClient.listVmwareDatacenters(resolvedCredential, zoneExternalId, zoneId));
            }
            return complete(result, DrConstants.HEALTH_CONNECTED, DrConstants.HEALTH_REASON_MOLD_API_OK,
                    "Mold inventory was discovered", started);
        } catch (InventoryException e) {
            String reason = e.getResponseCode() == 401 || e.getResponseCode() == 403
                    ? DrConstants.HEALTH_REASON_CREDENTIAL_INVALID
                    : (e.getResponseCode() > 0 ? DrConstants.HEALTH_REASON_ENDPOINT_HTTP_ERROR : DrConstants.HEALTH_REASON_ENDPOINT_UNREACHABLE);
            return complete(result, DrConstants.HEALTH_DISCONNECTED, reason, e.getMessage(), started);
        } finally {
            if (resolvedCredential != null) {
                resolvedCredential.close();
            }
        }
    }

    private DrSiteVO requireSite(long siteId) {
        DrSiteVO site = drSiteDao.findById(siteId);
        if (site == null || site.getRemoved() != null) {
            throw new InvalidParameterValueException(DrConstants.ERROR_SITE_NOT_FOUND + ": " + siteId);
        }
        return site;
    }

    private DrSiteVO transientSite(DrSiteInventoryRequest request) {
        DrSiteVO site = new DrSiteVO("transient", request.getSiteType(), null);
        DrSiteCredentialInput input = request.getCredentialInput();
        if (input != null) {
            site.setEndpoint(input.getEndpoint());
        }
        return site;
    }

    private DrResolvedSiteCredential transientCredential(DrSiteCredentialInput input) {
        if (input == null || !input.hasCredentialData()) {
            return null;
        }
        DrSiteCredentialVO credential = new DrSiteCredentialVO(0L, DrConstants.CREDENTIAL_TYPE_MOLD_API);
        credential.setEndpoint(input.getEndpoint());
        credential.setTlsVerify(input.getTlsVerify() == null ? Boolean.TRUE : input.getTlsVerify());
        credential.setState(DrConstants.CREDENTIAL_STATE_CONFIGURED);
        credential.setSecretPayload(GSON.toJson(input.toSecretPayload()));
        return new DrResolvedSiteCredential(credential, input.toSecretPayload());
    }

    private boolean isMoldSite(String siteType) {
        return StringUtils.startsWith(StringUtils.upperCase(siteType), "MOLD_");
    }

    private DrSiteInventoryResult complete(DrSiteInventoryResult result, String state, String reasonCode, String message, long started) {
        result.setHealthState(state);
        result.setReasonCode(reasonCode);
        result.setMessage(message);
        result.setLatencyMs(System.currentTimeMillis() - started);
        if (result.getCheckedAt() == null) {
            result.setCheckedAt(new Date());
        }
        return result;
    }
}
