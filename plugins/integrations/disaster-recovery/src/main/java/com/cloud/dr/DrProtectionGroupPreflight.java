// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package com.cloud.dr;

import java.util.Collections;
import java.util.List;

public class DrProtectionGroupPreflight {
    private final String action;
    private final boolean ready;
    private final List<DrProtectionGroupPlanPreflight> plans;

    public DrProtectionGroupPreflight(String action, List<DrProtectionGroupPlanPreflight> plans) {
        this.action = action;
        this.plans = Collections.unmodifiableList(plans);
        this.ready = plans.stream().allMatch(DrProtectionGroupPlanPreflight::isEligible);
    }

    public String getAction() { return action; }
    public boolean isReady() { return ready; }
    public List<DrProtectionGroupPlanPreflight> getPlans() { return plans; }
}
