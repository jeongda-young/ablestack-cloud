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
package com.cloud.agent.api;

public class VmGuestNetworkSectionStatus {
    private String status;
    private String details;
    private boolean truncated;
    private Integer originalCount;
    private String source;
    private String errorCode;
    private long attemptedAt;
    private Long succeededAt;

    public VmGuestNetworkSectionStatus() {
    }

    public VmGuestNetworkSectionStatus(String status) {
        this.status = status;
    }

    public VmGuestNetworkSectionStatus(String status, String details) {
        this.status = status;
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public Integer getOriginalCount() {
        return originalCount;
    }

    public void setOriginalCount(Integer originalCount) {
        this.originalCount = originalCount;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public long getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(long attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public Long getSucceededAt() {
        return succeededAt;
    }

    public void setSucceededAt(Long succeededAt) {
        this.succeededAt = succeededAt;
    }
}
