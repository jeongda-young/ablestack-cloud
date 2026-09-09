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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FtctlDrCapabilitiesAnswer;
import com.cloud.agent.api.FtctlDrCapabilitiesCommand;
import com.cloud.agent.api.FtctlDrActionCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@ResourceWrapper(handles = FtctlDrCapabilitiesCommand.class)
public class LibvirtFtctlDrCapabilitiesCommandWrapper extends CommandWrapper<FtctlDrCapabilitiesCommand, Answer, LibvirtComputingResource> {
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final Map<String, String> ACTION_TO_CLI = buildActionToCliMap();

    @Override
    public Answer execute(FtctlDrCapabilitiesCommand command, LibvirtComputingResource serverResource) {
        CapabilitySnapshot snapshot = readCapabilities();
        List<String> missingCliCommands = missing(command.getRequiredCliCommands(), snapshot.supportedCliCommands);
        List<String> missingActions = missingActions(command.getRequiredActions(), snapshot.supportedCliCommands);
        List<String> missingFeatures = missing(command.getRequiredFeatures(), snapshot.supportedFeatures);
        boolean success = missingCliCommands.isEmpty() && missingActions.isEmpty() && missingFeatures.isEmpty();
        String details = success ? "FTCTL_DR capabilities satisfy requested action contract"
                : "FTCTL_DR capability mismatch: missing actions=" + missingActions + ", missing commands="
                        + missingCliCommands + ", missing features=" + missingFeatures;
        FtctlDrCapabilitiesAnswer answer = new FtctlDrCapabilitiesAnswer(command, success, details,
                command.getPlanUuid(), command.getRunUuid(), snapshot.supportedActions, snapshot.supportedCliCommands,
                missingActions, missingCliCommands, snapshot.ftctlVersion, snapshot.runtimeSchemaVersion,
                snapshot.capabilitiesJson);
        answer.setActionContractVersion(FtctlDrActionCommand.ACTION_CONTRACT_VERSION);
        answer.setSupportedFeatures(snapshot.supportedFeatures);
        answer.setMissingFeatures(missingFeatures);
        answer.setReprotectAuthorityContractVersions(snapshot.reprotectAuthorityContractVersions);
        answer.setActionCommandCodeSource(codeSource(FtctlDrActionCommand.class));
        answer.setWrapperCodeSource(codeSource(getClass()));
        if (StringUtils.contains(answer.getActionCommandCodeSource(), "cloud-plugin-hypervisor-kvm")) {
            logger.warn("FTCTL_DR action command was loaded from the KVM plugin instead of cloud-core: {}",
                    answer.getActionCommandCodeSource());
        }
        return answer;
    }

    private CapabilitySnapshot readCapabilities() {
        Script script = new Script("ablestack_vm_ftctl", DEFAULT_TIMEOUT_SECONDS * 1000L, logger);
        script.add("dr-capabilities");
        script.add("--json");
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        JsonObject payload = LibvirtFtctlWrapperHelper.parseJsonObject(output);
        if (script.getExitValue() == 0 && payload != null && payload.has("supported_commands")) {
            return fromPayload(payload, output);
        }
        logger.debug("Falling back to FTCTL_DR help capability detection because dr-capabilities failed: {}", StringUtils.defaultIfBlank(output, result));
        return fromHelp();
    }

    private CapabilitySnapshot fromPayload(JsonObject payload, String output) {
        CapabilitySnapshot snapshot = new CapabilitySnapshot();
        snapshot.capabilitiesJson = output;
        snapshot.ftctlVersion = LibvirtFtctlDrCommandHelper.getString(payload, "ftctl_version");
        snapshot.runtimeSchemaVersion = LibvirtFtctlDrCommandHelper.getString(payload, "runtime_schema_version");
        snapshot.supportedCliCommands.addAll(jsonArrayValues(payload.getAsJsonArray("supported_commands")));
        snapshot.supportedFeatures.addAll(jsonArrayValues(payload.getAsJsonArray("supported_features")));
        snapshot.reprotectAuthorityContractVersions.addAll(
                reprotectAuthorityContractVersions(payload));
        snapshot.supportedActions.addAll(toActionNames(snapshot.supportedCliCommands));
        return snapshot;
    }

    private CapabilitySnapshot fromHelp() {
        Script script = new Script("ablestack_vm_ftctl", DEFAULT_TIMEOUT_SECONDS * 1000L, logger);
        script.add("--help");
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        CapabilitySnapshot snapshot = new CapabilitySnapshot();
        snapshot.capabilitiesJson = output;
        if (script.getExitValue() != 0 && StringUtils.isBlank(output)) {
            snapshot.capabilitiesJson = result;
            return snapshot;
        }
        for (FtctlDrActionCommand.Action action : FtctlDrActionCommand.Action.values()) {
            if (StringUtils.contains(output, action.getCliCommand())) {
                snapshot.supportedCliCommands.add(action.getCliCommand());
                snapshot.supportedActions.add(action.name());
            }
        }
        if (StringUtils.contains(output, "dr-status")) {
            snapshot.supportedCliCommands.add("dr-status");
        }
        return snapshot;
    }

