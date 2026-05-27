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

public class AblestackV2KListVmwareVmsCommand extends Command {

    private String vcenter;
    private String datacenterName;
    private String username;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private String password;
    private String instanceName;
    private String keyword;
    private Long startIndex;
    private Long pageSize;

    public AblestackV2KListVmwareVmsCommand() {
    }

    public AblestackV2KListVmwareVmsCommand(String vcenter, String datacenterName, String username, String password, String instanceName) {
        this(vcenter, datacenterName, username, password, instanceName, null, null, null);
    }

    public AblestackV2KListVmwareVmsCommand(String vcenter, String datacenterName, String username, String password, String instanceName,
                                            String keyword, Long startIndex, Long pageSize) {
        this.vcenter = vcenter;
        this.datacenterName = datacenterName;
        this.username = username;
        this.password = password;
        this.instanceName = instanceName;
        this.keyword = keyword;
        this.startIndex = startIndex;
        this.pageSize = pageSize;
    }

    public String getVcenter() {
        return vcenter;
    }

    public String getDatacenterName() {
        return datacenterName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getKeyword() {
        return keyword;
    }

    public Long getStartIndex() {
        return startIndex;
    }

    public Long getPageSize() {
        return pageSize;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
