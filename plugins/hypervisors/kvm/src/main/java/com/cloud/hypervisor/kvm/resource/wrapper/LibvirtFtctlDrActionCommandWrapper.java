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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FtctlDrActionAnswer;
import com.cloud.agent.api.FtctlDrActionCommand;
import com.cloud.agent.api.FtctlDrErrorCodes;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

@ResourceWrapper(handles = FtctlDrActionCommand.class)
public class LibvirtFtctlDrActionCommandWrapper extends CommandWrapper<FtctlDrActionCommand, Answer, LibvirtComputingResource> {

    private static final int DEFAULT_TIMEOUT_SECONDS = 45;
    private static final Pattern SHA1_FINGERPRINT_PATTERN = Pattern.compile("(?i)(?:SHA1\\s+)?Fingerprint\\s*=\\s*([0-9A-F:]+)");

    @Override
    public Answer execute(FtctlDrActionCommand command, LibvirtComputingResource serverResource) {
        ActionDescriptor action = resolveAction(command);
        if (StringUtils.isBlank(action.getCliCommand())) {
            String message = "Missing or unsupported FTCTL_DR action"
                    + (StringUtils.isNotBlank(command.getActionName()) ? ": " + command.getActionName() : "");
            return new FtctlDrActionAnswer(command, false, message, command.getAction(),
                    command.getPlanUuid(), command.getRunUuid(), null, false, null, null, 0,
                    null, null, FtctlDrErrorCodes.AGENT_CAPABILITY_MISMATCH, 20, message, null);
        }
        if (StringUtils.isBlank(command.getPlanUuid())) {
            return new FtctlDrActionAnswer(command, false, "Missing DR plan UUID");
        }
        if (StringUtils.isBlank(command.getRunUuid())) {
            return new FtctlDrActionAnswer(command, false, "Missing DR run UUID");
        }
        String actionContractError = validateActionContract(command);
        if (StringUtils.isNotBlank(actionContractError)) {
            return new FtctlDrActionAnswer(command, false, actionContractError, command.getAction(),
                    command.getPlanUuid(), command.getRunUuid(), "error", false, "REJECTED",
                    "action-contract-validation", 0, null, null, "DR_ACTION_INTENT_MISMATCH",
                    20, actionContractError, null);
        }

        File profileFile = null;
        File artifactSpecFile = null;
        File authoritySpecFile = null;
        try {
            profileFile = LibvirtFtctlDrCommandHelper.writeProfileJson(command.getPlanUuid(), enrichProfileJson(command.getProfileJson(), serverResource));
            validateArtifactSpec(command);
            validateAuthoritySpec(command);
            artifactSpecFile = LibvirtFtctlDrCommandHelper.writeArtifactSpecJson(command.getRunUuid(), command.getArtifactSpecJson());
            authoritySpecFile = LibvirtFtctlDrCommandHelper.writeAuthoritySpecJson(command.getRunUuid(), command.getAuthoritySpecJson());
            return executeFtctl(command, action, profileFile, artifactSpecFile, authoritySpecFile);
        } catch (IOException e) {
            return new FtctlDrActionAnswer(command, false, "Unable to prepare FTCTL_DR command contract: " + e.getMessage());
        } finally {
            LibvirtFtctlDrCommandHelper.deleteQuietly(profileFile);
            LibvirtFtctlDrCommandHelper.deleteQuietly(artifactSpecFile);
            LibvirtFtctlDrCommandHelper.deleteQuietly(authoritySpecFile);
        }
    }

