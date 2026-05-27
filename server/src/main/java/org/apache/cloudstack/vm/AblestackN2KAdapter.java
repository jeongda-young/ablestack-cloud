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

import org.apache.cloudstack.api.command.admin.vm.ImportUnmanagedInstanceForAblestackN2KCmd;
import org.apache.cloudstack.api.response.UserVmResponse;

public class AblestackN2KAdapter implements MigrationToolAdapter {

    private final UnmanagedVMsManagerImpl unmanagedVMsManager;

    public AblestackN2KAdapter(UnmanagedVMsManagerImpl unmanagedVMsManager) {
        this.unmanagedVMsManager = unmanagedVMsManager;
    }

    @Override
    public ImportVmTask.MigrationTool getMigrationTool() {
        return ImportVmTask.MigrationTool.AblestackN2K;
    }

    @Override
    public boolean supports(AblestackVmMigrationRequest request) {
        return ImportVmTask.SourceProvider.Nutanix.equals(request.getSourceProvider())
                && ImportVmTask.TargetProvider.Cloud.equals(request.getTargetProvider())
                && request.getImportCommand() instanceof ImportUnmanagedInstanceForAblestackN2KCmd;
    }

    @Override
    public UserVmResponse execute(AblestackVmMigrationRequest request) {
        ImportUnmanagedInstanceForAblestackN2KCmd cmd = request.getImportCommand(ImportUnmanagedInstanceForAblestackN2KCmd.class);
        unmanagedVMsManager.startAblestackN2KVmImport(cmd);
        return new UserVmResponse();
    }
}
