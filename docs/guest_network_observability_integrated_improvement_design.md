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

# Guest Network Observability 통합 개선 상세 설계

## 문서 정보

- 상태: 구현 및 22.x Rocky Helper E2E 파일럿 검증 완료
- 작성일: 2026-07-27
- Cloud 작업 브랜치: `codex/guest-network-observability`
- qemu-exec-tools 설계 브랜치: `codex/guest-network-observability-design`
- 적용 계층: UI, API, Backend, DB, Agent, `ablestack-qemu-exec-tools`
- 선행 문서:
  - `docs/guest_network_observability_implementation_plan.md`
  - `docs/guest_network_observability_os_family_design.md`
  - `docs/guest_network_observability_primary_ip_design.md`
- 교차 저장소 문서:
  - `ablestack-qemu-exec-tools/docs/cloud_guest_network_observability_integration_design.md`

이 문서는 22.x 실환경에서 확인한 수집 공정성, Agent artifact 불일치,
QGA OS 판별 및 게스트 SELinux 제약을 하나의 구조로 해결하기 위한 구현 기준이다.
기존 수집 기능을 폐기하지 않고 wire/API 하위 호환을 유지하면서 단계적으로 전환한다.

## 1. 오류 원인과 AS-IS / TO-BE

| 구분 | 오류 원인 | AS-IS | TO-BE |
|---|---|---|---|
| Agent 배포 | Host 1·2와 Host 3의 활성 class가 다름 | Host 1·2에는 `QemuGuestOsFamilyResolver`가 없고 Host 3만 로컬 최신 build와 일치 | Agent가 `collectorBuildId`를 응답하고 배포 manifest로 3개 host class SHA-256 일치를 gate로 검사 |
| OS 판별 | 구형 Agent가 `rocky`, `debian`을 Linux로 판별하지 못함 | route/DNS가 `UNSUPPORTED` | 공통 `QemuGuestOsFamilyResolver`와 `ID_LIKE` 보조 판별을 모든 fallback이 공유 |
| QGA 권한 | RPC enabled와 실행 파일 접근 가능 여부가 다름 | `/bin/true` 성공만으로 guest-exec 준비 완료로 오판 | 실제 Helper/고정 명령 실행 결과를 error code로 분류 |
| SELinux | `virt_qemu_ga_t`가 `/usr/sbin/ip` 실행을 거부 | route와 다중 주소 role 판별 실패 | qemu-exec-tools가 전용 read-only Helper와 SELinux 정책을 설치 |
| 게스트 도구 | 대상 VM에 qemu-exec-tools/Helper가 없음 | Agent가 배포판 도구를 직접 호출 | Helper 우선, 미설치 VM은 기존 고정 fallback 유지 |
| QGA 정책 | 전체 활용을 위해 RPC를 모두 허용하지만 기능별 readiness가 없음 | 파일 RPC는 동작해도 네트워크 profile 실패를 구분하지 못함 | `policyMode=FULL`과 `cloud-network-observability` profile 상태를 별도로 보고 |
| host 공정성 | host ID 정렬 후 `max.hosts.per.cycle=1` 적용 | 낮은 host ID가 반복 선택되어 Host 3 지연 | oldest-due host 선택과 DB lease로 starvation 제거 |
| backoff | capability/도구 변경을 감지하지 못함 | 권한 변경 후에도 route/DNS가 최대 1,800초 대기 | capability/Helper fingerprint 변경 시 실패 section만 즉시 due |
| section 시각 | VM 전체 `observed_at`만 존재 | 최신 interface 시각이 오래된 route/DNS를 최신처럼 보이게 함 | section별 attempt/success/next-due/error를 별도 행에 저장 |
| UI 갱신 | 화면 갱신은 DB 재조회만 수행 | 사용자는 재수집으로 오해 | `새로고침`과 비동기 `지금 재수집`을 분리 |
| 부분 섹션 실패 | Agent가 구조화된 section 결과를 반환해도 최상위 `UNAVAILABLE`/error만 보고 VM 전체 실패로 저장 | route/readiness 단독 실패가 기존 IP/DNS까지 `STALE`/`COLLECTION_FAILED`로 덮음 | state 객체가 있으면 section-aware merge와 section별 backoff를 수행하고, state 자체가 없을 때만 전역 실패 처리 |

