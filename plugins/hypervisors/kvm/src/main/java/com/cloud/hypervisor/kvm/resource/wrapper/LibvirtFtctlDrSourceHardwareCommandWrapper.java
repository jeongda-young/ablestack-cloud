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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.FtctlDrSourceHardwareAnswer;
import com.cloud.agent.api.FtctlDrSourceHardwareCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.JsonObject;

@ResourceWrapper(handles = FtctlDrSourceHardwareCommand.class)
public class LibvirtFtctlDrSourceHardwareCommandWrapper
        extends CommandWrapper<FtctlDrSourceHardwareCommand, Answer, LibvirtComputingResource> {
    private static final long INVENTORY_TIMEOUT_MS = 30000L;
    private static final String INVENTORY_SCRIPT = String.join("\n",
            "import json, os, shutil, subprocess",
            "candidates = [os.environ.get('FTCTL_DR_VMWARE_GOVC_BIN'), os.environ.get('V2K_GOVC_BIN'),",
            " '/usr/share/ablestack/v2k/compat/vsphere80/bin/govc', '/usr/local/bin/govc', '/usr/bin/govc',",
            " shutil.which('govc'), '/usr/local/build/ftctl_selftest/compat/vsphere80/bin/govc']",
            "govc = next((p for p in candidates if p and os.path.isfile(p) and os.access(p, os.X_OK)), None)",
            "if not govc: raise SystemExit('govc binary was not found in the installed compatibility bundle')",
            "vm_ref = os.environ.get('FTCTL_DR_SOURCE_VM_REF', '').strip()",
            "env = os.environ.copy()",
            "try:",
            " out = subprocess.check_output([govc, 'vm.info', '-json', vm_ref], env=env, stderr=subprocess.STDOUT, text=True)",
            "except subprocess.CalledProcessError as exc:",
            " raise SystemExit((exc.output or str(exc)).strip())",
            "root = json.loads(out or '{}')",
            "items = root.get('virtualMachines') or root.get('VirtualMachines') or []",
            "vm = items[0] if items and isinstance(items[0], dict) else root",
            "config = vm.get('config') or vm.get('Config') or {}",
            "hardware = config.get('hardware') or config.get('Hardware') or {}",
            "boot = config.get('bootOptions') or config.get('BootOptions') or {}",
            "devices = hardware.get('device') or hardware.get('Device') or []",
            "def first(*values):",
            " for value in values:",
            "  if value is not None and value != '': return value",
            " return None",
            "def label(device):",
            " info = device.get('deviceInfo') or device.get('DeviceInfo') or {}",
            " return str(first(info.get('label'), info.get('Label'), device.get('label'), '') or '')",
            "def dtype(device):",
            " return str(first(device.get('type'), device.get('_typeName'), device.get('dynamicType'), label(device)) or '')",
            "def controller_kind(device):",
            " value = (dtype(device) + ' ' + label(device)).lower()",
            " if any(x in value for x in ('scsi', 'lsilogic', 'buslogic', 'paravirtual')): return 'scsi'",
            " if 'nvme' in value: return 'nvme'",
            " if any(x in value for x in ('sata', 'ahci')): return 'sata'",
            " if 'ide' in value: return 'ide'",
            " return None",
            "controllers = {}",
            "for device in devices:",
            " kind = controller_kind(device)",
            " key = first(device.get('key'), device.get('Key'))",
            " if kind and key is not None: controllers[str(key)] = kind",
            "disk_controllers = []",
            "for device in devices:",
            " value = (dtype(device) + ' ' + label(device)).lower()",
            " if 'virtualdisk' not in value and 'hard disk' not in value: continue",
            " key = first(device.get('controllerKey'), device.get('ControllerKey'))",
            " disk_controllers.append(controllers.get(str(key), 'scsi'))",
            "firmware = str(first(config.get('firmware'), config.get('Firmware'), '') or '').upper()",
            "secure = first(boot.get('efiSecureBootEnabled'), boot.get('EfiSecureBootEnabled'))",
            "result = {'sourceVmRef': vm_ref, 'firmware': firmware, 'secureBoot': secure,",
            " 'guestId': first(config.get('guestId'), config.get('GuestId')),",
            " 'cpuCount': first(hardware.get('numCPU'), hardware.get('NumCPU')),",
            " 'memoryMiB': first(hardware.get('memoryMB'), hardware.get('MemoryMB')),",
            " 'rootDiskController': disk_controllers[0] if disk_controllers else 'scsi',",
            " 'dataDiskController': disk_controllers[1] if len(disk_controllers) > 1 else (disk_controllers[0] if disk_controllers else 'scsi'),",
            " 'inventorySource': 'VCENTER_GOVC_AGENT'}",
            "print(json.dumps(result, separators=(',', ':')))");

    @Override
    public Answer execute(FtctlDrSourceHardwareCommand command, LibvirtComputingResource serverResource) {
        if (StringUtils.isAnyBlank(command.getEndpoint(), command.getPrincipal(), command.getPassword(), command.getSourceVmRef())) {
            return new FtctlDrSourceHardwareAnswer(command, false,
                    "vCenter endpoint, username, password, and source VM reference are required");
        }
        Script script = new Script("python3", INVENTORY_TIMEOUT_MS, logger);
        script.setAvoidLoggingCommand(true);
        script.add("-c");
        script.add(INVENTORY_SCRIPT);
        Map<String, String> environment = new HashMap<String, String>();
        environment.put("GOVC_URL", command.getEndpoint());
        environment.put("GOVC_USERNAME", command.getPrincipal());
        environment.put("GOVC_PASSWORD", command.getPassword());
        environment.put("GOVC_INSECURE", Boolean.TRUE.equals(command.getTlsVerify()) ? "false" : "true");
        environment.put("FTCTL_DR_SOURCE_VM_REF", command.getSourceVmRef());
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = script.execute(parser, environment);
        String output = LibvirtFtctlWrapperHelper.getOutput(result, parser);
        if (script.getExitValue() != 0) {
            return new FtctlDrSourceHardwareAnswer(command, false,
                    StringUtils.defaultIfBlank(output, "vCenter source hardware inventory failed"));
        }
        JsonObject payload = LibvirtFtctlWrapperHelper.parseJsonObject(output);
        if (payload == null) {
            return new FtctlDrSourceHardwareAnswer(command, false, "vCenter source hardware inventory returned invalid JSON");
        }
        FtctlDrSourceHardwareAnswer answer = new FtctlDrSourceHardwareAnswer(command, true,
                "vCenter source hardware inventory completed");
        answer.setSourceVmRef(getString(payload, "sourceVmRef"));
        answer.setFirmware(getString(payload, "firmware"));
        answer.setSecureBoot(getBoolean(payload, "secureBoot"));
        answer.setGuestId(getString(payload, "guestId"));
        answer.setCpuCount(getInteger(payload, "cpuCount"));
        answer.setMemoryMiB(getLong(payload, "memoryMiB"));
        answer.setRootDiskController(getString(payload, "rootDiskController"));
        answer.setDataDiskController(getString(payload, "dataDiskController"));
        answer.setInventorySource(getString(payload, "inventorySource"));
        return answer;
    }

    private String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    private Boolean getBoolean(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsBoolean() : null;
    }

    private Integer getInteger(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : null;
    }

    private Long getLong(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsLong() : null;
    }
}
