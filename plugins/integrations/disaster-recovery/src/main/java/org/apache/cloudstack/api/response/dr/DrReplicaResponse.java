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

import java.util.Date;

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.dr.DrReplicaVO;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DrReplicaVO.class)
public class DrReplicaResponse extends BaseResponse {
    @SerializedName("id")
    @Param(description = "the DR replica ID")
    private String id;

    @SerializedName("planid")
    @Param(description = "the DR plan ID")
    private Long planId;

    @SerializedName("targetsiteid")
    @Param(description = "the target site ID")
    private Long targetSiteId;

    @SerializedName("targetvmid")
    @Param(description = "the target virtual machine ID")
    private Long targetVmId;

    @SerializedName("targetexternalref")
    @Param(description = "the target external reference")
    private String targetExternalRef;

    @SerializedName("targetvmname")
    @Param(description = "the target virtual machine name")
    private String targetVmName;

    @SerializedName("state")
    @Param(description = "the replica state")
    private String state;

    @SerializedName("powerstate")
    @Param(description = "the target power state")
    private String powerState;

    @SerializedName("hypervisortype")
    @Param(description = "the target hypervisor type")
    private String hypervisorType;

    @SerializedName("activeside")
    @Param(description = "the active side")
    private String activeSide;

    @SerializedName("runtimestate")
    @Param(description = "the runtime state JSON")
    private String runtimeStateJson;

    @SerializedName("ownershipstate")
    @Param(description = "the target resource ownership state")
    private String ownershipState;

    @SerializedName("ownershipgeneration")
    @Param(description = "the target resource ownership generation")
    private Long ownershipGeneration;

    @SerializedName("materializationdigest")
    @Param(description = "the target materialization manifest SHA-256")
    private String materializationDigest;

    @SerializedName("powerstateobservedat")
    @Param(description = "the target power state observation time")
    private Date powerStateObservedAt;

    @SerializedName("created")
    @Param(description = "the replica creation time")
    private Date created;

    public void setId(String id) {
        this.id = id;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public void setTargetSiteId(Long targetSiteId) {
        this.targetSiteId = targetSiteId;
    }

    public void setTargetVmId(Long targetVmId) {
        this.targetVmId = targetVmId;
    }

    public void setTargetExternalRef(String targetExternalRef) {
        this.targetExternalRef = targetExternalRef;
    }

    public void setTargetVmName(String targetVmName) {
        this.targetVmName = targetVmName;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setPowerState(String powerState) {
        this.powerState = powerState;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public void setActiveSide(String activeSide) {
        this.activeSide = activeSide;
    }

    public void setRuntimeStateJson(String runtimeStateJson) {
        this.runtimeStateJson = runtimeStateJson;
    }

    public void setOwnershipState(String ownershipState) { this.ownershipState = ownershipState; }
    public void setOwnershipGeneration(Long ownershipGeneration) { this.ownershipGeneration = ownershipGeneration; }
    public void setMaterializationDigest(String materializationDigest) { this.materializationDigest = materializationDigest; }
    public void setPowerStateObservedAt(Date powerStateObservedAt) { this.powerStateObservedAt = powerStateObservedAt; }

    public void setCreated(Date created) {
        this.created = created;
    }
}
