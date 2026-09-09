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

public class FtctlDrSshAccessCommand extends Command {

    public enum Action {
        ENSURE_KEY,
        INSTALL_KEY,
        REMOVE_KEY
    }

    private Action action;
    private String profile;
    private String publicKey;
    private String keyComment;
    private String sshUser;
    private boolean applyFirewall;

    public FtctlDrSshAccessCommand() {
    }

    public FtctlDrSshAccessCommand(Action action, String profile) {
        this.action = action;
        this.profile = profile;
    }

    public Action getAction() {
        return action;
    }

    public String getProfile() {
        return profile;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getKeyComment() {
        return keyComment;
    }

    public void setKeyComment(String keyComment) {
        this.keyComment = keyComment;
    }

    public String getSshUser() {
        return sshUser;
    }

    public void setSshUser(String sshUser) {
        this.sshUser = sshUser;
    }

    public boolean isApplyFirewall() {
        return applyFirewall;
    }

    public void setApplyFirewall(boolean applyFirewall) {
        this.applyFirewall = applyFirewall;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
