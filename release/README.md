<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# ABLESTACK Release Metadata

The product version is stored in `product-version.properties`. Release
workflows combine it with the selected product branch, UTC build date, release
stage, and prerelease sequence.

Examples:

```text
ablestack-europa + alpha + 1 + 20260810
  -> v4.10.0-Europa-20260810-ALPHA1

ablestack-europa + ga + no sequence + 20260810
  -> v4.10.0-Europa-20260810
```

The workflow generates `release-metadata.json` once and passes the completed
`displayVersion` to downstream builds. Alpha, beta, and RC builds require a
positive sequence and are GitHub prereleases. GA builds reject a prerelease
sequence and are published as regular releases.

The login page already prefixes the configured build version with the product
title. Therefore, UI packages receive a value such as
`v4.10.0-Europa-20260810-ALPHA1`, without another `ABLESTACK` prefix.
