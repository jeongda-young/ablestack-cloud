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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FtctlActionAnswer;
import com.cloud.agent.api.FtctlActionCommand;
import com.cloud.agent.api.FtctlDrSshAccessCommand;
import com.cloud.agent.api.FtctlEventsAnswer;
import com.cloud.agent.api.FtctlEventsCommand;
import com.cloud.agent.api.FtctlRemotePreflightCommand;
import com.cloud.agent.api.FtctlStatusAnswer;
import com.cloud.agent.api.FtctlStatusCommand;
import com.cloud.agent.api.FtctlSyncAnswer;
import com.cloud.agent.api.FtctlSyncClusterCommand;
import com.cloud.agent.api.FtctlSyncProfileCommand;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.ftctl.dao.FtctlProtectionDao;
import com.cloud.ftctl.dao.FtctlProtectionVolumeDao;
import com.cloud.host.Host;
import com.cloud.host.DetailVO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.User;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.command.admin.ftctl.GetFtctlCheckCmd;
import org.apache.cloudstack.api.command.admin.ftctl.GetFtctlEventsCmd;
import org.apache.cloudstack.api.command.admin.ftctl.GetFtctlHealthCmd;
import org.apache.cloudstack.api.command.admin.ftctl.GetFtctlProtectionCmd;
import org.apache.cloudstack.api.command.admin.ftctl.ExecuteFtctlDrSiteAgentCommandCmd;
import org.apache.cloudstack.api.command.admin.ftctl.FailbackFtctlDrReplicaCmd;
import org.apache.cloudstack.api.command.admin.ftctl.InstallFtctlDrRemoteSshKeyCmd;
import org.apache.cloudstack.api.command.admin.ftctl.ListFtctlRemoteMoldHostsCmd;
import org.apache.cloudstack.api.command.admin.ftctl.ListFtctlRemoteMoldNetworksCmd;
import org.apache.cloudstack.api.command.admin.ftctl.ListFtctlRemoteMoldStoragePoolsCmd;
import org.apache.cloudstack.api.command.admin.ftctl.PrepareFtctlDrReplicaResourcesCmd;
import org.apache.cloudstack.api.command.admin.ftctl.PrepareFtctlDrRemoteSshAccessCmd;
import org.apache.cloudstack.api.command.admin.ftctl.RegisterFtctlProtectionCmd;
import org.apache.cloudstack.api.command.admin.ftctl.ReleaseFtctlProtectionCmd;
import org.apache.cloudstack.api.command.admin.ftctl.AdoptFtctlDrReplicaCmd;
import org.apache.cloudstack.api.command.admin.ftctl.ReleaseFtctlDrReplicaProtectionCmd;
import org.apache.cloudstack.api.command.admin.ftctl.ValidateFtctlRemoteMoldConnectionCmd;
import org.apache.cloudstack.api.response.ftctl.FtctlActionResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlCheckResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlDrReplicaResourcesResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlEventResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlEventsResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlHealthResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlProtectionResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlProtectionVolumeResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlRemoteMoldConnectionResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlRemoteMoldHostResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlRemoteMoldHostsResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlRemoteMoldNetworkResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlRemoteMoldNetworksResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlRemoteMoldStoragePoolResponse;
import org.apache.cloudstack.api.response.ftctl.FtctlRemoteMoldStoragePoolsResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextTimerTask;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementService;
import org.apache.cloudstack.outofbandmanagement.dao.OutOfBandManagementDao;

