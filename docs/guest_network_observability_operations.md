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

# 게스트 네트워크 관측성 운영 및 배포 가이드

## 1. 운영 원칙

이 기능은 UI → API → Backend/DB → Agent 경계를 따르는 읽기 전용 보조 기능이다.
VM, 볼륨, NIC와 기존 VM 통계 명령에 관측 payload를 결합하지 않는다.

- 초기값은 `vm.guest.network.details.enabled=false`다.
- 조회 API는 DB snapshot만 읽으며 Agent/QGA를 동기 호출하지 않는다.
- Agent의 관측 명령은 `GuestNetwork-Worker` 전용 bounded queue에서 실행한다.
- DNS/route `guest-exec` fallback은 별도로 켜기 전까지 비활성이다.
- 공유 환경에서는 host/zone allowlist와 1 VM batch로 시작한다.
- 설정한 allowlist가 비어 있으면 해당 범위 전체를 허용하지만, 값이 있으면서
  유효한 양의 ID가 하나도 없으면 수집 대상을 0으로 제한한다.

## 2. 사용자 화면 해석

VM 목록의 게스트 네트워크 요약과 상세의 `IP 구성` 탭에서 다음을 확인한다.

- Cloud 관리 IP와 게스트에서 실제 관측한 IP는 별도 영역이다.
- 한 인터페이스의 여러 IPv4/IPv6와 prefix가 모두 표시된다.
- loopback, tunnel/VPN, Cloud NIC 미매칭 인터페이스도 별도로 표시된다.
- DNS는 global/per-interface, local stub, upstream 확인 여부와 source를 표시한다.
- route는 IPv4/IPv6, default route, gateway, interface, metric, table을 표시한다.
- `STALE`은 마지막 성공값을 보존한 상태이며 마지막 성공 시각을 함께 확인해야 한다.
- `UNSUPPORTED`는 QGA/게스트 도구가 해당 section을 제공하지 않는 상태다.
- `PARTIAL`은 일부 section 실패 또는 안전 상한으로 데이터가 잘린 상태다.

`Unsupported guest OS for route fallback: <id>` 또는
`Unsupported guest OS for DNS fallback: <id>`는 guest-exec command
allowlist 거부와 구분한다. 이 메시지는 capability와 OS 정보 조회를 통과한
뒤 OS family resolver가 adapter를 선택하지 못했다는 뜻이다. 배포판 ID,
QGA OS 정보와 resolver 지원 여부를 먼저 확인하고 guest 내부 권한 문제로
단정하지 않는다.

## 3. 변경 artifact

| 계층 | 빌드 artifact | 주요 책임 |
|---|---|---|
| Core | `core/target/cloud-core-4.22.0.0-SNAPSHOT.jar` | Agent wire DTO와 command/answer |
| Agent | `agent/target/cloud-agent-4.22.0.0-SNAPSHOT.jar` | 전용 executor와 request 분류 |
| KVM | `plugins/hypervisors/kvm/target/cloud-plugin-hypervisor-kvm-4.22.0.0-SNAPSHOT.jar` | QGA interface/DNS/route 수집 |
| Schema | `engine/schema/target/cloud-engine-schema-4.22.0.0-SNAPSHOT.jar` | snapshot VO/DAO |
| API | `api/target/cloud-api-4.22.0.0-SNAPSHOT.jar` | list/detail response 계약 |
| Backend | `server/target/cloud-server-4.22.0.0-SNAPSHOT.jar` | collector, snapshot service, RBAC 조회 |
| UI | `ui/dist` | Ant Design Vue 목록/상세 UI |

공유 22.x host에는 새 branch의 전체 Agent/KVM jar를 바로 교체하지 않는다. 배포된
jar와 dependency set을 확인한 뒤, 백업한 기존 jar에 이번 기능의 변경 class만
반영하거나 전체 runtime이 정렬된 패키지를 별도 승인 후 사용한다.

## 4. DB migration 계약

fresh install은 `setup/db/create-schema.sql`, 기존 Europa upgrade는
`engine/schema/src/main/resources/META-INF/db/schema-Europa-After.sql`을 사용한다.
두 경로 모두 다음 계약의 `cloud.vm_guest_network_state`를 생성한다.

| column | type |
|---|---|
| `id` | `bigint unsigned`, PK, auto increment |
| `vm_id` | `bigint unsigned`, VM별 unique, `vm_instance.id` FK cascade |
| `schema_version` | `smallint unsigned`, default 1 |
| `status` | `varchar(32)` |
| `qga_version` | `varchar(64)` |
| `observed_at` | `datetime` |
| `last_success_at` | `datetime` |
| `payload_hash` | `char(64)` |
| `payload` | `mediumtext` |
| `error_code` | `varchar(64)` |
| `error_message` | `varchar(255)` |
| `created`, `updated` | `datetime` |

