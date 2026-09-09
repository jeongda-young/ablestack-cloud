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
package org.apache.cloudstack.api.response.ftctl;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

import java.util.List;

public class FtctlEventsResponse extends BaseResponse {

    @SerializedName("virtualmachineid")
    @Param(description = "the virtual machine ID")
    private Long virtualMachineId;

    @SerializedName("vmname")
    @Param(description = "the virtual machine name")
    private String vmName;

    @SerializedName("result")
    @Param(description = "the FTCTL events query result")
    private String result;

    @SerializedName("count")
    @Param(description = "the number of FTCTL events returned")
    private Integer count;

    @SerializedName("events")
    @Param(description = "the FTCTL event list", responseObject = FtctlEventResponse.class)
    private List<FtctlEventResponse> events;

    @SerializedName("latestprogress")
    @Param(description = "the latest FTCTL sync progress event details as JSON string")
    private String latestProgress;

    @SerializedName("syncprogresspercent")
    @Param(description = "the latest FTCTL sync progress percentage")
    private Double syncProgressPercent;

    @SerializedName("synccopiedbytes")
    @Param(description = "the latest FTCTL sync copied bytes")
    private Long syncCopiedBytes;

    @SerializedName("synctotalbytes")
    @Param(description = "the latest FTCTL sync total bytes")
    private Long syncTotalBytes;

    @SerializedName("syncready")
    @Param(description = "whether the latest FTCTL sync is ready")
    private Boolean syncReady;

    @SerializedName("syncdirection")
    @Param(description = "the latest FTCTL sync direction")
    private String syncDirection;

    @SerializedName("syncupdated")
    @Param(description = "the latest FTCTL sync progress update timestamp")
    private String syncUpdated;

    @SerializedName("syncprogressjson")
    @Param(description = "the latest FTCTL sync progress JSON")
    private String syncProgressJson;

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getVmName() {
        return vmName;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getCount() {
        return count;
    }

    public void setEvents(List<FtctlEventResponse> events) {
        this.events = events;
    }

    public List<FtctlEventResponse> getEvents() {
        return events;
    }

    public void setLatestProgress(String latestProgress) {
        this.latestProgress = latestProgress;
    }

    public String getLatestProgress() {
        return latestProgress;
    }

    public void setSyncProgressPercent(Double syncProgressPercent) {
        this.syncProgressPercent = syncProgressPercent;
    }

    public Double getSyncProgressPercent() {
        return syncProgressPercent;
    }

    public void setSyncCopiedBytes(Long syncCopiedBytes) {
        this.syncCopiedBytes = syncCopiedBytes;
    }

    public Long getSyncCopiedBytes() {
        return syncCopiedBytes;
    }

    public void setSyncTotalBytes(Long syncTotalBytes) {
        this.syncTotalBytes = syncTotalBytes;
    }

    public Long getSyncTotalBytes() {
        return syncTotalBytes;
    }

    public void setSyncReady(Boolean syncReady) {
        this.syncReady = syncReady;
    }

    public Boolean getSyncReady() {
        return syncReady;
    }

    public void setSyncDirection(String syncDirection) {
        this.syncDirection = syncDirection;
    }

    public String getSyncDirection() {
        return syncDirection;
    }

    public void setSyncUpdated(String syncUpdated) {
        this.syncUpdated = syncUpdated;
    }

    public String getSyncUpdated() {
        return syncUpdated;
    }

    public void setSyncProgressJson(String syncProgressJson) {
        this.syncProgressJson = syncProgressJson;
    }

    public String getSyncProgressJson() {
        return syncProgressJson;
    }
}
