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
package com.cloud.ssv;

import org.apache.cloudstack.api.command.user.ssv.CreateSSVCmd;
import org.apache.cloudstack.api.command.admin.ssv.ListAdminSSVCmd;
import org.apache.cloudstack.api.command.user.ssv.ListSSVCmd;
import org.apache.cloudstack.api.response.SSVResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;

public interface SSVService extends PluggableService, Configurable {

    static final ConfigKey<Boolean> SSVEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class,
            "cloud.shared.storage.vm.service.enabled",
            "false",
            "Indicates whether Desktop Service plugin is enabled or not. Management server restart needed on change",
            false);

    SSV findById(final Long id);

    SSV createSSV(CreateSSVCmd cmd) throws CloudRuntimeException;
    boolean startSSV(long ssvId, boolean onCreate) throws CloudRuntimeException;
    boolean stopSSV(long ssvId) throws CloudRuntimeException;
    boolean deleteSSV(Long ssvId) throws CloudRuntimeException;
    ListResponse<SSVResponse> listAdminSSV(ListAdminSSVCmd cmd);
    ListResponse<SSVResponse> listSSV(ListSSVCmd cmd);

    SSVResponse createSSVResponse(long ssvId);
}
