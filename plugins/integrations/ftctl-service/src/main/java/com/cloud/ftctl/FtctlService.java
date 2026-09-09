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

import com.cloud.agent.api.FtctlActionCommand;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.admin.ftctl.GetFtctlCheckCmd;
import org.apache.cloudstack.api.command.admin.ftctl.GetFtctlEventsCmd;
import org.apache.cloudstack.api.command.admin.ftctl.GetFtctlHealthCmd;
import org.apache.cloudstack.api.command.admin.ftctl.GetFtctlProtectionCmd;
import org.apache.cloudstack.api.command.admin.ftctl.ListFtctlRemoteMoldHostsCmd;
import org.apache.cloudstack.api.command.admin.ftctl.ListFtctlRemoteMoldNetworksCmd;
import org.apache.cloudstack.api.command.admin.ftctl.ListFtctlRemoteMoldStoragePoolsCmd;
import org.apache.cloudstack.api.command.admin.ftctl.InstallFtctlDrRemoteSshKeyCmd;
import org.apache.cloudstack.api.command.admin.ftctl.PrepareFtctlDrReplicaResourcesCmd;
import org.apache.cloudstack.api.command.admin.ftctl.PrepareFtctlDrRemoteSshAccessCmd;
import org.apache.cloudstack.api.command.admin.ftctl.RegisterFtctlProtectionCmd;
import org.apache.cloudstack.api.command.admin.ftctl.ValidateFtctlRemoteMoldConnectionCmd;
import org.apache.cloudstack.api.response.ftctl.FtctlActionResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlCheckResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlDrReplicaResourcesResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlEventsResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlHealthResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlProtectionResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlRemoteMoldConnectionResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlRemoteMoldHostsResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlRemoteMoldNetworksResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlRemoteMoldStoragePoolsResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

public interface FtctlService extends PluggableService, Configurable {

    ConfigKey<Boolean> FtctlServiceEnabled = new ConfigKey<>("Advanced", Boolean.class,
            "cloud.ftctl.service.enabled",
            "true",
            "Indicates whether FTCTL integration service plugin is enabled or not.",
            false);

    ConfigKey<Boolean> FtctlRuntimeStateSyncEnabled = new ConfigKey<>("Advanced", Boolean.class,
            "cloud.ftctl.runtime.state.sync.enabled",
            "true",
            "Indicates whether Cloud should periodically sync FTCTL runtime state through the agent.",
            false);

    ConfigKey<Integer> FtctlRuntimeStateSyncInterval = new ConfigKey<>("Advanced", Integer.class,
            "cloud.ftctl.runtime.state.sync.interval",
            "10",
            "Interval in seconds for syncing FTCTL runtime state through the agent.",
            false);

    ConfigKey<Boolean> FtctlCloudManagedFailoverMonitorEnabled = new ConfigKey<>("Advanced", Boolean.class,
            "cloud.ftctl.cloud.managed.failover.monitor.enabled",
            "true",
            "Indicates whether Cloud should reconcile Cloud-managed FTCTL automatic failover candidates.",
            false);

    ConfigKey<Integer> FtctlCloudManagedFailoverMonitorInterval = new ConfigKey<>("Advanced", Integer.class,
            "cloud.ftctl.cloud.managed.failover.monitor.interval",
            "10",
            "Interval in seconds for reconciling Cloud-managed FTCTL automatic failover candidates.",
            false);

    ConfigKey<Integer> FtctlCloudManagedFailoverConfirmations = new ConfigKey<>("Advanced", Integer.class,
            "cloud.ftctl.cloud.managed.failover.confirmations",
            "2",
            "Number of consecutive Cloud-managed FTCTL failover candidate observations required before fencing handling.",
            false);

    ConfigKey<Boolean> FtctlCloudManagedFailoverStartEnabled = new ConfigKey<>("Advanced", Boolean.class,
            "cloud.ftctl.cloud.managed.failover.start.enabled",
            "false",
            "Allows Cloud to start a Cloud-managed FTCTL standby VM automatically after a confirmed automatic failover candidate. Disabled by default.",
            false);

    FtctlProtectionResponse getFtctlProtection(GetFtctlProtectionCmd cmd) throws CloudRuntimeException;

    FtctlProtectionResponse registerFtctlProtection(RegisterFtctlProtectionCmd cmd) throws CloudRuntimeException;

    FtctlRemoteMoldConnectionResponse validateFtctlRemoteMoldConnection(ValidateFtctlRemoteMoldConnectionCmd cmd) throws CloudRuntimeException;

    FtctlRemoteMoldHostsResponse listFtctlRemoteMoldHosts(ListFtctlRemoteMoldHostsCmd cmd) throws CloudRuntimeException;

    FtctlRemoteMoldNetworksResponse listFtctlRemoteMoldNetworks(ListFtctlRemoteMoldNetworksCmd cmd) throws CloudRuntimeException;

    FtctlRemoteMoldStoragePoolsResponse listFtctlRemoteMoldStoragePools(ListFtctlRemoteMoldStoragePoolsCmd cmd) throws CloudRuntimeException;

    FtctlDrReplicaResourcesResponse prepareFtctlDrReplicaResources(PrepareFtctlDrReplicaResourcesCmd cmd) throws CloudRuntimeException;

    FtctlActionResponse prepareFtctlDrRemoteSshAccess(PrepareFtctlDrRemoteSshAccessCmd cmd) throws CloudRuntimeException;

    FtctlActionResponse installFtctlDrRemoteSshKey(InstallFtctlDrRemoteSshKeyCmd cmd) throws CloudRuntimeException;

    FtctlCheckResponse getFtctlCheck(GetFtctlCheckCmd cmd) throws CloudRuntimeException;

    FtctlEventsResponse getFtctlEvents(GetFtctlEventsCmd cmd) throws CloudRuntimeException;

    FtctlHealthResponse getFtctlHealth(GetFtctlHealthCmd cmd) throws CloudRuntimeException;

    FtctlActionResponse executeFtctlAction(Long virtualMachineId, FtctlActionCommand.Action action, boolean force) throws CloudRuntimeException;

    FtctlActionResponse failbackFtctlProtection(Long virtualMachineId, boolean force, String failbackTargetMoldType,
                                                String remoteMoldApiUrl, String remoteMoldApiKey, String remoteMoldSecretKey,
                                                String targetMoldApiUrl, String targetMoldApiKey, String targetMoldSecretKey)
            throws CloudRuntimeException;

    FtctlActionResponse releaseFtctlProtection(Long virtualMachineId, boolean force) throws CloudRuntimeException;

    FtctlActionResponse releaseFtctlDrReplicaProtection(Long virtualMachineId, boolean force, boolean cleanupTransport,
                                                        boolean abandonSource) throws CloudRuntimeException;

    FtctlActionResponse adoptFtctlDrReplica(Long virtualMachineId, boolean cleanupTransport) throws CloudRuntimeException;

    FtctlActionResponse failbackFtctlDrReplica(Long virtualMachineId,
                                               String targetMoldApiUrl, String targetMoldApiKey, String targetMoldSecretKey,
                                               String replicaMoldApiUrl, String replicaMoldApiKey, String replicaMoldSecretKey)
            throws CloudRuntimeException;

    FtctlActionResponse confirmFtctlFence(Long virtualMachineId) throws CloudRuntimeException;

    FtctlActionResponse confirmFtctlFence(Long virtualMachineId, String remoteMoldApiUrl,
                                          String remoteMoldApiKey, String remoteMoldSecretKey) throws CloudRuntimeException;
}
