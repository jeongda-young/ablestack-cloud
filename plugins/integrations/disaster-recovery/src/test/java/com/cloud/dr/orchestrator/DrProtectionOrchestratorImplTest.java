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
package com.cloud.dr.orchestrator;

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
import com.cloud.dr.DrEventVO;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrReplicaDiskVO;
import com.cloud.dr.DrReplicaVO;
import com.cloud.dr.DrWorkerPlacementService;
import com.cloud.dr.DrWorkerRole;
import com.cloud.dr.DrRunVO;
import com.cloud.dr.dao.DrEventDao;
import com.cloud.dr.dao.DrPlanDao;
import com.cloud.dr.dao.DrReplicaDao;
import com.cloud.dr.dao.DrReplicaDiskDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;

@RunWith(MockitoJUnitRunner.class)
public class DrProtectionOrchestratorImplTest {
    @Mock
    private DrPlanDao drPlanDao;
    @Mock
    private DrReplicaDao drReplicaDao;
    @Mock
    private DrReplicaDiskDao drReplicaDiskDao;
    @Mock
    private DrEventDao drEventDao;
    @Mock
    private HostDao hostDao;
    @Mock
    private DrWorkerPlacementService drWorkerPlacementService;

    @InjectMocks
    private DrProtectionOrchestratorImpl orchestrator;

    @Test
    public void prepareSyncRunMaterializesReplicaAndDiskMappings() {
        DrPlanVO plan = ftctlDrPlan(DrConstants.DIRECTION_KVM_TO_KVM);
        plan.setState(DrConstants.PLAN_STATE_READY);
        plan.setCoordinatorWorkerHostId(11L);
        plan.setMappingJson("{\"targetVmName\":\"replica-01\",\"disks\":[{\"device\":\"sda\",\"sourceVolumeId\":101,\"targetVolumeId\":201,\"format\":\"qcow2\",\"sizeBytes\":1024}]}");
        DrRunVO run = new DrRunVO(plan.getId(), DrConstants.RUN_TYPE_SYNC);
        Mockito.when(drPlanDao.findById(plan.getId())).thenReturn(plan);
        Mockito.when(drWorkerPlacementService.resolveWorkerHostId(plan, DrWorkerRole.COORDINATOR))
                .thenReturn(11L);
        Mockito.when(hostDao.findById(11L)).thenReturn(Mockito.mock(HostVO.class));
        Mockito.when(drReplicaDao.listActiveByPlanId(plan.getId())).thenReturn(Collections.<DrReplicaVO>emptyList());
        Mockito.when(drReplicaDao.persist(Mockito.any(DrReplicaVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(drReplicaDiskDao.listActiveByReplicaId(Mockito.anyLong())).thenReturn(Collections.<DrReplicaDiskVO>emptyList());
        Mockito.when(drReplicaDiskDao.persist(Mockito.any(DrReplicaDiskVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(drEventDao.persist(Mockito.any(DrEventVO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DrPlanVO prepared = orchestrator.prepareSyncRun(plan, run);

        Assert.assertSame(plan, prepared);
        Assert.assertEquals(DrConstants.PLAN_STATE_READY, plan.getState());
        Assert.assertNull(plan.getSourceWorkerHostId());
        Assert.assertNull(plan.getTargetWorkerHostId());
        Mockito.verify(drPlanDao).update(plan.getId(), plan);

        ArgumentCaptor<DrReplicaVO> replicaCaptor = ArgumentCaptor.forClass(DrReplicaVO.class);
        Mockito.verify(drReplicaDao).update(Mockito.anyLong(), replicaCaptor.capture());
        Assert.assertEquals(DrConstants.REPLICA_STATE_SKELETON_READY, replicaCaptor.getValue().getState());
        Assert.assertEquals(DrConstants.HYPERVISOR_TYPE_KVM, replicaCaptor.getValue().getHypervisorType());
        Assert.assertEquals("replica-01", replicaCaptor.getValue().getTargetVmName());

        ArgumentCaptor<DrReplicaDiskVO> diskCaptor = ArgumentCaptor.forClass(DrReplicaDiskVO.class);
        Mockito.verify(drReplicaDiskDao).update(Mockito.anyLong(), diskCaptor.capture());
        Assert.assertEquals(Long.valueOf(101L), diskCaptor.getValue().getSourceVolumeId());
        Assert.assertEquals(Long.valueOf(201L), diskCaptor.getValue().getTargetVolumeId());
        Assert.assertEquals(DrConstants.REPLICA_STATE_SKELETON_READY, diskCaptor.getValue().getState());

        ArgumentCaptor<DrEventVO> eventCaptor = ArgumentCaptor.forClass(DrEventVO.class);
        Mockito.verify(drEventDao).persist(eventCaptor.capture());
        Assert.assertEquals(DrConstants.EVENT_PROTECTION_PREPARED, eventCaptor.getValue().getEventType());
        Assert.assertEquals(DrConstants.EVENT_SEVERITY_INFO, eventCaptor.getValue().getSeverity());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void prepareSyncRunRejectsMissingDiskMappings() {
        DrPlanVO plan = ftctlDrPlan(DrConstants.DIRECTION_KVM_TO_VMWARE);
        plan.setCoordinatorWorkerHostId(11L);
        plan.setMappingJson("{}");
        DrReplicaVO replica = new DrReplicaVO(plan.getId(), plan.getTargetSiteId());
        Mockito.when(drPlanDao.findById(plan.getId())).thenReturn(plan);
        Mockito.when(drWorkerPlacementService.resolveWorkerHostId(plan, DrWorkerRole.COORDINATOR))
                .thenReturn(11L);
        Mockito.when(hostDao.findById(11L)).thenReturn(Mockito.mock(HostVO.class));
        Mockito.when(drReplicaDao.listActiveByPlanId(plan.getId())).thenReturn(Collections.singletonList(replica));
        Mockito.when(drEventDao.persist(Mockito.any(DrEventVO.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orchestrator.prepareSyncRun(plan, new DrRunVO(plan.getId(), DrConstants.RUN_TYPE_SYNC));
    }

    private DrPlanVO ftctlDrPlan(String direction) {
        DrPlanVO plan = new DrPlanVO("plan-" + direction, 1L, 2L, direction);
        plan.setEngineType(DrConstants.ENGINE_TYPE_FTCTL_DR);
        plan.setEngineBindingType(DrConstants.ENGINE_BINDING_TYPE_FTCTL_DR);
        plan.setAdminState(DrConstants.ADMIN_STATE_ENABLED);
        return plan;
    }
}