index는 `(status, observed_at)`, charset은 `utf8mb4`다. 배포 전 별도 DB clone에서
fresh schema와 Europa upgrade를 각각 실제 적용하고 `SHOW CREATE TABLE` 결과를
비교한다. migration 일부만 적용된 상태에서 Management Server를 시작하지 않는다.

## 5. 공유 22.x 최소 배포 순서

1. 기능 flag를 `false`로 고정하고 DB·Management·각 host jar와 UI를 백업한다.
2. DB clone에서 fresh/upgrade migration을 검증한 뒤 운영 DB에 upgrade SQL을 적용한다.
3. Core, Schema, API, Backend의 변경 class 또는 정렬된 management artifact만 반영한다.
4. Management Server를 재시작하고 기존 VM 목록, VM stats, Agent 연결을 먼저 확인한다.
5. host 한 대에 기존 배포 jar 백업 기반으로 Agent/KVM 변경 class만 반영한다.
6. 해당 host의 `mold-agent.service`를 재시작하고 ReadyAnswer, stats 수신을 확인한다.
7. UI 정적 artifact만 반영하고 기존 화면 회귀를 확인한다.
   - `/usr/share/cloudstack-management/webapp` 전체를 삭제하거나 교체하지 않는다.
   - backend servlet/resource가 있는 기존 `WEB-INF`는 반드시 보존한다.
   - 배포 후 UI HTTP 200뿐 아니라 비인증 API가 JSON `401`을 반환하는지
     확인한다. API `404`이면 `WEB-INF` 누락 여부를 먼저 점검한다.
8. 기능 활성화 전에 파일럿 VM의 `guest-info`, `guest-get-osinfo`와
   고정 route/DNS 명령을 읽기 전용으로 preflight한다.
   - Debian과 Ubuntu는 각각 `id=debian`, `id=ubuntu`가 Linux family로
     분류되는지 확인한다.
   - 실제 22.x QGA 7.2.22는 `kernel-name`을 제공하지 않을 수 있으므로
     이 필드만으로 판별하지 않는다.
   - `ip -j` 출력의 exit code와 JSON validity를 확인한다.
   - DNS는 `resolvectl` → `nmcli` → `/etc/resolv.conf` 순으로 실제
     사용 가능한 source를 확인한다.
   - Ubuntu 실행 표본이 없으면 미검증으로 기록하고 Debian 한 VM으로
     파일럿 범위를 제한한다.
9. 아래처럼 실제 DB ID를 사용해 zone/host allowlist와 최소 상한을 먼저 설정한다.

```text
vm.guest.network.details.enabled=false
vm.guest.network.details.zone.ids=<pilot-zone-db-id>
vm.guest.network.details.host.ids=<pilot-host-db-id>
vm.guest.network.details.max.concurrent.hosts=1
vm.guest.network.details.max.concurrent.vms.per.host=1
vm.guest.network.details.max.vms.per.host.cycle=1
vm.guest.network.details.exec.fallback.enabled=false
```

10. baseline을 확보한 뒤 기능을 `true`로 바꾸고 Management Server를 재시작한다.
11. 15분 이상 수락 gate를 측정한 뒤 VM 상한을 1 → 5 → 10 순으로만 확장한다.

`host.ids`와 `zone.ids`는 Cloud API UUID가 아니라 DB의 양의 숫자 ID다.

## 6. 성능 및 격리 수락 gate

같은 파일럿 host와 동일한 VM/작업 집합에서 off/on을 각각 측정한다.

| 지표 | 수집 방법 | 통과 기준 |
|---|---|---|
| VM start/stop p95 | 동일 작업 20회 이상의 API job elapsed | 증가 5% 이하 |
| volume attach/detach p95 | 동일 volume 조건의 API job elapsed | 증가 5% 이하 |
| NIC plug/unplug p95 | 동일 network 조건의 API job elapsed | 증가 5% 이하 |
| Management 평균 CPU | 동일 15분 구간의 process CPU | +2 percentage points 이하 |
| Agent 평균 CPU | 동일 15분 구간의 `mold-agent` CPU | +2 percentage points 이하 |
| Agent queue | `GuestNetwork-Worker` executor log의 Queue/Pending | bound 초과 0 |
| core 실패 | Basic/Stats/HA worker 및 async job 오류 | 증가 0 |
| snapshot 쓰기 | persistence metrics의 결과별 건수 | unchanged의 `PAYLOAD_UPDATED` 0 |
| section 시간 | KVM debug metrics의 interfaces/routes/dns ms | timeout 예산 이내 |

측정에 사용하는 로그:

```text
Agent:
  작업 상태 정보 [GuestNetwork-Worker]: Workers=... Active=... Queue=... Pending=...

KVM:
  Guest network collection metrics for VM [...]:
  status=..., totalMs=..., interfacesMs=..., routesMs=..., dnsMs=...

Management:
  Guest network snapshot persistence metrics for VM [...]:
  result=CREATED|PAYLOAD_UPDATED|METADATA_ONLY
```

