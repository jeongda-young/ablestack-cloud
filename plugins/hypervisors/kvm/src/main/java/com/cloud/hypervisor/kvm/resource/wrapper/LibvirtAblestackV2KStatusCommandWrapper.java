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

import com.cloud.agent.api.AblestackV2KStatusAnswer;
import com.cloud.agent.api.AblestackV2KStatusCommand;
import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ResourceWrapper(handles = AblestackV2KStatusCommand.class)
public class LibvirtAblestackV2KStatusCommandWrapper extends CommandWrapper<AblestackV2KStatusCommand, Answer, LibvirtComputingResource> {

    private static final Path V2K_FLEET_ROOT = Path.of("/var/lib/ablestack-v2k/fleet");
    private static final Pattern JSON_STRING_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern CLOUD_CUTOVER_COMPLETED_PATTERN = Pattern.compile("Cloud cutover completed:\\s*([0-9a-fA-F-]{36})");
    private static final String CLOUD_TARGET_PROVIDER = "ablestack-cloud";

    @Override
    public Answer execute(AblestackV2KStatusCommand cmd, LibvirtComputingResource serverResource) {
        if (StringUtils.isBlank(cmd.getVmName())) {
            return new AblestackV2KStatusAnswer(cmd, false, "Missing vm name for ablestack_v2k status command");
        }

        final long timeout = (long) cmd.getWait() * 1000;
        Script script = new Script("ablestack_v2k", timeout, logger);
        script.add("status");
        script.add("--vm", cmd.getVmName());
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        int exitValue = script.getExitValue();
        if (exitValue != 0) {
            return new AblestackV2KStatusAnswer(cmd, false,
                    StringUtils.defaultIfBlank(result, parser.getLines()));
        }

        String output = StringUtils.defaultIfBlank(parser.getLines(), StringUtils.defaultString(result));
        AblestackV2KStatusAnswer jsonAnswer = parseJsonStatus(cmd, output);
        if (jsonAnswer != null) {
            return jsonAnswer;
        }

        String[] lines = output.split("\\r?\\n");
        String dataLine = null;
        for (String line : lines) {
            String trimmedLine = StringUtils.trimToEmpty(line);
            if (StringUtils.isBlank(trimmedLine)) {
                continue;
            }
            if (trimmedLine.startsWith("VM ") || trimmedLine.startsWith("-")) {
                continue;
            }
            dataLine = trimmedLine;
        }

        if (StringUtils.isBlank(dataLine)) {
            return new AblestackV2KStatusAnswer(cmd, false, "Unable to parse ablestack_v2k status output: no data line found");
        }

        String[] columns = dataLine.split("\\s{2,}");
        if (columns.length < 6) {
            return new AblestackV2KStatusAnswer(cmd, false,
                    String.format("Unable to parse ablestack_v2k status output for VM %s", cmd.getVmName()));
        }

        String phase = columns[1];
        String migrationState = columns[2];
        String migrationStep = columns[3];
        String syncPhysical = columns[4];
        String workdir = String.join(" ", java.util.Arrays.copyOfRange(columns, 5, columns.length));

        AblestackV2KStatusAnswer fleetState = getLatestFleetState(cmd, phase, migrationState, migrationStep, syncPhysical, workdir);
        if (fleetState != null) {
            return fleetState;
        }

        return buildStatusAnswer(cmd, "OK", phase, migrationState, migrationStep, syncPhysical, workdir);
    }

