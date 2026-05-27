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
# `main -> ablestack-europa` 체리픽 후보 추적 문서

## 목적

이 문서는 `apache/cloudstack`의 `main`과 동일하게 유지되는 현재 `local/main` 브랜치에서,
`ablestack-europa`로 반영할 체리픽 후보를 체계적으로 선별하고 처리 내역을 기록하기 위한 작업 문서다.

핵심 목표는 다음과 같다.

- 이미 `europa`에 patch-equivalent 또는 기능 선반영된 커밋은 제외한다.
- 남은 후보는 영향도와 충돌 위험도 기준으로 배치화해 순차 반영한다.
- 반영/제외/보류/부분반영 상태를 문서에 계속 누적해 다음 sync 때 재검토 비용을 줄인다.

## 기준 시점

- 기준 일시: `2026-04-23`
- 소스 브랜치: `main` = `3166e64891fc75d4d32b66d874cff3f613b09b52`
- 대상 브랜치: `ablestack-europa` = `a69fb4b3e7b89ec63e4be28129e5c52747c76cf1`

## 현재 후보 규모

`git cherry -v ablestack-europa main` 기준 현재 상태는 아래와 같다.

- `+` 미반영 후보: `985`건
- `-` patch-equivalent 또는 선반영으로 간주 가능한 커밋: `1075`건
- 이번 문서의 우선 검토 배치: `33`건

즉, 전체 raw candidate는 `985`건이지만, 실제 작업은 우선 검토 배치를 먼저 처리하고 나머지는 후속 배치로 넘긴다.

1차 우선 검토 배치 `33`건의 판정 결과는 아래와 같다.

- `Already Satisfied`: `33`
- `Partially Satisfied`: `0`
- `Ready`: `0`
- 실제 체리픽 필요: `0`

따라서 다음 단계는 raw candidate 전체를 다시 보지 않고, 최근 `main` 커밋 중 `europa`에 같은 subject가 없는 항목을 다시 추출해 후속 검토 배치를 만드는 것이다.

## 판정 규칙

각 후보는 아래 절차로 판정한다.

1. `git cherry -v ablestack-europa main`으로 `+` 후보만 대상으로 잡는다.
2. `git show <sha> | git apply --reverse --check`가 통과하면 `Already Satisfied` 후보로 재분류한다.
3. 핵심 파일, 설정 키, API, DB 변경점이 `europa`에 다른 방식으로 선반영돼 있는지 기능 단위로 확인한다.
4. 선반영이 아니면 `replay/*` 작업 브랜치에서 체리픽하고, 충돌 해결 방안과 기능 영향도를 문서에 기록한다.

## 상태 표기

- `Pending`: 아직 검토 전
- `Ready`: 체리픽 후보로 유지
- `Already Satisfied`: `europa`에 사실상 반영됨
- `Partially Satisfied`: 일부만 반영됨, 후속 적응 필요
- `Cherry-picked`: 반영 완료
- `Deferred`: 대형 기능이라 후순위로 보류
- `Excluded`: 정책상 제외

## 갱신 명령

```bash
git cherry -v ablestack-europa main
git show <sha> | git apply --reverse --check
git show --stat <sha>
```

## 우선 검토 배치

### Batch A - 저위험 운영/서버 버그 수정

| Status | SHA | Subject | 메모 |
| --- | --- | --- | --- |
| `Already Satisfied` | `3b11663d87e3` | `Fix failure on agent reconnection (#8089)` | `europa`에 `PingAnswer.sendStartup`, agent 재초기화, smoke test까지 선반영 확인 |
| `Already Satisfied` | `3c7c75bacfd5` | `Clear pool id if volume allocation fails (#8202)` | `Allocated + poolId` 볼륨의 `poolId` 초기화 로직과 테스트가 `europa`에 선반영 확인 |
| `Already Satisfied` | `a11fc43788e8` | `server: fix diskoffering details in vm response (#8135)` | VM response의 disk offering 보정과 설명 문구가 `europa`에 선반영 확인 |
| `Already Satisfied` | `ce586e3eca5c` | `server: fix resource count during assign volume (#8171)` | `assign volume` 경로가 `check/increment/decrementVolumeResourceCount` 구조로 선반영 확인 |
| `Already Satisfied` | `de095ba70d2a` | `server: fix url check for storages without a valid url (#8353)` | storage URL 파싱 방어와 관련 테스트가 `europa`에 선반영 확인 |
| `Already Satisfied` | `08749d8354f2` | `server: skip password policies check on empty password (#8370)` | empty password / empty regex skip 로직이 `europa`에 선반영 확인 |
| `Already Satisfied` | `c3b77cb7b82b` | `Fix host stuck in connecting state (#8502)` | Ready/modifyStoragePool/script timeout 보호 로직이 `europa`에 선반영 확인 |
| `Already Satisfied` | `2b28a664fe0d` | `Updated jetty maxFormContentSize value to 1048576 bytes (#8420)` | `request.content.size`와 Jetty maxFormContentSize 적용 로직이 `europa`에 선반영 확인 |

### Batch B - 저위험 UI/UX 수정

| Status | SHA | Subject | 메모 |
| --- | --- | --- | --- |
| `Already Satisfied` | `e6f048bc2e83` | `CKS: fix wrong format of cluster size on UI (#8182)` | `/kubernetes` 경로의 size raw 표시 예외가 `europa`에 선반영 확인 |
| `Already Satisfied` | `127fd9d2f06e` | `UI: Project column in Default View (#8287)` | project column 렌더링과 default view 노출 로직이 `europa`에 선반영 확인 |
| `Already Satisfied` | `746bae740eaa` | `ui: fix default domainid for add account (#8435)` | `domainid=0` 문제는 사라졌고 route/user domain 기본값 구조로 선반영 확인 |
| `Already Satisfied` | `5c32a0edbaa5` | `ui: prevent scheduling readyforshutdown job when api inaccessible (#8448)` | `readyForShutdown` API 접근 가능할 때만 polling job 생성하는 가드가 `europa`에 선반영 확인 |
| `Already Satisfied` | `b2e29931e898` | `UI: fix icmp code/type of ACL rule are not display if the value is 0 (#8589)` | ACL rule 화면의 `0` 값 표시 조건식이 `europa`에 선반영 확인 |
| `Already Satisfied` | `19250403e645` | `ui: fix create k8s cluster multiple listing (#8539)` | Kubernetes cluster 생성 화면의 networks/keyPairs 초기화 및 중복 fetch 정리가 `europa`에 선반영 확인 |
| `Already Satisfied` | `c43b7c04f4cf` | `ui: fix labels when migrating instances from vmware (#8490)` | 동일 subject 커밋 `e4c6fba566`가 `europa` 이력에 존재 |
| `Already Satisfied` | `3bcf6f0faf49` | `Rename "Import QCOW...." to "Import QCOW2....." (#8519)` | 동일 subject 커밋 `1107dd3394`가 `europa` 이력에 존재 |

### Batch C - 중간 리스크 KVM/Storage 보강

| Status | SHA | Subject | 메모 |
| --- | --- | --- | --- |
| `Already Satisfied` | `db6dd52f443b` | `kvm: fix ide controller for rocky/alma vms (#8247)` | 동일 subject 커밋 `c2e75e5474`가 `europa` 이력에 존재 |
| `Already Satisfied` | `7ea068c4dcfa` | `kvm: fix error 'Failed to find passphrase for keystore: cloud.jks' when enable SSL for kvm agent (#7923)` | 동일 subject 커밋 `20d5ee32f5`가 `europa` 이력에 존재 |
| `Already Satisfied` | `267a457efc55` | `Externalize KVM HA heartbeat frequency (#6892)` | 동일 subject 커밋 `c52192d041`가 `europa` 이력에 존재 |
| `Already Satisfied` | `bba554bcc473` | `linstor: Fix possible NPE if Linstor storage-pool data missing (#8319)` | 동일 subject 커밋 `7b7b11f21c`가 `europa` 이력에 존재 |
| `Already Satisfied` | `68e504aff97d` | `Linstor backup snaphots (#8067)` | 동일 subject 커밋 `b7ba019c9e`가 `europa` 이력에 존재 |
| `Already Satisfied` | `b0910fc61d7b` | `Add dynamic secondary storage selection (#7659)` | 동일 subject 커밋 `70415f34ef`가 `europa` 이력에 존재 |
| `Already Satisfied` | `26b01f6f3be3` | `Flexible tags for hosts and storage pools (#7489)` | 동일 subject 커밋 `a53d53a4ef`가 `europa` 이력에 존재 |
| `Already Satisfied` | `60017723357c` | `multi local storage handling for kvm (#6699)` | 동일 subject 커밋 `0cc34fa1de`가 `europa` 이력에 존재 |
| `Already Satisfied` | `3bb318bab905` | `kvm: Add support for cgroupv2 (#8252)` | 동일 subject 커밋 `49dcde4975`가 `europa` 이력에 존재 |
| `Already Satisfied` | `5361b415e6af` | `Image Store: View Access status of the image store and view events (#8467)` | 동일 subject 커밋 `56fd791abf`가 `europa` 이력에 존재 |

### Batch D - 대형 기능, 별도 설계 후 진행

| Status | SHA | Subject | 메모 |
| --- | --- | --- | --- |
| `Already Satisfied` | `371ad9f55b35` | `New Feature: Import VMware VMs into KVM (#7881)` | 동일 subject 커밋 `8d4645d8d4`가 `europa` 이력에 존재 |
| `Already Satisfied` | `ab20b1220fea` | `KVM Ingestion - Import Instance (#7976)` | 동일 subject 커밋 `d7c71013fa`가 `europa` 이력에 존재 |
| `Already Satisfied` | `5651eab49cf3` | `ObjectStore Framework with MinIO and Simulator plugins (#7752)` | 동일 subject 커밋 `f0218dd822`가 `europa` 이력에 존재 |
| `Already Satisfied` | `33bb92acce2d` | `Veeam: Support Veeam 11 and 12 (#8241)` | 핵심 API/model/test 파일과 후속 수정 `7d5a018e36`가 `europa`에 존재 |
| `Already Satisfied` | `b34f09313738` | `veeam: fix some issues with restoring volume from backup and attaching it to VM (#8570)` | 동일 subject 커밋 `7d5a018e36`가 `europa` 이력에 존재 |
| `Already Satisfied` | `1f29f6f04096` | `Public IP quarantine feature (#7378)` | 동일 subject 커밋 `ece08ff703`가 `europa` 이력에 존재 |
| `Already Satisfied` | `ab70108f1573` | `CKS: create Security Groups for CKS clusters of each account (#8316)` | 동일 subject 커밋 `3ea73dcc72`가 `europa` 이력에 존재 |

