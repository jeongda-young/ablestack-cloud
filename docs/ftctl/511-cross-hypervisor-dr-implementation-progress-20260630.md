# Cross Hypervisor DR Implementation Progress

작성일: 2026-06-30

대상 브랜치: `feature/ftctl-cloud-integration`

## 1. 진행 원칙

- 구현은 설계 문서 500-510을 기준으로 한 단계씩 진행한다.
- 각 단계가 끝나면 이 문서에 구현 내용, 검증 결과, 다음 단계, 남은 단계를 기록한다.
- 기존 FTCTL KVM-to-KVM 성공 경로는 adapter 연결 전까지 직접 변경하지 않는다.
- Cloud UI는 host, qemu, FTCTL runtime을 직접 호출하지 않고 Cloud API만 호출한다.
- Cloud 변경은 변경 Maven module 단위로 검증한다.
- qemu/ftctl repo 변경은 실제 qemu-side contract 변경이 필요한 단계에서만 진행한다.

## 2. 전체 구현 단계

| 단계 | 영역 | 목표 | 상태 |
| --- | --- | --- | --- |
| 1 | DB/Entity | 신규 DR table, VO, DAO, Spring bean 기반 추가 | 완료 |
| 2 | Backend service | `DrSiteService`, `DrPlanService`, `DrRunService`, `DrProjectionService` 골격 | 완료 |
| 3 | API command/response | 신규 DR API command와 response 골격 | 완료 |
| 4 | FTCTL projection adapter | 기존 FTCTL 보호 상태를 신규 `DrPlan` 모델로 읽기 projection | 완료 |
| 5 | FTCTL action adapter | 신규 `DrRun` action을 기존 FTCTL 성공 경로로 위임 | 완료 |
| 6 | KVM-to-KVM vertical slice | 기존 4개 FTCTL 스토리지 조합 회귀 검증 | 완료 |
| 7 | Cloud UI | 신규 DR Sites/Plans/Run UI와 다크모드 대응 | 완료 |
| 8 | VMware target Phase 1 | VMware target skeleton/readiness 구현 | 완료 |
| 9 | V2K 연계 | V2K phase1/phase2를 `DrRunStep`으로 추적 | 완료 |
| 10 | 통합 검증/배포 준비 | 모듈 빌드, smoke, 배포 절차 정리 | 완료 |

현재 진행률: `10 / 10`

다음 단계: `없음 - 구현 계획 완료`

남은 단계: `0`

## 3. 단계별 결과

### 1단계: DB/Entity

상태: 완료

계획:

- `dr_site`, `dr_site_pair`, `dr_plan`, `dr_restore_point`, `dr_restore_point_artifact`, `dr_replica`, `dr_replica_disk`, `dr_run`, `dr_run_step`, `dr_event` DDL 추가
- fresh install schema와 active upgrade schema를 함께 반영
- 신규 VO/DAO class 추가
- `disaster-recovery` Spring context에 DAO bean 등록
- compile 전 정적 검증 수행

구현 내용:

- 신규 VO 10개 추가
  - `com.cloud.dr.DrSiteVO`
  - `com.cloud.dr.DrSitePairVO`
  - `com.cloud.dr.DrPlanVO`
  - `com.cloud.dr.DrRestorePointVO`
  - `com.cloud.dr.DrRestorePointArtifactVO`
  - `com.cloud.dr.DrReplicaVO`
  - `com.cloud.dr.DrReplicaDiskVO`
  - `com.cloud.dr.DrRunVO`
  - `com.cloud.dr.DrRunStepVO`
  - `com.cloud.dr.DrEventVO`
- 신규 DAO interface/implementation 10쌍 추가
  - `DrSiteDao`, `DrSiteDaoImpl`
  - `DrSitePairDao`, `DrSitePairDaoImpl`
  - `DrPlanDao`, `DrPlanDaoImpl`
  - `DrRestorePointDao`, `DrRestorePointDaoImpl`
  - `DrRestorePointArtifactDao`, `DrRestorePointArtifactDaoImpl`
  - `DrReplicaDao`, `DrReplicaDaoImpl`
  - `DrReplicaDiskDao`, `DrReplicaDiskDaoImpl`
  - `DrRunDao`, `DrRunDaoImpl`
  - `DrRunStepDao`, `DrRunStepDaoImpl`
  - `DrEventDao`, `DrEventDaoImpl`
- `plugins/integrations/disaster-recovery/src/main/resources/META-INF/cloudstack/disaster-recovery/spring-disaster-recovery-context.xml`에 신규 DAO bean 등록
- fresh schema `setup/db/create-schema.sql`에 신규 table drop/create 반영
- upgrade schema 반영
  - `engine/schema/src/main/resources/META-INF/db/schema-42200to42210.sql`
  - `engine/schema/src/main/resources/META-INF/db/schema-42210to42300.sql`
  - `engine/schema/src/main/resources/META-INF/db/schema-Europa-After.sql`
- `legacy_dr_cluster_id`는 fresh schema에 `disaster_recovery_cluster` table이 없는 경로를 고려해 FK 없이 nullable reference column과 index로 유지

검증 결과:

- Windows 작업트리 기준 `git diff --check` 통과
- WSL ext4 clean worktree `/home/ablecloud/work/dhslove/ablestack-cloud-cross-dr-step1-wt`에서 Maven 모듈 compile 성공
  - 명령: `mvn -pl plugins/integrations/disaster-recovery -am -DskipTests -Dcheckstyle.skip -Drat.skip=true compile`
  - 결과: `BUILD SUCCESS`
  - 완료 시각: `2026-06-30T21:33:17+09:00`

다음 단계:

- `2. Backend service` 구현
- 구현 대상:
  - `DrSiteService`
  - `DrPlanService`
  - `DrRunService`
  - `DrProjectionService`
  - `DrOrchestrator`
  - `DrRunExecutor`
  - `DrAdapterRegistry`

남은 단계:

- `9`

### 2단계: Backend service

상태: 완료

계획:

- `DrSiteService`, `DrPlanService`, `DrRunService`, `DrProjectionService` 골격 추가
- `DrOrchestrator`, `DrRunExecutor`를 분리해 API async job과 `DrRun` 실행 상태를 분리
- engine 직접 호출을 막기 위한 adapter contract와 `DrAdapterRegistry` 추가
- 기존 FTCTL KVM-to-KVM 성공 경로는 변경하지 않고 신규 공통 DR 경계만 추가
- Spring context에 신규 service/orchestrator/registry bean 등록

