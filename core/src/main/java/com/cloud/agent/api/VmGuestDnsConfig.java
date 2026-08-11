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

import java.util.ArrayList;
import java.util.List;

public class VmGuestDnsConfig {
    private String interfaceName;
    private String source;
    private boolean global;
    private List<VmGuestDnsServer> servers;
    private List<VmGuestDnsDomain> domains;

    public VmGuestDnsConfig() {
        servers = new ArrayList<>();
        domains = new ArrayList<>();
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }

    public List<VmGuestDnsServer> getServers() {
        return servers;
    }

    public void setServers(List<VmGuestDnsServer> servers) {
        this.servers = servers;
    }

    public List<VmGuestDnsDomain> getDomains() {
        return domains;
    }

    public void setDomains(List<VmGuestDnsDomain> domains) {
        this.domains = domains;
    }
}