## 이미 제외된 선반영 예시

아래는 이전 검토에서 `europa`에 이미 patch-equivalent 또는 기능 선반영으로 판단해 이번 우선 배치에서 제외한 예시다.

- `c6936889f5` `server: prevent adding vm compute details when not applicable`
- `e0fe953791` `NSX SDK list operations are pageable`
- `1fc4cb90bf` `Routed VR: accept packets from related and established connections`

## 상세 판정 기록

### Record 001 - `3b11663d87e3` `Fix failure on agent reconnection (#8089)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- `PingAnswer`에 `sendStartup` 플래그 추가
- management server가 host 상태를 보고 agent에 startup 재전송을 요청
- agent가 `PingAnswer.isSendStartup()`일 때 `sendStartup(link)`로 재초기화
- smoke test `test_host_ping.py` 추가

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [core/src/main/java/com/cloud/agent/api/PingAnswer.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/core/src/main/java/com/cloud/agent/api/PingAnswer.java)
  - `sendStartup` 필드가 이미 존재
  - Apache 원본보다 확장된 `avoidMsList`, `reconcileCommands`도 함께 관리
- [agent/src/main/java/com/cloud/agent/Agent.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/agent/src/main/java/com/cloud/agent/Agent.java)
  - `processPingAnswer()`에서 `answer.isSendStartup()`이면 `sendStartup(link)` 수행
  - 이후 `avoidMsList` 처리까지 추가됨
- [engine/orchestration/src/main/java/com/cloud/agent/manager/AgentManagerImpl.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/engine/orchestration/src/main/java/com/cloud/agent/manager/AgentManagerImpl.java)
  - host 상태가 `Up`이 아니면 `requestStartupCommand = true`
  - `new PingAnswer((PingCommand)cmd, avoidMsList, requestStartupCommand)`로 Apache 의도를 포함
- [test/integration/smoke/test_host_ping.py](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/test/integration/smoke/test_host_ping.py)
  - Apache 원본에서 추가된 smoke test가 이미 존재

추가 확인 결과:

- `git show 3b11663d87e3 | git apply --reverse --check`는 실패
- 하지만 실패 원인은 “미반영”이라기보다 `europa` 쪽이 같은 기능을 후속 확장 커밋으로 흡수해 patch shape가 달라졌기 때문으로 판단
- `git blame` 기준으로도 핵심 라인은 이후 커밋에서 이미 도입됨

이 항목은 이후 같은 유형의 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- reverse-apply 실패만으로 곧바로 `미반영`으로 보지 않는다
- 핵심 메서드, 상태 플래그, smoke test 존재 여부까지 같이 본다
- Apache 원본보다 확장된 형태로 기능이 살아 있으면 `Already Satisfied`로 분류한다

### Record 002 - `3c7c75bacfd5` `Clear pool id if volume allocation fails (#8202)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- `Allocated` 상태인데 `poolId`가 남아 있는 볼륨은 재배치 전에 `poolId`를 `NULL`로 초기화
- 초기화 후 `_volsDao.update(...)`로 반영
- 관련 planner/storage 테스트 추가

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [server/src/main/java/com/cloud/deploy/DeploymentPlanningManagerImpl.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/main/java/com/cloud/deploy/DeploymentPlanningManagerImpl.java:1765)
  - `Allocated` 상태 + `poolId != null`이면 `setPoolId(null)` 수행
  - `_volsDao.update(...)` 실패 시 예외 처리
  - 디버그 로그까지 포함
- [server/src/test/java/com/cloud/deploy/DeploymentPlanningManagerImplTest.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/test/java/com/cloud/deploy/DeploymentPlanningManagerImplTest.java:830)
  - `verify(vol1, times(1)).setPoolId(null);`
- [server/src/test/java/com/cloud/storage/StorageManagerImplTest.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/test/java/com/cloud/storage/StorageManagerImplTest.java:255)
  - `Allocated` 상태에서 `poolId` 유무에 따른 storage pool 호환성 테스트 존재

추가 확인 결과:

- `git show 3c7c75bacfd5 | git apply --reverse --check`는 실패
- 하지만 핵심 구현과 테스트가 현재 `europa`에 이미 존재
- `git blame` 기준으로도 핵심 라인은 `aeae5b5271` 시점에 반영되었고, 이후 로그/리팩터링 커밋이 추가됨

이 항목은 이후 비슷한 storage/planner 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- reverse-apply 실패만으로 `미반영`으로 단정하지 않는다
- planner 본 구현과 unit test가 함께 있는지 확인한다
- 원본 기능이 테스트까지 포함해 살아 있으면 `Already Satisfied`로 분류한다

### Record 003 - `a11fc43788e8` `server: fix diskoffering details in vm response (#8135)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- `UserVmResponse`의 `diskOfferingId`, `diskOfferingName` 설명 문구를 보강
- `UserVmJoinDaoImpl`에서 VM 응답의 disk offering 필드가 비어 있고 현재 row가 `ROOT` volume이 아니면 join row의 disk offering 값을 채워서 응답 정합성을 보정

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [api/src/main/java/org/apache/cloudstack/api/response/UserVmResponse.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/api/src/main/java/org/apache/cloudstack/api/response/UserVmResponse.java:178)
  - Apache 원본과 같은 취지의 설명 문구가 이미 반영됨
- [server/src/main/java/com/cloud/api/query/dao/UserVmJoinDaoImpl.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/main/java/com/cloud/api/query/dao/UserVmJoinDaoImpl.java:719)
  - `StringUtils.isEmpty(userVmData.getDiskOfferingId()) && !Volume.Type.ROOT.equals(uvo.getVolumeType())` 조건으로 disk offering 값을 보정

추가 확인 결과:

- `git show a11fc43788e8 | git apply --reverse --check`는 실패
- 하지만 Apache 원본의 핵심 변경 두 건이 모두 현재 `europa` 코드에 존재
- 이 항목도 patch shape 차이일 뿐 기능은 이미 선반영된 상태로 판단

이 항목은 이후 API response 정합성 관련 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- response DTO의 설명 문구 변경과 DAO 보정 로직을 함께 확인한다
- reverse-apply 실패보다 “응답 필드가 실제로 채워지는지”가 더 중요하다
- API 응답 보정 로직이 이미 있으면 `Already Satisfied`로 분류한다

### Record 004 - `ce586e3eca5c` `server: fix resource count during assign volume (#8171)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- `assign volume` 경로에서 `ResourceType.volume`은 용량이 아니라 “볼륨 개수”를 의미하므로
- 계정 변경 시 size 기반 GiB 회계가 아니라 count 기반 회계로 처리

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [server/src/main/java/com/cloud/storage/VolumeApiServiceImpl.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/main/java/com/cloud/storage/VolumeApiServiceImpl.java:4671)
  - `checkVolumeResourceLimit(...)`
  - `decrementVolumeResourceCount(...)`
  - `incrementVolumeResourceCount(...)`
  를 사용해 Apache 원본보다 상위 추상화된 resource count 회계 경로로 정리되어 있음
- [server/src/test/java/com/cloud/storage/VolumeApiServiceImplTest.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/test/java/com/cloud/storage/VolumeApiServiceImplTest.java:1493)
  - `decrementVolumeResourceCount(...)` 검증
- [server/src/test/java/com/cloud/storage/VolumeApiServiceImplTest.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/test/java/com/cloud/storage/VolumeApiServiceImplTest.java:1500)
  - `incrementVolumeResourceCount(...)` 검증

추가 확인 결과:

- `git show ce586e3eca5c | git apply --reverse --check`는 실패
- 하지만 현재 `europa`는 Apache 원본보다 더 진화한 resource limit helper 구조를 사용
- `git log`에도 같은 주제의 `2ea4986fd6` 이력이 확인됨

이 항목은 이후 resource count/resource reservation 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- 원본의 low-level count 수정이 상위 helper로 흡수됐는지 먼저 본다
- 현재 구현이 더 일반화된 회계 API를 쓰고 있으면 선반영 가능성이 높다
- 테스트가 해당 helper 호출을 검증하면 `Already Satisfied`로 분류한다

### Record 005 - `de095ba70d2a` `server: fix url check for storages without a valid url (#8353)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- `extractUriParamsAsMap()`에서 URL이 아닌 managed storage 문자열에 대해 예외로 중단하지 않고 빈 `Map`을 반환
- `scheme`이 없으면 빈 `Map`을 반환
- `port`는 유효할 때만 `uriParams`에 추가
- 관련 테스트 추가

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [server/src/main/java/com/cloud/storage/StorageManagerImpl.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/main/java/com/cloud/storage/StorageManagerImpl.java:1098)
  - `UriUtils.getUriInfo(url)` 실패 시 빈 `uriParams` 반환
  - `scheme == null`이면 빈 `uriParams` 반환
  - `URLDecoder` 처리 포함
  - `uriInfo.getPort() > 0`일 때만 `port` 추가
- [server/src/test/java/com/cloud/storage/StorageManagerImplTest.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/test/java/com/cloud/storage/StorageManagerImplTest.java:283)
  - SolidFire 스타일 문자열이면 빈 `Map` 반환 검증
- [server/src/test/java/com/cloud/storage/StorageManagerImplTest.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/test/java/com/cloud/storage/StorageManagerImplTest.java:291)
  - NFS URL이면 파라미터가 채워지는지 검증
- [server/src/test/java/com/cloud/storage/StorageManagerImplTest.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/test/java/com/cloud/storage/StorageManagerImplTest.java:302)
  - local storage host/path validation failure 테스트 존재

추가 확인 결과:

- `git show de095ba70d2a | git apply --reverse --check`는 실패
- 하지만 핵심 구현과 테스트가 현재 `europa`에 존재
- `git blame` 기준으로도 핵심 구현 라인은 2023-12-15 시점에 이미 반영됨

이 항목은 이후 storage URL validation 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- URL parser 방어 로직과 테스트를 세트로 본다
- managed storage처럼 “URL 형식이 아닐 수 있는 입력”을 허용하는 분기 유무를 본다
- 예외 대신 빈 `Map` 반환 구조가 이미 있으면 `Already Satisfied`로 분류한다

### Record 006 - `08749d8354f2` `server: skip password policies check on empty password (#8370)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- empty password면 password policy 검사를 건너뜀
- `password.policy.regex`가 `null`뿐 아니라 빈 문자열이어도 regex 검사를 건너뜀

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [server/src/main/java/com/cloud/user/PasswordPolicyImpl.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/main/java/com/cloud/user/PasswordPolicyImpl.java:59)
  - `if (StringUtils.isEmpty(password))` 분기가 이미 존재
  - empty password면 경고 로그 후 즉시 `return`
