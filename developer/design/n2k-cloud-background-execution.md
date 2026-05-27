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
# N2K Cloud Background Execution Alignment

## Goal

ABLESTACK Cloud must run ablestack-n2k with the same user-facing execution model as ablestack-v2k:

- The Cloud API job confirms that the migration task has started.
- Long-running phase progress is reported through the Import VM task list and detail view.
- The top UI job alert disappears after task registration/start, not after phase completion.

Existing CLI and wizard behavior must remain compatible. Operators running the tool manually should still see the migration progress in the terminal unless they explicitly ask for background execution.

## Current Difference

V2K uses a background/fleet handoff for split phase runs. CloudStack receives a successful answer once the runner is started, so the async API job completes quickly.

N2K currently executes `ablestack_n2k run` in the foreground from the KVM agent wrapper. The agent command returns only after phase1 or phase2 finishes, so the CloudStack async job remains running and the UI header alert stays visible for the entire phase.

## Execution Model

### CLI and Wizard

- `ablestack_n2k run` keeps foreground behavior by default.
- `ablestack_n2k wizard`, `migrate`, and `interactive` keep foreground behavior by default.
- `--foreground` explicitly forces foreground execution.
- `--background` explicitly starts a detached worker and returns after the worker has been launched.

This preserves existing automation and operator expectations.

### Cloud

The Cloud KVM wrapper passes `--background` when invoking `ablestack_n2k run`.

The background parent process performs argument validation, resolves the workdir, creates a lightweight runner state file, starts a detached foreground worker, and returns success once the worker is alive.

The worker calls the same `run --foreground` path, so the actual migration pipeline remains shared with CLI/wizard execution.

## State Model

The migration truth source remains:

- `manifest.json`
- `events.log`
- Cloud Import VM task records

The background runner adds only lightweight helper files inside the workdir:

- `runner.json`: state, pid, split, log path, timestamps, and exit code.
- `run-<split>.log`: detached worker stdout/stderr.

Status commands may use runner state only as a fallback while manifest/events are still being initialized.

## Cloud Backend Rules

- Initial N2K phase1 API stores task context and sets `Phase1_In_Progress`, then starts the background worker.
- It must not mark `Phase1_Completed` immediately after worker start.
- N2K phase2 API sets `Phase2_In_Progress`, starts the background worker, and starts the same style of background monitoring used by V2K.
- Phase completion and failure are derived later from status refresh/monitoring, not from the initial command return.

## UI Impact

No custom long-running UI poller is required for N2K. The existing Cloud async job poller can remain because the async job now represents "task started", not "phase completed".

The Import VM task list and detail view remain the source for long-running progress.

## Verification

1. CLI `ablestack_n2k wizard ...` runs foreground and shows progress until completion.
2. CLI `ablestack_n2k run --foreground ...` runs foreground.
3. CLI `ablestack_n2k run --background --split phase1 ...` returns quickly and leaves a running worker state.
4. Cloud N2K phase1 registration returns quickly, the header alert disappears, and the task list shows `Phase1_In_Progress`.
5. Cloud N2K phase1 completion later updates to `Phase1_Completed`.
6. Cloud N2K phase2 registration returns quickly, phase2 action disappears, and the task list shows `Phase2_In_Progress`.
7. Cloud N2K phase2 completion/failure is reflected through the existing task monitor.
