//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.backup.AblestackBackupFrameworkUtils;
import org.apache.cloudstack.backup.AblestackVeeamRestoreBackupCommand;
import org.apache.cloudstack.backup.BackupAnswer;
import org.apache.cloudstack.backup.BackupRestorePlan;
import org.apache.cloudstack.backup.BackupRestoreStage;
import org.apache.cloudstack.backup.BackupVolumeChainState;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.LibvirtException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@ResourceWrapper(handles = AblestackVeeamRestoreBackupCommand.class)
public class LibvirtAblestackVeeamRestoreBackupCommandWrapper extends CommandWrapper<AblestackVeeamRestoreBackupCommand, Answer, LibvirtComputingResource> {
    private static final String FILE_PATH_PLACEHOLDER = "%s/%s";
    private static final String COMMAND_EXIT_MARKER = "__CS_COMMAND_EXIT__=";
    private static final String ATTACH_QCOW2_DISK_COMMAND = " virsh attach-disk %s %s %s --driver qemu --subdriver qcow2 --cache none";
    private static final String ATTACH_RBD_DISK_XML_COMMAND = " virsh attach-device %s /dev/stdin <<EOF%sEOF";
    private static final String CURRENT_DEVICE = "virsh domblklist --domain %s | tail -n 3 | head -n 1 | awk '{print $1}'";

    @Override
    public Answer execute(final AblestackVeeamRestoreBackupCommand command, final LibvirtComputingResource serverResource) {
        final String backupPath = command.getBackupPath();
        final Boolean vmExists = command.isVmExists();
        final String diskType = command.getDiskType();
        final List<String> backedVolumeUUIDs = command.getBackupVolumesUUIDs();
        final List<String> backupFiles = command.getBackupFiles();
        final List<String> backupFileChains = command.getBackupFileChains();
        final List<BackupVolumeChainState> volumeChainStates = command.getVolumeChainStates();
        final List<PrimaryDataStoreTO> restoreVolumePools = command.getRestoreVolumePools();
        final List<String> restoreVolumePaths = command.getRestoreVolumePaths();
        final String restoreVolumeUuid = command.getRestoreVolumeUUID();
        final int timeout = command.getTimeout();
        final BackupRestorePlan restorePlan = command.getRestorePlan();
        final String cacheMode = command.getCacheMode();
        final KVMStoragePoolManager storagePoolMgr = serverResource.getStoragePoolMgr();
        String newVolumeId = null;

        try {
            if (Objects.isNull(vmExists)) {
                final PrimaryDataStoreTO restoreVolumePool = restoreVolumePools.get(0);
                final String restoreVolumePath = restoreVolumePaths.get(0);
                final int lastIndex = restoreVolumePath.lastIndexOf("/");
                newVolumeId = restoreVolumePath.substring(lastIndex + 1);
                restoreVolume(storagePoolMgr, backupPath, restoreVolumePool, restoreVolumePath, diskType, restoreVolumeUuid,
                        backupFiles, backupFileChains, volumeChainStates, command.getVmName(), command.getVmState(), timeout, cacheMode, restorePlan);
            } else if (Boolean.TRUE.equals(vmExists)) {
                restoreVolumesOfExistingVM(storagePoolMgr, restoreVolumePools, restoreVolumePaths, backedVolumeUUIDs,
                        backupPath, backupFiles, backupFileChains, volumeChainStates, timeout, restorePlan);
            } else {
                throw new CloudRuntimeException("Veeam restore currently supports existing VM and single volume restore only");
            }
        } catch (final CloudRuntimeException e) {
            final String errorMessage = e.getMessage() != null ? e.getMessage() : "";
            return new BackupAnswer(command, false, errorMessage);
        }

        return new BackupAnswer(command, true, newVolumeId);
    }

    private void restoreVolumesOfExistingVM(final KVMStoragePoolManager storagePoolMgr, final List<PrimaryDataStoreTO> restoreVolumePools,
            final List<String> restoreVolumePaths, final List<String> backedVolumesUUIDs, final String backupPath, final List<String> backupFiles,
            final List<String> backupFileChains, final List<BackupVolumeChainState> volumeChainStates, final int timeout,
            final BackupRestorePlan restorePlan) {
        String diskType = "root";
        try {
            validateChainStatePlan(volumeChainStates, restorePlan);
            for (int idx = 0; idx < restoreVolumePaths.size(); idx++) {
                final PrimaryDataStoreTO restoreVolumePool = restoreVolumePools.get(idx);
                final String restoreVolumePath = restoreVolumePaths.get(idx);
                final String backupVolumeUuid = backedVolumesUUIDs.get(idx);
                final List<String> localBackupPaths = getLocalBackupPaths(backupPath, backupFiles, backupFileChains, volumeChainStates, idx,
                        getLegacyBackupFileName(diskType, backupVolumeUuid));
                logger.info("Resolved Veeam local backup paths for existing VM volume [{}], target [{}]: {}",
                        backupVolumeUuid, restoreVolumePath, localBackupPaths);
                validateResolvedChainPaths(localBackupPaths, restoreVolumePath);
                diskType = "datadisk";
                if (!replaceVolumeWithBackup(storagePoolMgr, restoreVolumePool, restoreVolumePath, localBackupPaths, timeout, backupPath, idx)) {
                    throw new CloudRuntimeException(String.format("Unable to restore contents from the backup volume [%s].", backupVolumeUuid));
                }
            }
        } finally {
            cleanupBackupDirectory(backupPath, restorePlan);
        }
    }

    private void validateChainStatePlan(final List<BackupVolumeChainState> volumeChainStates, final BackupRestorePlan restorePlan) {
        if (AblestackBackupFrameworkUtils.hasRestoreStage(restorePlan, BackupRestoreStage.VALIDATE_CHAIN)
                && volumeChainStates != null && !volumeChainStates.isEmpty()) {
            try {
                AblestackBackupFrameworkUtils.validateVolumeChainStates(volumeChainStates);
            } catch (final IllegalArgumentException e) {
                throw new CloudRuntimeException(e.getMessage(), e);
            }
        }
    }

