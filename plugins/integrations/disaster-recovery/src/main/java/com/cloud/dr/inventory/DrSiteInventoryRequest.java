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

import com.cloud.dr.DrSiteCredentialInput;

public class DrSiteInventoryRequest {
    private Long siteId;
    private String siteType;
    private Long zoneId;
    private String zoneExternalId;
    private boolean includeZones = true;
    private boolean includeVmwareDatacenters;
    private DrSiteCredentialInput credentialInput;

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getSiteType() {
        return siteType;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneExternalId() {
        return zoneExternalId;
    }

    public void setZoneExternalId(String zoneExternalId) {
        this.zoneExternalId = zoneExternalId;
    }

    public boolean isIncludeZones() {
        return includeZones;
    }

    public void setIncludeZones(boolean includeZones) {
        this.includeZones = includeZones;
    }

    public boolean isIncludeVmwareDatacenters() {
        return includeVmwareDatacenters;
    }

    public void setIncludeVmwareDatacenters(boolean includeVmwareDatacenters) {
        this.includeVmwareDatacenters = includeVmwareDatacenters;
    }

    public DrSiteCredentialInput getCredentialInput() {
        return credentialInput;
    }

    public void setCredentialInput(DrSiteCredentialInput credentialInput) {
        this.credentialInput = credentialInput;
    }
}
