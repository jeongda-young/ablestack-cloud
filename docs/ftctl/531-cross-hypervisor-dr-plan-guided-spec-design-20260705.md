# Cross Hypervisor DR Plan Guided Spec Design

작성일: 2026-07-05

## 1. 배경

현재 DR Plan 생성/수정 대화상자는 `고급 엔진 설정` 영역에서 다음 값을 직접 입력받는다.

- `enginetype`
- `enginebindingtype`
- `enginebindingid`
- `coordinatorworkerhostid`
- `sourceworkerhostid`
- `targetworkerhostid`
- `mappingjson`
- `schedulejson`
- `policyjson`
- `quiescepolicyjson`

이 값들은 사용자가 이해해야 하는 업무 용어가 아니라 Cloud backend, Agent, ftctl DR engine이 공유하는 실행 계약이다. 특히 `mappingjson`, `schedulejson`, `policyjson`, `quiescepolicyjson`은 사용자가 직접 작성하기 어렵고, 잘못 작성해도 현재 UI는 JSON 구문만 검증한다. 그러나 실제 실행 경로에서는 worker host와 disk/resource mapping이 없으면 FTCTL_DR 준비/실행이 실패한다.

따라서 DR Plan UI는 raw JSON 입력 화면이 아니라 다음 흐름으로 변경한다.

1. 사용자는 site, source workload, target resource, RPO/RTO, consistency, 정책을 선택한다.
2. UI는 Cloud API를 호출해 inventory와 preflight 결과를 받는다.
3. Cloud backend가 typed 입력값과 inventory 결과로 canonical plan spec JSON을 생성한다.
4. Agent/ftctl은 기존처럼 canonical profile JSON을 받되, UI raw JSON을 신뢰하지 않는다.
5. raw JSON은 기본 UI에서 숨기고, 운영자/개발자용 expert preview로만 제공한다.

## 2. 현재 구현 기준

| 계층 | 현재 상태 | 문제 |
| --- | --- | --- |
| UI | `DrPlanList.vue`가 고급 collapse 안에서 raw JSON textarea와 worker host input을 표시 | 사용자에게 내부 계약을 직접 입력하게 함 |
| UI validation | `mappingjson`, `schedulejson`, `policyjson`, `quiescepolicyjson`을 `JSON.parse`만 수행 | 방향별 필수 resource mapping 누락을 사전에 막지 못함 |
| API | `CreateDrPlanCmd`, `UpdateDrPlanCmd`가 engine/binding/worker/json 필드를 직접 받음 | 외부 API surface가 내부 profile 구조에 결합됨 |
| Inventory API | `DrPlanInventoryResult`, `DrPlanInventoryResponse`는 현재 `sourceworkloads` 중심 | target datastore/network/storage/worker 후보를 선택 UI에 제공하지 못함 |
| Backend validation | `DrPlanServiceImpl`은 JSON 구문과 topology 중심 검증 | mapping 의미 검증, schedule/policy/quiesce schema 검증 부족 |
| Orchestrator | `DrProtectionOrchestratorImpl`은 FTCTL_DR sync 준비 시 worker와 disk mapping을 요구 | 사용자가 raw JSON을 잘못 작성하면 실행 시점에 실패 |
| Adapter | `FtctlDrUnifiedActionAdapter`가 plan JSON을 profile의 `mapping`, `schedule`, `policy`, `quiescePolicy`로 전달 | profile 계약은 필요하지만 UI 입력 포맷으로 노출되면 안 됨 |
| DB | `dr_plan`에 JSON column 보유 | canonical 실행 spec 저장 위치로는 유지 가능 |

## 3. 목표 UX

DR Plan 생성/수정 대화상자는 다음 영역으로 재구성한다.

| 영역 | 사용자 입력 | 생성되는 내부 값 |
| --- | --- | --- |
| 기본 정보 | 이름, 설명 | `name`, `description` |
| 사이트 매핑 | 원본 사이트, 대상 사이트, 자동 방향 | `source_site_id`, `target_site_id`, `direction`, `engine_type=FTCTL_DR` |
| 보호 대상 | 원본 VM/workload 선택 | `source_vm_id` 또는 `source_external_ref` |
| 대상 리소스 | 대상 VM 이름, datastore/storage pool, compute, network | `mapping_json` |
| 복구 목표 | RPO, RTO, sync interval, retention | `rpo_seconds`, `rto_seconds`, `schedule_json` |
| 일관성 | Crash-consistent, Guest quiesce, App-consistent | `quiesce_policy_json` |
| 정책 | test network isolation, failover power-on, retry, bandwidth limit | `policy_json` |
| 검토 | backend-generated summary, warnings, generated JSON preview | 저장 전 preflight 결과 |

기본 UI에서 제거한다.

- engine type select
- engine binding type/id input
- worker host 숫자/UUID 직접 입력
- mapping/schedule/policy/quiesce raw JSON textarea

expert mode에서만 표시한다.

- canonical JSON preview
- read-only generated profile summary
- 운영자 권한이 있고 feature flag가 켜진 경우 raw JSON override

## 4. UI 상세 설계

### 4.1 `DrPlanList.vue` form state 변경

현재 `defaultCreateForm()`의 raw JSON 중심 필드는 유지하되 UI 내부 저장값으로 격하한다. 기본 사용자 입력값은 typed field로 분리한다.

```js
defaultCreateForm () {
  return {
    name: '',
    description: '',
    sourcesiteid: undefined,
    targetsiteid: undefined,
    direction: '',
    sourceworkloadvalue: undefined,
    sourcevmid: undefined,
    sourceexternalref: '',
    targetvmname: '',
    targetstorageoption: undefined,
    targetcomputeoption: undefined,
    targetnetworkoption: undefined,
    sourceworkerhostid: undefined,
    targetworkerhostid: undefined,
    coordinatorworkerhostid: undefined,
    rposeconds: 300,
    rtoseconds: 300,
    syncintervalseconds: 300,
    retentioncount: 24,
    consistencymode: 'CRASH_CONSISTENT',
    testnetworkmode: 'ISOLATED',
    failoverpoweron: true,
    bandwidthlimitmbps: undefined,
    retrycount: 3,
    startsync: false,
    expertmode: false,
    mappingjson: '',
    schedulejson: '',
    policyjson: '',
    quiescepolicyjson: ''
  }
}
```

## 2026-07-07 Update: VMware Source Disk Size Is Mandatory For VMWARE_TO_KVM

Follow-up validation for plan `05527cbe-974e-4ca8-b65e-f844cb3420e7` showed
that the guided spec can still build an executable-looking plan when the VMware
source disk is selected but its size is unresolved. The generated
`vmware-disks.json` and `ablestack-disks.json` then carry `sizeBytes=0`, and
FTCTL correctly fails with `DR_TARGET_DISK_SIZE_UNRESOLVED`.

The guided spec contract is therefore tightened:

- `discoverDrPlanInventory` must return positive `capacityBytes`/`sizeBytes`
  for every selected VMware source disk, or return a blocking reason such as
  `SOURCE_DISK_SIZE_UNRESOLVED:<index>`.
- `DrPlanGuidedSpecBuilder` must not serialize `sizeBytes=0` as an executable
  disk. A missing or zero size becomes a blocking reason.
- `DrPlanTargetPlacementResolverImpl` must normalize KVM target disk metadata
  from storage and disk offering selections and must produce canonical
  `targetType`/`targetFormat` values.
- `previewDrPlanSpec` must expose `executionready=false` and disk-level
  readiness diagnostics when size/type/storage cannot be resolved.
- `createDrPlan(startsync=true)` and `updateDrPlan` for executable plans must
  call the same validator and reject unsafe mappings before agent dispatch.

Detailed code-level design is maintained in
`538-cross-hypervisor-dr-vmware-to-kvm-disk-size-and-projection-hardening-design-20260707.md`.

## 2026-07-07 Update: VMware Disk Detail Inventory Must Precede Guided Mapping

Follow-up validation showed that vCenter can return disk list items without
capacity or backing data. For example, `/rest/vcenter/vm/{vm}/hardware/disk`
can return only `disk=2000`, while
`/rest/vcenter/vm/{vm}/hardware/disk/2000` returns `capacity`, `label`, `type`,
and `backing.vmdk_file`.

The guided spec flow is therefore updated:

1. UI selects source workload.
2. UI calls `discoverDrPlanInventory(includedisks=true)`.
3. Backend reads VMware disk list, then enriches every disk through disk detail
   lookup.
4. API returns `sourcedisks[].details.capacityBytes`,
   `sourcedisks[].details.sizeBytes`, `sourcedisks[].details.path`, and
   `sourcedisks[].details.diskRef`.
5. UI builds disk rows only from detail-enriched inventory.
6. `previewDrPlanSpec`, `createDrPlan`, and `updateDrPlan` reject executable
   specs when any VMware source disk still has unresolved size.

Layer rule:

| Layer | Rule |
| --- | --- |
| UI | Do not infer size from a disk key. Show disk key as ID only. |
| API | Return detail-enriched disk inventory or disk-level blockers. |
| Backend | Own vCenter disk detail lookup and size/path normalization. |
| Agent | Receive only backend-approved executable profiles. |
| ftctl | Keep final guard for stale or unsafe profiles. |
| DB | Persist positive source disk size in canonical `mapping_json`. |

The storage-default UX remains governed by
`539-cross-hypervisor-dr-plan-storage-default-and-modal-layout-design-20260707.md`:
top-level target storage is a default/fallback, and per-disk target storage is
authoritative.

### 4.2 신규 UI 컴포넌트

DR Plan 대화상자가 계속 커지는 것을 막기 위해 다음 컴포넌트로 분리한다.

| 컴포넌트 | 역할 |
| --- | --- |
| `DrPlanSourceSelector.vue` | source site와 workload 선택 |
| `DrPlanTargetMapping.vue` | 방향별 target storage/compute/network 선택 |
| `DrPlanPolicyFields.vue` | RPO/RTO, schedule, retention, failover policy 선택 |
| `DrPlanConsistencyFields.vue` | consistency/quiesce mode 선택 |
| `DrPlanSpecPreview.vue` | backend-generated spec 요약, warnings, expert JSON preview |

`DrPlanList.vue`는 modal shell, API 호출, submit orchestration만 담당한다.

### 4.3 target mapping UI

방향별로 필요한 선택지를 다르게 표시한다.

| 방향 | 필수 target 입력 |
| --- | --- |
| `KVM_TO_KVM` | target storage pool, target network, target service offering, worker host |
| `KVM_TO_VMWARE` | datastore, resource pool/cluster, folder, portgroup, target VM name |
| `VMWARE_TO_VMWARE` | datastore, resource pool/cluster, folder, portgroup, target VM name |
| `VMWARE_TO_KVM` | target zone/cluster/host, storage pool, network, service offering |

source workload 선택 이후 backend inventory가 source disk/NIC 목록을 제공하면 UI는 disk/NIC별 row mapping을 표시한다.

```js
targetMappings: {
  disks: [
    {
      sourceDiskRef: 'disk-1000',
      sourceLabel: 'Hard disk 1',
      sizeBytes: 107374182400,
      targetStorageRef: 'datastore-23',
      targetDiskRef: '',
      provisioning: 'THIN'
    }
  ],
  networks: [
    {
      sourceNicRef: 'nic-4000',
      sourceNetworkName: 'VM Network',
      targetNetworkRef: 'dvportgroup-21',
      connectMode: 'DISCONNECTED',
      macPolicy: 'GENERATE'
    }
  ]
}
```

### 4.4 submit payload 생성

