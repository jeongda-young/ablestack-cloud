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

import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).parents[1] / "release_metadata.py"
SPEC = importlib.util.spec_from_file_location("release_metadata", MODULE_PATH)
RELEASE_METADATA = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(RELEASE_METADATA)


class ReleaseMetadataTest(unittest.TestCase):
    def test_alpha_europa_display_version(self):
        metadata = RELEASE_METADATA.derive_release_metadata(
            "4.10.0", "ablestack-europa", "alpha", "1", "20260810"
        )

        self.assertEqual(
            "v4.10.0-Europa-20260810-ALPHA1", metadata["displayVersion"]
        )
        self.assertEqual(
            "ABLESTACK v4.10.0-Europa-20260810-ALPHA1",
            metadata["releaseName"],
        )
        self.assertTrue(metadata["isPrerelease"])

    def test_ga_diplo_omits_stage_suffix(self):
        metadata = RELEASE_METADATA.derive_release_metadata(
            "4.10.0", "ablestack-diplo", "ga", "", "20260810"
        )

        self.assertEqual("v4.10.0-Diplo-20260810", metadata["displayVersion"])
        self.assertFalse(metadata["isPrerelease"])

    def test_prerelease_requires_positive_number(self):
        with self.assertRaisesRegex(ValueError, "positive release number"):
            RELEASE_METADATA.derive_release_metadata(
                "4.10.0", "ablestack-europa", "rc", "", "20260810"
            )

    def test_ga_rejects_prerelease_number(self):
        with self.assertRaisesRegex(ValueError, "must not define"):
            RELEASE_METADATA.derive_release_metadata(
                "4.10.0", "ablestack-europa", "ga", "1", "20260810"
            )

    def test_invalid_calendar_date_is_rejected(self):
        with self.assertRaisesRegex(ValueError, "valid YYYYMMDD"):
            RELEASE_METADATA.derive_release_metadata(
                "4.10.0", "ablestack-europa", "beta", "1", "20260230"
            )


if __name__ == "__main__":
    unittest.main()
