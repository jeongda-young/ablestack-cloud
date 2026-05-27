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
# UI 테스트 / 검토 체크리스트 - `2026-04-17` 이후 `ablestack-europa`

## 목적

이 문서는 `2026-04-17` 이후 `ablestack-europa` 브랜치에 반영된 변경 중에서,
반드시 UI 관점으로 다시 확인해야 하는 항목만 추린 운영용 체크리스트다.

여기서 말하는 UI 검토 범위는 다음을 모두 포함한다.

- 화면 구성, 라우팅, 폼, 문구, 버튼 노출
- UI가 호출하는 API 계약 변화
- 서버/패키징 변경 때문에 UI 기능이 보이거나 사라지는 경우
- 로그인, SAML, 세션, 권한, 백업, 가져오기/내보내기처럼 실제 사용자 흐름에 직접 영향을 주는 변경

## 범위 기준

- 기준 브랜치: `ablestack-europa`
- 기준 시점: `2026-04-17` 이후 반영분
- 포함 기준:
  - `ui:` 커밋
  - UI가 직접 의존하는 로그인/세션/API 변경
  - 화면은 같아도 실제 사용자 플로우가 달라지는 서버 변경

## 우선순위

- `P0`: 배포 직후 반드시 확인. 실제 장애로 이어졌거나, 로그인/핵심 업무 흐름을 막는 항목
- `P1`: 주요 기능 회귀 확인이 필요한 항목
- `P2`: 변경 영향은 분명하지만 상대적으로 후순위인 화면/동작 점검

## 필수 테스트 항목

| 우선순위 | 영역 | 반영 커밋 | 반드시 확인할 내용 |
| --- | --- | --- | --- |
| `P0` | 로그인 / 세션 / SAML | `1aabbb1777`, `14814af275`, `9e9abb99d6`, `0fee5fe3f7`, `7d62faab7e` | 로그인 후 대시보드 진입, `readyForShutdown` 431 미발생, logout / SAML 전환 동작, 로그인 화면의 불필요한 `401 listIdps` / `401 forgotPassword` 미발생, `domainId` camelCase 로그인 호환성 확인 |
| `P0` | 스토리지 > 백업 | `e086d987bd`, `8cda57843e`, `8d961d78f9`, `ef84709dd6` | 백업 목록 화면 진입 시 오류 없이 로딩, keyword 검색 정상, 삭제된 backup offering 연결 데이터가 있어도 화면이 깨지지 않는지, pending restore job이 있을 때 UI에서 삭제 차단/오류 메시지가 정상인지 |
| `P0` | 인스턴스 가져오기-내보내기 / VMware 외부 vCenter | `e3bd104382`, `dc2c39f283`, `53d063fb6d`, `fdfa6bdde5`, `c8f413e09e`, `b0b8f54ea8` | 외부 vCenter 입력 폼 렌더링, VMware VM 목록 조회, unsupported 서버에서는 액션 숨김, supported 서버에서는 액션 노출, guest OS 매핑 및 unmanaged import 관련 오류 메시지/예약 처리 확인 |
| `P0` | 테스트 서버 빌드 옵션 의존 기능 | `f8eb043952`, `fd0aa938f5` | VMware datacenter import 관련 API가 `noredist` 빌드에서만 살아나는지, dev release 산출물 배포 후 `listVmwareDcs` / `listVmwareDcVms` 기반 UI가 정상 동작하는지 |
| `P1` | 호스트 추가 | `df936b04df` | KVM host 추가 시 URL에 `host:port` 입력 가능 여부, 포트 미입력 시 기본 설정 fallback 동작, 잘못된 포트 입력 시 오류 메시지 확인 |
| `P1` | 네트워크 생성 | `b0e8bab312` | zone context 없이 global create-network 진입 시 버튼/흐름이 안전하게 막히는지, zone이 있을 때 기존 생성 플로우가 유지되는지 |
| `P1` | 템플릿-존 연결 삭제 | `e0181a4a87` | `이미지 > Template Zones`에서 삭제 후 상세에 머무르지 않고 목록으로 정상 복귀하는지 |
| `P1` | VPC tier 생성 | `ae44d154d7` | Add Tier 화면의 network offering 드롭다운에서 잘린 라벨이 아니라 전체 라벨이 표시되는지 |
| `P1` | VM 배포 / 보안그룹 | `a6928387ef` | VM 배포 wizard에서 security group 후보가 실제 deployment owner 기준으로만 보이는지, admin이 다른 계정으로 배포할 때 누락/과노출이 없는지 |
| `P1` | 기본 포털 언어 | `ab299b65a1` | 초기 접속 시 기본 언어가 설정값에 따라 반영되는지, 번역 메뉴 전환과 새로고침 후 유지 여부 확인 |
| `P1` | 백업에서 VM 생성 | `e086d987bd` | `Create VM from Backup` 화면에서 source backup 아키텍처가 유지되는지, architecture mismatch로 인한 잘못된 템플릿/오퍼링 선택이 없는지 |
| `P1` | 백업 복원 / NAS / LINSTOR | `8c47f676a8`, `e85e854cd1`, `c37ef5e0f1`, `e6d0c25dba` | KVM host 부재, timeout, LINSTOR restore 경로에서 UI가 무한 대기하지 않고 적절한 오류/상태를 보여주는지 |
| `P1` | 가져오기 / SharedMountPoint | `fee04fc506`, `0f43c06318` | SharedMountPoint 볼륨 import / unmanage / validation 화면에서 대상 볼륨 표시와 작업 성공/실패 메시지 확인 |
| `P1` | 백업 삭제 보호 | `8cda57843e` | restore 진행 중 또는 pending job이 있는 백업에 대해 UI에서 삭제 버튼 동작 결과가 안전한지 확인 |
| `P2` | Kubernetes / CKS 대시보드 | `16fdb49f92` | CKS/Headlamp 관련 대시보드 버튼과 링크가 기존 K8s dashboard 대신 Headlamp 흐름으로 자연스럽게 이어지는지 |
| `P2` | 상세 VM 시작 오류 노출 | `bcc7a07225` | VM start 실패 시 UI에 노출되는 상세 오류가 설정값에 따라 기대대로 보이거나 숨겨지는지 |
| `P2` | backing file import 설정 | `4227ac97a0` | 관련 설정 on/off에 따라 import UI에서 backing file 허용/차단 결과가 일관적인지 |

