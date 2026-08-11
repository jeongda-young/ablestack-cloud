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

# Europa SharedFS PR #878/#879 integration

## Scope

This change ports the reusable SharedFS work from upstream PR #878 and PR #879
to `ablestack-europa`. The port keeps Europa's current Storage Service UI and
schema ownership, and deliberately excludes the Diplo-only backup schema typo
fix from PR #879.

## Integration order

1. Stabilize `createSharedFileSystem` request construction and customized IOPS validation.
2. Reconcile existing SharedFS instances to the Storage Service runtime and use the SharedFS feature gate as the single gate.
3. Apply build, development-release, SystemVM timestamp, and RPM upgrade safeguards.
4. Add L2 static IPv4 desired state from API through persistence, VM deployment, QGA, and UI.
5. Validate API, server, KVM wrapper, schema, and UI behavior before publication.

## AS-IS / TO-BE

| Area | AS-IS | TO-BE |
|---|---|---|
| Create request | Empty optional values can become API parameters; customized IOPS can be partially supplied | Null and undefined values are omitted; customized IOPS require a positive min/max pair with min not greater than max |
| Feature gate | SharedFS and Storage Service can be governed by separate settings | `sharedfs.feature.enabled` governs the complete Europa SharedFS/Storage Service surface |
| Existing instances | Runtime compatibility depends on a later manual action | Eligible existing SharedFS instances are reconciled when management starts |
| Listener NIC | Runtime registration can retain a stale primary address | The current primary guest NIC address is synchronized before listener registration |
| L2 networking | SharedFS creation requires UserData or ConfigDrive | L2 can use an explicit static IPv4/prefix, optional gateway, and optional DNS values |
| SystemVM build | Artifact timestamps can vary in formatting | SystemVM timestamps are zero-padded and deterministic |
| RPM upgrade | Service state can be changed unintentionally | Running/enabled state is captured and restored across package upgrade |
| Development release | A stale draft can block replacement | Existing draft development releases are replaced safely |

## Database design

`shared_filesystem` stores network desired state so restart and reconciliation
do not depend on transient request data.

| Column | Type/default | Meaning |
|---|---|---|
| `network_mode` | `varchar(16) NOT NULL DEFAULT 'DHCP'` | `DHCP` or `STATIC` |
| `ip_address` | `varchar(45) NULL` | Requested host IPv4 address |
| `cidr` | `varchar(45) NULL` | Calculated network CIDR |
| `gateway` | `varchar(45) NULL` | Optional default gateway |
| `dns1` | `varchar(45) NULL` | Optional primary DNS |
| `dns2` | `varchar(45) NULL` | Optional secondary DNS |

Fresh 4.20-family installations receive the fields in
`schema-41910to42000.sql`. Existing Europa installations receive the same
fields through idempotent calls in `schema-Europa-After.sql`. The canonical
`cloud.shared_filesystem_view.sql` projects all six fields for API responses.
No Diplo backup schema migration is part of this port.

## API and backend

`createSharedFileSystem` accepts `networkmode`, `ipcidr`, `gateway`, `dns1`,
and `dns2`. Static mode is valid only for an L2 network. The server validates
IPv4/prefix syntax, address range, gateway membership, reserved addresses, and
duplicate NIC assignment before allocation. The selected address is reserved
during VM deployment and the persisted desired state is returned by
`listSharedFileSystems`.

The KVM wrapper exposes one fixed QGA operation,
`configure-sharedfs-static-network`. It writes a SharedFS-only state file,
helper, and systemd oneshot unit, then verifies the guest address. Generic
Storage Service commands continue through `ablestack-storagectl`.

## UI

The existing Europa SharedFS create dialog keeps its storage, protocol, and
dark-mode layout. Selecting an L2 network shows a DHCP/static selector. If the
network does not advertise UserData, static mode is selected and DHCP is
disabled. Changing networks clears stale address values. The request builder
emits static fields only in static mode while preserving customized IOPS and
storage selection fields.

## Verification

- API helper and SharedFS create request unit tests
- SharedFS service and Storage Service reconciliation unit tests
- NIC DAO and KVM QGA wrapper unit tests
- locale JSON parsing and schema consistency checks
- production UI build and focused Maven module tests
- PR changed-file and CI review before merge