- [server/src/main/java/com/cloud/user/PasswordPolicyImpl.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/server/src/main/java/com/cloud/user/PasswordPolicyImpl.java:247)
  - `if (StringUtils.isEmpty(passwordPolicyRegex))` 분기가 이미 존재
  - regex가 비어 있으면 검사 skip

추가 확인 결과:

- `git show 08749d8354f2 | git apply --reverse --check`는 실패
- 하지만 Apache 원본이 도입한 핵심 조건 분기 두 개가 현재 `europa` 구현에 모두 존재

이 항목은 이후 password policy 관련 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- 입력값이 empty/null일 때 조기 반환하는 방어 로직을 먼저 본다
- 설정값이 `null`뿐 아니라 빈 문자열까지 허용하는지 확인한다
- 핵심 조건 분기가 그대로 있으면 `Already Satisfied`로 분류한다

### Record 007 - `c3b77cb7b82b` `Fix host stuck in connecting state (#8502)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- `ReadyCommand`에 wait 설정 추가
- `ModifyStoragePoolCommand`에 wait 설정 추가
- KVM `LibvirtReadyCommandWrapper`의 script 실행에 timeout 추가

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [engine/orchestration/src/main/java/com/cloud/agent/manager/AgentManagerImpl.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/engine/orchestration/src/main/java/com/cloud/agent/manager/AgentManagerImpl.java:792)
  - `ready.setWait(ReadyCommandWait.value())`가 이미 존재
  - Apache 원본의 고정 `60`초보다 일반화된 설정 기반 구현
- [engine/storage/volume/src/main/java/org/apache/cloudstack/storage/datastore/provider/DefaultHostListener.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/engine/storage/volume/src/main/java/org/apache/cloudstack/storage/datastore/provider/DefaultHostListener.java:144)
  - `cmd.setWait(modifyStoragePoolCommandWait)`가 이미 존재
  - 현재는 `300`초 상수로 관리
- [plugins/hypervisors/kvm/src/main/java/com/cloud/hypervisor/kvm/resource/wrapper/LibvirtReadyCommandWrapper.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/plugins/hypervisors/kvm/src/main/java/com/cloud/hypervisor/kvm/resource/wrapper/LibvirtReadyCommandWrapper.java:68)
  - Apache 원본보다 진화한 형태로 `AgentProperties.AGENT_SCRIPT_TIMEOUT` 기반 timeout 실행 사용

추가 확인 결과:

- `git show c3b77cb7b82b | git apply --reverse --check`는 실패
- 하지만 Apache 원본의 핵심 수정 3개 포인트가 모두 현재 `europa`에 더 확장된 형태로 존재

이 항목은 이후 timeout/ready/host connect 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- 고정 timeout 숫자보다 현재 구현이 설정화/일반화됐는지 먼저 본다
- agent, management server, storage host listener의 3축이 함께 살아 있는지 확인한다
- 원본보다 상위 구조로 흡수된 경우 `Already Satisfied`로 분류한다

### Record 008 - `2b28a664fe0d` `Updated jetty maxFormContentSize value to 1048576 bytes (#8420)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- `client/conf/server.properties.in`에 `request.content.size=1048576` 추가
- `ServerDaemon`이 이 값을 읽어 Jetty의 form content size에 반영

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [client/conf/server.properties.in](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/client/conf/server.properties.in:33)
  - `request.content.size=1048576` 설정이 이미 존재
- [client/src/main/java/org/apache/cloudstack/ServerDaemon.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/client/src/main/java/org/apache/cloudstack/ServerDaemon.java:77)
  - `REQUEST_CONTENT_SIZE_KEY`, `DEFAULT_REQUEST_CONTENT_SIZE` 존재
- [client/src/main/java/org/apache/cloudstack/ServerDaemon.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/client/src/main/java/org/apache/cloudstack/ServerDaemon.java:144)
  - 설정값을 읽어 `setMaxFormContentSize(...)` 호출
- [client/src/main/java/org/apache/cloudstack/ServerDaemon.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/client/src/main/java/org/apache/cloudstack/ServerDaemon.java:198)
  - `server.setAttribute(ContextHandler.MAX_FORM_CONTENT_SIZE_KEY, maxFormContentSize)`
- [client/src/main/java/org/apache/cloudstack/ServerDaemon.java](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/client/src/main/java/org/apache/cloudstack/ServerDaemon.java:271)
  - `webApp.setMaxFormContentSize(maxFormContentSize)`

추가 확인 결과:

- `git show 2b28a664fe0d | git apply --reverse --check`는 실패
- 하지만 핵심 설정값과 적용 로직이 현재 `europa`에 모두 존재
- `git blame`상도 `request.content.size`와 `MAX_FORM_CONTENT_SIZE_KEY` 반영이 2024-01 시점에 이미 들어와 있음

이 항목은 이후 server bootstrap / Jetty 설정 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- 설정 키와 런타임 적용 지점을 함께 본다
- 설정 파일 한 줄뿐 아니라 bootstrap 코드 반영까지 확인한다
- 설정값과 적용 코드가 모두 있으면 `Already Satisfied`로 분류한다

### Record 009 - `e6f048bc2e83` `CKS: fix wrong format of cluster size on UI (#8182)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- `ListView.vue`의 `size` 표시에서 `/kubernetes` 경로는 값을 그대로 보여주고
- 그 외 경로만 GiB 단위로 변환해서 표시

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [ui/src/components/view/ListView.vue](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/components/view/ListView.vue:410)
  - `v-if="text && $route.path === '/kubernetes'"`
  - `v-else-if="text"`에서만 GiB 변환
  구조가 이미 존재

추가 확인 결과:

- `git show e6f048bc2e83 | git apply --reverse --check`는 실패
- 하지만 Apache 원본의 핵심 UI 조건 분기가 현재 `europa` 코드에 그대로 존재

이 항목은 이후 route별 UI 표시 예외 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- route 조건 분기와 실제 렌더링 표현식을 같이 본다
- 값 변환 여부가 경로별로 달라지는지 확인한다
- 핵심 조건 분기가 이미 있으면 `Already Satisfied`로 분류한다

### Record 010 - `127fd9d2f06e` `UI: Project column in Default View (#8287)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- default view에서 `listAllProjects`가 켜져 있으면 각 섹션 목록에 `project` column 추가
- `ListView.vue`에 `column.key === 'project'` 렌더링 추가
- `AutogenView.vue`에서 project toggle 시 선택 컬럼에 `project` 자동 포함

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [ui/src/components/view/ListView.vue](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/components/view/ListView.vue:754)
  - `column.key === 'project'` 렌더링 이미 존재
- [ui/src/views/AutogenView.vue](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/views/AutogenView.vue:1499)
  - `listAllProjects && !projectView`일 때 `selectedColumns.push('project')` 이미 존재
- 섹션 설정 파일들에도 이미 `project` column 조건이 들어가 있음
  - [ui/src/config/section/compute.js](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/config/section/compute.js:91)
  - [ui/src/config/section/event.js](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/config/section/event.js:28)
  - [ui/src/config/section/image.js](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/config/section/image.js:52)
  - [ui/src/config/section/network.js](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/config/section/network.js:43)
  - [ui/src/config/section/storage.js](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/config/section/storage.js:59)

추가 확인 결과:

- `git show 127fd9d2f06e | git apply --reverse --check`는 실패
- 하지만 원본이 건드린 핵심 3축인 `ListView`, `AutogenView`, section config들이 모두 현재 `europa`에 존재

이 항목은 이후 default view / project toggle 관련 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- 리스트 렌더링 컴포넌트와 컬럼 정의 파일을 함께 본다
- toggle 상태에 따라 선택 컬럼이 자동 보강되는지 확인한다
- 렌더링, 컬럼 정의, 선택 컬럼 로직이 모두 있으면 `Already Satisfied`로 분류한다

### Record 011 - `746bae740eaa` `ui: fix default domainid for add account (#8435)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- `AddAccount.vue`에서 domains 로딩 후 `this.form.domainid = 0`를 제거
- add account 화면의 기본 `domainid`가 잘못 `0`으로 덮어써지는 문제를 방지

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [ui/src/views/iam/AddAccount.vue](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/views/iam/AddAccount.vue:255)
  - 초기값을 `this.$route.query.domainid || this.$store.getters.userInfo.domainid`로 설정
  - 즉 Apache가 고치려던 `domainid = 0` 기본값 문제는 이미 존재하지 않음
- 현재 파일에는 `this.form.domainid = 0` 코드가 존재하지 않음

추가 확인 결과:

- `git show 746bae740eaa | git apply --reverse --check`는 실패
- 하지만 Apache 원본의 핵심 문제인 `domainid=0` 강제 초기화는 이미 제거된 상태
- 다만 [ui/src/views/iam/AddAccount.vue](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/views/iam/AddAccount.vue:361)의 `this.form.domainid = this.domainsList[0].id || ''`는 별도의 후속 UI 정리 후보로 볼 수 있음

이 항목은 이후 form 기본값 관련 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- 원본이 고치려는 “직접적인 잘못된 기본값”이 현재 코드에 남아 있는지 먼저 본다
- 현재 구현이 다른 방식의 기본값 초기화로 대체됐으면 `Already Satisfied`로 분류한다
- 다만 추가적인 로컬 후속 정리 후보는 별도 메모로 분리한다

### Record 012 - `5c32a0edbaa5` `ui: prevent scheduling readyforshutdown job when api inaccessible (#8448)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- `readyForShutdown` API 권한이 없는 계정에서는 polling job을 생성하지 않도록 `created()`에 가드 추가

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [ui/src/components/page/GlobalLayout.vue](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/components/page/GlobalLayout.vue:246)
  - `if ('readyForShutdown' in this.$store.getters.apis)` 조건이 이미 존재
- 같은 블록에서만 polling job 생성과 `SET_READY_FOR_SHUTDOWN_POLLING_JOB` commit을 수행

추가 확인 결과:

- `git show 5c32a0edbaa5 | git apply --reverse --check`는 실패
- 하지만 Apache 원본이 추가한 핵심 가드가 현재 `europa`에 그대로 존재

이 항목은 이후 UI polling / permission gate 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- polling job 생성 위치와 API 접근 가능 여부 가드를 함께 본다
- 권한이 없는 계정에서 job이 잡히지 않는지 확인한다
- 가드가 이미 있으면 `Already Satisfied`로 분류한다

