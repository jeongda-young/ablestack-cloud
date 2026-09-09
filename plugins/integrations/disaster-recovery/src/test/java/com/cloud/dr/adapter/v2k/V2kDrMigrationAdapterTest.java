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
package com.cloud.dr.adapter.v2k;

import java.util.Collections;

import org.apache.cloudstack.vm.ImportVmTask;
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
import com.cloud.dr.DrRunStepVO;
import com.cloud.dr.DrRunVO;
import com.cloud.dr.DrSiteVO;
import com.cloud.dr.adapter.DrAdapterResult;
import com.cloud.dr.adapter.DrExecutionContext;
import com.cloud.dr.dao.DrPlanDao;
import com.cloud.dr.dao.DrReplicaDao;
import com.cloud.dr.dao.DrRunStepDao;
import com.cloud.dr.dao.DrSiteDao;
import com.cloud.vm.ImportVMTaskVO;
import com.cloud.vm.dao.ImportVMTaskDao;

@RunWith(MockitoJUnitRunner.class)
public class V2kDrMigrationAdapterTest {
    @Mock
    private DrSiteDao drSiteDao;
    @Mock
    private DrPlanDao drPlanDao;
    @Mock
    private DrReplicaDao drReplicaDao;
    @Mock
    private DrRunStepDao drRunStepDao;
    @Mock
    private ImportVMTaskDao importVMTaskDao;

    @InjectMocks
    private V2kDrMigrationAdapter adapter;

