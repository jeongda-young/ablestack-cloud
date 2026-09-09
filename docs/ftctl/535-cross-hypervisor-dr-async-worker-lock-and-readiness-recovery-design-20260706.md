# Cross Hypervisor DR Async Worker Lock And Readiness Recovery Design

작성일: 2026-07-06

대상: Cloud UI, Cloud API, Cloud DR backend, Mold Agent KVM wrapper, ftctl DR runtime, Cloud DB

## 1. 목적

이번 설계는 DR Plan 동기화가 API/Agent 단계에서는 accepted로 보이지만 ftctl background worker가 실제 동기화 및 target materialization 단계로 진입하지 못해 다음 단계 진행 평가가 Fail이 되는 문제를 구조적으로 제거하기 위한 코드 수준 설계다.

핵심 원칙은 다음과 같다.

- UI/API는 장시간 DR 작업 완료를 기다리지 않는다.
- `ACCEPTED`는 "명령 접수"일 뿐이고 `WORKER_RUNNING`, `TARGET_MATERIALIZING`, `TARGET_READY`와 분리한다.
- Cloud는 `target VM`, `target storage`, `restore point`, `durable checkpoint`가 확인되기 전까지 sync run을 성공 처리하지 않는다.
- ftctl worker가 retryable lock에 막히면 숨기지 않고 status JSON과 Cloud DB에 retryable 상태로 남긴다.
- 다음 단계 진행 PASS는 target readiness를 기준으로 판단한다.

## 2. 현재 Fail 원인

관찰 대상:

- Plan UUID: `1c599ac4-728e-4d06-b202-2cc3e6db1247`
- Run UUID: `a43ed545-7678-40f0-9186-81fa1c6a2383`
- 방향: `VMWARE_TO_KVM`
- Target VM name: `Rokcy10-1-dr`

DB 및 ftctl runtime 관찰 결과:

- `dr_plan.state=SYNCING`
- `dr_run.state=ACCEPTED`
- `dr_run.error_code=DR_TARGET_VM_NOT_FOUND`
- `dr_replica.state=SKELETON_READY`
- `dr_replica.target_vm_id=NULL`
- `dr_replica.target_external_ref=NULL`
- `dr_replica_disk.target_volume_id=NULL`
- `dr_restore_point` row 없음
- Cloud `vm_instance`와 `volumes`에 target VM/volume row 없음
- `10.10.32.1`에만 runtime directory 존재
- `status.state`는 `sync-start-accepted`, `progress=1`, `target_materialized=false`, `target_vm_present=false`
- run log에는 다음 retryable lock이 남음

```json
{"command":"dr-sync-start","result":"locked","lock_file":"/run/ablestack-vm-ftctl/lock","holder_command":"dr-sync-start","holder_age_sec":"0","exit_code":20,"retryable":true,"retry_after_sec":2}
```

직접 원인은 ftctl parent action이 `dr-sync-start --wait=false`를 accepted로 기록한 뒤 같은 `dr-sync-start --wait=true` background worker를 실행하지만, worker가 다시 top-level global lock을 잡으려다 같은 명령의 lock에 막히는 구조다. Cloud는 최초 accepted를 기준으로 run을 accepted로 남겼고, projection은 target VM 미생성만 감지했지만 worker lock failure를 retryable run으로 승격하지 못했다.

## 3. 상태 모델

### 3.1 구분해야 하는 상태

| 상태 | 의미 | 소유 레이어 |
| --- | --- | --- |
| API_ACCEPTED | UI/API 요청을 Cloud가 접수함 | Cloud API |
| RUN_QUEUED | `dr_run`이 생성되어 backend worker가 실행 예정 | Cloud backend |
| ENGINE_ACCEPTED | Agent/ftctl이 run/profile을 받아 runtime state를 생성함 | Agent/ftctl |
| WORKER_STARTING | ftctl background worker를 기동 중 | ftctl |
| WORKER_RUNNING | ftctl worker가 plan/run lock을 잡고 실제 sync 수행 중 | ftctl |
| WORKER_RETRYING | worker가 retryable lock/resource busy 때문에 backoff 중 | ftctl/Cloud |
| TARGET_MATERIALIZING | target VM/volume/NIC 또는 restore point 생성 중 | ftctl/Cloud |
| TARGET_READY | target VM/storage/network/restore point/durable checkpoint 확인 완료 | Cloud backend |
| WORKER_STALLED | accepted 이후 worker heartbeat가 갱신되지 않음 | Cloud backend |
| FAILED | non-retryable 또는 retry budget 초과 | Cloud backend |

