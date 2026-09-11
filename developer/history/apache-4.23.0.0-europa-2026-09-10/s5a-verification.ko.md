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

# S5A: VM·KVM·migration·import·오퍼링 통합 검증

부모 Epic #987, 작업 #993, 구현 PR [#1024](https://github.com/ablecloud-team/ablestack-cloud/pull/1024). 소스 기준은 `2b4035e1fb03810ae572bb18fafabb74c22babc7`, 사용자 지정 DB 출발점은 `014895d8f3dc2f062f379b51ee62d36a0adae88a`이다.

## 현재 판정

직접 S5A 원본28개와 S2 연관 원본4개의 소스를 통합했다. 원본별 의도·Europa 적응 내용은 `s5a-review.tsv`에 기록한다. 2026-09-11 사용자 확정에 따라 기능 작업은 코드 통합·자동 검증·정상 병합으로 완료한다. PR은 Ready for review이며 실물 테스트를 병합 선행 조건으로 삼지 않는다. 실물 KVM/Ceph·FTCTL·migration/import/HA/다중 ISO 및 VMware 시나리오는 [#1025](https://github.com/ablecloud-team/ablestack-cloud/issues/1025)에 이관하여 모든 코드 병합 후 S8에서 실행한다. 미실행 실물 시험을 PASS로 표시하지 않는다.

## 동일 버전 DB 전환

기존 Complete 단계 `europa-4.23-s4-v1`을 변경해 재실행시키지 않고 새 단계 `europa-4.23-s5a-v1`을 추가했다. `DatabaseUpgradeChecker`가 `EuropaComputeSchemaUpgrade` 및 view 갱신을 실행한 뒤 Complete를 기록한다.

- 기존 `vm_schedule`과 `vm_scheduled_job`을 generic resource 테이블로 전환한다. ID, UUID, cron, timezone, action, enabled, 시간과 async job 연결을 보존한다.
- MySQL의 DDL 자동 커밋을 전제로 열·인덱스·FK를 확인하여 부분 전환부터 재시도한다. 새 인덱스를 생성한 뒤 기존 인덱스를 제거한다. 새·기존 테이블이 동시에 있으면 임의 병합하지 않고 시작을 거부한다.
- `vm_iso_map`의 VM/ISO 및 VM/slot unique 계약, resource schedule details, live CPU/RAM 설정, Windows Server 2025 VMware 매핑, 이벤트 이름과 retention 설정을 반영한다.
- 신규 설치와 기존 설치의 열 타입/null/default, 인덱스와 FK 동작을 비교했다. 열 표시 순서, COMMENT, AUTO_INCREMENT 카운터는 의미 계약 비교 대상이 아니다.

실제 MySQL 8.0 fixture 결과:

| 항목 | 결과 |
| --- | --- |
| 신규 설치의 실제 DatabaseCreator/JDBC 실행 | PASS, 454개 table/view, 명명된 단계11개 Complete |
| 전체014 기준 DB 복제본 업그레이드 | PASS, 454개 table/view, 기존 version51행 보존 |
| 기존 schedule5개/job5개 | 원래 열 값 보존, VirtualMachine resource type 추가 |
| 공식 S4 Rocky9.8 관리 서버 JAR에서 S5A 전환 | PASS, 기존 S4 journal timestamp 보존, retention23 보존 |
| DDL 자동 커밋 직후 강제 실패 | 21개 모든 경계에서 재시도·반복 실행 PASS |
| 중복 scheduled job, FK cascade, 모호한 테이블 | 중복 거부·삭제 전파·시작 거부 PASS |
| ALTER 권한 회수 | 시작 실패 및 S5A Pending |
| 권한 복구 후 JVM2개 동시 시작 | 양쪽 종료0 및 단일 Complete, 기존 journal 보존 |
| 신규 설치와 업그레이드 schema parity | 열·인덱스·FK 차이0 |
| 신규 설치 후2회 반복 checker 실행 | 전체 데이터·DDL·journal timestamp snapshot 동일 |

DB 구조 전환을 검증한 소스는 `0906ef2deb`이다. 이후 소스 보완은 권한 검사와 테스트이며 S5A DB 구현은 같다. 전체014 fixture의 retention 행은 초기 UPDATE 당시 존재하지 않았으므로 그 fixture에서 retention23을 검증했다고 주장하지 않는다. retention 보존은 별도 공식 S4 fixture에서 INSERT 후 확인했다.

합성 fixture의 cron 문자열을 그대로 보존하는 시험과 API cron 유효성 검사는 구분한다. 실제 API CRUD에는 유효한5필드 cron을 사용했다. 이 시험은 사용자 운영 데이터 dump나 운영 규모 upgrade 시간 검증이 아니다.

## 관리 서버와 API

조립된 OSS 관리 서버 JAR을 전용 DB와 loopback HTTP18993으로 실행하고 두 번 재시작했다. 신규 API로 생성 → 기존 VM API로 수정 → 신규 API로 삭제하는 흐름을3회 통과했다. 기존 schedule5개는 유지됐고 version51행과 모든 migration timestamp가 보존됐다.

VM schedule의 일반 API 권한만 확인하면 기존 `createVMSchedule` 등의 거부 규칙을 우회할 수 있어 추가 검사했다. VM 대상의 신규4개 API는 기존 VM API 권한도 요구한다. update는 저장된 schedule의 resource type을 사용하며 API 키를 동일하게 전달한다. AutoScaleVmGroup은 VM 전용 거부 규칙을 상속하지 않는다. 소유자 검사도 유지한다. 관리자 정상 CRUD와 권한 거부 시험은 서로 다른 검증이다.

관리 서버 fixture는 실제 KVM agent가 없는 환경이다. 초기 네트워크/LB, systemd Usage, 클러스터 주소·인증서 관련 백그라운드 경고는 남으며 운영 클러스터 전체 정상화를 뜻하지 않는다. 최종 권한 보완 이후 JAR의 API 결과는 검사 완료 시 이 문서에 추가한다.

## VM·HA·스토리지 계약

- HA: NFS/SharedMountPoint/RBD/CLVM4종과 스토리지별 opt-in, RBD volume 전달, 기존 timing을 유지했다. 잘못된·실패한·미확정 응답은 host 사망 증거가 아니다. 이웃의 alive 응답을 불명확한 OOBM 상태가 덮어쓰지 않는다. 기본 OOBM RESET과 storage-heartbeat reboot를 활성화하지 않는다. VM 작업 취소는 host 검사/복구 상태를 고려한다.
- Migration: 기존 후보 목록과 storage/affinity/UEFI/TPM/vGPU 경계를 유지한다. 빈 allocator 결과를 전체 호스트로 확장하지 않는다. user VM은 storage motion 시 zone 범위, system VM은 pod 범위를 유지한다.
- Import: powered-on·dynamic CPU/RAM, per-import CheckedReservation 해제, S3 소유권, VDDK와 target pool 계약을 유지한다. 변환 결과는 해당 UUID 이름의 디스크만 지우며 XML이 가리키는 parent/unrelated 디스크는 삭제하지 않는다. 등록된 storage provider를 사용하며 대상 개수 불일치는 부분 성공으로 반환하지 않는다.
- 다중 ISO: 기존 primary ISO 필드를 유지하고 추가 슬롯을 추적한다. ConfigDrive의 슬롯을 침범하지 않으며 API attach/detach를 VM별로 직렬화한다. 선택한 ISO만 분리하고 router/direct-download 경로를 유지한다.
- Live scaling/복제: requested RAM의 구버전 wire fallback, 고정·dynamic·constrained 입력, GPU/KVDO/TPM/UEFI 및 복제/NIC link/PXE 사용자 설정을 보존한다. 제약 범위 밖 값은 거부하며 조용히 최소값으로 바꾸지 않는다.
- Guest OS rule: 기존 후보·UEFI·TPM 제약에 추가 적용한다. JsInterpreter 생성자 변경은 Quota 호출부와 함께 반영했다.
- DRS: skip-DRS는 영속 VM details에서 일괄 조회하며 Europa KVDO 예외를 유지한다.
- FTCTL/DR: 진행 상태는 host 소유이며 VM detail에 telemetry mirror를 새로 만들지 않는다. FTCTL 도중 hangctl 자원을 파괴하는 경로를 추가하지 않는다.

## 빌드와 테스트

Rocky Linux9.8 linux/amd64, JDK17, Maven3.9.10, Node14.21.3/npm6, Python3.10을 사용한다. 소스·의존성·테스트는 Docker 내부에서 수행했다. NonOSS 의존성은 공식 Actions와 같은 고정 SHA `f94b5cfcd12e7ce50f9e732ff0e12affa9030640`을 사용했다.

현재 확인된 결과: UI30 suites/351 tests PASS, KVM HA/cleanup/import 집중21 tests PASS, server migration/allocator/template/API dispatcher 집중345 tests PASS. 최종 noredist+simulator clean install과 공식 Actions는 진행 중이며 최종 결과를 후속 반영한다. 중간 실행에서 발견한 미사용 import·Mockito fixture 오류를 수정했다. UI 집중 시험의 초기 exit137은 전체 coverage 수집 중 메모리 부족이며 최종 UI 전체 시험은 coverage=false로 성공했다.

`codecov`/Sonar 등 skip은 PASS가 아니다. PR conflict triage는 다른 PR #1022에 label을 쓰는 단계에서 integration 권한 오류로 실패했다. 이를 제품 빌드 성공이나 이번 PR의 실제 충돌로 해석하지 않는다. 워크플로를 비활성화하거나 관리자 우회 merge를 사용하지 않는다.

## 남은 게이트와 인수

- #1025/S8: 모든 코드 병합 후 시험용 KVM/Ceph 및 VMware 환경에서 시작/정지·고정/dynamic/constrained 복제·live scaling·storage migration·변환/import 실패 정리·HA·ISO를 검증한다. FTCTL 진행과 RBD parent/thin 보존을 실제 데이터로 확인한다. 이 인수 시험은 #993 코드 병합을 막지 않으며 실제 실행 전 PASS로 표시하지 않는다.
- #994: CLVM 관련 별도 배정 원본과 provider/backup/storage 변경은 S5B가 소유한다. S5A가 기존 CLVM 코드를 보존한 것을 해당 원본 적용으로 표시하지 않는다.
- #997: CKS live offering 연관 변경을 반영할 때 현재 requested RAM·고정 offering 경로를 인수한다.
- S2 공유 merge는 남은 side commit이 있으면 Pending을 유지한다. 특히 S5A로만 표시된 migration merge에도 S5B side commit이 있으므로 제목/표면 분류만으로 종료하지 않는다.
- 이후 같은4.23 DB 변경은 별도의 checkpoint를 사용한다. 이미 Complete인 S5A v1 SQL만 바꾸면 기존 설치에서는 다시 실행되지 않는다.
- 실제 운영 업그레이드 전 전체 DB 백업을 검증한다. DDL은 transaction rollback만으로 원복되지 않으므로 코드/DB 백업의 일치하는 조합으로 복구한다.

재현 도구와 환경 경계는 [s5a-fixtures](s5a-fixtures/README.ko.md), 원본별 검토는 [s5a-review.tsv](s5a-review.tsv)에 기록한다. 개발 DB와 기존 볼륨은 초기화하지 않았다.
