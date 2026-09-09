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

import org.junit.Assert;
import org.junit.Test;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrPlanVO;
import com.cloud.dr.DrRunVO;

public class DrFailoverExecutionPolicyTest {

    @Test
    public void kvmDisasterFailoverIsTargetOnly() {
        DrPlanVO plan = plan();
        DrRunVO run = run("{\"mode\":\"disaster\",\"finalSync\":false}");

        Assert.assertTrue(DrFailoverExecutionPolicy.isDisaster(run));
        Assert.assertFalse(DrFailoverExecutionPolicy.usesRemoteSource(plan, run,
                DrConstants.RUN_TYPE_FAILOVER, true));
        Assert.assertEquals(DrFailoverExecutionPolicy.SCOPE_TARGET_DISASTER,
                DrFailoverExecutionPolicy.schedulerTransitionScope(plan, run, true));
    }

    @Test
    public void kvmPlannedFailoverRetainsRemoteSourcePath() {
        DrPlanVO plan = plan();
        DrRunVO run = run("{\"mode\":\"planned\",\"finalSync\":true}");

        Assert.assertFalse(DrFailoverExecutionPolicy.isDisaster(run));
        Assert.assertTrue(DrFailoverExecutionPolicy.usesRemoteSource(plan, run,
                DrConstants.RUN_TYPE_FAILOVER, true));
        Assert.assertEquals(DrFailoverExecutionPolicy.SCOPE_REMOTE_SOURCE,
                DrFailoverExecutionPolicy.schedulerTransitionScope(plan, run, true));
    }

    private DrPlanVO plan() {
        DrPlanVO plan = new DrPlanVO("policy-plan", 1L, 2L, DrConstants.DIRECTION_KVM_TO_KVM);
        plan.setActiveSide(DrConstants.AUTHORITY_SIDE_SOURCE);
        return plan;
    }

    private DrRunVO run(String requestJson) {
        DrRunVO run = new DrRunVO(1L, DrConstants.RUN_TYPE_FAILOVER);
        run.setRequestJson(requestJson);
        return run;
    }
}
