// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0.
package com.cloud.dr.orchestrator;

import org.junit.Assert;
import org.junit.Test;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrRunVO;
import com.cloud.exception.InvalidParameterValueException;

public class DrOrchestratorImplTest {
    private final DrOrchestratorImpl orchestrator = new DrOrchestratorImpl();

    @Test(expected = InvalidParameterValueException.class)
    public void rejectsInlineFailbackCredentialsBeforePersistence() {
        orchestrator.validateRequestContainsNoSecrets(DrConstants.RUN_TYPE_FAILBACK,
                "{\"force\":true,\"remoteMoldSecretKey\":\"secret\"}");
    }

    @Test
    public void acceptsOperatorOnlyFailbackIntent() {
        orchestrator.validateRequestContainsNoSecrets(DrConstants.RUN_TYPE_FAILBACK,
                "{\"force\":true,\"reason\":\"planned return\"}");
    }

    @Test
    public void leavesLegacyFenceRequestValidationUnchanged() {
        orchestrator.validateRequestContainsNoSecrets(DrConstants.RUN_TYPE_FENCE_CONFIRM,
                "{\"remoteMoldSecretKey\":\"legacy-fence-secret\"}");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void rejectsIdempotencyKeyReusedForAnotherAction() {
        DrRunVO existing = new DrRunVO(1L, DrConstants.RUN_TYPE_TEST_FAILOVER);
        existing.setRequestJson("{\"actionIntent\":\"TEST_FAILOVER\"}");
        orchestrator.validateIdempotentRun(existing, DrConstants.RUN_TYPE_FAILOVER,
                "{\"actionIntent\":\"FAILOVER\"}");
    }

    @Test
    public void acceptsIdempotentRetryForSameAction() {
        DrRunVO existing = new DrRunVO(1L, DrConstants.RUN_TYPE_TEST_FAILOVER);
        existing.setRequestJson("{\"actionIntent\":\"TEST_FAILOVER\"}");
        orchestrator.validateIdempotentRun(existing, DrConstants.RUN_TYPE_TEST_FAILOVER,
                "{\"actionIntent\":\"TEST_FAILOVER\"}");
    }

    @Test
    public void cancellationRequestedRunIsRedispatchable() {
        Assert.assertTrue(orchestrator.isExecutorDispatchableState(DrConstants.RUN_STATE_CANCEL_REQUESTED));
        Assert.assertTrue(orchestrator.isExecutorDispatchableState(DrConstants.RUN_STATE_QUEUED));
        Assert.assertFalse(orchestrator.isExecutorDispatchableState(DrConstants.RUN_STATE_RUNNING));
    }
}
