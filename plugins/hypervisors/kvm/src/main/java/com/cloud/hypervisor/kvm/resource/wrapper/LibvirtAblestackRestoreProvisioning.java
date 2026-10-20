// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;

/** Per-worker destination policies, including aliases for temporary replacement files. */
final class LibvirtAblestackRestoreProvisioning implements AutoCloseable {
    private static final ThreadLocal<LibvirtAblestackRestoreProvisioning> CURRENT = new ThreadLocal<>();
    private final LibvirtAblestackRestoreProvisioning previous;
    private final Map<String, Storage.ProvisioningType> policies;

    private LibvirtAblestackRestoreProvisioning(Map<String, Storage.ProvisioningType> types) {
        policies = types == null ? null : new HashMap<>();
        if (types != null) { types.forEach((path, type) -> policies.put(key(path), type)); }
        previous = CURRENT.get();
        CURRENT.set(this);
    }

    static LibvirtAblestackRestoreProvisioning begin(Map<String, Storage.ProvisioningType> policies) {
        return new LibvirtAblestackRestoreProvisioning(policies);
    }

    static Storage.ProvisioningType type(String destination) {
        LibvirtAblestackRestoreProvisioning scope = CURRENT.get();
        // Commands saved by older agents did not record this policy; keep their previous conversion behavior.
        if (scope == null || scope.policies == null) { return null; }
        Storage.ProvisioningType type = scope.policies.get(key(destination));
        if (type == null) { throw new CloudRuntimeException("Restore destination provisioning policy is missing: " + destination); }
        return type;
    }

    static LibvirtAblestackRestoreProvisioning temporary(String destination, String temporary) {
        Storage.ProvisioningType type = type(destination);
        LibvirtAblestackRestoreProvisioning scope = CURRENT.get();
        Map<String, Storage.ProvisioningType> aliases = scope == null || scope.policies == null ? null : new HashMap<>(scope.policies);
        if (aliases != null) { aliases.put(key(temporary), type); }
        return begin(aliases);
    }

    private static String key(String path) { return Path.of(path).toAbsolutePath().normalize().toString(); }

    @Override
    public void close() {
        if (previous == null) { CURRENT.remove(); }
        else { CURRENT.set(previous); }
    }
}
