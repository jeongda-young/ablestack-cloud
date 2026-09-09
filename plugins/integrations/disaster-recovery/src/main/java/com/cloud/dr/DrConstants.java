// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.dr;

public final class DrConstants {
    public static final String ERROR_FENCE_CLEAR_INTERNAL_ONLY = "DR_FENCE_CLEAR_INTERNAL_ONLY";
    public static final String ERROR_TRANSITION_PREFLIGHT_INVALID = "DR_TRANSITION_PREFLIGHT_INVALID";
    public static final String ERROR_TRANSITION_AUTHORITY_INVALID = "DR_TRANSITION_AUTHORITY_INVALID";
    public static final String ERROR_SOURCE_ISOLATION_NOT_READY = "DR_SOURCE_ISOLATION_NOT_READY";
    public static final String ERROR_SOURCE_CLONE_FLATTEN_ACTIVE = "DR_SOURCE_CLONE_FLATTEN_ACTIVE";
    public static final String ERROR_TRANSITION_TARGET_NOT_SERVING = "DR_TRANSITION_TARGET_NOT_SERVING";
    public static final String ERROR_TRANSITION_TARGET_DOMAIN_NOT_FOUND = "DR_TRANSITION_TARGET_DOMAIN_NOT_FOUND";
    public static final String ERROR_TRANSITION_TARGET_HOST_UNREACHABLE = "DR_TRANSITION_TARGET_HOST_UNREACHABLE";
    public static final String ERROR_TRANSITION_ENGINE_PREFLIGHT_FAILED = "DR_TRANSITION_ENGINE_PREFLIGHT_FAILED";
    public static final String ERROR_AGENT_TRANSITION_PREFLIGHT_CONTRACT_MISMATCH =
            "DR_AGENT_TRANSITION_PREFLIGHT_CONTRACT_MISMATCH";
    public static final String ERROR_TEST_SESSION_BLOCKING = "DR_TEST_SESSION_BLOCKING";
    public static final String ENGINE_TYPE_FTCTL = "FTCTL";
    public static final String ENGINE_BINDING_TYPE_FTCTL = "FTCTL";
    public static final String ENGINE_TYPE_FTCTL_DR = "FTCTL_DR";
    public static final String ENGINE_BINDING_TYPE_FTCTL_DR = "FTCTL_DR";
    public static final String ENGINE_TYPE_VMWARE_PHASE1 = "VMWARE_PHASE1";
    public static final String ENGINE_BINDING_TYPE_VMWARE_PHASE1 = "VMWARE_PHASE1";
    public static final String ENGINE_TYPE_V2K = "V2K";
    public static final String ENGINE_BINDING_TYPE_V2K = "V2K";

    public static final String DIRECTION_KVM_TO_KVM = "KVM_TO_KVM";
    public static final String DIRECTION_KVM_TO_VMWARE = "KVM_TO_VMWARE";
    public static final String DIRECTION_VMWARE_TO_VMWARE = "VMWARE_TO_VMWARE";
    public static final String DIRECTION_VMWARE_TO_KVM = "VMWARE_TO_KVM";
    public static final String PROVIDER_PAIR_ABLESTACK_TO_ABLESTACK = "ABLESTACK_TO_ABLESTACK";
    public static final String PROVIDER_PAIR_ABLESTACK_TO_VMWARE = "ABLESTACK_TO_VMWARE";
    public static final String PROVIDER_PAIR_VMWARE_TO_ABLESTACK = "VMWARE_TO_ABLESTACK";
    public static final String PROVIDER_PAIR_VMWARE_TO_VMWARE = "VMWARE_TO_VMWARE";
    public static final String OPERATION_INTENT_FAILBACK_FINAL = "FAILBACK_FINAL";

    public static final String HYPERVISOR_TYPE_KVM = "KVM";
    public static final String HYPERVISOR_TYPE_VMWARE = "VMware";

    public static final String CREDENTIAL_TYPE_MOLD_API = "MOLD_API";
    public static final String CREDENTIAL_TYPE_VCENTER = "VCENTER";
    public static final String CREDENTIAL_STATE_CONFIGURED = "CONFIGURED";
    public static final String CREDENTIAL_STATE_CLEARED = "CLEARED";

