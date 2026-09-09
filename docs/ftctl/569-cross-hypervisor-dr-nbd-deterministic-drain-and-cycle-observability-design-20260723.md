# Cross-Hypervisor DR NBD Deterministic Drain And Cycle Observability Design

> 2026-07-27 후속 계약: FTCTL에서 생성된 NBD drain 증거가 Failover operation
> status로 승계되지 않는 경우의 exact checkpoint hydration과 Cloud 보상
> lifecycle은
> [577-cross-hypervisor-dr-failover-projection-evidence-and-compensation-design-20260727.md](577-cross-hypervisor-dr-failover-projection-evidence-and-compensation-design-20260727.md)
> 를 따른다.

- 문서 번호: 569
- 작성일: 2026-07-23
- 상태: 실환경 Preflight 및 구현 검증 완료
- 적용 방향: VMware -> ABLESTACK
- 적용 레이어: UI, API, DR Backend, Mold Agent, FTCTL, Cloud DB
- FTCTL 하위 설계:
  `ablestack-qemu-exec-tools/docs/ftctl/440-ftctl-dr-vmware-nbd-deterministic-drain-and-observability-design-20260723.md`
- 관련 설계:
  - [544-cross-hypervisor-dr-vmware-mover-nbd-source-graph-design-20260707.md](544-cross-hypervisor-dr-vmware-mover-nbd-source-graph-design-20260707.md)
  - [555-cross-hypervisor-dr-vmware-cbt-incremental-and-transfer-metrics-design-20260714.md](555-cross-hypervisor-dr-vmware-cbt-incremental-and-transfer-metrics-design-20260714.md)
  - [568-cross-hypervisor-dr-scheduler-service-and-automatic-recovery-design-20260722.md](568-cross-hypervisor-dr-scheduler-service-and-automatic-recovery-design-20260722.md)

## 1. 문제와 설계 목표

VMware CBT 증분 cycle은 데이터 적용을 완료한 것으로 표시되지만 host kernel에는
다음 NBD 오류가 RPO 주기마다 발생한다.

```text
I/O error, dev nbd1, sector 0 op 0x0:(READ)
Buffer I/O error on dev nbd1, logical block 0, async page read
```

원인은 FTCTL의 NBD disconnect가 udev/partition 비동기 작업의 종료를 기다리지
않는 수명주기 오류이다. 현재 Cloud는 이 상태를 typed field로 받지 않기 때문에
cycle을 성공으로 표시하고 다음 동기화와 테스트 페일오버를 허용할 수 있다.

본 설계의 목표는 다음과 같다.

1. FTCTL이 NBD 자원 수명주기의 유일한 실행 주체가 된다.
2. Agent는 FTCTL typed status를 전달하며 host kernel을 직접 추론하지 않는다.
3. Backend는 데이터 내구성과 NBD 정리 완료를 모두 cycle commit 조건으로 삼는다.
4. DB는 current runtime과 cycle audit에 NBD 정리 상태를 typed column으로 보존한다.
5. API/UI는 raw host 장치 정보 없이 운영자가 정상, 정리 중, 복구 필요를 구분하게
   한다.
6. 격리 상태에서는 일반 sync/failover를 차단하고 cleanup-only 복구만 허용한다.

## 2. 실환경 Preflight 결과

### 2.1 원인 상관관계

- `10.10.32.1`과 `10.10.32.3`의 kernel 오류 시각이 DR Scheduler의 RPO cycle
  시각과 일치했다.
- 해당 cycle은 `CBT_INCREMENTAL`과 양수 changed bytes로 완료되었다.
- 확인 시점의 NBD 장치는 pid, size, mount, holder가 모두 비어 있었다.
- 따라서 영구 디스크 불량이 아니라 detach 시점의 짧은 race이다.

### 2.2 성공한 검증 코드 순서

Scheduler가 없는 `10.10.32.2`에서 source와 target NBD 경로를 별도로 검증했다.

```text
flush
-> udevadm settle
-> 비활성 NBD partition device-mapper holder 제거
-> udevadm settle
-> partx -d
-> udevadm settle
-> disconnect
-> pid 없음 + size 0 + holder/partition/mount 0
-> udevadm settle
-> stable-free 재확인
```

