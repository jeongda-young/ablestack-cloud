// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
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
package org.apache.cloudstack.api.command.admin.ftctl;

import com.cloud.user.Account;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;

public abstract class AbstractFtctlRemoteMoldCmd extends BaseCmd {

    @Parameter(name = "remotemoldapiurl", type = CommandType.STRING, required = true,
            description = "the remote Mold API URL")
    private String remoteMoldApiUrl;

    @Parameter(name = "remotemoldapikey", type = CommandType.STRING, required = true,
            description = "the remote Mold API key")
    private String remoteMoldApiKey;

    @Parameter(name = "remotemoldsecretkey", type = CommandType.STRING, required = true,
            description = "the remote Mold secret key")
    private String remoteMoldSecretKey;

    public String getRemoteMoldApiUrl() {
        return remoteMoldApiUrl;
    }

    public String getRemoteMoldApiKey() {
        return remoteMoldApiKey;
    }

    public String getRemoteMoldSecretKey() {
        return remoteMoldSecretKey;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
