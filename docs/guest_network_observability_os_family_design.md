# Guest Network Observability QGA OS 계열 판별 개선 설계

작성일: 2026-07-26

작업 브랜치: `codex/guest-network-observability`

상태: 구현 및 22.x Debian 단일 VM 배포 검증 완료

## 1. 목적

QGA `guest-exec`와 `guest-get-osinfo`가 활성화된 Debian/Ubuntu를 포함한
Linux 가상머신에서 route와 DNS가 `UNSUPPORTED`로 저장되는 문제를 수정한다.

이 개선은 다음 경계를 유지한다.

- UI → API → Backend/DB → Agent 구조를 변경하지 않는다.
- UI와 API는 계속 DB snapshot만 조회한다.
- 기존 VM, 볼륨, NIC, stats, HA 명령과 신규 관측 명령의 executor를 결합하지 않는다.
- OS 판별은 고정된 route/DNS adapter를 선택하는 용도로만 사용한다.
- 실행 파일과 인수의 고정 allowlist, shell 금지, timeout, 출력 제한을 유지한다.
- 이 OS family 판별 변경 자체는 DB schema와 API response 계약을 변경하지
  않는다. 이후 주·보조 IP 역할 확장은 payload schema v2와 API response
  필드를 추가하며 `guest_network_observability_primary_ip_design.md`를
  기준으로 한다.

## 2. 결함 분석

현재 `QemuGuestNetworkStateParser.parseOsId()`는
`guest-get-osinfo` 응답에서 `id`, `kernel-name`, `name` 순으로 첫 값을
선택한다.

```java
String osId = readAnyString(result, "id", "kernel-name", "name");
```

route와 DNS fallback은 이 문자열에 `linux`가 포함된 경우만 Linux adapter를
선택한다.

```java
String normalizedOs = osId == null ? "" : osId.toLowerCase(Locale.ROOT);
if (normalizedOs.contains("linux")) {
    return collectLinux(...);
}
```

QGA가 반환하는 실제 배포판 ID는 `debian`, `ubuntu`, `rocky`, `centos`처럼
`linux`를 포함하지 않는다. 따라서 QGA capability 확인은 통과하지만 고정
allowlist 명령을 만들기 전에 `UnsupportedOperationException`이 발생한다.

이 문제는 guest-exec command allowlist의 거부가 아니다. 현재 화면에
`Unsupported guest OS for ... fallback: debian`이 표시됐다는 것은 다음
조건이 이미 충족됐다는 뜻이다.

- exec fallback 활성화
- `guest-exec` 활성화
- `guest-get-osinfo` 활성화
- `guest-get-osinfo` 실행 성공

## 3. 22.x preflight

### 3.1 안전 경계

2026-07-26에 공유 22.x 환경에서 다음 읽기 전용 preflight를 수행했다.

- 서비스 재시작 없음
- VM 상태 변경 없음
- 파일 생성 또는 패키지 설치 없음
- `guest-get-osinfo`는 실행 중인 VM당 한 번만 호출
- guest-exec는 기존 코드에 고정된 절대 경로와 인수만 사용
- route와 DNS 조회만 실행

대표 대상은 `10.10.22.3`의 실행 중 VM `i-2-608-VM`이다.

### 3.2 QGA 및 OS 정보

| 항목 | 결과 |
|---|---|
| QGA version | `7.2.22` |
| `guest-exec` | enabled |
| `guest-exec-status` | enabled |
| `guest-get-osinfo` | enabled |
| `guest-network-get-interfaces` | enabled |
| `guest-network-get-route` | unsupported |
| OS `id` | `debian` |
| OS `kernel-name` | 미제공 |
| OS `pretty-name` | `Debian GNU/Linux 12 (bookworm)` |
| 현재 `contains("linux")` 판별 | false |

세 호스트의 실행 중 domain 37개에 `guest-get-osinfo`만 호출한 탐색에서는
다음 ID를 확인했다.

| QGA OS ID | 표본 | `kernel-name` |
|---|---:|---|
| `debian` | 다수 | 미제공 |
| `rocky` | 다수 | 미제공 |
| `centos` | 2 | 미제공 |
| `mswindows` | 2 | 미제공 |