UI는 raw JSON을 직접 만들지 않고 typed field를 payload로 보낸다. 단, backend 전환 기간에는 `generated*json` preview를 받아 hidden field에 보관할 수 있다.

```js
buildPlanPayload () {
  return {
    name: this.createForm.name,
    description: this.createForm.description,
    sourcesiteid: this.createForm.sourcesiteid,
    targetsiteid: this.createForm.targetsiteid,
    direction: this.createForm.direction,
    sourcevmid: this.createForm.sourcevmid,
    sourceexternalref: this.createForm.sourceexternalref,
    targetvmname: this.createForm.targetvmname,
    targetstorageoption: this.createForm.targetstorageoption,
    targetcomputeoption: this.createForm.targetcomputeoption,
    targetnetworkoption: this.createForm.targetnetworkoption,
    sourceworkerhostid: this.createForm.sourceworkerhostid,
    targetworkerhostid: this.createForm.targetworkerhostid,
    coordinatorworkerhostid: this.createForm.coordinatorworkerhostid,
    rposeconds: this.createForm.rposeconds,
    rtoseconds: this.createForm.rtoseconds,
    syncintervalseconds: this.createForm.syncintervalseconds,
    retentioncount: this.createForm.retentioncount,
    consistencymode: this.createForm.consistencymode,
    testnetworkmode: this.createForm.testnetworkmode,
    failoverpoweron: this.createForm.failoverpoweron,
    bandwidthlimitmbps: this.createForm.bandwidthlimitmbps,
    retrycount: this.createForm.retrycount,
    startsync: this.createForm.startsync
  }
}
```

### 4.5 expert JSON 정책

기본 검증에서 `validatePlanJsonFields()`를 제거한다. 대신 다음 함수로 변경한다.

```js
validateExpertJsonFields () {
  if (!this.createForm.expertmode) return ''
  return [
    ['mappingjson', 'label.dr.mapping.json'],
    ['schedulejson', 'label.dr.schedule.json'],
    ['policyjson', 'label.dr.policy.json'],
    ['quiescepolicyjson', 'label.dr.quiesce.policy.json']
  ].map(([field, label]) => this.validateJsonField(field, label)).find(Boolean) || ''
}
```

expert mode가 꺼져 있으면 JSON textarea는 렌더링하지 않는다. submit payload에도 raw JSON을 포함하지 않는다.

## 5. API 상세 설계

### 5.1 `discoverDrPlanInventory`

현재 응답은 `sourceworkloads` 중심이다. 다음 response field를 추가한다.

```java
public class DrPlanInventoryResponse extends BaseResponse {
    @SerializedName("sourceworkloads")
    private List<DrInventoryOptionResponse> sourceWorkloads;

    @SerializedName("sourcedisks")
    private List<DrInventoryOptionResponse> sourceDisks;

    @SerializedName("sourcenetworks")
    private List<DrInventoryOptionResponse> sourceNetworks;

    @SerializedName("sourceworkerhosts")
    private List<DrInventoryOptionResponse> sourceWorkerHosts;

    @SerializedName("targetworkerhosts")
    private List<DrInventoryOptionResponse> targetWorkerHosts;

    @SerializedName("coordinatorworkerhosts")
    private List<DrInventoryOptionResponse> coordinatorWorkerHosts;

    @SerializedName("targetstorageoptions")
    private List<DrInventoryOptionResponse> targetStorageOptions;

    @SerializedName("targetcomputeoptions")
    private List<DrInventoryOptionResponse> targetComputeOptions;

    @SerializedName("targetnetworkoptions")
    private List<DrInventoryOptionResponse> targetNetworkOptions;

    @SerializedName("targetfolderoptions")
    private List<DrInventoryOptionResponse> targetFolderOptions;

    @SerializedName("enginetype")
    private String engineType;

    @SerializedName("warnings")
    private List<String> warnings;
}
```

`DrPlanInventoryResult`도 동일한 list를 보유한다.

```java
public class DrPlanInventoryResult {
    private List<DrInventoryOption> sourceWorkloads = new ArrayList<>();
    private List<DrInventoryOption> sourceDisks = new ArrayList<>();
    private List<DrInventoryOption> sourceNetworks = new ArrayList<>();
    private List<DrInventoryOption> sourceWorkerHosts = new ArrayList<>();
    private List<DrInventoryOption> targetWorkerHosts = new ArrayList<>();
    private List<DrInventoryOption> coordinatorWorkerHosts = new ArrayList<>();
    private List<DrInventoryOption> targetStorageOptions = new ArrayList<>();
    private List<DrInventoryOption> targetComputeOptions = new ArrayList<>();
    private List<DrInventoryOption> targetNetworkOptions = new ArrayList<>();
    private List<DrInventoryOption> targetFolderOptions = new ArrayList<>();
    private String engineType;
    private List<String> warnings = new ArrayList<>();
}
```

### 5.2 `previewDrPlanSpec`

생성 전에 UI가 backend-generated spec을 확인할 수 있도록 신규 async API를 추가한다.

| 항목 | 값 |
| --- | --- |
| API name | `previewDrPlanSpec` |
| Command | `PreviewDrPlanSpecCmd` |
| Service | `DrPlanSpecService.preview()` |
| Response | `DrPlanSpecPreviewResponse` |
| 동기/비동기 | async, inventory/preflight가 외부 endpoint를 호출할 수 있으므로 UI blocking 금지 |

요청 parameter는 `createDrPlan`의 typed parameter와 동일하되 DB insert를 하지 않는다.

```java
@APICommand(
    name = PreviewDrPlanSpecCmd.APINAME,
    responseObject = DrPlanSpecPreviewResponse.class,
    authorized = {RoleType.Admin})
public class PreviewDrPlanSpecCmd extends BaseAsyncCmd {
    public static final String APINAME = "previewDrPlanSpec";

    @Inject
    private DrPlanSpecService drPlanSpecService;

    @Parameter(name = "sourcesiteid", type = CommandType.UUID, entityType = DrSiteResponse.class, required = true)
    private Long sourceSiteId;

    @Parameter(name = "targetsiteid", type = CommandType.UUID, entityType = DrSiteResponse.class, required = true)
    private Long targetSiteId;

    @Parameter(name = "sourcevmid", type = CommandType.UUID, entityType = UserVmResponse.class)
    private Long sourceVmId;

    @Parameter(name = "sourceexternalref", type = CommandType.STRING)
    private String sourceExternalRef;

    @Parameter(name = "targetvmname", type = CommandType.STRING)
    private String targetVmName;

    @Parameter(name = "targetstorageoption", type = CommandType.STRING)
    private String targetStorageOption;

    @Parameter(name = "targetcomputeoption", type = CommandType.STRING)
    private String targetComputeOption;

    @Parameter(name = "targetnetworkoption", type = CommandType.STRING)
    private String targetNetworkOption;

    @Parameter(name = "syncintervalseconds", type = CommandType.INTEGER)
    private Integer syncIntervalSeconds;

    @Parameter(name = "retentioncount", type = CommandType.INTEGER)
    private Integer retentionCount;

    @Parameter(name = "consistencymode", type = CommandType.STRING)
    private String consistencyMode;

    @Parameter(name = "testnetworkmode", type = CommandType.STRING)
    private String testNetworkMode;
}
```

### 5.3 `createDrPlan` / `updateDrPlan`

기존 raw JSON parameter는 backward compatibility와 expert override 용도로 유지하되 기본 UI는 사용하지 않는다.

신규 typed parameter:

| Parameter | 저장/생성 대상 |
| --- | --- |
| `targetvmname` | `mapping_json.targetVmName` |
| `targetstorageoption` | `mapping_json.storage` 또는 disk mapping |
| `targetcomputeoption` | `mapping_json.compute` |
| `targetnetworkoption` | `mapping_json.network` 또는 NIC mapping |
| `syncintervalseconds` | `schedule_json.intervalSeconds` |
| `retentioncount` | `schedule_json.retention.count` |
| `consistencymode` | `quiesce_policy_json.mode` |
| `testnetworkmode` | `policy_json.testNetwork.mode` |
| `failoverpoweron` | `policy_json.failover.powerOn` |
| `bandwidthlimitmbps` | `policy_json.transport.bandwidthLimitMbps` |
| `retrycount` | `policy_json.retry.count` |

엔진 관련 parameter 처리 규칙:

- `enginetype`이 비어 있으면 backend가 site hypervisor와 direction으로 `FTCTL_DR`을 결정한다.
- `enginebindingtype`은 기본적으로 `enginetype`과 동일하게 backend가 채운다.
- `enginebindingid`는 기존 engine resource import/adopt 시나리오가 아니면 null이다.
- 일반 create/update UI는 engine binding 값을 보내지 않는다.

## 6. Backend 상세 설계

### 6.1 package 구조

신규 package:

`com.cloud.dr.plan.spec`

| class | 역할 |
| --- | --- |
| `DrPlanSpecService` | preview/create/update에서 spec build와 validation을 제공하는 service contract |
| `DrPlanSpecServiceImpl` | site, inventory, typed request를 조합해 canonical JSON 생성 |
| `DrPlanSpecRequest` | API command typed parameter DTO |
| `DrPlanSpec` | mapping/schedule/policy/quiesce canonical spec aggregate |
| `DrPlanSpecBuilder` | direction별 builder dispatch |
| `DrPlanSpecValidator` | 공통 validator interface |
| `FtctlDrPlanSpecValidator` | FTCTL_DR profile schema와 필수 worker/disk mapping 검증 |
| `VmwareTargetPlanSpecValidator` | datastore/resource pool/folder/network 필수 검증 |
| `KvmTargetPlanSpecValidator` | zone/cluster/storage/network/service offering 필수 검증 |

### 6.2 `DrPlanSpec`

```java
public class DrPlanSpec {
    private String engineType;
    private String engineBindingType;
    private Long engineBindingId;
    private Long sourceWorkerHostId;
    private Long targetWorkerHostId;
    private Long coordinatorWorkerHostId;
    private JsonObject mapping;
    private JsonObject schedule;
    private JsonObject policy;
    private JsonObject quiescePolicy;
    private List<String> warnings = new ArrayList<>();
}
```

### 6.3 `DrPlanSpecBuilder`

```java
public DrPlanSpec build(DrPlanSpecRequest request) {
    DrSiteVO sourceSite = requireSite(request.getSourceSiteId());
    DrSiteVO targetSite = requireSite(request.getTargetSiteId());
    String direction = resolveDirection(sourceSite, targetSite);
    String engineType = resolveEngineType(sourceSite, targetSite, direction);

    DrPlanSpec spec = new DrPlanSpec();
    spec.setEngineType(engineType);
    spec.setEngineBindingType(engineType);
    spec.setSourceWorkerHostId(resolveSourceWorker(request, sourceSite));
    spec.setTargetWorkerHostId(resolveTargetWorker(request, targetSite));
    spec.setCoordinatorWorkerHostId(resolveCoordinatorWorker(request, spec));
    spec.setMapping(buildMapping(request, sourceSite, targetSite, direction));
    spec.setSchedule(buildSchedule(request));
    spec.setPolicy(buildPolicy(request));
    spec.setQuiescePolicy(buildQuiescePolicy(request));

    validatorFor(direction, engineType).validate(spec, request);
    return spec;
}
```

### 6.4 canonical mapping JSON

FTCTL_DR profile에 전달할 `mapping_json`은 방향에 상관없이 다음 최상위 구조를 따른다.

