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

import com.cloud.agent.api.AblestackV2KListVmwareVmsAnswer;
import com.cloud.agent.api.AblestackV2KListVmwareVmsCommand;
import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.Gson;
import org.apache.cloudstack.vm.UnmanagedInstanceTO;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ResourceWrapper(handles = AblestackV2KListVmwareVmsCommand.class)
public class LibvirtAblestackV2KListVmwareVmsCommandWrapper extends CommandWrapper<AblestackV2KListVmwareVmsCommand, Answer, LibvirtComputingResource> {

    private static final Gson GSON = GsonHelper.getGson();
    private static final String INVENTORY_SCRIPT = String.join("\n",
            "import json, os, subprocess",
            "govc = os.environ.get('V2K_GOVC_BIN') or '/usr/local/bin/govc'",
            "datacenter = os.environ.get('V2K_DATACENTER', '').strip('/')",
            "instance_name = os.environ.get('V2K_INSTANCE_NAME', '').strip()",
            "keyword = os.environ.get('V2K_KEYWORD', '').strip().lower()",
            "start_index = int(os.environ.get('V2K_START_INDEX') or 0)",
            "page_size_value = os.environ.get('V2K_PAGE_SIZE')",
            "page_size = int(page_size_value) if page_size_value else None",
            "env = os.environ.copy()",
            "def run(args):",
            "    try:",
            "        return subprocess.check_output(args, env=env, stderr=subprocess.STDOUT, text=True)",
            "    except subprocess.CalledProcessError as e:",
            "        raise SystemExit((e.output or str(e)).strip())",
            "def run_optional(args):",
            "    try:",
            "        return subprocess.check_output(args, env=env, stderr=subprocess.STDOUT, text=True)",
            "    except subprocess.CalledProcessError:",
            "        return ''",
            "def first(*values):",
            "    for value in values:",
            "        if value is not None and value != '':",
            "            return value",
            "    return None",
            "def get_value(obj, *keys):",
            "    if not isinstance(obj, dict):",
            "        return None",
            "    for key in keys:",
            "        value = obj.get(key)",
            "        if value is not None and value != '':",
            "            return value",
            "    return None",
            "def power_state(value):",
            "    value = str(value or '').lower()",
            "    if value in ('poweredon', 'on', 'poweron'):",
            "        return 'PowerOn'",
            "    if value in ('poweredoff', 'off', 'poweroff'):",
            "        return 'PowerOff'",
            "    return 'PowerUnknown'",
            "def vm_name_from_path(path):",
            "    return (path or '').rstrip('/').split('/')[-1]",
            "def vm_paths():",
            "    base = '/' + datacenter + '/vm' if datacenter else '/'",
            "    if instance_name:",
            "        if instance_name.startswith('/'):",
            "            return [instance_name]",
            "        found = run([govc, 'find', base, '-type', 'm', '-name', instance_name]).splitlines()",
            "        return found or [instance_name]",
            "    return [line for line in run([govc, 'find', base, '-type', 'm']).splitlines() if line]",
            "def parse_datastore_path(path):",
            "    if not path or not path.startswith('[') or ']' not in path:",
            "        return None, path",
            "    datastore, rest = path[1:].split(']', 1)",
            "    return datastore, rest.strip()",
            "def controller_prefix(controller_type, controller_key):",
            "    ctype = str(controller_type or '').upper()",
            "    if 'SCSI' in ctype or 'LSILOGIC' in ctype or 'BUSLOGIC' in ctype or 'PARAVIRTUAL' in ctype:",
            "        return 'scsi'",
            "    if 'SATA' in ctype:",
            "        return 'sata'",
            "    if 'NVME' in ctype:",
            "        return 'nvme'",
            "    return str(controller_key or 'disk')",
            "def ref_parts(ref, default_type=None):",
            "    if isinstance(ref, dict):",
            "        return first(get_value(ref, 'type', 'Type', '_typeName'), default_type), get_value(ref, 'value', 'Value', 'val', 'Val')",
            "    text = str(ref or '').strip()",
            "    if not text or text == '<nil>':",
            "        return default_type, None",
            "    if ':' in text:",
            "        ref_type, ref_value = text.split(':', 1)",
            "        return ref_type, ref_value",
            "    return default_type, text",
            "def collect_property(ref_type, ref_value, prop):",
            "    if not ref_type or not ref_value:",
            "        return None",
            "    value = run_optional([govc, 'object.collect', '-s', f'{ref_type}:{ref_value}', prop]).strip()",
            "    if not value or value == '<nil>':",
            "        return None",
            "    return value",
            "def cluster_name_from_host(host_type, host_value):",
            "    cluster_name = collect_property(host_type, host_value, 'parent.name')",
            "    if cluster_name:",
            "        return cluster_name",
            "    parent = collect_property(host_type, host_value, 'parent')",
            "    parent_type, parent_value = ref_parts(parent, 'ClusterComputeResource')",
            "    for candidate_type in [parent_type, 'ClusterComputeResource', 'ComputeResource']:",
            "        cluster_name = collect_property(candidate_type, parent_value, 'name')",
            "        if cluster_name:",
            "            return cluster_name",
            "    return None",
            "def build_basic(path):",
            "    return {'name': vm_name_from_path(path), 'path': path, 'powerState': 'PowerUnknown', 'hypervisorType': 'VMware'}",
            "def compute_cpu_speed(summary_runtime, num_cpu):",
            "    try:",
            "        max_cpu_usage = summary_runtime.get('maxCpuUsage')",
            "        if max_cpu_usage and num_cpu:",
            "            return int(int(max_cpu_usage) / int(num_cpu))",
            "    except Exception:",
            "        pass",
            "    return None",
            "def build_detail(path):",
            "    raw_info = run_optional([govc, 'vm.info', '-json', path])",
            "    if not raw_info:",
            "        return build_basic(path)",
            "    info = json.loads(raw_info)",
            "    vm = ((info.get('virtualMachines') or info.get('VirtualMachines') or [{}])[0])",
            "    config = vm.get('config') or {}",
            "    hardware = config.get('hardware') or {}",
            "    runtime = vm.get('runtime') or {}",
            "    summary = vm.get('summary') or {}",
            "    summary_runtime = summary.get('runtime') or {}",
            "    guest = vm.get('guest') or {}",
            "    host_ref_type, host_ref_value = ref_parts(first(runtime.get('host'), summary_runtime.get('host')), 'HostSystem')",
            "    host_name = collect_property(host_ref_type, host_ref_value, 'name')",
            "    cluster_name = cluster_name_from_host(host_ref_type, host_ref_value)",
            "    host_version = collect_property(host_ref_type, host_ref_value, 'config.product.version')",
            "    num_cpu = hardware.get('numCPU')",
            "    instance = build_basic(path)",
            "    instance.update({",
            "        'name': first(vm.get('name'), vm_name_from_path(path)),",
            "        'powerState': power_state(first(runtime.get('powerState'), summary_runtime.get('powerState'))),",
            "        'cpuCores': num_cpu,",
            "        'cpuCoresPerSocket': hardware.get('numCoresPerSocket'),",
            "        'cpuSpeed': compute_cpu_speed(summary_runtime, num_cpu),",
            "        'memory': hardware.get('memoryMB'),",
            "        'operatingSystemId': first(config.get('guestId'), guest.get('guestId')),",
            "        'operatingSystem': first(config.get('guestFullName'), guest.get('guestFullName')),",
            "        'hostName': host_name,",
            "        'clusterName': cluster_name,",
            "        'hostHypervisorVersion': host_version,",
            "        'bootType': config.get('firmware'),",
            "        'bootMode': 'secure' if (config.get('bootOptions') or {}).get('efiSecureBootEnabled') else None",
            "    })",
            "    raw_devices = run_optional([govc, 'device.info', '-json', '-vm', path])",
            "    devices = []",
            "    if raw_devices:",
            "        parsed_devices = json.loads(raw_devices)",
            "        devices = parsed_devices.get('devices') or parsed_devices.get('Devices') or []",
            "    controllers = {device.get('key'): device for device in devices if str(device.get('type') or '').endswith('Controller')}",
            "    disks, nics = [], []",
            "    for device in devices:",
            "        dtype = str(device.get('type') or '')",
            "        info_obj = device.get('deviceInfo') or {}",
            "        if dtype == 'VirtualDisk':",
            "            controller = controllers.get(device.get('controllerKey')) or {}",
            "            controller_type = controller.get('type') or ''",
            "            bus = controller.get('busNumber')",
            "            unit = device.get('unitNumber')",
            "            prefix = controller_prefix(controller_type, device.get('controllerKey'))",
            "            disk_id = f'{prefix}{bus}:{unit}' if bus is not None and unit is not None else str(device.get('key'))",
            "            backing = device.get('backing') or {}",
            "            datastore, datastore_path = parse_datastore_path(backing.get('fileName'))",
            "            disks.append({",
            "                'diskId': disk_id,",
            "                'label': info_obj.get('label') or device.get('name') or disk_id,",
            "                'capacity': first(device.get('capacityInBytes'), (device.get('capacityInKB') or 0) * 1024),",
            "                'controller': controller_type or dtype,",
            "                'controllerUnit': unit,",
            "                'position': len(disks),",
            "                'imagePath': backing.get('fileName'),",
            "                'datastoreName': datastore,",
            "                'datastorePath': datastore_path,",
            "                'datastoreType': 'VMFS'",
            "            })",
            "        elif dtype.startswith('Virtual') and device.get('macAddress'):",
            "            backing = device.get('backing') or {}",
            "            mac = device.get('macAddress')",
            "            ip_addresses = []",
            "            for net in guest.get('net') or []:",
            "                if str(net.get('macAddress') or '').lower() == str(mac).lower():",
            "                    ip_addresses = net.get('ipAddress') or []",
            "                    break",
            "            nics.append({",
            "                'nicId': str(device.get('key')),",
            "                'adapterType': dtype,",
            "                'macAddress': mac,",
            "                'network': first(backing.get('deviceName'), info_obj.get('summary')),",
            "                'ipAddress': ip_addresses",
            "            })",
            "    instance['disks'] = disks",
            "    instance['nics'] = nics",
            "    return instance",
            "def matches_keyword(path):",
            "    if not keyword:",
            "        return True",
            "    lowered_path = str(path or '').lower()",
            "    return keyword in lowered_path or keyword in vm_name_from_path(path).lower()",
            "run([govc, 'about'])",
            "paths = [path for path in vm_paths() if matches_keyword(path)]",
            "total = len(paths)",
            "if instance_name:",
            "    page_paths = paths",
            "elif page_size is None or page_size < 0:",
            "    page_paths = paths[start_index:]",
            "else:",
            "    page_paths = paths[start_index:start_index + page_size]",
            "instances = [build_detail(path) for path in page_paths]",
            "print(json.dumps({'count': total, 'instances': instances}, ensure_ascii=False))");

