/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.agent.api;

import java.util.List;

public class LicenseHostCommand extends Command {

    private List<String> licenseHostName;
    private List<String> licenseHostValue;
    private Long id;

    public LicenseHostCommand() {
    }

    public LicenseHostCommand(Long id) {
        super();
        this.id = id;
    }

     public LicenseHostCommand(Long id, List<String> licenseHostName, List<String> licenseHostValue) {
         this.id = id;
         this.licenseHostName = licenseHostName;
         this.licenseHostValue = licenseHostValue;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public List<String> getLicenseHostName() {
        return licenseHostName;
     }

     public void setLicenseHostName(List<String> licenseHostName) {
         this.licenseHostName = licenseHostName;
    }

    public List<String> getLicenseHostValue() {
        return licenseHostValue;
     }

     public void setLicenseHostValue(List<String> licenseHostValue) {
         this.licenseHostValue = licenseHostValue;
    }

    public Long getId() {
        return id;
    }

     public void setId(Long id) {
         this.id = id;
    }
}