    private void validateResolvedChainPaths(final List<String> resolvedPaths, final String volumePath) {
        if (resolvedPaths == null || resolvedPaths.isEmpty()) {
            throw new CloudRuntimeException(String.format("No resolved backup chain paths found for volume [%s]", volumePath));
        }
    }

    private void cleanupBackupDirectory(final String backupPath, final BackupRestorePlan restorePlan) {
        if (AblestackBackupFrameworkUtils.hasRestoreStage(restorePlan, BackupRestoreStage.CLEANUP_SOURCE)) {
            deleteBackupDirectory(backupPath);
        }
    }

    private void restoreVolume(final KVMStoragePoolManager storagePoolMgr, final String backupPath, final PrimaryDataStoreTO volumePool,
            final String volumePath, final String diskType, final String volumeUUID, final List<String> backupFiles,
            final List<String> backupFileChains, final List<BackupVolumeChainState> volumeChainStates, final String vmName,
            final VirtualMachine.State vmState, final int timeout, final String cacheMode, final BackupRestorePlan restorePlan) {
        try {
            final List<String> localBackupPaths = getLocalBackupPaths(backupPath, backupFiles, backupFileChains, volumeChainStates, 0,
                    getLegacyBackupFileName(diskType, volumeUUID));
            logger.info("Resolved Veeam local backup paths for restored volume [{}], target [{}]: {}",
                    volumeUUID, volumePath, localBackupPaths);
            validateResolvedChainPaths(localBackupPaths, volumePath);
            if (!replaceVolumeWithBackup(storagePoolMgr, volumePool, volumePath, localBackupPaths, timeout, backupPath, 0, true)) {
                throw new CloudRuntimeException(String.format("Unable to restore contents from the backup volume [%s].", volumeUUID));
            }
            if (AblestackBackupFrameworkUtils.hasRestoreStage(restorePlan, BackupRestoreStage.ATTACH_VOLUME)
                    && VirtualMachine.State.Running.equals(vmState)) {
                if (!attachVolumeToVm(storagePoolMgr, vmName, volumePool, volumePath, cacheMode)) {
                    throw new CloudRuntimeException(String.format("Failed to attach volume to VM: %s", vmName));
                }
            }
        } finally {
            cleanupBackupDirectory(backupPath, restorePlan);
        }
    }

    private void deleteBackupDirectory(final String backupDirectory) {
        try {
            FileUtils.deleteDirectory(new File(backupDirectory));
        } catch (final IOException e) {
            logger.error(String.format("Failed to delete backup directory: %s", backupDirectory), e);
            throw new CloudRuntimeException("Failed to delete the backup directory");
        }
    }

    private List<String> getLocalBackupPaths(final String backupPath, final List<String> backupFiles, final List<String> backupFileChains,
            final List<BackupVolumeChainState> volumeChainStates, final int index, final String legacyBackupFileName) {
        final LinkedHashSet<String> localPaths = new LinkedHashSet<>();
        boolean resolvedFromVolumeChainStates = false;
        if (volumeChainStates != null && volumeChainStates.size() > index) {
            for (final String chainPath : volumeChainStates.get(index).getChainFiles()) {
                if (StringUtils.isBlank(chainPath)) {
                    continue;
                }
                localPaths.add(resolveBackupPath(backupPath, chainPath));
                resolvedFromVolumeChainStates = true;
            }
        }
        if (!resolvedFromVolumeChainStates && backupFileChains != null && backupFileChains.size() > index && StringUtils.isNotBlank(backupFileChains.get(index))) {
            for (final String chainPath : backupFileChains.get(index).split(";")) {
                if (StringUtils.isBlank(chainPath)) {
                    continue;
                }
                localPaths.add(resolveBackupPath(backupPath, chainPath));
            }
        }
        if (localPaths.isEmpty() && backupFiles != null && backupFiles.size() > index && StringUtils.isNotBlank(backupFiles.get(index))) {
            localPaths.add(resolveBackupPath(backupPath, backupFiles.get(index)));
        }
        if (localPaths.isEmpty()) {
            localPaths.add(String.format(FILE_PATH_PLACEHOLDER, backupPath, legacyBackupFileName));
        }
        return new ArrayList<>(localPaths);
    }

    private String resolveBackupPath(final String backupPath, final String chainPath) {
        if (chainPath.startsWith("/")) {
            return chainPath;
        }
        if (chainPath.contains("/")) {
            return String.format(FILE_PATH_PLACEHOLDER, backupPath, chainPath);
        }
        return String.format(FILE_PATH_PLACEHOLDER, backupPath, chainPath);
    }

    private String getLegacyBackupFileName(final String diskType, final String volumeUuid) {
        return String.format("%s.%s.qcow2", diskType.toLowerCase(Locale.ROOT), volumeUuid);
    }

    private boolean replaceVolumeWithBackup(final KVMStoragePoolManager storagePoolMgr, final PrimaryDataStoreTO volumePool,
            final String volumePath, final List<String> backupPaths, final int timeout, final String backupRootPath, final int backupIndex) {
        return replaceVolumeWithBackup(storagePoolMgr, volumePool, volumePath, backupPaths, timeout, backupRootPath, backupIndex, false);
    }

    private boolean replaceVolumeWithBackup(final KVMStoragePoolManager storagePoolMgr, final PrimaryDataStoreTO volumePool,
            final String volumePath, final List<String> backupPaths, final int timeout, final String backupRootPath, final int backupIndex,
            final boolean createTargetVolume) {
        if (backupPaths == null || backupPaths.isEmpty()) {
            return false;
        }
        if (volumePool.getPoolType() != Storage.StoragePoolType.RBD) {
            if (backupPaths.stream().anyMatch(path -> path.endsWith(".rbdiff"))) {
                return restoreIncrementalRbdBackupChainToFileVolume(volumePath, backupPaths, timeout, backupRootPath, backupIndex);
            }
            return replaceFileVolumeWithBackup(volumePath, backupPaths, timeout);
        }

        return replaceRbdVolumeWithBackup(storagePoolMgr, volumePool, volumePath, backupPaths, timeout, createTargetVolume);
    }