import javax.inject.Inject;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class FtctlServiceImpl extends ManagerBase implements FtctlService {
    private static final int FAILBACK_ACTION_WAIT_SECONDS = 900;
    private static final int UNPROTECT_ACTION_WAIT_SECONDS = 240;
    private static final int FTCTL_ACTION_LOCK_EXIT_CODE = 20;
    private static final int FTCTL_ACTION_LOCK_RETRY_INTERVAL_MILLIS = 2000;
    private static final int FTCTL_ACTION_LOCK_RETRY_TIMEOUT_MILLIS = 90000;
    private static final long CLOUD_MANAGED_FAILBACK_MONITOR_DELAY_MILLIS = 10000L;
    private static final long CLOUD_MANAGED_FAILBACK_MONITOR_INTERVAL_MILLIS = 10000L;
    private static final long CLOUD_MANAGED_FAILBACK_CONTEXT_TTL_MILLIS = Duration.ofHours(2).toMillis();
    private static final long RUNTIME_STATE_SYNC_DELAY_MILLIS = 10000L;
    private static final long MIN_RUNTIME_STATE_SYNC_INTERVAL_MILLIS = 5000L;
    private static final long CLOUD_MANAGED_FAILOVER_MONITOR_DELAY_MILLIS = 10000L;
    private static final long MIN_CLOUD_MANAGED_FAILOVER_MONITOR_INTERVAL_MILLIS = 5000L;
    private static final long REMOTE_MOLD_VM_START_POLL_INTERVAL_MILLIS = 5000L;
    private static final long REMOTE_MOLD_VM_START_TIMEOUT_MILLIS = 300000L;

    public static final String DETAIL_ENABLED = "ftctl.enabled";
    public static final String DETAIL_MODE = "ftctl.mode";
    public static final String DETAIL_BACKEND_MODE = "ftctl.backend.mode";
    public static final String DETAIL_PROVISIONING_BACKEND = "ftctl.provisioning.backend";
    public static final String DETAIL_PROVISIONING_STATE = "ftctl.provisioning.state";
    public static final String DETAIL_TARGET_STORAGE_SCOPE = "ftctl.target.storage.scope";
    public static final String DETAIL_TARGET_STORAGE_POOL_ID = "ftctl.target.storage.pool.id";
    public static final String DETAIL_TARGET_STORAGE_POOL_NAME = "ftctl.target.storage.pool.name";
    public static final String DETAIL_TARGET_NETWORK_IDS = "ftctl.target.network.ids";
    public static final String DETAIL_DR_PEER_SITE_TYPE = "ftctl.dr.peer.site.type";
    public static final String DETAIL_REMOTE_MOLD_API_URL = "ftctl.dr.remote.mold.api.url";
    public static final String DETAIL_REMOTE_PEER_HOST_ID = "ftctl.dr.remote.peer.host.id";
    public static final String DETAIL_REMOTE_PEER_HOST_NAME = "ftctl.dr.remote.peer.host.name";
    public static final String DETAIL_REMOTE_PEER_HOST_ADDRESS = "ftctl.dr.remote.peer.host.address";
    public static final String DETAIL_REMOTE_PEER_HOST_BLOCKCOPY_ADDRESS = "ftctl.dr.remote.peer.host.blockcopy.address";
    public static final String DETAIL_REMOTE_PEER_SSH_USER = "ftctl.dr.remote.peer.ssh.user";
    public static final String DETAIL_REMOTE_PEER_SSH_PORT = "ftctl.dr.remote.peer.ssh.port";
    public static final String DETAIL_REMOTE_PEER_SSH_OVERRIDE = "ftctl.dr.remote.peer.ssh.override";
    public static final String DETAIL_REMOTE_PEER_SSH_AUTO_SETUP = "ftctl.dr.remote.peer.ssh.auto.setup";
    public static final String DETAIL_REMOTE_PEER_LIBVIRT_URI = "ftctl.dr.remote.peer.libvirt.uri";
    public static final String DETAIL_REMOTE_TARGET_STORAGE_POOL_ID = "ftctl.dr.remote.target.storage.pool.id";
    public static final String DETAIL_REMOTE_TARGET_STORAGE_POOL_NAME = "ftctl.dr.remote.target.storage.pool.name";
    public static final String DETAIL_REMOTE_TARGET_STORAGE_POOL_PATH = "ftctl.dr.remote.target.storage.pool.path";
    public static final String DETAIL_REMOTE_TARGET_STORAGE_POOL_TYPE = "ftctl.dr.remote.target.storage.pool.type";
    public static final String DETAIL_REMOTE_REPLICA_VM_ID = "ftctl.dr.remote.replica.vm.id";
    public static final String DETAIL_REMOTE_REPLICA_VM_NAME = "ftctl.dr.remote.replica.vm.name";
    public static final String DETAIL_REMOTE_REPLICA_VM_INSTANCE_NAME = "ftctl.dr.remote.replica.vm.instance.name";
    public static final String DETAIL_REMOTE_REPLICA_VM_STATE = "ftctl.dr.remote.replica.vm.state";
    public static final String DETAIL_REMOTE_REPLICA_VM_HOST_ID = "ftctl.dr.remote.replica.vm.host.id";
    public static final String DETAIL_REMOTE_REPLICA_VM_HOST_NAME = "ftctl.dr.remote.replica.vm.host.name";
    public static final String DETAIL_REMOTE_REPLICA_VM_STATE_UPDATED = "ftctl.dr.remote.replica.vm.state.updated";
    public static final String DETAIL_REMOTE_REPLICA_VOLUME_PREFIX = "ftctl.dr.remote.replica.volume.";
    public static final String DETAIL_REMOTE_STANDBY_VM = "ftctl.standby.vm";
    public static final String DETAIL_REMOTE_PRIMARY_VM_ID = "ftctl.primary.vm.id";
    public static final String DETAIL_REMOTE_MOLD_REPLICA_VM = "ftctl.remote.replica.vm";
    public static final String DETAIL_REMOTE_SOURCE_VM_UUID = "ftctl.remote.source.vm.uuid";
    public static final String DETAIL_REMOTE_SOURCE_VM_NAME = "ftctl.remote.source.vm.name";
    public static final String DETAIL_REMOTE_SOURCE_VM_INSTANCE_NAME = "ftctl.remote.source.vm.instance.name";
    public static final String DETAIL_DR_RECOVERY_SOURCE_MOLD_API_URL = "ftctl.dr.recovery.source.mold.api.url";
    public static final String DETAIL_DR_RECOVERY_SESSION_UUID = "ftctl.dr.recovery.session.uuid";
    public static final String DETAIL_DR_RECOVERY_ROLE = "ftctl.dr.recovery.role";
    public static final String DETAIL_DR_RECOVERY_RELEASED = "ftctl.dr.recovery.released";
    public static final String DETAIL_DR_RECOVERY_UPDATED = "ftctl.dr.recovery.updated";
    public static final String DETAIL_DR_RECOVERY_LAST_ERROR = "ftctl.dr.recovery.last.error";
    public static final String DETAIL_FENCING_POLICY = "ftctl.fencing.policy";
    public static final String DETAIL_FENCING_IPMI_PRIMARY_HOST = "ftctl.fencing.ipmi.primary.host";
    public static final String DETAIL_FENCING_IPMI_SECONDARY_HOST = "ftctl.fencing.ipmi.secondary.host";
    public static final String DETAIL_FENCING_IPMI_PRIMARY_PORT = "ftctl.fencing.ipmi.primary.port";
    public static final String DETAIL_FENCING_IPMI_SECONDARY_PORT = "ftctl.fencing.ipmi.secondary.port";
    public static final String DETAIL_FENCING_IPMI_PRIMARY_USER = "ftctl.fencing.ipmi.primary.user";
    public static final String DETAIL_FENCING_IPMI_SECONDARY_USER = "ftctl.fencing.ipmi.secondary.user";
    public static final String DETAIL_FENCING_IPMI_PRIMARY_INTERFACE = "ftctl.fencing.ipmi.primary.interface";
    public static final String DETAIL_FENCING_IPMI_SECONDARY_INTERFACE = "ftctl.fencing.ipmi.secondary.interface";
    public static final String DETAIL_PEER_HOST_ID = "ftctl.peer.host.id";
    public static final String DETAIL_SECONDARY_VM_NAME = "ftctl.secondary.vm.name";
    public static final String DETAIL_SECONDARY_TARGET_DIR = "ftctl.secondary.target.dir";
    public static final String DETAIL_REMOTE_NBD_EXPORT_ADDR = "ftctl.remote.nbd.export.addr";
    public static final String DETAIL_LAST_PROTECTION_STATE = "ftctl.last.protection.state";
    public static final String DETAIL_LAST_TRANSPORT_STATE = "ftctl.last.transport.state";
    public static final String DETAIL_LAST_ACTIVE_SIDE = "ftctl.last.active.side";
    public static final String DETAIL_LAST_ADMIN_STATE = "ftctl.last.admin.state";
    public static final String DETAIL_LAST_FENCING_STATE = "ftctl.last.fencing.state";
    public static final String DETAIL_LAST_ERROR = "ftctl.last.error";
    public static final String DETAIL_CHECK_RESULT = "ftctl.check.result";
    public static final String DETAIL_CHECK_INVENTORY_RESULT = "ftctl.check.inventory.result";
    public static final String DETAIL_CHECK_PRIMARY_RC = "ftctl.check.primary.rc";
    public static final String DETAIL_CHECK_PEER_RC = "ftctl.check.peer.rc";
    public static final String DETAIL_CHECK_PEER_DOMAIN_EXPECTED = "ftctl.check.peer.domain.expected";
    public static final String DETAIL_CHECK_STANDBY_DOMAIN_STATE = "ftctl.check.standby.domain.state";
    public static final String DETAIL_CHECK_UPDATED = "ftctl.check.updated";
    public static final String DETAIL_HEALTH_RESULT = "ftctl.health.result";
    public static final String DETAIL_HEALTH_URI = "ftctl.health.uri";
    public static final String DETAIL_HEALTH_RC = "ftctl.health.rc";
    public static final String DETAIL_HEALTH_UPDATED = "ftctl.health.updated";
    public static final String DETAIL_FAILOVER_READY = "ftctl.failover.ready";
    public static final String DETAIL_FAILOVER_READY_UPDATED = "ftctl.failover.ready.updated";
    public static final String DETAIL_AUTO_FAILOVER_FAILURE_COUNT = "ftctl.auto.failover.failure.count";
    public static final String DETAIL_AUTO_FAILOVER_UPDATED = "ftctl.auto.failover.updated";
    public static final String DETAIL_AUTO_FAILOVER_REASON = "ftctl.auto.failover.reason";
    public static final String DETAIL_FENCING_RESULT = "ftctl.fencing.result";
    public static final String DETAIL_FENCING_REASON = "ftctl.fencing.reason";
    public static final String DETAIL_NIC_IDENTITY_STATE = "ftctl.nic.identity.state";
    public static final String DETAIL_NIC_IDENTITY_PRIMARY_PREFIX = "ftctl.nic.identity.primary.";
    public static final String DETAIL_NIC_IDENTITY_SECONDARY_PREFIX = "ftctl.nic.identity.secondary.";
    public static final String DETAIL_LIFECYCLE_GUARD = "ftctl.lifecycle.guard";
    public static final String DETAIL_LIFECYCLE_GUARD_ORIGINAL_HA_ENABLED = "ftctl.lifecycle.guard.original.ha.enabled";
    public static final String DETAIL_LIFECYCLE_GUARD_PRIMARY_HOST_ID = "ftctl.lifecycle.guard.primary.host.id";
    public static final String DETAIL_LIFECYCLE_GUARD_STARTED = "ftctl.lifecycle.guard.started";
    public static final String HOST_DETAIL_ENABLED = "ftctl.enabled";
    public static final String HOST_DETAIL_MANAGEMENT_IP = "ftctl.management.ip";
    public static final String HOST_DETAIL_LIBVIRT_URI = "ftctl.libvirt.uri";
    public static final String HOST_DETAIL_BLOCKCOPY_IP = "ftctl.blockcopy.ip";
    public static final String HOST_DETAIL_XCOLO_CONTROL_IP = "ftctl.xcolo.control.ip";
    public static final String HOST_DETAIL_XCOLO_DATA_IP = "ftctl.xcolo.data.ip";
    private static final String FENCING_POLICY_IPMI = "ipmi";
    private static final String OOBM_DRIVER_IPMITOOL = "ipmitool";
    private static final String DEFAULT_IPMI_INTERFACE = "lanplus";
    private static final String DR_PEER_SITE_TYPE_LOCAL_MOLD = "local-mold";
    private static final String DR_PEER_SITE_TYPE_REMOTE_MOLD = "remote-mold";
    private static final String FAILBACK_TARGET_MOLD_CURRENT = "current";
    private static final String FAILBACK_TARGET_MOLD_ORIGINAL_PRIMARY = "original-primary";
    private static final String FAILBACK_TARGET_MOLD_NEW = "new";
    private static final String DEFAULT_REMOTE_PEER_SSH_USER = "root";
    private static final String DEFAULT_REMOTE_PEER_SSH_PORT = "22";
    private static final String FTCTL_DEFAULT_XCOLO_MACHINE_TYPE = "pc-i440fx-9.2";
    private static final int FTCTL_XCOLO_AUTO_SLOT_COUNT = 16;
    private static final int FTCTL_XCOLO_AUTO_PROXY_BASE = 9100;
    private static final int FTCTL_XCOLO_AUTO_COMPARE_LOCAL_BASE = 9101;
    private static final int FTCTL_XCOLO_AUTO_MIRROR_BASE = 9103;
    private static final int FTCTL_XCOLO_AUTO_COMPARE_BASE = 9104;
    private static final int FTCTL_XCOLO_AUTO_COMPARE_OUT_BASE = 9105;
    private static final int FTCTL_XCOLO_AUTO_PORT_STEP = 10;
    private static final int FTCTL_XCOLO_AUTO_MIGRATE_BASE = 9198;
    private static final int FTCTL_XCOLO_AUTO_REMOTE_NBD_BASE = 11809;
    private static final String LAST_ERROR_FT_UNSUPPORTED_MACHINE_TYPE = "ft_unsupported_machine_type";
    private static final String LAST_ERROR_CLOUD_MANAGED_FAILOVER_CANDIDATE = "cloud_managed_failover_candidate";
    private static final String LAST_ERROR_CLOUD_MANAGED_FAILOVER_SUSPECT = "cloud_managed_failover_suspect";
    private static final String LAST_ERROR_CLOUD_MANAGED_FAILOVER_MANUAL_REQUIRED = "cloud_managed_failover_manual_required";
    private static final String FENCING_RESULT_MANUAL_REQUIRED = "manual-required";
    private static final String FENCING_RESULT_IPMI_CONFIRMED = "ipmi_confirmed_fenced";
    private static final String FENCING_RESULT_IPMI_UNKNOWN_ASSUMED = "ipmi_unknown_assumed_fenced";
    private static final String FENCING_RESULT_IPMI_FAILED_ASSUMED = "ipmi_failed_assumed_fenced";
    private static final String[] LEGACY_PROGRESS_DETAIL_KEYS = {
            "ftctl.sync.progress.percent",
            "ftctl.sync.copied.bytes",
            "ftctl.sync.total.bytes",
            "ftctl.sync.ready",
            "ftctl.sync.direction",
            "ftctl.sync.updated",
            "ftctl.sync.progress.json",
            "ftctl.sync.progress.event.bucket",
            "ftctl.failover.ready.sync.percent",
            "ftctl.failover.ready.sync.json"
    };
    private static final ConcurrentMap<String, Object> VM_DETAIL_LOCKS = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Object> cloudManagedFailbackCutbackLocks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, FailbackOperationContext> cloudManagedFailbackContexts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Object> cloudManagedFailoverLocks = new ConcurrentHashMap<>();
    private final AtomicBoolean runtimeStateSyncRunning = new AtomicBoolean(false);
    private final AtomicBoolean cloudManagedFailoverRunning = new AtomicBoolean(false);
    private Timer cloudManagedFailbackMonitorTimer;
    private Timer cloudManagedFailoverMonitorTimer;
    private Timer runtimeStateSyncTimer;

    @Inject
    private VMInstanceDetailsDao vmInstanceDetailsDao;
    @Inject
    private UserVmDao userVmDao;
    @Inject
    private NicDao nicDao;
    @Inject
    private UserVmManager userVmManager;
    @Inject
    private UserVmService userVmService;
    @Inject
    private AgentManager agentManager;
    @Inject
    private HostDetailsDao hostDetailsDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    private OutOfBandManagementDao outOfBandManagementDao;
    @Inject
    private OutOfBandManagementService outOfBandManagementService;
    @Inject
    private FtctlProtectionProvisioningService ftctlProtectionProvisioningService;
    @Inject
    private FtctlProtectionDao ftctlProtectionDao;
    @Inject
    private FtctlProtectionVolumeDao ftctlProtectionVolumeDao;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    private VolumeApiService volumeApiService;
    @Inject
    private EventDao eventDao;
    @Inject
    private ServiceOfferingDao serviceOfferingDao;

    @Override
    public boolean start() {
        if (cloudManagedFailbackMonitorTimer == null) {
            TimerTask monitorTask = new ManagedContextTimerTask() {
                @Override
                protected void runInContext() {
                    try {
                        reconcileCloudManagedFailbacks();
                    } catch (RuntimeException e) {
                        logger.warn("Unable to run FTCTL cloud-managed failback monitor", e);
                    }
                }
            };
            cloudManagedFailbackMonitorTimer = new Timer("FtctlCloudManagedFailbackMonitor", true);
            cloudManagedFailbackMonitorTimer.schedule(monitorTask,
                    CLOUD_MANAGED_FAILBACK_MONITOR_DELAY_MILLIS,
                    CLOUD_MANAGED_FAILBACK_MONITOR_INTERVAL_MILLIS);
        }
        if (runtimeStateSyncTimer == null && FtctlRuntimeStateSyncEnabled.value()) {
            TimerTask syncTask = new ManagedContextTimerTask() {
                @Override
                protected void runInContext() {
                    syncActiveProtectionRuntimeStates();
                }
            };
            long intervalMillis = Math.max(MIN_RUNTIME_STATE_SYNC_INTERVAL_MILLIS,
                    FtctlRuntimeStateSyncInterval.value() * 1000L);
            runtimeStateSyncTimer = new Timer("FtctlRuntimeStateSync", true);
            runtimeStateSyncTimer.schedule(syncTask, RUNTIME_STATE_SYNC_DELAY_MILLIS, intervalMillis);
        }
        if (cloudManagedFailoverMonitorTimer == null && FtctlCloudManagedFailoverMonitorEnabled.value()) {
            TimerTask monitorTask = new ManagedContextTimerTask() {
                @Override
                protected void runInContext() {
                    reconcileCloudManagedFailovers();
                }
            };
            long intervalMillis = Math.max(MIN_CLOUD_MANAGED_FAILOVER_MONITOR_INTERVAL_MILLIS,
                    FtctlCloudManagedFailoverMonitorInterval.value() * 1000L);
            cloudManagedFailoverMonitorTimer = new Timer("FtctlCloudManagedFailoverMonitor", true);
            cloudManagedFailoverMonitorTimer.schedule(monitorTask, CLOUD_MANAGED_FAILOVER_MONITOR_DELAY_MILLIS, intervalMillis);
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (cloudManagedFailbackMonitorTimer != null) {
            cloudManagedFailbackMonitorTimer.cancel();
            cloudManagedFailbackMonitorTimer = null;
        }
        if (runtimeStateSyncTimer != null) {
            runtimeStateSyncTimer.cancel();
            runtimeStateSyncTimer = null;
        }
        if (cloudManagedFailoverMonitorTimer != null) {
            cloudManagedFailoverMonitorTimer.cancel();
            cloudManagedFailoverMonitorTimer = null;
        }
        cloudManagedFailbackContexts.clear();
        return true;
    }

    @Override
    public FtctlProtectionResponse getFtctlProtection(GetFtctlProtectionCmd cmd) throws CloudRuntimeException {
        UserVmVO userVm = validateVirtualMachineExists(cmd.getVirtualMachineId());
        FtctlProtectionResponse response = buildProtectionResponse(userVm);
        response.setObjectName("ftctlprotection");
        return response;
    }

    @Override
    public FtctlProtectionResponse registerFtctlProtection(RegisterFtctlProtectionCmd cmd) throws CloudRuntimeException {
        UserVmVO userVm = validateVirtualMachineExists(cmd.getVirtualMachineId());
        validatePrimaryProtectionTarget(userVm);
        validateProtectionRegistrationVmState(userVm);
        clearClosedReplicaRecoverySessionBeforeRegister(userVm.getId());
        validateRegisterRequest(cmd);
        boolean remoteMoldDr = isRemoteMoldDr(cmd);
        StoragePoolVO targetStoragePool = validateTargetStoragePool(cmd, userVm);
        String backendMode = resolveBackendMode(cmd, targetStoragePool);
        String targetStorageScope = resolveTargetStorageScope(cmd, targetStoragePool, backendMode);
        String provisioningBackend = resolveProvisioningBackend(cmd);
        String secondaryTargetDir = resolveSecondaryTargetDir(cmd, targetStoragePool, backendMode);
        String remoteNbdExportAddr = resolveRemoteNbdExportAddr(cmd, backendMode);
        XcoloPortAllocation xcoloPortAllocation = resolveXcoloPortAllocation(cmd, userVm, backendMode, remoteNbdExportAddr);
        remoteNbdExportAddr = xcoloPortAllocation.remoteNbdExportAddr;
        validateResolvedRegisterRequest(cmd, backendMode, targetStorageScope, secondaryTargetDir, remoteNbdExportAddr);
        validateDrCloudManagedTargetNetworks(cmd, provisioningBackend);
        validateFtMachineTypeContractBeforeProvisioning(userVm, cmd);
        FtctlIpmiFencingConfig ipmiFencingConfig = resolveIpmiFencingConfig(userVm, cmd);
        String remoteDrSshKeyFile = null;
        if (remoteMoldDr) {
            if (Boolean.TRUE.equals(cmd.getRemotePeerSshAutoSetup())) {
                remoteDrSshKeyFile = prepareRemoteDrSshAccess(userVm,
                        cmd.getRemoteMoldApiUrl(), cmd.getRemoteMoldApiKey(), cmd.getRemoteMoldSecretKey(),
                        cmd.getRemotePeerHostUuid(), cmd.getRemotePeerHostAddress(), cmd.getRemotePeerSshUser(),
                        cmd.getRemotePeerSshPort(), cmd.getRemotePeerLibvirtUri(), secondaryTargetDir, remoteNbdExportAddr);
            } else {
                validateRemoteExecutionPath(userVm, cmd, secondaryTargetDir, remoteNbdExportAddr, null);
            }
        }
        FtctlProtectionProvisioningContext provisioningContext;
        try {
            provisioningContext = prepareProtection(userVm, cmd, targetStoragePool, backendMode, provisioningBackend);
        } catch (CloudRuntimeException e) {
            persistProvisioningFailure(cmd.getVirtualMachineId(), provisioningBackend, e);
            throw e;
        }

        putVmDetail(cmd.getVirtualMachineId(), DETAIL_ENABLED, String.valueOf(true));
        putVmDetail(cmd.getVirtualMachineId(), DETAIL_MODE, cmd.getMode());
        if (backendMode != null) {
            putVmDetail(cmd.getVirtualMachineId(), DETAIL_BACKEND_MODE, backendMode);
        }
        putVmDetail(cmd.getVirtualMachineId(), DETAIL_PROVISIONING_BACKEND, provisioningContext.getProvisioningBackend());
        putVmDetail(cmd.getVirtualMachineId(), DETAIL_PROVISIONING_STATE, provisioningContext.getProvisioningState());
        if (targetStorageScope != null) {
            putVmDetail(cmd.getVirtualMachineId(), DETAIL_TARGET_STORAGE_SCOPE, targetStorageScope);
        }
        if (targetStoragePool != null) {
            putVmDetail(cmd.getVirtualMachineId(), DETAIL_TARGET_STORAGE_POOL_ID, targetStoragePool.getUuid());
            putVmDetail(cmd.getVirtualMachineId(), DETAIL_TARGET_STORAGE_POOL_NAME, targetStoragePool.getName());
        }
        if (StringUtils.isNotBlank(cmd.getNetworkIds())) {
            putVmDetail(cmd.getVirtualMachineId(), DETAIL_TARGET_NETWORK_IDS, cmd.getNetworkIds());
        }
        if (remoteMoldDr) {
            persistRemoteMoldDrDetails(cmd);
        } else if ("dr".equalsIgnoreCase(cmd.getMode())) {
            putVmDetail(cmd.getVirtualMachineId(), DETAIL_DR_PEER_SITE_TYPE, DR_PEER_SITE_TYPE_LOCAL_MOLD);
        }
        if (cmd.getFencingPolicy() != null) {
            putVmDetail(cmd.getVirtualMachineId(), DETAIL_FENCING_POLICY, cmd.getFencingPolicy());
        }
        if (ipmiFencingConfig != null) {
            persistIpmiFencingDetails(cmd.getVirtualMachineId(), ipmiFencingConfig);
        }
        if (cmd.getPeerHostId() != null) {
            putVmDetail(cmd.getVirtualMachineId(), DETAIL_PEER_HOST_ID, String.valueOf(cmd.getPeerHostId()));
        }
        if (provisioningContext.getSecondaryVmName() != null) {
            putVmDetail(cmd.getVirtualMachineId(), DETAIL_SECONDARY_VM_NAME, provisioningContext.getSecondaryVmName());
        }
        if (secondaryTargetDir != null) {
            putVmDetail(cmd.getVirtualMachineId(), DETAIL_SECONDARY_TARGET_DIR, secondaryTargetDir);
        }
        if (remoteNbdExportAddr != null) {
            putVmDetail(cmd.getVirtualMachineId(), DETAIL_REMOTE_NBD_EXPORT_ADDR, remoteNbdExportAddr);
        }
        persistXcoloPortAllocation(userVm.getId(), xcoloPortAllocation);
        persistCloudManagedNicIdentities(userVm);

        Long hostId = getExecutionHostId(userVm);
        if (hostId != null) {
            boolean lifecycleGuardApplied = false;
            try {
                applyCloudManagedFtLifecycleGuard(userVm, provisioningContext);
                lifecycleGuardApplied = true;
                syncFtctlContext(userVm, cmd, targetStoragePool, targetStorageScope, backendMode, secondaryTargetDir, remoteNbdExportAddr, xcoloPortAllocation,
                        ipmiFencingConfig, provisioningContext, remoteDrSshKeyFile);
                String peerUri = remoteMoldDr ? resolveRemotePeerLibvirtUri(cmd) : resolvePeerUri(cmd.getPeerHostId());
                if (peerUri == null || peerUri.isBlank()) {
                    throw new CloudRuntimeException(String.format("Missing FTCTL peer libvirt URI on %s", remoteMoldDr ? "remote Mold host" : String.format("host %s", cmd.getPeerHostId())));
                }
                FtctlActionCommand actionCommand = new FtctlActionCommand(FtctlActionCommand.Action.PROTECT_START, userVm.getInstanceName());
                actionCommand.setMode(cmd.getMode());
                actionCommand.setPeerUri(peerUri);
                if (backendMode != null) {
                    actionCommand.setContextParam("ftctl.backend.mode", backendMode);
                }
                actionCommand.setContextParam("ftctl.provisioning.backend", provisioningContext.getProvisioningBackend());
                if (targetStorageScope != null) {
                    actionCommand.setContextParam("ftctl.target.storage.scope", targetStorageScope);
                }
                if (cmd.getFencingPolicy() != null) {
                    actionCommand.setContextParam("ftctl.fencing.policy", cmd.getFencingPolicy());
                }
                Answer answer = agentManager.send(hostId, actionCommand);
                if (!(answer instanceof FtctlActionAnswer) || !answer.getResult()) {
                    throw new CloudRuntimeException(String.format("Unable to start FTCTL protection job for VM %s: %s",
                            userVm.getUuid(), answer != null ? answer.getDetails() : "no answer"));
                }
            } catch (CloudRuntimeException e) {
                if (lifecycleGuardApplied) {
                    releaseCloudManagedFtLifecycleGuard(userVm);
                }
                persistRegistrationFailure(userVm, e);
                throw e;
            } catch (AgentUnavailableException | OperationTimedoutException e) {
                CloudRuntimeException failure = new CloudRuntimeException(String.format("Unable to execute FTCTL protection on host %s for VM %s",
                        hostId, userVm.getUuid()), e);
                if (lifecycleGuardApplied) {
                    releaseCloudManagedFtLifecycleGuard(userVm);
                }
                persistRegistrationFailure(userVm, failure);
                throw failure;
            }
        }

        FtctlProtectionResponse response = buildProtectionResponse(userVm);
        populateRuntimeStateFromAgent(userVm, response, true);
        publishFtctlEvent(userVm, EventTypes.EVENT_FTCTL_PROTECTION_REGISTER,
                String.format("Registered FTCTL protection for VM %s", userVm.getUuid()));
        response.setObjectName("ftctlprotection");
        return response;
    }

    @Override
    public FtctlRemoteMoldConnectionResponse validateFtctlRemoteMoldConnection(ValidateFtctlRemoteMoldConnectionCmd cmd) throws CloudRuntimeException {
        JsonObject response = callRemoteMoldApi(cmd.getRemoteMoldApiUrl(), cmd.getRemoteMoldApiKey(), cmd.getRemoteMoldSecretKey(),
                "listCapabilities", Collections.emptyMap());
        FtctlRemoteMoldConnectionResponse result = new FtctlRemoteMoldConnectionResponse();
        result.setObjectName("ftctlremotemoldconnection");
        boolean success = response != null && response.has("listcapabilitiesresponse");
        result.setSuccess(success);
        result.setMessage(success ? "OK" : "Unable to validate remote Mold connection");
        return result;
    }

    @Override
    public FtctlRemoteMoldHostsResponse listFtctlRemoteMoldHosts(ListFtctlRemoteMoldHostsCmd cmd) throws CloudRuntimeException {
        Map<String, String> params = new HashMap<>();
        params.put("type", "Routing");
        params.put("state", "Up");
        params.put("details", "all");
        params.put("listall", "true");
        params.put("page", "1");
        params.put("pagesize", "500");
        putIfNotBlank(params, "zoneid", cmd.getZoneId());
        putIfNotBlank(params, "clusterid", cmd.getClusterId());
        JsonObject json = callRemoteMoldApi(cmd.getRemoteMoldApiUrl(), cmd.getRemoteMoldApiKey(), cmd.getRemoteMoldSecretKey(),
                "listHosts", params);
        JsonArray array = getResponseArray(json, "listhostsresponse", "host");
        List<FtctlRemoteMoldHostResponse> hosts = new ArrayList<>();
        if (array != null) {
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                if (!"KVM".equalsIgnoreCase(getJsonString(object, "hypervisor"))) {
                    continue;
                }
                FtctlRemoteMoldHostResponse host = new FtctlRemoteMoldHostResponse();
                host.setObjectName("host");
                host.setId(getJsonString(object, "id"));
                host.setName(getJsonString(object, "name"));
                host.setIpAddress(getJsonString(object, "ipaddress"));
                host.setMigrationIp(getJsonString(object, "migrationip"));
                host.setClusterId(getJsonString(object, "clusterid"));
                host.setClusterName(getJsonString(object, "clustername"));
                host.setZoneId(getJsonString(object, "zoneid"));
                host.setZoneName(getJsonString(object, "zonename"));
                host.setHypervisor(getJsonString(object, "hypervisor"));
                hosts.add(host);
            }
        }
        FtctlRemoteMoldHostsResponse response = new FtctlRemoteMoldHostsResponse();
        response.setObjectName("ftctlremotemoldhosts");
        response.setHosts(hosts);
        return response;
    }

    @Override
    public FtctlRemoteMoldNetworksResponse listFtctlRemoteMoldNetworks(ListFtctlRemoteMoldNetworksCmd cmd) throws CloudRuntimeException {
        Map<String, String> params = new HashMap<>();
        params.put("listall", "true");
        params.put("page", "1");
        params.put("pagesize", "500");
        params.put("canusefordeploy", "true");
        params.put("traffictype", "Guest");
        params.put("type", "all");
        params.put("networkfilter", "all");
        putIfNotBlank(params, "zoneid", cmd.getZoneId());
        JsonObject json = callRemoteMoldApi(cmd.getRemoteMoldApiUrl(), cmd.getRemoteMoldApiKey(), cmd.getRemoteMoldSecretKey(),
                "listNetworks", params);
        JsonArray array = getResponseArray(json, "listnetworksresponse", "network");
        List<FtctlRemoteMoldNetworkResponse> networks = new ArrayList<>();
        if (array != null) {
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                FtctlRemoteMoldNetworkResponse network = new FtctlRemoteMoldNetworkResponse();
                network.setObjectName("network");
                network.setId(getJsonString(object, "id"));
                network.setName(getJsonString(object, "name"));
                network.setDisplayText(getJsonString(object, "displaytext"));
                network.setZoneId(getJsonString(object, "zoneid"));
                network.setZoneName(getJsonString(object, "zonename"));
                network.setType(getJsonString(object, "type"));
                network.setTrafficType(getJsonString(object, "traffictype"));
                network.setState(getJsonString(object, "state"));
                network.setCidr(getJsonString(object, "cidr"));
                networks.add(network);
            }
        }
        FtctlRemoteMoldNetworksResponse response = new FtctlRemoteMoldNetworksResponse();
        response.setObjectName("ftctlremotemoldnetworks");
        response.setNetworks(networks);
        return response;
    }

    @Override
    public FtctlRemoteMoldStoragePoolsResponse listFtctlRemoteMoldStoragePools(ListFtctlRemoteMoldStoragePoolsCmd cmd) throws CloudRuntimeException {
        Map<String, String> params = new HashMap<>();
        params.put("listall", "true");
        params.put("page", "1");
        params.put("pagesize", "500");
        putIfNotBlank(params, "zoneid", cmd.getZoneId());
        putIfNotBlank(params, "clusterid", cmd.getClusterId());
        putIfNotBlank(params, "hostid", cmd.getHostId());
        JsonObject json = callRemoteMoldApi(cmd.getRemoteMoldApiUrl(), cmd.getRemoteMoldApiKey(), cmd.getRemoteMoldSecretKey(),
                "listStoragePools", params);
        JsonArray array = getResponseArray(json, "liststoragepoolsresponse", "storagepool");
        List<FtctlRemoteMoldStoragePoolResponse> storagePools = new ArrayList<>();
        if (array != null) {
            for (JsonElement element : array) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                String state = getJsonString(object, "state");
                if (StringUtils.isNotBlank(state) && !"Up".equalsIgnoreCase(state)) {
                    continue;
                }
                FtctlRemoteMoldStoragePoolResponse storagePool = new FtctlRemoteMoldStoragePoolResponse();
                storagePool.setObjectName("storagepool");
                storagePool.setId(getJsonString(object, "id"));
                storagePool.setName(getJsonString(object, "name"));
                storagePool.setPath(StringUtils.defaultIfBlank(getJsonString(object, "path"), getJsonString(object, "url")));
                storagePool.setScope(getJsonString(object, "scope"));
                storagePool.setType(StringUtils.defaultIfBlank(getJsonString(object, "type"),
                        StringUtils.defaultIfBlank(getJsonString(object, "storagetype"), getJsonString(object, "pooltype"))));
                storagePool.setClusterId(getJsonString(object, "clusterid"));
                storagePool.setClusterName(getJsonString(object, "clustername"));
                storagePool.setState(state);
                storagePools.add(storagePool);
            }
        }
        FtctlRemoteMoldStoragePoolsResponse response = new FtctlRemoteMoldStoragePoolsResponse();
        response.setObjectName("ftctlremotemoldstoragepools");
        response.setStoragePools(storagePools);
        return response;
    }

    @Override
    public FtctlDrReplicaResourcesResponse prepareFtctlDrReplicaResources(PrepareFtctlDrReplicaResourcesCmd cmd) throws CloudRuntimeException {
        return ftctlProtectionProvisioningService.prepareDrReplicaResources(cmd);
    }

    @Override
    public FtctlActionResponse prepareFtctlDrRemoteSshAccess(PrepareFtctlDrRemoteSshAccessCmd cmd) throws CloudRuntimeException {
        UserVmVO userVm = validateVirtualMachineExists(cmd.getVirtualMachineId());
        validateProtectionRegistrationVmState(userVm);
        prepareRemoteDrSshAccess(userVm, cmd.getRemoteMoldApiUrl(), cmd.getRemoteMoldApiKey(), cmd.getRemoteMoldSecretKey(),
                cmd.getRemotePeerHostUuid(), cmd.getRemotePeerHostAddress(), cmd.getRemotePeerSshUser(),
                cmd.getRemotePeerSshPort(), cmd.getRemotePeerLibvirtUri(), cmd.getSecondaryTargetDir(), cmd.getRemoteNbdExportAddr());

        FtctlActionResponse response = new FtctlActionResponse();
        response.setObjectName("ftctlaction");
        response.setVirtualMachineId(userVm.getId());
        response.setVmName(userVm.getInstanceName());
        response.setAction("prepare-dr-remote-ssh-access");
        response.setResult("ok");
        response.setExitCode(0);
        response.setOutput("FTCTL DR remote SSH access prepared");
        return response;
    }

    private String prepareRemoteDrSshAccess(UserVmVO userVm, String remoteMoldApiUrl, String remoteMoldApiKey, String remoteMoldSecretKey,
                                            String remotePeerHostUuid, String remotePeerHostAddress, String remotePeerSshUser,
                                            String remotePeerSshPort, String remotePeerLibvirtUri,
                                            String secondaryTargetDir, String remoteNbdExportAddr) {
        if (StringUtils.isAnyBlank(remotePeerHostUuid, remotePeerHostAddress)) {
            throw new CloudRuntimeException("FTCTL DR remote SSH preparation requires remote peer host UUID and address");
        }
        String resolvedSshPort = StringUtils.defaultIfBlank(StringUtils.trimToNull(remotePeerSshPort), DEFAULT_REMOTE_PEER_SSH_PORT);
        validateSshPort(resolvedSshPort);
        String resolvedSshUser = StringUtils.defaultIfBlank(StringUtils.trimToNull(remotePeerSshUser), DEFAULT_REMOTE_PEER_SSH_USER);
        String peerLibvirtUri = resolveRemotePeerLibvirtUri(remotePeerLibvirtUri, remotePeerHostAddress, resolvedSshUser, resolvedSshPort);
        Long sourceHostId = getExecutionHostId(userVm);
        if (sourceHostId == null) {
            throw new CloudRuntimeException(String.format("Unable to resolve source host for FTCTL DR SSH preparation on VM %s", userVm.getUuid()));
        }

        FtctlDrSshAccessCommand ensureCommand = new FtctlDrSshAccessCommand(FtctlDrSshAccessCommand.Action.ENSURE_KEY, userVm.getInstanceName());
        FtctlSyncAnswer ensureAnswer = executeDrSshAccessCommand(sourceHostId, ensureCommand,
                String.format("Unable to prepare local FTCTL DR SSH key on host %s for VM %s", sourceHostId, userVm.getUuid()));
        JsonObject keyJson = parseFirstJsonObject(ensureAnswer.getOutput());
        String publicKey = getJsonString(keyJson, "public_key");
        String keyComment = getJsonString(keyJson, "key_comment");
        String privateKeyPath = getJsonString(keyJson, "private_key_path");
        if (StringUtils.isAnyBlank(publicKey, keyComment, privateKeyPath)) {
            throw new CloudRuntimeException(String.format("FTCTL DR SSH key preparation did not return a usable public key: %s",
                    ensureAnswer.getOutput()));
        }

        Map<String, String> params = new HashMap<>();
        params.put("hostid", remotePeerHostUuid);
        params.put("profile", userVm.getInstanceName());
        params.put("publickey", publicKey);
        params.put("keycomment", keyComment);
        params.put("sshuser", resolvedSshUser);
        params.put("applyfirewall", "true");
        callRemoteMoldApi(remoteMoldApiUrl, remoteMoldApiKey, remoteMoldSecretKey,
                InstallFtctlDrRemoteSshKeyCmd.APINAME, params);

        FtctlRemotePreflightCommand preflightCommand = new FtctlRemotePreflightCommand(
                userVm.getInstanceName(), "dr", peerLibvirtUri);
        preflightCommand.setSecondaryTargetDir(secondaryTargetDir);
        preflightCommand.setSecondarySshKeyFile(privateKeyPath);
        preflightCommand.setRemoteNbdExportAddr(remoteNbdExportAddr);
        try {
            Answer answer = agentManager.send(sourceHostId, preflightCommand);
            if (!(answer instanceof FtctlSyncAnswer) || !answer.getResult()) {
                throw new CloudRuntimeException(String.format("FTCTL DR remote SSH preparation preflight failed for VM %s on host %s: %s",
                        userVm.getUuid(), sourceHostId, answer != null ? answer.getDetails() : "no answer"));
            }
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException(String.format("Unable to execute FTCTL DR remote SSH preparation preflight on host %s for VM %s",
                    sourceHostId, userVm.getUuid()), e);
        }

        return privateKeyPath;
    }

    @Override
    public FtctlActionResponse installFtctlDrRemoteSshKey(InstallFtctlDrRemoteSshKeyCmd cmd) throws CloudRuntimeException {
        HostVO host = hostDao.findById(cmd.getHostId());
        if (host == null) {
            throw new CloudRuntimeException(String.format("FTCTL DR remote SSH key install target host not found: %s", cmd.getHostId()));
        }
        FtctlDrSshAccessCommand installCommand = new FtctlDrSshAccessCommand(FtctlDrSshAccessCommand.Action.INSTALL_KEY, cmd.getProfile());
        installCommand.setPublicKey(cmd.getPublicKey());
        installCommand.setKeyComment(cmd.getKeyComment());
        installCommand.setSshUser(StringUtils.defaultIfBlank(StringUtils.trimToNull(cmd.getSshUser()), DEFAULT_REMOTE_PEER_SSH_USER));
        installCommand.setApplyFirewall(Boolean.TRUE.equals(cmd.getApplyFirewall()));
        FtctlSyncAnswer answer = executeDrSshAccessCommand(host.getId(), installCommand,
                String.format("Unable to install FTCTL DR SSH key on host %s", host.getUuid()));

        FtctlActionResponse response = new FtctlActionResponse();
        response.setObjectName("ftctlaction");
        response.setAction("install-dr-remote-ssh-key");
        response.setResult(StringUtils.defaultIfBlank(answer.getFtctlResult(), answer.getResult() ? "ok" : "fail"));
        response.setExitCode(answer.getExitCode());
        response.setOutput(answer.getOutput());
        return response;
    }

    private void putIfNotBlank(Map<String, String> params, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            params.put(key, value);
        }
    }

    private JsonArray getResponseArray(JsonObject json, String responseKey, String arrayKey) {
        if (json == null || !json.has(responseKey) || !json.get(responseKey).isJsonObject()) {
            return null;
        }
        JsonObject response = json.getAsJsonObject(responseKey);
        if (!response.has(arrayKey) || !response.get(arrayKey).isJsonArray()) {
            return null;
        }
        return response.getAsJsonArray(arrayKey);
    }

    private JsonObject getJsonObject(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull() || !object.get(key).isJsonObject()) {
            return null;
        }
        return object.getAsJsonObject(key);
    }

    private JsonObject callRemoteMoldApi(String apiUrl, String apiKey, String secretKey, String command, Map<String, String> params) {
        if (StringUtils.isAnyBlank(apiUrl, apiKey, secretKey, command)) {
            throw new CloudRuntimeException("Remote Mold API URL, API key, secret key, and command are required");
        }
        try {
            String requestUrl = buildRemoteMoldSignedUrl(apiUrl, apiKey, secretKey, command, params);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new CloudRuntimeException(String.format("Remote Mold API %s failed with HTTP %s", command, response.statusCode()));
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject error = findRemoteMoldError(json);
            if (error != null) {
                String message = StringUtils.defaultIfBlank(getJsonString(error, "errortext"), getJsonString(error, "errorcode"));
                throw new CloudRuntimeException(String.format("Remote Mold API %s failed: %s", command, message));
            }
            return json;
        } catch (CloudRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CloudRuntimeException(String.format("Unable to call remote Mold API %s: %s", command, e.getMessage()), e);
        }
    }

    private JsonObject findRemoteMoldError(JsonObject json) {
        if (json == null) {
            return null;
        }
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (entry.getKey().endsWith("response") && entry.getValue().isJsonObject()) {
                JsonObject object = entry.getValue().getAsJsonObject();
                if (object.has("errorcode") || object.has("errortext")) {
                    return object;
                }
            }
        }
        return null;
    }

    private String buildRemoteMoldSignedUrl(String apiUrl, String apiKey, String secretKey, String command, Map<String, String> params)
            throws UnsupportedEncodingException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("command", command);
        requestParams.put("response", "json");
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue())) {
                    requestParams.put(entry.getKey(), entry.getValue());
                }
            }
        }
        requestParams.put("apiKey", apiKey);
        String apiParams = buildQueryString(requestParams, false);
        String signatureBase = buildQueryString(requestParams, true);
        String signature = signRemoteMoldRequest(signatureBase, secretKey);
        String separator = apiUrl.contains("?") ? "&" : "?";
        return apiUrl + separator + apiParams + "&signature=" + encode(signature);
    }

    private String buildQueryString(Map<String, String> params, boolean lowerCaseForSignature) throws UnsupportedEncodingException {
        List<Map.Entry<String, String>> entries = new ArrayList<>(params.entrySet());
        entries.sort(Comparator.comparing(entry -> entry.getKey().toLowerCase(Locale.ROOT)));
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : entries) {
            parts.add(entry.getKey() + "=" + encode(entry.getValue()));
        }
        String queryString = StringUtils.join(parts, "&");
        return lowerCaseForSignature ? queryString.toLowerCase(Locale.ROOT) : queryString;
    }

    private String signRemoteMoldRequest(String request, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(request.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to sign remote Mold API request", e);
        }
    }

    private String encode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
    }

    @Override
    public FtctlCheckResponse getFtctlCheck(GetFtctlCheckCmd cmd) throws CloudRuntimeException {
        UserVmVO requestedVm = validateVirtualMachineExists(cmd.getVirtualMachineId());
        if (isRemoteMoldReplicaStandbyVm(requestedVm.getId())) {
            return buildRemoteMoldStandbyCheckResponse(requestedVm);
        }
        FtctlProtectionVO protection = findActiveProtectionForVm(requestedVm.getId());
        UserVmVO runtimeVm = resolveRuntimeVmForProtectionView(requestedVm);
        Long sourceVmId = protection != null ? protection.getPrimaryVmId() : runtimeVm.getId();
        List<FtctlEventResponse> events = fetchRuntimeEventResponses(runtimeVm, 100);
        FtctlEventResponse latestInventoryCheck = findLatestEvent(events, "inventory.check");
        JsonObject latestInventoryCheckDetails = parseJsonObject(latestInventoryCheck != null ? latestInventoryCheck.getDetails() : null);
        FtctlEventResponse latestInventoryDisks = findLatestEvent(events, "inventory.disks");
        FtctlCheckResponse response = new FtctlCheckResponse();
        response.setObjectName("ftctlcheck");
        response.setVirtualMachineId(requestedVm.getId());
        response.setVmName(runtimeVm.getInstanceName());
        response.setResult(StringUtils.defaultIfBlank(latestInventoryCheck != null ? latestInventoryCheck.getResult() : null,
                StringUtils.defaultIfBlank(getDetailValue(sourceVmId, DETAIL_CHECK_RESULT), "not_available")));
        response.setInventoryResult(StringUtils.defaultIfBlank(latestInventoryDisks != null ? latestInventoryDisks.getResult() : null,
                StringUtils.defaultIfBlank(getDetailValue(sourceVmId, DETAIL_CHECK_INVENTORY_RESULT), "unknown")));
        response.setPrimaryRc(getJsonIntegerOrDefault(latestInventoryCheckDetails, "primary_rc", parseIntegerDetail(getDetailValue(sourceVmId, DETAIL_CHECK_PRIMARY_RC))));
        response.setPrimaryDomainState(StringUtils.defaultIfBlank(getJsonString(latestInventoryCheckDetails, "primary_domain_state"), null));
        response.setPeerRc(getJsonIntegerOrDefault(latestInventoryCheckDetails, "peer_rc", parseIntegerDetail(getDetailValue(sourceVmId, DETAIL_CHECK_PEER_RC))));
        response.setPeerDomainExpected(getJsonBooleanOrDefault(latestInventoryCheckDetails, "peer_domain_expected",
                parseBooleanDetail(getDetailValue(sourceVmId, DETAIL_CHECK_PEER_DOMAIN_EXPECTED))));
        response.setStandbyDomainState(StringUtils.defaultIfBlank(getJsonString(latestInventoryCheckDetails, "standby_domain_state"),
                getDetailValue(sourceVmId, DETAIL_CHECK_STANDBY_DOMAIN_STATE)));
        response.setProvisioningBackend(resolveFtctlCheckProvisioningBackend(protection, sourceVmId));
        return response;
    }

    private String resolveFtctlCheckSecondaryVmName(FtctlProtectionVO protection, Long sourceVmId) {
        if (protection == null) {
            return getDetailValue(sourceVmId, DETAIL_SECONDARY_VM_NAME);
        }
        String secondaryVmName = protection.getSecondaryVmName();
        if (StringUtils.isBlank(secondaryVmName) && protection.getSecondaryVmId() != null) {
            UserVmVO secondaryVm = userVmDao.findById(protection.getSecondaryVmId());
            secondaryVmName = secondaryVm != null ? secondaryVm.getInstanceName() : null;
        }
        return StringUtils.defaultIfBlank(secondaryVmName, getDetailValue(sourceVmId, DETAIL_SECONDARY_VM_NAME));
    }

    private String resolveFtctlCheckActiveSide(FtctlProtectionVO protection, Long sourceVmId) {
        return StringUtils.defaultIfBlank(protection != null ? protection.getActiveSide() : null,
                getDetailValue(sourceVmId, DETAIL_LAST_ACTIVE_SIDE));
    }

    private String resolveFtctlCheckProvisioningBackend(FtctlProtectionVO protection, Long sourceVmId) {
        return StringUtils.defaultIfBlank(protection != null ? protection.getProvisioningBackend() : null,
                getDetailValue(sourceVmId, DETAIL_PROVISIONING_BACKEND));
    }

    @Override
    public FtctlEventsResponse getFtctlEvents(GetFtctlEventsCmd cmd) throws CloudRuntimeException {
        UserVmVO requestedVm = validateVirtualMachineExists(cmd.getVirtualMachineId());
        if (isRemoteMoldReplicaStandbyVm(requestedVm.getId())) {
            return buildRemoteMoldStandbyEventsResponse(requestedVm);
        }
        UserVmVO runtimeVm = resolveRuntimeVmForProtectionView(requestedVm);
        FtctlEventsAnswer eventsAnswer = fetchRuntimeEvents(runtimeVm, cmd.getLimit());
        List<FtctlEventResponse> events = eventsAnswer != null ? parseEvents(eventsAnswer.getItemsJson()) : Collections.emptyList();
        JsonObject latestProgress = findLatestProgress(events);
        FtctlEventsResponse response = new FtctlEventsResponse();
        response.setObjectName("ftctlevents");
        response.setVirtualMachineId(requestedVm.getId());
        response.setVmName(eventsAnswer != null && StringUtils.isNotBlank(eventsAnswer.getVmName())
                ? eventsAnswer.getVmName() : runtimeVm.getInstanceName());
        response.setResult(eventsAnswer != null ? StringUtils.defaultIfBlank(eventsAnswer.getFtctlResult(), "ok") : "not_available");
        response.setCount(events.size());
        response.setEvents(events);
        applyLatestProgress(response, latestProgress);
        return response;
    }

    private void validateRegisterRequest(RegisterFtctlProtectionCmd cmd) {
        if (cmd.getMode() == null || cmd.getMode().isBlank()) {
            throw new CloudRuntimeException("FTCTL mode is required");
        }
        if (hasRemoteMoldParameters(cmd) && !"dr".equalsIgnoreCase(cmd.getMode())) {
            throw new CloudRuntimeException("FTCTL remote Mold parameters are allowed only for DR mode");
        }
        boolean remoteMoldDr = isRemoteMoldDr(cmd);
        if (remoteMoldDr) {
            validateRemoteMoldDrRegisterRequest(cmd);
            return;
        }
        if ("dr".equalsIgnoreCase(cmd.getMode()) && hasRemoteMoldParameters(cmd)) {
            throw new CloudRuntimeException("FTCTL DR remote Mold parameters require drpeersitetype=remote-mold");
        }
        if (isProtectionModeWithTargetStorage(cmd.getMode()) && cmd.getTargetStoragePoolId() == null) {
            throw new CloudRuntimeException("FTCTL target primary storage pool is required for HA/DR/FT");
        }
        if (("ha".equalsIgnoreCase(cmd.getMode()) || "dr".equalsIgnoreCase(cmd.getMode()) || "ft".equalsIgnoreCase(cmd.getMode()))
                && cmd.getPeerHostId() == null) {
            throw new CloudRuntimeException("FTCTL peer host is required for local Mold HA/DR/FT");
        }
        if ("ft".equalsIgnoreCase(cmd.getMode()) && isManualXcoloPortAllocation(cmd) &&
                (isBlank(cmd.getXcoloProxyEndpoint()) || isBlank(cmd.getXcoloNbdEndpoint()) || isBlank(cmd.getXcoloMigrateUri()))) {
            throw new CloudRuntimeException("FTCTL FT mode requires x-colo proxy, NBD, and migrate fields");
        }
    }

    private void validateResolvedRegisterRequest(RegisterFtctlProtectionCmd cmd, String backendMode, String targetStorageScope,
                                                String secondaryTargetDir, String remoteNbdExportAddr) {
        if (isRemoteMoldDr(cmd) && !"remote-nbd".equalsIgnoreCase(backendMode)) {
            throw new CloudRuntimeException("FTCTL DR remote Mold requires remote-nbd backend mode");
        }
        if ("remote-nbd".equalsIgnoreCase(backendMode) && !"secondary-local".equalsIgnoreCase(targetStorageScope)) {
            throw new CloudRuntimeException("FTCTL remote-nbd requires target storage scope secondary-local");
        }
        if ("shared-blockcopy".equalsIgnoreCase(backendMode) && !"shared".equalsIgnoreCase(targetStorageScope)) {
            throw new CloudRuntimeException("FTCTL shared-blockcopy requires target storage scope shared");
        }
        if ("remote-nbd".equalsIgnoreCase(backendMode) && (isBlank(secondaryTargetDir) || isBlank(remoteNbdExportAddr))) {
            throw new CloudRuntimeException("FTCTL remote-nbd requires secondary target directory and export address");
        }
        if ("ft".equalsIgnoreCase(cmd.getMode()) && cmd.getTargetStoragePoolId() == null) {
            throw new CloudRuntimeException("FTCTL FT mode requires a target primary storage pool");
        }
    }

    private void validateDrCloudManagedTargetNetworks(RegisterFtctlProtectionCmd cmd, String provisioningBackend) {
        if (!"dr".equalsIgnoreCase(cmd.getMode()) ||
                !FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED.equalsIgnoreCase(provisioningBackend)) {
            return;
        }
        if (StringUtils.isBlank(cmd.getNetworkIds())) {
            throw new CloudRuntimeException("FTCTL DR cloud-managed registration requires target network IDs for standby VM creation");
        }
    }

    private void validateFtMachineTypeContractBeforeProvisioning(UserVmVO userVm, RegisterFtctlProtectionCmd cmd) {
        if (!"ft".equalsIgnoreCase(cmd.getMode())) {
            return;
        }
        FtMachineTypeCompatibility compatibility = resolveFtMachineTypeCompatibility(userVm != null ? userVm.getId() : null);
        if (compatibility.compatible) {
            return;
        }
        String message = String.format("%s: %s", LAST_ERROR_FT_UNSUPPORTED_MACHINE_TYPE, compatibility.message);
        putVmDetail(userVm.getId(), DETAIL_LAST_ERROR, message);
        throw new CloudRuntimeException(message);
    }

    private boolean isRemoteMoldDr(RegisterFtctlProtectionCmd cmd) {
        return "dr".equalsIgnoreCase(cmd.getMode()) &&
                DR_PEER_SITE_TYPE_REMOTE_MOLD.equalsIgnoreCase(StringUtils.defaultIfBlank(cmd.getDrPeerSiteType(), DR_PEER_SITE_TYPE_LOCAL_MOLD));
    }

    private boolean hasRemoteMoldParameters(RegisterFtctlProtectionCmd cmd) {
        return DR_PEER_SITE_TYPE_REMOTE_MOLD.equalsIgnoreCase(StringUtils.trimToEmpty(cmd.getDrPeerSiteType())) ||
                hasAnyText(cmd.getRemoteMoldApiUrl(), cmd.getRemoteMoldApiKey(), cmd.getRemoteMoldSecretKey(),
                        cmd.getRemotePeerHostUuid(), cmd.getRemotePeerHostName(), cmd.getRemotePeerHostAddress(),
                        cmd.getRemotePeerHostBlockcopyAddress(), cmd.getRemotePeerSshUser(), cmd.getRemotePeerSshPort(),
                        cmd.getRemotePeerLibvirtUri(), cmd.getRemoteTargetStoragePoolUuid(),
                        cmd.getRemoteTargetStoragePoolName(), cmd.getRemoteTargetStoragePoolPath(), cmd.getRemoteTargetStoragePoolType());
    }

    private boolean hasAnyText(String... values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return true;
            }
        }
        return false;
    }

    private void validateRemoteMoldDrRegisterRequest(RegisterFtctlProtectionCmd cmd) {
        if (StringUtils.isAnyBlank(cmd.getRemoteMoldApiUrl(), cmd.getRemoteMoldApiKey(), cmd.getRemoteMoldSecretKey())) {
            throw new CloudRuntimeException("FTCTL DR remote Mold registration requires remote Mold API URL, API key, and secret key for Cloud-managed replica provisioning");
        }
        if (StringUtils.isAnyBlank(cmd.getRemotePeerHostUuid(), cmd.getRemotePeerHostAddress())) {
            throw new CloudRuntimeException("FTCTL DR remote Mold requires a resolved remote peer host UUID and address");
        }
        if (StringUtils.isAnyBlank(cmd.getRemoteTargetStoragePoolUuid(), cmd.getRemoteTargetStoragePoolPath())) {
            throw new CloudRuntimeException("FTCTL DR remote Mold requires a resolved remote target storage pool UUID and path");
        }
        validateRemotePeerSshPort(cmd);
        resolveRemotePeerLibvirtUri(cmd);
        if (StringUtils.isNotBlank(cmd.getBackendMode()) && !"remote-nbd".equalsIgnoreCase(cmd.getBackendMode())) {
            throw new CloudRuntimeException("FTCTL DR remote Mold supports only remote-nbd backend mode");
        }
        if (StringUtils.isNotBlank(cmd.getTargetStorageScope()) && !"secondary-local".equalsIgnoreCase(cmd.getTargetStorageScope())) {
            throw new CloudRuntimeException("FTCTL DR remote Mold requires target storage scope secondary-local");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void validateRemotePeerSshPort(RegisterFtctlProtectionCmd cmd) {
        validateSshPort(resolveRemotePeerSshPort(cmd));
    }

    private void validateSshPort(String port) {
        try {
            int parsed = Integer.parseInt(port);
            if (parsed < 1 || parsed > 65535) {
                throw new NumberFormatException("out of range");
            }
        } catch (NumberFormatException e) {
            throw new CloudRuntimeException(String.format("FTCTL DR remote Mold SSH port must be a number from 1 to 65535: %s", port));
        }
    }

    private String resolveRemotePeerLibvirtUri(PrepareFtctlDrRemoteSshAccessCmd cmd) {
        String sshUser = StringUtils.defaultIfBlank(StringUtils.trimToNull(cmd.getRemotePeerSshUser()), DEFAULT_REMOTE_PEER_SSH_USER);
        String sshPort = StringUtils.defaultIfBlank(StringUtils.trimToNull(cmd.getRemotePeerSshPort()), DEFAULT_REMOTE_PEER_SSH_PORT);
        return resolveRemotePeerLibvirtUri(cmd.getRemotePeerLibvirtUri(), cmd.getRemotePeerHostAddress(), sshUser, sshPort);
    }

    private FtctlSyncAnswer executeDrSshAccessCommand(Long hostId, FtctlDrSshAccessCommand command, String failureMessage) {
        try {
            Answer answer = agentManager.send(hostId, command);
            if (!(answer instanceof FtctlSyncAnswer) || !answer.getResult()) {
                throw new CloudRuntimeException(String.format("%s: %s", failureMessage,
                        answer != null ? answer.getDetails() : "no answer"));
            }
            return (FtctlSyncAnswer) answer;
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException(failureMessage, e);
        }
    }

    private String resolveRemotePeerSshUser(RegisterFtctlProtectionCmd cmd) {
        return StringUtils.defaultIfBlank(StringUtils.trimToNull(cmd.getRemotePeerSshUser()), DEFAULT_REMOTE_PEER_SSH_USER);
    }

    private String resolveRemotePeerSshPort(RegisterFtctlProtectionCmd cmd) {
        return StringUtils.defaultIfBlank(StringUtils.trimToNull(cmd.getRemotePeerSshPort()), DEFAULT_REMOTE_PEER_SSH_PORT);
    }

    private String resolveRemotePeerLibvirtUri(RegisterFtctlProtectionCmd cmd) {
        return resolveRemotePeerLibvirtUri(cmd.getRemotePeerLibvirtUri(), cmd.getRemotePeerHostAddress(),
                resolveRemotePeerSshUser(cmd), resolveRemotePeerSshPort(cmd));
    }

    private String resolveRemotePeerLibvirtUri(String remotePeerLibvirtUri, String remotePeerHostAddress, String remotePeerSshUser, String remotePeerSshPort) {
        String explicitUri = StringUtils.trimToNull(remotePeerLibvirtUri);
        if (explicitUri != null) {
            return explicitUri;
        }
        String host = StringUtils.trimToNull(remotePeerHostAddress);
        if (host == null) {
            throw new CloudRuntimeException("FTCTL DR remote Mold requires a remote peer host address");
        }
        return String.format("qemu+ssh://%s@%s:%s/system", remotePeerSshUser, host, remotePeerSshPort);
    }

    private void validateRemoteExecutionPath(UserVmVO userVm, RegisterFtctlProtectionCmd cmd,
                                             String secondaryTargetDir, String remoteNbdExportAddr, String secondarySshKeyFile) {
        Long hostId = getExecutionHostId(userVm);
        if (hostId == null) {
            throw new CloudRuntimeException(String.format("Unable to resolve source host for FTCTL remote preflight on VM %s", userVm.getUuid()));
        }
        FtctlRemotePreflightCommand preflightCommand = new FtctlRemotePreflightCommand(
                userVm.getInstanceName(), cmd.getMode(), resolveRemotePeerLibvirtUri(cmd));
        preflightCommand.setSecondaryTargetDir(secondaryTargetDir);
        preflightCommand.setSecondarySshKeyFile(secondarySshKeyFile);
        preflightCommand.setRemoteNbdExportAddr(remoteNbdExportAddr);
        try {
            Answer answer = agentManager.send(hostId, preflightCommand);
            if (!(answer instanceof FtctlSyncAnswer) || !answer.getResult()) {
                throw new CloudRuntimeException(String.format("FTCTL remote execution preflight failed for VM %s on host %s: %s",
                        userVm.getUuid(), hostId, answer != null ? answer.getDetails() : "no answer"));
            }
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException(String.format("Unable to execute FTCTL remote preflight on host %s for VM %s",
                    hostId, userVm.getUuid()), e);
        }
    }

    private boolean isProtectionModeWithTargetStorage(String mode) {
        return "ha".equalsIgnoreCase(mode) || "dr".equalsIgnoreCase(mode) || "ft".equalsIgnoreCase(mode);
    }

    private String resolveProvisioningBackend(RegisterFtctlProtectionCmd cmd) {
        if (isRemoteMoldDr(cmd)) {
            return FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED;
        }
        return StringUtils.defaultIfBlank(cmd.getProvisioningBackend(), FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED);
    }

    private FtctlProtectionProvisioningContext prepareProtection(UserVmVO userVm, RegisterFtctlProtectionCmd cmd, StoragePoolVO targetStoragePool,
                                                                String backendMode, String provisioningBackend) {
        if (isRemoteMoldDr(cmd) && FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED.equalsIgnoreCase(provisioningBackend)) {
            return prepareRemoteMoldCloudManagedProtection(userVm, cmd, backendMode);
        }
        FtctlProtectionProvisioningRequest request = new FtctlProtectionProvisioningRequest(
                userVm,
                cmd.getPeerHostId(),
                targetStoragePool,
                cmd.getMode(),
                backendMode,
                provisioningBackend,
                cmd.getFencingPolicy(),
                cmd.getSecondaryVmName(),
                cmd.getNetworkIds());
        return ftctlProtectionProvisioningService.prepareProtection(request);
    }

    private FtctlProtectionProvisioningContext prepareRemoteMoldCloudManagedProtection(UserVmVO userVm, RegisterFtctlProtectionCmd cmd, String backendMode) {
        Map<String, String> params = new HashMap<>();
        params.put("sourcevirtualmachineid", userVm.getUuid());
        params.put("sourcevirtualmachinename", resolveVmDisplayName(userVm.getId()));
        params.put("sourcevirtualmachineinstancename", userVm.getInstanceName());
        params.put("secondaryvmname", resolveSecondaryVmName(userVm, cmd));
        params.put("remotepeerhostuuid", cmd.getRemotePeerHostUuid());
        params.put("remotetargetstoragepooluuid", cmd.getRemoteTargetStoragePoolUuid());
        params.put("sourcehypervisor", userVm.getHypervisorType() != null ? userVm.getHypervisorType().name() : "KVM");
        params.put("sourcevmdetails", buildSourceVmDetailsJson(userVm).toString());
        params.put("sourcevolumes", buildSourceVolumesJson(userVm).toString());
        params.put("networkids", cmd.getNetworkIds());

        JsonObject json = callRemoteMoldApi(cmd.getRemoteMoldApiUrl(), cmd.getRemoteMoldApiKey(), cmd.getRemoteMoldSecretKey(),
                PrepareFtctlDrReplicaResourcesCmd.APINAME, params);
        RemoteReplicaResources resources = parseRemoteReplicaResources(json);
        persistRemoteReplicaProtection(userVm, cmd, backendMode, resources);
        persistRemoteReplicaDetails(userVm.getId(), resources);
        return new FtctlProtectionProvisioningContext(FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED,
                FtctlProtectionProvisioningService.STATE_READY,
                StringUtils.defaultIfBlank(resources.instanceName, resources.name),
                resources.diskMap);
    }

    private JsonObject buildSourceVmDetailsJson(UserVmVO userVm) {
        JsonObject details = new JsonObject();
        if (userVm != null) {
            userVmDao.loadDetails(userVm);
            Map<String, String> vmDetails = userVm.getDetails();
            if (vmDetails != null) {
                for (Map.Entry<String, String> entry : vmDetails.entrySet()) {
                    if (StringUtils.isNotBlank(entry.getKey()) && StringUtils.isNotBlank(entry.getValue()) &&
                            !StringUtils.startsWith(entry.getKey(), "ftctl.")) {
                        details.addProperty(entry.getKey(), entry.getValue());
                    }
                }
            }
            ServiceOfferingVO offering = serviceOfferingDao.findById(userVm.getServiceOfferingId());
            if (offering != null) {
                if (offering.getCpu() != null) {
                    details.addProperty(UsageEventVO.DynamicParameters.cpuNumber.name(), String.valueOf(offering.getCpu()));
                    details.addProperty(ApiConstants.CPU_NUMBER, String.valueOf(offering.getCpu()));
                }
                if (offering.getSpeed() != null) {
                    details.addProperty(UsageEventVO.DynamicParameters.cpuSpeed.name(), String.valueOf(offering.getSpeed()));
                    details.addProperty(ApiConstants.CPU_SPEED, String.valueOf(offering.getSpeed()));
                }
                if (offering.getRamSize() != null) {
                    details.addProperty(UsageEventVO.DynamicParameters.memory.name(), String.valueOf(offering.getRamSize()));
                    details.addProperty(ApiConstants.MEMORY, String.valueOf(offering.getRamSize()));
                }
            }
        }
        return details;
    }

    private JsonArray buildSourceVolumesJson(UserVmVO userVm) {
        List<VolumeVO> volumes = volumeDao.findByInstance(userVm.getId());
        if (volumes == null || volumes.isEmpty()) {
            throw new CloudRuntimeException(String.format("Unable to find source volumes for FTCTL DR remote Mold VM %s", userVm.getUuid()));
        }
        JsonArray array = new JsonArray();
        for (VolumeVO volume : volumes.stream()
                .filter(this::isProtectedVolumeType)
                .sorted((left, right) -> Long.compare(resolveDeviceIdForSort(left), resolveDeviceIdForSort(right)))
                .collect(Collectors.toList())) {
            JsonObject object = new JsonObject();
            object.addProperty("sourcevolumeid", String.valueOf(volume.getId()));
            object.addProperty("sourcevolumeuuid", volume.getUuid());
            object.addProperty("sourcedisktarget", resolveKvmDiskTarget(userVm.getId(), volume.getId()));
            object.addProperty("disklabel", resolveDiskLabel(volume));
            object.addProperty("type", volume.getVolumeType() != null ? volume.getVolumeType().name() : null);
            if (volume.getDeviceId() != null) {
                object.addProperty("deviceid", volume.getDeviceId());
            }
            object.addProperty("size", volume.getSize());
            Long minIops = normalizeFtctlIops(volume.getMinIops());
            Long maxIops = normalizeFtctlIops(volume.getMaxIops());
            if (minIops != null) {
                object.addProperty("miniops", minIops);
            }
            if (maxIops != null) {
                object.addProperty("maxiops", maxIops);
            }
            array.add(object);
        }
        if (array.size() == 0) {
            throw new CloudRuntimeException(String.format("FTCTL DR remote Mold VM %s has no protected ROOT/DATADISK volumes", userVm.getUuid()));
        }
        return array;
    }

    private Long normalizeFtctlIops(Long iops) {
        return iops != null && iops > 0 ? iops : null;
    }

    private String resolveSecondaryVmName(UserVmVO userVm, RegisterFtctlProtectionCmd cmd) {
        if (StringUtils.isNotBlank(cmd.getSecondaryVmName())) {
            return StringUtils.trim(cmd.getSecondaryVmName());
        }
        String baseName = stripFtctlReplicaRoleSuffix(StringUtils.defaultIfBlank(resolveVmDisplayName(userVm.getId()), userVm.getHostName()));
        if ("dr".equalsIgnoreCase(cmd.getMode())) {
            String targetSiteSuffix = resolveDrTargetSiteSuffix(cmd);
            if (StringUtils.isNotBlank(targetSiteSuffix)) {
                return String.format("%s-replica-%s", baseName, targetSiteSuffix);
            }
            return String.format("%s-replica", baseName);
        }
        return String.format("%s-standby", baseName);
    }

    private String stripFtctlReplicaRoleSuffix(String vmName) {
        String normalized = StringUtils.trimToEmpty(vmName);
        if (StringUtils.endsWithIgnoreCase(normalized, "-standby")) {
            return normalized.substring(0, normalized.length() - "-standby".length());
        }
        if (StringUtils.endsWithIgnoreCase(normalized, "-replica")) {
            return normalized.substring(0, normalized.length() - "-replica".length());
        }
        return normalized;
    }

    private String resolveDrTargetSiteSuffix(RegisterFtctlProtectionCmd cmd) {
        String address = isRemoteMoldDr(cmd) ? cmd.getRemotePeerHostAddress() : null;
        if (StringUtils.isBlank(address) && cmd.getPeerHostId() != null) {
            HostVO peerHost = hostDao.findById(cmd.getPeerHostId());
            if (peerHost != null) {
                address = StringUtils.defaultIfBlank(peerHost.getPrivateIpAddress(), peerHost.getName());
            }
        }
        return sanitizeReplicaNameSegment(extractSiteToken(address));
    }

    private String extractSiteToken(String value) {
        String normalized = StringUtils.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String[] parts = normalized.split("\\.");
        if (parts.length == 4 && StringUtils.isNumeric(parts[2])) {
            return parts[2];
        }
        return normalized;
    }

    private String sanitizeReplicaNameSegment(String value) {
        String normalized = StringUtils.lowerCase(StringUtils.trimToEmpty(value), Locale.ROOT)
                .replaceAll("[^a-z0-9-]+", "-")
                .replaceAll("^-+|-+$", "");
        return StringUtils.trimToNull(normalized);
    }

    private RemoteReplicaResources parseRemoteReplicaResources(JsonObject json) {
        JsonObject response = getResponseObject(json, "prepareftctldrreplicaresourcesresponse");
        if (response == null) {
            throw new CloudRuntimeException("Remote Mold did not return FTCTL DR replica resources");
        }
        JsonObject payload = unwrapRemoteReplicaResourcesPayload(response);
        RemoteReplicaResources resources = new RemoteReplicaResources();
        resources.vmId = getJsonString(payload, "remotevirtualmachineid");
        resources.name = getJsonString(payload, "remotevirtualmachinename");
        resources.instanceName = getJsonString(payload, "remotevirtualmachineinstancename");
        resources.state = getJsonString(payload, "remotevirtualmachinestate");
        resources.hostId = getJsonString(payload, "remotevirtualmachinehostid");
        resources.hostName = getJsonString(payload, "remotevirtualmachinehostname");
        resources.stateUpdated = getJsonString(payload, "remotevirtualmachinestateupdated");
        resources.diskMap = getJsonString(payload, "diskmap");
        JsonArray volumes = null;
        if (payload.has("volume") && payload.get("volume").isJsonArray()) {
            volumes = payload.getAsJsonArray("volume");
        }
        if (volumes != null) {
            for (JsonElement element : volumes) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject object = unwrapRemoteReplicaVolumePayload(element.getAsJsonObject());
                RemoteReplicaVolume volume = new RemoteReplicaVolume();
                volume.id = getJsonString(object, "id");
                volume.name = getJsonString(object, "name");
                volume.path = getJsonString(object, "path");
                volume.state = getJsonString(object, "state");
                volume.diskLabel = getJsonString(object, "disklabel");
                volume.sourceVolumeId = getJsonString(object, "sourcevolumeid");
                volume.sourceDiskTarget = getJsonString(object, "sourcedisktarget");
                resources.volumes.add(volume);
            }
        }
        if (resources.volumes.isEmpty()) {
            throw new CloudRuntimeException("Remote Mold returned no FTCTL DR replica volumes");
        }
        validateRemoteReplicaVolumeMetadata(resources);
        if (StringUtils.isBlank(resources.diskMap)) {
            resources.diskMap = buildRemoteReplicaDiskMap(resources.volumes);
        }
        if (StringUtils.isAnyBlank(resources.vmId, resources.diskMap)) {
            throw new CloudRuntimeException("Remote Mold returned incomplete FTCTL DR replica resources");
        }
        return resources;
    }

    private JsonObject unwrapRemoteReplicaResourcesPayload(JsonObject response) {
        if (response != null && response.has("ftctldrreplicaresources") && response.get("ftctldrreplicaresources").isJsonObject()) {
            return response.getAsJsonObject("ftctldrreplicaresources");
        }
        return response;
    }

    private JsonObject unwrapRemoteReplicaVolumePayload(JsonObject volume) {
        if (volume != null && volume.has("ftctldrreplicavolume") && volume.get("ftctldrreplicavolume").isJsonObject()) {
            return volume.getAsJsonObject("ftctldrreplicavolume");
        }
        return volume;
    }

    private void validateRemoteReplicaVolumeMetadata(RemoteReplicaResources resources) {
        for (RemoteReplicaVolume volume : resources.volumes) {
            if (StringUtils.isAnyBlank(volume.sourceVolumeId, volume.path)) {
                throw new CloudRuntimeException("Remote Mold returned incomplete FTCTL DR replica volume metadata");
            }
            if (StringUtils.isBlank(resources.diskMap) && StringUtils.isBlank(volume.sourceDiskTarget)) {
                throw new CloudRuntimeException("Remote Mold returned incomplete FTCTL DR replica resources");
            }
        }
    }

    private String buildRemoteReplicaDiskMap(List<RemoteReplicaVolume> volumes) {
        List<String> diskMapEntries = new ArrayList<>();
        for (RemoteReplicaVolume volume : volumes) {
            diskMapEntries.add(String.format("%s=%s", volume.sourceDiskTarget, volume.path));
        }
        return StringUtils.join(diskMapEntries, ";");
    }

    private JsonObject getResponseObject(JsonObject json, String responseKey) {
        if (json == null || !json.has(responseKey) || !json.get(responseKey).isJsonObject()) {
            return null;
        }
        return json.getAsJsonObject(responseKey);
    }

    private void persistRemoteReplicaProtection(UserVmVO userVm, RegisterFtctlProtectionCmd cmd, String backendMode,
                                                RemoteReplicaResources resources) {
        FtctlProtectionVO protection = ftctlProtectionDao.findActiveByPrimaryVmId(userVm.getId());
        boolean existingRecord = protection != null;
        if (protection == null) {
            protection = new FtctlProtectionVO(userVm.getId());
        }
        protection.setMode(cmd.getMode());
        protection.setBackendMode(backendMode);
        protection.setProvisioningBackend(FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED);
        protection.setProvisioningState(FtctlProtectionProvisioningService.STATE_READY);
        protection.setFencingPolicy(cmd.getFencingPolicy());
        protection.setPeerHostId(null);
        protection.setTargetStoragePoolId(null);
        protection.setSecondaryVmId(null);
        protection.setSecondaryVmName(StringUtils.defaultIfBlank(resources.instanceName, resources.name));
        protection.setLastError(null);
        protection.markUpdated();
        if (existingRecord) {
            ftctlProtectionDao.update(protection.getId(), protection);
        } else {
            protection = ftctlProtectionDao.persist(protection);
        }
        persistRemoteReplicaVolumeMappings(userVm, protection, resources);
    }

    private void persistXcoloPortAllocation(long primaryVmId, XcoloPortAllocation allocation) {
        if (allocation == null || !allocation.enabled) {
            return;
        }
        FtctlProtectionVO protection = ftctlProtectionDao.findActiveByPrimaryVmId(primaryVmId);
        if (protection == null) {
            throw new CloudRuntimeException(String.format("Unable to persist FTCTL x-colo port allocation: active protection not found for VM %s", primaryVmId));
        }
        protection.setXcoloPortAllocationMode(allocation.mode);
        protection.setXcoloPortSlot(allocation.slot);
        protection.markUpdated();
        ftctlProtectionDao.update(protection.getId(), protection);
    }

    private void persistRemoteReplicaVolumeMappings(UserVmVO userVm, FtctlProtectionVO protection, RemoteReplicaResources resources) {
        List<VolumeVO> primaryVolumes = volumeDao.findByInstance(userVm.getId());
        if (primaryVolumes == null || primaryVolumes.isEmpty()) {
            return;
        }
        Map<String, RemoteReplicaVolume> remoteVolumesByPrimaryId = new HashMap<>();
        for (RemoteReplicaVolume remoteVolume : resources.volumes) {
            if (StringUtils.isNotBlank(remoteVolume.sourceVolumeId)) {
                remoteVolumesByPrimaryId.put(remoteVolume.sourceVolumeId, remoteVolume);
            }
        }
        for (VolumeVO primaryVolume : primaryVolumes) {
            if (!isProtectedVolumeType(primaryVolume)) {
                continue;
            }
            RemoteReplicaVolume remoteVolume = remoteVolumesByPrimaryId.get(String.valueOf(primaryVolume.getId()));
            if (remoteVolume == null || StringUtils.isBlank(remoteVolume.path)) {
                throw new CloudRuntimeException(String.format("Remote Mold did not return a replica disk path for primary volume %s", primaryVolume.getUuid()));
            }
            FtctlProtectionVolumeVO protectionVolume = ftctlProtectionVolumeDao.findActiveByProtectionIdAndPrimaryVolumeId(protection.getId(), primaryVolume.getId());
            boolean existingRecord = protectionVolume != null;
            if (protectionVolume == null) {
                protectionVolume = new FtctlProtectionVolumeVO(protection.getId(), primaryVolume.getId());
            }
            protectionVolume.setPrimaryDiskPath(primaryVolume.getPath());
            protectionVolume.setDiskLabel(StringUtils.defaultIfBlank(remoteVolume.diskLabel, resolveDiskLabel(primaryVolume)));
            protectionVolume.setSecondaryVolumeId(null);
            protectionVolume.setSecondaryDiskPath(remoteVolume.path);
            protectionVolume.markUpdated();
            if (existingRecord) {
                ftctlProtectionVolumeDao.update(protectionVolume.getId(), protectionVolume);
            } else {
                ftctlProtectionVolumeDao.persist(protectionVolume);
            }
        }
    }

    private void persistRemoteReplicaDetails(Long virtualMachineId, RemoteReplicaResources resources) {
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_REPLICA_VM_ID, resources.vmId);
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_REPLICA_VM_NAME, resources.name);
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_REPLICA_VM_INSTANCE_NAME, resources.instanceName);
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_REPLICA_VM_STATE, resources.state);
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_REPLICA_VM_HOST_ID, resources.hostId);
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_REPLICA_VM_HOST_NAME, resources.hostName);
        if (hasRemoteReplicaStateSnapshot(resources)) {
            putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_REPLICA_VM_STATE_UPDATED,
                    StringUtils.defaultIfBlank(resources.stateUpdated, Instant.now().toString()));
        }
        for (RemoteReplicaVolume volume : resources.volumes) {
            if (StringUtils.isBlank(volume.sourceVolumeId)) {
                continue;
            }
            String prefix = DETAIL_REMOTE_REPLICA_VOLUME_PREFIX + volume.sourceVolumeId + ".";
            putVmDetailIfNotBlank(virtualMachineId, prefix + "id", volume.id);
            putVmDetailIfNotBlank(virtualMachineId, prefix + "name", volume.name);
            putVmDetailIfNotBlank(virtualMachineId, prefix + "path", volume.path);
            putVmDetailIfNotBlank(virtualMachineId, prefix + "state", volume.state);
            putVmDetailIfNotBlank(virtualMachineId, prefix + "disk.label", volume.diskLabel);
        }
    }

    private boolean hasRemoteReplicaStateSnapshot(RemoteReplicaResources resources) {
        return resources != null && (StringUtils.isNotBlank(resources.state) ||
                StringUtils.isNotBlank(resources.hostId) ||
                StringUtils.isNotBlank(resources.hostName));
    }

    private void persistProvisioningFailure(Long virtualMachineId, String provisioningBackend, CloudRuntimeException e) {
        putVmDetail(virtualMachineId, DETAIL_PROVISIONING_BACKEND, provisioningBackend);
        putVmDetail(virtualMachineId, DETAIL_PROVISIONING_STATE, FtctlProtectionProvisioningService.STATE_PROVISIONING_FAILED);
        putVmDetail(virtualMachineId, DETAIL_LAST_ERROR, e.getMessage());
    }

    private void persistRemoteMoldDrDetails(RegisterFtctlProtectionCmd cmd) {
        Long virtualMachineId = cmd.getVirtualMachineId();
        putVmDetail(virtualMachineId, DETAIL_DR_PEER_SITE_TYPE, DR_PEER_SITE_TYPE_REMOTE_MOLD);
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_MOLD_API_URL, cmd.getRemoteMoldApiUrl());
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_PEER_HOST_ID, cmd.getRemotePeerHostUuid());
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_PEER_HOST_NAME, cmd.getRemotePeerHostName());
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_PEER_HOST_ADDRESS, cmd.getRemotePeerHostAddress());
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_PEER_HOST_BLOCKCOPY_ADDRESS, cmd.getRemotePeerHostBlockcopyAddress());
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_PEER_SSH_USER, resolveRemotePeerSshUser(cmd));
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_PEER_SSH_PORT, resolveRemotePeerSshPort(cmd));
        if (cmd.getRemotePeerSshOverride() != null) {
            putVmDetail(virtualMachineId, DETAIL_REMOTE_PEER_SSH_OVERRIDE, String.valueOf(cmd.getRemotePeerSshOverride()));
        }
        if (cmd.getRemotePeerSshAutoSetup() != null) {
            putVmDetail(virtualMachineId, DETAIL_REMOTE_PEER_SSH_AUTO_SETUP, String.valueOf(cmd.getRemotePeerSshAutoSetup()));
        }
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_PEER_LIBVIRT_URI, resolveRemotePeerLibvirtUri(cmd));
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_TARGET_STORAGE_POOL_ID, cmd.getRemoteTargetStoragePoolUuid());
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_TARGET_STORAGE_POOL_NAME, cmd.getRemoteTargetStoragePoolName());
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_TARGET_STORAGE_POOL_PATH, cmd.getRemoteTargetStoragePoolPath());
        putVmDetailIfNotBlank(virtualMachineId, DETAIL_REMOTE_TARGET_STORAGE_POOL_TYPE, cmd.getRemoteTargetStoragePoolType());
    }

    private void persistRegistrationFailure(UserVmVO userVm, CloudRuntimeException e) {
        if (userVm == null) {
            return;
        }
        String message = StringUtils.defaultIfBlank(e != null ? e.getMessage() : null, "FTCTL registration failed");
        Long virtualMachineId = userVm.getId();
        putVmDetail(virtualMachineId, DETAIL_LAST_PROTECTION_STATE, "error");
        putVmDetail(virtualMachineId, DETAIL_LAST_TRANSPORT_STATE, "failed");
        putVmDetail(virtualMachineId, DETAIL_LAST_ACTIVE_SIDE, "primary");
        putVmDetail(virtualMachineId, DETAIL_LAST_ADMIN_STATE, "active");
        putVmDetail(virtualMachineId, DETAIL_LAST_FENCING_STATE, "clear");
        putVmDetail(virtualMachineId, DETAIL_LAST_ERROR, message);

        FtctlProtectionVO protection = ftctlProtectionDao.findActiveByPrimaryVmId(virtualMachineId);
        if (protection != null) {
            protection.setProtectionState("error");
            protection.setTransportState("failed");
            protection.setActiveSide("primary");
            protection.setAdminState("active");
            protection.setFencingState("clear");
            protection.setLastError(message);
            protection.markUpdated();
            ftctlProtectionDao.update(protection.getId(), protection);
        }
        publishFtctlEvent(userVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                String.format("FTCTL protection registration failed for VM %s: %s", userVm.getUuid(), message));
    }

    private void applyCloudManagedFtLifecycleGuard(UserVmVO userVm, FtctlProtectionProvisioningContext provisioningContext) {
        if (userVm == null || provisioningContext == null ||
                !"ft".equalsIgnoreCase(StringUtils.trimToEmpty(getDetailValue(userVm.getId(), DETAIL_MODE))) ||
                !FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED.equalsIgnoreCase(StringUtils.trimToEmpty(provisioningContext.getProvisioningBackend()))) {
            return;
        }

        if (StringUtils.isBlank(getDetailValue(userVm.getId(), DETAIL_LIFECYCLE_GUARD_ORIGINAL_HA_ENABLED))) {
            putVmDetail(userVm.getId(), DETAIL_LIFECYCLE_GUARD_ORIGINAL_HA_ENABLED, String.valueOf(userVm.isHaEnabled()));
        }
        putVmDetail(userVm.getId(), DETAIL_LIFECYCLE_GUARD, "ft-cloud-managed-protecting");
        putVmDetail(userVm.getId(), DETAIL_LIFECYCLE_GUARD_STARTED, Instant.now().toString());
        if (userVm.getHostId() != null) {
            putVmDetail(userVm.getId(), DETAIL_LIFECYCLE_GUARD_PRIMARY_HOST_ID, String.valueOf(userVm.getHostId()));
        }

        if (userVm.isHaEnabled()) {
            UserVmVO update = userVmDao.createForUpdate();
            update.setHaEnabled(false);
            userVmDao.update(userVm.getId(), update);
            userVm.setHaEnabled(false);
            logger.info(String.format("Temporarily disabled Cloud HA for FT cloud-managed lifecycle guard on VM %s", userVm.getUuid()));
        }
    }

    private void releaseCloudManagedFtLifecycleGuard(UserVmVO userVm) {
        if (userVm == null || StringUtils.isBlank(getDetailValue(userVm.getId(), DETAIL_LIFECYCLE_GUARD))) {
            return;
        }

        String originalHaEnabled = getDetailValue(userVm.getId(), DETAIL_LIFECYCLE_GUARD_ORIGINAL_HA_ENABLED);
        if (StringUtils.isNotBlank(originalHaEnabled)) {
            boolean restoreHa = Boolean.parseBoolean(originalHaEnabled);
            if (userVm.isHaEnabled() != restoreHa) {
                UserVmVO update = userVmDao.createForUpdate();
                update.setHaEnabled(restoreHa);
                userVmDao.update(userVm.getId(), update);
                userVm.setHaEnabled(restoreHa);
                logger.info(String.format("Restored Cloud HA=%s after FT lifecycle guard release on VM %s", restoreHa, userVm.getUuid()));
            }
        }

        removeVmDetail(userVm.getId(), DETAIL_LIFECYCLE_GUARD);
        removeVmDetail(userVm.getId(), DETAIL_LIFECYCLE_GUARD_ORIGINAL_HA_ENABLED);
        removeVmDetail(userVm.getId(), DETAIL_LIFECYCLE_GUARD_PRIMARY_HOST_ID);
        removeVmDetail(userVm.getId(), DETAIL_LIFECYCLE_GUARD_STARTED);
    }

    private StoragePoolVO validateTargetStoragePool(RegisterFtctlProtectionCmd cmd, UserVmVO userVm) {
        if (!isProtectionModeWithTargetStorage(cmd.getMode())) {
            return null;
        }
        if (isRemoteMoldDr(cmd)) {
            return null;
        }
        StoragePoolVO storagePool = primaryDataStoreDao.findById(cmd.getTargetStoragePoolId());
        if (storagePool == null) {
            throw new CloudRuntimeException(String.format("Unable to find FTCTL target primary storage pool with id %s", cmd.getTargetStoragePoolId()));
        }
        if (storagePool.getDataCenterId() != userVm.getDataCenterId()) {
            throw new CloudRuntimeException(String.format("FTCTL target primary storage pool %s is not in VM zone %s",
                    storagePool.getUuid(), userVm.getDataCenterId()));
        }
        return storagePool;
    }

    private String resolveTargetStorageScope(RegisterFtctlProtectionCmd cmd, StoragePoolVO storagePool, String backendMode) {
        if ("remote-nbd".equalsIgnoreCase(backendMode)) {
            return "secondary-local";
        }
        if ("shared-blockcopy".equalsIgnoreCase(backendMode)) {
            return "shared";
        }
        if (storagePool != null && storagePool.getScope() != null) {
            switch (storagePool.getScope()) {
                case HOST:
                    return "secondary-local";
                case CLUSTER:
                case ZONE:
                    return "shared";
                default:
                    return storagePool.getScope().name().toLowerCase();
            }
        }
        return cmd.getTargetStorageScope();
    }

    private String resolveBackendMode(RegisterFtctlProtectionCmd cmd, StoragePoolVO storagePool) {
        if (isRemoteMoldDr(cmd)) {
            return "remote-nbd";
        }
        if ("ft".equalsIgnoreCase(StringUtils.trimToEmpty(cmd != null ? cmd.getMode() : null))) {
            String requestedBackendMode = StringUtils.trimToNull(cmd.getBackendMode());
            if (requestedBackendMode == null) {
                return "remote-nbd";
            }
            if (!"remote-nbd".equalsIgnoreCase(requestedBackendMode)) {
                throw new CloudRuntimeException("FTCTL FT mode requires remote-nbd backend mode");
            }
            return requestedBackendMode;
        }
        if (isHostScopedStoragePool(storagePool)) {
            return "remote-nbd";
        }
        if (!isBlank(cmd.getBackendMode())) {
            return cmd.getBackendMode();
        }
        return "shared-blockcopy";
    }

    private String resolveSecondaryTargetDir(RegisterFtctlProtectionCmd cmd, StoragePoolVO storagePool, String backendMode) {
        if (!isBlank(cmd.getSecondaryTargetDir())) {
            return cmd.getSecondaryTargetDir();
        }
        if (isRemoteMoldDr(cmd)) {
            return StringUtils.removeEnd(cmd.getRemoteTargetStoragePoolPath(), "/");
        }
        if (!"remote-nbd".equalsIgnoreCase(backendMode)) {
            return cmd.getSecondaryTargetDir();
        }
        if (storagePool == null || isBlank(storagePool.getPath())) {
            throw new CloudRuntimeException("FTCTL remote-nbd target storage pool path is required");
        }
        return storagePool.getPath().replaceAll("/+$", "");
    }

    private String resolveRemoteNbdExportAddr(RegisterFtctlProtectionCmd cmd, String backendMode) {
        if (!isBlank(cmd.getRemoteNbdExportAddr())) {
            return cmd.getRemoteNbdExportAddr();
        }
        if (!"remote-nbd".equalsIgnoreCase(backendMode)) {
            return cmd.getRemoteNbdExportAddr();
        }
        if ("ft".equalsIgnoreCase(cmd.getMode()) && !isManualXcoloPortAllocation(cmd)) {
            return null;
        }
        if (isRemoteMoldDr(cmd)) {
            String exportAddress = StringUtils.defaultIfBlank(cmd.getRemotePeerHostBlockcopyAddress(), cmd.getRemotePeerHostAddress());
            if (isBlank(exportAddress)) {
                throw new CloudRuntimeException("Unable to resolve FTCTL remote-nbd export address for remote Mold peer host");
            }
            return String.format("%s:10809", exportAddress);
        }
        HostVO peerHost = hostDao.findById(cmd.getPeerHostId());
        if (peerHost == null) {
            throw new CloudRuntimeException(String.format("Unable to resolve FTCTL peer host %s", cmd.getPeerHostId()));
        }
        String exportAddress = resolveHostField(peerHost, HOST_DETAIL_BLOCKCOPY_IP, peerHost.getPrivateIpAddress());
        if (isBlank(exportAddress)) {
            throw new CloudRuntimeException(String.format("Unable to resolve FTCTL remote-nbd export address for peer host %s", cmd.getPeerHostId()));
        }
        return String.format("%s:10809", exportAddress);
    }

    private boolean isManualXcoloPortAllocation(RegisterFtctlProtectionCmd cmd) {
        return "manual".equalsIgnoreCase(StringUtils.trimToEmpty(cmd != null ? cmd.getXcoloPortAllocationMode() : null));
    }

    private XcoloPortAllocation resolveXcoloPortAllocation(RegisterFtctlProtectionCmd cmd, UserVmVO userVm, String backendMode, String remoteNbdExportAddr) {
        if (!"ft".equalsIgnoreCase(cmd.getMode())) {
            return XcoloPortAllocation.disabled(remoteNbdExportAddr);
        }
        if (!"remote-nbd".equalsIgnoreCase(backendMode)) {
            throw new CloudRuntimeException("FTCTL FT mode requires remote-nbd backend mode");
        }
        String mode = StringUtils.defaultIfBlank(StringUtils.trimToNull(cmd.getXcoloPortAllocationMode()), "auto").toLowerCase(Locale.ROOT);
        switch (mode) {
            case "manual":
                return resolveManualXcoloPortAllocation(cmd, remoteNbdExportAddr);
            case "auto":
                return resolveAutomaticXcoloPortAllocation(cmd, userVm);
            default:
                throw new CloudRuntimeException(String.format("Unsupported FTCTL x-colo port allocation mode: %s", mode));
        }
    }

    private XcoloPortAllocation resolveManualXcoloPortAllocation(RegisterFtctlProtectionCmd cmd, String remoteNbdExportAddr) {
        EndpointHostPort proxy = parseTcpEndpoint(cmd.getXcoloProxyEndpoint(), "x-colo proxy endpoint");
        EndpointHostPort nbd = parseTcpEndpoint(cmd.getXcoloNbdEndpoint(), "x-colo NBD endpoint");
        EndpointHostPort migrate = parseTcpEndpoint(cmd.getXcoloMigrateUri(), "x-colo migrate URI");
        EndpointHostPort remoteNbd = StringUtils.isBlank(remoteNbdExportAddr)
                ? nbd : parseHostPort(remoteNbdExportAddr, "remote NBD export address");
        if (!remoteNbd.equalsHostPort(nbd)) {
            throw new CloudRuntimeException(String.format("FTCTL remote NBD export address %s must match x-colo NBD endpoint %s",
                    remoteNbd.hostPort(), nbd.hostPort()));
        }
        return new XcoloPortAllocation("manual", null, remoteNbd.hostPort(), cmd.getXcoloProxyEndpoint(), cmd.getXcoloNbdEndpoint(),
                cmd.getXcoloMigrateUri(), 9003, 9004, 9001, 9005, migrate.port, proxy.port, true);
    }

    private XcoloPortAllocation resolveAutomaticXcoloPortAllocation(RegisterFtctlProtectionCmd cmd, UserVmVO userVm) {
        String peerAddress = resolveXcoloPeerAddress(cmd);
        Set<Integer> usedSlots = collectActiveXcoloPortSlots(userVm != null ? userVm.getId() : null);
        for (int slot = 0; slot < FTCTL_XCOLO_AUTO_SLOT_COUNT; slot++) {
            if (!usedSlots.contains(slot)) {
                return buildAutomaticXcoloPortAllocation(peerAddress, slot);
            }
        }
        throw new CloudRuntimeException(String.format("Unable to allocate FTCTL x-colo ports: all %s automatic slots are in use",
                FTCTL_XCOLO_AUTO_SLOT_COUNT));
    }

    private XcoloPortAllocation buildAutomaticXcoloPortAllocation(String peerAddress, int slot) {
        int proxyPort = FTCTL_XCOLO_AUTO_PROXY_BASE + slot * FTCTL_XCOLO_AUTO_PORT_STEP;
        int compareLocalPort = FTCTL_XCOLO_AUTO_COMPARE_LOCAL_BASE + slot * FTCTL_XCOLO_AUTO_PORT_STEP;
        int mirrorPort = FTCTL_XCOLO_AUTO_MIRROR_BASE + slot * FTCTL_XCOLO_AUTO_PORT_STEP;
        int comparePort = FTCTL_XCOLO_AUTO_COMPARE_BASE + slot * FTCTL_XCOLO_AUTO_PORT_STEP;
        int compareOutPort = FTCTL_XCOLO_AUTO_COMPARE_OUT_BASE + slot * FTCTL_XCOLO_AUTO_PORT_STEP;
        int controlPort = FTCTL_XCOLO_AUTO_MIGRATE_BASE + slot;
        int remoteNbdPort = FTCTL_XCOLO_AUTO_REMOTE_NBD_BASE + slot;
        String remoteNbdExportAddr = String.format("%s:%s", peerAddress, remoteNbdPort);
        String proxyEndpoint = String.format("tcp:%s:%s", peerAddress, proxyPort);
        String nbdEndpoint = String.format("tcp:%s:%s", peerAddress, remoteNbdPort);
        String migrateUri = String.format("tcp:%s:%s", peerAddress, controlPort);
        return new XcoloPortAllocation("auto", slot, remoteNbdExportAddr, proxyEndpoint, nbdEndpoint, migrateUri,
                mirrorPort, comparePort, compareLocalPort, compareOutPort, controlPort, proxyPort, true);
    }

    private String resolveXcoloPeerAddress(RegisterFtctlProtectionCmd cmd) {
        if (isRemoteMoldDr(cmd)) {
            String address = StringUtils.defaultIfBlank(cmd.getRemotePeerHostBlockcopyAddress(), cmd.getRemotePeerHostAddress());
            if (StringUtils.isBlank(address)) {
                throw new CloudRuntimeException("Unable to resolve FTCTL x-colo peer address for remote Mold peer host");
            }
            return address;
        }
        HostVO peerHost = hostDao.findById(cmd.getPeerHostId());
        if (peerHost == null) {
            throw new CloudRuntimeException(String.format("Unable to resolve FTCTL peer host %s", cmd.getPeerHostId()));
        }
        String address = StringUtils.defaultIfBlank(resolveHostField(peerHost, HOST_DETAIL_XCOLO_DATA_IP, null),
                resolveHostField(peerHost, HOST_DETAIL_BLOCKCOPY_IP, peerHost.getPrivateIpAddress()));
        if (StringUtils.isBlank(address)) {
            throw new CloudRuntimeException(String.format("Unable to resolve FTCTL x-colo peer address for host %s", cmd.getPeerHostId()));
        }
        return address;
    }

    private Set<Integer> collectActiveXcoloPortSlots(Long currentVmId) {
        Set<Integer> used = new HashSet<>();
        List<FtctlProtectionVO> protections = ftctlProtectionDao.listActive();
        if (protections == null) {
            return used;
        }
        for (FtctlProtectionVO protection : protections) {
            if (protection == null || (currentVmId != null && protection.getPrimaryVmId() == currentVmId)) {
                continue;
            }
            if (protection.getXcoloPortSlot() != null) {
                used.add(protection.getXcoloPortSlot());
            }
        }
        return used;
    }

    private EndpointHostPort parseTcpEndpoint(String endpoint, String fieldName) {
        String value = StringUtils.trimToEmpty(endpoint);
        if (!StringUtils.startsWithIgnoreCase(value, "tcp:")) {
            throw new CloudRuntimeException(String.format("FTCTL %s must use tcp:<host>:<port>", fieldName));
        }
        return parseHostPort(StringUtils.substring(value, 4), fieldName);
    }

    private EndpointHostPort parseHostPort(String endpoint, String fieldName) {
        String value = StringUtils.trimToEmpty(endpoint);
        int separator = value.lastIndexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            throw new CloudRuntimeException(String.format("FTCTL %s must use <host>:<port>", fieldName));
        }
        String host = StringUtils.trimToEmpty(value.substring(0, separator));
        String portText = StringUtils.trimToEmpty(value.substring(separator + 1));
        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            throw new CloudRuntimeException(String.format("FTCTL %s port must be numeric: %s", fieldName, portText));
        }
        if (StringUtils.isBlank(host) || !isValidTcpPort(port)) {
            throw new CloudRuntimeException(String.format("FTCTL %s must use a valid host and TCP port", fieldName));
        }
        return new EndpointHostPort(host, port);
    }

    private boolean isValidTcpPort(int port) {
        return port > 0 && port <= 65535;
    }

    private static final class EndpointHostPort {
        private final String host;
        private final int port;

        private EndpointHostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }

        private String hostPort() {
            return String.format("%s:%s", host, port);
        }

        private boolean equalsHostPort(EndpointHostPort other) {
            return other != null && port == other.port && StringUtils.equalsIgnoreCase(host, other.host);
        }
    }

    private static final class XcoloPortAllocation {
        private final String mode;
        private final Integer slot;
        private final String remoteNbdExportAddr;
        private final String proxyEndpoint;
        private final String nbdEndpoint;
        private final String migrateUri;
        private final int mirrorPort;
        private final int comparePort;
        private final int compareLocalPort;
        private final int compareOutPort;
        private final int controlPort;
        private final int proxyPort;
        private final boolean enabled;

        private XcoloPortAllocation(String mode, Integer slot, String remoteNbdExportAddr, String proxyEndpoint, String nbdEndpoint, String migrateUri,
                                    int mirrorPort, int comparePort, int compareLocalPort, int compareOutPort,
                                    int controlPort, int proxyPort, boolean enabled) {
            this.mode = mode;
            this.slot = slot;
            this.remoteNbdExportAddr = remoteNbdExportAddr;
            this.proxyEndpoint = proxyEndpoint;
            this.nbdEndpoint = nbdEndpoint;
            this.migrateUri = migrateUri;
            this.mirrorPort = mirrorPort;
            this.comparePort = comparePort;
            this.compareLocalPort = compareLocalPort;
            this.compareOutPort = compareOutPort;
            this.controlPort = controlPort;
            this.proxyPort = proxyPort;
            this.enabled = enabled;
        }

        private static XcoloPortAllocation disabled(String remoteNbdExportAddr) {
            return new XcoloPortAllocation(null, null, remoteNbdExportAddr, null, null, null,
                    0, 0, 0, 0, 0, 0, false);
        }
    }

    private boolean isHostScopedStoragePool(StoragePoolVO storagePool) {
        return storagePool != null && storagePool.getScope() != null && "HOST".equalsIgnoreCase(storagePool.getScope().name());
    }

    @Override
    public FtctlHealthResponse getFtctlHealth(GetFtctlHealthCmd cmd) throws CloudRuntimeException {
        UserVmVO requestedVm = validateVirtualMachineExists(cmd.getVirtualMachineId());
        if (isRemoteMoldReplicaStandbyVm(requestedVm.getId())) {
            return buildRemoteMoldStandbyHealthResponse(requestedVm);
        }
        UserVmVO runtimeVm = resolveRuntimeVmForProtectionView(requestedVm);
        FtctlProtectionVO protection = findActiveProtectionForVm(requestedVm.getId());
        Long sourceVmId = protection != null ? protection.getPrimaryVmId() : runtimeVm.getId();
        Long hostId = getExecutionHostId(runtimeVm);
        List<FtctlEventResponse> events = fetchRuntimeEventResponses(runtimeVm, 100);
        FtctlEventResponse latestHealth = findLatestEvent(events, "libvirt.local");
        JsonObject latestHealthDetails = parseJsonObject(latestHealth != null ? latestHealth.getDetails() : null);
        FtctlHealthResponse response = new FtctlHealthResponse();
        response.setObjectName("ftctlhealth");
        response.setVirtualMachineId(requestedVm.getId());
        response.setHostId(hostId);
        response.setHostName(resolveHostName(hostId));
        response.setResult(StringUtils.defaultIfBlank(latestHealth != null ? latestHealth.getResult() : null,
                StringUtils.defaultIfBlank(getDetailValue(sourceVmId, DETAIL_HEALTH_RESULT), "not_available")));
        response.setUri(StringUtils.defaultIfBlank(getJsonString(latestHealthDetails, "uri"),
                StringUtils.defaultIfBlank(getDetailValue(sourceVmId, DETAIL_HEALTH_URI), resolvePeerUri(hostId))));
        response.setRc(latestHealth != null && latestHealth.getRc() != null
                ? latestHealth.getRc() : parseIntegerDetail(getDetailValue(sourceVmId, DETAIL_HEALTH_RC)));
        return response;
    }

    @Override
    public FtctlActionResponse executeFtctlAction(Long virtualMachineId, FtctlActionCommand.Action action, boolean force) throws CloudRuntimeException {
        UserVmVO userVm = validateVirtualMachineExists(virtualMachineId);
        validatePrimaryProtectionTarget(userVm);
        if (action == FtctlActionCommand.Action.FAILBACK) {
            FtctlProtectionVO protection = ftctlProtectionDao.findActiveByPrimaryVmId(userVm.getId());
            if (isCloudManagedProvisioning(protection)) {
                return executeCloudManagedFailback(userVm, protection, null);
            }
        }
        return executeFtctlAgentAction(userVm, action, force);
    }

    @Override
    public FtctlActionResponse failbackFtctlProtection(Long virtualMachineId, boolean force, String failbackTargetMoldType,
                                                       String remoteMoldApiUrl, String remoteMoldApiKey, String remoteMoldSecretKey,
                                                       String targetMoldApiUrl, String targetMoldApiKey, String targetMoldSecretKey)
            throws CloudRuntimeException {
        UserVmVO userVm = validateVirtualMachineExists(virtualMachineId);
        validatePrimaryProtectionTarget(userVm);
        FtctlProtectionVO protection = ftctlProtectionDao.findActiveByPrimaryVmId(userVm.getId());
        if (isCloudManagedProvisioning(protection)) {
            FailbackTargetContext targetContext = buildFailbackTargetContext(userVm, protection, failbackTargetMoldType,
                    remoteMoldApiUrl, remoteMoldApiKey, remoteMoldSecretKey, targetMoldApiUrl, targetMoldApiKey, targetMoldSecretKey);
            return executeCloudManagedFailback(userVm, protection, targetContext);
        }
        return executeFtctlAgentAction(userVm, FtctlActionCommand.Action.FAILBACK, force);
    }

    @Override
    public FtctlActionResponse releaseFtctlProtection(Long virtualMachineId, boolean force) throws CloudRuntimeException {
        UserVmVO primaryVm = validateVirtualMachineExists(virtualMachineId);
        validatePrimaryProtectionTarget(primaryVm);
        FtctlProtectionVO protection = ftctlProtectionDao.findActiveByPrimaryVmId(primaryVm.getId());
        if (protection == null) {
            throw new CloudRuntimeException(String.format("Unable to find active FTCTL protection for VM %s", primaryVm.getUuid()));
        }
        validateReleaseProtectionState(primaryVm, protection, force);

        List<String> forceWarnings = new ArrayList<>();
        FtctlActionResponse response;
        try {
            response = executeFtctlAgentAction(primaryVm, FtctlActionCommand.Action.UNPROTECT, force);
        } catch (CloudRuntimeException e) {
            if (!force) {
                throw e;
            }
            addForcedReleaseWarning(primaryVm, forceWarnings,
                    String.format("Host-side FTCTL unprotect failed; continuing forced release cleanup: %s", e.getMessage()));
            response = buildForcedReleaseFallbackResponse(primaryVm, e.getMessage());
        }
        if (force) {
            try {
                validateUnprotectRuntimeRelease(primaryVm, protection, response);
            } catch (CloudRuntimeException e) {
                addForcedReleaseWarning(primaryVm, forceWarnings,
                        String.format("Host-side FTCTL unprotect cleanup could not be fully verified: %s", e.getMessage()));
            }
        } else {
            validateUnprotectRuntimeRelease(primaryVm, protection, response);
        }
        try {
            releaseCloudManagedStandbyResources(primaryVm, protection);
        } catch (CloudRuntimeException e) {
            if (!force) {
                throw e;
            }
            addForcedReleaseWarning(primaryVm, forceWarnings,
                    String.format("Cloud-managed standby cleanup failed during forced release: %s", e.getMessage()));
        }
        releaseCloudManagedFtLifecycleGuard(primaryVm);
        markProtectionRowsRemoved(protection);
        vmInstanceDetailsDao.removeDetailsWithPrefix(primaryVm.getId(), "ftctl.");
        applyForcedReleaseWarnings(response, forceWarnings);
        response.setAction(FtctlActionCommand.Action.UNPROTECT.name());
        response.setProtectionState("disabled");
        response.setTransportState("stopped");
        response.setActiveSide("primary");
        response.setAdminState("inactive");
        response.setFencingState("clear");
        response.setLastError(null);
        publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_RELEASE,
                String.format("%s FTCTL protection for VM %s",
                        force ? "Force released" : "Released", primaryVm.getUuid()));
        return response;
    }

    @Override
    public FtctlActionResponse releaseFtctlDrReplicaProtection(Long virtualMachineId, boolean force, boolean cleanupTransport,
                                                               boolean abandonSource) throws CloudRuntimeException {
        UserVmVO replicaVm = validateRemoteMoldReplicaVmForSiteRecovery(virtualMachineId, false);
        List<String> warnings = new ArrayList<>();
        if (cleanupTransport) {
            cleanupReplicaSiteTransportState(replicaVm, force, warnings);
        }
        clearRemoteMoldReplicaSession(replicaVm);
        FtctlActionResponse response = buildReplicaSiteRecoveryActionResponse(replicaVm,
                ReleaseFtctlDrReplicaProtectionCmd.APINAME, warnings.isEmpty() ? "ok" : "warn",
                "Remote FTCTL DR replica protection released without deleting the replica VM or volumes");
        applyForcedReleaseWarnings(response, warnings);
        publishFtctlEvent(replicaVm, EventTypes.EVENT_FTCTL_PROTECTION_RELEASE,
                String.format("Released remote FTCTL DR replica protection for VM %s without deleting replica resources", replicaVm.getUuid()));
        return response;
    }

    @Override
    public FtctlActionResponse adoptFtctlDrReplica(Long virtualMachineId, boolean cleanupTransport) throws CloudRuntimeException {
        UserVmVO replicaVm = validateRemoteMoldReplicaVmForSiteRecovery(virtualMachineId, true);
        List<String> warnings = new ArrayList<>();
        if (cleanupTransport) {
            cleanupReplicaSiteTransportState(replicaVm, true, warnings);
        }
        clearRemoteMoldReplicaSession(replicaVm);
        FtctlActionResponse response = buildReplicaSiteRecoveryActionResponse(replicaVm,
                AdoptFtctlDrReplicaCmd.APINAME, warnings.isEmpty() ? "ok" : "warn",
                "Remote FTCTL DR replica adopted as an independent production VM");
        applyForcedReleaseWarnings(response, warnings);
        publishFtctlEvent(replicaVm, EventTypes.EVENT_FTCTL_PROTECTION_RELEASE,
                String.format("Adopted remote FTCTL DR replica VM %s as an independent production VM", replicaVm.getUuid()));
        return response;
    }

    @Override
    public FtctlActionResponse failbackFtctlDrReplica(Long virtualMachineId,
                                                      String targetMoldApiUrl, String targetMoldApiKey, String targetMoldSecretKey,
                                                      String replicaMoldApiUrl, String replicaMoldApiKey, String replicaMoldSecretKey)
            throws CloudRuntimeException {
        UserVmVO replicaVm = validateRemoteMoldReplicaVmForSiteRecovery(virtualMachineId, true);
        validateReplicaSiteDelegatedFailbackState(replicaVm);

        RemoteMoldCredentials targetMoldCredentials = buildRequiredRemoteMoldCredentials("target/source Mold",
                targetMoldApiUrl, targetMoldApiKey, targetMoldSecretKey);
        RemoteMoldCredentials replicaMoldCredentials = buildRequiredRemoteMoldCredentials("current replica Mold",
                replicaMoldApiUrl, replicaMoldApiKey, replicaMoldSecretKey);

        JsonObject sourceVm = resolveReplicaFailbackSourceVm(replicaVm, targetMoldCredentials);
        String sourceVmUuid = getJsonString(sourceVm, "id");
        validateDelegatedSourceProtection(replicaVm, targetMoldCredentials, sourceVmUuid);

        Map<String, String> params = new HashMap<>();
        params.put("virtualmachineid", sourceVmUuid);
        params.put("failbacktargetmoldtype", FAILBACK_TARGET_MOLD_ORIGINAL_PRIMARY);
        params.put("remotemoldapiurl", replicaMoldCredentials.apiUrl);
        params.put("remotemoldapikey", replicaMoldCredentials.apiKey);
        params.put("remotemoldsecretkey", replicaMoldCredentials.secretKey);
        JsonObject delegatedResponse = callRemoteMoldApi(targetMoldCredentials.apiUrl, targetMoldCredentials.apiKey,
                targetMoldCredentials.secretKey, "failbackFtctlProtection", params);

        String delegatedJobId = getRemoteMoldResponseString(delegatedResponse, "failbackftctlprotectionresponse", "jobid");
        FtctlActionResponse response = buildReplicaSiteDelegatedFailbackResponse(replicaVm, sourceVmUuid, delegatedJobId);
        publishFtctlEvent(replicaVm, EventTypes.EVENT_FTCTL_PROTECTION_FAILBACK,
                String.format("Delegated FTCTL DR replica failback for VM %s to source Mold VM %s%s",
                        replicaVm.getUuid(), sourceVmUuid,
                        StringUtils.isNotBlank(delegatedJobId) ? String.format(" with remote job %s", delegatedJobId) : ""));
        return response;
    }

    private UserVmVO validateRemoteMoldReplicaVmForSiteRecovery(Long virtualMachineId, boolean requireRunning) {
        UserVmVO replicaVm = validateVirtualMachineExists(virtualMachineId);
        if (!isRemoteMoldReplicaStandbyVm(replicaVm.getId())) {
            throw new CloudRuntimeException(String.format("VM %s is not an FTCTL remote DR replica recovery target", replicaVm.getUuid()));
        }
        if (requireRunning && replicaVm.getState() != VirtualMachine.State.Running) {
            throw new CloudRuntimeException(String.format("FTCTL DR replica site recovery requires VM %s to be Running, current state is %s",
                    replicaVm.getUuid(), replicaVm.getState()));
        }
        return replicaVm;
    }

    private void validateReplicaSiteDelegatedFailbackState(UserVmVO replicaVm) {
        if (replicaVm == null || replicaVm.getState() != VirtualMachine.State.Running) {
            throw new CloudRuntimeException("FTCTL DR replica-side failback requires the replica VM to be Running");
        }
    }

    private RemoteMoldCredentials buildRequiredRemoteMoldCredentials(String label, String apiUrl, String apiKey, String secretKey) {
        if (StringUtils.isAnyBlank(apiUrl, apiKey, secretKey)) {
            throw new CloudRuntimeException(String.format("%s API URL, API key, and secret key are required", label));
        }
        return new RemoteMoldCredentials(StringUtils.trim(apiUrl), StringUtils.trim(apiKey), StringUtils.trim(secretKey));
    }

    private JsonObject resolveReplicaFailbackSourceVm(UserVmVO replicaVm, RemoteMoldCredentials targetMoldCredentials) {
        String sourceVmUuid = StringUtils.trimToNull(getDetailValue(replicaVm.getId(), DETAIL_REMOTE_SOURCE_VM_UUID));
        String sourceVmName = StringUtils.trimToNull(getDetailValue(replicaVm.getId(), DETAIL_REMOTE_SOURCE_VM_NAME));
        String sourceVmInstanceName = StringUtils.trimToNull(getDetailValue(replicaVm.getId(), DETAIL_REMOTE_SOURCE_VM_INSTANCE_NAME));

        JsonObject sourceVm = fetchRemoteMoldVirtualMachine(targetMoldCredentials, sourceVmUuid);
        if (sourceVm == null && StringUtils.isNotBlank(sourceVmName)) {
            sourceVm = findRemoteMoldVirtualMachineByName(targetMoldCredentials, sourceVmName);
        }
        if (sourceVm == null && StringUtils.isNotBlank(sourceVmInstanceName)) {
            sourceVm = findRemoteMoldVirtualMachineByName(targetMoldCredentials, sourceVmInstanceName);
        }
        if (sourceVm == null) {
            throw new CloudRuntimeException(String.format(
                    "Replica-side FTCTL DR failback for VM %s requires an existing source VM and source protection row on the target/source Mold. New or rebuilt Mold failback is not available in this delegated build.",
                    replicaVm.getUuid()));
        }
        String resolvedUuid = getJsonString(sourceVm, "id");
        if (StringUtils.isBlank(resolvedUuid)) {
            throw new CloudRuntimeException(String.format("Target/source Mold returned a source VM without an id for replica VM %s",
                    replicaVm.getUuid()));
        }
        return sourceVm;
    }

    private JsonObject findRemoteMoldVirtualMachineByName(RemoteMoldCredentials credentials, String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }
        Map<String, String> params = new HashMap<>();
        params.put("name", name);
        params.put("listall", "true");
        JsonObject json = callRemoteMoldApi(credentials.apiUrl, credentials.apiKey, credentials.secretKey,
                "listVirtualMachines", params);
        JsonObject vm = extractFirstRemoteMoldVirtualMachine(json);
        if (vm != null) {
            return vm;
        }
        params.clear();
        params.put("keyword", name);
        params.put("listall", "true");
        json = callRemoteMoldApi(credentials.apiUrl, credentials.apiKey, credentials.secretKey,
                "listVirtualMachines", params);
        return extractFirstRemoteMoldVirtualMachine(json);
    }

    private JsonObject extractFirstRemoteMoldVirtualMachine(JsonObject json) {
        JsonArray vms = getResponseArray(json, "listvirtualmachinesresponse", "virtualmachine");
        if (vms == null || vms.size() == 0 || !vms.get(0).isJsonObject()) {
            return null;
        }
        return vms.get(0).getAsJsonObject();
    }

    private void validateDelegatedSourceProtection(UserVmVO replicaVm, RemoteMoldCredentials targetMoldCredentials, String sourceVmUuid) {
        Map<String, String> params = new HashMap<>();
        params.put("virtualmachineid", sourceVmUuid);
        JsonObject json = callRemoteMoldApi(targetMoldCredentials.apiUrl, targetMoldCredentials.apiKey,
                targetMoldCredentials.secretKey, "getFtctlProtection", params);
        JsonObject response = getResponseObject(json, "getftctlprotectionresponse");
        JsonObject protection = getJsonObject(response, "ftctlprotection");
        if (protection == null) {
            protection = response;
        }
        String role = getJsonString(protection, "protectionrole");
        String mode = getJsonString(protection, "mode");
        String enabled = getJsonString(protection, "enabled");
        if (!"primary".equalsIgnoreCase(StringUtils.trimToEmpty(role)) ||
                !"dr".equalsIgnoreCase(StringUtils.trimToEmpty(mode)) ||
                !"true".equalsIgnoreCase(StringUtils.trimToEmpty(enabled))) {
            throw new CloudRuntimeException(String.format(
                    "Replica-side FTCTL DR failback for VM %s can delegate only to a target/source Mold that still has the active source-side DR protection row for VM %s. Use the full replica-controller recovery workflow for a new or rebuilt Mold.",
                    replicaVm.getUuid(), sourceVmUuid));
        }
    }

    private FtctlActionResponse buildReplicaSiteDelegatedFailbackResponse(UserVmVO replicaVm, String sourceVmUuid, String delegatedJobId) {
        FtctlActionResponse response = new FtctlActionResponse();
        response.setObjectName("ftctlaction");
        response.setVirtualMachineId(replicaVm.getId());
        response.setVmName(replicaVm.getInstanceName());
        response.setAction(FailbackFtctlDrReplicaCmd.APINAME);
        response.setResult("delegated");
        response.setExitCode(0);
        response.setOutput(StringUtils.isNotBlank(delegatedJobId)
                ? String.format("Delegated failback to source Mold VM %s, remote job %s", sourceVmUuid, delegatedJobId)
                : String.format("Delegated failback to source Mold VM %s", sourceVmUuid));
        response.setMode("dr");
        response.setProtectionState("failed_over");
        response.setTransportState("failed_over");
        response.setActiveSide("secondary");
        response.setAdminState("active");
        response.setFencingState("clear");
        response.setLastError(null);
        return response;
    }

    private void cleanupReplicaSiteTransportState(UserVmVO replicaVm, boolean force, List<String> warnings) {
        Long hostId = getExecutionHostId(replicaVm);
        if (hostId == null) {
            String warning = String.format("Unable to resolve execution host for replica VM %s; skipped host-side FTCTL transport cleanup",
                    replicaVm.getUuid());
            if (force) {
                addReplicaSiteRecoveryWarning(replicaVm, warnings, warning);
                return;
            }
            throw new CloudRuntimeException(warning);
        }
        String profile = StringUtils.defaultIfBlank(getDetailValue(replicaVm.getId(), DETAIL_REMOTE_SOURCE_VM_INSTANCE_NAME),
                StringUtils.defaultIfBlank(getDetailValue(replicaVm.getId(), DETAIL_REMOTE_SOURCE_VM_NAME), replicaVm.getInstanceName()));
        try {
            FtctlActionCommand unprotectCommand = new FtctlActionCommand(FtctlActionCommand.Action.UNPROTECT, profile);
            unprotectCommand.setForce(true);
            unprotectCommand.setForceCleanup(true);
            unprotectCommand.setWait(UNPROTECT_ACTION_WAIT_SECONDS);
            Answer answer = agentManager.send(hostId, unprotectCommand);
            if (!(answer instanceof FtctlActionAnswer) || !answer.getResult()) {
                throw new CloudRuntimeException(answer != null ? answer.getDetails() : "no answer");
            }
        } catch (CloudRuntimeException | AgentUnavailableException | OperationTimedoutException e) {
            String warning = String.format("Host-side FTCTL replica transport cleanup failed on host %s for VM %s: %s",
                    hostId, replicaVm.getUuid(), e.getMessage());
            if (!force) {
                throw new CloudRuntimeException(warning, e);
            }
            addReplicaSiteRecoveryWarning(replicaVm, warnings, warning);
        }
        try {
            FtctlDrSshAccessCommand removeKeyCommand = new FtctlDrSshAccessCommand(FtctlDrSshAccessCommand.Action.REMOVE_KEY, profile);
            removeKeyCommand.setSshUser(DEFAULT_REMOTE_PEER_SSH_USER);
            Answer answer = agentManager.send(hostId, removeKeyCommand);
            if (!(answer instanceof FtctlSyncAnswer) || !answer.getResult()) {
                throw new CloudRuntimeException(answer != null ? answer.getDetails() : "no answer");
            }
        } catch (CloudRuntimeException | AgentUnavailableException | OperationTimedoutException e) {
            addReplicaSiteRecoveryWarning(replicaVm, warnings,
                    String.format("Replica host DR SSH key cleanup was best-effort and did not complete on host %s for VM %s: %s",
                            hostId, replicaVm.getUuid(), e.getMessage()));
        }
    }

    private void addReplicaSiteRecoveryWarning(UserVmVO replicaVm, List<String> warnings, String warning) {
        String message = StringUtils.abbreviate(StringUtils.defaultString(warning), 1024);
        warnings.add(message);
        logger.warn(message);
        putVmDetail(replicaVm.getId(), DETAIL_DR_RECOVERY_LAST_ERROR, message);
    }

    private void clearRemoteMoldReplicaSession(UserVmVO replicaVm) {
        vmInstanceDetailsDao.removeDetailsWithPrefix(replicaVm.getId(), "ftctl.");
    }

    private FtctlActionResponse buildReplicaSiteRecoveryActionResponse(UserVmVO replicaVm, String action, String result, String output) {
        FtctlActionResponse response = new FtctlActionResponse();
        response.setObjectName("ftctlaction");
        response.setVirtualMachineId(replicaVm.getId());
        response.setVmName(replicaVm.getInstanceName());
        response.setAction(action);
        response.setResult(result);
        response.setExitCode(0);
        response.setOutput(output);
        response.setMode("dr");
        response.setProtectionState("disabled");
        response.setTransportState("stopped");
        response.setActiveSide("primary");
        response.setAdminState("inactive");
        response.setFencingState("clear");
        response.setLastError(null);
        return response;
    }

    private FtctlActionResponse buildForcedReleaseFallbackResponse(UserVmVO primaryVm, String errorMessage) {
        FtctlActionResponse response = new FtctlActionResponse();
        response.setObjectName("ftctlaction");
        response.setVirtualMachineId(primaryVm.getId());
        response.setVmName(primaryVm.getInstanceName());
        response.setAction(FtctlActionCommand.Action.UNPROTECT.name());
        response.setResult("warn");
        response.setOutput(String.format("Forced release continued after host-side FTCTL unprotect failure: %s",
                StringUtils.abbreviate(StringUtils.defaultString(errorMessage), 1024)));
        return response;
    }

    private void addForcedReleaseWarning(UserVmVO primaryVm, List<String> warnings, String warning) {
        String message = StringUtils.abbreviate(StringUtils.defaultString(warning), 1024);
        warnings.add(message);
        logger.warn(message);
        publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_RELEASE,
                String.format("FTCTL forced protection release warning for VM %s: %s",
                        primaryVm.getUuid(), StringUtils.abbreviate(message, 512)));
    }

    private void applyForcedReleaseWarnings(FtctlActionResponse response, List<String> warnings) {
        if (warnings.isEmpty()) {
            return;
        }
        response.setResult("warn");
        String warningOutput = String.format("Forced release warnings: %s", StringUtils.join(warnings, "; "));
        response.setOutput(StringUtils.trimToEmpty(response.getOutput()) + "\n" + warningOutput);
    }

    private void validateReleaseProtectionState(UserVmVO primaryVm, FtctlProtectionVO protection, boolean force) {
        String activeSide = StringUtils.trimToEmpty(protection.getActiveSide()).toLowerCase(Locale.ROOT);
        if (!force && StringUtils.isNotBlank(activeSide) && !"primary".equals(activeSide)) {
            throw new CloudRuntimeException(String.format("FTCTL protection release for VM %s requires active side primary, current active side is %s. Fail back first.",
                    primaryVm.getUuid(), protection.getActiveSide()));
        }
    }

    private void validateUnprotectRuntimeRelease(UserVmVO primaryVm, FtctlProtectionVO protection, FtctlActionResponse response) {
        if (!"remote-nbd".equalsIgnoreCase(StringUtils.trimToEmpty(protection.getBackendMode()))) {
            return;
        }
        String output = StringUtils.trimToEmpty(response.getOutput());
        try {
            JsonObject object = JsonParser.parseString(output).getAsJsonObject();
            boolean remoteNbdRequired = object.has("remote_nbd_required") && object.get("remote_nbd_required").getAsBoolean();
            boolean remoteNbdReleased = object.has("remote_nbd_released") && object.get("remote_nbd_released").getAsBoolean();
            if (remoteNbdRequired && remoteNbdReleased) {
                return;
            }
        } catch (RuntimeException e) {
            markProtectionReleaseFailed(primaryVm, protection,
                    String.format("remote_nbd_release_unverified: invalid unprotect output: %s", output));
            throw new CloudRuntimeException(String.format("Unable to verify remote-NBD cleanup for FTCTL protection release of VM %s: %s",
                    primaryVm.getUuid(), output), e);
        }

        markProtectionReleaseFailed(primaryVm, protection,
                String.format("remote_nbd_release_unverified: %s", output));
        throw new CloudRuntimeException(String.format("FTCTL remote-NBD cleanup was not confirmed for protection release of VM %s: %s",
                primaryVm.getUuid(), output));
    }

    private void markProtectionReleaseFailed(UserVmVO primaryVm, FtctlProtectionVO protection, String lastError) {
        protection.setProtectionState("error");
        protection.setTransportState("failed");
        protection.setLastError(StringUtils.abbreviate(lastError, 1024));
        ftctlProtectionDao.update(protection.getId(), protection);
        publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_RELEASE,
                String.format("FTCTL protection release failed for VM %s: %s", primaryVm.getUuid(), StringUtils.abbreviate(lastError, 512)));
    }

    private void releaseCloudManagedStandbyResources(UserVmVO primaryVm, FtctlProtectionVO protection) {
        if (!isCloudManagedProvisioning(protection) || protection.getSecondaryVmId() == null) {
            return;
        }
        List<Long> secondaryVolumeIds = collectSecondaryVolumeIdsForProtection(protection);
        UserVmVO secondaryVm = userVmDao.findById(protection.getSecondaryVmId());
        if (secondaryVm == null || secondaryVm.getRemoved() != null) {
            destroySecondaryVolumes(primaryVm, secondaryVolumeIds);
            return;
        }
        if (secondaryVm.getState() == VirtualMachine.State.Running) {
            stopSecondaryVmForProtectionRelease(primaryVm, secondaryVm);
            secondaryVm = validateVirtualMachineExists(secondaryVm.getId());
        }
        try {
            userVmService.destroyVm(secondaryVm.getId(), true);
            UserVmVO destroyedSecondaryVm = userVmDao.findById(secondaryVm.getId());
            if (destroyedSecondaryVm != null && destroyedSecondaryVm.getRemoved() == null) {
                boolean expunged = userVmManager.expunge(destroyedSecondaryVm);
                if (!expunged) {
                    throw new CloudRuntimeException(String.format("Cloud-managed expunge returned false for FTCTL secondary VM %s", secondaryVm.getUuid()));
                }
            }
            destroySecondaryVolumes(primaryVm, secondaryVolumeIds);
            publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                    String.format("Destroyed and expunged FTCTL secondary VM %s during protection release for primary VM %s",
                            secondaryVm.getUuid(), primaryVm.getUuid()));
        } catch (ResourceUnavailableException | ConcurrentOperationException e) {
            throw new CloudRuntimeException(String.format("Unable to destroy FTCTL secondary VM %s for primary VM %s during protection release",
                    secondaryVm.getUuid(), primaryVm.getUuid()), e);
        }
    }

    private List<Long> collectSecondaryVolumeIdsForProtection(FtctlProtectionVO protection) {
        List<Long> secondaryVolumeIds = new ArrayList<>();
        List<FtctlProtectionVolumeVO> volumes = ftctlProtectionVolumeDao.listActiveByProtectionId(protection.getId());
        if (volumes != null) {
            for (FtctlProtectionVolumeVO volume : volumes) {
                if (volume.getSecondaryVolumeId() != null && !secondaryVolumeIds.contains(volume.getSecondaryVolumeId())) {
                    secondaryVolumeIds.add(volume.getSecondaryVolumeId());
                }
            }
        }
        if (protection.getSecondaryVmId() != null) {
            List<VolumeVO> attachedVolumes = volumeDao.findByInstance(protection.getSecondaryVmId());
            if (attachedVolumes != null) {
                for (VolumeVO volume : attachedVolumes) {
                    if (volume != null && !secondaryVolumeIds.contains(volume.getId())) {
                        secondaryVolumeIds.add(volume.getId());
                    }
                }
            }
        }
        return secondaryVolumeIds;
    }

    private void destroySecondaryVolumes(UserVmVO primaryVm, List<Long> secondaryVolumeIds) {
        if (secondaryVolumeIds == null || secondaryVolumeIds.isEmpty()) {
            return;
        }
        for (Long volumeId : secondaryVolumeIds) {
            VolumeVO volume = volumeId != null ? volumeDao.findById(volumeId) : null;
            if (volume == null || volume.getRemoved() != null || volume.getState() == Volume.State.Expunged) {
                continue;
            }
            if (volume.getInstanceId() != null) {
                volumeDao.detachVolume(volume.getId());
            }
            try {
                volumeApiService.destroyVolume(volume.getId(), CallContext.current().getCallingAccount(), true, true);
            } catch (RuntimeException e) {
                throw new CloudRuntimeException(String.format("Unable to destroy FTCTL secondary volume %s for primary VM %s during protection release",
                        volume.getUuid(), primaryVm.getUuid()), e);
            }
        }
    }

    private void stopSecondaryVmForProtectionRelease(UserVmVO primaryVm, UserVmVO secondaryVm) {
        try {
            userVmService.stopVirtualMachine(secondaryVm.getId(), true);
            publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                    String.format("Stopped FTCTL secondary VM %s during protection release for primary VM %s",
                            secondaryVm.getUuid(), primaryVm.getUuid()));
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException(String.format("Unable to stop FTCTL secondary VM %s for primary VM %s during protection release",
                    secondaryVm.getUuid(), primaryVm.getUuid()), e);
        }
    }

    private void markProtectionRowsRemoved(FtctlProtectionVO protection) {
        List<FtctlProtectionVolumeVO> volumes = ftctlProtectionVolumeDao.listActiveByProtectionId(protection.getId());
        if (volumes != null) {
            for (FtctlProtectionVolumeVO volume : volumes) {
                volume.markRemoved();
                ftctlProtectionVolumeDao.update(volume.getId(), volume);
                ftctlProtectionVolumeDao.remove(volume.getId());
            }
        }
        protection.setProtectionState("disabled");
        protection.setTransportState("stopped");
        protection.setActiveSide("primary");
        protection.setAdminState("inactive");
        protection.setFencingState("clear");
        protection.setLastError(null);
        protection.markRemoved();
        ftctlProtectionDao.update(protection.getId(), protection);
        ftctlProtectionDao.remove(protection.getId());
    }

    private FtctlActionResponse executeFtctlAgentAction(UserVmVO userVm, FtctlActionCommand.Action action, boolean force) throws CloudRuntimeException {
        Long hostId = requireExecutionHostId(userVm);
        try {
            FtctlActionCommand actionCommand = new FtctlActionCommand(action, userVm.getInstanceName());
            actionCommand.setForce(force || action == FtctlActionCommand.Action.FAILOVER || action == FtctlActionCommand.Action.FAILOVER_PREPARE ||
                    action == FtctlActionCommand.Action.FAILBACK ||
                    action == FtctlActionCommand.Action.FAILBACK_SYNC || action == FtctlActionCommand.Action.FAILBACK_FINALIZE ||
                    action == FtctlActionCommand.Action.FAILBACK_REPROTECT ||
                    action == FtctlActionCommand.Action.UNPROTECT);
            actionCommand.setForceCleanup(force && action == FtctlActionCommand.Action.UNPROTECT);
            if (action == FtctlActionCommand.Action.FAILBACK ||
                    action == FtctlActionCommand.Action.FAILBACK_FINALIZE || action == FtctlActionCommand.Action.FAILBACK_REPROTECT) {
                actionCommand.setWait(FAILBACK_ACTION_WAIT_SECONDS);
            } else if (action == FtctlActionCommand.Action.UNPROTECT) {
                actionCommand.setWait(UNPROTECT_ACTION_WAIT_SECONDS);
            }
            FtctlActionAnswer actionAnswer = sendFtctlActionWithLockRetry(hostId, actionCommand, userVm);
            return buildFtctlActionResponse(userVm, action, actionAnswer);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException(String.format("Unable to execute FTCTL action %s for VM %s", action, userVm.getUuid()), e);
        }
    }

    private FtctlActionAnswer sendFtctlActionWithLockRetry(Long hostId, FtctlActionCommand actionCommand, UserVmVO userVm)
            throws AgentUnavailableException, OperationTimedoutException {
        long deadline = System.currentTimeMillis() + FTCTL_ACTION_LOCK_RETRY_TIMEOUT_MILLIS;
        int attempt = 0;
        while (true) {
            attempt++;
            Answer answer = agentManager.send(hostId, actionCommand);
            if (!(answer instanceof FtctlActionAnswer)) {
                throw new CloudRuntimeException(String.format("Unexpected FTCTL action answer type for VM %s", userVm.getUuid()));
            }
            FtctlActionAnswer actionAnswer = (FtctlActionAnswer) answer;
            if (actionAnswer.getResult()) {
                if (attempt > 1) {
                    logger.info(String.format("FTCTL action %s for VM %s succeeded after %s lock retry attempt(s)",
                            actionCommand.getAction(), userVm.getUuid(), attempt - 1));
                }
                return actionAnswer;
            }
            if (!isRetryableFtctlLock(actionAnswer)) {
                throw new CloudRuntimeException(String.format("FTCTL action %s failed for VM %s: %s",
                        actionCommand.getAction(), userVm.getUuid(), actionAnswer.getDetails()));
            }
            if (isManualFailoverContinuationAction(actionCommand.getAction()) &&
                    isManualFailoverTerminalSuccess(ftctlProtectionDao.findActiveByPrimaryVmId(userVm.getId()))) {
                throw new FtctlActionLockedException(String.format("FTCTL action %s for VM %s was locked after final failover state had already converged: %s",
                        actionCommand.getAction(), userVm.getUuid(), actionAnswer.getDetails()), actionAnswer);
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new FtctlActionLockedException(String.format("FTCTL action %s for VM %s remained locked after %s ms: %s",
                        actionCommand.getAction(), userVm.getUuid(), FTCTL_ACTION_LOCK_RETRY_TIMEOUT_MILLIS,
                        actionAnswer.getDetails()), actionAnswer);
            }
            logger.info(String.format("FTCTL action %s for VM %s is locked by another ftctl process; retrying in %s ms",
                    actionCommand.getAction(), userVm.getUuid(), FTCTL_ACTION_LOCK_RETRY_INTERVAL_MILLIS));
            sleepBeforeFtctlLockRetry(actionCommand.getAction(), userVm);
        }
    }

    private void sleepBeforeFtctlLockRetry(FtctlActionCommand.Action action, UserVmVO userVm) {
        try {
            Thread.sleep(FTCTL_ACTION_LOCK_RETRY_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CloudRuntimeException(String.format("Interrupted while waiting to retry FTCTL action %s for VM %s",
                    action, userVm.getUuid()), e);
        }
    }

    private boolean isRetryableFtctlLock(FtctlActionAnswer actionAnswer) {
        if (actionAnswer == null) {
            return false;
        }
        if (Integer.valueOf(FTCTL_ACTION_LOCK_EXIT_CODE).equals(actionAnswer.getExitCode())) {
            return true;
        }
        if ("locked".equalsIgnoreCase(StringUtils.trimToEmpty(actionAnswer.getFtctlResult()))) {
            return true;
        }
        String details = StringUtils.defaultString(actionAnswer.getDetails());
        String output = StringUtils.defaultString(actionAnswer.getOutput());
        return details.contains("\"result\":\"locked\"") || output.contains("\"result\":\"locked\"");
    }

    private boolean isManualFailoverContinuationAction(FtctlActionCommand.Action action) {
        return action == FtctlActionCommand.Action.FENCE_CONFIRM || action == FtctlActionCommand.Action.FAILOVER;
    }

    private FtctlActionResponse buildFtctlActionResponse(UserVmVO userVm, FtctlActionCommand.Action action, FtctlActionAnswer actionAnswer) {
        FtctlActionResponse response = new FtctlActionResponse();
        response.setObjectName("ftctlaction");
        response.setVirtualMachineId(userVm.getId());
        response.setVmName(userVm.getInstanceName());
        response.setAction(action.name());
        response.setResult(actionAnswer.getFtctlResult());
        response.setExitCode(actionAnswer.getExitCode());
        response.setOutput(actionAnswer.getOutput());
        FtctlStatusAnswer statusAnswer = fetchRuntimeStatus(userVm);
        if (statusAnswer != null) {
            response.setMode(statusAnswer.getMode());
            response.setProtectionState(statusAnswer.getProtectionState());
            response.setTransportState(statusAnswer.getTransportState());
            response.setActiveSide(statusAnswer.getActiveSide());
            response.setAdminState(statusAnswer.getAdminState());
            response.setFencingState(statusAnswer.getFencingState());
            response.setLastError(statusAnswer.getLastError());
            persistRuntimeState(userVm, statusAnswer);
            updateFailoverReadyMarker(userVm, action, statusAnswer);
        }
        publishFtctlEvent(userVm, resolveFtctlActionEventType(action),
                String.format("Executed FTCTL action %s for VM %s", action.name(), userVm.getUuid()));
        return response;
    }

    private boolean isCloudManagedProvisioning(FtctlProtectionVO protection) {
        return protection != null &&
                FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED.equalsIgnoreCase(StringUtils.trimToEmpty(protection.getProvisioningBackend()));
    }

    private FtctlActionResponse executeCloudManagedFailback(UserVmVO primaryVm, FtctlProtectionVO protection, FailbackTargetContext targetContext) {
        FtctlProtectionVO latestProtection = syncFailbackRuntimeStateAndRefreshProtection(primaryVm);
        if (isCloudManagedFailbackReverseReady(latestProtection)) {
            FailbackTargetContext effectiveContext = targetContext != null ? targetContext : loadCloudManagedFailbackContext(primaryVm, latestProtection);
            continueCloudManagedFailbackAfterReverseSync(primaryVm, latestProtection, effectiveContext, true);
            latestProtection = refreshProtection(primaryVm);
            return buildActionResponseFromProtection(primaryVm, FtctlActionCommand.Action.FAILBACK, latestProtection,
                    "cloud-managed failback cutback continued");
        }
        if (isCloudManagedFailbackInProgress(latestProtection)) {
            storeCloudManagedFailbackContext(primaryVm, latestProtection, targetContext);
            latestProtection = refreshProtection(primaryVm);
            return buildActionResponseFromProtection(primaryVm, FtctlActionCommand.Action.FAILBACK, latestProtection,
                    "cloud-managed failback already in progress");
        }

        storeCloudManagedFailbackContext(primaryVm, latestProtection, targetContext);
        try {
            FtctlActionResponse response = executeFtctlAgentAction(primaryVm, FtctlActionCommand.Action.FAILBACK_SYNC, true);
            response.setAction(FtctlActionCommand.Action.FAILBACK.name());
            latestProtection = syncFailbackRuntimeStateAndRefreshProtection(primaryVm);
            if (isCloudManagedFailbackReverseReady(latestProtection)) {
                FailbackTargetContext effectiveContext = targetContext != null ? targetContext : loadCloudManagedFailbackContext(primaryVm, latestProtection);
                continueCloudManagedFailbackAfterReverseSync(primaryVm, latestProtection, effectiveContext, true);
                latestProtection = refreshProtection(primaryVm);
                return buildActionResponseFromProtection(primaryVm, FtctlActionCommand.Action.FAILBACK, latestProtection,
                        "cloud-managed failback cutback continued after reverse sync ready");
            }
            publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                    String.format("Started cloud-managed FTCTL failback reverse sync for VM %s", primaryVm.getUuid()));
            return response;
        } catch (RuntimeException e) {
            clearCloudManagedFailbackContext(latestProtection);
            throw e;
        }
    }

    private void reconcileCloudManagedFailbacks() {
        pruneExpiredCloudManagedFailbackContexts();
        List<FtctlProtectionVO> protections = ftctlProtectionDao.listActiveByProtectionState("failing_back");
        for (FtctlProtectionVO protection : protections) {
            UserVmVO primaryVm = protection != null ? userVmDao.findById(protection.getPrimaryVmId()) : null;
            if (primaryVm == null || primaryVm.getRemoved() != null) {
                continue;
            }
            try {
                reconcileCloudManagedFailback(primaryVm, protection);
            } catch (RuntimeException e) {
                logger.warn(String.format("Unable to reconcile cloud-managed FTCTL failback for VM %s", primaryVm.getUuid()), e);
            }
        }
    }

    private FtctlProtectionVO syncFailbackRuntimeStateAndRefreshProtection(UserVmVO primaryVm) {
        FtctlStatusAnswer statusAnswer = fetchRuntimeStatus(primaryVm);
        if (statusAnswer != null) {
            persistRuntimeState(primaryVm, statusAnswer);
        }
        return refreshProtection(primaryVm);
    }

    private void reconcileCloudManagedFailback(UserVmVO primaryVm, FtctlProtectionVO protection) {
        if (!isCloudManagedFailbackCandidate(protection)) {
            return;
        }
        FtctlStatusAnswer statusAnswer = fetchRuntimeStatus(primaryVm);
        if (statusAnswer != null) {
            persistRuntimeState(primaryVm, statusAnswer);
        }
        FtctlProtectionVO latestProtection = refreshProtection(primaryVm);
        if (isCloudManagedFailbackReverseReady(latestProtection)) {
            continueCloudManagedFailbackAfterReverseSync(primaryVm, latestProtection,
                    loadCloudManagedFailbackContext(primaryVm, latestProtection), false);
        } else if (isCloudManagedFailbackInProgress(latestProtection)) {
            touchCloudManagedFailbackContext(latestProtection);
        }
    }

    private void continueCloudManagedFailbackAfterReverseSync(UserVmVO primaryVm, FtctlProtectionVO protection,
                                                             FailbackTargetContext targetContext, boolean operatorRequested) {
        Object marker = new Object();
        if (cloudManagedFailbackCutbackLocks.putIfAbsent(protection.getId(), marker) != null) {
            return;
        }
        try {
            FtctlProtectionVO latestProtection = refreshProtection(primaryVm);
            if (!isCloudManagedFailbackReverseReady(latestProtection)) {
                return;
            }
            if (isRemoteMoldDrProtection(primaryVm, latestProtection)) {
                continueRemoteMoldDrFailbackAfterReverseSync(primaryVm, latestProtection, targetContext, operatorRequested);
                return;
            }
            UserVmVO secondaryVm = resolveSecondaryVmForManualFailover(primaryVm, latestProtection);
            Long primaryHostId = requireExecutionHostId(primaryVm);

            publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                    String.format("Stopping FTCTL secondary VM %s during cloud-managed failback for primary VM %s",
                            secondaryVm.getUuid(), primaryVm.getUuid()));
            stopSecondaryVmForCloudManagedFailback(primaryVm, secondaryVm);

            executeFtctlAgentAction(primaryVm, FtctlActionCommand.Action.FAILBACK_FINALIZE, true);

            latestProtection = refreshProtection(primaryVm);
            secondaryVm = validateVirtualMachineExists(secondaryVm.getId());
            handoffNicIdentityToPrimary(primaryVm, secondaryVm, latestProtection);

            publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                    String.format("Starting FTCTL primary VM %s during cloud-managed failback", primaryVm.getUuid()));
            startPrimaryVmForCloudManagedFailback(primaryVm, primaryHostId);

            UserVmVO refreshedPrimaryVm = validateVirtualMachineExists(primaryVm.getId());
            executeFtctlAgentAction(refreshedPrimaryVm, FtctlActionCommand.Action.FAILBACK_REPROTECT, true);
            publishFtctlEvent(refreshedPrimaryVm, EventTypes.EVENT_FTCTL_PROTECTION_FAILBACK,
                    String.format("Executed cloud-managed FTCTL failback for VM %s", refreshedPrimaryVm.getUuid()));
            clearCloudManagedFailbackContext(latestProtection);
        } catch (RuntimeException e) {
            clearCloudManagedFailbackContext(protection);
            markCloudManagedFailbackFailed(primaryVm, protection, e);
            throw e;
        } finally {
            cloudManagedFailbackCutbackLocks.remove(protection.getId(), marker);
        }
    }

    private void continueRemoteMoldDrFailbackAfterReverseSync(UserVmVO primaryVm, FtctlProtectionVO protection,
                                                              FailbackTargetContext targetContext, boolean operatorRequested) {
        if (targetContext == null || targetContext.remoteMoldCredentials == null) {
            if (operatorRequested) {
                throw new CloudRuntimeException(String.format(
                        "FTCTL DR remote Mold failback for VM %s requires one-time remote Mold API URL, API key, and secret key",
                        primaryVm.getUuid()));
            }
            markCloudManagedFailbackCutbackRequired(primaryVm, protection, "cloud_managed_failback_context_required");
            logger.info(String.format("FTCTL DR remote Mold failback for VM %s is waiting for operator-provided target Mold credentials",
                    primaryVm.getUuid()));
            return;
        }
        if (FAILBACK_TARGET_MOLD_NEW.equals(targetContext.targetMoldType)) {
            throw new CloudRuntimeException(String.format(
                    "FTCTL DR failback to a new target Mold for VM %s requires target primary reprovisioning and is not available in this build",
                    primaryVm.getUuid()));
        }

        Long primaryHostId = requireExecutionHostId(primaryVm);
        RemoteMoldCredentials remoteMoldCredentials = targetContext.remoteMoldCredentials;

        try {
            stopRemoteMoldReplicaVmForCloudManagedFailback(primaryVm, remoteMoldCredentials);
        } catch (CloudRuntimeException e) {
            if (!operatorRequested && isRemoteMoldCutbackCredentialRetryable(e)) {
                markCloudManagedFailbackCutbackRequired(primaryVm, protection, "remote_mold_cutback_requires_operator_retry");
                clearCloudManagedFailbackContext(protection);
                logger.warn(String.format("FTCTL DR remote Mold failback for VM %s needs fresh operator credentials for cutback: %s",
                        primaryVm.getUuid(), StringUtils.abbreviate(e.getMessage(), 256)));
                return;
            }
            throw e;
        }

        executeFtctlAgentAction(primaryVm, FtctlActionCommand.Action.FAILBACK_FINALIZE, true);

        publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                String.format("Starting FTCTL primary VM %s during DR remote Mold cloud-managed failback", primaryVm.getUuid()));
        startPrimaryVmForCloudManagedFailback(primaryVm, primaryHostId);

        UserVmVO refreshedPrimaryVm = validateVirtualMachineExists(primaryVm.getId());
        executeFtctlAgentAction(refreshedPrimaryVm, FtctlActionCommand.Action.FAILBACK_REPROTECT, true);
        publishFtctlEvent(refreshedPrimaryVm, EventTypes.EVENT_FTCTL_PROTECTION_FAILBACK,
                String.format("Executed DR remote Mold cloud-managed FTCTL failback for VM %s", refreshedPrimaryVm.getUuid()));
        clearCloudManagedFailbackContext(protection);
    }

    private void storeCloudManagedFailbackContext(UserVmVO primaryVm, FtctlProtectionVO protection, FailbackTargetContext targetContext) {
        if (targetContext == null || protection == null || protection.getId() <= 0) {
            return;
        }
        cloudManagedFailbackContexts.put(protection.getId(), new FailbackOperationContext(targetContext));
        logger.info(String.format("Stored transient FTCTL failback operation context for VM %s protection %s target Mold type %s",
                primaryVm != null ? primaryVm.getUuid() : "unknown", protection.getId(), targetContext.targetMoldType));
    }

    private FailbackTargetContext loadCloudManagedFailbackContext(UserVmVO primaryVm, FtctlProtectionVO protection) {
        if (protection == null || protection.getId() <= 0) {
            return null;
        }
        FailbackOperationContext context = cloudManagedFailbackContexts.get(protection.getId());
        if (context == null) {
            return null;
        }
        if (context.isExpired()) {
            cloudManagedFailbackContexts.remove(protection.getId(), context);
            logger.info(String.format("Expired transient FTCTL failback operation context for VM %s protection %s",
                    primaryVm != null ? primaryVm.getUuid() : "unknown", protection.getId()));
            return null;
        }
        context.touch();
        return context.targetContext;
    }

    private void touchCloudManagedFailbackContext(FtctlProtectionVO protection) {
        if (protection == null || protection.getId() <= 0) {
            return;
        }
        FailbackOperationContext context = cloudManagedFailbackContexts.get(protection.getId());
        if (context != null) {
            if (context.isExpired()) {
                cloudManagedFailbackContexts.remove(protection.getId(), context);
            } else {
                context.touch();
            }
        }
    }

    private void clearCloudManagedFailbackContext(FtctlProtectionVO protection) {
        if (protection != null && protection.getId() > 0) {
            cloudManagedFailbackContexts.remove(protection.getId());
        }
    }

    private void pruneExpiredCloudManagedFailbackContexts() {
        cloudManagedFailbackContexts.forEach((protectionId, context) -> {
            if (context == null || context.isExpired()) {
                cloudManagedFailbackContexts.remove(protectionId, context);
            }
        });
    }

    private boolean isRemoteMoldCutbackCredentialRetryable(CloudRuntimeException e) {
        String message = StringUtils.trimToEmpty(e != null ? e.getMessage() : null).toLowerCase(Locale.ROOT);
        return message.contains("remote mold api") &&
                (message.contains("http 401") || message.contains("http 403") || message.contains("unauthorized") ||
                        message.contains("forbidden") || message.contains("signature") || message.contains("api key") ||
                        message.contains("secret"));
    }

    private void markCloudManagedFailbackCutbackRequired(UserVmVO primaryVm, FtctlProtectionVO protection, String reason) {
        FtctlProtectionVO latestProtection = ftctlProtectionDao.findActiveByPrimaryVmId(primaryVm.getId());
        if (latestProtection == null) {
            latestProtection = protection;
        }
        if (latestProtection == null) {
            return;
        }
        if ("reverse_sync_cutback_required".equalsIgnoreCase(StringUtils.trimToEmpty(latestProtection.getTransportState())) &&
                StringUtils.equals(StringUtils.trimToEmpty(latestProtection.getLastError()), reason)) {
            return;
        }
        latestProtection.setProtectionState("failing_back");
        latestProtection.setTransportState("reverse_sync_cutback_required");
        latestProtection.setActiveSide("secondary");
        latestProtection.setFencingState(StringUtils.defaultIfBlank(latestProtection.getFencingState(), "clear"));
        latestProtection.setLastError(reason);
        latestProtection.markUpdated();
        ftctlProtectionDao.update(latestProtection.getId(), latestProtection);
        putVmDetail(primaryVm.getId(), DETAIL_LAST_PROTECTION_STATE, "failing_back");
        putVmDetail(primaryVm.getId(), DETAIL_LAST_TRANSPORT_STATE, "reverse_sync_cutback_required");
        putVmDetail(primaryVm.getId(), DETAIL_LAST_ACTIVE_SIDE, "secondary");
        putVmDetail(primaryVm.getId(), DETAIL_LAST_FENCING_STATE, StringUtils.defaultIfBlank(latestProtection.getFencingState(), "clear"));
        putVmDetail(primaryVm.getId(), DETAIL_LAST_ERROR, reason);
        publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                String.format("FTCTL DR failback for VM %s requires operator continuation after reverse sync ready: %s",
                        primaryVm.getUuid(), reason));
    }

    private FailbackTargetContext buildFailbackTargetContext(UserVmVO primaryVm, FtctlProtectionVO protection, String failbackTargetMoldType,
                                                            String remoteMoldApiUrl, String remoteMoldApiKey, String remoteMoldSecretKey,
                                                            String targetMoldApiUrl, String targetMoldApiKey, String targetMoldSecretKey) {
        String normalizedTargetType = normalizeFailbackTargetMoldType(failbackTargetMoldType);
        RemoteMoldCredentials remoteMoldCredentials = resolveFailbackRemoteMoldCredentials(primaryVm, protection,
                remoteMoldApiUrl, remoteMoldApiKey, remoteMoldSecretKey, targetMoldApiUrl, targetMoldApiKey, targetMoldSecretKey);
        RemoteMoldCredentials targetMoldCredentials = buildOptionalRemoteMoldCredentials(targetMoldApiUrl, targetMoldApiKey, targetMoldSecretKey);
        return new FailbackTargetContext(normalizedTargetType, remoteMoldCredentials, targetMoldCredentials);
    }

    private String normalizeFailbackTargetMoldType(String failbackTargetMoldType) {
        String value = StringUtils.trimToEmpty(failbackTargetMoldType).toLowerCase(Locale.ROOT);
        if (StringUtils.isBlank(value)) {
            return FAILBACK_TARGET_MOLD_ORIGINAL_PRIMARY;
        }
        if (FAILBACK_TARGET_MOLD_CURRENT.equals(value) || FAILBACK_TARGET_MOLD_ORIGINAL_PRIMARY.equals(value) ||
                FAILBACK_TARGET_MOLD_NEW.equals(value)) {
            return value;
        }
        throw new CloudRuntimeException(String.format("Unsupported FTCTL DR failback target Mold type: %s", failbackTargetMoldType));
    }

    private RemoteMoldCredentials resolveFailbackRemoteMoldCredentials(UserVmVO primaryVm, FtctlProtectionVO protection,
                                                                       String remoteMoldApiUrl, String remoteMoldApiKey, String remoteMoldSecretKey,
                                                                       String targetMoldApiUrl, String targetMoldApiKey, String targetMoldSecretKey) {
        if (!isRemoteMoldDrProtection(primaryVm, protection)) {
            return null;
        }
        String resolvedApiUrl = StringUtils.defaultIfBlank(StringUtils.trimToNull(remoteMoldApiUrl),
                StringUtils.defaultIfBlank(StringUtils.trimToNull(targetMoldApiUrl), getDetailValue(primaryVm.getId(), DETAIL_REMOTE_MOLD_API_URL)));
        String resolvedApiKey = StringUtils.defaultIfBlank(StringUtils.trimToNull(remoteMoldApiKey), StringUtils.trimToNull(targetMoldApiKey));
        String resolvedSecretKey = StringUtils.defaultIfBlank(StringUtils.trimToNull(remoteMoldSecretKey), StringUtils.trimToNull(targetMoldSecretKey));
        if (StringUtils.isAnyBlank(resolvedApiUrl, resolvedApiKey, resolvedSecretKey)) {
            throw new CloudRuntimeException(String.format(
                    "FTCTL DR remote Mold failback for VM %s requires one-time remote Mold API URL, API key, and secret key",
                    primaryVm.getUuid()));
        }
        return new RemoteMoldCredentials(resolvedApiUrl, resolvedApiKey, resolvedSecretKey);
    }

    private RemoteMoldCredentials buildOptionalRemoteMoldCredentials(String apiUrl, String apiKey, String secretKey) {
        if (StringUtils.isAllBlank(apiUrl, apiKey, secretKey)) {
            return null;
        }
        if (StringUtils.isAnyBlank(apiUrl, apiKey, secretKey)) {
            throw new CloudRuntimeException("Target Mold API URL, API key, and secret key must be provided together");
        }
        return new RemoteMoldCredentials(StringUtils.trim(apiUrl), StringUtils.trim(apiKey), StringUtils.trim(secretKey));
    }

    private void stopRemoteMoldReplicaVmForCloudManagedFailback(UserVmVO primaryVm, RemoteMoldCredentials credentials) {
        String remoteVmUuid = StringUtils.trimToNull(getDetailValue(primaryVm.getId(), DETAIL_REMOTE_REPLICA_VM_ID));
        if (StringUtils.isBlank(remoteVmUuid)) {
            throw new CloudRuntimeException(String.format("FTCTL DR remote Mold failback for VM %s does not have a remote replica VM UUID",
                    primaryVm.getUuid()));
        }
        JsonObject remoteVm = fetchRemoteMoldVirtualMachine(credentials, remoteVmUuid);
        if (remoteVm != null) {
            persistRemoteMoldReplicaVmSnapshot(primaryVm, remoteVm);
            if ("stopped".equalsIgnoreCase(StringUtils.trimToEmpty(getJsonString(remoteVm, "state")))) {
                logger.info(String.format("FTCTL DR remote Mold replica VM %s is already stopped for primary VM %s failback",
                        remoteVmUuid, primaryVm.getUuid()));
                return;
            }
        }

        Map<String, String> params = new HashMap<>();
        params.put("id", remoteVmUuid);
        params.put("forced", "true");
        JsonObject stopResponse = callRemoteMoldApi(credentials.apiUrl, credentials.apiKey, credentials.secretKey,
                "stopVirtualMachine", params);
        JsonObject stoppedVm = extractRemoteMoldVmFromResponse(stopResponse, "stopvirtualmachineresponse");
        if (stoppedVm == null) {
            String jobId = getRemoteMoldResponseString(stopResponse, "stopvirtualmachineresponse", "jobid");
            if (StringUtils.isBlank(jobId)) {
                throw new CloudRuntimeException(String.format("Remote Mold stopVirtualMachine for FTCTL replica VM %s did not return a job ID",
                        remoteVmUuid));
            }
            stoppedVm = waitForRemoteMoldVmJob(credentials, jobId, remoteVmUuid, "stopVirtualMachine");
        }
        if (stoppedVm == null) {
            stoppedVm = fetchRemoteMoldVirtualMachine(credentials, remoteVmUuid);
        }
        persistRemoteMoldReplicaVmSnapshot(primaryVm, stoppedVm);
        publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                String.format("Stopped remote Mold FTCTL replica VM %s for primary VM %s during cloud-managed failback",
                        remoteVmUuid, primaryVm.getUuid()));
    }

    private boolean isCloudManagedFailbackCandidate(FtctlProtectionVO protection) {
        return isCloudManagedProvisioning(protection) &&
                isCloudManagedFailbackMode(protection) &&
                "failing_back".equalsIgnoreCase(StringUtils.trimToEmpty(protection.getProtectionState())) &&
                "secondary".equalsIgnoreCase(StringUtils.trimToEmpty(protection.getActiveSide()));
    }

    private boolean isCloudManagedFailbackMode(FtctlProtectionVO protection) {
        String mode = StringUtils.trimToEmpty(protection != null ? protection.getMode() : null).toLowerCase(Locale.ROOT);
        return "ha".equals(mode) || "dr".equals(mode);
    }

    private boolean isCloudManagedFailbackInProgress(FtctlProtectionVO protection) {
        if (!isCloudManagedFailbackCandidate(protection)) {
            return false;
        }
        String transportState = StringUtils.trimToEmpty(protection.getTransportState()).toLowerCase(Locale.ROOT);
        return "reverse_syncing".equals(transportState) ||
                "reverse_sync_ready".equals(transportState) ||
                "reverse_sync_cutback_required".equals(transportState) ||
                "secondary_stopping".equals(transportState) ||
                "finalizing".equals(transportState) ||
                "primary_restoring".equals(transportState);
    }

    private boolean isCloudManagedFailbackReverseReady(FtctlProtectionVO protection) {
        if (!isCloudManagedFailbackCandidate(protection)) {
            return false;
        }
        String transportState = StringUtils.trimToEmpty(protection.getTransportState()).toLowerCase(Locale.ROOT);
        return "reverse_sync_ready".equals(transportState) || "reverse_sync_cutback_required".equals(transportState);
    }

    private void markCloudManagedFailbackFailed(UserVmVO primaryVm, FtctlProtectionVO protection, RuntimeException cause) {
        FtctlProtectionVO latestProtection = ftctlProtectionDao.findActiveByPrimaryVmId(primaryVm.getId());
        if (latestProtection == null) {
            latestProtection = protection;
        }
        String lastError = StringUtils.abbreviate(String.format("cloud_managed_failback_failed: %s", cause.getMessage()), 1024);
        latestProtection.setProtectionState("error");
        latestProtection.setTransportState("failback_failed");
        latestProtection.setLastError(lastError);
        latestProtection.markUpdated();
        ftctlProtectionDao.update(latestProtection.getId(), latestProtection);
        putVmDetail(primaryVm.getId(), DETAIL_LAST_PROTECTION_STATE, "error");
        putVmDetail(primaryVm.getId(), DETAIL_LAST_TRANSPORT_STATE, "failback_failed");
        putVmDetail(primaryVm.getId(), DETAIL_LAST_ERROR, lastError);
        publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                String.format("Cloud-managed FTCTL failback failed for VM %s: %s", primaryVm.getUuid(), StringUtils.abbreviate(cause.getMessage(), 512)));
    }

    private FtctlProtectionVO refreshProtection(UserVmVO primaryVm) {
        FtctlProtectionVO protection = ftctlProtectionDao.findActiveByPrimaryVmId(primaryVm.getId());
        if (protection == null) {
            throw new CloudRuntimeException(String.format("Unable to find active FTCTL protection for VM %s", primaryVm.getUuid()));
        }
        return protection;
    }

    private void stopSecondaryVmForCloudManagedFailback(UserVmVO primaryVm, UserVmVO secondaryVm) {
        UserVmVO currentSecondaryVm = validateVirtualMachineExists(secondaryVm.getId());
        if (currentSecondaryVm.getState() == VirtualMachine.State.Stopped) {
            logger.info(String.format("FTCTL secondary VM %s is already stopped for failback to primary VM %s",
                    currentSecondaryVm.getUuid(), primaryVm.getUuid()));
            return;
        }
        try {
            userVmService.stopVirtualMachine(currentSecondaryVm.getId(), true);
            publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                    String.format("Stopped FTCTL secondary VM %s during cloud-managed failback for primary VM %s",
                            currentSecondaryVm.getUuid(), primaryVm.getUuid()));
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException(String.format("Unable to stop FTCTL secondary VM %s for primary VM %s during cloud-managed failback",
                    currentSecondaryVm.getUuid(), primaryVm.getUuid()), e);
        }
    }

    private void startPrimaryVmForCloudManagedFailback(UserVmVO primaryVm, Long primaryHostId) {
        UserVmVO currentPrimaryVm = validateVirtualMachineExists(primaryVm.getId());
        if (currentPrimaryVm.getState() == VirtualMachine.State.Running) {
            logger.info(String.format("FTCTL primary VM %s is already running during cloud-managed failback", currentPrimaryVm.getUuid()));
            return;
        }
        try {
            userVmManager.startVirtualMachine(currentPrimaryVm.getId(), primaryHostId, new HashMap<VirtualMachineProfile.Param, Object>(), null);
            publishFtctlEvent(currentPrimaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                    String.format("Started FTCTL primary VM %s during cloud-managed failback", currentPrimaryVm.getUuid()));
        } catch (ConcurrentOperationException | ResourceUnavailableException | InsufficientCapacityException | ResourceAllocationException e) {
            throw new CloudRuntimeException(String.format("Unable to start FTCTL primary VM %s on host %s during cloud-managed failback",
                    currentPrimaryVm.getUuid(), primaryHostId), e);
        }
    }

    void reconcileCloudManagedFailovers() {
        if (!FtctlServiceEnabled.value() || !FtctlCloudManagedFailoverMonitorEnabled.value() ||
                !cloudManagedFailoverRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            List<FtctlProtectionVO> protections = ftctlProtectionDao.listActive();
            if (protections == null || protections.isEmpty()) {
                return;
            }
            for (FtctlProtectionVO protection : protections) {
                reconcileCloudManagedFailover(protection);
            }
        } catch (RuntimeException e) {
            logger.warn("Unable to reconcile FTCTL cloud-managed failover candidates", e);
        } finally {
            cloudManagedFailoverRunning.set(false);
        }
    }

    private void reconcileCloudManagedFailover(FtctlProtectionVO protection) {
        if (!isCloudManagedAutomaticFailoverProtection(protection)) {
            return;
        }
        UserVmVO primaryVm = userVmDao.findById(protection.getPrimaryVmId());
        if (primaryVm == null || primaryVm.getRemoved() != null) {
            return;
        }
        Object marker = new Object();
        if (cloudManagedFailoverLocks.putIfAbsent(protection.getId(), marker) != null) {
            return;
        }
        try {
            FtctlStatusAnswer statusAnswer = fetchRuntimeStatus(primaryVm);
            if (statusAnswer != null) {
                persistRuntimeState(primaryVm, statusAnswer);
            }
            FtctlProtectionVO latestProtection = refreshProtection(primaryVm);
            if (!isCloudManagedAutomaticFailoverProtection(latestProtection)) {
                clearCloudManagedFailoverCandidate(primaryVm.getId());
                return;
            }
            CloudManagedFailoverSignal signal = detectCloudManagedFailoverSignal(primaryVm, latestProtection, statusAnswer);
            if (!signal.candidate) {
                clearCloudManagedFailoverCandidate(primaryVm.getId());
                return;
            }
            int failureCount = incrementCloudManagedFailoverFailureCount(primaryVm.getId(), signal.reason);
            int requiredCount = Math.max(1, FtctlCloudManagedFailoverConfirmations.value());
            if (failureCount < requiredCount) {
                markCloudManagedFailoverSuspect(primaryVm, latestProtection, signal.reason, failureCount, requiredCount);
                return;
            }
            handleConfirmedCloudManagedFailover(primaryVm, latestProtection, signal, statusAnswer);
        } catch (RuntimeException e) {
            logger.warn(String.format("Unable to reconcile cloud-managed FTCTL failover candidate for VM %s", primaryVm.getUuid()), e);
        } finally {
            cloudManagedFailoverLocks.remove(protection.getId(), marker);
        }
    }

    private boolean isCloudManagedAutomaticFailoverProtection(FtctlProtectionVO protection) {
        if (!isCloudManagedProvisioning(protection)) {
            return false;
        }
        String mode = StringUtils.trimToEmpty(protection.getMode()).toLowerCase(Locale.ROOT);
        if (!"ha".equals(mode) && !"dr".equals(mode)) {
            return false;
        }
        String activeSide = StringUtils.trimToEmpty(protection.getActiveSide()).toLowerCase(Locale.ROOT);
        if (StringUtils.isNotBlank(activeSide) && !"primary".equals(activeSide)) {
            return false;
        }
        String adminState = StringUtils.trimToEmpty(protection.getAdminState()).toLowerCase(Locale.ROOT);
        if ("paused".equals(adminState) || "inactive".equals(adminState)) {
            return false;
        }
        String protectionState = StringUtils.trimToEmpty(protection.getProtectionState()).toLowerCase(Locale.ROOT);
        return !"failed_over".equals(protectionState) && !"failing_back".equals(protectionState) &&
                !"disabled".equals(protectionState) && !"error".equals(protectionState);
    }

    private CloudManagedFailoverSignal detectCloudManagedFailoverSignal(UserVmVO primaryVm, FtctlProtectionVO protection,
                                                                        FtctlStatusAnswer statusAnswer) {
        String detailLastError = getDetailValue(primaryVm.getId(), DETAIL_LAST_ERROR);
        if (isCloudManagedFailoverRuntimeCandidate(detailLastError) ||
                isCloudManagedFailoverRuntimeCandidate(protection.getLastError())) {
            return new CloudManagedFailoverSignal(true,
                    StringUtils.defaultIfBlank(getDetailValue(primaryVm.getId(), DETAIL_AUTO_FAILOVER_REASON), "primary_domain_missing"));
        }
        if (isCloudManagedFailoverSuspect(protection)) {
            return new CloudManagedFailoverSignal(true,
                    StringUtils.defaultIfBlank(getDetailValue(primaryVm.getId(), DETAIL_AUTO_FAILOVER_REASON), "previous_failover_suspect"));
        }
        if (primaryVm.getState() != VirtualMachine.State.Running) {
            return new CloudManagedFailoverSignal(true, "primary_vm_not_running");
        }
        HostVO sourceHost = resolveExecutionHost(primaryVm);
        if (sourceHost != null && sourceHost.getStatus() != null && sourceHost.getStatus().lostConnection()) {
            return new CloudManagedFailoverSignal(true, "primary_host_lost_connection");
        }
        if (statusAnswer == null && sourceHost != null && sourceHost.getStatus() != null && sourceHost.getStatus().lostConnection()) {
            return new CloudManagedFailoverSignal(true, "runtime_unavailable_and_primary_host_lost_connection");
        }
        return new CloudManagedFailoverSignal(false, null);
    }

    private boolean isCloudManagedFailoverRuntimeCandidate(String lastError) {
        String value = StringUtils.trimToEmpty(lastError);
        return LAST_ERROR_CLOUD_MANAGED_FAILOVER_CANDIDATE.equalsIgnoreCase(value) ||
                LAST_ERROR_CLOUD_MANAGED_FAILOVER_SUSPECT.equalsIgnoreCase(value);
    }

    private boolean isCloudManagedFailoverSuspect(FtctlProtectionVO protection) {
        return protection != null &&
                ("failover_suspect".equalsIgnoreCase(StringUtils.trimToEmpty(protection.getProtectionState())) ||
                        LAST_ERROR_CLOUD_MANAGED_FAILOVER_SUSPECT.equalsIgnoreCase(StringUtils.trimToEmpty(protection.getLastError())));
    }

    private HostVO resolveExecutionHost(UserVmVO userVm) {
        Long hostId = getExecutionHostId(userVm);
        return hostId != null ? hostDao.findById(hostId) : null;
    }

    private int incrementCloudManagedFailoverFailureCount(Long virtualMachineId, String reason) {
        Integer current = parseIntegerDetail(getDetailValue(virtualMachineId, DETAIL_AUTO_FAILOVER_FAILURE_COUNT));
        int next = current != null ? current + 1 : 1;
        putVmDetail(virtualMachineId, DETAIL_AUTO_FAILOVER_FAILURE_COUNT, String.valueOf(next));
        putVmDetail(virtualMachineId, DETAIL_AUTO_FAILOVER_UPDATED, Instant.now().toString());
        putVmDetail(virtualMachineId, DETAIL_AUTO_FAILOVER_REASON, StringUtils.defaultIfBlank(reason, "unknown"));
        return next;
    }

    private void clearCloudManagedFailoverCandidate(Long virtualMachineId) {
        removeVmDetail(virtualMachineId, DETAIL_AUTO_FAILOVER_FAILURE_COUNT);
        removeVmDetail(virtualMachineId, DETAIL_AUTO_FAILOVER_UPDATED);
        removeVmDetail(virtualMachineId, DETAIL_AUTO_FAILOVER_REASON);
    }

    private void markCloudManagedFailoverSuspect(UserVmVO primaryVm, FtctlProtectionVO protection, String reason,
                                                 int failureCount, int requiredCount) {
        updateCloudManagedFailoverState(primaryVm, protection,
                "failover_suspect",
                StringUtils.defaultIfBlank(protection.getTransportState(), "mirroring"),
                "primary",
                StringUtils.defaultIfBlank(protection.getFencingState(), "clear"),
                LAST_ERROR_CLOUD_MANAGED_FAILOVER_SUSPECT,
                null,
                reason);
        publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                String.format("Cloud-managed FTCTL failover candidate observed for VM %s (%s/%s): %s",
                        primaryVm.getUuid(), failureCount, requiredCount, reason));
    }

    private void handleConfirmedCloudManagedFailover(UserVmVO primaryVm, FtctlProtectionVO protection,
                                                     CloudManagedFailoverSignal signal, FtctlStatusAnswer statusAnswer) {
        String mode = StringUtils.trimToEmpty(protection.getMode()).toLowerCase(Locale.ROOT);
        if (isRemoteMoldDrProtection(primaryVm, protection)) {
            markCloudManagedFailoverManualRequired(primaryVm, protection,
                    "remote_mold_automatic_orchestration_requires_target_site_controller", signal.reason);
            return;
        }
        if ("ha".equals(mode)) {
            markCloudManagedFailoverManualRequired(primaryVm, protection,
                    "ha_cloud_managed_automatic_cutover_requires_cloud_ha_controller_integration", signal.reason);
            return;
        }
        if (!"dr".equals(mode)) {
            markCloudManagedFailoverManualRequired(primaryVm, protection, "unsupported_mode", signal.reason);
            return;
        }
        if (!FENCING_POLICY_IPMI.equalsIgnoreCase(StringUtils.trimToEmpty(protection.getFencingPolicy()))) {
            markCloudManagedFailoverManualRequired(primaryVm, protection, "automatic_fencing_requires_ipmi_policy", signal.reason);
            return;
        }
        CloudManagedFencingDecision decision = decideCloudManagedIpmiFencing(primaryVm);
        if (!decision.fenced) {
            markCloudManagedFailoverManualRequired(primaryVm, protection, "automatic_fencing_not_confirmed",
                    decision.result, decision.reason);
            return;
        }
        if (!FtctlCloudManagedFailoverStartEnabled.value()) {
            markCloudManagedFailoverManualRequired(primaryVm, protection, "automatic_start_disabled", decision.result, decision.reason);
            return;
        }
        if (statusAnswer == null) {
            markCloudManagedFailoverManualRequired(primaryVm, protection, "primary_host_agent_unavailable_for_qemu_cutover",
                    decision.result, decision.reason);
            return;
        }
        executeCloudManagedLocalMoldDrFailover(primaryVm, protection, decision);
    }

    private CloudManagedFencingDecision decideCloudManagedIpmiFencing(UserVmVO primaryVm) {
        Long hostId = getExecutionHostId(primaryVm);
        HostVO host = hostId != null ? hostDao.findById(hostId) : null;
        OutOfBandManagement oobm = hostId != null ? outOfBandManagementDao.findByHost(hostId) : null;
        if (!isUsableIpmiOobm(oobm)) {
            return new CloudManagedFencingDecision(true, FENCING_RESULT_IPMI_UNKNOWN_ASSUMED,
                    "source_host_oobm_unknown");
        }
        try {
            outOfBandManagementService.executePowerOperation(host, OutOfBandManagement.PowerOperation.OFF, null);
            return new CloudManagedFencingDecision(true, FENCING_RESULT_IPMI_CONFIRMED,
                    "source_host_power_off_requested");
        } catch (RuntimeException e) {
            logger.warn(String.format("FTCTL IPMI fencing command failed for VM %s on host %s; assuming fenced for DR disaster handling",
                    primaryVm.getUuid(), hostId), e);
            return new CloudManagedFencingDecision(true, FENCING_RESULT_IPMI_FAILED_ASSUMED,
                    StringUtils.abbreviate(StringUtils.defaultIfBlank(e.getMessage(), "ipmi_power_off_failed"), 512));
        }
    }

    private boolean isUsableIpmiOobm(OutOfBandManagement oobm) {
        return oobm != null && oobm.isEnabled() &&
                OOBM_DRIVER_IPMITOOL.equalsIgnoreCase(StringUtils.trimToEmpty(oobm.getDriver())) &&
                !StringUtils.isAnyBlank(oobm.getAddress(), oobm.getUsername(), oobm.getPassword());
    }

    private void markCloudManagedFailoverManualRequired(UserVmVO primaryVm, FtctlProtectionVO protection,
                                                        String reason, String candidateReason) {
        putVmDetail(primaryVm.getId(), DETAIL_AUTO_FAILOVER_REASON, StringUtils.defaultIfBlank(candidateReason, reason));
        markCloudManagedFailoverManualRequired(primaryVm, protection, reason,
                FENCING_RESULT_MANUAL_REQUIRED,
                reason);
    }

    private void markCloudManagedFailoverManualRequired(UserVmVO primaryVm, FtctlProtectionVO protection,
                                                        String reason, String fencingResult, String fencingReason) {
        putVmDetail(primaryVm.getId(), DETAIL_FENCING_RESULT, fencingResult);
        putVmDetail(primaryVm.getId(), DETAIL_FENCING_REASON, StringUtils.defaultIfBlank(fencingReason, reason));
        updateCloudManagedFailoverState(primaryVm, protection,
                "failover_required",
                StringUtils.defaultIfBlank(protection.getTransportState(), "mirroring"),
                "primary",
                "required",
                LAST_ERROR_CLOUD_MANAGED_FAILOVER_MANUAL_REQUIRED,
                fencingResult,
                StringUtils.defaultIfBlank(fencingReason, reason));
        publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_FAILOVER,
                String.format("Cloud-managed FTCTL failover for VM %s requires operator action: %s",
                        primaryVm.getUuid(), reason));
    }

    private void executeCloudManagedLocalMoldDrFailover(UserVmVO primaryVm, FtctlProtectionVO protection,
                                                        CloudManagedFencingDecision decision) {
        try {
            executeFtctlAgentAction(primaryVm, FtctlActionCommand.Action.FAILOVER, true);
            executeFtctlAgentAction(primaryVm, FtctlActionCommand.Action.FENCE_CONFIRM, false);
            executeFtctlAgentAction(primaryVm, FtctlActionCommand.Action.FAILOVER_PREPARE, true);
            FtctlProtectionVO latestProtection = refreshProtection(primaryVm);
            UserVmVO secondaryVm = resolveSecondaryVmForManualFailover(primaryVm, latestProtection);
            handoffNicIdentityToSecondaryIfNeeded(primaryVm, secondaryVm, latestProtection);
            startSecondaryVmForManualFailover(primaryVm, secondaryVm);
            FtctlActionResponse response = executeFtctlAgentAction(primaryVm, FtctlActionCommand.Action.FAILOVER, true);
            putVmDetail(primaryVm.getId(), DETAIL_FENCING_RESULT, decision.result);
            putVmDetail(primaryVm.getId(), DETAIL_FENCING_REASON, decision.reason);
            clearCloudManagedFailoverCandidate(primaryVm.getId());
            publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_FAILOVER,
                    String.format("Executed Cloud-managed FTCTL DR automatic failover for VM %s: %s",
                            primaryVm.getUuid(), response.getOutput()));
        } catch (RuntimeException e) {
            markCloudManagedFailoverManualRequired(primaryVm, protection,
                    "automatic_failover_execution_failed", FENCING_RESULT_MANUAL_REQUIRED,
                    StringUtils.abbreviate(StringUtils.defaultIfBlank(e.getMessage(), "automatic_failover_execution_failed"), 512));
            throw e;
        }
    }

    private void updateCloudManagedFailoverState(UserVmVO primaryVm, FtctlProtectionVO protection,
                                                 String protectionState, String transportState, String activeSide,
                                                 String fencingState, String lastError, String fencingResult,
                                                 String reason) {
        if (protectionState != null) {
            protection.setProtectionState(protectionState);
            putVmDetail(primaryVm.getId(), DETAIL_LAST_PROTECTION_STATE, protectionState);
        }
        if (transportState != null) {
            protection.setTransportState(transportState);
            putVmDetail(primaryVm.getId(), DETAIL_LAST_TRANSPORT_STATE, transportState);
        }
        if (activeSide != null) {
            protection.setActiveSide(activeSide);
            putVmDetail(primaryVm.getId(), DETAIL_LAST_ACTIVE_SIDE, activeSide);
        }
        protection.setAdminState(StringUtils.defaultIfBlank(protection.getAdminState(), "active"));
        putVmDetail(primaryVm.getId(), DETAIL_LAST_ADMIN_STATE, protection.getAdminState());
        if (fencingState != null) {
            protection.setFencingState(fencingState);
            putVmDetail(primaryVm.getId(), DETAIL_LAST_FENCING_STATE, fencingState);
        }
        protection.setLastError(lastError);
        putVmDetail(primaryVm.getId(), DETAIL_LAST_ERROR, StringUtils.defaultString(lastError));
        if (StringUtils.isNotBlank(fencingResult)) {
            putVmDetail(primaryVm.getId(), DETAIL_FENCING_RESULT, fencingResult);
        }
        if (StringUtils.isNotBlank(reason)) {
            putVmDetail(primaryVm.getId(), DETAIL_FENCING_REASON, reason);
        }
        protection.markUpdated();
        ftctlProtectionDao.update(protection.getId(), protection);
    }

    @Override
    public FtctlActionResponse confirmFtctlFence(Long virtualMachineId) throws CloudRuntimeException {
        return confirmFtctlFence(virtualMachineId, null, null, null);
    }

    @Override
    public FtctlActionResponse confirmFtctlFence(Long virtualMachineId, String remoteMoldApiUrl,
                                                 String remoteMoldApiKey, String remoteMoldSecretKey) throws CloudRuntimeException {
        UserVmVO primaryVm = validateVirtualMachineExists(virtualMachineId);
        validatePrimaryProtectionTarget(primaryVm);
        FtctlProtectionVO protection = ftctlProtectionDao.findActiveByPrimaryVmId(primaryVm.getId());
        if (protection == null) {
            throw new CloudRuntimeException(String.format("Unable to find active FTCTL protection for VM %s", primaryVm.getUuid()));
        }
        validatePrimaryVmStoppedForManualFence(primaryVm);
        if (isManualFailoverTerminalSuccess(protection)) {
            return buildActionResponseFromProtection(primaryVm, FtctlActionCommand.Action.FENCE_CONFIRM, protection,
                    "manual failover already completed");
        }
        RemoteMoldCredentials remoteMoldCredentials = resolveRemoteMoldCredentialsForManualFailover(primaryVm, protection,
                remoteMoldApiUrl, remoteMoldApiKey, remoteMoldSecretKey);

        try {
            executeFtctlAction(primaryVm.getId(), FtctlActionCommand.Action.FENCE_CONFIRM, false);
        } catch (FtctlActionLockedException e) {
            FtctlProtectionVO finalProtection = refreshProtection(primaryVm);
            if (!isManualFailoverTerminalSuccess(finalProtection)) {
                throw e;
            }
            logger.info(String.format("FTCTL manual fence confirmation for VM %s reached terminal failed-over state before confirm response returned; returning final persisted state",
                    primaryVm.getUuid()));
            return buildActionResponseFromProtection(primaryVm, FtctlActionCommand.Action.FENCE_CONFIRM, finalProtection,
                    "manual failover already completed after fence confirm lock retry exhaustion");
        }
        executeFtctlAction(primaryVm.getId(), FtctlActionCommand.Action.FAILOVER_PREPARE, true);
        protection = validateManualFailoverTransportReadyOrMarker(primaryVm);
        if (remoteMoldCredentials != null) {
            startRemoteMoldSecondaryVmForManualFailover(primaryVm, remoteMoldCredentials);
        } else {
            UserVmVO secondaryVm = resolveSecondaryVmForManualFailover(primaryVm, protection);
            handoffNicIdentityToSecondaryIfNeeded(primaryVm, secondaryVm, protection);
            startSecondaryVmForManualFailover(primaryVm, secondaryVm);
        }
        FtctlActionResponse response;
        try {
            response = executeFtctlAction(primaryVm.getId(), FtctlActionCommand.Action.FAILOVER, true);
        } catch (FtctlActionLockedException e) {
            FtctlProtectionVO finalProtection = refreshProtection(primaryVm);
            if (!isManualFailoverTerminalSuccess(finalProtection)) {
                throw e;
            }
            logger.info(String.format("FTCTL manual fence confirmation for VM %s reached terminal failed-over state after a lock race; returning final persisted state",
                    primaryVm.getUuid()));
            response = buildActionResponseFromProtection(primaryVm, FtctlActionCommand.Action.FAILOVER, finalProtection,
                    "manual failover already completed after ftctl lock retry exhaustion");
        }
        response.setAction(FtctlActionCommand.Action.FENCE_CONFIRM.name());
        return response;
    }

    private RemoteMoldCredentials resolveRemoteMoldCredentialsForManualFailover(UserVmVO primaryVm, FtctlProtectionVO protection,
                                                                                String remoteMoldApiUrl, String remoteMoldApiKey,
                                                                                String remoteMoldSecretKey) {
        if (!isRemoteMoldDrProtection(primaryVm, protection)) {
            return null;
        }
        String resolvedApiUrl = StringUtils.defaultIfBlank(StringUtils.trimToNull(remoteMoldApiUrl),
                getDetailValue(primaryVm.getId(), DETAIL_REMOTE_MOLD_API_URL));
        String resolvedApiKey = StringUtils.trimToNull(remoteMoldApiKey);
        String resolvedSecretKey = StringUtils.trimToNull(remoteMoldSecretKey);
        if (StringUtils.isAnyBlank(resolvedApiUrl, resolvedApiKey, resolvedSecretKey)) {
            throw new CloudRuntimeException(String.format(
                    "FTCTL DR remote Mold fence confirmation for VM %s requires remote Mold API URL, API key, and secret key",
                    primaryVm.getUuid()));
        }
        return new RemoteMoldCredentials(resolvedApiUrl, resolvedApiKey, resolvedSecretKey);
    }

    private boolean isRemoteMoldDrProtection(UserVmVO primaryVm, FtctlProtectionVO protection) {
        if (primaryVm == null || protection == null) {
            return false;
        }
        String mode = StringUtils.defaultIfBlank(protection.getMode(), getDetailValue(primaryVm.getId(), DETAIL_MODE));
        return "dr".equalsIgnoreCase(StringUtils.trimToEmpty(mode)) &&
                DR_PEER_SITE_TYPE_REMOTE_MOLD.equalsIgnoreCase(getDetailValue(primaryVm.getId(), DETAIL_DR_PEER_SITE_TYPE));
    }

    private void handoffNicIdentityToSecondaryIfNeeded(UserVmVO primaryVm, UserVmVO secondaryVm, FtctlProtectionVO protection) {
        if (!isCloudManagedProvisioning(protection)) {
            return;
        }
        if (secondaryVm.getState() == VirtualMachine.State.Running) {
            logger.info(String.format("Skipping FTCTL NIC identity handoff for primary VM %s because secondary VM %s is already running",
                    primaryVm.getUuid(), secondaryVm.getUuid()));
            return;
        }
        handoffNicIdentityToSecondary(primaryVm, secondaryVm, protection);
    }

    private boolean isManualFailoverTerminalSuccess(FtctlProtectionVO protection) {
        return protection != null &&
                "failed_over".equalsIgnoreCase(StringUtils.trimToEmpty(protection.getProtectionState())) &&
                "failed_over".equalsIgnoreCase(StringUtils.trimToEmpty(protection.getTransportState())) &&
                "secondary".equalsIgnoreCase(StringUtils.trimToEmpty(protection.getActiveSide())) &&
                "active".equalsIgnoreCase(StringUtils.trimToEmpty(protection.getAdminState())) &&
                "manual-fenced".equalsIgnoreCase(StringUtils.trimToEmpty(protection.getFencingState())) &&
                StringUtils.isBlank(protection.getLastError());
    }

    private FtctlActionResponse buildActionResponseFromProtection(UserVmVO userVm, FtctlActionCommand.Action action,
                                                                  FtctlProtectionVO protection, String output) {
        FtctlActionResponse response = new FtctlActionResponse();
        response.setObjectName("ftctlaction");
        response.setVirtualMachineId(userVm.getId());
        response.setVmName(userVm.getInstanceName());
        response.setAction(action.name());
        response.setResult("ok");
        response.setExitCode(0);
        response.setOutput(output);
        response.setMode(protection.getMode());
        response.setProtectionState(protection.getProtectionState());
        response.setTransportState(protection.getTransportState());
        response.setActiveSide(protection.getActiveSide());
        response.setAdminState(protection.getAdminState());
        response.setFencingState(protection.getFencingState());
        response.setLastError(protection.getLastError());
        return response;
    }

    private FtctlProtectionVO validateManualFailoverTransportReady(UserVmVO primaryVm) {
        FtctlProtectionVO protection = ftctlProtectionDao.findActiveByPrimaryVmId(primaryVm.getId());
        if (protection == null) {
            throw new CloudRuntimeException(String.format("Unable to find active FTCTL protection for VM %s", primaryVm.getUuid()));
        }
        if (!requiresManualFailoverTransportReady(protection)) {
            return protection;
        }
        String transportState = StringUtils.trimToEmpty(protection.getTransportState()).toLowerCase(Locale.ROOT);
        if (!"mirroring".equals(transportState) && !"failed_over".equals(transportState)) {
            throw new CloudRuntimeException(String.format(
                    "FTCTL manual fence confirmation requires transport state mirroring or failed_over before starting secondary VM for primary VM %s, current state is %s",
                    primaryVm.getUuid(), StringUtils.defaultIfBlank(protection.getTransportState(), "unknown")));
        }
        return protection;
    }

    private FtctlProtectionVO validateManualFailoverTransportReadyOrMarker(UserVmVO primaryVm) {
        FtctlProtectionVO protection = ftctlProtectionDao.findActiveByPrimaryVmId(primaryVm.getId());
        if (protection == null) {
            throw new CloudRuntimeException(String.format("Unable to find active FTCTL protection for VM %s", primaryVm.getUuid()));
        }
        if (hasFailoverReadyMarker(primaryVm, protection)) {
            logger.info(String.format("FTCTL manual fence confirmation for VM %s is continuing with cached failover-ready marker after primary stop",
                    primaryVm.getUuid()));
            return protection;
        }
        return validateManualFailoverTransportReady(primaryVm);
    }

    private boolean requiresManualFailoverTransportReady(FtctlProtectionVO protection) {
        String mode = StringUtils.trimToEmpty(protection.getMode()).toLowerCase(Locale.ROOT);
        String backendMode = StringUtils.trimToEmpty(protection.getBackendMode()).toLowerCase(Locale.ROOT);
        return ("ha".equals(mode) || "dr".equals(mode)) && ("shared-blockcopy".equals(backendMode) || "remote-nbd".equals(backendMode));
    }

    private boolean hasFailoverReadyMarker(UserVmVO primaryVm, FtctlProtectionVO protection) {
        if (!requiresManualFailoverTransportReady(protection)) {
            return true;
        }
        return "true".equalsIgnoreCase(StringUtils.trimToEmpty(getDetailValue(primaryVm.getId(), DETAIL_FAILOVER_READY)));
    }

    private void updateFailoverReadyMarker(UserVmVO userVm, FtctlActionCommand.Action action, FtctlStatusAnswer statusAnswer) {
        if (userVm == null || action == null || statusAnswer == null) {
            return;
        }
        if (action == FtctlActionCommand.Action.FAILOVER && isManualFailoverReadyStatus(statusAnswer)) {
            Long virtualMachineId = userVm.getId();
            putVmDetail(virtualMachineId, DETAIL_FAILOVER_READY, String.valueOf(true));
            putVmDetail(virtualMachineId, DETAIL_FAILOVER_READY_UPDATED, String.valueOf(System.currentTimeMillis()));
            publishFtctlEvent(userVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                    String.format("Recorded FTCTL failover-ready marker for VM %s before manual fencing", userVm.getUuid()));
            return;
        }
        if (action == FtctlActionCommand.Action.FAILBACK || isManualFailoverTerminalStatus(statusAnswer)) {
            clearFailoverReadyMarker(userVm.getId());
        }
    }

    private boolean isManualFailoverReadyStatus(FtctlStatusAnswer statusAnswer) {
        String mode = statusAnswer != null ? StringUtils.trimToEmpty(statusAnswer.getMode()) : "";
        return statusAnswer != null &&
                ("ha".equalsIgnoreCase(mode) || "dr".equalsIgnoreCase(mode)) &&
                "failing_over".equalsIgnoreCase(StringUtils.trimToEmpty(statusAnswer.getProtectionState())) &&
                "mirroring".equalsIgnoreCase(StringUtils.trimToEmpty(statusAnswer.getTransportState())) &&
                "primary".equalsIgnoreCase(StringUtils.trimToEmpty(statusAnswer.getActiveSide())) &&
                "required".equalsIgnoreCase(StringUtils.trimToEmpty(statusAnswer.getFencingState())) &&
                "manual_fencing_required".equalsIgnoreCase(StringUtils.trimToEmpty(statusAnswer.getLastError()));
    }

    private boolean isManualFailoverTerminalStatus(FtctlStatusAnswer statusAnswer) {
        return statusAnswer != null &&
                "failed_over".equalsIgnoreCase(StringUtils.trimToEmpty(statusAnswer.getProtectionState())) &&
                "failed_over".equalsIgnoreCase(StringUtils.trimToEmpty(statusAnswer.getTransportState())) &&
                "secondary".equalsIgnoreCase(StringUtils.trimToEmpty(statusAnswer.getActiveSide())) &&
                "manual-fenced".equalsIgnoreCase(StringUtils.trimToEmpty(statusAnswer.getFencingState())) &&
                StringUtils.isBlank(statusAnswer.getLastError());
    }

    private void clearFailoverReadyMarker(Long virtualMachineId) {
        removeVmDetail(virtualMachineId, DETAIL_FAILOVER_READY);
        removeVmDetail(virtualMachineId, DETAIL_FAILOVER_READY_UPDATED);
        purgeLegacyProgressDetails(virtualMachineId);
    }

    private void validatePrimaryVmStoppedForManualFence(UserVmVO primaryVm) {
        VirtualMachine.State state = primaryVm.getState();
        if (state != VirtualMachine.State.Stopped) {
            throw new CloudRuntimeException(String.format("FTCTL manual fence confirmation requires primary VM %s to be Stopped, current state is %s",
                    primaryVm.getUuid(), state));
        }
    }

    private UserVmVO resolveSecondaryVmForManualFailover(UserVmVO primaryVm, FtctlProtectionVO protection) {
        Long secondaryVmId = protection.getSecondaryVmId();
        if (secondaryVmId == null) {
            throw new CloudRuntimeException(String.format("FTCTL protection for VM %s does not have a secondary VM", primaryVm.getUuid()));
        }
        UserVmVO secondaryVm = userVmDao.findById(secondaryVmId);
        if (secondaryVm == null) {
            throw new CloudRuntimeException(String.format("Unable to find FTCTL secondary VM %s for primary VM %s", secondaryVmId, primaryVm.getUuid()));
        }
        return secondaryVm;
    }

    private void startSecondaryVmForManualFailover(UserVmVO primaryVm, UserVmVO secondaryVm) {
        if (secondaryVm.getState() == VirtualMachine.State.Running) {
            logger.info(String.format("FTCTL secondary VM %s is already running for primary VM %s", secondaryVm.getUuid(), primaryVm.getUuid()));
            return;
        }
        Long peerHostId = resolvePeerHostId(primaryVm.getId());
        try {
            userVmManager.startVirtualMachine(secondaryVm.getId(), peerHostId, new HashMap<VirtualMachineProfile.Param, Object>(), null);
            publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                    String.format("Started FTCTL secondary VM %s for primary VM %s after manual fence confirmation",
                            secondaryVm.getUuid(), primaryVm.getUuid()));
        } catch (ConcurrentOperationException | ResourceUnavailableException | InsufficientCapacityException | ResourceAllocationException e) {
            repairSecondaryVmStateAfterFailedStart(primaryVm, secondaryVm);
            throw new CloudRuntimeException(String.format("Unable to start FTCTL secondary VM %s for primary VM %s",
                    secondaryVm.getUuid(), primaryVm.getUuid()), e);
        }
    }

    private void startRemoteMoldSecondaryVmForManualFailover(UserVmVO primaryVm, RemoteMoldCredentials credentials) {
        String remoteVmUuid = StringUtils.trimToNull(getDetailValue(primaryVm.getId(), DETAIL_REMOTE_REPLICA_VM_ID));
        if (StringUtils.isBlank(remoteVmUuid)) {
            throw new CloudRuntimeException(String.format("FTCTL DR remote Mold protection for VM %s does not have a remote replica VM UUID",
                    primaryVm.getUuid()));
        }
        String remoteHostUuid = StringUtils.trimToNull(getDetailValue(primaryVm.getId(), DETAIL_REMOTE_PEER_HOST_ID));
        JsonObject remoteVm = fetchRemoteMoldVirtualMachine(credentials, remoteVmUuid);
        if (remoteVm != null) {
            persistRemoteMoldReplicaVmSnapshot(primaryVm, remoteVm);
            if ("running".equalsIgnoreCase(StringUtils.trimToEmpty(getJsonString(remoteVm, "state")))) {
                logger.info(String.format("FTCTL DR remote Mold replica VM %s is already running for primary VM %s",
                        remoteVmUuid, primaryVm.getUuid()));
                return;
            }
        }

        Map<String, String> params = new HashMap<>();
        params.put("id", remoteVmUuid);
        putIfNotBlank(params, "hostid", remoteHostUuid);
        JsonObject startResponse = callRemoteMoldApi(credentials.apiUrl, credentials.apiKey, credentials.secretKey,
                "startVirtualMachine", params);
        JsonObject startedVm = extractRemoteMoldVmFromResponse(startResponse, "startvirtualmachineresponse");
        if (startedVm == null) {
            String jobId = getRemoteMoldResponseString(startResponse, "startvirtualmachineresponse", "jobid");
            if (StringUtils.isBlank(jobId)) {
                throw new CloudRuntimeException(String.format("Remote Mold startVirtualMachine for FTCTL replica VM %s did not return a job ID",
                        remoteVmUuid));
            }
            startedVm = waitForRemoteMoldVmStartJob(credentials, jobId, remoteVmUuid);
        }
        if (startedVm == null) {
            startedVm = fetchRemoteMoldVirtualMachine(credentials, remoteVmUuid);
        }
        persistRemoteMoldReplicaVmSnapshot(primaryVm, startedVm);
        publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                String.format("Started remote Mold FTCTL replica VM %s for primary VM %s after manual fence confirmation",
                        remoteVmUuid, primaryVm.getUuid()));
    }

    private JsonObject fetchRemoteMoldVirtualMachine(RemoteMoldCredentials credentials, String remoteVmUuid) {
        if (StringUtils.isBlank(remoteVmUuid)) {
            return null;
        }
        Map<String, String> params = new HashMap<>();
        params.put("id", remoteVmUuid);
        params.put("listall", "true");
        JsonObject json = callRemoteMoldApi(credentials.apiUrl, credentials.apiKey, credentials.secretKey,
                "listVirtualMachines", params);
        JsonArray vms = getResponseArray(json, "listvirtualmachinesresponse", "virtualmachine");
        if (vms == null || vms.size() == 0 || !vms.get(0).isJsonObject()) {
            return null;
        }
        return vms.get(0).getAsJsonObject();
    }

    private JsonObject waitForRemoteMoldVmStartJob(RemoteMoldCredentials credentials, String jobId, String remoteVmUuid) {
        return waitForRemoteMoldVmJob(credentials, jobId, remoteVmUuid, "startVirtualMachine");
    }

    private JsonObject waitForRemoteMoldVmJob(RemoteMoldCredentials credentials, String jobId, String remoteVmUuid, String actionName) {
        long deadline = System.currentTimeMillis() + REMOTE_MOLD_VM_START_TIMEOUT_MILLIS;
        while (System.currentTimeMillis() <= deadline) {
            Map<String, String> params = new HashMap<>();
            params.put("jobid", jobId);
            JsonObject json = callRemoteMoldApi(credentials.apiUrl, credentials.apiKey, credentials.secretKey,
                    "queryAsyncJobResult", params);
            JsonObject response = getResponseObject(json, "queryasyncjobresultresponse");
            Integer jobStatus = getJsonInteger(response, "jobstatus");
            if (jobStatus != null && jobStatus == 1) {
                JsonObject jobResult = getJsonObject(response, "jobresult");
                JsonObject vm = getJsonObject(jobResult, "virtualmachine");
                return vm != null ? vm : fetchRemoteMoldVirtualMachine(credentials, remoteVmUuid);
            }
            if (jobStatus != null && jobStatus == 2) {
                JsonObject jobResult = getJsonObject(response, "jobresult");
                String errorText = StringUtils.defaultIfBlank(getJsonString(jobResult, "errortext"),
                        StringUtils.defaultIfBlank(getJsonString(response, "jobresultcode"), "remote Mold VM action failed"));
                throw new CloudRuntimeException(String.format("Remote Mold %s failed for FTCTL replica VM %s: %s",
                        actionName, remoteVmUuid, errorText));
            }
            sleepRemoteMoldPollInterval(jobId, remoteVmUuid);
        }
        throw new CloudRuntimeException(String.format("Timed out waiting for remote Mold %s job %s for FTCTL replica VM %s",
                actionName, jobId, remoteVmUuid));
    }

    private void sleepRemoteMoldPollInterval(String jobId, String remoteVmUuid) {
        try {
            Thread.sleep(REMOTE_MOLD_VM_START_POLL_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CloudRuntimeException(String.format("Interrupted while waiting for remote Mold startVirtualMachine job %s for FTCTL replica VM %s",
                    jobId, remoteVmUuid), e);
        }
    }

    private JsonObject extractRemoteMoldVmFromResponse(JsonObject json, String responseKey) {
        JsonObject response = getResponseObject(json, responseKey);
        return getJsonObject(response, "virtualmachine");
    }

    private String getRemoteMoldResponseString(JsonObject json, String responseKey, String key) {
        return getJsonString(getResponseObject(json, responseKey), key);
    }

    private void persistRemoteMoldReplicaVmSnapshot(UserVmVO primaryVm, JsonObject remoteVm) {
        if (primaryVm == null || remoteVm == null) {
            return;
        }
        Long sourceVmId = primaryVm.getId();
        putVmDetailIfNotBlank(sourceVmId, DETAIL_REMOTE_REPLICA_VM_ID, getJsonString(remoteVm, "id"));
        putVmDetailIfNotBlank(sourceVmId, DETAIL_REMOTE_REPLICA_VM_NAME,
                StringUtils.defaultIfBlank(getJsonString(remoteVm, "displayname"), getJsonString(remoteVm, "name")));
        putVmDetailIfNotBlank(sourceVmId, DETAIL_REMOTE_REPLICA_VM_INSTANCE_NAME,
                StringUtils.defaultIfBlank(getJsonString(remoteVm, "instancename"), getJsonString(remoteVm, "name")));
        putVmDetailIfNotBlank(sourceVmId, DETAIL_REMOTE_REPLICA_VM_STATE, getJsonString(remoteVm, "state"));
        putVmDetailIfNotBlank(sourceVmId, DETAIL_REMOTE_REPLICA_VM_HOST_ID, getJsonString(remoteVm, "hostid"));
        putVmDetailIfNotBlank(sourceVmId, DETAIL_REMOTE_REPLICA_VM_HOST_NAME, getJsonString(remoteVm, "hostname"));
        putVmDetail(sourceVmId, DETAIL_REMOTE_REPLICA_VM_STATE_UPDATED, Instant.now().toString());
    }

    private void repairSecondaryVmStateAfterFailedStart(UserVmVO primaryVm, UserVmVO secondaryVm) {
        if (secondaryVm == null) {
            return;
        }
        UserVmVO refreshedSecondaryVm = userVmDao.findById(secondaryVm.getId());
        if (refreshedSecondaryVm == null || refreshedSecondaryVm.getState() != VirtualMachine.State.Running) {
            return;
        }
        try {
            userVmService.stopVirtualMachine(refreshedSecondaryVm.getId(), true);
            publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                    String.format("Stopped FTCTL secondary VM %s after failed Cloud-managed manual failover start reconciliation",
                            refreshedSecondaryVm.getUuid()));
        } catch (ConcurrentOperationException e) {
            logger.warn(String.format("Unable to reconcile FTCTL secondary VM %s after failed manual failover start",
                    refreshedSecondaryVm.getUuid()), e);
        }
    }

    private void handoffNicIdentityToSecondary(UserVmVO primaryVm, UserVmVO secondaryVm, FtctlProtectionVO protection) {
        if (!FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED.equalsIgnoreCase(StringUtils.trimToEmpty(protection.getProvisioningBackend()))) {
            return;
        }
        if (secondaryVm.getState() != VirtualMachine.State.Stopped) {
            failNicIdentityHandoff(primaryVm, protection, String.format("secondary VM %s must be Stopped before NIC identity handoff, current state is %s",
                    secondaryVm.getUuid(), secondaryVm.getState()));
        }

        List<NicVO> primaryNics = nicDao.listByVmIdOrderByDeviceId(primaryVm.getId());
        List<NicVO> secondaryNics = nicDao.listByVmIdOrderByDeviceId(secondaryVm.getId());
        if (primaryNics == null || primaryNics.isEmpty()) {
            failNicIdentityHandoff(primaryVm, protection, String.format("primary VM %s has no NICs to hand off", primaryVm.getUuid()));
        }
        if (secondaryNics == null || secondaryNics.isEmpty()) {
            failNicIdentityHandoff(primaryVm, protection, String.format("secondary VM %s has no NICs to receive identity", secondaryVm.getUuid()));
        }
        persistCanonicalNicIdentities(primaryVm, secondaryVm, protection, false);

        int handoffCount = 0;
        for (NicVO primaryNic : primaryNics) {
            NicVO secondaryNic = findMatchingSecondaryNic(primaryNic, secondaryNics);
            if (secondaryNic == null) {
                failNicIdentityHandoff(primaryVm, protection, String.format(
                        "secondary VM %s does not have a matching NIC for primary network %s device %s",
                        secondaryVm.getUuid(), primaryNic.getNetworkId(), primaryNic.getDeviceId()));
            }
            NicIdentity primaryIdentity = requireStoredNicIdentity(primaryVm, protection, DETAIL_NIC_IDENTITY_PRIMARY_PREFIX, primaryNic);
            NicIdentity secondaryIdentity = requireStoredNicIdentity(primaryVm, protection, DETAIL_NIC_IDENTITY_SECONDARY_PREFIX, primaryNic);
            secondaryIdentity.applyTo(primaryNic);
            primaryIdentity.applyTo(secondaryNic);
            nicDao.update(primaryNic.getId(), primaryNic);
            nicDao.update(secondaryNic.getId(), secondaryNic);
            handoffCount++;
        }

        putVmDetail(primaryVm.getId(), DETAIL_NIC_IDENTITY_STATE, "secondary-owned");
        publishFtctlEvent(primaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                String.format("Handed off FTCTL NIC identity from primary VM %s to secondary VM %s for %s NIC(s)",
                        primaryVm.getUuid(), secondaryVm.getUuid(), handoffCount));
    }

    private void handoffNicIdentityToPrimary(UserVmVO primaryVm, UserVmVO secondaryVm, FtctlProtectionVO protection) {
        if (!isCloudManagedProvisioning(protection)) {
            return;
        }
        String nicIdentityState = StringUtils.trimToEmpty(getDetailValue(primaryVm.getId(), DETAIL_NIC_IDENTITY_STATE));
        if (!"secondary-owned".equalsIgnoreCase(nicIdentityState)) {
            logger.info(String.format("FTCTL NIC identity for primary VM %s is already primary-owned or unset: %s",
                    primaryVm.getUuid(), nicIdentityState));
            return;
        }

        UserVmVO currentPrimaryVm = validateVirtualMachineExists(primaryVm.getId());
        UserVmVO currentSecondaryVm = validateVirtualMachineExists(secondaryVm.getId());
        if (currentPrimaryVm.getState() != VirtualMachine.State.Stopped) {
            failNicIdentityHandoff(currentPrimaryVm, protection, String.format("primary VM %s must be Stopped before NIC identity failback, current state is %s",
                    currentPrimaryVm.getUuid(), currentPrimaryVm.getState()));
        }
        if (currentSecondaryVm.getState() != VirtualMachine.State.Stopped) {
            failNicIdentityHandoff(currentPrimaryVm, protection, String.format("secondary VM %s must be Stopped before NIC identity failback, current state is %s",
                    currentSecondaryVm.getUuid(), currentSecondaryVm.getState()));
        }

        List<NicVO> primaryNics = nicDao.listByVmIdOrderByDeviceId(currentPrimaryVm.getId());
        List<NicVO> secondaryNics = nicDao.listByVmIdOrderByDeviceId(currentSecondaryVm.getId());
        if (primaryNics == null || primaryNics.isEmpty()) {
            failNicIdentityHandoff(currentPrimaryVm, protection, String.format("primary VM %s has no NICs to restore", currentPrimaryVm.getUuid()));
        }
        if (secondaryNics == null || secondaryNics.isEmpty()) {
            failNicIdentityHandoff(currentPrimaryVm, protection, String.format("secondary VM %s has no NICs to return identity", currentSecondaryVm.getUuid()));
        }

        int handoffCount = 0;
        for (NicVO primaryNic : primaryNics) {
            NicVO secondaryNic = findMatchingSecondaryNic(primaryNic, secondaryNics);
            if (secondaryNic == null) {
                failNicIdentityHandoff(currentPrimaryVm, protection, String.format(
                        "secondary VM %s does not have a matching NIC for primary network %s device %s",
                        currentSecondaryVm.getUuid(), primaryNic.getNetworkId(), primaryNic.getDeviceId()));
            }
            NicIdentity primaryIdentity = requireStoredNicIdentity(currentPrimaryVm, protection, DETAIL_NIC_IDENTITY_PRIMARY_PREFIX, primaryNic);
            NicIdentity secondaryIdentity = requireStoredNicIdentity(currentPrimaryVm, protection, DETAIL_NIC_IDENTITY_SECONDARY_PREFIX, primaryNic);
            primaryIdentity.applyTo(primaryNic);
            secondaryIdentity.applyTo(secondaryNic);
            nicDao.update(primaryNic.getId(), primaryNic);
            nicDao.update(secondaryNic.getId(), secondaryNic);
            handoffCount++;
        }

        putVmDetail(currentPrimaryVm.getId(), DETAIL_NIC_IDENTITY_STATE, "primary-owned");
        publishFtctlEvent(currentPrimaryVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                String.format("Returned FTCTL NIC identity from secondary VM %s to primary VM %s for %s NIC(s)",
                        currentSecondaryVm.getUuid(), currentPrimaryVm.getUuid(), handoffCount));
    }

    private NicVO findMatchingSecondaryNic(NicVO primaryNic, List<NicVO> secondaryNics) {
        NicVO deviceMatch = null;
        for (NicVO secondaryNic : secondaryNics) {
            if (Objects.equals(secondaryNic.getNetworkId(), primaryNic.getNetworkId()) &&
                    Objects.equals(secondaryNic.getDeviceId(), primaryNic.getDeviceId())) {
                return secondaryNic;
            }
            if (Objects.equals(secondaryNic.getDeviceId(), primaryNic.getDeviceId())) {
                deviceMatch = secondaryNic;
            }
        }
        return deviceMatch;
    }

    private void persistCloudManagedNicIdentities(UserVmVO primaryVm) {
        FtctlProtectionVO protection = ftctlProtectionDao.findActiveByPrimaryVmId(primaryVm.getId());
        if (!isCloudManagedProvisioning(protection) || protection.getSecondaryVmId() == null) {
            return;
        }
        UserVmVO secondaryVm = userVmDao.findById(protection.getSecondaryVmId());
        if (secondaryVm == null) {
            throw new CloudRuntimeException(String.format("Unable to find FTCTL secondary VM %s for primary VM %s",
                    protection.getSecondaryVmId(), primaryVm.getUuid()));
        }
        persistCanonicalNicIdentities(primaryVm, secondaryVm, protection, true);
        String nicIdentityState = StringUtils.trimToEmpty(getDetailValue(primaryVm.getId(), DETAIL_NIC_IDENTITY_STATE));
        if (StringUtils.isBlank(nicIdentityState)) {
            putVmDetail(primaryVm.getId(), DETAIL_NIC_IDENTITY_STATE, "primary-owned");
        }
    }

    private void persistCanonicalNicIdentities(UserVmVO primaryVm, UserVmVO secondaryVm, FtctlProtectionVO protection, boolean overwriteSecondary) {
        List<NicVO> primaryNics = nicDao.listByVmIdOrderByDeviceId(primaryVm.getId());
        List<NicVO> secondaryNics = nicDao.listByVmIdOrderByDeviceId(secondaryVm.getId());
        if (primaryNics == null || primaryNics.isEmpty()) {
            failNicIdentityHandoff(primaryVm, protection, String.format("primary VM %s has no NICs to persist identity", primaryVm.getUuid()));
        }
        if (secondaryNics == null || secondaryNics.isEmpty()) {
            failNicIdentityHandoff(primaryVm, protection, String.format("secondary VM %s has no NICs to persist identity", secondaryVm.getUuid()));
        }

        for (NicVO primaryNic : primaryNics) {
            NicVO secondaryNic = findMatchingSecondaryNic(primaryNic, secondaryNics);
            if (secondaryNic == null) {
                failNicIdentityHandoff(primaryVm, protection, String.format(
                        "secondary VM %s does not have a matching NIC for primary network %s device %s",
                        secondaryVm.getUuid(), primaryNic.getNetworkId(), primaryNic.getDeviceId()));
            }
            persistNicIdentityIfMissing(primaryVm.getId(), DETAIL_NIC_IDENTITY_PRIMARY_PREFIX, primaryNic, false);
            persistNicIdentityIfMissing(primaryVm.getId(), DETAIL_NIC_IDENTITY_SECONDARY_PREFIX, primaryNic, secondaryNic, overwriteSecondary);
        }
    }

    private void persistNicIdentityIfMissing(Long primaryVmId, String prefix, NicVO keyNic, boolean overwrite) {
        persistNicIdentityIfMissing(primaryVmId, prefix, keyNic, keyNic, overwrite);
    }

    private void persistNicIdentityIfMissing(Long primaryVmId, String prefix, NicVO keyNic, NicVO sourceNic, boolean overwrite) {
        String key = buildNicIdentityDetailKey(prefix, keyNic);
        if (!overwrite && StringUtils.isNotBlank(getDetailValue(primaryVmId, key))) {
            return;
        }
        putVmDetail(primaryVmId, key, new NicIdentity(sourceNic).toJson());
    }

    private NicIdentity requireStoredNicIdentity(UserVmVO primaryVm, FtctlProtectionVO protection, String prefix, NicVO keyNic) {
        String key = buildNicIdentityDetailKey(prefix, keyNic);
        String value = getDetailValue(primaryVm.getId(), key);
        if (StringUtils.isBlank(value)) {
            failNicIdentityHandoff(primaryVm, protection, String.format("missing stored NIC identity %s for network %s device %s",
                    key, keyNic.getNetworkId(), keyNic.getDeviceId()));
        }
        try {
            return NicIdentity.fromJson(value);
        } catch (RuntimeException e) {
            failNicIdentityHandoff(primaryVm, protection, String.format("invalid stored NIC identity %s for network %s device %s",
                    key, keyNic.getNetworkId(), keyNic.getDeviceId()));
            return null;
        }
    }

    private String buildNicIdentityDetailKey(String prefix, NicVO nic) {
        return String.format(Locale.ROOT, "%s%s.%s", prefix, nic.getNetworkId(), nic.getDeviceId());
    }

    private void failNicIdentityHandoff(UserVmVO primaryVm, FtctlProtectionVO protection, String reason) {
        String lastError = String.format("nic_identity_handoff_failed: %s", reason);
        protection.setLastError(lastError);
        protection.markUpdated();
        ftctlProtectionDao.update(protection.getId(), protection);
        putVmDetail(primaryVm.getId(), DETAIL_LAST_ERROR, lastError);
        throw new CloudRuntimeException(String.format("FTCTL NIC identity handoff failed for VM %s: %s", primaryVm.getUuid(), reason));
    }

    private static final class NicIdentity {
        private final String ipv4Address;
        private final String ipv4Gateway;
        private final String ipv4Netmask;
        private final String ipv6Address;
        private final String ipv6Gateway;
        private final String ipv6Cidr;
        private final String macAddress;

        private NicIdentity(NicVO nic) {
            ipv4Address = nic.getIPv4Address();
            ipv4Gateway = nic.getIPv4Gateway();
            ipv4Netmask = nic.getIPv4Netmask();
            ipv6Address = nic.getIPv6Address();
            ipv6Gateway = nic.getIPv6Gateway();
            ipv6Cidr = nic.getIPv6Cidr();
            macAddress = nic.getMacAddress();
        }

        private NicIdentity(String ipv4Address, String ipv4Gateway, String ipv4Netmask, String ipv6Address,
                            String ipv6Gateway, String ipv6Cidr, String macAddress) {
            this.ipv4Address = ipv4Address;
            this.ipv4Gateway = ipv4Gateway;
            this.ipv4Netmask = ipv4Netmask;
            this.ipv6Address = ipv6Address;
            this.ipv6Gateway = ipv6Gateway;
            this.ipv6Cidr = ipv6Cidr;
            this.macAddress = macAddress;
        }

        private static NicIdentity fromJson(String value) {
            JsonObject object = JsonParser.parseString(value).getAsJsonObject();
            return new NicIdentity(
                    getNullableJsonString(object, "ipv4Address"),
                    getNullableJsonString(object, "ipv4Gateway"),
                    getNullableJsonString(object, "ipv4Netmask"),
                    getNullableJsonString(object, "ipv6Address"),
                    getNullableJsonString(object, "ipv6Gateway"),
                    getNullableJsonString(object, "ipv6Cidr"),
                    getNullableJsonString(object, "macAddress"));
        }

        private String toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("ipv4Address", ipv4Address);
            object.addProperty("ipv4Gateway", ipv4Gateway);
            object.addProperty("ipv4Netmask", ipv4Netmask);
            object.addProperty("ipv6Address", ipv6Address);
            object.addProperty("ipv6Gateway", ipv6Gateway);
            object.addProperty("ipv6Cidr", ipv6Cidr);
            object.addProperty("macAddress", macAddress);
            return object.toString();
        }

        private static String getNullableJsonString(JsonObject object, String key) {
            if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
                return null;
            }
            return object.get(key).getAsString();
        }

        private void applyTo(NicVO nic) {
            nic.setIPv4Address(ipv4Address);
            nic.setIPv4Gateway(ipv4Gateway);
            nic.setIPv4Netmask(ipv4Netmask);
            nic.setIPv6Address(ipv6Address);
            nic.setIPv6Gateway(ipv6Gateway);
            nic.setIPv6Cidr(ipv6Cidr);
            nic.setMacAddress(macAddress);
        }
    }

    private Long resolvePeerHostId(Long primaryVmId) {
        String peerHostId = getDetailValue(primaryVmId, DETAIL_PEER_HOST_ID);
        if (StringUtils.isBlank(peerHostId)) {
            return null;
        }
        try {
            return Long.parseLong(peerHostId);
        } catch (NumberFormatException e) {
            throw new CloudRuntimeException(String.format("Invalid FTCTL peer host id %s for VM %s", peerHostId, primaryVmId), e);
        }
    }

    private UserVmVO validateVirtualMachineExists(Long virtualMachineId) {
        UserVmVO userVm = virtualMachineId != null ? userVmDao.findById(virtualMachineId) : null;
        if (userVm == null) {
            throw new CloudRuntimeException(String.format("Unable to find virtual machine with id %s", virtualMachineId));
        }
        return userVm;
    }

    private void validatePrimaryProtectionTarget(UserVmVO userVm) {
        if (userVm == null) {
            return;
        }
        FtctlProtectionVO protection = ftctlProtectionDao.findActiveBySecondaryVmId(userVm.getId());
        if (protection != null) {
            throw new CloudRuntimeException(String.format("FTCTL standby VM %s is managed from primary VM %s",
                    userVm.getUuid(), protection.getPrimaryVmId()));
        }
    }

    private void validateProtectionRegistrationVmState(UserVmVO userVm) {
        if (userVm == null) {
            return;
        }
        if (userVm.getState() != VirtualMachine.State.Running) {
            throw new CloudRuntimeException(String.format("FTCTL protection registration requires VM %s to be Running, current state is %s",
                    userVm.getUuid(), userVm.getState()));
        }
    }

    private FtctlProtectionResponse buildProtectionResponse(UserVmVO requestedVm) {
        Long requestedVmId = requestedVm != null ? requestedVm.getId() : null;
        FtctlProtectionVO protection = findActiveProtectionForVm(requestedVmId);
        if (protection == null && isClosedReplicaRecoverySession(requestedVmId)) {
            return buildUnconfiguredProtectionResponse(requestedVm);
        }
        if (protection == null && isRemoteMoldReplicaStandbyVm(requestedVmId)) {
            return buildRemoteMoldStandbyProtectionResponse(requestedVm);
        }
        boolean standbyView = isStandbyProtectionVm(requestedVmId, protection);
        Long primaryVmId = protection != null ? protection.getPrimaryVmId() : requestedVmId;
        Long sourceVmId = primaryVmId;
        FtctlProtectionResponse response = new FtctlProtectionResponse();
        response.setVirtualMachineId(requestedVmId);
        response.setProtectionRole(standbyView ? "standby" : protection != null ? "primary" : null);
        response.setPrimaryVirtualMachineId(primaryVmId);
        response.setPrimaryVirtualMachineName(resolveVmDisplayName(primaryVmId));
        response.setPrimaryVirtualMachineUuid(resolveVmUuid(primaryVmId));
        response.setPrimaryVirtualMachineState(resolveVmState(primaryVmId));
        response.setPrimaryVirtualMachineHostId(resolveVmHostId(primaryVmId));
        response.setPrimaryVirtualMachineHostName(resolveVmHostName(primaryVmId));
        Long secondaryVmId = protection != null ? protection.getSecondaryVmId() : null;
        response.setSecondaryVirtualMachineId(secondaryVmId);
        response.setSecondaryVirtualMachineUuid(resolveVmUuid(secondaryVmId));
        response.setSecondaryVirtualMachineDisplayName(resolveVmDisplayName(secondaryVmId));
        response.setSecondaryVirtualMachineState(resolveVmState(secondaryVmId));
        response.setSecondaryVirtualMachineHostId(resolveVmHostId(secondaryVmId));
        response.setSecondaryVirtualMachineHostName(resolveVmHostName(secondaryVmId));
        response.setEnabled(getDetailValue(sourceVmId, DETAIL_ENABLED));
        response.setMode(getDetailValue(sourceVmId, DETAIL_MODE));
        response.setBackendMode(getDetailValue(sourceVmId, DETAIL_BACKEND_MODE));
        response.setProvisioningBackend(getDetailValue(sourceVmId, DETAIL_PROVISIONING_BACKEND));
        response.setProvisioningState(getDetailValue(sourceVmId, DETAIL_PROVISIONING_STATE));
        response.setTargetStorageScope(getDetailValue(sourceVmId, DETAIL_TARGET_STORAGE_SCOPE));
        String drPeerSiteType = getDetailValue(sourceVmId, DETAIL_DR_PEER_SITE_TYPE);
        response.setDrPeerSiteType(drPeerSiteType);
        response.setRemoteMoldApiUrl(getDetailValue(sourceVmId, DETAIL_REMOTE_MOLD_API_URL));
        boolean remoteMoldDr = DR_PEER_SITE_TYPE_REMOTE_MOLD.equalsIgnoreCase(drPeerSiteType);
        if (remoteMoldDr && secondaryVmId == null) {
            populateRemoteReplicaVmSnapshot(sourceVmId, response);
        }
        response.setTargetStoragePoolId(StringUtils.defaultIfBlank(getDetailValue(sourceVmId, DETAIL_TARGET_STORAGE_POOL_ID),
                remoteMoldDr ? getDetailValue(sourceVmId, DETAIL_REMOTE_TARGET_STORAGE_POOL_ID) : null));
        response.setTargetStoragePoolName(StringUtils.defaultIfBlank(getDetailValue(sourceVmId, DETAIL_TARGET_STORAGE_POOL_NAME),
                remoteMoldDr ? getDetailValue(sourceVmId, DETAIL_REMOTE_TARGET_STORAGE_POOL_NAME) : null));
        response.setFencingPolicy(getDetailValue(sourceVmId, DETAIL_FENCING_POLICY));
        String peerHostId = StringUtils.defaultIfBlank(getDetailValue(sourceVmId, DETAIL_PEER_HOST_ID),
                remoteMoldDr ? getDetailValue(sourceVmId, DETAIL_REMOTE_PEER_HOST_ID) : null);
        response.setPeerHostId(peerHostId);
        response.setPeerHostName(remoteMoldDr ? getDetailValue(sourceVmId, DETAIL_REMOTE_PEER_HOST_NAME) : resolvePeerHostName(peerHostId));
        response.setSecondaryVmName(StringUtils.defaultIfBlank(protection != null ? protection.getSecondaryVmName() : null,
                getDetailValue(sourceVmId, DETAIL_SECONDARY_VM_NAME)));
        response.setSecondaryTargetDir(getDetailValue(sourceVmId, DETAIL_SECONDARY_TARGET_DIR));
        populateCloudManagedDiskDetails(sourceVmId, protection, response);
        populateXcoloPortAllocationDetails(sourceVmId, protection, response);
        populateFtMachineCompatibility(sourceVmId, response);
        response.setProtectionState(getDetailValue(sourceVmId, DETAIL_LAST_PROTECTION_STATE));
        response.setTransportState(getDetailValue(sourceVmId, DETAIL_LAST_TRANSPORT_STATE));
        response.setActiveSide(getDetailValue(sourceVmId, DETAIL_LAST_ACTIVE_SIDE));
        response.setAdminState(getDetailValue(sourceVmId, DETAIL_LAST_ADMIN_STATE));
        response.setFencingState(getDetailValue(sourceVmId, DETAIL_LAST_FENCING_STATE));
        response.setFencingResult(getDetailValue(sourceVmId, DETAIL_FENCING_RESULT));
        response.setFencingReason(getDetailValue(sourceVmId, DETAIL_FENCING_REASON));
        response.setLastError(getDetailValue(sourceVmId, DETAIL_LAST_ERROR));
        purgeLegacyProgressDetails(sourceVmId);
        return response;
    }

    private void populateXcoloPortAllocationDetails(Long sourceVmId, FtctlProtectionVO protection, FtctlProtectionResponse response) {
        response.setRemoteNbdExportAddr(getDetailValue(sourceVmId, DETAIL_REMOTE_NBD_EXPORT_ADDR));
        XcoloPortAllocation allocation = resolveStoredXcoloPortAllocation(sourceVmId, protection);
        if (allocation == null || !allocation.enabled) {
            return;
        }
        response.setRemoteNbdExportAddr(allocation.remoteNbdExportAddr);
        response.setXcoloProxyEndpoint(allocation.proxyEndpoint);
        response.setXcoloNbdEndpoint(allocation.nbdEndpoint);
        response.setXcoloMigrateUri(allocation.migrateUri);
    }

    private XcoloPortAllocation resolveStoredXcoloPortAllocation(Long sourceVmId, FtctlProtectionVO protection) {
        if (protection == null || !"auto".equalsIgnoreCase(protection.getXcoloPortAllocationMode()) || protection.getXcoloPortSlot() == null) {
            return XcoloPortAllocation.disabled(getDetailValue(sourceVmId, DETAIL_REMOTE_NBD_EXPORT_ADDR));
        }
        String peerAddress = resolveStoredXcoloPeerAddress(sourceVmId, protection);
        if (StringUtils.isBlank(peerAddress)) {
            logger.debug("Unable to resolve stored FTCTL x-colo peer address for VM [{}] protection [{}]", sourceVmId, protection.getId());
            return XcoloPortAllocation.disabled(getDetailValue(sourceVmId, DETAIL_REMOTE_NBD_EXPORT_ADDR));
        }
        return buildAutomaticXcoloPortAllocation(peerAddress, protection.getXcoloPortSlot());
    }

    private String resolveStoredXcoloPeerAddress(Long sourceVmId, FtctlProtectionVO protection) {
        String drPeerSiteType = getDetailValue(sourceVmId, DETAIL_DR_PEER_SITE_TYPE);
        if (DR_PEER_SITE_TYPE_REMOTE_MOLD.equalsIgnoreCase(drPeerSiteType)) {
            return StringUtils.defaultIfBlank(getDetailValue(sourceVmId, DETAIL_REMOTE_PEER_HOST_BLOCKCOPY_ADDRESS),
                    getDetailValue(sourceVmId, DETAIL_REMOTE_PEER_HOST_ADDRESS));
        }
        Long peerHostId = protection != null ? protection.getPeerHostId() : null;
        if (peerHostId == null) {
            peerHostId = parseLongOrNull(getDetailValue(sourceVmId, DETAIL_PEER_HOST_ID));
        }
        if (peerHostId == null) {
            return null;
        }
        HostVO peerHost = hostDao.findById(peerHostId);
        if (peerHost == null) {
            return null;
        }
        return StringUtils.defaultIfBlank(resolveHostField(peerHost, HOST_DETAIL_XCOLO_DATA_IP, null),
                resolveHostField(peerHost, HOST_DETAIL_BLOCKCOPY_IP, peerHost.getPrivateIpAddress()));
    }

    private Long parseLongOrNull(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(StringUtils.trim(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private FtctlProtectionResponse buildUnconfiguredProtectionResponse(UserVmVO requestedVm) {
        FtctlProtectionResponse response = new FtctlProtectionResponse();
        Long requestedVmId = requestedVm != null ? requestedVm.getId() : null;
        response.setVirtualMachineId(requestedVmId);
        response.setPrimaryVirtualMachineId(requestedVmId);
        response.setPrimaryVirtualMachineName(resolveVmDisplayName(requestedVmId));
        response.setPrimaryVirtualMachineUuid(resolveVmUuid(requestedVmId));
        response.setPrimaryVirtualMachineState(resolveVmState(requestedVmId));
        response.setPrimaryVirtualMachineHostId(resolveVmHostId(requestedVmId));
        response.setPrimaryVirtualMachineHostName(resolveVmHostName(requestedVmId));
        populateFtMachineCompatibility(requestedVmId, response);
        return response;
    }

    private FtctlProtectionResponse buildRemoteMoldStandbyProtectionResponse(UserVmVO standbyVm) {
        FtctlProtectionResponse response = new FtctlProtectionResponse();
        Long standbyVmId = standbyVm != null ? standbyVm.getId() : null;
        boolean runningReplica = standbyVm != null && standbyVm.getState() == VirtualMachine.State.Running;
        String recoveryRole = StringUtils.defaultIfBlank(getDetailValue(standbyVmId, DETAIL_DR_RECOVERY_ROLE),
                runningReplica ? "active-replica" : "standby");
        response.setVirtualMachineId(standbyVmId);
        response.setProtectionRole("standby");
        response.setPrimaryVirtualMachineUuid(StringUtils.defaultIfBlank(getDetailValue(standbyVmId, DETAIL_REMOTE_SOURCE_VM_UUID),
                getDetailValue(standbyVmId, DETAIL_REMOTE_PRIMARY_VM_ID)));
        response.setPrimaryVirtualMachineName(StringUtils.defaultIfBlank(getDetailValue(standbyVmId, DETAIL_REMOTE_SOURCE_VM_NAME),
                getDetailValue(standbyVmId, DETAIL_REMOTE_SOURCE_VM_INSTANCE_NAME)));
        response.setSecondaryVirtualMachineId(standbyVmId);
        response.setSecondaryVirtualMachineUuid(standbyVm != null ? standbyVm.getUuid() : null);
        response.setSecondaryVirtualMachineDisplayName(standbyVm != null ? StringUtils.defaultIfBlank(standbyVm.getDisplayName(), standbyVm.getHostName()) : null);
        response.setSecondaryVirtualMachineState(standbyVm != null && standbyVm.getState() != null ? standbyVm.getState().toString() : null);
        Long hostId = getExecutionHostId(standbyVm);
        response.setSecondaryVirtualMachineHostId(hostId);
        response.setSecondaryVirtualMachineHostName(resolveHostName(hostId));
        response.setEnabled("true");
        response.setMode("dr");
        response.setBackendMode("remote-nbd");
        response.setProvisioningBackend(FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED);
        response.setProvisioningState(FtctlProtectionProvisioningService.STATE_READY);
        response.setDrPeerSiteType(DR_PEER_SITE_TYPE_REMOTE_MOLD);
        response.setRemoteMoldApiUrl(getDetailValue(standbyVmId, DETAIL_DR_RECOVERY_SOURCE_MOLD_API_URL));
        response.setProtectionState(runningReplica ? "failed_over" : "protected");
        response.setTransportState(runningReplica ? "failed_over" : "not_available");
        response.setActiveSide(runningReplica ? "secondary" : "primary");
        response.setAdminState(runningReplica ? "active" : "read-only");
        response.setFencingState(runningReplica ? "clear" : "not_available");
        response.setFencingResult(getDetailValue(standbyVmId, DETAIL_FENCING_RESULT));
        response.setFencingReason(StringUtils.defaultIfBlank(getDetailValue(standbyVmId, DETAIL_FENCING_REASON), recoveryRole));
        response.setLastError(getDetailValue(standbyVmId, DETAIL_DR_RECOVERY_LAST_ERROR));
        response.setSecondaryVmName(standbyVm != null ? standbyVm.getInstanceName() : null);
        populateRemoteMoldStandbyVolumes(standbyVmId, response);
        return response;
    }

    private FtctlCheckResponse buildRemoteMoldStandbyCheckResponse(UserVmVO standbyVm) {
        FtctlCheckResponse response = new FtctlCheckResponse();
        response.setObjectName("ftctlcheck");
        response.setVirtualMachineId(standbyVm != null ? standbyVm.getId() : null);
        response.setVmName(standbyVm != null ? standbyVm.getInstanceName() : null);
        response.setResult("not_available");
        response.setInventoryResult("not_available");
        response.setStandbyDomainState("not_available");
        response.setProvisioningBackend(FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED);
        return response;
    }

    private FtctlHealthResponse buildRemoteMoldStandbyHealthResponse(UserVmVO standbyVm) {
        FtctlHealthResponse response = new FtctlHealthResponse();
        response.setObjectName("ftctlhealth");
        response.setVirtualMachineId(standbyVm != null ? standbyVm.getId() : null);
        Long hostId = getExecutionHostId(standbyVm);
        response.setHostId(hostId);
        response.setHostName(resolveHostName(hostId));
        response.setResult("not_available");
        return response;
    }

    private FtctlEventsResponse buildRemoteMoldStandbyEventsResponse(UserVmVO standbyVm) {
        FtctlEventsResponse response = new FtctlEventsResponse();
        response.setObjectName("ftctlevents");
        response.setVirtualMachineId(standbyVm != null ? standbyVm.getId() : null);
        response.setVmName(standbyVm != null ? standbyVm.getInstanceName() : null);
        response.setResult("not_available");
        response.setCount(0);
        response.setEvents(Collections.emptyList());
        return response;
    }

    private boolean isRemoteMoldReplicaStandbyVm(Long virtualMachineId) {
        return isTrueDetail(virtualMachineId, DETAIL_REMOTE_MOLD_REPLICA_VM) &&
                isTrueDetail(virtualMachineId, DETAIL_REMOTE_STANDBY_VM);
    }

    private boolean isClosedReplicaRecoverySession(Long virtualMachineId) {
        if (!isTrueDetail(virtualMachineId, DETAIL_DR_RECOVERY_RELEASED)) {
            return false;
        }
        String role = StringUtils.trimToEmpty(getDetailValue(virtualMachineId, DETAIL_DR_RECOVERY_ROLE)).toLowerCase(Locale.ROOT);
        return "adopted".equals(role) || "released".equals(role);
    }

    private void clearClosedReplicaRecoverySessionBeforeRegister(Long virtualMachineId) {
        if (findActiveProtectionForVm(virtualMachineId) == null && isClosedReplicaRecoverySession(virtualMachineId)) {
            vmInstanceDetailsDao.removeDetailsWithPrefix(virtualMachineId, "ftctl.");
        }
    }

    private boolean isTrueDetail(Long virtualMachineId, String key) {
        return "true".equalsIgnoreCase(StringUtils.trimToEmpty(getDetailValue(virtualMachineId, key)));
    }

    private void populateRemoteMoldStandbyVolumes(Long standbyVmId, FtctlProtectionResponse response) {
        if (standbyVmId == null || response == null) {
            return;
        }
        List<VolumeVO> volumes = volumeDao.findByInstance(standbyVmId);
        if (volumes == null || volumes.isEmpty()) {
            return;
        }
        List<FtctlProtectionVolumeResponse> secondaryVolumes = new ArrayList<>();
        List<String> secondaryDiskEntries = new ArrayList<>();
        volumes.stream()
                .filter(volume -> volume != null && volume.getRemoved() == null && isProtectedVolumeType(volume))
                .sorted(Comparator.comparingLong(this::resolveDeviceIdForSort))
                .forEach(volume -> {
                    FtctlProtectionVolumeResponse volumeResponse = new FtctlProtectionVolumeResponse();
                    volumeResponse.setObjectName("ftctlprotectionvolume");
                    volumeResponse.setId(volume.getUuid());
                    volumeResponse.setName(resolveVolumeDisplayName(volume));
                    volumeResponse.setPath(volume.getPath());
                    volumeResponse.setState(volume.getState() != null ? volume.getState().toString() : null);
                    volumeResponse.setDiskLabel(resolveDiskLabel(volume));
                    secondaryVolumes.add(volumeResponse);
                    if (StringUtils.isNotBlank(volume.getPath())) {
                        secondaryDiskEntries.add(String.format("%s=%s", resolveDiskLabel(volume), volume.getPath()));
                    }
                });
        if (!secondaryVolumes.isEmpty()) {
            response.setSecondaryVolumes(secondaryVolumes);
        }
        if (!secondaryDiskEntries.isEmpty()) {
            response.setSecondaryTargetDisk(secondaryDiskEntries.stream().collect(Collectors.joining(";")));
        }
    }

    private FtctlProtectionVO findActiveProtectionForVm(Long virtualMachineId) {
        if (virtualMachineId == null) {
            return null;
        }
        FtctlProtectionVO protection = ftctlProtectionDao.findActiveByPrimaryVmId(virtualMachineId);
        return protection != null ? protection : ftctlProtectionDao.findActiveBySecondaryVmId(virtualMachineId);
    }

    private boolean isStandbyProtectionVm(Long virtualMachineId, FtctlProtectionVO protection) {
        return virtualMachineId != null && protection != null && protection.getSecondaryVmId() != null &&
                protection.getSecondaryVmId().equals(virtualMachineId);
    }

    private UserVmVO resolveRuntimeVmForProtectionView(UserVmVO requestedVm) {
        if (requestedVm == null) {
            return null;
        }
        FtctlProtectionVO protection = findActiveProtectionForVm(requestedVm.getId());
        if (isStandbyProtectionVm(requestedVm.getId(), protection)) {
            return userVmDao.findById(protection.getPrimaryVmId());
        }
        return requestedVm;
    }

    private String resolveVmDisplayName(Long virtualMachineId) {
        UserVmVO vm = virtualMachineId != null ? userVmDao.findById(virtualMachineId) : null;
        return vm != null ? StringUtils.defaultIfBlank(vm.getDisplayName(), vm.getHostName()) : null;
    }

    private String resolveVmUuid(Long virtualMachineId) {
        UserVmVO vm = virtualMachineId != null ? userVmDao.findById(virtualMachineId) : null;
        return vm != null ? vm.getUuid() : null;
    }

    private String resolveVmState(Long virtualMachineId) {
        UserVmVO vm = virtualMachineId != null ? userVmDao.findById(virtualMachineId) : null;
        return vm != null && vm.getState() != null ? vm.getState().toString() : null;
    }

    private Long resolveVmHostId(Long virtualMachineId) {
        UserVmVO vm = virtualMachineId != null ? userVmDao.findById(virtualMachineId) : null;
        return vm != null ? vm.getHostId() : null;
    }

    private String resolveVmHostName(Long virtualMachineId) {
        Long hostId = resolveVmHostId(virtualMachineId);
        return resolveHostName(hostId);
    }

    private void populateCloudManagedDiskDetails(Long primaryVmId, FtctlProtectionVO protection, FtctlProtectionResponse response) {
        if (protection == null && primaryVmId != null) {
            protection = ftctlProtectionDao.findActiveByPrimaryVmId(primaryVmId);
        }
        if (protection == null) {
            return;
        }
        List<FtctlProtectionVolumeVO> volumes = ftctlProtectionVolumeDao.listActiveByProtectionId(protection.getId());
        if (volumes == null || volumes.isEmpty()) {
            return;
        }
        StoragePoolVO targetStoragePool = protection.getTargetStoragePoolId() != null ? primaryDataStoreDao.findById(protection.getTargetStoragePoolId()) : null;
        List<String> secondaryDiskEntries = new ArrayList<>();
        List<String> diskMapEntries = new ArrayList<>();
        List<FtctlProtectionVolumeResponse> secondaryVolumes = new ArrayList<>();
        for (FtctlProtectionVolumeVO volume : volumes) {
            String secondaryPath = resolveFtctlSecondaryDiskPath(targetStoragePool, volume.getSecondaryDiskPath());
            VolumeVO secondaryVolume = volume.getSecondaryVolumeId() != null ? volumeDao.findById(volume.getSecondaryVolumeId()) : null;
            String remoteVolumePrefix = DETAIL_REMOTE_REPLICA_VOLUME_PREFIX + volume.getPrimaryVolumeId() + ".";
            String remoteVolumeId = primaryVmId != null ? getDetailValue(primaryVmId, remoteVolumePrefix + "id") : null;
            if (secondaryVolume != null || StringUtils.isNotBlank(remoteVolumeId)) {
                FtctlProtectionVolumeResponse volumeResponse = new FtctlProtectionVolumeResponse();
                volumeResponse.setObjectName("ftctlprotectionvolume");
                volumeResponse.setId(secondaryVolume != null ? secondaryVolume.getUuid() : remoteVolumeId);
                volumeResponse.setName(secondaryVolume != null ? resolveVolumeDisplayName(secondaryVolume.getId()) : getDetailValue(primaryVmId, remoteVolumePrefix + "name"));
                volumeResponse.setPath(StringUtils.defaultIfBlank(secondaryPath,
                        secondaryVolume != null ? secondaryVolume.getPath() : getDetailValue(primaryVmId, remoteVolumePrefix + "path")));
                volumeResponse.setState(secondaryVolume != null && secondaryVolume.getState() != null ?
                        secondaryVolume.getState().toString() : getDetailValue(primaryVmId, remoteVolumePrefix + "state"));
                volumeResponse.setDiskLabel(volume.getDiskLabel());
                secondaryVolumes.add(volumeResponse);
            }
            if (StringUtils.isNotBlank(secondaryPath)) {
                String label = StringUtils.defaultIfBlank(volume.getDiskLabel(), resolveVolumeDisplayName(volume.getPrimaryVolumeId()));
                secondaryDiskEntries.add(String.format("%s=%s", label, secondaryPath));
                diskMapEntries.add(String.format("%s=%s", resolveKvmDiskTarget(primaryVmId, volume.getPrimaryVolumeId()), secondaryPath));
            }
        }
        if (!secondaryDiskEntries.isEmpty()) {
            response.setSecondaryTargetDisk(secondaryDiskEntries.stream().collect(Collectors.joining(";")));
            response.setDiskMap(diskMapEntries.stream().collect(Collectors.joining(";")));
        }
        if (!secondaryVolumes.isEmpty()) {
            response.setSecondaryVolumes(secondaryVolumes);
        }
    }

    private void populateRemoteReplicaVmSnapshot(Long sourceVmId, FtctlProtectionResponse response) {
        if (sourceVmId == null || response == null) {
            return;
        }
        response.setSecondaryVirtualMachineUuid(getDetailValue(sourceVmId, DETAIL_REMOTE_REPLICA_VM_ID));
        response.setSecondaryVirtualMachineDisplayName(getDetailValue(sourceVmId, DETAIL_REMOTE_REPLICA_VM_NAME));
        response.setSecondaryVirtualMachineState(getDetailValue(sourceVmId, DETAIL_REMOTE_REPLICA_VM_STATE));
        response.setSecondaryVirtualMachineHostId(parseOptionalLong(getDetailValue(sourceVmId, DETAIL_REMOTE_REPLICA_VM_HOST_ID)));
        response.setSecondaryVirtualMachineHostName(getDetailValue(sourceVmId, DETAIL_REMOTE_REPLICA_VM_HOST_NAME));
    }

    private Long parseOptionalLong(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String resolveFtctlSecondaryDiskPath(StoragePoolVO targetStoragePool, String secondaryDiskPath) {
        String normalizedPath = StringUtils.trimToNull(secondaryDiskPath);
        if (normalizedPath == null || StringUtils.startsWithAny(normalizedPath, "/dev/", "rbd:")) {
            return normalizedPath;
        }
        if (targetStoragePool == null) {
            return normalizedPath;
        }
        if (!Storage.StoragePoolType.RBD.equals(targetStoragePool.getPoolType())) {
            if (StringUtils.startsWith(normalizedPath, "/") || StringUtils.isBlank(targetStoragePool.getPath())) {
                return normalizedPath;
            }
            return String.format("%s/%s", StringUtils.removeEnd(targetStoragePool.getPath(), "/"), StringUtils.removeStart(normalizedPath, "/"));
        }

        String poolName = resolveRbdPoolName(targetStoragePool.getPath());
        if (StringUtils.isBlank(poolName)) {
            return normalizedPath;
        }
        String imageName = StringUtils.stripStart(normalizedPath, "/");
        String poolPrefix = poolName + "/";
        if (imageName.startsWith(poolPrefix)) {
            imageName = imageName.substring(poolPrefix.length());
        }
        return StringUtils.isNotBlank(imageName) ? String.format("/dev/rbd/%s/%s", poolName, imageName) : normalizedPath;
    }

    private String resolveRbdPoolName(String poolPath) {
        String normalizedPath = StringUtils.stripEnd(StringUtils.trimToEmpty(poolPath), "/");
        if (normalizedPath.startsWith("rbd://")) {
            normalizedPath = normalizedPath.substring("rbd://".length());
        }
        int lastSlash = normalizedPath.lastIndexOf('/');
        if (lastSlash >= 0) {
            normalizedPath = normalizedPath.substring(lastSlash + 1);
        }
        return normalizedPath;
    }

    private String resolveVolumeDisplayName(long volumeId) {
        VolumeVO volume = volumeDao.findById(volumeId);
        if (volume == null) {
            return String.valueOf(volumeId);
        }
        return resolveVolumeDisplayName(volume);
    }

    private String resolveVolumeDisplayName(VolumeVO volume) {
        if (volume == null) {
            return null;
        }
        return StringUtils.defaultIfBlank(volume.getName(), StringUtils.defaultIfBlank(volume.getPath(), String.valueOf(volume.getId())));
    }

    private boolean isProtectedVolumeType(VolumeVO volume) {
        return volume != null && (volume.getVolumeType() == Volume.Type.ROOT || volume.getVolumeType() == Volume.Type.DATADISK);
    }

    private long resolveDeviceIdForSort(VolumeVO volume) {
        return volume != null && volume.getDeviceId() != null ? volume.getDeviceId() : Long.MAX_VALUE;
    }

    private String resolveDiskLabel(VolumeVO volume) {
        String volumeType = "volume";
        if (volume.getVolumeType() == Volume.Type.ROOT) {
            volumeType = "root";
        } else if (volume.getVolumeType() == Volume.Type.DATADISK) {
            volumeType = "data";
        } else if (volume.getVolumeType() != null) {
            volumeType = volume.getVolumeType().name().toLowerCase(Locale.ROOT);
        }
        String deviceId = volume.getDeviceId() != null ? String.valueOf(volume.getDeviceId()) : String.valueOf(volume.getId());
        return String.format("%s-%s", volumeType, deviceId);
    }

    private String resolveKvmDiskTarget(Long virtualMachineId, long volumeId) {
        VolumeVO volume = volumeDao.findById(volumeId);
        Long deviceId = volume != null ? volume.getDeviceId() : null;
        if (deviceId == null) {
            return String.valueOf(volumeId);
        }
        String prefix = resolveKvmDiskPrefix(virtualMachineId, volume);
        if (deviceId < 0 || deviceId > 25) {
            return String.format("%s%s", prefix, deviceId);
        }
        return String.format("%s%c", prefix, (char) ('a' + deviceId));
    }

    private String resolveKvmDiskPrefix(Long virtualMachineId, VolumeVO volume) {
        String controller = resolveDiskController(virtualMachineId, volume);
        String normalizedController = StringUtils.defaultString(controller).toLowerCase(Locale.ROOT);
        if (normalizedController.contains("scsi") || normalizedController.contains("sata")) {
            return "sd";
        }
        if (normalizedController.contains("ide")) {
            return "hd";
        }
        return "sd";
    }

    private String resolveDiskController(Long virtualMachineId, VolumeVO volume) {
        if (virtualMachineId == null || volume == null) {
            return null;
        }
        UserVmVO primaryVm = userVmDao.findById(virtualMachineId);
        if (primaryVm == null) {
            return null;
        }
        userVmDao.loadDetails(primaryVm);
        Map<String, String> details = primaryVm.getDetails();
        if (details == null || details.isEmpty()) {
            return "scsi";
        }
        if (volume.getVolumeType() == Volume.Type.ROOT) {
            return details.get(VmDetailConstants.ROOT_DISK_CONTROLLER);
        }
        if (volume.getVolumeType() == Volume.Type.DATADISK) {
            return StringUtils.defaultIfBlank(details.get(VmDetailConstants.DATA_DISK_CONTROLLER),
                    details.get(VmDetailConstants.ROOT_DISK_CONTROLLER));
        }
        return StringUtils.defaultIfBlank(details.get(VmDetailConstants.DATA_DISK_CONTROLLER),
                details.get(VmDetailConstants.ROOT_DISK_CONTROLLER));
    }

    private String resolvePeerHostName(String peerHostId) {
        if (peerHostId == null || peerHostId.isBlank()) {
            return null;
        }
        try {
            return resolveHostName(Long.parseLong(peerHostId));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String resolveHostName(Long hostId) {
        HostVO host = hostId != null ? hostDao.findById(hostId) : null;
        return host != null ? host.getName() : null;
    }

    private String getDetailValue(Long virtualMachineId, String key) {
        if (virtualMachineId == null || StringUtils.isBlank(key)) {
            return null;
        }
        VMInstanceDetailVO detail = vmInstanceDetailsDao.findDetail(virtualMachineId, key);
        return detail != null ? detail.getValue() : null;
    }

    private void populateFtMachineCompatibility(Long virtualMachineId, FtctlProtectionResponse response) {
        if (response == null) {
            return;
        }
        FtMachineTypeCompatibility compatibility = resolveFtMachineTypeCompatibility(virtualMachineId);
        response.setFtMachineType(compatibility.machineType);
        response.setFtMachineCompatible(compatibility.compatible);
        response.setFtMachineCompatibilityMessage(compatibility.message);
    }

    private FtMachineTypeCompatibility resolveFtMachineTypeCompatibility(Long virtualMachineId) {
        String machineType = StringUtils.trimToNull(getDetailValue(virtualMachineId, VmDetailConstants.KVM_GUEST_OS_MACHINE_TYPE));
        if (StringUtils.isBlank(machineType)) {
            return new FtMachineTypeCompatibility(null, false,
                    String.format("FT requires an explicit source VM machine type contract. Create the VM with FT compatibility (%s).",
                            FTCTL_DEFAULT_XCOLO_MACHINE_TYPE));
        }
        String normalizedMachineType = StringUtils.lowerCase(machineType, Locale.ROOT);
        if (StringUtils.startsWith(normalizedMachineType, "pc-i440fx-")) {
            return new FtMachineTypeCompatibility(machineType, true,
                    String.format("FT-compatible machine type: %s", machineType));
        }
        return new FtMachineTypeCompatibility(machineType, false,
                String.format("FT supports only pc-i440fx machine types for this QEMU version. Current configured machine type: %s.",
                        machineType));
    }

    private static final class FtMachineTypeCompatibility {
        private final String machineType;
        private final boolean compatible;
        private final String message;

        private FtMachineTypeCompatibility(String machineType, boolean compatible, String message) {
            this.machineType = machineType;
            this.compatible = compatible;
            this.message = message;
        }
    }

    private void putVmDetail(Long virtualMachineId, String key, String value) {
        if (virtualMachineId == null || StringUtils.isBlank(key)) {
            return;
        }
        Object lock = VM_DETAIL_LOCKS.computeIfAbsent(String.format("%s:%s", virtualMachineId, key), ignored -> new Object());
        synchronized (lock) {
            vmInstanceDetailsDao.removeDetail(virtualMachineId, key);
            vmInstanceDetailsDao.addDetail(virtualMachineId, key, value, true);
        }
    }

    private void putVmDetailIfNotBlank(Long virtualMachineId, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            putVmDetail(virtualMachineId, key, value);
        }
    }

    private void removeVmDetail(Long virtualMachineId, String key) {
        if (virtualMachineId == null || StringUtils.isBlank(key)) {
            return;
        }
        Object lock = VM_DETAIL_LOCKS.computeIfAbsent(String.format("%s:%s", virtualMachineId, key), ignored -> new Object());
        synchronized (lock) {
            vmInstanceDetailsDao.removeDetail(virtualMachineId, key);
        }
    }

    private Long getExecutionHostId(UserVmVO userVm) {
        if (userVm == null) {
            return null;
        }
        return userVm.getHostId() != null ? userVm.getHostId() : userVm.getLastHostId();
    }

    private Long requireExecutionHostId(UserVmVO userVm) {
        Long hostId = getExecutionHostId(userVm);
        if (hostId == null) {
            throw new CloudRuntimeException(String.format("Unable to resolve execution host for VM %s", userVm != null ? userVm.getUuid() : "unknown"));
        }
        return hostId;
    }

    private String resolvePeerUri(Long peerHostId) {
        if (peerHostId == null) {
            return null;
        }
        DetailVO detail = hostDetailsDao.findDetail(peerHostId, HOST_DETAIL_LIBVIRT_URI);
        if (detail != null && detail.getValue() != null && !detail.getValue().isBlank()) {
            return detail.getValue();
        }
        HostVO host = hostDao.findById(peerHostId);
        if (host == null || host.getPrivateIpAddress() == null || host.getPrivateIpAddress().isBlank()) {
            return null;
        }
        return String.format("qemu+ssh://%s/system", host.getPrivateIpAddress());
    }

    private void populateRuntimeStateFromAgent(UserVmVO userVm, FtctlProtectionResponse response, boolean persistState) {
        FtctlStatusAnswer statusAnswer = fetchRuntimeStatus(userVm);
        if (statusAnswer == null) {
            return;
        }
        response.setEnabled("true");
        if (statusAnswer.getMode() != null && !statusAnswer.getMode().isBlank()) {
            response.setMode(statusAnswer.getMode());
        }
        response.setProtectionState(statusAnswer.getProtectionState());
        response.setTransportState(statusAnswer.getTransportState());
        response.setActiveSide(statusAnswer.getActiveSide());
        response.setAdminState(statusAnswer.getAdminState());
        response.setFencingState(statusAnswer.getFencingState());
        response.setLastError(statusAnswer.getLastError());
        if (persistState) {
            persistRuntimeState(userVm, statusAnswer);
        }
    }

    private FtctlStatusAnswer fetchRuntimeStatus(UserVmVO userVm) {
        Long hostId = getExecutionHostId(userVm);
        if (hostId == null || userVm == null) {
            return null;
        }
        try {
            Answer answer = agentManager.send(hostId, new FtctlStatusCommand(userVm.getInstanceName()));
            if (!(answer instanceof FtctlStatusAnswer) || !answer.getResult()) {
                return null;
            }
            return (FtctlStatusAnswer) answer;
        } catch (AgentUnavailableException | OperationTimedoutException ignored) {
            return null;
        }
    }

    private FtctlEventsAnswer fetchRuntimeEvents(UserVmVO userVm, Integer limit) {
        Long hostId = getExecutionHostId(userVm);
        if (hostId == null || userVm == null) {
            return null;
        }
        try {
            Answer answer = agentManager.send(hostId, new FtctlEventsCommand(userVm.getInstanceName(), limit));
            if (!(answer instanceof FtctlEventsAnswer) || !answer.getResult()) {
                return null;
            }
            return (FtctlEventsAnswer) answer;
        } catch (AgentUnavailableException | OperationTimedoutException ignored) {
            return null;
        }
    }

    private List<FtctlEventResponse> fetchRuntimeEventResponses(UserVmVO userVm, Integer limit) {
        FtctlEventsAnswer answer = fetchRuntimeEvents(userVm, limit);
        return answer != null ? parseEvents(answer.getItemsJson()) : Collections.emptyList();
    }

    void syncActiveProtectionRuntimeStates() {
        if (!FtctlServiceEnabled.value() || !FtctlRuntimeStateSyncEnabled.value() ||
                !runtimeStateSyncRunning.compareAndSet(false, true)) {
            return;
        }
        try {
            List<FtctlProtectionVO> protections = ftctlProtectionDao.listActive();
            if (protections == null || protections.isEmpty()) {
                return;
            }
            for (FtctlProtectionVO protection : protections) {
                syncProtectionRuntimeState(protection);
            }
        } catch (RuntimeException e) {
            logger.warn("Unable to sync FTCTL runtime state", e);
        } finally {
            runtimeStateSyncRunning.set(false);
        }
    }

    private void syncProtectionRuntimeState(FtctlProtectionVO protection) {
        if (protection == null) {
            return;
        }
        UserVmVO primaryVm = userVmDao.findById(protection.getPrimaryVmId());
        if (primaryVm == null) {
            return;
        }
        FtctlStatusAnswer statusAnswer = fetchRuntimeStatus(primaryVm);
        if (statusAnswer == null) {
            logger.debug(String.format("Skipping FTCTL runtime sync for VM %s because no agent runtime state was available",
                    primaryVm.getUuid()));
            return;
        }
        persistRuntimeState(primaryVm, statusAnswer);
        try {
            FtctlProtectionVO latestProtection = refreshProtection(primaryVm);
            if (isCloudManagedFailbackReverseReady(latestProtection)) {
                continueCloudManagedFailbackAfterReverseSync(primaryVm, latestProtection,
                        loadCloudManagedFailbackContext(primaryVm, latestProtection), false);
            }
        } catch (RuntimeException e) {
            logger.warn(String.format("Unable to continue cloud-managed FTCTL failback after runtime sync for VM %s",
                    primaryVm.getUuid()), e);
        }
    }

    private boolean persistRuntimeState(UserVmVO userVm, FtctlStatusAnswer statusAnswer) {
        if (userVm == null || statusAnswer == null) {
            return false;
        }
        Long virtualMachineId = userVm.getId();
        if (statusAnswer.getProtectionState() != null) {
            putVmDetail(virtualMachineId, DETAIL_LAST_PROTECTION_STATE, statusAnswer.getProtectionState());
        }
        if (statusAnswer.getTransportState() != null) {
            putVmDetail(virtualMachineId, DETAIL_LAST_TRANSPORT_STATE, statusAnswer.getTransportState());
        }
        if (statusAnswer.getActiveSide() != null) {
            putVmDetail(virtualMachineId, DETAIL_LAST_ACTIVE_SIDE, statusAnswer.getActiveSide());
        }
        if (statusAnswer.getAdminState() != null) {
            putVmDetail(virtualMachineId, DETAIL_LAST_ADMIN_STATE, statusAnswer.getAdminState());
        }
        if (statusAnswer.getFencingState() != null) {
            putVmDetail(virtualMachineId, DETAIL_LAST_FENCING_STATE, statusAnswer.getFencingState());
        }
        if (statusAnswer.getLastError() != null) {
            putVmDetail(virtualMachineId, DETAIL_LAST_ERROR, statusAnswer.getLastError());
        }
        if (statusAnswer.getMode() != null) {
            putVmDetail(virtualMachineId, DETAIL_MODE, statusAnswer.getMode());
        }
        purgeLegacyProgressDetails(virtualMachineId);
        boolean changed = persistProtectionRuntimeState(userVm, statusAnswer);
        if (changed) {
            publishFtctlEvent(userVm, EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE,
                    String.format("Updated FTCTL runtime state for VM %s: protection=%s, transport=%s, activeSide=%s, admin=%s, fencing=%s",
                            userVm.getUuid(), statusAnswer.getProtectionState(), statusAnswer.getTransportState(),
                            statusAnswer.getActiveSide(), statusAnswer.getAdminState(), statusAnswer.getFencingState()));
        }
        return changed;
    }

    private void purgeLegacyProgressDetails(Long virtualMachineId) {
        if (virtualMachineId == null) {
            return;
        }
        for (String key : LEGACY_PROGRESS_DETAIL_KEYS) {
            removeVmDetail(virtualMachineId, key);
        }
    }

    private Integer parseIntegerDetail(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Boolean parseBooleanDetail(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return Boolean.valueOf(value);
    }

    private boolean persistProtectionRuntimeState(UserVmVO userVm, FtctlStatusAnswer statusAnswer) {
        FtctlProtectionVO protection = findActiveProtectionForVm(userVm.getId());
        if (protection == null) {
            return false;
        }
        boolean changed = isProtectionRuntimeStateChanged(protection, statusAnswer);
        if (!changed) {
            return false;
        }
        if (statusAnswer.getMode() != null) {
            protection.setMode(statusAnswer.getMode());
        }
        if (statusAnswer.getAdminState() != null) {
            protection.setAdminState(statusAnswer.getAdminState());
        }
        if (statusAnswer.getProtectionState() != null) {
            protection.setProtectionState(statusAnswer.getProtectionState());
        }
        if (statusAnswer.getTransportState() != null) {
            protection.setTransportState(statusAnswer.getTransportState());
        }
        if (statusAnswer.getActiveSide() != null) {
            protection.setActiveSide(statusAnswer.getActiveSide());
        }
        if (statusAnswer.getFencingState() != null) {
            protection.setFencingState(statusAnswer.getFencingState());
        }
        if (statusAnswer.getLastError() != null) {
            protection.setLastError(statusAnswer.getLastError());
        }
        protection.markUpdated();
        ftctlProtectionDao.update(protection.getId(), protection);
        return true;
    }

    private boolean isProtectionRuntimeStateChanged(FtctlProtectionVO protection, FtctlStatusAnswer statusAnswer) {
        if (protection == null || statusAnswer == null) {
            return false;
        }
        return isChanged(protection.getMode(), statusAnswer.getMode()) ||
                isChanged(protection.getAdminState(), statusAnswer.getAdminState()) ||
                isChanged(protection.getProtectionState(), statusAnswer.getProtectionState()) ||
                isChanged(protection.getTransportState(), statusAnswer.getTransportState()) ||
                isChanged(protection.getActiveSide(), statusAnswer.getActiveSide()) ||
                isChanged(protection.getFencingState(), statusAnswer.getFencingState()) ||
                isChanged(protection.getLastError(), statusAnswer.getLastError());
    }

    private boolean isChanged(String existing, String incoming) {
        return incoming != null && !StringUtils.equals(existing, incoming);
    }

    private String resolveFtctlActionEventType(FtctlActionCommand.Action action) {
        if (action == null) {
            return EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE;
        }
        switch (action) {
            case PROTECT:
                return EventTypes.EVENT_FTCTL_PROTECTION_REGISTER;
            case PAUSE_PROTECTION:
                return EventTypes.EVENT_FTCTL_PROTECTION_PAUSE;
            case RESUME_PROTECTION:
                return EventTypes.EVENT_FTCTL_PROTECTION_RESUME;
            case FAILOVER:
            case FAILOVER_PREPARE:
                return EventTypes.EVENT_FTCTL_PROTECTION_FAILOVER;
            case FAILBACK:
                return EventTypes.EVENT_FTCTL_PROTECTION_FAILBACK;
            case FAILBACK_SYNC:
            case FAILBACK_FINALIZE:
            case FAILBACK_REPROTECT:
                return EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE;
            case UNPROTECT:
                return EventTypes.EVENT_FTCTL_PROTECTION_RELEASE;
            case FENCE_CONFIRM:
                return EventTypes.EVENT_FTCTL_PROTECTION_FENCE_CONFIRM;
            case FENCE_CLEAR:
                return EventTypes.EVENT_FTCTL_PROTECTION_FENCE_CLEAR;
            default:
                return EventTypes.EVENT_FTCTL_PROTECTION_STATE_UPDATE;
        }
    }

    private void publishFtctlEvent(UserVmVO userVm, String eventType, String description) {
        if (userVm == null || StringUtils.isBlank(eventType)) {
            return;
        }
        try {
            ActionEventUtils.onCompletedActionEvent(User.UID_SYSTEM, userVm.getAccountId(), EventVO.LEVEL_INFO, eventType,
                    description, userVm.getId(), ApiCommandResourceType.VirtualMachine.toString(), 0);
        } catch (RuntimeException e) {
            logger.warn(String.format("Unable to publish FTCTL event %s for VM %s", eventType, userVm.getUuid()), e);
        }
    }

    private void syncFtctlContext(UserVmVO userVm, RegisterFtctlProtectionCmd cmd, StoragePoolVO targetStoragePool,
                                  String targetStorageScope, String backendMode, String secondaryTargetDir,
                                  String remoteNbdExportAddr, XcoloPortAllocation xcoloPortAllocation, FtctlIpmiFencingConfig ipmiFencingConfig,
                                  FtctlProtectionProvisioningContext provisioningContext, String remoteDrSshKeyFile) {
        Long localHostId = requireExecutionHostId(userVm);
        HostVO localHost = hostDao.findById(localHostId);
        if (localHost == null) {
            throw new CloudRuntimeException(String.format("Unable to resolve local host for VM %s", userVm.getUuid()));
        }
        boolean remoteMoldDr = isRemoteMoldDr(cmd);
        HostVO peerHost = remoteMoldDr ? null : hostDao.findById(cmd.getPeerHostId());
        if (!remoteMoldDr && peerHost == null) {
            throw new CloudRuntimeException(String.format("Unable to resolve peer host for VM %s", userVm.getUuid()));
        }
        if (remoteMoldDr) {
            validateLocalFtctlHost(localHost, userVm);
        } else {
            validateFtctlHosts(localHost, peerHost, userVm);
        }

        FtctlSyncClusterCommand clusterCommand = new FtctlSyncClusterCommand();
        clusterCommand.setClusterName(buildClusterName(localHost, userVm));
        clusterCommand.setLocalHostId(String.valueOf(localHostId));
        clusterCommand.setLocalRole("primary");
        clusterCommand.setLocalManagementIp(resolveHostField(localHost, HOST_DETAIL_MANAGEMENT_IP, localHost.getPrivateIpAddress()));
        clusterCommand.setLocalLibvirtUri(resolveHostField(localHost, HOST_DETAIL_LIBVIRT_URI,
                String.format("qemu+ssh://%s/system", localHost.getPrivateIpAddress())));
        clusterCommand.setLocalBlockcopyIp(resolveHostField(localHost, HOST_DETAIL_BLOCKCOPY_IP, localHost.getPrivateIpAddress()));
        clusterCommand.setLocalXcoloControlIp(resolveHostField(localHost, HOST_DETAIL_XCOLO_CONTROL_IP, localHost.getPrivateIpAddress()));
        clusterCommand.setLocalXcoloDataIp(resolveHostField(localHost, HOST_DETAIL_XCOLO_DATA_IP, localHost.getPrivateIpAddress()));
        String peerLibvirtUri;
        if (remoteMoldDr) {
            String peerAddress = cmd.getRemotePeerHostAddress();
            String peerBlockcopyAddress = StringUtils.defaultIfBlank(cmd.getRemotePeerHostBlockcopyAddress(), peerAddress);
            peerLibvirtUri = resolveRemotePeerLibvirtUri(cmd);
            clusterCommand.setPeerHostId(cmd.getRemotePeerHostUuid());
            clusterCommand.setPeerRole("secondary");
            clusterCommand.setPeerManagementIp(peerAddress);
            clusterCommand.setPeerLibvirtUri(peerLibvirtUri);
            clusterCommand.setPeerBlockcopyIp(peerBlockcopyAddress);
            clusterCommand.setPeerXcoloControlIp(peerAddress);
            clusterCommand.setPeerXcoloDataIp(peerBlockcopyAddress);
        } else {
            clusterCommand.setPeerHostId(String.valueOf(cmd.getPeerHostId()));
            clusterCommand.setPeerRole("secondary");
            clusterCommand.setPeerManagementIp(resolveHostField(peerHost, HOST_DETAIL_MANAGEMENT_IP, peerHost.getPrivateIpAddress()));
            peerLibvirtUri = resolveHostField(peerHost, HOST_DETAIL_LIBVIRT_URI,
                    String.format("qemu+ssh://%s/system", peerHost.getPrivateIpAddress()));
            clusterCommand.setPeerLibvirtUri(peerLibvirtUri);
            clusterCommand.setPeerBlockcopyIp(resolveHostField(peerHost, HOST_DETAIL_BLOCKCOPY_IP, peerHost.getPrivateIpAddress()));
            clusterCommand.setPeerXcoloControlIp(resolveHostField(peerHost, HOST_DETAIL_XCOLO_CONTROL_IP, peerHost.getPrivateIpAddress()));
            clusterCommand.setPeerXcoloDataIp(resolveHostField(peerHost, HOST_DETAIL_XCOLO_DATA_IP, peerHost.getPrivateIpAddress()));
        }

        FtctlSyncProfileCommand profileCommand = new FtctlSyncProfileCommand(userVm.getInstanceName(), cmd.getMode(), peerLibvirtUri);
        profileCommand.setProfileName(userVm.getUuid());
        profileCommand.setBackendMode(backendMode);
        profileCommand.setProvisioningBackend(provisioningContext.getProvisioningBackend());
        profileCommand.setProvisioningState(provisioningContext.getProvisioningState());
        profileCommand.setTargetStorageScope(targetStorageScope);
        if (targetStoragePool != null) {
            profileCommand.setTargetStoragePoolId(targetStoragePool.getUuid());
            profileCommand.setTargetStoragePoolName(targetStoragePool.getName());
            profileCommand.setTargetStoragePoolPath(targetStoragePool.getPath());
            if (targetStoragePool.getPoolType() != null) {
                profileCommand.setTargetStoragePoolType(targetStoragePool.getPoolType().name());
            }
        } else if (remoteMoldDr) {
            profileCommand.setTargetStoragePoolId(cmd.getRemoteTargetStoragePoolUuid());
            profileCommand.setTargetStoragePoolName(cmd.getRemoteTargetStoragePoolName());
            profileCommand.setTargetStoragePoolPath(cmd.getRemoteTargetStoragePoolPath());
            profileCommand.setTargetStoragePoolType(cmd.getRemoteTargetStoragePoolType());
        }
        String diskMap = provisioningContext.getDiskMap();
        validateCloudManagedDiskMap(provisioningContext, backendMode, diskMap);
        profileCommand.setDiskMap(diskMap);
        profileCommand.setSecondaryVmName(provisioningContext.getSecondaryVmName());
        profileCommand.setFencingPolicy(cmd.getFencingPolicy());
        profileCommand.setSecondaryTargetDir(secondaryTargetDir);
        profileCommand.setSecondarySshKeyFile(remoteDrSshKeyFile);
        profileCommand.setRemoteNbdExportAddr(remoteNbdExportAddr);
        if (xcoloPortAllocation != null && xcoloPortAllocation.enabled) {
            profileCommand.setXcoloProxyEndpoint(xcoloPortAllocation.proxyEndpoint);
            profileCommand.setXcoloNbdEndpoint(xcoloPortAllocation.nbdEndpoint);
            profileCommand.setXcoloMigrateUri(xcoloPortAllocation.migrateUri);
            profileCommand.setXcoloMirrorPort(String.valueOf(xcoloPortAllocation.mirrorPort));
            profileCommand.setXcoloComparePort(String.valueOf(xcoloPortAllocation.comparePort));
            profileCommand.setXcoloCompareLocalPort(String.valueOf(xcoloPortAllocation.compareLocalPort));
            profileCommand.setXcoloCompareOutPort(String.valueOf(xcoloPortAllocation.compareOutPort));
            profileCommand.setXcoloControlPort(String.valueOf(xcoloPortAllocation.controlPort));
        }
        if (ipmiFencingConfig != null) {
            ipmiFencingConfig.applyTo(profileCommand);
        }

        executeSyncCommand(localHostId, clusterCommand, "cluster");
        executeSyncCommand(localHostId, profileCommand, "profile");
    }

    private void validateCloudManagedDiskMap(FtctlProtectionProvisioningContext provisioningContext, String backendMode, String diskMap) {
        if (provisioningContext == null ||
                !FtctlProtectionProvisioningService.BACKEND_CLOUD_MANAGED.equalsIgnoreCase(provisioningContext.getProvisioningBackend())) {
            return;
        }
        if (StringUtils.isBlank(diskMap) || "auto".equalsIgnoreCase(StringUtils.trim(diskMap))) {
            throw new CloudRuntimeException("FTCTL cloud-managed provisioning requires an explicit disk map");
        }
        String[] entries = StringUtils.split(diskMap, ';');
        if (entries == null || entries.length == 0) {
            throw new CloudRuntimeException("FTCTL cloud-managed provisioning resolved an empty disk map");
        }
        for (String entry : entries) {
            String[] parts = StringUtils.split(entry, "=", 2);
            if (parts == null || parts.length != 2 || StringUtils.isAnyBlank(parts[0], parts[1])) {
                throw new CloudRuntimeException(String.format("FTCTL cloud-managed provisioning resolved an invalid disk map entry: %s", entry));
            }
            if ("remote-nbd".equalsIgnoreCase(backendMode) && !StringUtils.startsWith(parts[1], "/")) {
                throw new CloudRuntimeException(String.format("FTCTL cloud-managed remote-nbd target %s must use an absolute Cloud-managed path: %s", parts[0], parts[1]));
            }
        }
    }

    private void executeSyncCommand(Long hostId, com.cloud.agent.api.Command command, String syncType) {
        try {
            sendFtctlSyncWithLockRetry(hostId, command, syncType);
        } catch (AgentUnavailableException | OperationTimedoutException e) {
            throw new CloudRuntimeException(String.format("Unable to execute FTCTL %s sync on host %s", syncType, hostId), e);
        }
    }

    private FtctlSyncAnswer sendFtctlSyncWithLockRetry(Long hostId, com.cloud.agent.api.Command command, String syncType)
            throws AgentUnavailableException, OperationTimedoutException {
        long deadline = System.currentTimeMillis() + FTCTL_ACTION_LOCK_RETRY_TIMEOUT_MILLIS;
        int attempt = 0;
        while (true) {
            attempt++;
            Answer answer = agentManager.send(hostId, command);
            if (!(answer instanceof FtctlSyncAnswer)) {
                throw new CloudRuntimeException(String.format("Unexpected FTCTL %s sync answer type on host %s", syncType, hostId));
            }
            FtctlSyncAnswer syncAnswer = (FtctlSyncAnswer) answer;
            if (syncAnswer.getResult()) {
                if (attempt > 1) {
                    logger.info(String.format("FTCTL %s sync on host %s succeeded after %s lock retry attempt(s)",
                            syncType, hostId, attempt - 1));
                }
                return syncAnswer;
            }
            if (!isRetryableFtctlLock(syncAnswer)) {
                throw new CloudRuntimeException(String.format("FTCTL %s sync failed on host %s: %s",
                        syncType, hostId, syncAnswer.getDetails()));
            }
            if (System.currentTimeMillis() >= deadline) {
                throw new CloudRuntimeException(String.format("FTCTL %s sync on host %s remained locked after %s ms: %s",
                        syncType, hostId, FTCTL_ACTION_LOCK_RETRY_TIMEOUT_MILLIS, syncAnswer.getDetails()));
            }
            logger.info(String.format("FTCTL %s sync on host %s is locked by another ftctl process; retrying in %s ms",
                    syncType, hostId, FTCTL_ACTION_LOCK_RETRY_INTERVAL_MILLIS));
            sleepBeforeFtctlLockRetry(syncType, hostId);
        }
    }

    private void sleepBeforeFtctlLockRetry(String syncType, Long hostId) {
        try {
            Thread.sleep(FTCTL_ACTION_LOCK_RETRY_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CloudRuntimeException(String.format("Interrupted while waiting to retry FTCTL %s sync on host %s",
                    syncType, hostId), e);
        }
    }

    private boolean isRetryableFtctlLock(FtctlSyncAnswer syncAnswer) {
        if (syncAnswer == null) {
            return false;
        }
        if (Integer.valueOf(FTCTL_ACTION_LOCK_EXIT_CODE).equals(syncAnswer.getExitCode())) {
            return true;
        }
        if ("locked".equalsIgnoreCase(StringUtils.trimToEmpty(syncAnswer.getFtctlResult()))) {
            return true;
        }
        String details = StringUtils.defaultString(syncAnswer.getDetails());
        String output = StringUtils.defaultString(syncAnswer.getOutput());
        return details.contains("\"result\":\"locked\"") || output.contains("\"result\":\"locked\"");
    }

    private String buildClusterName(HostVO localHost, UserVmVO userVm) {
        if (localHost != null && localHost.getClusterId() > 0) {
            return String.format("cluster-%s", localHost.getClusterId());
        }
        if (userVm.getDataCenterId() > 0) {
            return String.format("zone-%s", userVm.getDataCenterId());
        }
        return "ftctl-cluster";
    }

    private String resolveHostField(HostVO host, String key, String fallback) {
        if (host == null) {
            return fallback;
        }
        DetailVO detail = hostDetailsDao.findDetail(host.getId(), key);
        if (detail != null && detail.getValue() != null && !detail.getValue().isBlank()) {
            return detail.getValue();
        }
        return fallback;
    }

    private void validateFtctlHosts(HostVO localHost, HostVO peerHost, UserVmVO userVm) {
        if (localHost.getId() == peerHost.getId()) {
            throw new CloudRuntimeException(String.format("FTCTL peer host must differ from execution host for VM %s", userVm.getUuid()));
        }
        validateLocalFtctlHost(localHost, userVm);
        if (localHost.getType() != Host.Type.Routing || peerHost.getType() != Host.Type.Routing) {
            throw new CloudRuntimeException(String.format("FTCTL requires routing hosts for VM %s", userVm.getUuid()));
        }
        if (!"KVM".equalsIgnoreCase(String.valueOf(peerHost.getHypervisorType()))) {
            throw new CloudRuntimeException(String.format("FTCTL requires KVM hosts for VM %s", userVm.getUuid()));
        }
    }

    private void validateLocalFtctlHost(HostVO localHost, UserVmVO userVm) {
        if (localHost.getType() != Host.Type.Routing) {
            throw new CloudRuntimeException(String.format("FTCTL requires a routing execution host for VM %s", userVm.getUuid()));
        }
        if (!"KVM".equalsIgnoreCase(String.valueOf(localHost.getHypervisorType()))) {
            throw new CloudRuntimeException(String.format("FTCTL requires a KVM execution host for VM %s", userVm.getUuid()));
        }
    }

    private FtctlIpmiFencingConfig resolveIpmiFencingConfig(UserVmVO userVm, RegisterFtctlProtectionCmd cmd) {
        if (!FENCING_POLICY_IPMI.equalsIgnoreCase(StringUtils.trimToEmpty(cmd.getFencingPolicy()))) {
            return null;
        }
        Long localHostId = requireExecutionHostId(userVm);
        FtctlIpmiEndpoint primary = resolveIpmiEndpoint(localHostId, "primary");
        FtctlIpmiEndpoint secondary;
        if (isRemoteMoldDr(cmd)) {
            secondary = unknownIpmiEndpoint();
        } else {
            Long peerHostId = cmd.getPeerHostId();
            if (peerHostId == null) {
                throw new CloudRuntimeException("FTCTL IPMI fencing requires a peer host");
            }
            secondary = resolveIpmiEndpoint(peerHostId, "peer");
        }
        return new FtctlIpmiFencingConfig(primary, secondary);
    }

    private FtctlIpmiEndpoint resolveIpmiEndpoint(Long hostId, String role) {
        OutOfBandManagement oobm = hostId == null ? null : outOfBandManagementDao.findByHost(hostId);
        if (oobm == null) {
            logger.warn(String.format("FTCTL IPMI fencing has no OOBM configuration on %s host %s; recording IPMI UNKNOWN", role, hostId));
            return unknownIpmiEndpoint();
        }
        if (!oobm.isEnabled()) {
            logger.warn(String.format("FTCTL IPMI fencing has disabled OOBM on %s host %s; recording IPMI UNKNOWN", role, hostId));
            return unknownIpmiEndpoint();
        }
        if (!OOBM_DRIVER_IPMITOOL.equalsIgnoreCase(StringUtils.trimToEmpty(oobm.getDriver()))) {
            logger.warn(String.format("FTCTL IPMI fencing requires ipmitool OOBM driver on %s host %s; recording IPMI UNKNOWN", role, hostId));
            return unknownIpmiEndpoint();
        }
        if (StringUtils.isAnyBlank(oobm.getAddress(), oobm.getUsername(), oobm.getPassword())) {
            logger.warn(String.format("FTCTL IPMI fencing has incomplete OOBM address, username, or password on %s host %s; recording IPMI UNKNOWN",
                    role, hostId));
            return unknownIpmiEndpoint();
        }
        return new FtctlIpmiEndpoint(
                oobm.getAddress(),
                StringUtils.trimToNull(oobm.getPort()),
                oobm.getUsername(),
                oobm.getPassword(),
                DEFAULT_IPMI_INTERFACE);
    }

    private FtctlIpmiEndpoint unknownIpmiEndpoint() {
        return new FtctlIpmiEndpoint("UNKNOWN", null, "", "", DEFAULT_IPMI_INTERFACE);
    }

    private void persistIpmiFencingDetails(Long virtualMachineId, FtctlIpmiFencingConfig config) {
        putVmDetail(virtualMachineId, DETAIL_FENCING_IPMI_PRIMARY_HOST, config.primary.host);
        putVmDetail(virtualMachineId, DETAIL_FENCING_IPMI_SECONDARY_HOST, config.secondary.host);
        if (config.primary.port != null) {
            putVmDetail(virtualMachineId, DETAIL_FENCING_IPMI_PRIMARY_PORT, config.primary.port);
        }
        if (config.secondary.port != null) {
            putVmDetail(virtualMachineId, DETAIL_FENCING_IPMI_SECONDARY_PORT, config.secondary.port);
        }
        putVmDetail(virtualMachineId, DETAIL_FENCING_IPMI_PRIMARY_USER, config.primary.user);
        putVmDetail(virtualMachineId, DETAIL_FENCING_IPMI_SECONDARY_USER, config.secondary.user);
        putVmDetail(virtualMachineId, DETAIL_FENCING_IPMI_PRIMARY_INTERFACE, config.primary.ipmiInterface);
        putVmDetail(virtualMachineId, DETAIL_FENCING_IPMI_SECONDARY_INTERFACE, config.secondary.ipmiInterface);
    }

    private static class FtctlIpmiFencingConfig {
        private final FtctlIpmiEndpoint primary;
        private final FtctlIpmiEndpoint secondary;

        private FtctlIpmiFencingConfig(FtctlIpmiEndpoint primary, FtctlIpmiEndpoint secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        private void applyTo(FtctlSyncProfileCommand command) {
            command.setFencingIpmiPrimaryHost(primary.host);
            command.setFencingIpmiPrimaryPort(primary.port);
            command.setFencingIpmiPrimaryUser(primary.user);
            command.setFencingIpmiPrimaryPassword(primary.password);
            command.setFencingIpmiPrimaryInterface(primary.ipmiInterface);
            command.setFencingIpmiSecondaryHost(secondary.host);
            command.setFencingIpmiSecondaryPort(secondary.port);
            command.setFencingIpmiSecondaryUser(secondary.user);
            command.setFencingIpmiSecondaryPassword(secondary.password);
            command.setFencingIpmiSecondaryInterface(secondary.ipmiInterface);
        }
    }

    private static class FtctlIpmiEndpoint {
        private final String host;
        private final String port;
        private final String user;
        private final String password;
        private final String ipmiInterface;

        private FtctlIpmiEndpoint(String host, String port, String user, String password, String ipmiInterface) {
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
            this.ipmiInterface = ipmiInterface;
        }
    }

    private static final class RemoteMoldCredentials {
        private final String apiUrl;
        private final String apiKey;
        private final String secretKey;

        private RemoteMoldCredentials(String apiUrl, String apiKey, String secretKey) {
            this.apiUrl = apiUrl;
            this.apiKey = apiKey;
            this.secretKey = secretKey;
        }
    }

    private static final class FailbackTargetContext {
        private final String targetMoldType;
        private final RemoteMoldCredentials remoteMoldCredentials;
        private final RemoteMoldCredentials targetMoldCredentials;

        private FailbackTargetContext(String targetMoldType, RemoteMoldCredentials remoteMoldCredentials,
                                      RemoteMoldCredentials targetMoldCredentials) {
            this.targetMoldType = targetMoldType;
            this.remoteMoldCredentials = remoteMoldCredentials;
            this.targetMoldCredentials = targetMoldCredentials;
        }
    }

    private static final class FailbackOperationContext {
        private final String sessionUuid;
        private final FailbackTargetContext targetContext;
        private final long createdAtMillis;
        private final long expiresAtMillis;
        private volatile long lastAccessMillis;

        private FailbackOperationContext(FailbackTargetContext targetContext) {
            long now = System.currentTimeMillis();
            this.sessionUuid = UUID.randomUUID().toString();
            this.targetContext = targetContext;
            this.createdAtMillis = now;
            this.lastAccessMillis = now;
            this.expiresAtMillis = now + CLOUD_MANAGED_FAILBACK_CONTEXT_TTL_MILLIS;
        }

        private void touch() {
            this.lastAccessMillis = System.currentTimeMillis();
        }

        private boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }

    private static final class RemoteReplicaResources {
        private String vmId;
        private String name;
        private String instanceName;
        private String state;
        private String hostId;
        private String hostName;
        private String stateUpdated;
        private String diskMap;
        private final List<RemoteReplicaVolume> volumes = new ArrayList<>();
    }

    private static final class RemoteReplicaVolume {
        private String id;
        private String name;
        private String path;
        private String state;
        private String diskLabel;
        private String sourceVolumeId;
        private String sourceDiskTarget;
    }

    private List<FtctlEventResponse> parseEvents(String itemsJson) {
        List<FtctlEventResponse> events = new ArrayList<>();
        if (itemsJson == null || itemsJson.isBlank()) {
            return events;
        }
        try {
            JsonElement element = JsonParser.parseString(itemsJson);
            if (!element.isJsonArray()) {
                return events;
            }
            JsonArray array = element.getAsJsonArray();
            for (JsonElement entry : array) {
                if (!entry.isJsonObject()) {
                    continue;
                }
                JsonObject object = entry.getAsJsonObject();
                FtctlEventResponse response = new FtctlEventResponse();
                response.setObjectName("ftctlevent");
                response.setTimestamp(getJsonString(object, "ts"));
                response.setScanId(getJsonString(object, "scan_id"));
                response.setVmName(getJsonString(object, "vm"));
                response.setStage(getJsonString(object, "stage"));
                response.setEvent(getJsonString(object, "event"));
                response.setResult(getJsonString(object, "result"));
                response.setRc(getJsonInteger(object, "rc"));
                response.setDetails(object.has("details") && object.get("details").isJsonObject()
                        ? object.getAsJsonObject("details").toString() : null);
                events.add(response);
            }
        } catch (RuntimeException ignored) {
            // Keep empty list when events payload cannot be parsed.
        }
        return events;
    }

    private JsonObject findLatestProgress(List<FtctlEventResponse> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        for (int i = events.size() - 1; i >= 0; i--) {
            FtctlEventResponse event = events.get(i);
            if (!isProgressEvent(event)) {
                continue;
            }
            JsonObject progress = parseProgressDetails(event);
            if (progress != null && progress.has("percent")) {
                return progress;
            }
        }
        return null;
    }

    private FtctlEventResponse findLatestEvent(List<FtctlEventResponse> events, String eventName) {
        if (events == null || events.isEmpty() || StringUtils.isBlank(eventName)) {
            return null;
        }
        String normalizedEventName = StringUtils.lowerCase(eventName, Locale.ROOT);
        for (int i = events.size() - 1; i >= 0; i--) {
            FtctlEventResponse event = events.get(i);
            if (event != null && normalizedEventName.equals(StringUtils.lowerCase(event.getEvent(), Locale.ROOT))) {
                return event;
            }
        }
        return null;
    }

    private boolean isProgressEvent(FtctlEventResponse event) {
        if (event == null || StringUtils.isBlank(event.getEvent())) {
            return false;
        }
        String eventName = StringUtils.lowerCase(event.getEvent(), Locale.ROOT);
        return "blockcopy.progress".equals(eventName) || "reverse_sync.progress".equals(eventName);
    }

    private JsonObject parseProgressDetails(FtctlEventResponse event) {
        JsonObject details = parseJsonObject(event.getDetails());
        if (details == null) {
            return null;
        }
        JsonObject progress = details.deepCopy();
        if (!progress.has("direction") || progress.get("direction").isJsonNull()) {
            progress.addProperty("direction", "reverse_sync.progress".equals(StringUtils.lowerCase(event.getEvent(), Locale.ROOT))
                    ? "reverse" : "forward");
        }
        if ((!progress.has("updated") || progress.get("updated").isJsonNull()) && StringUtils.isNotBlank(event.getTimestamp())) {
            progress.addProperty("updated", event.getTimestamp());
        }
        if ((!progress.has("stage") || progress.get("stage").isJsonNull()) && StringUtils.isNotBlank(event.getStage())) {
            progress.addProperty("stage", event.getStage());
        }
        return progress;
    }

    private JsonObject parseJsonObject(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private JsonObject parseFirstJsonObject(String output) {
        if (StringUtils.isBlank(output)) {
            return null;
        }
        String trimmed = output.trim();
        JsonObject object = parseJsonObject(trimmed);
        if (object != null) {
            return object;
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return parseJsonObject(trimmed.substring(start, end + 1));
        }
        return null;
    }

    private void applyLatestProgress(FtctlEventsResponse response, JsonObject latestProgress) {
        if (response == null || latestProgress == null) {
            return;
        }
        response.setLatestProgress(latestProgress.toString());
        response.setSyncProgressJson(latestProgress.toString());
        response.setSyncProgressPercent(getJsonDouble(latestProgress, "percent"));
        response.setSyncCopiedBytes(getJsonLong(latestProgress, "copied_bytes"));
        response.setSyncTotalBytes(getJsonLong(latestProgress, "total_bytes"));
        response.setSyncReady(getJsonBoolean(latestProgress, "ready"));
        response.setSyncDirection(getJsonString(latestProgress, "direction"));
        response.setSyncUpdated(getJsonString(latestProgress, "updated"));
    }

    private String getJsonString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return object.get(key).getAsString();
    }

    private Integer getJsonInteger(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Integer getJsonIntegerOrDefault(JsonObject object, String key, Integer defaultValue) {
        Integer value = getJsonInteger(object, key);
        return value != null ? value : defaultValue;
    }

    private Long getJsonLong(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsLong();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Double getJsonDouble(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsDouble();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Boolean getJsonBoolean(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Boolean getJsonBooleanOrDefault(JsonObject object, String key, Boolean defaultValue) {
        Boolean value = getJsonBoolean(object, key);
        return value != null ? value : defaultValue;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        if (!FtctlServiceEnabled.value()) {
            return cmdList;
        }
        cmdList.add(GetFtctlProtectionCmd.class);
        cmdList.add(RegisterFtctlProtectionCmd.class);
        cmdList.add(ValidateFtctlRemoteMoldConnectionCmd.class);
        cmdList.add(ListFtctlRemoteMoldHostsCmd.class);
        cmdList.add(ListFtctlRemoteMoldNetworksCmd.class);
        cmdList.add(ListFtctlRemoteMoldStoragePoolsCmd.class);
        cmdList.add(PrepareFtctlDrReplicaResourcesCmd.class);
        cmdList.add(PrepareFtctlDrRemoteSshAccessCmd.class);
        cmdList.add(InstallFtctlDrRemoteSshKeyCmd.class);
        cmdList.add(GetFtctlCheckCmd.class);
        cmdList.add(GetFtctlEventsCmd.class);
        cmdList.add(GetFtctlHealthCmd.class);
        cmdList.add(ExecuteFtctlDrSiteAgentCommandCmd.class);
        cmdList.add(org.apache.cloudstack.api.command.admin.ftctl.PauseFtctlProtectionCmd.class);
        cmdList.add(org.apache.cloudstack.api.command.admin.ftctl.ResumeFtctlProtectionCmd.class);
        cmdList.add(org.apache.cloudstack.api.command.admin.ftctl.FailoverFtctlProtectionCmd.class);
        cmdList.add(org.apache.cloudstack.api.command.admin.ftctl.FailbackFtctlProtectionCmd.class);
        cmdList.add(org.apache.cloudstack.api.command.admin.ftctl.ConfirmFtctlFenceCmd.class);
        cmdList.add(org.apache.cloudstack.api.command.admin.ftctl.ClearFtctlFenceCmd.class);
        cmdList.add(ReleaseFtctlProtectionCmd.class);
        cmdList.add(ReleaseFtctlDrReplicaProtectionCmd.class);
        cmdList.add(AdoptFtctlDrReplicaCmd.class);
        cmdList.add(FailbackFtctlDrReplicaCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return FtctlService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                FtctlServiceEnabled,
                FtctlRuntimeStateSyncEnabled,
                FtctlRuntimeStateSyncInterval,
                FtctlCloudManagedFailoverMonitorEnabled,
                FtctlCloudManagedFailoverMonitorInterval,
                FtctlCloudManagedFailoverConfirmations,
                FtctlCloudManagedFailoverStartEnabled
        };
    }

    private static final class CloudManagedFailoverSignal {
        private final boolean candidate;
        private final String reason;

        private CloudManagedFailoverSignal(boolean candidate, String reason) {
            this.candidate = candidate;
            this.reason = reason;
        }
    }

    private static final class CloudManagedFencingDecision {
        private final boolean fenced;
        private final String result;
        private final String reason;

        private CloudManagedFencingDecision(boolean fenced, String result, String reason) {
            this.fenced = fenced;
            this.result = result;
            this.reason = reason;
        }
    }

    private static final class FtctlActionLockedException extends CloudRuntimeException {
        private final FtctlActionAnswer actionAnswer;

        private FtctlActionLockedException(String message, FtctlActionAnswer actionAnswer) {
            super(message);
            this.actionAnswer = actionAnswer;
        }

        private FtctlActionAnswer getActionAnswer() {
            return actionAnswer;
        }
    }
}
