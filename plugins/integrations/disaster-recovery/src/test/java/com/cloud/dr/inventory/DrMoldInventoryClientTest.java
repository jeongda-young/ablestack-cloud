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
package com.cloud.dr.inventory;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DrMoldInventoryClientTest {
    private final DrMoldInventoryClient client = new DrMoldInventoryClient();

    @Test
    public void extractsNestedFtctlBrokerPayloadFromCloudApiEnvelope() {
        JsonObject response = JsonParser.parseString("{\"executeftctldrsiteagentcommandresponse\":{"
                + "\"ftctldrsiteagentcommand\":{\"answerclass\":\"StatusAnswer\",\"answerjson\":\"{}\"}}}")
                .getAsJsonObject();

        JsonObject payload = client.extractSiteAgentCommandResponse(response);

        Assert.assertEquals("StatusAnswer", payload.get("answerclass").getAsString());
        Assert.assertEquals("{}", payload.get("answerjson").getAsString());
    }

    @Test
    public void preservesFlatFtctlBrokerPayloadForCompatibility() {
        JsonObject response = JsonParser.parseString("{\"executeftctldrsiteagentcommandresponse\":{"
                + "\"answerclass\":\"StatusAnswer\",\"answerjson\":\"{}\"}}")
                .getAsJsonObject();

        JsonObject payload = client.extractSiteAgentCommandResponse(response);

        Assert.assertEquals("StatusAnswer", payload.get("answerclass").getAsString());
        Assert.assertEquals("{}", payload.get("answerjson").getAsString());
    }

    @Test
    public void interpretsUefiDetailKeyAsBootTypeAndItsValueAsBootMode() {
        JsonObject vm = JsonParser.parseString("{\"id\":\"vm-1\",\"details\":{\"UEFI\":\"LEGACY\"}}")
                .getAsJsonObject();

        Map<String, String> hardware = client.extractVirtualMachineHardware(vm);

        Assert.assertEquals("UEFI", hardware.get("firmware"));
        Assert.assertEquals("UEFI", hardware.get("bootType"));
        Assert.assertEquals("LEGACY", hardware.get("bootMode"));
        Assert.assertEquals("false", hardware.get("secureBoot"));
        Assert.assertEquals("LEGACY", hardware.get("vmDetail.UEFI"));
    }

    @Test
    public void interpretsSecureUefiDetailWithoutChangingVmwareInventoryPath() {
        JsonObject vm = JsonParser.parseString("{\"boottype\":\"BIOS\",\"details\":{\"UEFI\":\"SECURE\"}}")
                .getAsJsonObject();

        Map<String, String> hardware = client.extractVirtualMachineHardware(vm);

        Assert.assertEquals("UEFI", hardware.get("bootType"));
        Assert.assertEquals("SECURE", hardware.get("bootMode"));
        Assert.assertEquals("true", hardware.get("secureBoot"));
    }

    @Test
    public void defaultsKvmWithoutUefiDetailToLegacyBios() {
        JsonObject vm = JsonParser.parseString("{\"hypervisor\":\"KVM\",\"details\":{}}")
                .getAsJsonObject();

        Map<String, String> hardware = client.extractVirtualMachineHardware(vm);

        Assert.assertEquals("BIOS", hardware.get("firmware"));
        Assert.assertEquals("BIOS", hardware.get("bootType"));
        Assert.assertEquals("LEGACY", hardware.get("bootMode"));
        Assert.assertEquals("false", hardware.get("secureBoot"));
    }

    @Test
    public void preservesAllSourceVmDetailValuesForKvmReplicationSnapshot() {
        JsonObject vm = JsonParser.parseString("{\"hypervisor\":\"KVM\",\"details\":{"
                + "\"UEFI\":\"LEGACY\",\"tpmversion\":\"NONE\",\"io.policy\":\"io_uring\","
                + "\"iothreads\":\"true\",\"clone.fast.status\":\"running\"}}")
                .getAsJsonObject();

        Map<String, String> hardware = client.extractVirtualMachineHardware(vm);

        Assert.assertEquals("LEGACY", hardware.get("vmDetail.UEFI"));
        Assert.assertEquals("NONE", hardware.get("vmDetail.tpmversion"));
        Assert.assertEquals("io_uring", hardware.get("vmDetail.io.policy"));
        Assert.assertEquals("true", hardware.get("vmDetail.iothreads"));
        Assert.assertEquals("running", hardware.get("vmDetail.clone.fast.status"));
    }

    @Test
    public void resolvesSharedMountPointVolumeAsQcow2() {
        JsonObject volume = JsonParser.parseString("{\"path\":\"volume-uuid\"}").getAsJsonObject();

        Assert.assertEquals("qcow2", client.resolveVolumeFormat(volume, "SharedMountPoint", "volume-uuid"));
    }

    @Test
    public void preservesExplicitVolumeFormatAndRbdRawContract() {
        JsonObject explicit = JsonParser.parseString("{\"format\":\"QCOW2\"}").getAsJsonObject();

        Assert.assertEquals("qcow2", client.resolveVolumeFormat(explicit, "SharedMountPoint", "volume-uuid"));
        Assert.assertEquals("raw", client.resolveVolumeFormat(new JsonObject(), "RBD", "pool/image"));
    }

    @Test
    public void extractsExactAsyncVmPowerFailure() {
        JsonObject payload = JsonParser.parseString("{\"jobstatus\":2,\"jobresultcode\":530,"
                + "\"jobresult\":{\"errorcode\":530,\"errortext\":"
                + "\"Unable to stop VM while SharedMountPoint clone flatten is running.\"}}")
                .getAsJsonObject();

        Assert.assertEquals("Unable to stop VM while SharedMountPoint clone flatten is running.",
                client.asyncJobError(payload));
    }
}