    private String validateActionContract(FtctlDrActionCommand command) {
        if (StringUtils.isNotBlank(command.getActionIntent())
                && !StringUtils.equalsIgnoreCase(command.getActionIntent(), command.getRunType())) {
            return "DR_ACTION_INTENT_MISMATCH: action intent does not match run type";
        }
        String expectedRunType = expectedRunType(command.getAction());
        if (StringUtils.isNotBlank(expectedRunType)
                && !StringUtils.equalsIgnoreCase(expectedRunType, command.getRunType())) {
            return "DR_ACTION_INTENT_MISMATCH: FTCTL action does not match run type";
        }
        if (command.getAction() == FtctlDrActionCommand.Action.FAILBACK_COMMIT
                || command.getAction() == FtctlDrActionCommand.Action.FAILBACK_COMMIT_STATUS) {
            if (!StringUtils.equals("DR_FAILBACK_COMMIT_V1", command.getFailbackCommitContractVersion())
                    || StringUtils.isAnyBlank(command.getFailbackSessionId(),
                            command.getFailbackCommitAttemptId(), command.getFailbackCommitEnvelopeSha256())
                    || command.getFailbackCheckpointSequence() == null
                    || command.getFailbackAuthorityGeneration() == null
                    || command.getFailbackBaselineGeneration() == null) {
                return "DR_FAILBACK_COMMIT_CONTRACT_INVALID: typed failback commit envelope is incomplete";
            }
        }
        if (command.getAction() == FtctlDrActionCommand.Action.CUTOVER_COMMIT) {
            if (!StringUtils.equals("DR_CUTOVER_COMMIT_V2", command.getCutoverCommitContractVersion())
                    || StringUtils.isAnyBlank(command.getCutoverEngineSessionId(), command.getCutoverCloudSessionId(),
                            command.getCutoverManifestSha256(), command.getCutoverCommitAttemptId(),
                            command.getCutoverCommitEnvelopeSha256(), command.getCutoverTargetExternalRef(),
                            command.getCutoverTargetPowerState(), command.getCutoverBootValidationState(),
                            command.getCutoverSourceFenceState(), command.getCutoverSourcePowerState())
                    || command.getCutoverCheckpointSequence() == null
                    || command.getCutoverAuthorityGeneration() == null
                    || command.getCutoverTargetVmId() == null) {
                return "DR_CUTOVER_COMMIT_CONTRACT_INVALID: typed cutover commit envelope is incomplete";
            }
        }
        if (command.getAction() == FtctlDrActionCommand.Action.CUTOVER_COMMIT_STATUS
                && (!StringUtils.equals("DR_CUTOVER_COMMIT_V2", command.getCutoverCommitContractVersion())
                        || StringUtils.isAnyBlank(command.getCutoverEngineSessionId(),
                                command.getCutoverCommitAttemptId(), command.getCutoverCommitEnvelopeSha256()))) {
            return "DR_CUTOVER_COMMIT_CONTRACT_INVALID: typed cutover commit status identity is incomplete";
        }
        return null;
    }

    private String expectedRunType(FtctlDrActionCommand.Action action) {
        if (action == null) {
            return null;
        }
        switch (action) {
            case SYNC:
                return "SYNC";
            case RECOVER_SYNC:
                return "RECOVER_SYNC";
            case PAUSE_SYNC:
                return "PAUSE_SYNC";
            case RESUME_SYNC:
                return "RESUME_SYNC";
            case TEST_PREPARE:
            case TEST_FAILOVER:
                return "TEST_FAILOVER";
            case TEST_CLEANUP:
            case TEST_ARTIFACT_CLEANUP:
                return "TEST_CLEANUP";
            case FAILOVER:
                return "FAILOVER";
            case FAILBACK:
                return "FAILBACK";
            case REPROTECT:
                return "REPROTECT";
            case RELEASE:
                return "RELEASE";
            default:
                return null;
        }
    }

    static void validateAuthoritySpec(FtctlDrActionCommand command) throws IOException {
        if (command.getAction() != FtctlDrActionCommand.Action.REPROTECT) {
            return;
        }
        JsonObject spec = LibvirtFtctlWrapperHelper.parseJsonObject(command.getAuthoritySpecJson());
        String specContractVersion = spec != null
                ? LibvirtFtctlDrCommandHelper.getString(spec, "contractVersion") : null;
        if (StringUtils.isBlank(command.getAuthorityContractVersion())
                || !StringUtils.equals(command.getAuthorityContractVersion(), specContractVersion)) {
            throw new IOException("DR_REPROTECT_AUTHORITY_INVALID: command and authority spec contract versions must match");
        }
        if (spec == null
                || !StringUtils.equals("TARGET", LibvirtFtctlDrCommandHelper.getString(spec, "expectedActiveSide"))
                || LibvirtFtctlDrCommandHelper.getLong(spec, "authorityGeneration") == null
                || LibvirtFtctlDrCommandHelper.getLong(spec, "checkpointSequence") == null
                || LibvirtFtctlDrCommandHelper.getLong(spec, "targetVmId") == null
                || StringUtils.isBlank(LibvirtFtctlDrCommandHelper.getString(spec, "cutoverSessionId"))) {
            throw new IOException("DR_REPROTECT_AUTHORITY_INVALID: committed target authority fields are required");
        }
    }

