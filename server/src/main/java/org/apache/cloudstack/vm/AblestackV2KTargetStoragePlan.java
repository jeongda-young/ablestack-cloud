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
package org.apache.cloudstack.vm;

import java.util.LinkedHashMap;
import java.util.Map;

public class AblestackV2KTargetStoragePlan {

    private final String targetProvider;
    private final String targetProfile;
    private final String targetStorage;
    private final String targetFormat;
    private final String destinationPath;
    private final String targetMapJson;
    private final String storageRoot;
    private final String poolType;

    public AblestackV2KTargetStoragePlan(String targetProvider, String targetProfile, String targetStorage,
                                         String targetFormat, String destinationPath, String targetMapJson,
                                         String storageRoot, String poolType) {
        this.targetProvider = targetProvider;
        this.targetProfile = targetProfile;
        this.targetStorage = targetStorage;
        this.targetFormat = targetFormat;
        this.destinationPath = destinationPath;
        this.targetMapJson = targetMapJson;
        this.storageRoot = storageRoot;
        this.poolType = poolType;
    }

    public String getTargetProvider() {
        return targetProvider;
    }

    public String getTargetProfile() {
        return targetProfile;
    }

    public String getTargetStorage() {
        return targetStorage;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public String getTargetMapJson() {
        return targetMapJson;
    }

    public String getStorageRoot() {
        return storageRoot;
    }

    public String getPoolType() {
        return poolType;
    }

    public Map<String, String> toContextMap() {
        Map<String, String> context = new LinkedHashMap<>();
        context.put("targetProvider", targetProvider);
        context.put("targetProfile", targetProfile);
        context.put("targetStorage", targetStorage);
        context.put("targetFormat", targetFormat);
        context.put("destinationPath", destinationPath);
        context.put("targetMapJson", targetMapJson);
        context.put("storageRoot", storageRoot);
        context.put("poolType", poolType);
        return context;
    }
}
