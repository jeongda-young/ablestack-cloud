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
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrResolvedSiteCredential;
import com.cloud.dr.DrSiteCredentialVO;
import com.cloud.dr.DrSiteVO;
import com.google.gson.JsonObject;

public class DrMoldSiteProbe implements DrSiteProbe {
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;
    private static final String API_COMMAND_LIST_CAPABILITIES = "listCapabilities";

    @Override
    public boolean supports(DrSiteVO site, DrSiteCredentialVO credential) {
        return credential != null && StringUtils.equals(DrConstants.CREDENTIAL_TYPE_MOLD_API, credential.getCredentialType());
    }

    @Override
    public DrSiteHealthCheckResult check(DrSiteVO site, DrResolvedSiteCredential credential) {
        long started = System.currentTimeMillis();
        Date checkedAt = new Date();
        try {
            String endpoint = StringUtils.defaultIfBlank(credential.getCredential().getEndpoint(), site.getEndpoint());
            String apiEndpoint = normalizeMoldApiEndpoint(endpoint);
            JsonObject secret = credential.getSecretPayload();
            String apiKey = getSecret(secret, "apiKey");
            String secretKey = getSecret(secret, "secretKey");
            if (StringUtils.isAnyBlank(apiEndpoint, apiKey, secretKey)) {
                return result(DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_CREDENTIAL_MISSING,
                        "Mold API endpoint, API key, and secret key are required", started, checkedAt, false);
            }

            Map<String, String> params = new LinkedHashMap<String, String>();
            params.put("command", API_COMMAND_LIST_CAPABILITIES);
            params.put("response", "json");
            params.put("apiKey", apiKey);
            String query = DrSiteProbeSupport.buildQuery(params);
            String signature = DrSiteProbeSupport.signCloudStackRequest(params, secretKey);
            String requestUrl = apiEndpoint + "?" + query + "&signature=" + signature;
            HttpURLConnection connection = DrSiteProbeSupport.openConnection(requestUrl, "GET", credential.getCredential().getTlsVerify(), CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/json");
            int responseCode = connection.getResponseCode();
            DrSiteProbeSupport.readBody(connection);
            if (responseCode >= 200 && responseCode < 300) {
                return result(DrConstants.HEALTH_CONNECTED, DrConstants.HEALTH_REASON_MOLD_API_OK,
                        "Mold API credentials were validated", started, checkedAt, true);
            }
            if (responseCode == 401 || responseCode == 403) {
                return result(DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_CREDENTIAL_INVALID,
                        "Mold API authentication failed with HTTP " + responseCode, started, checkedAt, false);
            }
            return result(DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_ENDPOINT_HTTP_ERROR,
                    "Mold API returned HTTP " + responseCode, started, checkedAt, false);
        } catch (Exception e) {
            return result(DrConstants.HEALTH_DISCONNECTED, DrConstants.HEALTH_REASON_ENDPOINT_UNREACHABLE,
                    "Mold API endpoint is unreachable: " + e.getClass().getSimpleName(), started, checkedAt, false);
        }
    }

    private String normalizeMoldApiEndpoint(String endpoint) throws Exception {
        String normalized = DrSiteProbeSupport.normalizeEndpoint(endpoint, "http");
        String lower = StringUtils.lowerCase(normalized);
        if (StringUtils.contains(lower, "/client/api")) {
            return normalized;
        }
        return DrSiteProbeSupport.appendPath(normalized, "/client/api");
    }

    private String getSecret(JsonObject secret, String key) {
        return secret != null && secret.has(key) && !secret.get(key).isJsonNull() ? StringUtils.trimToNull(secret.get(key).getAsString()) : null;
    }

    private DrSiteHealthCheckResult result(String state, String reasonCode, String message, long started, Date checkedAt, boolean credentialValidated) {
        return new DrSiteHealthCheckResult(state, reasonCode, message, System.currentTimeMillis() - started, checkedAt, credentialValidated, moldProbeDetails());
    }

    private JsonObject moldProbeDetails() {
        JsonObject details = new JsonObject();
        details.addProperty("probe", getClass().getSimpleName());
        details.addProperty("apiCommand", API_COMMAND_LIST_CAPABILITIES);
        details.addProperty("authAlgorithm", DrSiteProbeSupport.CLOUDSTACK_API_HMAC_ALGORITHM);
        return details;
    }
}
