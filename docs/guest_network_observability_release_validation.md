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

# Guest Network Observability 배포 Artifact 및 DB Migration 검증

## 1. 검증 기준

- 검증 일자: 2026-07-25
- 작업 브랜치: `codex/guest-network-observability`
- 소스 커밋: `9796c3c2253e6a5596abce9785354bab2006b5ac`
- 기준 브랜치: `ablestack-europa`
- DB 엔진: MariaDB `10.5.29`
- DB 실행 방식: `/tmp` 임시 datadir, Unix socket 전용, `skip_networking=1`
- shared 22.x 관리 서버와 운영 DB에는 접속하거나 변경하지 않았다.

## 2. 배포 Artifact

Artifact bundle:

```text
/root/work/ablestack-cloud/dist/guest-network-observability/9796c3c2253e6a5596abce9785354bab2006b5ac
```

Backend/Agent/API/KVM artifact는 다음 명령의 clean build 결과에서 추출했다.

```bash
mvn -pl agent,api,engine/schema,server,plugins/hypervisors/kvm -am -DskipTests clean package
```

- Maven reactor: 37개 모듈 모두 성공
- 결과: `BUILD SUCCESS`
- 소요 시간: 293.18초

UI artifact는 다음 production build 결과를 재현 가능한 tarball로 묶었다.

```bash
NODE_OPTIONS=--openssl-legacy-provider npm run build
```

- 결과: `DONE Build complete`
- 소요 시간: 329.90초
- tar 조건: 파일명 정렬, numeric owner `0`, 소스 커밋 시각 고정, gzip timestamp 제거

### 2.1 파일 및 SHA-256

| 파일 | 크기(byte) | SHA-256 |
|---|---:|---|
| `cloud-agent-4.22.0.0-SNAPSHOT.jar` | 96,642 | `4518320d968ad3dcc3715607d48dbf7324c93d6652258ca0e02920efbcab6e2b` |
| `cloud-api-4.22.0.0-SNAPSHOT.jar` | 3,082,380 | `2262e4b844974e4f0b438cc9df5e255f0c99fe40b67e7d22c86262a80869709e` |
| `cloud-core-4.22.0.0-SNAPSHOT.jar` | 849,212 | `8e213fc7ef863ec31bf17b754961439dd572d6f2e0fb27f2f9e829aac80e8d90` |
| `cloud-engine-schema-4.22.0.0-SNAPSHOT.jar` | 2,424,695 | `4ba9023c2e726281b5a6031e9d6256b42142ad0ab17b08fd7a0cb6742a9efb75` |
| `cloud-plugin-hypervisor-kvm-4.22.0.0-SNAPSHOT.jar` | 1,166,556 | `7c320f849a5b0a17c65a4ea3839afe173af0ff999d3fb17f218a56e1aa3ddffb` |
| `cloud-server-4.22.0.0-SNAPSHOT.jar` | 4,691,675 | `c2a1fb49d4f2dea66ee69b1c15c49af8c4a67e2dd29d5efb8b919461602767af` |
| `cloudstack-ui-dist-4.22.0.0-SNAPSHOT.tar.gz` | 13,064,005 | `f9cb23ed9bf34fbb5c41cbbe1490b1ddbf51b0b73f89bbc4549d4919a5beae9d` |
| `create-schema.sql` | 142,669 | `59b372d2a4f05957aae6f2501798f9291ba6d9f95d0eb837b220e13545567db2` |
| `schema-Europa-After.sql` | 11,006 | `5af1438dd8ff8729c3ffddae690306998701698f1c98a0e1ef2dcd6183991365` |

Bundle의 `SHA256SUMS`에 위 값이 기록되어 있으며 `sha256sum -c SHA256SUMS` 결과는 9개 파일 모두 `OK`였다.

### 2.2 Artifact 내용 확인

다음 신규 class가 대상 JAR에 포함된 것을 확인했다.

- Core: `GetVmGuestNetworkStateCommand`
- Agent: `Agent`
- API: `GetVirtualMachineGuestNetworkStateCmd`
- Schema: `VmGuestNetworkStateDaoImpl`
- Server: `VmGuestNetworkCollector`
- KVM: `LibvirtGetVmGuestNetworkStateCommandWrapper`

## 3. Fresh schema 적용 검증

빈 `cloud` database를 생성한 뒤 current `setup/db/create-schema.sql` 전체를 MariaDB client로 적용했다.

- 적용 결과: 성공
- 소요 시간: 3.45초
- `vm_guest_network_state` 생성: 성공
- Engine/charset: `InnoDB`, `utf8mb4`
- Primary key: `id`
- VM별 unique key: `uc_vm_guest_network_state__vm_id`
- 상태 조회 index: `i_vm_guest_network_state__status_observed_at(status, observed_at)`
- FK: `vm_id -> vm_instance.id ON DELETE CASCADE`

실제 DML 검증 결과:

- snapshot INSERT/UPDATE/SELECT: 성공
- payload 내 IPv4, 보조 IPv4, IPv6, DNS, IPv4/IPv6 route 보존: 성공
- `JSON_VALID(payload)`: `1`
- 동일 VM 두 번째 INSERT: 예상대로 `ERROR 1062`, unique key 동작
- 부모 `vm_instance` 삭제 후 snapshot row: `0`, cascade 동작

