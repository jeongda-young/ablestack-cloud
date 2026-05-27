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

import com.cloud.vm.ImportVMTaskEventVO;
import com.cloud.vm.ImportVMTaskVO;
import com.cloud.vm.dao.ImportVMTaskDao;
import com.cloud.vm.dao.ImportVMTaskEventDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ImportVmTasksManagerImplAblestackTest {

    private ImportVmTasksManagerImpl manager;

    @Mock
    private ImportVMTaskDao importVMTaskDao;
    @Mock
    private ImportVMTaskEventDao importVMTaskEventDao;

    @Before
    public void setUp() {
        manager = new ImportVmTasksManagerImpl();
        ReflectionTestUtils.setField(manager, "importVMTaskDao", importVMTaskDao);
        ReflectionTestUtils.setField(manager, "importVMTaskEventDao", importVMTaskEventDao);
    }

    @Test
    public void updateRuntimeStatusPersistsNormalizedFieldsRawStatusAndEventTimeline() {
        ImportVMTaskVO task = new ImportVMTaskVO();
        task.setId(42L);
        task.setCurrentPhase(ImportVmTask.MigrationPhase.Phase1.getValue());
        task.setMigrationState(ImportVmTask.MigrationState.Running.getValue());
        ImportVmTaskStatus status = new ImportVmTaskStatus("phase1", "completed", "phase2-ready",
                "/var/lib/ablestack-n2k/vm/run", "100%");

        manager.updateImportVMTaskRuntimeStatus(task, status, "{\"raw\":true}", "Phase1 completed");

        Assert.assertEquals("phase1", task.getCurrentPhase());
        Assert.assertEquals("completed", task.getMigrationState());
        Assert.assertEquals("phase2-ready", task.getMigrationStep());
        Assert.assertEquals("/var/lib/ablestack-n2k/vm/run", task.getWorkdir());
        Assert.assertEquals("{\"raw\":true}", task.getStatusJson());
        Assert.assertEquals("Phase1 completed", task.getDescription());
        Mockito.verify(importVMTaskDao).update(Mockito.eq(42L), Mockito.same(task));

        ArgumentCaptor<ImportVMTaskEventVO> eventCaptor = ArgumentCaptor.forClass(ImportVMTaskEventVO.class);
        Mockito.verify(importVMTaskEventDao).persist(eventCaptor.capture());
        ImportVMTaskEventVO event = eventCaptor.getValue();
        Assert.assertEquals(42L, event.getTaskId());
        Assert.assertEquals("status", event.getEventType());
        Assert.assertEquals("phase1", event.getPhase());
        Assert.assertEquals("completed", event.getState());
        Assert.assertEquals("phase2-ready", event.getStep());
        Assert.assertEquals("Phase1 completed", event.getMessage());
        Assert.assertNull(event.getPayloadJson());
    }

    @Test
    public void sanitizeEventPayloadMasksSensitiveValuesAndKeepsOperationalContext() {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("provider", "nutanix");
        payload.put("password", "super-secret");
        payload.put("apiSecretKey", "api-secret");
        payload.put("usernamehint", "admin");

        @SuppressWarnings("unchecked")
        Map<String, String> sanitized = ReflectionTestUtils.invokeMethod(manager, "sanitizeEventPayload", payload);

        Assert.assertEquals("nutanix", sanitized.get("provider"));
        Assert.assertEquals("******", sanitized.get("password"));
        Assert.assertEquals("******", sanitized.get("apiSecretKey"));
        Assert.assertEquals("admin", sanitized.get("usernamehint"));
    }
}
