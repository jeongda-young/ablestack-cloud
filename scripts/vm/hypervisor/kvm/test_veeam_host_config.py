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

"""Retries retain remote resources and preserve local originals and credentials."""
import contextlib
import importlib.util
import io
import os
from pathlib import Path
import shlex
import stat
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("veeam_host_config", Path(__file__).with_name("veeam-host-config.py"))
config = importlib.util.module_from_spec(spec)
spec.loader.exec_module(config)


class FakeApi(config.MoldApi):
    def __init__(self, stage):
        self.url = "http://mold.test:8080/client/api"
        self.calls, self.offers = [], []
        self.settings = {
            ("backup.framework.enabled", ""): "true", ("backup.framework.enabled", "zone"): "true",
            ("kvm.incremental.backup", ""): "true", ("kvm.incremental.backup", "cluster"): "true",
            ("kvm.backup.chain.size", ""): "8",
            ("backup.framework.provider.plugin", ""): "ablestack-nas,ablestack-veeam",
            ("backup.framework.provider.plugin", "zone"): "ablestack-netbackup,ablestack-veeam",
            ("backup.plugin.ablestack-veeam.url", "zone"): "https://veeam.test:9398/api/",
            ("backup.plugin.ablestack-veeam.username", "zone"): "administrator",
            ("backup.plugin.ablestack-veeam.password", "zone"): "synthetic-password",
            ("backup.thirdparty.staging.enable", ""): "true",
            ("backup.thirdparty.staging.mount.path", ""): str(stage),
            ("backup.thirdparty.staging.root.path", ""): str(stage / "backups"),
        }
        self.hosts = [{"name": "host-test", "ipaddress": "192.0.2.1", "zoneid": "zone", "clusterid": "cluster"}]

    def call(self, command, **params):
        self.calls.append((command, params))
        if command == "listConfigurations":
            key = (params["name"], params.get("zoneid", params.get("clusterid", "")))
            return {"configuration": [{"name": key[0], "value": self.settings[key]}]} if key in self.settings else {}
        if command == "updateConfiguration":
            self.settings[(params["name"], params.get("zoneid", params.get("clusterid", "")))] = params["value"]
            return {"configuration": {"name": params["name"]}}
        if command == "listHosts":
            return {"host": self.hosts}
        if command == "listBackupOfferings":
            return {"backupoffering": self.offers[:]}
        if command == "importBackupOffering":
            self.offers.append({"name": params["name"], "id": "offering", "provider": config.PROVIDER,
                                "externalid": "veeam", "zoneid": "zone"})
            return {"jobid": "import-job"}
        if command == "queryAsyncJobResult":
            return {"jobstatus": 1}
        raise AssertionError(command)


class VeeamHostConfigTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.locations = {"etc": self.root / "etc", "units": self.root / "units", "ini": self.root / "veeam.ini", "logs": self.root / "logs"}
        self.locations["ini"].write_text("# Agent comment\n[scripts]\ntimeoutPrePost = 60\n[other]\nkeep = yes\n")
        self.api = FakeApi(self.root / "stage")
        self.options = ["--job-name", "Job with spaces", "--mold-url", self.api.url,
                        "--admin-apikey", "synthetic-api-key", "--admin-secretkey", "synthetic-api-secret",
                        "--veeam-host", "veeam.test", "--veeam-password", "synthetic-password",
                        "--kvm-hostname", "host-test", "--vm-schedule-backup", "false", "--vm-exclude", "",
                        "--agent-payload-path", str(self.root / "payload"), "--secret-key-file", str(self.root / "host.key")]

    def run_config(self, options=None, ctl=None):
        with patch.object(config, "paths", return_value=self.locations), patch.object(config, "require_root"), \
                patch.object(config.shutil, "which", return_value="/bin/systemctl"), patch.object(config, "MoldApi", return_value=self.api), \
                patch.object(config.os.path, "ismount", return_value=True), patch.object(config, "systemctl", side_effect=ctl or (lambda *a, **kw: True)), \
                patch.object(config.sys.stdin, "isatty", return_value=False), contextlib.redirect_stderr(io.StringIO()) as output:
            result = config.main(self.options if options is None else options)
        return result, output.getvalue()

    def test_second_run_reuses_remote_resources_and_identical_files(self):
        result, output = self.run_config()
        self.assertEqual(0, result, output)
        self.assertNotIn("Restart the management server", output)
        before = {p: (p.read_bytes(), p.stat().st_mtime_ns) for p in self.root.rglob("*") if p.is_file()}
        self.api.calls.clear()
        result, output = self.run_config()
        self.assertEqual(0, result, output)
        self.assertNotIn("Restart the management server", output)
        self.assertFalse(any(cmd in ("updateConfiguration", "importBackupOffering") for cmd, _ in self.api.calls))
        for path, expected in before.items():
            self.assertEqual(expected, (path.read_bytes(), path.stat().st_mtime_ns), str(path))
        self.assertEqual(1, len(self.api.offers))
        self.assertEqual(1, len(list(self.root.rglob("*.bak.*"))))

    def test_default_parent_payload_follows_common_root_without_changing_offering_or_chain(self):
        options = self.options[:]
        index = options.index("--agent-payload-path")
        del options[index:index + 2]
        result, output = self.run_config(options)
        self.assertEqual(0, result, output)
        conf = self.locations["etc"] / "Job_with_spaces.conf"
        expected = Path(self.api.settings[("backup.thirdparty.staging.root.path", "")]).resolve() / "ablestack-veeam/bootstrap/host-test/Job_with_spaces"
        self.assertIn("VEEAM_AGENT_PAYLOAD_PATH=" + str(expected), conf.read_text())
        self.assertIn("VEEAM_AGENT_PAYLOAD_MODE=COMMON", conf.read_text())
        self.api.settings[("backup.thirdparty.staging.root.path", "")] += "/new-root"
        self.api.calls.clear()
        result, output = self.run_config(options)
        self.assertEqual(0, result, output)
        self.assertIn("/new-root/ablestack-veeam/bootstrap/host-test/Job_with_spaces", conf.read_text())
        self.assertFalse(any(cmd in ("updateConfiguration", "importBackupOffering") for cmd, _ in self.api.calls))

    def test_changed_configuration_is_backed_up_without_reencrypting_unchanged_secret(self):
        self.assertEqual(0, self.run_config()[0])
        conf = self.locations["etc"] / "Job_with_spaces.conf"
        secret = self.locations["etc"] / "secrets/Job_with_spaces.enc"
        original, encrypted = conf.read_bytes(), secret.read_bytes()
        result, output = self.run_config(self.options + ["--vm-include", "i-2-10-VM"])
        self.assertEqual(0, result, output)
        self.assertIn(b"VM_INCLUDE=i-2-10-VM", conf.read_bytes())
        self.assertEqual(original, next(conf.parent.glob(conf.name + ".bak.*")).read_bytes())
        self.assertEqual(encrypted, secret.read_bytes())
        self.assertEqual(0o600, stat.S_IMODE(conf.stat().st_mode))

    def test_failed_atomic_replace_keeps_original_and_retry_succeeds(self):
        target = self.root / "config"
        target.write_text("original")
        files = config.FileInstaller()
        with patch.object(config.os, "replace", side_effect=OSError("injected replacement failure")):
            with self.assertRaises(OSError):
                files.write(target, "changed")
        self.assertEqual("original", target.read_text())
        self.assertEqual("original", next(self.root.glob("config.bak.*")).read_text())
        self.assertFalse(list(self.root.glob(".config.*")))
        self.assertTrue(files.write(target, "changed"))
        self.assertFalse(files.write(target, "changed"))

    def test_failed_agent_restart_is_retried_with_an_already_updated_ini(self):
        def fail_restart(*args, **kwargs):
            if args == ("restart", "veeamservice.service"):
                raise config.ConfigurationError("injected service failure")
            return True
        result, output = self.run_config(ctl=fail_restart)
        self.assertEqual(1, result)
        self.assertIn("activate restore watcher", output)
        pending = self.locations["etc"] / ".agent-restart-required"
        self.assertTrue(pending.exists())
        calls = []
        result, output = self.run_config(ctl=lambda *args, **kwargs: calls.append(args) or True)
        self.assertEqual(0, result, output)
        self.assertIn(("restart", "veeamservice.service"), calls)
        self.assertFalse(pending.exists())
        self.assertEqual(1, len(self.api.offers))

    def test_local_failure_after_offering_creation_retries_without_duplicate(self):
        write = config.FileInstaller.write
        def fail_job(files, path, content, mode=0o600):
            if path.name == "Job_with_spaces.conf":
                raise OSError("injected Job write failure")
            return write(files, path, content, mode)
        with patch.object(config.FileInstaller, "write", new=fail_job):
            result, output = self.run_config()
        self.assertEqual(1, result)
        self.assertIn("write Job configuration", output)
        self.assertEqual(1, len(self.api.offers))
        self.assertEqual(0, self.run_config()[0])
        self.assertEqual(1, len(self.api.offers))

    def test_full_mode_updates_global_and_cluster_without_changing_chain(self):
        result, output = self.run_config(self.options + ["--incremental-backup", "false"])
        self.assertEqual(1, result)
        self.assertIn("Restart the management server", output)
        updates = [params for cmd, params in self.api.calls if cmd == "updateConfiguration"]
        self.assertEqual([{"name": "kvm.incremental.backup", "value": "false"},
                          {"name": "kvm.incremental.backup", "value": "false", "clusterid": "cluster"}], updates)
        self.assertFalse(self.api.offers)
        self.api.calls.clear()
        result, output = self.run_config(self.options + ["--incremental-backup", "false"])
        self.assertEqual(0, result, output)
        self.assertNotIn("Restart the management server", output)
        self.assertFalse(any(cmd == "updateConfiguration" for cmd, _ in self.api.calls))

    def test_provider_lists_preserve_others_in_both_scopes(self):
        self.api.settings[("backup.framework.provider.plugin", "")] = "ablestack-nas"
        self.api.settings[("backup.framework.provider.plugin", "zone")] = "ablestack-netbackup"
        self.assertEqual(0, self.run_config()[0])
        self.assertEqual("ablestack-nas,ablestack-veeam", self.api.settings[("backup.framework.provider.plugin", "")])
        self.assertEqual("ablestack-netbackup,ablestack-veeam", self.api.settings[("backup.framework.provider.plugin", "zone")])

    def test_failed_setting_lookup_stops_before_any_updates(self):
        del self.api.settings[("backup.framework.enabled", "")]
        result, output = self.run_config()
        self.assertEqual(1, result)
        self.assertIn("unavailable", output)
        self.assertFalse(any(cmd in ("updateConfiguration", "importBackupOffering") for cmd, _ in self.api.calls))

    def test_host_search_never_falls_back_to_another_host(self):
        self.api.hosts[0]["name"] = "another-host"
        result, output = self.run_config()
        self.assertEqual(1, result)
        self.assertIn("uniquely match", output)
        self.assertFalse(any(cmd == "updateConfiguration" for cmd, _ in self.api.calls))

    def test_schedule_selection_is_persisted_with_sentinels(self):
        options = self.options[:]
        index = options.index("--vm-exclude")
        del options[index:index + 2]
        options[options.index("--vm-schedule-backup") + 1] = "true"
        result, output = self.run_config(options)
        self.assertEqual(0, result, output)
        values = dict(line.split("=", 1) for line in (self.locations["etc"] / "Job_with_spaces.conf").read_text().splitlines() if "=" in line)
        self.assertEqual(["*"], shlex.split(values["VM_INCLUDE"]))
        self.assertEqual(["*"], shlex.split(values["VM_EXCLUDE"]))
        self.assertEqual("true", values["VM_SCHEDULE_BACKUP"])

    def test_schedule_prompt_skips_filters(self):
        args = config.parse_args(self.options)
        args.vm_schedule_backup = args.vm_include = args.vm_exclude = None
        with patch("builtins.input", return_value="true") as ask:
            config.select_vms(args, interactive=True)
        self.assertEqual(1, ask.call_count)
        self.assertEqual(("*", "*"), (args.vm_include, args.vm_exclude))

    def test_host_prompt_asks_filters_in_order(self):
        args = config.parse_args(self.options)
        args.vm_schedule_backup = args.vm_include = args.vm_exclude = None
        with patch("builtins.input", side_effect=["false", "i-2-1-VM", "i-2-2-VM"]) as ask:
            config.select_vms(args, interactive=True)
        self.assertEqual(3, ask.call_count)
        self.assertEqual(("i-2-1-VM", "i-2-2-VM"), (args.vm_include, args.vm_exclude))

    def test_install_only_installs_without_creating_job_or_enabling_watcher(self):
        calls = []
        result, output = self.run_config(["--install-only", "--secret-key-file", str(self.root / "host.key")],
                                         ctl=lambda *args, **kw: calls.append(args) or True)
        self.assertEqual(0, result, output)
        self.assertFalse(list(self.locations["etc"].glob("*.conf")))
        self.assertFalse(self.api.calls)
        self.assertEqual([("daemon-reload",)], calls)
        self.assertTrue((self.locations["etc"] / "veeam-host-config.py").exists())
        self.assertTrue((self.locations["etc"] / "mold-veeam-restore-agent.service").exists())

    def test_uploaded_wrapper_calls_local_hook_without_arguments(self):
        self.assertEqual(0, self.run_config()[0])
        etc = self.locations["etc"]
        (etc / "hooks/pre-notify.Job_with_spaces").write_text('#!/bin/sh\nprintf "HOOK_OK\\n"\n')
        script = etc / "Job_with_spaces-pre-notify.sh"
        result = subprocess.run(["bash", str(script)], capture_output=True, text=True, timeout=10)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("HOOK_OK\n", result.stdout)
        self.assertNotIn("ssh", script.read_text())

    def test_sanitized_name_collision_is_rejected(self):
        self.assertEqual(0, self.run_config()[0])
        options = self.options[:]
        options[options.index("--job-name") + 1] = "Job/with/spaces"
        result, output = self.run_config(options)
        self.assertEqual(1, result)
        self.assertIn("same filename", output)

    def test_staging_validation_failure_prevents_remote_changes(self):
        with patch.object(config, "staging_root", side_effect=config.ConfigurationError("not mounted")):
            result, output = self.run_config()
        self.assertEqual(1, result)
        self.assertIn("not mounted", output)
        self.assertFalse(any(cmd in ("updateConfiguration", "importBackupOffering") for cmd, _ in self.api.calls))

    def test_agent_timeout_preserves_comments_and_other_sections(self):
        self.assertEqual(0, self.run_config()[0])
        text = self.locations["ini"].read_text()
        self.assertIn("# Agent comment", text)
        self.assertIn("timeoutPrePost = 1800", text)
        self.assertIn("[other]\nkeep = yes", text)

    def test_encrypted_remote_password_is_preserved_unless_explicitly_rotated(self):
        self.api.settings[("backup.plugin.ablestack-veeam.password", "zone")] = "opaque-encrypted-value"
        self.assertEqual(0, self.run_config()[0])
        self.assertFalse(any(cmd == "updateConfiguration" for cmd, _ in self.api.calls))
        self.api.calls.clear()
        result, output = self.run_config(self.options + ["--update-veeam-credentials"])
        self.assertEqual(0, result, output)
        password_updates = [p for cmd, p in self.api.calls if cmd == "updateConfiguration"]
        self.assertEqual([{"name": "backup.plugin.ablestack-veeam.password", "value": "synthetic-password", "zoneid": "zone"}], password_updates)
        self.assertNotIn("synthetic-password", output)
        self.assertNotIn("synthetic-api-secret", output)

    def test_pending_offering_import_is_waited_on_retry_instead_of_resubmitted(self):
        call = self.api.call
        def pending_import(command, **params):
            if command == "importBackupOffering":
                self.api.calls.append((command, params))
                return {"jobid": "pending-import"}
            return call(command, **params)
        with patch.object(self.api, "call", side_effect=pending_import), \
                patch.object(self.api, "wait", side_effect=config.ConfigurationError("import timeout")):
            result, output = self.run_config()
        self.assertEqual(1, result)
        pending = self.locations["etc"] / ".offering-import.json"
        self.assertTrue(pending.exists())
        def finish(job):
            self.assertEqual("pending-import", job)
            self.api.offers.append({"name": "VeeamBackup", "id": "offering", "provider": config.PROVIDER, "externalid": "veeam", "zoneid": "zone"})
        with patch.object(self.api, "wait", side_effect=finish):
            result, output = self.run_config()
        self.assertEqual(0, result, output)
        self.assertFalse(pending.exists())
        self.assertEqual(1, sum(cmd == "importBackupOffering" for cmd, _ in self.api.calls))

    def test_confirmed_import_failure_clears_pending_job_for_retry(self):
        with patch.object(self.api, "wait", side_effect=config.AsyncJobFailed("confirmed failure")):
            result, output = self.run_config()
        self.assertEqual(1, result)
        self.assertIn("confirmed failure", output)
        self.assertFalse((self.locations["etc"] / ".offering-import.json").exists())

    def test_installed_entrypoint_can_load_the_bundled_crypto_helpers(self):
        self.assertEqual(0, self.run_config()[0])
        entry = self.locations["etc"] / "veeam-host-config.py"
        import sys
        result = subprocess.run([sys.executable, str(entry), "--help"], capture_output=True, text=True, timeout=10)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("--vm-schedule-backup", result.stdout)

    def test_unicode_job_configuration_is_resolved_by_the_runtime(self):
        options = self.options[:]
        job = "Mold 테스트 (Lab) @1"
        options[options.index("--job-name") + 1] = job
        result, output = self.run_config(options)
        self.assertEqual(0, result, output)
        etc = self.locations["etc"]
        result = subprocess.run(["bash", "-c", 'source "$TEST_LIBRARY"; mold_backup_resolve_conf_path'],
                                env=dict(os.environ, TEST_LIBRARY=str(etc / "mold-backup.lib.sh"),
                                         ABLESTACK_VEEAM_ETC_DIR=str(etc), VEEAM_JOB_NAME=job),
                                capture_output=True, text=True, timeout=10)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(str(etc / (config.safe_name(job) + ".conf")), result.stdout.strip())

    def test_restore_agent_watches_all_custom_jobs_but_not_backup_copies(self):
        etc = self.locations["etc"]
        etc.mkdir()
        for job in ("Custom Job 1", "Other Job 2"):
            (etc / (config.safe_name(job) + ".conf")).write_text(config.render_config({"VEEAM_JOB_NAME": job}))
        (etc / "old.conf.bak.previous").write_text("VEEAM_JOB_NAME=Unwanted\n")
        (etc / "unrelated.conf").write_text("VM_INCLUDE=\"*\"\n")
        target = self.root / "watched-jobs"
        cli = etc / "mold-backup.sh"
        cli.write_text('#!/bin/sh\nprintf "%s\\n" "$VEEAM_JOB_NAME" >> "$TEST_WATCHED"\n')
        cli.chmod(0o755)
        result = subprocess.run(["bash", str(config.RUNTIME_DIR / "mold-veeam-restore-agent.sh")],
                                env=dict(os.environ, ABLESTACK_VEEAM_ETC_DIR=str(etc), TEST_WATCHED=str(target), VEEAM_JOB_NAME="Inherited Job"),
                                capture_output=True, text=True, timeout=10)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(["Custom Job 1", "Other Job 2"], target.read_text().splitlines())


if __name__ == "__main__":
    unittest.main()
