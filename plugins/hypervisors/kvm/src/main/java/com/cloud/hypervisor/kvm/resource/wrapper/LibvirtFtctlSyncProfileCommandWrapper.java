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
package com.cloud.hypervisor.kvm.resource.wrapper;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FtctlSyncAnswer;
import com.cloud.agent.api.FtctlSyncProfileCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.commons.lang3.StringUtils;

@ResourceWrapper(handles = FtctlSyncProfileCommand.class)
public class LibvirtFtctlSyncProfileCommandWrapper extends CommandWrapper<FtctlSyncProfileCommand, Answer, LibvirtComputingResource> {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    @Override
    public Answer execute(FtctlSyncProfileCommand command, LibvirtComputingResource serverResource) {
        if (StringUtils.isBlank(command.getVmName()) || StringUtils.isBlank(command.getMode()) || StringUtils.isBlank(command.getPeerUri())) {
            return new FtctlSyncAnswer(command, false, "Missing FTCTL profile sync parameters");
        }

        final long timeout = (long) (command.getWait() > 0 ? command.getWait() : DEFAULT_TIMEOUT_SECONDS) * 1000;
        Script script = new Script("ablestack_vm_ftctl", timeout, logger);
        script.add("config");
        script.add("profile-upsert");
        script.add("--vm", command.getVmName());
        script.add("--mode", command.getMode());
        script.add("--peer", command.getPeerUri());
        if (StringUtils.isNotBlank(command.getProfileName())) {
            script.add("--profile", command.getProfileName());
        }
        if (StringUtils.isNotBlank(command.getBackendMode())) {
            script.add("--backend-mode", command.getBackendMode());
        }
        if (StringUtils.isNotBlank(command.getProvisioningBackend())) {
            script.add("--provisioning-backend", command.getProvisioningBackend());
        }
        if (StringUtils.isNotBlank(command.getProvisioningState())) {
            script.add("--provisioning-state", command.getProvisioningState());
        }
        if (StringUtils.isNotBlank(command.getTargetStorageScope())) {
            script.add("--target-storage-scope", command.getTargetStorageScope());
        }
        String diskMap = resolveDiskMap(command, timeout);
        if (StringUtils.isNotBlank(diskMap)) {
            script.add("--disk-map", diskMap);
        }
        if (StringUtils.isNotBlank(command.getSecondaryVmName())) {
            script.add("--secondary-vm-name", command.getSecondaryVmName());
        }
        if (StringUtils.isNotBlank(command.getFencingPolicy())) {
            script.add("--fencing-policy", command.getFencingPolicy());
        }
        if (StringUtils.isNotBlank(command.getFencingIpmiPrimaryHost())) {
            script.add("--fencing-ipmi-primary-host", command.getFencingIpmiPrimaryHost());
        }
        if (StringUtils.isNotBlank(command.getFencingIpmiPrimaryPort())) {
            script.add("--fencing-ipmi-primary-port", command.getFencingIpmiPrimaryPort());
        }
        if (StringUtils.isNotBlank(command.getFencingIpmiPrimaryUser())) {
            script.add("--fencing-ipmi-primary-user", command.getFencingIpmiPrimaryUser());
        }
        if (StringUtils.isNotBlank(command.getFencingIpmiPrimaryPassword())) {
            script.add("--fencing-ipmi-primary-password", command.getFencingIpmiPrimaryPassword());
        }
        if (StringUtils.isNotBlank(command.getFencingIpmiPrimaryInterface())) {
            script.add("--fencing-ipmi-primary-interface", command.getFencingIpmiPrimaryInterface());
        }
        if (StringUtils.isNotBlank(command.getFencingIpmiSecondaryHost())) {
            script.add("--fencing-ipmi-secondary-host", command.getFencingIpmiSecondaryHost());
        }
        if (StringUtils.isNotBlank(command.getFencingIpmiSecondaryPort())) {
            script.add("--fencing-ipmi-secondary-port", command.getFencingIpmiSecondaryPort());
        }
        if (StringUtils.isNotBlank(command.getFencingIpmiSecondaryUser())) {
            script.add("--fencing-ipmi-secondary-user", command.getFencingIpmiSecondaryUser());
        }
        if (StringUtils.isNotBlank(command.getFencingIpmiSecondaryPassword())) {
            script.add("--fencing-ipmi-secondary-password", command.getFencingIpmiSecondaryPassword());
        }
        if (StringUtils.isNotBlank(command.getFencingIpmiSecondaryInterface())) {
            script.add("--fencing-ipmi-secondary-interface", command.getFencingIpmiSecondaryInterface());
        }
        if (StringUtils.isNotBlank(command.getSecondaryTargetDir())) {
            script.add("--secondary-target-dir", command.getSecondaryTargetDir());
        }
        if (StringUtils.isNotBlank(command.getSecondarySshKeyFile())) {
            script.add("--secondary-ssh-key-file", command.getSecondarySshKeyFile());
        }
        if (StringUtils.isNotBlank(command.getRemoteNbdExportAddr())) {
            script.add("--remote-nbd-export-addr", command.getRemoteNbdExportAddr());
        }
        if (StringUtils.isNotBlank(command.getXcoloProxyEndpoint())) {
            script.add("--xcolo-proxy-endpoint", command.getXcoloProxyEndpoint());
        }
        if (StringUtils.isNotBlank(command.getXcoloNbdEndpoint())) {
            script.add("--xcolo-nbd-endpoint", command.getXcoloNbdEndpoint());
        }
        if (StringUtils.isNotBlank(command.getXcoloMigrateUri())) {
            script.add("--xcolo-migrate-uri", command.getXcoloMigrateUri());
        }
        if (StringUtils.isNotBlank(command.getXcoloMirrorPort())) {
            script.add("--xcolo-mirror-port", command.getXcoloMirrorPort());
        }
        if (StringUtils.isNotBlank(command.getXcoloComparePort())) {
            script.add("--xcolo-compare-port", command.getXcoloComparePort());
        }
        if (StringUtils.isNotBlank(command.getXcoloCompareLocalPort())) {
            script.add("--xcolo-compare-local-port", command.getXcoloCompareLocalPort());
        }
        if (StringUtils.isNotBlank(command.getXcoloCompareOutPort())) {
            script.add("--xcolo-compare-out-port", command.getXcoloCompareOutPort());
        }
        if (StringUtils.isNotBlank(command.getXcoloControlPort())) {
            script.add("--xcolo-control-port", command.getXcoloControlPort());
        }
        script.add("--json");

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        int exitValue = script.getExitValue();

        return new FtctlSyncAnswer(command, exitValue == 0,
                StringUtils.defaultIfBlank(output, exitValue == 0 ? "OK" : "FTCTL profile-upsert failed"),
                exitValue == 0 ? "ok" : "fail", exitValue, output);
    }

