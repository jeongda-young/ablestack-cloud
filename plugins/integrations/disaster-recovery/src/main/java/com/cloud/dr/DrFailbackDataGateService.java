// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.dr;

public interface DrFailbackDataGateService {
    DrFailbackDataGateResult validate(DrPlanVO plan, DrRunVO run, DrFailbackSessionVO session);
}
