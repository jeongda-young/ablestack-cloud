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
# Import VM Task Sync Progress Design

## Goal

The VM import task list should show migration progress as an operator-oriented sync status instead of a raw engine status sentence. Phase, state, and workdir are already available in other columns and in the detail drawer, so the list column must focus on current sync work and transferred bytes.

## UI Changes

- Rename the `Description` column to `Sync Progress Status`.
- Swap the `PHASE` and `Current Step` columns so `PHASE` appears first.
- The sync progress column shows only:
  - current sync step label: `Base Sync`, `Incr Sync`, or `Final Sync`
  - current sync transferred bytes: `done / total (percent)`
  - cumulative transferred bytes from base through final when available
- The `Current Step` column shows normalized step labels:
  - `Init`
  - `Base Snap`
  - `Base Sync`
  - `Incr Snap`
  - `Incr Sync`
  - `Guest Shutdown`
  - `Final Snap`
  - `Final Sync`
  - `Initramfs/WinPE`
  - `Migration Completed`

## Engine Status Contract

Both `ablestack_v2k` and `ablestack_n2k` expose structured sync progress in their JSON status output.

```json
{
  "step": "BASE_SYNC",
  "display_step": "Base Sync",
  "sync_progress": {
    "mode": "base",
    "kind": "physical",
    "done_bytes": 10737418240,
    "total_bytes": 21474836480,
    "percent": 50
  },
  "sync_total": {
    "done_bytes": 11811160064,
    "known_total_bytes": 22548578304,
    "percent": 52
  }
}
```

Rules:

- `sync` is scoped to the current sync phase only.
- `sync_total` is cumulative from Base Sync through the current sync phase.
- Incremental/final totals are the changed-region bytes for the active window.
- When a delta total is not known yet, `total_bytes` is `0` and the UI displays `-`.
- Existing string fields such as `SYNC(Physical)` are retained as fallback compatibility only.

## Cloud API Contract

`ImportVMTaskResponse` is extended with:

- `displaystep`
- `syncprogresslabel`
- `syncdonebytes`
- `synctotalbytes`
- `syncpercent`
- `synccumulativedonebytes`
- `synccumulativeknownbytes`
- `synccumulativepercent`

The Cloud agent wrappers prefer engine JSON fields. If they are absent, the server falls back to existing `syncphysical` string parsing.

## Backend Normalization

The backend maps engine-specific steps to a shared display vocabulary:

| Engine step | Display step |
| --- | --- |
| `init`, `preflight`, `inventory`, `prepare-target-storage`, `prepare_target_storage` | `Init` |
| `snapshot.base`, `v3-snapshot-base`, `recovery-point-base`, `BASE_SNAP` | `Base Snap` |
| `sync.base`, `sync-base`, `base_sync`, `BASE_SYNC` | `Base Sync` |
| `snapshot.incr`, `v3-snapshot-incr`, `recovery-point-incr`, `INCR_SNAP` | `Incr Snap` |
| `sync.incr`, `sync-incr`, `incr_sync`, `INCR_SYNC` | `Incr Sync` |
| `shutdown-source`, `shutdown_guest_start` | `Guest Shutdown` |
| `snapshot.final`, `v3-snapshot-final`, `recovery-point-final`, `FINAL_SNAP` | `Final Snap` |
| `sync.final`, `sync-final`, `final_sync`, `FINAL_SYNC` | `Final Sync` |
| `linux_bootstrap`, `initramfs`, `winpe`, `WINPE` | `Initramfs/WinPE` |
| completed import task | `Migration Completed` |

## Verification

1. Running Base Sync shows current Base Sync bytes and cumulative bytes.
2. Running Incr Sync shows changed-region bytes, not whole VM bytes.
3. Running Final Sync shows final changed-region bytes, not whole VM bytes.
4. Completed tasks show `Migration Completed` and preserve the last sync progress in details/events.
5. Legacy tasks without structured sync fields still render a best-effort progress string from `syncphysical`.
