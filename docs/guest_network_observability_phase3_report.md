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

# Guest Network Observability Phase 3 완료 보고

작성일: 2026-07-24

작업 브랜치: `codex/guest-network-observability`
작업 범위: DB snapshot 조회 API, VM 목록 IP 요약, VM 상세 게스트 네트워크 UI

## 1. 완료 결과

Phase 3의 IP API 및 UI 구현을 완료했다.

- VM 상세 API는 DB에 저장된 최신 guest network snapshot만 읽는다.
- VM 목록은 Cloud 관리 IP와 guest-observed IPv4/IPv6를 구분해 표시한다.
- 다중 NIC와 NIC별 다중 IPv4/IPv6 주소를 누락 없이 상세 탭에 표시한다.
- `STALE`, `PARTIAL`, `STOPPED`, `UNSUPPORTED`, `UNAVAILABLE`, `NOT_COLLECTED` 상태를 화면에서 구분한다.
- UI 조회 과정에서 Agent 또는 host endpoint를 동기 호출하지 않는다.
- Phase 3에는 DB schema 변경이 없다. Phase 2에서 추가한 `vm_guest_network_state`를 읽기만 한다.

DNS와 route는 이번 단계의 목록/상세 응답에 포함하지 않았다. 기존 계획대로 route는 Phase 4, DNS는 Phase 5에서 추가한다.

## 2. 구현 구조

```text
VM 목록/상세 UI
  └─ Cloud API
       ├─ listVirtualMachines?details=guestnetwork
       └─ getVirtualMachineGuestNetworkState
            └─ Backend guest network read service
                 └─ vm_guest_network_state DAO
                      └─ DB snapshot

Agent/Host command: 호출 없음
```

수집 경로와 조회 경로를 분리했다. Phase 2 collector만 전용 Agent command를 실행하며, Phase 3 API는 이미 저장된 snapshot만 반환한다.

## 3. API 및 Backend

### 3.1 상세 API

- 명령: `getVirtualMachineGuestNetworkState`
- 필수 파라미터: `virtualmachineid`
- 권한: Admin, Resource Admin, Domain Admin, User
- RBAC: VM 파라미터에 `ListEntry` ACL을 적용하고 Backend에서 호출 계정의 VM 접근 권한을 다시 검사한다.
- 반환 정보:
  - snapshot status, schema version
  - QGA version
  - last observed, last success
  - error code/message
  - guest interface name, MAC, Cloud NIC ID, loopback 여부
  - 모든 IPv4/IPv6 주소, prefix, scope
  - section별 수집 상태

VM에 snapshot이 없으면 실패시키지 않고 `NOT_COLLECTED`를 반환한다. 저장 payload가 비어 있거나 손상된 경우에도 metadata와 빈 interface 목록을 안전하게 반환한다.

### 3.2 VM 목록 summary

`VMDetails.guestnetwork`를 추가했다.

- `details=guestnetwork` 또는 `details=all`일 때만 목록 응답에 `guestnetwork` summary를 포함한다.
- 기존 `listVirtualMachines` 기본 details에서는 `guestnetwork`를 제외했다.
- 실제 ABLESTACK VM 목록 UI만 `details=guestnetwork`를 명시한다.
- summary에는 상태, 관측 시각, interface 수, 모든 IPv4/IPv6 주소를 포함한다.
- DNS와 route는 목록 payload에 포함하지 않는다.

목록의 snapshot 조회는 VM마다 DAO를 호출하지 않는다. 현재 페이지의 VM ID를 모아 한 번의 `IN` query로 읽으므로 N+1 DB 조회를 만들지 않는다.

### 3.3 Agent 독립성

조회 서비스의 의존성은 다음 세 가지뿐이다.

- `VmGuestNetworkStateDao`
- `UserVmDao`
- `AccountManager`

`AgentManager`, libvirt resource, host command service는 주입하지 않았다. 구조 테스트에서 조회 서비스 field에 `AgentManager` 타입이 없음을 검증했다.

## 4. UI

### 4.1 VM 목록

기존 `ipaddress` 열 안에서 다음 두 줄을 구분한다.

