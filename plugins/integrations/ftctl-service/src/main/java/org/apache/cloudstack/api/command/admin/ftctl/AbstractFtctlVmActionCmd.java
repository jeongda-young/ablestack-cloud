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
package org.apache.cloudstack.api.command.admin.ftctl;

import com.cloud.agent.api.FtctlActionCommand;
import com.cloud.event.EventTypes;
import com.cloud.ftctl.FtctlService;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlActionResponse;

import javax.inject.Inject;

public abstract class AbstractFtctlVmActionCmd extends BaseAsyncCmd {

    @Inject
    protected FtctlService ftctlService;

    @Parameter(name = "virtualmachineid", type = CommandType.UUID, entityType = UserVmResponse.class,
            required = true, description = "the virtual machine ID")
    private Long virtualMachineId;

    protected abstract FtctlActionCommand.Action getAction();

    protected boolean useForce() {
        return false;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    @Override
    public void execute() throws ServerApiException {
        FtctlActionResponse response = ftctlService.executeFtctlAction(virtualMachineId, getAction(), useForce());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = _entityMgr.findById(UserVm.class, getVirtualMachineId());
        return vm != null ? vm.getAccountId() : Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        FtctlActionCommand.Action action = getAction();
        if (action == FtctlActionCommand.Action.PAUSE_PROTECTION) {
            return EventTypes.EVENT_FTCTL_PROTECTION_PAUSE;
        }
        if (action == FtctlActionCommand.Action.RESUME_PROTECTION) {
            return EventTypes.EVENT_FTCTL_PROTECTION_RESUME;
        }
        if (action == FtctlActionCommand.Action.FAILOVER) {
            return EventTypes.EVENT_FTCTL_PROTECTION_FAILOVER;
        }
        if (action == FtctlActionCommand.Action.FAILBACK) {
            return EventTypes.EVENT_FTCTL_PROTECTION_FAILBACK;
        }
        if (action == FtctlActionCommand.Action.UNPROTECT) {
            return EventTypes.EVENT_FTCTL_PROTECTION_RELEASE;
        }
        if (action == FtctlActionCommand.Action.FENCE_CONFIRM) {
            return EventTypes.EVENT_FTCTL_PROTECTION_FENCE_CONFIRM;
        }
        if (action == FtctlActionCommand.Action.FENCE_CLEAR) {
            return EventTypes.EVENT_FTCTL_PROTECTION_FENCE_CLEAR;
        }
        return EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE;
    }

    @Override
    public String getEventDescription() {
        UserVm vm = _entityMgr.findById(UserVm.class, getVirtualMachineId());
        String identifier = vm != null ? vm.getUuid() : String.valueOf(getVirtualMachineId());
        return String.format("Executing FTCTL action %s for VM %s", getAction().name(), identifier);
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.VirtualMachine;
    }

    @Override
    public Long getApiResourceId() {
        return getVirtualMachineId();
    }
}