    private String resolveDiskMap(FtctlSyncProfileCommand command, long timeout) {
        if (StringUtils.isNotBlank(command.getDiskMap())) {
            return normalizeExplicitDiskMap(command, timeout);
        }
        if (!StringUtils.equalsIgnoreCase(command.getBackendMode(), "shared-blockcopy")) {
            return null;
        }
        if (StringUtils.isAnyBlank(command.getVmName(), command.getSecondaryVmName(), command.getTargetStoragePoolPath())) {
            return null;
        }

        Script script = new Script("bash", timeout, logger);
        script.add("-lc", buildDiskMapScript(command));
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        if (script.getExitValue() != 0) {
            logger.warn("Unable to build FTCTL disk map for VM [{}]: {}", command.getVmName(), output);
            return null;
        }
        return StringUtils.trimToNull(output);
    }

    private String normalizeExplicitDiskMap(FtctlSyncProfileCommand command, long timeout) {
        if (!StringUtils.equalsIgnoreCase(command.getProvisioningBackend(), "cloud-managed")) {
            return command.getDiskMap();
        }

        Script script = new Script("bash", timeout, logger);
        script.add("-lc", buildNormalizeExplicitDiskMapScript(command));
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser);
        String output = StringUtils.trimToNull(LibvirtFtctlWrapperHelper.getOutput(result, parser));
        if (script.getExitValue() != 0 || StringUtils.isBlank(output) || !StringUtils.contains(output, "=")) {
            logger.warn("Unable to normalize FTCTL disk map for VM [{}]; using Cloud-resolved disk map [{}]: {}",
                    command.getVmName(), command.getDiskMap(), output);
            return command.getDiskMap();
        }
        if (!StringUtils.equals(output, command.getDiskMap())) {
            logger.info("Normalized FTCTL disk map for VM [{}] from [{}] to [{}]",
                    command.getVmName(), command.getDiskMap(), output);
        }
        return output;
    }

    private String buildNormalizeExplicitDiskMapScript(FtctlSyncProfileCommand command) {
        return "FTCTL_VM=" + shellQuote(command.getVmName()) +
                " FTCTL_DISK_MAP=" + shellQuote(command.getDiskMap()) +
                " python3 - <<'PY'\n" +
                        "import os\n" +
                        "import subprocess\n" +
                        "import xml.etree.ElementTree as ET\n" +
                        "\n" +
                        "vm = os.environ['FTCTL_VM']\n" +
                        "disk_map = os.environ.get('FTCTL_DISK_MAP', '')\n" +
                        "\n" +
                        "entries = []\n" +
                        "for raw in disk_map.split(';'):\n" +
                        "    raw = raw.strip()\n" +
                        "    if not raw:\n" +
                        "        continue\n" +
                        "    if '=' not in raw:\n" +
                        "        raise SystemExit('invalid_disk_map_entry')\n" +
                        "    key, value = raw.split('=', 1)\n" +
                        "    key = key.strip()\n" +
                        "    value = value.strip()\n" +
                        "    if not key or not value:\n" +
                        "        raise SystemExit('invalid_disk_map_entry')\n" +
                        "    entries.append((key, value))\n" +
                        "\n" +
                        "if not entries:\n" +
                        "    raise SystemExit('empty_disk_map')\n" +
                        "\n" +
                        "xml = subprocess.check_output(['virsh', 'dumpxml', vm], text=True)\n" +
                        "root = ET.fromstring(xml)\n" +
                        "targets = []\n" +
                        "for disk in root.findall('./devices/disk'):\n" +
                        "    if disk.get('device') != 'disk':\n" +
                        "        continue\n" +
                        "    target = disk.find('target')\n" +
                        "    source = disk.find('source')\n" +
                        "    if target is None or source is None:\n" +
                        "        continue\n" +
                        "    dev = target.get('dev')\n" +
                        "    if dev:\n" +
                        "        targets.append(dev)\n" +
                        "\n" +
                        "if not targets:\n" +
                        "    raise SystemExit('empty_domain_disks')\n" +
                        "\n" +
                        "entry_keys = [entry[0] for entry in entries]\n" +
                        "target_set = set(targets)\n" +
                        "if all(key in target_set for key in entry_keys):\n" +
                        "    print(';'.join('%s=%s' % entry for entry in entries))\n" +
                        "    raise SystemExit(0)\n" +
                        "\n" +
                        "if len(entries) != len(targets):\n" +
                        "    raise SystemExit('disk_count_mismatch')\n" +
                        "\n" +
                        "print(';'.join('%s=%s' % (targets[index], entries[index][1]) for index in range(len(entries))))\n" +
                        "PY";
    }

    private String buildDiskMapScript(FtctlSyncProfileCommand command) {
        return "FTCTL_VM=" + shellQuote(command.getVmName()) +
                " FTCTL_SECONDARY_VM=" + shellQuote(command.getSecondaryVmName()) +
                " FTCTL_POOL_PATH=" + shellQuote(command.getTargetStoragePoolPath()) +
                " FTCTL_POOL_TYPE=" + shellQuote(StringUtils.defaultString(command.getTargetStoragePoolType())) +
                " python3 - <<'PY'\n" +
                        "import os\n" +
                        "import posixpath\n" +
                        "import subprocess\n" +
                        "import sys\n" +
                        "import xml.etree.ElementTree as ET\n" +
                        "\n" +
                        "vm = os.environ['FTCTL_VM']\n" +
                        "secondary = os.environ['FTCTL_SECONDARY_VM']\n" +
                        "pool_path = os.environ.get('FTCTL_POOL_PATH', '')\n" +
                        "pool_type = os.environ.get('FTCTL_POOL_TYPE', '')\n" +
                        "\n" +
                        "def pool_name(path):\n" +
                        "    value = (path or '').strip().rstrip('/')\n" +
                        "    if value.startswith('rbd://'):\n" +
                        "        value = value[len('rbd://'):]\n" +
                        "        return value.rsplit('/', 1)[-1] if '/' in value else value\n" +
                        "    return value.rsplit('/', 1)[-1] if '/' in value else value\n" +
                        "\n" +
                        "def source_name(source):\n" +
                        "    if source is None:\n" +
                        "        return ''\n" +
                        "    value = source.get('dev') or source.get('file') or source.get('name') or ''\n" +
                        "    return value.rsplit('/', 1)[-1]\n" +
                        "\n" +
                        "def dest_name(target, source):\n" +
                        "    base = source_name(source)\n" +
                        "    if base.startswith(vm + '-'):\n" +
                        "        return secondary + base[len(vm):]\n" +
                        "    return secondary + '-' + target\n" +
                        "\n" +
                        "xml = subprocess.check_output(['virsh', 'dumpxml', vm], text=True)\n" +
                        "root = ET.fromstring(xml)\n" +
                        "entries = []\n" +
                        "rbd_pool = pool_name(pool_path)\n" +
                        "for disk in root.findall('./devices/disk'):\n" +
                        "    if disk.get('device') != 'disk':\n" +
                        "        continue\n" +
                        "    target = disk.find('target')\n" +
                        "    source = disk.find('source')\n" +
                        "    if target is None or source is None:\n" +
                        "        continue\n" +
                        "    dev = target.get('dev')\n" +
                        "    if not dev:\n" +
                        "        continue\n" +
                        "    image = dest_name(dev, source)\n" +
                        "    if pool_type.lower() == 'rbd' or pool_path.startswith('rbd://'):\n" +
                        "        if not rbd_pool:\n" +
                        "            raise SystemExit('missing_rbd_pool')\n" +
                        "        dest = 'rbd:%s/%s' % (rbd_pool, image)\n" +
                        "    elif pool_path.startswith('/'):\n" +
                        "        dest = posixpath.join(pool_path, secondary, image)\n" +
                        "    else:\n" +
                        "        raise SystemExit('unsupported_pool_path')\n" +
                        "    entries.append('%s=%s' % (dev, dest))\n" +
                        "if not entries:\n" +
                        "    raise SystemExit('empty_disk_map')\n" +
                        "print(';'.join(entries))\n" +
                        "PY";
    }

    private String shellQuote(String value) {
        return "'" + StringUtils.defaultString(value).replace("'", "'\"'\"'") + "'";
    }
}