### 3.2 PASS 판정

다음 단계 진행 PASS는 아래 조건을 모두 만족해야 한다.

- `dr_replica.target_vm_id` 또는 `dr_replica.target_external_ref` 존재
- target VM inventory 존재
- target storage/volume inventory 존재
- required network/NIC inventory 존재
- `dr_restore_point` 또는 ftctl restore point metadata 존재
- `last_target_durable_at` 또는 durable checkpoint 존재
- ftctl status의 `target_materialized=true`
- latest sync run이 `SUCCEEDED`

`ENGINE_ACCEPTED`, `SYNCING`, `sync-start-accepted`, `progress=1`, `progress=100`만으로는 PASS가 아니다.

## 4. UI 설계

수정 대상:

- `ui/src/views/infra/dr/DrPlanList.vue`
- `ui/src/views/infra/dr/DrPlanOverview.vue`
- `ui/src/components/dr/DrRunProgress.vue`
- `ui/src/components/dr/DrActionToolbar.vue`
- `ui/src/components/dr/DrStatusPill.vue`
- `ui/src/api/dr.js`
- `ui/src/utils/dr/*`
- `ui/public/locales/ko_KR.json`
- `ui/public/locales/en.json`

### 4.1 표시 상태

UI는 `plan.state`만 보지 않고 API가 내려주는 readiness/run/worker 정보를 합성한다.

```js
export function normalizeDrPlanDisplayState(plan) {
  const readiness = plan?.readinessstate
  const run = plan?.lastrun || {}
  const workerState = run.workerstate || plan.workerstate

  if (readiness === 'TARGET_READY') return 'READY'
  if (workerState === 'RETRYING' || run.retryable) return 'RETRYING'
  if (workerState === 'STALLED') return 'ATTENTION'
  if (readiness === 'TARGET_MATERIALIZING') return 'MATERIALIZING'
  if (readiness === 'ENGINE_ACCEPTED') return 'ACCEPTED'
  if (readiness === 'FAILED') return 'ERROR'
  return plan?.state || 'UNKNOWN'
}
```

### 4.2 사용자 메시지

| 조건 | 표시 메시지 |
| --- | --- |
| `ENGINE_ACCEPTED` | DR 동기화 요청이 접수되었습니다. 대상 준비 상태를 확인하는 중입니다. |
| `WORKER_STARTING` | DR 엔진 작업자를 시작하는 중입니다. |
| `WORKER_RETRYING` | DR 엔진이 다른 작업을 정리 중입니다. 잠시 후 자동 재시도합니다. |
| `WORKER_STALLED` | DR 엔진 작업자 진행이 멈췄습니다. 재동기화 또는 관리자 확인이 필요합니다. |
| `TARGET_MATERIALIZING` | 대상 가상머신과 복구 지점을 준비하는 중입니다. |
| `DR_TARGET_VM_NOT_FOUND` | 대상 가상머신이 아직 생성되지 않았습니다. |

raw ftctl JSON은 기본 화면에 노출하지 않고 상세 진단/이벤트 영역에서만 펼쳐 볼 수 있게 한다.

### 4.3 Action gating

`DrActionToolbar.vue`는 다음 기준을 적용한다.

```js
function canFailover(plan) {
  return plan.readinessstate === 'TARGET_READY' &&
    plan.targetmaterialized === true &&
    !hasActiveRun(plan)
}

function canStartSync(plan) {
  return ['CONFIG_READY', 'ENGINE_ACCEPTED', 'DEGRADED', 'TARGET_READY', 'FAILED'].includes(plan.readinessstate) &&
    !hasActiveNonRetryingRun(plan)
}

function hasActiveNonRetryingRun(plan) {
  const state = plan?.lastrun?.state
  return ['QUEUED', 'PREPARING', 'DISPATCHING', 'ACCEPTED', 'RUNNING'].includes(state)
}
```