Ubuntu 실행 표본은 현재 22.x 환경에서 확인되지 않았다. Ubuntu는 실제
preflight 미확보 상태를 숨기지 않고, 구현 단계에서 `id=ubuntu` fixture와
배포 전 Ubuntu VM preflight를 필수 gate로 둔다.

실제 QGA 7.2.22가 Linux 및 Windows 표본 모두에서 `kernel-name`을 제공하지
않았으므로 `kernel-name=Linux`만 사용하는 설계는 채택하지 않는다.

### 3.3 고정 guest-exec 검증

동일 Debian VM에서 현재 코드의 고정 allowlist 명령을 직접 검증했다.

| 명령 | 결과 |
|---|---|
| `/usr/sbin/ip -j -4 route show table all` | exit 0, JSON, 10 routes |
| `/usr/sbin/ip -j -6 route show table all` | exit 0, JSON, 0 routes |
| `/usr/bin/resolvectl status --no-pager` | 실행 파일 없음 |
| `/usr/bin/nmcli ... device show` | 실행 파일 없음 |
| `/usr/bin/cat /etc/resolv.conf` | exit 0, 74 bytes |

이는 다음을 증명한다.

- route 실패 원인은 guest-exec 권한이나 `ip` 명령이 아니다.
- DNS source 우선순위와 `/etc/resolv.conf` 최종 fallback은 실제 Debian
  환경에 적합하다.
- OS family 판별만 통과하면 현재 adapter로 route와 DNS를 수집할 수 있다.

## 4. 설계 결정

### 4.1 문자열 OS ID 대신 명시적 모델 사용

`com.cloud.hypervisor.kvm.resource` package에 다음 public 내부 계약을
추가한다. wrapper가 `resource.wrapper` 하위 package에 있으므로
package-private type은 사용하지 않는다.

```java
public final class QemuGuestOsInfo {
    private final String id;
    private final String kernelName;
    private final String name;
    private final String prettyName;
}

public enum QemuGuestOsFamily {
    LINUX,
    WINDOWS,
    UNSUPPORTED
}

public final class QemuGuestOsFamilyResolution {
    private final QemuGuestOsFamily family;
    private final String source;
    private final QemuGuestOsInfo osInfo;
}
```

`source`는 `id:debian`, `kernel-name:linux`,
`pretty-name:linux` 또는 `unsupported`처럼 판별 근거를 기록한다.
이 값은 debug log와 section 상세 사유에만 사용하고 API schema는 변경하지
않는다.

### 4.2 parser 변경

`QemuGuestNetworkStateParser`에 다음 메서드를 추가한다.

```java
public QemuGuestOsInfo parseOsInfo(String json)
```

이 메서드는 `guest-get-osinfo`의 다음 필드를 독립적으로 읽고 소문자 판별용
정규화와 원본 진단값을 분리한다.

- `id`
- `kernel-name`
- `name`
- `pretty-name`

기존 `parseOsId()`는 즉시 삭제하지 않는다. `parseOsInfo()` 결과에서
기존과 같은 `id` → `kernel-name` → `name` 순서로 첫 non-blank 값을
반환해 기존 테스트와 호출자의 호환성을 유지한다. 신규 수집 경로에서는
`parseOsId()`를 사용하지 않는다.

### 4.3 OS family resolver

신규 `QemuGuestOsFamilyResolver`를 추가한다. 판별 순서는 다음과 같다.

1. 정규화한 `id`가 Windows ID 집합이면 `WINDOWS`
2. 정규화한 `id`가 Linux 배포판 ID 집합이면 `LINUX`
3. 선택적 `kernel-name`이 정확히 `linux`면 `LINUX`
4. `name` 또는 `pretty-name`에 독립된 `linux` 토큰이 있으면 `LINUX`
5. 그 외에는 `UNSUPPORTED`

모든 비교값은 `trim()` 후 `Locale.ROOT` 소문자로 정규화한다. 표시 이름의
Linux 판별은 단순 `contains()`가 아니라 영숫자가 아닌 경계로 구분된
`linux` token만 허용한다.

