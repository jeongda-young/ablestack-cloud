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

public class AblestackV2KCleanupCommand extends Command {

    private String workdir;
    private String domainName;
    private boolean keepSourceSnapshots;
    private boolean removeWorkdir;
    private boolean undefineDomain;

    public AblestackV2KCleanupCommand() {
    }

    public AblestackV2KCleanupCommand(String workdir, String domainName, boolean keepSourceSnapshots,
                                      boolean removeWorkdir, boolean undefineDomain) {
        this.workdir = workdir;
        this.domainName = domainName;
        this.keepSourceSnapshots = keepSourceSnapshots;
        this.removeWorkdir = removeWorkdir;
        this.undefineDomain = undefineDomain;
    }

    public String getWorkdir() {
        return workdir;
    }

    public String getDomainName() {
        return domainName;
    }

    public boolean isKeepSourceSnapshots() {
        return keepSourceSnapshots;
    }

    public boolean isRemoveWorkdir() {
        return removeWorkdir;
    }

    public boolean isUndefineDomain() {
        return undefineDomain;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