Failover, test failover, failback은 `TARGET_READY` 전에는 비활성화한다. Sync 재시도는 `RETRYING`, `WORKER_STALLED`, `DEGRADED` 상태에서 명시적으로 허용하되 중복 run 생성은 backend active-run serializing으로 한 번 더 막는다.

## 5. API 설계

수정 대상:

- `org/apache/cloudstack/api/response/dr/DrPlanResponse.java`
- `org/apache/cloudstack/api/response/dr/DrRunResponse.java`
- `com/cloud/dr/response/DrResponseGenerator.java`
- `org/apache/cloudstack/api/command/admin/dr/*DrPlan*Cmd.java`
- `ui/src/api/dr.js`

### 5.1 `DrRunResponse` 확장

추가 또는 보강 필드:

```java
@SerializedName("workerstate")
private String workerState;

@SerializedName("workerpid")
private Long workerPid;

@SerializedName("workerlastheartbeat")
private Date workerLastHeartbeat;

@SerializedName("workerexitcode")
private Integer workerExitCode;

@SerializedName("retryable")
private Boolean retryable;

@SerializedName("retryafterseconds")
private Integer retryAfterSeconds;

@SerializedName("nextretryat")
private Date nextRetryAt;

@SerializedName("runtimeupdatedat")
private Date runtimeUpdatedAt;
```

필드는 우선 `dr_run.last_status_json`에서 파싱한다. DB 컬럼 추가 없이 시작하고, 조회 부하가 문제가 되면 cache column을 별도 추가한다.

### 5.2 `DrPlanResponse` readiness 확장

기존 readiness 필드에 worker 상태를 추가한다.

```java
@SerializedName("workerstate")
private String workerState;

@SerializedName("workerstalled")
private Boolean workerStalled;

@SerializedName("readinessstate")
private String readinessState;

@SerializedName("readinessreasoncode")
private String readinessReasonCode;

@SerializedName("readinessmessage")
private String readinessMessage;
```

### 5.3 명령 API 응답 원칙

`startDrPlanSync`, `failoverDrPlan`, `failbackDrPlan` 등은 즉시 작업 완료를 기다리지 않는다. 응답은 아래 의미만 가진다.

```json
{
  "jobid": "cloud-async-job-id",
  "runid": "dr-run-uuid",
  "accepted": true,
  "engineaccepted": false,
  "readinessstate": "RUN_QUEUED"
}
```

Cloud async job success 역시 "DR 완료"가 아니라 "run 생성 및 backend dispatch 시작"이다. UI는 `getDrPlan`, `listDrRuns`, `listDrRunSteps`로 진행 상태를 polling한다.

## 6. Backend 설계

수정 대상:

- `com/cloud/dr/orchestrator/DrRunExecutorImpl.java`
- `com/cloud/dr/adapter/DrAdapterResult.java`
- `com/cloud/dr/adapter/ftctl/FtctlDrUnifiedActionAdapter.java`
- `com/cloud/dr/adapter/ftctl/FtctlDrRuntimeProjectionAdapter.java`
- `com/cloud/dr/DrPlanReadinessValidator.java`
- `com/cloud/dr/DrConstants.java`
- `com/cloud/dr/dao/DrRunDao*.java`
- `com/cloud/dr/dao/DrRunStepDao*.java`

### 6.1 `DrAdapterResult` 보강

현재 `accepted()`는 retry metadata가 없다. accepted와 worker admission을 분리하기 위해 다음 팩토리를 추가한다.

```java
public static DrAdapterResult accepted(String message, String detailsJson, String externalJobRef,
        String workerState, Long workerPid) {
    return new DrAdapterResult(true, null, message, detailsJson, false,
            externalJobRef, false, null, workerState, workerPid);
}

public static DrAdapterResult retryableAccepted(String errorCode, String message, String detailsJson,
        String externalJobRef, Integer retryAfterSeconds) {
    return new DrAdapterResult(true, errorCode, message, detailsJson, false,
            externalJobRef, true, retryAfterSeconds, "RETRYING", null);
}
```

기존 public API와 충돌하지 않도록 기존 constructor는 private overload를 유지한다.

