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
package org.apache.cloudstack.vm;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.admin.vm.ImportUnmanagedInstanceForAblestackN2KCmd;
import org.apache.cloudstack.api.command.admin.vm.ImportUnmanagedInstanceForAblestackV2KCmd;
import org.apache.cloudstack.api.command.admin.vm.ImportVmCmd;

public class AblestackVmMigrationRequest {

    private final ImportVmTask.MigrationTool migrationTool;
    private final ImportVmTask.SourceProvider sourceProvider;
    private final ImportVmTask.TargetProvider targetProvider;
    private final String splitMode;
    private final String continuationTaskId;
    private final ImportVmCmd importCommand;

    public AblestackVmMigrationRequest(ImportVmTask.MigrationTool migrationTool,
                                       ImportVmTask.SourceProvider sourceProvider,
                                       ImportVmTask.TargetProvider targetProvider,
                                       String splitMode,
                                       String continuationTaskId,
                                       ImportVmCmd importCommand) {
        this.migrationTool = migrationTool;
        this.sourceProvider = sourceProvider;
        this.targetProvider = targetProvider;
        this.splitMode = splitMode;
        this.continuationTaskId = continuationTaskId;
        this.importCommand = importCommand;
    }

    public static AblestackVmMigrationRequest forAblestackV2K(ImportUnmanagedInstanceForAblestackV2KCmd cmd) {
        return new AblestackVmMigrationRequest(ImportVmTask.MigrationTool.AblestackV2K,
                ImportVmTask.SourceProvider.VMware, ImportVmTask.TargetProvider.Cloud,
                cmd.getSplitMode(), cmd.getImportVmTaskId(), cmd);
    }

    public static AblestackVmMigrationRequest forAblestackN2K(ImportUnmanagedInstanceForAblestackN2KCmd cmd) {
        return new AblestackVmMigrationRequest(ImportVmTask.MigrationTool.AblestackN2K,
                ImportVmTask.SourceProvider.Nutanix, ImportVmTask.TargetProvider.Cloud,
                cmd.getSplitMode(), cmd.getImportVmTaskId(), cmd);
    }

    public ImportVmTask.MigrationTool getMigrationTool() {
        return migrationTool;
    }

    public ImportVmTask.SourceProvider getSourceProvider() {
        return sourceProvider;
    }

    public ImportVmTask.TargetProvider getTargetProvider() {
        return targetProvider;
    }

    public String getSplitMode() {
        return splitMode;
    }

    public String getContinuationTaskId() {
        return continuationTaskId;
    }

    public ImportVmCmd getImportCommand() {
        return importCommand;
    }

    public <T extends ImportVmCmd> T getImportCommand(Class<T> commandType) {
        if (commandType.isInstance(importCommand)) {
            return commandType.cast(importCommand);
        }
        throw new CloudRuntimeException(String.format("Import command %s is not compatible with %s",
                importCommand != null ? importCommand.getClass().getSimpleName() : "null", commandType.getSimpleName()));
    }
}
