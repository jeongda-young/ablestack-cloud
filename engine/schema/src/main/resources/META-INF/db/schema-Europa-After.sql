-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--   http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing,
-- software distributed under the License is distributed on an
-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
-- KIND, either express or implied.  See the License for the
-- specific language governing permissions and limitations
-- under the License.

--;
-- Schema upgrade from ablestack-allo to ablestack-bronto
--;

-- Add v2k_step column to import_vm_task table for ablestack-v2k workflow status persistence
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','v2k_step', 'varchar(32) DEFAULT ''None'' COMMENT "Ablestack-v2k importing VM task step"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','cluster_id', 'bigint unsigned COMMENT "Cluster ID used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','service_offering_id', 'bigint unsigned COMMENT "Service offering ID used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','v2k_target_storage_pool_id', 'bigint unsigned COMMENT "Primary storage pool ID used as ablestack-v2k target"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','source_cluster_name', 'varchar(255) COMMENT "Source VMware cluster name used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','source_host_name', 'varchar(255) COMMENT "Source VMware host name used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','vcenter_id', 'bigint unsigned COMMENT "Existing vCenter ID used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','vcenter_username', 'varchar(255) COMMENT "vCenter username used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','vcenter_password', 'varchar(255) COMMENT "vCenter password used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','service_offering_details', 'text COMMENT "Serialized custom service offering details used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','nic_network_map', 'text COMMENT "Serialized NIC selection map used by the import task, including network and optional IP address"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','migration_tool', 'varchar(32) DEFAULT ''legacy'' COMMENT "Migration tool used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','source_provider', 'varchar(32) COMMENT "Source provider used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','target_provider', 'varchar(32) COMMENT "Target provider used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','target_profile', 'varchar(64) COMMENT "Resolved target profile used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','target_storage_pool_id', 'bigint unsigned COMMENT "Resolved target primary storage pool ID used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','target_format', 'varchar(16) COMMENT "Resolved target disk format used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','target_storage_type', 'varchar(32) COMMENT "Resolved target storage type used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','target_vm_name', 'varchar(255) COMMENT "Target VM name used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','source_endpoint', 'varchar(255) COMMENT "Source endpoint used by the import task without secrets"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','source_ref', 'varchar(255) COMMENT "Provider-specific source VM reference used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','source_inventory_json', 'mediumtext COMMENT "Serialized source VM inventory snapshot"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','source_context_json', 'mediumtext COMMENT "Serialized non-secret source context"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','source_credential_id', 'bigint unsigned COMMENT "Encrypted credential row used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','target_context_json', 'mediumtext COMMENT "Serialized target context and disk map"');
CALL `cloud`.`IDEMPOTENT_CHANGE_COLUMN`('cloud.import_vm_task', 'source_inventory_json', 'source_inventory_json', 'mediumtext COMMENT "Serialized source VM inventory snapshot"');
CALL `cloud`.`IDEMPOTENT_CHANGE_COLUMN`('cloud.import_vm_task', 'source_context_json', 'source_context_json', 'mediumtext COMMENT "Serialized non-secret source context"');
CALL `cloud`.`IDEMPOTENT_CHANGE_COLUMN`('cloud.import_vm_task', 'target_context_json', 'target_context_json', 'mediumtext COMMENT "Serialized target context and disk map"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','workdir', 'varchar(1024) COMMENT "Tool workdir used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','split_mode', 'varchar(16) COMMENT "Requested split mode used by the import task"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','current_phase', 'varchar(32) COMMENT "Current normalized migration phase"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','migration_state', 'varchar(32) COMMENT "Current normalized migration state"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','migration_step', 'varchar(255) COMMENT "Current normalized migration step"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','cutover_policy', 'varchar(32) COMMENT "Cutover policy used by phase2 or full migration"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','status_json', 'mediumtext COMMENT "Latest normalized migration status payload"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.import_vm_task','error_code', 'varchar(64) COMMENT "Normalized migration error code"');

ALTER TABLE `cloud`.`import_vm_task` CONVERT TO CHARACTER SET utf8mb4;

