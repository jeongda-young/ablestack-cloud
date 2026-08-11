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

import argparse
import datetime
import json
import os
import re
import tempfile
from pathlib import Path


PRODUCT_VERSION_PATTERN = re.compile(r"^[0-9]+\.[0-9]+\.[0-9]+$")
BRANCH_METADATA = {
    "ablestack-europa": ("Europa", "Mold.Europa"),
    "ablestack-diplo": ("Diplo", "Mold.Diplo"),
}
PRERELEASE_STAGES = {"alpha", "beta", "rc"}


def read_product_version(version_file):
    properties = {}
    for raw_line in Path(version_file).read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        key, separator, value = line.partition("=")
        if not separator:
            raise ValueError(f"Invalid product version property: {raw_line}")
        properties[key.strip()] = value.strip()

    product_version = properties.get("product.version", "")
    if not PRODUCT_VERSION_PATTERN.fullmatch(product_version):
        raise ValueError(
            "product.version must use numeric major.minor.patch format"
        )
    return product_version


def derive_release_metadata(
        product_version, base_branch, stage, stage_number, build_date,
        git_commit="", workflow_run_id=""):
    if base_branch not in BRANCH_METADATA:
        raise ValueError(f"Unsupported base branch: {base_branch}")
    if stage not in PRERELEASE_STAGES | {"ga"}:
        raise ValueError(f"Unsupported release stage: {stage}")

    try:
        datetime.datetime.strptime(build_date, "%Y%m%d")
    except ValueError as error:
        raise ValueError("build date must be a valid YYYYMMDD value") from error

    normalized_number = None
    if stage in PRERELEASE_STAGES:
        try:
            normalized_number = int(stage_number)
        except (TypeError, ValueError) as error:
            raise ValueError(f"{stage} requires a positive release number") from error
        if normalized_number < 1 or str(normalized_number) != str(stage_number):
            raise ValueError(f"{stage} requires a positive release number")
    elif stage_number not in (None, ""):
        raise ValueError("ga must not define a prerelease number")

    codename, brand = BRANCH_METADATA[base_branch]
    display_version = f"v{product_version}-{codename}-{build_date}"
    if normalized_number is not None:
        display_version += f"-{stage.upper()}{normalized_number}"

    return {
        "product": "ABLESTACK",
        "productVersion": product_version,
        "codename": codename,
        "brand": brand,
        "buildDate": build_date,
        "releaseStage": stage,
        "prereleaseNumber": normalized_number,
        "displayVersion": display_version,
        "releaseName": f"ABLESTACK {display_version}",
        "releaseTag": display_version,
        "artifactVersion": display_version.removeprefix("v"),
        "isPrerelease": stage != "ga",
        "gitCommit": git_commit,
        "workflowRunId": workflow_run_id,
    }


def write_json_atomic(output_path, metadata):
    output = Path(output_path)
    output.parent.mkdir(parents=True, exist_ok=True)
    file_descriptor, temporary_path = tempfile.mkstemp(
        prefix=f"{output.name}.", dir=output.parent
    )
    try:
        with os.fdopen(file_descriptor, "w", encoding="utf-8") as stream:
            json.dump(metadata, stream, indent=2, ensure_ascii=True)
            stream.write("\n")
        os.replace(temporary_path, output)
    except Exception:
        if os.path.exists(temporary_path):
            os.unlink(temporary_path)
        raise


def write_github_outputs(output_path, metadata):
    outputs = {
        "brand": metadata["brand"],
        "product_version": metadata["productVersion"],
        "codename": metadata["codename"],
        "build_date": metadata["buildDate"],
        "release_stage": metadata["releaseStage"],
        "prerelease_number": metadata["prereleaseNumber"] or "",
        "display_version": metadata["displayVersion"],
        "release_name": metadata["releaseName"],
        "release_tag": metadata["releaseTag"],
        "artifact_version": metadata["artifactVersion"],
        "is_prerelease": str(metadata["isPrerelease"]).lower(),
    }
    with Path(output_path).open("a", encoding="utf-8") as stream:
        for key, value in outputs.items():
            stream.write(f"{key}={value}\n")


def parse_arguments():
    parser = argparse.ArgumentParser(description="Generate ABLESTACK release metadata")
    parser.add_argument("--version-file", default="release/product-version.properties")
    parser.add_argument("--base-branch", required=True)
    parser.add_argument("--stage", required=True, choices=["alpha", "beta", "rc", "ga"])
    parser.add_argument("--number", default="")
    parser.add_argument("--build-date", default="")
    parser.add_argument("--git-commit", default="")
    parser.add_argument("--workflow-run-id", default="")
    parser.add_argument("--output", required=True)
    parser.add_argument("--github-output")
    return parser.parse_args()


def main():
    arguments = parse_arguments()
    build_date = arguments.build_date or datetime.datetime.now(
        datetime.timezone.utc
    ).strftime("%Y%m%d")
    metadata = derive_release_metadata(
        read_product_version(arguments.version_file),
        arguments.base_branch,
        arguments.stage,
        arguments.number,
        build_date,
        arguments.git_commit,
        arguments.workflow_run_id,
    )
    write_json_atomic(arguments.output, metadata)
    if arguments.github_output:
        write_github_outputs(arguments.github_output, metadata)
    print(metadata["displayVersion"])


if __name__ == "__main__":
    main()
