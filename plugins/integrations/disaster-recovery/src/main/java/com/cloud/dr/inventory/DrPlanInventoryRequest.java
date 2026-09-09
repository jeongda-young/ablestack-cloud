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
package com.cloud.dr.inventory;

public class DrPlanInventoryRequest {
    private Long sourceSiteId;
    private Long targetSiteId;
    private Long sourceVmId;
    private String sourceExternalRef;
    private String keyword;
    private Boolean includePlacement;
    private Boolean includeDisks;
    private Boolean includeNetworks;

    public Long getSourceSiteId() {
        return sourceSiteId;
    }

    public void setSourceSiteId(Long sourceSiteId) {
        this.sourceSiteId = sourceSiteId;
    }

    public Long getTargetSiteId() {
        return targetSiteId;
    }

    public void setTargetSiteId(Long targetSiteId) {
        this.targetSiteId = targetSiteId;
    }

    public Long getSourceVmId() {
        return sourceVmId;
    }

    public void setSourceVmId(Long sourceVmId) {
        this.sourceVmId = sourceVmId;
    }

    public String getSourceExternalRef() {
        return sourceExternalRef;
    }

    public void setSourceExternalRef(String sourceExternalRef) {
        this.sourceExternalRef = sourceExternalRef;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Boolean getIncludePlacement() {
        return includePlacement;
    }

    public void setIncludePlacement(Boolean includePlacement) {
        this.includePlacement = includePlacement;
    }

    public Boolean getIncludeDisks() {
        return includeDisks;
    }

    public void setIncludeDisks(Boolean includeDisks) {
        this.includeDisks = includeDisks;
    }

    public Boolean getIncludeNetworks() {
        return includeNetworks;
    }

    public void setIncludeNetworks(Boolean includeNetworks) {
        this.includeNetworks = includeNetworks;
    }

    public boolean includePlacement() {
        return includePlacement == null || Boolean.TRUE.equals(includePlacement);
    }

    public boolean includeDisks() {
        return Boolean.TRUE.equals(includeDisks);
    }

    public boolean includeNetworks() {
        return Boolean.TRUE.equals(includeNetworks);
    }
}