    private void validateArtifactSpec(FtctlDrActionCommand command) throws IOException {
        if (command.getAction() != FtctlDrActionCommand.Action.TEST_PREPARE) {
            return;
        }
        JsonObject spec = LibvirtFtctlWrapperHelper.parseJsonObject(command.getArtifactSpecJson());
        if (spec == null || !StringUtils.equals("3", LibvirtFtctlDrCommandHelper.getString(spec, "contractVersion"))) {
            throw new IOException("DR_TEST_ARTIFACT_SPEC_INVALID: contractVersion 3 is required");
        }
        if (!spec.has("disks") || !spec.get("disks").isJsonArray() || spec.getAsJsonArray("disks").size() == 0) {
            throw new IOException("DR_TEST_ARTIFACT_SPEC_INVALID: at least one disk locator is required");
        }
        JsonArray disks = spec.getAsJsonArray("disks");
        for (int index = 0; index < disks.size(); index++) {
            JsonElement element = disks.get(index);
            if (element == null || !element.isJsonObject()) {
                throw new IOException("DR_TEST_ARTIFACT_SPEC_INVALID: disk " + index + " is not an object");
            }
            JsonObject disk = element.getAsJsonObject();
            String provider = LibvirtFtctlDrCommandHelper.getString(disk, "provider");
            String locator = LibvirtFtctlDrCommandHelper.getString(disk, "canonicalLocator");
            if (StringUtils.equalsIgnoreCase(provider, "RBD")) {
                String rbdSpec = StringUtils.removeStart(locator, "rbd:");
                if (!StringUtils.startsWith(locator, "rbd:") || !StringUtils.contains(rbdSpec, "/")) {
                    throw new IOException("DR_TEST_ARTIFACT_LOCATOR_INVALID: disk " + index + " requires rbd:pool/image");
                }
            } else if (StringUtils.equalsIgnoreCase(provider, "FILE")) {
                if (!StringUtils.startsWith(locator, "file:/")) {
                    throw new IOException("DR_TEST_ARTIFACT_LOCATOR_INVALID: disk " + index + " requires file:/absolute/path");
                }
            } else {
                throw new IOException("DR_TEST_ARTIFACT_PROVIDER_UNSUPPORTED: disk " + index + " provider=" + provider);
            }
        }
    }

    private String enrichProfileJson(String profileJson, LibvirtComputingResource serverResource) {
        JsonObject profile = LibvirtFtctlWrapperHelper.parseJsonObject(profileJson);
        if (profile == null || profile.entrySet().isEmpty() || !profileHasVmwareSource(profile) || serverResource == null) {
            return profileJson;
        }
        JsonObject credentials = objectAt(profile, "credentials");
        JsonObject sourceCredential = objectAt(credentials, "source");
        if (sourceCredential.entrySet().isEmpty()) {
            return profileJson;
        }
        if (StringUtils.isBlank(LibvirtFtctlDrCommandHelper.getString(sourceCredential, "vddkLibdir"))
                && StringUtils.isNotBlank(serverResource.getVddkLibDir())) {
            sourceCredential.addProperty("vddkLibdir", serverResource.getVddkLibDir());
        }
        if (StringUtils.isBlank(LibvirtFtctlDrCommandHelper.getString(sourceCredential, "vddkVersion"))
                && StringUtils.isNotBlank(serverResource.getVddkVersion())) {
            sourceCredential.addProperty("vddkVersion", serverResource.getVddkVersion());
        }
        enrichVcenterThumbprint(sourceCredential, profile);
        return profile.toString();
    }

    private void enrichVcenterThumbprint(JsonObject sourceCredential, JsonObject profile) {
        if (sourceCredential == null || Boolean.TRUE.equals(LibvirtFtctlDrCommandHelper.getBoolean(sourceCredential, "tlsVerify"))) {
            return;
        }
        String thumbprint = StringUtils.defaultIfBlank(
                LibvirtFtctlDrCommandHelper.getString(sourceCredential, "thumbprint"),
                LibvirtFtctlDrCommandHelper.getString(sourceCredential, "tlsThumbprint"));
        if (StringUtils.isNotBlank(thumbprint)) {
            sourceCredential.addProperty("thumbprint", thumbprint);
            if (StringUtils.isBlank(LibvirtFtctlDrCommandHelper.getString(sourceCredential, "thumbprintSource"))) {
                sourceCredential.addProperty("thumbprintSource", "runtime");
            }
            return;
        }
        String endpoint = LibvirtFtctlDrCommandHelper.getString(sourceCredential, "endpoint");
        thumbprint = getVcenterThumbprint(endpoint, DEFAULT_TIMEOUT_SECONDS * 1000L, LibvirtFtctlDrCommandHelper.getString(profile, "name"));
        if (StringUtils.isNotBlank(thumbprint)) {
            sourceCredential.addProperty("thumbprint", thumbprint);
            sourceCredential.addProperty("thumbprintPresent", true);
            sourceCredential.addProperty("thumbprintSource", "agent-auto");
        } else {
            sourceCredential.addProperty("thumbprintPresent", false);
            sourceCredential.addProperty("thumbprintSource", "agent-unresolved");
        }
    }

    protected String getVcenterThumbprint(String endpoint, long timeout, String planName) {
        String connectTarget = normalizeVcenterConnectTarget(endpoint);
        if (StringUtils.isBlank(connectTarget)) {
            return null;
        }
        String command = String.format("openssl s_client -connect '%s' </dev/null 2>/dev/null | openssl x509 -fingerprint -sha1 -noout",
                StringUtils.replace(connectTarget, "'", "'\\''"));
        Script script = new Script("/bin/bash", timeout, logger);
        script.add("-c");
        script.add(command);
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        script.execute(parser);
        if (script.getExitValue() != 0) {
            logger.warn("({}) Failed to fetch vCenter thumbprint for {}", planName, connectTarget);
            return null;
        }
        String thumbprint = extractSha1Fingerprint(parser.getLines());
        if (StringUtils.isBlank(thumbprint)) {
            logger.warn("({}) Failed to parse vCenter thumbprint for {}", planName, connectTarget);
            return null;
        }
        return thumbprint;
    }

