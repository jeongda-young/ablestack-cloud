# DR 보호 그룹 자식 Run-Cycle 종결 정합성 설계

## 1. 목적

보호 그룹의 `전체 재동기화`에서 Full Seed가 정상 완료된 직후 스케줄러가 다음 증분 Cycle을 시작하면, Cloud 자식 Run이 계속 `RUNNING`으로 남는 경쟁 조건을 제거한다.

이번 변경은 검증된 VMware -> ABLESTACK(RBD) 데이터 경로를 변경하지 않는다. FTCTL의 Full Seed/CBT 전송, NBD 예약 범위, RBD 쓰기 방식은 그대로 두고 Cloud의 Run 종결 소유권과 UI 표시만 보강한다.

## 2. 오류 원인

기존 Cloud Run 성공 판정은 `latest_completed_producer_run_uuid == dr_run.uuid`를 요구했다. Full Seed Cycle N이 끝난 뒤 스케줄러가 증분 Cycle N+1을 먼저 시작하거나 완료하면 최신 producer UUID가 스케줄러 Run으로 바뀐다. 이때 다음 사실은 모두 참이지만 그룹 자식 Run은 완료되지 않았다.

- 자식 Run이 요청한 Full Seed Cycle N은 canonical `dr_sync_cycle`에 영구 완료됨
- run-scoped FTCTL 상태의 `control_request_run_uuid`는 자식 Run UUID임
- FTCTL terminal 증거는 `terminal_authoritative=true`임
- 다음 Cycle N+1은 별도 producer UUID로 정상 실행 중임

producer UUID는 Cycle 생산자 추적값이지 Cloud 요청 Run의 완료 소유권이 아니다.

## 3. 불변 계약

### 3.1 Run-Cycle 결속

`dr_run`에 다음 필드를 추가한다.

| 필드 | 형식 | 의미 |
|---|---|---|
| `accepted_cycle_sequence` | `bigint unsigned` | 해당 Run이 수락받은 canonical Cycle sequence |
| `accepted_cycle_token` | `varchar(255)` | `<plan_uuid>:<sequence>` 불변 식별자 |

FTCTL이 Cycle sequence를 할당해 Cloud가 현재/완료 Cycle을 처음 투영하는 시점에 한 번만 기록한다. 값이 기록된 후에는 다음 스케줄러 Cycle이나 producer UUID로 덮어쓰지 않는다.

### 3.2 성공 판정

Full Seed Run은 다음 조건이 모두 참일 때 성공이다.

1. run-scoped 상태의 `control_request_run_uuid == dr_run.uuid`
2. `terminal_authoritative=true` 또는 `terminal_source=ENGINE_TERMINAL`
3. `dr_sync_cycle(plan_id, accepted_cycle_sequence)`가 존재
4. Cycle token이 `accepted_cycle_token`과 일치
5. `requested_mode=FULL_RESEED`
6. Cycle state가 `READY/COMPLETED/TARGET_READY`
7. commit state가 `LOCAL_DURABLE/COMMITTED/DURABLE`
8. Cycle `completed`가 기록됨

`latest_completed_producer_run_uuid`와 latest completed mode는 위 판정에 사용하지 않는다. 두 값은 최신 Cycle의 생산자 및 운영 상태 투영에만 사용한다.

### 3.3 이전 실행 복구

결속 필드가 없는 기존 실행은 `dr_sync_cycle.run_id == dr_run.id`이면서 `requested_mode=FULL_RESEED`, `completed IS NOT NULL`인 최신 Cycle을 찾아 결속한다. 이는 배포 전 멈춘 그룹 Run을 데이터 수정 없이 자동 복구하기 위한 제한적 호환 규칙이다.

## 4. 계층별 설계

### DB

- 세 개의 Europa schema 경로에 두 컬럼을 idempotent하게 추가한다.
- 새 설치용 `CREATE TABLE dr_run`에도 동일 컬럼을 포함한다.
- canonical Cycle의 `(plan_id, sequence)` 유일성 및 alias 종결 규칙은 문서 610을 유지한다.

### Cloud Backend

