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

import java.net.HttpURLConnection;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrResolvedSiteCredential;
import com.cloud.dr.DrSiteCredentialVO;
import com.cloud.dr.DrSiteVO;
import com.google.gson.JsonObject;

public class DrVmwareDirectSiteProbe implements DrSiteProbe {
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final int THUMBPRINT_TIMEOUT_MS = 10000;

    @Override
    public boolean supports(DrSiteVO site, DrSiteCredentialVO credential) {
        return credential != null && StringUtils.equals(DrConstants.CREDENTIAL_TYPE_VCENTER, credential.getCredentialType());
    }

    @Override
    public DrSiteHealthCheckResult check(DrSiteVO site, DrResolvedSiteCredential credential) {
        long started = System.currentTimeMillis();
        Date checkedAt = new Date();
        try {
            String endpoint = StringUtils.defaultIfBlank(credential.getCredential().getEndpoint(), site.getEndpoint());
            String rootEndpoint = DrSiteProbeSupport.normalizeRootEndpoint(endpoint, "https");
            JsonObject secret = credential.getSecretPayload();
            String principal = StringUtils.trimToNull(credential.getCredential().getPrincipal());
            String password = getSecret(secret, "password");
            if (StringUtils.isAnyBlank(rootEndpoint, principal, password)) {
                return result(DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_CREDENTIAL_MISSING,
                        "vCenter URL, username, and password are required", started, checkedAt, false, rootEndpoint);
            }

            DrSiteHealthCheckResult restResult = checkRestSession(rootEndpoint, principal, password, credential, started, checkedAt);
            if (!StringUtils.equals(DrConstants.HEALTH_DEGRADED, restResult.getHealthState())) {
                return restResult;
            }
            return checkSdkReachability(rootEndpoint, principal, password, credential, started, checkedAt);
        } catch (Exception e) {
            return result(DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_ENDPOINT_UNREACHABLE,
                    "vCenter endpoint is unreachable: " + e.getClass().getSimpleName(), started, checkedAt, false, null);
        }
    }

    private DrSiteHealthCheckResult checkRestSession(String rootEndpoint, String principal, String password, DrResolvedSiteCredential credential,
            long started, Date checkedAt) throws Exception {
        String url = DrSiteProbeSupport.appendPath(rootEndpoint, "/rest/com/vmware/cis/session");
        HttpURLConnection connection = DrSiteProbeSupport.openConnection(url, "POST", credential.getCredential().getTlsVerify(), CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", DrSiteProbeSupport.basicAuth(principal, password));
        int responseCode = connection.getResponseCode();
        DrSiteProbeSupport.readBody(connection);
        if (responseCode >= 200 && responseCode < 300) {
            return result(DrConstants.HEALTH_CONNECTED, DrConstants.HEALTH_REASON_VCENTER_API_OK,
                    "vCenter credentials were validated", started, checkedAt, true, rootEndpoint);
        }
        if (responseCode == 401 || responseCode == 403) {
            return result(DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_CREDENTIAL_INVALID,
                    "vCenter authentication failed with HTTP " + responseCode, started, checkedAt, false, rootEndpoint);
        }
        if (responseCode == 404 || responseCode == 405) {
            return result(DrConstants.HEALTH_DEGRADED, DrConstants.HEALTH_REASON_VCENTER_ENDPOINT_REACHABLE,
                    "vCenter REST session API was not available; checking SDK endpoint", started, checkedAt, false, rootEndpoint);
        }
        return result(DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_ENDPOINT_HTTP_ERROR,
                "vCenter REST session API returned HTTP " + responseCode, started, checkedAt, false, rootEndpoint);
    }

    private DrSiteHealthCheckResult checkSdkReachability(String rootEndpoint, String principal, String password, DrResolvedSiteCredential credential,
            long started, Date checkedAt) throws Exception {
        String url = DrSiteProbeSupport.appendPath(rootEndpoint, "/sdk");
        HttpURLConnection connection = DrSiteProbeSupport.openConnection(url, "GET", credential.getCredential().getTlsVerify(), CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "text/xml, application/xml, */*");
        connection.setRequestProperty("Authorization", DrSiteProbeSupport.basicAuth(principal, password));
        int responseCode = connection.getResponseCode();
        DrSiteProbeSupport.readBody(connection);
        if (responseCode == 401 || responseCode == 403) {
            return result(DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_CREDENTIAL_INVALID,
                    "vCenter SDK authentication failed with HTTP " + responseCode, started, checkedAt, false, rootEndpoint);
        }
        if (responseCode >= 200 && responseCode < 500) {
            return result(DrConstants.HEALTH_DEGRADED, DrConstants.HEALTH_REASON_VCENTER_ENDPOINT_REACHABLE,
                    "vCenter endpoint is reachable, but REST credential validation is not available", started, checkedAt, false, rootEndpoint);
        }
        return result(DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_ENDPOINT_HTTP_ERROR,
                "vCenter SDK endpoint returned HTTP " + responseCode, started, checkedAt, false, rootEndpoint);
    }

    private String getSecret(JsonObject secret, String key) {
        return secret != null && secret.has(key) && !secret.get(key).isJsonNull() ? StringUtils.trimToNull(secret.get(key).getAsString()) : null;
    }

    private DrSiteHealthCheckResult result(String state, String reasonCode, String message, long started, Date checkedAt,
            boolean credentialValidated, String rootEndpoint) {
        return new DrSiteHealthCheckResult(state, reasonCode, message, System.currentTimeMillis() - started, checkedAt,
                credentialValidated, buildDetails(rootEndpoint));
    }

    private JsonObject buildDetails(String rootEndpoint) {
        JsonObject details = new JsonObject();
        if (StringUtils.isBlank(rootEndpoint)) {
            details.addProperty("vcenterThumbprintPresent", false);
            details.addProperty("vcenterThumbprintSource", "missing-endpoint");
            return details;
        }
        details.addProperty("vcenterEndpoint", rootEndpoint);
        try {
            String thumbprint = DrSiteProbeSupport.fetchSha1Thumbprint(rootEndpoint, THUMBPRINT_TIMEOUT_MS);
            if (StringUtils.isNotBlank(thumbprint)) {
                details.addProperty("vcenterThumbprint", thumbprint);
                details.addProperty("vcenterThumbprintPresent", true);
                details.addProperty("vcenterThumbprintSource", "probe");
                return details;
            }
        } catch (Exception e) {
            details.addProperty("vcenterThumbprintError", e.getClass().getSimpleName());
        }
        details.addProperty("vcenterThumbprintPresent", false);
        details.addProperty("vcenterThumbprintSource", "unresolved");
        return details;
    }
}
