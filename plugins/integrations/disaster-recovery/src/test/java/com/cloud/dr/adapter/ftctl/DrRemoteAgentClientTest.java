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
package com.cloud.dr.adapter.ftctl;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.api.FtctlDrActionAnswer;
import com.cloud.agent.api.FtctlDrActionCommand;
import com.cloud.agent.api.FtctlDrCancelAnswer;
import com.cloud.agent.api.FtctlDrCancelCommand;
import com.cloud.agent.api.FtctlDrStatusAnswer;
import com.cloud.agent.api.FtctlDrStatusCommand;
import com.cloud.dr.DrConstants;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrResolvedSiteCredential;
import com.cloud.dr.DrSiteCredentialService;
import com.cloud.dr.DrSiteVO;
import com.cloud.dr.dao.DrSiteDao;
import com.cloud.dr.inventory.DrMoldInventoryClient;

public class DrRemoteAgentClientTest {

    @Test
    public void sourceWorkerUuidDoesNotPersistObservedVmPlacement() {
        DrRemoteAgentClient client = new DrRemoteAgentClient();
        DrSiteDao siteDao = Mockito.mock(DrSiteDao.class);
        DrSiteCredentialService credentialService = Mockito.mock(DrSiteCredentialService.class);
        DrMoldInventoryClient inventoryClient = Mockito.mock(DrMoldInventoryClient.class);
        DrResolvedSiteCredential credential = Mockito.mock(DrResolvedSiteCredential.class);
        DrSiteVO site = Mockito.mock(DrSiteVO.class);
        ReflectionTestUtils.setField(client, "drSiteDao", siteDao);
        ReflectionTestUtils.setField(client, "drSiteCredentialService", credentialService);
        ReflectionTestUtils.setField(client, "drMoldInventoryClient", inventoryClient);

        DrPlanVO plan = new DrPlanVO("remote-source", 1L, 2L, DrConstants.DIRECTION_KVM_TO_KVM);
        ReflectionTestUtils.setField(plan, "id", 42L);
        plan.setSourceExternalRef("source-vm-uuid");
        plan.setMappingJson("{\"source\":{\"hardware\":{}}}");
        Assert.assertNull(client.sourceWorkerUuid(plan));
        Assert.assertFalse(plan.getMappingJson().contains("sourceWorkerHostUuid"));
        Mockito.verifyNoInteractions(inventoryClient);
    }

    @Test
    public void sourceWorkerUuidDoesNotUseDurableMappingAsRoutingAuthority() {
        DrRemoteAgentClient client = new DrRemoteAgentClient();
        DrSiteDao siteDao = Mockito.mock(DrSiteDao.class);
        DrSiteCredentialService credentialService = Mockito.mock(DrSiteCredentialService.class);
        DrMoldInventoryClient inventoryClient = Mockito.mock(DrMoldInventoryClient.class);
        ReflectionTestUtils.setField(client, "drSiteDao", siteDao);
        ReflectionTestUtils.setField(client, "drSiteCredentialService", credentialService);
        ReflectionTestUtils.setField(client, "drMoldInventoryClient", inventoryClient);

        DrPlanVO plan = new DrPlanVO("remote-source", 1L, 2L, DrConstants.DIRECTION_KVM_TO_KVM);
        plan.setSourceExternalRef("source-vm-uuid");
        plan.setMappingJson("{\"sourceWorkerHostUuid\":\"durable-source-host-uuid\"}");
        Assert.assertNull(client.sourceWorkerUuid(plan));
        Mockito.verifyNoInteractions(inventoryClient);
    }

    @Test
    public void sourceTransitionRunUuidIsDeterministicAndSchemaSafe() {
        String planUuid = "7ec74483-8554-415d-ac56-f62f8b17fbd0";
        String parentRunUuid = "d64fdb24-9fb3-49ad-82de-2556db63698b";

        String first = DrRemoteAgentClient.sourceTransitionRunUuid(planUuid, parentRunUuid,
                FtctlDrActionCommand.Action.RESUME_SYNC);
        String second = DrRemoteAgentClient.sourceTransitionRunUuid(planUuid, parentRunUuid,
                FtctlDrActionCommand.Action.RESUME_SYNC);
        String pause = DrRemoteAgentClient.sourceTransitionRunUuid(planUuid, parentRunUuid,
                FtctlDrActionCommand.Action.PAUSE_SYNC);

        Assert.assertEquals(first, second);
        Assert.assertNotEquals(first, pause);
        Assert.assertEquals(36, first.length());
        Assert.assertEquals(first, UUID.fromString(first).toString());
    }