### 6.2 `DrRunExecutorImpl.acceptRun`

accepted는 terminal이 아니며 retry state를 무조건 지우면 안 된다.

TO-BE:

```java
private void acceptRun(DrPlanVO plan, DrRunVO run, DrAdapterResult result) {
    recordStep(run.getId(), STEP_DISPATCH, STEP_ORDER_DISPATCH, SUCCEEDED, 60, result.getDetailsJson(), null, null);
    recordStep(run.getId(), STEP_AGENT_ACCEPT, STEP_ORDER_AGENT_ACCEPT, SUCCEEDED, 70, result.getDetailsJson(), null, null);

    run.setState(result.isRetryable() ? RUN_STATE_RETRYING : RUN_STATE_ACCEPTED);
    run.setCurrentStepName(result.isRetryable() ? "worker-retry-wait" : "agent-accepted");
    run.setExternalJobRef(result.getExternalJobRef());
    run.setEngineAccepted(!result.isRetryable());
    run.setRetryable(result.isRetryable());
    run.setRetryAfterSeconds(result.getRetryAfterSeconds());
    run.setNextRetryAt(result.isRetryable() ? computeNextRetryAt(result) : null);
    run.setProjectionState(result.isRetryable() ? "worker-retrying" : "accepted");
    run.setLastStatusJson(result.getDetailsJson());
    drRunDao.update(run.getId(), run);

    if (result.isRetryable()) {
        scheduleRetry(run.getId(), result.getRetryAfterSeconds());
    } else {
        markPlanAccepted(plan, run);
        refreshProjection(plan.getId());
    }
}
```

### 6.3 Active run 직렬화

동일 plan에 대해 active run이 있으면 새 run을 만들지 않는다.

```java
private void assertNoActiveRun(long planId, String requestedRunType) {
    DrRunVO active = drRunDao.findActiveByPlanId(planId);
    if (active != null && !isRetryOverrideAllowed(active, requestedRunType)) {
        throw new CloudRuntimeException("DR plan already has active run " + active.getUuid());
    }
}
```

허용되는 예외는 `RETRYING` 상태의 동일 action을 내부 scheduler가 재큐잉하는 경우뿐이다.

### 6.4 `FtctlDrUnifiedActionAdapter`

Agent action accepted 직후 동기화 완료를 기다리지는 않지만, worker admission 상태는 bounded probe로 확인한다.

```java
private DrAdapterResult toAdapterResult(...) {
    FtctlDrActionAnswer actionAnswer = (FtctlDrActionAnswer) answer;
    JsonObject payload = parseObject(actionAnswer.getOutput());

    if (isRetryableLock(payload)) {
        return DrAdapterResult.retryable(ERROR_ENGINE_BUSY_RETRYABLE, buildBusyMessage(payload),
                detailsJson, integerValue(payload, "retry_after_sec"));
    }

    if (isAccepted(actionAnswer, payload)) {
        JsonObject admission = probeWorkerAdmission(context, hostId, payload);
        if (isRetryableLock(admission)) {
            return DrAdapterResult.retryableAccepted(ERROR_ENGINE_BUSY_RETRYABLE, buildBusyMessage(admission),
                    GSON.toJson(detailsWithAdmission), externalJobRef, integerValue(admission, "retry_after_sec"));
        }
        return DrAdapterResult.accepted(message, GSON.toJson(detailsWithAdmission),
                externalJobRef, stringValue(admission, "worker_state"), longValue(admission, "worker_pid"));
    }
}
```

`probeWorkerAdmission`은 최대 2초 안에 `dr-status`를 1~2회 호출한다. target materialization을 기다리는 probe가 아니라 worker가 상태 파일을 갱신했는지 보는 짧은 admission probe다.

### 6.5 `FtctlDrRuntimeProjectionAdapter`

projection은 다음 순서로 판정한다.

