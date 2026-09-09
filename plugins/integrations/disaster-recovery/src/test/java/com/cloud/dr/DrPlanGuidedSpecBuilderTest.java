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

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DrPlanGuidedSpecBuilderTest {

    @Test
    public void preservesSharedMountPointSourceTypeAndFormat() {
        DrPlanVO plan = new DrPlanVO("shared-qcow2", 1L, 2L, DrConstants.DIRECTION_KVM_TO_KVM);
        DrPlanGuidedSpec spec = new DrPlanGuidedSpec();
        spec.setDiskMappingsJson("[{\"sourcePath\":\"/mnt/glue-gfs/source-volume\","
                + "\"sourceType\":\"file\",\"sourceFormat\":\"qcow2\","
                + "\"source\":{\"path\":\"/mnt/glue-gfs/source-volume\",\"format\":\"qcow2\"},"
                + "\"target\":{\"name\":\"target-volume\",\"type\":\"file\",\"format\":\"qcow2\"}}]");

        JsonObject disk = JsonParser.parseString(new DrPlanGuidedSpecBuilder().build(plan, spec).getMappingJson())
                .getAsJsonObject().getAsJsonArray("disks").get(0).getAsJsonObject();

        Assert.assertEquals("file", disk.get("sourceType").getAsString());
        Assert.assertEquals("qcow2", disk.get("sourceFormat").getAsString());
        Assert.assertEquals("file", disk.getAsJsonObject("source").get("type").getAsString());
        Assert.assertEquals("qcow2", disk.getAsJsonObject("source").get("format").getAsString());
    }
}