    private boolean restoreIncrementalRbdBackupChainToFileVolume(final String volumePath, final List<String> backupPaths, final int timeout,
            final String backupRootPath, final int backupIndex) {
        if (StringUtils.isBlank(backupRootPath)) {
            throw new CloudRuntimeException("Unable to locate backup root path for incremental RBD restore");
        }
        final RbdImageSpec sourceImage = getRbdImageSpecFromMetadata(backupRootPath, backupIndex);
        final String tempImage = sourceImage.buildTempImageSpec();
        try {
            if (!importBackupChainToTemporaryRbd(backupPaths, timeout, sourceImage, tempImage)) {
                return false;
            }
            return convertTemporaryRbdToFileVolume(volumePath, timeout, sourceImage, tempImage);
        } finally {
            removeTemporaryRbdImage(sourceImage, tempImage, timeout);
        }
    }

    private String getFirstExistingBackupPath(final List<String> backupPaths) {
        for (final String backupPath : backupPaths) {
            if (StringUtils.isNotBlank(backupPath) && Files.exists(Paths.get(backupPath))) {
                return backupPath;
            }
        }
        return backupPaths.get(0);
    }

    private String getLastExistingBackupPath(final List<String> backupPaths) {
        for (int i = backupPaths.size() - 1; i >= 0; i--) {
            final String backupPath = backupPaths.get(i);
            if (StringUtils.isNotBlank(backupPath) && Files.exists(Paths.get(backupPath))) {
                return backupPath;
            }
        }
        return backupPaths.get(backupPaths.size() - 1);
    }

    private boolean replaceFileVolumeWithBackup(final String volumePath, final String backupPath, final int timeout) {
        QemuImgFile srcBackupFile = null;
        Path temporaryVolumePath = null;
        try {
            final QemuImg qemu = new QemuImg(timeout * 1000, true, false);
            srcBackupFile = new QemuImgFile(backupPath, getBackupFileFormat(backupPath));
            final QemuImg.PhysicalDiskFormat targetFormat = getFileVolumeFormat(volumePath);
            temporaryVolumePath = Files.createTempFile("cs-veeam-restore-volume-", "." + targetFormat.toString().toLowerCase(Locale.ROOT));
            Files.deleteIfExists(temporaryVolumePath);
            final QemuImgFile temporaryVolumeFile = new QemuImgFile(temporaryVolumePath.toString(), targetFormat);
            logger.info("Converting Veeam file volume from backup [{}] format [{}] to temporary target [{}] format [{}] before replacing final target [{}]",
                    srcBackupFile.getFileName(), srcBackupFile.getFormat(), temporaryVolumeFile.getFileName(), temporaryVolumeFile.getFormat(), volumePath);
            qemu.convert(srcBackupFile, temporaryVolumeFile);
            Files.copy(temporaryVolumePath, Paths.get(volumePath), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (final QemuImgException | LibvirtException | IOException e) {
            final String srcFilename = srcBackupFile != null ? srcBackupFile.getFileName() : null;
            logger.error("Failed to convert backup {} to volume {}, the error was: {}", srcFilename, volumePath, e.getMessage());
            return false;
        } finally {
            if (temporaryVolumePath != null) {
                try {
                    Files.deleteIfExists(temporaryVolumePath);
                } catch (final IOException e) {
                    logger.warn("Failed to delete temporary Veeam restored volume file {}", temporaryVolumePath, e);
                }
            }
        }
    }

    private boolean replaceFileVolumeWithBackup(final String volumePath, final List<String> backupPaths, final int timeout) {
        if (backupPaths == null || backupPaths.isEmpty()) {
            return false;
        }
        if (backupPaths.size() == 1) {
            return replaceFileVolumeWithBackup(volumePath, getLastExistingBackupPath(backupPaths), timeout);
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("cs-veeam-qcow2-chain-");
            Path latestChainFile = null;
            Path previousChainFile = null;
            for (int index = 0; index < backupPaths.size(); index++) {
                final String backupPath = backupPaths.get(index);
                if (StringUtils.isBlank(backupPath)) {
                    continue;
                }
                final Path source = Paths.get(backupPath);
                if (!Files.exists(source)) {
                    throw new CloudRuntimeException(String.format("Missing QCOW2 backup chain file [%s] for restore", backupPath));
                }
                final Path copiedChainFile = tempDir.resolve(String.format("%03d-%s", index, source.getFileName()));
                Files.copy(source, copiedChainFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                if (previousChainFile != null) {
                    rebaseBackupChainFile(copiedChainFile, previousChainFile, timeout);
                }
                previousChainFile = copiedChainFile;
                latestChainFile = copiedChainFile;
            }
            if (latestChainFile == null) {
                throw new CloudRuntimeException(String.format("No QCOW2 backup chain files were prepared for restore to volume [%s]", volumePath));
            }
            return replaceFileVolumeWithBackup(volumePath, latestChainFile.toString(), timeout);
        } catch (final IOException e) {
            logger.error("Failed to reconstruct QCOW2 backup chain {} for volume {}: {}", backupPaths, volumePath, e.getMessage(), e);
            return false;
        } finally {
            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir.toFile());
                } catch (final IOException e) {
                    logger.warn("Failed to delete temporary QCOW2 restore chain directory {}", tempDir, e);
                }
            }
        }
    }

    private void rebaseBackupChainFile(final Path child, final Path parent, final int timeout) throws IOException {
        final String command = String.format("qemu-img rebase -u -F qcow2 -b %s %s", quote(parent.toString()), quote(child.toString()));
        final CommandExecutionResult result = executeBashCommandWithResult(command, timeout, "Rebase QCOW2 restore chain");
        if (result.exitCode != 0) {
            throw new IOException(String.format("qemu-img rebase failed for %s with parent %s: %s", child, parent, result.output));
        }
    }

