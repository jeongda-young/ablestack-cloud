// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
package com.cloud.vm.guestnetwork;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.VmGuestNetworkSectionStatus;
import com.cloud.agent.api.VmGuestNetworkState;
import com.cloud.serializer.GsonHelper;
import com.cloud.vm.VmGuestNetworkSectionStateVO;
import com.cloud.vm.dao.VmGuestNetworkSectionStateDao;
import com.google.gson.Gson;

@Component
public class VmGuestNetworkScheduleServiceImpl implements VmGuestNetworkScheduleService {
    public static final String INTERFACES = "interfaces";
    public static final String ROUTES = "routes";
    public static final String DNS = "dns";
    public static final String READINESS = "readiness";
    private static final List<String> ALL_SECTIONS =
            java.util.Arrays.asList(INTERFACES, ROUTES, DNS, READINESS);

    @Inject
    private VmGuestNetworkSectionStateDao sectionDao;

    private final Gson gson = GsonHelper.getGson();

    @Override
    public Map<Long, DueWork> findDueWork(Collection<Long> vmIds, Date now) {
        if (vmIds == null || vmIds.isEmpty()) {
            return Collections.emptyMap();
        }
        ensureRows(vmIds, now);
        Map<Long, Set<String>> dueSections = new LinkedHashMap<>();
        Map<Long, Date> oldest = new LinkedHashMap<>();
        for (VmGuestNetworkSectionStateVO row : sectionDao.listByVmIds(vmIds)) {
            if (row.getNextDueAt() == null || row.getNextDueAt().after(now)
                    || (row.getLeaseUntil() != null && row.getLeaseUntil().after(now))) {
                continue;
            }
            dueSections.computeIfAbsent(row.getVmId(), ignored -> new LinkedHashSet<>())
                    .add(row.getSection());
            Date current = oldest.get(row.getVmId());
            if (current == null || row.getNextDueAt().before(current)) {
                oldest.put(row.getVmId(), row.getNextDueAt());
            }
        }
        Map<Long, DueWork> result = new LinkedHashMap<>();
        dueSections.forEach((vmId, sections) ->
                result.put(vmId, new DueWork(sections, oldest.get(vmId))));
        return result;
    }

    private void ensureRows(Collection<Long> vmIds, Date now) {
        Map<Long, Set<String>> existing = new LinkedHashMap<>();
        sectionDao.listByVmIds(vmIds).forEach(row ->
                existing.computeIfAbsent(row.getVmId(), ignored -> new LinkedHashSet<>())
                        .add(row.getSection()));
        for (Long vmId : vmIds) {
            Set<String> sections = existing.getOrDefault(vmId, Collections.emptySet());
            for (String section : ALL_SECTIONS) {
                if (!sections.contains(section)) {
                    sectionDao.persist(new VmGuestNetworkSectionStateVO(vmId, section, now));
                }
            }
        }
    }

    @Override
    public void claim(Collection<Long> vmIds, String leaseOwner, Date now, int leaseSeconds) {
        Date leaseUntil = new Date(now.getTime() + Math.max(1, leaseSeconds) * 1000L);
        Map<Long, DueWork> due = findDueWork(vmIds, now);
        for (Map.Entry<Long, DueWork> entry : due.entrySet()) {
            for (String section : entry.getValue().getSections()) {
                VmGuestNetworkSectionStateVO row =
                        sectionDao.findByVmIdAndSection(entry.getKey(), section);
                if (row == null || row.getNextDueAt().after(now)
                        || (row.getLeaseUntil() != null && row.getLeaseUntil().after(now))) {
                    continue;
                }
                row.setLeaseOwner(leaseOwner);
                row.setLeaseUntil(leaseUntil);
                row.setUpdated(now);
                sectionDao.update(row.getId(), row);
            }
        }
    }

    @Override
    public Set<String> getClaimedSections(long vmId, String leaseOwner, Date now) {
        Set<String> result = new LinkedHashSet<>();
        for (VmGuestNetworkSectionStateVO row : sectionDao.listByVmId(vmId)) {
            if (leaseOwner.equals(row.getLeaseOwner())
                    && row.getLeaseUntil() != null && row.getLeaseUntil().after(now)) {
                result.add(row.getSection());
            }
        }
        return result;
    }

    @Override
    public void complete(long vmId, VmGuestNetworkState state, Date observedAt,
            long interfaceIntervalSeconds, long routeIntervalSeconds,
            long dnsIntervalSeconds, int jitterPercent, long maxBackoffSeconds) {
        for (VmGuestNetworkSectionStateVO row : sectionDao.listByVmId(vmId)) {
            VmGuestNetworkSectionStatus status = state.getSectionStatuses() == null
                    ? null : state.getSectionStatuses().get(row.getSection());
            if (status == null || "NOT_DUE".equals(status.getStatus())) {
                continue;
            }
            row.setStatus(status.getStatus());
            row.setSource(status.getSource());
            row.setObservedAt(observedAt);
            row.setErrorCode(status.getErrorCode());
            row.setErrorMessage(abbreviate(status.getDetails()));
            String payload = sectionPayload(row.getSection(), state);
            String hash = sha256(payload);
            if (!StringUtils.equals(hash, row.getPayloadHash())) {
                row.setPayload(payload);
                row.setPayloadHash(hash);
            }
            if (isSuccessful(status.getStatus())) {
                row.setFailureCount(0);
                row.setLastSuccessAt(observedAt);
                row.setNextDueAt(nextDue(vmId, observedAt,
                        interval(row.getSection(), interfaceIntervalSeconds,
                                routeIntervalSeconds, dnsIntervalSeconds),
                        jitterPercent, row.getSection().hashCode()));
                row.setErrorCode(null);
                row.setErrorMessage(null);
            } else {
                scheduleFailure(row, vmId, observedAt,
                        interval(row.getSection(), interfaceIntervalSeconds,
                                routeIntervalSeconds, dnsIntervalSeconds),
                        jitterPercent, maxBackoffSeconds);
            }
            clearLease(row);
            row.setUpdated(observedAt);
            sectionDao.update(row.getId(), row);
        }
    }

