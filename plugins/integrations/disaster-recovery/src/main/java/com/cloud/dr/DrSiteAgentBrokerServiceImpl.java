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
package com.cloud.dr;

import javax.inject.Inject;

import org.apache.cloudstack.api.response.dr.DrSiteAgentCommandResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlDrSiteAgentCommandResponse;

import com.cloud.ftctl.FtctlDrSiteAgentBrokerService;
import com.cloud.utils.component.ManagerBase;

/**
 * Backward-compatible DR API facade for the canonical FTCTL site broker.
 * Worker discovery and dispatch policy must remain owned by the FTCTL broker
 * so the two public API names cannot drift into different placement contracts.
 */
public class DrSiteAgentBrokerServiceImpl extends ManagerBase implements DrSiteAgentBrokerService {

    @Inject private FtctlDrSiteAgentBrokerService ftctlDrSiteAgentBrokerService;

    @Override
    public DrSiteAgentCommandResponse execute(String commandType, String commandJson, String workerHostUuid) {
        FtctlDrSiteAgentCommandResponse source = ftctlDrSiteAgentBrokerService.execute(
                commandType, commandJson, workerHostUuid);
        DrSiteAgentCommandResponse response = new DrSiteAgentCommandResponse();
        response.setObjectName("drsiteagentcommand");
        response.setCommandType(source.getCommandType());
        response.setWorkerHostUuid(source.getWorkerHostUuid());
        response.setResult(source.getResult());
        response.setDetails(source.getDetails());
        response.setAnswerClass(source.getAnswerClass());
        response.setAnswerJson(source.getAnswerJson());
        return response;
    }
}
