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
package com.cloud.dr.health;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrResolvedSiteCredential;
import com.cloud.dr.DrSiteCredentialService;
import com.cloud.dr.DrSiteCredentialVO;
import com.cloud.dr.DrSiteVO;
import com.cloud.dr.dao.DrSiteCredentialDao;
import com.cloud.utils.component.ManagerBase;

public class DrSiteHealthCheckServiceImpl extends ManagerBase implements DrSiteHealthCheckService {
    private static final Logger LOGGER = LogManager.getLogger(DrSiteHealthCheckServiceImpl.class);

    @Inject
    private DrSiteCredentialService drSiteCredentialService;
    @Inject
    private DrSiteCredentialDao drSiteCredentialDao;

    private final List<DrSiteProbe> probes = Arrays.asList(new DrMoldSiteProbe(), new DrVmwareDirectSiteProbe());

    @Override
    public DrSiteHealthCheckResult checkSite(DrSiteVO site, boolean persistCredentialValidation) {
        long started = System.currentTimeMillis();
        Date checkedAt = new Date();
        DrResolvedSiteCredential resolvedCredential = drSiteCredentialService.resolveCredential(site);
        if (resolvedCredential == null || !resolvedCredential.hasSecrets()) {
            return result(DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_CREDENTIAL_MISSING,
                    "DR site credentials are not configured", started, checkedAt, false);
        }
        try {
            DrSiteProbe probe = findProbe(site, resolvedCredential.getCredential());
            if (probe == null) {
                return result(DrConstants.HEALTH_UNKNOWN, DrConstants.HEALTH_REASON_UNSUPPORTED_SITE_TYPE,
                        "No DR site probe supports this site and credential type", started, checkedAt, false);
            }
            DrSiteHealthCheckResult result = probe.check(site, resolvedCredential);
            if (persistCredentialValidation && result.isCredentialValidated()) {
                markCredentialValidated(resolvedCredential.getCredential(), result.getCheckedAt());
            }
            return result;
        } catch (RuntimeException e) {
            LOGGER.warn(String.format("Failed to check DR site %s health", site != null ? site.getId() : null), e);
            return result(DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_ENDPOINT_UNREACHABLE,
                    "DR site health check failed: " + e.getClass().getSimpleName(), started, checkedAt, false);
        } finally {
            resolvedCredential.close();
        }
    }

    private DrSiteProbe findProbe(DrSiteVO site, DrSiteCredentialVO credential) {
        for (DrSiteProbe probe : probes) {
            if (probe.supports(site, credential)) {
                return probe;
            }
        }
        return null;
    }

    private void markCredentialValidated(DrSiteCredentialVO credential, Date validatedAt) {
        if (credential == null || credential.getId() <= 0) {
            return;
        }
        DrSiteCredentialVO latest = drSiteCredentialDao.findById(credential.getId());
        if (latest == null || latest.getRemoved() != null) {
            return;
        }
        latest.setLastValidated(validatedAt == null ? new Date() : validatedAt);
        latest.markUpdated();
        drSiteCredentialDao.update(latest.getId(), latest);
    }

    private DrSiteHealthCheckResult result(String state, String reasonCode, String message, long started, Date checkedAt, boolean credentialValidated) {
        return new DrSiteHealthCheckResult(state, reasonCode, message, System.currentTimeMillis() - started, checkedAt, credentialValidated);
    }
}