    @Test
    public void syncTracksCompletedPhase1AndMarksPlanReady() {
        DrPlanVO plan = v2kPlan();
        DrRunVO run = new DrRunVO(plan.getId(), DrConstants.RUN_TYPE_SYNC);
        ImportVMTaskVO task = task(ImportVmTask.V2KStep.Phase1_Completed.name(), ImportVmTask.TaskState.Running);
        Mockito.when(drSiteDao.findById(1L)).thenReturn(site("source", DrConstants.HYPERVISOR_TYPE_VMWARE));
        Mockito.when(drSiteDao.findById(2L)).thenReturn(site("target", DrConstants.HYPERVISOR_TYPE_KVM));
        Mockito.when(importVMTaskDao.findById(900L)).thenReturn(task);
        Mockito.when(drReplicaDao.listActiveByPlanId(plan.getId())).thenReturn(Collections.<DrReplicaVO>emptyList());
        Mockito.when(drReplicaDao.persist(Mockito.any(DrReplicaVO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DrAdapterResult result = adapter.execute(new DrExecutionContext(plan, run));

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(DrConstants.PLAN_STATE_PHASE1_READY, plan.getState());
        Assert.assertNotNull(plan.getTargetReadyAt());
        Mockito.verify(drPlanDao).update(plan.getId(), plan);

        ArgumentCaptor<DrReplicaVO> replicaCaptor = ArgumentCaptor.forClass(DrReplicaVO.class);
        Mockito.verify(drReplicaDao).persist(replicaCaptor.capture());
        Assert.assertEquals(DrConstants.REPLICA_STATE_PHASE1_READY, replicaCaptor.getValue().getState());
        Assert.assertEquals("v2k-import-task://task-uuid", replicaCaptor.getValue().getTargetExternalRef());

        ArgumentCaptor<DrRunStepVO> stepCaptor = ArgumentCaptor.forClass(DrRunStepVO.class);
        Mockito.verify(drRunStepDao).persist(stepCaptor.capture());
        Assert.assertEquals("v2k-phase1", stepCaptor.getValue().getStepName());
        Assert.assertEquals(DrConstants.STEP_STATE_SUCCEEDED, stepCaptor.getValue().getState());
    }

    @Test
    public void failoverBeforePhase2CompletionRequiresExistingV2kPhase2Action() {
        DrPlanVO plan = v2kPlan();
        plan.setState(DrConstants.PLAN_STATE_PHASE1_READY);
        DrRunVO run = new DrRunVO(plan.getId(), DrConstants.RUN_TYPE_FAILOVER);
        ImportVMTaskVO task = task(ImportVmTask.V2KStep.Phase1_Completed.name(), ImportVmTask.TaskState.Running);
        Mockito.when(drSiteDao.findById(1L)).thenReturn(site("source", DrConstants.HYPERVISOR_TYPE_VMWARE));
        Mockito.when(drSiteDao.findById(2L)).thenReturn(site("target", DrConstants.HYPERVISOR_TYPE_KVM));
        Mockito.when(importVMTaskDao.findById(900L)).thenReturn(task);

        DrAdapterResult result = adapter.execute(new DrExecutionContext(plan, run));

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(DrConstants.ERROR_V2K_PHASE2_REQUIRED, result.getErrorCode());
        Mockito.verifyNoInteractions(drReplicaDao);
    }

    @Test
    public void failoverTracksCompletedPhase2AndMarksPlanFailedOver() {
        DrPlanVO plan = v2kPlan();
        plan.setState(DrConstants.PLAN_STATE_PHASE1_READY);
        DrRunVO run = new DrRunVO(plan.getId(), DrConstants.RUN_TYPE_FAILOVER);
        ImportVMTaskVO task = task(ImportVmTask.V2KStep.Phase2_Completed.name(), ImportVmTask.TaskState.Completed);
        task.setVmId(701L);
        Mockito.when(drSiteDao.findById(1L)).thenReturn(site("source", DrConstants.HYPERVISOR_TYPE_VMWARE));
        Mockito.when(drSiteDao.findById(2L)).thenReturn(site("target", DrConstants.HYPERVISOR_TYPE_KVM));
        Mockito.when(importVMTaskDao.findById(900L)).thenReturn(task);
        Mockito.when(drReplicaDao.listActiveByPlanId(plan.getId())).thenReturn(Collections.<DrReplicaVO>emptyList());
        Mockito.when(drReplicaDao.persist(Mockito.any(DrReplicaVO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DrAdapterResult result = adapter.execute(new DrExecutionContext(plan, run));

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(DrConstants.PLAN_STATE_FAILED_OVER, plan.getState());
        Mockito.verify(drPlanDao).update(plan.getId(), plan);

        ArgumentCaptor<DrReplicaVO> replicaCaptor = ArgumentCaptor.forClass(DrReplicaVO.class);
        Mockito.verify(drReplicaDao).persist(replicaCaptor.capture());
        Assert.assertEquals(DrConstants.REPLICA_STATE_FAILED_OVER, replicaCaptor.getValue().getState());
        Assert.assertEquals(Long.valueOf(701L), replicaCaptor.getValue().getTargetVmId());
    }

    @Test
    public void failbackIsExplicitlyUnsupported() {
        DrPlanVO plan = v2kPlan();
        DrRunVO run = new DrRunVO(plan.getId(), DrConstants.RUN_TYPE_FAILBACK);
        Mockito.when(drSiteDao.findById(1L)).thenReturn(site("source", DrConstants.HYPERVISOR_TYPE_VMWARE));
        Mockito.when(drSiteDao.findById(2L)).thenReturn(site("target", DrConstants.HYPERVISOR_TYPE_KVM));

        DrAdapterResult result = adapter.execute(new DrExecutionContext(plan, run));

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(DrConstants.ERROR_ACTION_UNSUPPORTED, result.getErrorCode());
        Mockito.verifyNoInteractions(importVMTaskDao);
    }

    private DrPlanVO v2kPlan() {
        DrPlanVO plan = new DrPlanVO("vmware-to-kvm", 1L, 2L, DrConstants.DIRECTION_VMWARE_TO_KVM);
        plan.setSourceExternalRef("vmware-vm-01");
        plan.setEngineType(DrConstants.ENGINE_TYPE_V2K);
        plan.setEngineBindingType(DrConstants.ENGINE_BINDING_TYPE_V2K);
        plan.setEngineBindingId(900L);
        return plan;
    }

    private ImportVMTaskVO task(String v2kStep, ImportVmTask.TaskState state) {
        ImportVMTaskVO task = new ImportVMTaskVO();
        task.setId(900L);
        task.setUuid("task-uuid");
        task.setDisplayName("vmware-vm-01");
        task.setSourceVMName("vmware-vm-01");
        task.setTargetVMName("vmware-vm-01");
        task.setMigrationTool(ImportVmTask.MigrationTool.AblestackV2K.getValue());
        task.setSourceProvider(ImportVmTask.SourceProvider.VMware.getValue());
        task.setTargetProvider(ImportVmTask.TargetProvider.KVM.getValue());
        task.setV2kStep(v2kStep);
        task.setState(state);
        return task;
    }

    private DrSiteVO site(String name, String hypervisorType) {
        return new DrSiteVO(name, "LOCAL", hypervisorType);
    }
}
