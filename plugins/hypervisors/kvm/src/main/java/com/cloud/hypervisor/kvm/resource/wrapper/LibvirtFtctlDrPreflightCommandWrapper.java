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

import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FtctlDrPreflightAnswer;
import com.cloud.agent.api.FtctlDrPreflightCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonObject;

@ResourceWrapper(handles = FtctlDrPreflightCommand.class)
public class LibvirtFtctlDrPreflightCommandWrapper extends CommandWrapper<FtctlDrPreflightCommand, Answer, LibvirtComputingResource> {

    private static final int DEFAULT_TIMEOUT_SECONDS = 45;

    @Override
    public Answer execute(FtctlDrPreflightCommand command, LibvirtComputingResource serverResource) {
        if (StringUtils.isBlank(command.getPlanUuid())) {
            return new FtctlDrPreflightAnswer(command, false, "Missing DR plan UUID");
        }

        File profileFile = null;
        try {
            profileFile = LibvirtFtctlDrCommandHelper.writeProfileJson(command.getPlanUuid(), command.getProfileJson());
            return executeFtctl(command, profileFile);
        } catch (IOException e) {
            return new FtctlDrPreflightAnswer(command, false,
                    "Unable to write temporary FTCTL_DR preflight profile: " + e.getMessage());
        } finally {
            LibvirtFtctlDrCommandHelper.deleteQuietly(profileFile);
        }
    }

    private FtctlDrPreflightAnswer executeFtctl(FtctlDrPreflightCommand command, File profileFile) {
        final long timeout = (long) (command.getWait() > 0 ? command.getWait() : DEFAULT_TIMEOUT_SECONDS) * 1000;
        Script script = new Script("ablestack_vm_ftctl", timeout, logger);
        script.add("dr-plan-apply");
        LibvirtFtctlDrCommandHelper.addPlanRunArgs(script, command.getPlanUuid(), null);
        LibvirtFtctlDrCommandHelper.addProfileJsonArg(script, profileFile);
        if (StringUtils.isNotBlank(command.getRole())) {
            script.add("--role");
            script.add(command.getRole());
        }
        script.add("--dry-run");
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        JsonObject payload = LibvirtFtctlWrapperHelper.parseJsonObject(output);
        int exitValue = script.getExitValue();
        boolean success = exitValue == 0;

        return new FtctlDrPreflightAnswer(command, success,
                StringUtils.defaultIfBlank(output, success ? "OK" : "FTCTL_DR preflight failed"),
                command.getPlanUuid(),
                LibvirtFtctlDrCommandHelper.getString(payload, "result"),
                LibvirtFtctlDrCommandHelper.getBoolean(payload, "capable"),
                LibvirtFtctlDrCommandHelper.getString(payload, "error_code"),
                exitValue, output, payload != null ? payload.toString() : null);
    }
}
