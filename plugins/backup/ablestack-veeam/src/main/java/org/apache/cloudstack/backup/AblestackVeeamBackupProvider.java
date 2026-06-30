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

package org.apache.cloudstack.backup;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.backup.ablestackveeam.AblestackVeeamClient;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;

import static org.apache.cloudstack.backup.BackupManager.BackupFrameworkEnabled;

/**
 * Ablestack Veeam backup provider for KVM: seeds from Veeam restore points, then incremental NAS backups.
 */
public class AblestackVeeamBackupProvider extends AdapterBase implements BackupProvider, Configurable {

    public static final String PROVIDER_NAME = "ablestack-veeam";
    public static final String DETAIL_VEEAM_RESTORE_POINT_ID = "ablestack.veeam.restore.point.id";
    public static final String DETAIL_VEEAM_VM_NAME = "ablestack.veeam.vm.name";

    public ConfigKey<String> AblestackVeeamUrl = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.ablestack-veeam.url", "https://localhost:9398/api/",
            "The Ablestack Veeam B&R REST API URL.", true, ConfigKey.Scope.Zone, BackupFrameworkEnabled.key());

    public ConfigKey<Integer> AblestackVeeamVersion = new ConfigKey<>("Advanced", Integer.class,
            "backup.plugin.ablestack-veeam.version", "0",
            "Veeam server major version (0 = auto-detect via PowerShell).", true, ConfigKey.Scope.Zone, BackupFrameworkEnabled.key());

    private ConfigKey<String> AblestackVeeamUsername = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.ablestack-veeam.username", "administrator",
            "Veeam B&R username for Ablestack Veeam plugin.", true, ConfigKey.Scope.Zone, BackupFrameworkEnabled.key());

    private ConfigKey<String> AblestackVeeamPassword = new ConfigKey<>("Secure", String.class,
            "backup.plugin.ablestack-veeam.password", "",
            "Veeam B&R password for Ablestack Veeam plugin.", true, ConfigKey.Scope.Zone, BackupFrameworkEnabled.key());

    private ConfigKey<Boolean> AblestackVeeamValidateSsl = new ConfigKey<>("Advanced", Boolean.class,
            "backup.plugin.ablestack-veeam.validate.ssl", "false",
            "Validate SSL when connecting to Veeam REST API.", true, ConfigKey.Scope.Zone, BackupFrameworkEnabled.key());

    private ConfigKey<Integer> AblestackVeeamApiTimeout = new ConfigKey<>("Advanced", Integer.class,
            "backup.plugin.ablestack-veeam.request.timeout", "300",
            "Veeam API request timeout in seconds.", true, ConfigKey.Scope.Zone, BackupFrameworkEnabled.key());

    private ConfigKey<Integer> AblestackVeeamRestoreTimeout = new ConfigKey<>("Advanced", Integer.class,
            "backup.plugin.ablestack-veeam.restore.timeout", "3600",
            "Veeam export/restore operation timeout in seconds.", true, ConfigKey.Scope.Zone, BackupFrameworkEnabled.key());

    private ConfigKey<Integer> AblestackVeeamTaskPollInterval = new ConfigKey<>("Advanced", Integer.class,
            "backup.plugin.ablestack-veeam.task.poll.interval", "5",
            "Veeam task poll interval in seconds.", true, ConfigKey.Scope.Zone, BackupFrameworkEnabled.key());

    private ConfigKey<Integer> AblestackVeeamTaskPollMaxRetry = new ConfigKey<>("Advanced", Integer.class,
            "backup.plugin.ablestack-veeam.task.poll.max.retry", "240",
            "Max retries when polling Veeam tasks.", true, ConfigKey.Scope.Zone, BackupFrameworkEnabled.key());

    public ConfigKey<String> AblestackVeeamStagingPath = new ConfigKey<>("Advanced", String.class,
            "backup.plugin.ablestack-veeam.staging.path", "/var/ablestack-veeam-staging",
            "Shared staging directory on the Veeam server (must be visible to KVM hosts) for disk export before NAS seed import.",
            true, ConfigKey.Scope.Zone, BackupFrameworkEnabled.key());

    @Inject
    private AblestackNasBackupProvider nasBackupProvider;

    @Inject
    private BackupManager backupManager;

    protected AblestackVeeamClient getClient(final Long zoneId) {
        try {
            return new AblestackVeeamClient(
                    AblestackVeeamUrl.valueIn(zoneId),
                    AblestackVeeamVersion.valueIn(zoneId),
                    AblestackVeeamUsername.valueIn(zoneId),
                    AblestackVeeamPassword.valueIn(zoneId),
                    AblestackVeeamValidateSsl.valueIn(zoneId),
                    AblestackVeeamApiTimeout.valueIn(zoneId),
                    AblestackVeeamRestoreTimeout.valueIn(zoneId),
                    AblestackVeeamTaskPollInterval.valueIn(zoneId),
                    AblestackVeeamTaskPollMaxRetry.valueIn(zoneId));
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException("Failed to parse Ablestack Veeam API URL: " + e.getMessage());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new CloudRuntimeException("Failed to build Ablestack Veeam client: " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }

    @Override
    public String getDescription() {
        return "Ablestack Veeam + NAS KVM Backup Plugin";
    }

    @Override
    public List<BackupOffering> listBackupOfferings(Long zoneId) {
        return nasBackupProvider.listBackupOfferings(zoneId);
    }

    @Override
    public boolean isValidProviderOffering(Long zoneId, String uuid) {
        return nasBackupProvider.isValidProviderOffering(zoneId, uuid);
    }

    @Override
    public boolean assignVMToBackupOffering(VirtualMachine vm, BackupOffering backupOffering) {
        if (!Hypervisor.HypervisorType.KVM.equals(vm.getHypervisorType())) {
            throw new CloudRuntimeException("Ablestack Veeam backup provider supports KVM instances only");
        }
        return true;
    }

    @Override
    public boolean removeVMFromBackupOffering(VirtualMachine vm) {
        return true;
    }

    @Override
    public boolean willDeleteBackupsOnOfferingRemoval() {
        return false;
    }

    @Override
    public Pair<Boolean, Backup> takeBackup(VirtualMachine vm, Boolean quiesceVM) {
        if (!nasBackupProvider.hasBackedUpSeed(vm)) {
            final String restorePointId = resolveRestorePointId(vm);
            final String stagingSubDir = String.format("%s/%s", vm.getInstanceName(), System.currentTimeMillis());
            final String stagingPath = String.format("%s/%s", AblestackVeeamStagingPath.valueIn(vm.getDataCenterId()), stagingSubDir);
            logger.info("No NAS seed found for VM [{}], exporting Veeam restore point [{}] to [{}]",
                    vm.getInstanceName(), restorePointId, stagingPath);
            final List<String> stagingDisks = getClient(vm.getDataCenterId())
                    .exportRestorePointDisksToStaging(restorePointId, stagingPath);
            final Pair<Boolean, Backup> seedResult = nasBackupProvider.importVeeamBackupSeed(
                    vm, stagingDisks, restorePointId, "vmdk", true);
            if (!seedResult.first()) {
                return seedResult;
            }
            tagBackupAsVeeamSourced(seedResult.second(), restorePointId, vm);
            return seedResult;
        }
        final Pair<Boolean, Backup> result = nasBackupProvider.takeBackup(vm, quiesceVM);
        if (result.second() != null) {
            tagBackupAsVeeamSourced(result.second(), getVmRestorePointDetail(vm), vm);
        }
        return result;
    }

    @Override
    public Pair<Boolean, Backup> takeBackup(VirtualMachine vm, Boolean quiesceVM, Long backupScheduleId) {
        return takeBackup(vm, quiesceVM);
    }

    @Override
    public Pair<Boolean, Backup> importAblestackVeeamBackupSeed(VirtualMachine vm, String veeamRestorePointId,
            List<String> stagingDiskPaths, String sourceFormat, Boolean bootstrapCheckpoint) {
        List<String> staging = stagingDiskPaths;
        if (CollectionUtils.isEmpty(staging)) {
            if (StringUtils.isBlank(veeamRestorePointId)) {
                throw new CloudRuntimeException("Either veeam restore point id or staging disk paths are required");
            }
            final String stagingSubDir = String.format("%s/%s-import", vm.getInstanceName(), System.currentTimeMillis());
            final String stagingPath = String.format("%s/%s", AblestackVeeamStagingPath.valueIn(vm.getDataCenterId()), stagingSubDir);
            staging = getClient(vm.getDataCenterId()).exportRestorePointDisksToStaging(veeamRestorePointId, stagingPath);
        }
        final Pair<Boolean, Backup> result = nasBackupProvider.importVeeamBackupSeed(
                vm, staging, veeamRestorePointId, sourceFormat, bootstrapCheckpoint);
        if (result.second() != null) {
            tagBackupAsVeeamSourced(result.second(), veeamRestorePointId, vm);
        }
        return result;
    }

    private void tagBackupAsVeeamSourced(Backup backup, String restorePointId, VirtualMachine vm) {
        if (backup == null) {
            return;
        }
        nasBackupProvider.updateBackupDetail(backup, AblestackNasBackupProvider.DETAIL_BACKUP_SOURCE, PROVIDER_NAME);
        if (StringUtils.isNotBlank(restorePointId)) {
            nasBackupProvider.updateBackupDetail(backup, DETAIL_VEEAM_RESTORE_POINT_ID, restorePointId);
        }
        nasBackupProvider.updateBackupDetail(backup, DETAIL_VEEAM_VM_NAME, vm.getInstanceName());
    }

    private String resolveRestorePointId(VirtualMachine vm) {
        final String fromVm = getVmRestorePointDetail(vm);
        if (StringUtils.isNotBlank(fromVm)) {
            return fromVm;
        }
        final List<Backup.RestorePoint> points = listRestorePoints(vm);
        if (CollectionUtils.isEmpty(points)) {
            throw new CloudRuntimeException(String.format(
                    "No Veeam restore point found for VM [%s]. Set detail [%s] or import a seed via API.",
                    vm.getInstanceName(), DETAIL_VEEAM_RESTORE_POINT_ID));
        }
        return points.get(0).getId();
    }

    private String getVmRestorePointDetail(VirtualMachine vm) {
        Map<String, String> details = backupManager.getBackupDetailsFromVM(vm);
        return details != null ? details.get(DETAIL_VEEAM_RESTORE_POINT_ID) : null;
    }

    @Override
    public boolean deleteBackup(Backup backup, boolean forced) {
        return nasBackupProvider.deleteBackup(backup, forced);
    }

    @Override
    public Pair<Boolean, String> restoreBackupToVM(VirtualMachine vm, Backup backup, String hostIp, String dataStoreUuid) {
        return nasBackupProvider.restoreBackupToVM(vm, backup, hostIp, dataStoreUuid);
    }

    @Override
    public Pair<Boolean, String> restoreBackupToVM(Long backupId, String vmName) {
        return nasBackupProvider.restoreBackupToVM(backupId, vmName);
    }

    @Override
    public boolean restoreVMFromBackup(VirtualMachine vm, Backup backup) {
        return nasBackupProvider.restoreVMFromBackup(vm, backup);
    }

    @Override
    public Pair<Boolean, String> restoreBackedUpVolume(Backup backup, Backup.VolumeInfo backupVolumeInfo, String hostIp,
            String dataStoreUuid, Pair<String, VirtualMachine.State> vmNameAndState) {
        return nasBackupProvider.restoreBackedUpVolume(backup, backupVolumeInfo, hostIp, dataStoreUuid, vmNameAndState);
    }

    @Override
    public void syncBackupMetrics(Long zoneId) {
        nasBackupProvider.syncBackupMetrics(zoneId);
    }

    @Override
    public List<Backup.RestorePoint> listRestorePoints(VirtualMachine vm) {
        final String veeamVmName = getVeeamSourceVmName(vm);
        final AblestackVeeamClient client = getClient(vm.getDataCenterId());
        client.syncBackupRepository();
        return client.listRestorePointsForVmDisplayName(veeamVmName);
    }

    private String getVeeamSourceVmName(VirtualMachine vm) {
        Map<String, String> details = backupManager.getBackupDetailsFromVM(vm);
        if (details != null && StringUtils.isNotBlank(details.get(DETAIL_VEEAM_VM_NAME))) {
            return details.get(DETAIL_VEEAM_VM_NAME);
        }
        return vm.getInstanceName();
    }

    @Override
    public Backup createNewBackupEntryForRestorePoint(Backup.RestorePoint restorePoint, VirtualMachine vm) {
        throw new CloudRuntimeException("Use importVeeamNasBackupSeed API to register a Veeam restore point as NAS seed");
    }

    @Override
    public boolean supportsInstanceFromBackup() {
        return nasBackupProvider.supportsInstanceFromBackup();
    }

    @Override
    public Pair<Long, Long> getBackupStorageStats(Long zoneId) {
        return nasBackupProvider.getBackupStorageStats(zoneId);
    }

    @Override
    public void syncBackupStorageStats(Long zoneId) {
        nasBackupProvider.syncBackupStorageStats(zoneId);
    }

    @Override
    public void syncBackups(VirtualMachine vm) {
        nasBackupProvider.syncBackups(vm);
    }

    /**
     * Backups for this provider are NAS-managed (delegated to nasBackupProvider).
     * Veeam restore points returned by {@link #listRestorePoints(VirtualMachine)} are
     * a seed source only, not the authoritative backup list. The generic out-of-band
     * sync reconciles DB backups against listRestorePoints() and DELETES any DB backup
     * that has no matching Veeam restore point, which would wrongly wipe NAS/agent backups
     * (e.g. when Veeam Enterprise Manager reports no restore points for the VM display name).
     * Opt out so those backups are not destroyed; NAS sync is handled via syncBackups(vm).
     */
    @Override
    public boolean supportsOutOfBandBackupSync() {
        return false;
    }

    @Override
    public boolean checkBackupAgent(Long zoneId) {
        return true;
    }

    @Override
    public boolean installBackupAgent(Long zoneId) {
        return true;
    }

    @Override
    public boolean importBackupPlan(Long zoneId, String retentionPeriod, String externalId) {
        return true;
    }

    @Override
    public boolean updateBackupPlan(Long zoneId, String retentionPeriod, String externalId) {
        return true;
    }

    @Override
    public Boolean crossZoneInstanceCreationEnabled(BackupOffering backupOffering) {
        return nasBackupProvider.crossZoneInstanceCreationEnabled(backupOffering);
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                AblestackVeeamUrl,
                AblestackVeeamVersion,
                AblestackVeeamUsername,
                AblestackVeeamPassword,
                AblestackVeeamValidateSsl,
                AblestackVeeamApiTimeout,
                AblestackVeeamRestoreTimeout,
                AblestackVeeamTaskPollInterval,
                AblestackVeeamTaskPollMaxRetry,
                AblestackVeeamStagingPath
        };
    }

    @Override
    public String getConfigComponentName() {
        return BackupService.class.getSimpleName();
    }

    @Override
    public boolean supportsVolumeLevelChainState() {
        return nasBackupProvider.supportsVolumeLevelChainState();
    }

    @Override
    public boolean supportsRestorePlan() {
        return nasBackupProvider.supportsRestorePlan();
    }

    @Override
    public boolean supportsRestoreChainValidation() {
        return nasBackupProvider.supportsRestoreChainValidation();
    }
}
