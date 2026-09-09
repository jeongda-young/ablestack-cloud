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

import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FtctlDrCancelAnswer;
import com.cloud.agent.api.FtctlDrCancelCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonObject;

@ResourceWrapper(handles = FtctlDrCancelCommand.class)
public class LibvirtFtctlDrCancelCommandWrapper extends CommandWrapper<FtctlDrCancelCommand, Answer, LibvirtComputingResource> {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    @Override
    public Answer execute(FtctlDrCancelCommand command, LibvirtComputingResource serverResource) {
        if (StringUtils.isBlank(command.getPlanUuid())) {
            return new FtctlDrCancelAnswer(command, false, "Missing DR plan UUID");
        }
        if (StringUtils.isBlank(command.getRunUuid())) {
            return new FtctlDrCancelAnswer(command, false, "Missing DR run UUID");
        }

        final long timeout = (long) (command.getWait() > 0 ? command.getWait() : DEFAULT_TIMEOUT_SECONDS) * 1000;
        Script script = new Script("ablestack_vm_ftctl", timeout, logger);
        script.add("dr-cancel");
        LibvirtFtctlDrCommandHelper.addPlanRunArgs(script, command.getPlanUuid(), command.getRunUuid());
        if (command.isForce()) {
            script.add("--force");
        }
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        JsonObject payload = LibvirtFtctlWrapperHelper.parseJsonObject(output);
        int exitValue = script.getExitValue();
        boolean success = exitValue == 0;

        return new FtctlDrCancelAnswer(command, success,
                StringUtils.defaultIfBlank(output, success ? "OK" : "FTCTL_DR cancel failed"),
                command.getPlanUuid(), command.getRunUuid(),
                LibvirtFtctlDrCommandHelper.getString(payload, "result"),
                LibvirtFtctlDrCommandHelper.getBoolean(payload, "accepted"),
                LibvirtFtctlDrCommandHelper.getString(payload, "error_code"),
                exitValue, output);
    }
}
