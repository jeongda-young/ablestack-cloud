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
package com.cloud.vm.guestnetwork;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.VmGuestNetworkState;
import com.cloud.agent.api.VmGuestNetworkSectionStatus;
import com.cloud.agent.api.VmGuestToolsInfo;
import com.cloud.serializer.GsonHelper;
import com.cloud.vm.VmGuestNetworkStateVO;
import com.cloud.vm.dao.VmGuestNetworkStateDao;
import com.cloud.vm.guestnetwork.VmGuestNetworkPayloadCanonicalizer.CanonicalPayload;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

@Component
public class VmGuestNetworkStateServiceImpl implements VmGuestNetworkStateService {
    private static final Logger LOGGER = LogManager.getLogger(VmGuestNetworkStateServiceImpl.class);
    static final int MAX_ERROR_MESSAGE_LENGTH = 255;

    @Inject
    private VmGuestNetworkStateDao stateDao;

    private final VmGuestNetworkPayloadCanonicalizer canonicalizer;
    private final Gson gson = GsonHelper.getGson();

    public VmGuestNetworkStateServiceImpl() {
        this(new VmGuestNetworkPayloadCanonicalizer());
    }

    VmGuestNetworkStateServiceImpl(VmGuestNetworkPayloadCanonicalizer canonicalizer) {
        this.canonicalizer = canonicalizer;
    }

    @Override
    public VmGuestNetworkStateVO findByVmId(long vmId) {
        return stateDao.findByVmId(vmId);
    }

    @Override
    public PersistResult persistSuccess(long vmId, VmGuestNetworkState state, Date observedAt) {
        VmGuestNetworkStateVO existing = stateDao.findByVmId(vmId);
        VmGuestNetworkState mergedState = mergeSections(existing, state);
        CanonicalPayload canonicalPayload = canonicalizer.canonicalize(mergedState);
        Date now = copy(observedAt);
        if (existing == null) {
            VmGuestNetworkStateVO created = new VmGuestNetworkStateVO(vmId, now);
            applySuccess(created, mergedState, canonicalPayload, now);
            stateDao.persist(created);
            logPersistResult(vmId, PersistResult.CREATED);
            return PersistResult.CREATED;
        }

        boolean payloadChanged = !StringUtils.equals(existing.getPayloadHash(), canonicalPayload.getHash());
        applySuccess(existing, mergedState, canonicalPayload, now);
        if (payloadChanged) {
            stateDao.updateSnapshot(existing);
            logPersistResult(vmId, PersistResult.PAYLOAD_UPDATED);
            return PersistResult.PAYLOAD_UPDATED;
        }
        stateDao.updateMetadata(existing);
        logPersistResult(vmId, PersistResult.METADATA_ONLY);
        return PersistResult.METADATA_ONLY;
    }

    private void logPersistResult(long vmId, PersistResult result) {
        LOGGER.debug("Guest network snapshot persistence metrics for VM [{}]: result={}", vmId, result);
    }

