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
package com.cloud.ftctl;

import com.cloud.utils.component.Manager;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.admin.ftctl.PrepareFtctlDrReplicaResourcesCmd;
import org.apache.cloudstack.api.response.ftctl.FtctlDrReplicaResourcesResponse;

public interface FtctlProtectionProvisioningService extends Manager {

    String BACKEND_LIBVIRT_MANAGED = "libvirt-managed";
    String BACKEND_CLOUD_MANAGED = "cloud-managed";
    String STATE_READY = "Ready";
    String STATE_NOT_IMPLEMENTED = "NotImplemented";
    String STATE_STANDBY_ALLOCATED = "StandbyAllocated";
    String STATE_PROVISIONING_FAILED = "ProvisioningFailed";

    FtctlProtectionProvisioningContext prepareProtection(FtctlProtectionProvisioningRequest request) throws CloudRuntimeException;

    FtctlDrReplicaResourcesResponse prepareDrReplicaResources(PrepareFtctlDrReplicaResourcesCmd cmd) throws CloudRuntimeException;
}
