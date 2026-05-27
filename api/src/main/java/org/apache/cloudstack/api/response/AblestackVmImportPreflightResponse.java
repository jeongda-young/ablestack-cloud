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
package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.BaseResponse;

public class AblestackVmImportPreflightResponse extends BaseResponse {

    @SerializedName("success")
    @Param(description = "true when the preflight checks passed")
    private Boolean success;

    @SerializedName("migrationtool")
    @Param(description = "the migration tool selected for the import")
    private String migrationTool;

    @SerializedName("sourceprovider")
    @Param(description = "the source provider selected for the import")
    private String sourceProvider;

    @SerializedName("targetprovider")
    @Param(description = "the target provider selected for the import")
    private String targetProvider;

    @SerializedName("sourceapi")
    @Param(description = "the source API path selected after probing")
    private String sourceApi;

    @SerializedName("sourcevmcount")
    @Param(description = "the number of source VMs visible through the selected source API")
    private Integer sourceVmCount;

    @SerializedName("sourcevmname")
    @Param(description = "the matched source VM name when a VM filter is supplied")
    private String sourceVmName;

    @SerializedName("targetstorage")
    @Param(description = "the resolved target storage type")
    private String targetStorage;

    @SerializedName("targetformat")
    @Param(description = "the resolved target disk format")
    private String targetFormat;

    @SerializedName("message")
    @Param(description = "human-readable preflight result")
    private String message;

    @SerializedName("details")
    @Param(description = "non-secret preflight details")
    private String details;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMigrationTool() {
        return migrationTool;
    }

    public void setMigrationTool(String migrationTool) {
        this.migrationTool = migrationTool;
    }

    public String getSourceProvider() {
        return sourceProvider;
    }

    public void setSourceProvider(String sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    public String getTargetProvider() {
        return targetProvider;
    }

    public void setTargetProvider(String targetProvider) {
        this.targetProvider = targetProvider;
    }

    public String getSourceApi() {
        return sourceApi;
    }

    public void setSourceApi(String sourceApi) {
        this.sourceApi = sourceApi;
    }

    public Integer getSourceVmCount() {
        return sourceVmCount;
    }

    public void setSourceVmCount(Integer sourceVmCount) {
        this.sourceVmCount = sourceVmCount;
    }

    public String getSourceVmName() {
        return sourceVmName;
    }

    public void setSourceVmName(String sourceVmName) {
        this.sourceVmName = sourceVmName;
    }

    public String getTargetStorage() {
        return targetStorage;
    }

    public void setTargetStorage(String targetStorage) {
        this.targetStorage = targetStorage;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
