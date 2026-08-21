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

# #911 Storage Service 런타임 인플레이스 업그레이드 상세 설계

## 문서 정보

| 항목 | 내용 |
| --- | --- |
| 연계 이슈 | [#911 운영 중 Storage Service 런타임 코드 인플레이스 업그레이드 지원](https://github.com/ablecloud-team/ablestack-cloud/issues/911) |
| 작성일 | 2026-08-21 |
| 작업 브랜치 | `sharedfs/issue-911-runtime-inplace-upgrade` |
| 문서 상태 | 상세 설계 완료, 구현 전 |
| 검증 환경 | 22.x 개발 환경, Storage Service VM `i-2-608-VM`, 실행 호스트 `10.10.22.3` |

## 1. 목적

Storage Service SystemVM의 NFS, SMB, iSCSI, NVMe-oF 제어 코드는 현재 SystemVM
템플릿에 포함되어 배포된다. 런타임 코드 결함을 수정하려면 새 템플릿을 만들고
서비스 VM을 다시 생성해야 하므로, 데이터 서비스의 수정 배포 비용과 중단 위험이
크다.

이 설계는 커널이나 운영체제 패키지를 교체하지 않는 범위에서 Storage Service가
소유한 런타임 코드를 서명된 번들로 전달하고, 실행 중인 SystemVM 안에서 검증,
원자적 활성화, 상태 확인 및 자동 롤백하는 기능을 정의한다.

핵심 목표는 다음과 같다.

- 관리 서버에서 선택한 검증된 런타임 번들을 QGA로 SystemVM에 전송한다.
- 전송 중단과 재시도를 허용하고 동일 요청을 멱등하게 처리한다.
- 서명, 해시, 호환성, 디스크 여유 공간 및 현재 서비스 상태를 활성화 전에 검증한다.
- 프로토콜 세션과 데이터 마운트를 유지한 채 런타임 코드 포인터만 원자적으로 전환한다.
- 새 런타임 검증 실패 시 직전 검증 버전으로 자동 복구한다.
- 템플릿 버전과 런타임 버전을 분리해 API와 UI에서 명확히 표시한다.

## 2. 범위와 비범위

### 2.1 인플레이스 업그레이드 허용 범위

- `ablestack-storagectl`
- `ablestack-storage-boot-reconcile`
- `ablestack-storage-monitor`
- 위 실행 파일이 전용으로 사용하는 Python 모듈과 보조 스크립트
- Storage Service 전용 systemd unit 또는 drop-in 중 무중단 적용이 검증된 항목
- 자체 점검, 호환성 검사 및 런타임 전환을 위한 전용 updater

### 2.2 인플레이스 업그레이드 금지 범위

다음 변경은 런타임 번들로 처리하지 않고 SystemVM 템플릿 재빌드 및 서비스 VM
유지보수 절차로 보낸다.

- 커널, 커널 모듈 및 configfs 기능
- OS 패키지, 공용 라이브러리 및 패키지 의존성
- QEMU Guest Agent, libvirt, 하이퍼바이저 호스트 에이전트
- 디스크 파티션, 부트로더 또는 VM 재부팅이 필요한 변경
- `serviceImpact`가 `PROTOCOL_RESTART` 또는 `VM_REBOOT`인 번들
- 신뢰 키 저장소와 bootstrap updater 자체를 같은 트랜잭션에서 교체하는 변경

최초 구현은 `serviceImpact=NONE`만 허용한다. 그 외 영향 수준은 Preflight에서 실패로
판정하고 템플릿 업그레이드 또는 유지보수 작업이 필요하다는 이유를 반환한다.

## 3. 현행 구조와 문제점

### 3.1 코드 경로

| 계층 | 현재 구성 | 확인된 한계 |
| --- | --- | --- |
| API | `StorageServiceHostCommand`, `StorageServiceHostAnswer` | 일반 operation 문자열과 JSON payload 중심이며 대용량 파일 전송 계약이 없음 |
| 관리 서버 | `StorageServiceGuestCommandDispatcherImpl` | 단일 명령 실행만 담당하며 업그레이드 상태 머신과 재개 정보가 없음 |
| KVM 에이전트 | `LibvirtStorageServiceHostCommandWrapper` | 전체 payload를 Base64 문자열로 셸에 삽입한 뒤 `guest-exec` 실행 |
| SystemVM | `/usr/local/bin/ablestack-storagectl` 등 고정 경로 | 버전 디렉터리, 원자적 전환 포인터, 서명 검증 및 자동 롤백이 없음 |
| 부팅 복구 | `ablestack-storage-reconcile.service` | 고정 경로의 reconcile 실행 파일을 호출 |
| DB | Storage Service 구성 및 상태 테이블 | 런타임 번들, 업그레이드 트랜잭션, 현재/직전 버전 기록이 없음 |
| UI | 프로토콜 및 볼륨 관리 중심 | 템플릿 버전과 런타임 버전, Preflight 및 롤백 UI가 없음 |

### 3.2 실패 위험

- QGA 명령 중단 시 전송 완료 위치와 재시도 여부를 판단할 수 없다.
- 실행 파일을 직접 덮어쓰면 쓰기 도중 프로세스가 해당 파일을 읽을 수 있다.
- 새 코드가 시작되지 않아도 이전 버전을 자동 복원할 기준점이 없다.
- 관리 서버 재시작 후 업그레이드 작업의 현재 단계를 복구할 영속 상태가 없다.
- 구성 변경 실패와 런타임 코드 변경 실패가 동일한 `Error` 상태로 섞일 수 있다.

## 4. 실제 환경 Preflight 검증

### 4.1 검증 원칙

실행 중인 다중 프로토콜 Storage Service VM의 운영 경로와 desired state는 변경하지
않았다. 모든 파일은 `/opt/ablestack/storage-runtime-preflight` 아래에 만들고 검증 후
삭제했다. 검증 전후에 실행 파일 해시, systemd 상태, 리스너, 마운트 정보를 비교했다.

### 4.2 환경

| 항목 | 결과 |
| --- | --- |
| SystemVM | `i-2-608-VM` |
| 실행 호스트 | `10.10.22.3` |
| QGA 버전 | `7.2.22` |
| 전송 명령 | `guest-file-open`, `guest-file-write`, `guest-file-seek`, `guest-file-flush`, `guest-file-close` 사용 가능 |
| 실행 명령 | `guest-exec`, `guest-exec-status` 사용 가능 |
| 서명 도구 | OpenSSL 3.0.20, Ed25519 서명 검증 가능 |
| 동시 실행 제어 | `flock` 배타 잠금 성공 |
| 활성 서비스 | NFS, SMB, iSCSI, NVMe-oF 리스너가 존재하는 상태에서 검증 |

### 4.3 검증 결과

| 검증 항목 | 수행 결과 | 설계 반영 |
| --- | --- | --- |
| QGA 청크 전송 | 약 180 KiB 번들 3개를 각각 48 KiB 단위 4개 청크로 전송 성공 | 원시 청크 크기를 48 KiB로 고정하고 Base64 확장 크기를 별도 제한 |
| 해시 검증 | SystemVM 내부 SHA-256이 송신 측과 일치 | 청크 완료 후 archive SHA-256 필수 검증 |
| 오프셋 재전송 | offset 4 쓰기, offset 0 쓰기, offset 4 재전송 후 `AAAABBBB` 확인 | 명시적 offset과 청크 해시로 동일 청크 재요청을 멱등 처리 |
| 버전 전환 | `current.next` 심볼릭 링크 생성 후 `mv -Tf`로 v1에서 v2 전환 성공 | 실행 파일 직접 덮어쓰기 금지, 링크 원자 전환 사용 |
| 자체 점검 | v1, v2 모두 자체 점검 JSON 정상 반환 | 활성화 직후 updater가 고정된 self-test 계약 실행 |
| 실패 롤백 | 의도적으로 실패하는 vbad 활성화 후 v2로 자동 복구 | 새 버전 검증 실패 시 `previous`로 자동 전환 후 재검증 |
| 서명 검증 | 임시 Ed25519 키로 정확한 manifest 검증 성공, 변조 manifest 거부 | manifest 원문 바이트에 대한 detached Ed25519 서명 사용 |
| 잠금 | `flock` 배타 잠금 성공 | VM 내부 업그레이드 동시 실행 차단 |
| 운영 영향 | 검증 전후 실행 파일 해시, 서비스, 리스너, 마운트가 동일 | Preflight는 활성 런타임과 desired state를 변경하지 않음 |
| 원격 JSON 전달 | 이스케이프 없는 SSH 인자 전달은 QGA JSON 따옴표가 소실되어 실패 | JSON 문자열 연결 금지, 구조화 직렬화와 원격 인자 이스케이프를 단일 래퍼에서 강제 |

Preflight 결과로 QGA 기반의 재개 가능한 파일 전송, Ed25519 서명 검증, 버전 디렉터리
및 원자적 링크 전환, 실패 시 직전 버전 복원이 현재 22.x 개발 환경에서 기술적으로
가능함을 확인했다.

## 5. 목표 아키텍처

```text
Mold UI
  -> Management Server async job
    -> StorageServiceRuntimeUpgradeOrchestrator
      -> AgentManager
        -> KVM StorageServiceRuntimeHostCommandWrapper
          -> QGA guest-file-* / guest-exec
            -> ablestack-storage-runtime-updater
              -> versioned release + atomic current link
              -> existing desired-state reconcile
```

관리 서버가 트랜잭션의 제어 평면이 되고 SystemVM updater가 게스트 내부 파일 및 전환
작업의 유일한 쓰기 주체가 된다. KVM 에이전트는 임의 셸 명령을 전달하지 않고 정해진
QGA 파일 전송 및 updater 명령만 수행한다.

## 6. 런타임 번들 계약

### 6.1 산출물

```text
ablestack-storage-runtime-<version>.tar.gz
manifest.json
manifest.sig
SHA256SUMS
```

`manifest.sig`는 `manifest.json`의 파일 바이트를 재직렬화하지 않고 그대로 서명한
Ed25519 detached signature이다. 검증 키는 `keyId`로 선택한다.

### 6.2 manifest 필드

| 필드 | 설명 |
| --- | --- |
| `bundleVersion` | 제품 런타임 버전. DB와 SystemVM 디렉터리의 식별자 |
| `runtimeAbiVersion` | updater 및 wrapper와의 실행 계약 버전 |
| `desiredStateSchemaVersion` | 기존 desired state를 읽을 수 있는 스키마 버전 |
| `minManagerVersion`, `maxManagerVersion` | 관리 서버 호환 범위 |
| `minAgentVersion`, `maxAgentVersion` | 호스트 에이전트 호환 범위 |
| `minTemplateVersion`, `maxTemplateVersion` | bootstrap 및 OS 기능 호환 범위 |
| `serviceImpact` | `NONE`, `PROTOCOL_RESTART`, `VM_REBOOT` |
| `files[]` | 상대 경로, SHA-256, mode, owner, group |
| `keyId` | 신뢰 공개 키 식별자 |
| `buildCommit`, `buildTime` | 빌드 추적 정보 |

### 6.3 압축 해제 안전 규칙

- 절대 경로, `..`, 심볼릭 링크, 하드 링크, device node를 거부한다.
- 번들 내 경로는 manifest에 선언된 allowlist와 정확히 일치해야 한다.
- 파일 수, 개별 파일 크기, 전체 해제 크기, 경로 깊이에 상한을 둔다.
- 해제 후 각 파일의 SHA-256, mode, owner, group을 검증한다.
- 서명과 archive SHA-256 검증이 끝나기 전에는 실행 권한을 부여하지 않는다.

## 7. SystemVM 파일 배치와 updater

### 7.1 디렉터리

```text
/opt/ablestack/storage-runtime/
  updater/ablestack-storage-runtime-updater
  trusted-keys/<keyId>.pem
  releases/<bundleVersion>/
  current -> releases/<bundleVersion>
  previous -> releases/<bundleVersion>

/var/lib/ablestack-storage/runtime-updates/<transactionId>/
  manifest.json
  manifest.sig
  bundle.tar.gz
  state.json
  chunks/

/run/lock/ablestack-storage-runtime-upgrade.lock
```

`/usr/local/bin/ablestack-storagectl`, boot reconcile 및 monitor 진입점은 `current` 아래의
실행 파일을 호출하는 최소 wrapper로 변경한다. wrapper, updater, 신뢰 키는 같은
트랜잭션에서 교체하지 않는다. updater 자체 갱신은 템플릿 경로로 제한한다.

### 7.2 updater 명령 계약

```text
ablestack-storage-runtime-updater capabilities
ablestack-storage-runtime-updater begin
ablestack-storage-runtime-updater stage
ablestack-storage-runtime-updater verify
ablestack-storage-runtime-updater preflight
ablestack-storage-runtime-updater activate
ablestack-storage-runtime-updater status
ablestack-storage-runtime-updater rollback
ablestack-storage-runtime-updater cleanup
```

입력은 JSON stdin, 출력은 단일 JSON 객체로 통일한다. 상태 파일은 임시 파일 기록,
`fsync`, 원자적 rename 순서로 갱신한다. 모든 명령은 동일 transaction ID에 대해
멱등해야 한다.

### 7.3 기존 템플릿 bootstrap

- 새 SystemVM 템플릿에는 updater, wrapper, 신뢰 공개 키를 기본 포함한다.
- 기존 VM은 capability 응답에 updater가 없을 때만 bootstrap 후보가 된다.
- 관리 서버와 호스트 에이전트가 알고 있는 템플릿 버전 및 bootstrap 대상 파일
  해시가 allowlist와 일치해야 주입을 허용한다.
- bootstrap 파일은 제품 빌드에 고정된 SHA-256으로 재검증한다.
- bootstrap은 패키지 설치, 커널 변경 또는 신뢰 키 임의 추가를 수행하지 않는다.
- 검증할 수 없는 구형 템플릿은 인플레이스 업그레이드를 비활성화하고 템플릿 교체를 안내한다.

## 8. QGA 전송 및 KVM 에이전트 설계

### 8.1 전용 명령 객체

기존 일반 목적 `StorageServiceHostCommand`에 대용량 번들을 넣지 않고 다음 객체를
추가한다.

- `StorageServiceRuntimeHostCommand`
- `StorageServiceRuntimeHostAnswer`
- `StorageServiceRuntimeOperation`
  - `CAPABILITIES`
  - `BEGIN`
  - `WRITE_CHUNK`
  - `FINALIZE`
  - `PREFLIGHT`
  - `ACTIVATE`
  - `STATUS`
  - `ROLLBACK`
  - `CLEANUP`

KVM 플러그인에는 `LibvirtStorageServiceRuntimeHostCommandWrapper`를 추가한다.
기존 프로토콜 desired-state 명령 래퍼의 동작은 변경하지 않는다.

### 8.2 청크 전송

- 원시 데이터 청크는 Preflight에서 검증한 48 KiB로 고정한다.
- 각 요청은 transaction ID, sequence, byte offset, raw length, chunk SHA-256 및 Base64
  payload를 가진다.
- 게스트 파일은 transaction 전용 임시 경로만 허용한다.
- 각 청크는 `guest-file-open(r+b)`, `guest-file-seek`, `guest-file-write`,
  `guest-file-flush`, `guest-file-close` 순서로 쓴다.
- 같은 offset과 hash의 재요청은 성공으로 처리한다.
- 같은 offset에 다른 hash가 오면 트랜잭션을 중단한다.
- `FINALIZE`에서 파일 길이와 전체 SHA-256을 검증한다.
- 관리 서버 또는 에이전트 재시작 후 `STATUS`로 마지막 검증 청크를 조회하여 재개한다.

QGA JSON은 Jackson 등 구조화된 직렬화로 생성하고, 원격 virsh 실행 인자는 한 곳에서
이스케이프한다. shell fragment 연결과 전체 번들의 command-line 삽입을 금지한다.

### 8.3 guest-exec 제한

에이전트가 실행할 수 있는 경로는 고정된
`/opt/ablestack/storage-runtime/updater/ablestack-storage-runtime-updater`로 제한한다.
operation은 enum allowlist로 검증하며 사용자 입력을 셸 명령으로 조합하지 않는다.

## 9. 업그레이드 상태 머신

```text
AVAILABLE
  -> RECEIVING
  -> RECEIVED
  -> VERIFIED
  -> PREFLIGHT_OK
  -> ACTIVATING
  -> RECONCILING
  -> VERIFYING
  -> COMPLETE

실패:
  -> ROLLING_BACK
  -> ROLLED_BACK
  -> FAILED_MANUAL
```

관리 서버 DB와 SystemVM `state.json`에 동일 transaction ID와 phase를 기록한다.
phase 변경은 이전 phase의 완료 증거가 있을 때만 허용한다.

### 9.1 활성화 절차

1. 인스턴스별 관리 서버 잠금과 게스트 `flock`을 획득한다.
2. 현재 런타임 버전과 파일 해시, 서비스 상태, 리스너, 마운트, 세션 수 및 monitor
   상태를 snapshot한다.
3. 번들 서명, 해시, ABI, desired-state 스키마, 템플릿 및 디스크 여유 공간을 검증한다.
4. 새 release의 자체 점검을 비활성 상태에서 실행한다.
5. `current.next`를 만들고 `mv -Tf`로 `current`를 원자 전환한다.
6. 기존에 저장된 desired state를 새 boot reconcile로 다시 적용한다.
7. NFS, SMB, iSCSI, NVMe-oF 중 활성 프로토콜의 리스너와 런타임 객체를 검증한다.
8. `serviceImpact=NONE`인 경우 기존 리스너, 마운트 및 세션의 손실이 없는지 비교한다.
9. 검증 성공 시 현재/직전 runtime bundle을 DB에 확정하고 `COMPLETE`로 전환한다.

### 9.2 자동 롤백

활성화 후 검증이 실패하면 다음을 자동 실행한다.

1. `current`를 `previous` release로 원자 전환한다.
2. 직전 runtime의 boot reconcile로 동일 desired state를 재적용한다.
3. 프로토콜 리스너, 마운트, 세션 및 monitor를 다시 검증한다.
4. 복구 성공 시 `ROLLED_BACK`, 실패 시 `FAILED_MANUAL`로 기록한다.
5. 오류 응답에는 새 버전 실패 원인과 롤백 검증 결과를 분리해 포함한다.

런타임 롤백은 코드만 되돌리고 desired state를 변경하지 않는다. 구성 자체의 변경 실패
복구는 #892와 #909의 desired-state/LKG 설계가 담당한다.

## 10. 관리 서버 설계

### 10.1 신규 서비스

| 클래스 | 책임 |
| --- | --- |
| `StorageServiceRuntimeUpgradeManager` | 공개 서비스 계약과 권한 경계 |
| `StorageServiceRuntimeUpgradeManagerImpl` | API 요청 검증 및 async job 시작 |
| `StorageServiceRuntimeBundleRegistry` | 신뢰 가능한 번들 메타데이터 조회 및 등록 |
| `StorageServiceRuntimeCompatibilityValidator` | manager, agent, template, ABI, desired-state 호환성 검증 |
| `StorageServiceRuntimeUpgradeOrchestrator` | 상태 머신, 전송 재개, 활성화, 검증 및 롤백 조정 |
| `StorageServiceRuntimeHealthVerifier` | 프로토콜별 리스너, 마운트, 세션 및 monitor 검증 |

`StorageServiceManagerImpl.getCommands()`와 `StorageService` 명령 목록에는 관리자 전용
runtime API를 등록한다. 기존 NFS, SMB, iSCSI, NVMe-oF API 계약은 변경하지 않는다.

### 10.2 동시 실행과 장애 복구

- 최초 구현은 Storage Service instance별 DB 잠금과 게스트 `flock`을 사용한다.
- active upgrade가 있으면 프로토콜 구성 변경과 다른 runtime upgrade를 거부한다.
- heartbeat가 만료된 작업은 DB와 게스트 상태를 함께 조회하여 재개 또는 롤백한다.
- #897의 공통 장기 작업 큐가 구현되면 동일 lock/phase/progress 인터페이스로 교체한다.
- 관리 서버 재시작 시 `ACTIVATING` 이후 작업은 무조건 재실행하지 않고 게스트의
  current/previous 및 검증 증거를 먼저 확인한다.

## 11. API 설계

모든 변경 API는 관리자 전용 async API로 구현한다.

| API | 동작 |
| --- | --- |
| `listStorageServiceRuntimeBundles` | 사용 가능한 서명 번들과 호환 범위 조회 |
| `registerStorageServiceRuntimeBundle` | 신뢰된 제품 artifact 위치와 메타데이터 등록 |
| `getStorageServiceRuntimeUpgradeCapabilities` | 대상 VM의 updater, QGA, 템플릿, 현재 버전 조회 |
| `preflightStorageServiceRuntimeUpgrade` | 변경 없이 호환성, 영향, 공간, 서비스 상태 검증 |
| `upgradeStorageServiceRuntime` | 전송, 활성화, 검증 및 필요 시 자동 롤백 |
| `listStorageServiceRuntimeUpgrades` | 작업 이력, phase, 진행률, 결과 조회 |
| `rollbackStorageServiceRuntimeUpgrade` | 직전 검증 버전으로 명시적 롤백 |
| `deactivateStorageServiceRuntimeBundle` | 신규 적용 대상에서 번들 제외. 사용 중 번들은 삭제하지 않음 |

응답 객체는 다음처럼 분리한다.

- `StorageServiceRuntimeBundleResponse`
- `StorageServiceRuntimeCapabilityResponse`
- `StorageServiceRuntimeUpgradeResponse`

오류 코드는 최소한 다음을 구분한다.

- `RUNTIME_BUNDLE_SIGNATURE_INVALID`
- `RUNTIME_BUNDLE_INCOMPATIBLE`
- `RUNTIME_PREFLIGHT_FAILED`
- `RUNTIME_TRANSFER_FAILED`
- `RUNTIME_ACTIVATION_FAILED_ROLLED_BACK`
- `RUNTIME_ROLLBACK_FAILED_MANUAL_ACTION_REQUIRED`
- `RUNTIME_UPGRADE_CONFLICT`

## 12. DB 설계

스키마는 `schema-Europa-After.sql`과 실제 Europa 업그레이드 경로인
`schema-42210to42300.sql`에 동일하게 반영한다.

### 12.1 `storage_service_runtime_bundle`

| 컬럼 | 용도 |
| --- | --- |
| `id`, `uuid` | 내부 ID와 API UUID |
| `version` | 고유 bundle version |
| `runtime_abi_version` | updater ABI |
| `desired_state_schema_version` | desired-state 호환 버전 |
| `min_manager_version`, `max_manager_version` | 관리 서버 호환 범위 |
| `min_agent_version`, `max_agent_version` | 에이전트 호환 범위 |
| `min_template_version`, `max_template_version` | 템플릿 호환 범위 |
| `service_impact` | 영향 수준 |
| `artifact_location`, `artifact_size` | 신뢰 artifact 위치와 크기 |
| `sha256`, `manifest_sha256` | bundle과 manifest 해시 |
| `signature`, `signing_key_id` | 서명과 키 식별자 |
| `state`, `created`, `removed` | 배포 가능 상태와 수명주기 |

DB에는 번들 바이너리와 비밀 키를 저장하지 않는다.

### 12.2 `storage_service_runtime_upgrade`

| 컬럼 | 용도 |
| --- | --- |
| `id`, `uuid` | 작업 식별자 |
| `instance_id` | 대상 Storage Service instance |
| `bundle_id`, `previous_bundle_id` | 대상/직전 번들 |
| `state`, `phase`, `progress` | 상태 머신과 진행률 |
| `transaction_id` | 관리 서버와 게스트가 공유하는 고유 ID |
| `started`, `heartbeat`, `completed` | 장기 작업 복구 기준 |
| `preflight_json` | 변경 전 검증 결과 |
| `verification_json` | 활성화 후 검증 결과 |
| `rollback_result_json` | 자동/수동 롤백 결과 |
| `error_code`, `error_message` | 구조화 오류 |
| `created_by` | 감사 추적 사용자 |

### 12.3 `storage_service_instance` 확장

- `current_runtime_bundle_id`
- `previous_runtime_bundle_id`
- `runtime_state`
- `runtime_verified_at`

현재 번들 확정은 이전 값과 revision을 조건으로 한 CAS update로 처리해 동시에 실행된
작업이 서로의 결과를 덮어쓰지 못하게 한다.

## 13. UI 설계

SharedFS 상세 화면의 공통 작업에 `런타임 업그레이드`를 추가한다.

### 13.1 화면 구성

- 세로형 모달과 다크모드 토큰을 사용한다.
- 현재 SystemVM 템플릿 버전과 현재 Storage runtime 버전을 별도 표시한다.
- 호환 번들만 선택 가능하게 하고 비활성 사유를 tooltip으로 제공한다.
- Preflight 결과에 영향 수준, 필요 공간, 현재 서비스 상태 및 예상 작업을 표시한다.
- 사용자가 확인하면 모달을 닫고 상단 async 알림과 작업 이력에서 진행 상태를 갱신한다.
- 전체 탭을 다시 로딩하거나 화면을 흐리게 하지 않고 runtime 관련 필드만 갱신한다.
- 실패 시 활성화 오류와 자동 롤백 결과를 분리해 표시한다.
- 수동 롤백은 직전 검증 버전이 있고 다른 작업이 없을 때만 활성화한다.

### 13.2 상태 표시

- `Current`, `Upgrade available`, `Transferring`, `Preflight`, `Activating`,
  `Verifying`, `Rolled back`, `Manual recovery required`
- phase, 진행률, 시작/마지막 heartbeat, 대상 버전, 직전 버전
- 프로토콜별 검증 결과와 monitor cache 갱신 시각

## 14. 보안 및 감사

- 제품 빌드의 오프라인 Ed25519 개인 키로 manifest를 서명한다.
- SystemVM에는 공개 키만 포함하고 `keyId` 기반 키 교체 중첩 기간을 지원한다.
- manifest 원문 바이트를 서명 검증하며 파싱 후 재직렬화한 값을 검증하지 않는다.
- 버전 다운그레이드는 일반 upgrade API에서 거부하고 명시적 rollback API만 허용한다.
- 같은 bundle version에 다른 SHA-256을 등록할 수 없다.
- API는 관리자 권한과 대상 인스턴스 접근 권한을 모두 검증한다.
- 번들 등록, Preflight, 활성화, 롤백 및 실패는 event/audit log에 남긴다.
- desired state의 AD, CHAP, 로컬 사용자 비밀번호 등 비밀 값은 snapshot이나 로그에 남기지 않는다.

## 15. 릴리즈 파이프라인

KVM SystemVM 템플릿과 runtime bundle은 같은 staged SystemVM 소스에서 생성한다.

1. Storage Service runtime 파일을 staging 디렉터리에 설치한다.
2. manifest를 만들고 파일 해시와 권한을 기록한다.
3. runtime bundle과 KVM SystemVM 템플릿을 각각 생성한다.
4. 템플릿 내부 초기 runtime과 bundle의 version 및 파일 SHA-256 일치를 검사한다.
5. manifest를 Ed25519로 서명한다.
6. bundle, manifest, signature 및 `SHA256SUMS`를 release artifact로 게시한다.
7. CI에서 서명 검증, 안전한 압축 해제, self-test 및 실패 bundle 거부를 실행한다.

서명 키가 없는 일반 PR 빌드는 테스트 키로 검증하되 release artifact를 게시하지 않는다.
정식 릴리즈는 보호된 CI secret과 승인된 환경에서만 서명한다.

## 16. 테스트 설계

### 16.1 단위 테스트

- manifest 파싱, 경로 traversal 및 링크 거부
- Ed25519 정상/변조/알 수 없는 keyId 검증
- manager, agent, template, ABI, desired-state 버전 호환성
- 상태 머신의 정상 전이와 잘못된 전이 거부
- chunk offset/hash 멱등성과 충돌 거부
- CAS update와 동시 작업 거부

### 16.2 KVM 에이전트 테스트

- QGA capability 감지
- 48 KiB 다중 청크 전송 및 전체 해시 검증
- 전송 중 agent/management 재시작 후 재개
- JSON 인자 이스케이프와 비 ASCII 오류 메시지 보존
- 허용되지 않은 guest 경로와 operation 거부

### 16.3 SystemVM 테스트

- 안전한 압축 해제와 파일 권한 적용
- 자체 점검 성공/실패
- `current` 원자 전환
- activation 실패 후 `previous` 자동 복구
- updater 및 `flock` 멱등성
- boot reconcile과 monitor가 `current` runtime을 사용하는지 확인

### 16.4 E2E 테스트

- NFS, SMB, iSCSI, NVMe-oF가 함께 활성화된 VM에서 `serviceImpact=NONE` 업그레이드
- 각 프로토콜의 기존 세션, 리스너, 마운트 및 쓰기 작업 유지
- 관리 서버/호스트 에이전트/SystemVM별 장애 주입과 재개
- 손상 bundle, 잘못된 서명, 공간 부족, 비호환 템플릿 거부
- 새 버전 검증 실패 후 자동 롤백 및 서비스 정상화
- SystemVM 재부팅 후 선택된 runtime과 desired state 복구
- API, DB, SystemVM 상태 및 UI 표시 정합성

## 17. 구현 순서

1. runtime bundle, manifest, 서명 및 updater 계약과 단위 테스트
2. KVM 에이전트 전용 QGA 파일 전송 명령과 재개 테스트
3. DB migration, DAO, 관리 서버 orchestrator 및 관리자 API
4. SystemVM wrapper, updater, boot reconcile 연계 및 KVM 템플릿 반영
5. UI Preflight, 진행률, 결과 및 롤백 화면
6. 릴리즈 bundle 생성/서명 파이프라인과 다중 프로토콜 E2E

각 단계는 이전 단계의 artifact와 상태 계약을 변경하지 않는 조건으로 진행한다.

## 18. 관련 이슈와 경계

| 이슈 | 연계 방식 |
| --- | --- |
| #892 구성 변경 원자성/롤백 | #911은 runtime code만 전환하고 기존 desired state를 그대로 reconcile한다. 구성 데이터 rollback은 #892가 담당한다. |
| #897 장기 작업/리소스 보호 | #911의 instance lock, phase, progress, heartbeat 계약을 추후 공통 작업 큐로 수용한다. |
| #909 백업 번들/LKG 복원 | #911 검증 결과를 LKG 증거로 제공할 수 있으나 runtime bundle과 구성 백업 번들은 분리 보관한다. |

## 19. 완료 기준

- 실행 중인 Storage Service VM에 서명된 runtime bundle을 QGA로 재개 가능하게 전송한다.
- 잘못된 서명, 손상 파일, 비호환 버전 및 영향 수준을 활성화 전에 거부한다.
- 실행 파일을 덮어쓰지 않고 versioned release와 원자적 `current` 전환을 사용한다.
- NFS, SMB, iSCSI, NVMe-oF 서비스 상태를 활성화 후 검증한다.
- 검증 실패 시 직전 검증 runtime으로 자동 롤백하고 결과를 API, DB, UI에 일치시킨다.
- 관리 서버 또는 에이전트 재시작 후 동일 transaction을 중복 실행하지 않고 재개한다.
- SystemVM 재부팅 후 선택된 runtime과 기존 desired state가 정상 복구된다.
- 템플릿 버전과 runtime 버전이 UI와 API에서 분리되어 추적된다.
- 릴리즈 artifact의 번들, manifest, 서명 및 SystemVM 템플릿 내 runtime 해시가 일치한다.

## 20. 결론

실제 22.x Storage Service VM Preflight에서 QGA 청크 전송과 오프셋 재전송,
Ed25519 서명 검증, 버전 디렉터리 전환 및 실패 시 자동 롤백을 확인했다. 따라서
Storage Service 소유 런타임에 한정한 인플레이스 업그레이드는 현재 아키텍처에서
구현 가능하다.

구현의 핵심 안전 장치는 서명된 불변 bundle, 전용 QGA 전송 명령, 원자적 포인터
전환, 기존 desired state 재적용, 프로토콜별 사후 검증 및 자동 롤백이다. 커널이나
패키지 의존성이 바뀌는 변경은 이 경로에 포함하지 않고 KVM SystemVM 템플릿
업그레이드로 명확히 분리한다.