### Record 013 - `b2e29931e898` `UI: fix icmp code/type of ACL rule are not display if the value is 0 (#8589)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- ACL rule 화면에서 `startport`, `endport`, `icmpcode`, `icmptype` 값이 `0`이어도 숨기지 않도록
- truthy 검사 대신 `!== undefined` 조건으로 렌더링

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [ui/src/views/network/AclRulesTab.vue](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/views/network/AclRulesTab.vue:82)
  - `v-if="element.startport !== undefined"`
  - `v-if="element.endport !== undefined"`
  - `v-if="element.icmpcode !== undefined"`
  - `v-if="element.icmptype !== undefined"`
  가 이미 존재
- 현재 브랜치에서는 Apache 원본 파일명 `AclListRulesTab.vue`가 [AclRulesTab.vue](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/views/network/AclRulesTab.vue:82) 로 정리된 상태

추가 확인 결과:

- 원본 patch는 파일 경로 차이 때문에 reverse-apply가 그대로 맞지 않았음
- 하지만 핵심 렌더링 조건식은 현재 `europa`에 이미 존재
- `git blame` 기준으로 해당 조건식은 별도 선행 커밋 `23f685d4c12`에서 먼저 반영되어 있음

이 항목은 이후 UI 경로 변경/파일 rename이 있는 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- reverse-apply 실패 원인이 기능 미반영인지 파일 rename인지 먼저 구분한다
- 화면 컴포넌트가 이동되었어도 핵심 조건식이 존재하면 `Already Satisfied`로 분류한다
- truthy 검사와 `!== undefined` 검사의 차이가 실제 `0` 표시 문제를 해결하는지까지 확인한다

### Record 014 - `19250403e645` `ui: fix create k8s cluster multiple listing (#8539)`

- 판정: `Already Satisfied`
- 일시: `2026-04-23`
- `europa` 커밋: 별도 체리픽 불필요

원본 Apache 커밋의 핵심 의도는 아래와 같다.

- `CreateKubernetesCluster.vue`에서 `networks`와 `keyPairs`의 empty entry 초기화를 공통화
- 초기 `fetchData()`에서 불필요한 `fetchNetworkData()` 중복 호출을 제거
- `fetchNetworkData()` 실행 전 `this.networks = []`로 초기화한 뒤, 완료 시 `[emptyEntry].concat(this.networks)` 형태로 목록을 재구성해 중복 표기를 방지

`europa` 현재 구현에서 확인한 선반영 근거는 아래와 같다.

- [ui/src/views/compute/CreateKubernetesCluster.vue](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/views/compute/CreateKubernetesCluster.vue:560)
  - `this.emptyEntry = { id: null, name: '' }`
  - `this.networks = [this.emptyEntry]`
  - `this.keyPairs = [this.emptyEntry]`
  로 이미 동일한 초기화 구조 사용
- [ui/src/views/compute/CreateKubernetesCluster.vue](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/views/compute/CreateKubernetesCluster.vue:614)
  - `fetchData()`는 `fetchZoneData()`만 먼저 호출하고, network는 zone change 이후 조건부로 가져오는 구조
  - 즉 Apache가 제거한 초기 중복 `fetchNetworkData()`가 현재 코드에는 없음
- [ui/src/views/compute/CreateKubernetesCluster.vue](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/ui/src/views/compute/CreateKubernetesCluster.vue:805)
  - `fetchNetworkData()`에서 `this.networks = []`로 먼저 초기화
  - 완료 시 `this.networks = [this.emptyEntry].concat(this.networks)`로 목록 재구성

추가 확인 결과:

- `git show 19250403e645 | git apply --reverse --check`는 중간 hunk가 맞지 않아 실패
- 하지만 실패 지점은 현재 `europa`가 owner/account/project와 admin role 분기를 포함한 확장형 구현이기 때문으로 판단
- Apache 원본이 해결하려던 “중복 listing” 방지 핵심 구조는 현재 코드에 이미 존재

이 항목은 이후 UI 목록 초기화/중복 로딩 관련 후보를 검토할 때 아래 기준의 대표 예시로 사용한다.

- `created()` 초기값, 초기 fetch 흐름, 실제 데이터 재할당 시점을 함께 본다
- `concat` 이전에 배열을 비우는지 여부가 중복 표시 방지의 핵심인지 확인한다
- 현재 구현이 owner/role 확장까지 포함한 상위 구조면 `Already Satisfied`로 분류한다

## 자동 판정 배치 - `Record 015` ~ `Record 033`

이번 배치의 나머지 후보 19건은 아래 기준으로 일괄 `Already Satisfied`로 분류했다.

- `ablestack-europa` 이력에 동일 subject 커밋이 직접 존재하는 경우
- 원본 patch가 건드린 핵심 파일과 기능이 현재 `europa` 코드에 그대로 존재하는 경우
- 대형 기능이라도 원본이 추가한 API/model/test 파일과 후속 수정 커밋이 함께 존재해 기능 묶음이 이미 흡수된 경우

| Record | SHA | Subject | 판정 근거 |
| --- | --- | --- | --- |
| `015` | `c43b7c04f4cf` | `ui: fix labels when migrating instances from vmware (#8490)` | 동일 subject 커밋 `e4c6fba566` 확인 |
| `016` | `3bcf6f0faf49` | `Rename "Import QCOW...." to "Import QCOW2....." (#8519)` | 동일 subject 커밋 `1107dd3394` 확인 |
| `017` | `db6dd52f443b` | `kvm: fix ide controller for rocky/alma vms (#8247)` | 동일 subject 커밋 `c2e75e5474` 확인 |
| `018` | `7ea068c4dcfa` | `kvm: fix error 'Failed to find passphrase for keystore: cloud.jks' when enable SSL for kvm agent (#7923)` | 동일 subject 커밋 `20d5ee32f5` 확인 |
| `019` | `267a457efc55` | `Externalize KVM HA heartbeat frequency (#6892)` | 동일 subject 커밋 `c52192d041` 확인 |
| `020` | `bba554bcc473` | `linstor: Fix possible NPE if Linstor storage-pool data missing (#8319)` | 동일 subject 커밋 `7b7b11f21c` 확인 |
| `021` | `68e504aff97d` | `Linstor backup snaphots (#8067)` | 동일 subject 커밋 `b7ba019c9e` 확인 |
| `022` | `b0910fc61d7b` | `Add dynamic secondary storage selection (#7659)` | 동일 subject 커밋 `70415f34ef` 확인 |
| `023` | `26b01f6f3be3` | `Flexible tags for hosts and storage pools (#7489)` | 동일 subject 커밋 `a53d53a4ef` 확인 |
| `024` | `60017723357c` | `multi local storage handling for kvm (#6699)` | 동일 subject 커밋 `0cc34fa1de` 확인 |
| `025` | `3bb318bab905` | `kvm: Add support for cgroupv2 (#8252)` | 동일 subject 커밋 `49dcde4975` 확인 |
| `026` | `5361b415e6af` | `Image Store: View Access status of the image store and view events (#8467)` | 동일 subject 커밋 `56fd791abf` 확인 |
| `027` | `371ad9f55b35` | `New Feature: Import VMware VMs into KVM (#7881)` | 동일 subject 커밋 `8d4645d8d4` 확인 |
| `028` | `ab20b1220fea` | `KVM Ingestion - Import Instance (#7976)` | 동일 subject 커밋 `d7c71013fa` 확인 |
| `029` | `5651eab49cf3` | `ObjectStore Framework with MinIO and Simulator plugins (#7752)` | 동일 subject 커밋 `f0218dd822` 확인 |
| `030` | `33bb92acce2d` | `Veeam: Support Veeam 11 and 12 (#8241)` | `VmRestorePoint`, `BackupFile`, `PrepareForBackupRestorationCommand`, 관련 테스트와 후속 fix `7d5a018e36` 확인 |
| `031` | `b34f09313738` | `veeam: fix some issues with restoring volume from backup and attaching it to VM (#8570)` | 동일 subject 커밋 `7d5a018e36` 확인 |
| `032` | `1f29f6f04096` | `Public IP quarantine feature (#7378)` | 동일 subject 커밋 `ece08ff703` 확인 |
| `033` | `ab70108f1573` | `CKS: create Security Groups for CKS clusters of each account (#8316)` | 동일 subject 커밋 `3ea73dcc72` 확인 |

이 배치에서 별도 체리픽이 필요한 항목은 없었다.

## 다음 추출 배치

최근 `main` 커밋 `300`건을 기준으로 아래 방식으로 후속 후보를 다시 추출했다.

1. `main` 최근 `300` non-merge 커밋 수집
2. `ablestack-europa`에 동일 subject가 있는 커밋 제외
3. docs/ci/meta 성격 커밋 제외

현재 관찰값은 아래와 같다.

- 최근 `main` `300`건 기준 raw 후보: `110`
- 그중 우선 검토 가치가 높은 non-doc/meta 후보: `65`

이 중에서 이전 `33`건과 겹치지 않고, 기능적으로 새 검토 가치가 높은 항목을 다음 배치로 선정한다.

### Batch E - 다음 우선 검토 배치

| Status | SHA | Subject | 선정 이유 |
| --- | --- | --- | --- |
| `Excluded` | `3166e64891fc` | `Add support for new variables to the GUI whitelabel runtime system (#12760)` | `europa`의 의도적 누락/변경 영역으로 체리픽 제외 |
| `Already Satisfied` | `83f705ddc588` | `Static Routes with nexthop non-functional for private gateways (#12859)` | reverse-apply 통과로 선반영 확인 |
| `Already Satisfied` | `05c59630e0ae` | `fix: LB Creation avoid 404 API errors due to non-needed patches (#12835)` | `NsxApiClient`에 pool/member 비교, monitor profile 보강, virtual server guard 로직 존재 |
| `Already Satisfied` | `160876c6d7d3` | `Fix: API Thread held forever during force deleting across MS (#12968)` | propagated delete의 forced flags와 peer 오류 전파 로직이 현재 코드에 존재 |
| `Already Satisfied` | `5013cf2af649` | `Fix user password reset mail template value  (#12882)` | `europa` 커밋 `251319d446`로 반영 이력 확인 |
| `Already Satisfied` | `ae455ee193ec` | `VPC restart cleanup for Public networks with multi-CIDR data (#12622)` | public network sanitize SQL과 `TrafficType.Public` guard가 현재 코드에 존재 |
| `Already Satisfied` | `6f1aa96b4cd5` | `engine/schema: fix new systemvm template is not registered during upgrade if hypervisor is not KVM (#12952)` | non-KVM hypervisor의 `amd64` arch 등록 로직이 현재 코드에 존재 |
| `Already Satisfied` | `e10c066cc143` | `Fix NPE during VM setup for pvlan (#12781)` | reverse-apply 통과로 선반영 확인 |
| `Already Satisfied` | `18075ae4a96b` | `Add support for Headlamp dashboard for kubernetes; deprecate legacy kubernetes dashboard (#12776)` | reverse-apply 통과 및 `europa` 커밋 `16fdb49f92` 확인 |
| `Already Satisfied` | `0edd577f4bb8` | `Fix: KVM Direct Download URL injection` | reverse-apply 통과로 선반영 확인 |
| `Already Satisfied` | `09ee0927e9bb` | `[4.22] Prevent Load Balancer rule creation when adding a VM from a different network (#12785)` | `verifyLoadBalancerRuleNetwork()`가 현재 코드에 존재 |
| `Already Satisfied` | `71daf84c9e89` | `Show security group selection in Basic zone VM deployment and fix SG listing for cross-domain deployments (#12775)` | reverse-apply 통과로 선반영 확인 |