```json
{
  "schemaVersion": "dr-plan-mapping/v1",
  "direction": "KVM_TO_VMWARE",
  "targetVmName": "prod-vm-dr",
  "target": {
    "siteId": "target-site-uuid",
    "hypervisor": "VMWARE",
    "computeRef": "resgroup-42",
    "storageRef": "datastore-23",
    "networkRef": "dvportgroup-11",
    "folderPath": "/ABLESTACK-DR"
  },
  "disks": [
    {
      "label": "root",
      "sourceVolumeId": 100,
      "sourceRef": "volume-uuid",
      "targetRef": "datastore-23/root.vmdk",
      "format": "VMDK",
      "sizeBytes": 107374182400
    }
  ],
  "networks": [
    {
      "sourceRef": "nic-1",
      "targetRef": "dvportgroup-11",
      "connectMode": "DISCONNECTED",
      "macPolicy": "GENERATE"
    }
  ]
}
```

`DrProtectionOrchestratorImpl.parseDiskMappings()`가 이미 `disks`, `diskMappings`, `volumes`, `volumeMappings`를 읽으므로, 신규 builder는 기본적으로 `disks`를 사용한다.

### 6.5 schedule JSON

```json
{
  "schemaVersion": "dr-plan-schedule/v1",
  "intervalSeconds": 300,
  "retention": {
    "count": 24,
    "minimumReadyPoints": 1
  },
  "startImmediately": true
}
```

규칙:

- `intervalSeconds`는 `rpoSeconds`보다 클 수 없다. 클 경우 warning이 아니라 hard blocker로 처리한다.
- engine이 schedule을 실제로 소비하지 않는 단계에서는 UI에 세부 schedule 항목을 숨기고 `rpoSeconds` 기반 기본값만 생성한다.

### 6.6 policy JSON

```json
{
  "schemaVersion": "dr-plan-policy/v1",
  "testNetwork": {
    "mode": "ISOLATED"
  },
  "failover": {
    "powerOn": true,
    "fenceSource": true
  },
  "retry": {
    "count": 3,
    "backoffSeconds": 30
  },
  "transport": {
    "bandwidthLimitMbps": 0
  }
}
```

규칙:

- `bandwidthLimitMbps=0`은 제한 없음이다.
- `testNetwork.mode=PRODUCTION`은 별도 acknowledgement가 없으면 거부한다.
- `fenceSource=true`는 ABLESTACK source 또는 양쪽 제어 가능 시나리오에서만 기본값이다.

### 6.7 quiesce policy JSON

```json
{
  "schemaVersion": "dr-plan-quiesce/v1",
  "mode": "CRASH_CONSISTENT",
  "guestToolsRequired": false,
  "timeoutSeconds": 60,
  "fallback": "CRASH_CONSISTENT"
}
```

mode:

| mode | 의미 |
| --- | --- |
| `CRASH_CONSISTENT` | guest quiesce 없이 crash-consistent restore point 생성 |
| `GUEST_QUIESCE` | QGA 또는 VMware Tools freeze/thaw 시도 |
| `APPLICATION_CONSISTENT` | guest tools와 application hook 필요 |

backend preflight는 source workload별 guest tool availability를 확인하고 불가능한 mode를 hard blocker 또는 fallback warning으로 반환한다.

### 6.8 `DrPlanServiceImpl` 반영

`createPlan()` 흐름:

1. `CreateDrPlanCmd`가 typed parameter로 `DrPlanSpecRequest` 생성
2. `DrPlanSpecService.buildForCreate(request)` 호출
3. `DrPlanVO`에 engine, worker host, canonical JSON set
4. 기존 `validatePlan()` 실행
5. insert

`updatePlan()` 흐름:

1. active run/runtime resource guard 유지
2. mutable field만 변경하는 경우 기존 값 유지
3. target mapping/policy/quiesce 변경 요청이 있으면 `DrPlanSpecService.buildForUpdate(plan, request)` 호출
4. canonical JSON과 worker host 변경 시 runtime resource 존재 여부를 다시 검사

### 6.9 의미 검증

기존 `validateJson()`은 제거하지 않고 expert/backward compatibility 검증으로 유지한다. 신규 의미 검증은 다음 항목을 본다.

| 검증 | 위치 |
| --- | --- |
| worker host가 하나 이상 존재 | `FtctlDrPlanSpecValidator` |
| coordinator host가 dispatch 가능한 Cloud host인지 | `FtctlDrPlanSpecValidator` |
| source/target disk mapping이 비어 있지 않음 | `FtctlDrPlanSpecValidator` |
| 각 disk mapping에 sourceRef/sourceVolumeId와 targetRef/targetVolumeId 존재 | `FtctlDrPlanSpecValidator` |
| VMware target datastore/ref 존재 | `VmwareTargetPlanSpecValidator` |
| VMware resourcePoolRef 또는 clusterRef 존재 | `VmwareTargetPlanSpecValidator` |
| VMware folder/network 존재 | `VmwareTargetPlanSpecValidator` |
| KVM target storage/network/service offering 존재 | `KvmTargetPlanSpecValidator` |
| schedule interval <= RPO | `DrPlanSpecValidator` |
| consistency mode와 guest tool availability 일치 | `DrPlanSpecValidator` |

## 7. Inventory service 상세 설계

### 7.1 `DrPlanInventoryServiceImpl`

현재 source site credential로 source workload만 조회한다. 다음 순서로 확장한다.

```java
public DrPlanInventoryResult discover(DrPlanInventoryRequest request) {
    DrSiteVO sourceSite = requireSite(request.getSourceSiteId(), "source");
    DrSiteVO targetSite = requireSite(request.getTargetSiteId(), "target");
    String direction = resolveDirection(sourceSite, targetSite);

    DrPlanInventoryResult result = baseResult(sourceSite, targetSite, direction);
    result.setEngineType(resolveEngineType(sourceSite, targetSite, direction));
    result.setSourceWorkloads(discoverSourceWorkloads(sourceSite, request));

    if (request.hasSelectedWorkload()) {
        result.setSourceDisks(discoverSourceDisks(sourceSite, request));
        result.setSourceNetworks(discoverSourceNetworks(sourceSite, request));
    }

    result.setSourceWorkerHosts(discoverWorkerHosts(sourceSite, SOURCE));
    result.setTargetWorkerHosts(discoverWorkerHosts(targetSite, TARGET));
    result.setCoordinatorWorkerHosts(discoverCoordinatorHosts(sourceSite, targetSite));
    result.setTargetStorageOptions(discoverTargetStorage(targetSite, direction));
    result.setTargetComputeOptions(discoverTargetCompute(targetSite, direction));
    result.setTargetNetworkOptions(discoverTargetNetworks(targetSite, direction));
    result.setTargetFolderOptions(discoverTargetFolders(targetSite, direction));
    return complete(result, CONNECTED, OK, "DR plan inventory was discovered", started);
}
```

### 7.2 ABLESTACK/Mold inventory

`DrMoldInventoryClient`에 다음 조회를 추가한다.

| 메서드 | Mold API |
| --- | --- |
| `listVirtualMachines` | `listVirtualMachines` |
| `listVmDisks` | `listVolumes` 또는 VM details |
| `listNetworks` | `listNetworks` |
| `listStoragePools` | `listStoragePools` |
| `listClusters` | `listClusters` |
| `listHosts` | `listHosts` |
| `listServiceOfferings` | `listServiceOfferings` |
| `listDiskOfferings` | `listDiskOfferings` |

HMAC-SHA256 signing과 credential 처리 규칙은 DR Site inventory와 동일하게 유지한다.

### 7.3 VMware inventory

`DrVmwareInventoryClient`에 다음 조회를 추가한다.

| 메서드 | vCenter 대상 |
| --- | --- |
| `listVirtualMachines` | VM inventory |
| `listVmDisks` | VM hardware disk backing |
| `listVmNetworks` | VM NIC backing |
| `listDatastores` | datastore inventory |
| `listResourcePools` | resource pool/cluster |
| `listFolders` | VM folder |
| `listNetworks` | standard portgroup/dvPortgroup |

vCenter session token과 password는 response, log, health history에 남기지 않는다.

## 8. Agent/ftctl 영향

Cloud UI와 API가 raw JSON을 받지 않더라도 Agent/ftctl profile 계약은 유지한다.

`FtctlDrUnifiedActionAdapter.buildProfileJson()`은 계속 다음 구조를 만든다.

```json
{
  "engine": "FTCTL_DR",
  "direction": "KVM_TO_VMWARE",
  "workers": {},
  "mapping": {},
  "schedule": {},
  "policy": {},
  "quiescePolicy": {}
}
```

변경점은 profile의 출처다.

| 항목 | 기존 | 변경 |
| --- | --- | --- |
| `mapping` | 사용자가 입력한 `mappingjson` | backend `DrPlanSpecBuilder`가 생성한 canonical JSON |
| `schedule` | 사용자가 입력한 `schedulejson` | RPO/RTO/retention typed field 기반 생성 |
| `policy` | 사용자가 입력한 `policyjson` | UI policy control 기반 생성 |
| `quiescePolicy` | 사용자가 입력한 `quiescepolicyjson` | consistency selector 기반 생성 |

ftctl 쪽 추가 권장:

1. profile `schemaVersion` 검증
2. `mapping.disks[]` 필수 field 검증
3. 지원하지 않는 `schedule/policy/quiescePolicy` key를 warning 또는 error로 명확히 반환
4. engine이 아직 소비하지 않는 정책은 Cloud UI에서 숨김

## 9. DB 영향

즉시 필요한 신규 column은 없다.

기존 `dr_plan` column을 canonical spec 저장소로 유지한다.

| column | 사용 |
| --- | --- |
| `mapping_json` | backend-generated mapping canonical JSON |
| `schedule_json` | backend-generated schedule canonical JSON |
| `policy_json` | backend-generated policy canonical JSON |
| `quiesce_policy_json` | backend-generated quiesce canonical JSON |
| `source_worker_host_id` | typed selector 결과 |
| `target_worker_host_id` | typed selector 결과 |
| `coordinator_worker_host_id` | typed selector 결과 |

향후 검색/필터가 필요한 경우에만 별도 column을 추가한다.

후보:

- `sync_interval_seconds`
- `retention_count`
- `consistency_mode`
- `test_network_mode`

이번 설계 범위에서는 DB migration 없이 API/backend/UI 변경으로 처리한다.

## 10. 문서/구현 갭

기존 문서에는 `discoverDrPlanInventory`가 `sourceworkerhosts`, `targetworkerhosts`를 반환한다고 되어 있으나, 현재 구현의 `DrPlanInventoryResult`와 `DrPlanInventoryResponse`는 `sourceworkloads`만 보유한다. 따라서 다음 구현에서는 문서와 구현을 맞추는 것이 필수다.

| 항목 | 문서 기대 | 현재 구현 | 보강 |
| --- | --- | --- | --- |
| source workload | 있음 | 있음 | 유지 |
| source worker host | 있음 | 없음 | DTO/service/response 추가 |
| target worker host | 있음 | 없음 | DTO/service/response 추가 |
| coordinator worker host | 필요 | 없음 | DTO/service/response 추가 |
| source disk/NIC | 필요 | 없음 | workload 선택 후 조회 |
| target storage/compute/network | 필요 | 없음 | target site inventory 조회 |
| generated spec preview | 필요 | 없음 | `previewDrPlanSpec` 추가 |

## 11. 구현 순서