    private boolean convertTemporaryRbdToFileVolume(final String volumePath, final int timeout, final RbdImageSpec sourceImage, final String tempImage) {
        QemuImgFile srcBackupFile = null;
        QemuImgFile destVolumeFile = null;
        try {
            final QemuImg qemu = new QemuImg(timeout * 1000, true, false);
            srcBackupFile = new QemuImgFile(sourceImage.buildQemuUri(tempImage), QemuImg.PhysicalDiskFormat.RAW);
            destVolumeFile = new QemuImgFile(volumePath, getFileVolumeFormat(volumePath));
            qemu.convert(srcBackupFile, destVolumeFile);
            return true;
        } catch (final QemuImgException | LibvirtException e) {
            final String srcFilename = srcBackupFile != null ? srcBackupFile.getFileName() : tempImage;
            final String destFilename = destVolumeFile != null ? destVolumeFile.getFileName() : volumePath;
            logger.error("Failed to convert temporary RBD {} to volume {}, the error was: {}", srcFilename, destFilename, e.getMessage());
            return false;
        }
    }

    private QemuImg.PhysicalDiskFormat getBackupFileFormat(final String backupPath) {
        if (backupPath.endsWith(".raw")) {
            return QemuImg.PhysicalDiskFormat.RAW;
        }
        return QemuImg.PhysicalDiskFormat.QCOW2;
    }

    private QemuImg.PhysicalDiskFormat getFileVolumeFormat(final String volumePath) {
        if (!Files.exists(Paths.get(volumePath))) {
            return QemuImg.PhysicalDiskFormat.QCOW2;
        }
        try {
            final QemuImg qemu = new QemuImg(0);
            final Map<String, String> info = qemu.info(new QemuImgFile(volumePath));
            final String format = info.get("file_format");
            if (StringUtils.isNotBlank(format)) {
                return QemuImg.PhysicalDiskFormat.valueOf(format.toUpperCase(Locale.ROOT));
            }
        } catch (final QemuImgException | LibvirtException | IllegalArgumentException e) {
            logger.warn("Failed to detect file volume format for path {}. Falling back to qcow2.", volumePath, e);
        }
        return QemuImg.PhysicalDiskFormat.QCOW2;
    }

    private boolean replaceRbdVolumeWithBackup(final KVMStoragePoolManager storagePoolMgr, final PrimaryDataStoreTO volumePool,
            final String volumePath, final List<String> backupPaths, final int timeout, final boolean createTargetVolume) {
        if (backupPaths.stream().anyMatch(path -> path.endsWith(".rbdiff"))) {
            return restoreIncrementalRbdBackupChain(storagePoolMgr, volumePool, volumePath, backupPaths, timeout, createTargetVolume);
        }

        final String backupPath = getFirstExistingBackupPath(backupPaths);
        final KVMStoragePool volumeStoragePool = storagePoolMgr.getStoragePool(volumePool.getPoolType(), volumePool.getUuid());
        final String normalizedVolumePath = normalizeRbdVolumePath(volumePath, volumeStoragePool);
        if (getBackupFileFormat(backupPath) == QemuImg.PhysicalDiskFormat.RAW) {
            return importRawBackupToRbd(volumeStoragePool, normalizedVolumePath, backupPath, timeout, createTargetVolume);
        }

        QemuImg qemu;
        QemuImgFile destVolumeFile = null;
        QemuImgFile srcBackupFile = null;
        try {
            qemu = new QemuImg(timeout * 1000, true, false);
            if (!createTargetVolume) {
                final KVMPhysicalDisk rdbDisk = volumeStoragePool.getPhysicalDisk(normalizedVolumePath);
                logger.debug("Restoring RBD volume: {}", rdbDisk.toString());
                qemu.setSkipTargetVolumeCreation(true);
            }
        } catch (final LibvirtException ex) {
            throw new CloudRuntimeException("Failed to create qemu-img command to restore RBD volume with backup", ex);
        }

        try {
            srcBackupFile = new QemuImgFile(backupPath, getBackupFileFormat(backupPath));
            final String rbdDestVolumeFile = KVMPhysicalDisk.RBDStringBuilder(volumeStoragePool, normalizedVolumePath);
            destVolumeFile = new QemuImgFile(rbdDestVolumeFile, QemuImg.PhysicalDiskFormat.RAW);
            qemu.convert(srcBackupFile, destVolumeFile);
            return true;
        } catch (final QemuImgException | LibvirtException e) {
            final String srcFilename = srcBackupFile != null ? srcBackupFile.getFileName() : null;
            final String destFilename = destVolumeFile != null ? destVolumeFile.getFileName() : null;
            logger.error("Failed to convert backup {} to volume {}, the error was: {}", srcFilename, destFilename, e.getMessage());
            return false;
        }
    }

    private boolean importRawBackupToRbd(final KVMStoragePool volumeStoragePool, final String volumePath, final String backupPath, final int timeout,
            final boolean createTargetVolume) {
        if (!createTargetVolume && !deleteExistingRbdVolumeIfPresent(volumeStoragePool, volumePath)) {
            logger.error("Failed to delete existing RBD volume {} before raw import", volumePath);
            return false;
        }

        final String importCommand = buildRbdImportCommand(volumeStoragePool, backupPath, volumePath);
        final CommandExecutionResult importResult = executeBashCommandWithResult(importCommand, timeout, "Import raw backup to RBD");
        if (importResult.exitCode != 0) {
            logger.error("Failed to import raw backup {} into volume {}. Exit code: {}, output: {}", backupPath, volumePath, importResult.exitCode, importResult.output);
            return false;
        }
        return true;
    }

    private boolean deleteExistingRbdVolumeIfPresent(final KVMStoragePool volumeStoragePool, final String volumePath) {
        try {
            return volumeStoragePool.deletePhysicalDisk(volumePath, Storage.ImageFormat.RAW);
        } catch (final CloudRuntimeException e) {
            if (isMissingRbdImageError(e)) {
                logger.info("Skipping deletion for missing RBD volume {} before restore", volumePath);
                return true;
            }
            throw e;
        }
    }

