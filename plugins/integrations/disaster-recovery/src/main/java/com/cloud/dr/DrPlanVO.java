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
@Table(name = "dr_plan")
public class DrPlanVO implements InternalIdentity {

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

    @Column(name = "source_site_id")
    private long sourceSiteId;

    @Column(name = "target_site_id")
    private long targetSiteId;

    @Column(name = "source_vm_id")
    private Long sourceVmId;

    @Column(name = "source_external_ref")
    private String sourceExternalRef;

    @Column(name = "direction")
    private String direction;

    @Column(name = "engine_type")
    private String engineType;

    @Column(name = "engine_binding_type")
    private String engineBindingType;

    @Column(name = "engine_binding_id")
    private Long engineBindingId;

    @Column(name = "state")
    private String state;

    @Column(name = "admin_state")
    private String adminState;

    @Column(name = "active_side")
    private String activeSide;

    @Column(name = "rpo_seconds")
    private Integer rpoSeconds;

    @Column(name = "rto_seconds")
    private Integer rtoSeconds;

    @Column(name = "schedule_json", length = 65535)
    private String scheduleJson;

    @Column(name = "policy_json", length = 65535)
    private String policyJson;

    @Column(name = "mapping_json", length = 65535)
    private String mappingJson;

    @Column(name = "quiesce_policy_json", length = 65535)
    private String quiescePolicyJson;

    @Column(name = "source_worker_host_id")
    private Long sourceWorkerHostId;

    @Column(name = "target_worker_host_id")
    private Long targetWorkerHostId;

    @Column(name = "coordinator_worker_host_id")
    private Long coordinatorWorkerHostId;

    @Column(name = "protection_group_uuid")
    private String protectionGroupUuid;

    @Column(name = "protection_group_name")
    private String protectionGroupName;

    @Column(name = "protection_group_order")
    private Integer protectionGroupOrder;

    @Column(name = "protection_group_max_parallel")
    private Integer protectionGroupMaxParallel;

    @Column(name = "protection_group_quiesce_required")
    private Boolean protectionGroupQuiesceRequired;

    @Column(name = "last_source_checkpoint_at")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastSourceCheckpointAt;

    @Column(name = "last_target_durable_at")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastTargetDurableAt;

    @Column(name = "target_ready_at")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date targetReadyAt;

    @Column(name = "target_ready_rpo_seconds")
    private Integer targetReadyRpoSeconds;

    @Column(name = "last_run_id")
    private Long lastRunId;

