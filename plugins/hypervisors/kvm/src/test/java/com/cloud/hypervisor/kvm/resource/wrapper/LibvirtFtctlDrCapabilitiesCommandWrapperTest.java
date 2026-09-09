// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.JsonParser;

public class LibvirtFtctlDrCapabilitiesCommandWrapperTest {

    @Test
    public void parsesReprotectAuthorityContractsFromRuntimeCapabilities() {
        Assert.assertEquals(Arrays.asList("2026-07-23", "2026-08-26"),
                LibvirtFtctlDrCapabilitiesCommandWrapper.reprotectAuthorityContractVersions(
                        JsonParser.parseString("{\"reprotect_authority_contract_versions\":"
                                + "[\"2026-07-23\",\"2026-08-26\"]}").getAsJsonObject()));
    }

    @Test
    public void missingReprotectAuthorityContractsRemainEmpty() {
        Assert.assertTrue(LibvirtFtctlDrCapabilitiesCommandWrapper.reprotectAuthorityContractVersions(
                JsonParser.parseString("{}").getAsJsonObject()).isEmpty());
    }
}
