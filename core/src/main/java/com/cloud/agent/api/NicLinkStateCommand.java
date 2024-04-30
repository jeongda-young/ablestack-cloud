//
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
//

package com.cloud.agent.api;

import com.cloud.agent.api.to.NicTO;

public class NicLinkStateCommand extends Command {

    NicTO nic;
    String instanceName;
    boolean linkState;

    public NicTO getNic() {
        return nic;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    protected NicLinkStateCommand() {
    }

    public NicLinkStateCommand(NicTO nic, String instanceName, boolean linkState) {
        this.nic = nic;
        this.instanceName = instanceName;
        this.linkState = linkState;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public boolean getLinkState() {
        return linkState;
    }

    public void setLinkState(boolean linkState) {
        this.linkState = linkState;
    }


}