이 배치는 다음 라운드에서 다시 아래 절차로 순차 판정한다.

1. Apache 원본 patch 검토
2. `git apply --reverse --check`
3. `europa` 현재 구현/테스트 대조
4. `Already Satisfied / Partially Satisfied / Ready / Excluded` 판정

### Batch E 판정 결과

- `Already Satisfied`: `11`
- `Excluded`: `1`
- `Ready`: `0`
- 실제 신규 반영 검토 필요: `0`

#### Batch E 상세 판정

| Record | SHA | Decision | 핵심 근거 |
| --- | --- | --- | --- |
| `034` | `3166e64891fc` | `Excluded` | `europa`의 의도적 누락/변경 영역으로 유지하기로 결정 |
| `035` | `83f705ddc588` | `Already Satisfied` | reverse-apply 통과 |
| `036` | `05c59630e0ae` | `Already Satisfied` | `NsxApiClient`에 `hasSamePoolMembers`, `getMonitorProfile`, `patchMonitoringProfile`, `lbVirtualServerName` guard 존재 |
| `037` | `160876c6d7d3` | `Already Satisfied` | `PropagateResourceEventCommand` forced flags, `executeUserRequest(..., forced, forceDeleteStorage)`, peer error propagation 존재 |
| `038` | `5013cf2af649` | `Already Satisfied` | `europa` 커밋 `251319d446` 확인 |
| `039` | `ae455ee193ec` | `Already Satisfied` | public network sanitize SQL 및 `TrafficType.Public` 예외 처리 존재 |
| `040` | `6f1aa96b4cd5` | `Already Satisfied` | `SystemVmTemplateRegistration.hypervisorList`가 non-KVM에 `CPU.CPUArch.amd64` 사용 |
| `041` | `e10c066cc143` | `Already Satisfied` | reverse-apply 통과 |
| `042` | `18075ae4a96b` | `Already Satisfied` | reverse-apply 통과 및 `europa` 커밋 `16fdb49f92` 확인 |
| `043` | `0edd577f4bb8` | `Already Satisfied` | reverse-apply 통과 |
| `044` | `09ee0927e9bb` | `Already Satisfied` | `LoadBalancingRulesManagerImpl.verifyLoadBalancerRuleNetwork()` 존재 |
| `045` | `71daf84c9e89` | `Already Satisfied` | reverse-apply 통과 |

#### Batch E에서 제외로 확정한 항목

- `3166e64891fc`
  - 현재 `europa`는 base GUI whitelabel runtime system은 보유
  - Apache follow-up이 추가한 새 runtime 변수와 validator refactor는 이번 브랜치에서 의도적으로 수용하지 않음
  - 따라서 누락이 아니라 branch-specific divergence로 간주하고 체리픽 대상에서 제외

### Batch F - 다음 실제 검토 배치

아래 배치는 최근 `main` 커밋 중에서 다음 기준으로 다시 추린 것이다.

1. `ablestack-europa`에 동일 subject가 직접 보이지 않는 항목
2. docs/ci/meta 성격을 제외한 기능 커밋
3. 대형 프레임워크 추가보다 기능 단위로 검토하기 쉬운 항목 우선

| Status | SHA | Subject | 선정 이유 |
| --- | --- | --- | --- |
| `Already Satisfied` | `38abe2df0bb9` | `Allow list async jobs by resource type alone (#13011)` | `europa` 커밋 `d8d95533d9` 및 reverse-apply 통과 |
| `Already Satisfied` | `47c5bb8ee7a7` | `Support list/query async jobs by resource (#12983)` | `europa` 커밋 `80db10ae73` 확인 |
| `Already Satisfied` | `273699cf5663` | `kvm: fix wrong CheckVirtualMachineAnswer when vm does not exist (#12928)` | `europa` 커밋 `1e46b092e6` 및 reverse-apply 통과 |
| `Already Satisfied` | `470812100ea6` | `server: set template type to ROUTING or USER if template type is not specified when upload a template (#12768)` | `TemplateManagerImpl.validateTemplateType()`와 NULL type 보정 SQL이 현재 코드에 존재 |
| `Already Satisfied` | `4ba4bd33c3cf` | `replace GROUP_CONCAT with JSON_ARRAYAGG to avoid errors like Row 19 was cut by GROUP_CONCAT (#12777)` | schema에 `JSON_ARRAYAGG`가 이미 존재하고 reverse-apply 통과 |
| `Already Satisfied` | `03de62bf3890` | `Support Linstor Primary Storage for NAS BnR (#12796)` | `RestoreBackupCommand.restoreVolumeSizes`, `NASBackupProvider`의 Linstor path/suffix, KVM wrapper와 `nasbackup.sh` 지원이 현재 코드에 존재 |
| `Already Satisfied` | `24fd440ee728` | `Fix create VM from backup` | `CreateVMFromBackupCmd.backupId`에 `@ACL`이 이미 존재 |
| `Already Satisfied` | `2416db2a4439` | `Fix NPE on external/unmanaged instance import using custom offerings (#12884)` | `checkVmResourceLimitsForUnmanagedInstanceImport()`와 관련 테스트가 현재 코드에 존재 |
| `Already Satisfied` | `131ea9f7aceb` | `Fix PowerFlex 4.x issues with take & revert instance snapshots (#12880)` | `ScaleIOVMSnapshotStrategy`의 snapshot-name 매핑과 `ScaleIOGatewayClientImpl`의 PowerFlex 4.x overwrite body 분기 존재 |
| `Already Satisfied` | `1ff9eec9977f` | `Load arch data for backup from template during create instance from backup (#12801)` | `CreateVMFromBackup.vue`의 `fetchBackupArch()`/`backupArch`와 `DeployVMFromBackup.vue`의 architecture preload가 현재 코드에 존재 |
| `Already Satisfied` | `27e4d979f121` | `Clean up backup references to their schedules when the schedules are deleted (#12401)` | `BackupScheduleDaoImpl.remove()`의 `backup_schedule_id = NULL` 처리와 `backup_interval_type` drop SQL 존재 |
| `Already Satisfied` | `416679fae138` | `Fix domain parsing for GPU & add Display controller in the supported PCI class (#12981)` | `gpudiscovery.sh`의 `Display controller` 지원과 `LibvirtGpuDef`의 full/short PCI domain parsing 보정 존재 |

이 배치는 아래 절차로 판정을 완료했다.

1. Apache 원본 patch 검토
2. `git apply --reverse --check`
3. `europa` 현재 구현/테스트 대조
4. `Already Satisfied / Partially Satisfied / Ready / Excluded` 판정

### Batch F 판정 결과

- `Already Satisfied`: `12`
- `Partially Satisfied`: `0`
- `Ready`: `0`
- 실제 신규 반영 검토 필요: `0`

#### Batch F 상세 판정

| Record | SHA | Decision | 핵심 근거 |
| --- | --- | --- | --- |
| `046` | `38abe2df0bb9` | `Already Satisfied` | `europa` 커밋 `d8d95533d9` 및 reverse-apply 통과 |
| `047` | `47c5bb8ee7a7` | `Already Satisfied` | `europa` 커밋 `80db10ae73` 확인 |
| `048` | `273699cf5663` | `Already Satisfied` | `europa` 커밋 `1e46b092e6` 및 reverse-apply 통과 |
| `049` | `470812100ea6` | `Already Satisfied` | `TemplateManagerImpl.validateTemplateType()`와 `schema-42200to42210.sql`의 NULL type -> `USER` 보정 SQL 존재 |
| `050` | `4ba4bd33c3cf` | `Already Satisfied` | `schema-42010to42100.sql`에 `JSON_ARRAYAGG`가 이미 존재하고 reverse-apply 통과 |
| `051` | `03de62bf3890` | `Already Satisfied` | `RestoreBackupCommand.restoreVolumeSizes`, `NASBackupProvider`의 Linstor path/suffix, `LibvirtRestoreBackupCommandWrapper`와 `nasbackup.sh` Linstor 지원 존재 |
| `052` | `24fd440ee728` | `Already Satisfied` | `CreateVMFromBackupCmd.backupId`에 `@ACL` 존재 |
| `053` | `2416db2a4439` | `Already Satisfied` | `checkVmResourceLimitsForUnmanagedInstanceImport()`, `getDetailAsInteger()` 및 관련 테스트 존재 |
| `054` | `131ea9f7aceb` | `Already Satisfied` | `ScaleIOVMSnapshotStrategy`의 snapshot name 기반 매핑과 `ScaleIOGatewayClientImpl`의 PowerFlex 4.x overwrite body 분기 존재 |
| `055` | `1ff9eec9977f` | `Already Satisfied` | `CreateVMFromBackup.vue.fetchBackupArch()`와 `DeployVMFromBackup.vue`의 `selectedArchitecture` preload 존재 |
| `056` | `27e4d979f121` | `Already Satisfied` | `BackupScheduleDaoImpl.remove()`의 `backup_schedule_id = NULL` 처리와 `backup_interval_type` drop SQL 존재 |
| `057` | `416679fae138` | `Already Satisfied` | `gpudiscovery.sh`의 `Display controller` 처리와 `LibvirtGpuDef`의 short/full PCI domain parsing 보정 존재 |

## 처리 기록 템플릿

후보를 실제로 처리할 때는 아래 형식으로 기록한다.

| Date | SHA | Decision | Europa Commit | 영향도/충돌 요약 |
| --- | --- | --- | --- | --- |
| `YYYY-MM-DD` | `<apache sha>` | `Cherry-picked / Already Satisfied / Deferred / Excluded` | `<europa sha or ->` | `<핵심 판단>` |

## 2026-04-23 기준점

이 문서의 현재 목적은 “누락 커밋을 지금 당장 전부 반영한다”가 아니라, `2026-04-23` 시점의 `main -> ablestack-europa` 기준점을 고정하는 것이다.

즉 아래 의미로 사용한다.