게스트 LVM 자동 활성화로 생긴 holder는 NBD partition의 sysfs holder로 직접
확인되고 mount 또는 활성 swap 사용 흔적이 없을 때만 제거한다. 이름이 같다는
이유로 host VG를 비활성화하지 않으며, 사용 중이거나 비 device-mapper holder인
경우 `DR_NBD_DEVICE_BUSY`로 격리한다.

module 전역 `max_part=0`은 v2k partition 처리와 충돌할 수 있으므로 사용하지
않는다. FTCTL은 `/dev/nbd16`~`/dev/nbd31`을 예약하고 이 범위에만 udev
blkid/LVM 자동 탐색 억제 rule을 적용한다. v2k를 포함한 기존 도구는 lower
NBD pool과 기존 module partition 설정을 계속 사용한다.

| 경로 | 구현 | 결과 |
|---|---|---|
| VMware source 대체 경로 | `nbdkit` + `nbd-client` | clean detach, I/O 오류 없음 |
| ABLESTACK target 경로 | `qemu-nbd` | clean detach, I/O 오류 없음 |

설계는 sleep 기반 완화가 아니라 이 순서와 종료 조건을 규범으로 사용한다.

### 2.3 실제 RPO cycle 추가 검증과 target 경로 보정

예약 NBD pool 배포 후 실제 changed-data cycle을 관찰한 결과 source
`/dev/nbd16`은 정상이었지만, Cloud가 전달한 KRBD `targetPath`를 다시 감싼
target `/dev/nbd17`에서만 sector-0 I/O 오류가 재현되었다. 독립 preflight의
clean detach만으로는 실제 target 데이터 경로의 중복 계층을 발견하지 못한
것이다.

Cloud는 대상 볼륨 생성을 소유한다. `targetPath`는 예상 KRBD 경로일 수 있으나
worker에서 항상 map되어 있다는 보장은 없다. FTCTL은 다음 계약을 따른다.

- source는 VDDK/NBD로 읽고, `rbd:<pool>/<image>` 대상은 native
  `python-rados`/`python-rbd`로 직접 쓴다.
- librbd `Image.flush()`가 성공해야 data durable로 판정한다.
- 실제 블록 target은 `fsync`와 `blockdev --flushbufs`를 사용한다.
- 이 cycle의 `nbdSourceDeviceCount=1`, `nbdTargetDeviceCount=0`은 정상이다.
- target path가 비블록 URI인 호환 경로에서만 target qemu-nbd를 사용한다.

따라서 Cloud/API/DB는 raw 장치 경로를 노출하거나 저장하지 않고 기존 typed
NBD count와 teardown 상태로 직접 대상 경로 사용 여부를 표현한다.

## 3. 레이어별 책임

| 레이어 | 책임 | 금지 사항 |
|---|---|---|
| UI | current/cycle 정리 상태 표시, 복구 action gating | host NBD 직접 조회, dmesg 해석 |
| API | typed field와 async recovery 명령 제공 | raw `/dev/nbdN`, credential 노출 |
| Backend | status projection, commit/eligibility/recovery 정책 | disconnect 직접 실행 |
| Agent | FTCTL JSON 실행 및 typed DTO 전달 | kernel log 기반 성공/실패 추론 |
| FTCTL | attach/flush/drain/quarantine/cleanup-only recovery | 실패 disconnect 무시, 격리 장치 재사용 |
| DB | current runtime과 cycle audit의 typed 상태 저장 | raw device inventory 영구 저장 |

## 4. 공통 상태 계약

### 4.1 NBD teardown 상태

```text
NOT_APPLICABLE | DRAINING | DRAINED | QUARANTINED
```

| 상태 | Cloud 의미 | UI 의미 |
|---|---|---|
| `NOT_APPLICABLE` | 해당 cycle이 NBD를 사용하지 않음 | 표시 생략 |
| `DRAINING` | 데이터 적용 후 bounded cleanup 진행 중 | `데이터 경로 정리 중` |
| `DRAINED` | 모든 cycle-owned NBD가 stable-free | `정상 정리` |
| `QUARANTINED` | cleanup 실패로 장치와 Plan 격리 | `복구 필요` |

### 4.2 cycle state와 commit state

성공 cycle:

```text
state = COMPLETED
commitState = COMMITTED
nbdTeardownState = DRAINED
incrementalVerified = true 또는 유효한 no-change
```

target flush 후 drain 실패:

```text
state = NBD_TEARDOWN_FAILED
commitState = TARGET_DURABLE_CLEANUP_PENDING
nbdTeardownState = QUARANTINED
incrementalVerified = false
```