    private boolean isMissingRbdImageError(final CloudRuntimeException e) {
        final String message = e.getMessage();
        return StringUtils.containsIgnoreCase(message, "Failed to open image")
                && StringUtils.containsIgnoreCase(message, "No such file or directory");
    }

    private boolean restoreIncrementalRbdBackupChain(final KVMStoragePoolManager storagePoolMgr, final PrimaryDataStoreTO volumePool,
            final String volumePath, final List<String> backupPaths, final int timeout, final boolean createTargetVolume) {
        if (backupPaths.isEmpty() || !backupPaths.get(0).endsWith(".raw")) {
            throw new CloudRuntimeException("Incremental RBD backup chain is missing the base full backup");
        }

        final String normalizedVolumePath = normalizeRbdVolumePath(volumePath, storagePoolMgr.getStoragePool(volumePool.getPoolType(), volumePool.getUuid()));
        if (!replaceRbdVolumeWithBackup(storagePoolMgr, volumePool, normalizedVolumePath, List.of(backupPaths.get(0)), timeout, createTargetVolume)) {
            return false;
        }

        final KVMStoragePool volumeStoragePool = storagePoolMgr.getStoragePool(volumePool.getPoolType(), volumePool.getUuid());
        final List<String> restoreSnapshots = new ArrayList<>();
        try {
            final Map<String, String> baseMetadata = readRbdBackupMetadata(backupPaths.get(0));
            final String baseCheckpoint = baseMetadata.get("checkpoint_name");
            if (StringUtils.isNotBlank(baseCheckpoint)) {
                if (!ensureRbdSnapshotExists(volumeStoragePool, normalizedVolumePath, baseCheckpoint, timeout)) {
                    return false;
                }
                restoreSnapshots.add(baseCheckpoint);
            }

            for (int index = 1; index < backupPaths.size(); index++) {
                final String backupPath = backupPaths.get(index);
                if (!backupPath.endsWith(".rbdiff")) {
                    continue;
                }
                final Map<String, String> metadata = readRbdBackupMetadata(backupPath);
                final String parentCheckpoint = metadata.get("parent_checkpoint_name");
                final String checkpoint = metadata.get("checkpoint_name");
                if (StringUtils.isBlank(parentCheckpoint) || StringUtils.isBlank(checkpoint)) {
                    throw new CloudRuntimeException(String.format("RBD incremental backup metadata is incomplete for %s", backupPath));
                }
                if (!rbdSnapshotExists(volumeStoragePool, normalizedVolumePath, parentCheckpoint, timeout)) {
                    throw new CloudRuntimeException(String.format("Required parent snapshot %s is missing on volume %s", parentCheckpoint, normalizedVolumePath));
                }
                final String importDiffCommand = buildRbdImportDiffCommand(volumeStoragePool, backupPath, normalizedVolumePath);
                final CommandExecutionResult importDiffResult = executeBashCommandWithResult(importDiffCommand, timeout, "Import RBD diff to target volume");
                if (importDiffResult.exitCode != 0) {
                    logger.error("Failed to import RBD diff {} into volume {}. Exit code: {}, output: {}", backupPath, normalizedVolumePath,
                            importDiffResult.exitCode, importDiffResult.output);
                    return false;
                }
                if (!ensureRbdSnapshotExists(volumeStoragePool, normalizedVolumePath, checkpoint, timeout)) {
                    return false;
                }
                restoreSnapshots.add(checkpoint);
            }
            return true;
        } finally {
            cleanupRbdRestoreSnapshots(volumeStoragePool, normalizedVolumePath, restoreSnapshots, timeout);
        }
    }

    private String normalizeRbdVolumePath(final String volumePath, final KVMStoragePool storagePool) {
        if (StringUtils.isBlank(volumePath)) {
            return volumePath;
        }
        String normalized = volumePath;
        final String poolPath = storagePool.getSourceDir();
        if (StringUtils.isNotBlank(poolPath)) {
            final String poolPrefix = poolPath + "/";
            if (normalized.startsWith(poolPrefix)) {
                normalized = normalized.substring(poolPrefix.length());
            }
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
        }
        return normalized;
    }

    private String buildRbdImportDiffCommand(final KVMStoragePool storagePool, final String backupPath, final String volumePath) {
        final StringBuilder command = new StringBuilder("rbd");
        if (StringUtils.isNotBlank(storagePool.getSourceHost())) {
            command.append(" -m ").append(formatRbdMonHosts(storagePool.getSourceHost(), storagePool.getSourcePort()));
        }
        if (StringUtils.isNotBlank(storagePool.getAuthUserName())) {
            command.append(" --id ").append(storagePool.getAuthUserName());
        }
        if (StringUtils.isNotBlank(storagePool.getAuthSecret())) {
            command.append(" --key ").append(storagePool.getAuthSecret());
        }
        command.append(" import-diff ").append(backupPath).append(" ").append(volumePath);
        return command.toString();
    }

    private String buildRbdImportCommand(final KVMStoragePool storagePool, final String backupPath, final String volumePath) {
        final StringBuilder command = new StringBuilder("rbd");
        if (StringUtils.isNotBlank(storagePool.getSourceHost())) {
            command.append(" -m ").append(formatRbdMonHosts(storagePool.getSourceHost(), storagePool.getSourcePort()));
        }
        if (StringUtils.isNotBlank(storagePool.getAuthUserName())) {
            command.append(" --id ").append(storagePool.getAuthUserName());
        }
        if (StringUtils.isNotBlank(storagePool.getAuthSecret())) {
            command.append(" --key ").append(storagePool.getAuthSecret());
        }
        command.append(" import ").append(backupPath).append(" ").append(volumePath);
        return command.toString();
    }