CALL `cloud`.`IDEMPOTENT_ADD_KEY`('i_import_vm_task__zone_tool_state_created', 'cloud.import_vm_task', '(`zone_id`, `migration_tool`, `state`, `created`)');
CALL `cloud`.`IDEMPOTENT_ADD_KEY`('i_import_vm_task__zone_source_state_created', 'cloud.import_vm_task', '(`zone_id`, `source_provider`, `state`, `created`)');
CALL `cloud`.`IDEMPOTENT_ADD_KEY`('i_import_vm_task__target_phase_state', 'cloud.import_vm_task', '(`target_provider`, `current_phase`, `migration_state`)');
CALL `cloud`.`IDEMPOTENT_ADD_KEY`('i_import_vm_task__source_credential_id', 'cloud.import_vm_task', '(`source_credential_id`)');

CREATE TABLE IF NOT EXISTS `cloud`.`import_vm_task_event`(
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40) NOT NULL COMMENT 'UUID',
    `task_id` bigint unsigned NOT NULL COMMENT 'Import VM task ID',
    `event_type` varchar(64) NOT NULL COMMENT 'Import VM task event type',
    `phase` varchar(32) COMMENT 'Migration phase at event time',
    `state` varchar(32) COMMENT 'Migration state at event time',
    `step` varchar(255) COMMENT 'Migration step at event time',
    `message` text COMMENT 'Import VM task event message',
    `payload_json` mediumtext COMMENT 'Serialized event payload without secrets',
    `created` datetime NOT NULL COMMENT 'date created',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_import_vm_task_event__task_id` FOREIGN KEY `fk_import_vm_task_event__task_id` (`task_id`) REFERENCES `import_vm_task`(`id`) ON DELETE CASCADE,
    INDEX `i_import_vm_task_event__task_id_created`(`task_id`, `created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Storage Service is governed by the SharedFS feature gate in Europa.
DELETE FROM `cloud`.`configuration` WHERE `name` = 'storage.service.feature.enabled';

-- Persist the desired SharedFS L2 static network configuration for fresh and upgraded Europa environments.
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.shared_filesystem', 'network_mode', 'varchar(16) NOT NULL DEFAULT ''DHCP'' COMMENT "SharedFS VM network addressing mode"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.shared_filesystem', 'ip_address', 'varchar(45) DEFAULT NULL COMMENT "Requested static IPv4 address"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.shared_filesystem', 'cidr', 'varchar(45) DEFAULT NULL COMMENT "Requested static IPv4 CIDR"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.shared_filesystem', 'gateway', 'varchar(45) DEFAULT NULL COMMENT "Requested static IPv4 gateway"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.shared_filesystem', 'dns1', 'varchar(45) DEFAULT NULL COMMENT "Requested primary DNS server"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.shared_filesystem', 'dns2', 'varchar(45) DEFAULT NULL COMMENT "Requested secondary DNS server"');

