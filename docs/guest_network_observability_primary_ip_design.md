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

# Guest Network Observability 주·보조 IP 및 대표 IP 설계

작성일: 2026-07-27

작업 브랜치: `codex/guest-network-observability`

상태: 구현 및 22.x 배포·인증 UI 검증 완료

## 1. 요구사항과 우선순위

- NIC에 설정된 주 IP와 보조 IP를 구분한다.
- 가상머신 대표 IP는 주 IP만 사용한다.
- QGA가 역할을 확인할 수 있으면 QGA 주 IP가 Cloud 주 IP보다 우선한다.
- QGA가 역할을 확인하지 못할 때만 기본 Cloud NIC의 주 IP를 fallback한다.
- QGA 역할과 Cloud 역할이 다르면 한쪽을 덮어쓰지 않고 둘 다 표시한다.

```text
QGA 주 IP 확인 성공
  └─ QGA 주 IP → 가상머신 대표 IP

QGA 주 IP 미확인
  └─ 기본 Cloud NIC 주 IP → 대표 IP fallback
```

## 2. 22.x 대상 VM Preflight

대상 VM은 `665980f9-423f-4914-9c90-bd4c1ae2de22`
(`i-2-608-VM`, Host `10.10.22.3`)이다. 서비스 재시작, VM 변경, 패키지
설치 없이 읽기 전용 QGA 명령만 실행했다.

표준 `guest-network-get-interfaces`는 eth0의 주소 네 개를 반환했지만
주·보조 속성은 제공하지 않았다. Linux 고정 allowlist
`/usr/sbin/ip -j address show` 결과는 다음과 같았다.

| 주소 | Linux flag | QGA 역할 |
|---|---|---|
| `10.10.254.230/16` | `secondary` 없음, `dynamic=true` | 주 IP |
| `10.10.22.201/16` | `secondary=true` | 보조 IP |
| `10.10.22.202/16` | `secondary=true` | 보조 IP |
| `10.10.22.203/16` | `secondary=true` | 보조 IP |

`ip -j -4 route show table all`의 default route와 connected route는
`prefsrc=10.10.254.230`을 반환했고, `ip -j -4 route get 1.1.1.1`도 같은
preferred source를 반환했다. Cloud 기본 NIC의 주 IP는
`10.10.22.203`이므로 이 VM에서는 QGA 역할과 Cloud 역할이 다르다.

기대 UI는 다음과 같다.

- `10.10.254.230/16`: `주 IP`, `대표`, QGA
- `10.10.22.201/16`, `.202/16`: `보조 IP`
- `10.10.22.203/16`: `보조 IP`, `Cloud 주 IP`

## 3. Agent 구현

`VmGuestIpAddress` payload를 schema v2로 확장한다.

```java
String role;        // PRIMARY, SECONDARY, UNKNOWN
String roleSource;  // QGA_SINGLE_ADDRESS,
                    // QGA_LINUX_ADDRESS_FLAGS,
                    // QGA_WINDOWS_SKIP_AS_SOURCE
boolean representative;
```

표준 QGA 주소 응답을 먼저 수집한다.

- loopback/link-local/multicast를 제외한 유효 주소가 1개면 추가 실행 없이
  `PRIMARY`, `QGA_SINGLE_ADDRESS`, `representative=true`로 확정한다.
- 유효 주소가 2개 이상이고 exec fallback이 활성화된 VM에만 OS별 고정
  allowlist adapter를 실행한다.
- Linux는 `/usr/sbin/ip -j address show`를 우선 사용하고 실행 파일이
  없을 때만 `/usr/bin/ip`로 fallback한다.
- Windows는 고정 PowerShell `Get-NetIPAddress` projection의
  `SkipAsSource`와 기본 route interface를 사용한다.
- shell, 사용자 입력 경로, 임의 인수는 허용하지 않는다.
- 역할 보강 실패는 주소 목록 자체를 버리지 않고 interface section을
  `PARTIAL`로 저장한다.

## 4. API와 Backend