    private String formatRbdMonHosts(final String hosts, final int port) {
        final String[] hostValues = hosts.split(",");
        final List<String> formattedHosts = new ArrayList<>();
        for (final String host : hostValues) {
            final String normalizedHost = host.replace("[", "").replace("]", "").trim();
            if (StringUtils.isBlank(normalizedHost)) {
                continue;
            }
            formattedHosts.add(port > 0 ? normalizedHost + ":" + port : normalizedHost);
        }
        return String.join(",", formattedHosts);
    }

    private boolean importBackupChainToTemporaryRbd(final List<String> backupPaths, final int timeout, final RbdImageSpec sourceImage, final String tempImage) {
        if (backupPaths.isEmpty() || !backupPaths.get(0).endsWith(".raw")) {
            throw new CloudRuntimeException("Incremental RBD backup chain is missing the base full backup");
        }
        final String importCommand = sourceImage.buildRbdCommand("import", quote(backupPaths.get(0)), quote(tempImage));
        final CommandExecutionResult importResult = executeBashCommandWithResult(importCommand, timeout, "Import raw backup to temporary RBD");
        if (importResult.exitCode != 0) {
            logger.error("Failed to import base RBD backup {} into temporary image {}. Exit code: {}, output: {}", backupPaths.get(0), tempImage,
                    importResult.exitCode, importResult.output);
            return false;
        }
        final List<String> restoreSnapshots = new ArrayList<>();
        try {
            final Map<String, String> baseMetadata = readRbdBackupMetadata(backupPaths.get(0));
            final String baseCheckpoint = baseMetadata.get("checkpoint_name");
            if (StringUtils.isNotBlank(baseCheckpoint)) {
                if (!ensureRbdSnapshotExists(sourceImage, tempImage, baseCheckpoint, timeout)) {
                    return false;
                }
                restoreSnapshots.add(baseCheckpoint);
            }
            for (int index = 1; index < backupPaths.size(); index++) {
                final String backupPath = backupPaths.get(index);
                if (!backupPath.endsWith(".rbdiff")) {
                    continue;
                }
                final Map<String, String> metadata = readRbdBackupMetadata(backupPath);
                final String parentCheckpoint = metadata.get("parent_checkpoint_name");
                final String checkpoint = metadata.get("checkpoint_name");
                if (StringUtils.isBlank(parentCheckpoint) || StringUtils.isBlank(checkpoint)) {
                    throw new CloudRuntimeException(String.format("RBD incremental backup metadata is incomplete for %s", backupPath));
                }
                if (!rbdSnapshotExists(sourceImage, tempImage, parentCheckpoint, timeout)) {
                    throw new CloudRuntimeException(String.format("Required parent snapshot %s is missing on temporary image %s", parentCheckpoint, tempImage));
                }
                final String importDiffCommand = sourceImage.buildRbdCommand("import-diff", quote(backupPath), quote(tempImage));
                final CommandExecutionResult importDiffResult = executeBashCommandWithResult(importDiffCommand, timeout, "Import RBD diff to temporary image");
                if (importDiffResult.exitCode != 0) {
                    logger.error("Failed to import RBD diff {} into temporary image {}. Exit code: {}, output: {}", backupPath, tempImage,
                            importDiffResult.exitCode, importDiffResult.output);
                    return false;
                }
                if (!ensureRbdSnapshotExists(sourceImage, tempImage, checkpoint, timeout)) {
                    return false;
                }
                restoreSnapshots.add(checkpoint);
            }
            return true;
        } finally {
            cleanupRbdRestoreSnapshots(sourceImage, tempImage, restoreSnapshots, timeout);
        }
    }

