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
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ImportVMTaskResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.ImportVmTasksManager;

import javax.inject.Inject;

@APICommand(name = "listImportVmTasks",
        description = "List running import virtual machine tasks from a unmanaged hosts into CloudStack",
        responseObject = ImportVMTaskResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "4.22")
public class ListImportVMTasksCmd extends BaseListCmd {

    @Inject
    public ImportVmTasksManager importVmTasksManager;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            required = true,
            description = "the zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.ACCOUNT_ID,
            type = CommandType.UUID,
            entityType = AccountResponse.class,
            description = "the ID of the Account")
    private Long accountId;

    @Parameter(name = ApiConstants.VCENTER,
            type = CommandType.STRING,
            description = "The name/ip of vCenter. Make sure it is IP address or full qualified domain name for host running vCenter server.")
    private String vcenter;

    @Parameter(name = ApiConstants.CONVERT_INSTANCE_HOST_ID,
            type = CommandType.UUID,
            entityType = HostResponse.class,
            description = "Conversion host of the importing task")
    private Long convertHostId;

    @Parameter(name = ApiConstants.TASKS_FILTER, type = CommandType.STRING, description = "Filter tasks by state, valid options are: All, Running, Completed, Failed")
    private String tasksFilter;

    @Parameter(name = "migrationtool",
            type = CommandType.STRING,
            description = "Filter tasks by migration tool, for example legacy, ablestack_v2k, or ablestack_n2k")
    private String migrationTool;

    @Parameter(name = "sourceprovider",
            type = CommandType.STRING,
            description = "Filter tasks by source provider, for example vmware or nutanix")
    private String sourceProvider;

    @Parameter(name = "targetprovider",
            type = CommandType.STRING,
            description = "Filter tasks by target provider, for example cloud or kvm")
    private String targetProvider;

    @Parameter(name = "targetprofile",
            type = CommandType.STRING,
            description = "Filter tasks by target profile")
    private String targetProfile;

    @Parameter(name = "currentphase",
            type = CommandType.STRING,
            description = "Filter tasks by normalized current migration phase")
    private String currentPhase;

    @Parameter(name = "migrationstate",
            type = CommandType.STRING,
            description = "Filter tasks by normalized migration state")
    private String migrationState;

    public Long getZoneId() {
        return zoneId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getVcenter() {
        return vcenter;
    }

    public Long getConvertHostId() {
        return convertHostId;
    }

    public String getTasksFilter() {
        return tasksFilter;
    }

    public String getMigrationTool() {
        return migrationTool;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public String getTargetProvider() {
        return targetProvider;
    }

    public String getTargetProfile() {
        return targetProfile;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public String getMigrationState() {
        return migrationState;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        ListResponse<ImportVMTaskResponse> response = importVmTasksManager.listImportVMTasks(this);
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
