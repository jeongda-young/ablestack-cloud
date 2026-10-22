// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.backup.netbackup;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.apache.cloudstack.backup.ThirdPartyBackupManifest;
import org.apache.cloudstack.backup.ThirdPartyBackupRestore;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;

import com.cloud.utils.exception.CloudRuntimeException;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class AblestackNetBackupClientTest {
    private static final String API_KEY = "synthetic-netbackup-api-key";
    private static final String CONTENT_TYPE = "application/vnd.netbackup+json;version=12.0";
    private static final String BACKUP_UUID = "d723cbdc-4c7e-49c4-abe1-949b09c4196c";

    @Rule
    public WireMockRule server = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Test
    public void sourceGateUsesTheApiKeyWithoutABearerPrefix() throws Exception {
        server.stubFor(get(urlPathEqualTo("/netbackup/admin/jobs/"))
                .withQueryParam("filter", equalTo("policyName eq 'ablecube4'"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(API_KEY))
                .withHeader(HttpHeaders.ACCEPT, equalTo(CONTENT_TYPE))
                .withHeader("X-API-Key", absent())
                .willReturn(aResponse().withStatus(200).withBody("{\"data\":[]}")));

        assertTrue(client().volumeSourceReady("ablecube4"));
        server.verify(1, getRequestedFor(urlPathEqualTo("/netbackup/admin/jobs/")));
    }

    @Test
    public void authenticationFailureDoesNotReleaseTheSourceGate() throws Exception {
        server.stubFor(get(urlPathEqualTo("/netbackup/admin/jobs/"))
                .willReturn(aResponse().withStatus(401)));

        CloudRuntimeException error = assertThrows(CloudRuntimeException.class,
                () -> client().volumeSourceReady("ablecube4"));
        assertTrue(error.getMessage().contains("401"));
    }

    @Test
    public void volumeAndMetadataPoliciesSubmitAndConfirmTheExactArtifactWithRawAuthentication() throws Exception {
        String template = "{\"data\":{\"attributes\":{\"policy\":{\"policyType\":\"Standard\","
                + "\"clients\":[{\"hostName\":\"ablecube4\"}],\"policyAttributes\":{\"active\":true},"
                + "\"schedules\":[{\"scheduleName\":\"full\",\"backupType\":\"Full Backup\"}],"
                + "\"backupSelections\":{\"selections\":[\"/mnt/glue-gfs/ablestack-netbackup/bootstrap/ablecube4\"]}}}}}";
        server.stubFor(get(urlEqualTo("/netbackup/config/policies/ablecube4"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(API_KEY))
                .willReturn(aResponse().withStatus(200).withBody(template)));
        server.stubFor(post(urlEqualTo("/netbackup/config/policies"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(API_KEY))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .willReturn(aResponse().withStatus(201)));
        server.stubFor(post(urlEqualTo("/netbackup/admin/manual-backup"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo(API_KEY))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(CONTENT_TYPE))
                .willReturn(aResponse().withStatus(202)));

        for (boolean metadata : new boolean[]{false, true}) {
            ThirdPartyBackupManifest.Artifact artifact = new ThirdPartyBackupManifest.Artifact();
            artifact.backupUuid = BACKUP_UUID;
            artifact.path = "/mnt/glue-gfs/ablestack-netbackup/i-2-317-VM/2026.10.22.09.55.37.495"
                    + (metadata ? "" : "/root.raw");
            AblestackNetBackupClient client = client();
            client.backupVolumeArtifact("ablecube4", "ablecube4", "192.0.2.4", artifact, metadata);
            String policy = artifact.jobId.substring("policy:".length());
            assertTrue(policy.startsWith("ABLESTACK-" + BACKUP_UUID + "-"));
            if (metadata) { assertEquals("ABLESTACK-" + BACKUP_UUID + "-metadata", policy); }
            assertFalse(artifact.completed);
            server.verify(postRequestedFor(urlEqualTo("/netbackup/config/policies"))
                    .withRequestBody(matchingJsonPath("$.data.attributes.policy.backupSelections.selections[0]", equalTo(artifact.path)))
                    .withRequestBody(matchingJsonPath("$.data.attributes.policy.clients[0].hostName", equalTo("ablecube4"))));
            server.verify(postRequestedFor(urlEqualTo("/netbackup/admin/manual-backup"))
                    .withRequestBody(matchingJsonPath("$.data.attributes.policyName", equalTo(policy)))
                    .withRequestBody(matchingJsonPath("$.data.attributes.scheduleName", equalTo("full"))));

            server.stubFor(get(urlPathEqualTo("/netbackup/admin/jobs/"))
                    .withQueryParam("filter", equalTo("policyName eq '" + policy + "'"))
                    .withHeader(HttpHeaders.AUTHORIZATION, equalTo(API_KEY))
                    .willReturn(aResponse().withStatus(200).withBody("{\"data\":[{\"id\":\"42\"}]}")));
            client.backupVolumeArtifact("ablecube4", "ablecube4", "192.0.2.4", artifact, metadata);
            assertEquals("42", artifact.jobId);

            JSONObject attributes = new JSONObject().put("policyName", policy).put("state", "DONE")
                    .put("status", metadata ? (Object) "0" : 0);
            server.stubFor(get(urlEqualTo("/netbackup/admin/jobs/42"))
                    .withHeader(HttpHeaders.AUTHORIZATION, equalTo(API_KEY))
                    .willReturn(aResponse().withStatus(200).withBody(new JSONObject()
                            .put("data", new JSONObject().put("attributes", attributes)).toString())));
            server.stubFor(get(urlPathEqualTo("/netbackup/catalog/images"))
                    .withQueryParam("filter", equalTo("policyName eq '" + policy + "'"))
                    .withHeader(HttpHeaders.AUTHORIZATION, equalTo(API_KEY))
                    .willReturn(aResponse().withStatus(200).withBody("{\"data\":[{\"id\":\"ablecube4_1792630519\","
                            + "\"attributes\":{\"backupTime\":\"2026-10-22T00:55:19Z\"}}]}")));
            client.backupVolumeArtifact("ablecube4", "ablecube4", "192.0.2.4", artifact, metadata);
            assertTrue(artifact.completed);
            assertEquals("ablecube4_1792630519", artifact.externalId);
        }
    }

    @Test
    public void restorePollingRequiresACompletedJobAndAnExplicitZeroStatus() throws Exception {
        for (Object code : new Object[]{0, "0", 1, "1", JSONObject.NULL}) {
            for (String state : new String[]{"ACTIVE", "DONE", "SUSPENDED"}) {
                JSONObject attributes = new JSONObject().put("state", state).put("status", code);
                server.stubFor(get(urlEqualTo("/netbackup/admin/jobs/42"))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo(API_KEY))
                        .willReturn(aResponse().withStatus(200).withBody(new JSONObject()
                                .put("data", new JSONObject().put("attributes", attributes)).toString())));

                ThirdPartyBackupRestore.Result result = client().pollVolumeRestore("42");
                assertEquals("DONE".equals(state) && "0".equals(String.valueOf(code)), result.completed);
            }
        }
    }

    @Test
    public void imageExpirationUsesRawAuthenticationAndKeepsMpaApprovalPending() throws Exception {
        for (int status : new int[]{200, 204, 202}) {
            server.stubFor(post(urlEqualTo("/netbackup/catalog/expire-images"))
                    .withHeader(HttpHeaders.AUTHORIZATION, equalTo(API_KEY))
                    .withRequestBody(containing("\"backupId\":\"ablecube4_1792630519\""))
                    .withRequestBody(containing("\"copyNumber\":1"))
                    .willReturn(aResponse().withStatus(status).withHeader("X-NetBackup-MPA-Ticket", "ticket-1")));

            AblestackNetBackupClient.ExpireImageResult result = client().expireBackupImage("ablecube4_1792630519", 1);
            assertEquals(status != 202, result.isCompleted());
            if (status == 202) { assertEquals("ticket-1", result.getMpaTicket()); }
        }
    }

    private AblestackNetBackupClient client() throws Exception {
        return new AblestackNetBackupClient("http://localhost:" + server.port() + "/netbackup", API_KEY, 5);
    }
}
