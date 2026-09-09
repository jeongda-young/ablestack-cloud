// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package com.cloud.dr;

import java.util.List;

import com.cloud.host.HostVO;

public interface DrWorkerPlacementService {
    Long resolveWorkerHostId(DrPlanVO plan, DrWorkerRole role);
    Long resolveWorkerHostId(DrPlanVO plan, DrRunVO run, DrWorkerRole role);
    List<HostVO> listEligibleWorkers(DrPlanVO plan, DrWorkerRole role);
}