    static List<String> reprotectAuthorityContractVersions(JsonObject payload) {
        return jsonArrayValues(payload != null
                ? payload.getAsJsonArray("reprotect_authority_contract_versions") : null);
    }

    private static List<String> jsonArrayValues(JsonArray array) {
        List<String> values = new ArrayList<String>();
        if (array == null) {
            return values;
        }
        for (JsonElement element : array) {
            if (element != null && element.isJsonPrimitive()) {
                String value = StringUtils.trimToNull(element.getAsString());
                if (value != null) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private List<String> toActionNames(List<String> cliCommands) {
        List<String> actions = new ArrayList<String>();
        for (Map.Entry<String, String> entry : ACTION_TO_CLI.entrySet()) {
            if (containsIgnoreCase(cliCommands, entry.getValue())) {
                actions.add(entry.getKey());
            }
        }
        return actions;
    }

    private List<String> missing(List<String> required, List<String> supported) {
        List<String> missing = new ArrayList<String>();
        if (required == null) {
            return missing;
        }
        for (String value : required) {
            if (StringUtils.isNotBlank(value) && !containsIgnoreCase(supported, value)) {
                missing.add(value);
            }
        }
        return missing;
    }

    private List<String> missingActions(List<String> requiredActions, List<String> supportedCliCommands) {
        List<String> missing = new ArrayList<String>();
        if (requiredActions == null) {
            return missing;
        }
        Set<String> supported = new HashSet<String>();
        for (String actionName : toActionNames(supportedCliCommands)) {
            supported.add(StringUtils.upperCase(actionName, Locale.ROOT));
        }
        for (String required : requiredActions) {
            String normalized = StringUtils.upperCase(required, Locale.ROOT);
            String requiredCli = ACTION_TO_CLI.get(normalized);
            boolean cliSupported = StringUtils.isNotBlank(requiredCli) && containsIgnoreCase(supportedCliCommands, requiredCli);
            if (StringUtils.isNotBlank(required) && !supported.contains(normalized) && !cliSupported) {
                missing.add(required);
            }
        }
        return missing;
    }

    private static Map<String, String> buildActionToCliMap() {
        Map<String, String> actions = new LinkedHashMap<String, String>();
        actions.put("SYNC", "dr-sync-start");
        actions.put("RECOVER_SYNC", "dr-sync-recover");
        actions.put("PAUSE_SYNC", "dr-sync-pause");
        actions.put("RESUME_SYNC", "dr-sync-resume");
        actions.put("TEST_FAILOVER", "dr-test-failover");
        actions.put("TEST_CLEANUP", "dr-test-cleanup");
        actions.put("TEST_PREPARE", "dr-test-prepare");
        actions.put("TEST_ARTIFACT_CLEANUP", "dr-test-artifact-cleanup");
        actions.put("FAILOVER", "dr-failover");
        actions.put("FAILBACK", "dr-failback");
        actions.put("REPROTECT", "dr-reprotect");
        actions.put("TARGET_MATERIALIZED", "dr-target-materialized");
        actions.put("TARGET_EXPORT_START", "dr-target-export-start");
        actions.put("TARGET_EXPORT_STOP", "dr-target-export-stop");
        actions.put("CUTOVER_COMMIT", "dr-cutover-commit");
        actions.put("FAILOVER_ABORT", "dr-failover-abort");
        actions.put("FAILBACK_COMMIT", "dr-failback-commit");
        actions.put("FAILBACK_ABORT", "dr-failback-abort");
        actions.put("RELEASE", "dr-release");
        return Collections.unmodifiableMap(actions);
    }

    private String codeSource(Class<?> type) {
        try {
            URL location = type.getProtectionDomain().getCodeSource().getLocation();
            return location != null ? location.toExternalForm() : "unknown";
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    private boolean containsIgnoreCase(List<String> values, String needle) {
        if (StringUtils.isBlank(needle) || values == null) {
            return false;
        }
        for (String value : values) {
            if (StringUtils.equalsIgnoreCase(value, needle)) {
                return true;
            }
        }
        return false;
    }

    private static final class CapabilitySnapshot {
        private final List<String> supportedActions = new ArrayList<String>();
        private final List<String> supportedCliCommands = new ArrayList<String>();
        private final List<String> supportedFeatures = new ArrayList<String>();
        private final List<String> reprotectAuthorityContractVersions = new ArrayList<String>();
        private String ftctlVersion;
        private String runtimeSchemaVersion;
        private String capabilitiesJson;
    }
}