구현 내용:

- 공통 상수 추가
  - `com.cloud.dr.DrConstants`
- 신규 service interface/implementation 추가
  - `DrSiteService`, `DrSiteServiceImpl`
  - `DrPlanService`, `DrPlanServiceImpl`
  - `DrRunService`, `DrRunServiceImpl`
  - `DrProjectionService`, `DrProjectionServiceImpl`
- 신규 orchestrator/executor 추가
  - `com.cloud.dr.orchestrator.DrOrchestrator`
  - `com.cloud.dr.orchestrator.DrOrchestratorImpl`
  - `com.cloud.dr.orchestrator.DrRunExecutor`
  - `com.cloud.dr.orchestrator.DrRunExecutorImpl`
- 신규 adapter contract/registry 추가
  - `DrReplicationEngine`
  - `DrFencingAdapter`
  - `DrProjectionAdapter`
  - `DrAdapterRegistry`, `DrAdapterRegistryImpl`
  - `DrAdapterResult`
  - `DrExecutionContext`
- `DrSiteServiceImpl`
  - site create/update/list/get/delete/check 골격 구현
  - active site name 중복 방지
  - 기본 state/health state 보정
- `DrPlanServiceImpl`
  - plan create/update/list/get/enable/disable/delete 골격 구현
  - source/target site 존재 확인
  - source VM, engine binding 기준 active plan 중복 방지
  - active run이 있으면 plan delete 거부
  - adapter registry 존재 여부를 action eligibility에 반영
- `DrRunServiceImpl` 및 `DrOrchestratorImpl`
  - plan별 active run guard 추가
  - idempotency key 재요청 시 기존 run 반환
  - run 생성 시 `dr_run`, 최초 `dr_run_step`, `dr_event`를 transaction 안에서 기록
  - executor dispatch는 `QUEUED` 상태 기록까지만 수행하고 외부 engine은 호출하지 않음
  - cancel/failure 기록 경로 추가
- `DrProjectionServiceImpl`
  - plan/run event 조회 경로 추가
  - projection adapter가 없으면 best-effort refresh는 no-op, strict refresh는 `DR_ENGINE_UNAVAILABLE` 반환
- `plugins/integrations/disaster-recovery/src/main/resources/META-INF/cloudstack/disaster-recovery/spring-disaster-recovery-context.xml`에 신규 service/orchestrator/registry bean 등록

검증 결과:

- Windows 작업트리 기준 `git diff --check` 통과
- WSL ext4 clean worktree `/home/ablecloud/work/dhslove/ablestack-cloud-cross-dr-step2-wt`에서 Maven 모듈 compile 성공
  - 명령: `mvn -pl plugins/integrations/disaster-recovery -am -DskipTests -Dcheckstyle.skip -Drat.skip=true compile`
  - 결과: `BUILD SUCCESS`
  - 완료 시각: `2026-06-30T21:47:36+09:00`

경계:

- 기존 `ftctl-service` module, qemu/ftctl runtime, 기존 rbd/qcow2 성공 경로는 변경하지 않음
- 이번 단계의 executor는 run을 terminal success로 만들지 않음
- 실제 FTCTL/VMware/V2K adapter 연결은 후속 단계에서 수행

다음 단계:

- `3. API command/response` 구현
- 구현 대상:
  - 신규 DR API command class
  - 신규 DR response class
  - response generator/helper
  - service method를 API command에서 호출하는 command wiring

남은 단계:

- `8`

### 3단계: API command/response

상태: 완료

계획:

- 신규 DR API command와 response class 골격 추가
- command는 기존 `disaster-recovery` integration plugin 소스 트리 안에 배치
- API command는 Cloud UI가 직접 qemu/FTCTL runtime을 호출하지 않도록 service 계층만 호출
- action API는 `DrRunService`를 통해 `DrRun`을 생성하고, Cloud async job 성공과 DR run 상태를 분리
- response는 secret/API key/private key 원문을 노출하지 않음
- 기존 `DisasterRecoveryCluster` API와 FTCTL API는 제거하거나 semantic을 바꾸지 않음

구현 내용:

- 신규 response class 7개 추가
  - `org.apache.cloudstack.api.response.dr.DrSiteResponse`
  - `org.apache.cloudstack.api.response.dr.DrPlanResponse`
  - `org.apache.cloudstack.api.response.dr.DrRunResponse`
  - `org.apache.cloudstack.api.response.dr.DrRunStepResponse`
  - `org.apache.cloudstack.api.response.dr.DrEventResponse`
  - `org.apache.cloudstack.api.response.dr.DrReplicaResponse`
  - `org.apache.cloudstack.api.response.dr.DrRestorePointResponse`
- 신규 response helper 추가
  - `com.cloud.dr.response.DrResponseGenerator`
  - VO를 API response로 변환
  - `credentialRef` masking은 legacy 응답에만 적용한다. 신규 설계는 `dr_site_credential` 암호화 저장과 credential 상태 summary를 사용한다.
  - `DrRunResponse`에 `accepted`, `planid`, `runstate`, `steps`, `progresspercent` 계열 정보 제공
- 신규 site command 추가
  - `CreateDrSiteCmd`
  - `ListDrSitesCmd`
  - `GetDrSiteCmd`
  - `UpdateDrSiteCmd`
  - `DeleteDrSiteCmd`
  - `CheckDrSiteCmd`
- 신규 plan command 추가
  - `CreateDrPlanCmd`
  - `ListDrPlansCmd`
  - `GetDrPlanCmd`
  - `UpdateDrPlanCmd`
  - `EnableDrPlanCmd`
  - `DisableDrPlanCmd`
  - `DeleteDrPlanCmd`
- 신규 action command 추가
  - `AbstractDrPlanActionCmd`
  - `StartDrSyncCmd`
  - `StartDrTestFailoverCmd`
  - `StopDrTestFailoverCmd`
  - `StartDrFailoverCmd`
  - `ConfirmDrFenceClearCmd`
  - `StartDrFailbackCmd`
  - `StartDrReprotectCmd`
  - `AdoptDrReplicaCmd`
  - `CancelDrRunCmd`
