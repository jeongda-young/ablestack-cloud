# Guest Network Observability Phase 4 완료 보고

작성일: 2026-07-24

작업 브랜치: `codex/guest-network-observability`
작업 범위: IPv4/IPv6 routing 수집, 정규화, snapshot/API/UI 표시

## 1. 완료 결과

Phase 4의 route 수집 및 UI 구현을 완료했다.

- 표준 QGA `guest-network-get-route` capability 확인과 parser
- Linux `/usr/sbin/ip`, `/usr/bin/ip` IPv4/IPv6 JSON fallback
- Windows PowerShell `Get-NetRoute` JSON fallback
- 고정 allowlist, timeout polling, output 크기 제한, base64 및 UTF-8 검증
- destination/prefix, gateway, interface, metric, table, protocol, scope, default route 정규화
- DB snapshot section 병합과 stale 보존
- 상세 API route 응답
- Ant Design route table, IPv4/IPv6/default filter, 검색, 정렬

Phase 4에는 DB schema 변경이 없다. Phase 2에서 추가한 `vm_guest_network_state.payload`에 route section이 함께 저장된다.

## 2. 수집 우선순위

```text
guest-info
  ├─ guest-network-get-route enabled
  │    └─ 표준 QGA route 수집
  └─ 표준 명령 미지원 또는 실패
       ├─ exec fallback disabled → UNSUPPORTED/UNAVAILABLE
       └─ exec fallback enabled
            ├─ guest-get-osinfo
            ├─ Linux 고정 ip -j route
            └─ Windows 고정 Get-NetRoute
```

표준 route 명령이 성공하면 fallback은 실행하지 않는다. fallback은 global setting을 명시적으로 활성화한 경우에만 실행한다.

## 3. 안전한 guest-exec

### 3.1 허용 명령

Linux:

```text
/usr/sbin/ip -j -4 route show table all
/usr/sbin/ip -j -6 route show table all
/usr/bin/ip  -j -4 route show table all
/usr/bin/ip  -j -6 route show table all
```

Windows:

```text
C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe
  -NoProfile
  -NonInteractive
  -Command
  <고정 Get-NetRoute Select-Object 및 ConvertTo-Json 스크립트>
```

다음 동작은 허용하지 않는다.

- 외부 입력으로 실행 파일 또는 인수 생성
- `/bin/sh -c`
- `cmd.exe /c`
- 일반 목적 guest command API
- UI/API를 통한 arbitrary command 전달

### 3.2 실행 제한

- `guest-exec` PID를 받은 뒤 `guest-exec-status`만 polling한다.
- 기본 command timeout을 적용한다.
- timeout 또는 interrupt 시 마지막 status 조회를 시도하고 작업을 종료한다.
- stdout/stderr는 base64 decode 전후 크기를 확인한다.
- decoded 출력 기본 제한은 1 MiB이다.
- UTF-8 malformed/unmappable 입력은 거부한다.
- JSON 파싱 실패는 route section 실패로 제한한다.
- route는 최대 4,096개를 보존하고 초과분은 `PARTIAL` 및 `truncated`로 표시한다.

## 4. 부하 및 cadence

interface와 route의 due schedule을 분리했다.

| 상황 | 실행 내용 |
|---|---|
| interface만 due | interface capability/표준 명령만 실행 |
| route만 due | route capability/route 명령만 실행, NIC DAO 조회 없음 |
| 둘 다 due | 한 전용 Agent command 안에서 due section만 순차 실행 |
| route unsupported/failure | route 전용 exponential backoff |
| fallback disabled | guest-exec 호출 0 |
| 기능 전체 disabled | 기존과 동일하게 scheduler/worker/QGA/DB 작업 0 |

기본 route interval은 600초이며 interface 성공이 route due 시간을 다시 뒤로 미루지 않는다.

## 5. snapshot 병합 및 상태

section별로 다음 정책을 적용했다.

- `NOT_DUE`: 기존 성공 section과 데이터를 그대로 유지
- `OK`, `EMPTY`: 새 결과로 교체
- `PARTIAL`: 제한된 새 결과와 truncation metadata 저장
- `UNAVAILABLE`, `UNSUPPORTED` + 기존 성공 데이터 존재: 마지막 성공 데이터를 유지하고 section을 `STALE`로 기록
- 성공 데이터 없음: 실제 `UNAVAILABLE` 또는 `UNSUPPORTED` 상태 기록

따라서 route-only cycle이 interface/IP를 제거하지 않고, route 실패도 마지막 성공 route를 즉시 삭제하지 않는다.

## 6. 정규화 모델

route별로 가능한 범위에서 다음 필드를 보존한다.

- `family`: `IPv4`, `IPv6`
- `destination`
- `prefix`
- `gateway`
- `interfacename`
- `metric`
- `table`
- `protocol`
- `scope`
- `default`

`default`, `0.0.0.0/0`, `::/0`는 각각 정규화된 default route로 처리한다. Linux on-link 및 Windows `0.0.0.0`, `::` next hop은 gateway를 임의 생성하지 않고 `null`로 처리한다.

## 7. API

기존 `getVirtualMachineGuestNetworkState` 응답에 `routes` 배열을 추가했다.