1. `DrPlanInventoryResult`, `DrPlanInventoryResponse`, `DrResponseGenerator`에 확장 field 추가
2. `DrPlanInventoryServiceImpl`에 worker host와 target resource discovery 추가
3. `DrMoldInventoryClient`, `DrVmwareInventoryClient`에 target inventory 메서드 추가
4. `DrPlanSpecRequest`, `DrPlanSpec`, `DrPlanSpecBuilder`, validator package 추가
5. `previewDrPlanSpec` async API 추가
6. `CreateDrPlanCmd`, `UpdateDrPlanCmd`에 typed parameter 추가
7. `DrPlanServiceImpl` create/update에서 `DrPlanSpecService` 사용
8. `DrPlanList.vue`에서 raw JSON 기본 노출 제거
9. `DrPlanTargetMapping`, `DrPlanPolicyFields`, `DrPlanConsistencyFields`, `DrPlanSpecPreview` 추가
10. i18n label/help/placeholder 보강
11. targeted Maven test와 UI lint/build 수행
12. 배포 후 active bundle marker와 `/client/` HTTP 200 검증

## 12. 테스트 기준

| 계층 | 검증 |
| --- | --- |
| UI unit/lint | `DrPlanList.vue`와 신규 DR form component lint |
| API | `previewDrPlanSpec`, `discoverDrPlanInventory`, `createDrPlan`, `updateDrPlan` parameter/response test |
| Backend | direction별 `DrPlanSpecBuilderTest`, validator test |
| Adapter | 기존 `FtctlDrUnifiedActionAdapterTest` profile JSON 유지 |
| Orchestrator | disk mapping 누락/정상 mapping test |
| DB | 신규 column 없음. 기존 JSON 저장/조회 회귀 test |
| Runtime smoke | Plan 생성 -> generated JSON 저장 -> sync run이 worker/mapping 누락 없이 시작되는지 확인 |

## 13. 2026-07-05 실행 준비성 보강 설계

### 13.1 검증 결과 요약

`VMWARE_TO_KVM` DR Plan 생성/조회 경로는 UI, API, DB 관점에서는 정상으로 이어진다. 그러나 생성된 Plan이 곧바로 FTCTL_DR 실행 준비가 완료된 상태는 아니다. 현재 코드 기준으로 다음 공백이 남아 있다.

| 항목 | 현재 상태 | 실행 영향 |
| --- | --- | --- |
| Plan row | `state=NEW`, `admin_state=ENABLED`, source/target site와 source VM ref 저장 | 조회와 수정은 가능 |
| Worker binding | `source_worker_host_id`, `target_worker_host_id`, `coordinator_worker_host_id`가 비어 있을 수 있음 | `DrProtectionOrchestratorImpl.materializeWorkerBindings()`와 `FtctlDrUnifiedActionAdapter`에서 sync dispatch 실패 |
| Disk mapping | `DrPlanGuidedSpecBuilder`가 `targetStorageRef`, `targetComputeRef` 등 상위 hint만 저장하고 `disks[]`를 만들지 않음 | `DrProtectionOrchestratorImpl.parseDiskMappings()`가 빈 mapping으로 판단하고 sync 준비 단계에서 실패 |
| Action eligibility | `DrPlanServiceImpl.getActionEligibility()`가 runtime readiness 없이 `sync`, `releaseProtection`을 열 수 있음 | UI가 아직 보호 자원이 없는 Plan에 실행/해제 버튼을 보여줄 수 있음 |
| Inventory | KVM target site에 내부 `zone_id`가 없거나 target resource 후보가 비어도 Plan 저장이 가능 | 사용자는 정상 생성으로 보지만 실행 시 target storage/worker를 확정할 수 없음 |
| UI/API parameter | `listDrPlans` 호출에 `listall` 같은 미등록 parameter가 섞일 수 있음 | 기능 실패는 아니지만 management log warning이 반복됨 |

따라서 Plan lifecycle은 다음 두 단계를 명확히 분리한다.

1. `CONFIGURED`: 사용자가 Plan을 저장했고 사이트/대상 VM 참조가 DB에 있다.
2. `EXECUTION_READY`: worker binding, disk mapping, target storage/compute/network, schedule/policy/quiesce가 모두 canonical contract로 검증됐다.

### 13.2 Readiness contract

DB migration 없이 1차 구현한다. `dr_plan`의 기존 column과 `mapping_json`을 읽어 backend가 계산형 readiness를 응답에 추가한다.

```json
{
  "readiness": {
    "state": "CONFIG_INCOMPLETE",
    "executionReady": false,
    "blockingReasons": [
      "SOURCE_WORKER_REQUIRED",
      "TARGET_WORKER_REQUIRED",
      "COORDINATOR_WORKER_REQUIRED",
      "DISK_MAPPING_REQUIRED"
    ],
    "warnings": []
  }
}
```

권장 enum:

| 값 | 의미 |
| --- | --- |
| `CONFIG_INCOMPLETE` | 저장은 가능하지만 sync/failover 실행 불가 |
| `EXECUTION_READY` | sync run 생성 가능 |
| `RUNTIME_ACTIVE` | sync/test/failover/failback run 또는 replica가 존재 |
| `RELEASE_READY` | 보호 자원 또는 runtime profile이 있어 release 가능 |

### 13.3 UI 설계

수정 대상:

- `ui/src/views/infra/dr/DrPlanList.vue`
- `ui/src/components/dr/DrActionToolbar.vue`
- `ui/src/api/dr.js`
- `ui/public/locales/en.json`
- `ui/public/locales/ko_KR.json`

`DrPlanList.vue`는 다음 상태를 추가한다.

```javascript
createForm: {
  ...
  sourceworkloadvalue: undefined,
  sourceworkloaddisks: [],
  diskmappings: [],
  targetstorageref: '',
  targetcomputeref: '',
  targetnetworkref: '',
  sourceworkerhostid: '',
  targetworkerhostid: '',
  coordinatorworkerhostid: '',
  allowdraft: true
}
```

UI 검증 규칙:

| 조건 | 처리 |
| --- | --- |
| `startsync=true` | `executionReady`가 아니면 submit 차단 |
| `startsync=false`, `allowdraft=true` | 저장 허용, 상세/목록에 `구성 미완료` readiness 표시 |
| target KVM site인데 worker option이 없음 | "대상 사이트 Zone/호스트 매핑을 먼저 완료해야 합니다"로 표시 |
| source VM disk가 조회됨 | disk row별 target storage/format/ref를 선택하게 함 |
| source VM disk가 조회되지 않음 | sync 시작 차단, Plan draft 저장만 허용 |

`DrActionToolbar.vue`는 backend `actioneligibility`를 최종 권한으로 사용하되, disabled reason을 함께 표시한다. UI가 자체 판단으로 release/sync를 열지 않는다.

### 13.4 API 설계

수정 대상:

- `DiscoverDrPlanInventoryCmd`
- `PreviewDrPlanSpecCmd`
- `CreateDrPlanCmd`
- `UpdateDrPlanCmd`
- `DrPlanResponse`
- `DrPlanInventoryResponse`
- `DrPlanSpecPreviewResponse`

`discoverDrPlanInventory` 응답은 source workload 선택 이후 disk 정보를 포함해야 한다.

```json
{
  "sourceworkloads": [
    {
      "id": "vm-4486",
      "name": "rocky10-1",
      "details": {
        "disks": [
          {
            "ref": "disk-1000",
            "label": "Hard disk 1",
            "capacityBytes": 42949672960,
            "controller": "scsi0:0",
            "format": "vmdk"
          }
        ]
      }
    }
  ],
  "targetstorageoptions": [],
  "targetcomputeoptions": [],
  "targetnetworkoptions": [],
  "sourceworkerhosts": [],
  "targetworkerhosts": [],
  "coordinatorworkerhosts": []
}
```

`previewDrPlanSpec`는 저장 전 동일 validator를 호출한다.

```json
{
  "executionReady": false,
  "blockingReasons": ["DISK_MAPPING_REQUIRED"],
  "generatedmappingjson": "{...}",
  "generatedschedulejson": "{...}",
  "generatedpolicyjson": "{...}",
  "generatedquiescepolicyjson": "{...}"
}
```

`createDrPlan`/`updateDrPlan`에 typed disk mapping parameter를 추가한다.

| parameter | 설명 |
| --- | --- |
| `guidedplan` | guided mode marker |
| `allowdraft` | 실행 준비가 안 된 Plan 저장 허용 여부 |
| `diskmappingsjson` | UI가 선택한 source disk와 target storage/format/ref 배열 |
| `sourceworkerhostid` | source reader worker |
| `targetworkerhostid` | target writer worker |
| `coordinatorworkerhostid` | orchestration owner |

`listDrPlans`는 UI에서 보내는 parameter와 API 정의를 맞춘다. API가 `listall`을 지원하지 않는다면 UI wrapper에서 제거하고, CloudStack 표준 pagination parameter만 보낸다.

### 13.5 Backend 설계

수정 대상:

- `plugins/integrations/disaster-recovery/src/main/java/com/cloud/dr/DrPlanServiceImpl.java`
- `plugins/integrations/disaster-recovery/src/main/java/com/cloud/dr/DrPlanGuidedSpec.java`
- `plugins/integrations/disaster-recovery/src/main/java/com/cloud/dr/DrPlanGuidedSpecBuilder.java`
- `plugins/integrations/disaster-recovery/src/main/java/com/cloud/dr/DrPlanGeneratedSpec.java`
- `plugins/integrations/disaster-recovery/src/main/java/com/cloud/dr/inventory/DrPlanInventoryServiceImpl.java`
- `plugins/integrations/disaster-recovery/src/main/java/com/cloud/dr/orchestrator/DrProtectionOrchestratorImpl.java`
- `plugins/integrations/disaster-recovery/src/main/java/com/cloud/dr/adapter/ftctl/FtctlDrUnifiedActionAdapter.java`

새 helper를 추가한다.

```java
public final class DrPlanReadiness {
    private final boolean executionReady;
    private final boolean releaseReady;
    private final List<String> blockingReasons;
    private final List<String> warnings;
}
```

```java
public interface DrPlanReadinessValidator {
    DrPlanReadiness validateForExecution(DrPlanVO plan);
    DrPlanReadiness validateForRelease(DrPlanVO plan);
}
```

검증 규칙:

| rule | 대상 |
| --- | --- |
| FTCTL_DR plan에는 coordinator/source/target worker가 있어야 함 | execution |
| `mapping_json.disks[]` 또는 호환 alias 배열이 하나 이상 있어야 함 | execution |
| 각 disk mapping에는 source ref와 target storage/ref/format이 있어야 함 | execution |
| target KVM이면 site `zone_id`와 target storage option이 resolve되어야 함 | execution |
| `dr_replica`, `dr_replica_disk`, runtime profile, active/completed sync run 중 하나가 있어야 함 | release |

`DrPlanGuidedSpecBuilder`는 다음 구조의 `disks[]`를 생성한다.

```json
{
  "schemaVersion": "DR_PLAN_GUIDED_SPEC_V1",
  "direction": "VMWARE_TO_KVM",
  "sourceSiteId": 2,
  "targetSiteId": 3,
  "sourceExternalRef": "vm-4486",
  "targetVmName": "rocky10-1-dr",
  "targetStorageRef": "pool-uuid-or-id",
  "targetComputeRef": "host-or-cluster-ref",
  "targetNetworkRef": "network-ref",
  "disks": [
    {
      "label": "Hard disk 1",
      "sourceRef": "disk-1000",
      "sourcePath": "[datastore1] vm/vm.vmdk",
      "sourceFormat": "vmdk",
      "targetRef": "rocky10-1-dr-disk-0",
      "targetStorageRef": "pool-uuid-or-id",
      "targetFormat": "qcow2"
    }
  ]
}
```

`DrPlanServiceImpl.getActionEligibility()`는 readiness를 사용하도록 조정한다.