- 신규 query command 추가
  - `ListDrRestorePointsCmd`
  - `ListDrReplicasCmd`
  - `ListDrRunsCmd`
  - `GetDrRunCmd`
  - `ListDrRunStepsCmd`
  - `ListDrEventsCmd`
- service 조회 보강
  - `DrRunService.listRunSteps(long runId)`
  - `DrProjectionService.listReplicas(long planId)`
  - `DrProjectionService.listRestorePoints(long planId)`
- event type 보강
  - `DR.SITE.*`
  - `DR.PLAN.*`
  - `DR.RUN.CANCEL`
  - `DR.FENCE.CONFIRM`
- Spring context에 `drResponseGenerator` bean 등록
- `DisasterRecoveryClusterServiceImpl#getCommands()`에 신규 command class 등록

검증 결과:

- Windows 작업트리 기준 `git diff --check` 통과
- WSL ext4 clean worktree `/home/ablecloud/work/dhslove/ablestack-cloud-cross-dr-step3-wt`에서 Maven 모듈 compile 성공
  - 명령: `mvn -pl plugins/integrations/disaster-recovery -am -DskipTests -Dcheckstyle.skip -Drat.skip=true compile`
  - 결과: `BUILD SUCCESS`
  - 완료 시각: `2026-06-30T22:08:57+09:00`

경계:

- action command는 engine/FTCTL runtime을 직접 호출하지 않음
- action command는 `DrRun` 생성 및 executor queue handoff까지만 수행
- 기존 `DisasterRecoveryCluster` API와 기존 FTCTL API는 유지
- 실제 기존 FTCTL 보호 상태 projection/import는 다음 단계에서 구현

다음 단계:

- `4. FTCTL projection adapter` 구현
- 구현 대상:
  - 기존 `ftctl_protection` / VM detail / runtime state를 신규 `DrPlan`, `DrReplica`, `DrEvent` 관점으로 읽는 projection
  - KVM-to-KVM 기존 성공 경로를 변경하지 않는 read-only adapter
  - `get/listDrPlans`, `listDrReplicas`, `listDrEvents`에서 FTCTL 상태가 보이도록 연결

남은 단계:

- `7`

### 4단계: FTCTL projection adapter

상태: 완료

계획:

- 기존 `ftctl_protection`, `ftctl_protection_volume`, VM detail을 신규 DR projection으로 읽는다.
- FTCTL runtime/qemu/host action을 호출하지 않는 read-only adapter로 구현한다.
- `DrPlan`, `DrReplica`, `DrReplicaDisk`, `DrEvent`가 FTCTL 상태를 볼 수 있게 한다.
- `get/listDrPlans`, `listDrReplicas`, `listDrEvents` 조회 시 best-effort projection refresh를 수행한다.
- 기존 FTCTL KVM-to-KVM 성공 경로와 rbd/qcow2 처리 경로는 변경하지 않는다.

구현 내용:

- FTCTL projection adapter 추가
  - `com.cloud.dr.adapter.ftctl.FtctlDrProjectionAdapter`
  - adapter key: `engineType=FTCTL`, `engineBindingType=FTCTL`
  - `DrPlan.engine_binding_id`가 있으면 `ftctl_protection.id`로 조회
  - binding id가 없으면 `DrPlan.source_vm_id` 기준 active `ftctl_protection` 조회
  - active FTCTL 보호가 없으면 `DR_FTCTL_PROTECTION_NOT_FOUND`로 plan error projection
- `DrPlan` projection
  - `source_vm_id`, `engine_type`, `engine_binding_type`, `engine_binding_id` 보정
  - FTCTL `protection_state`, `provisioning_state`, `last_error`를 `DrPlan.state`, `last_error_code`, `last_error_message`에 반영
  - standby VM이 확인되면 `target_ready_at`을 FTCTL 갱신 시각 기준으로 보강
- `DrReplica` projection
  - secondary VM id/name/uuid를 target identity로 반영
  - FTCTL active side, fencing/protection/transport/provisioning 상태를 `runtime_state_json`으로 투영
  - VM detail은 `ftctl.` prefix만 포함하고 secret/password/token/credential/private-key 계열 key는 제외
- `DrReplicaDisk` projection
  - `ftctl_protection_volume`을 source volume 기준으로 idempotent upsert
  - primary/secondary disk path, volume id, replication state, 추정 target format(`rbd`, `qcow2`) 반영
- `DrEvent` projection
  - `PROJECTION_REFRESH` event source를 `FTCTL`로 기록
  - 최신 projection details가 동일하면 새 event를 생성하지 않아 조회 refresh로 event가 무한 누적되지 않도록 처리
- DAO 보강
  - `DrEventDao.findLatestByPlanIdAndEventType(long planId, String eventType)`
  - `DrReplicaDiskDao.findActiveByReplicaIdAndSourceVolumeId(long replicaId, long sourceVolumeId)`
- Adapter registry 보강
  - `DrAdapterRegistryImpl`에 Spring list setter 추가
  - `ftctlDrProjectionAdapter` bean을 `projectionAdapters`로 등록
- 조회 API 연결
  - `GetDrPlanCmd`, `ListDrPlansCmd`에서 best-effort projection refresh 수행
  - `DrProjectionServiceImpl.listReplicas`, `listPlanEvents`에서 best-effort projection refresh 수행
  - `engineType` 또는 `engineBindingType` 중 하나만 `FTCTL`이어도 projection adapter를 찾도록 fallback 적용

검증 결과:

- Windows 작업트리 기준 `git diff --check` 통과
- WSL ext4 clean worktree `/home/ablecloud/work/dhslove/ablestack-cloud-cross-dr-step4-wt`에서 Maven 모듈 compile 성공
  - 명령: `mvn -pl plugins/integrations/disaster-recovery -am -DskipTests -Dcheckstyle.skip -Drat.skip=true compile`
  - 결과: `BUILD SUCCESS`
  - 완료 시각: `2026-06-30T23:13:48+09:00`

경계:

- 기존 `plugins/integrations/ftctl-service` API/service action은 변경하지 않음
- qemu/ftctl repo와 host runtime script는 변경하지 않음
- 기존 rbd->rbd, rbd->qcow2, qcow2->rbd, qcow2->qcow2 성공 경로는 변경하지 않음
- 이번 단계는 read-only projection이며 `DrRun` action 위임은 다음 단계에서 구현

다음 단계:

