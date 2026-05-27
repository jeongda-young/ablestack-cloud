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
import org.apache.cloudstack.api.command.admin.vm.ImportVmCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.util.Collections;

public class AblestackVmMigrationManagerImplTest {

    @Test
    public void importVmValidatesSourceAndTargetBeforeExecutingSupportedTool() {
        MigrationToolAdapter toolAdapter = Mockito.mock(MigrationToolAdapter.class);
        MigrationSourceAdapter sourceAdapter = Mockito.mock(MigrationSourceAdapter.class);
        MigrationTargetAdapter targetAdapter = Mockito.mock(MigrationTargetAdapter.class);
        ImportVmCmd cmd = Mockito.mock(ImportVmCmd.class);
        UserVmResponse expectedResponse = new UserVmResponse();
        AblestackVmMigrationRequest request = new AblestackVmMigrationRequest(
                ImportVmTask.MigrationTool.AblestackN2K,
                ImportVmTask.SourceProvider.Nutanix,
                ImportVmTask.TargetProvider.Cloud,
                "phase1",
                null,
                cmd);

        Mockito.when(toolAdapter.getMigrationTool()).thenReturn(ImportVmTask.MigrationTool.AblestackN2K);
        Mockito.when(toolAdapter.supports(request)).thenReturn(true);
        Mockito.when(toolAdapter.execute(request)).thenReturn(expectedResponse);
        Mockito.when(sourceAdapter.getSourceProvider()).thenReturn(ImportVmTask.SourceProvider.Nutanix);
        Mockito.when(targetAdapter.getTargetProvider()).thenReturn(ImportVmTask.TargetProvider.Cloud);

        AblestackVmMigrationManagerImpl manager = new AblestackVmMigrationManagerImpl(
                Collections.singletonList(toolAdapter),
                Collections.singletonList(sourceAdapter),
                Collections.singletonList(targetAdapter));

        UserVmResponse actualResponse = manager.importVm(request);

        Assert.assertSame(expectedResponse, actualResponse);
        InOrder inOrder = Mockito.inOrder(sourceAdapter, targetAdapter, toolAdapter);
        inOrder.verify(sourceAdapter).validate(request);
        inOrder.verify(targetAdapter).validate(request);
        inOrder.verify(toolAdapter).supports(request);
        inOrder.verify(toolAdapter).execute(request);
    }

    @Test
    public void importVmRejectsUnsupportedToolCombination() {
        MigrationToolAdapter toolAdapter = Mockito.mock(MigrationToolAdapter.class);
        ImportVmCmd cmd = Mockito.mock(ImportVmCmd.class);
        AblestackVmMigrationRequest request = new AblestackVmMigrationRequest(
                ImportVmTask.MigrationTool.AblestackN2K,
                ImportVmTask.SourceProvider.Nutanix,
                ImportVmTask.TargetProvider.Cloud,
                "phase1",
                null,
                cmd);

        Mockito.when(toolAdapter.getMigrationTool()).thenReturn(ImportVmTask.MigrationTool.AblestackN2K);
        Mockito.when(toolAdapter.supports(request)).thenReturn(false);

        AblestackVmMigrationManagerImpl manager = new AblestackVmMigrationManagerImpl(Collections.singletonList(toolAdapter));

        Assert.assertThrows(InvalidParameterValueException.class, () -> manager.importVm(request));
        Mockito.verify(toolAdapter, Mockito.never()).execute(Mockito.any());
    }

    @Test
    public void importVmRejectsRequestWithoutRequiredProviders() {
        AblestackVmMigrationManagerImpl manager = new AblestackVmMigrationManagerImpl(Collections.emptyList());
        AblestackVmMigrationRequest request = new AblestackVmMigrationRequest(
                ImportVmTask.MigrationTool.AblestackN2K,
                null,
                ImportVmTask.TargetProvider.Cloud,
                "phase1",
                null,
                Mockito.mock(ImportVmCmd.class));

        Assert.assertThrows(InvalidParameterValueException.class, () -> manager.importVm(request));
    }
}
