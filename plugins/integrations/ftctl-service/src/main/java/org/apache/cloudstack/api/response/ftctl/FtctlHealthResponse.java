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

public class FtctlHealthResponse extends BaseResponse {

    @SerializedName("virtualmachineid")
    @Param(description = "the virtual machine ID")
    private Long virtualMachineId;

    @SerializedName("hostid")
    @Param(description = "the execution host ID")
    private Long hostId;

    @SerializedName("hostname")
    @Param(description = "the execution host name")
    private String hostName;

    @SerializedName("result")
    @Param(description = "the FTCTL health result")
    private String result;

    @SerializedName("uri")
    @Param(description = "the libvirt URI used by FTCTL health")
    private String uri;

    @SerializedName("rc")
    @Param(description = "the FTCTL health return code")
    private Integer rc;

    public void setVirtualMachineId(Long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setRc(Integer rc) {
        this.rc = rc;
    }
}
