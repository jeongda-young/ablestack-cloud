#!/usr/bin/env python3
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.

"""Veeam provisioning prompts, persisted VM schedule selection and runtime isolation."""

import os
from pathlib import Path
import pty
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).parent
VEEAM = ROOT / "veeam"


class VmBackupSelectionTest(unittest.TestCase):
    def select(self, mode="", include="", exclude="", interactive=None, **values):
        script = r'''
set -euo pipefail
source "$TEST_LIBRARY"
VM_SCHEDULE_BACKUP="$TEST_MODE"
VM_INCLUDE="$TEST_INCLUDE" VM_EXCLUDE="$TEST_EXCLUDE"
mold_backup_configure_vm_selection
printf 'RESULT:%s|%s|%s\n' "$VM_SCHEDULE_BACKUP" "$VM_INCLUDE" "$VM_EXCLUDE"
'''
        env = dict(os.environ, TEST_LIBRARY=str(VEEAM / "mold-backup.lib.sh"),
                   TEST_MODE=mode, TEST_INCLUDE=include, TEST_EXCLUDE=exclude, **values)
        if interactive is None:
            return subprocess.run(["bash", "-c", script], capture_output=True, text=True, env=env, timeout=10)
        master, slave = pty.openpty()
        try:
            process = subprocess.Popen(["bash", "-c", script], stdin=slave, stdout=subprocess.PIPE,
                                       stderr=subprocess.PIPE, text=True, env=env)
            os.write(master, interactive.encode())
            stdout, stderr = process.communicate(timeout=10)
            return subprocess.CompletedProcess(process.args, process.returncode, stdout, stderr)
        finally:
            os.close(master)
            os.close(slave)

    def test_schedule_choice_skips_include_and_exclude_prompts(self):
        result = self.select(interactive="true\n")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("RESULT:true|*|*", result.stdout)
        self.assertIn("per-VM", result.stderr)
        self.assertNotIn("VM_INCLUDE [", result.stderr)
        self.assertNotIn("VM_EXCLUDE [", result.stderr)

    def test_host_choice_prompts_for_filters_in_order(self):
        result = self.select(interactive="false\ni-2-1-VM\ni-2-2-VM\n")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("RESULT:false|i-2-1-VM|i-2-2-VM", result.stdout)
        self.assertLess(result.stderr.index("per-VM"), result.stderr.index("VM_INCLUDE ["))
        self.assertLess(result.stderr.index("VM_INCLUDE ["), result.stderr.index("VM_EXCLUDE ["))

    def test_noninteractive_defaults_to_host_filters(self):
        result = self.select()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("RESULT:false|*|", result.stdout)

    def test_switching_from_schedules_to_filters_discards_the_sentinel(self):
        result = self.select("false", "*", "*", VM_SCHEDULE_BACKUP_PREVIOUS="true")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("RESULT:false|*|\n", result.stdout)

    def test_explicit_host_filters_are_preserved(self):
        result = self.select("false", "i-2-1-VM", "i-2-2-VM", VM_INCLUDE_CLI_SET="true", VM_EXCLUDE_CLI_SET="true")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("RESULT:false|i-2-1-VM|i-2-2-VM", result.stdout)

    def test_manual_filters_with_schedule_mode_are_rejected(self):
        result = self.select("true", "*", "", VM_INCLUDE_CLI_SET="true")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("require --vm-schedule-backup false", result.stderr)

    def test_both_veeam_tools_reject_invalid_selection_before_writing(self):
        for tool in ("veeam_config.sh", "bootstrap-host-veeam-env.sh"):
            for options in (("--vm-schedule-backup", "invalid"),
                            ("--vm-schedule-backup", "true", "--vm-exclude", "*")):
                with self.subTest(tool=tool, options=options), tempfile.TemporaryDirectory() as directory:
                    destination = Path(directory) / "not-created"
                    result = subprocess.run(["bash", str(VEEAM / tool), *options], capture_output=True, text=True, timeout=10,
                                            env=dict(os.environ, ABLESTACK_VEEAM_ETC_DIR=str(destination),
                                                     MOLD_BACKUP_SHARE_DIR=directory))
                    self.assertNotEqual(0, result.returncode)
                    self.assertIn("--vm-schedule-backup", result.stderr)
                    self.assertFalse(destination.exists())

    def configure_veeam(self, directory, *options):
        # Exercise configuration persistence while isolating unrelated credential encryption dependencies.
        scripts = Path(directory) / "test-tools"
        scripts.mkdir(exist_ok=True)
        for name in ("veeam_config.sh", "mold-backup.lib.sh", "mold-guest-common.sh"):
            shutil.copyfile(VEEAM / name, scripts / name)
        helper = scripts / "mold-backup-secret.sh"
        helper.write_text('''#!/bin/bash
if [[ "$1" == ensure-key ]]; then exit 0; fi
if [[ "$1" == encrypt ]]; then
  while [[ $# -gt 0 ]]; do
    if [[ "$1" == --out ]]; then cat > "$2"; exit 0; fi
    shift
  done
fi
exit 1
''')
        helper.chmod(0o755)
        command = ["bash", str(scripts / "veeam_config.sh"), "--job-name", "test-job",
                   "--mold-url", "http://mold.test/client/api", "--api-key", "synthetic-api-key",
                   "--api-secret", "synthetic-secret", "--zone-id", "zone-test", "--kvm-host", "192.0.2.1",
                   "--secret-key-file", str(Path(directory) / "host.key"), "--no-configure-mold", *options]
        return subprocess.run(command, capture_output=True, text=True, timeout=20,
                              env=dict(os.environ, ABLESTACK_VEEAM_ETC_DIR=directory, MOLD_BACKUP_SHARE_DIR=directory))

    def test_veeam_writes_flag_and_sentinels_into_job_configuration(self):
        with tempfile.TemporaryDirectory() as directory:
            result = self.configure_veeam(directory, "--vm-schedule-backup", "true")
            self.assertEqual(0, result.returncode, result.stderr)
            text = (Path(directory) / "test-job.conf").read_text()
            self.assertIn('VM_SCHEDULE_BACKUP="true"', text)
            self.assertIn('VM_INCLUDE="*"', text)
            self.assertIn('VM_EXCLUDE="*"', text)

    def test_veeam_reconfiguration_to_false_preserves_explicit_empty_exclude(self):
        with tempfile.TemporaryDirectory() as directory:
            result = self.configure_veeam(directory, "--vm-schedule-backup", "true")
            self.assertEqual(0, result.returncode, result.stderr)
            result = self.configure_veeam(directory, "--vm-schedule-backup", "false",
                                          "--vm-include", "i-2-1-VM", "--vm-exclude", "")
            self.assertEqual(0, result.returncode, result.stderr)
            text = (Path(directory) / "test-job.conf").read_text()
            self.assertIn('VM_SCHEDULE_BACKUP="false"', text)
            self.assertIn('VM_INCLUDE="i-2-1-VM"', text)
            self.assertIn('VM_EXCLUDE=""', text)

    def test_runtime_load_does_not_inherit_the_previous_jobs_flag(self):
        for provider in ("netbackup", "veeam"):
            with self.subTest(provider=provider), tempfile.TemporaryDirectory() as directory:
                config = Path(directory) / "job.conf"
                config.write_text('VM_SCHEDULE_BACKUP=true\nVM_INCLUDE="*"\nVM_EXCLUDE="*"\n'
                                  'MOLD_URL="http://mold.test/client/api"\nADMIN_APIKEY="synthetic-key"\n')
                if provider == "netbackup":
                    library = ROOT / "netbackup-host-common.sh"
                    body = r'''
resolve_config_file_path() { echo "$TEST_CONFIG"; }
log() { :; }
load_admin_secretkey() { :; }
load_backup_staging_root_from_mold() { :; }
load_staging_timeout_from_data_operation_config() { :; }
load_policy_schedule_config
'''
                else:
                    library = VEEAM / "mold-backup.lib.sh"
                    body = r'''
mold_backup_resolve_conf_path() { echo "$TEST_CONFIG"; }
mold_backup_notify_log() { :; }
mold_backup_resolve_api_secret() { :; }
mold_backup_supplement_guest_config() { :; }
mold_backup_apply_datadisk_profile() { :; }
mold_backup_load_common_staging() { :; }
mold_backup_load_config
'''
                script = 'set -euo pipefail\nsource "$TEST_LIBRARY"\n' + body
                script += 'echo "FIRST:$VM_SCHEDULE_BACKUP"\nsed "/^VM_SCHEDULE_BACKUP=/d" "$TEST_CONFIG" > "$TEST_CONFIG.next"\nmv "$TEST_CONFIG.next" "$TEST_CONFIG"\n'
                script += body.splitlines()[-1] + '\necho "SECOND:$VM_SCHEDULE_BACKUP"\n'
                result = subprocess.run(["bash", "-c", script], capture_output=True, text=True, timeout=10,
                                        env=dict(os.environ, TEST_LIBRARY=str(library), TEST_CONFIG=str(config)))
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertEqual(["FIRST:true", "SECOND:"], result.stdout.splitlines())


if __name__ == "__main__":
    unittest.main()