## 2. 22.x Preflight 근거

### 2.1 대상

- VM UUID: `444698e3-e1c0-4ea7-a3b7-e94834c67afb`
- libvirt name: `i-2-379-VM`
- Host: `ablecube22-2`
- Guest: Rocky Linux 9.4
- QGA: 8.2.0

### 2.2 검증 결과

- DB interface 관측 간격은 약 127초로 120초 ±20% 범위다.
- QGA 42개 command 중 41개가 enabled이며 `guest-get-devices`만 runtime disabled다.
- 다음 command는 enabled다.
  - `guest-exec`
  - `guest-exec-status`
  - `guest-file-open`
  - `guest-file-read`
  - `guest-file-close`
  - `guest-get-osinfo`
  - `guest-network-get-interfaces`
- `guest-file-open/read/close`로 `/etc/os-release`를 읽는 실제 파일 RPC가 성공했다.
- `/bin/true`, `/usr/bin/id`, `/usr/bin/cat /etc/resolv.conf` guest-exec가 성공했다.
- `/usr/sbin/ip` guest-exec는 `Permission denied`로 실패했다.
- 실행 context는 `system_u:system_r:virt_qemu_ga_t:s0`다.
- `/usr/bin/agent_policy_fix`, `/usr/local/bin/agent_policy_fix`,
  `guest-network-snapshot`은 설치되어 있지 않다.
- `/etc/sysconfig/qemu-ga`는 전체 RPC allow 정책으로 변경되어 있다.

따라서 전체 RPC 허용은 성공했지만 네트워크 관측 profile은 준비되지 않은 상태다.
설계는 이 두 상태를 동일한 Boolean으로 합치지 않는다.

### 2.3 격리 DB DDL 검증

공유 22.x DB를 변경하지 않고 WSL의 socket-only MariaDB 10.5.29 임시 인스턴스에서
8장의 aggregate ALTER와 section table DDL을 적용했다.

- aggregate 신규 column: 7개 생성
- section table column: 17개 생성
- unique/due-lease/vm-status index 생성
- VM foreign key와 `ON DELETE CASCADE` 생성
- 검증 후 임시 DB와 socket/datadir 제거

실제 구현 시에는 동일 DDL을 fresh schema와 비식별 22.x upgrade clone에 다시
적용하고 최종 `SHOW CREATE TABLE`을 비교한다.

## 3. 목표 책임 구조

```text
GuestNetworkTab.vue
  -> getVirtualMachineGuestNetworkState (DB-only)
  -> refreshVirtualMachineGuestNetworkState (async enqueue)
       |
VmGuestNetworkApiService
       |
VmGuestNetworkCollector / VmGuestNetworkScheduleService
       |
GetVmGuestNetworkStateCommand
       |
LibvirtGetVmGuestNetworkStateCommandWrapper
       |
QGA standard command
  -> ablestack guest-network-snapshot
  -> legacy fixed guest-exec fallback
```

계층별 책임:

- UI: response 표시, DB refresh, async recollection 요청
- API: RBAC, parameter 검증, response DTO
- Backend: due/lease/backoff, Agent admission, section merge, 상태 파생
- DB: 최신 section payload와 지속 가능한 schedule/lease
- Agent: read-only source 선택, 고정 command 실행, parsing, 오류 분류
- qemu-exec-tools: QGA 전체 RPC 정책, Helper, SELinux/AppArmor, guest readiness

Cloud는 게스트 패키지 또는 정책을 자동 변경하지 않는다. qemu-exec-tools는 Cloud DB,
Cloud RBAC, 수집 schedule을 알지 않는다.

## 4. 공통 버전 및 상태 계약

### 4.1 버전

- Cloud aggregate payload: schema version `3`
- qemu-exec-tools Helper payload: schema version `1`
- readiness payload: schema version `1`

서로 다른 schema version을 하나의 필드로 재사용하지 않는다.

### 4.2 readiness

```java
public enum VmGuestReadinessStatus {
    READY,
    TOOLS_NOT_INSTALLED,
    POLICY_NOT_READY,
    HELPER_NOT_FOUND,
    HELPER_SCHEMA_UNSUPPORTED,
    SECURITY_POLICY_NOT_READY,
    UNKNOWN
}
```

QGA policy mode:

```java
public enum VmGuestQgaPolicyMode {
    FULL,
    CUSTOM,
    UNKNOWN
}
```

`FULL`은 현재 QGA가 제공하는 RPC를 전체 허용한다는 의미다. 특정 executable이
SELinux/AppArmor에 의해 실행 가능한지는 readiness profile에서 별도로 판단한다.

### 4.3 section 오류 코드

```java
public enum VmGuestNetworkErrorCode {
    QGA_NOT_CONNECTED,
    QGA_RPC_DISABLED,
    OS_UNSUPPORTED,
    HELPER_NOT_INSTALLED,
    HELPER_SCHEMA_UNSUPPORTED,
    EXEC_PERMISSION_DENIED,
    EXECUTABLE_NOT_FOUND,
    EXEC_TIMEOUT,
    EXEC_EXIT_NONZERO,
    OUTPUT_LIMIT_EXCEEDED,
    INVALID_UTF8,
    INVALID_JSON,
    AGENT_QUEUE_REJECTED,
    AGENT_VERSION_MISMATCH
}
```

UI 문구는 `details` 문자열을 parsing하지 않고 `errorCode`를 기준으로 선택한다.

## 5. qemu-exec-tools 상세 설계

교차 저장소의 파일 단위 구현 기준은
`docs/cloud_guest_network_observability_integration_design.md`에 둔다.
Cloud가 의존하는 공개 계약은 다음과 같다.

### 5.1 전체 RPC 정책

```bash
agent_policy_fix --policy full --apply
agent_policy_fix --policy full --check --json
agent_policy_fix --check-profile cloud-network-observability --json
```

`--policy full` 알고리즘:

1. OS와 QGA config/service 위치를 탐지한다.
2. 설치된 QGA가 지원하는 command 목록을 구한다.
3. active allow/block 설정과 vendor 기본값을 읽는다.
4. 지원 command 전체를 allow 목록으로 만든다.
5. runtime compile-disabled command는 정책 실패로 취급하지 않는다.
6. desired hash가 다를 때만 backup, write, restart한다.
7. restart와 설정 검증 실패 시 원본을 복구한다.

기존처럼 파일 RPC를 포함한 전체 허용 목적은 유지한다. Cloud Agent의 네트워크
collector가 사용할 수 있는 path/argument는 Agent 코드의 별도 allowlist로 제한한다.

### 5.2 Helper CLI

고정 경로:

```text
/usr/libexec/ablestack-qemu-exec-tools/guest-network-snapshot
```

호출:

```text
guest-network-snapshot --schema 1 --sections addresses,routes,dns
```

`--sections` 값은 Agent enum에서만 생성하며 API/UI 입력을 전달하지 않는다.

Helper는 stdout에 단일 JSON document만 쓰고 진단 로그는 제한된 stderr에 쓴다.

```json
{
  "schemaVersion": 1,
  "tool": {
    "name": "ablestack-qemu-exec-tools",
    "version": "package-version"
  },
  "profile": {
    "name": "cloud-network-observability",
    "version": 1,
    "status": "READY"
  },
  "os": {
    "id": "rocky",
    "idLike": ["rhel", "centos"]
  },
  "sections": {
    "addresses": {"status": "OK", "source": "linux-ip-json"},
    "routes": {"status": "OK", "source": "linux-ip-json"},
    "dns": {"status": "OK", "source": "networkmanager"}
  },
  "interfaces": [],
  "routes": [],
  "dns": {}
}
```

Helper는 네트워크 설정을 변경하지 않는다. 임시 파일이 필요하면 private runtime
directory에서 생성하고 종료 시 제거한다.

### 5.3 SELinux/AppArmor

RHEL 계열:

- source policy를 RPM build에 포함한다.
- install 시 고정 module name/version으로 설치한다.
- `audit2allow`를 runtime에서 자동 실행하지 않는다.
- `virt_qemu_ga_t`에서 전용 Helper 실행과 read-only 조회에 필요한 권한만 부여한다.
- SELinux permissive 전환이나 executable relabel을 수행하지 않는다.

Ubuntu/Debian:

- QGA systemd/AppArmor 제한을 검사한다.
- policy가 필요하면 package-owned drop-in/profile로 배포한다.
- “기본 허용” 메시지만 출력하고 성공 처리하지 않는다.

## 6. Agent 상세 설계