초기 Linux ID 집합:

```text
almalinux, alpine, amzn, arch, archlinux, centos, clear-linux-os,
coreos, debian, fedora, flatcar, gentoo, kali, linuxmint, mariner,
ol, opensuse, opensuse-leap, opensuse-tumbleweed, photon, rhel,
rocky, sled, sles, ubuntu
```

초기 Windows ID 집합:

```text
mswindows, windows, win32
```

ID 집합은 외부 설정이나 API 입력을 받지 않는 immutable 상수다. 알 수 없는
ID를 Linux로 추정하지 않으며 `UNSUPPORTED`로 닫힌다. 다만 QGA가
`kernel-name=Linux` 또는 `pretty-name`에 명확한 Linux 토큰을 제공하는
경우에는 신규 배포판도 안전하게 Linux adapter를 선택할 수 있다.

### 4.4 wrapper의 1회 조회와 공유

`LibvirtGetVmGuestNetworkStateCommandWrapper.GuestContext`는 기존 `osId`
문자열 대신 `QemuGuestOsFamilyResolution`을 저장한다.

```java
private QemuGuestOsFamilyResolution getOsFamily(
        GetVmGuestNetworkStateCommand command,
        Domain domain,
        GuestContext context)
```

route와 DNS가 같은 cycle에 due여도 `guest-get-osinfo`는 한 번만 호출한다.
resolver 결과를 route/DNS adapter에 공유한다.

```text
guest-info
  └─ route 또는 DNS fallback 필요
       └─ guest-get-osinfo 1회
            └─ QemuGuestOsFamilyResolver
                 ├─ LINUX  → 고정 Linux adapter
                 ├─ WINDOWS → 고정 Windows adapter
                 └─ UNSUPPORTED → guest-exec 0회
```

### 4.5 route/DNS fallback signature

`QemuGuestRouteFallback.collect()`와 `QemuGuestDnsFallback.collect()`은
임의 문자열을 직접 판별하지 않는다.

변경 전:

```java
collect(executor, String osId, int timeoutSeconds, int maxOutputBytes)
```

변경 후:

```java
collect(executor, QemuGuestOsFamilyResolution os,
        int timeoutSeconds, int maxOutputBytes)
```

각 adapter는 `os.getFamily()`에 대해서만 switch한다. 명령 경로와 인수
allowlist는 현재 코드 그대로 유지한다.

### 4.6 실패 상태와 진단

`UNSUPPORTED` 상세는 다음처럼 판별 근거를 포함한다.

```text
Unsupported guest OS for route fallback:
id=freebsd, kernel-name=-, name=FreeBSD
```

255자 section 상세 제한을 적용하고 제어 문자를 제거한다. VM 이름, 사용자
입력 또는 guest-exec 출력 전체를 오류 상세에 포함하지 않는다.

OS family 판별 실패는 section `UNSUPPORTED`다. 허용된 adapter를 선택한 뒤
실행 파일 부재, exit code, timeout 또는 파싱 실패가 발생하면 기존 정책대로
`UNAVAILABLE` 또는 마지막 성공값 `STALE`로 처리한다.

## 5. 보안 및 부하 영향

OS ID 지원 확대가 arbitrary guest-exec 허용으로 이어지지 않도록 다음을
변경 금지 항목으로 둔다.

- `/bin/sh -c`, `cmd.exe /c` 추가 금지
- 실행 파일, 인수, 파일 경로의 외부 입력 금지
- route/DNS 이외의 generic guest command API 금지
- 현재 절대 경로 및 인수 allowlist 확대 금지
- 기존 output limit, UTF-8, base64, timeout 검증 완화 금지

추가 QGA 호출은 없다. 기존에도 route/DNS fallback 전에
`guest-get-osinfo`를 한 번 실행했으며, 개선 후에도 cycle당 최대 한 번이다.
resolver는 메모리 내 상수 집합 조회이므로 Agent 부하 증가는 무시할 수 있는
수준이다.

## 6. 코드 변경 범위

