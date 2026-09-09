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

import java.util.HashMap;
import java.util.Map;

public class FtctlActionCommand extends Command {

    public enum Action {
        PROTECT("protect"),
        PROTECT_START("protect-start"),
        PAUSE_PROTECTION("pause-protection"),
        RESUME_PROTECTION("resume-protection"),
        FAILOVER("failover"),
        FAILOVER_PREPARE("failover-prepare"),
        FAILBACK("failback"),
        FAILBACK_SYNC("failback-sync"),
        FAILBACK_FINALIZE("failback-finalize"),
        FAILBACK_REPROTECT("failback-reprotect"),
        UNPROTECT("unprotect"),
        FENCE_CONFIRM("fence-confirm"),
        FENCE_CLEAR("fence-clear");

        private final String cliCommand;

        Action(String cliCommand) {
            this.cliCommand = cliCommand;
        }

        public String getCliCommand() {
            return cliCommand;
        }
    }

    private Action action;
    private String vmName;
    private String mode;
    private String peerUri;
    private String profileName;
    private boolean force;
    private boolean forceCleanup;
    private Map<String, String> context = new HashMap<>();

    public FtctlActionCommand() {
    }

    public FtctlActionCommand(Action action, String vmName) {
        this.action = action;
        this.vmName = vmName;
    }

    public Action getAction() {
        return action;
    }

    public String getVmName() {
        return vmName;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getPeerUri() {
        return peerUri;
    }

    public void setPeerUri(String peerUri) {
        this.peerUri = peerUri;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public boolean isForceCleanup() {
        return forceCleanup;
    }

    public void setForceCleanup(boolean forceCleanup) {
        this.forceCleanup = forceCleanup;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public void setContext(Map<String, String> context) {
        this.context = context == null ? new HashMap<>() : context;
    }

    public void setContextParam(String key, String value) {
        if (key != null && value != null) {
            context.put(key, value);
        }
    }

    public String getContextParam(String key) {
        return key == null ? null : context.get(key);
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