- `DrRunVO`: accepted Cycle sequence/token 추가
- `DrSyncCycleDao`: `findLatestCompletedByRunIdAndRequestedMode()` 추가
- `FtctlDrRuntimeProjectionAdapter`:
  - current/completed Cycle 투영 트랜잭션 안에서 Run-Cycle 결속
  - run-scoped terminal + 결속 Cycle durable 조건으로 Full Seed Run 종결
  - producer UUID는 Cycle metadata에만 유지
- `DrProtectionGroupServiceImpl`:
  - 자식 Run이 비종결이고 결속 Cycle이 durable이면 `DrProjectionService.refreshPlanProjection()` 수행
  - 관리 서버 재시작 후 복구된 그룹 감시기도 같은 규칙으로 수렴

### Agent / FTCTL

신규 명령이나 전송 변경은 없다. 기존 status 계약의 다음 필드를 사용한다.

- `control_request_run_uuid`
- `terminal_authoritative`
- `terminal_source`
- canonical completed Cycle snapshot

FTCTL producer UUID는 계속 Cycle 생산자 추적에 사용한다. Cloud Run 종결 소유권에는 사용하지 않는다.

### API / UI

그룹 progress JSON에 다음 필드를 추가한다.

- `terminalizationState`: `RESULT_FINALIZING` 또는 `CONSISTENCY_WARNING`
- `terminalizationAgeSeconds`
- `acceptedCycleSequence`

Cycle durable 완료 후 30초 이내에는 `결과 반영 중`을 표시한다. 30초를 넘겨도 자식 Run이 종결되지 않으면 `정합성 경고`를 표시한다. 두 상태 모두 다크 모드용 기존 status pill 색상 토큰을 사용한다.

## 5. 동시성 검증

필수 테스트 시나리오는 다음과 같다.

1. 그룹 자식 Run R이 Full Seed Cycle N을 요청
2. N이 `LOCAL_DURABLE/READY`로 완료되고 R에 sequence/token 결속
3. Cloud가 R을 종결하기 전에 스케줄러가 Cycle N+1 CBT 증분을 시작 또는 완료
4. latest producer/mode는 N+1 값으로 변경
5. run-scoped FTCTL terminal의 control request는 R을 유지
6. Cloud는 N의 canonical durable 증거로 R을 `SUCCEEDED` 처리
7. 그룹은 모든 자식 성공 수를 반영해 `SUCCEEDED` 종결

## 6. AS-IS / TO-BE

| 구분 | AS-IS | TO-BE |
|---|---|---|
| Run 완료 소유권 | 최신 producer UUID | 수락 시 결속한 Cycle sequence/token |
| 다음 증분 선행 | 자식 Run이 계속 RUNNING | 기존 Full Seed Run 정상 SUCCEEDED |
| producer UUID | Cycle 추적과 Run 완료에 혼용 | Cycle 생산자 추적 전용 |
| 그룹 감시기 | 자식 Run DB 상태만 반복 조회 | durable Cycle 발견 시 projection 자동 복구 |
| 재시작 복구 | 멈춘 자식이 24시간까지 잔류 | canonical Cycle + terminal 증거로 수렴 |
| UI | 계속 진행 중 | 30초 이내 결과 반영 중, 이후 정합성 경고 |
| 데이터 경로 | 검증된 Full Seed/CBT/RBD 경로 | 변경 없음 |

## 6.1 Binding ownership correction

The immutable accepted Cycle is bound from the operation-scoped tuple
`control_request_run_uuid + transfer_cycle_sequence + transfer_mode`.
The Cycle producer UUID and `dr_sync_cycle.run_id` remain producer metadata and
must not be required to equal the Cloud child Run UUID. This distinction is
required because the scheduler can produce the accepted Full Seed Cycle while
the Cloud group child Run remains the owner of the control request.

The group monitor refreshes every non-terminal Full Seed child even before an
accepted Cycle has been persisted. The refresh binds the operation to the
canonical Cycle, verifies its durable terminal state, and completes the child.
The concurrency test begins with no accepted Cycle on the child, uses a
different scheduler producer for Cycle N, advances the scheduler to N+1, and
still requires the child and group to finish successfully.