CREATE TABLE IF NOT EXISTS `cloud`.`import_vm_task_credential`(
    `id` bigint unsigned NOT NULL auto_increment COMMENT 'id',
    `uuid` varchar(40) NOT NULL COMMENT 'UUID',
    `task_id` bigint unsigned NOT NULL COMMENT 'Import VM task ID',
    `provider` varchar(32) NOT NULL COMMENT 'Credential source provider',
    `credential_type` varchar(32) NOT NULL COMMENT 'Credential type',
    `username_hint` varchar(255) COMMENT 'Non-secret username hint',
    `encrypted_payload` mediumtext NOT NULL COMMENT 'Encrypted credential payload',
    `encryption_version` varchar(32) NOT NULL COMMENT 'Credential encryption version',
    `key_id` varchar(128) COMMENT 'Credential encryption key ID',
    `created` datetime NOT NULL COMMENT 'date created',
    `updated` datetime COMMENT 'date updated if not null',
    `removed` datetime COMMENT 'date removed if not null',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_import_vm_task_credential__task_id` FOREIGN KEY `fk_import_vm_task_credential__task_id` (`task_id`) REFERENCES `import_vm_task`(`id`) ON DELETE CASCADE,
    INDEX `i_import_vm_task_credential__task_id_created`(`task_id`, `created`),
    INDEX `i_import_vm_task_credential__task_id_removed`(`task_id`, `removed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `cloud`.`import_vm_task_event` CONVERT TO CHARACTER SET utf8mb4;
ALTER TABLE `cloud`.`import_vm_task_credential` CONVERT TO CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS `cloud`.`vm_guest_network_state` (
    `id` bigint unsigned NOT NULL auto_increment,
    `vm_id` bigint unsigned NOT NULL COMMENT 'VM instance id',
    `schema_version` smallint unsigned NOT NULL DEFAULT 1 COMMENT 'Guest network payload schema version',
    `status` varchar(32) NOT NULL COMMENT 'Current collection status',
    `qga_version` varchar(64) COMMENT 'Last observed QEMU guest agent version',
    `collector_build_id` varchar(128) COMMENT 'Collector build or manifest identifier',
    `collector_host_id` bigint unsigned COMMENT 'Host that produced the snapshot',
    `capability_hash` char(64) COMMENT 'SHA-256 of enabled QGA capabilities',
    `guest_tools_version` varchar(64) COMMENT 'ABLESTACK guest tools version',
    `qga_policy_mode` varchar(16) COMMENT 'Observed QGA RPC policy mode',
    `readiness_status` varchar(32) COMMENT 'Guest tools readiness status',
    `readiness_checked_at` datetime COMMENT 'Last readiness check time',
    `observed_at` datetime NOT NULL COMMENT 'Last collection attempt time',
    `last_success_at` datetime COMMENT 'Last successful collection time',
    `payload_hash` char(64) COMMENT 'SHA-256 of canonical payload',
    `payload` mediumtext COMMENT 'Canonical guest network state JSON',
    `error_code` varchar(64) COMMENT 'Structured collection error code',
    `error_message` varchar(255) COMMENT 'Bounded collection error message',
    `created` datetime NOT NULL COMMENT 'Creation time',
    `updated` datetime NOT NULL COMMENT 'Last update time',
    PRIMARY KEY (`id`),
    CONSTRAINT `uc_vm_guest_network_state__vm_id` UNIQUE (`vm_id`),
    CONSTRAINT `fk_vm_guest_network_state__vm_id` FOREIGN KEY `fk_vm_guest_network_state__vm_id` (`vm_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE,
    INDEX `i_vm_guest_network_state__status_observed_at` (`status`, `observed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_guest_network_state','collector_build_id', 'varchar(128) COMMENT "Collector build or manifest identifier"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_guest_network_state','collector_host_id', 'bigint unsigned COMMENT "Host that produced the snapshot"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_guest_network_state','capability_hash', 'char(64) COMMENT "SHA-256 of enabled QGA capabilities"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_guest_network_state','guest_tools_version', 'varchar(64) COMMENT "ABLESTACK guest tools version"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_guest_network_state','qga_policy_mode', 'varchar(16) COMMENT "Observed QGA RPC policy mode"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_guest_network_state','readiness_status', 'varchar(32) COMMENT "Guest tools readiness status"');
CALL `cloud`.`IDEMPOTENT_ADD_COLUMN`('cloud.vm_guest_network_state','readiness_checked_at', 'datetime COMMENT "Last readiness check time"');

CREATE TABLE IF NOT EXISTS `cloud`.`vm_guest_network_section_state` (
    `id` bigint unsigned NOT NULL auto_increment,
    `vm_id` bigint unsigned NOT NULL,
    `section` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL DEFAULT 'NOT_COLLECTED',
    `source` varchar(64),
    `observed_at` datetime,
    `last_success_at` datetime,
    `next_due_at` datetime NOT NULL,
    `failure_count` smallint unsigned NOT NULL DEFAULT 0,
    `error_code` varchar(64),
    `error_message` varchar(255),
    `payload_hash` char(64),
    `payload` mediumtext,
    `lease_owner` varchar(128),
    `lease_until` datetime,
    `created` datetime NOT NULL,
    `updated` datetime NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uc_vm_guest_network_section__vm_section` (`vm_id`, `section`),
    KEY `i_vm_guest_network_section__due_lease` (`next_due_at`, `lease_until`),
    KEY `i_vm_guest_network_section__vm_status` (`vm_id`, `status`),
    CONSTRAINT `fk_vm_guest_network_section__vm_id`
      FOREIGN KEY (`vm_id`) REFERENCES `vm_instance` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
