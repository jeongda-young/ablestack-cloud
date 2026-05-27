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
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.commons.lang3.StringUtils;

@APICommand(name = "importUnmanagedInstanceForAblestackV2K",
        description = "Import virtual machine from VMware into CloudStack using ablestack-v2k workflow",
        responseObject = UserVmResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = true,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin},
        since = "4.19.0")
public class ImportUnmanagedInstanceForAblestackV2KCmd extends ImportVmCmd {

    private static final String DEFAULT_SPLIT_MODE = "phase1";

    @Parameter(name = "split",
            type = CommandType.STRING,
            description = "(only for importing VMs from VMware to KVM with ablestack-v2k) split-run mode: phase1 or phase2")
    private String splitMode;

    @Parameter(name = ApiConstants.IMPORT_VM_TASK_ID,
            type = CommandType.STRING,
            description = "(only for task continuation) existing import VM task ID to continue on the original conversion host")
    private String importVmTaskId;

    @Parameter(name = "taskaction",
            type = CommandType.STRING,
            description = "(only with importvmtaskid) task action to execute: phase2, resume, or retryfromstart")
    private String taskAction;

    public String getSplitMode() {
        return StringUtils.defaultIfBlank(splitMode, DEFAULT_SPLIT_MODE);
    }

    public String getImportVmTaskId() {
        return importVmTaskId;
    }

    public String getTaskAction() {
        return taskAction;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        UserVmResponse response = vmImportService.importVmForAblestackV2K(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
