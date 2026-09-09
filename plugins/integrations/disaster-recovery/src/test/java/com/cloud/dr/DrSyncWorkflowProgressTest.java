// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0.
package com.cloud.dr;

import org.junit.Assert;
import org.junit.Test;

public class DrSyncWorkflowProgressTest {

    @Test
    public void mapsTransferIntoPostAcceptanceWorkflowRange() {
        Assert.assertEquals(76, DrSyncWorkflowProgress.resolve(1, 22D, null, null, false));
        Assert.assertEquals(95, DrSyncWorkflowProgress.resolve(40, 100D, null, null, false));
    }

    @Test
    public void derivesTransferPercentFromBytesAndNeverRegresses() {
        Assert.assertEquals(83, DrSyncWorkflowProgress.resolve(70, null, 50L, 100L, false));
        Assert.assertEquals(90, DrSyncWorkflowProgress.resolve(90, 25D, null, null, false));
    }

    @Test
    public void reservesTargetMaterializationAndTerminalCompletionRanges() {
        Assert.assertEquals(97, DrSyncWorkflowProgress.resolve(95, 100D, null, null, true));
    }
}
