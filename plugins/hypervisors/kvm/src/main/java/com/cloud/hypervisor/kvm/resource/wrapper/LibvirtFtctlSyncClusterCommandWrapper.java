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
import com.cloud.agent.api.FtctlSyncAnswer;
import com.cloud.agent.api.FtctlSyncClusterCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.commons.lang3.StringUtils;

@ResourceWrapper(handles = FtctlSyncClusterCommand.class)
public class LibvirtFtctlSyncClusterCommandWrapper extends CommandWrapper<FtctlSyncClusterCommand, Answer, LibvirtComputingResource> {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    @Override
    public Answer execute(FtctlSyncClusterCommand command, LibvirtComputingResource serverResource) {
        if (StringUtils.isBlank(command.getClusterName()) || StringUtils.isBlank(command.getLocalHostId())) {
            return new FtctlSyncAnswer(command, false, "Missing FTCTL cluster sync parameters");
        }

        final long timeout = (long) (command.getWait() > 0 ? command.getWait() : DEFAULT_TIMEOUT_SECONDS) * 1000;
        String output = "";

        FtctlSyncAnswer localSync = executeHostUpsert(timeout, command,
                command.getClusterName(), command.getLocalHostId(), command.getLocalRole(),
                command.getLocalManagementIp(), command.getLocalLibvirtUri(), command.getLocalBlockcopyIp(),
                command.getLocalXcoloControlIp(), command.getLocalXcoloDataIp(), true);
        if (!localSync.getResult()) {
            return localSync;
        }
        output += localSync.getOutput() != null ? localSync.getOutput() : "";

        if (StringUtils.isNotBlank(command.getPeerHostId())) {
            FtctlSyncAnswer peerSync = executeHostUpsert(timeout, command,
                    command.getClusterName(), command.getPeerHostId(), command.getPeerRole(),
                    command.getPeerManagementIp(), command.getPeerLibvirtUri(), command.getPeerBlockcopyIp(),
                    command.getPeerXcoloControlIp(), command.getPeerXcoloDataIp(), false);
            if (!peerSync.getResult()) {
                return peerSync;
            }
            output += peerSync.getOutput() != null ? System.lineSeparator() + peerSync.getOutput() : "";
        }

        return new FtctlSyncAnswer(command, true, "OK", "ok", 0, output);
    }

    private FtctlSyncAnswer executeHostUpsert(long timeout, FtctlSyncClusterCommand origin,
                                              String clusterName, String hostId, String role,
                                              String managementIp, String libvirtUri, String blockcopyIp,
                                              String xcoloControlIp, String xcoloDataIp, boolean initializeCluster) {
        if (StringUtils.isBlank(hostId) || StringUtils.isBlank(managementIp) || StringUtils.isBlank(libvirtUri)) {
            return new FtctlSyncAnswer(origin, false, "Missing FTCTL host inventory parameters");
        }

        if (initializeCluster) {
            Script initScript = new Script("ablestack_vm_ftctl", timeout, logger);
            initScript.add("config");
            initScript.add("init-cluster");
            initScript.add("--cluster-name", clusterName);
            initScript.add("--local-host-id", hostId);
            initScript.add("--json");
            OutputInterpreter.AllLinesParser initParser = new OutputInterpreter.AllLinesParser();
            String initResult = initScript.execute(initParser);
            if (initScript.getExitValue() != 0) {
                String output = LibvirtFtctlWrapperHelper.getOutput(initResult, initParser);
                return new FtctlSyncAnswer(origin, false, StringUtils.defaultIfBlank(output, "FTCTL init-cluster failed"),
                        "fail", initScript.getExitValue(), output);
            }
        }

        Script upsertScript = new Script("ablestack_vm_ftctl", timeout, logger);
        upsertScript.add("config");
        upsertScript.add("host-upsert");
        upsertScript.add("--host-id", hostId);
        upsertScript.add("--role", StringUtils.defaultIfBlank(role, "generic"));
        upsertScript.add("--management-ip", managementIp);
        upsertScript.add("--libvirt-uri", libvirtUri);
        upsertScript.add("--blockcopy-ip", StringUtils.defaultIfBlank(blockcopyIp, managementIp));
        upsertScript.add("--xcolo-control-ip", StringUtils.defaultIfBlank(xcoloControlIp, managementIp));
        upsertScript.add("--xcolo-data-ip", StringUtils.defaultIfBlank(xcoloDataIp, managementIp));
        upsertScript.add("--json");
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = upsertScript.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        int exitValue = upsertScript.getExitValue();
        return new FtctlSyncAnswer(origin, exitValue == 0,
                StringUtils.defaultIfBlank(output, exitValue == 0 ? "OK" : "FTCTL host-upsert failed"),
                exitValue == 0 ? "ok" : "fail", exitValue, output);
    }
}
