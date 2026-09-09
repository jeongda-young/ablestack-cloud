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
import com.cloud.agent.api.FtctlCheckAnswer;
import com.cloud.agent.api.FtctlCheckCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

@ResourceWrapper(handles = FtctlCheckCommand.class)
public class LibvirtFtctlCheckCommandWrapper extends CommandWrapper<FtctlCheckCommand, Answer, LibvirtComputingResource> {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    @Override
    public Answer execute(FtctlCheckCommand command, LibvirtComputingResource serverResource) {
        if (StringUtils.isBlank(command.getVmName())) {
            return new FtctlCheckAnswer(command, false, "Missing VM name for ftctl check command");
        }

        final long timeout = (long) (command.getWait() > 0 ? command.getWait() : DEFAULT_TIMEOUT_SECONDS) * 1000;
        Script script = new Script("ablestack_vm_ftctl", timeout, logger);
        script.add("check");
        script.add("--vm", command.getVmName());
        if (StringUtils.isNotBlank(command.getSecondaryVmName())) {
            script.add("--secondary-vm-name", command.getSecondaryVmName());
        }
        if (StringUtils.isNotBlank(command.getActiveSide())) {
            script.add("--active-side", command.getActiveSide());
        }
        if (StringUtils.isNotBlank(command.getProvisioningBackend())) {
            script.add("--provisioning-backend", command.getProvisioningBackend());
        }
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        JsonObject payload = LibvirtFtctlWrapperHelper.parseJsonObject(output);
        int exitValue = script.getExitValue();

        if (payload == null) {
            return new FtctlCheckAnswer(command, false,
                    StringUtils.defaultIfBlank(output, String.format("Unable to parse ftctl check output for VM %s", command.getVmName())));
        }

        boolean success = exitValue == 0;
        return new FtctlCheckAnswer(command, success, StringUtils.defaultIfBlank(output, success ? "OK" : "ftctl check failed"),
                LibvirtFtctlWrapperHelper.getString(payload, "result"),
                LibvirtFtctlWrapperHelper.getString(payload, "inventory_result"),
                LibvirtFtctlWrapperHelper.getString(payload, "vm"),
                LibvirtFtctlWrapperHelper.getInteger(payload, "primary_rc"),
                LibvirtFtctlWrapperHelper.getInteger(payload, "peer_rc"),
                LibvirtFtctlWrapperHelper.getBoolean(payload, "peer_domain_expected"),
                LibvirtFtctlWrapperHelper.getString(payload, "standby_domain_state"),
                LibvirtFtctlWrapperHelper.getString(payload, "provisioning_backend"));
    }
}
