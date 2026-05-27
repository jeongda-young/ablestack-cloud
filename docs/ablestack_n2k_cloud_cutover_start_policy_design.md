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

# ABLESTACK-N2K Cloud Cutover Start Policy Design

## 배경

`ablestack_n2k wizard --apply` 또는 `ablestack_n2k run --apply`는 Cloud 대상 VM을 생성하되 시작하지 않는다. 반면 Cloud UI/API를 통해 실행하는 N2K 경로는 KVM agent wrapper가 항상 `--start`를 추가했기 때문에, 사용자가 cutover까지 완료한 뒤 대상 VM을 정지 상태로 남기는 정책을 선택할 수 없었다.

## 설계

### API

`importUnmanagedInstanceForAblestackN2K`에 `starttargetvm` Boolean 파라미터를 추가한다. 기본값은 기존 동작과 동일하게 `true`이다.

- `true`: Phase2 cutover 이후 Cloud 대상 VM을 시작한다. wrapper는 `--start`를 전달한다.
- `false`: Phase2 cutover 이후 Cloud 대상 VM을 정지 상태로 둔다. wrapper는 `--apply`만 전달한다.

### 작업 컨텍스트

Phase1에서 선택한 정책은 import VM task의 source context JSON에 `startTargetVm`으로 저장한다. Phase2, resume, retry 요청에서 `starttargetvm`이 명시되지 않으면 저장된 값을 재사용한다. 저장값도 없으면 하위 호환을 위해 `true`로 처리한다.

### Agent wrapper

Cloud target provider(`ablestack-cloud`)인 경우 기존의 무조건 `--start` 호출을 `cmd.isStartTargetVm()`에 따라 분기한다. CLI/wizard의 기존 `--apply`/`--start` 의미는 변경하지 않는다.

### UI

초기 가져오기 대화상자의 N2K 섹션에 대상 VM 시작 여부 스위치를 추가한다. 기본값은 시작이다. Phase2 실행 모달에는 기존 설정 유지, 시작, 정지 유지 중 선택할 수 있는 드롭다운을 제공한다. 기본값은 기존 설정 유지로 두어 Phase1에서 저장한 정책을 보존한다.

## 빌드/배포

Cloud는 릴리즈 빌드가 아니라 Maven module 빌드와 UI module 빌드만 수행한다. 22번 공유 개발 환경에는 변경된 management/backend jar, agent jar, UI 정적 파일만 반영한다.

## DB 변경

신규 테이블/칼럼은 없다. 기존 import VM task source context JSON에 `startTargetVm` 키만 추가로 저장한다.
