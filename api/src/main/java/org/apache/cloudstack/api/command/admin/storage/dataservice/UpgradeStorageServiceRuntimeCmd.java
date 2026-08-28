// Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
// See the NOTICE file distributed with this work for additional information regarding copyright ownership.
// Licensed under the Apache License, Version 2.0.
package org.apache.cloudstack.api.command.admin.storage.dataservice;

import javax.inject.Inject;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.response.StorageServiceRuntimeUpgradeResponse;
import org.apache.cloudstack.storage.dataservice.StorageService;

@APICommand(name = "upgradeStorageServiceRuntime", responseObject = StorageServiceRuntimeUpgradeResponse.class,
        description = "Activates a preflighted Storage Service runtime bundle.", since = "4.23.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class UpgradeStorageServiceRuntimeCmd extends BaseAsyncCmd implements AdminCmd {
    @Inject private StorageService storageService;
    @Parameter(name = "upgradeid", type = CommandType.UUID, entityType = StorageServiceRuntimeUpgradeResponse.class, required = true) private Long upgradeId;
    public Long getUpgradeId() { return upgradeId; }
    public long getEntityOwnerId() { return 0; }
    public String getEventType() { return "STORAGE.SERVICE.RUNTIME.UPGRADE"; }
    public String getEventDescription() { return "Upgrading Storage Service runtime transaction " + upgradeId; }
    public void execute() { final StorageServiceRuntimeUpgradeResponse response = storageService.upgradeStorageServiceRuntime(this); response.setResponseName(getCommandName()); setResponseObject(response); }
}
