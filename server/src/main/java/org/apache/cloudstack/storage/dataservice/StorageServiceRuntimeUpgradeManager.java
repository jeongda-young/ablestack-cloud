// Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
// See the NOTICE file distributed with this work for additional information regarding copyright ownership.
// Licensed under the Apache License, Version 2.0.
package org.apache.cloudstack.storage.dataservice;

import org.apache.cloudstack.api.command.admin.storage.dataservice.GetStorageServiceRuntimeUpgradeCapabilitiesCmd;
import org.apache.cloudstack.api.command.admin.storage.dataservice.ListStorageServiceRuntimeBundlesCmd;
import org.apache.cloudstack.api.command.admin.storage.dataservice.ListStorageServiceRuntimeUpgradesCmd;
import org.apache.cloudstack.api.command.admin.storage.dataservice.PreflightStorageServiceRuntimeUpgradeCmd;
import org.apache.cloudstack.api.command.admin.storage.dataservice.RegisterStorageServiceRuntimeBundleCmd;
import org.apache.cloudstack.api.command.admin.storage.dataservice.RollbackStorageServiceRuntimeUpgradeCmd;
import org.apache.cloudstack.api.command.admin.storage.dataservice.UpgradeStorageServiceRuntimeCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.StorageServiceRuntimeBundleResponse;
import org.apache.cloudstack.api.response.StorageServiceRuntimeCapabilityResponse;
import org.apache.cloudstack.api.response.StorageServiceRuntimeUpgradeResponse;

public interface StorageServiceRuntimeUpgradeManager {
    StorageServiceRuntimeBundleResponse register(RegisterStorageServiceRuntimeBundleCmd cmd);
    ListResponse<StorageServiceRuntimeBundleResponse> listBundles(ListStorageServiceRuntimeBundlesCmd cmd);
    StorageServiceRuntimeCapabilityResponse capabilities(GetStorageServiceRuntimeUpgradeCapabilitiesCmd cmd);
    StorageServiceRuntimeUpgradeResponse preflight(PreflightStorageServiceRuntimeUpgradeCmd cmd);
    StorageServiceRuntimeUpgradeResponse upgrade(UpgradeStorageServiceRuntimeCmd cmd);
    ListResponse<StorageServiceRuntimeUpgradeResponse> listUpgrades(ListStorageServiceRuntimeUpgradesCmd cmd);
    StorageServiceRuntimeUpgradeResponse rollback(RollbackStorageServiceRuntimeUpgradeCmd cmd);
}
