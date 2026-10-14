// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cloudstack.backup.commvault.AblestackCommvaultClient;
import org.junit.Test;
import org.apache.cloudstack.backup.dao.BackupDetailsDao;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.host.HostVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;

public class AblestackCommvaultBackupProviderTest {
    @Test
    public void clientsAreReusedPerZoneAndRecreatedWhenConfigurationChanges() {
        AtomicInteger creations = new AtomicInteger();
        AblestackCommvaultBackupProvider provider = new AblestackCommvaultBackupProvider() {
            @Override
            AblestackCommvaultClient createClient(ClientSettings settings) {
                creations.incrementAndGet();
                return mock(AblestackCommvaultClient.class);
            }
        };
        AblestackCommvaultBackupProvider.ClientSettings original =
                new AblestackCommvaultBackupProvider.ClientSettings("https://commvault/api", "admin", "password", true, 30);
        AblestackCommvaultBackupProvider.ClientSettings changed =
                new AblestackCommvaultBackupProvider.ClientSettings("https://commvault/api", "admin", "new-password", true, 30);

        AblestackCommvaultClient zoneOneClient = provider.getOrCreateClient(1L, original);
        assertSame(zoneOneClient, provider.getOrCreateClient(1L, original));
        assertNotSame(zoneOneClient, provider.getOrCreateClient(2L, original));
        assertNotSame(zoneOneClient, provider.getOrCreateClient(1L, changed));
        assertEquals(3, creations.get());
    }

    @Test
    public void primaryCopyCacheIsSharedAcrossJobsScopedByZoneAndInvalidatedWhenCopyDisappears() {
        AblestackCommvaultBackupProvider provider = new AblestackCommvaultBackupProvider();
        AblestackCommvaultClient client = mock(AblestackCommvaultClient.class);
        when(client.isPrimaryStoragePolicyCopy("4", "3")).thenReturn(true);
        when(client.isPrimaryStoragePolicyCopy("4", "7")).thenReturn(true);
        List<AblestackCommvaultClient.JobRetentionInfo> firstJob = List.of(
                new AblestackCommvaultClient.JobRetentionInfo("4", "3", "1791590564"));
        List<AblestackCommvaultClient.JobRetentionInfo> changedJob = List.of(
                new AblestackCommvaultClient.JobRetentionInfo("4", "7", "1791590564"));

        assertEquals("3", provider.resolvePrimaryStoragePolicyCopyId(1L, "4", firstJob, client));
        assertEquals("3", provider.resolvePrimaryStoragePolicyCopyId(1L, "4", firstJob, client));
        verify(client, times(1)).isPrimaryStoragePolicyCopy("4", "3");

        assertEquals("3", provider.resolvePrimaryStoragePolicyCopyId(2L, "4", firstJob, client));
        verify(client, times(2)).isPrimaryStoragePolicyCopy("4", "3");

        assertEquals("7", provider.resolvePrimaryStoragePolicyCopyId(1L, "4", changedJob, client));
        verify(client, times(1)).isPrimaryStoragePolicyCopy("4", "7");
    }

    @Test
    public void checkpointIsSavedBeforeDeletionAndRetriesUseTheSavedCopy() throws Exception {
        AblestackCommvaultBackupProvider provider = new AblestackCommvaultBackupProvider();
        AgentManager agent = mock(AgentManager.class);
        BackupDetailsDao detailsDao = mock(BackupDetailsDao.class);
        ReflectionTestUtils.setField(provider, "agentManager", agent);
        ReflectionTestUtils.setField(provider, "backupDetailsDao", detailsDao);
        Backup backup = mock(Backup.class);
        HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(14L);
        when(backup.getId()).thenReturn(1L);
        Map<String, String> details = new HashMap<>();
        details.put("commvault.backup.engine", "QCOW2");
        details.put("commvault.checkpoint.path", "/staging/checkpoints/current.xml");
        when(backup.getDetails()).thenReturn(details);
        when(backup.getDetail(any())).thenAnswer(invocation -> details.get(invocation.getArgument(0)));
        String xml = "<domaincheckpoint><name>current</name></domaincheckpoint>";
        when(agent.send(eq(14L), any(AblestackCommvaultReadFileCommand.class)))
                .thenAnswer(invocation -> new Answer(invocation.getArgument(1), true, xml));
        ThirdPartyBackupVolumeService.Transfer transfer = ReflectionTestUtils.invokeMethod(provider, "volumeTransfer", mock(VirtualMachine.class), backup, host);

        doThrow(new CloudRuntimeException("DB unavailable")).doNothing().when(detailsDao)
                .addDetail(1L, "commvault.checkpoint.xml", xml, false);
        assertThrows(CloudRuntimeException.class, () -> transfer.completed(backup));
        assertEquals(null, details.get("commvault.checkpoint.xml"));
        transfer.completed(backup);
        assertEquals(xml, details.get("commvault.checkpoint.xml"));
        // Simulate a lost finalization response: staging is now gone, metadata is durable.
        when(agent.send(eq(14L), any(AblestackCommvaultReadFileCommand.class)))
                .thenThrow(new CloudRuntimeException("Staging was already removed"));
        transfer.completed(backup);
        verify(agent, times(2)).send(eq(14L), any(AblestackCommvaultReadFileCommand.class));
        verify(detailsDao, times(2)).addDetail(1L, "commvault.checkpoint.xml", xml, false);
    }
}
