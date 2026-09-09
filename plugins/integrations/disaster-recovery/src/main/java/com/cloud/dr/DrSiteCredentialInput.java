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

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;

public class DrSiteCredentialInput {
    private String credentialType;
    private String endpoint;
    private String principal;
    private String apiKey;
    private String secretKey;
    private String password;
    private Boolean tlsVerify;

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getTlsVerify() {
        return tlsVerify;
    }

    public void setTlsVerify(Boolean tlsVerify) {
        this.tlsVerify = tlsVerify;
    }

    public boolean hasCredentialData() {
        return StringUtils.isNotBlank(endpoint)
                || StringUtils.isNotBlank(principal)
                || StringUtils.isNotBlank(apiKey)
                || StringUtils.isNotBlank(secretKey)
                || StringUtils.isNotBlank(password)
                || tlsVerify != null;
    }

    public JsonObject toSecretPayload() {
        JsonObject payload = new JsonObject();
        if (StringUtils.isNotBlank(apiKey)) {
            payload.addProperty("apiKey", apiKey);
        }
        if (StringUtils.isNotBlank(secretKey)) {
            payload.addProperty("secretKey", secretKey);
        }
        if (StringUtils.isNotBlank(password)) {
            payload.addProperty("password", password);
        }
        return payload;
    }
}
