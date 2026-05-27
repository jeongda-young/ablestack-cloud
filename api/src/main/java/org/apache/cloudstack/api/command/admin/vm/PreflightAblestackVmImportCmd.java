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
import org.apache.cloudstack.api.response.AblestackVmImportPreflightResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.VmImportService;

import javax.inject.Inject;

@APICommand(name = "preflightAblestackVmImport",
        description = "Preflight source and target checks for ABLESTACK v2k/n2k VM import",
        responseObject = AblestackVmImportPreflightResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = true,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "4.22")
public class PreflightAblestackVmImportCmd extends BaseCmd {

    @Inject
    public VmImportService vmImportService;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            required = true,
            description = "the zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.CLUSTER_ID,
            type = CommandType.UUID,
            entityType = ClusterResponse.class,
            description = "the target KVM cluster ID")
    private Long clusterId;

    @Parameter(name = ApiConstants.CONVERT_INSTANCE_HOST_ID,
            type = CommandType.UUID,
            entityType = HostResponse.class,
            description = "the KVM host that will run the import tool")
    private Long convertInstanceHostId;

    @Parameter(name = ApiConstants.CONVERT_INSTANCE_STORAGE_POOL_ID,
            type = CommandType.UUID,
            entityType = StoragePoolResponse.class,
            description = "the target primary storage pool ID")
    private Long targetStoragePoolId;

    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID,
            type = CommandType.UUID,
            entityType = ServiceOfferingResponse.class,
            description = "the target service offering ID")
    private Long serviceOfferingId;

    @Parameter(name = "migrationtool",
            type = CommandType.STRING,
            description = "migration tool. Supported values: ablestack_n2k, ablestack_v2k")
    private String migrationTool;

    @Parameter(name = "sourceprovider",
            type = CommandType.STRING,
            required = true,
            description = "source provider. Supported values: nutanix, vmware")
    private String sourceProvider;

    @Parameter(name = ApiConstants.HOST,
            type = CommandType.STRING,
            required = true,
            description = "source endpoint, for example Nutanix Prism Central endpoint")
    private String host;

    @Parameter(name = ApiConstants.USERNAME,
            type = CommandType.STRING,
            description = "the source username")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD,
            type = CommandType.STRING,
            description = "the source password")
    private String password;

    @Parameter(name = "sourceapi",
            type = CommandType.STRING,
            description = "source API selection. Supported values for Nutanix: auto, v4, v3, v2")
    private String sourceApi;

    @Parameter(name = "sourcevmname",
            type = CommandType.STRING,
            description = "optional source VM name or UUID to validate")
    private String sourceVmName;

    @Parameter(name = "insecure",
            type = CommandType.BOOLEAN,
            description = "skip TLS verification for source endpoint when true")
    private Boolean insecure;

    public Long getZoneId() {
        return zoneId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Long getConvertInstanceHostId() {
        return convertInstanceHostId;
    }

    public Long getTargetStoragePoolId() {
        return targetStoragePoolId;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public String getMigrationTool() {
        return migrationTool;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public String getHost() {
        return host;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSourceApi() {
        return sourceApi;
    }

    public String getSourceVmName() {
        return sourceVmName;
    }

    public boolean isInsecure() {
        return Boolean.TRUE.equals(insecure);
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        AblestackVmImportPreflightResponse response = vmImportService.preflightAblestackVmImport(this);
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
