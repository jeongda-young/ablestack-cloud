// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.dr;

import com.google.gson.JsonObject;

public interface DrFailbackLifecycleService {
    DrFailbackSessionVO reconcile(DrPlanVO plan, DrRunVO run, JsonObject runtime);

    boolean requiresCancellationCompensation(DrRunVO run);

    boolean cancelAndRestoreTargetAuthority(DrPlanVO plan, DrRunVO run);
}
