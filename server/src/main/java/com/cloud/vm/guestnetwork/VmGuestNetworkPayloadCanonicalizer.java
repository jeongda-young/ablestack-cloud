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
package com.cloud.vm.guestnetwork;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.cloud.agent.api.VmGuestNetworkState;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class VmGuestNetworkPayloadCanonicalizer {
    static final int MAX_PAYLOAD_BYTES = 2 * 1024 * 1024;
    private final Gson gson = GsonHelper.getGson();

    public CanonicalPayload canonicalize(VmGuestNetworkState state) {
        JsonObject root = gson.toJsonTree(state).getAsJsonObject();
        root.remove("observedAt");
        String payload = canonicalizeElement(root).toString();
        if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Guest network payload exceeds 2 MiB");
        }
        return new CanonicalPayload(payload, sha256(payload));
    }

    private JsonElement canonicalizeElement(JsonElement element) {
        if (element.isJsonObject()) {
            Map<String, JsonElement> sorted = new TreeMap<>();
            element.getAsJsonObject().entrySet().forEach(entry ->
                    sorted.put(entry.getKey(), canonicalizeElement(entry.getValue())));
            JsonObject result = new JsonObject();
            sorted.forEach(result::add);
            return result;
        }
        if (element.isJsonArray()) {
            List<JsonElement> values = new ArrayList<>();
            element.getAsJsonArray().forEach(value -> values.add(canonicalizeElement(value)));
            values.sort(Comparator.comparing(JsonElement::toString));
            JsonArray result = new JsonArray();
            values.forEach(result::add);
            return result;
        }
        return element.deepCopy();
    }

    private String sha256(String payload) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("SHA-256 is not available", e);
        }
    }

    public static final class CanonicalPayload {
        private final String payload;
        private final String hash;

        CanonicalPayload(String payload, String hash) {
            this.payload = payload;
            this.hash = hash;
        }

        public String getPayload() {
            return payload;
        }

        public String getHash() {
            return hash;
        }
    }
}