```java
private ProjectionDecision classifySyncProjection(DrPlanVO plan, DrRunVO run, FtctlDrStatusAnswer status) {
    JsonObject runtime = parseObject(status.getStatusJson());

    if (isRetryableRuntimeBusy(status, runtime)) {
        return ProjectionDecision.retryable(ERROR_ENGINE_BUSY_RETRYABLE,
                retryAfter(runtime), status.getStatusJson());
    }

    if (isAcceptedButWorkerMissing(run, status, runtime)) {
        if (isAcceptedStale(run, status, runtime)) {
            return ProjectionDecision.retryable(ERROR_ENGINE_WORKER_STALLED,
                    DEFAULT_WORKER_RETRY_SECONDS, status.getStatusJson());
        }
        return ProjectionDecision.pending("WORKER_STARTING", status.getStatusJson());
    }

    if (!isSyncTargetReady(plan, status, runtime)) {
        return ProjectionDecision.pending(ERROR_TARGET_VM_NOT_FOUND, status.getStatusJson());
    }

    return ProjectionDecision.succeeded(status.getStatusJson());
}
```

stale 기준:

```java
private boolean isAcceptedStale(DrRunVO run, FtctlDrStatusAnswer status, JsonObject runtime) {
    if (!StringUtils.equals(run.getState(), RUN_STATE_ACCEPTED)) {
        return false;
    }
    if (longValue(runtime, "worker_pid") != null) {
        return false;
    }
    Date runtimeUpdatedAt = parseIsoDate(stringValue(runtime, "updated_at"));
    Date base = runtimeUpdatedAt != null ? runtimeUpdatedAt : run.getAcceptedAt();
    return base != null && System.currentTimeMillis() - base.getTime() > acceptedWorkerStallMillis();
}
```

retryable projection은 `DrRunExecutorImpl.retryRun()`과 같은 code path를 사용한다. projection이 retry 상태를 만들 때도 open run steps를 `RETRYING`으로 닫고 `retry-wait` step을 생성한다.

## 7. Agent 설계

수정 대상:

- `LibvirtFtctlDrActionCommandWrapper.java`
- `LibvirtFtctlDrStatusCommandWrapper.java`
- `LibvirtFtctlDrCommandHelper.java`
- `FtctlDrActionAnswer.java`
- `FtctlDrStatusAnswer.java`

### 7.1 Action wrapper

`LibvirtFtctlDrActionCommandWrapper.executeFtctl()`는 accepted 응답을 그대로 성공으로 보존하되, 다음 필드를 payload에서 추출해 answer/details에 담는다.

- `worker_pid`
- `worker_state`
- `retryable`
- `retry_after_sec`
- `holder_command`
- `holder_pid`
- `lock_scope`
- `run_exists`
- `updated_at`

`probeAcceptedStatus()`는 `accepted=true`뿐 아니라 retryable lock도 반환할 수 있어야 한다.

```java
if (isRetryableLock(payload)) {
    return new FtctlDrActionAnswer(command, false, output, action, planUuid, runUuid,
            "locked", false, state, step, progress, externalJobRef, eventsOffset,
            ERROR_ENGINE_BUSY_RETRYABLE, exitValue, output, payload.toString());
}
```

### 7.2 Status wrapper

`dr-status`는 이미 `timeout --kill-after`로 bounded 실행한다. 다음 필드를 추가로 `FtctlDrStatusAnswer`에 매핑한다.

- `worker_pid`
- `worker_state`
- `worker_started_at`
- `worker_updated_at`
- `worker_exit_code`
- `retryable`
- `retry_after_sec`
- `run_exists`
- `runtime_exists`
- `profile_exists`

필드 추가가 부담되면 1차 구현은 `statusJson`에 보존하고 backend가 JSON에서 직접 파싱한다.

## 8. ftctl 설계

수정 대상:

- `lib/ftctl/libvirt_wrap.sh`
- `lib/ftctl/dr_runtime.sh`
- `lib/ftctl/dr_scheduler.sh`
- `bin/ablestack_vm_ftctl.sh`

### 8.1 lock scope 분리

현재 `ftctl_command_requires_lock()`는 대부분의 명령에 global lock을 적용한다. DR async worker는 global lock이 아니라 plan lock을 사용해야 한다.

TO-BE:

```bash
ftctl_lock_path_for_command() {
  local command="${1-}" vm="${2-}" plan="${CLI_PLAN:-}"
  case "${command}" in
    dr-sync-worker|dr-failover-worker|dr-failback-worker|dr-reprotect-worker)
      printf '%s/dr-runtime/locks/%s.lock\n' "${FTCTL_RUN_DIR}" "$(ftctl_state_vm_key "${plan}")"
      ;;
    dr-status)
      return 1
      ;;
    *)
      ...
      ;;
  esac
}
```

