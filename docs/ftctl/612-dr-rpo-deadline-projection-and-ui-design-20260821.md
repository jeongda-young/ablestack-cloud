# DR RPO Deadline Projection And UI Design

## 1. 목적

FTCTL의 durable deadline 상태를 Cloud가 구조적으로 저장·투영하고 UI가 현재 RPO, 다음 동기화 예정 시각과 초과 상태를 일관되게 표시한다. 그룹 Full Seed 성공과 이후 지속 보호 상태를 분리한다.

## 2. 계층별 계약

### FTCTL / Agent

- `target_rpo_seconds`
- `latest_completed_cycle_sequence`
- `scheduler_next_run_at`
- `scheduler_execution_budget_seconds`
- `scheduler_cycle_wall_duration_seconds`

Agent wrapper는 위 필드를 typed answer로 전달한다. 데이터 전송 명령과 locator는 변경하지 않는다.

### Cloud Backend / DB

- `dr_plan_runtime.scheduler_next_run_at`에 다음 실행 예정 시각을 캐시한다.
- 목표 RPO는 `dr_plan.rpo_seconds`, 현재 age는 최신 durable 시각과 현재 시각으로 계산한다.
- 상태는 `WITHIN_RPO`, `RPO_DUE_SOON`, `OVERDUE`로 구분한다.
- `RPO_DUE_SOON`은 목표의 80% 이상이고 아직 초과하지 않은 상태다.
- 목표 초과 즉시 `rpo_overdue=true`, `protection_state=DEGRADED`로 투영한다. 운영상 grace가 필요하면 별도 필드로 노출하며 목표값 자체를 변경하지 않는다.
- API는 `schedulernextrunat`, `rpostatus`, `rpoageseconds`, `rpooverdue`를 반환한다.

### UI

- 목록 RPO는 `현재 / 목표`를 유지한다.
- 80% 미만은 정상, 80~100%는 `RPO 임박`, 초과는 `RPO 초과`로 표시한다.
- 상세 보호 정보에 `마지막 완료 복제`, `다음 동기화 예정`, `실행 예산`을 표시한다.
- 그룹 결과에는 초기 동기화 결과와 현재 지속 보호 집계를 분리한다.
- 다크 모드에서는 기존 DR status token을 사용하고 고정 밝은 배경색을 사용하지 않는다.

## 3. 동시성 및 캐시

- Backend는 projection poll마다 현재 시각으로 RPO age를 다시 계산한다.
- 동일 authority generation보다 오래된 status는 next-run 캐시를 덮어쓰지 않는다.
- Cycle 완료와 다음 실행 예정 시각 갱신을 같은 runtime projection 트랜잭션으로 처리한다.
- 그룹 완료 progress JSON은 역사적 Full Seed 결과이며 현재 RPO의 원천으로 사용하지 않는다.

## 4. 검증

1. 79%, 80%, 100%, 100% 초과 경계 테스트.
2. stale projection이 새 next-run을 덮어쓰지 않는 테스트.
3. 세 VM 동시 10 Cycle에서 목록/상세 RPO와 DB durable 시각 비교.
4. 밝은/어두운 테마에서 정상, 임박, 초과 pill과 텍스트 대비 확인.
5. 기존 그룹 3/3 terminal, active lease 0 및 VMware -> ABLESTACK(RBD) 회귀 확인.

## 5. AS-IS / TO-BE

| 계층 | AS-IS | TO-BE |
|---|---|---|
| Backend 판정 | 목표 + 암묵적 grace 뒤 초과 | 목표 초과 즉시 DEGRADED, grace 별도 |
| 다음 실행 | API/UI에서 알 수 없음 | typed status, DB cache, API 표시 |
| UI 상태 | 현재/목표 숫자 중심 | 정상/임박/초과와 예정 시각 |
| 그룹 표시 | Full Seed 성공이 중심 | 초기 성공과 지속 보호 분리 |
| 다크 모드 | 기존 개별 스타일 의존 | DR 상태 token 일관 사용 |
