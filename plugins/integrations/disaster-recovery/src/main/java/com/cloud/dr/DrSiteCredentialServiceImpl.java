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

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.dao.DrSiteCredentialDao;
import com.cloud.dr.dao.DrSiteDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class DrSiteCredentialServiceImpl extends ManagerBase implements DrSiteCredentialService {
    private static final Gson GSON = new Gson();

    @Inject
    private DrSiteCredentialDao drSiteCredentialDao;
    @Inject
    private DrSiteDao drSiteDao;

    @Override
    public DrSiteCredentialVO findActiveCredential(long siteId) {
        return drSiteCredentialDao.findConfiguredBySiteId(siteId);
    }

    @Override
    public DrSiteCredentialVO findConfiguredCredential(DrSiteVO site) {
        if (site == null || site.getId() <= 0) {
            return null;
        }
        if (site.getCredentialId() != null) {
            DrSiteCredentialVO credential = drSiteCredentialDao.findConfiguredByIdAndSiteId(site.getCredentialId(), site.getId());
            if (credential != null) {
                return credential;
            }
        }
        return drSiteCredentialDao.findConfiguredBySiteId(site.getId());
    }

    @Override
    public DrSiteCredentialVO findLatestCredential(long siteId) {
        return drSiteCredentialDao.findLatestBySiteId(siteId);
    }

    @Override
    public DrSiteCredentialVO upsertCredential(DrSiteVO site, DrSiteCredentialInput input) {
        if (site == null || site.getId() <= 0) {
            throw new InvalidParameterValueException("DR site is required before saving credentials");
        }
        if (input == null || !input.hasCredentialData()) {
            return findConfiguredCredential(site);
        }

        String credentialType = inferCredentialType(site, input);
        validateInput(credentialType, input);

        DrSiteCredentialVO credential = findConfiguredCredential(site);
        if (credential == null) {
            credential = new DrSiteCredentialVO(site.getId(), credentialType);
        } else {
            credential.setCredentialType(credentialType);
        }
        credential.setEndpoint(StringUtils.trimToNull(input.getEndpoint()));
        credential.setPrincipal(StringUtils.trimToNull(input.getPrincipal()));
        credential.setSecretPayload(GSON.toJson(input.toSecretPayload()));
        credential.setTlsVerify(input.getTlsVerify() == null ? Boolean.TRUE : input.getTlsVerify());
        credential.setState(DrConstants.CREDENTIAL_STATE_CONFIGURED);
        credential.markUpdated();

        if (credential.getId() > 0) {
            drSiteCredentialDao.update(credential.getId(), credential);
        } else {
            credential = drSiteCredentialDao.persist(credential);
        }

        site.setCredentialId(credential.getId());
        site.setCredentialRef(null);
        site.markUpdated();
        drSiteDao.update(site.getId(), site);
        return credential;
    }

    @Override
    public void clearCredential(DrSiteVO site) {
        if (site == null || site.getId() <= 0) {
            return;
        }
        DrSiteCredentialVO credential = findConfiguredCredential(site);
        if (credential != null) {
            clearCredentialRow(credential);
        }
        if (site.getCredentialId() != null || site.getCredentialRef() != null) {
            site.setCredentialId(null);
            site.setCredentialRef(null);
            site.markUpdated();
            drSiteDao.update(site.getId(), site);
        }
    }

    @Override
    public void clearCredentialsForDeletedSite(long siteId) {
        List<DrSiteCredentialVO> credentials = drSiteCredentialDao.listBySiteId(siteId);
        for (DrSiteCredentialVO credential : credentials) {
            if (credential.getRemoved() != null) {
                continue;
            }
            clearCredentialRow(credential);
        }
    }

    @Override
    public DrResolvedSiteCredential resolveCredential(DrSiteVO site) {
        if (site == null || site.getId() <= 0) {
            return null;
        }
        DrSiteCredentialVO credential = findConfiguredCredential(site);
        if (credential == null || !StringUtils.equals(DrConstants.CREDENTIAL_STATE_CONFIGURED, credential.getState())) {
            return null;
        }
        JsonObject payload = DrResolvedSiteCredential.parseSecretPayload(credential.getSecretPayload());
        return new DrResolvedSiteCredential(credential, payload);
    }

    @Override
    public boolean hasUsableCredential(DrSiteVO site) {
        DrResolvedSiteCredential credential = resolveCredential(site);
        return credential != null && credential.hasSecrets();
    }

    @Override
    public String inferCredentialType(DrSiteVO site, DrSiteCredentialInput input) {
        if (input != null && StringUtils.isNotBlank(input.getCredentialType())) {
            return StringUtils.upperCase(input.getCredentialType());
        }
        String siteType = site != null ? StringUtils.upperCase(site.getSiteType()) : "";
        String hypervisorType = site != null ? StringUtils.upperCase(site.getHypervisorType()) : "";
        if (StringUtils.startsWith(siteType, "MOLD_")) {
            return DrConstants.CREDENTIAL_TYPE_MOLD_API;
        }
        if (StringUtils.equals(siteType, "VMWARE_DIRECT")
                || StringUtils.equals(hypervisorType, StringUtils.upperCase(DrConstants.HYPERVISOR_TYPE_VMWARE))) {
            return DrConstants.CREDENTIAL_TYPE_VCENTER;
        }
        return DrConstants.CREDENTIAL_TYPE_MOLD_API;
    }

    private void clearCredentialRow(DrSiteCredentialVO credential) {
        credential.setState(DrConstants.CREDENTIAL_STATE_CLEARED);
        credential.markUpdated();
        if (!drSiteCredentialDao.update(credential.getId(), credential)) {
            throw new CloudRuntimeException("Failed to clear DR site credential " + credential.getId());
        }
        if (!drSiteCredentialDao.remove(credential.getId())) {
            throw new CloudRuntimeException("Failed to delete DR site credential " + credential.getId());
        }
        DrSiteCredentialVO removedCredential = drSiteCredentialDao.findByIdIncludingRemoved(credential.getId());
        if (removedCredential == null || removedCredential.getRemoved() == null) {
            throw new CloudRuntimeException("DR site credential soft delete was not persisted " + credential.getId());
        }
    }

    private void validateInput(String credentialType, DrSiteCredentialInput input) {
        if (StringUtils.isBlank(input.getEndpoint())) {
            throw new InvalidParameterValueException("DR site credential endpoint is required");
        }
        if (StringUtils.equals(DrConstants.CREDENTIAL_TYPE_VCENTER, credentialType)) {
            if (StringUtils.isBlank(input.getPrincipal()) || StringUtils.isBlank(input.getPassword())) {
                throw new InvalidParameterValueException("vCenter username and password are required");
            }
            return;
        }
        if (StringUtils.equals(DrConstants.CREDENTIAL_TYPE_MOLD_API, credentialType)) {
            if (StringUtils.isBlank(input.getApiKey()) || StringUtils.isBlank(input.getSecretKey())) {
                throw new InvalidParameterValueException("Mold API key and secret key are required");
            }
            return;
        }
        throw new InvalidParameterValueException("Unsupported DR site credential type: " + credentialType);
    }
}