- Cloud: Cloud가 관리하는 기존 NIC IP
- Guest: VM 내부에서 관측된 IPv4/IPv6

게스트 주소는 IPv4와 IPv6를 색상이 다른 Ant Design tag로 표시한다. 처음 세 주소 이후에는 `+N 더보기`와 tooltip으로 나머지 주소를 확인할 수 있다. 수집 상태가 정상이 아니면 주소 옆에 상태 tag를 표시한다.

### 4.2 VM 상세

KVM VM이며 새 API 권한이 노출된 경우에만 `IP 구성` 탭을 표시한다.

- 저장 snapshot 조회라는 안내
- 수집 상태, QGA 버전, 최근 관측/성공 시각, interface 수, schema version
- interface별 이름, loopback 여부, MAC, Cloud NIC ID
- interface에 설정된 모든 IPv4/IPv6와 prefix
- address scope tooltip
- 상태별 warning/error alert
- DB snapshot 재조회 버튼

새로고침 버튼도 동일한 Cloud API를 다시 읽을 뿐 Agent 수집을 즉시 실행하지 않는다.

### 4.3 상태 표현

| 상태 | UI 의미 |
|---|---|
| `OK` | 최신 snapshot 정상 |
| `PARTIAL` | 일부 section만 수집됨 |
| `STALE` | 최근 수집 실패, 마지막 성공 snapshot 표시 |
| `STOPPED` | VM 정지, 마지막 성공 snapshot 유지 |
| `UNSUPPORTED` | 대상 VM 또는 수집 경로 미지원 |
| `UNAVAILABLE` | 현재 수집 불가 |
| `NOT_COLLECTED` | 아직 저장 snapshot 없음 |

영문 및 한국어 locale을 함께 추가했다.

## 5. 부하 최소화 결과

- 기존 API 기본 요청: 신규 DB 조회 0
- UI VM 목록 요청: 페이지당 snapshot batch query 1회
- VM 상세 요청: VM snapshot row 단건 조회 1회
- API/UI 새로고침: Agent 호출 0, host command 0
- DNS/route payload: Phase 3 목록에서 제외
- 기존 stats, VM lifecycle, volume, network command 경로: 변경 없음

## 6. 검증

실행한 검증:

```bash
mvn -pl api \
  -Dtest=GetVirtualMachineGuestNetworkStateCmdTest,ListVMsCmdTest test

mvn -pl engine/schema,server -am \
  -Dtest=VmGuestNetworkStateDaoImplTest,VmGuestNetworkApiServiceImplTest,QueryManagerGuestNetworkTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

NODE_OPTIONS=--openssl-legacy-provider npx eslint \
  src/components/view/GuestNetworkSummary.vue \
  src/components/view/ListView.vue \
  src/views/compute/GuestNetworkTab.vue \
  src/views/compute/InstanceTab.vue \
  src/config/section/compute.js

jq empty public/locales/en.json public/locales/ko_KR.json
NODE_OPTIONS=--openssl-legacy-provider npm run build
git diff --check
```

검증 항목:

- API command 실행, ACL, IPv4/IPv6 serialization
- 기본 VM 목록이 guest snapshot을 읽지 않는 조건
- explicit `details=guestnetwork`
- DAO 단일 `IN` query
- Backend RBAC 및 snapshot response mapping
- 잘못된 저장 payload의 안전한 처리
- 조회 서비스의 Agent 의존성 부재
- 목록 summary 단일 batch service 호출
- Vue lint 및 production build
- 영문/한국어 locale JSON

기존 KVM plugin POM의 중복 `org.json:json` 경고와 Browserslist DB 갱신 안내는 그대로 출력되지만 이번 변경의 오류는 아니다.

## 7. 배포 상태 및 다음 단계

- local WSL 작업 트리에만 구현했다.
- commit, push, PR은 수행하지 않았다.
- 공용 22.x management/UI/host에는 배포하지 않았다.
- service restart와 DB migration은 수행하지 않았다.

다음 단계는 Phase 4의 IPv4/IPv6 route 수집, 정규화, API section 및 UI table 구현이다.