    public static final String ADMIN_STATE_ENABLED = "ENABLED";
    public static final String ADMIN_STATE_DISABLED = "DISABLED";

    public static final String HEALTH_CONNECTED = "CONNECTED";
    public static final String HEALTH_DEGRADED = "DEGRADED";
    public static final String HEALTH_DISCONNECTED = "DISCONNECTED";
    public static final String HEALTH_UNKNOWN = "UNKNOWN";

    public static final String ACTION_REASON_CUTOVER_NOT_READY = "DR_ACTION_CUTOVER_NOT_READY";

    public static final String HEALTH_REASON_CREDENTIAL_MISSING = "CREDENTIAL_MISSING";
    public static final String HEALTH_REASON_CREDENTIAL_INVALID = "CREDENTIAL_INVALID";
    public static final String HEALTH_REASON_ENDPOINT_UNREACHABLE = "ENDPOINT_UNREACHABLE";
    public static final String HEALTH_REASON_ENDPOINT_HTTP_ERROR = "ENDPOINT_HTTP_ERROR";
    public static final String HEALTH_REASON_MOLD_API_OK = "MOLD_API_OK";
    public static final String HEALTH_REASON_VCENTER_API_OK = "VCENTER_API_OK";
    public static final String HEALTH_REASON_VCENTER_ENDPOINT_REACHABLE = "VCENTER_ENDPOINT_REACHABLE";
    public static final String HEALTH_REASON_UNSUPPORTED_SITE_TYPE = "UNSUPPORTED_SITE_TYPE";

    public static final String HEALTH_TRIGGER_MANUAL = "MANUAL";
    public static final String HEALTH_TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String HEALTH_TRIGGER_CREATE = "CREATE";
    public static final String HEALTH_TRIGGER_UPDATE = "UPDATE";
    public static final String HEALTH_TRIGGER_PREFLIGHT = "PREFLIGHT";

    public static final String PLAN_STATE_NEW = "NEW";
    public static final String PLAN_STATE_ENABLED = "ENABLED";
    public static final String PLAN_STATE_PHASE1_READY = "PHASE1_READY";
    public static final String PLAN_STATE_SYNCING = "SYNCING";
    public static final String PLAN_STATE_READY = "READY";
    public static final String PLAN_STATE_TESTING = "TESTING";
    public static final String PLAN_STATE_PAUSED = "PAUSED";
    public static final String PLAN_STATE_ERROR = "ERROR";
    public static final String PLAN_STATE_FAILED_OVER = "FAILED_OVER";
    public static final String PLAN_STATE_COMMIT_VERIFYING = "COMMIT_VERIFYING";
    public static final String PLAN_STATE_UNPROTECTED = "UNPROTECTED";

    public static final String CUTOVER_STATE_FAILED_BACK = "FAILED_BACK";
    public static final String CUTOVER_STATE_SUPERSEDED = "SUPERSEDED";
    public static final String AUTHORITY_SIDE_SOURCE = "SOURCE";
    public static final String AUTHORITY_SIDE_TARGET = "TARGET";
    public static final String AUTHORITY_INCONSISTENT_STALE_CUTOVER = "DR_AUTHORITY_STALE_CUTOVER";
    public static final String AUTHORITY_INCONSISTENT_TARGET_SESSION_MISSING = "DR_AUTHORITY_TARGET_SESSION_MISSING";

    public static final String REPLICA_STATE_NEW = "NEW";
    public static final String REPLICA_STATE_SKELETON_READY = "SKELETON_READY";
    public static final String REPLICA_STATE_PHASE1_READY = "PHASE1_READY";
    public static final String REPLICA_STATE_READY = "READY";
    public static final String REPLICA_STATE_ERROR = "ERROR";
    public static final String REPLICA_STATE_FAILED_OVER = "FAILED_OVER";
    public static final String REPLICA_POWER_STATE_POWERED_OFF = "POWERED_OFF";

    public static final String RUN_STATE_QUEUED = "QUEUED";
    public static final String RUN_STATE_PREPARING = "PREPARING";
    public static final String RUN_STATE_DISPATCHING = "DISPATCHING";
    public static final String RUN_STATE_ACCEPTED = "ACCEPTED";
    public static final String RUN_STATE_RUNNING = "RUNNING";
    public static final String RUN_STATE_RETRYING = "RETRYING";
    public static final String RUN_STATE_CANCEL_REQUESTED = "CANCEL_REQUESTED";
    public static final String RUN_STATE_SUCCEEDED = "SUCCEEDED";
    public static final String RUN_STATE_FAILED = "FAILED";
    public static final String RUN_STATE_CANCELED = "CANCELED";