- 조회 API는 계속 DB snapshot만 읽는다.
- API refresh는 Agent 또는 host command를 실행하지 않는다.
- VM RBAC와 `ListEntry` ACL은 Phase 3 구조를 그대로 사용한다.
- VM 목록 summary에는 route를 추가하지 않아 목록 payload와 DB 부하를 제한한다.

## 8. UI

VM 상세 `IP 구성` 탭 아래에 routing table을 추가했다.

- default route 강조
- IPv4/IPv6 색상 구분
- destination/prefix
- gateway 또는 On-link
- interface
- metric
- table
- protocol
- scope
- 전체 속성 검색
- IPv4, IPv6, default route filter
- 주요 열 정렬
- pagination 및 horizontal scroll
- route section의 `STALE`, `PARTIAL`, `UNSUPPORTED`, `UNAVAILABLE` alert

UI는 기존 snapshot API 한 개만 호출한다.

## 9. 설정

추가 설정:

```text
vm.guest.network.details.exec.fallback.enabled=false
vm.guest.network.details.exec.output.limit.bytes=1048576
```

기존 설정:

```text
vm.guest.network.details.route.interval=600
vm.guest.network.details.command.timeout=3
vm.guest.network.details.failure.backoff.max=1800
```

fallback은 운영자가 검증 후 명시적으로 활성화하기 전까지 실행되지 않는다.

## 10. 검증

검증 명령:

```bash
mvn -pl core,plugins/hypervisors/kvm -am \
  -Dtest=GetVmGuestNetworkStateCommandTest,QemuGuestNetworkStateParserTest,QemuGuestRouteFallbackTest,LibvirtGetVmGuestNetworkStateCommandWrapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

mvn -pl server -am \
  -Dtest=GetVirtualMachineGuestNetworkStateCmdTest,VmGuestNetworkCollectionPolicyTest,VmGuestNetworkCollectorTest,VmGuestNetworkStateServiceImplTest,VmGuestNetworkApiServiceImplTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

NODE_OPTIONS=--openssl-legacy-provider npx eslint src/views/compute/GuestNetworkTab.vue
jq empty public/locales/en.json public/locales/ko_KR.json
NODE_OPTIONS=--openssl-legacy-provider npm run build
git diff --check
```

검증 범위:

- 표준 QGA IPv4/IPv6 route fixture
- Linux IPv4/IPv6 `ip -j` fixture
- `/usr/sbin/ip` 미존재 시 `/usr/bin/ip` fallback
- Windows `Get-NetRoute` fixture
- default route 및 on-link 정규화
- route 최대 개수 제한
- allowlist 외 명령 거부
- output limit
- timeout 후 final status 확인
- section별 cadence
- route-only cycle의 NIC DAO/interface 요청 부재
- due가 아닌 interface snapshot 병합
- route 실패 시 마지막 성공 route stale 보존
- API route serialization
- UI lint, locale JSON, production build

검증 결과:

- Core/KVM 대상 테스트: 25개 통과
- API/Backend 대상 테스트: 25개 통과
- 합계: 50개 통과, failure/error 0
- UI ESLint: 통과
- 영문/한글 locale JSON: 통과
- UI production build: 통과
- `git diff --check`: 통과

기존 KVM plugin POM의 중복 `org.json:json` 경고와 Browserslist DB 갱신 안내는 이번 변경과 무관한 기존 경고다.

## 11. 22.x 사후 preflight와 보완 설계

2026-07-26 실제 22.x QGA 7.2.22에서 route fallback의 OS 계열 판별
결함을 확인했다.

- Debian VM은 `guest-exec`, `guest-exec-status`, `guest-get-osinfo`가
  모두 enabled였다.
- `guest-get-osinfo`는 `id=debian`, `kernel-name` 미제공,
  `pretty-name=Debian GNU/Linux 12 (bookworm)`를 반환했다.
- 기존 `normalizedOs.contains("linux")`는 `debian`을 거부하므로 고정
  route adapter에 도달하지 못한다.
- 동일 VM에서 `/usr/sbin/ip -j -4 route show table all`은 exit 0,
  유효한 JSON route 10건을 반환했다.
- `/usr/sbin/ip -j -6 route show table all`도 exit 0과 유효한 빈
  JSON 배열을 반환했다.

Phase 4 사후 보완으로 문자열 포함 판별을 명시적 fail-closed OS family
resolver로 교체했다. Debian/Ubuntu/Rocky/CentOS/Windows/unsupported
회귀 테스트와 22.x Debian 단일 VM gate를 통과했으며, 실제 IPv4/IPv6
route 명령 결과에서 route 10개가 `OK`로 저장됐다.

상세 설계는
`docs/guest_network_observability_os_family_design.md`를 따른다.

## 12. 현재 배포 상태

- Host 3 기존 KVM plugin JAR에 OS family 관련 class 19개만 patch했다.
- `mold-agent.service` 재시작 후 ReadyAnswer와 기존 stats를 확인했다.
- 대상 Debian VM에서 route 10개와 DNS 서버 2개가 모두 `OK`로 저장됐다.
- Ubuntu fixture 검증은 통과했지만 실행 표본이 없어 실제 VM gate는 남아 있다.
- commit, push, PR은 이번 보완 구현 단계에서 수행하지 않았다.
