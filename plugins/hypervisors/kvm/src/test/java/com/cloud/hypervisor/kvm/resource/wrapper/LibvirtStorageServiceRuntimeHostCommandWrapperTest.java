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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import org.junit.Assert;
import org.junit.Test;

import com.cloud.agent.api.StorageServiceRuntimeFileType;
import com.cloud.agent.api.StorageServiceRuntimeHostCommand;
import com.cloud.agent.api.StorageServiceRuntimeOperation;
import com.google.gson.JsonObject;

public class LibvirtStorageServiceRuntimeHostCommandWrapperTest {
    private final LibvirtStorageServiceRuntimeHostCommandWrapper wrapper = new LibvirtStorageServiceRuntimeHostCommandWrapper();

    @Test
    public void testRuntimeFilePathsAreFixedByType() throws Exception {
        final byte[] value = "chunk".getBytes(StandardCharsets.UTF_8);
        final StorageServiceRuntimeHostCommand bundle = chunkCommand(StorageServiceRuntimeFileType.BUNDLE, "tx-1", null, value);
        final StorageServiceRuntimeHostCommand updater = chunkCommand(StorageServiceRuntimeFileType.UPDATER_ENTRY, "tx-1", null, value);
        final StorageServiceRuntimeHostCommand key = chunkCommand(StorageServiceRuntimeFileType.TRUSTED_KEY, "tx-1", "release-key", value);

        Assert.assertEquals("/var/lib/ablestack-storage/runtime-updates/tx-1/bundle.tar.gz", wrapper.resolveGuestFile(bundle));
        Assert.assertEquals("/usr/local/bin/ablestack-storage-runtime-updater", wrapper.resolveGuestFile(updater));
        Assert.assertEquals("/opt/ablestack/storage-runtime/trusted-keys/release-key.pem", wrapper.resolveGuestFile(key));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRuntimeFilePathRejectsTraversalIdentifier() throws Exception {
        final byte[] value = "chunk".getBytes(StandardCharsets.UTF_8);
        wrapper.resolveGuestFile(chunkCommand(StorageServiceRuntimeFileType.BUNDLE, "../escape", null, value));
    }

    @Test
    public void testChunkHashAndLengthAreVerified() throws Exception {
        final byte[] value = "chunk-data".getBytes(StandardCharsets.UTF_8);
        final StorageServiceRuntimeHostCommand command = chunkCommand(StorageServiceRuntimeFileType.MANIFEST, "tx-1", null, value);
        Assert.assertArrayEquals(value, wrapper.validateChunk(command));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testChunkHashMismatchIsRejected() throws Exception {
        final byte[] value = "chunk-data".getBytes(StandardCharsets.UTF_8);
        final StorageServiceRuntimeHostCommand command = new StorageServiceRuntimeHostCommand(
                "sharedfs-test", "tx-1", StorageServiceRuntimeFileType.MANIFEST, null, 0,
                value.length, repeat("00", 32), Base64.getEncoder().encodeToString(value), true, true, 60);
        wrapper.validateChunk(command);
    }

    @Test
    public void testUpdaterGuestExecDoesNotUseShell() {
        final StorageServiceRuntimeHostCommand command = new StorageServiceRuntimeHostCommand(
                "sharedfs-test", StorageServiceRuntimeOperation.PREFLIGHT, "tx-1", "{}", 60);
        final JsonObject qga = wrapper.buildUpdaterGuestExecCommand(command, "/tmp/request.json");
        final JsonObject arguments = qga.getAsJsonObject("arguments");
        Assert.assertEquals("/usr/local/bin/ablestack-storage-runtime-updater", arguments.get("path").getAsString());
        Assert.assertFalse(qga.toString().contains("/bin/bash"));
        Assert.assertTrue(qga.toString().contains("preflight"));
        Assert.assertTrue(qga.toString().contains("--request"));
    }

    @Test
    public void testQgaWriteCommandCarriesOffsetSafePayload() throws Exception {
        final JsonObject seek = wrapper.buildGuestFileSeekCommand(7L, 49152L);
        final JsonObject write = wrapper.buildGuestFileWriteCommand(7L, "YWJj");
        Assert.assertEquals(49152L, seek.getAsJsonObject("arguments").get("offset").getAsLong());
        Assert.assertEquals(0, seek.getAsJsonObject("arguments").get("whence").getAsInt());
        Assert.assertEquals("YWJj", write.getAsJsonObject("arguments").get("buf-b64").getAsString());
    }

    private StorageServiceRuntimeHostCommand chunkCommand(final StorageServiceRuntimeFileType type,
            final String transactionId, final String keyId, final byte[] value) throws Exception {
        final String hash = hex(MessageDigest.getInstance("SHA-256").digest(value));
        return new StorageServiceRuntimeHostCommand(
                "sharedfs-test", transactionId, type, keyId, 0, value.length, hash,
                Base64.getEncoder().encodeToString(value), true, true, 60);
    }

    private String hex(final byte[] value) {
        final StringBuilder result = new StringBuilder(value.length * 2);
        for (final byte item : value) {
            result.append(String.format("%02x", item & 0xff));
        }
        return result.toString();
    }

    private String repeat(final String value, final int count) {
        final StringBuilder result = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) {
            result.append(value);
        }
        return result.toString();
    }
}
