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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FtctlDrSshAccessCommand;
import com.cloud.agent.api.FtctlSyncAnswer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.commons.lang3.StringUtils;

@ResourceWrapper(handles = FtctlDrSshAccessCommand.class)
public class LibvirtFtctlDrSshAccessCommandWrapper extends CommandWrapper<FtctlDrSshAccessCommand, Answer, LibvirtComputingResource> {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    @Override
    public Answer execute(FtctlDrSshAccessCommand command, LibvirtComputingResource serverResource) {
        if (command.getAction() == null || StringUtils.isBlank(command.getProfile())) {
            return new FtctlSyncAnswer(command, false, "Missing FTCTL DR SSH access parameters");
        }

        final long timeout = (long) (command.getWait() > 0 ? command.getWait() : DEFAULT_TIMEOUT_SECONDS) * 1000;
        if (command.isApplyFirewall()) {
            Answer firewallAnswer = executeFirewalldApply(command, timeout);
            if (!firewallAnswer.getResult()) {
                return firewallAnswer;
            }
        }

        Script script = new Script("ablestack_vm_ftctl", timeout, logger);
        switch (command.getAction()) {
            case ENSURE_KEY:
                script.add("dr-key-ensure");
                break;
            case INSTALL_KEY:
                if (StringUtils.isBlank(command.getPublicKey())) {
                    return new FtctlSyncAnswer(command, false, "Missing FTCTL DR SSH public key");
                }
                script.add("dr-key-install");
                script.add("--public-key", command.getPublicKey());
                break;
            case REMOVE_KEY:
                script.add("dr-key-remove");
                break;
            default:
                return new FtctlSyncAnswer(command, false, "Unsupported FTCTL DR SSH access action");
        }
        script.add("--profile", command.getProfile());
        if (StringUtils.isNotBlank(command.getKeyComment())) {
            script.add("--key-comment", command.getKeyComment());
        }
        if (StringUtils.isNotBlank(command.getSshUser())) {
            script.add("--ssh-user", command.getSshUser());
        }
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        int exitValue = script.getExitValue();
        return new FtctlSyncAnswer(command, exitValue == 0,
                StringUtils.defaultIfBlank(output, exitValue == 0 ? "OK" : "FTCTL DR SSH access command failed"),
                exitValue == 0 ? "ok" : "fail", exitValue, output);
    }

    private Answer executeFirewalldApply(FtctlDrSshAccessCommand command, long timeout) {
        Script script = new Script("ablestack_vm_ftctl_firewalld", timeout, logger);
        script.add("apply");
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        int exitValue = script.getExitValue();
        return new FtctlSyncAnswer(command, exitValue == 0,
                StringUtils.defaultIfBlank(output, exitValue == 0 ? "OK" : "FTCTL firewalld apply failed"),
                exitValue == 0 ? "ok" : "fail", exitValue, output);
    }
}
