// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file distributed with this work.
package com.cloud.dr;

import java.util.List;

import com.cloud.utils.component.Manager;

public interface DrProtectionGroupService extends Manager {
    String configureGroup(List<Long> planIds, String groupName, Integer maxParallel, Boolean quiesceRequired);
    DrProtectionGroupPreflight previewGroupRun(List<Long> planIds, String action, Boolean quiesceRequired);
    DrGroupRunVO startGroupRun(List<Long> planIds, String action, Integer maxParallel, Boolean quiesceRequired,
            boolean fullReseed, Long requestedByUserId);
    List<DrGroupRunVO> listGroupRuns(String groupUuid);
}
