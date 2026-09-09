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
@Table(name = "dr_run_step")
public class DrRunStepVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "run_id")
    private long runId;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "step_order")
    private int stepOrder;

    @Column(name = "state")
    private String state;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "started")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date started;

    @Column(name = "completed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date completed;

    @Column(name = "details_json", length = 65535)
    private String detailsJson;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", length = 4096)
    private String errorMessage;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    protected DrRunStepVO() {
    }

    public DrRunStepVO(long runId, String stepName, int stepOrder) {
        this.runId = runId;
        this.stepName = stepName;
        this.stepOrder = stepOrder;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public long getRunId() {
        return runId;
    }

    public String getStepName() {
        return stepName;
    }

    public int getStepOrder() {
        return stepOrder;
    }

    public String getState() {
        return state;
    }

    public Integer getProgress() {
        return progress;
    }

    public Date getStarted() {
        return started;
    }

    public Date getCompleted() {
        return completed;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
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

    public void setState(String state) {
        this.state = state;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public void setCompleted(Date completed) {
        this.completed = completed;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void markUpdated() {
        updated = new Date();
    }

    public void markRemoved() {
        removed = new Date();
        markUpdated();
    }
}
