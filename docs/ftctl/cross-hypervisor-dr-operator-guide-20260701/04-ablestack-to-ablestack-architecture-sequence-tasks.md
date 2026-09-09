# ABLESTACK -> ABLESTACK DR 아키텍처, 시퀀스, 기능 및 단계별 태스크

문서 기준일: 2026-07-01  
방향: `KVM_TO_KVM`  
권장 엔진: `FTCTL_DR`

## 1. 아키텍처

ABLESTACK 운영 VM을 ABLESTACK DR site로 보호하는 방향이다. Plan과 UI를 보유한 Cloud가 원본/대상 위치와 관계없이 제어 권한을 가지며, 각 사이트 Cloud는 서명된 Mold API를 통해 로컬 Agent/FTCTL 작업과 Cloud-owned VM/볼륨 수명주기를 수행한다. RBD 경로는 Cloud가 만든 대상 이미지를 remote-NBD/librbd로 열고 전체 시드, RBD 증분, 선택형 QEMU live mirror를 수행한다. Ceph `rbd-mirror`는 사용하지 않는다.

```mermaid
flowchart LR
  UI["Cloud UI"] --> API["Cloud DR API"]
  API --> DB[("Cloud DB")]
  API --> EXEC["DrRunDispatcher"]
  EXEC --> ADP["FTCTL_DR Adapter"]
  ADP --> AGENT["Coordinator/Worker Agent"]
  AGENT --> FT["ablestack_vm_ftctl"]
  FT --> SCHED["dr_scheduler"]
  SCHED --> AD["dr_ablestack.sh"]
  AD --> SRC["Source disk<br/>RBD / block / qcow2"]
  AD --> TGT["Target disk<br/>RBD / block / qcow2"]
  AD --> CKPT["Manifest / Checkpoint"]
  FT --> STATUS["dr-status"]
  STATUS --> PROJ["Cloud Projection"]
  PROJ --> DB
  DB --> UI
```

## 2. 기능 범위

| 기능 | 현재 흐름 |
| --- | --- |
| 보호 설정 | ABLESTACK source VM과 target disk mapping을 Plan에 저장 |
| Target 준비 | target RBD create, block size check, qcow2 create를 수행 |
| Sync 시작 | `dr-sync-start` 후 scheduler가 checkpoint loop를 시작 |
| 지속 복제 | 기본은 RBD snapshot/diff 증분이며, preflight가 통과하면 QEMU live mirror 기반 near-real-time 모드를 선택할 수 있다. |
| Restore Point | full seed 완료 시 manifest/checkpoint와 RPO projection 생성 |
| Test Failover | target-ready restore point로 test artifact/session 생성 |
| Failover | final checkpoint 선택 후 active side를 TARGET으로 전환 |
| Failback | reverse profile로 target -> source checkpoint 수행 |
| Reprotect | reverse direction으로 지속 복제를 재개 |

## 3. 시퀀스

```mermaid
sequenceDiagram
  participant UI as Cloud UI
  participant API as Cloud API
  participant DB as Cloud DB
  participant Agent as KVM Agent
  participant FT as ftctl
  participant Driver as ABLESTACK driver
  participant Disk as Source/Target disks

  UI->>API: createDrPlan(KVM_TO_KVM, FTCTL_DR)
  API->>DB: plan, mapping, worker 저장
  UI->>API: startDrSync
  API->>DB: dr_run SYNC/QUEUED
  API-->>UI: accepted run
  API->>Agent: FtctlDrActionCommand(SYNC)
  Agent->>FT: dr-sync-start --wait=false
  FT->>Driver: target prepare
  Driver->>Disk: RBD/file/block target 생성 또는 검증
  FT->>Driver: replication cycle
  Driver->>Disk: qemu-img convert source -> target
  Driver-->>FT: manifest/checkpoint
  API->>Agent: dr-status
  Agent-->>API: RPO/progress/state
  API->>DB: projection update
  DB-->>UI: 최신 상태 표시
```

## 4. 단계별 태스크

| 단계 | Operator/UI 태스크 | Backend/Agent/ftctl 태스크 |
| --- | --- | --- |
| 1. Site 등록 | 운영 ABLESTACK site와 DR ABLESTACK site 등록. UI에서 각 Mold API URL/API Key/Secret Key 입력 | `dr_site.hypervisorType=KVM`, `dr_site_credential.credential_type=MOLD_API` |
| 2. Plan 생성 | `KVM_TO_KVM`, `FTCTL_DR`, source VM, RPO/RTO 입력 | topology와 duplicate plan 검증 |
| 3. Disk mapping | source disk path와 target path/type/format/size 지정 | `dr_ablestack_canonicalize_profile`로 disk map 생성 |
| 4. Worker 지정 | source/target/coordinator host 지정 | coordinator가 Agent command를 받음 |
| 5. Sync 시작 | Start Sync | target disk 준비, scheduler 시작 |
| 6. Checkpoint 확인 | UI에서 restore point와 RPO 확인 | 매 cycle manifest/checkpoint 생성 |
| 7. Test Failover | target restore point로 테스트 | test session/artifact 기록 |
| 8. Failover | planned/disaster 선택 | planned는 final checkpoint 후 TARGET active |
| 9. Failback | source 복구 후 failback | reverse profile, reverse checkpoint, SOURCE active |
| 10. Reprotect/Release | 재보호 또는 보호 해제 | scheduler control 또는 runtime release |

## 5. RPO/RTO 분석

| 항목 | 분석 |
| --- | --- |
| RPO | 기본 모드는 마지막 durable RBD 증분 checkpoint를 기준으로 측정한다. near-real-time 모드는 write mirror와 주기적 durable checkpoint를 결합하며 zero-RPO로 표기하지 않는다. |
| RTO | target disk가 준비되어 있고 restore point가 최신이면 failover 상태 전환은 빠르지만, 실제 VM lifecycle hook이 없으면 전원/등록 단계가 별도 작업으로 남는다. |
| 개선 포인트 | RBD baseline을 항상 하나 보존하고 새 checkpoint가 durable한 뒤 교체한다. live mirror는 capability와 네트워크 preflight 통과 시에만 제공한다. |

## 6. 소스 검토 결과와 운영 전 확인 사항

| 항목 | 판정 |
| --- | --- |
| UI/API/Run/Agent/ftctl 제어 경로 | 연결됨 |
| ABLESTACK target disk 준비 | 구현됨. RBD/file/block target 준비 경로가 있다. |
| 지속 복제 | 부분 구현. scheduler는 반복 실행되지만 현재 ABLESTACK cycle은 full seed copy이다. |
| 실제 VM lifecycle | runtime은 상태 projection 중심이며 실제 target VM power-on/provision hook은 운영 통합 필요 |
| 흐름 단절 위험 | disk mapping 누락 시 `WAITING_FOR_DISK_MAP`, full seed 장기화 시 RPO 악화 |
