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
package com.cloud.hypervisor.kvm.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.cloud.hypervisor.kvm.resource.BoundedQgaGuestExec.GuestExecFailure;
import com.cloud.hypervisor.kvm.resource.BoundedQgaGuestExec.Operation;

public class BoundedQgaGuestExecTest {
    @Test
    public void testLaunchFailureClassifiesMissingHelper() throws Exception {
        BoundedQgaGuestExec executor = new BoundedQgaGuestExec();
        try {
            executor.execute((command, timeout) -> {
                throw new Exception("Failed to execute child process: No such file or directory");
            }, Operation.ABLESTACK_NETWORK_SNAPSHOT, 1, 65536);
            fail("Expected missing helper failure");
        } catch (GuestExecFailure e) {
            assertEquals("HELPER_NOT_INSTALLED", e.getErrorCode());
        }
    }

    @Test
    public void testLaunchFailureClassifiesPermissionDenied() throws Exception {
        BoundedQgaGuestExec executor = new BoundedQgaGuestExec();
        try {
            executor.execute((command, timeout) -> {
                throw new Exception("Failed to execute child process: Permission denied");
            }, Operation.ABLESTACK_NETWORK_SNAPSHOT, 1, 65536);
            fail("Expected permission failure");
        } catch (GuestExecFailure e) {
            assertEquals("EXEC_PERMISSION_DENIED", e.getErrorCode());
        }
    }
}
