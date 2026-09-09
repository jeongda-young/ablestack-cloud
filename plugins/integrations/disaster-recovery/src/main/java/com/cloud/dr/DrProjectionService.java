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
package com.cloud.dr;

import java.util.List;

import com.cloud.utils.Pair;

public interface DrProjectionService {
    DrPlanVO refreshPlanProjection(long planId, boolean bestEffort);

    boolean projectTerminalActionResult(long planId, DrRunVO run, String detailsJson);

    List<DrReplicaVO> listReplicas(long planId);

    List<DrRestorePointVO> listRestorePoints(long planId);

    List<DrRestorePointVO> listRestorePoints(long planId, long startIndex, long pageSize);

    long countRestorePoints(long planId);

    List<DrEventVO> listPlanEvents(long planId);

    List<DrEventVO> listRunEvents(long runId);

    Pair<List<DrEventVO>, Integer> listPlanEvents(long planId, long startIndex, long pageSize);

    Pair<List<DrEventVO>, Integer> listRunEvents(long runId, long startIndex, long pageSize);
}