- `5. FTCTL action adapter` 구현
- 구현 대상
  - 신규 `DrRun` action을 기존 FTCTL 성공 API/service 경로로 위임
  - FTCTL lock/conflict를 `DR_ENGINE_BUSY` 계열 결과로 변환
  - failover/failback/reprotect/adopt/delete action의 controller boundary 유지

남은 단계:

- `6`

### 5단계: FTCTL action adapter

상태: 완료

계획:

- 신규 `DrRun` action을 기존 FTCTL service 성공 경로로 위임한다.
- `ftctl-service` 내부 성공 로직과 qemu/ftctl runtime script는 변경하지 않는다.
- `DrRun`, `DrRunStep`, `DrEvent`가 실제 실행 결과를 terminal 상태로 남기도록 executor를 연결한다.
- FTCTL lock/conflict는 `DR_ENGINE_BUSY`, action 실패는 `DR_ENGINE_ACTION_FAILED`로 표준화한다.
- `TEST_FAILOVER`, `TEST_CLEANUP`은 아직 구현하지 않고 명시적인 unsupported 실패로 둔다.

구현 내용:

- `dr_run.request_json` 컬럼과 `DrRunVO.requestJson` 필드를 추가했다.
  - action API의 `restorePointId`, `replicaId`, `dryRun`, `force`, `acknowledgement`, `reason`을 저장한다.
  - `startDrFailover`는 `disaster`, `skipSourceFenceRequest`도 request JSON에 포함한다.
  - fresh schema와 active upgrade schema 3개 모두에 반영했다.
- `com.cloud.dr.adapter.ftctl.FtctlDrActionAdapter`를 추가했다.
  - adapter key: `engineType=FTCTL`, `engineBindingType=FTCTL`
  - `SYNC` -> `FtctlService.executeFtctlAction(..., PROTECT_START, force)`
  - `FAILOVER` -> `FtctlService.executeFtctlAction(..., FAILOVER, force)`
  - `FENCE_CONFIRM` -> `FtctlService.confirmFtctlFence(primaryVmId)`
  - `FAILBACK` -> `FtctlService.failbackFtctlProtection(...)`
  - `REPROTECT` -> `FtctlService.executeFtctlAction(..., FAILBACK_REPROTECT, force)`
  - `ADOPT` -> `FtctlService.adoptFtctlDrReplica(targetVmId, cleanupTransport)`
  - `TEST_FAILOVER`, `TEST_CLEANUP` 등 미구현 action은 `DR_ACTION_UNSUPPORTED`로 실패시킨다.
- `DrRunExecutorImpl`을 실제 실행 경로로 연결했다.
  - run 상태를 `RUNNING`으로 전환하고 `RUN_STARTED` event를 기록한다.
  - `execute` step을 `RUNNING`으로 기록한 뒤 adapter 실행 결과에 따라 `SUCCEEDED` 또는 `FAILED` step을 추가한다.
  - 성공 시 run을 `SUCCEEDED`, 실패 시 run을 `FAILED`로 완료 처리한다.
  - 성공 이후 projection refresh를 best-effort로 수행한다.
- `DrOrchestratorImpl.executeRun`의 queued event 기록 순서를 executor 호출 전으로 조정했다.
  - 동기 executor가 즉시 terminal 상태를 만들더라도 event 순서가 `CREATED -> QUEUED -> STARTED -> SUCCEEDED/FAILED`가 되도록 했다.
- Spring context에 `ftctlDrActionAdapter` bean을 등록하고 `replicationEngines`, `fencingAdapters`에 연결했다.
- `disaster-recovery` Maven module이 `cloud-plugin-integrations-ftctl-service`를 참조하도록 POM dependency를 추가했다.

검증 결과:

- Windows 작업트리 기준 `git diff --check` 통과
- WSL ext4 clean worktree `/home/ablecloud/work/dhslove/ablestack-cloud-cross-dr-step5-wt`에서 Maven 모듈 compile 성공
  - 명령: `mvn -pl plugins/integrations/disaster-recovery -am -DskipTests -Dcheckstyle.skip -Drat.skip=true compile`
  - 결과: `BUILD SUCCESS`
  - 완료 시각: `2026-06-30T23:33:17+09:00`

경계:

- 기존 `plugins/integrations/ftctl-service` service/action 구현은 변경하지 않았다.
- qemu/ftctl repo와 host runtime script는 변경하지 않았다.
- 기존 rbd->rbd, rbd->qcow2, qcow2->rbd, qcow2->qcow2 FTCTL 성공 로직은 직접 변경하지 않았다.
- 이번 단계는 KVM-to-KVM FTCTL action 위임 연결까지이며, VMware target/V2K action은 아직 연결하지 않았다.

다음 단계:

- `6. KVM-to-KVM vertical slice` 구현 및 회귀 검증 준비
- 구현 대상:
  - 기존 4개 FTCTL 스토리지 조합에서 신규 DR API/action facade가 기존 FTCTL service 경로를 정상 호출하는지 검증
  - run/step/event/projection 결과가 Cloud API 응답으로 일관되게 보이는지 확인
  - 필요 시 KVM-to-KVM smoke용 API/DB 확인 절차 문서화

남은 단계:

- `5`

### 6단계: KVM-to-KVM vertical slice

상태: 완료

계획:

- 기존 FTCTL KVM-to-KVM 4개 스토리지 조합을 신규 DR API/action facade 관점에서 회귀 보호한다.
- 신규 DR adapter가 rbd/qcow2 분기를 재구현하지 않고 기존 FTCTL service 성공 경로만 호출하는지 검증한다.
- `DrRun`, `DrRunStep`, `DrEvent` terminal 기록이 adapter 실행 결과와 일관되는지 검증한다.
- 실제 10.10.32 클러스터 재테스트 전에 코드 레벨 smoke 기준과 수동/live 확인 항목을 문서화한다.

구현 내용:

- `docs/ftctl/512-cross-hypervisor-dr-kvm-to-kvm-vertical-slice-smoke-20260630.md`를 추가했다.
  - rbd->rbd, rbd->qcow2, qcow2->rbd, qcow2->qcow2 4개 조합을 KVM FTCTL smoke matrix로 정의했다.
  - code-level smoke와 live regression checklist를 분리했다.
  - DR adapter가 storage backend를 해석하거나 qemu/ftctl runtime profile을 직접 생성하지 않는다는 경계를 명시했다.