`TARGET_DURABLE_CLEANUP_PENDING`은 데이터가 target에 기록되었을 가능성은
인정하지만 다음 기준점으로 사용할 수 없다는 뜻이다. Backend는 이전 committed
cycle과 baseline을 계속 권위 상태로 유지한다.

## 5. Agent/Core DTO 설계

### 5.1 `FtctlDrCycleSnapshot`

파일:

```text
core/src/main/java/com/cloud/agent/api/FtctlDrCycleSnapshot.java
```

추가 필드:

```java
private String nbdTeardownState;
private Long nbdTeardownStartedAtEpochMs;
private Long nbdTeardownCompletedAtEpochMs;
private Long nbdTeardownDurationMs;
private Integer nbdSourceDeviceCount;
private Integer nbdTargetDeviceCount;
private Integer nbdQuarantinedDeviceCount;
private String nbdTeardownErrorCode;
private String nbdTeardownErrorMessage;
```

getter/setter를 추가한다. DTO는 serializable contract이므로 기존 생성자 서명을
깨지 않고 optional field로 확장한다.

### 5.2 `FtctlDrStatusAnswer`

파일:

```text
core/src/main/java/com/cloud/agent/api/FtctlDrStatusAnswer.java
```

현재 runtime 집계:

```java
private String nbdTeardownState;
private Integer nbdQuarantinedDeviceCount;
private String nbdTeardownErrorCode;
private String nbdTeardownErrorMessage;
```

latest completed cycle은 기존 `FtctlDrCycleSnapshot`을 사용한다. current
runtime 필드는 active cleanup 또는 quarantine을 즉시 표시하기 위해 별도로 둔다.

## 6. KVM Agent wrapper 설계

파일:

```text
plugins/hypervisors/kvm/src/main/java/com/cloud/hypervisor/kvm/resource/wrapper/
  LibvirtFtctlDrStatusCommandWrapper.java
```

변경:

1. FTCTL `dr-status --json`의 camelCase 필드를 null-safe하게 파싱한다.
2. 숫자 필드는 음수 값을 거부하고 `null`로 정규화한다.
3. 허용하지 않은 teardown state는 `UNKNOWN`으로 전달하지 않고
   `DR_STATUS_CONTRACT_INVALID` answer failure로 분류한다.
4. raw device 배열과 host path는 DTO에 담지 않는다.
5. Agent는 `journalctl` 또는 `dmesg`를 호출하지 않는다.

호환성:

- 구 FTCTL에서 필드가 없으면 `NOT_APPLICABLE`이 아니라 `null`로 전달한다.
- Backend는 FTCTL capability/version이 새 contract를 선언했는데 필드가 없을 때만
  projection integrity 오류로 처리한다.

## 7. Backend projection 설계

### 7.1 Runtime projection

파일:

```text
plugins/integrations/disaster-recovery/src/main/java/com/cloud/dr/adapter/ftctl/
  FtctlDrRuntimeProjectionAdapter.java
```

추가 함수:

```java
private NbdTeardownProjection validateNbdTeardown(
    FtctlDrStatusAnswer status,
    FtctlDrCycleSnapshot cycle);

private void projectNbdTeardownRuntime(
    DrPlanRuntimeVO runtime,
    NbdTeardownProjection projection);

private void projectNbdTeardownCycle(
    DrSyncCycleVO cycle,
    FtctlDrCycleSnapshot snapshot);
```

검증 규칙:

```text
cycle.state == COMPLETED
  -> teardownState는 DRAINED 또는 NOT_APPLICABLE

incrementalVerified == true
  -> teardownState == DRAINED

teardownState == QUARANTINED
  -> quarantinedDeviceCount > 0
  -> errorCode 필수

teardownState == DRAINED
  -> quarantinedDeviceCount == 0
  -> completedAt/duration은 음수일 수 없음
```

규칙 위반 시 최신 정상 cycle을 덮어쓰지 않고:

```text
projectionIntegrityState = INVALID
projectionIntegrityCode = DR_NBD_TEARDOWN_CONTRACT_INVALID
protectionState = DEGRADED
```

### 7.2 Idempotency

projection key는 기존 `(plan_id, engine_run_uuid, sequence)`를 유지한다.
동일 sequence 재조회 시:

- `DRAINING -> DRAINED/QUARANTINED` 전이는 허용한다.
- terminal `DRAINED`를 `DRAINING`으로 되돌리지 않는다.
- terminal `QUARANTINED`를 일반 status refresh가 `DRAINING`으로 되돌리지 않는다.
- cleanup recovery의 새 authority sequence가 있을 때만
  `QUARANTINED -> DRAINED`를 허용한다.

## 8. DB 설계

### 8.1 `dr_plan_runtime`

추가 column:

```sql
`nbd_teardown_state` varchar(32) DEFAULT NULL,
`nbd_quarantined_device_count` int unsigned NOT NULL DEFAULT 0,
`nbd_teardown_error_code` varchar(128) DEFAULT NULL,
`nbd_teardown_error_message` varchar(4096) DEFAULT NULL
```

current eligibility 조회를 위해 다음 index를 추가한다.

```sql
KEY `i_dr_plan_runtime__nbd_teardown_state`
    (`nbd_teardown_state`, `updated`)
```

### 8.2 `dr_sync_cycle`

추가 column:

```sql
`nbd_teardown_state` varchar(32) DEFAULT NULL,
`nbd_teardown_started_at` datetime DEFAULT NULL,
`nbd_teardown_completed_at` datetime DEFAULT NULL,
`nbd_teardown_duration_ms` bigint unsigned DEFAULT NULL,
`nbd_source_device_count` int unsigned DEFAULT NULL,
`nbd_target_device_count` int unsigned DEFAULT NULL,
`nbd_quarantined_device_count` int unsigned NOT NULL DEFAULT 0,
`nbd_teardown_error_code` varchar(128) DEFAULT NULL,
`nbd_teardown_error_message` varchar(4096) DEFAULT NULL
```

### 8.3 VO/DAO

파일:

```text
plugins/integrations/disaster-recovery/src/main/java/com/cloud/dr/DrPlanRuntimeVO.java
plugins/integrations/disaster-recovery/src/main/java/com/cloud/dr/DrSyncCycleVO.java
```

JPA field와 getter/setter를 추가한다. DAO update는 기존 transaction 안에서
cycle metrics와 teardown field를 함께 기록한다. 상태와 오류 field를 서로 다른
transaction에서 기록하지 않는다.

### 8.4 Migration

동일 DDL을 다음 경로에 idempotent하게 반영한다.

```text
setup/db/create-schema.sql
engine/schema/src/main/resources/META-INF/db/schema-Europa-After.sql
engine/schema/src/main/resources/META-INF/db/schema-42200to42210.sql
engine/schema/src/main/resources/META-INF/db/schema-42210to42300.sql
```

기존 row backfill:

```text
기존 completed NBD cycle -> NULL
현재 capability가 새 contract를 보고한 이후 cycle -> typed state 필수
```

과거 row를 근거 없이 `DRAINED`로 backfill하지 않는다.

## 9. API 설계

### 9.1 조회 응답

다음 응답에 typed field를 추가한다.

```text
getDrProtectionView
listDrSyncCheckpoints
listDrPlans / getDrPlan
```

response field:

```text
nbdteardownstate
nbdteardowndurationms
nbdquarantineddevicecount
nbdteardownerrorcode
nbdteardownerrormessage
```

오류 메시지는 credential/path를 제거한 FTCTL sanitized message만 반환한다.
raw NBD device 이름은 반환하지 않는다.

### 9.2 복구 명령

새 public API를 만들지 않고 기존 async `recoverDrSync`를 확장한다.

Backend가 `nbdTeardownState=QUARANTINED`를 발견하면:

```text
RECOVER_SYNC
  -> Agent
  -> ftctl dr-sync-recover
  -> cleanup-only reconcile
  -> DRAINED 확인
  -> 기존 desired RUNNING이면 scheduler resume
```

API 응답은 job ID를 즉시 반환하며 disconnect 완료를 동기 대기하지 않는다.

## 10. Action eligibility 설계

중앙 eligibility service와 UI는 같은 규칙을 사용한다.

| 상태 | 동기화 시작 | 테스트 페일오버 | 실제 페일오버 | 동기화 복구 |
|---|---:|---:|---:|---:|
| `DRAINING` | 비활성 | 비활성 | 비활성 | 비활성 |
| `DRAINED` | 기존 상태 규칙 | 기존 readiness 규칙 | 기존 readiness 규칙 | 불필요 |
| `QUARANTINED` | 비활성 | 비활성 | 비활성 | 활성 |
| `NULL` + 구 FTCTL | capability 규칙 | capability 규칙 | capability 규칙 | 기존 규칙 |

