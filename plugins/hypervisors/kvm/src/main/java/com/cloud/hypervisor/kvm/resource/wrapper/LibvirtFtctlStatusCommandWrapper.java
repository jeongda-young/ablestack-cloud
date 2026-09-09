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
import com.cloud.agent.api.FtctlStatusAnswer;
import com.cloud.agent.api.FtctlStatusCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

@ResourceWrapper(handles = FtctlStatusCommand.class)
public class LibvirtFtctlStatusCommandWrapper extends CommandWrapper<FtctlStatusCommand, Answer, LibvirtComputingResource> {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    @Override
    public Answer execute(FtctlStatusCommand command, LibvirtComputingResource serverResource) {
        if (StringUtils.isBlank(command.getVmName())) {
            return new FtctlStatusAnswer(command, false, "Missing VM name for ftctl status command");
        }

        final long timeout = (long) (command.getWait() > 0 ? command.getWait() : DEFAULT_TIMEOUT_SECONDS) * 1000;
        Script script = new Script("ablestack_vm_ftctl", timeout, logger);
        script.add("status");
        script.add("--vm", command.getVmName());
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        JsonObject payload = LibvirtFtctlWrapperHelper.parseJsonObject(output);
        int exitValue = script.getExitValue();

        if (payload == null) {
            return new FtctlStatusAnswer(command, false,
                    StringUtils.defaultIfBlank(output, String.format("Unable to parse ftctl status output for VM %s", command.getVmName())));
        }

        boolean success = exitValue == 0;
        JsonObject syncProgress = LibvirtFtctlWrapperHelper.getObject(payload, "sync_progress");
        return new FtctlStatusAnswer(command, success, StringUtils.defaultIfBlank(output, success ? "OK" : "ftctl status failed"),
                LibvirtFtctlWrapperHelper.getString(payload, "result"),
                LibvirtFtctlWrapperHelper.getString(payload, "vm"),
                LibvirtFtctlWrapperHelper.getString(payload, "mode"),
                LibvirtFtctlWrapperHelper.getString(payload, "protection_state"),
                LibvirtFtctlWrapperHelper.getString(payload, "transport_state"),
                LibvirtFtctlWrapperHelper.getString(payload, "active_side"),
                LibvirtFtctlWrapperHelper.getString(payload, "admin_state"),
                LibvirtFtctlWrapperHelper.getString(payload, "fencing_state"),
                LibvirtFtctlWrapperHelper.getString(payload, "last_error"),
                LibvirtFtctlWrapperHelper.getString(payload, "last_reconcile_ts"),
                LibvirtFtctlWrapperHelper.getInteger(payload, "rearm_count"),
                LibvirtFtctlWrapperHelper.getInteger(payload, "failover_count"),
                LibvirtFtctlWrapperHelper.getDouble(syncProgress, "percent"),
                LibvirtFtctlWrapperHelper.getLong(syncProgress, "copied_bytes"),
                LibvirtFtctlWrapperHelper.getLong(syncProgress, "total_bytes"),
                LibvirtFtctlWrapperHelper.getBoolean(syncProgress, "ready"),
                LibvirtFtctlWrapperHelper.getString(syncProgress, "direction"),
                LibvirtFtctlWrapperHelper.getString(syncProgress, "updated"),
                syncProgress != null ? syncProgress.toString() : null);
    }
}
