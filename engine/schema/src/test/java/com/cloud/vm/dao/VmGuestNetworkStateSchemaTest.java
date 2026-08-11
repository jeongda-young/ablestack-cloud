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
package com.cloud.vm.dao;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class VmGuestNetworkStateSchemaTest {
    private static final String TABLE_NAME = "vm_guest_network_state";

    @Test
    public void testFreshAndUpgradeSchemasContainTheSameRequiredContract() throws IOException {
        Path root = findRepositoryRoot();
        String fresh = read(root.resolve("setup/db/create-schema.sql"));
        String upgrade = read(root.resolve(
                "engine/schema/src/main/resources/META-INF/db/schema-Europa-After.sql"));

        assertSchemaContract(fresh, false);
        assertSchemaContract(upgrade, true);
    }

    private Path findRepositoryRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("setup/db/create-schema.sql"))
                    && Files.isDirectory(current.resolve("engine/schema"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate the repository root");
    }

    private void assertSchemaContract(String schema, boolean idempotentCreate) {
        String table = tableBlock(schema);
        assertTrue(table.contains((idempotentCreate ? "CREATE TABLE IF NOT EXISTS" : "CREATE TABLE")
                + " `cloud`.`" + TABLE_NAME + "`"));
        assertTrue(table.contains("`id` bigint unsigned NOT NULL auto_increment"));
        assertTrue(table.contains("`vm_id` bigint unsigned NOT NULL"));
        assertTrue(table.contains("`schema_version` smallint unsigned NOT NULL DEFAULT 1"));
        assertTrue(table.contains("`status` varchar(32) NOT NULL"));
        assertTrue(table.contains("`qga_version` varchar(64)"));
        assertTrue(table.contains("`collector_build_id` varchar(128)"));
        assertTrue(table.contains("`collector_host_id` bigint unsigned"));
        assertTrue(table.contains("`capability_hash` char(64)"));
        assertTrue(table.contains("`guest_tools_version` varchar(64)"));
        assertTrue(table.contains("`qga_policy_mode` varchar(16)"));
        assertTrue(table.contains("`readiness_status` varchar(32)"));
        assertTrue(table.contains("`readiness_checked_at` datetime"));
        assertTrue(table.contains("`observed_at` datetime NOT NULL"));
        assertTrue(table.contains("`last_success_at` datetime"));
        assertTrue(table.contains("`payload_hash` char(64)"));
        assertTrue(table.contains("`payload` mediumtext"));
        assertTrue(table.contains("`error_code` varchar(64)"));
        assertTrue(table.contains("`error_message` varchar(255)"));
        assertTrue(table.contains("`created` datetime NOT NULL"));
        assertTrue(table.contains("`updated` datetime NOT NULL"));
        assertTrue(table.contains("PRIMARY KEY (`id`)"));
        assertTrue(table.contains("UNIQUE (`vm_id`)"));
        assertTrue(table.contains("REFERENCES `vm_instance` (`id`) ON DELETE CASCADE"));
        assertTrue(table.contains("(`status`, `observed_at`)"));
        assertTrue(table.endsWith("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"));
        assertSectionSchemaContract(schema, idempotentCreate);
    }

    private void assertSectionSchemaContract(String schema, boolean idempotentCreate) {
        String tableName = "vm_guest_network_section_state";
        int start = schema.indexOf((idempotentCreate
                ? "CREATE TABLE IF NOT EXISTS" : "CREATE TABLE")
                + " `cloud`.`" + tableName + "`");
        assertTrue("Missing " + tableName + " CREATE TABLE statement", start >= 0);
        int end = schema.indexOf("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;", start);
        assertTrue("Missing " + tableName + " table terminator", end >= 0);
        String table = schema.substring(start, end);
        assertTrue(table.contains("UNIQUE KEY `uc_vm_guest_network_section__vm_section`"));
        assertTrue(table.contains("`next_due_at` datetime NOT NULL"));
        assertTrue(table.contains("`lease_owner` varchar(128)"));
        assertTrue(table.contains("`lease_until` datetime"));
        assertTrue(table.contains("ON DELETE CASCADE"));
    }

    private String tableBlock(String schema) {
        int start = schema.indexOf("CREATE TABLE `cloud`.`" + TABLE_NAME + "`");
        if (start < 0) {
            start = schema.indexOf("CREATE TABLE IF NOT EXISTS `cloud`.`" + TABLE_NAME + "`");
        }
        assertTrue("Missing " + TABLE_NAME + " CREATE TABLE statement", start >= 0);
        int end = schema.indexOf("ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;", start);
        assertTrue("Missing " + TABLE_NAME + " table terminator", end >= 0);
        return schema.substring(start, end + "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;".length());
    }

    private String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
