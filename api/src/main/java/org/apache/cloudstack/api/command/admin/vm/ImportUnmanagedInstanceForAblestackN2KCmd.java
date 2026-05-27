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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

@APICommand(name = "importUnmanagedInstanceForAblestackN2K",
        description = "Import virtual machine from Nutanix into CloudStack using ablestack-n2k workflow",
        responseObject = UserVmResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = true,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin},
        since = "4.22")
public class ImportUnmanagedInstanceForAblestackN2KCmd extends ImportVmCmd {

    private static final String DEFAULT_SPLIT_MODE = "phase1";
    private static final String DEFAULT_SOURCE_API = "v3";
    private static final Long DEFAULT_RETENTION_SECONDS = 1209600L;

    @Parameter(name = "split",
            type = CommandType.STRING,
            description = "(only for importing VMs from Nutanix to KVM with ablestack-n2k) split-run mode: phase1, phase2 or full")
    private String splitMode;

    @Parameter(name = ApiConstants.IMPORT_VM_TASK_ID,
            type = CommandType.STRING,
            description = "(only for task continuation) existing import VM task ID to continue on the original conversion host")
    private String importVmTaskId;

    @Parameter(name = "taskaction",
            type = CommandType.STRING,
            description = "(only with importvmtaskid) task action to execute: phase2, resume, or retryfromstart")
    private String taskAction;

    @Parameter(name = "sourceapi",
            type = CommandType.STRING,
            description = "source API for ablestack-n2k run. Current Cloud-managed execution uses v3 snapshot/NFS data path")
    private String sourceApi;

    @Parameter(name = "insecure",
            type = CommandType.BOOLEAN,
            description = "skip TLS verification for Nutanix Prism when true")
    private Boolean insecure;

    @Parameter(name = "retentionseconds",
            type = CommandType.LONG,
            description = "source snapshot/recovery point retention time in seconds for ablestack-n2k. Default is 1209600 seconds (14 days)")
    private Long retentionSeconds;

    @Parameter(name = "starttargetvm",
            type = CommandType.BOOLEAN,
            description = "start the imported Cloud target VM after ablestack-n2k phase2 cutover. Default is true")
    private Boolean startTargetVm;

    public String getSplitMode() {
        return StringUtils.defaultIfBlank(splitMode, DEFAULT_SPLIT_MODE);
    }

    public String getImportVmTaskId() {
        return importVmTaskId;
    }

    public String getTaskAction() {
        return taskAction;
    }

    public String getSourceApi() {
        return StringUtils.defaultIfBlank(sourceApi, DEFAULT_SOURCE_API);
    }

    public boolean isInsecure() {
        return BooleanUtils.toBooleanDefaultIfNull(insecure, true);
    }

    public Long getRequestedRetentionSeconds() {
        return retentionSeconds;
    }

    public long getRetentionSeconds() {
        return retentionSeconds != null ? retentionSeconds : DEFAULT_RETENTION_SECONDS;
    }

    public Boolean getRequestedStartTargetVm() {
        return startTargetVm;
    }

    public boolean isStartTargetVm() {
        return BooleanUtils.toBooleanDefaultIfNull(startTargetVm, true);
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        UserVmResponse response = vmImportService.importVmForAblestackN2K(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
