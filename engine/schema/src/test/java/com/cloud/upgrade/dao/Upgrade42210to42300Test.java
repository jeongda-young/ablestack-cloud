// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.upgrade.dao;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.utils.crypt.DBEncryptionUtil;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class Upgrade42210to42300Test {

    private Upgrade42210to42300 upgrade;

    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement columnStatement;
    @Mock
    private PreparedStatement legacyStatement;
    @Mock
    private PreparedStatement activeStatement;
    @Mock
    private PreparedStatement insertStatement;
    @Mock
    private PreparedStatement updateStatement;
    @Mock
    private PreparedStatement removeStatement;
    @Mock
    private ResultSet columnResult;
    @Mock
    private ResultSet legacyResult;
    @Mock
    private ResultSet activeResult;

    @Before
    public void setUp() throws Exception {
        upgrade = new Upgrade42210to42300();
        when(connection.prepareStatement(Upgrade42210to42300.LEGACY_API_KEY_COLUMNS_QUERY)).thenReturn(columnStatement);
        when(columnStatement.executeQuery()).thenReturn(columnResult);
    }

    @Test
    public void migrateLegacyUserApiKeyPairsInsertsEncryptedSecret() throws Exception {
        configureMigrationStatements();
        when(columnResult.next()).thenReturn(true);
        when(columnResult.getInt(1)).thenReturn(2);
        when(legacyResult.next()).thenReturn(true, false);
        when(legacyResult.getLong(1)).thenReturn(7L);
        when(legacyResult.getLong(2)).thenReturn(8L);
        when(legacyResult.getLong(3)).thenReturn(9L);
        when(legacyResult.getString(4)).thenReturn("api-key");
        when(legacyResult.getString(5)).thenReturn("plain-secret");
        when(activeResult.next()).thenReturn(false);

        try (MockedStatic<DBEncryptionUtil> encryption = mockStatic(DBEncryptionUtil.class)) {
            encryption.when(() -> DBEncryptionUtil.encrypt("plain-secret")).thenReturn("encrypted-secret");
            upgrade.migrateLegacyUserApiKeyPairs(connection);
        }

        verify(insertStatement).setString(eq(1), anyString());
        verify(insertStatement).setLong(2, 7L);
        verify(insertStatement).setLong(3, 8L);
        verify(insertStatement).setLong(4, 9L);
        verify(insertStatement).setString(5, "api-key");
        verify(insertStatement).setString(6, "encrypted-secret");
        verify(insertStatement).executeUpdate();
        verify(updateStatement, never()).executeUpdate();
        verify(removeStatement, never()).executeUpdate();
    }

    @Test
    public void migrateLegacyUserApiKeyPairsUpdatesOldestAndRemovesDuplicate() throws Exception {
        configureMigrationStatements();
        when(columnResult.next()).thenReturn(true);
        when(columnResult.getInt(1)).thenReturn(2);
        when(legacyResult.next()).thenReturn(true, false);
        when(legacyResult.getLong(1)).thenReturn(2L);
        when(legacyResult.getLong(2)).thenReturn(1L);
        when(legacyResult.getLong(3)).thenReturn(2L);
        when(legacyResult.getString(4)).thenReturn("admin-api-key");
        when(legacyResult.getString(5)).thenReturn("plain-secret");
        when(activeResult.next()).thenReturn(true, true, false);
        when(activeResult.getLong(1)).thenReturn(1L, 4L);

        try (MockedStatic<DBEncryptionUtil> encryption = mockStatic(DBEncryptionUtil.class)) {
            encryption.when(() -> DBEncryptionUtil.encrypt("plain-secret")).thenReturn("encrypted-secret");
            upgrade.migrateLegacyUserApiKeyPairs(connection);
        }

        verify(updateStatement).setString(1, "encrypted-secret");
        verify(updateStatement).setLong(2, 1L);
        verify(updateStatement).executeUpdate();
        verify(removeStatement).setLong(1, 4L);
        verify(removeStatement).executeUpdate();
        verify(insertStatement, never()).executeUpdate();
    }

    @Test
    public void migrateLegacyUserApiKeyPairsSkipsWhenLegacyColumnsAreAbsent() throws Exception {
        when(columnResult.next()).thenReturn(true);
        when(columnResult.getInt(1)).thenReturn(0);

        upgrade.migrateLegacyUserApiKeyPairs(connection);

        verify(connection, never()).prepareStatement(Upgrade42210to42300.LEGACY_API_KEYS_QUERY);
    }

    @Test
    public void prepareScriptCanonicalizesDrSyncCycleByPlanSequence() throws Exception {
        InputStream[] scripts = upgrade.getPrepareScripts();
        Assert.assertEquals(1, scripts.length);
        String sql;
        try (InputStream script = scripts[0]) {
            sql = new String(script.readAllBytes(), StandardCharsets.UTF_8);
        }

        Assert.assertTrue(sql.contains("uk_dr_sync_cycle__plan_sequence"));
        Assert.assertTrue(sql.contains("dr_sync_cycle_canonical"));
        Assert.assertTrue(sql.contains("alias.id <> canonical.keep_id"));
        Assert.assertTrue(sql.contains("i_dr_sync_cycle__plan_run_sequence"));
        Assert.assertTrue(sql.contains("accepted_cycle_sequence"));
        Assert.assertTrue(sql.contains("accepted_cycle_token"));
    }

    private void configureMigrationStatements() throws Exception {
        when(connection.prepareStatement(Upgrade42210to42300.LEGACY_API_KEYS_QUERY)).thenReturn(legacyStatement);
        when(connection.prepareStatement(Upgrade42210to42300.ACTIVE_KEY_PAIRS_QUERY)).thenReturn(activeStatement);
        when(connection.prepareStatement(Upgrade42210to42300.INSERT_KEY_PAIR)).thenReturn(insertStatement);
        when(connection.prepareStatement(Upgrade42210to42300.UPDATE_KEY_PAIR_SECRET)).thenReturn(updateStatement);
        when(connection.prepareStatement(Upgrade42210to42300.REMOVE_DUPLICATE_KEY_PAIR)).thenReturn(removeStatement);
        when(legacyStatement.executeQuery()).thenReturn(legacyResult);
        when(activeStatement.executeQuery()).thenReturn(activeResult);
    }
}