## 권장 테스트 시나리오

### 1. 로그인 / 세션 / SAML 회귀

- `admin` 계정으로 로그인 후 `#/dashboard` 도달 확인
- 콘솔과 네트워크에서 아래 항목 확인
  - `readyForShutdown` `431` 미발생
  - `/client/undefined` 호출 미발생
  - 불필요한 `logoutresponse` 오류 미발생
  - 로그인 화면에서 `listIdps` / `forgotPassword` 무의미한 `401` 미발생
- SAML이 활성화된 환경이라면
  - 로그인 후 `managementserverid` 유지
  - SAML account switcher / logout 동작
  - 헤더 렌더링 이상 여부

### 2. 스토리지 > 백업

- `스토리지 > 백업` 첫 진입 시 목록 로딩
- keyword 검색
- backup offering이 삭제된 이력 데이터가 있는 행 표시
- `Create VM from Backup`
- restore / delete / pending restore 충돌 시 오류 메시지

### 3. 인스턴스 가져오기-내보내기

- `VMware` 선택 후 `VMware에서 ABLESTACK 클러스터로 인스턴스 가져오기`
- `VMware vCenter Datacenter 소스 선택 = 외부`
- vCenter 정보 입력 후 VM 목록 조회
- supported 서버:
  - VMware 목록 정상 조회
  - guest OS / host / cluster 정보 표시
- unsupported 서버:
  - 해당 action 자체가 숨겨져야 함

### 4. 네트워크 / 이미지 / 배포

- `VPC Tier` 추가 시 network offering 라벨 가독성 확인
- `Create Network`를 zone context 없이 여는 경로 차단 확인
- `Template Zones` 삭제 후 목록 복귀
- VM 배포 wizard에서 security group 후보 범위 확인

### 5. 운영자용 관리 화면

- `호스트 추가`에서 KVM URL `host:port` 입력
- 포트가 지정된 host와 미지정 host를 각각 추가 시도
- 실패 시 메시지와 성공 시 저장된 동작 확인

## 이번 브랜치에서 특히 주의할 포인트

- VMware datacenter import는 단순 UI 문제가 아니라 `noredist` 빌드 여부에 따라 기능 자체가 사라질 수 있다.
- 로그인 / SAML / maintenance polling은 화면 콘솔 잡음 수준이 아니라 대시보드 사용성에 직접 영향을 준다.
- 백업 화면은 실제로 null dereference와 list API 오류가 발생했던 영역이라 단순 smoke test로 끝내면 안 된다.
- import/export 계열은 UI form 렌더링, API 존재 여부, 서버측 reservation 처리까지 함께 봐야 한다.

## 추천 실행 순서

1. 로그인 / 대시보드 / logout / SAML
2. 스토리지 > 백업
3. 인스턴스 가져오기-내보내기 / VMware
4. 네트워크 / 이미지 / VM 배포
5. 호스트 추가 / 운영자 기능

## 문서 사용 규칙

- 이후 `2026-04-17` 이후 반영분에 대한 UI 검토를 요청받으면 이 문서를 기본 체크리스트로 사용한다.
- 새 장애가 실제로 재현되면 이 문서에 항목을 추가하고 우선순위를 조정한다.
- 빌드/배포 경로는 [build-release-policy.ko.md](/Users/dhslove/Documents/GitHub/dhslove/ablestack-cloud/developer/build-release-policy.ko.md)를 따른다.
