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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DrResolvedSiteCredential implements AutoCloseable {
    private final DrSiteCredentialVO credential;
    private final JsonObject secretPayload;

    public DrResolvedSiteCredential(DrSiteCredentialVO credential, JsonObject secretPayload) {
        this.credential = credential;
        this.secretPayload = secretPayload == null ? new JsonObject() : secretPayload;
    }

    public DrSiteCredentialVO getCredential() {
        return credential;
    }

    public JsonObject getSecretPayload() {
        return secretPayload;
    }

    public boolean hasSecrets() {
        return secretPayload.entrySet() != null && !secretPayload.entrySet().isEmpty();
    }

    public JsonObject toRuntimeJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", credential.getCredentialType());
        object.addProperty("endpoint", credential.getEndpoint());
        object.addProperty("principal", credential.getPrincipal());
        object.addProperty("tlsVerify", !Boolean.FALSE.equals(credential.getTlsVerify()));
        object.add("auth", secretPayload.deepCopy());
        return object;
    }

    public static JsonObject parseSecretPayload(String payload) {
        if (StringUtils.isBlank(payload)) {
            return new JsonObject();
        }
        try {
            JsonElement parsed = JsonParser.parseString(payload);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException e) {
            return new JsonObject();
        }
    }

    @Override
    public void close() {
        List<String> keys = new ArrayList<String>();
        for (Map.Entry<String, JsonElement> entry : secretPayload.entrySet()) {
            keys.add(entry.getKey());
        }
        for (String key : keys) {
            secretPayload.remove(key);
        }
    }
}