    public static final String RUN_TYPE_SYNC = "SYNC";
    public static final String RUN_TYPE_RECOVER_SYNC = "RECOVER_SYNC";
    public static final String RUN_TYPE_PAUSE_SYNC = "PAUSE_SYNC";
    public static final String RUN_TYPE_RESUME_SYNC = "RESUME_SYNC";
    public static final String RUN_TYPE_TEST_FAILOVER = "TEST_FAILOVER";
    public static final String RUN_TYPE_TEST_CLEANUP = "TEST_CLEANUP";
    public static final String RUN_TYPE_FAILOVER = "FAILOVER";
    public static final String RUN_TYPE_FENCE_CONFIRM = "FENCE_CONFIRM";
    public static final String RUN_TYPE_FAILBACK = "FAILBACK";
    public static final String RUN_TYPE_REPROTECT = "REPROTECT";
    public static final String RUN_TYPE_ADOPT = "ADOPT";
    public static final String RUN_TYPE_RELEASE = "RELEASE";

    public static final String RELEASE_DISPOSITION_RETAIN_OPERATIONAL_VM = "RETAIN_OPERATIONAL_VM";
    public static final String RELEASE_DISPOSITION_DELETE_STANDBY_REPLICA = "DELETE_STANDBY_REPLICA";
    public static final String ERROR_RELEASE_DISPOSITION_INVALID = "DR_RELEASE_DISPOSITION_INVALID";
    public static final String ERROR_RELEASE_TARGET_AUTHORITY_ACTIVE = "DR_RELEASE_TARGET_AUTHORITY_ACTIVE";
    public static final String ERROR_RELEASE_TARGET_NOT_DELETABLE = "DR_RELEASE_TARGET_NOT_DELETABLE";

    public static final String SCHEDULER_RECOVERY_NONE = "NONE";
    public static final String SCHEDULER_RECOVERY_PENDING = "PENDING";
    public static final String SCHEDULER_RECOVERY_REQUIRED = "REQUIRED";
    public static final String SCHEDULER_RECOVERY_RECOVERING = "RECOVERING";
    public static final String SCHEDULER_RECOVERY_SUCCEEDED = "SUCCEEDED";
    public static final String SCHEDULER_RECOVERY_FAILED = "FAILED";
    public static final String SCHEDULER_RECOVERY_SUPPRESSED = "SUPPRESSED";

    public static final String STEP_STATE_QUEUED = "QUEUED";
    public static final String STEP_STATE_RUNNING = "RUNNING";
    public static final String STEP_STATE_SUCCEEDED = "SUCCEEDED";
    public static final String STEP_STATE_FAILED = "FAILED";
    public static final String STEP_STATE_CANCELED = "CANCELED";
    public static final String STEP_STATE_SKIPPED = "SKIPPED";
    public static final String STEP_STATE_RETRYING = "RETRYING";

    public static final String EVENT_SEVERITY_INFO = "INFO";
    public static final String EVENT_SEVERITY_WARN = "WARN";
    public static final String EVENT_SEVERITY_ERROR = "ERROR";
    public static final String EVENT_SOURCE_CLOUD = "CLOUD";
    public static final String EVENT_SOURCE_AGENT = "AGENT";
    public static final String EVENT_SOURCE_FTCTL = "FTCTL";
    public static final String EVENT_SOURCE_FTCTL_DR = "FTCTL_DR";
    public static final String EVENT_SOURCE_V2K = "V2K";