- `2026-04-23`를 baseline date로 본다.
- 이 날짜를 기준으로 `europa`가 `main`의 변경을 사실상 수용했다고 선언할 수 있도록, 당시 기준 후보군을 고정한다.
- 이 후보군이 모두 `Already Satisfied / Excluded / Cherry-picked / Deferred`로 닫히면,
  이후 iteration에서는 `2026-04-23` 이후 `main`에 새로 들어온 커밋만 검토하면 된다.

사용자 확인 결과, `ablestack-europa`는 적어도 `2025`년 커밋 기준까지는 선반영 또는 다른 형태의 기능 반영이 충분히 진행된 상태로 본다.
따라서 baseline candidate pool은 `2026-01-01` 이후 `main` 커밋으로 한정한다.

`2026-04-23` 기준 관찰값은 아래와 같다.

- `main`의 `2026`년 일반(non-merge) 커밋: `355`건
- 이 중 `git cherry -v ablestack-europa main`의 `+`와 교집합인 `2026 baseline candidate pool`: `237`건

여기서 `237`건은 “반드시 미반영된 기능 237개”가 아니라,
`2026-04-23` 시점에 `europa`에서 아직 patch-equivalent로는 식별되지 않는 `main`의 2026년 후보군 전체를 뜻한다.

이 `237`건의 inventory snapshot은 아래 파일에 별도로 고정해 둔다.

- [main-to-europa-2026-only-inventory-2026-04-23.txt](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/developer/history/main-to-europa-2026-only-inventory-2026-04-23.txt)

운영 원칙은 다음과 같다.

1. 위 inventory 파일을 `2026-04-23 baseline candidate pool`의 고정 스냅샷으로 취급한다.
2. 이미 이 문서에서 `Already Satisfied` 또는 `Excluded`로 닫힌 SHA는 baseline 소거 대상으로 본다.
3. baseline pool이 모두 닫히면, 차기 sync iteration부터는 `2026-04-23` 이후 `main`에 추가된 커밋만 보면 된다.
4. 차후 기준일이 새로 필요해지면 같은 방식으로 새 snapshot inventory를 별도 파일로 남긴다.

## 2026 baseline pool 1차 자동 분류

`2026 baseline candidate pool` `237`건 전체를 대상으로 아래 기준의 1차 자동 분류를 수행했다.

1. 이 문서에 이미 기록된 SHA 또는 축약 SHA가 있는지 확인
2. `ablestack-europa` 이력에 동일 subject가 있는지 확인
3. `git show <sha> | git apply --reverse --check` 통과 여부 확인
4. 위 조건에 걸리지 않는 항목만 `needs_source_review`로 남김

산출물은 아래 TSV 파일로 고정해 둔다.

- [main-to-europa-2026-baseline-pretriage-2026-04-23.tsv](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/developer/history/main-to-europa-2026-baseline-pretriage-2026-04-23.tsv)

`2026-04-23` 기준 1차 자동 분류 결과는 아래와 같다.

- `documented`: `24`
- `exact_subject_hit`: `107`
- `reverse_apply_pass`: `35`
- `needs_source_review`: `71`

해석은 다음과 같다.

- `documented`
  - 이미 이 문서에서 `Already Satisfied / Excluded` 등으로 판정한 항목
- `exact_subject_hit`
  - `europa`에 동일 subject 커밋이 존재해 선반영 가능성이 매우 높은 항목
- `reverse_apply_pass`
  - patch-equivalent로 흡수됐을 가능성이 높은 항목
- `needs_source_review`
  - 위 세 조건으로는 자동으로 닫히지 않아 실제 소스 대조가 필요한 항목

즉 baseline pool `237`건 전체를 한 번에 사람이 다시 읽는 대신, 우선 실제 수동 판정이 필요한 잔여 집합은 `71`건으로 줄여서 진행한다.

### Batch G - 2026 baseline 잔여 후보 1차 수동 판정 배치

아래 항목은 `needs_source_review` `71`건 중에서 docs/CI/version bump류를 제외하고,
기능 영향도와 차기 체리픽 가능성을 기준으로 먼저 보는 배치다.

| Status | SHA | Subject | 선정 이유 |
| --- | --- | --- | --- |
| `Already Satisfied` | `6e810989b638` | `HAProxy Configuration: network.loadbalancer.haproxy.idle.timeout (#12586)` | 기존 sync 문서와 현재 `network.loadbalancer.haproxy.idle.timeout` config key 존재로 선반영 확인 |
| `Already Satisfied` | `d75acb6efcc2` | `Fix rollback disk snapshots on instance snapshot failure (#12949)` | 기존 sync 문서에 반영 이력 존재 |
| `Already Satisfied` | `ed575cc0a107` | `New config.json variable to set the ACS default language (#12863)` | 기존 sync 문서와 현재 default language runtime config 반영 이력 확인 |
| `Already Satisfied` | `19b4ef106931` | `server: reserve backup, bucket resource limits during operations` | 기존 sync 문서 및 동일 subject 반영 이력 확인 |
| `Already Satisfied` | `9f57a4dd19f1` | `Unhide setting \`js.interpretation.enabled\` (#12605)` | 기존 sync 문서와 현재 helper/config 반영 이력 확인 |
| `Already Satisfied` | `7c7b2ae75d1b` | `Fix KVM incremental volume snapshot creation (#12666)` | 기존 sync 문서에 반영 이력 존재 |
| `Already Satisfied` | `b196e97cc36c` | `Prevent deletion of account and domain if either of them has deleted protected instance (#12901)` | 기존 sync 문서와 DAO/service 보호 로직 반영 이력 확인 |
| `Already Satisfied` | `df7ff9727192` | `Create volume on a specified storage pool (#12966)` | 기존 sync 문서와 현재 volume-on-storage-pool 지원 확인 |
| `Already Satisfied` | `68bd05630614` | `Support timeout configuration for Create and Restore NAS backup (#12964)` | 기존 sync 문서와 NAS backup timeout config 반영 이력 확인 |
| `Already Satisfied` | `b0b3dc91f536` | `fix: support SharedMountPoint volume checks for importVm (#12946)` | 기존 sync 문서와 SharedMountPoint volume check 반영 이력 확인 |
| `Already Satisfied` | `b5858029bb51` | `Fix listing service offerings with different host tags (#12919)` | 기존 sync 문서와 host tags listing 반영 이력 확인 |
| `Already Satisfied` | `7ba5240b311f` | `Block backup deletion while create-VM-from-backup or restore jobs are in progress (#12792)` | `BackupManagerImpl`의 in-progress delete guard와 기존 sync 문서 확인 |
| `Already Satisfied` | `68030df10b1f` | `VM start error handling improvements and config to expose error to users (#12894)` | `EXPOSE_ERRORS_TO_USER` config와 `canExposeError()` 흐름이 현재 코드에 존재 |
| `Already Satisfied` | `6ca6aa1c3f01` | `Fix NPE in NASBackupProvider when no running KVM host is available (#12805)` | `NASBackupProvider`의 no-host null guard가 현재 코드에 존재 |
| `Already Satisfied` | `b22dbbe2d7ad` | `Fix Revert Instance to Snapshot with custom service offering (#12885)` | `VMSnapshotManagerImpl`의 custom service offering revert 로직이 현재 코드에 존재 |

### Batch G 판정 결과

- `Already Satisfied`: `15`
- `Partially Satisfied`: `0`
- `Ready`: `0`
- 실제 신규 반영 검토 필요: `0`

#### Batch G 상세 판정

| Record | SHA | Decision | 핵심 근거 |
| --- | --- | --- | --- |
| `058` | `6e810989b638` | `Already Satisfied` | 기존 sync 문서와 `NetworkOrchestrationService`의 `network.loadbalancer.haproxy.idle.timeout` config key 존재 |
| `059` | `d75acb6efcc2` | `Already Satisfied` | 기존 sync 문서에 snapshot rollback failure 보강 반영 이력 존재 |
| `060` | `ed575cc0a107` | `Already Satisfied` | 기존 sync 문서와 default language runtime config 반영 이력 존재 |
| `061` | `19b4ef106931` | `Already Satisfied` | 기존 sync 문서 및 backup/bucket resource reservation 흐름 반영 이력 확인 |
| `062` | `9f57a4dd19f1` | `Already Satisfied` | 기존 sync 문서와 `js.interpretation.enabled` helper/config 반영 이력 확인 |
| `063` | `7c7b2ae75d1b` | `Already Satisfied` | 기존 sync 문서에 KVM incremental volume snapshot creation 보강 반영 이력 존재 |
| `064` | `b196e97cc36c` | `Already Satisfied` | 기존 sync 문서와 delete-protected instance guard 반영 이력 존재 |
| `065` | `df7ff9727192` | `Already Satisfied` | 기존 sync 문서와 현재 create-volume-on-storage-pool 지원 확인 |
| `066` | `68bd05630614` | `Already Satisfied` | 기존 sync 문서와 현재 NAS backup create/restore timeout config 존재 |
| `067` | `b0b3dc91f536` | `Already Satisfied` | 기존 sync 문서와 SharedMountPoint volume check 반영 이력 존재 |
| `068` | `b5858029bb51` | `Already Satisfied` | 기존 sync 문서와 host tags listing/service offering 반영 이력 존재 |
| `069` | `7ba5240b311f` | `Already Satisfied` | `BackupManagerImpl`가 create-from-backup / restore 진행 중 backup delete를 차단 |
| `070` | `68030df10b1f` | `Already Satisfied` | `ConfigurationManagerImpl.EXPOSE_ERRORS_TO_USER`와 `VirtualMachineManagerImpl.canExposeError()` 존재 |
| `071` | `6ca6aa1c3f01` | `Already Satisfied` | `NASBackupProvider`의 no running KVM host 예외/return guard가 현재 코드에 존재 |
| `072` | `b22dbbe2d7ad` | `Already Satisfied` | `VMSnapshotManagerImpl`의 custom service offering revert helper와 revert 경로 존재 |

### Batch H - 2026 baseline 잔여 후보 2차 수동 판정 배치

아래 항목은 `Batch G` 이후 남은 `needs_source_review` 후보 중, 메타/버전 bump가 아닌 실제 기능성 변경 위주로 다시 묶은 배치다.