## 4. Upgrade migration 적용 검증

upgrade clone은 다음 순서로 구성했다.

1. 직전 커밋(`HEAD^`)의 `setup/db/create-schema.sql` 적용
2. `schema-42100to42200.sql`의 import VM predecessor table 구간 적용
3. 직전 커밋의 `schema-Europa-After.sql` 전체 적용
4. 신규 테이블이 없는 상태와 기존 VM sentinel row 존재 확인
5. current `schema-Europa-After.sql` 전체 적용

적용 전:

- 전체 table 수: 139
- `vm_guest_network_state`: 0개
- 기존 VM sentinel row: 1개

적용 후:

- current Europa migration 적용 결과: 성공
- 소요 시간: 0.29초
- 전체 table 수: 140
- `vm_guest_network_state`: 1개
- 기존 VM sentinel row: 1개
- migration 재실행: 성공, 0.28초

실제 DML과 제약조건 검증은 fresh 경로와 동일하게 모두 통과했다.

- IPv4 및 보조 IPv4 보존: 성공
- IPv6 보존: 성공
- DNS 보존: 성공
- IPv4/IPv6 route 보존: 성공
- valid JSON: 성공
- VM별 unique 제약: 성공
- parent delete cascade: 성공

동일하게 한 row를 INSERT/DELETE한 상태에서 `SHOW CREATE TABLE` 결과를 SHA-256으로 비교했다.

```text
fresh:   dde3246a9059941fa4acb34013e2d16593f93c307b40d9ff1f5e850cba6837d9
upgrade: dde3246a9059941fa4acb34013e2d16593f93c307b40d9ff1f5e850cba6837d9
```

따라서 fresh와 upgrade 경로의 최종 table DDL은 일치한다.

## 5. 1차 격리 검증 범위

- 이번 검증은 직전 Git 커밋으로 재구성한 격리 schema clone을 사용했다.
- 실제 shared 22.x 운영 DB dump나 운영 credential은 사용하지 않았다.
- 전체 `schema-42100to42200.sql`은 최소 fresh baseline에 과거 `router_health_check` table이 없어 단독 재생할 수 없으므로, 이번 기능의 predecessor인 import VM table 구간과 직전 Europa migration 전체를 적용해 이전 상태를 구성했다.
- 이 단계 이후 실제 shared 22.x의 비식별화 DB dump clone과 최소
  artifact 배포 파일럿을 별도로 수행했다.

## 6. 실제 22.x DB Clone 및 최소 배포

운영자 승인과 SSH key 인증 확인 후 다음 검증을 추가 완료했다.

- 실제 MySQL 8.0.41 `cloud` DB를 raw dump 파일 없이 socket-only clone으로 복제
- 사용자/API key/host/VM/detail/import credential 비식별화 또는 제거
- 596개 기존 VM을 보존한 upgrade migration과 재실행 성공
- fresh/actual-upgrade 정규화 DDL SHA-256 일치
- 실제 관리 DB migration 성공, 기능 기본값 `false`
- 실제 배포 revision 기준 관리/API/DAO/UI artifact 재빌드
- KVM Host 1에만 최소 Agent/Core/KVM class patch
- Host 2/3 artifact 미변경
- 전체 IPv4/IPv6와 section 상태 API 확인
- VM/Volume/NIC 각 20회 비활성/활성 p95 비교
- 최종 기능 및 exec fallback `false`

세부 수치, 배포 SHA-256, 발견 사항과 rollback 정보는
`docs/guest_network_observability_22x_pilot_report.md`에 기록했다.

## 7. QGA OS family 사후 preflight

2026-07-26 배포 UI 확인 과정에서 Debian route/DNS가
`Unsupported guest OS ... debian`으로 표시되는 원인을 재검증했다.

- 실제 QGA capability와 고정 guest-exec route/DNS 명령은 정상이다.
- QGA 7.2.22는 `id=debian`을 반환하고 `kernel-name`을 제공하지 않았다.
- 기존 production 코드는 OS ID에 `linux` 문자열이 포함된 경우만 Linux
  adapter를 선택하므로 Debian/Rocky/CentOS를 실행 전에 거부한다.
- 실제 Debian VM에서 IPv4 route JSON 10건과 `/etc/resolv.conf` 읽기를
  확인했다.

기존 migration, API/UI, command allowlist 및 부하 검증을 유지하면서
fail-closed OS family resolver를 구현했다. KVM 관련 테스트 38개와
Management collector 테스트 12개가 통과했다.

Host 3 단일 VM 최소 배포 후 Debian 대상의 최종 snapshot은 전체 `OK`,
interface `OK` 2개, route `OK` 10개, DNS `OK` 서버 2개
(`source=resolv.conf`)다. Ubuntu 실행 표본은 현재 22.x에 없어 자동
fixture 검증은 완료하고 후속 실환경 gate를 남긴다.

상세 변경 범위와 수락 조건은
`docs/guest_network_observability_os_family_design.md`에 기록했다.
