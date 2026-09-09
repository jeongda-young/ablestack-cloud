// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0.
package com.cloud.dr;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dr.dao.DrPlanRuntimeDao;

@RunWith(MockitoJUnitRunner.class)
public class DrProtectionAuthorityServiceImplTest {
    private static final long PLAN_ID = 41L;

    @Mock
    private DrPlanRuntimeDao drPlanRuntimeDao;

    @InjectMocks
    private DrProtectionAuthorityServiceImpl service;

    @Test
    public void dueSoonCheckpointRemainsEligibleForCutover() {
        DrPlanRuntimeVO runtime = readyRuntime("RPO_DUE_SOON", false);
        Mockito.when(drPlanRuntimeDao.findByPlanId(PLAN_ID)).thenReturn(runtime);

        DrProtectionAuthoritySnapshot authority = service.getAuthority(PLAN_ID);

        Assert.assertTrue(authority.isNormalCutoverReady());
        Assert.assertNull(authority.getNormalCutoverReason());
    }

    @Test
    public void overdueCheckpointBlocksCutover() {
        DrPlanRuntimeVO runtime = readyRuntime("OVERDUE", true);
        Mockito.when(drPlanRuntimeDao.findByPlanId(PLAN_ID)).thenReturn(runtime);

        DrProtectionAuthoritySnapshot authority = service.getAuthority(PLAN_ID);

        Assert.assertFalse(authority.isNormalCutoverReady());
        Assert.assertEquals("DR_RPO_OVERDUE", authority.getNormalCutoverReason());
    }

    private DrPlanRuntimeVO readyRuntime(String freshnessState, boolean rpoOverdue) {
        DrPlanRuntimeVO runtime = new DrPlanRuntimeVO(PLAN_ID);
        runtime.setProtectionState(DrConstants.PLAN_STATE_READY);
        runtime.setFreshnessState(freshnessState);
        runtime.setSchedulerPidAlive(true);
        runtime.setOwnerMatched(true);
        runtime.setSchedulerHealthState("HEALTHY");
        runtime.setRpoOverdue(rpoOverdue);
        runtime.setLatestCompletedIncrementalVerified(true);
        runtime.setConsecutiveAutomaticReseedCount(0);
        runtime.setCurrentCycleState("COMPLETED");
        runtime.setErrorCode(null);
        return runtime;
    }
}