    public static final String EVENT_RUN_CREATED = "RUN_CREATED";
    public static final String EVENT_RUN_QUEUED = "RUN_QUEUED";
    public static final String EVENT_RUN_STARTED = "RUN_STARTED";
    public static final String EVENT_RUN_ACCEPTED = "RUN_ACCEPTED";
    public static final String EVENT_RUN_SUCCEEDED = "RUN_SUCCEEDED";
    public static final String EVENT_RUN_FAILED = "RUN_FAILED";
    public static final String EVENT_RUN_CANCELED = "RUN_CANCELED";
    public static final String EVENT_PROTECTION_PREPARED = "PROTECTION_PREPARED";
    public static final String EVENT_PROJECTION_REFRESH = "PROJECTION_REFRESH";
    public static final String EVENT_TARGET_MATERIALIZED = "TARGET_MATERIALIZED";
    public static final String EVENT_AGENT_CAPABILITY_CHECK = "AGENT_CAPABILITY_CHECK";
    public static final String EVENT_TARGET_MATERIALIZATION_RECOVERED = "TARGET_MATERIALIZATION_RECOVERED";
    public static final String EVENT_TEST_VM_ACTIVE = "TEST_VM_ACTIVE";

    public static final String ERROR_PLAN_NOT_FOUND = "DR_PLAN_NOT_FOUND";
    public static final String ERROR_SITE_NOT_FOUND = "DR_SITE_NOT_FOUND";
    public static final String ERROR_DUPLICATE_SITE = "DR_DUPLICATE_SITE";
    public static final String ERROR_DUPLICATE_PLAN = "DR_DUPLICATE_PLAN";
    public static final String ERROR_ACTIVE_PLAN_EXISTS = "DR_ACTIVE_PLAN_EXISTS";
    public static final String ERROR_ACTIVE_RUN_EXISTS = "DR_ACTIVE_RUN_EXISTS";
    public static final String ERROR_RUNTIME_RESOURCE_EXISTS = "DR_RUNTIME_RESOURCE_EXISTS";
    public static final String ERROR_RUN_NOT_FOUND = "DR_RUN_NOT_FOUND";
    public static final String ERROR_ENGINE_UNAVAILABLE = "DR_ENGINE_UNAVAILABLE";
    public static final String ERROR_AGENT_DISPATCH_TIMEOUT = "DR_AGENT_DISPATCH_TIMEOUT";
    public static final String ERROR_AGENT_UNAVAILABLE = "DR_AGENT_UNAVAILABLE";
    public static final String ERROR_AGENT_ACCEPT_TIMEOUT = "DR_AGENT_ACCEPT_TIMEOUT";
    public static final String ERROR_AGENT_CAPABILITY_MISMATCH = "DR_AGENT_CAPABILITY_MISMATCH";
    public static final String ERROR_FTCTL_ACTION_UNAVAILABLE = "DR_FTCTL_ACTION_UNAVAILABLE";
    public static final String ERROR_RUNTIME_STARTING = "DR_RUNTIME_STARTING";
    public static final String ERROR_RUNTIME_NOT_CREATED = "DR_RUNTIME_NOT_CREATED";
    public static final String ERROR_PROJECTION_UNAVAILABLE = "DR_PROJECTION_UNAVAILABLE";
    public static final String ERROR_FTCTL_PROTECTION_NOT_FOUND = "DR_FTCTL_PROTECTION_NOT_FOUND";
    public static final String ERROR_ACTION_UNSUPPORTED = "DR_ACTION_UNSUPPORTED";
    public static final String ERROR_ENGINE_BUSY = "DR_ENGINE_BUSY";
    public static final String ERROR_ENGINE_BUSY_RETRYABLE = "DR_ENGINE_BUSY_RETRYABLE";
    public static final String ERROR_ENGINE_BUSY_TIMEOUT = "DR_ENGINE_BUSY_TIMEOUT";
    public static final String ERROR_CONTROL_PROTOCOL_UNSUPPORTED = "DR_CONTROL_PROTOCOL_UNSUPPORTED";
    public static final String ERROR_ENGINE_WORKER_STALLED = "DR_ENGINE_WORKER_STALLED";
    public static final String ERROR_ENGINE_WORKER_FAILED = "DR_ENGINE_WORKER_FAILED";
    public static final String ERROR_ENGINE_ACTION_FAILED = "DR_ENGINE_ACTION_FAILED";
    public static final String ERROR_REVERSE_SNAPSHOT_OPEN_FAILED = "DR_REVERSE_SNAPSHOT_OPEN_FAILED";
    public static final String ERROR_TERMINAL_PUBLICATION_TIMEOUT = "DR_TERMINAL_PUBLICATION_TIMEOUT";
    public static final String ERROR_ENGINE_UNSUPPORTED = "DR_ENGINE_UNSUPPORTED";
    public static final String ERROR_STATUS_TIMEOUT = "DR_STATUS_TIMEOUT";
    public static final String ERROR_PROJECTION_STALE = "DR_PROJECTION_STALE";
    public static final String ERROR_MANAGEMENT_SERVING_PROCESS_STALE = "DR_MANAGEMENT_SERVING_PROCESS_STALE";
    public static final String ERROR_TARGET_UNAVAILABLE = "DR_TARGET_UNAVAILABLE";
    public static final String ERROR_TARGET_MAPPING_INVALID = "DR_TARGET_MAPPING_INVALID";
    public static final String ERROR_WORKER_BINDING_INVALID = "DR_WORKER_BINDING_INVALID";
    public static final String ERROR_TARGET_OWNERSHIP_CONFLICT = "DR_TARGET_OWNERSHIP_CONFLICT";
    public static final String ERROR_TARGET_NOT_READY = "DR_TARGET_NOT_READY";
    public static final String ERROR_TARGET_VM_NOT_FOUND = "DR_TARGET_VM_NOT_FOUND";
    public static final String ERROR_TARGET_STORAGE_NOT_FOUND = "DR_TARGET_STORAGE_NOT_FOUND";
    public static final String ERROR_TARGET_DISK_TYPE_INVALID = "DR_TARGET_DISK_TYPE_INVALID";
    public static final String ERROR_TARGET_DISK_MAPPING_INVALID = "DR_TARGET_DISK_MAPPING_INVALID";
    public static final String ERROR_TARGET_DISK_SIZE_UNRESOLVED = "DR_TARGET_DISK_SIZE_UNRESOLVED";
    public static final String ERROR_TARGET_DISK_PREPARE_FAILED = "DR_TARGET_DISK_PREPARE_FAILED";
    public static final String ERROR_CUTOVER_MANIFEST_INVALID = "DR_CUTOVER_MANIFEST_INVALID";
    public static final String ERROR_GUEST_OS_UNRESOLVED = "DR_GUEST_OS_UNRESOLVED";
    public static final String ERROR_TARGET_DISK_MAP_MISSING = "DR_TARGET_DISK_MAP_MISSING";
    public static final String ERROR_TARGET_DISK_LOCATOR_INVALID = "DR_TARGET_DISK_LOCATOR_INVALID";
    public static final String ERROR_TARGET_DISK_NOT_DURABLE = "DR_TARGET_DISK_NOT_DURABLE";
    public static final String ERROR_FORWARD_TARGET_MAP_MISSING = "DR_FORWARD_TARGET_MAP_MISSING";
    public static final String ERROR_FORWARD_TARGET_MAP_INVALID = "DR_FORWARD_TARGET_MAP_INVALID";
    public static final String ERROR_FORWARD_TARGET_MAP_GENERATOR_UNAVAILABLE = "DR_FORWARD_TARGET_MAP_GENERATOR_UNAVAILABLE";
    public static final String ERROR_FORWARD_TARGET_MAP_CARDINALITY_MISMATCH = "DR_FORWARD_TARGET_MAP_CARDINALITY_MISMATCH";
    public static final String ERROR_FORWARD_TARGET_LOCATOR_INVALID = "DR_FORWARD_TARGET_LOCATOR_INVALID";
    public static final String ERROR_GUEST_PREP_RUNTIME_UNAVAILABLE = "DR_GUEST_PREP_RUNTIME_UNAVAILABLE";
    public static final String ERROR_GUEST_PREP_SESSION_MISSING = "DR_GUEST_PREP_SESSION_MISSING";
    public static final String ERROR_GUEST_PREP_MANIFEST_TOOL_MISSING = "DR_GUEST_PREP_MANIFEST_TOOL_MISSING";
    public static final String ERROR_GUEST_PREP_V2K_RUNTIME_MISSING = "DR_GUEST_PREP_V2K_RUNTIME_MISSING";
    public static final String ERROR_GUEST_PREP_PROFILE_INVALID = "DR_GUEST_PREP_PROFILE_INVALID";
    public static final String ERROR_GUEST_PREP_WINPE_ISO_MISSING = "DR_GUEST_PREP_WINPE_ISO_MISSING";
    public static final String ERROR_GUEST_PREP_VIRTIO_ISO_MISSING = "DR_GUEST_PREP_VIRTIO_ISO_MISSING";
    public static final String ERROR_GUEST_PREPARATION_FAILED = "DR_GUEST_PREPARATION_FAILED";
    public static final String ERROR_TEST_CHECKPOINT_GUEST_FS_INCONSISTENT =
            "DR_TEST_CHECKPOINT_GUEST_FS_INCONSISTENT";
    public static final String ERROR_SOURCE_ISOLATION_UNCONFIRMED = "DR_SOURCE_ISOLATION_UNCONFIRMED";
    public static final String ERROR_TARGET_STORAGE_UNRESOLVED = "DR_TARGET_STORAGE_UNRESOLVED";
    public static final String ERROR_TARGET_VM_MATERIALIZE_FAILED = "DR_TARGET_VM_MATERIALIZE_FAILED";
    public static final String ERROR_REPROTECT_AUTHORITY_INVALID = "DR_REPROTECT_AUTHORITY_INVALID";
    public static final String ERROR_REPROTECT_REQUIRES_TARGET_ACTIVE = "DR_REPROTECT_REQUIRES_TARGET_ACTIVE";
    public static final String ERROR_REPROTECT_CUTOVER_NOT_COMMITTED = "DR_REPROTECT_CUTOVER_NOT_COMMITTED";
    public static final String ERROR_REPROTECT_TARGET_IDENTITY_INVALID = "DR_REPROTECT_TARGET_IDENTITY_INVALID";
    public static final String ERROR_REPROTECT_CHECKPOINT_MISMATCH = "DR_REPROTECT_CHECKPOINT_MISMATCH";
    public static final String ERROR_REPROTECT_TARGET_RUNTIME_NOT_RUNNING = "DR_REPROTECT_TARGET_RUNTIME_NOT_RUNNING";
    public static final String ERROR_REPROTECT_TARGET_RUNTIME_UNKNOWN = "DR_REPROTECT_TARGET_RUNTIME_UNKNOWN";
    public static final String ERROR_FAILBACK_INLINE_CREDENTIAL_UNSUPPORTED = "DR_FAILBACK_INLINE_CREDENTIAL_UNSUPPORTED";
    public static final String ERROR_FAILBACK_PREFLIGHT_NOT_READY = "DR_FAILBACK_PREFLIGHT_NOT_READY";
    public static final String ERROR_FAILBACK_REQUIRES_TARGET_ACTIVE = "DR_FAILBACK_REQUIRES_TARGET_ACTIVE";
    public static final String ERROR_FAILBACK_SITE_NOT_READY = "DR_FAILBACK_SITE_NOT_READY";
    public static final String ERROR_FAILBACK_CREDENTIAL_NOT_READY = "DR_FAILBACK_CREDENTIAL_NOT_READY";
    public static final String ERROR_FAILBACK_CHECKPOINT_NOT_READY = "DR_FAILBACK_CHECKPOINT_NOT_READY";
    public static final String ERROR_FAILBACK_COMMIT_ACK_PENDING = "DR_FAILBACK_COMMIT_ACK_PENDING";
    public static final String ERROR_ACTION_SECRET_INPUT_FORBIDDEN = "DR_ACTION_SECRET_INPUT_FORBIDDEN";
    public static final String ERROR_ACTION_INTENT_MISMATCH = "DR_ACTION_INTENT_MISMATCH";
    public static final String ERROR_ACTION_IDEMPOTENCY_CONFLICT = "DR_ACTION_IDEMPOTENCY_CONFLICT";
    public static final String ERROR_SOURCE_HARDWARE_CHANGED = "SOURCE_HARDWARE_CHANGED";
    public static final String ERROR_TARGET_VM_HARDWARE_MISMATCH = "TARGET_VM_HARDWARE_MISMATCH";
    public static final String ERROR_TARGET_VOLUME_IMPORT_FAILED = "DR_TARGET_VOLUME_IMPORT_FAILED";
    public static final String ERROR_TARGET_NETWORK_MATERIALIZE_FAILED = "DR_TARGET_NETWORK_MATERIALIZE_FAILED";
    public static final String ERROR_SOURCE_DISK_SIZE_UNKNOWN = "DR_SOURCE_DISK_SIZE_UNKNOWN";
    public static final String ERROR_ABLESTACK_DRIVER_FAILED = "DR_ABLESTACK_DRIVER_FAILED";
    public static final String ERROR_VMWARE_MOVER_UNAVAILABLE = "DR_VMWARE_MOVER_UNAVAILABLE";
    public static final String ERROR_VMWARE_MOVER_FAILED = "DR_VMWARE_MOVER_FAILED";
    public static final String ERROR_VMWARE_MOVER_SOURCE_GRAPH_INVALID = "DR_VMWARE_MOVER_SOURCE_GRAPH_INVALID";
    public static final String ERROR_VMWARE_NBDKIT_FAILED = "DR_VMWARE_NBDKIT_FAILED";
    public static final String ERROR_VMWARE_VDDK_CONNECT_INVALID = "DR_VMWARE_VDDK_CONNECT_INVALID";
    public static final String ERROR_VMWARE_VDDK_THUMBPRINT_UNRESOLVED = "DR_VMWARE_VDDK_THUMBPRINT_UNRESOLVED";
    public static final String ERROR_VMWARE_VDDK_EXPORT_UNAVAILABLE = "DR_VMWARE_VDDK_EXPORT_UNAVAILABLE";
    public static final String ERROR_VMWARE_VDDK_SOURCE_LOCKED = "DR_VMWARE_VDDK_SOURCE_LOCKED";
    public static final String ERROR_VMWARE_VDDK_OPEN_DENIED = "DR_VMWARE_VDDK_OPEN_DENIED";
    public static final String ERROR_VMWARE_CBT_DISABLED = "DR_VMWARE_CBT_DISABLED";
    public static final String ERROR_VMWARE_CBT_ENABLE_FAILED = "DR_VMWARE_CBT_ENABLE_FAILED";
    public static final String ERROR_VMWARE_CBT_VERIFY_FAILED = "DR_VMWARE_CBT_VERIFY_FAILED";
    public static final String ERROR_VMWARE_CBT_DISK_ID_UNRESOLVED = "DR_VMWARE_CBT_DISK_ID_UNRESOLVED";
    public static final String ERROR_VMWARE_CBT_CHANGE_ID_MISSING = "DR_VMWARE_CBT_CHANGE_ID_MISSING";
    public static final String ERROR_VMWARE_CBT_QUERY_FAILED = "DR_VMWARE_CBT_QUERY_FAILED";
    public static final String ERROR_VMWARE_CBT_SNAPSHOT_CONFLICT = "DR_VMWARE_CBT_SNAPSHOT_CONFLICT";
    public static final String ERROR_VMWARE_SNAPSHOT_REF_UNRESOLVED = "DR_VMWARE_SNAPSHOT_REF_UNRESOLVED";
    public static final String ERROR_VMWARE_SNAPSHOT_CLEANUP_REQUIRED = "DR_VMWARE_SNAPSHOT_CLEANUP_REQUIRED";
    public static final String ERROR_CBT_METRICS_INVALID = "DR_CBT_METRICS_INVALID";
    public static final String ERROR_CBT_LOCAL_COMMIT_FAILED = "DR_CBT_LOCAL_COMMIT_FAILED";
    public static final String ERROR_VDDK_LIBDIR_UNRESOLVED = "DR_VDDK_LIBDIR_UNRESOLVED";
    public static final String ERROR_VDDK_LIBRARY_LOAD_FAILED = "DR_VDDK_LIBRARY_LOAD_FAILED";
    public static final String ERROR_RESTORE_POINT_NOT_FOUND = "DR_RESTORE_POINT_NOT_FOUND";
    public static final String ERROR_DURABLE_CHECKPOINT_NOT_FOUND = "DR_DURABLE_CHECKPOINT_NOT_FOUND";
    public static final String ERROR_CREDENTIAL_INVALID = "DR_CREDENTIAL_INVALID";
    public static final String ERROR_V2K_TASK_NOT_FOUND = "DR_V2K_TASK_NOT_FOUND";
    public static final String ERROR_V2K_PHASE1_REQUIRED = "DR_V2K_PHASE1_REQUIRED";
    public static final String ERROR_V2K_PHASE2_REQUIRED = "DR_V2K_PHASE2_REQUIRED";

    private DrConstants() {
    }
}