### 6.1 wire request 정리

현재 여러 `Set<String>`로 VM별 due section을 표현하는 생성자는 확장성이 낮다.
다음 value object를 도입한다.

```java
public class VmGuestNetworkCollectionRequest {
    private String vmName;
    private Map<String, String> cloudNicIdsByMac;
    private Set<VmGuestNetworkSection> sections;
    private boolean cachedInterfaceCapability;
    private boolean preferGuestToolsHelper;
    private int minimumHelperSchemaVersion;
    private boolean forceReadinessProbe;
}
```

```java
public enum VmGuestNetworkSection {
    INTERFACES,
    ROUTES,
    DNS,
    READINESS
}
```

주소 role/representative enrichment는 `INTERFACES`의 일부이며 독립 DB section으로
저장하지 않는다.

`GetVmGuestNetworkStateCommand`는 `List<VmGuestNetworkCollectionRequest>`를 새 필드로
추가한다. 기존 필드는 한 release 동안 유지하고 Agent가 새 필드가 비어 있으면 기존
필드를 변환해 사용한다.

### 6.2 수집 context

```java
final class GuestNetworkCollectionContext {
    Domain domain;
    VmGuestNetworkCollectionRequest request;
    Map<String, Boolean> capabilities;
    String capabilityHash;
    QemuGuestOsFamilyResolution osFamily;
    GuestToolsSnapshot helperSnapshot;
}
```

한 VM cycle에서 `guest-info`, `guest-get-osinfo`, Helper 결과를 한 번만 조회하고
interfaces/address-role/routes/DNS가 공유한다.

### 6.3 source abstraction

```java
interface GuestNetworkDataSource {
    boolean supports(GuestNetworkCollectionContext context);
    GuestNetworkPartialResult collect(
            GuestNetworkCollectionContext context,
            EnumSet<VmGuestNetworkSection> sections);
    String sourceId();
}
```

구현:

- `QgaStandardGuestNetworkDataSource`
- `AbleStackGuestToolsDataSource`
- `LegacyGuestExecDataSource`

선택 순서:

1. interface inventory는 표준 `guest-network-get-interfaces`
2. address role/routes/DNS는 Helper
3. Helper 미설치 또는 schema 미지원이면 legacy fixed fallback
4. 모든 실패는 section별 상태로 반환

표준 QGA interface 결과가 주소 목록의 source of truth다. Helper 주소는 같은
normalized MAC/interface/address에 role과 representative 정보만 enrichment한다.

### 6.4 guest-exec 공통화

현재 address/route/DNS fallback에 중복된 launch/poll/decode 로직을 다음 클래스로
통합한다.

```java
final class BoundedQgaGuestExec {
    GuestExecResult execute(
            Domain domain,
            GuestExecOperation operation,
            int timeoutSeconds,
            int maxOutputBytes);
}
```

```java
enum GuestExecOperation {
    ABLESTACK_NETWORK_SNAPSHOT,
    LINUX_IP_ADDRESS,
    LINUX_IP_ROUTE_V4,
    LINUX_IP_ROUTE_V6,
    LINUX_RESOLVECTL,
    LINUX_NMCLI_DNS,
    LINUX_RESOLV_CONF,
    WINDOWS_ADDRESS,
    WINDOWS_ROUTE,
    WINDOWS_DNS
}
```

enum이 absolute path와 immutable argument list를 소유한다. 외부 문자열로 operation을
생성할 수 없게 한다.

`BoundedQgaGuestExec`가 담당할 항목:

- `guest-exec`/`guest-exec-status`
- timeout과 interrupt
- stdout/stderr base64
- UTF-8 strict decode
- output size 제한
- QGA error text의 구조화 error code 변환

### 6.5 Agent 응답 확장

`VmGuestNetworkState`에 다음 optional 필드를 추가한다.

```java
private String collectorBuildId;
private Long collectorHostId;
private String capabilityHash;
private VmGuestToolsInfo guestTools;
```

```java
public class VmGuestToolsInfo {
    private boolean installed;
    private String version;
    private int helperSchemaVersion;
    private String qgaPolicyMode;
    private String readinessStatus;
    private String profileVersion;
}
```

`VmGuestNetworkSectionStatus` 확장:

```java
private String source;
private String errorCode;
private long attemptedAt;
private Long succeededAt;
```