slow/timeout QGA를 재현할 때는 파일럿 VM만 대상으로 하고, 동시에 VM·볼륨·NIC
작업을 실행해 core 실패 증가가 0인지 확인한다. gate 하나라도 실패하면 범위를
확장하지 않고 즉시 기능을 끈다.

## 7. 롤백

1. `vm.guest.network.details.enabled=false`로 변경하고 Management Server를 재시작한다.
2. 동적 활성화용 scheduler의 유휴 flag 확인 외 worker/QGA/DB write가 0인지 확인한다.
3. UI는 snapshot이 없을 때 기존 Cloud NIC 표시로 fallback하는지 확인한다.
4. 문제가 계속되면 UI, Management artifact, host jar를 역순으로 백업본에 복원한다.
5. 각 host에서 `mold-agent.service`와 ReadyAnswer/stats를 확인한다.
6. `vm_guest_network_state` 테이블은 즉시 삭제하지 않는다. 기능만 중단하고 DB
   downgrade 또는 table 제거는 별도 승인과 백업 후 수행한다.

## 8. 배포 기록 양식

배포 보고에는 다음을 남긴다.

- source commit과 빌드 artifact checksum
- DB migration 적용/검증 결과
- 대상 zone/host/VM ID
- off/on 측정 구간과 p95/CPU/queue/write/section 결과
- ReadyAnswer, 기존 VM stats, VM/volume/NIC 회귀 결과
- rollback 실행 여부와 백업 위치
- QGA OS ID/family 판별 source와 Debian/Ubuntu preflight 결과

## 9. qemu-exec-tools 및 Agent 일관성 gate

게스트 준비와 Host Agent 배포는 서로 다른 artifact gate로 관리한다.

### 9.1 게스트 readiness

향후 qemu-exec-tools 개선 버전에서는 다음 결과를 기록한다.

```text
agent_policy_fix --policy full --check --json
agent_policy_fix --check-profile cloud-network-observability --json
```

확인 항목:

- QGA package/service/version
- `policyMode=FULL`
- 지원 RPC 수, policy enabled 수, runtime disabled 목록
- qemu-exec-tools/Helper/profile version
- SELinux/AppArmor 상태
- 실제 Helper address/route/DNS 결과

전체 QGA RPC 허용 성공만으로 network profile을 `READY`로 판정하지 않는다.
qemu-exec-tools 미설치 VM은 `TOOLS_NOT_INSTALLED`로 기록하고 Agent legacy fallback
결과를 별도로 확인한다.

### 9.2 Host Agent manifest

각 host의 활성 `cloud-core`와 KVM plugin에서 기능 관련 class SHA-256을 기록한다.

- `GetVmGuestNetworkStateCommand`
- `VmGuestNetworkState`
- `VmGuestIpAddress`
- `LibvirtGetVmGuestNetworkStateCommandWrapper`
- `QemuGuestOsFamilyResolver`
- address/route/DNS/Helper source class

Host 1·2·3 manifest가 다르면 기능 검증을 시작하지 않는다. shared 22.x에서는 현재
runtime jar backup에 필요한 class만 patch하고 호스트별로 순차 재시작한다.

### 9.3 실제 재수집

- UI `새로고침`: DB snapshot 조회만 수행
- UI `지금 재수집`: async API가 section schedule을 due로 변경
- API thread는 Agent를 직접 호출하지 않음
- refresh 전후 section `observed_at`을 비교해 실제 실행을 판정

상세 절차와 DB lease/fingerprint 계약은
`docs/guest_network_observability_integrated_improvement_design.md`를 따른다.

## 10. 부분 section 실패 운영 규칙

Agent answer에 VM별 `VmGuestNetworkState`가 있으면 최상위 상태가
`UNAVAILABLE`이어도 구조화된 관측으로 처리한다.

- `interfaces`, `routes`, `dns`, `readiness`를 section별로 저장·backoff한다.
- 이번 요청에서 실패한 section만 `UNAVAILABLE` 또는 기존 성공 payload가 있으면
  `STALE`로 만든다.
- `NOT_DUE` section은 기존 payload와 마지막 성공 시각을 유지한다.
- Agent transport 실패나 VM state 객체 부재만 `COLLECTION_FAILED` 전역 실패다.
- 운영 확인은 aggregate 상태만 보지 않고 section의 `status`, `error_code`,
  `observed_at`, `last_success_at`, `next_due_at`을 함께 조회한다.

Rocky guest에서 `policyMode=FULL`과 `TOOLS_NOT_INSTALLED`가 동시에 표시되는 것은
모순이 아니다. 전자는 QGA RPC 정책이고 후자는 Helper/security profile 준비 상태다.
Host에 qemu-exec-tools를 설치해도 guest package는 자동 설치되지 않으므로,
console/SSH/image provisioning 등 별도 privileged guest lifecycle로 설치한다.
