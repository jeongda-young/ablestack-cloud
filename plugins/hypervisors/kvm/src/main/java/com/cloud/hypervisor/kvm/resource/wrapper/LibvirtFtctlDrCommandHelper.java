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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.script.Script;
import com.google.gson.JsonObject;

public final class LibvirtFtctlDrCommandHelper {

    private LibvirtFtctlDrCommandHelper() {
    }

    public static File writeProfileJson(String planUuid, String profileJson) throws IOException {
        return writeOwnerOnlyJson("ftctl-dr-" + safeToken(planUuid, "plan") + "-", profileJson);
    }

    public static File writeArtifactSpecJson(String runUuid, String artifactSpecJson) throws IOException {
        return writeOwnerOnlyJson("ftctl-dr-artifact-" + safeToken(runUuid, "run") + "-", artifactSpecJson);
    }

    public static File writeAuthoritySpecJson(String runUuid, String authoritySpecJson) throws IOException {
        return writeOwnerOnlyJson("ftctl-dr-authority-" + safeToken(runUuid, "run") + "-", authoritySpecJson);
    }

    private static File writeOwnerOnlyJson(String prefix, String json) throws IOException {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        File file = File.createTempFile(prefix, ".json");
        restrictOwnerOnly(file);
        Files.write(file.toPath(), json.getBytes(StandardCharsets.UTF_8));
        restrictOwnerOnly(file);
        return file;
    }

    private static String safeToken(String value, String fallback) {
        return StringUtils.defaultIfBlank(value, fallback).replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static void restrictOwnerOnly(File file) {
        file.setReadable(false, false);
        file.setWritable(false, false);
        file.setExecutable(false, false);
        file.setReadable(true, true);
        file.setWritable(true, true);
    }

    public static void deleteQuietly(File file) {
        if (file != null) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException ignored) {
            }
        }
    }

    public static void addPlanRunArgs(Script script, String planUuid, String runUuid) {
        if (StringUtils.isNotBlank(planUuid)) {
            script.add("--plan");
            script.add(planUuid);
        }
        if (StringUtils.isNotBlank(runUuid)) {
            script.add("--run");
            script.add(runUuid);
        }
    }

    public static void addProfileJsonArg(Script script, File profileFile) {
        if (profileFile != null) {
            script.add("--profile-json");
            script.add(profileFile.getAbsolutePath());
        }
    }

    public static void addArtifactSpecJsonArg(Script script, File artifactSpecFile) {
        if (artifactSpecFile != null) {
            script.add("--artifact-spec-json");
            script.add(artifactSpecFile.getAbsolutePath());
        }
    }

    public static void addAuthoritySpecJsonArg(Script script, File authoritySpecFile) {
        if (authoritySpecFile != null) {
            script.add("--authority-spec-json");
            script.add(authoritySpecFile.getAbsolutePath());
        }
    }

    public static String getString(JsonObject payload, String key) {
        return LibvirtFtctlWrapperHelper.getString(payload, key);
    }

    public static Integer getInteger(JsonObject payload, String key) {
        return LibvirtFtctlWrapperHelper.getInteger(payload, key);
    }

    public static Long getLong(JsonObject payload, String key) {
        return LibvirtFtctlWrapperHelper.getLong(payload, key);
    }

    public static Boolean getBoolean(JsonObject payload, String key) {
        return LibvirtFtctlWrapperHelper.getBoolean(payload, key);
    }
}
