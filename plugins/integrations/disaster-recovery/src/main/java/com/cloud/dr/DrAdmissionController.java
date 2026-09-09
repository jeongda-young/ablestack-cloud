// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package com.cloud.dr;

public interface DrAdmissionController {
    boolean acquire(DrPlanVO plan, DrRunVO run);
    void renew(long runId);
    void release(long runId);
    String operationClass(DrRunVO run);
}
