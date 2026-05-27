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
package org.apache.cloudstack.api.command.admin.vm;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ImportVMTaskResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.ImportVmTasksManager;
import org.apache.commons.lang3.BooleanUtils;

import javax.inject.Inject;

@APICommand(name = "executeImportVmTaskAction",
        description = "Execute a generic action on an import virtual machine task",
        responseObject = ImportVMTaskResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "4.22")
public class ExecuteImportVMTaskActionCmd extends BaseCmd {

    @Inject
    public ImportVmTasksManager importVmTasksManager;

    @Parameter(name = ApiConstants.IMPORT_VM_TASK_ID,
            type = CommandType.STRING,
            required = true,
            description = "the import VM task ID")
    private String importVmTaskId;

    @Parameter(name = ApiConstants.ACTION,
            type = CommandType.STRING,
            required = true,
            description = "the task action to execute. Supported values are: refresh, phase2, resume, retryfromstart, cancel, delete, clearcredentials")
    private String action;

    @Parameter(name = "cleanup",
            type = CommandType.BOOLEAN,
            description = "whether to cleanup runtime artifacts such as workdir when the selected action supports cleanup")
    private Boolean cleanup;

    @Parameter(name = "removecredentials",
            type = CommandType.BOOLEAN,
            description = "whether to remove stored encrypted source credentials when the selected action supports credential cleanup")
    private Boolean removeCredentials;

    @Parameter(name = "force",
            type = CommandType.BOOLEAN,
            description = "force the selected action when it is otherwise restricted")
    private Boolean force;

    public String getImportVmTaskId() {
        return importVmTaskId;
    }

    public String getAction() {
        return action;
    }

    public boolean isCleanup() {
        return BooleanUtils.toBooleanDefaultIfNull(cleanup, false);
    }

    public boolean isRemoveCredentials() {
        return BooleanUtils.toBooleanDefaultIfNull(removeCredentials, false);
    }

    public boolean isForced() {
        return BooleanUtils.toBooleanDefaultIfNull(force, false);
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        ImportVMTaskResponse response = importVmTasksManager.executeImportVMTaskAction(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if (account != null) {
            return account.getId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
