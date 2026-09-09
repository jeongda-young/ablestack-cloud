# Cross Hypervisor DR Site-derived Failback 구현 결과

## 1. 목적

이 문서는 `571-cross-hypervisor-dr-site-derived-failback-contract-design-20260725.md`
설계를 실제 코드, 빌드, 배포 환경에 반영한 결과를 기록한다.

일반 Failback은 DR Plan에 이미 등록된 원본 Site와 현재 활성 Site를 사용한다.
UI에서 Mold 유형, API URL, API Key, Secret Key를 다시 입력받지 않는다.

## 2. 적용 범위

### 2.1 UI

- Failback 대화상자에서 다음 수동 입력을 제거했다.
  - Failback 대상 Mold 유형
  - Remote Mold API URL/API Key/Secret Key
  - Target Mold API URL/API Key/Secret Key
- Plan에서 계산한 다음 정보를 읽기 전용으로 표시한다.
  - 현재 활성 Site
  - Failback 목적지 Site
  - 최신 durable checkpoint
  - Site 연결 및 자격증명 준비 상태
- Preflight가 실패하면 확인 버튼을 비활성화하고 차단 원인을 표시한다.
- 실행 요청에는 `force`, `reason`, `acknowledgement`만 포함한다.

### 2.2 API

- `getDrFailbackPreflight` API를 추가했다.
- 응답은 Site, provider, hypervisor, 연결 상태, credential 상태와 checkpoint
  메타데이터만 반환한다.
- `startDrFailback`의 legacy credential 파라미터는 호환 가능한 API 인식만
  유지하고, 값이 전달되면
  `DR_FAILBACK_INLINE_CREDENTIAL_UNSUPPORTED`로 거부한다.

### 2.3 Backend

- `DrFailbackPreflightService`를 추가했다.
- 일반 Failback 경로를 다음과 같이 고정했다.
  - active Site: `plan.targetSite`
  - destination Site: `plan.sourceSite`
- 다음 조건을 실행 전에 검증한다.
  - Plan 상태가 `FAILED_OVER`
  - active side가 `TARGET`
  - 양쪽 Site가 활성 상태이고 health가 `CONNECTED`
  - 양쪽 Site credential이 backend에서 resolve 가능
  - target-ready durable checkpoint 존재
- `FtctlDrUnifiedActionAdapter`가 Agent dispatch 직전에 동일 Preflight를
  다시 실행한다.
- `DrOrchestratorImpl`은 FAILBACK request JSON에서 password, secret,
  token, apiKey 계열 키를 재귀적으로 거부한다.

### 2.4 Agent 및 FTCTL

- Agent/FTCTL 명령 계약은 이미 Site-derived runtime credential 파일을
  지원하므로 소스 변경을 하지 않았다.
- Cloud backend가 Site credential을 resolve하고, Agent는 일시적인
  `credentials.json`을 FTCTL에 전달한다.
- 설치된 FTCTL은 credential 파일을 `0600`으로 저장하고 durable
  profile에는 redacted 값만 남긴다.
- FTCTL은 reverse-copy/finalize data plane을 수행하며 Cloud VM lifecycle과
  Site 선택 권한을 소유하지 않는다.

### 2.5 DB

- 신규 테이블이나 컬럼은 추가하지 않았다.
- 세 schema upgrade 경로에 legacy FAILBACK request JSON의 credential
  필드를 제거하는 idempotent `JSON_REMOVE` 보정 SQL을 추가했다.
- 운영 DB에는 같은 보정 SQL을 직접 적용했다.

## 3. 변경 파일

### 3.1 Cloud Backend/API

- `DrConstants.java`
- `DrFailbackPreflightResult.java`
- `DrFailbackPreflightService.java`
- `DrFailbackPreflightServiceImpl.java`
- `StartDrFailbackCmd.java`
- `GetDrFailbackPreflightCmd.java`
- `DrFailbackPreflightResponse.java`
- `DrOrchestratorImpl.java`
- `FtctlDrUnifiedActionAdapter.java`
- `DisasterRecoveryClusterServiceImpl.java`
- `spring-disaster-recovery-context.xml`

### 3.2 UI

- `ui/src/api/dr.js`
- `ui/src/views/infra/dr/DrPlanList.vue`
- 한국어/영어 locale 파일

### 3.3 DB Upgrade

- `schema-42200to42210.sql`
- `schema-42210to42300.sql`
- `schema-Europa-After.sql`

## 4. 테스트 및 빌드 결과

### 4.1 Maven 변경 모듈 빌드

WSL ext4 clone에서 다음 변경 모듈만 빌드했다.

```text
engine/schema
plugins/integrations/disaster-recovery
```

실행한 테스트:

- `DrFailbackPreflightServiceImplTest`
- `FtctlDrUnifiedActionAdapterTest`
- `DrOrchestratorImplTest`

결과:

```text
BUILD SUCCESS
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
Checkstyle violations: 0
```

### 4.2 UI 빌드

WSL ext4 clone에서 다음 명령으로 빌드했다.

```bash
NODE_OPTIONS=--openssl-legacy-provider npm run build
```

결과:

- build 성공
- bundle에서 `getDrFailbackPreflight` 확인
- 한국어/영어 신규 locale marker 확인

### 4.3 FTCTL Self-test

`10.10.32.2`에서 다음 self-test를 실행했다.

```text
selftest_case_dr_vmware_cbt_preflight_uses_runtime_credentials_file
```

결과:

```text
SELFTEST_RC=0
```

## 5. 배포 결과

### 5.1 Cloud 변경 클래스

- 대상: `10.10.32.10`
- 원칙: 전체 Cloud 패키지 교체 없이 변경 class/resource만 기존 JAR에 반영
- 대상 JAR:
  `/usr/share/cloudstack-management/lib/cloudstack-4.22.0.0-Mold.Europa-202606280754.jar`
- 백업:
  `/root/dr-failback-deploy-20260725-210441`

배포 후 확인:

- `mold` 서비스 `active`
- 신규 API class가 runtime JAR에 존재
- 최근 journal에 fatal 배포 오류 없음

### 5.2 UI

- 대상: `/usr/share/cloudstack-management/webapp`
- `WEB-INF`를 보존하고 정적 UI 산출물만 반영

배포 후 확인:

- `WEB-INF` 존재
- `/client/` HTTP 200
- active bundle에서 `getDrFailbackPreflight` marker 확인

### 5.3 FTCTL

- 이번 변경은 Cloud의 제어권, API 계약, UI 입력 제거 작업이다.
- FTCTL 소스 및 패키지 변경은 없으므로 GitHub Actions 재빌드/재배포를
  수행하지 않았다.
- `10.10.32.1`, `10.10.32.2`, `10.10.32.3` 설치본에서 다음을 확인했다.
  - `ablestack-vm-ftctl.timer=active`
  - `credentials.json` runtime 계약 존재
  - credential 파일 `chmod 0600` 처리 존재

## 6. 라이브 Preflight 검증

검증 Plan:

```text
2514a846-64a2-4bc7-ba88-38a874410782
```

검증 결과:

```text
Plan state: FAILED_OVER
Active side: TARGET
Active Site: 32 ABLESTACK Cluster / CONNECTED / CONFIGURED
Destination Site: 21 VMware ESXi Cluster / CONNECTED / CONFIGURED
Latest durable checkpoint: 439
Preflight ready: true
Active runs: 0
Legacy FAILBACK credential JSON rows: 0
```

실제 UI Failback 대화상자에서도 다음을 확인했다.

- 현재 활성 Site와 목적지 Site가 읽기 전용으로 표시됨
- 최신 durable checkpoint가 표시됨
- legacy Mold selector 및 credential 입력란이 없음
- Preflight가 READY이므로 확인 버튼이 활성화됨

검증 중 실제 Failback 실행은 하지 않았다.

## 7. AS-IS / TO-BE

| 구성요소 | AS-IS | TO-BE |
| --- | --- | --- |
| UI | 사용자가 Failback 시 Mold 유형과 양쪽 credential을 재입력 | Plan 기반 Site 경로와 준비 상태만 표시하고 실행 의도만 입력 |
| API | action 요청에 URL/API Key/Secret Key 전달 가능 | inline credential을 typed error로 거부하고 non-secret 요청만 수신 |
| Backend | action 입력값과 Site credential의 권한이 혼재 | Plan source/target Site에서 경로와 credential을 단일하게 resolve |
| Agent | legacy action field가 섞일 가능성 | backend가 만든 일시적 runtime credential 파일만 전달 |
| FTCTL | 사용자 선택값이 data plane에 유입될 가능성 | reverse-copy/finalize data plane만 수행 |
| DB | FAILBACK request JSON에 secret이 남을 가능성 | 신규 저장 차단 및 기존 legacy key idempotent 제거 |
| 운영 | 실행 시점까지 경로 오류를 발견하기 어려움 | modal 조회와 dispatch 직전의 이중 Preflight |

## 8. 재테스트 준비 판정

판정: **PASS**

근거:

1. Maven 변경 모듈 테스트와 UI build가 성공했다.
2. Cloud 변경 class와 UI가 실제 management runtime에 반영됐다.
3. Mold 서비스, `/client/`, `WEB-INF`가 정상이다.
4. 라이브 API와 UI가 같은 Site-derived 경로와 checkpoint를 표시한다.
5. Plan에 active run이 없고 Preflight가 READY이다.
6. DB에 legacy FAILBACK credential JSON이 없다.
7. 세 호스트의 FTCTL timer와 runtime credential 보안 계약이 정상이다.

다음 재테스트는 이 Plan의 `페일백` 작업을 UI에서 실행하고, async run의
Agent acceptance, reverse-copy, finalize, active-side 전환을 순서대로
검증한다.

## 9. 2026-07-26 실제 Failback 검증 보정

위 PASS는 Site-derived modal/API/credential 배포와 시작 Preflight에 대한
판정이다. 이후 Plan `2514a846-64a2-4bc7-ba88-38a874410782`에서 실제
Failback을 실행한 결과는 다음과 같다.

- FTCTL reverse checkpoint 440: PASS
- Cloud Run 98: `SUCCEEDED`
- 실제 TARGET `i-2-256-VM`: Running
- 실제 SOURCE VMware VM `w22-01`: poweredOff
- FTCTL/Plan 표시: `READY/SOURCE`

따라서 end-to-end 서비스 Failback은 **FAIL**이다. reverse data-plane 완료가
VM lifecycle/authority 완료로 조기 승격되었다.

후속 재테스트 준비 판정:

```text
FAIL - 문서 574의 DATA_READY, Cloud lifecycle, FAILBACK_COMMIT 구현 필요
```

상세 설계는
`574-cross-hypervisor-dr-cloud-owned-failback-lifecycle-commit-design-20260726.md`
를 따른다.