## 7. Backend 및 scheduling 상세 설계

### 7.1 기존 문제

- `VmGuestNetworkCollectionPolicy` schedule이 management memory에만 있다.
- management restart 후 모든 VM이 동시에 due가 될 수 있다.
- host ID 정렬과 작은 cycle limit이 starvation을 만든다.
- interface 성공으로 aggregate `observed_at`이 갱신되어 route/DNS 시각을 알 수 없다.

### 7.2 신규 서비스

```java
interface VmGuestNetworkScheduleService {
    List<ClaimedVmGuestNetworkWork> claimDueWork(
            Date now, String leaseOwner, int maxHosts, int maxVmsPerHost);
    void completeSection(long vmId, VmGuestNetworkSection section,
            VmGuestNetworkSectionResult result, Date completedAt);
    void failSection(long vmId, VmGuestNetworkSection section,
            String errorCode, String errorMessage, Date failedAt);
    void requestRefresh(long vmId, Set<VmGuestNetworkSection> sections,
            long requestedBy);
    void invalidateOnFingerprintChange(long vmId,
            String oldFingerprint, String newFingerprint);
}
```

claim 단계:

1. 짧은 전용 global lock을 획득한다.
2. `next_due_at <= now`이고 lease가 만료된 section을 조회한다.
3. host별 `MIN(next_due_at)`가 오래된 host부터 선택한다.
4. host 내 VM은 가장 오래 overdue된 순서로 선택한다.
5. 선택 section에 `lease_owner`, `lease_until`을 기록한다.
6. lock을 해제한 뒤 Agent I/O를 수행한다.

원격 Agent 호출 동안 global lock을 보유하지 않는다. lease 만료 후 다른 management
server가 복구할 수 있다.

### 7.3 fingerprint

```text
SHA-256(
  qgaVersion
  + sorted enabled capability names
  + collectorBuildId
  + guestToolsVersion
  + helperSchemaVersion
  + qgaPolicyMode
  + profileVersion
)
```

fingerprint 변경 시:

- 성공 section의 정상 cadence는 유지한다.
- `UNSUPPORTED`, `UNAVAILABLE`, `STALE` section의 failure count를 0으로 초기화한다.
- 해당 section의 `next_due_at`을 즉시 시각으로 변경한다.
- 동일 fingerprint 반복에는 추가 reset을 하지 않는다.

### 7.4 부하 정책

- active host: 1
- active VM per host: 1
- interface: 120초 ±20%
- routes/DNS: 600초 ±20%
- failure max backoff: 1,800초
- manual refresh cooldown: VM당 30초
- Helper negative cache: 600초, fingerprint 변경 시 폐기
- Agent output: 기본 1 MiB, 절대 상한 2 MiB

interface address hash가 같고 role이 이미 확인되었으면 address-role Helper 실행을
생략한다. routes/DNS와 role이 함께 due면 Helper를 한 번만 실행한다.

## 8. DB 상세 설계

### 8.1 aggregate metadata 확장

`vm_guest_network_state`는 목록 summary와 하위 호환 aggregate metadata로 유지한다.

```sql
ALTER TABLE `cloud`.`vm_guest_network_state`
    ADD COLUMN `collector_build_id` varchar(128) NULL AFTER `qga_version`,
    ADD COLUMN `collector_host_id` bigint unsigned NULL AFTER `collector_build_id`,
    ADD COLUMN `capability_hash` char(64) NULL AFTER `collector_host_id`,
    ADD COLUMN `guest_tools_version` varchar(64) NULL AFTER `capability_hash`,
    ADD COLUMN `qga_policy_mode` varchar(16) NULL AFTER `guest_tools_version`,
    ADD COLUMN `readiness_status` varchar(32) NULL AFTER `qga_policy_mode`,
    ADD COLUMN `readiness_checked_at` datetime NULL AFTER `readiness_status`;
```

### 8.2 section authoritative table