- `FtctlDrActionAdapterTest`를 추가했다.
  - 4개 스토리지 조합 parameterized test로 `SYNC`가 기존 `FtctlService.executeFtctlAction(..., PROTECT_START, ...)` 경로로 위임되는지 검증했다.
  - FTCTL lock/exit 75 결과가 신규 DR 결과의 `DR_ENGINE_BUSY`로 매핑되는지 검증했다.
  - 미구현 `TEST_FAILOVER`가 `DR_ACTION_UNSUPPORTED`로 끝나고 FTCTL runtime service를 호출하지 않는지 검증했다.
- `DrRunExecutorImplTest`를 추가했다.
  - KVM FTCTL run 성공 시 run이 `SUCCEEDED`로 끝나고 `execute` step의 running/terminal 기록, `RUN_STARTED`/`RUN_SUCCEEDED` event, projection refresh가 남는지 검증했다.
  - KVM FTCTL adapter가 없을 때 run이 `DR_ENGINE_UNAVAILABLE`로 실패하고 projection refresh를 호출하지 않는지 검증했다.

검증 결과:

- Windows 작업트리 기준 `git diff --check` 통과
- WSL ext4 작업트리 `/home/ablecloud/work/dhslove/ablestack-cloud-cross-dr-step6-wt`에서 Maven 모듈 test 성공
  - 명령: `mvn -pl plugins/integrations/disaster-recovery -am -Dcheckstyle.skip -Drat.skip=true -Dtest=FtctlDrActionAdapterTest,DrRunExecutorImplTest -DfailIfNoTests=false test`
  - 결과: `BUILD SUCCESS`
  - 테스트: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`
  - 완료 시각: `2026-06-30T23:43:13+09:00`

경계:

- 기존 `plugins/integrations/ftctl-service` service/action 구현은 변경하지 않았다.
- qemu/ftctl repo와 host runtime script는 변경하지 않았다.
- 기존 rbd->rbd, rbd->qcow2, qcow2->rbd, qcow2->qcow2 FTCTL 성공 로직은 직접 변경하지 않았다.
- 이번 단계는 code-level smoke와 회귀 체크리스트 작성까지이며, 10.10.32 클러스터 배포/live PASS 검증은 수행하지 않았다.

다음 단계:

- `7. Cloud UI` 구현
- 구현 대상:
  - DR Sites/Plans/Run 화면과 API 연동
  - 실행 이력/step/event 표시
  - 기존 Fault Protection UI와 충돌하지 않는 navigation 배치
  - 다크모드 대응 스타일 적용

남은 단계:

- `4`

### 7단계: Cloud UI

상태: 완료

계획:

- 신규 Cross Hypervisor DR Sites/Plans 화면을 Cloud API 기반으로 추가한다.
- DR plan detail에서 overview, restore point, replica, run, event projection을 확인할 수 있게 한다.
- VM 상세 화면에서 해당 VM과 연결된 DR plan projection을 확인하고 허용된 action만 실행할 수 있게 한다.
- UI는 host, qemu, FTCTL runtime을 직접 호출하지 않고 신규 Cloud DR API만 호출한다.
- 다크모드에서도 상태 pill, KPI, topology, event/runtime code block이 읽히도록 공통 스타일을 추가한다.

구현 내용:

- Cloud DR API wrapper를 추가했다.
  - `ui/src/api/dr.js`
  - `listDrSites`, `getDrSite`, `checkDrSite`, `createDrSite`
  - `listDrPlans`, `getDrPlan`, `createDrPlan`
  - `listDrRuns`, `getDrRun`, `listDrRunSteps`, `listDrEvents`
  - `listDrReplicas`, `listDrRestorePoints`
  - `startDrAction`, `extractDrList`, `extractDrObject`, `normalizeActionEligibility`
- DR 공통 UI component를 추가했다.
  - `ui/src/components/dr/DrStatusPill.vue`
  - `ui/src/components/dr/DrRpoKpi.vue`
  - `ui/src/components/dr/DrRunProgress.vue`
  - `ui/src/components/dr/DrTopology.vue`
  - `ui/src/components/dr/DrActionToolbar.vue`
- Infrastructure navigation에 신규 route entry를 추가했다.
  - `ui/src/config/section/infra/drSite.js`
  - `ui/src/config/section/infra/drPlan.js`
  - `ui/src/config/section/infra.js`
- 신규 Infrastructure DR 화면을 추가했다.
  - `ui/src/views/infra/dr/DrSiteList.vue`
  - `ui/src/views/infra/dr/DrPlanList.vue`
  - `ui/src/views/infra/dr/DrPlanOverview.vue`
  - `ui/src/views/infra/dr/DrRestorePointsTab.vue`
  - `ui/src/views/infra/dr/DrReplicaTab.vue`
  - `ui/src/views/infra/dr/DrRunsTab.vue`
  - `ui/src/views/infra/dr/DrEventsTab.vue`
- VM 상세 화면에 DR plan projection tab을 추가했다.
  - `ui/src/views/compute/dr/DrPlanVmTab.vue`
  - `ui/src/views/compute/InstanceTab.vue`
- 공통 dark-mode 대응 스타일과 locale key를 추가했다.
  - `ui/src/style/cross-dr.less`
  - `ui/src/style/index.less`
  - `ui/public/locales/en.json`
  - `ui/public/locales/ko_KR.json`

검증 결과:

- Windows 작업트리 기준 `git diff --check` 통과
- WSL ext4 작업트리 기준 locale JSON parse 통과
  - `ui/public/locales/en.json`
  - `ui/public/locales/ko_KR.json`
- WSL ext4 작업트리 기준 변경 UI 파일 targeted eslint 통과
  - 명령: `npx eslint src/api/dr.js src/components/dr src/config/section/infra.js src/config/section/infra/drPlan.js src/config/section/infra/drSite.js src/views/compute/InstanceTab.vue src/views/compute/dr/DrPlanVmTab.vue src/views/infra/dr --ext .js,.vue`
- WSL ext4 작업트리 기준 production build 성공
  - 명령: `NODE_OPTIONS=--openssl-legacy-provider npm run build`
  - 결과: `DONE Build complete. The dist directory is ready to be deployed.`
- 전체 `npm run lint -- --no-fix`는 신규 UI 파일까지 도달하기 전에 기존 tracked test 파일 `tests/unit/views/compute/RegisterFtctlProtection.spec.js`의 들여쓰기 오류에서 중단됐다.
  - 중단 위치: line 60-65
  - 이 파일은 7단계 변경 범위가 아니므로 수정하지 않았다.

경계:

- 기존 Fault Protection UI, legacy DR cluster UI, FTCTL service 성공 경로, qemu/ftctl host runtime script는 변경하지 않았다.
- 이번 단계는 Cloud UI source/build 검증까지이며, management 서버 배포와 10.10.32 live UI 확인은 수행하지 않았다.
- VM 상세 DR plan projection은 현재 응답의 VM 식별자가 충분할 때 client-side best-effort로 매칭한다. backend response에 source VM UUID/display projection이 보강되면 이 tab의 정확도를 더 높일 수 있다.
- 상세 wizard/preflight UX는 기본 create modal과 readiness check action까지만 연결했다. 고급 단계형 wizard polish는 통합 검증/운영 UX 단계에서 확장 가능하다.

다음 단계:

- `8. VMware target Phase 1` 구현
- 구현 대상:
  - VMware target site readiness skeleton
  - VMware adapter registry/readiness response 연결
  - KVM source to VMware target plan validation의 최소 수직 경로
  - 기존 KVM-to-KVM FTCTL 성공 경로와 분리되는 guardrail 보강

남은 단계:

- `3`

### 8단계: VMware target Phase 1

상태: 완료

계획:

- VMware target site와 `KVM_TO_VMWARE` plan의 최소 readiness/skeleton 경로를 구현한다.
- 실제 VMDK 변환, datastore upload, vCenter VM 생성은 Phase 1 범위 밖으로 두고, 이후 VMware service/SDK 연결 지점을 details JSON에 명확히 남긴다.
- `SYNC` 실행 시 target mapping을 검증하고 `DrReplica.state=SKELETON_READY` record를 idempotent하게 생성/갱신한다.
- `TARGET_READY` restore point 전에는 `testFailover`, `failover`, `failback`, `reprotect`, `adoptReplica`를 열지 않는다.
- 기존 FTCTL KVM-to-KVM 성공 경로와 action adapter를 직접 변경하지 않는다.

구현 내용:

- VMware Phase 1용 engine/상태/error 상수를 추가했다.
  - `ENGINE_TYPE_VMWARE_PHASE1`
  - `ENGINE_BINDING_TYPE_VMWARE_PHASE1`
  - `DIRECTION_KVM_TO_VMWARE`
  - `REPLICA_STATE_SKELETON_READY`
  - `DR_TARGET_UNAVAILABLE`, `DR_TARGET_MAPPING_INVALID`, `DR_TARGET_OWNERSHIP_CONFLICT`, `DR_TARGET_NOT_READY`, `DR_CREDENTIAL_INVALID`
- `DrPlanServiceImpl.getActionEligibility`에 VMware Phase 1 guardrail을 추가했다.
  - `sync`는 engine이 있고 active run이 없으면 허용
  - `testFailover`와 `failover`는 `targetReadyAt` 전까지 비활성화
  - `failback`, `reprotect`, `adoptReplica`는 Phase 1에서 비활성화
  - FTCTL plan의 기존 action surface는 유지
- `VmwarePhase1TargetAdapter`를 추가했다.
  - `DrReplicationEngine` 구현체로 `VMWARE_PHASE1/VMWARE_PHASE1` registry key를 사용
  - `validatePlan`에서 `KVM_TO_VMWARE`, source KVM, target VMware, vCenter endpoint 또는 `vmwareDatacenterId`, backend-managed vCenter credential, mapping JSON을 검증
  - mapping 필수 항목: `targetDatastoreRef`, `resourcePoolRef` 또는 `clusterRef`, `targetFolderPath`, `targetNetworkRef`
  - `PRODUCTION_ON_FAILOVER` network connect mode는 Phase 1 skeleton에서 거부
  - `SYNC` 실행 시 `DrReplica`를 `SKELETON_READY`/`POWERED_OFF`/`VMware`로 기록
  - `targetExternalRef`는 실제 MoRef가 준비되기 전까지 `vmware-phase1://...` ownership placeholder로 기록
  - `runtimeStateJson`에 ownership marker, mapping, `vcenterOperation=NOT_STARTED`, `materializationState=NOT_STARTED`, `targetReady=false`를 기록