| 파일 | 변경 |
|---|---|
| `QemuGuestOsInfo.java` | QGA OS 정보 immutable DTO |
| `QemuGuestOsFamily.java` | `LINUX`, `WINDOWS`, `UNSUPPORTED` enum |
| `QemuGuestOsFamilyResolution.java` | family, 판별 source, OS 정보 |
| `QemuGuestOsFamilyResolver.java` | 신규 fail-closed family resolver |
| `QemuGuestNetworkStateParser.java` | `parseOsInfo`, 호환 `parseOsId` |
| `QemuGuestRouteFallback.java` | 문자열 포함 판별 제거, enum dispatch |
| `QemuGuestDnsFallback.java` | 문자열 포함 판별 제거, enum dispatch |
| `LibvirtGetVmGuestNetworkStateCommandWrapper.java` | OS 정보 1회 조회 및 resolver 결과 공유 |
| parser/resolver/fallback/wrapper tests | 배포판 ID 및 호출 횟수 회귀 테스트 |
| QGA fixture | 비식별 Debian/Ubuntu/Rocky/CentOS/Windows OS 정보 |

Core command/answer, DAO, API response, UI 및 DB migration은 변경하지 않는다.
실환경 검증에서 section 상태 병합이 다음 수집 시각을 잘못 연장하는 별도
Backend 결함을 발견해 `VmGuestNetworkCollector`의 스케줄 기록 시점을
보정했다. 이는 수집 payload나 API 계약을 바꾸지 않는 collector 내부
정합성 수정이다.

## 7. 자동 테스트 설계

### 7.1 parser

- `id=debian`, `kernel-name` 없음
- `id=ubuntu`, `kernel-name` 없음
- `id=rocky`, `pretty-name=Rocky Linux`
- `id=centos`, `pretty-name=CentOS Linux`
- `id=mswindows`
- 빈 필드와 malformed response

### 7.2 resolver table test

| 입력 | 기대 family | 기대 source |
|---|---|---|
| `id=debian` | LINUX | `id:debian` |
| `id=ubuntu` | LINUX | `id:ubuntu` |
| `id=rocky` | LINUX | `id:rocky` |
| `id=centos` | LINUX | `id:centos` |
| unknown ID + `kernel-name=Linux` | LINUX | `kernel-name:linux` |
| unknown ID + `pretty-name=Example Linux` | LINUX | `pretty-name:linux` |
| `id=mswindows` | WINDOWS | `id:mswindows` |
| `id=freebsd` | UNSUPPORTED | `unsupported` |
| 모든 값 없음 | UNSUPPORTED | `unsupported` |

### 7.3 route/DNS

- Debian/Ubuntu resolution에서 Linux 고정 명령만 생성
- Rocky/CentOS resolution에서 동일 Linux adapter 사용
- Windows resolution에서 기존 PowerShell adapter 사용
- `UNSUPPORTED`에서는 guest-exec 호출 0
- generic shell과 비허용 경로/인수 계속 거부
- route IPv4/IPv6 JSON 파싱과 DNS source 우선순위 유지

### 7.4 wrapper

- route와 DNS가 동시에 due여도 `guest-get-osinfo` 1회
- family resolver 실패 시 두 section은 `UNSUPPORTED`, guest-exec 0회
- route 성공/DNS 성공 및 section 독립 상태
- 한 section 실패가 interface 또는 다른 section을 제거하지 않음

## 8. 구현 후 22.x preflight 및 배포 gate

### Gate A. 배포 전 실제 VM 확인

기능 flag와 exec fallback을 끈 상태에서 대상 VM에 다음을 확인한다.

1. `guest-info`의 `guest-exec`, `guest-exec-status`,
   `guest-get-osinfo`
2. `guest-get-osinfo`의 `id`, 선택적 `kernel-name`, 표시 이름
3. Linux 고정 route 명령의 exit code와 JSON validity
4. DNS source 우선순위별 실행 가능 여부
5. 출력 크기가 설정 상한 이내인지 확인

Debian과 Ubuntu 각각 한 VM이 필요하다. Ubuntu 실행 표본이 없으면 Ubuntu
실환경 gate는 미완료로 기록하고 배포 범위를 Debian 한 VM으로 제한한다.

