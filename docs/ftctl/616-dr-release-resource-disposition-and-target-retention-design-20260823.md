# DR Release Resource Disposition And Target Retention Design

## 1. 목표

보호 관계 종료 시 운영자가 대상 자원의 운명을 명시적으로 선택하게 한다. 기본값은
항상 보존이며, 대상 VM을 새 운영 원본으로 사용할 수 있는 상태에서는 삭제 선택을
허용하지 않는다. DR 계획 삭제는 메타데이터 정리이고 물리 자원 삭제 수단이 아니다.

## 2. 사용자 시나리오

### 2.1 대기 복제 자원 제거

원본 사이트가 정상이고 DR 보호만 완전히 철거하는 경우 `보호 관계 종료`에서
`대기 복제 가상머신과 디스크 삭제`를 선택한다. 서버는 target authority가 아니고,
VM이 Stopped이며, 계획이 소유한 VM과 volume인지 확인한 후에만 삭제한다.

### 2.2 대상 VM을 새 원본으로 보존

원본 사이트가 파괴되어 target으로 failover한 경우 `대상 가상머신 보존`만 허용한다.
보호 관계를 종료해도 VM, volume, NIC, network는 남는다. 운영자는 보존된 VM을
새 source workload로 선택해 별도의 DR 계획을 구성할 수 있다.

## 3. UI 계약

- 메뉴 이름은 `보호 관계 종료`다.
- 모달의 기본 선택은 `대상 가상머신 보존`이다.
- `대기 복제 가상머신과 디스크 삭제`는 SOURCE authority에서만 선택할 수 있다.
- TARGET authority 또는 `FAILED_OVER*` 상태에서는 삭제 라디오를 비활성화하고
  보존 이유를 표시한다.
- 삭제 선택은 VM과 plan-owned volume이 영구 삭제됨을 경고한다.
- `DR 계획 삭제`는 계획 메타데이터만 삭제하고 VM, 디스크, 네트워크는 삭제하지
  않는다는 확인 문구를 사용한다.
- dark mode는 Ant Design/Cloud UI token을 사용하고 disabled 항목의 대비를 낮춘다.
- release가 `UNPROTECTED / DISABLED`로 종결되면 UI는 중지된 scheduler와 삭제된
  replica projection을 장애로 해석하지 않는다. 계획의 terminal protection state를
  runtime health보다 우선해 `UNPROTECTED`로 표시한다.

## 4. API와 DB 계약

`releaseDrProtection`에 `resourcedisposition`을 추가한다.

| 값 | 기본값 | 동작 |
| --- | --- | --- |
| `RETAIN_OPERATIONAL_VM` | 예 | 물리 자원 보존 |
| `DELETE_STANDBY_REPLICA` | 아니오 | 검증된 대기 복제 VM/volume 삭제 |

처분 값은 기존 `dr_run.request_json.resourceDisposition`에 저장하므로 schema 변경은
필요하지 않다. 별도 임시 UI 상태를 삭제 판단의 근거로 사용하지 않는다.

## 5. Backend 처리 순서

1. API admission에서 값, plan, authority, 대상 VM state, ownership을 검증한다.
2. Agent를 통해 FTCTL `dr-release`를 비동기로 실행한다.
3. FTCTL terminal release 증거를 받은 뒤 Cloud executor가 처분을 다시 검증한다.
4. 보존 모드는 Cloud 자원을 변경하지 않는다.
5. 삭제 모드는 plan-owned target VM을 destroy/expunge하고 plan-owned volume을
   destroy한 뒤 replica/disk ownership 상태를 갱신한다.
6. 처분 완료 후 Run을 terminal success로 종결한다. 삭제 실패는 성공으로 숨기지
   않고 명시적 release cleanup failure가 된다.

## 6. 안전 조건

- target authority 또는 `FAILED_OVER*`: 삭제 금지
- source VM ID와 target VM ID 동일: 삭제 금지
- ownership 불일치: 삭제 금지
- target VM이 Stopped가 아님: 삭제 금지
- 없는/이미 삭제된 자원: 멱등 성공
- FTCTL은 Cloud 자원을 직접 삭제하지 않음