```java
DrPlanReadiness readiness = readinessValidator.validateForExecution(plan);
DrPlanReadiness releaseReadiness = readinessValidator.validateForRelease(plan);

eligibility.put("sync",
        enabled && !activeRun && hasEngine && ftctlDrPlan && readiness.isExecutionReady());
eligibility.put("releaseProtection",
        enabled && !activeRun && hasEngine && ftctlDrPlan && releaseReadiness.isReleaseReady());
```

### 13.6 Agent/ftctl 설계

Agent와 ftctl은 UI draft를 직접 해석하지 않는다. Cloud backend가 canonical profile을 만든 뒤 Agent command에 넣는다.

| 계층 | 보강 |
| --- | --- |
| KVM Agent helper | worker/profile 생성 전에 `mapping.disks[]`와 worker id를 검증하고, 누락 시 `CONFIG_INCOMPLETE`를 반환 |
| `lib/ftctl/dr_vmware.sh` | guided alias를 유지하되 disk mapping 없이는 sync manifest를 만들지 않음 |
| ftctl status | 실행 전 구성 오류는 generic failure가 아니라 `CONFIG_INCOMPLETE` reason으로 보고 |
| selftest | `VMWARE_TO_KVM` guided mapping에 `disks[]`가 있을 때 manifest가 생성되는지 검증 |

### 13.7 DB 설계

1차 보강에서는 신규 column을 만들지 않는다.

| 저장 위치 | 사용 |
| --- | --- |
| `dr_plan.mapping_json` | canonical mapping과 `disks[]` 저장 |
| `dr_plan.*_worker_host_id` | worker binding 저장 |
| `dr_replica` | sync가 실제 시작된 후 target runtime 자원 추적 |
| `dr_replica_disk` | sync가 실제 시작된 후 disk별 source/target 추적 |
| `dr_run` | action별 async 실행 상태 |
| `dr_event` | readiness/preflight warning 또는 runtime event 표시 |

향후 검색/필터 성능이 필요하면 `dr_plan.config_state`, `dr_plan.config_error_json` column을 추가할 수 있으나, 현재는 API 응답 계산값으로 충분하다.

### 13.8 기존 생성 Plan 처리

이미 생성된 Plan이 worker/disk mapping 없이 존재하는 경우 삭제하지 않는다.

1. 목록과 상세 화면에 `구성 미완료`로 표시한다.
2. `sync`, `failover`, `releaseProtection`은 비활성화한다.
3. `수정`에서 worker와 disk mapping을 보강할 수 있게 한다.
4. `previewDrPlanSpec`가 `EXECUTION_READY`를 반환하면 이후 sync action을 열어준다.

### 13.9 보강 테스트 기준

| 범위 | 기준 |
| --- | --- |
| UI | worker/disk 누락 시 startsync 차단, draft 저장 가능 |
| API | `previewDrPlanSpec`가 blocking reason을 반환 |
| Backend | `getActionEligibility()`가 `NEW` + no runtime plan에 release를 열지 않음 |
| Orchestrator | disk mapping 누락 실패는 Plan 저장 전 preview에서 먼저 검출 |
| Agent/ftctl | generated profile에 `mapping.disks[]`가 포함됨 |
| DB | 신규 column 없이 기존 JSON/worker field로 round-trip |
| Runtime smoke | Plan 생성 -> preview ready -> sync run 생성 -> `dr_run`, `dr_replica`, `dr_replica_disk` 생성 |

## 14. AS-IS / TO-BE

| 구분 | AS-IS | TO-BE |
| --- | --- | --- |
| 고급 설정 의미 | 내부 engine/profile 값을 사용자에게 직접 노출 | 사용자 입력은 업무 개념만 받고 내부 spec은 backend가 생성 |
| Engine 선택 | UI select로 표시 | site/direction 기반 backend 자동 결정 |
| Engine binding | 사용자가 직접 입력 가능 | 기존 resource import/adopt 외에는 backend 내부값 |
| Worker host | UUID/숫자 직접 입력 | inventory select 또는 자동 선택 |
| Mapping | raw JSON textarea | target resource/disk/NIC 선택 UI |
| Schedule | raw JSON textarea | RPO/RTO/sync interval/retention typed field |
| Policy | raw JSON textarea | failover/test/retry/transport policy typed field |
| Quiesce | raw JSON textarea | consistency mode selector |
| Validation | JSON syntax 중심 | 방향별 의미 검증과 preflight |
| API | raw JSON parameter 중심 | typed parameter + backend-generated canonical JSON |
| Backend | 저장 전 JSON parse | `DrPlanSpecBuilder`와 validator로 canonical spec 생성 |
| Agent/ftctl | UI raw JSON이 profile로 전달 | backend canonical JSON이 profile로 전달 |
| DB | JSON column에 임의 JSON 저장 | 동일 column에 schemaVersion 포함 canonical JSON 저장 |
| Expert mode | 없음 또는 일반 고급 설정 | read-only preview 기본, 제한적 override |

## 15. 2026-07-06 VMware -> ABLESTACK 선택형 Target Inventory 보강 설계

### 15.1 문제 정의

`VMWARE_TO_KVM` 방향에서 현재 Plan 생성 대화상자는 source VM은 선택형으로 조회하지만, ABLESTACK 대상 VM을 만들기 위한 Zone, worker host, storage, compute offering, disk offering, network, disk mapping은 아직 완전한 선택형 흐름이 아니다.

코드 기준으로 확인된 현재 구조는 다음과 같다.

| 영역 | 현재 코드 | 문제 |
| --- | --- | --- |
| UI target storage | `DrPlanList.vue`가 `targetStorageOptions.length > 0`이면 `<a-select>`, 아니면 `<a-input>`을 표시 | 정상 사용자 흐름에서 storage pool ID를 수동 입력하게 될 수 있음 |
| UI target compute | `DrPlanList.vue`가 `targetComputeOptions.length > 0`이면 `<a-select>`, 아니면 `<a-input>`을 표시 | KVM target에서 compute offering이 아니라 worker host 후보가 compute option처럼 표시됨 |
| UI target network | `DrPlanList.vue`가 항상 `<a-input v-model:value="createForm.targetnetworkref">` 사용 | ABLESTACK network를 사용자가 수동 입력해야 함 |
| UI worker host | worker 후보가 없으면 `<a-input>` fallback 사용 | host UUID/local ID를 사용자가 알아야 하는 구조 |
| Backend inventory | `DrPlanInventoryServiceImpl.listTargetNetworkOptions()`와 `listTargetFolderOptions()`가 빈 배열 반환 | UI가 선택지를 만들 수 없음 |
| Backend worker/storage | `listWorkerHosts()`와 `listTargetStorageOptions()`가 `DrSiteVO.zoneId` 없으면 빈 배열 반환 | target site가 zone에 연결되지 않으면 Plan inventory 전체가 빈 상태가 됨 |
| VMware source detail | `DrVmwareInventoryClient.listVirtualMachines()`가 VM 목록만 조회 | source disk/NIC를 알 수 없어 `disks[]`를 자동 구성할 수 없음 |
| Readiness | `DrPlanReadinessValidator`가 coordinator worker와 disk mapping 중심으로 검증 | target zone, service offering, disk offering, network, target worker 누락을 조기에 설명하지 못함 |

따라서 일반 운영자 UI의 기준은 "입력"이 아니라 "조회된 후보 선택"이어야 한다. 수동 참조 입력은 expert mode에서만 제한적으로 남긴다.

### 15.2 사용자 흐름 기준

`VMWARE_TO_KVM` Plan 생성/수정은 다음 흐름을 따른다.

1. source site와 target site를 선택한다.
2. backend가 site hypervisor를 기준으로 direction을 `VMWARE_TO_KVM`으로 계산한다.
3. source site credential로 vCenter VM 목록을 조회하고 source VM을 선택한다.
4. source VM 선택 즉시 backend가 VM 상세 disk/NIC inventory를 조회한다.
5. target ABLESTACK site의 Zone이 설정되어 있는지 확인한다.
6. target Zone 기준으로 worker host, primary storage pool, service offering, disk offering, network를 조회한다.
7. 후보가 1개인 field만 자동 선택한다.
8. 후보가 0개이면 warning/blocking reason을 표시하고 저장 또는 실행 준비를 막는다.
9. 후보가 2개 이상이면 사용자가 명시적으로 선택해야 한다.
10. disk mapping table에서 source disk별 target disk offering/storage/name을 선택한다.
11. `previewDrPlanSpec`으로 backend-generated canonical spec과 readiness를 확인한다.
12. `EXECUTION_READY`일 때만 sync/failover 실행 action을 활성화한다.

### 15.3 UI 코드 설계

대상 파일:

- `ui/src/views/infra/dr/DrPlanList.vue`
- 필요 시 `ui/src/views/infra/dr/components/DrPlanDiskMappingTable.vue` 신규 분리
- `ui/src/locale/{ko-KR,en}.json` 또는 현재 프로젝트의 DR i18n 파일

#### 15.3.1 일반 mode에서 input fallback 제거

현재 패턴:

```vue
<a-select v-if="targetStorageOptions.length > 0" ... />
<a-input v-else v-model:value="createForm.targetstorageref" ... />
```

보강 후 패턴:

```vue
<a-select
  v-if="targetStorageOptions.length > 0"
  v-model:value="createForm.targetstorageref"
  showSearch
  allowClear
  optionFilterProp="label"
  :placeholder="$t('message.dr.plan.target.storage.placeholder')" />
<a-alert
  v-else
  type="warning"
  showIcon
  :message="$t('message.dr.plan.target.storage.empty')" />
```

동일 원칙을 다음 field에 적용한다.

| Field | 일반 mode | Expert mode |
| --- | --- | --- |
| `targetstorageref` | select 또는 warning | 수동 입력 허용 |
| `targetcomputeref` | service offering select | 수동 입력 허용 |
| `targetnetworkref` | network select | 수동 입력 허용 |
| `sourceworkerhostid` | source가 KVM일 때 select 또는 warning | 수동 입력 허용 |
| `targetworkerhostid` | target이 KVM일 때 select 또는 warning | 수동 입력 허용 |
| `coordinatorworkerhostid` | select 또는 warning | 수동 입력 허용 |
| `diskmappingsjson` | disk mapping table이 생성 | raw JSON textarea 허용 |

#### 15.3.2 form state 보강

`createForm`에 다음 typed field를 추가한다.

```js
createForm: {
  targetzoneid: '',
  targetserviceofferingid: '',
  targetnetworkids: [],
  targetdiskmappings: [],
  targetstorageref: '',
  targetworkerhostid: '',
  coordinatorworkerhostid: '',
  expertmode: false
}
```

inventory option state:

```js
targetZoneOption: null,
targetZoneBlockingReasons: [],
targetServiceOfferingOptions: [],
targetDiskOfferingOptions: [],
targetNetworkOptions: [],
sourceDiskOptions: [],
sourceNicOptions: [],
autoSelectedFields: {}
```

#### 15.3.3 자동 선택 정책

`applyDefaultGuidedSelections()`는 다음 helper로 단순화한다.

```js
autoSelectSingleOption (field, options) {
  const selectable = (options || []).filter(option => option.selectable !== false)
  if (selectable.length === 1 && !this.createForm[field]) {
    this.createForm[field] = selectable[0].value
    this.autoSelectedFields[field] = true
  }
}
```

자동 선택 허용:

- 후보가 정확히 1개인 coordinator worker host
- 후보가 정확히 1개인 target worker host
- 후보가 정확히 1개인 target storage pool
- 후보가 정확히 1개인 service offering
- 후보가 정확히 1개인 network
- source VM 이름 기반 `targetvmname = <sourceName>-dr`
- 기본 RPO/RTO 300초

