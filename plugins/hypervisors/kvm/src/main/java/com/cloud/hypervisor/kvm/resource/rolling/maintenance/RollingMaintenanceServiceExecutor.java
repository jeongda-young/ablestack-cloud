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
package com.cloud.hypervisor.kvm.resource.rolling.maintenance;

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.stream.Stream;

public class RollingMaintenanceServiceExecutor extends RollingMaintenanceExecutorBase implements RollingMaintenanceExecutor {

    private static final String servicePrefix = "cloudstack-rolling-maintenance";
    private static final String resultsFileSuffix = "rolling-maintenance-results";
    private static final String outputFileSuffix = "rolling-maintenance-output";
    private static final Path runtimeDirectory = Paths.get("/run/cloudstack/rolling-maintenance");


    public RollingMaintenanceServiceExecutor(String hooksDir) {
        super(hooksDir);
    }

    private String generateInstanceName(String stage, String file, String payload) {
        String input = String.format("%s\n%s\n%s\n%s\n%s\n%s", stage, file, getTimeout(),
                getResultsFilePath(), getOutputFilePath(), StringUtils.defaultString(payload));
        return stage + "-" + sha256(input).substring(0, 16);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CloudRuntimeException("SHA-256 digest is not available", e);
        }
    }

    private String base64(String value) {
        return Base64.getEncoder().encodeToString(StringUtils.defaultString(value).getBytes(StandardCharsets.UTF_8));
    }

    private Path getMetadataFilePath(String instanceName) {
        return runtimeDirectory.resolve(instanceName + ".metadata");
    }

    private void prepareServiceMetadata(String instanceName, String stage, String file, String payload) {
        try {
            Files.createDirectories(runtimeDirectory);
            Files.deleteIfExists(Paths.get(getResultsFilePath()));
            Files.deleteIfExists(Paths.get(getOutputFilePath()));

            String metadata = String.format("stage=%s%nscript=%s%ntimeout=%s%nresults=%s%noutput=%s%npayload=%s%n",
                    base64(stage), base64(file), getTimeout(), base64(getResultsFilePath()),
                    base64(getOutputFilePath()), base64(payload));
            Files.write(getMetadataFilePath(instanceName), metadata.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to prepare rolling maintenance service metadata", e);
        }
    }

    private String invokeService(String action, String instanceName) {
        logger.debug("Invoking rolling maintenance service instance " + instanceName + " with action: " + action);
        final OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String service = servicePrefix + "@" + instanceName;
        Script command = new Script("/bin/bash", logger);
        command.add("-c");
        command.add(String.format("/bin/systemctl %s %s 2>&1 || true", action, service));
        String result = command.execute(parser);
        int exitValue = command.getExitValue();
        logger.trace("Execution: " + command.toString() + " - exit code: " + exitValue +
                ": " + result + (StringUtils.isNotBlank(parser.getLines()) ? parser.getLines() : ""));
        return StringUtils.isBlank(result) ? parser.getLines().replace("\n", " ") : result;
    }

    @Override
    public Pair<Boolean, String> startStageExecution(String stage, File scriptFile, int timeout, String payload) {
        checkHooksDirectory();
        setTimeout(timeout);
        String instanceName = generateInstanceName(stage, scriptFile.getAbsolutePath(), payload);
        prepareServiceMetadata(instanceName, stage, scriptFile.getAbsolutePath(), payload);
        String result = invokeService("start", instanceName);
        if (StringUtils.isNotBlank(result)) {
            throw new CloudRuntimeException("Error starting stage: " + stage + " execution: " + result);
        }
        logger.trace("Stage " + stage + "execution started");
        return new Pair<>(true, "OK");
    }

    private String getResultsFilePath() {
        return getHooksDir() + resultsFileSuffix;
    }

    private String getOutputFilePath() {
        return getHooksDir() + outputFileSuffix;
    }

    private String readFromFile(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines( Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }

    @Override
    public String getStageExecutionOutput(String stage, File scriptFile) {
        return readFromFile(getOutputFilePath());
    }

    @Override
    public boolean isStageRunning(String stage, File scriptFile, String payload) {
        String instanceName = generateInstanceName(stage, scriptFile.getAbsolutePath(), payload);
        String result = invokeService("is-active", instanceName);
        if (StringUtils.isNotBlank(result) && result.equals("failed")) {
            String status = invokeService("status", instanceName);
            String errorMsg = "Stage " + stage + " execution failed, status: " + status;
            logger.error(errorMsg);
            throw new CloudRuntimeException(errorMsg);
        }
        return StringUtils.isNotBlank(result) && result.equals("active");
    }

    @Override
    public boolean getStageExecutionSuccess(String stage, File scriptFile) {
        String fileContent = readResultFileWithRetry();
        if (StringUtils.isBlank(fileContent)) {
            throw new CloudRuntimeException("Empty content in file " + getResultsFilePath());
        }
        fileContent = fileContent.replace("\n", "");
        String[] parts = fileContent.split(",");
        if (parts.length < 3) {
            throw new CloudRuntimeException("Results file " + getResultsFilePath() + " unexpected content: " + fileContent);
        }
        if (!parts[0].equalsIgnoreCase(stage)) {
            throw new CloudRuntimeException("Expected stage " + stage + " results but got stage " + parts[0]);
        }
        setAvoidMaintenance(Boolean.parseBoolean(parts[2]));
        return Boolean.parseBoolean(parts[1]);
    }

    private String readResultFileWithRetry() {
        String fileContent = StringUtils.EMPTY;
        for (int i = 0; i < 10; i++) {
            fileContent = readFromFile(getResultsFilePath());
            if (StringUtils.isNotBlank(fileContent)) {
                return fileContent;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return fileContent;
            }
        }
        return fileContent;
    }
}
