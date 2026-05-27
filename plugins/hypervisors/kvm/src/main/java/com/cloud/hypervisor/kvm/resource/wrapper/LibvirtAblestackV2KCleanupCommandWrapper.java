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

import com.cloud.agent.api.AblestackV2KCleanupCommand;
import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

@ResourceWrapper(handles = AblestackV2KCleanupCommand.class)
public class LibvirtAblestackV2KCleanupCommandWrapper extends CommandWrapper<AblestackV2KCleanupCommand, Answer, LibvirtComputingResource> {

    private static final String ABLESTACK_V2K_WORKDIR_BASE = "/var/lib/ablestack-v2k";

    @Override
    public Answer execute(AblestackV2KCleanupCommand cmd, LibvirtComputingResource serverResource) {
        if (StringUtils.isAllBlank(cmd.getWorkdir(), cmd.getDomainName())) {
            return new Answer(cmd, false, "Missing workdir or domain name for ablestack-v2k cleanup command");
        }

        StringBuilder details = new StringBuilder();
        boolean success = true;
        if (StringUtils.isNotBlank(cmd.getWorkdir())) {
            Answer cleanupAnswer = runV2KCleanup(cmd);
            details.append(cleanupAnswer.getDetails());
            success &= cleanupAnswer.getResult();
        }
        if (cmd.isUndefineDomain() && StringUtils.isNotBlank(cmd.getDomainName())) {
            Answer undefineAnswer = undefineDomain(cmd);
            if (details.length() > 0) {
                details.append("; ");
            }
            details.append(undefineAnswer.getDetails());
            success &= undefineAnswer.getResult();
        }
        if (cmd.isRemoveWorkdir()) {
            String cleanupPath = StringUtils.defaultIfBlank(cmd.getWorkdir(), buildLegacyVmWorkdir(cmd.getDomainName()));
            try {
                String pathMessage = removeV2KWorkdir(cleanupPath);
                if (details.length() > 0) {
                    details.append("; ");
                }
                details.append(pathMessage);
            } catch (CloudRuntimeException e) {
                if (details.length() > 0) {
                    details.append("; ");
                }
                details.append(e.getMessage());
                success = false;
            }
        }
        return new Answer(cmd, success, StringUtils.defaultIfBlank(details.toString(), "ablestack_v2k cleanup completed"));
    }

    private Answer runV2KCleanup(AblestackV2KCleanupCommand cmd) {
        final long timeout = (long) Math.max(cmd.getWait(), 60) * 1000;
        Script script = new Script("ablestack_v2k", timeout, logger);
        script.add("--workdir", cmd.getWorkdir());
        script.add("--force");
        script.add("cleanup");
        if (cmd.isKeepSourceSnapshots()) {
            script.add("--keep-snapshots");
        }
        if (!cmd.isRemoveWorkdir()) {
            script.add("--keep-workdir");
        }

        String result = script.execute();
        int exitValue = script.getExitValue();
        boolean cleanupOk = exitValue == 0
                || StringUtils.containsIgnoreCase(result, "not found")
                || StringUtils.containsIgnoreCase(result, "does not exist");
        return new Answer(cmd, cleanupOk, cleanupOk
                ? StringUtils.defaultIfBlank(result, "ablestack_v2k cleanup completed")
                : StringUtils.defaultIfBlank(result, String.format("ablestack_v2k cleanup failed with exit code %d", exitValue)));
    }

    private Answer undefineDomain(AblestackV2KCleanupCommand cmd) {
        final long timeout = (long) Math.max(cmd.getWait(), 30) * 1000;
        Script script = new Script("virsh", timeout, logger);
        script.add("undefine");
        script.add("--nvram");
        script.add("--domain", cmd.getDomainName());

        String result = script.execute();
        int exitValue = script.getExitValue();
        String details = StringUtils.defaultIfBlank(result, String.format("Failed to undefine domain %s (exit=%d)", cmd.getDomainName(), exitValue));
        boolean undefined = exitValue == 0 || StringUtils.containsIgnoreCase(details, "domain not found");
        return new Answer(cmd, undefined, exitValue == 0
                ? String.format("Undefined domain %s", cmd.getDomainName())
                : String.format("Domain %s is already undefined", cmd.getDomainName()));
    }

    private String buildLegacyVmWorkdir(String domainName) {
        return StringUtils.isBlank(domainName) ? null : ABLESTACK_V2K_WORKDIR_BASE + File.separator + domainName;
    }

    private String removeV2KWorkdir(String workdir) {
        if (StringUtils.isBlank(workdir)) {
            return "No v2k workdir to remove";
        }
        File baseDir = new File(ABLESTACK_V2K_WORKDIR_BASE);
        File target = new File(workdir);
        try {
            String baseCanonicalPath = baseDir.getCanonicalPath();
            String targetCanonicalPath = target.getCanonicalPath();
            if (!StringUtils.startsWith(targetCanonicalPath, baseCanonicalPath + File.separator)) {
                throw new CloudRuntimeException(String.format("Invalid ablestack-v2k workdir path: %s", targetCanonicalPath));
            }
            if (!target.exists()) {
                return String.format("Workdir %s does not exist", targetCanonicalPath);
            }
            FileUtils.deleteDirectory(target);
            return String.format("Removed workdir %s", targetCanonicalPath);
        } catch (IOException e) {
            throw new CloudRuntimeException(String.format("Failed to remove ablestack-v2k workdir %s", workdir), e);
        }
    }
}
