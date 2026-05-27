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

package org.apache.cloudstack.backup.ablestackveeam;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.veeam.VeeamClient;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Ablestack Veeam client: extends the stock Veeam REST/SSH client with NAS seed export helpers.
 */
public class AblestackVeeamClient extends VeeamClient {

    public AblestackVeeamClient(final String url, final Integer version, final String username, final String password,
            final boolean validateCertificate, final int timeout, final int restoreTimeout, final int taskPollInterval,
            final int taskPollMaxRetry) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        super(url, version, username, password, validateCertificate, timeout, restoreTimeout, taskPollInterval, taskPollMaxRetry);
    }

    /**
     * Export all hard disks from a Veeam restore point to a directory on the Veeam server.
     * The directory must be reachable from the KVM hypervisor (shared NFS recommended).
     *
     * @return absolute paths of exported disk files on the Veeam server
     */
    public List<String> exportRestorePointDisksToStaging(final String restorePointId, final String stagingPath) {
        logger.debug(String.format("Exporting Veeam restore point [%s] to staging [%s]", restorePointId, stagingPath));
        final String escapedStaging = stagingPath.replace("'", "''");
        final String escapedId = restorePointId.replace("'", "''");
        final List<String> cmds = Arrays.asList(
                String.format("$staging = '%s'", escapedStaging),
                "New-Item -ItemType Directory -Force -Path $staging | Out-Null",
                String.format("$restorePoint = Get-VBRRestorePoint | Where-Object { $_.Id -eq '%s' -or $_.Id.Guid -eq '%s' }", escapedId, escapedId),
                "if (-not $restorePoint) { Write-Output 'Failed: restore point not found'; Exit 1 }",
                "$session = Start-VBRFLRSession -RestorePoint $restorePoint",
                "$items = Get-VBRFLRItem -Session $session",
                "$exported = @()",
                "foreach ($item in $items) {",
                "  if ($item.Type -eq 'HardDisk') {",
                "    $target = Join-Path $staging ($item.Name + '.vmdk')",
                "    Copy-VBRFLRItem -FLRSession $session -Item $item -Destination $target",
                "    $exported += $target",
                "  }",
                "}",
                "Stop-VBRFLRSession -Session $session",
                "if ($exported.Count -eq 0) { Write-Output 'Failed: no disks exported'; Exit 1 }",
                "$exported -join ','"
        );
        Pair<Boolean, String> result = executePowerShellCommands(cmds);
        if (result == null || !result.first() || StringUtils.isBlank(result.second())) {
            throw new CloudRuntimeException(String.format("Failed to export Veeam restore point [%s] to [%s]", restorePointId, stagingPath));
        }
        if (result.second().contains("Failed:")) {
            throw new CloudRuntimeException(result.second().trim());
        }
        return Arrays.stream(result.second().trim().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * List Veeam restore points for a VM display name (KVM migration; no vCenter hierarchy required).
     */
    public List<Backup.RestorePoint> listRestorePointsForVmDisplayName(final String vmDisplayName) {
        final String escapedName = vmDisplayName.replace("'", "''");
        final List<String> cmds = Arrays.asList(
                String.format("$points = Get-VBRRestorePoint | Where-Object { $_.VmName -eq '%s' -or $_.Name -like '*%s*' }", escapedName, escapedName),
                "if (-not $points) { Exit 0 }",
                "$points | Sort-Object CreationTime -Descending | ForEach-Object {",
                "  Write-Output $_.Id.Guid",
                "  Write-Output $_.CreationTime.ToString('yyyy-MM-ddTHH:mm:ss')",
                "  Write-Output $_.Type",
                "  Write-Output '-----'",
                "}"
        );
        Pair<Boolean, String> response = executePowerShellCommands(cmds);
        if (response == null || !response.first() || StringUtils.isBlank(response.second())) {
            return new ArrayList<>();
        }
        final List<Backup.RestorePoint> restorePoints = new ArrayList<>();
        for (final String block : response.second().split("-----\r\n")) {
            final String[] parts = block.trim().split("\r\n");
            if (parts.length < 3) {
                continue;
            }
            try {
                final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                final Date created = fmt.parse(parts[1].trim());
                restorePoints.add(new Backup.RestorePoint(parts[0].trim(), created, parts[2].trim(), null, null));
            } catch (ParseException e) {
                logger.warn("Skipping unparseable Veeam restore point block: {}", block);
            }
        }
        return restorePoints;
    }
}