### Gate B. 기존 jar 기반 class patch

공유 22.x에서는 신규 branch 전체 KVM jar를 교체하지 않는다.

1. 대상 host 한 대의 기존 `cloud-core`, `cloud-agent`, KVM plugin jar 백업
2. 이번 변경 class와 내부 신규 resolver/model class만 patch
3. `mold-agent.service` 재시작
4. ReadyAnswer와 기존 stats 수신 확인
5. 기능 flag는 아직 `false`

### Gate C. 단일 VM 기능 확인

host/zone/VM scope를 한 VM으로 제한하고 직렬 수집한다.

- max concurrent hosts: 1
- max concurrent VMs per host: 1
- max VMs per host cycle: 1
- route/DNS interval: 60초 이상

수락 조건:

- Debian: route `OK` 또는 `EMPTY`, DNS `OK` 또는 `EMPTY`
- Ubuntu: 실행 표본이 있을 때 동일 기준
- `Unsupported guest OS ... debian|ubuntu` 0건
- route source `guest-exec-linux-ip`
- Debian preflight 환경의 DNS source `/etc/resolv.conf`
- guest-get-osinfo cycle당 1회
- `GuestNetwork-Worker` queue overflow 0
- 기존 VM stats 및 ReadyAnswer 오류 증가 0

### Gate D. 회귀 및 rollback

- 기존 VM/볼륨/NIC p95 증가 5% 이하
- Agent/Management CPU 증가 기존 수락 기준 이내
- 문제 발생 시 기능과 exec fallback을 끄고 patch 전 jar로 복구
- DB snapshot table은 삭제하지 않음

## 9. 구현 및 22.x 검증 결과

### 9.1 구현 결과

설계한 OS 정보 DTO, family enum/resolution, fail-closed resolver를 추가하고
route/DNS fallback의 문자열 `contains("linux")` 분기를 enum dispatch로
교체했다. route와 DNS가 동시에 필요한 경우에도 `GuestContext`가
`guest-get-osinfo` 결과를 한 번만 캐시해 공유한다.

자동 테스트 결과:

| 범위 | 테스트 | 결과 |
|---|---:|---|
| KVM parser/resolver/route/DNS/wrapper | 38 | 성공 |
| Management collector | 12 | 성공 |
| 합계 | 50 | 성공 |

Checkstyle 오류는 0건이다. Debian과 Ubuntu QGA fixture를 추가했으며,
FreeBSD와 같이 지원 목록에 없는 OS는 guest-exec를 호출하지 않고
`UNSUPPORTED`로 닫히는 것을 검증했다.

### 9.2 실환경에서 추가 발견한 Backend 스케줄 결함

최초 Agent patch 후에도 route/DNS 재수집이 발생하지 않는 현상을 추적했다.
`VmGuestNetworkStateServiceImpl.mergeSections()`가 persistence 과정에서
입력 객체의 `NOT_DUE`를 이전 `UNSUPPORTED`로 병합한 뒤,
`VmGuestNetworkCollector`가 변형된 객체를 이용해 다음 실행 시각을
계산하고 있었다. 그 결과 interface-only 주기마다 route/DNS 실패 backoff가
다시 연장됐다.

collector가 persistence 호출 전에 section 상태의 얕은 복사본을 만들고,
그 원본 상태로 스케줄을 기록하도록 수정했다. 이 회귀는
`VmGuestNetworkCollectorTest`에 추가했으며, DB schema와 API 계약은
변경하지 않는다.

### 9.3 22.x Debian 단일 VM 결과

2026-07-26에 `10.10.22.3`의 기존 KVM plugin JAR에 이번 변경 class 19개만
patch하고 `mold-agent.service`를 재시작했다. 관리 서버에는 collector
class 한 개만 patch했다. 전체 Cloud 또는 Agent runtime을 교체하지 않았다.

대상 `i-2-608-VM`의 최종 snapshot:

| 항목 | 결과 |
|---|---|
| 전체 상태 | `OK` |
| interface | `OK`, 2개 |
| route | `OK`, 10개 |
| DNS | `OK`, 서버 2개 |
| DNS source | `resolv.conf` |
| upstream 확인 | `true` |

