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

"""NetBackup provisioning mode, configuration scope, and input validation regressions."""

import contextlib
import importlib.util
import io
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("netbackup_host_config", Path(__file__).with_name("netbackup-host-config.py"))
config = importlib.util.module_from_spec(spec)
spec.loader.exec_module(config)


class NetBackupConfigurationTest(unittest.TestCase):
    def arguments(self, *options):
        argv = ["netbackup-host-config.py", "--policy-name", "host-test", "--mold-url", "http://mold.test/client/api",
                "--admin-apikey", "synthetic-key", "--admin-secretkey", "synthetic-secret",
                "--netbackup-url", "https://netbackup.test/netbackup", "--netbackup-apikey", "synthetic-netbackup-key"]
        with patch.object(sys, "argv", argv + list(options)):
            return config.parse_args()

    def configure(self, args, current_incremental="true", chain="8", global_incremental=None, global_plugins="ablestack-netbackup", zone_plugins="ablestack-netbackup"):
        if global_incremental is None:
            global_incremental = current_incremental
        values = {"backup.framework.enabled": "true", "kvm.incremental.backup": current_incremental,
                  "kvm.backup.chain.size": chain, "backup.framework.provider.plugin": "ablestack-netbackup",
                  "backup.plugin.netbackup.url": args.netbackup_url, "backup.plugin.netbackup.apikey": args.netbackup_apikey}
        def read_value(name, *args, **kwargs):
            if name == "backup.framework.provider.plugin":
                return zone_plugins if len(args) > 3 and args[3] else global_plugins
            if name == "kvm.incremental.backup" and not kwargs.get("cluster_id"):
                return global_incremental
            return values[name]

        with patch.object(config, "get_configuration_value", side_effect=read_value) as read, \
                patch.object(config, "update_configuration_value") as update, \
                contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            restart = config.ensure_backup_framework_configuration("zone-id", "cluster-id", args)
        return restart, read, update

    def test_provider_registration_preserves_each_scope_and_adds_ablestack_name(self):
        _, _, update = self.configure(self.arguments(), global_plugins="ablestack-nas", zone_plugins="ablestack-veeam")
        calls = [call.args for call in update.call_args_list if call.args[0] == "backup.framework.provider.plugin"]
        self.assertEqual(2, len(calls))
        self.assertEqual(("backup.framework.provider.plugin", "ablestack-nas,ablestack-netbackup"), calls[0][:2])
        self.assertIsNone(calls[0][-1])
        self.assertEqual(("backup.framework.provider.plugin", "ablestack-veeam,ablestack-netbackup"), calls[1][:2])
        self.assertEqual("zone-id", calls[1][-1])

    def test_existing_invocations_default_to_incremental_without_resetting_chain_size(self):
        args = self.arguments()
        self.assertTrue(args.incremental_backup)
        self.assertIsNone(args.backup_chain_size)
        restart, read, update = self.configure(args, chain="25")
        self.assertFalse(restart)
        update.assert_not_called()
        self.assertNotIn("kvm.backup.chain.size", [call.args[0] for call in read.call_args_list])

    def test_interactive_selection_asks_mode_before_host_filters(self):
        args = self.arguments()
        with patch.object(sys.stdin, "isatty", return_value=True), patch("builtins.input", side_effect=["false", "i-2-1-VM", "i-2-2-VM"]) as read:
            config.configure_vm_selection(args)
        self.assertFalse(args.vm_schedule_backup)
        self.assertEqual("i-2-1-VM", args.vm_include)
        self.assertEqual("i-2-2-VM", args.vm_exclude)
        self.assertIn("per-VM", read.call_args_list[0].args[0])
        self.assertIn("VM_INCLUDE", read.call_args_list[1].args[0])
        self.assertIn("VM_EXCLUDE", read.call_args_list[2].args[0])

    def test_schedule_mode_skips_both_filter_prompts(self):
        args = self.arguments()
        with patch.object(sys.stdin, "isatty", return_value=True), patch("builtins.input", return_value="true") as read:
            config.configure_vm_selection(args)
        self.assertTrue(args.vm_schedule_backup)
        self.assertEqual(("*", "*"), (args.vm_include, args.vm_exclude))
        self.assertEqual(1, read.call_count)

    def test_noninteractive_selection_defaults_to_all_host_vms(self):
        args = self.arguments()
        with patch.object(sys.stdin, "isatty", return_value=False), patch("builtins.input") as read:
            config.configure_vm_selection(args)
        self.assertFalse(args.vm_schedule_backup)
        self.assertEqual(("*", ""), (args.vm_include, args.vm_exclude))
        read.assert_not_called()

    def test_explicit_selection_and_filters_support_unattended_configuration(self):
        args = self.arguments("--vm-schedule-backup", "false", "--vm-include", "i-2-1-VM", "--vm-exclude", "i-2-2-VM")
        with patch.object(sys.stdin, "isatty", return_value=True), patch("builtins.input") as read:
            config.configure_vm_selection(args)
        self.assertEqual(("i-2-1-VM", "i-2-2-VM"), (args.vm_include, args.vm_exclude))
        read.assert_not_called()

    def test_schedule_flag_and_sentinels_are_saved_together(self):
        args = self.arguments("--vm-schedule-backup", "true")
        with patch.object(sys.stdin, "isatty", return_value=False):
            config.configure_vm_selection(args)
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "policy.conf"
            config.write_config_file(path, args)
            text = path.read_text()
        self.assertIn("VM_SCHEDULE_BACKUP=true", text)
        self.assertIn('VM_INCLUDE="*"', text)
        self.assertIn('VM_EXCLUDE="*"', text)

    def test_schedule_mode_rejects_manual_filters(self):
        args = self.arguments("--vm-schedule-backup", "true", "--vm-include", "*")
        with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            config.configure_vm_selection(args)

    def test_full_mode_updates_global_and_cluster_flags_and_requires_restart(self):
        args = self.arguments("--incremental-backup", "false")
        restart, read, update = self.configure(args)
        self.assertTrue(restart)
        self.assertEqual(2, update.call_count)
        self.assertEqual([("kvm.incremental.backup", "false"), ("kvm.incremental.backup", "false")],
                         [call.args[:2] for call in update.call_args_list])
        self.assertEqual({}, update.call_args_list[0].kwargs)
        self.assertEqual({"cluster_id": "cluster-id"}, update.call_args_list[1].kwargs)
        self.assertNotIn("kvm.backup.chain.size", [call.args[0] for call in read.call_args_list])

    def test_global_flag_is_updated_even_when_cluster_already_matches(self):
        args = self.arguments("--incremental-backup", "false")
        restart, _, update = self.configure(args, current_incremental="false", global_incremental="true")
        self.assertTrue(restart)
        update.assert_called_once_with("kvm.incremental.backup", "false", args.mold_url, args.admin_apikey,
                                       args.admin_secretkey)

    def test_cluster_override_is_updated_even_when_global_already_matches(self):
        args = self.arguments("--incremental-backup", "false")
        restart, _, update = self.configure(args, current_incremental="true", global_incremental="false")
        self.assertTrue(restart)
        update.assert_called_once_with("kvm.incremental.backup", "false", args.mold_url, args.admin_apikey,
                                       args.admin_secretkey, cluster_id="cluster-id")

    def test_rerunning_full_mode_is_idempotent(self):
        restart, _, update = self.configure(self.arguments("--incremental-backup", "false"), current_incremental="false")
        self.assertFalse(restart)
        update.assert_not_called()

    def test_reenabling_incremental_applies_the_requested_chain_size(self):
        args = self.arguments("--incremental-backup", "true", "--backup-chain-size", "4")
        restart, _, update = self.configure(args, current_incremental="false")
        self.assertTrue(restart)
        self.assertEqual([("kvm.incremental.backup", "true"), ("kvm.incremental.backup", "true"), ("kvm.backup.chain.size", "4")],
                         [call.args[:2] for call in update.call_args_list])
        self.assertNotIn("cluster_id", update.call_args_list[0].kwargs)
        self.assertEqual("cluster-id", update.call_args_list[1].kwargs["cluster_id"])
        self.assertNotIn("cluster_id", update.call_args_list[2].kwargs)

    def test_chain_change_alone_does_not_require_restart(self):
        restart, _, update = self.configure(self.arguments("--backup-chain-size", "6"))
        self.assertFalse(restart)
        self.assertEqual(("kvm.backup.chain.size", "6"), update.call_args.args[:2])

    def test_boolean_input_is_case_insensitive(self):
        self.assertFalse(self.arguments("--incremental-backup", "FALSE").incremental_backup)
        self.assertTrue(self.arguments("--incremental-backup", "True").incremental_backup)

    def test_invalid_boolean_is_rejected_during_parsing(self):
        with contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            self.arguments("--incremental-backup", "invalid")

    def test_full_mode_rejects_chain_option_before_key_or_api_changes(self):
        args = self.arguments("--incremental-backup", "false", "--backup-chain-size", "8")
        with patch.object(config, "ensure_key") as key, patch.object(config, "invoke_mold_api") as api, \
                contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
            config.validate_args(args)
        key.assert_not_called()
        api.assert_not_called()

    def test_nonpositive_chain_size_is_rejected(self):
        for value in ("0", "-1"):
            with self.subTest(value=value), patch.object(config, "ensure_key") as key, \
                    contextlib.redirect_stderr(io.StringIO()), self.assertRaises(SystemExit):
                config.validate_args(self.arguments("--backup-chain-size", value))
            key.assert_not_called()

    def test_full_configuration_never_updates_chain_even_if_validation_is_bypassed(self):
        args = self.arguments("--incremental-backup", "false", "--backup-chain-size", "7")
        _, read, update = self.configure(args, current_incremental="false")
        update.assert_not_called()
        self.assertNotIn("kvm.backup.chain.size", [call.args[0] for call in read.call_args_list])


if __name__ == "__main__":
    unittest.main()