`dr-status`는 lock-free read-only 명령으로 유지한다.

### 8.2 parent action과 worker command 분리

현재 `ftctl_dr_runtime_start_background_worker()`는 같은 action을 `--wait=true`로 다시 실행한다. 이를 전용 worker command로 분리한다.

```bash
ftctl_dr_runtime_start_background_worker() {
  local action="$1" plan="$2" run="$3" role="$4"
  local worker_action

  worker_action="$(ftctl_dr_runtime_worker_action_for "${action}")"
  ftctl_dr_runtime_path_set "${run_path}" \
    "worker_state=STARTING" \
    "worker_pid=" \
    "retryable=false" \
    "updated_at=$(ftctl_now_iso8601)"
  cp -f "${run_path}" "${status_path}"

  (
    export FTCTL_DR_RUNTIME_WORKER=1
    exec nohup ablestack_vm_ftctl "${worker_action}" \
      --plan "${plan}" \
      --run "${run}" \
      --profile-json "${profile_path}" \
      --role "${role:-coordinator}" \
      --json >>"${log_path}" 2>&1
  ) >/dev/null 2>&1 &

  local pid=$!
  ftctl_dr_runtime_path_set "${run_path}" "worker_pid=${pid}" "worker_state=STARTING"
  cp -f "${run_path}" "${status_path}"
}
```

전용 worker command 예:

- `dr-sync-worker`
- `dr-test-failover-worker`
- `dr-failover-worker`
- `dr-failback-worker`
- `dr-reprotect-worker`

worker command는 top-level accepted path를 다시 타지 않고 실제 driver/scheduler path만 실행한다.

### 8.3 parent lock release

전용 worker command를 추가하지 않는 1차 안전 패치로는 delegation branch에서 worker spawn 전에 top-level lock을 명시적으로 release한다.

```bash
if ftctl_dr_runtime_should_delegate_action ...; then
  cp -f "${run_path}" "${status_path}"
  ftctl_lock_release || true
  ftctl_dr_runtime_start_background_worker ...
  emit accepted
fi
```

단, 이 방식은 같은 CLI action 재진입 구조가 남기 때문에 최종 구조는 8.2의 worker command 분리다.

### 8.4 retryable lock persistence

worker가 lock을 얻지 못하면 로그에만 남기지 말고 run/status state를 갱신한다.

```bash
ftctl_dr_runtime_mark_worker_retrying() {
  ftctl_dr_runtime_path_set "${run_path}" \
    "worker_state=RETRYING" \
    "state=SYNCING" \
    "step=worker-lock-retry" \
    "progress=1" \
    "retryable=true" \
    "retry_after_sec=${retry_after}" \
    "error_code=DR_ENGINE_BUSY_RETRYABLE" \
    "holder_command=${holder_command}" \
    "updated_at=$(ftctl_now_iso8601)"
  cp -f "${run_path}" "${status_path}"
}
```

retry budget 초과 시:

```text
worker_state=FAILED
state=ERROR
step=worker-lock-timeout
error_code=DR_ENGINE_BUSY_TIMEOUT
accepted=false
retryable=false
```

### 8.5 status JSON 보강

`ftctl_dr_runtime_emit_state_json()`는 다음 필드를 항상 출력한다.

```json
{
  "worker_state": "STARTING|RUNNING|RETRYING|FAILED|SUCCEEDED",
  "worker_pid": 12345,
  "worker_exit_code": 20,
  "worker_started_at": "2026-07-06T17:52:24+09:00",
  "worker_updated_at": "2026-07-06T17:52:26+09:00",
  "retryable": true,
  "retry_after_sec": 2,
  "lock_scope": "plan",
  "holder_command": "dr-sync-start",
  "run_exists": true,
  "target_materialized": false
}
```

`run_exists=false`는 run state file이 실제로 없을 때만 반환한다. run file이 존재하는데 active run resolver가 못 찾는 경우는 `run_resolved=false`, `run_exists=true`처럼 분리한다.