## 7. 배포 및 재테스트 판정

1. schema 두 컬럼 반영
2. 변경된 Cloud disaster-recovery 클래스와 UI 정적 자산 배포
3. management 재시작 후 `/client/` HTTP 200 및 `WEB-INF` 보존 확인
4. FTCTL status 계약 필드와 설치 버전 확인
5. Ubuntu/Rocky/Windows 세 계획을 보호 그룹으로 Full Seed 실행
6. 각 자식의 accepted Cycle과 canonical Cycle 일치 확인
7. 후속 증분 Cycle이 시작되어도 자식 및 그룹이 모두 `SUCCEEDED`인지 확인

PASS는 전송 성공뿐 아니라 자식 Run, 그룹 Run, canonical Cycle, UI 집계가 모두 terminal 상태로 일치할 때만 부여한다.

## 8. 2026-08-18 terminal journal 경쟁 조건 보강

### 8.1 확인된 실패 경로

Ubuntu/Rocky/Windows 그룹 Full Seed에서 데이터는 모두 영구 저장됐지만 다음 경쟁 조건이 확인됐다.

1. FTCTL 스케줄러가 요청 Cycle N을 `LOCAL_DURABLE`로 완료한다.
2. 요청 Run 파일은 `full-resync-completed/100%`가 되지만 terminal journal이 없는 짧은 구간이 생긴다.
3. 다음 CBT Cycle N+1이 시작돼 Plan 범위 owner와 producer가 변경된다.
4. Cloud가 Cycle N을 아직 참조하는 비종결 자식 Run보다 N+1 투영을 먼저 처리한다.
5. Cycle N 또는 그 alias가 `SUPERSEDED`되고 자식 Run, 그룹 집계, 자원 lease가 종결되지 않는다.

### 8.2 FTCTL 계약

- 요청 Cycle N의 commit, restore point, latest-completed snapshot 기록이 끝난 직후 다음 sleep/증분 진입 전에 `runs/<run>.journals/terminal.state`를 원자 기록한다.
- operation Run 파일에는 `control_request_run_uuid`, `requested_cycle_state=COMPLETED`, `terminal_authoritative=true`, `runtime_endpoints_drained=true`를 보존한다.
- Plan scheduler의 이후 owner 값은 `dr-status --run`의 immutable request owner를 덮어쓰지 않는다.
- `dr-status --run`은 `full-resync-completed/100%`, completed Full Seed, durable commit, cycle token이 모두 일치하는 경우에만 누락 terminal journal을 복구한다. 전송 중 상태나 불완전 commit은 복구하지 않는다.
- capability `dr-requested-cycle-terminal-v1`으로 새 계약을 광고한다.

### 8.3 Cloud 계약

- `accepted_cycle_sequence`를 참조하는 비종결 Run이 존재하면 후속 completed Cycle 정리가 그 Cycle을 `SUPERSEDED` 처리하지 않는다.
- 그룹 Full Seed의 admission lease는 Agent 수락 시 해제하지 않는다. 그룹 감시기가 terminal 수렴 전까지 갱신하고, 자식 terminal + accepted durable Cycle 확인 시 그룹 성공 집계와 함께 해제한다.
- 그룹 감시기는 관리 서버 재시작 후에도 동일한 수렴 함수를 호출한다.
- UI progress JSON은 `dataTransferCompleted=true`와 terminalization 상태를 분리한다.

### 8.4 UI 표현

- `RESULT_FINALIZING`: `데이터 전송 완료`, 상세 문구는 영구 결과 확인 중으로 표시한다.
- `CONSISTENCY_WARNING`: `결과 정합성 확인 실패`, 상세 문구는 전송 실패가 아니라 종결 확인 실패임을 명시한다.
- 밝은/어두운 테마 모두 기존 DR status 색상 토큰과 고대비 파랑/노랑을 사용한다.

### 8.5 AS-IS / TO-BE

