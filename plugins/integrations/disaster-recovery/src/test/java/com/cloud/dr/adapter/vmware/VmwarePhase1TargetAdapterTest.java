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
package com.cloud.dr.adapter.vmware;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrReplicaVO;
import com.cloud.dr.DrRunVO;
import com.cloud.dr.DrSiteVO;
import com.cloud.dr.adapter.DrAdapterResult;
import com.cloud.dr.adapter.DrExecutionContext;
import com.cloud.dr.dao.DrPlanDao;
import com.cloud.dr.dao.DrReplicaDao;
import com.cloud.dr.dao.DrSiteDao;

@RunWith(MockitoJUnitRunner.class)
public class VmwarePhase1TargetAdapterTest {
    @Mock
    private DrSiteDao drSiteDao;
    @Mock
    private DrReplicaDao drReplicaDao;
    @Mock
    private DrPlanDao drPlanDao;

    @InjectMocks
    private VmwarePhase1TargetAdapter adapter;

    @Test
    public void syncCreatesSkeletonReadyReplicaRecord() {
        DrPlanVO plan = vmwarePlan(validMapping());
        DrRunVO run = new DrRunVO(plan.getId(), DrConstants.RUN_TYPE_SYNC);
        Mockito.when(drSiteDao.findById(1L)).thenReturn(site("source", DrConstants.HYPERVISOR_TYPE_KVM, null, null));
        Mockito.when(drSiteDao.findById(2L)).thenReturn(site("target", DrConstants.HYPERVISOR_TYPE_VMWARE, "https://vcenter.example", "vc-ref"));
        Mockito.when(drReplicaDao.listActiveByPlanId(plan.getId())).thenReturn(Collections.<DrReplicaVO>emptyList());
        Mockito.when(drReplicaDao.persist(Mockito.any(DrReplicaVO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DrAdapterResult result = adapter.execute(new DrExecutionContext(plan, run));

        Assert.assertTrue(result.isSuccess());
        ArgumentCaptor<DrReplicaVO> replicaCaptor = ArgumentCaptor.forClass(DrReplicaVO.class);
        Mockito.verify(drReplicaDao).persist(replicaCaptor.capture());
        DrReplicaVO replica = replicaCaptor.getValue();
        Assert.assertEquals(DrConstants.REPLICA_STATE_SKELETON_READY, replica.getState());
        Assert.assertEquals(DrConstants.REPLICA_POWER_STATE_POWERED_OFF, replica.getPowerState());
        Assert.assertEquals(DrConstants.HYPERVISOR_TYPE_VMWARE, replica.getHypervisorType());
        Assert.assertEquals("r97-link-04-vmware-standby", replica.getTargetVmName());
        Assert.assertTrue(replica.getTargetExternalRef().startsWith("vmware-phase1://site/2/plan/"));
        Assert.assertTrue(replica.getRuntimeStateJson().contains("\"vcenterOperation\":\"NOT_STARTED\""));
        Assert.assertEquals(DrConstants.PLAN_STATE_ENABLED, plan.getState());
        Assert.assertNull(plan.getTargetReadyAt());
        Mockito.verify(drPlanDao).update(plan.getId(), plan);
    }

    @Test
    public void invalidDatastoreMappingFailsValidationWithoutPersistingReplica() {
        DrPlanVO plan = vmwarePlan("{\"targetVmName\":\"bad\",\"resourcePoolRef\":\"rp-1\",\"targetFolderPath\":\"/dr\",\"targetNetworkRef\":\"pg-isolated\"}");

        DrAdapterResult result = adapter.validatePlan(plan);

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(DrConstants.ERROR_TARGET_MAPPING_INVALID, result.getErrorCode());
        Assert.assertTrue(result.getMessage().contains("targetDatastoreRef"));
        Mockito.verifyNoInteractions(drReplicaDao);
    }

    @Test
    public void failoverBeforeTargetReadyIsRejected() {
        DrPlanVO plan = vmwarePlan(validMapping());
        DrRunVO run = new DrRunVO(plan.getId(), DrConstants.RUN_TYPE_FAILOVER);

        DrAdapterResult result = adapter.execute(new DrExecutionContext(plan, run));

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(DrConstants.ERROR_TARGET_NOT_READY, result.getErrorCode());
        Mockito.verifyNoInteractions(drReplicaDao);
    }

    private DrPlanVO vmwarePlan(String mappingJson) {
        DrPlanVO plan = new DrPlanVO("kvm-to-vmware", 1L, 2L, DrConstants.DIRECTION_KVM_TO_VMWARE);
        plan.setSourceVmId(404L);
        plan.setSourceExternalRef("i-2-404-VM");
        plan.setEngineType(DrConstants.ENGINE_TYPE_VMWARE_PHASE1);
        plan.setEngineBindingType(DrConstants.ENGINE_BINDING_TYPE_VMWARE_PHASE1);
        plan.setMappingJson(mappingJson);
        return plan;
    }

    private DrSiteVO site(String name, String hypervisorType, String endpoint, String credentialRef) {
        DrSiteVO site = new DrSiteVO(name, "LOCAL", hypervisorType);
        site.setEndpoint(endpoint);
        site.setCredentialRef(credentialRef);
        return site;
    }

    private String validMapping() {
        return "{"
                + "\"targetVmName\":\"r97-link-04-vmware-standby\","
                + "\"targetDatastoreRef\":\"datastore-01\","
                + "\"resourcePoolRef\":\"resgroup-42\","
                + "\"targetFolderPath\":\"/ABLESTACK-DR\","
                + "\"networkMappings\":[{\"targetNetworkRef\":\"pg-dr-isolated\",\"connectMode\":\"ISOLATED\",\"macPolicy\":\"GENERATE\"}]"
                + "}";
    }
}
