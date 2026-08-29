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
package org.apache.cloudstack.storage.dataservice;

import javax.inject.Inject;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.StorageServiceRuntimeHostAnswer;
import com.cloud.agent.api.StorageServiceRuntimeHostCommand;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

public class StorageServiceRuntimeHostDispatcherImpl implements StorageServiceRuntimeHostDispatcher {
    @Inject private AgentManager agentManager;
    @Inject private VMInstanceDao vmInstanceDao;

    public StorageServiceRuntimeHostAnswer dispatch(final long vmId, final StorageServiceRuntimeHostCommand command) {
        final VMInstanceVO vm = vmInstanceDao.findById(vmId);
        if (vm == null) {
            throw new CloudRuntimeException("Unable to find Storage Service System VM with id " + vmId);
        }
        if (vm.getHostId() == null) {
            final String vmName = vm.getHostName() == null || vm.getHostName().trim().isEmpty()
                    ? vm.getInstanceName() : vm.getHostName();
            throw new CloudRuntimeException("Storage Service System VM is not running on a host: " + vmName);
        }
        final Answer answer = agentManager.easySend(vm.getHostId(), command);
        if (!(answer instanceof StorageServiceRuntimeHostAnswer)) {
            throw new CloudRuntimeException(answer == null ? "No response from Storage Service runtime host command" :
                    "Unexpected Storage Service runtime host answer: " + answer.getClass().getName());
        }
        return (StorageServiceRuntimeHostAnswer) answer;
    }
}
