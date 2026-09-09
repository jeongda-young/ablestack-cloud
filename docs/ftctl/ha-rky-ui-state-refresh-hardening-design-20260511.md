# HA-RKY 장애 보호 탭 UI 보완 설계 - 2026-05-11

## 배경

HA-RKY 전체 절차에서 보호 조치, failover, failback, 보호 해제가 완료되었다. 이후 UI 관점에서 다음 보완점이 확인되었다.

- Primary VM이 실행 중이 아닌 상태에서도 보호 조치 버튼이 활성화되어 보호 등록을 시도할 수 있다.
- 보호 조치 직후 장애 보호 탭이 즉시 모니터링 화면으로 전환되지 않거나, 보호 row가 만들어지기 전에 한 번 조회한 뒤 자동 갱신이 약해질 수 있다.
- qemu FTCTL `events.log` 중심 표시로 변경하면서 VM 실행 상태처럼 Cloud DB가 정본인 정보가 화면에서 빠질 수 있다.

## 원칙

- UI는 host libvirt 또는 FTCTL 엔진을 직접 호출하지 않는다.
- Cloud UI는 Cloud API 응답만 표시하고, 필요 시 명령을 Cloud backend로 전달한다.
- qemu FTCTL `events.log`는 작업 이벤트와 block copy 진행률의 소스다.
- VM 실행 상태, host 배치, standby VM/volume 식별 정보는 Cloud DB의 값을 Cloud backend가 API 응답에 포함해 UI에 제공한다.
- Cloud backend도 UI와 같은 사전 조건을 검증해 API 직접 호출로 잘못된 보호 등록이 실행되지 않게 한다.

## 개선 설계

### 1. 보호 조치 버튼 Running 조건

`FtctlTab.vue`의 보호 조치 버튼은 다음 조건을 모두 만족할 때만 활성화한다.

- `registerFtctlProtection` API 권한 있음
- 관리자 계정
- KVM VM
- standby 관리 view가 아님
- VM 상태가 `Running`
- 기존 보호 구성이 없음
- 화면 refresh 또는 보호 등록 작업이 진행 중이 아님

VM이 `Running`이 아니면 버튼을 비활성화하고 안내 문구를 표시한다.

backend의 `registerFtctlProtection`도 `userVm.getState() == Running` 조건을 강제한다. 이 검증은 standby VM/volume 생성, 보호 row 생성, VM detail 기록 전에 수행한다.

### 2. 보호 조치 직후 자동 갱신

보호 등록 modal은 응답 payload와 job id를 parent `FtctlTab`에 전달한다.

`FtctlTab`은 보호 등록 완료 이벤트를 받으면 다음 순서로 동작한다.

1. 현재 탭을 유지한다.
2. modal을 닫는다.
3. 응답 payload가 있으면 화면 상태에 즉시 반영한다.
4. post-register polling을 시작한다.
5. polling 동안 `getFtctlProtection`, `getFtctlEvents`, `getFtctlCheck`, `getFtctlHealth`를 background로 반복 조회한다.
6. `protectionConfigured=true`가 감지되면 일반 자동 갱신 루프로 전환한다.

polling은 화면 깜빡임을 피하기 위해 기존 상태를 비우지 않고 변경 데이터만 갱신한다. 최초 로딩 spinner는 사용하지 않고, progress 영역의 작은 refresh indicator만 사용한다.

### 3. VM 상태 표시 복구

`FtctlProtectionResponse`에 다음 Cloud DB 기반 필드를 추가한다.

- `primaryvirtualmachinestate`
- `primaryvirtualmachinehostid`
- `primaryvirtualmachinehostname`
- `secondaryvirtualmachinestate`
- `secondaryvirtualmachinehostid`
- `secondaryvirtualmachinehostname`

`FtctlServiceImpl.buildProtectionResponse()`는 primary/secondary VM을 DB에서 조회해 위 필드를 채운다.

UI는 summary 영역과 detail 영역에서 primary/secondary VM 상태를 표시한다.

- 보호 미구성 상태: 현재 `resource.state`를 표시한다.
- 보호 구성 상태: `getFtctlProtection` 응답의 primary/secondary VM 상태와 host 정보를 표시한다.

## 기대 효과

- Stopped VM에서 보호 조치가 UI와 backend 양쪽에서 차단된다.
- 보호 등록 직후 장애 보호 탭이 자동으로 모니터링 상태로 전환된다.
- qemu `events.log` 기반 이벤트 표시를 유지하면서 Cloud DB 기반 VM 상태가 누락되지 않는다.
