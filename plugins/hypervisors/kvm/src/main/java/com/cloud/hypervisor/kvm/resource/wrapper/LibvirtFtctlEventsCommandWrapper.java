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
import com.cloud.agent.api.FtctlEventsAnswer;
import com.cloud.agent.api.FtctlEventsCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

@ResourceWrapper(handles = FtctlEventsCommand.class)
public class LibvirtFtctlEventsCommandWrapper extends CommandWrapper<FtctlEventsCommand, Answer, LibvirtComputingResource> {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    @Override
    public Answer execute(FtctlEventsCommand command, LibvirtComputingResource serverResource) {
        final long timeout = (long) (command.getWait() > 0 ? command.getWait() : DEFAULT_TIMEOUT_SECONDS) * 1000;
        Script script = new Script("ablestack_vm_ftctl", timeout, logger);
        script.add("events");
        if (StringUtils.isNotBlank(command.getVmName())) {
            script.add("--vm", command.getVmName());
        }
        if (command.getLimit() != null && command.getLimit() > 0) {
            script.add("--limit", String.valueOf(command.getLimit()));
        }
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        JsonObject payload = LibvirtFtctlWrapperHelper.parseJsonObject(output);
        int exitValue = script.getExitValue();

        if (payload == null) {
            return new FtctlEventsAnswer(command, false,
                    StringUtils.defaultIfBlank(output, "Unable to parse ftctl events output"));
        }

        JsonArray items = payload.has("items") && payload.get("items").isJsonArray() ? payload.getAsJsonArray("items") : new JsonArray();
        return new FtctlEventsAnswer(command, exitValue == 0,
                StringUtils.defaultIfBlank(output, exitValue == 0 ? "OK" : "ftctl events failed"),
                LibvirtFtctlWrapperHelper.getString(payload, "result"),
                LibvirtFtctlWrapperHelper.getString(payload, "vm"),
                LibvirtFtctlWrapperHelper.getInteger(payload, "count"),
                items.toString());
    }
}
