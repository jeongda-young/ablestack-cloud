// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.dr;

import com.google.gson.JsonArray;

public interface DrPlanOwnedTransportService {
    boolean supports(DrPlanVO plan);

    JsonArray startForwardTargetExport(DrPlanVO plan, DrRunVO run, String profileJson);

    JsonArray startReverseTargetExport(DrPlanVO plan, DrRunVO run, String profileJson);

    void stopForwardTargetExport(DrPlanVO plan, DrRunVO run, String profileJson,
            Long checkpointSequence);

    void stopReverseTargetExport(DrPlanVO plan, DrRunVO run);
}