    private VmGuestNetworkState mergeSections(VmGuestNetworkStateVO existing,
            VmGuestNetworkState incoming) {
        if (existing == null || StringUtils.isBlank(existing.getPayload()) || incoming == null) {
            return incoming;
        }
        VmGuestNetworkState previous;
        try {
            previous = gson.fromJson(existing.getPayload(), VmGuestNetworkState.class);
        } catch (JsonParseException e) {
            return incoming;
        }
        if (previous == null) {
            return incoming;
        }
        Map<String, VmGuestNetworkSectionStatus> incomingSections = incoming.getSectionStatuses();
        Map<String, VmGuestNetworkSectionStatus> previousSections = previous.getSectionStatuses();
        if (incomingSections == null) {
            incomingSections = new LinkedHashMap<>();
            incoming.setSectionStatuses(incomingSections);
        }
        if (isNotDue(incomingSections.get("interfaces"))) {
            incoming.setInterfaces(previous.getInterfaces());
            copySection(previousSections, incomingSections, "interfaces");
        } else if (isFailed(incomingSections.get("interfaces"))
                && previous.getInterfaces() != null && !previous.getInterfaces().isEmpty()) {
            incoming.setInterfaces(previous.getInterfaces());
            markStale(incomingSections, "interfaces");
        }
        if (isNotDue(incomingSections.get("routes"))) {
            incoming.setRoutes(previous.getRoutes());
            copySection(previousSections, incomingSections, "routes");
        } else if (isFailed(incomingSections.get("routes"))
                && previous.getRoutes() != null && !previous.getRoutes().isEmpty()) {
            incoming.setRoutes(previous.getRoutes());
            markStale(incomingSections, "routes");
        }
        if (isNotDue(incomingSections.get("dns"))) {
            incoming.setDns(previous.getDns());
            copySection(previousSections, incomingSections, "dns");
        } else if (isFailed(incomingSections.get("dns"))
                && hasDnsData(previous)) {
            incoming.setDns(previous.getDns());
            markStale(incomingSections, "dns");
        }
        Map<String, Boolean> capabilities = new LinkedHashMap<>();
        if (previous.getCapabilities() != null) {
            capabilities.putAll(previous.getCapabilities());
        }
        if (incoming.getCapabilities() != null) {
            capabilities.putAll(incoming.getCapabilities());
        }
        incoming.setCapabilities(capabilities);
        if (StringUtils.isBlank(incoming.getAgentVersion())) {
            incoming.setAgentVersion(previous.getAgentVersion());
        }
        if (StringUtils.isBlank(incoming.getCollectorBuildId())) {
            incoming.setCollectorBuildId(previous.getCollectorBuildId());
        }
        if (incoming.getCollectorHostId() == null) {
            incoming.setCollectorHostId(previous.getCollectorHostId());
        }
        if (StringUtils.isBlank(incoming.getCapabilityHash())) {
            incoming.setCapabilityHash(previous.getCapabilityHash());
        }
        if (incoming.getGuestTools() == null) {
            incoming.setGuestTools(previous.getGuestTools());
        }
        incoming.setStatus(deriveOverallStatus(incomingSections, incoming.getStatus()));
        return incoming;
    }

    private boolean isNotDue(VmGuestNetworkSectionStatus section) {
        return section != null && "NOT_DUE".equals(section.getStatus());
    }

    private boolean isFailed(VmGuestNetworkSectionStatus section) {
        return section != null
                && ("UNAVAILABLE".equals(section.getStatus())
                    || "UNSUPPORTED".equals(section.getStatus()));
    }

    private boolean hasDnsData(VmGuestNetworkState state) {
        if (state.getDns() == null) {
            return false;
        }
        return (state.getDns().getConfigurations() != null
                && !state.getDns().getConfigurations().isEmpty())
                || (state.getDns().getServers() != null
                && !state.getDns().getServers().isEmpty())
                || (state.getDns().getSearchDomains() != null
                && !state.getDns().getSearchDomains().isEmpty());
    }

    private void markStale(Map<String, VmGuestNetworkSectionStatus> sections, String name) {
        VmGuestNetworkSectionStatus failed = sections.get(name);
        String details = "Last successful " + name + " snapshot retained";
        if (StringUtils.isNotBlank(failed.getDetails())) {
            details += ": " + failed.getDetails();
        }
        sections.put(name, new VmGuestNetworkSectionStatus("STALE", truncate(details)));
    }

    private void copySection(Map<String, VmGuestNetworkSectionStatus> source,
            Map<String, VmGuestNetworkSectionStatus> target, String name) {
        if (source != null && source.get(name) != null) {
            target.put(name, source.get(name));
        }
    }