Backend 재검증은 UI disable과 독립적으로 수행한다. `QUARANTINED` 상태에서
일반 sync/test/failover 요청이 오면:

```text
DR_NBD_CLEANUP_REQUIRED
```

를 반환한다.

## 11. UI 설계

### 11.1 보호 정보

관련 화면:

```text
ui/src/views/infra/dr/
```

보호 정보의 current status 영역에 다음 상태를 표시한다.

| 값 | 표시 | 색 |
|---|---|---|
| `DRAINING` | 데이터 경로 정리 중 | 파랑 |
| `DRAINED` | 데이터 경로 정상 | 초록 |
| `QUARANTINED` | 데이터 경로 복구 필요 | 빨강 |
| `NULL/NOT_APPLICABLE` | 행 생략 | 없음 |

`QUARANTINED`일 때만 sanitized 오류 설명과 `동기화 복구` action을 제공한다.
커널 메시지와 `/dev/nbdN`은 일반 사용자에게 표시하지 않는다.

### 11.2 동기화 이력

파일:

```text
ui/src/views/infra/dr/DrSyncCheckpointsTab.vue
```

추가 column:

```text
데이터 경로 정리
정리 시간
```

표시 규칙:

- `DRAINED`: `정상`, duration을 ms 또는 s로 표시
- `QUARANTINED`: `복구 필요`, error code tooltip
- 과거 row의 `null`: `기록 없음`
- `NOT_APPLICABLE`: `해당 없음`

기존 changed bytes, transfer bytes, effective mode와 함께 보여 cycle이
증분 데이터 적용과 자원 정리를 모두 완료했는지 확인할 수 있게 한다.

### 11.3 다크 모드와 polling

- 기존 theme token을 사용하고 hard-coded white background를 추가하지 않는다.
- protection view의 주기적 cache refresh에 teardown aggregate를 포함한다.
- polling 중 전체 skeleton으로 되돌리지 않고 변경된 status만 갱신한다.
- `DRAINING`이 timeout을 넘으면 UI가 임의로 실패로 바꾸지 않고 Backend의
  `QUARANTINED` 투영을 기다린다.

### 11.4 i18n

영문/한글 locale에 최소 다음 label을 추가한다.

```text
label.dr.nbd.teardown.state
label.dr.nbd.teardown.duration
label.dr.nbd.teardown.draining
label.dr.nbd.teardown.drained
label.dr.nbd.teardown.quarantined
label.dr.nbd.teardown.not.recorded
message.dr.nbd.cleanup.required
```

## 12. Cache 설계

`dr_plan_view_cache`의 protection payload에 다음 aggregate를 포함한다.

```json
{
  "nbdTeardownState": "DRAINED",
  "nbdQuarantinedDeviceCount": 0,
  "nbdTeardownErrorCode": null
}
```

cache generation은 `dr_plan_runtime.updated`와 cycle projection transaction
완료 이후에 수행한다. terminal state가 바뀌면 cache를 즉시 invalidate한다.

## 13. 오류 처리

| FTCTL 오류 | Backend 상태 | 사용자 조치 |
|---|---|---|
| `DR_NBD_TARGET_FLUSH_FAILED` | FAILED/DEGRADED | 데이터 경로 점검 후 복구 |
| `DR_NBD_DISCONNECT_FAILED` | QUARANTINED/DEGRADED | 동기화 복구 |
| `DR_NBD_TEARDOWN_TIMEOUT` | QUARANTINED/DEGRADED | 동기화 복구, 반복 시 host 점검 |
| `DR_NBD_DEVICE_BUSY` | QUARANTINED/DEGRADED | holder/mount 원인 점검 |
| `DR_NBD_DEVICE_QUARANTINED` | RECOVERY_REQUIRED | 일반 작업 차단 |
| contract 위반 | projection INVALID | FTCTL/Agent 버전 정합성 점검 |

NBD cleanup 실패를 일반 sync error 문자열에 합치지 않는다. 데이터 복사 실패와
자원 정리 실패는 원인과 복구 동작이 다르다.

## 14. 테스트 설계

### 14.1 Agent

