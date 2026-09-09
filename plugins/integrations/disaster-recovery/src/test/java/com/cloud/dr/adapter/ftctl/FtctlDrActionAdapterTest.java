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

import java.util.Arrays;
import java.util.Collection;

import org.apache.cloudstack.api.response.ftctl.FtctlActionResponse;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.cloud.agent.api.FtctlActionCommand;
import com.cloud.dr.DrConstants;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrRunVO;
import com.cloud.dr.adapter.DrAdapterResult;
import com.cloud.dr.adapter.DrExecutionContext;
import com.cloud.dr.dao.DrReplicaDao;
import com.cloud.ftctl.FtctlProtectionVO;
import com.cloud.ftctl.FtctlService;
import com.cloud.ftctl.dao.FtctlProtectionDao;

@RunWith(Parameterized.class)
public class FtctlDrActionAdapterTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Parameterized.Parameters(name = "{0}->{1}")
    public static Collection<Object[]> storagePairs() {
        return Arrays.asList(new Object[][] {
                {"rbd", "rbd"},
                {"rbd", "qcow2"},
                {"qcow2", "rbd"},
                {"qcow2", "qcow2"}
        });
    }

    @Parameterized.Parameter
    public String sourceStorage;

    @Parameterized.Parameter(1)
    public String targetStorage;

    @Mock
    private FtctlService ftctlService;
    @Mock
    private FtctlProtectionDao ftctlProtectionDao;
    @Mock
    private DrReplicaDao drReplicaDao;

    @InjectMocks
    private FtctlDrActionAdapter adapter;

    @Test
    public void syncDelegatesAllKvmStoragePairsToExistingFtctlProtectStartPath() {
        DrExecutionContext context = context(DrConstants.RUN_TYPE_SYNC,
                String.format("{\"sourceStorage\":\"%s\",\"targetStorage\":\"%s\"}", sourceStorage, targetStorage));
        Mockito.when(ftctlProtectionDao.findById(801L)).thenReturn(new FtctlProtectionVO(101L));
        Mockito.when(ftctlService.executeFtctlAction(101L, FtctlActionCommand.Action.PROTECT_START, false)).thenReturn(actionResponse("ok", 0));

        DrAdapterResult result = adapter.execute(context);

        Assert.assertTrue(result.isSuccess());
        Mockito.verify(ftctlService).executeFtctlAction(101L, FtctlActionCommand.Action.PROTECT_START, false);
        Mockito.verify(ftctlService, Mockito.never()).failbackFtctlProtection(Mockito.anyLong(), Mockito.anyBoolean(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(ftctlService, Mockito.never()).adoptFtctlDrReplica(Mockito.anyLong(), Mockito.anyBoolean());
    }

    @Test
    public void lockedFtctlResultMapsToEngineBusyForAllKvmStoragePairs() {
        DrExecutionContext context = context(DrConstants.RUN_TYPE_FAILOVER,
                String.format("{\"sourceStorage\":\"%s\",\"targetStorage\":\"%s\"}", sourceStorage, targetStorage));
        Mockito.when(ftctlProtectionDao.findById(801L)).thenReturn(new FtctlProtectionVO(101L));
        Mockito.when(ftctlService.executeFtctlAction(101L, FtctlActionCommand.Action.FAILOVER, true)).thenReturn(actionResponse("locked", 75));

        DrAdapterResult result = adapter.execute(context);

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(DrConstants.ERROR_ENGINE_BUSY, result.getErrorCode());
    }

    @Test
    public void unsupportedTestFailoverDoesNotTouchFtctlRuntimeForAllKvmStoragePairs() {
        DrExecutionContext context = context(DrConstants.RUN_TYPE_TEST_FAILOVER,
                String.format("{\"sourceStorage\":\"%s\",\"targetStorage\":\"%s\"}", sourceStorage, targetStorage));

        DrAdapterResult result = adapter.execute(context);

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(DrConstants.ERROR_ACTION_UNSUPPORTED, result.getErrorCode());
        Mockito.verifyNoInteractions(ftctlService);
    }

    private DrExecutionContext context(String runType, String requestJson) {
        DrPlanVO plan = new DrPlanVO("kvm-ftctl-plan", 1L, 2L, "KVM_TO_KVM");
        plan.setSourceVmId(101L);
        plan.setEngineType(DrConstants.ENGINE_TYPE_FTCTL);
        plan.setEngineBindingType(DrConstants.ENGINE_BINDING_TYPE_FTCTL);
        plan.setEngineBindingId(801L);

        DrRunVO run = new DrRunVO(plan.getId(), runType);
        run.setRequestJson(requestJson);
        return new DrExecutionContext(plan, run);
    }

    private FtctlActionResponse actionResponse(String result, Integer exitCode) {
        FtctlActionResponse response = new FtctlActionResponse();
        response.setResult(result);
        response.setExitCode(exitCode);
        return response;
    }
}