    private String normalizeVcenterConnectTarget(String endpoint) {
        String normalized = StringUtils.trimToNull(endpoint);
        if (normalized == null) {
            return null;
        }
        normalized = StringUtils.removeStart(normalized, "https://");
        normalized = StringUtils.removeStart(normalized, "http://");
        normalized = StringUtils.substringBefore(normalized, "/");
        if (StringUtils.isBlank(normalized)) {
            return null;
        }
        return StringUtils.contains(normalized, ":") ? normalized : normalized + ":443";
    }

    private String extractSha1Fingerprint(String output) {
        String parsedOutput = StringUtils.trimToEmpty(output);
        if (StringUtils.isBlank(parsedOutput)) {
            return null;
        }
        for (String line : parsedOutput.split("\\R")) {
            String trimmedLine = StringUtils.trimToEmpty(line);
            if (StringUtils.isBlank(trimmedLine)) {
                continue;
            }
            Matcher matcher = SHA1_FINGERPRINT_PATTERN.matcher(trimmedLine);
            if (matcher.find()) {
                return matcher.group(1).toUpperCase(Locale.ROOT);
            }
            if (trimmedLine.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2})+")) {
                return trimmedLine.toUpperCase(Locale.ROOT);
            }
        }
        return null;
    }

    private boolean profileHasVmwareSource(JsonObject profile) {
        String direction = LibvirtFtctlDrCommandHelper.getString(profile, "direction");
        JsonObject source = objectAt(profile, "source");
        String provider = LibvirtFtctlDrCommandHelper.getString(source, "provider");
        String driver = LibvirtFtctlDrCommandHelper.getString(source, "driver");
        return StringUtils.startsWithIgnoreCase(direction, "VMWARE_")
                || StringUtils.equalsIgnoreCase(provider, "VMWARE")
                || StringUtils.containsIgnoreCase(driver, "VMWARE")
                || StringUtils.containsIgnoreCase(driver, "VDDK");
    }

    private JsonObject objectAt(JsonObject object, String key) {
        if (object == null || StringUtils.isBlank(key) || !object.has(key) || !object.get(key).isJsonObject()) {
            return new JsonObject();
        }
        return object.getAsJsonObject(key);
    }

    private ActionDescriptor resolveAction(FtctlDrActionCommand command) {
        if (command == null) {
            return new ActionDescriptor(null);
        }
        if (command.getAction() != null) {
            return new ActionDescriptor(command.getAction().getCliCommand());
        }
        String actionName = StringUtils.trimToNull(command.getActionName());
        String cliCommand = StringUtils.trimToNull(command.getCliCommand());
        if (StringUtils.isNotBlank(actionName)) {
            for (FtctlDrActionCommand.Action action : FtctlDrActionCommand.Action.values()) {
                if (StringUtils.equalsIgnoreCase(action.name(), actionName)) {
                    command.setAction(action);
                    return new ActionDescriptor(StringUtils.defaultIfBlank(cliCommand, action.getCliCommand()));
                }
            }
        }
        return new ActionDescriptor(cliCommand);
    }

    private FtctlDrActionAnswer executeFtctl(FtctlDrActionCommand command, ActionDescriptor action, File profileFile,
            File artifactSpecFile, File authoritySpecFile) {
        final long timeout = (long) (command.getWait() > 0 ? command.getWait() : DEFAULT_TIMEOUT_SECONDS) * 1000;
        Script script = new Script("ablestack_vm_ftctl", timeout, logger);
        script.add(action.getCliCommand());
        LibvirtFtctlDrCommandHelper.addPlanRunArgs(script, command.getPlanUuid(), command.getRunUuid());
        LibvirtFtctlDrCommandHelper.addProfileJsonArg(script, profileFile);
        LibvirtFtctlDrCommandHelper.addArtifactSpecJsonArg(script, artifactSpecFile);
        LibvirtFtctlDrCommandHelper.addAuthoritySpecJsonArg(script, authoritySpecFile);
        if (StringUtils.isNotBlank(command.getRole())) {
            script.add("--role");
            script.add(command.getRole());
        }
        if (StringUtils.isNotBlank(command.getMode())) {
            script.add("--mode");
            script.add(command.getMode());
        }
        String restorePointSelector = resolveRestorePointSelector(command);
        if (StringUtils.isNotBlank(restorePointSelector)) {
            script.add("--restore-point");
            script.add(restorePointSelector);
        }
        if (command.isForce()) {
            script.add("--force");
        }
        if (command.isDryRun()) {
            script.add("--dry-run");
        }
        addContextArg(script, command, "targetVmId", "--target-vm-id");
        addContextArg(script, command, "targetExternalRef", "--target-external-ref");
        addContextArg(script, command, "targetVmName", "--target-vm-name");
        addContextArg(script, command, "targetNetworkId", "--target-network-id");
        addContextArg(script, command, "targetVolumeMapJson", "--target-volume-map-json");
        addContextArg(script, command, "targetReadyRpoSeconds", "--target-ready-rpo-seconds");
        addContextArg(script, command, "materializationSpecJson", "--materialization-spec-json");
        addContextArg(script, command, "materializationSpecSha256", "--materialization-spec-sha256");
        addContextArg(script, command, "cutoverSessionId", "--session-id");
        addStringArg(script, command.getCutoverCommitContractVersion(), "--commit-contract-version");
        addStringArg(script, command.getCutoverEngineSessionId(), "--engine-session-id");
        addStringArg(script, command.getCutoverCloudSessionId(), "--cloud-session-id");
        addLongArg(script, command.getCutoverCheckpointSequence(), "--checkpoint-sequence");
        addStringArg(script, command.getCutoverManifestSha256(), "--manifest-sha256");
        addLongArg(script, command.getCutoverAuthorityGeneration(), "--authority-generation");
        addStringArg(script, command.getCutoverCommitAttemptId(), "--commit-attempt-id");
        addStringArg(script, command.getCutoverCommitEnvelopeSha256(), "--commit-envelope-sha256");
        addLongArg(script, command.getCutoverTargetVmId(), "--target-vm-id");
        addStringArg(script, command.getCutoverTargetExternalRef(), "--target-external-ref");
        addStringArg(script, command.getCutoverTargetPowerState(), "--target-power-state");
        addStringArg(script, command.getCutoverBootValidationState(), "--boot-validation-state");
        addStringArg(script, command.getCutoverSourceFenceState(), "--source-fence-state");
        addStringArg(script, command.getCutoverSourcePowerState(), "--source-power-state");
        addStringArg(script, command.getFailbackCommitContractVersion(), "--commit-contract-version");
        addStringArg(script, command.getFailbackSessionId(), "--session-id");
        addLongArg(script, command.getFailbackCheckpointSequence(), "--checkpoint-sequence");
        addLongArg(script, command.getFailbackAuthorityGeneration(), "--authority-generation");
        addLongArg(script, command.getFailbackBaselineGeneration(), "--baseline-generation");
        addStringArg(script, command.getFailbackEvidenceRunUuid(), "--evidence-run");
        addStringArg(script, command.getFailbackCommitAttemptId(), "--commit-attempt-id");
        addStringArg(script, command.getFailbackCommitEnvelopeSha256(), "--commit-envelope-sha256");
        addLongArg(script, command.getResumeBaselineCheckpointSequence(), "--resume-baseline-checkpoint-sequence");
        addLongArg(script, command.getMinimumCompletedCheckpointSequence(), "--minimum-completed-checkpoint-sequence");
        addLongArg(script, command.getAuthoritySequenceFloor(), "--authority-sequence-floor");
        if (command.isForceImmediateCycle()) {
            script.add("--force-immediate-cycle");
        }
        addStringArg(script, command.getFailbackTargetPowerState(), "--target-power-state");
        addStringArg(script, command.getFailbackSourcePowerState(), "--source-power-state");
        addStringArg(script, command.getFailbackBootValidationState(), "--boot-validation-state");
        addContextArg(script, command, "rollbackPhase", "--phase");
        if (!command.isWaitForCompletion()) {
            script.add("--wait=false");
        }
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new LibvirtFtctlWrapperHelper.FtctlProcessOutputParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        JsonObject payload = LibvirtFtctlWrapperHelper.parseJsonObject(output);
        int exitValue = script.getExitValue();
        boolean success = exitValue == 0 && (!isSemanticFailureStatus(payload)
                || isAcceptedCanceledFailbackAbortPrepare(command, payload)
                || isCompletedFailbackAbort(command, payload)
                || isCompletedFailoverAbort(command, payload));
        boolean transportAmbiguous = shouldProbeStatus(result, output, payload, exitValue);
        if (!success && command.getAction() == FtctlDrActionCommand.Action.CUTOVER_COMMIT
                && transportAmbiguous) {
            FtctlDrActionAnswer verified = probeCutoverCommitStatus(command);
            if (verified != null) {
                return verified;
            }
        }
        if (!success && command.getAction() == FtctlDrActionCommand.Action.FAILBACK_COMMIT
                && transportAmbiguous) {
            FtctlDrActionAnswer verified = probeFailbackCommitStatus(command);
            if (verified != null) {
                return verified;
            }
        }
        if (!success && !command.isWaitForCompletion() && transportAmbiguous) {
            FtctlDrActionAnswer accepted = probeAcceptedStatus(command);
            if (accepted != null) {
                return accepted;
            }
        }

        return new FtctlDrActionAnswer(command, success,
                StringUtils.defaultIfBlank(output, success ? "OK" : "FTCTL_DR action failed"),
                command.getAction(), command.getPlanUuid(), command.getRunUuid(),
                LibvirtFtctlDrCommandHelper.getString(payload, "result"),
                LibvirtFtctlDrCommandHelper.getBoolean(payload, "accepted"),
                LibvirtFtctlDrCommandHelper.getString(payload, "state"),
                LibvirtFtctlDrCommandHelper.getString(payload, "step"),
                LibvirtFtctlDrCommandHelper.getInteger(payload, "progress"),
                LibvirtFtctlDrCommandHelper.getString(payload, "external_job_ref"),
                LibvirtFtctlDrCommandHelper.getLong(payload, "events_offset"),
                success ? null : StringUtils.defaultIfBlank(LibvirtFtctlDrCommandHelper.getString(payload, "error_code"),
                        transportAmbiguous ? "DR_AGENT_ACCEPT_TIMEOUT" : null),
                exitValue, output, payload != null ? payload.toString() : null);
    }

    private void addContextArg(Script script, FtctlDrActionCommand command, String key, String option) {
        if (command.getContext() == null) {
            return;
        }
        String value = command.getContext().get(key);
        if (StringUtils.isNotBlank(value)) {
            script.add(option);
            script.add(value);
        }
    }

    private void addLongArg(Script script, Long value, String option) {
        if (value != null) {
            script.add(option);
            script.add(String.valueOf(value));
        }
    }

    private void addStringArg(Script script, String value, String option) {
        if (StringUtils.isNotBlank(value)) {
            script.add(option);
            script.add(value);
        }
    }

    static boolean shouldProbeStatus(String result, String output, JsonObject payload, int exitValue) {
        if (exitValue == 0 && payload != null) {
            return false;
        }
        if (payload != null && isSemanticFailureStatus(payload)) {
            return false;
        }
        return StringUtils.isBlank(output)
                || containsTransportFailure(result)
                || (payload == null && containsTransportFailure(output));
    }

    private static boolean containsTransportFailure(String value) {
        return StringUtils.containsIgnoreCase(value, "timed out")
                || StringUtils.containsIgnoreCase(value, "timeout")
                || StringUtils.containsIgnoreCase(value, "stream closed");
    }

    private FtctlDrActionAnswer probeFailbackCommitStatus(FtctlDrActionCommand command) {
        Script script = new Script("ablestack_vm_ftctl", 10000, logger);
        script.add(FtctlDrActionCommand.Action.FAILBACK_COMMIT_STATUS.getCliCommand());
        LibvirtFtctlDrCommandHelper.addPlanRunArgs(script, command.getPlanUuid(), command.getRunUuid());
        addStringArg(script, command.getFailbackCommitContractVersion(), "--commit-contract-version");
        addStringArg(script, command.getFailbackSessionId(), "--session-id");
        addStringArg(script, command.getFailbackCommitAttemptId(), "--commit-attempt-id");
        addStringArg(script, command.getFailbackCommitEnvelopeSha256(), "--commit-envelope-sha256");
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new LibvirtFtctlWrapperHelper.FtctlProcessOutputParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        JsonObject payload = LibvirtFtctlWrapperHelper.parseSingleJsonObject(output);
        if (script.getExitValue() != 0 || payload == null) {
            return null;
        }
        String outcome = LibvirtFtctlDrCommandHelper.getString(payload, "failback_commit_outcome");
        boolean acknowledged = StringUtils.equalsIgnoreCase(outcome, "ACKNOWLEDGED");
        return new FtctlDrActionAnswer(command, acknowledged,
                StringUtils.defaultIfBlank(output, "FTCTL_DR failback commit status"),
                command.getAction(), command.getPlanUuid(), command.getRunUuid(),
                LibvirtFtctlDrCommandHelper.getString(payload, "result"),
                acknowledged,
                LibvirtFtctlDrCommandHelper.getString(payload, "state"),
                LibvirtFtctlDrCommandHelper.getString(payload, "step"),
                LibvirtFtctlDrCommandHelper.getInteger(payload, "progress"),
                command.getRunUuid(),
                LibvirtFtctlDrCommandHelper.getLong(payload, "events_offset"),
                LibvirtFtctlDrCommandHelper.getString(payload, "error_code"),
                script.getExitValue(), output, payload.toString());
    }

    private FtctlDrActionAnswer probeCutoverCommitStatus(FtctlDrActionCommand command) {
        Script script = new Script("ablestack_vm_ftctl", 10000, logger);
        script.add(FtctlDrActionCommand.Action.CUTOVER_COMMIT_STATUS.getCliCommand());
        LibvirtFtctlDrCommandHelper.addPlanRunArgs(script, command.getPlanUuid(), command.getRunUuid());
        addStringArg(script, command.getCutoverCommitContractVersion(), "--commit-contract-version");
        addStringArg(script, command.getCutoverEngineSessionId(), "--engine-session-id");
        addStringArg(script, command.getCutoverCommitAttemptId(), "--commit-attempt-id");
        addStringArg(script, command.getCutoverCommitEnvelopeSha256(), "--commit-envelope-sha256");
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new LibvirtFtctlWrapperHelper.FtctlProcessOutputParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        JsonObject payload = LibvirtFtctlWrapperHelper.parseSingleJsonObject(output);
        if (script.getExitValue() != 0 || payload == null) {
            return null;
        }
        String outcome = LibvirtFtctlDrCommandHelper.getString(payload, "commit_outcome");
        boolean acknowledged = StringUtils.equalsIgnoreCase(outcome, "ACKNOWLEDGED");
        return new FtctlDrActionAnswer(command, acknowledged,
                StringUtils.defaultIfBlank(output, "FTCTL_DR cutover commit status"),
                command.getAction(), command.getPlanUuid(), command.getRunUuid(),
                LibvirtFtctlDrCommandHelper.getString(payload, "result"), acknowledged,
                LibvirtFtctlDrCommandHelper.getString(payload, "state"),
                LibvirtFtctlDrCommandHelper.getString(payload, "commit_state"),
                acknowledged ? 100 : 95, command.getRunUuid(),
                LibvirtFtctlDrCommandHelper.getLong(payload, "events_offset"),
                LibvirtFtctlDrCommandHelper.getString(payload, "error_code"),
                script.getExitValue(), output, payload.toString());
    }

    private FtctlDrActionAnswer probeAcceptedStatus(FtctlDrActionCommand command) {
        Script script = new Script("ablestack_vm_ftctl", 10000, logger);
        script.add("dr-status");
        LibvirtFtctlDrCommandHelper.addPlanRunArgs(script, command.getPlanUuid(), command.getRunUuid());
        script.add("--events-limit");
        script.add("0");
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new LibvirtFtctlWrapperHelper.FtctlProcessOutputParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        JsonObject payload = LibvirtFtctlWrapperHelper.parseSingleJsonObject(output);
        if (!isAcceptedStatus(script.getExitValue(), payload)) {
            return null;
        }
        return new FtctlDrActionAnswer(command, true, StringUtils.defaultIfBlank(output, "FTCTL_DR action accepted by status probe"),
                command.getAction(), command.getPlanUuid(), command.getRunUuid(),
                LibvirtFtctlDrCommandHelper.getString(payload, "result"),
                true,
                LibvirtFtctlDrCommandHelper.getString(payload, "state"),
                LibvirtFtctlDrCommandHelper.getString(payload, "step"),
                LibvirtFtctlDrCommandHelper.getInteger(payload, "progress"),
                StringUtils.defaultIfBlank(LibvirtFtctlDrCommandHelper.getString(payload, "external_job_ref"), command.getRunUuid()),
                LibvirtFtctlDrCommandHelper.getLong(payload, "events_offset"),
                LibvirtFtctlDrCommandHelper.getString(payload, "error_code"),
                0, output, payload != null ? payload.toString() : null);
    }

    static boolean isAcceptedStatus(int exitValue, JsonObject payload) {
        if (payload == null || isSemanticFailureStatus(payload)) {
            return false;
        }
        Boolean accepted = LibvirtFtctlDrCommandHelper.getBoolean(payload, "accepted");
        String state = StringUtils.upperCase(LibvirtFtctlDrCommandHelper.getString(payload, "state"));
        String result = StringUtils.lowerCase(LibvirtFtctlDrCommandHelper.getString(payload, "result"));
        return exitValue == 0 && (Boolean.TRUE.equals(accepted)
                || StringUtils.equalsAny(result, "accepted", "ok", "success", "delegated", "warn")
                || StringUtils.equalsAny(state, "SYNCING", "RUNNING", "READY", "TARGET_READY", "PAUSED", "TESTING"));
    }

    static boolean isSemanticFailureStatus(JsonObject payload) {
        if (payload == null) {
            return false;
        }
        Boolean accepted = LibvirtFtctlDrCommandHelper.getBoolean(payload, "accepted");
        String state = StringUtils.upperCase(LibvirtFtctlDrCommandHelper.getString(payload, "state"));
        String errorCode = LibvirtFtctlDrCommandHelper.getString(payload, "error_code");
        return Boolean.FALSE.equals(accepted)
                || StringUtils.isNotBlank(errorCode)
                || StringUtils.equalsAny(state, "ERROR", "FAILED", "REJECTED", "CANCELED", "CANCELLED");
    }

    static boolean isAcceptedCanceledFailbackAbortPrepare(FtctlDrActionCommand command, JsonObject payload) {
        if (command == null || payload == null
                || command.getAction() != FtctlDrActionCommand.Action.FAILBACK_ABORT
                || command.getContext() == null
                || !StringUtils.equalsIgnoreCase(command.getContext().get("rollbackPhase"), "prepare")) {
            return false;
        }
        Boolean accepted = LibvirtFtctlDrCommandHelper.getBoolean(payload, "accepted");
        String result = StringUtils.lowerCase(LibvirtFtctlDrCommandHelper.getString(payload, "result"));
        String state = StringUtils.upperCase(LibvirtFtctlDrCommandHelper.getString(payload, "state"));
        String rollbackState = StringUtils.upperCase(
                LibvirtFtctlDrCommandHelper.getString(payload, "rollback_state"));
        String errorCode = LibvirtFtctlDrCommandHelper.getString(payload, "error_code");
        return !Boolean.FALSE.equals(accepted)
                && StringUtils.equalsAny(result, "ok", "success")
                && StringUtils.equals(state, "CANCELED")
                && StringUtils.equals(rollbackState, "FENCED")
                && StringUtils.isBlank(errorCode);
    }

    static boolean isCompletedFailbackAbort(FtctlDrActionCommand command, JsonObject payload) {
        if (command == null || payload == null
                || command.getAction() != FtctlDrActionCommand.Action.FAILBACK_ABORT) {
            return false;
        }
        String result = StringUtils.lowerCase(LibvirtFtctlDrCommandHelper.getString(payload, "result"));
        String state = StringUtils.upperCase(LibvirtFtctlDrCommandHelper.getString(payload, "state"));
        String rollbackState = StringUtils.upperCase(
                LibvirtFtctlDrCommandHelper.getString(payload, "rollback_state"));
        String cloudLifecycleState = StringUtils.upperCase(
                LibvirtFtctlDrCommandHelper.getString(payload, "cloud_lifecycle_state"));
        String commitOutcome = StringUtils.upperCase(
                LibvirtFtctlDrCommandHelper.getString(payload, "failback_commit_outcome"));
        String activeSide = StringUtils.upperCase(LibvirtFtctlDrCommandHelper.getString(payload, "active_side"));
        String sourcePowerState = StringUtils.upperCase(
                LibvirtFtctlDrCommandHelper.getString(payload, "source_power_state"));
        String targetPowerState = StringUtils.upperCase(
                LibvirtFtctlDrCommandHelper.getString(payload, "target_power_state"));
        String errorCode = LibvirtFtctlDrCommandHelper.getString(payload, "error_code");
        return StringUtils.equalsAny(result, "ok", "success")
                && StringUtils.equals(state, "FAILED_OVER")
                && StringUtils.equals(rollbackState, "COMPLETED")
                && StringUtils.equals(cloudLifecycleState, "ABORTED")
                && StringUtils.equals(commitOutcome, "ROLLED_BACK")
                && StringUtils.equals(activeSide, "TARGET")
                && StringUtils.equals(sourcePowerState, "POWERED_OFF")
                && StringUtils.equals(targetPowerState, "POWERED_ON")
                && StringUtils.isBlank(errorCode);
    }

    static boolean isCompletedFailoverAbort(FtctlDrActionCommand command, JsonObject payload) {
        if (command == null || payload == null
                || command.getAction() != FtctlDrActionCommand.Action.FAILOVER_ABORT) {
            return false;
        }
        String result = StringUtils.lowerCase(LibvirtFtctlDrCommandHelper.getString(payload, "result"));
        String state = StringUtils.upperCase(LibvirtFtctlDrCommandHelper.getString(payload, "state"));
        String step = LibvirtFtctlDrCommandHelper.getString(payload, "step");
        String activeSide = StringUtils.upperCase(LibvirtFtctlDrCommandHelper.getString(payload, "active_side"));
        String targetPowerState = StringUtils.upperCase(
                LibvirtFtctlDrCommandHelper.getString(payload, "target_power_state"));
        String errorCode = LibvirtFtctlDrCommandHelper.getString(payload, "error_code");
        return StringUtils.equalsAny(result, "ok", "success")
                && StringUtils.equals(state, "ABORTED")
                && StringUtils.equals(step, "failover-preparation-aborted")
                && StringUtils.equals(activeSide, "SOURCE")
                && StringUtils.equals(targetPowerState, "POWERED_OFF")
                && StringUtils.isBlank(errorCode);
    }

    private String resolveRestorePointSelector(FtctlDrActionCommand command) {
        if (StringUtils.isNotBlank(command.getCheckpointRef())) {
            return command.getCheckpointRef();
        }
        JsonObject request = LibvirtFtctlWrapperHelper.parseJsonObject(command.getRequestJson());
        String restorePointRef = LibvirtFtctlWrapperHelper.getString(request, "restorePointRef");
        if (StringUtils.isNotBlank(restorePointRef)) {
            return restorePointRef;
        }
        if (command.getContext() != null) {
            restorePointRef = command.getContext().get("restorePointRef");
            if (StringUtils.isNotBlank(restorePointRef)) {
                return restorePointRef;
            }
        }
        return null;
    }

    private static final class ActionDescriptor {
        private final String cliCommand;

        private ActionDescriptor(String cliCommand) {
            this.cliCommand = cliCommand;
        }

        private String getCliCommand() {
            return cliCommand;
        }
    }
}
