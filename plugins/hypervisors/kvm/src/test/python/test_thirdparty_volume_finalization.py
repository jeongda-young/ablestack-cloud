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

import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[6] / "scripts/vm/hypervisor/kvm"))
import thirdparty_staging_cleanup as cleanup
import thirdparty_volume_backup as backup


class CompletedStagingCleanupTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name).resolve()
        self.stage = self.base / "stage"
        self.root = self.stage / "ablestack-commvault/i-2-220-VM/2026.10.12.16.55.52.320"
        self.root.mkdir(parents=True)
        self.job = self.base / "control/jobs/backup-1"
        self.job.mkdir(parents=True)
        self.reservations = self.stage / ".volume-reservations"
        self.reservations.mkdir()
        (self.reservations / "filesystem.id").write_text("fs-1")
        (self.reservations / "backup-1.json").write_text("{}")
        (self.root / ".volume-bootstrap").write_text("backup-1")
        (self.root / "root.qcow2").write_bytes(b"volume")
        (self.root / "backup-manifest.json").write_text("{}")
        (self.root / "domain-config.xml").write_text("<domain/>")
        (self.root / ".staging.complete").write_text("complete")
        (self.root / "checkpoints").mkdir()
        (self.root / "checkpoints/current.xml").write_text("<domaincheckpoint/>")
        self.source = self.base / "primary/current-bitmap"
        self.source.parent.mkdir()
        self.source.write_text("incremental baseline")
        manifest = {
            "version": 1, "backupUuid": "backup-1", "provider": "ablestack-commvault",
            "vmName": "i-2-220-VM", "timestamp": self.root.name, "backupType": "FULL",
            "volumes": [{"uuid": "volume-1", "deviceId": 0, "provisionedBytes": 100,
                         "engine": "QCOW2", "chain": [{"backupUuid": "backup-1", "path": str(self.root / "root.qcow2")}]}]
        }
        self.plan = {"manifest": manifest, "stageRoot": str(self.stage), "backupPath": str(self.root)}
        self.manifest = copy.deepcopy(manifest)
        self.manifest["complete"] = True
        artifact = self.manifest["volumes"][0]["chain"][0]
        artifact.update(completed=True, jobId="1127", externalId="1127", sourceHost="ablecube4")
        self.manifest["metadata"] = {"backupUuid": "backup-1", "path": str(self.root), "completed": True,
                                     "jobId": "1129", "externalId": "1129", "sourceHost": "ablecube4"}
        backup.atomic(self.job / "volume-engine.json", {"stageFilesystemId": "fs-1"})
        self.mountinfo = "1 0 0:1 / / rw - ext4 /dev/test rw\n"
        # Exercise the real path/symlink checks with a portable /proc mountinfo fixture.
        def cleanup_path(value):
            if str(value) == "/proc/self/mountinfo":
                return mock.Mock(read_text=lambda: self.mountinfo)
            return Path(value)
        patch = mock.patch.object(cleanup, "Path", side_effect=cleanup_path)
        patch.start()
        self.addCleanup(patch.stop)
        patch = mock.patch.object(backup, "cleanup_source")
        self.source_cleanup = patch.start()
        self.addCleanup(patch.stop)

    def finalize(self, purge=True):
        return backup.finalize_host(self.plan, self.job, self.manifest, purge_staging=purge)

    def test_engine_keeps_metadata_until_controller_authorizes_purge(self):
        self.finalize(purge=False)
        self.assertFalse((self.root / "root.qcow2").exists())
        self.assertTrue((self.root / "backup-manifest.json").exists())
        self.assertTrue((self.root / "checkpoints/current.xml").exists())
        self.source_cleanup.assert_called_once_with(self.plan, self.job, self.job / "volume-engine.json", keep_checkpoint=True)

    def test_controller_removes_every_staged_copy_and_preserves_source_and_other_backup(self):
        other = self.root.parent / "2026.10.12.16.50.00.000"
        other.mkdir()
        (other / "keep.xml").write_text("other backup")
        result = self.finalize()
        self.assertEqual("COMPLETED", result["stagingCleanupState"])
        self.assertFalse(self.root.exists())
        self.assertTrue((other / "keep.xml").exists())
        self.assertEqual("incremental baseline", self.source.read_text())
        self.assertFalse((self.reservations / "backup-1.json").exists())
        self.finalize()  # Lost response/restart does not require deleted metadata.
        self.source_cleanup.assert_called_once()

    def test_legacy_finalization_receipt_does_not_skip_metadata_cleanup(self):
        backup.atomic(self.job / "volume-finalization.json", {"version": 1, "backupUuid": "backup-1", "state": "COMPLETED"})
        self.finalize()
        self.assertFalse(self.root.exists())
        self.assertFalse(self.root.parent.exists())
        self.source_cleanup.assert_not_called()

    def test_partial_directory_deletion_is_retryable_after_ownership_marker_was_removed(self):
        def interrupted_remove(root):
            (root / ".volume-bootstrap").unlink()
            (root / "domain-config.xml").unlink()
            raise OSError("temporary storage failure")
        with mock.patch.object(backup.shutil, "rmtree", side_effect=interrupted_remove):
            with self.assertRaises(OSError):
                self.finalize()
        result = json.loads((self.job / "volume-finalization.json").read_text())
        self.assertEqual("WAITING", result["stagingCleanupState"])
        self.finalize()
        self.assertFalse(self.root.exists())
        self.assertEqual("COMPLETED", json.loads((self.job / "volume-finalization.json").read_text())["stagingCleanupState"])

    def test_unconfirmed_external_metadata_is_never_removed(self):
        self.manifest["metadata"]["completed"] = False
        with self.assertRaisesRegex(RuntimeError, "metadata transfer is not confirmed"):
            self.finalize()
        self.assertTrue((self.root / "root.qcow2").exists())
        self.source_cleanup.assert_not_called()

    def test_wrong_directory_owner_and_changed_filesystem_are_protected(self):
        (self.root / ".volume-bootstrap").write_text("another-backup")
        with self.assertRaisesRegex(RuntimeError, "ownership is unconfirmed"):
            self.finalize()
        (self.root / ".volume-bootstrap").write_text("backup-1")
        (self.reservations / "filesystem.id").write_text("replacement-filesystem")
        with self.assertRaisesRegex(RuntimeError, "filesystem changed"):
            self.finalize()
        self.assertTrue((self.root / "backup-manifest.json").exists())

    def test_symlink_and_nested_mount_are_protected(self):
        self.mountinfo += "2 1 0:2 / %s rw - ext4 /dev/other rw\n" % (self.root / "checkpoints")
        with self.assertRaisesRegex(RuntimeError, "mount point"):
            self.finalize()
        self.mountinfo = self.mountinfo.splitlines()[0] + "\n"
        original = self.root.with_name("protected-data")
        self.root.rename(original)
        self.root.symlink_to(original, target_is_directory=True)
        with self.assertRaisesRegex(RuntimeError, "symbolic link"):
            self.finalize()
        self.assertTrue((original / "backup-manifest.json").exists())


if __name__ == "__main__":
    unittest.main()