    private AblestackV2KStatusAnswer parseJsonStatus(AblestackV2KStatusCommand cmd, String output) {
        try {
            JsonElement parsed = new JsonParser().parse(output);
            JsonObject status = null;
            if (parsed.isJsonArray()) {
                JsonArray array = parsed.getAsJsonArray();
                for (JsonElement item : array) {
                    if (!item.isJsonObject()) {
                        continue;
                    }
                    JsonObject candidate = item.getAsJsonObject();
                    if (StringUtils.equals(getString(candidate, "vm"), cmd.getVmName())) {
                        status = candidate;
                        break;
                    }
                }
                if (status == null && array.size() > 0 && array.get(0).isJsonObject()) {
                    status = array.get(0).getAsJsonObject();
                }
            } else if (parsed.isJsonObject()) {
                status = parsed.getAsJsonObject();
            }
            if (status == null) {
                return null;
            }
            String phase = getString(status, "phase");
            String migrationState = getString(status, "state");
            String migrationStep = StringUtils.defaultIfBlank(getString(status, "display_step"), getString(status, "step"));
            String syncPhysical = getString(status, "sync");
            String workdir = getString(status, "workdir");
            AblestackV2KStatusAnswer answer = buildStatusAnswer(cmd, "OK", phase, migrationState, migrationStep, syncPhysical, workdir);
            applyStructuredProgress(answer, status);
            return answer;
        } catch (RuntimeException e) {
            logger.debug("Unable to parse ablestack_v2k JSON status output for VM {}", cmd.getVmName(), e);
            return null;
        }
    }

    private void applyStructuredProgress(AblestackV2KStatusAnswer answer, JsonObject status) {
        answer.setDisplayStep(StringUtils.defaultIfBlank(getString(status, "display_step"), getString(status, "step")));
        JsonObject syncProgress = getObject(status, "sync_progress");
        if (syncProgress != null) {
            answer.setSyncProgressLabel(getString(syncProgress, "mode"));
            answer.setSyncDoneBytes(getLong(syncProgress, "done_bytes"));
            answer.setSyncTotalBytes(getLong(syncProgress, "total_bytes"));
            answer.setSyncPercent(getInteger(syncProgress, "percent"));
        }
        JsonObject syncTotal = getObject(status, "sync_total");
        if (syncTotal != null) {
            answer.setSyncCumulativeDoneBytes(getLong(syncTotal, "done_bytes"));
            answer.setSyncCumulativeKnownBytes(getLong(syncTotal, "known_total_bytes"));
            answer.setSyncCumulativePercent(getInteger(syncTotal, "percent"));
        }
    }

    private AblestackV2KStatusAnswer getLatestFleetState(AblestackV2KStatusCommand cmd, String phase, String migrationState,
                                                         String migrationStep, String syncPhysical, String workdir) {
        if (!isUnknownStatus(phase, migrationState) || !Files.isDirectory(V2K_FLEET_ROOT)) {
            return null;
        }
        Path latestStatePath = findLatestFleetStatePath(cmd.getVmName());
        if (latestStatePath == null) {
            return null;
        }
        try {
            String json = Files.readString(latestStatePath, StandardCharsets.UTF_8);
            String fleetPhase = StringUtils.defaultIfBlank(getJsonStringField(json, "phase"), phase);
            String fleetState = StringUtils.defaultIfBlank(getJsonStringField(json, "state"), migrationState);
            String fleetWorkdir = StringUtils.defaultIfBlank(getJsonStringField(json, "workdir"), workdir);
            String step = StringUtils.defaultIfBlank(migrationStep, "-");
            if (StringUtils.isBlank(step) || StringUtils.equals(step, "-") || StringUtils.equalsIgnoreCase(step, "unknown")) {
                step = fleetState;
            }
            return buildStatusAnswer(cmd, "OK", fleetPhase, fleetState, step, syncPhysical, fleetWorkdir);
        } catch (IOException e) {
            logger.debug("Unable to read ablestack-v2k fleet state from {}", latestStatePath, e);
            return null;
        }
    }