DB DDL은 변경하지 않는다. 기존 JSON payload column에 schema v2가
저장되며 v1 역직렬화도 유지한다.

상세 주소 response:

```json
{
  "family": "IPv4",
  "address": "10.10.254.230",
  "prefix": 16,
  "role": "PRIMARY",
  "rolesource": "QGA_LINUX_ADDRESS_FLAGS",
  "representative": true
}
```

목록 summary에는 전체 주소 배열과 별도로
`representativeaddress`, `representativeprefix`,
`representativefamily`, `representativesource`만 추가한다. route/DNS
payload는 목록 응답에 포함하지 않는다. stale/stopped/unavailable
snapshot에서는 QGA 대표 주소를 내보내지 않아 UI가 Cloud fallback을
사용하게 한다.

## 5. UI

- 목록: 기본 Cloud NIC 주 IP를 `C`, QGA 대표 IP를 `G`로 표시한다.
- QGA 대표가 없으면 `G` 위치에 기본 Cloud NIC 주 IP fallback을 표시한다.
- 정보 카드: QGA 대표 주소를 최우선으로 표시하고 `QGA · 주 IP` 배지를
  붙인다. fallback이면 `Cloud · 주 IP`를 표시한다.
- IP 구성 탭: QGA `주 IP/보조 IP/역할 미확인`, `대표`, Cloud
  `주 IP/보조 IP` 배지를 독립적으로 표시한다.
- 표시 순서는 대표 → QGA 주 → QGA 보조 → 역할 미확인이다.
- 색상과 경계는 Ant Design theme token을 사용해 일반/다크 테마를 함께
  지원한다.

## 6. 부하 및 격리

- 조회 UI/API는 기존과 같이 DB snapshot만 읽고 Agent를 호출하지 않는다.
- address-role adapter는 전용 `GetVmGuestNetworkStateCommand` wrapper
  안에서만 실행하며 VM/볼륨/NIC 핵심 libvirt command에 연결하지 않는다.
- 단일 주소 VM에는 추가 QGA 호출이 없다.
- 다중 주소 VM에는 interface 주기마다 address command 1회만 추가한다.
- 같은 cycle의 route/DNS와 `guest-get-osinfo` 결과를 공유한다.
- timeout과 최대 출력 크기는 기존 guest network command의 bounded
  설정을 그대로 사용한다.

## 7. 배포 및 검증 Gate

1. 실제 Preflight fixture로 Agent parser/wrapper 테스트
2. schema v1 역호환과 schema v2 API response 테스트
3. UI lint, locale JSON, production build
4. Host 3 KVM plugin의 변경 class만 patch 후 `mold-agent.service` 확인
5. 관리 서버 core/api/server 변경 class만 patch 후 `mold.service` 확인
6. 정적 UI만 배포하고 `WEB-INF`, `config.json` 보존
7. 대상 VM snapshot이 schema v2로 갱신됐는지 API 확인
8. 목록, 정보 카드, IP 구성, route, DNS, 상태/새로고침, 일반/다크 테마 확인

## 8. 22.x 배포 및 Backend 검증 결과

2026-07-27에 변경 class와 UI 정적 파일만 22.x에 배포했다. 관리 서버와
Host 3의 원본 JAR 및 기존 UI는
`/var/backups/guest-network-primary-ip-b551ab4c`에 보존했다.
DB DDL과 가상머신, 볼륨, 네트워크 리소스는 변경하지 않았다.

| 항목 | 결과 |
|---|---|
| 관리 서비스 | `mold.service=active`, API 8080 및 Agent 8250 listen |
| Host 3 Agent | `mold-agent.service=active`, `ReadyCommand` 처리 |
| UI | 로컬/원격 `index.html` SHA-256 일치, HTTP 200 |
| 보존 파일 | 기존 `config.json` SHA-256 유지, `WEB-INF` 유지 |
| 대상 snapshot | schema v2, `OK`, QGA 7.2.22 |
| interface | eth0 및 lo, section `OK` |
| route | IPv4 10개, section `OK` |
| DNS | 서버 2개, search domain 1개, section `OK` |

