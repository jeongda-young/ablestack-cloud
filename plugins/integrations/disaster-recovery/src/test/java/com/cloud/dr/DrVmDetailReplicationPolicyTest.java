// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information.
package com.cloud.dr;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class DrVmDetailReplicationPolicyTest {
    @Test
    public void kvmToKvmCopiesSourceVmDetailsAndRejectsOnlyTargetBoundState() {
        Map<String, String> source = new LinkedHashMap<String, String>();
        source.put("UEFI", "LEGACY");
        source.put("tpmversion", "NONE");
        source.put("io.policy", "io_uring");
        source.put("iothreads", "true");
        source.put("cpuNumber", "4");
        source.put("cpuSpeed", "2200");
        source.put("memory", "8192");
        source.put("clone.fast.status", "running");
        source.put("ftctl.enabled", "true");
        source.put("dr.plan.id", "99");
        source.put("volumeId", "77");
        source.put("deployvm", "true");
        source.put("boot.mode", "LEGACY");

        Map<String, String> copied = DrVmDetailReplicationPolicy.copyableSourceDetails(
                DrConstants.DIRECTION_KVM_TO_KVM, source);

        Assert.assertEquals("LEGACY", copied.get("UEFI"));
        Assert.assertEquals("NONE", copied.get("tpmversion"));
        Assert.assertEquals("io_uring", copied.get("io.policy"));
        Assert.assertEquals("true", copied.get("iothreads"));
        Assert.assertFalse(copied.containsKey("cpuNumber"));
        Assert.assertFalse(copied.containsKey("cpuSpeed"));
        Assert.assertFalse(copied.containsKey("memory"));
        Assert.assertFalse(copied.containsKey("clone.fast.status"));
        Assert.assertFalse(copied.containsKey("ftctl.enabled"));
        Assert.assertFalse(copied.containsKey("dr.plan.id"));
        Assert.assertFalse(copied.containsKey("volumeId"));
        Assert.assertFalse(copied.containsKey("deployvm"));
        Assert.assertFalse(copied.containsKey("boot.mode"));
    }

    @Test
    public void vmwarePathDoesNotAdoptKvmVmDetailsContract() {
        Map<String, String> source = new LinkedHashMap<String, String>();
        source.put("UEFI", "SECURE");

        Assert.assertTrue(DrVmDetailReplicationPolicy.copyableSourceDetails(
                DrConstants.DIRECTION_VMWARE_TO_KVM, source).isEmpty());
    }
}
