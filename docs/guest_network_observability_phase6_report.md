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

# Guest Network Observability Phase 6 완료 보고서

## 1. 결과

Phase 6의 repository 통합 검증과 shared 22.x 최소 배포 준비를 완료했다.

- 작업 브랜치: `codex/guest-network-observability`
- 기준 브랜치: `ablestack-europa`
- 기준 동기화: `ablestack-europa...upstream/ablestack-europa = 0 0`
- 기능 기본값: 비활성
- shared 22.x 배포/서비스 재시작: 수행하지 않음
- commit/push/PR: 수행하지 않음

실제 shared 22.x에서만 측정할 수 있는 Management/Agent CPU와 실제
VM start/stop, volume attach/detach, NIC plug/unplug p95는 배포 전 준비 항목이
아니라 배포 후 수락 gate로 분리했다. 측정하지 않은 운영 수치를 통과로 기록하지
않았으며, 실행 방법과 중단 기준은 운영 가이드에 고정했다.

## 2. Phase 6 보강 구현

### 2.1 파일럿 범위 제한

전역 on/off만으로는 shared 환경에서 안전한 단계 활성화가 어려워 다음 설정을
추가했다.

```text
vm.guest.network.details.host.ids=
vm.guest.network.details.zone.ids=
```

- 빈 값은 enabled 상태에서 전체 범위를 의미한다.
- 값이 있으면 지정한 양의 DB ID에 해당하는 VM만 수집한다.
- 오입력으로 유효한 ID가 하나도 없으면 fail-closed로 수집 대상을 0으로 만든다.
- Running KVM User VM 제한과 host/VM/cycle 상한은 그대로 적용한다.

### 2.2 부하 및 migration 자동 검증

- 1,000 VM 입력에서 host cycle 상한 50개만 처리한다.
- batch 10 설정에서 Agent command는 정확히 5회이며 DB persistence는 50회다.
- 4,098 route 입력은 4,096개로 제한하고 `truncated`와 원본 개수를 보존한다.
- fresh schema와 Europa upgrade schema의 column, constraint, index, charset 계약을
  동일한 자동 테스트로 검증한다.
- 잘못된 host/zone scope가 전체 수집으로 확대되지 않는 테스트를 추가했다.

### 2.3 운영 측정 지점

- Agent의 기존 executor monitor에서 `GuestNetwork-Worker`의 workers, active,
  queue, pending, completed를 확인한다.
- KVM wrapper는 VM별 total/interface/route/DNS 실행 시간을 debug log로 남긴다.
- Backend snapshot service는 `CREATED`, `PAYLOAD_UPDATED`, `METADATA_ONLY`
  결과를 debug log로 남긴다.
- 이 측정은 기능 경로 안에서 로그 level이 활성화된 경우에만 출력하며, UI/API
  조회가 Agent 호출을 만드는 구조는 추가하지 않았다.

## 3. 통합 검증 결과

### 3.1 Maven

최종 통합 명령은 Core, Agent, API, Schema, Backend, KVM의 게스트 네트워크 관련
테스트와 기존 StatsCollector 회귀를 한 reactor에서 실행한다.

검증 범위:

- feature disabled: scheduler/worker/QGA/DB interaction 0
- 전용 Agent queue 포화: Basic worker 작업 정상 완료
- mixed guest/core request 차단
- 다중 IPv4/IPv6, DNS, route와 payload 상한
- 한 VM 실패가 같은 batch의 다른 VM을 중단하지 않음
- VM scope NIC 매핑과 API RBAC
- API 반복 조회가 Agent/QGA를 호출하지 않음
- unchanged payload의 snapshot rewrite 0
- fresh/upgrade schema 계약 동일
- 1,000 VM load 상한과 host/zone allowlist fail-closed

최종 결과:

| 모듈 | 테스트 수 | 결과 |
|---|---:|---|
| API | 14 | 통과 |
| Schema | 4 | 통과 |
| Core | 3 | 통과 |
| Agent | 27 | 통과 |
| Backend/Stats | 89 | 통과 |
| KVM | 34 | 통과 |
| 합계 | 171 | 실패/오류/skip 0 |

```text
BUILD SUCCESS
ELAPSED=78.52
MAX_RSS_KB=1181336
```

elapsed와 RSS는 로컬 Maven reactor 자체의 검증 비용이며 실제 Management/Agent
runtime 성능값으로 사용하지 않는다.