    private String deriveOverallStatus(Map<String, VmGuestNetworkSectionStatus> sections,
            String fallback) {
        int considered = 0;
        int successful = 0;
        int partial = 0;
        int stale = 0;
        int unavailable = 0;
        for (String name : new String[] {"interfaces", "routes", "dns"}) {
            VmGuestNetworkSectionStatus section = sections.get(name);
            if (section == null || "NOT_DUE".equals(section.getStatus())
                    || "NOT_COLLECTED".equals(section.getStatus())) {
                continue;
            }
            considered++;
            if ("OK".equals(section.getStatus()) || "EMPTY".equals(section.getStatus())) {
                successful++;
            } else if ("PARTIAL".equals(section.getStatus())) {
                partial++;
            } else if ("STALE".equals(section.getStatus())) {
                stale++;
            } else if ("UNAVAILABLE".equals(section.getStatus())) {
                unavailable++;
            }
        }
        if (considered == 0) {
            return fallback;
        }
        if (partial > 0 || (stale > 0 && successful > 0)) {
            return "PARTIAL";
        }
        if (successful == considered) {
            return "OK";
        }
        if (successful > 0) {
            return "PARTIAL";
        }
        if (stale > 0) {
            return "STALE";
        }
        return unavailable > 0 ? "UNAVAILABLE" : "UNSUPPORTED";
    }

    @Override
    public PersistResult persistFailure(long vmId, VmGuestNetworkState state, String errorCode,
            String errorMessage, Date observedAt) {
        VmGuestNetworkStateVO existing = stateDao.findByVmId(vmId);
        Date now = copy(observedAt);
        if (existing == null) {
            existing = new VmGuestNetworkStateVO(vmId, now);
            existing.setStatus(state != null && StringUtils.isNotBlank(state.getStatus())
                    ? state.getStatus() : "UNAVAILABLE");
            existing.setQgaVersion(state == null ? null : state.getAgentVersion());
            existing.setErrorCode(errorCode);
            existing.setErrorMessage(truncate(errorMessage));
            stateDao.persist(existing);
            return PersistResult.CREATED;
        }

        existing.setStatus(StringUtils.isNotBlank(existing.getPayload()) ? "STALE" : "UNAVAILABLE");
        if (state != null && StringUtils.isNotBlank(state.getAgentVersion())) {
            existing.setQgaVersion(state.getAgentVersion());
        }
        existing.setObservedAt(now);
        existing.setErrorCode(errorCode);
        existing.setErrorMessage(truncate(errorMessage));
        existing.setUpdated(now);
        stateDao.updateMetadata(existing);
        return PersistResult.METADATA_ONLY;
    }

    @Override
    public PersistResult markStopped(long vmId, Date observedAt) {
        VmGuestNetworkStateVO existing = stateDao.findByVmId(vmId);
        if (existing == null) {
            return PersistResult.NOT_FOUND;
        }
        Date now = copy(observedAt);
        existing.setStatus("STOPPED");
        existing.setObservedAt(now);
        existing.setUpdated(now);
        existing.setErrorCode(null);
        existing.setErrorMessage(null);
        stateDao.updateMetadata(existing);
        return PersistResult.METADATA_ONLY;
    }

    @Override
    public boolean removeByVmId(long vmId) {
        return stateDao.removeByVmId(vmId);
    }

    private void applySuccess(VmGuestNetworkStateVO target, VmGuestNetworkState state,
            CanonicalPayload payload, Date now) {
        target.setSchemaVersion(state.getSchemaVersion());
        target.setStatus(StringUtils.defaultIfBlank(state.getStatus(), "OK"));
        if (StringUtils.isNotBlank(state.getAgentVersion())) {
            target.setQgaVersion(state.getAgentVersion());
        }
        target.setCollectorBuildId(state.getCollectorBuildId());
        target.setCollectorHostId(state.getCollectorHostId());
        target.setCapabilityHash(state.getCapabilityHash());
        VmGuestToolsInfo tools = state.getGuestTools();
        if (tools != null) {
            target.setGuestToolsVersion(tools.getVersion());
            target.setQgaPolicyMode(tools.getQgaPolicyMode());
            target.setReadinessStatus(tools.getReadinessStatus());
            target.setReadinessCheckedAt(now);
        }
        target.setObservedAt(now);
        target.setLastSuccessAt(now);
        target.setPayloadHash(payload.getHash());
        target.setPayload(payload.getPayload());
        target.setErrorCode(null);
        target.setErrorMessage(null);
        target.setUpdated(now);
    }

    private String truncate(String value) {
        return StringUtils.abbreviate(value, MAX_ERROR_MESSAGE_LENGTH);
    }

    private Date copy(Date value) {
        return value == null ? new Date() : new Date(value.getTime());
    }
}