    private Map<String, String> readRbdBackupMetadata(final String backupPath) {
        final Path metadataPath = Paths.get(backupPath).getParent().resolve("rbd-backup.meta");
        if (!Files.exists(metadataPath)) {
            throw new CloudRuntimeException(String.format("RBD backup metadata file not found: %s", metadataPath));
        }
        try {
            return Files.readAllLines(metadataPath).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && line.contains("="))
                    .map(line -> line.split("=", 2))
                    .collect(java.util.stream.Collectors.toMap(parts -> parts[0], parts -> parts[1], (left, right) -> right));
        } catch (final IOException e) {
            throw new CloudRuntimeException(String.format("Failed to read RBD backup metadata: %s", metadataPath), e);
        }
    }

    private boolean ensureRbdSnapshotExists(final KVMStoragePool storagePool, final String volumePath, final String snapshotName, final int timeout) {
        if (rbdSnapshotExists(storagePool, volumePath, snapshotName, timeout)) {
            return true;
        }
        final String createSnapshotCommand = buildRbdSnapshotCommand(storagePool, "snap create", volumePath + "@" + snapshotName);
        final CommandExecutionResult createSnapshotResult = executeBashCommandWithResult(createSnapshotCommand, timeout, "Create RBD snapshot on target volume");
        if (createSnapshotResult.exitCode != 0) {
            logger.error("Failed to create RBD snapshot {} on volume {}. Exit code: {}, output: {}", snapshotName, volumePath,
                    createSnapshotResult.exitCode, createSnapshotResult.output);
            return false;
        }
        return true;
    }

    private boolean ensureRbdSnapshotExists(final RbdImageSpec imageSpec, final String image, final String snapshotName, final int timeout) {
        if (rbdSnapshotExists(imageSpec, image, snapshotName, timeout)) {
            return true;
        }
        final String createSnapshotCommand = imageSpec.buildRbdCommand("snap", "create", quote(image + "@" + snapshotName));
        final CommandExecutionResult createSnapshotResult = executeBashCommandWithResult(createSnapshotCommand, timeout, "Create RBD snapshot on temporary image");
        if (createSnapshotResult.exitCode != 0) {
            logger.error("Failed to create RBD snapshot {} on image {}. Exit code: {}, output: {}", snapshotName, image,
                    createSnapshotResult.exitCode, createSnapshotResult.output);
            return false;
        }
        return true;
    }

    private CommandExecutionResult executeBashCommandWithResult(final String command, final int timeoutInSeconds, final String description) {
        logger.debug("{} command: {}", description, command);
        final String wrappedCommand = String.format("set -o pipefail; { %s; } 2>&1; rc=$?; echo \"%s${rc}\"", command, COMMAND_EXIT_MARKER);
        final String output = Script.runSimpleBashScriptWithFullResult(wrappedCommand, timeoutInSeconds);
        if (StringUtils.isBlank(output)) {
            return new CommandExecutionResult(-1, "");
        }
        final int markerIndex = output.lastIndexOf(COMMAND_EXIT_MARKER);
        if (markerIndex < 0) {
            logger.warn("{} command output did not include an exit marker. Output: {}", description, output);
            return new CommandExecutionResult(-1, output.trim());
        }
        final String commandOutput = output.substring(0, markerIndex).trim();
        final String exitCodeString = output.substring(markerIndex + COMMAND_EXIT_MARKER.length()).trim();
        int exitCode;
        try {
            exitCode = Integer.parseInt(exitCodeString);
        } catch (final NumberFormatException e) {
            logger.warn("{} command exit marker was not a valid integer. Output: {}", description, output, e);
            exitCode = -1;
        }
        if (exitCode == 0) {
            logger.debug("{} command completed successfully. Output: {}", description, commandOutput);
        } else {
            logger.error("{} command failed with exit code {}. Output: {}", description, exitCode, commandOutput);
        }
        return new CommandExecutionResult(exitCode, commandOutput);
    }

    private static final class CommandExecutionResult {
        private final int exitCode;
        private final String output;

        private CommandExecutionResult(final int exitCode, final String output) {
            this.exitCode = exitCode;
            this.output = output;
        }
    }

    private boolean rbdSnapshotExists(final KVMStoragePool storagePool, final String volumePath, final String snapshotName, final int timeout) {
        final String existsCommand = buildRbdSnapshotCommand(storagePool, "snap ls", volumePath) + " | awk 'NR>1 {print $2}' | grep -Fx " + quote(snapshotName);
        return Script.runSimpleBashScriptForExitValue(existsCommand, timeout * 1000, false) == 0;
    }

    private boolean rbdSnapshotExists(final RbdImageSpec imageSpec, final String image, final String snapshotName, final int timeout) {
        final String existsCommand = imageSpec.buildRbdCommand("snap", "ls", quote(image)) + " | awk 'NR>1 {print $2}' | grep -Fx " + quote(snapshotName);
        return Script.runSimpleBashScriptForExitValue(existsCommand, timeout * 1000, false) == 0;
    }

    private void cleanupRbdRestoreSnapshots(final KVMStoragePool storagePool, final String volumePath, final List<String> snapshotNames, final int timeout) {
        for (int index = snapshotNames.size() - 1; index >= 0; index--) {
            final String snapshotName = snapshotNames.get(index);
            final String removeSnapshotCommand = buildRbdSnapshotCommand(storagePool, "snap rm", volumePath + "@" + snapshotName);
            Script.runSimpleBashScriptForExitValue(removeSnapshotCommand, timeout * 1000, false);
        }
    }

    private void cleanupRbdRestoreSnapshots(final RbdImageSpec imageSpec, final String image, final List<String> snapshotNames, final int timeout) {
        for (int index = snapshotNames.size() - 1; index >= 0; index--) {
            final String snapshotName = snapshotNames.get(index);
            final String removeSnapshotCommand = imageSpec.buildRbdCommand("snap", "rm", quote(image + "@" + snapshotName));
            Script.runSimpleBashScriptForExitValue(removeSnapshotCommand, timeout * 1000, false);
        }
    }

    private String buildRbdSnapshotCommand(final KVMStoragePool storagePool, final String action, final String target) {
        final StringBuilder command = new StringBuilder("rbd");
        if (StringUtils.isNotBlank(storagePool.getSourceHost())) {
            command.append(" -m ").append(formatRbdMonHosts(storagePool.getSourceHost(), storagePool.getSourcePort()));
        }
        if (StringUtils.isNotBlank(storagePool.getAuthUserName())) {
            command.append(" --id ").append(storagePool.getAuthUserName());
        }
        if (StringUtils.isNotBlank(storagePool.getAuthSecret())) {
            command.append(" --key ").append(storagePool.getAuthSecret());
        }
        command.append(" ").append(action).append(" ").append(target);
        return command.toString();
    }

    private void removeTemporaryRbdImage(final RbdImageSpec sourceImage, final String tempImage, final int timeout) {
        final String removeCommand = sourceImage.buildRbdCommand("rm", quote(tempImage));
        Script.runSimpleBashScriptForExitValue(removeCommand, timeout * 1000, false);
    }

    private RbdImageSpec getRbdImageSpecFromMetadata(final String backupRootPath, final int backupIndex) {
        final Path metadataPath = Paths.get(backupRootPath, "rbd-backup.meta");
        if (!Files.exists(metadataPath)) {
            throw new CloudRuntimeException(String.format("RBD backup metadata file not found: %s", metadataPath));
        }
        try {
            final Map<String, String> metadata = Files.readAllLines(metadataPath).stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && line.contains("="))
                    .map(line -> line.split("=", 2))
                    .collect(java.util.stream.Collectors.toMap(parts -> parts[0], parts -> parts[1], (left, right) -> right));
            final String diskPaths = metadata.get("disk_paths");
            if (StringUtils.isBlank(diskPaths)) {
                throw new CloudRuntimeException("RBD backup metadata does not contain disk_paths");
            }
            final List<String> values = Arrays.asList(diskPaths.split(","));
            if (backupIndex >= values.size()) {
                throw new CloudRuntimeException(String.format("RBD backup metadata does not contain disk path for index %d", backupIndex));
            }
            return RbdImageSpec.fromUri(values.get(backupIndex));
        } catch (final IOException e) {
            throw new CloudRuntimeException(String.format("Failed to read RBD backup metadata: %s", metadataPath), e);
        }
    }

    private String quote(final String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    private boolean attachVolumeToVm(final KVMStoragePoolManager storagePoolMgr, final String vmName, final PrimaryDataStoreTO volumePool,
            final String volumePath, String cacheMode) {
        final String deviceToAttachDiskTo = getDeviceToAttachDisk(vmName);
        final int exitValue;
        if (volumePool.getPoolType() != Storage.StoragePoolType.RBD) {
            exitValue = Script.runSimpleBashScriptForExitValue(String.format(ATTACH_QCOW2_DISK_COMMAND, vmName, volumePath, deviceToAttachDiskTo));
        } else {
            final String xmlForRbdDisk = getXmlForRbdDisk(storagePoolMgr, volumePool, volumePath, deviceToAttachDiskTo, cacheMode);
            logger.debug("RBD disk xml to attach: {}", xmlForRbdDisk);
            exitValue = Script.runSimpleBashScriptForExitValue(String.format(ATTACH_RBD_DISK_XML_COMMAND, vmName, xmlForRbdDisk));
        }
        return exitValue == 0;
    }

    private String getDeviceToAttachDisk(final String vmName) {
        final String currentDevice = Script.runSimpleBashScript(String.format(CURRENT_DEVICE, vmName));
        if (StringUtils.isBlank(currentDevice)) {
            throw new CloudRuntimeException(String.format("Unable to determine next disk target for VM [%s].", vmName));
        }
        final char lastChar = currentDevice.charAt(currentDevice.length() - 1);
        final char incrementedChar = (char) (lastChar + 1);
        return currentDevice.substring(0, currentDevice.length() - 1) + incrementedChar;
    }

    private String getXmlForRbdDisk(final KVMStoragePoolManager storagePoolMgr, final PrimaryDataStoreTO volumePool,
            final String volumePath, final String deviceToAttachDiskTo, String cacheMode) {
        final StringBuilder diskBuilder = new StringBuilder();
        diskBuilder.append("\n<disk ");
        diskBuilder.append(" device='disk'");
        diskBuilder.append(" type='network'");
        diskBuilder.append(">\n");

        diskBuilder.append("<driver name='qemu' type='raw'");
        if (StringUtils.isBlank(cacheMode)) {
            cacheMode = "none";
        }
        diskBuilder.append(" cache='").append(cacheMode).append("'/> \n");

        diskBuilder.append("<source ");
        diskBuilder.append(" protocol='rbd'");
        diskBuilder.append(" name='").append(volumePath).append("'");
        diskBuilder.append(">\n");
        if (StringUtils.isNotBlank(volumePool.getHost())) {
            diskBuilder.append("<host name='").append(volumePool.getHost());
            if (volumePool.getPort() != 0) {
                diskBuilder.append("' port='");
                diskBuilder.append(volumePool.getPort());
            }
            diskBuilder.append("'/>\n");
        }
        diskBuilder.append("</source>\n");
        String authUserName = null;
        final KVMStoragePool primaryPool = storagePoolMgr.getStoragePool(volumePool.getPoolType(), volumePool.getUuid());
        if (primaryPool != null) {
            authUserName = primaryPool.getAuthUserName();
        }
        if (StringUtils.isNotBlank(authUserName)) {
            diskBuilder.append("<auth username='").append(authUserName).append("'>\n");
            diskBuilder.append("<secret type='ceph' uuid='").append(volumePool.getUuid()).append("'/>\n");
            diskBuilder.append("</auth>\n");
        }
        diskBuilder.append("<target dev='").append(deviceToAttachDiskTo).append("'");
        diskBuilder.append(" bus='virtio'");
        diskBuilder.append("/>\n");
        diskBuilder.append("</disk>\n");
        return diskBuilder.toString();
    }

    private static final class RbdImageSpec {
        private final String monHosts;
        private final String port;
        private final String pool;
        private final String image;

        private RbdImageSpec(final String monHosts, final String port, final String pool, final String image) {
            this.monHosts = monHosts;
            this.port = port;
            this.pool = pool;
            this.image = image;
        }

        private static RbdImageSpec fromUri(final String uri) {
            if (StringUtils.isBlank(uri) || !uri.startsWith("rbd:")) {
                throw new CloudRuntimeException(String.format("Unsupported RBD URI in metadata: %s", uri));
            }
            final String withoutScheme = uri.substring("rbd:".length());
            final int colonIndex = withoutScheme.indexOf(':');
            final String poolAndImage = colonIndex >= 0 ? withoutScheme.substring(0, colonIndex) : withoutScheme;
            final String options = colonIndex >= 0 ? withoutScheme.substring(colonIndex + 1) : "";
            final int slashIndex = poolAndImage.indexOf('/');
            if (slashIndex < 0) {
                throw new CloudRuntimeException(String.format("Malformed RBD URI in metadata: %s", uri));
            }
            final String pool = poolAndImage.substring(0, slashIndex);
            final String image = poolAndImage.substring(slashIndex + 1);
            String monHosts = "";
            String port = "";
            for (final String option : options.split(":")) {
                if (option.startsWith("mon_host=")) {
                    monHosts = option.substring("mon_host=".length());
                } else if (option.startsWith("port=")) {
                    port = option.substring("port=".length());
                }
            }
            return new RbdImageSpec(monHosts, port, pool, image);
        }

        private String buildTempImageSpec() {
            return pool + "/" + image + "-restore-temp";
        }

        private String buildQemuUri(final String targetImage) {
            final StringBuilder builder = new StringBuilder("rbd:");
            builder.append(targetImage);
            if (StringUtils.isNotBlank(monHosts)) {
                builder.append(":mon_host=").append(monHosts);
            }
            if (StringUtils.isNotBlank(port)) {
                builder.append(":port=").append(port);
            }
            return builder.toString();
        }

        private String buildRbdCommand(final String... tokens) {
            final StringBuilder builder = new StringBuilder("rbd");
            if (StringUtils.isNotBlank(monHosts)) {
                builder.append(" -m ").append(monHosts);
                if (StringUtils.isNotBlank(port)) {
                    builder.append(":").append(port);
                }
            }
            for (final String token : tokens) {
                builder.append(" ").append(token);
            }
            return builder.toString();
        }
    }
}