- 신규 field가 있는 FTCTL JSON parsing
- 필드가 없는 구 FTCTL 호환
- 잘못된 state와 음수 metric 거부
- raw device 정보가 DTO에 포함되지 않는지 확인

### 14.2 Backend

- `DRAINING -> DRAINED`
- `DRAINING -> QUARANTINED`
- terminal state 역행 방지
- `incrementalVerified=true` + non-DRAINED contract 거부
- quarantine 시 action eligibility 차단
- `recoverDrSync` cleanup-only dispatch
- latest good cycle과 baseline 보존

### 14.3 DB/API

- schema fresh install와 upgrade idempotency
- VO/DAO round trip
- current runtime/cycle field 동시 commit
- response serialization과 credential/path 비노출
- cache invalidation

### 14.4 UI

- status pill과 duration formatting
- null/NOT_APPLICABLE 표시
- quarantine action gating
- polling 중 화면 유지
- dark/light theme contrast

### 14.5 통합

1. Linux/Windows Plan 각각 연속 3회 incremental cycle
2. changed bytes와 `DRAINED`가 같은 sequence에 저장되는지 확인
3. kernel에 새 sector-0 NBD I/O 오류가 없는지 확인
4. drain timeout fault injection으로 quarantine 확인
5. sync/test/failover 차단 확인
6. async recovery가 cleanup-only로 완료되는지 확인
7. 이전 committed baseline에서 다음 incremental이 이어지는지 확인

## 15. 권장 구현 순서

1. FTCTL drain/stable-free/quarantine와 selftest
2. FTCTL status/event contract
3. Agent core DTO와 KVM wrapper
4. DB migration, VO/DAO
5. Backend projection/commit/eligibility/recovery
6. API response와 protection cache
7. UI protection/history/i18n/dark mode
8. 변경 Maven module build와 UI build
9. qemu GitHub Actions RPM build
10. Cloud changed class/UI와 FTCTL 동시 배포
11. Linux/Windows fault injection 및 연속 RPO cycle 재검증

FTCTL과 Agent/Cloud는 contract 필드를 기준으로 함께 배포한다. Cloud가 신규
capability를 요구하는 동안 구 FTCTL이 설치된 host는 action eligibility에서
`ENGINE_UPGRADE_REQUIRED`로 차단한다.

## 16. AS-IS / TO-BE

| 레이어 | AS-IS | TO-BE |
|---|---|---|
| UI | cycle 성공만 표시 | 데이터 적용과 NBD 정리 상태를 함께 표시 |
| API | teardown typed field 없음 | current/cycle teardown field와 async recovery |
| Backend | flush 이후 cycle 성공 가능 | native librbd target 내구성 확인과 source NBD `DRAINED`까지 commit gate, quarantine eligibility |
| Agent | 기존 cycle metric만 전달 | FTCTL teardown aggregate를 typed DTO로 전달 |
| FTCTL | librbd target URI도 target NBD로 재래핑하고 즉시 disconnect | native librbd 직접 쓰기, source NBD deterministic drain, stable-free, quarantine |
| DB | kernel race 이력 없음 | runtime/cycle typed audit |
| 복구 | worker 재시작 또는 재동기화 | cleanup-only drain 후 scheduler 재개 |
| baseline | cleanup 실패와 독립적으로 전진 가능 | 이전 committed baseline 유지 |
| 보안 | 상세 확인에 host 로그 필요 | sanitized aggregate만 API/UI 노출 |

## 17. 완료 기준

- 모든 VMware -> ABLESTACK source NBD 종료 경로가 deterministic drain을 사용한다.
- Cloud 관리 RBD 대상 cycle은 target NBD를 만들지 않고
  `nbdTargetDeviceCount=0`으로 완료한다.
- `COMPLETED`/`incrementalVerified=true` cycle은 반드시 `DRAINED`이다.
- `QUARANTINED` 상태에서 sync/test/failover가 UI와 Backend 모두 차단된다.
- recovery는 async cleanup-only이며 데이터 재전송 없이 완료된다.
- DB/API/cache/UI가 같은 teardown state와 sequence를 표시한다.
- 연속 RPO cycle 동안 host kernel NBD sector-0 I/O 오류가 재발하지 않는다.
- 기존 ABLESTACK -> ABLESTACK, ABLESTACK -> VMware, VMware -> VMware 및
  RBD/QCOW2 FT/HA 경로에는 동작 변경이 없다.
