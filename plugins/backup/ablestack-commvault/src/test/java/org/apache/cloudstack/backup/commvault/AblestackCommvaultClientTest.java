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

package org.apache.cloudstack.backup.commvault;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.backup.ThirdPartyBackupManifest;
import org.apache.cloudstack.backup.ThirdPartyBackupVolumeService;

import com.cloud.utils.exception.CloudRuntimeException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

public class AblestackCommvaultClientTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Test
    public void getJobRetentionInfoReadsAllCopiesWithOneAdvancedDetailsRequest() throws Exception {
        wireMockRule.stubFor(post(urlEqualTo("/api/Login"))
                .willReturn(aResponse().withStatus(200).withBody("{\"token\":\"test-token\"}")));
        wireMockRule.stubFor(get(urlEqualTo("/api/Job/1055/AdvancedDetails?infoType=1"))
                .willReturn(aResponse().withStatus(200).withBody("{\"jobRetention\":{\"storagePolicyRetentionInfoList\":["
                        + "{\"storagePolicyId\":4,\"copyRetentionInfoList\":["
                        + "{\"storagePolicyCopyId\":3,\"retentionDays\":1791590564},"
                        + "{\"storagePolicyCopyId\":5,\"retentionDays\":1791590564}]}]}}")));

        AblestackCommvaultClient client = new AblestackCommvaultClient(
                "http://localhost:" + wireMockRule.port() + "/api", "admin", "password", true, 5);
        List<AblestackCommvaultClient.JobRetentionInfo> retentionInfo = client.getJobRetentionInfo("1055");

        assertEquals(2, retentionInfo.size());
        assertEquals("4", retentionInfo.get(0).getStoragePolicyId());
        assertEquals("3", retentionInfo.get(0).getStoragePolicyCopyId());
        assertEquals("1791590564", retentionInfo.get(0).getRetainedUntil());
        assertEquals("5", retentionInfo.get(1).getStoragePolicyCopyId());
        wireMockRule.verify(1, getRequestedFor(urlEqualTo("/api/Job/1055/AdvancedDetails?infoType=1")));
    }

    @Test
    public void getRequestReauthenticatesAndRetriesOnce() throws Exception {
        stubTokenRenewal();
        String path = "/api/Job/1055/AdvancedDetails?infoType=1";
        wireMockRule.stubFor(get(urlEqualTo(path)).withHeader("Authtoken", equalTo("old-token"))
                .willReturn(aResponse().withStatus(401)));
        wireMockRule.stubFor(get(urlEqualTo(path)).withHeader("Authtoken", equalTo("new-token"))
                .willReturn(aResponse().withStatus(200).withBody("{\"jobRetention\":{\"storagePolicyRetentionInfoList\":[]}}")));

        assertTrue(newClient().getJobRetentionInfo("1055").isEmpty());
        wireMockRule.verify(2, getRequestedFor(urlEqualTo(path)));
        wireMockRule.verify(2, postRequestedFor(urlEqualTo("/api/Login")));
    }

    @Test
    public void jsonPostReauthenticatesAndRetriesOnce() throws Exception {
        stubTokenRenewal();
        String path = "/api/JobDetails";
        wireMockRule.stubFor(post(urlEqualTo(path)).withHeader("Authtoken", equalTo("old-token"))
                .willReturn(aResponse().withStatus(401)));
        wireMockRule.stubFor(post(urlEqualTo(path)).withHeader("Authtoken", equalTo("new-token"))
                .willReturn(aResponse().withStatus(200).withBody("{\"job\":{}}")));

        assertEquals("{\"job\":{}}", newClient().getJobDetails("1055"));
        wireMockRule.verify(2, postRequestedFor(urlEqualTo(path)));
        wireMockRule.verify(2, postRequestedFor(urlEqualTo("/api/Login")));
    }

    @Test
    public void deleteReauthenticatesAndRetriesOnce() throws Exception {
        stubTokenRenewal();
        String path = "/api/Backupset/9";
        wireMockRule.stubFor(delete(urlEqualTo(path)).withHeader("Authtoken", equalTo("old-token"))
                .willReturn(aResponse().withStatus(401)));
        wireMockRule.stubFor(delete(urlEqualTo(path)).withHeader("Authtoken", equalTo("new-token"))
                .willReturn(aResponse().withStatus(200)));

        assertTrue(newClient().deleteBackupSet("9"));
        wireMockRule.verify(2, deleteRequestedFor(urlEqualTo(path)));
        wireMockRule.verify(2, postRequestedFor(urlEqualTo("/api/Login")));
    }

    @Test
    public void secondUnauthorizedResponseDoesNotTriggerAnotherRetry() throws Exception {
        stubTokenRenewal();
        String path = "/api/Job/1055/AdvancedDetails?infoType=1";
        wireMockRule.stubFor(get(urlEqualTo(path)).willReturn(aResponse().withStatus(401)));

        assertThrows(ServerApiException.class, () -> newClient().getJobRetentionInfo("1055"));
        wireMockRule.verify(2, getRequestedFor(urlEqualTo(path)));
        wireMockRule.verify(2, postRequestedFor(urlEqualTo("/api/Login")));
    }

    @Test
    public void catalogHttpFailureIncludesStatusAndServiceErrorInsteadOfReportingMissing() throws Exception {
        ThirdPartyBackupVolumeService.ArtifactInventory inventory = newInventory();
        wireMockRule.stubFor(post(urlEqualTo("/api/DoBrowse"))
                .willReturn(aResponse().withStatus(400)
                        .withBody("{\"errorCode\":12,\"errorMessage\":\"Invalid browse\\nrequest\",\"other\":\"not included\"}")));

        CloudRuntimeException error = assertThrows(CloudRuntimeException.class, () -> inventory.exists(artifact()));
        assertEquals("Commvault artifact catalog query failed (/DoBrowse, HTTP 400); errorCode=12; Invalid browse request", error.getMessage());
    }

    @Test
    public void emptyCatalogResponseRemainsAQueryFailure() throws Exception {
        ThirdPartyBackupVolumeService.ArtifactInventory inventory = newInventory();
        wireMockRule.stubFor(post(urlEqualTo("/api/DoBrowse")).willReturn(aResponse().withStatus(200)));

        CloudRuntimeException error = assertThrows(CloudRuntimeException.class, () -> inventory.exists(artifact()));
        assertEquals("Commvault artifact catalog query failed (/DoBrowse, HTTP 200): empty response", error.getMessage());
    }

    @Test
    public void nonJsonCatalogHttpFailureKeepsStatusWithoutIncludingHtml() throws Exception {
        ThirdPartyBackupVolumeService.ArtifactInventory inventory = newInventory();
        wireMockRule.stubFor(post(urlEqualTo("/api/DoBrowse"))
                .willReturn(aResponse().withStatus(404).withBody("<html>Proxy error page</html>")));

        CloudRuntimeException error = assertThrows(CloudRuntimeException.class, () -> inventory.exists(artifact()));
        assertEquals("Commvault artifact catalog query failed (/DoBrowse, HTTP 404)", error.getMessage());
    }

    @Test
    public void catalogAvailabilityRequiresTheSavedPathAndJob() throws Exception {
        ThirdPartyBackupVolumeService.ArtifactInventory inventory = newInventory();
        wireMockRule.stubFor(post(urlEqualTo("/api/DoBrowse")).atPriority(10)
                .willReturn(aResponse().withStatus(400).withBody("{\"errorMessage\":\"Invalid JSON browse types\"}")));
        wireMockRule.stubFor(post(urlEqualTo("/api/DoBrowse"))
                .atPriority(1)
                .withRequestBody(equalToJson("{\"opType\":0,\"options\":{\"showDeletedFiles\":true,\"restoreIndex\":true}}", true, true))
                .willReturn(aResponse().withStatus(200).withBody(catalogResult("1127"))));

        assertTrue(inventory.exists(artifact()));
        wireMockRule.verify(postRequestedFor(urlEqualTo("/api/DoBrowse"))
                .withRequestBody(matchingJsonPath("$.paths[0].path", equalTo("/mnt/staging/vm/recovery-point")))
                .withRequestBody(matchingJsonPath("$.timeRange.toTime", equalTo("1791791903"))));

        wireMockRule.stubFor(post(urlEqualTo("/api/DoBrowse"))
                .atPriority(1)
                .withRequestBody(equalToJson("{\"opType\":0,\"options\":{\"showDeletedFiles\":true,\"restoreIndex\":true}}", true, true))
                .willReturn(aResponse().withStatus(200).withBody(catalogResult("9999"))));
        assertThrows(CloudRuntimeException.class, () -> newInventory().exists(artifact()));
    }

    @Test
    public void metadataCatalogBrowseChecksTheManifestWithJsonOperationTypes() throws Exception {
        ThirdPartyBackupVolumeService.ArtifactInventory inventory = newInventory();
        ThirdPartyBackupManifest.Artifact metadata = artifact();
        metadata.path = "/mnt/staging/vm/recovery-point";
        metadata.checkpointName = "recovery-point";
        metadata.jobId = "1129";
        wireMockRule.stubFor(post(urlEqualTo("/api/DoBrowse"))
                .withRequestBody(equalToJson("{\"opType\":0,\"options\":{\"showDeletedFiles\":true,\"restoreIndex\":true},"
                        + "\"paths\":[{\"path\":\"/mnt/staging/vm/recovery-point\"}]}", true, true))
                .willReturn(aResponse().withStatus(200).withBody(catalogResult("1129").replace("root.qcow2", "backup-manifest.json"))));

        assertTrue(inventory.exists(metadata));
    }

    @Test
    public void onlyACompleteEmptyCatalogResultReportsMissing() throws Exception {
        ThirdPartyBackupVolumeService.ArtifactInventory inventory = newInventory();
        wireMockRule.stubFor(post(urlEqualTo("/api/DoBrowse"))
                .willReturn(aResponse().withStatus(200).withBody("{\"browseResponses\":["
                        + "{\"respType\":0,\"browseResult\":{\"queryId\":\"dataQuery\",\"dataResultSet\":[]}},"
                        + "{\"respType\":0,\"browseResult\":{\"queryId\":\"countQuery\",\"aggrResultSet\":{\"result\":0}}}]}")));

        assertFalse(inventory.exists(artifact()));
    }

    private ThirdPartyBackupVolumeService.ArtifactInventory newInventory() throws Exception {
        wireMockRule.stubFor(post(urlEqualTo("/api/Login"))
                .willReturn(aResponse().withStatus(200).withBody("{\"token\":\"test-token\"}")));
        wireMockRule.stubFor(get(urlEqualTo("/api/Client"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"clientProperties\":[{\"client\":{\"clientEntity\":{\"clientId\":1,\"clientName\":\"ablecube4\"}}}]}")));
        wireMockRule.stubFor(get(urlEqualTo("/api/Client/1"))
                .willReturn(aResponse().withStatus(200).withBody("{\"clientProperties\":[{}]}")));
        wireMockRule.stubFor(get(urlEqualTo("/api/Subclient?clientId=1"))
                .willReturn(aResponse().withStatus(200)
                        .withBody("{\"subClientProperties\":[{\"subClientEntity\":{\"backupsetName\":\"i-2-220-VM\","
                                + "\"applicationId\":29,\"subclientId\":2,\"backupsetId\":3,\"instanceId\":4}}]}")));
        return newClient().volumeArtifactInventory("i-2-220-VM");
    }

    private ThirdPartyBackupManifest.Artifact artifact() {
        ThirdPartyBackupManifest.Artifact artifact = new ThirdPartyBackupManifest.Artifact();
        artifact.path = "/mnt/staging/vm/recovery-point/root.qcow2";
        artifact.sourceHost = "ablecube4";
        artifact.jobId = "1127";
        artifact.backupTime = "1791791903";
        artifact.completed = true;
        return artifact;
    }

    private String catalogResult(String jobId) {
        return "{\"browseResponses\":[{\"respType\":0,\"browseResult\":{\"queryId\":\"dataQuery\",\"dataResultSet\":["
                + "{\"displayPath\":\"/mnt/staging/vm/recovery-point/root.qcow2\",\"advancedData\":{\"backupJobId\":\"" + jobId + "\"}}]}},"
                + "{\"respType\":0,\"browseResult\":{\"queryId\":\"countQuery\",\"aggrResultSet\":{\"result\":1}}}]}";
    }

    private void stubTokenRenewal() {
        wireMockRule.stubFor(post(urlEqualTo("/api/Login"))
                .inScenario("token renewal")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(200).withBody("{\"token\":\"old-token\"}"))
                .willSetStateTo("expired"));
        wireMockRule.stubFor(post(urlEqualTo("/api/Login"))
                .inScenario("token renewal")
                .whenScenarioStateIs("expired")
                .willReturn(aResponse().withStatus(200).withBody("{\"token\":\"new-token\"}")));
    }

    private AblestackCommvaultClient newClient() throws Exception {
        return new AblestackCommvaultClient("http://localhost:" + wireMockRule.port() + "/api", "admin", "password", true, 5);
    }
}