- Spring registry에 `vmwarePhase1TargetAdapter`를 등록했다.
  - `disaster-recovery` context의 `replicationEngines`에 FTCTL adapter와 함께 추가
- 단위 테스트를 추가/보강했다.
  - `VmwarePhase1TargetAdapterTest`
  - `DrPlanServiceImplTest`
  - 기존 `FtctlDrActionAdapterTest`, `DrRunExecutorImplTest`를 같이 실행해 FTCTL 회귀를 확인

검증 결과:

- Windows 작업트리 기준 `git diff --check` 통과
- WSL ext4 작업트리 `/home/ablecloud/work/dhslove/ablestack-cloud-cross-dr-step6-wt`에서 Maven 모듈 test 성공
  - 명령: `mvn -pl plugins/integrations/disaster-recovery -am -Dcheckstyle.skip -Drat.skip=true -Dtest=VmwarePhase1TargetAdapterTest,DrPlanServiceImplTest,FtctlDrActionAdapterTest,DrRunExecutorImplTest -DfailIfNoTests=false test`
  - 결과: `BUILD SUCCESS`
  - 테스트: `Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`
  - 완료 시각: `2026-07-01T00:26:53+09:00`

경계:

- 이번 단계는 vCenter API로 실제 powered-off VM을 생성하지 않는다. 현재 구현은 VMware service/SDK 연결 전 readiness와 skeleton tracking record를 만드는 Phase 1 backend guardrail이다.
- 실제 VMDK 변환, upload, disk attach, CBT/VADP, source snapshot copy, 운영 failover/failback/reprotect는 구현하지 않았다.
- `TARGET_READY` restore point와 replica disk readiness가 없으면 failover/test failover는 API adapter와 UI eligibility 양쪽에서 차단된다.
- 기존 `FtctlDrActionAdapter`, `plugins/integrations/ftctl-service`, qemu/ftctl runtime script는 변경하지 않았다.

다음 단계:

- `9. V2K 연계` 구현
- 구현 대상:
  - V2K phase1/phase2 실행 결과를 `DrRunStep`으로 표현
  - `VMWARE_TO_KVM` direction guardrail 추가
  - 기존 V2K/import 경로를 직접 재작성하지 않는 adapter wrapper 설계
  - V2K 미구현 action은 명확한 `DR_ACTION_UNSUPPORTED` 또는 phase-specific error로 차단

남은 단계:

- `2`

### 9단계: V2K 연계

상태: 완료

계획:

- `VMWARE_TO_KVM` direction을 `V2K` replication engine으로 연결한다.
- 기존 `importUnmanagedInstanceForAblestackV2K`/`import_vm_task` 실행 경로는 재작성하지 않는다.
- `DrPlan.engineBindingId` 또는 run/mapping JSON의 `importVmTaskId`/`importVmTaskUuid`로 기존 V2K import task를 참조한다.
- `SYNC`는 V2K Phase1 완료 여부를 `DrRunStep`에 기록하고, Phase1 완료 시 `DrPlan.state=PHASE1_READY`로 표시한다.
- `FAILOVER`는 V2K Phase2 완료 여부를 `DrRunStep`에 기록하고, Phase2 완료 전에는 phase-specific error로 차단한다.
- `FAILBACK`, `REPROTECT`, `ADOPT`, `TEST_FAILOVER`는 초기 범위에서 명확히 unsupported로 차단한다.
- 기존 FTCTL KVM-to-KVM 성공 경로와 VMware Phase 1 skeleton 경로는 직접 변경하지 않는다.

구현 내용:

- V2K용 공통 상수와 phase/error 상수를 추가했다.
  - `ENGINE_TYPE_V2K`
  - `ENGINE_BINDING_TYPE_V2K`
  - `DIRECTION_VMWARE_TO_KVM`
  - `PLAN_STATE_PHASE1_READY`
  - `REPLICA_STATE_PHASE1_READY`
  - `EVENT_SOURCE_V2K`
  - `DR_V2K_TASK_NOT_FOUND`, `DR_V2K_PHASE1_REQUIRED`, `DR_V2K_PHASE2_REQUIRED`
- `DrPlanServiceImpl.getActionEligibility`에 V2K guardrail을 추가했다.
  - `sync`는 engine이 있고 active run이 없으면 허용
  - `failover`는 `PHASE1_READY` 또는 `targetReadyAt` 이후에만 허용
  - `testFailover`, `failback`, `reprotect`, `adoptReplica`는 V2K 초기 범위에서 비활성화
  - FTCTL plan의 기존 action surface는 유지
- `V2kDrMigrationAdapter`를 추가했다.
  - `DrReplicationEngine` 구현체로 `V2K/V2K` registry key를 사용
  - `validatePlan`에서 `VMWARE_TO_KVM`, source VMware site, target KVM site를 검증
  - `SYNC` 실행 시 기존 `import_vm_task`를 찾아 `v2k-phase1` step을 기록
  - `Phase1_Completed` 또는 동등한 phase/migration state를 확인하면 `DrReplica.state=PHASE1_READY`, `DrPlan.state=PHASE1_READY`, `targetReadyAt`을 갱신
  - `FAILOVER` 실행 시 기존 `import_vm_task`의 phase2 상태를 확인해 `v2k-phase2` step을 기록
  - Phase1 미완료는 `DR_V2K_PHASE1_REQUIRED`, Phase2 미완료는 `DR_V2K_PHASE2_REQUIRED`, 진행 중 task는 `DR_ENGINE_BUSY`로 변환
  - Phase2 완료 시 `DrReplica.state=FAILED_OVER`, `DrPlan.state=FAILED_OVER`로 갱신
  - task details에는 V2K step, current phase, migration state/step, workdir, target storage, target VM id/name을 포함하되 credential 필드는 노출하지 않음
- Spring registry에 `v2kDrMigrationAdapter`를 등록했다.
  - `disaster-recovery` context의 `replicationEngines`에 FTCTL adapter, VMware Phase1 adapter와 함께 추가
- 단위 테스트를 추가/보강했다.
  - `V2kDrMigrationAdapterTest`
  - `DrPlanServiceImplTest`
  - 기존 `VmwarePhase1TargetAdapterTest`, `FtctlDrActionAdapterTest`, `DrRunExecutorImplTest`를 같이 실행해 회귀를 확인

검증 결과:

- Windows 작업트리 기준 `git diff --check` 통과
- WSL ext4 작업트리 `/home/ablecloud/work/dhslove/ablestack-cloud-cross-dr-step6-wt`에서 Maven 모듈 test 성공
  - 명령: `mvn -pl plugins/integrations/disaster-recovery -am -Dcheckstyle.skip -Drat.skip=true -Dtest=V2kDrMigrationAdapterTest,VmwarePhase1TargetAdapterTest,DrPlanServiceImplTest,FtctlDrActionAdapterTest,DrRunExecutorImplTest -DfailIfNoTests=false test`
  - 결과: `BUILD SUCCESS`
  - 테스트: `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`
  - 완료 시각: `2026-07-01T01:35:06+09:00`

경계:

- 이번 단계는 기존 V2K 실행 명령을 직접 생성하거나 `ImportUnmanagedInstanceForAblestackV2KCmd`를 내부에서 조립하지 않는다. 기존 V2K phase1/phase2 작업은 기존 API/import task 경로가 계속 소유한다.
- DR adapter는 기존 V2K task의 phase 상태를 추적하고 DR plan/run/replica 상태로 투영하는 wrapper다.
- Phase2 시작 자체는 아직 `importUnmanagedInstanceForAblestackV2K split=phase2 taskaction=phase2` 경로를 사용해야 한다.
- V2K reverse failback/reprotect는 별도 reverse path 설계 전까지 지원하지 않는다.
- 기존 `FtctlDrActionAdapter`, `plugins/integrations/ftctl-service`, qemu/ftctl runtime script는 변경하지 않았다.

다음 단계:

- `10. 통합 검증/배포 준비` 진행
- 구현 대상:
  - 전체 변경 파일 기준 module/package smoke 재정리
  - 배포 대상 변경 class/resource 목록 정리
  - Cloud changed Maven module 산출물 기준 반영 절차 문서화
  - qemu/ftctl 변경 필요 여부 최종 재확인
  - live 배포 전/후 검증 체크리스트 정리

남은 단계:

- `1`

### 10단계: 통합 검증/배포 준비

상태: 완료

계획:

- 전체 변경 범위 기준으로 Cloud backend Maven module package smoke를 수행한다.
- UI 정적 bundle build smoke를 수행하고, DR UI marker가 산출물에 포함되는지 확인한다.
- 배포 시 반영해야 하는 Cloud 변경 class/resource, UI 정적 asset, DB schema 변경 범위를 문서화한다.
- qemu/ftctl repo 변경 필요 여부를 최종 확인한다.
- 실제 10.10.32 클러스터 배포 전/후 체크리스트와 rollback 기준을 별도 문서로 남긴다.

구현 내용:

- 통합 스모크와 배포 준비 문서를 추가했다.
  - `docs/ftctl/513-cross-hypervisor-dr-integration-smoke-and-deployment-prep-20260701.md`
- Cloud backend 배포 단위를 `plugins/integrations/disaster-recovery` Maven module 산출물 기준으로 정리했다.
- Maven package 산출물에 다음 핵심 class/resource가 포함됨을 확인했다.
  - `com/cloud/dr/DrConstants.class`
  - `com/cloud/dr/DrPlanServiceImpl.class`
  - `com/cloud/dr/adapter/ftctl/FtctlDrActionAdapter.class`
  - `com/cloud/dr/adapter/vmware/VmwarePhase1TargetAdapter.class`
  - `com/cloud/dr/adapter/v2k/V2kDrMigrationAdapter.class`
  - `META-INF/cloudstack/disaster-recovery/spring-disaster-recovery-context.xml`
- UI build 산출물에 다음 핵심 marker가 포함됨을 확인했다.
  - `fetchDrPlans`
  - `DrRunProgress`
  - `cross-dr`
  - `listDrSites`
  - `startDrSync`
  - `label.dr.plan.add`
  - `label.dr.failover.confirm`
  - `message.dr.failoverStarted`
- 이번 10단계에서는 qemu/ftctl runtime contract 변경이 없어서 qemu/ftctl source, package, host script 배포 대상은 없다고 정리했다.

검증 결과:

- Windows 작업트리 기준 `git diff --check` 통과
  - 대상: `docs/ftctl`, `setup/db/create-schema.sql`, `engine/schema`, `plugins/integrations/disaster-recovery`, `ui`
- WSL ext4 작업트리 `/home/ablecloud/work/dhslove/ablestack-cloud-cross-dr-step6-wt`에서 Cloud backend Maven package smoke 성공
  - 명령: `mvn -pl plugins/integrations/disaster-recovery -am -Dcheckstyle.skip -Drat.skip=true -DskipTests package`
  - 결과: `BUILD SUCCESS`
  - 완료 시각: `2026-07-01T11:19:27+09:00`
  - 산출물: `/home/ablecloud/work/dhslove/ablestack-cloud-cross-dr-step6-wt/plugins/integrations/disaster-recovery/target/cloud-plugin-integrations-disaster-recovery-4.22.0.0-SNAPSHOT.jar`
  - 크기: `308K`
  - SHA256: `d43593942a8d21b95d934f6cb6b6cafbbbfaf0cee901962b87bdb11c2e63b203`
- WSL ext4 작업트리 `/home/ablecloud/work/dhslove/ablestack-cloud/ui`에서 UI build smoke 성공
  - 명령: `NODE_OPTIONS=--openssl-legacy-provider npm run build`
  - 결과: `DONE Build complete. The dist directory is ready to be deployed.`
  - 산출물: `/home/ablecloud/work/dhslove/ablestack-cloud/ui/dist`
  - 크기: `54M`
  - 선별 정적 파일 수: `483`
  - Vue CLI asset size warning은 기존 대형 vendor/locales/app bundle 경고이며 build failure는 아니다.

경계:

- 실제 10.10.32 클러스터 배포, DB migration 적용, Cloud management 재시작, UI webapp 반영은 수행하지 않았다.
- Cloud full build는 수행하지 않았다. 이번 검증은 사용자 원칙에 맞춰 변경 Maven module package smoke와 UI build smoke까지만 수행했다.
- qemu/ftctl repo는 이번 단계에서 source 변경, GitHub Actions package build, host 배포 대상이 없다.
- 배포 시 active UI 경로는 `/usr/share/cloudstack-management/webapp`이며, `WEB-INF`를 보존하고 정적 asset만 반영해야 한다.

다음 단계:

- 구현 계획 기준 10단계가 모두 완료되었다.
- 사용자가 요청하면 이 상태에서 commit/push 또는 10.10.32 클러스터 대상 배포/검증 단계로 전환한다.

남은 단계:

- `0`

### 2026-07-02 추가 구현: DR 상세 화면 표준화

상태: 완료

구현 내용:

- DR Site/Plan 상세 화면의 custom left card와 `a-descriptions bordered` 기반 상세 표를 제거했다.
- `DrResourceInfoCard.vue`를 추가해 볼륨 상세의 `vm-info-card`, `resource-details`, `resource-detail-item` class 계약을 따르는 DR 전용 좌측 정보 카드를 제공한다.
- `DrResourceDetailsTab.vue`를 추가해 볼륨 상세의 row 기반 `DetailsTab` 계열과 같은 label/value 목록을 제공한다.
- `DrSiteList.vue`, `DrPlanList.vue`, `DrPlanOverview.vue`를 표준 컴포넌트 기반으로 연결했다.
- 상세 첫 탭은 `details`/`label.details`를 사용하며, 기존 `overview` URL state는 `details`로 normalize한다.
- `cross-dr.less`에 DR 표준 상세 row/card의 줄바꿈, 링크 색상, 다크모드 대비 보강 스타일을 추가했다.

계층별 판단:

| 계층 | 변경 | 판단 |
|---|---|---|
| UI | 있음 | 화면 표준화 대상 |
| API | 없음 | 기존 `DrSiteResponse`, `DrPlanResponse` 필드를 그대로 사용 |
| Backend | 없음 | action/state/run 흐름 변경 없음 |
| Agent | 없음 | host command 계약 변경 없음 |
| ftctl | 없음 | runtime profile/status/event 계약 변경 없음 |
| DB | 없음 | 신규 스키마/데이터 변경 불필요 |

검증 기준:

- UI build 성공
- 활성 webapp에 `dr-standard-info-card`, `dr-standard-detail-row` marker 반영
- `/usr/share/cloudstack-management/webapp/WEB-INF` 보존
- management `/client/` HTTP 200 응답
