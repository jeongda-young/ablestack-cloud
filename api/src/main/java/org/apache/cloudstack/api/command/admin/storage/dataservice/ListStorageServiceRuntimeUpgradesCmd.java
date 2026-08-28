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
package org.apache.cloudstack.api.command.admin.storage.dataservice;

import javax.inject.Inject;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SharedFSResponse;
import org.apache.cloudstack.api.response.StorageServiceRuntimeUpgradeResponse;
import org.apache.cloudstack.storage.dataservice.StorageService;

@APICommand(name = "listStorageServiceRuntimeUpgrades", responseObject = StorageServiceRuntimeUpgradeResponse.class,
        description = "Lists Storage Service runtime upgrade transactions.", since = "4.23.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class ListStorageServiceRuntimeUpgradesCmd extends BaseListCmd implements AdminCmd {
    @Inject private StorageService storageService;
    @Parameter(name = "id", type = CommandType.UUID, entityType = StorageServiceRuntimeUpgradeResponse.class) private Long id;
    @Parameter(name = "sharedfilesystemid", type = CommandType.UUID, entityType = SharedFSResponse.class) private Long sharedFileSystemId;
    public Long getId() { return id; }
    public Long getSharedFileSystemId() { return sharedFileSystemId; }
    public void execute() { final ListResponse<StorageServiceRuntimeUpgradeResponse> response = storageService.listStorageServiceRuntimeUpgrades(this); response.setResponseName(getCommandName()); setResponseObject(response); }
}