    @Test
    public void sourceResumeDispatchesRehydratedProfile() {
        DrRemoteAgentClient client = Mockito.spy(new DrRemoteAgentClient());
        DrPlanVO plan = new DrPlanVO("remote-source", 1L, 2L, DrConstants.DIRECTION_KVM_TO_KVM);
        plan.setSourceExternalRef("source-vm-uuid");
        plan.setMappingJson("{\"source\":{\"hardware\":{\"sourceHostUuid\":\"source-host-uuid\"}}}");
        String profileJson = "{\"planUuid\":\"" + plan.getUuid()
                + "\",\"transport\":{\"mode\":\"site-agent-nbd\",\"exports\":[{\"device\":\"sda\"}]}}";
        FtctlDrActionCommand[] captured = new FtctlDrActionCommand[1];
        Mockito.doAnswer(invocation -> {
            captured[0] = invocation.getArgument(2);
            return new FtctlDrActionAnswer(captured[0], true, "resumed");
        }).when(client).execute(Mockito.eq(plan), Mockito.eq("ACTION"),
                Mockito.isA(FtctlDrActionCommand.class), Mockito.isNull(),
                Mockito.eq(FtctlDrActionAnswer.class));

        FtctlDrActionAnswer answer = client.transitionSourceScheduler(plan,
                FtctlDrActionCommand.Action.RESUME_SYNC, "parent-run", profileJson, 9L, 10L, 676L);

        Assert.assertTrue(answer.getResult());
        Assert.assertNotNull(captured[0]);
        Assert.assertEquals(profileJson, captured[0].getProfileJson());
        Assert.assertNull(captured[0].getSourceWorkerUuid());
        Assert.assertEquals(DrConstants.RUN_TYPE_RESUME_SYNC, captured[0].getRunType());
        Assert.assertEquals(Long.valueOf(9), captured[0].getResumeBaselineCheckpointSequence());
        Assert.assertEquals(Long.valueOf(10), captured[0].getMinimumCompletedCheckpointSequence());
        Assert.assertEquals(Long.valueOf(676), captured[0].getAuthoritySequenceFloor());
        Assert.assertEquals("source-vm-uuid", captured[0].getContext().get("sourceVmUuid"));
    }

    @Test
    public void sourceCancelUsesTypedRemoteAgentContract() {
        DrRemoteAgentClient client = Mockito.spy(new DrRemoteAgentClient());
        DrPlanVO plan = new DrPlanVO("remote-source", 1L, 2L, DrConstants.DIRECTION_KVM_TO_KVM);
        plan.setSourceExternalRef("source-vm-uuid");
        plan.setMappingJson("{\"source\":{\"hardware\":{\"sourceHostUuid\":\"source-host-uuid\"}}}");
        FtctlDrCancelCommand[] captured = new FtctlDrCancelCommand[1];
        Mockito.doAnswer(invocation -> {
            captured[0] = invocation.getArgument(2);
            return new FtctlDrCancelAnswer(captured[0], true, "canceled", plan.getUuid(), "run-uuid",
                    "canceled", true, null, 0, "{\"state\":\"CANCELED\"}");
        }).when(client).execute(Mockito.eq(plan), Mockito.eq("CANCEL"),
                Mockito.isA(FtctlDrCancelCommand.class), Mockito.isNull(),
                Mockito.eq(FtctlDrCancelAnswer.class));

        FtctlDrCancelAnswer answer = client.cancelSourceRun(plan, "run-uuid");

        Assert.assertTrue(answer.getResult());
        Assert.assertEquals(plan.getUuid(), captured[0].getPlanUuid());
        Assert.assertEquals("run-uuid", captured[0].getRunUuid());
        Assert.assertEquals("source-vm-uuid", captured[0].getSourceVmUuid());
    }