    private AblestackV2KStatusAnswer buildStatusAnswer(AblestackV2KStatusCommand cmd, String details, String phase, String migrationState,
                                                       String migrationStep, String syncPhysical, String workdir) {
        String targetProvider = null;
        String cloudVmId = null;
        Path manifestPath = resolveManifestPath(workdir);
        if (manifestPath != null) {
            try {
                String manifestJson = Files.readString(manifestPath, StandardCharsets.UTF_8);
                targetProvider = getJsonStringField(manifestJson, "provider");
                cloudVmId = getJsonStringField(manifestJson, "vm_id");
            } catch (IOException e) {
                logger.debug("Unable to read ablestack-v2k manifest from {}", manifestPath, e);
            }
        }
        if (StringUtils.isBlank(cloudVmId) && isCompletedPhase2(phase, migrationState)) {
            cloudVmId = getLatestCloudCutoverVmId(cmd.getVmName());
            if (StringUtils.isNotBlank(cloudVmId)) {
                targetProvider = CLOUD_TARGET_PROVIDER;
            }
        }
        return new AblestackV2KStatusAnswer(cmd, true, details, phase, migrationState, migrationStep,
                syncPhysical, workdir, targetProvider, cloudVmId);
    }

    private Path resolveManifestPath(String workdir) {
        if (StringUtils.isBlank(workdir)) {
            return null;
        }
        try {
            Path workdirPath = Path.of(StringUtils.trim(workdir));
            if (!workdirPath.isAbsolute()) {
                return null;
            }
            Path manifestPath = workdirPath.resolve("manifest.json");
            return Files.isRegularFile(manifestPath) ? manifestPath : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private boolean isUnknownStatus(String phase, String migrationState) {
        return StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(phase), "unknown")
                && StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(migrationState), "unknown");
    }

    private boolean isCompletedPhase2(String phase, String migrationState) {
        return StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(phase), "phase2")
                && StringUtils.equalsAnyIgnoreCase(StringUtils.trimToEmpty(migrationState), "done", "completed", "success");
    }

    private Path findLatestFleetStatePath(String vmName) {
        String stateFileName = vmName + ".json";
        try (Stream<Path> paths = Files.walk(V2K_FLEET_ROOT, 3)) {
            Optional<Path> latestPath = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> StringUtils.equals(path.getFileName().toString(), stateFileName))
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()));
            return latestPath.orElse(null);
        } catch (IOException e) {
            logger.debug("Unable to scan ablestack-v2k fleet state directory {}", V2K_FLEET_ROOT, e);
            return null;
        }
    }

    private String getLatestCloudCutoverVmId(String vmName) {
        Path latestOutputPath = findLatestFleetOutputPath(vmName);
        if (latestOutputPath == null) {
            return null;
        }
        try {
            String output = Files.readString(latestOutputPath, StandardCharsets.UTF_8);
            Matcher matcher = CLOUD_CUTOVER_COMPLETED_PATTERN.matcher(output);
            String cloudVmId = null;
            while (matcher.find()) {
                cloudVmId = matcher.group(1);
            }
            return cloudVmId;
        } catch (IOException e) {
            logger.debug("Unable to read ablestack-v2k fleet output from {}", latestOutputPath, e);
            return null;
        }
    }

    private Path findLatestFleetOutputPath(String vmName) {
        String outputFileName = vmName + ".out";
        try (Stream<Path> paths = Files.walk(V2K_FLEET_ROOT, 2)) {
            Optional<Path> latestPath = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> StringUtils.equals(path.getFileName().toString(), outputFileName))
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()));
            return latestPath.orElse(null);
        } catch (IOException e) {
            logger.debug("Unable to scan ablestack-v2k fleet output directory {}", V2K_FLEET_ROOT, e);
            return null;
        }
    }

    private String getJsonStringField(String json, String fieldName) {
        Matcher matcher = Pattern.compile(String.format(JSON_STRING_FIELD_PATTERN.pattern(), Pattern.quote(fieldName))).matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private JsonObject getObject(JsonObject object, String memberName) {
        if (object == null) {
            return null;
        }
        JsonElement element = object.get(memberName);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private String getString(JsonObject object, String memberName) {
        if (object == null) {
            return null;
        }
        JsonElement element = object.get(memberName);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        return element.getAsString();
    }

    private Long getLong(JsonObject object, String memberName) {
        if (object == null) {
            return null;
        }
        JsonElement element = object.get(memberName);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
            return null;
        }
        try {
            return element.getAsLong();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Integer getInteger(JsonObject object, String memberName) {
        Long value = getLong(object, memberName);
        return value != null ? value.intValue() : null;
    }
}