자동 선택 금지:

- 후보가 2개 이상인 service offering, disk offering, storage, network
- source disk 상세가 없는 상태의 disk mapping
- target Zone이 site에 설정되어 있지 않은 상태의 임의 Zone 선택

#### 15.3.4 source VM 선택 후 disk/NIC 조회

`changeSourceWorkload(optionKey)`는 source VM 이름과 ref만 저장하지 않고, 선택된 source VM의 상세 inventory를 다시 조회한다.

```js
async changeSourceWorkload (optionKey) {
  const workload = this.findSourceWorkload(optionKey)
  this.applySourceWorkload(workload)
  await this.fetchDrPlanInventory({
    sourcesiteid: this.createForm.sourcesiteid,
    targetsiteid: this.createForm.targetsiteid,
    sourceexternalref: workload.value,
    includeplacement: true,
    includedisks: true,
    includenetworks: true
  })
  this.rebuildDiskMappingRows()
  this.previewGuidedSpec()
}
```

disk mapping row:

```js
{
  sourceDiskRef: 'disk-1000',
  sourcePath: '[datastore1] Rocky/Rocky.vmdk',
  sourceLabel: 'Hard disk 1',
  capacityBytes: 42949672960,
  boot: true,
  targetDiskName: 'Rocky-dr-disk-0',
  targetDiskOfferingId: '',
  targetStorageRef: ''
}
```

#### 15.3.5 UI validation

`validatePlanForm()` 또는 submit 직전 validation은 direction별로 다음을 검사한다.

| Direction | Required UI values |
| --- | --- |
| `VMWARE_TO_KVM` | source site, target site, source workload, target VM name, target Zone, coordinator worker, target worker, service offering, network, source disk inventory, disk mapping별 disk offering/storage |
| `KVM_TO_VMWARE` | source worker, coordinator worker, VMware target compute/folder/network/storage equivalent |
| `KVM_TO_KVM` | source worker, target worker, coordinator worker, target storage, service offering, network, disk mapping |
| `VMWARE_TO_VMWARE` | source workload, VMware target placement, VMware datastore/network/folder mapping |

UI validation은 사용자 편의 목적이고, 최종 권한은 backend `previewDrPlanSpec`과 `DrPlanReadinessValidator`가 가진다.

### 15.4 API 코드 설계

대상 command/response:

- `DiscoverDrPlanInventoryCmd`
- `DrPlanInventoryRequest`
- `DrPlanInventoryResult`
- `DrPlanInventoryResponse`
- `DrInventoryOptionResponse`
- `PreviewDrPlanSpecCmd`
- `CreateDrPlanCmd`
- `UpdateDrPlanCmd`

`discoverDrPlanInventory`에 optional parameter를 추가한다.

| Parameter | Type | 의미 |
| --- | --- | --- |
| `sourceexternalref` | STRING | 선택된 remote VM ref. VMware source일 때 disk/NIC 상세 조회 기준 |
| `sourcevmid` | UUID/LONG | local ABLESTACK source VM 상세 조회 기준 |
| `includeplacement` | BOOLEAN | target placement 후보 포함 여부 |
| `includedisks` | BOOLEAN | source disk 후보 포함 여부 |
| `includenetworks` | BOOLEAN | source NIC 및 target network 후보 포함 여부 |

response field를 추가한다.

| Response field | Type | 의미 |
| --- | --- | --- |
| `targetzone` | object | target site에 연결된 local Zone 정보 |
| `targetserviceofferings` | list | target ABLESTACK VM 생성 service offering 후보 |
| `targetdiskofferings` | list | target disk 생성 disk offering 후보 |
| `targetnetworkoptions` | list | target network 후보 |
| `sourcedisks` | list | source VM disk 후보 |
| `sourcenics` | list | source VM NIC 후보 |
| `blockingreasons` | list | inventory 단계에서 이미 확인된 blocker |
| `warnings` | list | 자동 선택/추론/권고 메시지 |

`CreateDrPlanCmd`와 `UpdateDrPlanCmd`는 기존 JSON field를 유지하되 일반 mode에서는 다음 typed parameter를 우선 사용한다.

| Parameter | 저장 위치 |
| --- | --- |
| `targetzoneid` | `mapping_json.target.zoneId` |
| `targetserviceofferingid` | `mapping_json.target.serviceOfferingId` |
| `targetnetworkids` | `mapping_json.target.networks[]` |
| `targetstorageref` | `mapping_json.target.storageRef`, disk별 default |
| `targetworkerhostid` | `dr_plan.target_worker_host_id` |
| `coordinatorworkerhostid` | `dr_plan.coordinator_worker_host_id` |
| `diskmappingsjson` | `mapping_json.disks[]` |

### 15.5 Backend inventory 코드 설계

대상 class:

- `DrPlanInventoryServiceImpl`
- `DrVmwareInventoryClient`
- `DrMoldInventoryClient`
- `DrResponseGenerator`
- CloudStack DAO: `DataCenterDao`, `HostDao`, `PrimaryDataStoreDao`, `ServiceOfferingDao`, `DiskOfferingDao`, `NetworkDao`

#### 15.5.1 target Zone resolver

`DrPlanInventoryServiceImpl`에 target Zone resolver를 추가한다.

```java
private DrResolvedTargetZone resolveTargetZone(DrSiteVO targetSite) {
    if (!isKvm(targetSite)) {
        return DrResolvedTargetZone.notRequired();
    }
    if (targetSite.getZoneId() != null) {
        return DrResolvedTargetZone.of(targetSite.getZoneId(), targetSite.getZoneName(), false);
    }
    if (dataCenterDao != null) {
        List<DataCenterVO> enabledZones = dataCenterDao.listEnabledZones();
        if (enabledZones != null && enabledZones.size() == 1) {
            DataCenterVO zone = enabledZones.get(0);
            return DrResolvedTargetZone.inferred(zone.getId(), zone.getName(), "TARGET_SITE_ZONE_INFERRED");
        }
    }
    return DrResolvedTargetZone.blocked("TARGET_SITE_ZONE_REQUIRED");
}
```

주의: infer는 inventory 표시와 기존 데이터 복구를 위한 임시 보조일 뿐이다. `createDrSite`/`updateDrSite` 단계에서 KVM target site는 Zone을 명시 저장해야 하며, Plan 저장 전에는 `TARGET_SITE_ZONE_REQUIRED`를 해소해야 한다.

#### 15.5.2 target placement option 조회

`populateGuidedTargetOptions()`는 다음 순서로 바꾼다.

```java
DrResolvedTargetZone zone = resolveTargetZone(targetSite);
result.setTargetZone(zone.toOption());
result.addBlockingReasons(zone.getBlockingReasons());
if (zone.isBlocked()) {
    return;
}
result.setTargetWorkerHosts(listWorkerHosts(targetSite, zone.getZoneId(), "TARGET_WORKER_HOST"));
result.setCoordinatorWorkerHosts(mergeOptions(sourceWorkerHosts, result.getTargetWorkerHosts()));
result.setTargetStorageOptions(listTargetStorageOptions(targetSite, zone.getZoneId()));
result.setTargetServiceOfferings(listTargetServiceOfferingOptions(zone.getZoneId()));
result.setTargetDiskOfferings(listTargetDiskOfferingOptions(zone.getZoneId()));
result.setTargetNetworkOptions(listTargetNetworkOptions(targetSite, zone.getZoneId()));
```

`listTargetComputeOptions()`는 KVM target에서 worker host를 compute option으로 재사용하지 않는다. KVM target의 compute option은 service offering이다. target host는 별도의 `targetworkerhosts`로만 전달한다.

#### 15.5.3 VMware source disk/NIC 상세 조회

`DrVmwareInventoryClient`에 source VM 상세 조회를 추가한다.

```java
public DrVmwareVirtualMachineDetail getVirtualMachineDetail(
        DrResolvedSiteCredential credential,
        String vmRef) {
    String sessionId = openSession(...);
    JsonObject vm = fetchVmDetail(rootEndpoint, sessionId, vmRef, tlsVerify);
    JsonArray disks = fetchVmHardwareArray(rootEndpoint, sessionId, vmRef, "disk", tlsVerify);
    JsonArray nics = fetchVmHardwareArray(rootEndpoint, sessionId, vmRef, "ethernet", tlsVerify);
    return toVirtualMachineDetail(vmRef, vm, disks, nics);
}
```

조회 endpoint fallback:

| API flavor | VM detail | Disk | NIC |
| --- | --- | --- | --- |
| vCenter REST legacy | `/rest/vcenter/vm/{vm}` | `/rest/vcenter/vm/{vm}/hardware/disk` | `/rest/vcenter/vm/{vm}/hardware/ethernet` |
| vCenter API | `/api/vcenter/vm/{vm}` | `/api/vcenter/vm/{vm}/hardware/disk` | `/api/vcenter/vm/{vm}/hardware/ethernet` |

반환 객체 예:

```java
public class DrVmwareVirtualMachineDetail {
    private String vmRef;
    private String name;
    private List<DrSourceDiskOption> disks;
    private List<DrSourceNicOption> nics;
}
```

### 15.6 Mapping JSON 설계

`DrPlanGuidedSpecBuilder.buildMapping()`은 다음 canonical 구조를 생성한다.

```json
{
  "schemaVersion": "DR_PLAN_GUIDED_SPEC_V1",
  "direction": "VMWARE_TO_KVM",
  "sourceSiteId": 2,
  "targetSiteId": 3,
  "sourceExternalRef": "vm-4486",
  "target": {
    "hypervisor": "KVM",
    "zoneId": 1,
    "serviceOfferingId": "service-offering-uuid-or-id",
    "workerHostId": 2,
    "networks": [
      {
        "networkId": "network-uuid-or-id",
        "role": "default"
      }
    ],
    "storageRef": "pool-uuid-or-id"
  },
  "disks": [
    {
      "source": {
        "diskRef": "disk-1000",
        "label": "Hard disk 1",
        "vmdkPath": "[datastore1] Rocky/Rocky.vmdk",
        "capacityBytes": 42949672960,
        "boot": true
      },
      "target": {
        "name": "Rocky-dr-disk-0",
        "diskOfferingId": "disk-offering-uuid-or-id",
        "storageRef": "pool-uuid-or-id",
        "format": "qcow2"
      }
    }
  ]
}
```

`targetStorageRef`, `targetComputeRef`, `targetNetworkRef` 같은 legacy alias는 backward compatibility로 읽되, canonical 저장 기준은 `target.*`와 `disks[].target.*`이다.

### 15.7 Readiness validator 보강

`DrPlanReadinessValidator`에 reason을 추가한다.

```java
public static final String REASON_TARGET_SITE_ZONE_REQUIRED = "TARGET_SITE_ZONE_REQUIRED";
public static final String REASON_TARGET_WORKER_REQUIRED = "TARGET_WORKER_REQUIRED";
public static final String REASON_TARGET_STORAGE_REQUIRED = "TARGET_STORAGE_REQUIRED";
public static final String REASON_TARGET_SERVICE_OFFERING_REQUIRED = "TARGET_SERVICE_OFFERING_REQUIRED";
public static final String REASON_TARGET_NETWORK_REQUIRED = "TARGET_NETWORK_REQUIRED";
public static final String REASON_SOURCE_DISK_INVENTORY_REQUIRED = "SOURCE_DISK_INVENTORY_REQUIRED";
public static final String REASON_TARGET_DISK_OFFERING_REQUIRED = "TARGET_DISK_OFFERING_REQUIRED";
```