## 7. 테스트

### Case A: 종료와 대상 삭제

UI에서 SOURCE authority 계획의 보호 관계 종료를 열고 삭제를 선택한다. Run과
release tombstone 성공, active replica/claim 종료, target VM 및 plan-owned volume
삭제, UI 목록 제거를 확인한다.

### Case B: 종료와 대상 보존

UI에서 failover 완료 계획의 보호 관계 종료를 연다. 삭제 선택이 비활성화되어야
한다. 보존으로 실행한 후 Run은 성공하고 target VM, volume, NIC, network가 남으며
VM이 새 source workload 후보로 조회되어야 한다.

두 케이스 모두 release 후 목록과 상세 상태는 `UNPROTECTED`여야 한다. 의도적으로
종료된 scheduler의 `STOPPED/DEAD` 또는 제거된 active replica는 `DEGRADED` 판정의
근거가 될 수 없다.

## 8. AS-IS / TO-BE

| 구분 | AS-IS | TO-BE |
| --- | --- | --- |
| 보호 종료 | 자원 처분 선택 없음 | 보존/대기 복제 삭제 명시 |
| 기본값 | 의미가 암묵적 | 보존 기본값 |
| 운영 target | 실수 가능성 설명 부족 | 삭제 UI/API 모두 차단 |
| 실제 삭제 | 명확한 소유권 계약 없음 | Cloud 소유 자원만 삭제 |
| 계획 삭제 | 자원 효과 혼동 | 메타데이터 전용으로 명시 |
| 감사 증거 | 처분 의도 누락 | Run request와 FTCTL tombstone 기록 |
| 종료 후 UI | 정지 scheduler를 성능 저하로 오인 가능 | `UNPROTECTED` terminal 상태 우선 |

## 9. 테스트 환경 검증 결과

2026-08-23에 32 클러스터 UI에서 두 처분 경로를 실제 실행했다. API를 직접
호출해 동작을 대신하지 않았으며, 로그인 후 `보호 관계 종료` 모달에서 처분 방식,
사유, 확인 문구를 선택해 제출했다.

| 케이스 | 계획 / Run | Cloud 결과 | 자원 결과 | FTCTL 결과 | 판정 |
| --- | --- | --- | --- | --- | --- |
| 보존 | `utest1 DR Plan` / `2a578066-42bd-4f95-b807-3f8b0cfe7ea0` | `UNPROTECTED / DISABLED`, Run `SUCCEEDED` | `utest1-dr` VM `Stopped`, volume `Ready`, `removed IS NULL` | `RELEASED / UNPROTECTED / STOPPED`, `RETAIN_OPERATIONAL_VM` | PASS |
| 삭제 | `r9-01 DR Plan` / `fc248a22-51f1-42b4-b28a-cde6200e3540` | `UNPROTECTED / DISABLED`, Run `SUCCEEDED` | VM `Expunging`, plan-owned volumes `Expunged` | `RELEASED / UNPROTECTED / STOPPED`, `DELETE_STANDBY_REPLICA` | PASS |

보존된 VM은 표준 가상머신 목록에서 `utest1-dr (i-2-274-VM)`로 조회되며 Cloud가
계속 관리한다. 새 원본으로 전환할 때는 기존 원본의 격리를 먼저 확인하고 이 VM을
기동한 뒤 새 DR 계획을 구성한다. 이번 검증에서는 중복 IP와 split-brain을 피하기
위해 보존 VM을 임의 기동하지 않았다.

두 계획 모두 UI 목록에서 `UNPROTECTED`로 표시되고, release 이후 계획별 scheduler
프로세스가 남지 않았다. 관리 로그에서 두 Release Run과 연관된 오류/예외는 없었다.
동일 UI 정적 자산은 32/22 관리 서버에 배포했으며 두 서버 모두 `/client/` HTTP 200,
`WEB-INF` 보존, FTCTL UI marker 존재를 확인했다.
