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

# Guest Network Observability Phase 2 완료 보고서

## 1. 결과

Phase 2의 저장 계층과 Management 수집기를 구현했다.

현재 구현 경계는 다음과 같다.

```text
UI (Phase 3 이후)
  -> API (Phase 3 이후)
    -> Backend service / vm_guest_network_state
      -> VmGuestNetworkCollector
        -> 전용 GetVmGuestNetworkStateCommand
          -> Agent 전용 GuestNetwork-Worker
            -> KVM QGA read-only command
```

이번 단계에서는 UI와 조회 API를 추가하지 않았다. Backend는 Agent 응답을 해석해 최신 snapshot만 DB에 저장하며, 다음 Phase의 API는 이 DB snapshot만 읽도록 한다.

## 2. DB 변경

fresh install과 기존 Europa 환경 모두에 `cloud.vm_guest_network_state`를 추가했다.

적용 위치:

- fresh schema: `setup/db/create-schema.sql`
- Europa upgrade hook: `engine/schema/src/main/resources/META-INF/db/schema-Europa-After.sql`

정확한 migration 구조:

| column | type | 의미 |
|---|---|---|
| `id` | `bigint unsigned` | PK, auto increment |
| `vm_id` | `bigint unsigned` | VM FK, VM별 unique |
| `schema_version` | `smallint unsigned` | payload schema version, 기본값 1 |
| `status` | `varchar(32)` | `OK`, `STALE`, `STOPPED`, `UNAVAILABLE` 등 |
| `qga_version` | `varchar(64)` | 마지막 확인 QGA version |
| `observed_at` | `datetime` | 마지막 수집 시도 시각 |
| `last_success_at` | `datetime` | 마지막 성공 시각 |
| `payload_hash` | `char(64)` | canonical payload SHA-256 |
| `payload` | `mediumtext` | canonical guest network JSON |
| `error_code` | `varchar(64)` | 구조화된 오류 코드 |
| `error_message` | `varchar(255)` | 길이를 제한한 오류 내용 |
| `created` | `datetime` | 생성 시각 |
| `updated` | `datetime` | 갱신 시각 |

제약과 index:

- PK: `id`
- unique: `uc_vm_guest_network_state__vm_id (vm_id)`
- FK: `vm_id -> vm_instance.id ON DELETE CASCADE`
- index: `i_vm_guest_network_state__status_observed_at (status, observed_at)`
- charset: `utf8mb4`

VM expunge 경로에서 DAO 정리를 명시적으로 실행하며 FK cascade도 안전망으로 둔다.

## 3. 저장 정책

- 최초 성공: canonical payload, hash, 상태와 시각을 모두 저장한다.
- 변경 성공: hash가 달라진 경우에만 payload와 hash를 갱신한다.
- 미변경 성공: 상태·관측 시각 등 metadata만 갱신하고 payload column은 다시 쓰지 않는다.
- 정상 빈 응답: 빈 interface 배열을 새로운 정상 snapshot으로 저장해 과거 주소를 제거한다.
- 수집 실패: 마지막 성공 payload와 hash는 유지하고 상태를 `STALE`로 바꾼다.
- 최초 실패: 보존할 payload가 없으므로 `UNAVAILABLE` 계열 상태를 저장한다.
- VM 중지: 마지막 payload를 유지하고 상태를 `STOPPED`로 바꾼다.
- VM 삭제/expunge: snapshot을 삭제한다.
- payload는 volatile `observedAt`을 hash 대상에서 제외하고 object key와 array 순서를 정규화한다.
- canonical payload는 2 MiB를 초과할 수 없다.

## 4. 수집기와 핵심 명령 격리

`VmGuestNetworkCollector`는 `StatsCollector`와 VM lifecycle handler에 연결하지 않은 별도 Spring manager다.

- 전용 command/answer/wrapper만 사용한다.
- Agent에서는 Phase 1에서 추가한 `GuestNetwork-Worker`로 실행된다.
- VM start/stop/migrate, volume attach/detach/resize, NIC plug/unplug, network rule 및 `GetVmStatsCommand` 경로를 수정하거나 호출하지 않는다.
- 기존 libvirt operation lock이나 device lock을 획득하지 않는다.
- 수집 오류와 timeout은 snapshot을 stale 처리하며 핵심 command의 answer/transaction으로 전파하지 않는다.

## 5. 부하 최소화

기본값은 `vm.guest.network.details.enabled=false`다. 이 상태에서는 scheduler와 worker 자체를 만들지 않으므로 신규 QGA 호출과 신규 DB 작업이 발생하지 않는다. 설정을 켠 뒤 collector를 시작하려면 Management Server 재시작이 필요하다.

활성 시 적용하는 제한:

- 대상: Running KVM User VM만
- optional host/zone DB ID allowlist: Phase 6에서 추가, 잘못된 non-empty 값은 fail-closed
- scan: 30초 간격으로 due 여부만 확인
- interface 기본 주기: 120초
- DNS/route 예약 주기: 각각 600초
- deterministic VM별 jitter: 기본 20%
- QGA capability cache: 기본 600초
- 실패 exponential backoff: 최대 1800초
- QGA command timeout: 기본 3초
- 전체 cycle timeout: 기본 60초
- 동시 host: 기본 2개
- host command batch VM: 기본 1개
- host별 cycle VM 상한: 기본 50개
- cycle별 host 상한: 기본 50개
- 동일 Management Server cycle overlap: `AtomicBoolean` gate
- 동일 host overlap: active-host gate
- 다중 Management Server overlap: 전용 DB global lock `VmGuestNetworkCollector.scan`
- global lock 대기: 1초, 획득 실패 시 해당 cycle 생략
- capability cache 적중 시 `guest-info`를 생략하고 interface command만 실행

DNS와 route의 실제 수집은 각각 Phase 5와 Phase 4에서 구현한다. Phase 2에서는 section별 독립 due schedule과 저장 모델을 준비했다.

## 6. 설정 목록

| key | 기본값 |
|---|---:|
| `vm.guest.network.details.enabled` | `false` |
| `vm.guest.network.details.host.ids` | 빈 값(전체) |
| `vm.guest.network.details.zone.ids` | 빈 값(전체) |
| `vm.guest.network.details.interface.interval` | `120` |
| `vm.guest.network.details.dns.interval` | `600` |
| `vm.guest.network.details.route.interval` | `600` |
| `vm.guest.network.details.jitter.percent` | `20` |
| `vm.guest.network.details.max.concurrent.hosts` | `2` |
| `vm.guest.network.details.max.concurrent.vms.per.host` | `1` |
| `vm.guest.network.details.max.vms.per.host.cycle` | `50` |
| `vm.guest.network.details.max.hosts.per.cycle` | `50` |
| `vm.guest.network.details.failure.backoff.max` | `1800` |
| `vm.guest.network.details.capability.cache.ttl` | `600` |
| `vm.guest.network.details.command.timeout` | `3` |
| `vm.guest.network.details.cycle.timeout` | `60` |

## 7. 검증

실행한 대상 테스트:

```text
engine/schema:
  VmGuestNetworkStateDaoImplTest                         2 passed

core:
  GetVmGuestNetworkStateCommandTest                     2 passed

plugins/hypervisors/kvm:
  QemuGuestNetworkStateParserTest                       4 passed
  LibvirtGetVmGuestNetworkStateCommandWrapperTest       4 passed

server:
  VmGuestNetworkPayloadCanonicalizerTest                1 passed
  VmGuestNetworkStateServiceImplTest                    4 passed
  VmGuestNetworkCollectionPolicyTest                    3 passed
  VmGuestNetworkCollectorTest                           6 passed
```

총 26건이 통과해야 Phase 2를 완료로 판단한다. 검증 항목은 다음을 포함한다.

- disabled 상태 scheduler/worker/QGA/DB 작업 0
- global lock 경합 시 Agent/DB 작업 0
- 동일 payload의 snapshot rewrite 0
- 정상 empty 응답이 payload 변경으로 처리됨
- 실패 시 마지막 payload 보존 및 `STALE`
- stopped 상태에서 마지막 payload 보존
- jitter, section cadence, capability TTL, failure backoff
- host/VM cycle 및 command batch 상한
- VM별 NIC map 격리
- capability cache 적중 시 `guest-info` QGA probe 생략
- command JSON 왕복에서 cache hint 보존
- DAO의 VM 범위 cleanup과 snapshot update 경계
- 한 VM의 payload/DB 저장 실패가 같은 batch의 다른 VM 저장을 중단하지 않음

Maven compile/checkstyle도 대상 reactor에서 통과했다. 저장소에는 기존 KVM POM의 중복 `org.json:json` dependency 경고가 있으나 이번 변경에서 새로 발생한 오류는 아니다.

## 8. 배포 상태와 다음 단계

shared 22.x 환경에는 DB migration, Management artifact, Agent class를 배포하지 않았다. 서비스 재시작도 수행하지 않았다.

다음은 Phase 3이다.

- DB snapshot만 읽는 IP 상세/목록 API
- API RBAC와 serialization
- 조회 API가 Agent/QGA를 호출하지 않는 테스트
- 승인된 Ant Design Vue 프로토타입 기반 IPv4/IPv6 다중 주소 UI
- stale/partial/unsupported 표시

DNS와 route 수집 및 UI는 Phase 4와 Phase 5에서 이어서 구현한다.
