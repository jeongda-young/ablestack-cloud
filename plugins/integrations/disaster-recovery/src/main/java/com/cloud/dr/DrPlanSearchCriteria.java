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

public class DrPlanSearchCriteria {
    private final Long id;
    private final String keyword;
    private final String state;
    private final Long sourceSiteId;
    private final Long targetSiteId;
    private final String direction;
    private final String engineType;
    private final Long startIndex;
    private final Long pageSize;

    public DrPlanSearchCriteria(Long id, String keyword, String state, Long sourceSiteId, Long targetSiteId,
            String direction, String engineType, Long startIndex, Long pageSize) {
        this.id = id;
        this.keyword = keyword;
        this.state = state;
        this.sourceSiteId = sourceSiteId;
        this.targetSiteId = targetSiteId;
        this.direction = direction;
        this.engineType = engineType;
        this.startIndex = startIndex;
        this.pageSize = pageSize;
    }

    public Long getId() { return id; }
    public String getKeyword() { return keyword; }
    public String getState() { return state; }
    public Long getSourceSiteId() { return sourceSiteId; }
    public Long getTargetSiteId() { return targetSiteId; }
    public String getDirection() { return direction; }
    public String getEngineType() { return engineType; }
    public Long getStartIndex() { return startIndex; }
    public Long getPageSize() { return pageSize; }
}