```sql
CREATE TABLE IF NOT EXISTS `cloud`.`vm_guest_network_section_state` (
    `id` bigint unsigned NOT NULL auto_increment,
    `vm_id` bigint unsigned NOT NULL,
    `section` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL DEFAULT 'NOT_COLLECTED',
    `source` varchar(64) NULL,
    `observed_at` datetime NULL,
    `last_success_at` datetime NULL,
    `next_due_at` datetime NOT NULL,
    `failure_count` smallint unsigned NOT NULL DEFAULT 0,
    `error_code` varchar(64) NULL,
    `error_message` varchar(255) NULL,
    `payload_hash` char(64) NULL,
    `payload` mediumtext NULL,
    `lease_owner` varchar(128) NULL,
    `lease_until` datetime NULL,
    `created` datetime NOT NULL,
    `updated` datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uc_vm_guest_network_section__vm_section` (`vm_id`, `section`),
    KEY `i_vm_guest_network_section__due_lease`
        (`next_due_at`, `lease_until`),
    KEY `i_vm_guest_network_section__vm_status`
        (`vm_id`, `status`),
    CONSTRAINT `fk_vm_guest_network_section__vm_id`
        FOREIGN KEY (`vm_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

section payload:

- `interfaces`: interface/address 배열
- `routes`: route 배열
- `dns`: DNS object
- `readiness`: guestTools/readiness object

aggregate 상태는 section 행으로부터 파생한다. payload hash가 같으면 해당 section의
`payload`를 rewrite하지 않고 metadata만 갱신한다.

### 8.3 migration

upgrade:

1. aggregate metadata column 추가
2. section table 생성
3. 기존 snapshot VM에 section row를 생성
4. 기존 payload의 section status를 seed
5. 정확한 과거 section 시각은 알 수 없으므로 `observed_at`을 aggregate 시각으로
   seed하고 `source=legacy-aggregate-migration`을 기록
6. `next_due_at`은 migration 완료 시각으로 설정해 자연스럽게 재수집

fresh schema와 upgrade SQL의 최종 DDL을 실제 22.x DB clone에서 비교한다.

## 9. API 상세 설계

### 9.1 상세 조회

기존 `getVirtualMachineGuestNetworkState`에 optional 필드를 추가한다.

```json
{
  "collector": {
    "buildid": "commit-or-manifest-id",
    "hostid": "host-uuid"
  },
  "guesttools": {
    "installed": false,
    "version": null,
    "helperschemaversion": 0,
    "qgapolicymode": "FULL",
    "readinessstatus": "TOOLS_NOT_INSTALLED",
    "profileversion": null,
    "checked": "..."
  },
  "sections": [
    {
      "name": "routes",
      "status": "UNAVAILABLE",
      "source": "legacy-guest-exec",
      "errorcode": "EXEC_PERMISSION_DENIED",
      "observed": "...",
      "lastsuccess": null,
      "nextdue": "..."
    }
  ]
}
```

신규 DTO:

- `GuestNetworkCollectorResponse`
- `GuestToolsResponse`

`GuestNetworkSectionResponse`에는 source/errorCode/observed/lastSuccess/nextDue를 추가한다.
기존 필드는 제거하지 않는다.

### 9.2 비동기 재수집

```text
refreshVirtualMachineGuestNetworkState
```

parameter:

- `id`: VM UUID
- `sections`: optional `interfaces,routes,dns,readiness`

동작:

- async command
- VM 접근 권한과 별도 update 권한 검사
- schedule row를 due로 만들고 backoff를 초기화
- API thread가 Agent를 직접 호출하지 않음
- cooldown 내 요청은 기존 pending request를 반환

response:

```json
{
  "accepted": true,
  "requestid": "...",
  "requestedsections": ["routes", "dns", "readiness"],
  "nexteligible": "..."
}
```

## 10. UI 상세 설계

### 10.1 컴포넌트

`GuestNetworkTab.vue`를 다음 하위 컴포넌트로 분리한다.

- `GuestNetworkCollectionHeader.vue`
- `GuestInterfaceList.vue`
- `GuestRouteTable.vue`
- `GuestDnsTable.vue`

상위 탭은 API 호출과 state 전달만 담당한다.

### 10.2 동작

- `새로고침`: DB snapshot 재조회
- `지금 재수집`: async refresh API 호출
- refresh accepted 후 2초부터 exponential UI poll, 최대 30초
- request 전 `observed`보다 새로운 section attempt가 확인되면 poll 종료
- VM stopped 또는 collector disabled이면 재수집 버튼 disabled

### 10.3 사용자 안내와 readiness 비노출

상세 `IP 구성` 탭은 readiness/collector/Helper 진단 카드를 표시하지 않는다.
QGA policy, qemu-exec-tools version, network profile, collector build/host와
구조화 오류는 API/DB/운영 진단 데이터로 유지하되 일반 화면에서는 숨긴다.

탭 상단의 단일 정보성 안내에는 다음 내용만 표시한다.

- 화면은 DB에 저장된 최신 snapshot을 조회하며 새로고침은 Host/Agent를 호출하지 않음
- Rocky Linux 등 일부 운영체제는 route/DNS 수집을 위해 ABLESTACK 게스트 도구가
  필요할 수 있음

안내는 특정 VM의 설치 실패를 단정하는 경고가 아니며, 수집 상태와 section별 오류는
기존 수집 상태 표와 해당 section에서 계속 확인한다.

목록 화면은 기존 대표 IP 한 개와 `+N`, 상태 tag만 유지한다. readiness 세부 정보와
DNS/route는 목록 API에 포함하지 않는다.

모든 신규 색상은 Ant Design theme token을 사용하고 light/dark에서 별도 hard-coded
배경색을 추가하지 않는다.

## 11. 테스트 설계

### 11.1 qemu-exec-tools

- OS fixture: Rocky/RHEL/Alma/Ubuntu/Debian
- FULL policy가 파일 RPC를 포함한 지원 command 전체를 유지
- compile-disabled command가 policy failure를 만들지 않음
- 설정 unchanged 시 backup/restart 0
- restart 실패 rollback
- SELinux enforcing에서 Helper 성공
- Helper JSON schema/상한/부분 실패

### 11.2 Agent

- Helper 우선 선택
- Helper missing/schema mismatch 시 legacy fallback
- QGA interface + Helper role merge
- 한 cycle에서 Helper 1회
- permission denied error mapping
- output/UTF-8/JSON 제한
- 임의 path/argument 생성 불가

### 11.3 Backend/DB

- oldest-due host 선택
- `maxHostsPerCycle=1`에서 host 순환/starvation 없음
- claim lock 해제 후 Agent I/O
- lease timeout 복구
- section별 backoff
- fingerprint 변경 즉시 retry
- unchanged section payload rewrite 0
- migration fresh/upgrade DDL 동등성

### 11.4 API/UI

- 상세 response 하위 호환
- refresh RBAC/cooldown/async
- DB refresh와 recollection 동작 분리
- light/dark 안내 문구와 기존 수집 결과 가독성
- readiness/collector/Helper 진단 카드가 일반 UI에 노출되지 않음

## 12. 권장 구현 순서

1. qemu-exec-tools FULL policy 검증과 idempotent apply
2. Helper JSON contract, SELinux/AppArmor, package test
3. Agent 공통 guest-exec executor와 Helper source
4. Agent build/readiness/error wire DTO
5. section table migration과 DAO
6. persisted schedule/lease와 oldest-due selector
7. 상세 API 확장과 async refresh
8. UI 안내/refresh 컴포넌트
9. Host 1·2·3 runtime-aligned class patch와 manifest 검증
10. Rocky/RHEL/Ubuntu/Debian 22.x E2E 및 부하 gate

## 13. 완료 조건

- Host 1·2·3 collector class manifest가 동일하다.
- FULL QGA policy에서 file RPC 실제 read preflight가 성공한다.
- Rocky SELinux enforcing 상태에서 Helper가 address/routes/DNS를 수집한다.
- qemu-exec-tools 미설치 VM은 legacy fallback과 정확한 readiness를 반환한다.
- route/DNS/address-role가 실제 실패 원인을 구조화해 반환한다.
- Host 3이 host 선택에서 starvation되지 않는다.
- section별 시각과 backoff가 DB/API/UI에서 일치한다.
- UI 새로고침은 Agent를 호출하지 않고, 재수집은 async schedule만 변경한다.
- 기능 비활성 시 QGA 호출과 section DB write가 0이다.
- VM/볼륨/NIC/통계 핵심 명령 p95 증가가 기존 성능 gate를 넘지 않는다.

## 14. 구현 및 22.x 재검증 결과

### 14.1 구현된 계층

- UI: DB 갱신과 비동기 재수집 분리, readiness/collector 진단 카드 비노출,
  OS별 게스트 도구 필요 가능성 안내, section 시각과 오류 및 대표 IP와 주·보조 IP
  표시, Ant Design light/dark token 적용
- API: `refreshVirtualMachineGuestNetworkState`, optional collector/guest tools/section
  metadata response, RBAC와 cooldown
- Backend: persistent oldest-due schedule, DB lease, section별 backoff/jitter,
  capability fingerprint 무효화, 구조화된 `UNAVAILABLE`의 section-aware merge
- DB: aggregate metadata 20개 column과 17-column
  `vm_guest_network_section_state`, fresh/upgrade migration
- Agent: OS family resolver, bounded fixed guest-exec, Helper 우선/fallback,
  collector build/host/readiness/error code
- qemu-exec-tools: FULL policy 유지, read-only snapshot Helper, SELinux policy source,
  RPM/DEB lifecycle 및 smoke test

### 14.2 빌드 및 migration

- Cloud 전체 package와 UI production build 통과
- 최종 Backend 회귀 테스트:
  `VmGuestNetworkCollectorTest` 14건,
  `VmGuestNetworkStateServiceImplTest` 7건, 합계 21건 통과
- KVM guest-exec/Helper/wrapper/address 테스트 합계 19건 통과
- 비식별 22.x DB clone의 fresh/upgrade 최종 DDL 일치
- 실제 22.x DB에서 aggregate 20 columns, section table 17 columns 확인
- qemu-exec-tools smoke 통과 및 RPM 생성:
  `ablestack-qemu-exec-tools-0.9.3-1.el9.el9.noarch.rpm`

### 14.3 배포 및 동작

- Management/UI와 Host 1·2·3의 runtime-aligned 최소 class를 배포했다.
- 세 Host는 모두 `Up`, `mold-agent.service`는 active이며 section schedule의
  관측 시각이 각 Host에서 계속 갱신된다.
- Host 2에 qemu-exec-tools 0.9.3 RPM을 파일럿 설치했으며 host package의
  guest customization skip guard와 Agent 정상 상태를 확인했다.
- 대상 Rocky VM의 최종 API/UI 상태:
  - aggregate `PARTIAL`, schema version 3, collector host ID 2
  - `ens3` IPv4 `10.1.1.41/24`는 QGA 주 IP이자 대표 IP
  - IPv6 link-local, loopback IPv4/IPv6 표시
  - DNS `10.1.1.1`, `8.8.8.8`, search domain 유지
  - routes만 `UNAVAILABLE`이며 `/usr/sbin/ip` SELinux permission denied를 표시
  - readiness는 `TOOLS_NOT_INSTALLED`, error code `HELPER_NOT_INSTALLED`
- 실제 light/dark UI에서 `IP 구성` 상단 안내와 IP/DNS/route 격리 표시,
  재수집 버튼을 확인했다. readiness/collector/Helper 진단 카드는 제거했으며
  최종 reload 후에도 수집 결과가 정상 유지됐다.

### 14.4 Rocky Helper E2E 완료 및 남은 metadata 개선

QGA `guest-file-*`로 RPM 431,432 bytes를 대상 Rocky guest의 `/var/tmp`에
전송했고 guest 내부 SHA-256이 배포 artifact와 일치함을 확인했다. QGA context
`virt_qemu_ga_t`에서는 `/usr/bin/rpm`, `/usr/bin/dnf` 실행이 모두 permission
denied이므로 console/SSH 등 privileged guest lifecycle에서 패키지를 설치했다.

설치 후 같은 QGA context에서 고정 Helper 실행이 성공했다.

- Helper schema/profile: `1` / `READY`
- addresses/routes/DNS: 모두 `OK`
- Cloud aggregate/readiness: `OK` / `READY`
- route: IPv4/IPv6 12개
- DNS: 서버 2개와 search domain
- 실제 다크 UI와 reload 이후 신규 console error: 0건

패키지 설치 후 업로드 RPM은 QGA를 통해 `/var/tmp`에서 제거했다.

남은 비차단 metadata 개선은 guest tools version이다. Helper가 runtime에
`rpm -q`를 호출하지만 Helper domain에서 RPM 실행이 제한되어 현재 `unknown`을
반환한다. 기능 수집에는 영향이 없으며, package build 시 version manifest를
설치하고 Helper가 read-only 파일에서 읽도록 변경하는 것이 후속 권장안이다.