    @Override
    public void fail(long vmId, String errorCode, String errorMessage, Date observedAt,
            long interfaceIntervalSeconds, long routeIntervalSeconds,
            long dnsIntervalSeconds, int jitterPercent, long maxBackoffSeconds) {
        for (VmGuestNetworkSectionStateVO row : sectionDao.listByVmId(vmId)) {
            if (row.getLeaseOwner() == null) {
                continue;
            }
            row.setStatus(row.getLastSuccessAt() == null ? "UNAVAILABLE" : "STALE");
            row.setObservedAt(observedAt);
            row.setErrorCode(errorCode);
            row.setErrorMessage(abbreviate(errorMessage));
            scheduleFailure(row, vmId, observedAt,
                    interval(row.getSection(), interfaceIntervalSeconds,
                            routeIntervalSeconds, dnsIntervalSeconds),
                    jitterPercent, maxBackoffSeconds);
            clearLease(row);
            row.setUpdated(observedAt);
            sectionDao.update(row.getId(), row);
        }
    }

    @Override
    public boolean requestRefresh(long vmId, Set<String> sections, Date now, int cooldownSeconds) {
        ensureRows(Collections.singleton(vmId), now);
        boolean accepted = false;
        Set<String> requested = sections == null || sections.isEmpty()
                ? new LinkedHashSet<>(ALL_SECTIONS) : sections;
        for (VmGuestNetworkSectionStateVO row : sectionDao.listByVmId(vmId)) {
            if (!requested.contains(row.getSection())) {
                continue;
            }
            if (row.getUpdated() != null
                    && now.getTime() - row.getUpdated().getTime() < cooldownSeconds * 1000L
                    && row.getNextDueAt() != null && !row.getNextDueAt().after(now)) {
                continue;
            }
            row.setNextDueAt(now);
            row.setFailureCount(0);
            clearLease(row);
            row.setUpdated(now);
            sectionDao.update(row.getId(), row);
            accepted = true;
        }
        return accepted;
    }

    @Override
    public void invalidateFailedSections(long vmId, Date now) {
        for (VmGuestNetworkSectionStateVO row : sectionDao.listByVmId(vmId)) {
            if (isSuccessful(row.getStatus())) {
                continue;
            }
            row.setFailureCount(0);
            row.setNextDueAt(now);
            clearLease(row);
            row.setUpdated(now);
            sectionDao.update(row.getId(), row);
        }
    }

    @Override
    public List<VmGuestNetworkSectionStateVO> listByVmId(long vmId) {
        return new ArrayList<>(sectionDao.listByVmId(vmId));
    }

    private void scheduleFailure(VmGuestNetworkSectionStateVO row, long vmId, Date now,
            long baseSeconds, int jitterPercent, long maxBackoffSeconds) {
        int failures = Math.min(30, row.getFailureCount() + 1);
        row.setFailureCount(failures);
        long multiplier = 1L << Math.min(20, failures - 1);
        long backoff = Math.min(maxBackoffSeconds, Math.max(1L, baseSeconds) * multiplier);
        row.setNextDueAt(nextDue(vmId, now, backoff, jitterPercent,
                row.getSection().hashCode()));
    }

    private Date nextDue(long vmId, Date now, long intervalSeconds,
            int jitterPercent, int salt) {
        long intervalMillis = Math.max(1L, intervalSeconds) * 1000L;
        int boundedPercent = Math.min(50, Math.max(0, jitterPercent));
        long range = intervalMillis * boundedPercent / 100L;
        long offset = range == 0 ? 0
                : Math.floorMod(mix(vmId ^ salt), range * 2L + 1L) - range;
        return new Date(now.getTime() + intervalMillis + offset);
    }

    private long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        return value ^ value >>> 33;
    }

    private long interval(String section, long interfaces, long routes, long dns) {
        if (INTERFACES.equals(section)) {
            return interfaces;
        }
        if (ROUTES.equals(section) || READINESS.equals(section)) {
            return routes;
        }
        return dns;
    }

    private boolean isSuccessful(String status) {
        return "OK".equals(status) || "EMPTY".equals(status) || "PARTIAL".equals(status);
    }

    private String sectionPayload(String section, VmGuestNetworkState state) {
        if (INTERFACES.equals(section)) {
            return gson.toJson(state.getInterfaces());
        }
        if (ROUTES.equals(section)) {
            return gson.toJson(state.getRoutes());
        }
        if (DNS.equals(section)) {
            return gson.toJson(state.getDns());
        }
        return gson.toJson(state.getGuestTools());
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(StringUtils.defaultString(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item & 0xff));
            }
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash guest network section payload", e);
        }
    }

    private String abbreviate(String value) {
        return StringUtils.abbreviate(value, 255);
    }

    private void clearLease(VmGuestNetworkSectionStateVO row) {
        row.setLeaseOwner(null);
        row.setLeaseUntil(null);
    }
}