## 9. DB 설계

1차 구현은 신규 컬럼 없이 기존 필드를 사용한다.

사용 필드:

- `dr_run.state`
- `dr_run.retryable`
- `dr_run.retry_count`
- `dr_run.retry_after_seconds`
- `dr_run.next_retry_at`
- `dr_run.last_status_json`
- `dr_run.projection_state`
- `dr_run.projection_checked`
- `dr_run.current_step_name`
- `dr_plan.state`
- `dr_plan.last_error_code`
- `dr_plan.last_error_message`
- `dr_plan.last_source_checkpoint_at`
- `dr_plan.last_target_durable_at`
- `dr_plan.target_ready_at`
- `dr_plan.target_ready_rpo_seconds`
- `dr_replica.target_vm_id`
- `dr_replica.target_external_ref`
- `dr_replica_disk.target_volume_id`
- `dr_restore_point`

선택적 2차 컬럼:

```sql
ALTER TABLE dr_run
  ADD COLUMN worker_state varchar(32) DEFAULT NULL,
  ADD COLUMN worker_pid bigint DEFAULT NULL,
  ADD COLUMN worker_updated_at datetime DEFAULT NULL,
  ADD COLUMN worker_exit_code int DEFAULT NULL;
```

2차 컬럼은 조회 성능 최적화용 cache이며 source of truth는 ftctl status와 Cloud inventory projection이다.

### 9.1 run state 전이

```text
QUEUED
  -> DISPATCHING
  -> ACCEPTED
  -> RUNNING
  -> RETRYING
  -> ACCEPTED/RUNNING
  -> SUCCEEDED
  -> FAILED
```

`ACCEPTED` 상태가 stall threshold를 넘으면 `RETRYING` 또는 `FAILED`로 전환한다. `ACCEPTED`가 무기한 유지되면 안 된다.

### 9.2 target readiness 보정

projection worker는 다음 SQL에 걸리는 false success를 보정한다.

```sql
SELECT p.id, p.uuid, r.id AS run_id, r.state AS run_state,
       rp.target_vm_id, rp.target_external_ref, COUNT(pt.id) AS restore_points
  FROM dr_plan p
  JOIN dr_run r ON r.id = p.last_run_id
  LEFT JOIN dr_replica rp ON rp.plan_id = p.id AND rp.removed IS NULL
  LEFT JOIN dr_restore_point pt ON pt.plan_id = p.id AND pt.removed IS NULL
 WHERE p.removed IS NULL
 GROUP BY p.id, r.id, rp.id
HAVING r.state = 'SUCCEEDED'
   AND (rp.target_vm_id IS NULL OR rp.target_external_ref IS NULL OR restore_points = 0);
```

보정은 수동 SQL보다 projection code path로 수행한다.

## 10. 레이어별 AS-IS / TO-BE

| Layer | AS-IS | TO-BE |
| --- | --- | --- |
| UI | `SYNCING`/accepted와 target 준비 완료가 섞여 보일 수 있음 | accepted, worker, materializing, ready를 분리 표시 |
| API | run accepted 정보는 있으나 worker 상태가 명확하지 않음 | `workerstate`, `retryable`, `readinessstate`를 응답에 포함 |
| Backend | accepted 이후 worker self-lock/stall을 retryable run으로 승격하지 못함 | projection에서 worker missing/stalled/retryable lock을 감지하고 retry path로 전환 |
| Agent | ftctl accepted JSON을 성공으로 전달하고 worker admission 정보가 약함 | bounded admission probe와 worker/retry metadata 전달 |
| ftctl | parent action이 같은 command worker를 global lock 아래 재실행 가능 | parent action과 worker command 분리, worker는 plan/run lock 사용 |
| DB | skeleton row만 있고 target inventory가 없어도 run이 accepted 상태로 오래 남음 | retry metadata와 last status를 저장하고 target readiness 전에는 PASS 금지 |

## 11. 구현 순서

