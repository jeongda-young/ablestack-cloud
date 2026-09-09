# FTCTL instance_name 및 transient standby check 보정 구현 - 2026-05-01

## 배경

3단계 재시험 후 다음 보완점이 확인됐다.

- virsh domain 조회는 Cloud `display_name`이 아니라 `instance_name` 기준이어야 한다.
- Cloud-managed standby VM은 Cloud UI/DB에는 존재하지만 `Stopped` 상태이고, ablestack-cloud VM은 기본적으로 transient이므로 stopped 상태에서는 peer libvirt domain으로 조회되지 않을 수 있다.
- 기존 FTCTL `check`는 peer에서 primary VM 이름으로 `dominfo`를 수행해 `inventory_result=warn`, `peer_rc=1`을 반환했다.

## 반영 내용

### ablestack-cloud

- primary VM에 대한 FTCTL 명령은 기존처럼 `userVm.getInstanceName()`을 사용한다.
- Cloud-managed standby VM의 FTCTL runtime 이름은 `standbyVm.getInstanceName()`을 사용하도록 변경했다.
- KVM wrapper가 `FtctlSyncProfileCommand`의 `provisioningBackend`, `provisioningState`를 FTCTL CLI에 전달하도록 변경했다.
- `getFtctlCheck` 응답에 다음 필드를 추가했다.
  - `peerdomainexpected`
  - `standbydomainstate`
  - `provisioningbackend`

### ablestack-qemu-exec-tools

- FTCTL profile에 다음 필드를 저장/표시하도록 추가했다.
  - `FTCTL_PROFILE_PROVISIONING_BACKEND`
  - `FTCTL_PROFILE_PROVISIONING_STATE`
- `config profile-upsert`에 다음 옵션을 추가했다.
  - `--provisioning-backend`
  - `--provisioning-state`
- status state에 `provisioning_backend`, `provisioning_state`를 포함하도록 변경했다.
- `cloud-managed` + `prepared-transient` 상태에서 peer domain 미존재를 예상 상태로 해석하도록 `check`를 보정했다.

## 보정된 check/status 예시

```json
{
  "command": "check",
  "vm": "i-2-281-VM",
  "result": "ok",
  "inventory_result": "ok",
  "primary_rc": 0,
  "peer_rc": 1,
  "peer_domain_expected": false,
  "standby_domain_state": "not-defined-expected",
  "provisioning_backend": "cloud-managed"
}
```

`peer_rc=1`은 raw probe 결과로 유지하지만, `inventory_result=ok`와 `peer_domain_expected=false`를 함께 반환해 Cloud-managed transient standby에서 정상적으로 예상되는 상태임을 표현한다.

status 예시:

```json
{
  "provisioning_backend": "cloud-managed",
  "provisioning_state": "Ready",
  "standby_state": "prepared-transient",
  "standby_domain_state": "not-defined-expected",
  "peer_domain_expected": "false"
}
```

## 검증

로컬 Maven/RPM 빌드는 실행하지 않았다.

수행한 빠른 검증:

- FTCTL shell 문법 검사:
  - `bash -n bin/ablestack_vm_ftctl.sh lib/ftctl/profile.sh lib/ftctl/inventory.sh lib/ftctl/orchestrator.sh lib/ftctl/standby.sh lib/ftctl/state.sh`
- ablestack-cloud 변경 파일 whitespace 검사:
  - `git diff --check`
- WSL 임시 FTCTL config와 fake `virsh`를 사용한 cloud-managed transient check/status 미니 테스트:
  - `inventory_result=ok`
  - `peer_rc=1`
  - `peer_domain_expected=false`
  - `standby_domain_state=not-defined-expected`
  - `provisioning_backend=cloud-managed`

참고:

- 전체 `ablestack_vm_ftctl_selftest.sh`는 기존 shellcheck 경고 때문에 shellcheck 단계에서 중단됐다.
- 해당 경고는 이번 변경과 직접 관련 없는 기존 shellcheck 항목을 포함한다.

## 후속 필요 작업

- GitHub Actions 빌드 산출물로 Cloud/FTCTL 패키지를 생성한다.
- 테스트 서버에 Actions 산출물을 설치한다.
- 3단계 재시험을 반복해 FTCTL profile의 `secondary_vm_name`이 Cloud `instance_name`(`i-...-VM`)으로 들어가는지 확인한다.
- `getFtctlCheck` 응답에서 `inventoryresult=ok`, `peerdomainexpected=false`, `standbydomainstate=not-defined-expected`가 UI에 표시되는지 확인한다.
