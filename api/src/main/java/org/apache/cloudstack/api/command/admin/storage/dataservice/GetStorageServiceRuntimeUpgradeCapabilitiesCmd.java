// Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
// See the NOTICE file distributed with this work for additional information regarding copyright ownership.
// Licensed under the Apache License, Version 2.0.
package org.apache.cloudstack.api.command.admin.storage.dataservice;

import javax.inject.Inject;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.response.SharedFSResponse;
import org.apache.cloudstack.api.response.StorageServiceRuntimeCapabilityResponse;
import org.apache.cloudstack.storage.dataservice.StorageService;

@APICommand(name = "getStorageServiceRuntimeUpgradeCapabilities", responseObject = StorageServiceRuntimeCapabilityResponse.class,
        description = "Returns runtime upgrade capabilities for a Shared FileSystem.", since = "4.23.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class GetStorageServiceRuntimeUpgradeCapabilitiesCmd extends BaseCmd implements AdminCmd {
    @Inject private StorageService storageService;
    @Parameter(name = "sharedfilesystemid", type = CommandType.UUID, entityType = SharedFSResponse.class, required = true) private Long sharedFileSystemId;
    public Long getSharedFileSystemId() { return sharedFileSystemId; }
    public long getEntityOwnerId() { return 0; }
    public void execute() { final StorageServiceRuntimeCapabilityResponse response = storageService.getStorageServiceRuntimeUpgradeCapabilities(this); response.setResponseName(getCommandName()); setResponseObject(response); }
}
