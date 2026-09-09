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
import com.cloud.agent.api.FtctlRemotePreflightCommand;
import com.cloud.agent.api.FtctlSyncAnswer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.commons.lang3.StringUtils;

@ResourceWrapper(handles = FtctlRemotePreflightCommand.class)
public class LibvirtFtctlRemotePreflightCommandWrapper extends CommandWrapper<FtctlRemotePreflightCommand, Answer, LibvirtComputingResource> {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    @Override
    public Answer execute(FtctlRemotePreflightCommand command, LibvirtComputingResource serverResource) {
        if (StringUtils.isAnyBlank(command.getVmName(), command.getMode(), command.getPeerUri())) {
            return new FtctlSyncAnswer(command, false, "Missing FTCTL remote preflight parameters");
        }

        final long timeout = (long) (command.getWait() > 0 ? command.getWait() : DEFAULT_TIMEOUT_SECONDS) * 1000;
        Script script = new Script("ablestack_vm_ftctl", timeout, logger);
        script.add("preflight-remote");
        script.add("--vm", command.getVmName());
        script.add("--mode", command.getMode());
        script.add("--peer", command.getPeerUri());
        if (StringUtils.isNotBlank(command.getSecondaryTargetDir())) {
            script.add("--secondary-target-dir", command.getSecondaryTargetDir());
        }
        if (StringUtils.isNotBlank(command.getSecondarySshKeyFile())) {
            script.add("--secondary-ssh-key-file", command.getSecondarySshKeyFile());
        }
        if (StringUtils.isNotBlank(command.getRemoteNbdExportAddr())) {
            script.add("--remote-nbd-export-addr", command.getRemoteNbdExportAddr());
        }
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        int exitValue = script.getExitValue();

        return new FtctlSyncAnswer(command, exitValue == 0,
                StringUtils.defaultIfBlank(output, exitValue == 0 ? "OK" : "FTCTL remote preflight failed"),
                exitValue == 0 ? "ok" : "fail", exitValue, output);
    }
}
