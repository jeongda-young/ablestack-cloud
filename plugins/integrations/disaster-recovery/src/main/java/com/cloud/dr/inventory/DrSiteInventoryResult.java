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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DrSiteInventoryResult {
    private String siteId;
    private String siteType;
    private String healthState;
    private String reasonCode;
    private String message;
    private Long latencyMs;
    private Date checkedAt;
    private List<DrInventoryOption> zones = new ArrayList<DrInventoryOption>();
    private List<DrInventoryOption> vmwareDatacenters = new ArrayList<DrInventoryOption>();

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getSiteType() {
        return siteType;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public String getHealthState() {
        return healthState;
    }

    public void setHealthState(String healthState) {
        this.healthState = healthState;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Date getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Date checkedAt) {
        this.checkedAt = checkedAt;
    }

    public List<DrInventoryOption> getZones() {
        return zones;
    }

    public void setZones(List<DrInventoryOption> zones) {
        this.zones = zones;
    }

    public List<DrInventoryOption> getVmwareDatacenters() {
        return vmwareDatacenters;
    }

    public void setVmwareDatacenters(List<DrInventoryOption> vmwareDatacenters) {
        this.vmwareDatacenters = vmwareDatacenters;
    }
}
