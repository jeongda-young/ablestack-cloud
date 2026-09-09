// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.response.ftctl;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

public class FtctlRemoteMoldHostResponse extends BaseResponse {

    @SerializedName("id")
    @Param(description = "the remote Mold host ID")
    private String id;

    @SerializedName("name")
    @Param(description = "the remote Mold host name")
    private String name;

    @SerializedName("ipaddress")
    @Param(description = "the remote Mold host management IP address")
    private String ipAddress;

    @SerializedName("migrationip")
    @Param(description = "the remote Mold host migration or blockcopy IP address")
    private String migrationIp;

    @SerializedName("clusterid")
    @Param(description = "the remote Mold host cluster ID")
    private String clusterId;

    @SerializedName("clustername")
    @Param(description = "the remote Mold host cluster name")
    private String clusterName;

    @SerializedName("zoneid")
    @Param(description = "the remote Mold host zone ID")
    private String zoneId;

    @SerializedName("zonename")
    @Param(description = "the remote Mold host zone name")
    private String zoneName;

    @SerializedName("hypervisor")
    @Param(description = "the remote Mold host hypervisor")
    private String hypervisor;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setMigrationIp(String migrationIp) {
        this.migrationIp = migrationIp;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }
}
