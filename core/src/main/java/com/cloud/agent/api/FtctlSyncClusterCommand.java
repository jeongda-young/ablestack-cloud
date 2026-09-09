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

public class FtctlSyncClusterCommand extends Command {

    private String clusterName;
    private String localHostId;
    private String localRole;
    private String localManagementIp;
    private String localLibvirtUri;
    private String localBlockcopyIp;
    private String localXcoloControlIp;
    private String localXcoloDataIp;
    private String peerHostId;
    private String peerRole;
    private String peerManagementIp;
    private String peerLibvirtUri;
    private String peerBlockcopyIp;
    private String peerXcoloControlIp;
    private String peerXcoloDataIp;

    public FtctlSyncClusterCommand() {
    }

    public FtctlSyncClusterCommand(String clusterName, String localHostId, String peerHostId) {
        this.clusterName = clusterName;
        this.localHostId = localHostId;
        this.peerHostId = peerHostId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getLocalHostId() {
        return localHostId;
    }

    public void setLocalHostId(String localHostId) {
        this.localHostId = localHostId;
    }

    public String getLocalRole() {
        return localRole;
    }

    public void setLocalRole(String localRole) {
        this.localRole = localRole;
    }

    public String getLocalManagementIp() {
        return localManagementIp;
    }

    public void setLocalManagementIp(String localManagementIp) {
        this.localManagementIp = localManagementIp;
    }

    public String getLocalLibvirtUri() {
        return localLibvirtUri;
    }

    public void setLocalLibvirtUri(String localLibvirtUri) {
        this.localLibvirtUri = localLibvirtUri;
    }

    public String getLocalBlockcopyIp() {
        return localBlockcopyIp;
    }

    public void setLocalBlockcopyIp(String localBlockcopyIp) {
        this.localBlockcopyIp = localBlockcopyIp;
    }

    public String getLocalXcoloControlIp() {
        return localXcoloControlIp;
    }

    public void setLocalXcoloControlIp(String localXcoloControlIp) {
        this.localXcoloControlIp = localXcoloControlIp;
    }

    public String getLocalXcoloDataIp() {
        return localXcoloDataIp;
    }

    public void setLocalXcoloDataIp(String localXcoloDataIp) {
        this.localXcoloDataIp = localXcoloDataIp;
    }

    public String getPeerHostId() {
        return peerHostId;
    }

    public void setPeerHostId(String peerHostId) {
        this.peerHostId = peerHostId;
    }

    public String getPeerRole() {
        return peerRole;
    }

    public void setPeerRole(String peerRole) {
        this.peerRole = peerRole;
    }

    public String getPeerManagementIp() {
        return peerManagementIp;
    }

    public void setPeerManagementIp(String peerManagementIp) {
        this.peerManagementIp = peerManagementIp;
    }

    public String getPeerLibvirtUri() {
        return peerLibvirtUri;
    }

    public void setPeerLibvirtUri(String peerLibvirtUri) {
        this.peerLibvirtUri = peerLibvirtUri;
    }

    public String getPeerBlockcopyIp() {
        return peerBlockcopyIp;
    }

    public void setPeerBlockcopyIp(String peerBlockcopyIp) {
        this.peerBlockcopyIp = peerBlockcopyIp;
    }

    public String getPeerXcoloControlIp() {
        return peerXcoloControlIp;
    }

    public void setPeerXcoloControlIp(String peerXcoloControlIp) {
        this.peerXcoloControlIp = peerXcoloControlIp;
    }

    public String getPeerXcoloDataIp() {
        return peerXcoloDataIp;
    }

    public void setPeerXcoloDataIp(String peerXcoloDataIp) {
        this.peerXcoloDataIp = peerXcoloDataIp;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
