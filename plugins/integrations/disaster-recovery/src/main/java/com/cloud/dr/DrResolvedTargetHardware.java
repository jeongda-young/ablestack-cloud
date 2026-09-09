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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;

public class DrResolvedTargetHardware {
    private ApiConstants.BootType bootType;
    private ApiConstants.BootMode bootMode;
    private String rootDiskController;
    private String dataDiskController;
    private Boolean ioThreadsEnabled;
    private ApiConstants.IoDriverPolicy ioPolicy;

    public ApiConstants.BootType getBootType() {
        return bootType;
    }

    public void setBootType(ApiConstants.BootType bootType) {
        this.bootType = bootType;
    }

    public ApiConstants.BootMode getBootMode() {
        return bootMode;
    }

    public void setBootMode(ApiConstants.BootMode bootMode) {
        this.bootMode = bootMode;
    }

    public String getRootDiskController() {
        return rootDiskController;
    }

    public void setRootDiskController(String rootDiskController) {
        this.rootDiskController = StringUtils.trimToNull(rootDiskController);
    }

    public String getDataDiskController() {
        return dataDiskController;
    }

    public void setDataDiskController(String dataDiskController) {
        this.dataDiskController = StringUtils.trimToNull(dataDiskController);
    }

    public Boolean getIoThreadsEnabled() {
        return ioThreadsEnabled;
    }

    public void setIoThreadsEnabled(Boolean ioThreadsEnabled) {
        this.ioThreadsEnabled = ioThreadsEnabled;
    }

    public ApiConstants.IoDriverPolicy getIoPolicy() {
        return ioPolicy;
    }

    public void setIoPolicy(ApiConstants.IoDriverPolicy ioPolicy) {
        this.ioPolicy = ioPolicy;
    }

    public JsonObject toJsonObject() {
        JsonObject object = new JsonObject();
        if (bootType != null) {
            object.addProperty("bootType", bootType.toString());
        }
        if (bootMode != null) {
            object.addProperty("bootMode", bootMode.toString());
        }
        if (StringUtils.isNotBlank(rootDiskController)) {
            object.addProperty("rootDiskController", rootDiskController);
        }
        if (StringUtils.isNotBlank(dataDiskController)) {
            object.addProperty("dataDiskController", dataDiskController);
        }
        if (ioThreadsEnabled != null) {
            object.addProperty("ioThreadsEnabled", ioThreadsEnabled);
        }
        if (ioPolicy != null) {
            object.addProperty("ioPolicy", ioPolicy.toString());
        }
        return object;
    }
}
