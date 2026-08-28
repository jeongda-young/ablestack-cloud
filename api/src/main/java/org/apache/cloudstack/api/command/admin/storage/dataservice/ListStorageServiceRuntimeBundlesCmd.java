// Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
// See the NOTICE file distributed with this work for additional information regarding copyright ownership.
// Licensed under the Apache License, Version 2.0.
package org.apache.cloudstack.api.command.admin.storage.dataservice;

import javax.inject.Inject;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.StorageServiceRuntimeBundleResponse;
import org.apache.cloudstack.storage.dataservice.StorageService;

@APICommand(name = "listStorageServiceRuntimeBundles", responseObject = StorageServiceRuntimeBundleResponse.class,
        description = "Lists registered Storage Service runtime bundles.", since = "4.23.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, authorized = {RoleType.Admin})
public class ListStorageServiceRuntimeBundlesCmd extends BaseListCmd implements AdminCmd {
    @Inject private StorageService storageService;
    @Parameter(name = "id", type = CommandType.UUID, entityType = StorageServiceRuntimeBundleResponse.class) private Long id;
    @Parameter(name = "version", type = CommandType.STRING) private String version;
    public Long getId() { return id; }
    public String getVersion() { return version; }
    public void execute() { final ListResponse<StorageServiceRuntimeBundleResponse> response = storageService.listStorageServiceRuntimeBundles(this); response.setResponseName(getCommandName()); setResponseObject(response); }
}