인증 API의 `getVirtualMachineGuestNetworkState` 응답은 eth0을 다음과 같이
반환했다.

- `10.10.254.230/16`: `PRIMARY`,
  `QGA_LINUX_ADDRESS_FLAGS`, `representative=true`
- `10.10.22.201/16`, `.202/16`, `.203/16`: `SECONDARY`,
  `representative=false`

`listVirtualMachines`의 `guestnetwork` summary도
`representativeaddress=10.10.254.230`,
`representativesource=QGA_LINUX_ADDRESS_FLAGS`를 반환했다. 따라서
수집 → DB → API와 목록 summary까지 QGA 우선 규칙이 실제 22.x에서
검증됐다.

인증 UI의 첫 검증에서 메트릭 API 경로가 `details`를 다시 구성할 때
`guestnetwork`를 누락해 정보 카드가 Cloud IP를 표시하는 문제를
발견했다. `AutogenView.vue`의 메트릭 상세 항목에 `guestnetwork`를
추가하고 UI를 다시 빌드·배포했다.

최종 production build의 `index.html` SHA-256은
`8e9f224c501ec3dab0df7c49e257c488fd8c90dab144108385aa26ea6b9dc01e`이며
원격 파일과 일치한다. 재배포 전 UI는
`/var/backups/guest-network-primary-ip-ui-metrics-b551ab4c`에 보존했다.

인증 UI Gate 8의 최종 결과는 다음과 같다.

- 목록: Cloud `10.10.22.203`, QGA 대표 `10.10.254.230/16`,
  `주`, `+4`, `정상`을 한 줄에 표시
- 목록 popover: 전체 주소 5개와 대표 주소 표시
- 정보 카드: `10.10.254.230`, `QGA · 주 IP` 표시
- IP 구성: QGA 주 IP/대표, QGA 보조 IP, Cloud 주·보조 IP를 독립 표시
- 상태: schema v2, `OK`, interface 2개
- route: 10개 표시, 검색과 전체/IPv4/IPv6/기본 라우팅 필터 동작
- DNS: `resolv.conf`, 서버 2개, search domain `lan` 표시
- 새로고침: DB snapshot 재조회 후 `OK` 유지
- 테마: 일반/다크 모두 역할 tag와 카드의 전경·배경·경계 대비 확인
- 최종 재로딩 이후 신규 browser console error 없음

검증 후 사용자 테마는 기존 다크 모드로 복원했다.

## 9. qemu-exec-tools Helper 연계 보완

Rocky/RHEL 계열에서 다중 주소의 `PRIMARY`/`SECONDARY` flag를 얻기 위해
사용하는 `/usr/sbin/ip -j address show`는 QGA RPC가 enabled여도
SELinux `virt_qemu_ga_t`에서 거부될 수 있다.

주소 역할 수집 source는 다음 순서를 사용한다.

1. qemu-exec-tools `guest-network-snapshot`의 address section
2. 기존 `QemuGuestAddressRoleFallback`
3. 역할을 추정하지 않고 `UNKNOWN`

Agent는 표준 `guest-network-get-interfaces` 주소를 source of truth로 유지하고,
Helper 결과는 normalized MAC/interface/address가 일치하는 항목의 role과
representative만 enrichment한다. Helper가 반환한 주소를 QGA 표준 결과에 없는
새 주소로 임의 추가하지 않는다.

부하 제한:

- interface address hash가 같고 역할이 확인된 경우 Helper 재실행 생략
- address role, route, DNS가 함께 due면 Helper 한 번만 실행
- 단일 유효 주소의 기존 `QGA_SINGLE_ADDRESS` 최적화 유지

Helper version/schema 또는 QGA capability fingerprint가 바뀌면 실패했던
address-role section의 backoff를 초기화한다. 관련 wire DTO, readiness, DB section
metadata는 `docs/guest_network_observability_integrated_improvement_design.md`를
따른다.
