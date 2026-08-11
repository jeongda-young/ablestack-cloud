// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
package org.apache.cloudstack.api.response;

import java.util.Date;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class GuestToolsResponse extends BaseResponse {
    @SerializedName("installed")
    @Param(description = "Whether ABLESTACK guest tools were observed")
    private boolean installed;
    @SerializedName("version")
    @Param(description = "ABLESTACK guest tools package version")
    private String version;
    @SerializedName("qgapolicymode")
    @Param(description = "Observed QGA RPC policy mode")
    private String qgaPolicyMode;
    @SerializedName("readinessstatus")
    @Param(description = "Guest network observation readiness status")
    private String readinessStatus;
    @SerializedName("checked")
    @Param(description = "Last readiness check time")
    private Date checked;

    public boolean isInstalled() { return installed; }
    public void setInstalled(boolean installed) { this.installed = installed; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getQgaPolicyMode() { return qgaPolicyMode; }
    public void setQgaPolicyMode(String qgaPolicyMode) { this.qgaPolicyMode = qgaPolicyMode; }
    public String getReadinessStatus() { return readinessStatus; }
    public void setReadinessStatus(String readinessStatus) { this.readinessStatus = readinessStatus; }
    public Date getChecked() { return checked; }
    public void setChecked(Date checked) { this.checked = checked; }
}
