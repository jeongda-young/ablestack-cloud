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

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "dr_site")
public class DrSiteVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "site_type")
    private String siteType;

    @Column(name = "hypervisor_type")
    private String hypervisorType;

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "credential_ref")
    private String credentialRef;

    @Column(name = "credential_id")
    private Long credentialId;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "zone_external_id")
    private String zoneExternalId;

    @Column(name = "zone_name")
    private String zoneName;

    @Column(name = "vmware_datacenter_id")
    private Long vmwareDatacenterId;

    @Column(name = "vmware_datacenter_external_id")
    private String vmwareDatacenterExternalId;

    @Column(name = "vmware_datacenter_name")
    private String vmwareDatacenterName;

    @Column(name = "state")
    private String state;

    @Column(name = "health_state")
    private String healthState;

    @Column(name = "capabilities_json", length = 65535)
    private String capabilitiesJson;

    @Column(name = "last_checked")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastChecked;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    protected DrSiteVO() {
    }

    public DrSiteVO(String name, String siteType, String hypervisorType) {
        this.name = name;
        this.siteType = siteType;
        this.hypervisorType = hypervisorType;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSiteType() {
        return siteType;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getCredentialRef() {
        return credentialRef;
    }

    public Long getCredentialId() {
        return credentialId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getZoneExternalId() {
        return zoneExternalId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public Long getVmwareDatacenterId() {
        return vmwareDatacenterId;
    }

    public String getVmwareDatacenterExternalId() {
        return vmwareDatacenterExternalId;
    }

    public String getVmwareDatacenterName() {
        return vmwareDatacenterName;
    }

    public String getState() {
        return state;
    }

    public String getHealthState() {
        return healthState;
    }

    public String getCapabilitiesJson() {
        return capabilitiesJson;
    }

    public Date getLastChecked() {
        return lastChecked;
    }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSiteType(String siteType) {
        this.siteType = siteType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setCredentialRef(String credentialRef) {
        this.credentialRef = credentialRef;
    }

    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneExternalId(String zoneExternalId) {
        this.zoneExternalId = zoneExternalId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setVmwareDatacenterId(Long vmwareDatacenterId) {
        this.vmwareDatacenterId = vmwareDatacenterId;
    }

    public void setVmwareDatacenterExternalId(String vmwareDatacenterExternalId) {
        this.vmwareDatacenterExternalId = vmwareDatacenterExternalId;
    }

    public void setVmwareDatacenterName(String vmwareDatacenterName) {
        this.vmwareDatacenterName = vmwareDatacenterName;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setHealthState(String healthState) {
        this.healthState = healthState;
    }

    public void setCapabilitiesJson(String capabilitiesJson) {
        this.capabilitiesJson = capabilitiesJson;
    }

    public void setLastChecked(Date lastChecked) {
        this.lastChecked = lastChecked;
    }

    public void markUpdated() {
        updated = new Date();
    }

    public void markRemoved() {
        removed = new Date();
        markUpdated();
    }
}