`VMWARE_TO_KVM` execution readiness 조건:

| 조건 | 검증 위치 |
| --- | --- |
| target KVM site가 Zone에 연결됨 | `validateTargetKvmPlacement()` |
| coordinator worker가 존재함 | 기존 `validateWorkers()` 유지 |
| target worker가 존재함 | `validateTargetKvmPlacement()` |
| service offering이 지정됨 | `validateTargetKvmPlacement()` |
| network가 1개 이상 지정됨 | `validateTargetKvmPlacement()` |
| source disk inventory 또는 source disk ref가 존재함 | `validateDiskMappings()` |
| disk별 target disk offering과 storage가 지정됨 | `validateDiskMappings()` |

### 15.8 DB 설계

1차 구현은 신규 `dr_plan` column 없이 진행한다.

| 저장 대상 | 저장 위치 |
| --- | --- |
| target Zone | `dr_site.zone_id`, `dr_site.zone_external_id`, `dr_site.zone_name`, `dr_plan.mapping_json.target.zoneId` |
| service offering | `dr_plan.mapping_json.target.serviceOfferingId` |
| target network | `dr_plan.mapping_json.target.networks[]` |
| disk offering | `dr_plan.mapping_json.disks[].target.diskOfferingId` |
| storage pool | `dr_plan.mapping_json.target.storageRef`, `dr_plan.mapping_json.disks[].target.storageRef` |
| worker binding | `dr_plan.target_worker_host_id`, `dr_plan.coordinator_worker_host_id` |

DB migration은 다음 경우에만 후속으로 추가한다.

- Plan 목록에서 offering/network/storage 조건 검색이 필요해지는 경우
- disk mapping을 별도 이력/감사 대상으로 조회해야 하는 경우
- 다중 NIC/disk mapping의 변경 이력을 row 단위로 추적해야 하는 경우

### 15.9 Agent/ftctl 설계

UI와 API는 ftctl에 직접 명령하지 않는다. Cloud backend가 canonical profile을 만들고 Agent가 ftctl에 전달한다.

| 구성요소 | 보강 |
| --- | --- |
| `FtctlDrUnifiedActionAdapter` | `mapping_json.target`과 `disks[]`가 완성된 경우에만 profile 생성 |
| Agent command | missing target placement는 host 명령 전 `CONFIG_INCOMPLETE`로 반환 |
| ftctl | `target.serviceOfferingId`, `target.networks[]`, `disks[].target.diskOfferingId`, `disks[].target.storageRef` 누락 시 실행 거부 |
| selftest | `VMWARE_TO_KVM` complete mapping과 missing offering/network/disk offering case 추가 |

### 15.10 구현 순서

1. KVM target site Zone 필수 저장 및 inventory resolver 보강
2. `discoverDrPlanInventory` 응답에 target service offering, disk offering, network, source disk/NIC 추가
3. VMware VM 상세 disk/NIC 조회 client 구현
4. DR Plan dialog에서 일반 mode input fallback 제거
5. disk mapping table UI 구현
6. typed parameter를 canonical mapping JSON으로 변환
7. `DrPlanReadinessValidator`에 KVM target placement 검증 추가
8. action eligibility와 `previewDrPlanSpec` 응답에 신규 blocker 노출
9. Agent/ftctl profile preflight와 selftest 보강
10. smoke: site inventory, Plan create draft, Plan execution-ready, sync action enablement 검증

### 15.11 AS-IS / TO-BE

| 구분 | AS-IS | TO-BE |
| --- | --- | --- |
| target storage | 후보 없으면 수동 입력 | 후보 select, 없으면 blocking warning |
| target compute | KVM host 후보를 compute처럼 사용 | service offering select로 분리 |
| target worker | 후보 없으면 수동 입력 | host select, 없으면 blocking warning |
| target network | 수동 입력 | network select |
| disk mapping | source disk 상세 없이 raw JSON 또는 빈 값 | source disk 조회 후 row 단위 mapping |
| Zone 누락 | option이 빈 배열이라 원인 파악 어려움 | `TARGET_SITE_ZONE_REQUIRED`로 명시 |
| 자동 선택 | 후보 1개일 때 일부 field 자동 선택 | 모든 선택형 field에 동일 정책 적용 |
| readiness | worker와 disk mapping 위주 | zone, worker, storage, service offering, network, disk offering까지 검증 |
| 일반 사용자 UX | 내부 ID를 알아야 할 수 있음 | 내부 ID 노출 없이 선택/검증 중심 |
| expert mode | 기본 흐름과 섞임 | 제한적 override로 격리 |
# 2026-07-06 Runtime Target Placement Contract Addendum

This addendum supersedes any ambiguous target placement wording below for `VMWARE_TO_KVM` and other KVM-target directions.

The DR Plan dialog must not ask a normal operator to type raw engine JSON or internal runtime paths. The UI collects guided selections, the API/backend stores canonical target placement in `dr_plan.mapping_json`, Agent forwards that canonical profile, and ftctl validates/derives runtime paths.

Required guided fields for a KVM target:

| Field | UI source | Saved contract |
| --- | --- | --- |
| Target Zone | Target DR Site Zone or inferred single enabled Zone | `mapping.target.zoneId` and legacy `targetZoneId` |
| Target worker | Host inventory select | `dr_plan.target_worker_host_id` and `mapping.target.workerHostId` |
| Coordinator worker | Host inventory select | `dr_plan.coordinator_worker_host_id` |
| Target storage | Storage pool inventory select | `mapping.target.storageRef`, `disks[].target.storageRef` |
| Service offering | Service offering inventory select | `mapping.target.serviceOfferingId` |
| Network | Network inventory select | `mapping.target.networks[]` |
| Source disks | VMware VM hardware discovery | `disks[].source.*` |
| Target disk name | Auto-generated editable text | `disks[].target.name` and top-level `targetRef` |
| Target disk offering | Disk offering inventory select | `disks[].target.diskOfferingId` |
| Target storage details | Backend storage inventory details | `disks[].target.storagePath`, `storagePoolType`, `storageHostAddress`, `krbdPath` |

`discoverDrPlanInventory` must return enough target storage details for runtime path derivation without exposing the operator to path editing:

- `targetstorageoptions[].details.path`
- `targetstorageoptions[].details.poolType`
- `targetstorageoptions[].details.hostAddress`
- `targetstorageoptions[].details.krbdPath`

`FtctlDrUnifiedActionAdapter` must merge `mapping.target` into `profile.target` before sending the Agent command. This ensures ftctl receives one endpoint object with both site/provider metadata and generated target placement.

ftctl must treat KVM target placement as a preflight contract:

| Missing value | Runtime result |
| --- | --- |
| Target Zone | `CONFIG_INCOMPLETE`, `DR_TARGET_MAPPING_INVALID:TARGET_SITE_ZONE_REQUIRED` |
| Target storage | `CONFIG_INCOMPLETE`, `DR_TARGET_MAPPING_INVALID:TARGET_STORAGE_REQUIRED` |
| Service offering | `CONFIG_INCOMPLETE`, `DR_TARGET_MAPPING_INVALID:TARGET_SERVICE_OFFERING_REQUIRED` |
| Network | `CONFIG_INCOMPLETE`, `DR_TARGET_MAPPING_INVALID:TARGET_NETWORK_REQUIRED` |
| Disk mapping | `CONFIG_INCOMPLETE`, `DR_TARGET_MAPPING_INVALID:DISK_MAPPING_REQUIRED` |
| Disk target name/path | `CONFIG_INCOMPLETE`, `DR_TARGET_MAPPING_INVALID:DISK_TARGET_REQUIRED:<index>` |
| Disk runtime path cannot be derived | `CONFIG_INCOMPLETE`, `DR_TARGET_MAPPING_INVALID:DISK_TARGET_PATH_REQUIRED:<index>` |
| Disk offering | `CONFIG_INCOMPLETE`, `DR_TARGET_MAPPING_INVALID:TARGET_DISK_OFFERING_REQUIRED:<index>` |

Path derivation is an engine concern, not a user input concern:

- RBD/KRBD storage: `krbdPath / target disk name`
- File storage: `storagePath / target disk name.qcow2`
- Explicit RBD spec: `rbd:<pool> / target disk name`

If none of those can be derived, runtime work must not start. No new table or column is required for this pass; target placement remains in `dr_site` Zone fields and `dr_plan.mapping_json`.

# 2026-07-06 Compact Guided Payload And Backend-Authoritative Placement Addendum

This addendum supersedes the earlier interim wording that allowed the UI-submitted `diskmappingsjson` to include storage runtime details such as `targetStoragePath`, `storagePoolType`, `storageHostAddress`, or `krbdPath`.

The structurally correct contract is:

1. UI collects operator selections and sends compact references only.
2. API accepts compact guided JSON with an explicit length limit large enough for multi-disk plans.
3. Backend resolves selected references against Cloud inventory and writes the canonical runtime placement to `dr_plan.mapping_json`.
4. Agent forwards only the backend-generated canonical profile.
5. ftctl validates the backend-generated profile and refuses runtime work when a path cannot be derived.

## A. UI Contract

`ui/src/views/infra/dr/DrPlanList.vue` keeps inventory option `detailsObject` for display only. Submit payload generation must not copy runtime paths from option details into `diskmappingsjson`.

Replace the current verbose disk payload builder with a compact builder:

```js
buildDiskMappingsJson () {
  if (!this.diskMappingRows.length) {
    return ''
  }
  return JSON.stringify(this.diskMappingRows.map((row, index) => ({
    sourceRef: row.sourceDiskRef,
    sourcePath: row.sourcePath,
    targetRef: row.targetDiskName,
    targetStorageRef: row.targetStorageRef,
    targetDiskOfferingId: row.targetDiskOfferingId,
    source: {
      diskRef: row.sourceDiskRef,
      label: row.sourceLabel,
      vmdkPath: row.sourcePath,
      capacityBytes: row.capacityBytes,
      boot: index === 0
    },
    target: {
      name: row.targetDiskName,
      storageRef: row.targetStorageRef,
      diskOfferingId: row.targetDiskOfferingId,
      format: 'qcow2'
    }
  })))
}
```

The following fields must never be trusted from UI submit payload:

- `targetStoragePath`
- `targetStorageType`
- `targetStorageKrbdPath`
- `target.storagePath`
- `target.storagePoolType`
- `target.storageHostAddress`
- `target.krbdPath`

Disk mapping layout must be modal-width safe:

```less
.cross-dr-disk-mapping-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  width: 100%;
}

.cross-dr-disk-field,
.cross-dr-disk-mapping-row .ant-input,
.cross-dr-disk-mapping-row .ant-select {
  min-width: 0;
  width: 100%;
}

@media (min-width: 720px) {
  .cross-dr-disk-mapping-row {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }

  .cross-dr-disk-source {
    grid-column: 1 / -1;
  }
}
```

If the table grows further, move the row rendering into `ui/src/components/dr/DrPlanDiskMappingTable.vue`, but keep `DrPlanList.vue` as the modal orchestration owner.

## B. API Contract

The guided JSON fields are still command parameters, so they must declare an explicit length. This is compatibility safety, not the primary mechanism for carrying runtime details.

Update these command classes:

- `CreateDrPlanCmd.java`
- `UpdateDrPlanCmd.java`
- `PreviewDrPlanSpecCmd.java`

Required annotation policy:

```java
@Parameter(name = "diskmappingsjson", type = CommandType.STRING,
        description = "the compact guided disk mapping JSON array", length = 65535)
private String diskMappingsJson;

@Parameter(name = "mappingjson", type = CommandType.STRING,
        description = "the plan mapping JSON", length = 65535)
private String mappingJson;

@Parameter(name = "schedulejson", type = CommandType.STRING,
        description = "the sync schedule JSON", length = 65535)
private String scheduleJson;

@Parameter(name = "policyjson", type = CommandType.STRING,
        description = "the plan policy JSON", length = 65535)
private String policyJson;

@Parameter(name = "quiescepolicyjson", type = CommandType.STRING,
        description = "the quiesce policy JSON", length = 65535)
private String quiescePolicyJson;
```

`diskmappingsjson` remains compact because a VM can still have enough disks to exceed the CloudStack default 255 character parameter limit.

## C. Backend Placement Resolver

Add a backend-only resolver so runtime placement is authoritative on the Cloud side:

```java
package com.cloud.dr;

public interface DrPlanTargetPlacementResolver {
    DrResolvedTargetPlacement resolve(DrPlanVO plan, DrPlanGuidedSpec spec);
}
```

Suggested implementation:

```java
public class DrPlanTargetPlacementResolverImpl implements DrPlanTargetPlacementResolver {
    @Inject private DataCenterDao dataCenterDao;
    @Inject private HostDao hostDao;
    @Inject private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject private DiskOfferingDao diskOfferingDao;
    @Inject private ServiceOfferingDao serviceOfferingDao;
    @Inject private NetworkDao networkDao;

    @Override
    public DrResolvedTargetPlacement resolve(DrPlanVO plan, DrPlanGuidedSpec spec) {
        // 1. Resolve target Zone from spec.targetZoneId or target DR Site Zone.
        // 2. Resolve target worker and ensure it belongs to the target Zone.
        // 3. Resolve target storage pool and expose backend-owned path/poolType/hostAddress/krbdPath.
        // 4. Resolve service offering and network IDs.
        // 5. Resolve each disk's storage and disk offering.
        // 6. Return blocking reasons rather than partially resolved runtime data.
    }
}
```

Suggested value objects:

```java
public class DrResolvedTargetPlacement {
    private Long zoneId;
    private Long workerHostId;
    private String targetVmName;
    private String storageRef;
    private String storagePath;
    private String storagePoolType;
    private String storageHostAddress;
    private String krbdPath;
    private String serviceOfferingId;
    private List<DrResolvedNetworkMapping> networks;
    private List<DrResolvedDiskMapping> disks;
    private List<String> blockingReasons;
}
```

`DrPlanGuidedSpecBuilder` must use this resolver before writing `mapping_json`:

```java
DrResolvedTargetPlacement placement = targetPlacementResolver.resolve(plan, spec);
JsonObject mapping = buildMapping(plan, spec, placement);
```

`parseDiskMappings()` must sanitize caller input. It may preserve source identity and selected target IDs, but it must drop runtime fields provided by the caller. Backend enrichment then writes:

- `mapping.target.zoneId`
- `mapping.target.workerHostId`
- `mapping.target.storageRef`
- `mapping.target.serviceOfferingId`
- `mapping.target.networks[]`
- `mapping.disks[].target.storageRef`
- `mapping.disks[].target.storagePath`
- `mapping.disks[].target.storagePoolType`
- `mapping.disks[].target.storageHostAddress`
- `mapping.disks[].target.krbdPath`
- `mapping.disks[].target.diskOfferingId`

`DrPlanReadinessValidator` must consume the same resolver result. This prevents one path from marking a plan ready while another path later fails during Agent or ftctl execution.

## D. Agent And ftctl Contract

`FtctlDrUnifiedActionAdapter` continues to merge `mapping.target` into `profile.target`, but it must only forward backend-generated runtime placement. If a legacy plan contains UI-injected path fields, a create/update pass should rewrite it through the resolver before execution readiness becomes true.

ftctl remains the final preflight gate:

- if backend did not provide enough storage data to derive a target path, return `CONFIG_INCOMPLETE`
- include `DR_TARGET_MAPPING_INVALID:DISK_TARGET_PATH_REQUIRED:<index>`
- do not start background copy or create partial runtime state

## E. DB Contract

No schema change is required for this improvement. Existing persistence remains:

- `dr_site.zone_id` / local site Zone fields
- `dr_plan.mapping_json`
- `dr_plan.schedule_json`
- `dr_plan.policy_json`
- `dr_plan.quiesce_policy_json`

The important change is ownership: selected IDs come from UI, runtime paths come from backend inventory, and executable profile data comes from backend-generated canonical JSON.

## F. AS-IS / TO-BE

| Area | AS-IS | TO-BE |
| --- | --- | --- |
| Disk mapping UI | Fixed 4-column grid can overflow modal width. | Responsive one/two-column layout with `min-width: 0` controls. |
| Submit payload | UI sends storage path, pool type, host address, and krbd path in `diskmappingsjson`. | UI sends compact selected IDs only. |
| API validation | `diskmappingsjson` uses default 255 character limit. | Guided JSON parameters declare explicit `length = 65535`. |
| Backend authority | Runtime details can be copied from UI option details. | Backend resolves storage/service/network/disk details from Cloud inventory. |
| Readiness | Validator parses already-built mapping and may drift from builder. | Builder and readiness share `DrPlanTargetPlacementResolver`. |
| Security | Malicious or stale UI payload could inject a runtime path. | Runtime path fields from UI are stripped and ignored. |
| Agent | Receives profile generated from mixed UI/backend data. | Receives backend-authoritative canonical placement. |
| ftctl | Protects runtime with final preflight only. | Keeps final preflight while Cloud catches the same blockers before execution. |

# 2026-07-06 Implementation Consistency Note

The implemented contract for guided DR Plan creation and edit is now:

1. UI sends compact guided selections only.
2. `CreateDrPlanCmd`, `UpdateDrPlanCmd`, and `PreviewDrPlanSpecCmd` accept guided JSON fields with `length = 65535`.
3. `DrPlanGuidedSpecBuilder` is a Spring-managed bean and uses `DrPlanTargetPlacementResolver`.
4. `DrPlanTargetPlacementResolverImpl` is the backend authority for KVM target Zone, worker, storage, service offering, network, and disk offering resolution.
5. `DrPlanReadinessValidator` uses the same resolver for KVM target plans.
6. Agent/ftctl receive backend-generated canonical `mapping.target` and `mapping.disks[]` only.
7. No DB schema change is required for this pass.

UI-submitted disk mappings may include source disk identity and selected target refs. They must not be treated as authoritative for these runtime fields:

- `targetStoragePath`
- `targetStorageType`
- `targetStorageKrbdPath`
- `target.storagePath`
- `target.storagePoolType`
- `target.storageHostAddress`
- `target.krbdPath`

Those fields are backend-owned and must be generated from Cloud inventory when the plan is previewed, created, or updated.

## 2026-07-07 Update: Target Storage Default And Disk-level Authority

DR Plan guided input now distinguishes default target storage from per-disk
target storage. The detailed layered design is documented in
[539-cross-hypervisor-dr-plan-storage-default-and-modal-layout-design-20260707.md](539-cross-hypervisor-dr-plan-storage-default-and-modal-layout-design-20260707.md).

Updated guided contract:

- `targetstorageref` is a default/fallback target storage value.
- `diskmappingsjson[].targetStorageRef` or
  `diskmappingsjson[].target.storageRef` is the authoritative target storage
  for that disk.
- The UI must not require `targetstorageref` when every disk row has a target
  storage value.
- The backend resolver must use disk-level storage first and only fall back to
  the default storage when the disk row does not specify storage.
- `DrPlanGuidedSpecBuilder` and `DrPlanReadinessValidator` must share the same
  disk-first storage precedence.
- No DB schema, Agent command, or ftctl CLI change is required.

## 2026-07-06 보강: Guided Spec 이후 실행 상태 계약

Guided spec은 DR Plan 생성 입력과 canonical JSON 생성을 담당한다. 생성 이후 action dispatch, FTCTL acceptance, runtime projection, 실패 복구 계약은 [533-cross-hypervisor-dr-dispatch-projection-recovery-design-20260706.md](533-cross-hypervisor-dr-dispatch-projection-recovery-design-20260706.md)를 따른다.

연계 원칙:

- preview/create 단계에서 생성한 canonical spec은 FTCTL profile의 입력이지만, action 성공을 의미하지 않는다.
- sync 시작 후 UI는 guided spec readiness가 아니라 latest run/projection state를 기준으로 실행 상태를 표시한다.
- target placement, disk/network mapping은 action 전 validation에서 확정하고, dispatch 실패 시 plan을 `SYNCING`으로 남기지 않는다.
- Agent timeout, FTCTL runtime 미생성, FTCTL engine reported failure는 서로 다른 error code로 저장한다.

## 2026-07-07 Update: Guided Spec Dialog Presentation Standard

Guided spec input is still the canonical DR Plan creation/edit payload, but the
dialog presentation must follow the SharedFS-style standard documented in
[540-cross-hypervisor-dr-plan-sharedfs-dialog-standard-design-20260707.md](540-cross-hypervisor-dr-plan-sharedfs-dialog-standard-design-20260707.md).

Guided spec rules:

- Keep guided payload fields unchanged.
- Present the guided form in semantic `a-collapse` sections: basic, sites,
  workload, objectives, target placement, disk mapping, workers, policy, and
  advanced.
- Use `planDialogSections.js` to map guided field names and readiness blocking
  reasons to the section that must be opened after validation failure.
- Keep JSON preview/override under the advanced section only; normal users
  should complete the plan through guided fields.
- Do not add section metadata to `mapping_json`, `schedule_json`, `policy_json`,
  or API requests.

The result is a clearer modal while preserving the guided spec contract already
used by backend, Agent dispatch, and ftctl profile generation.

## 2026-07-07 Update: Guided Dialog Alert And Gutter Refinement

The guided DR Plan dialog uses the SharedFS-style modal contract. Its dark-mode
alert and right-gutter correction is defined in
[540-cross-hypervisor-dr-plan-sharedfs-dialog-standard-design-20260707.md](540-cross-hypervisor-dr-plan-sharedfs-dialog-standard-design-20260707.md#13-2026-07-07-refinement-dark-alert-and-right-gutter).

Guided spec rules remain unchanged:

- guided field names are unchanged;
- generated `mapping_json`, `schedule_json`, `policy_json`, and
  `quiesce_policy_json` are unchanged;
- section and visual state remain UI-only;
- preview/create/update payloads must not include layout state.

Implementation must therefore modify CSS and, if needed, component class names
only. It must not modify `DrPlanGuidedSpecBuilder` or readiness validation for
this visual issue.

## 2026-07-10 Update: Backend-owned Source Hardware In Guided Spec V2

The guided dialog remains user-friendly, but the source hardware values shown
in its review panel are backend-owned. The UI must not synthesize or submit
trusted source firmware/Secure Boot values.

Updated contract:

- selecting a VMware source VM triggers detailed vCenter inventory through the
  backend;
- preview returns read-only `sourcehardware` and `resolvedtargethardware`;
- preview/create/update re-resolve the selected source VM server-side;
- `DrPlanGuidedSpecBuilder` persists canonical
  `mapping_json.source.hardware` plus its SHA-256 fingerprint;
- missing VMware firmware or Secure Boot status is a blocking reason, not a
  BIOS/LEGACY default;
- the review panel shows source boot, resolved target boot, controllers,
  `io_uring`, IOThreads, and inventory timestamp;
- a source hardware change after Plan creation requires re-preview before
  sync.

The canonical JSON, API fields, resolver rules, and tests are defined in
section 16 of
`548-cross-hypervisor-dr-agent-action-compatibility-and-state-convergence-design-20260709.md`.