| Status | SHA | Subject | 선정 이유 |
| --- | --- | --- | --- |
| `Already Satisfied` | `c6936889f5cb` | `server: prevent adding vm compute details when not applicable (#12637)` | `UserVmManagerImpl`와 관련 테스트에 CPU/Memory/CPUSpeed details 가드와 후속 테스트가 현재 코드에 존재 |
| `Already Satisfied` | `80ee7f183f7a` | `Fix six package incompatiblity with EL10 (#12799)` | `europa`는 `packaging/centos7/cloud.spec` 경로로 적응되어 `python3-six`, `python3-protobuf`, `mysql_connector_python` wheel 처리까지 존재 |
| `Already Satisfied` | `d38c1f8d1250` | `Fix error message while creating local storage pool (#12767)` | `StorageManagerImpl.isLocalStorageEnabledForZone()`와 zone-level local storage 비활성 메시지가 현재 코드에 존재 |
| `Already Satisfied` | `84676afd5cc7` | `Check for null host before proceeding with VM volume operations in managed storage while restoring VM (#12879)` | `UserVmManagerImpl`의 managed storage restore 경로에 null host 가드가 현재 코드에 존재 |
| `Already Satisfied` | `4b7370a6017a` | `upgrade: skip the upgrade paths which are not needed (#12881)` | `DatabaseVersionHierarchy`의 filtered path 계산과 `schema-42000to42010.sql`의 `INSERT IGNORE`가 현재 코드에 존재 |
| `Already Satisfied` | `d6c39772b217` | `Set management server id from cookies after saml login (#12858)` | `SAMLUtils`가 `managementserverid` cookie를 설정하고 `ui/src/permission.js`가 이를 `SET_MS_ID`에 반영 |
| `Already Satisfied` | `bce55945ece8` | `Mark VMs in error state when expunge fails during destroy operation (#12749)` | `UserVmManagerImpl.transitionExpungingToError()`와 관련 테스트가 현재 코드에 존재 |
| `Already Satisfied` | `e8d57d1b0dc1` | `Implement/fix limit validation for secondary storage` | `ImageStoreUploadMonitorImpl`, `TemplateManagerImpl`, `HypervisorTemplateAdapter`에 secondary storage reservation/check 흐름이 존재 |
| `Already Satisfied` | `9db630932e0c` | `Address public IP limit validations` | `CheckedReservation`, `ApiDBUtils.getSystemAccount()`, DAO `setParametersIfNotNull()`와 public IP reservation 흐름이 현재 코드에 존재 |
| `Already Satisfied` | `e93ae1a4f455` | `New config key "allow.import.volume.with.backing.file" to skip volume backing (#12809)` | `AllowImportVolumeWithBackingFile` config와 volume backing file guard가 현재 코드에 존재 |
| `Already Satisfied` | `2359061f663a` | `api: remove required flag of gatewayid in CreateStaticRouteCmd (#12786)` | `CreateStaticRouteCmd.gatewayId`에서 `required = true`가 이미 제거된 상태 |
| `Already Satisfied` | `56dc11980f60` | `test_accounts.py failure fix - keep the camelCase parameter "domainId" (#12689)` | `ApiServerService.getDomainId()`, `ApiServer`, OAuth login authenticator 경로가 camelCase `domainId`를 유지 |

### Batch H 판정 결과

- `Already Satisfied`: `12`
- `Partially Satisfied`: `0`
- `Ready`: `0`
- 실제 신규 반영 검토 필요: `0`

#### Batch H 상세 판정

| Record | SHA | Decision | 핵심 근거 |
| --- | --- | --- | --- |
| `073` | `c6936889f5cb` | `Already Satisfied` | `UserVmManagerImpl`의 CPU/Memory/CPUSpeed detail 가드와 `UserVmManagerImplTest`/`KVMGuruTest` 보강이 현재 코드에 존재 |
| `074` | `80ee7f183f7a` | `Already Satisfied` | `packaging/centos7/cloud.spec`에 `python3-six`, `python3-protobuf`, Python 버전별 `mysql_connector_python` wheel 처리 존재 |
| `075` | `d38c1f8d1250` | `Already Satisfied` | `StorageManagerImpl.isLocalStorageEnabledForZone()`와 `Local storage is not enabled for zone` 예외가 현재 코드에 존재 |
| `076` | `84676afd5cc7` | `Already Satisfied` | `UserVmManagerImpl`의 managed storage restore 경로에 `host == null` 방어 로직이 존재 |
| `077` | `4b7370a6017a` | `Already Satisfied` | `DatabaseVersionHierarchy.getPath()`의 filtered upgrade path 계산과 `schema-42000to42010.sql`의 `INSERT IGNORE` 존재 |
| `078` | `d6c39772b217` | `Already Satisfied` | `SAMLUtils`가 `managementserverid` cookie를 설정하고 `permission.js`가 이를 `SET_MS_ID`로 읽음 |
| `079` | `bce55945ece8` | `Already Satisfied` | `UserVmManagerImpl.transitionExpungingToError()`와 관련 unit tests 존재 |
| `080` | `e8d57d1b0dc1` | `Already Satisfied` | `ImageStoreUploadMonitorImpl`, `TemplateManagerImpl`, `HypervisorTemplateAdapter`에 secondary storage reservation/check 흐름 존재 |
| `081` | `9db630932e0c` | `Already Satisfied` | `ConfigurationManagerImpl`, `NetworkServiceImpl`, `CheckedReservation`, DAO null-safe lookup 보강이 현재 코드에 존재 |
| `082` | `e93ae1a4f455` | `Already Satisfied` | `AllowImportVolumeWithBackingFile` config와 `VolumeImportUnmanageManagerImpl`/`UnmanagedVMsManagerImpl` backing file guard 존재 |
| `083` | `2359061f663a` | `Already Satisfied` | `CreateStaticRouteCmd.gatewayId`가 현재 optional 상태이며 `required = true`가 없음 |
| `084` | `56dc11980f60` | `Already Satisfied` | `ApiServerService.getDomainId()`, `ApiServer`, OAuth login authenticator의 camelCase `domainId` 처리 유지 |

### Batch I - 2026 baseline 잔여 후보 3차 수동 판정 배치

아래 항목은 `Batch H` 이후 남은 잔여 후보 중, 기존 sync 문서에 이미 흔적이 있거나 현재 코드 시그널이 분명한 기능성 변경 위주로 다시 묶은 배치다.

| Status | SHA | Subject | 선정 이유 |
| --- | --- | --- | --- |
| `Already Satisfied` | `feb60769305e` | `Remove unused config consoleproxy.cmd.port (#12807)` | 기존 sync 문서와 현재 `ConsoleProxyManagerImpl.getConfigKeys()`/`schema-42200to42210.sql` 상태로 선반영 확인 |
| `Already Satisfied` | `8ce1c9876eea` | `fix restore volume from backup and attach` | 기존 sync 문서에 `RestoreVolumeFromBackupAndAttachToVMCmd` ACL 반영 이력 존재 |
| `Already Satisfied` | `7aa0558c5b91` | `ui: avoid 404 after deleting template zones (#12681)` | 기존 sync 문서에 이미 반영된 UI redirect source change로 기록됨 |
| `Already Satisfied` | `95816b44e933` | `extensions: allow reserved resource details` | 기존 sync 문서와 현재 reserved resource details 확장 이력 확인 |
| `Cherry-picked` | `9dd93cef7605` | `Support for custom SSH port for KVM hosts from the host url on add host and the configuration (#12571)` | `europa`에 host-specific SSH port handling을 반영 완료 |
| `Already Satisfied` | `4bcd509193fc` | `Fix resource limit reservation and check during StartVirtualMachine` | 현재 `UserVmManagerImpl`의 `CheckedReservation` 기반 start VM reservation 흐름 존재 |
| `Already Satisfied` | `e0ef3a694723` | `Check resource reservation on volume snapshot creation` | 현재 `SnapshotManagerImpl`의 snapshot/storage reservation 흐름 존재 |
| `Already Satisfied` | `b025e85fc57b` | `Check resource reservation on volume creation` | 현재 `VolumeApiServiceImpl`의 volume/primary storage reservation 흐름 존재 |
| `Already Satisfied` | `81a8ac8e1ffa` | `secondary storage resource limit for upload` | 기존 sync 문서와 현재 `ImageStoreUploadMonitorImpl`/`TemplateManagerImpl` secondary storage reservation 존재 |
| `Already Satisfied` | `03dfe4d1f3e1` | `secondary storage resource limit for download` | 기존 sync 문서와 현재 `DownloadListener`/`SnapshotManagerImpl` secondary storage reservation 존재 |
| `Already Satisfied` | `d11d182c7155` | `[22.0] Fix resource limit reservation and check during StartVirtualMachine` | 현재 코드가 원본 main 흐름보다 앞선 reservation 구조를 이미 포함 |
| `Already Satisfied` | `3d678e726ad3` | `[22.0] resource reservation on volume snapshot creation` | 현재 코드가 원본 main snapshot reservation 흐름을 이미 포함 |

### Batch I 판정 결과

- `Already Satisfied`: `11`
- `Partially Satisfied`: `0`
- `Ready`: `0`
- 실제 신규 반영 검토 필요: `0`

#### Batch I 상세 판정

| Record | SHA | Decision | 핵심 근거 |
| --- | --- | --- | --- |
| `085` | `feb60769305e` | `Already Satisfied` | `ConsoleProxyManagerImpl.getConfigKeys()`에서 제거돼 있고 `schema-42200to42210.sql`에서 `consoleproxy.cmd.port` 삭제 SQL 존재 |
| `086` | `8ce1c9876eea` | `Already Satisfied` | 기존 sync 문서 `기록 008`에서 `RestoreVolumeFromBackupAndAttachToVMCmd` ACL 반영 이력과 `europa` 체리픽 SHA가 이미 기록됨 |
| `087` | `7aa0558c5b91` | `Already Satisfied` | 기존 sync 문서 `기록 102`에서 이미 반영된 template-zone delete redirect source change로 정리됨 |
| `088` | `95816b44e933` | `Already Satisfied` | 기존 sync 문서 `기록 022` 근처의 extension reserved resource details 반영 이력과 `europa` 체리픽 SHA 존재 |
| `089` | `9dd93cef7605` | `Cherry-picked` | `AddHostCmd`, `Host`, `AgentManager`, `AgentManagerImpl`, `LibvirtServerDiscoverer`, `ResourceManagerImpl`, `NetworkerBackupProvider`, `SSHCmdHelper`까지 host-specific SSH port 지원 반영 완료 |
| `090` | `4bcd509193fc` | `Already Satisfied` | `UserVmManagerImpl`의 `startVirtualMachineUnchecked()` 앞단에 `CheckedReservation` 기반 VM/CPU/MEM/GPU reservation 존재 |
| `091` | `e0ef3a694723` | `Already Satisfied` | `SnapshotManagerImpl`의 `CheckedReservation` 기반 snapshot/storage reservation 존재 |
| `092` | `b025e85fc57b` | `Already Satisfied` | `VolumeApiServiceImpl`의 `CheckedReservation` 기반 volume/primary storage reservation 존재 |
| `093` | `81a8ac8e1ffa` | `Already Satisfied` | 기존 sync 문서와 `ImageStoreUploadMonitorImpl`/`TemplateManagerImpl`의 secondary storage reservation 존재 |
| `094` | `03dfe4d1f3e1` | `Already Satisfied` | 기존 sync 문서와 `DownloadListener`/`SnapshotManagerImpl`의 secondary storage reservation 존재 |
| `095` | `d11d182c7155` | `Already Satisfied` | `[22.0]` backport 대상 기능이 현재 `UserVmManagerImpl` reservation 구조에 이미 포함 |
| `096` | `3d678e726ad3` | `Already Satisfied` | `[22.0]` backport 대상 기능이 현재 `SnapshotManagerImpl` reservation 구조에 이미 포함 |