| 구분 | AS-IS | TO-BE |
|---|---|---|
| terminal 기록 | 다음 Cycle과 경쟁 | 다음 Cycle 전에 원자 journal 기록 |
| Run owner | 현재 scheduler owner로 덮어씀 | 요청 Run owner 불변 보존 |
| 상태 복구 | journal 누락 시 계속 RUNNING | durable 증거 완전 일치 시 제한 복구 |
| Cycle 정리 | accepted Cycle도 SUPERSEDED 가능 | 비종결 Run이 pin한 Cycle 제외 |
| admission lease | Agent 수락 직후 해제 | 그룹 자식 terminal까지 갱신 후 해제 |
| 그룹 집계 | 자식 종결과 별도 갱신 | terminal/Cycle/집계/lease를 한 수렴 트랜잭션에서 확정 |
| UI | 일반 진행 중 또는 모호한 경고 | 전송 완료와 결과 확인 실패를 구분 |

### 8.6 필수 회귀 검증

1. Linux 1-disk Full Seed 완료 직후 증분 시작
2. Linux 2-disk Full Seed 완료 직후 증분 시작
3. Windows 2-disk Full Seed 완료 직후 증분 시작
4. 각 요청 Run의 terminal journal, owner, accepted sequence/token 유지
5. pinned Cycle 비대체, 자식 Run `SUCCEEDED`, 그룹 `3/3 SUCCEEDED`, active lease 0

## 9. 2026-08-20 terminal barrier 및 그룹 정합성 집계

### 9.1 Backend 계약

- 그룹 progress의 Plan별 `terminalizationState`를 집계해
  `resultFinalizingCount`, `consistencyWarningCount`, `dataTransferCompletedCount`를
  최상위 필드로 제공한다.
- `consistencyWarningCount > 0`이면 그룹 실행이 아직 성공 terminal이 아니더라도 데이터
  전송 실패로 오인하지 않도록 `resultVerificationState=CONSISTENCY_WARNING`을 제공한다.
- 모든 자식 Run의 terminal 증거와 accepted durable Cycle이 수렴한 트랜잭션에서 자식
  성공, 그룹 성공 카운트, admission lease 해제를 함께 확정한다.
- FTCTL의 terminal journal이 없는 동안에는 Cloud가 Run을 임의 성공 처리하거나 lease를
  해제하지 않는다.

### 9.2 UI 계약

- 보호 그룹 진행 패널은 `성공/실패/전체`와 별도로 `결과 반영 중`, `정합성 경고`의
  구성원 수를 표시한다.
- 구성원 행은 기존 `RESULT_FINALIZING`/`CONSISTENCY_WARNING` 상태 pill을 유지한다.
- 다크 모드에서는 기존 DR 상태 토큰을 사용하며 밝은 고정 배경색을 추가하지 않는다.
- 경고는 전송 실패가 아니라 “데이터 전송 완료 후 종결 증거 확인 실패”임을 명시한다.

### 9.3 배포 및 복구 계약

FTCTL RPM 설치 후 Plan scheduler를 IDLE 시점에 rolling reload한 다음 Cloud/UI를
배포한다. 기존 Ubuntu/Rocky 비종결 Run은 DB를 직접 수정하지 않고 강화된
`dr-status --run`의 scheduler sequence fallback으로 terminal journal을 복구해야 한다.
PASS 조건은 그룹 `3/3 SUCCEEDED`, 자식 Run 모두 terminal authoritative, active lease 0,
다음 증분 scheduler 정상 실행이다.

### 9.4 AS-IS / TO-BE

| 계층 | AS-IS | TO-BE |
|---|---|---|
| FTCTL | terminal 누락 후 다음 증분 진행 가능 | terminal 발행을 다음 증분 전 필수 barrier로 처리 |
| Cloud | Plan별 경고만 progress JSON에 존재 | 최상위 집계와 구성원 결과를 함께 제공 |
| UI | 그룹은 일반 RUNNING처럼 보임 | 결과 반영 중/정합성 경고 수를 명시 |
| 배포 | RPM 파일과 실행 scheduler 코드가 다를 수 있음 | IDLE rolling reload와 hash/start time 확인 |
