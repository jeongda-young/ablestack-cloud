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

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.api.response.UserVmResponse;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AblestackVmMigrationManagerImpl implements AblestackVmMigrationManager {

    private final Map<ImportVmTask.MigrationTool, MigrationToolAdapter> toolAdapters;
    private final Map<ImportVmTask.SourceProvider, MigrationSourceAdapter> sourceAdapters;
    private final Map<ImportVmTask.TargetProvider, MigrationTargetAdapter> targetAdapters;

    public AblestackVmMigrationManagerImpl(List<MigrationToolAdapter> toolAdapters) {
        this(toolAdapters, Collections.emptyList(), Collections.emptyList());
    }

    public AblestackVmMigrationManagerImpl(List<MigrationToolAdapter> toolAdapters,
                                           List<MigrationSourceAdapter> sourceAdapters,
                                           List<MigrationTargetAdapter> targetAdapters) {
        this.toolAdapters = indexToolAdapters(toolAdapters);
        this.sourceAdapters = indexSourceAdapters(sourceAdapters);
        this.targetAdapters = indexTargetAdapters(targetAdapters);
    }

    @Override
    public UserVmResponse importVm(AblestackVmMigrationRequest request) {
        validateRequest(request);
        MigrationSourceAdapter sourceAdapter = sourceAdapters.get(request.getSourceProvider());
        if (sourceAdapter != null) {
            sourceAdapter.validate(request);
        }
        MigrationTargetAdapter targetAdapter = targetAdapters.get(request.getTargetProvider());
        if (targetAdapter != null) {
            targetAdapter.validate(request);
        }

        MigrationToolAdapter toolAdapter = toolAdapters.get(request.getMigrationTool());
        if (toolAdapter == null || !toolAdapter.supports(request)) {
            throw new InvalidParameterValueException(String.format("Migration tool %s does not support %s to %s import",
                    request.getMigrationTool(), request.getSourceProvider(), request.getTargetProvider()));
        }
        return toolAdapter.execute(request);
    }

    private void validateRequest(AblestackVmMigrationRequest request) {
        if (request == null) {
            throw new InvalidParameterValueException("Migration request cannot be null");
        }
        if (request.getMigrationTool() == null) {
            throw new InvalidParameterValueException("Migration tool is required");
        }
        if (request.getSourceProvider() == null) {
            throw new InvalidParameterValueException("Source provider is required");
        }
        if (request.getTargetProvider() == null) {
            throw new InvalidParameterValueException("Target provider is required");
        }
    }

    private Map<ImportVmTask.MigrationTool, MigrationToolAdapter> indexToolAdapters(List<MigrationToolAdapter> adapters) {
        Map<ImportVmTask.MigrationTool, MigrationToolAdapter> indexedAdapters = new EnumMap<>(ImportVmTask.MigrationTool.class);
        for (MigrationToolAdapter adapter : adapters) {
            indexedAdapters.put(adapter.getMigrationTool(), adapter);
        }
        return indexedAdapters;
    }

    private Map<ImportVmTask.SourceProvider, MigrationSourceAdapter> indexSourceAdapters(List<MigrationSourceAdapter> adapters) {
        Map<ImportVmTask.SourceProvider, MigrationSourceAdapter> indexedAdapters = new EnumMap<>(ImportVmTask.SourceProvider.class);
        for (MigrationSourceAdapter adapter : adapters) {
            indexedAdapters.put(adapter.getSourceProvider(), adapter);
        }
        return indexedAdapters;
    }

    private Map<ImportVmTask.TargetProvider, MigrationTargetAdapter> indexTargetAdapters(List<MigrationTargetAdapter> adapters) {
        Map<ImportVmTask.TargetProvider, MigrationTargetAdapter> indexedAdapters = new EnumMap<>(ImportVmTask.TargetProvider.class);
        for (MigrationTargetAdapter adapter : adapters) {
            indexedAdapters.put(adapter.getTargetProvider(), adapter);
        }
        return indexedAdapters;
    }
}