### Batch J - 2026 baseline 잔여 후보 4차 수동 판정 배치

아래 항목은 `Batch I` 이후 남은 잔여 후보 전체다. 성격상 다음 세 부류로 나뉜다.

- 메타/릴리즈/CI/저자 정보/리뷰 코멘트 커밋
- 이미 닫힌 기능의 review follow-up 또는 merge-fix 커밋
- 이미 반영된 mainline 기능의 `[20.3]` / `[22.0]` backport 파생 커밋

이 배치에서는 위 성격을 기준으로 모두 `Excluded`로 닫는다. 즉, `europa` baseline 기준의 별도 기능 반영 대상으로는 보지 않는다.

| Status | SHA | Subject | 선정 이유 |
| --- | --- | --- | --- |
| `Excluded` | `f820d0125de3` | `fix end of files and codespell errors` | 메타 정리 커밋 |
| `Excluded` | `13842a626d7e` | `Address reviews` | 이미 닫힌 resource-limit 계열의 review follow-up |
| `Excluded` | `8eb162cb996d` | `Updating pom.xml version numbers for release 4.20.4.0-SNAPSHOT` | 릴리즈/version bump |
| `Excluded` | `d6f4fc3ac402` | `Updating pom.xml version numbers for release 22.0.1` | 릴리즈/version bump |
| `Excluded` | `c8599040b475` | `Updating pom.xml version numbers for release 4.20.3.0` | 릴리즈/version bump |
| `Excluded` | `6bcbb008b45a` | `Bump \`actions/checkout\` to \`v6\` (#12164)` | CI/meta 커밋 |
| `Excluded` | `f3331344566b` | `Address merge issues` | 이미 반영된 기능의 merge-fix 성격 |
| `Excluded` | `9c0c8da706ea` | `[22.0] Address limit checks for VM, CPU, memory, volume, and primary storage` | mainline resource-limit 기능의 backport 파생 커밋 |
| `Excluded` | `61afb4cb782a` | `fix identation` | 메타 정리 커밋 |
| `Excluded` | `23b19a9776de` | `review comments` | review-only follow-up |
| `Excluded` | `dc7068a13517` | `Address public IP limit validations` | `9db630932e0c`로 닫힌 기능의 earlier/review variant |
| `Excluded` | `0a4b4c6af05c` | `[20.3] Address limit checks for VM, CPU, memory, volume, and primary storage` | mainline resource-limit 기능의 backport 파생 커밋 |
| `Excluded` | `06ee2fea76d1` | `Implement/fix limit validation for secondary storage` | `e8d57d1b0dc1`로 닫힌 기능의 earlier variant |
| `Excluded` | `7faa1b650b92` | `[20.3] resource allocation vpc` | 이미 반영된 resource allocation 흐름의 backport 파생 커밋 |
| `Excluded` | `1593944553fb` | `[20.3] Implement/fix limit validation for projects` | 이미 반영된 project limit 기능의 backport 파생 커밋 |
| `Excluded` | `4dd91feb277a` | `[20.3] resource instance limits` | 이미 반영된 instance limits 기능의 backport 파생 커밋 |
| `Excluded` | `89df31816480` | `[20.3] resource allocation` | 이미 반영된 resource allocation 기능의 backport 파생 커밋 |
| `Excluded` | `07c3dc86b2df` | `[22.0] Consider infinite resources when calculating secondary storage limit for upload operations` | 이미 반영된 secondary storage handling의 backport 파생 커밋 |
| `Excluded` | `3b42fbf3b246` | `Fixing CI failures (#12789)` | CI/fixup 커밋 |
| `Excluded` | `9bbd32a8ef03` | `Add DaanHoogland to the list of contributors` | 메타/저자 정보 커밋 |
| `Excluded` | `58916eb60803` | `Use lateral join (introduced in MySQL 8.0.14) with subquery on user_statistics table in account_view for netstats (#12631)` | upstream 최종 상태가 revert된 흐름이며 `europa`는 최종 상태를 유지 |
| `Excluded` | `831ef82ff9b6` | `[22.0] resource allocation vpc` | 이미 반영된 resource allocation 흐름의 backport 파생 커밋 |
| `Excluded` | `8d269cf5bef1` | `[22.0] Implement/fix limit validation for projects` | 이미 반영된 project limit 기능의 backport 파생 커밋 |
| `Excluded` | `003c8408179b` | `[22.0] resource instance limits` | 이미 반영된 instance limits 기능의 backport 파생 커밋 |
| `Excluded` | `37e365777074` | `[22.0] resource allocation` | 이미 반영된 resource allocation 기능의 backport 파생 커밋 |
| `Excluded` | `a566af35f5c8` | `Review comment on pull request #12436` | review-only follow-up |
| `Excluded` | `f1f779a08d94` | `Cleanupe8d57d1b0dc173ef55c6386f91f1d77b8b8c9830 2026-03-17 Implement/fix limit validation for secondary storage` | cleanup/fixup 성격의 파생 커밋 |

### Batch J 판정 결과

- `Excluded`: `27`
- `Already Satisfied`: `0`
- `Partially Satisfied`: `0`
- `Ready`: `0`
- 실제 신규 반영 검토 필요: `0`

## 기준점 마감 선언

`2026-04-23` 기준으로 정의한 `main -> ablestack-europa` baseline candidate pool은 모두 처리 완료됐다.

- 실제 신규 반영이 필요했던 항목은 `9dd93cef7605` `1건`이며, `europa`에 반영 완료
- 나머지 후보는 `Already Satisfied` 또는 `Excluded`로 모두 닫힘
- 따라서 운영 기준상 `2026-04-23` 시점의 `main` 변경은 `europa`에 반영된 상태로 본다

이후 iteration에서는 이 문서와 inventory 파일을 기준점 기록으로 유지하고, `2026-04-23` 이후 `main`에 새로 추가된 커밋만 신규 반영 대상으로 검토한다.

#### Batch J 상세 판정

| Record | SHA | Decision | 핵심 근거 |
| --- | --- | --- | --- |
| `097` | `f820d0125de3` | `Excluded` | codespell/EOF 정리 성격의 메타 커밋 |
| `098` | `13842a626d7e` | `Excluded` | 이미 닫힌 resource-limit 기능의 review follow-up |
| `099` | `8eb162cb996d` | `Excluded` | 릴리즈 version bump |
| `100` | `d6f4fc3ac402` | `Excluded` | 릴리즈 version bump |
| `101` | `c8599040b475` | `Excluded` | 릴리즈 version bump |
| `102` | `6bcbb008b45a` | `Excluded` | CI/meta 갱신 |
| `103` | `f3331344566b` | `Excluded` | 이미 반영된 기능의 merge-fix 성격 |
| `104` | `9c0c8da706ea` | `Excluded` | mainline 기능의 `[22.0]` backport 파생 커밋 |
| `105` | `61afb4cb782a` | `Excluded` | formatting/fix identation 메타 커밋 |
| `106` | `23b19a9776de` | `Excluded` | review-only follow-up |
| `107` | `dc7068a13517` | `Excluded` | `9db630932e0c`로 닫힌 public IP limit 기능의 earlier/review variant |
| `108` | `0a4b4c6af05c` | `Excluded` | mainline 기능의 `[20.3]` backport 파생 커밋 |
| `109` | `06ee2fea76d1` | `Excluded` | `e8d57d1b0dc1`로 닫힌 secondary storage limit 기능의 earlier variant |
| `110` | `7faa1b650b92` | `Excluded` | 이미 반영된 VPC resource allocation의 `[20.3]` backport |
| `111` | `1593944553fb` | `Excluded` | 이미 반영된 project limit 기능의 `[20.3]` backport |
| `112` | `4dd91feb277a` | `Excluded` | 이미 반영된 instance limits 기능의 `[20.3]` backport |
| `113` | `89df31816480` | `Excluded` | 이미 반영된 resource allocation 기능의 `[20.3]` backport |
| `114` | `07c3dc86b2df` | `Excluded` | 이미 반영된 secondary storage handling의 `[22.0]` backport |
| `115` | `3b42fbf3b246` | `Excluded` | CI fixup |
| `116` | `9bbd32a8ef03` | `Excluded` | contributor metadata |
| `117` | `58916eb60803` | `Excluded` | upstream final state is reverted and `europa` keeps the final state |
| `118` | `831ef82ff9b6` | `Excluded` | 이미 반영된 VPC resource allocation의 `[22.0]` backport |
| `119` | `8d269cf5bef1` | `Excluded` | 이미 반영된 project limit 기능의 `[22.0]` backport |
| `120` | `003c8408179b` | `Excluded` | 이미 반영된 instance limits 기능의 `[22.0]` backport |
| `121` | `37e365777074` | `Excluded` | 이미 반영된 resource allocation 기능의 `[22.0]` backport |
| `122` | `a566af35f5c8` | `Excluded` | review-only follow-up |
| `123` | `f1f779a08d94` | `Excluded` | cleanup/fixup 성격의 파생 커밋 |

## 다음 실행 순서

1. 다음 iteration부터는 [main-to-europa-2026-only-inventory-2026-04-23.txt](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/developer/history/main-to-europa-2026-only-inventory-2026-04-23.txt) 기준으로만 후보를 뽑는다.
2. [main-to-europa-2026-baseline-pretriage-2026-04-23.tsv](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/developer/history/main-to-europa-2026-baseline-pretriage-2026-04-23.tsv) 기준으로 `needs_source_review`만 수동 판정 대상으로 삼는다.
3. `Batch J`까지 판정이 완료됐으므로, 현재 `needs_source_review` 잔여 항목은 `0`건이다.
4. 다음 iteration부터는 `2026-04-23` 이후 `main`에 새로 추가된 커밋만 신규 후보로 추출한다.
5. baseline inventory는 참고용 스냅샷으로 유지하고, 차기 iteration에서는 새 기준일 스냅샷을 별도 파일로 남긴다.
