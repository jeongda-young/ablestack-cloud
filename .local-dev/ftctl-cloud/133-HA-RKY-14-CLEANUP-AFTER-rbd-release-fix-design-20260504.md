# 133. HA-RKY-14 CLEANUP-AFTER RBD release fix design

## 배경

- HA-RKY-14 보호 해제 재시험에서 standby root volume은 expunge 되었지만 data volume expunge가 실패했다.
- 실패 시점의 data RBD image에는 watcher가 남아 있었고, primary host 쪽에 `/dev/rbd...` 장치가 계속 map된 상태였다.
- Cloud의 `releaseFtctlProtection`은 qemu/ftctl `UNPROTECT` 성공 후 곧바로 standby VM과 secondary volume expunge를 수행한다.

## 원인 판단

- qemu/ftctl `UNPROTECT` 경로는 `query-block-jobs`로 job을 찾은 뒤 `block-job-cancel force=true`만 전송하고 즉시 runtime state를 삭제했다.
- `block-job-cancel`은 취소 요청이며, QEMU block graph에서 destination RBD 참조가 사라졌다는 완료 보장이 아니다.
- shared-blockcopy 시작 시 ftctl이 destination `/dev/rbd/...`를 직접 `rbd map`하지만, 기존 `UNPROTECT`에는 해당 destination을 다시 unmap하는 대칭 처리가 없었다.
- runtime state 삭제가 먼저 수행되면 `.state.blockcopy`에 있던 target/destination 정보가 사라져 사후 release 검증도 불가능하다.
- 따라서 Cloud가 volume expunge를 시작할 때 QEMU 또는 kernel RBD가 data destination image를 아직 잡고 있을 수 있고, 이번 실패 증상과 일치한다.

## 수정 방향

### qemu/ftctl

1. `UNPROTECT` 시작 시 `.state.blockcopy`와 `.state.blockcopy.reverse`의 destination 목록을 보존한다.
2. `block-job-cancel` 전송 후 `query-block-jobs`가 비워질 때까지 대기한다.
3. `query-named-block-nodes`에서 destination RBD 경로 참조가 사라질 때까지 대기한다.
4. destination이 `/dev/rbd/...`이면 local krbd 장치를 `rbd unmap`하고 장치가 사라질 때까지 확인한다.
5. 위 release 검증이 끝난 뒤에만 runtime state 파일을 삭제한다.
6. 각 단계는 Cloud Event 추적을 위해 ftctl event log에 남긴다.

### Cloud

1. `UNPROTECT`는 blockjob/RBD release까지 기다리는 장기 작업이므로 agent command wait 값을 기본 60초보다 길게 설정한다.
2. qemu/ftctl `UNPROTECT`가 실패하면 Cloud는 standby VM/volume expunge로 넘어가지 않는다.
3. 성공 응답 이후에만 기존 cleanup 흐름을 진행한다.

## 검증 계획

- qemu selftest에 `UNPROTECT`가 blockjob cancel 이후 `query-block-jobs`, `query-named-block-nodes` release 확인을 거친 뒤 state를 삭제하는 테스트를 추가한다.
- Cloud unit test에서 `releaseFtctlProtection`의 `UNPROTECT` command wait 값이 release 대기 시간에 맞게 설정되는지 확인한다.
- 빌드 후 HA-RKY-14를 깨끗한 상태에서 다시 수행하여 root/data secondary volume이 모두 expunge되고 protection row가 제거되는지 확인한다.