    @Override
    public Answer execute(AblestackV2KListVmwareVmsCommand command, LibvirtComputingResource serverResource) {
        if (StringUtils.isAnyBlank(command.getVcenter(), command.getDatacenterName(), command.getUsername(), command.getPassword())) {
            return new Answer(command, false, "Missing required parameter(s) for ablestack_v2k VMware inventory: vcenter, datacenterName, username, password");
        }

        Script script = new Script("python3", 300_000L, logger);
        script.setAvoidLoggingCommand(true);
        script.add("-c");
        script.add(INVENTORY_SCRIPT);

        Map<String, String> environment = new HashMap<>();
        environment.put("GOVC_URL", command.getVcenter());
        environment.put("GOVC_USERNAME", command.getUsername());
        environment.put("GOVC_PASSWORD", command.getPassword());
        environment.put("GOVC_INSECURE", "1");
        environment.put("V2K_DATACENTER", command.getDatacenterName());
        environment.put("V2K_INSTANCE_NAME", StringUtils.defaultString(command.getInstanceName()));
        environment.put("V2K_KEYWORD", StringUtils.defaultString(command.getKeyword()));
        if (command.getStartIndex() != null) {
            environment.put("V2K_START_INDEX", command.getStartIndex().toString());
        }
        if (command.getPageSize() != null) {
            environment.put("V2K_PAGE_SIZE", command.getPageSize().toString());
        }

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        script.execute(parser, environment);
        String output = StringUtils.trimToEmpty(parser.getLines());
        if (script.getExitValue() != 0) {
            return new Answer(command, false, StringUtils.defaultIfBlank(output,
                    String.format("ablestack_v2k VMware inventory failed with exit code %d", script.getExitValue())));
        }

        try {
            VmwareInventoryResult result = GSON.fromJson(output, VmwareInventoryResult.class);
            List<UnmanagedInstanceTO> instances = result != null && result.instances != null ? result.instances : Collections.emptyList();
            Integer count = result != null && result.count != null ? result.count : instances.size();
            return new AblestackV2KListVmwareVmsAnswer(command, "VMware inventory listed successfully", instances, count);
        } catch (RuntimeException e) {
            return new Answer(command, false, "Unable to parse ablestack_v2k VMware inventory output: " + e.getMessage());
        }
    }

    private static class VmwareInventoryResult {
        private Integer count;
        private List<UnmanagedInstanceTO> instances;
    }
}
