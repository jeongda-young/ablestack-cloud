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
package com.cloud.dr;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.utils.db.Encrypt;

@Entity
@Table(name = "dr_site_credential")
public class DrSiteCredentialVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "site_id")
    private long siteId;

    @Column(name = "credential_type")
    private String credentialType;

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "principal")
    private String principal;

    @Encrypt
    @Column(name = "secret_payload", length = 65535)
    private String secretPayload;

    @Column(name = "tls_verify")
    private Boolean tlsVerify;

    @Column(name = "state")
    private String state;

    @Column(name = "last_validated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastValidated;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = new Date();

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date updated;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    protected DrSiteCredentialVO() {
    }

    public DrSiteCredentialVO(long siteId, String credentialType) {
        this.siteId = siteId;
        this.credentialType = credentialType;
    }

    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public long getSiteId() {
        return siteId;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getPrincipal() {
        return principal;
    }

    public String getSecretPayload() {
        return secretPayload;
    }

    public Boolean getTlsVerify() {
        return tlsVerify;
    }

    public String getState() {
        return state;
    }

    public Date getLastValidated() {
        return lastValidated;
    }

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public void setSecretPayload(String secretPayload) {
        this.secretPayload = secretPayload;
    }

    public void setTlsVerify(Boolean tlsVerify) {
        this.tlsVerify = tlsVerify;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setLastValidated(Date lastValidated) {
        this.lastValidated = lastValidated;
    }

    public void markUpdated() {
        updated = new Date();
    }

    public void markRemoved() {
        removed = new Date();
        markUpdated();
    }
}