    @Column(name = "last_error_code")
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 4096)
    private String lastErrorMessage;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    protected DrPlanVO() {
    }

    public DrPlanVO(String name, long sourceSiteId, long targetSiteId, String direction) {
        this.name = name;
        this.sourceSiteId = sourceSiteId;
        this.targetSiteId = targetSiteId;
        this.direction = direction;
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

    public long getSourceSiteId() {
        return sourceSiteId;
    }

    public long getTargetSiteId() {
        return targetSiteId;
    }

    public Long getSourceVmId() {
        return sourceVmId;
    }

    public String getSourceExternalRef() {
        return sourceExternalRef;
    }

    public String getDirection() {
        return direction;
    }

    public String getEngineType() {
        return engineType;
    }

    public String getEngineBindingType() {
        return engineBindingType;
    }

    public Long getEngineBindingId() {
        return engineBindingId;
    }

    public String getState() {
        return state;
    }

    public String getAdminState() {
        return adminState;
    }

    public String getActiveSide() {
        return activeSide;
    }

    public Integer getRpoSeconds() {
        return rpoSeconds;
    }

    public Integer getRtoSeconds() {
        return rtoSeconds;
    }

    public String getScheduleJson() {
        return scheduleJson;
    }

    public String getPolicyJson() {
        return policyJson;
    }

    public String getMappingJson() {
        return mappingJson;
    }

    public String getQuiescePolicyJson() {
        return quiescePolicyJson;
    }

    public Long getSourceWorkerHostId() {
        return sourceWorkerHostId;
    }

    public Long getTargetWorkerHostId() {
        return targetWorkerHostId;
    }

    public Long getCoordinatorWorkerHostId() {
        return coordinatorWorkerHostId;
    }

    public String getProtectionGroupUuid() { return protectionGroupUuid; }
    public String getProtectionGroupName() { return protectionGroupName; }
    public Integer getProtectionGroupOrder() { return protectionGroupOrder; }
    public Integer getProtectionGroupMaxParallel() { return protectionGroupMaxParallel; }
    public Boolean getProtectionGroupQuiesceRequired() { return protectionGroupQuiesceRequired; }

    public Date getLastSourceCheckpointAt() {
        return lastSourceCheckpointAt;
    }

    public Date getLastTargetDurableAt() {
        return lastTargetDurableAt;
    }

    public Date getTargetReadyAt() {
        return targetReadyAt;
    }

    public Integer getTargetReadyRpoSeconds() {
        return targetReadyRpoSeconds;
    }

    public Long getLastRunId() {
        return lastRunId;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
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

    public void setSourceVmId(Long sourceVmId) {
        this.sourceVmId = sourceVmId;
    }

    public void setSourceExternalRef(String sourceExternalRef) {
        this.sourceExternalRef = sourceExternalRef;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public void setEngineBindingType(String engineBindingType) {
        this.engineBindingType = engineBindingType;
    }

    public void setEngineBindingId(Long engineBindingId) {
        this.engineBindingId = engineBindingId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setAdminState(String adminState) {
        this.adminState = adminState;
    }

    public void setActiveSide(String activeSide) {
        this.activeSide = activeSide;
    }

    public void setRpoSeconds(Integer rpoSeconds) {
        this.rpoSeconds = rpoSeconds;
    }

    public void setRtoSeconds(Integer rtoSeconds) {
        this.rtoSeconds = rtoSeconds;
    }

    public void setScheduleJson(String scheduleJson) {
        this.scheduleJson = scheduleJson;
    }

    public void setPolicyJson(String policyJson) {
        this.policyJson = policyJson;
    }

    public void setMappingJson(String mappingJson) {
        this.mappingJson = mappingJson;
    }

    public void setQuiescePolicyJson(String quiescePolicyJson) {
        this.quiescePolicyJson = quiescePolicyJson;
    }

    public void setSourceWorkerHostId(Long sourceWorkerHostId) {
        this.sourceWorkerHostId = sourceWorkerHostId;
    }

    public void setTargetWorkerHostId(Long targetWorkerHostId) {
        this.targetWorkerHostId = targetWorkerHostId;
    }

    public void setCoordinatorWorkerHostId(Long coordinatorWorkerHostId) {
        this.coordinatorWorkerHostId = coordinatorWorkerHostId;
    }

    public void setProtectionGroupUuid(String value) { protectionGroupUuid = value; }
    public void setProtectionGroupName(String value) { protectionGroupName = value; }
    public void setProtectionGroupOrder(Integer value) { protectionGroupOrder = value; }
    public void setProtectionGroupMaxParallel(Integer value) { protectionGroupMaxParallel = value; }
    public void setProtectionGroupQuiesceRequired(Boolean value) { protectionGroupQuiesceRequired = value; }

    public void setLastSourceCheckpointAt(Date lastSourceCheckpointAt) {
        this.lastSourceCheckpointAt = lastSourceCheckpointAt;
    }

    public void setLastTargetDurableAt(Date lastTargetDurableAt) {
        this.lastTargetDurableAt = lastTargetDurableAt;
    }

    public void setTargetReadyAt(Date targetReadyAt) {
        this.targetReadyAt = targetReadyAt;
    }

    public void setTargetReadyRpoSeconds(Integer targetReadyRpoSeconds) {
        this.targetReadyRpoSeconds = targetReadyRpoSeconds;
    }

    public void setLastRunId(Long lastRunId) {
        this.lastRunId = lastRunId;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public void markUpdated() {
        updated = new Date();
    }

    public void markRemoved() {
        removed = new Date();
        markUpdated();
    }
}
