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

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

public class LibvirtFtctlWrapperHelperTest {

    @Test
    public void failedProcessReturnsDrainedOutputWithoutReadingClosedStream() {
        LibvirtFtctlWrapperHelper.FtctlProcessOutputParser parser =
                new LibvirtFtctlWrapperHelper.FtctlProcessOutputParser();

        parser.interpret(new BufferedReader(new StringReader("{\"result\":\"error\",\"error_code\":\"DR_TEST\"}\n")));

        Assert.assertEquals("{\"result\":\"error\",\"error_code\":\"DR_TEST\"}\n",
                parser.processError(new BufferedReader(new StringReader("unused"))));
    }

    @Test
    public void successfulProcessPreservesLargeJsonOutput() {
        String payload = "{\"result\":\"ok\",\"details\":\"" + "x".repeat(128 * 1024) + "\"}";
        LibvirtFtctlWrapperHelper.FtctlProcessOutputParser parser =
                new LibvirtFtctlWrapperHelper.FtctlProcessOutputParser();

        parser.interpret(new BufferedReader(new StringReader(payload)));

        Assert.assertEquals(payload + "\n", parser.getLines());
    }
}
