// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0.
package com.cloud.dr;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DrFtctlActionCapabilitySnapshot {
    private final Map<String, String> blockingReasons;
    private final Map<String, Map<String, String>> reasonArgs;

    DrFtctlActionCapabilitySnapshot(Map<String, String> blockingReasons,
            Map<String, Map<String, String>> reasonArgs) {
        this.blockingReasons = blockingReasons == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, String>(blockingReasons));
        this.reasonArgs = reasonArgs == null ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Map<String, String>>(reasonArgs));
    }

    public String getBlockingReason(String action) {
        return blockingReasons.get(action);
    }

    public Map<String, String> getReasonArgs(String action) {
        Map<String, String> args = reasonArgs.get(action);
        return args == null ? Collections.emptyMap() : args;
    }
}
