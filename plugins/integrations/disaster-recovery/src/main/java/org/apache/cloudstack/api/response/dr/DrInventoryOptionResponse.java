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
package org.apache.cloudstack.api.response.dr;

import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DrInventoryOptionResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the external inventory ID")
    private String id;

    @SerializedName("value")
    @Param(description = "the value that can be submitted back to Cloud APIs")
    private String value;

    @SerializedName("externalid")
    @Param(description = "the remote inventory external ID")
    private String externalId;

    @SerializedName("localid")
    @Param(description = "the optional local numeric inventory ID")
    private String localId;

    @SerializedName("referencetype")
    @Param(description = "the DR plan source reference type")
    private String referenceType;

    @SerializedName("sourcevmid")
    @Param(description = "the local Cloud VM database ID when the option maps to a local user VM")
    private Long sourceVmId;

    @SerializedName("externalref")
    @Param(description = "the external source workload reference")
    private String externalRef;

    @SerializedName("state")
    @Param(description = "the source workload power or lifecycle state")
    private String state;

    @SerializedName("hypervisor")
    @Param(description = "the source workload hypervisor type")
    private String hypervisorType;

    @SerializedName("name")
    @Param(description = "the display name")
    private String name;

    @SerializedName("description")
    @Param(description = "the display description")
    private String description;

    @SerializedName("type")
    @Param(description = "the inventory option type")
    private String type;

    @SerializedName("selectable")
    @Param(description = "true if the option can be selected")
    private Boolean selectable;

    @SerializedName("details")
    @Param(description = "non-secret option metadata JSON")
    private String detailsJson;

    public void setId(String id) {
        this.id = id;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public void setSourceVmId(Long sourceVmId) {
        this.sourceVmId = sourceVmId;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSelectable(Boolean selectable) {
        this.selectable = selectable;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }
}
