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
# Import VM Task Action Design

## Goal

ABLESTACK Cloud must operate v2k/n2k migration tasks from the VM import task list. Operators need to continue a failed task, restart from the beginning, cancel an in-progress task, delete task history, clear stored credentials, and execute Phase2 from a single UI action menu.

## Scope

- Source engines: `ablestack_v2k`, `ablestack_n2k`
- Target provider: ABLESTACK Cloud
- UI entry point: `ManageInstances` import task list
- API entry point: `executeImportVmTaskAction`
- Task storage: `import_vm_task`, `import_vm_task_event`, `import_vm_task_credential`

## Implementation Status

The implementation exposes only actions that can be executed safely with the current Cloud task context.

| Action | Status |
| --- | --- |
| `refresh` | Implemented and exposed. |
| `phase2` | Implemented by the existing Phase2 path and exposed from the action dropdown. |
| `cancel` | Implemented and exposed for running v2k/n2k tasks. |
| `delete` | Implemented and exposed for failed, cancelled, and completed tasks. |
| `clearcredentials` | Implemented and exposed for non-running tasks with stored credentials. |
| `resume` | Implemented through the existing v2k/n2k import API with `taskaction=resume`; the KVM wrapper passes the engine global `--resume` before `run`. |
| `retryfromstart` | Implemented through the existing v2k/n2k import API with `taskaction=retryfromstart`; the previous runtime is cleaned up and the same task context is re-entered with a fresh workdir/target map. |

## State Model

Existing task states are extended as follows.

| State | Meaning |
| --- | --- |
| Running | Engine or Cloud-side import workflow is active or waiting for the next phase. |
| Completed | Cloud import workflow completed and the target VM is recorded. |
| Failed | Engine or Cloud-side workflow failed. |
| Cancelling | Operator requested cancellation and cleanup is being attempted. |
| Cancelled | Cleanup/cancel completed or was made safe enough to stop tracking as running. |

Deletion uses the existing `removed` column and is treated as a soft delete. List APIs hide removed tasks by default.

## Actions

| Action | Eligible task state | Behavior |
| --- | --- | --- |
| `refresh` | Any visible task | Re-read status and return the updated task response. |
| `phase2` | Running + `Phase1_Completed` | UI opens confirmation/credential modal and uses the existing Phase2 import path. |
| `resume` | Failed, Cancelled | Reuses the same task context, workdir, target selection, Cloud API context, and encrypted source credential. The engine receives `--resume` before `run`. |
| `retryfromstart` | Failed, Cancelled, Phase1 completed/waiting | Reuses the same task and source/target selections, cleans runtime leftovers, generates a fresh workdir/target map, and restarts from Phase1. |
| `cancel` | Running | Marks task as cancelling, sends engine cleanup/cancel command to the original conversion host, then marks cancelled. |
| `delete` | Failed, Cancelled, Completed | Soft deletes the task, optionally removes stored credentials and workdir. Running delete is rejected unless forced. |
| `clearcredentials` | Non-running with stored credential | Removes encrypted source credentials for the task. |

## API Contract

`executeImportVmTaskAction` accepts cleanup-oriented actions:

- `importvmtaskid`: task UUID
- `action`: `refresh`, `cancel`, `delete`, `clearcredentials`
- `cleanup`: optional boolean; when true, remove workdir/runtime leftovers when the action supports it
- `removecredentials`: optional boolean; when true, remove encrypted credentials with delete/cancel
- `force`: optional boolean; allows risky variants such as deleting a running task

Engine re-entry actions use the existing import APIs so they can keep the async job and target-context behavior of the original v2k/n2k flow:

- `importUnmanagedInstanceForAblestackV2K&importvmtaskid=<uuid>&taskaction=resume`
- `importUnmanagedInstanceForAblestackV2K&importvmtaskid=<uuid>&taskaction=retryfromstart`
- `importUnmanagedInstanceForAblestackN2K&importvmtaskid=<uuid>&taskaction=resume`
- `importUnmanagedInstanceForAblestackN2K&importvmtaskid=<uuid>&taskaction=retryfromstart`

Long-running operations should not keep the UI notification open until conversion ends. The UI registers the async action, closes the confirmation modal, and refreshes the task list; detailed progress remains visible in the task table/detail drawer.

## Engine Cleanup

### v2k

`AblestackV2KCleanupCommand` is introduced for Cloud task actions. It should:

- cleanup by workdir when available
- fallback to domain name for old tasks
- call `ablestack_v2k cleanup --force --keep-workdir|...` where safe
- undefine temporary libvirt domains if requested
- keep cleanup idempotent

### n2k

`AblestackN2KCleanupCommand` is reused and must remain idempotent. Cancel/delete use:

- `keepSourcePoints=true` for cancel or failed cleanup by default
- `removeWorkdir=true` when deleting or explicitly cleaning task artifacts

## UI Contract

All state-changing actions are displayed in the `actions` column only.

- The dropdown button label is `작업 선택`.
- Phase2 is removed from the step column and appears in the dropdown.
- Selecting a menu item never executes immediately.
- A confirmation modal is shown with the action name, target VM, current state, and action impact.
- The API is called only after the operator confirms.
- Destructive actions use danger styling.
- `Details` remains a direct read-only button.

## Implementation Notes

- `availableactions` remains server-calculated and is the UI source of truth.
- Every accepted action writes an `import_vm_task_event`.
- Event payloads must be sanitized and must never include credentials or API secrets.
- Credential reuse relies on `import_vm_task_credential.encrypted_payload`.
- Retry-from-start must create a new workdir and new target disk names. It keeps the task identity so the operator can continue from the same row and event history.

## Verification

1. Failed v2k task exposes resume, retry-from-start, delete, and credential cleanup when eligible.
2. Running v2k/n2k task exposes cancel only as a state-changing action.
3. Phase1-completed v2k/n2k task exposes Phase2 in the dropdown and no button in the step column.
4. Deleted tasks disappear from the default list but remain in DB with `removed`.
5. Cancel/delete cleanup is idempotent.
6. UI confirmation modal is shown before every state-changing action.
