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
package com.cloud.vm.guestnetwork;

import java.util.Date;

import com.cloud.agent.api.VmGuestNetworkState;
import com.cloud.vm.VmGuestNetworkStateVO;

public interface VmGuestNetworkStateService {
    VmGuestNetworkStateVO findByVmId(long vmId);

    PersistResult persistSuccess(long vmId, VmGuestNetworkState state, Date observedAt);

    PersistResult persistFailure(long vmId, VmGuestNetworkState state, String errorCode,
            String errorMessage, Date observedAt);

    PersistResult markStopped(long vmId, Date observedAt);

    boolean removeByVmId(long vmId);

    enum PersistResult {
        CREATED,
        PAYLOAD_UPDATED,
        METADATA_ONLY,
        REMOVED,
        NOT_FOUND
    }
}
