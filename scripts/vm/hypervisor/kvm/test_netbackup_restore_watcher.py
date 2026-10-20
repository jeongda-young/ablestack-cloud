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

"""NetBackup restore completion, partial success validation, and callback regressions."""

import importlib.util
import json
import os
from datetime import datetime, timezone
from pathlib import Path
import tempfile
import time
import unittest
from unittest.mock import patch


class NetBackupRestoreWatcherTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name).resolve()
        self.stage = self.base / "stage"
        self.root = self.stage / "i-2-10-VM/2026.10.20.10.00.00.000"
        self.root.mkdir(parents=True)
        config = self.base / "watcher.conf"
        config.write_text(f'NETBACKUP_STAGING_ROOT="{self.stage}"\nLOG_FILE="{self.base}/watcher.log"\nNETBACKUP_CLIENT_NAME="host-test"\n')
        spec = importlib.util.spec_from_file_location("netbackup_restore_watcher", Path(__file__).with_name("netbackup-host-restore-watcher.py"))
        self.watcher = importlib.util.module_from_spec(spec)
        with patch.dict(os.environ, NETBACKUP_WATCHER_CONFIG=str(config)):
            spec.loader.exec_module(self.watcher)
        self.manifest = {
            "version": 1, "provider": "ablestack-netbackup", "backupUuid": "backup-test", "vmName": self.root.parent.name,
            "timestamp": self.root.name, "complete": True, "backupType": "FULL", "volumes": [
                {"uuid": "root-volume", "deviceId": 0, "provisionedBytes": 100, "engine": "QCOW2", "chain": [
                    {"backupUuid": "backup-test", "path": str(self.root / "root.root-volume.qcow2"), "completed": True,
                     "externalId": "host-test_1792458000", "jobId": "100", "sourceHost": "host-test"}]}]}
        self.write_manifest()
        (self.root / "domain-config.xml").write_text(f'<domain><name>{self.root.parent.name}</name><uuid>vm-test</uuid></domain>')
        (self.root / ".staging.complete").write_text("volume_pipeline=true\nbackup_uuid=backup-test\n")
        self.logs = ["Warning bptm: cannot set permissions on " + str(self.root / "domain-config.xml")]
        self.selections = [str(self.root)]

    def write_manifest(self):
        (self.root / "backup-manifest.json").write_text(json.dumps(self.manifest))

    def job(self, code=1, state="DONE", release="10.5"):
        # Jobs API v12 is the common contract used with both supported releases.
        # Exercise JSON number/string decoding without assuming release-specific enums.
        value = code if release == "10.5" or code is None else str(code)
        attrs = {"state": state, "jobType": "RESTORE", "status": value, "destinationClient": "host-test",
                 "startTime": time.time() - 30, "endTime": time.time(), "restoreBackupIDs": ["host-test_1792458000"]}
        if release == "11.1":
            for key in ("startTime", "endTime"):
                attrs[key] = datetime.fromtimestamp(attrs[key], timezone.utc).isoformat().replace("+00:00", "Z")
        return {"id": "200", "type": "job", "attributes": attrs}

    def poll(self, job=None, state=None, logs=None, detail_error=None):
        job = job or self.job()
        state = {} if state is None else state
        def api(path, params=None):
            if path.endswith("/try-logs"):
                if detail_error:
                    raise detail_error
                return {"data": {"attributes": {"log": self.logs if logs is None else logs}}}
            if path.endswith("/file-lists"):
                return {"data": {"attributes": {"fileList": self.selections}}}
            if path.endswith("/200"):
                return {"data": job}
            self.fail("Unexpected API call: " + path)
        with patch.object(self.watcher, "fetch_restore_jobs", return_value=[job]), \
                patch.object(self.watcher, "netbackup_get", side_effect=api), \
                patch.object(self.watcher, "invoke_restore_notify", return_value="submitted") as notify:
            self.watcher.poll_once(state)
        return state, notify

    def test_supported_release_terminal_status_matrix(self):
        for release in ("10.5", "11.1"):
            for code, state, outcome in ((0, "DONE", "SUCCESS"), (1, "DONE", "PARTIAL"), (5, "DONE", "FAILED"),
                                         (0, "ACTIVE", "PENDING"), (1, "INCOMPLETE", "PENDING"),
                                         (0, "SUSPENDED", "PENDING"), (0, "REQUEUED", "PENDING"),
                                         (None, "DONE", "UNKNOWN"), (5, "SUCCESS", "FAILED"),
                                         (0, "FAILED", "FAILED"), (0, "mystery", "UNKNOWN")):
                with self.subTest(release=release, code=code, state=state):
                    self.assertEqual(outcome, self.watcher.restore_job_outcome(self.job(code, state, release)))

    def test_conflicting_status_codes_are_unknown(self):
        job = self.job(0)
        job["statusCode"] = 1
        self.assertEqual("UNKNOWN", self.watcher.restore_job_outcome(job))

    def test_job_timestamps_support_epoch_and_utc_forms_without_local_timezone_assumptions(self):
        expected = datetime(2026, 10, 20, 1, 0, 0, tzinfo=timezone.utc).timestamp()
        for value in (str(int(expected)), str(int(expected * 1000)), str(expected), "2026-10-20T01:00:00Z",
                      "2026-10-20T01:00:00+00:00", "2026-10-20T10:00:00+09:00", "2026-10-20 01:00:00"):
            with self.subTest(value=value):
                self.assertEqual(expected, self.watcher.parse_timestamp(value))
        self.assertAlmostEqual(expected + 0.123456, self.watcher.parse_timestamp("2026-10-20T01:00:00.123456789Z"), places=5)

    def test_success_zero_keeps_the_existing_catalog_callback(self):
        for release in ("10.5", "11.1"):
            with self.subTest(release=release):
                state, notify = self.poll(self.job(0, release=release))
                notify.assert_called_once_with("host-test_1792458000", "200")
                self.assertEqual("0", state["processed"]["200"]["netbackupStatusCode"])

    def test_verified_partial_success_submits_only_the_verified_metadata_directory(self):
        for release in ("10.5", "11.1"):
            with self.subTest(release=release):
                state, notify = self.poll(self.job(release=release))
                notify.assert_called_once_with(str(self.root), "200")
                proof = state["processed"]["200"]["validation"]
                self.assertEqual("VERIFIED", proof["status"])
                self.assertEqual(3, len(proof["requiredFiles"]))
                self.assertEqual(self.logs, proof["details"])

    def test_partial_rbd_metadata_does_not_require_volume_payloads_locally(self):
        self.manifest["volumes"][0]["engine"] = "RBD_DIFF"
        self.manifest["volumes"][0]["chain"][0]["path"] = str(self.root / "root.root-volume.raw")
        self.write_manifest()
        _, notify = self.poll()
        notify.assert_called_once()

    def test_partial_missing_required_files_never_notifies(self):
        for name in ("backup-manifest.json", "domain-config.xml", ".staging.complete"):
            with self.subTest(name=name):
                file = self.root / name
                content = file.read_bytes()
                file.unlink()
                state, notify = self.poll()
                notify.assert_not_called()
                self.assertEqual("PARTIAL_RESTORE_BLOCKED", state["restoreValidation"]["200"]["status"])
                file.write_bytes(content)

    def test_partial_data_error_blocks_even_when_metadata_exists(self):
        for message in ("Error: checksum mismatch", "Error: cannot read domain-config.xml: permission denied",
                        "Warning: failed to restore root.raw", "Error: unexpected restore error",
                        "Warning: cannot set permissions on root.qcow2"):
            with self.subTest(message=message):
                state, notify = self.poll(logs=[message])
                notify.assert_not_called()
                self.assertIn("reason", state["restoreValidation"]["200"])

    def test_permission_warning_does_not_hide_partially_restored_file_contents(self):
        _, notify = self.poll(logs=self.logs + ["INF - TAR PARTIALLY RESTORED 1 FILES"])
        notify.assert_not_called()

    def test_permission_denied_on_file_content_is_not_an_attribute_only_warning(self):
        _, notify = self.poll(logs=["Error: restore failed for backup-manifest.json due to permissions"])
        notify.assert_not_called()

    def test_partial_without_detailed_errors_or_with_unavailable_logs_is_blocked(self):
        for logs in ([], ["INF: restore partially successful (status 1)"]):
            with self.subTest(logs=logs):
                _, notify = self.poll(logs=logs)
                notify.assert_not_called()
        _, notify = self.poll(detail_error=RuntimeError("HTTP 403"))
        notify.assert_not_called()

    def test_partial_diagnostic_file_warning_can_proceed(self):
        _, notify = self.poll(logs=["Error: cannot restore " + str(self.root / "dominfo.xml")])
        notify.assert_called_once()

    def test_netbackup_wrn_attribute_warning_can_proceed(self):
        _, notify = self.poll(logs=["WRN - unable to restore file permissions on " + str(self.root / "domain-config.xml")])
        notify.assert_called_once()

    def test_partial_stale_metadata_is_blocked(self):
        job = self.job()
        job["attributes"]["startTime"] = time.time() + 60
        state, notify = self.poll(job)
        notify.assert_not_called()
        self.assertIn("predates", state["restoreValidation"]["200"]["reason"])

    def test_original_backup_mtime_is_not_mistaken_for_stale_restore_data(self):
        for path in self.root.iterdir():
            os.utime(path, (946684800, 946684800))
        _, notify = self.poll()
        notify.assert_called_once()

    def test_partial_invalid_metadata_content_is_blocked(self):
        path = self.root / "backup-manifest.json"
        for content in ("", "{", "{}", json.dumps(dict(self.manifest, complete=False))):
            with self.subTest(content=content):
                path.write_text(content)
                _, notify = self.poll()
                notify.assert_not_called()

    def test_partial_mismatched_vm_xml_or_completion_marker_is_blocked(self):
        (self.root / "domain-config.xml").write_text("<domain><name>wrong-vm</name><uuid>vm-test</uuid></domain>")
        _, notify = self.poll()
        notify.assert_not_called()
        (self.root / "domain-config.xml").write_text(f'<domain><name>{self.root.parent.name}</name><uuid>vm-test</uuid></domain>')
        (self.root / ".staging.complete").write_text("volume_pipeline=true\nbackup_uuid=other-backup\n")
        _, notify = self.poll()
        notify.assert_not_called()

    def test_partial_symlink_is_not_accepted_as_required_metadata(self):
        file = self.root / "backup-manifest.json"
        target = self.base / "manifest.json"
        file.rename(target)
        file.symlink_to(target)
        _, notify = self.poll()
        notify.assert_not_called()

    def test_partial_selection_of_only_one_metadata_file_is_insufficient(self):
        self.selections = [str(self.root / "backup-manifest.json")]
        _, notify = self.poll()
        notify.assert_not_called()

    def test_partial_with_each_required_file_selected_is_accepted(self):
        self.selections = [str(self.root / name) for name in ("backup-manifest.json", "domain-config.xml", ".staging.complete")]
        _, notify = self.poll()
        notify.assert_called_once()

    def test_failed_job_records_error_and_never_notifies(self):
        for release in ("10.5", "11.1"):
            with self.subTest(release=release):
                state, notify = self.poll(self.job(5, release=release), logs=["Error: restore failed"])
                notify.assert_not_called()
                self.assertEqual("failed", state["processed"]["200"]["status"])
                self.assertEqual("5", state["processed"]["200"]["netbackupStatusCode"])

    def test_active_or_unknown_job_never_notifies(self):
        for code, status in ((0, "ACTIVE"), (0, "QUEUED"), (None, "DONE"), (0, "unexpected")):
            with self.subTest(code=code, status=status):
                _, notify = self.poll(self.job(code, status))
                notify.assert_not_called()

    def test_missing_job_details_do_not_fall_back_to_partial_success(self):
        with patch.object(self.watcher, "fetch_restore_job", side_effect=RuntimeError("HTTP 503")):
            state, notify = self.poll()
        notify.assert_not_called()
        self.assertEqual("WAITING_JOB_DETAILS", state["restoreValidation"]["200"]["status"])

    def test_job_details_must_match_the_exact_job_id(self):
        other_job = self.job()
        other_job["id"] = "201"
        with patch.object(self.watcher, "netbackup_get", return_value={"data": other_job}):
            with self.assertRaises(ValueError):
                self.watcher.fetch_restore_job("200")

    def test_partial_manifest_with_an_unconfirmed_volume_is_blocked(self):
        self.manifest["volumes"][0]["chain"][0]["completed"] = False
        self.write_manifest()
        _, notify = self.poll()
        notify.assert_not_called()

    def test_blocked_partial_job_is_revalidated_and_success_is_not_submitted_twice(self):
        marker = self.root / ".staging.complete"
        marker.unlink()
        state, notify = self.poll()
        notify.assert_not_called()
        marker.write_text("volume_pipeline=true\nbackup_uuid=backup-test\n")
        state, notify = self.poll(state=state)
        notify.assert_called_once()
        _, notify = self.poll(state=state)
        notify.assert_not_called()

    def test_startup_baseline_skips_completed_partial_jobs_without_triggering_them(self):
        state = {}
        with patch.object(self.watcher, "fetch_restore_jobs", return_value=[self.job()]), \
                patch.object(self.watcher, "candidate_restore_identifiers", return_value=[]):
            self.watcher.mark_existing_jobs_processed(state)
        self.assertEqual("initialJobBaseline", state["processed"]["200"]["skipped"])
        _, notify = self.poll(state=state)
        notify.assert_not_called()


if __name__ == "__main__":
    unittest.main()