기존의 `Unsupported guest OS ... debian`은 재현되지 않았고, Agent
ReadyAnswer와 관리 서버의 8080/8250 listener 및 세 호스트 연결을 확인했다.
Ubuntu 실행 표본은 현재 클러스터에 없어 fixture/단위 테스트까지만
완료했으며 실제 Ubuntu VM gate는 후속 기능 테스트 항목으로 남긴다.

## 10. 완료 조건

구현 완료는 다음을 모두 만족할 때 선언한다.

- 문자열 `contains("linux")` OS dispatch가 production 코드에서 제거됨
- Debian/Ubuntu/Rocky/CentOS/Windows/unsupported resolver test 통과
- 기존 command allowlist 보안 test 통과
- 22.x Debian preflight와 단일 VM 수집 통과
- 22.x Ubuntu 실행 표본이 있으면 Ubuntu 수집 통과
- Ubuntu 표본이 없으면 미검증 상태와 후속 gate를 배포 보고서에 명시
- UI/API/DB 계약과 기존 executor 격리 유지
- 관련 구현계획, Phase 4/5 보고서, 파일럿 보고서, 운영 가이드 갱신

위 조건 중 Ubuntu 실환경 표본 검증을 제외한 항목을 충족했다. Ubuntu
실행 VM이 준비되면 동일한 단일 VM scope로 Gate A와 Gate C만 추가
수행한다.

## 11. 2026-07-27 Rocky 및 qemu-exec-tools 연계 보완

### 11.1 추가 원인

Rocky 9.4 대상 `i-2-379-VM`은 `guest-get-osinfo`에서 `id=rocky`를
반환하고 `guest-exec`도 enabled였다. 그러나 Host 2의 활성 KVM plugin에는
이 문서에서 구현한 `QemuGuestOsFamilyResolver` 계열 class가 없었다.

- Host 1·2: 구형 wrapper/route/DNS fallback
- Host 3: 로컬 최신 build와 관련 class SHA-256 일치

따라서 OS resolver 코드 자체뿐 아니라 모든 대상 host의 runtime class
manifest 일치가 배포 gate에 포함되어야 한다.

최신 Agent를 배포한 이후에도 Rocky의 `/usr/sbin/ip` guest-exec는
`virt_qemu_ga_t`에서 `Permission denied`가 발생한다. `/bin/true`,
`/usr/bin/cat /etc/resolv.conf`, QGA file read RPC는 성공하므로
`guest-exec enabled`를 network readiness와 동일시할 수 없다.

### 11.2 조정된 source 선택

OS family resolver는 유지하되 Linux adapter source는 다음 순서로 변경한다.

1. QGA 표준 명령
2. qemu-exec-tools `guest-network-snapshot` Helper
3. 기존 고정 `ip`/`resolvectl`/`nmcli`/`cat` fallback

Helper는 `ID`와 `ID_LIKE`를 함께 반환한다. Agent resolver는 QGA OS 정보가
우선이며 Helper OS 정보는 adapter 진단과 일치성 확인에 사용한다.

### 11.3 책임 경계

- qemu-exec-tools는 QGA 전체 RPC 허용 정책을 유지한다.
- qemu-exec-tools가 전용 Helper와 SELinux/AppArmor 준비를 담당한다.
- Cloud Agent는 게스트 policy/package를 변경하지 않는다.
- Cloud Agent는 enum 기반 Helper/legacy operation만 실행한다.

상세 class, payload, DDL, schedule 설계는
`docs/guest_network_observability_integrated_improvement_design.md`를 따른다.

### 11.4 추가 gate

- Host 1·2·3 collector class manifest 일치
- QGA `policyMode=FULL`과 network readiness 상태 분리
- Rocky SELinux enforcing에서 Helper address/routes/DNS 성공
- Helper 미설치 VM legacy fallback
- `EXEC_PERMISSION_DENIED`와 `HELPER_NOT_INSTALLED` 구조화 오류
- capability/Helper/Agent fingerprint 변경 후 실패 section 즉시 retry
