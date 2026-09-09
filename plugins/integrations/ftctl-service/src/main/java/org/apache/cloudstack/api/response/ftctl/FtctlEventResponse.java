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

public class FtctlEventResponse extends BaseResponse {

    @SerializedName("ts")
    @Param(description = "the FTCTL event timestamp")
    private String timestamp;

    @SerializedName("scanid")
    @Param(description = "the FTCTL scan ID")
    private String scanId;

    @SerializedName("vm")
    @Param(description = "the FTCTL event virtual machine name")
    private String vmName;

    @SerializedName("stage")
    @Param(description = "the FTCTL event stage")
    private String stage;

    @SerializedName("event")
    @Param(description = "the FTCTL event name")
    private String event;

    @SerializedName("result")
    @Param(description = "the FTCTL event result")
    private String result;

    @SerializedName("rc")
    @Param(description = "the FTCTL event return code")
    private Integer rc;

    @SerializedName("details")
    @Param(description = "the FTCTL event details as JSON string")
    private String details;

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setScanId(String scanId) {
        this.scanId = scanId;
    }

    public String getScanId() {
        return scanId;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getVmName() {
        return vmName;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getStage() {
        return stage;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getEvent() {
        return event;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setRc(Integer rc) {
        this.rc = rc;
    }

    public Integer getRc() {
        return rc;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}