    @Test
    public void sourceStatusCarriesVmIdentityWithoutWorkerBinding() {
        DrRemoteAgentClient client = Mockito.spy(new DrRemoteAgentClient());
        DrPlanVO plan = new DrPlanVO("remote-source", 1L, 2L, DrConstants.DIRECTION_KVM_TO_KVM);
        plan.setSourceExternalRef("source-vm-uuid");
        FtctlDrStatusCommand[] captured = new FtctlDrStatusCommand[1];
        Mockito.doAnswer(invocation -> {
            captured[0] = invocation.getArgument(2);
            return new FtctlDrStatusAnswer(captured[0], true, "ready");
        }).when(client).execute(Mockito.eq(plan), Mockito.eq("STATUS"),
                Mockito.isA(FtctlDrStatusCommand.class), Mockito.isNull(),
                Mockito.eq(FtctlDrStatusAnswer.class));

        client.fetchSourceStatus(plan, null, FtctlDrStatusCommand.StatusScope.PLAN_AUTHORITY);

        Assert.assertEquals("source-vm-uuid", captured[0].getSourceVmUuid());
    }

    @Test
    public void sourceActionCarriesVmIdentityForCommandTimePlacement() {
        DrRemoteAgentClient client = new DrRemoteAgentClient();
        DrPlanVO plan = new DrPlanVO("remote-source", 1L, 2L, DrConstants.DIRECTION_KVM_TO_KVM);
        plan.setSourceExternalRef("source-vm-uuid");
        FtctlDrActionCommand command = new FtctlDrActionCommand(
                FtctlDrActionCommand.Action.CUTOVER_COMMIT, plan.getUuid(), "run-uuid");

        ReflectionTestUtils.invokeMethod(client, "attachSourceVmIdentity", plan, command);

        Assert.assertEquals("source-vm-uuid", command.getContext().get("sourceVmUuid"));
        Assert.assertNull(command.getSourceWorkerUuid());
    }

    @Test
    public void sourcePowerObservationReadsMoldWithoutChangingPowerState() {
        DrRemoteAgentClient client = Mockito.spy(new DrRemoteAgentClient());
        DrSiteDao siteDao = Mockito.mock(DrSiteDao.class);
        DrSiteCredentialService credentialService = Mockito.mock(DrSiteCredentialService.class);
        DrMoldInventoryClient inventoryClient = Mockito.mock(DrMoldInventoryClient.class);
        DrResolvedSiteCredential credential = Mockito.mock(DrResolvedSiteCredential.class);
        DrSiteVO site = Mockito.mock(DrSiteVO.class);
        ReflectionTestUtils.setField(client, "drSiteDao", siteDao);
        ReflectionTestUtils.setField(client, "drSiteCredentialService", credentialService);
        ReflectionTestUtils.setField(client, "drMoldInventoryClient", inventoryClient);
        DrPlanVO plan = new DrPlanVO("remote-source", 1L, 2L, DrConstants.DIRECTION_KVM_TO_KVM);
        plan.setSourceExternalRef("source-vm-uuid");
        Mockito.doReturn(true).when(client).isRemoteKvmSource(plan);
        Mockito.when(siteDao.findById(1L)).thenReturn(site);
        Mockito.when(credentialService.resolveCredential(site)).thenReturn(credential);
        Mockito.when(credential.hasSecrets()).thenReturn(true);
        Mockito.when(inventoryClient.getVirtualMachinePowerState(credential, "source-vm-uuid"))
                .thenReturn("POWERED_OFF");

        Assert.assertEquals("POWERED_OFF", client.getSourceVmPowerState(plan));
        Mockito.verify(inventoryClient).getVirtualMachinePowerState(credential, "source-vm-uuid");
        Mockito.verify(inventoryClient, Mockito.never()).ensureVirtualMachinePowerState(
                Mockito.any(), Mockito.anyString(), Mockito.anyBoolean());
        Mockito.verify(credential).close();
    }
}
