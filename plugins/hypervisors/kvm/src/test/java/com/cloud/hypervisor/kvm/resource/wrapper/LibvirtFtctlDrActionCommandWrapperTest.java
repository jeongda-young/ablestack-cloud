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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.cloud.agent.api.FtctlDrActionCommand;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LibvirtFtctlDrActionCommandWrapperTest {

    private static final String REPROTECT_AUTHORITY_SPEC = "{"
            + "\"contractVersion\":\"" + FtctlDrActionCommand.REPROTECT_AUTHORITY_CONTRACT_VERSION + "\","
            + "\"expectedActiveSide\":\"TARGET\","
            + "\"authorityGeneration\":344,"
            + "\"checkpointSequence\":344,"
            + "\"targetVmId\":157,"
            + "\"cutoverSessionId\":\"cutover-a\"}";

    @Test
    public void reprotectAuthorityValidationUsesSharedCommandContractVersion() throws IOException {
        FtctlDrActionCommand command = new FtctlDrActionCommand(
                FtctlDrActionCommand.Action.REPROTECT, "plan-a", "run-a");
        command.setAuthorityContractVersion(FtctlDrActionCommand.REPROTECT_AUTHORITY_CONTRACT_VERSION);
        command.setAuthoritySpecJson(REPROTECT_AUTHORITY_SPEC);

        LibvirtFtctlDrActionCommandWrapper.validateAuthoritySpec(command);
    }

    @Test
    public void reprotectAuthorityValidationAcceptsMatchingLegacyContractVersion() throws IOException {
        FtctlDrActionCommand command = new FtctlDrActionCommand(
                FtctlDrActionCommand.Action.REPROTECT, "plan-a", "run-a");
        command.setAuthorityContractVersion("2026-07-23");
        command.setAuthoritySpecJson(REPROTECT_AUTHORITY_SPEC.replace(
                FtctlDrActionCommand.REPROTECT_AUTHORITY_CONTRACT_VERSION, "2026-07-23"));

        LibvirtFtctlDrActionCommandWrapper.validateAuthoritySpec(command);
    }

    @Test
    public void reprotectAuthorityValidationRejectsCommandSpecVersionMismatch() {
        FtctlDrActionCommand command = new FtctlDrActionCommand(
                FtctlDrActionCommand.Action.REPROTECT, "plan-a", "run-a");
        command.setAuthorityContractVersion("2026-07-23");
        command.setAuthoritySpecJson(REPROTECT_AUTHORITY_SPEC);

        try {
            LibvirtFtctlDrActionCommandWrapper.validateAuthoritySpec(command);
            Assert.fail("Mismatched reprotect authority contracts must be rejected");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("must match"));
        }
    }

    @Test
    public void statusQuerySuccessDoesNotAcceptFailedRun() {
        JsonObject payload = new JsonParser().parse("{\"result\":\"ok\",\"accepted\":false,"
                + "\"state\":\"ERROR\",\"error_code\":\"DR_RECOVERY_FAILED\"}").getAsJsonObject();

        Assert.assertTrue(LibvirtFtctlDrActionCommandWrapper.isSemanticFailureStatus(payload));
        Assert.assertFalse(LibvirtFtctlDrActionCommandWrapper.isAcceptedStatus(0, payload));
    }

    @Test
    public void statusQueryAcceptsHealthyRunningRun() {
        JsonObject payload = new JsonParser().parse("{\"result\":\"ok\",\"accepted\":true,"
                + "\"state\":\"SYNCING\",\"error_code\":\"\"}").getAsJsonObject();

        Assert.assertFalse(LibvirtFtctlDrActionCommandWrapper.isSemanticFailureStatus(payload));
        Assert.assertTrue(LibvirtFtctlDrActionCommandWrapper.isAcceptedStatus(0, payload));
    }

    @Test
    public void acceptedJsonDoesNotTreatTimeoutPolicyFieldsAsTransportFailure() {
        String output = "{\"result\":\"accepted\",\"accepted\":true,\"state\":\"TESTING\","
                + "\"testBootTimeoutSeconds\":180,\"boot_timeout_seconds\":180,\"error_code\":\"\"}";
        JsonObject payload = new JsonParser().parse(output).getAsJsonObject();

        Assert.assertFalse(LibvirtFtctlDrActionCommandWrapper.shouldProbeStatus(null, output, payload, 0));
    }

    @Test
    public void unstructuredTransportTimeoutStillRequiresStatusProbe() {
        Assert.assertTrue(LibvirtFtctlDrActionCommandWrapper.shouldProbeStatus(
                "Command timed out", "", null, 124));
    }

    @Test
    public void structuredEngineFailureIsNotReclassifiedAsTransportTimeout() {
        String output = "{\"result\":\"error\",\"accepted\":false,\"state\":\"FAILED\","
                + "\"error_code\":\"DR_TEST_VM_BOOT_TIMEOUT\"}";
        JsonObject payload = new JsonParser().parse(output).getAsJsonObject();

        Assert.assertFalse(LibvirtFtctlDrActionCommandWrapper.shouldProbeStatus(
                "Command failed", output, payload, 20));
    }
}