### 3.2 UI

```text
npm run lint -- --no-fix
  DONE  No lint errors found!

NODE_OPTIONS=--openssl-legacy-provider npm run build
  DONE  Build complete. The dist directory is ready to be deployed.
```

Node 16과 webpack 4 조합의 OpenSSL 호환을 위해 기존 저장소 검증 방식인
`--openssl-legacy-provider`를 사용했다. locale JSON도 별도 파싱 검증한다.

### 3.3 정적 검증

- `git diff --check`
- UI `en.json`, `ko_KR.json` JSON 파싱
- DB fresh/upgrade table contract test
- 변경 artifact 목록 확인

모든 정적 검증이 통과했다.

## 4. 핵심 명령 격리 판단

로컬 자동화 gate에서 다음을 확인했다.

- 신규 command는 `GuestNetwork-Worker`만 사용한다.
- VM/볼륨/NIC/GetVmStats command와 혼합된 request는 실행 전에 거부한다.
- queue가 active 1 + queued 1로 포화된 상태에서 추가 관측 요청만 reject된다.
- 같은 상태에서 Basic worker의 core task는 1초 이내 정상 완료된다.
- collector의 timeout, persistence exception, 한 VM parser 오류는 다른 VM과 core
  command 결과로 전파되지 않는다.

이는 실행 경로와 backpressure 격리를 증명한다. 실제 libvirt 작업 p95 5% gate는
shared 파일럿 환경에서 off/on 동일 조건으로 측정해야 하며, 아직 통과로 간주하지
않는다.

## 5. DB 및 artifact

DB 변경은 `cloud.vm_guest_network_state` 한 개 테이블이다. 정확한 column, FK,
unique/index, fresh/upgrade 경로는 운영 가이드에 기록했다.

배포 대상은 Core, Agent, KVM, Schema, API, Backend와 UI 정적 artifact다. shared
22.x host에는 전체 최신 jar를 교체하지 않고 현재 배포 jar 백업을 기준으로 변경
class만 반영하는 절차를 사용한다.

## 6. 배포와 rollback 준비

`docs/guest_network_observability_operations.md`에 다음을 작성했다.

- DB clone fresh/upgrade 검증
- 기능 off 상태의 Management-first 최소 배포
- host 한 대의 기존 jar 백업 기반 class 반영
- zone/host/VM 1개 파일럿과 단계 확장
- off/on p95, CPU, queue, DB write, section time gate
- ReadyAnswer와 기존 stats 회귀 확인
- feature flag off 및 artifact 역순 복원
- 신규 table을 즉시 삭제하지 않는 DB rollback 원칙

## 7. QGA OS family 사후 보완

2026-07-26 실제 22.x preflight에서 QGA의 배포판 ID가 `debian`,
`rocky`, `centos`로 반환되고 `kernel-name`은 제공되지 않는 것을
확인했다. 기존 `contains("linux")` dispatch는 이 Linux VM들을 고정
route/DNS adapter 실행 전에 `UNSUPPORTED`로 처리한다.

동일 Debian VM에서 고정 `/usr/sbin/ip` IPv4/IPv6 명령과
`/usr/bin/cat /etc/resolv.conf`는 성공했다. 따라서 기존 command
allowlist와 executor 격리는 유지하고 KVM plugin 내부 OS family 판별만
교체하는 설계를 채택한다.

상세 코드 설계, 테스트와 구현 후 22.x gate는
`docs/guest_network_observability_os_family_design.md`에 기록했다.
보완 구현과 Debian acceptance는 완료했다. Ubuntu fixture 테스트는
통과했지만 현재 클러스터에 실행 표본이 없어 Ubuntu 실환경 gate만 남아 있다.

## 8. 완료 경계

Phase 6의 코드, 자동화 검증, build, migration 계약과 최초 22.x 파일럿은
완료했다. 추가 OS family 보완은 Host 3 KVM plugin의 관련 class 19개와
Management collector class 한 개만 최소 patch해 검증했다.

대상 Debian VM의 최종 snapshot은 interface `OK` 2개, route `OK` 10개,
DNS `OK` 서버 2개이며 전체 상태는 `OK`다. Agent ReadyAnswer, 기존 stats,
관리 API와 호스트 연결을 확인했다. Ubuntu 실환경 gate는 실행 VM이
준비될 때 동일 단일 VM scope로 수행한다.