1. ftctl `dr-status` JSON에 worker/retry/run existence 필드 추가.
2. ftctl async worker command를 parent action과 분리하거나, 1차로 delegation branch에서 lock release 후 worker spawn.
3. ftctl worker lock conflict를 status.state와 run state file에 `RETRYING`으로 persist.
4. Agent status/action wrapper가 worker/retry fields를 `statusJson`과 answer details에 보존.
5. Backend adapter가 retryable lock과 accepted-but-worker-missing을 구분.
6. Backend projection이 accepted stale threshold를 적용하고 `RETRYING`/`WORKER_STALLED`로 전환.
7. API response가 worker/readiness fields를 내려줌.
8. UI가 accepted/worker/materializing/ready를 분리 표시하고 action gating 적용.
9. 기존 false-success run 보정 projection을 1회 실행.
10. 새 plan 생성, sync start, retryable lock simulation, target materialization PASS까지 smoke 검증.

## 12. 검증 기준

### 12.1 self-lock 재현 검증

재현:

1. `dr-sync-start --wait=false` 실행.
2. background worker가 같은 global lock으로 blocked 되는 상황 유도.

PASS:

- ftctl status에 `worker_state=RETRYING`, `retryable=true`, `retry_after_sec`가 기록된다.
- Cloud run은 `ACCEPTED`로 방치되지 않고 `RETRYING`으로 전환된다.
- UI는 "자동 재시도 중"으로 표시한다.

### 12.2 target readiness 검증

PASS:

- target VM/volume/restore point가 없으면 readiness는 `TARGET_MATERIALIZING` 또는 `DEGRADED`.
- Failover 버튼은 비활성.
- `dr_run.state=SUCCEEDED`가 되지 않는다.

### 12.3 최종 PASS 검증

PASS:

- `dr_replica.target_vm_id` 또는 `target_external_ref` 채워짐.
- target volume row가 채워짐.
- restore point가 1개 이상 생성됨.
- `last_target_durable_at` 또는 durable checkpoint가 기록됨.
- API `readinessstate=TARGET_READY`.
- UI에서 다음 단계 action이 활성화됨.
## 13. Implementation Update - 2026-07-06

Implemented with the following scoped contract:

- ftctl delegated `dr-sync-start --wait=false` releases the global command lock before spawning the background worker.
- ftctl run/status files now carry worker lifecycle metadata: `worker_state`, `worker_pid`, `worker_started_at`, `worker_updated_at`, `worker_exit_code`, `retryable`, `retry_after_sec`, `lock_file`, `holder_pid`, `holder_command`, and `holder_age_sec`.
- ftctl lock conflicts for DR commands are persisted back into the run/status files as `worker_state=RETRYING` and `error_code=DR_ENGINE_BUSY_RETRYABLE` when a plan/run context is available.
- The KVM Agent status wrapper parses worker/retry fields into `FtctlDrStatusAnswer` while preserving raw `statusJson`.
- Cloud projection treats retryable worker lock/stall as a closed failed run with `retryable=true`, so the UI can show the cause and the active-run gate is released for an operator retry.
- Cloud projection treats `worker_state=FAILED` as engine failure instead of allowing target-readiness checks to mask the root cause.
- Readiness prioritizes the latest run error over generic target VM missing when the worker failed or stalled.
- The UI translates the new worker error codes and displays retry metadata in the DR run progress card.
- DB schema was not expanded; existing `dr_run` retry/error/status columns and projection event details carry the new state.

## 2026-07-07 Worker Terminal Projection Update

The previous lock recovery design covers retryable lock conflicts. A new terminal worker case was found: the worker was accepted, but later failed inside the ABLESTACK target driver. The host runtime status had `worker_state=FAILED`, `worker_exit_code=32`, and `state=ERROR`, while Cloud still exposed the run as `ACCEPTED`.

The detailed follow-up design is:

- [536-cross-hypervisor-dr-terminal-projection-and-target-driver-contract-design-20260707.md](536-cross-hypervisor-dr-terminal-projection-and-target-driver-contract-design-20260707.md)

The worker state model must distinguish:

- retryable lock wait: keep run accepted/running and expose retry-after information,
- active worker execution: keep run running and poll,
- terminal worker failure: mark run failed and plan error immediately,
- terminal worker success: mark target readiness only after durable target materialization is confirmed.

The backend must not require a separate user action to discover terminal runtime failure. Normal UI/API reads must refresh projection and close stale accepted runs automatically.
