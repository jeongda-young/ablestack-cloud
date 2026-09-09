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

public class FtctlDrSourceHardwareCommand extends Command {
    private String endpoint;
    private String principal;
    private String password;
    private Boolean tlsVerify;
    private String sourceVmRef;

    protected FtctlDrSourceHardwareCommand() {
    }

    public FtctlDrSourceHardwareCommand(String endpoint, String principal, String password, Boolean tlsVerify,
            String sourceVmRef) {
        this.endpoint = endpoint;
        this.principal = principal;
        this.password = password;
        this.tlsVerify = tlsVerify;
        this.sourceVmRef = sourceVmRef;
    }

    public String getEndpoint() { return endpoint; }
    public String getPrincipal() { return principal; }
    public String getPassword() { return password; }
    public Boolean getTlsVerify() { return tlsVerify; }
    public String getSourceVmRef() { return sourceVmRef; }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
