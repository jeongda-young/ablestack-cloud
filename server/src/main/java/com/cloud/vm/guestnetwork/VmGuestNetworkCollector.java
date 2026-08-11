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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmGuestNetworkStateAnswer;
import com.cloud.agent.api.GetVmGuestNetworkStateCommand;
import com.cloud.agent.api.VmGuestNetworkSectionStatus;
import com.cloud.agent.api.VmGuestNetworkState;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VmGuestNetworkStateVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class VmGuestNetworkCollector extends ManagerBase implements Configurable {
    private static final Logger LOGGER = LogManager.getLogger(VmGuestNetworkCollector.class);
    private static final String INTERFACE_CAPABILITY = "guest-network-get-interfaces";
    private static final String COLLECTOR_GLOBAL_LOCK = "VmGuestNetworkCollector.scan";
    private static final int GLOBAL_LOCK_TIMEOUT_SECONDS = 1;
    private static final int COLLECTOR_SCAN_INTERVAL_SECONDS = 30;

    public static final ConfigKey<Boolean> ENABLED = new ConfigKey<>("Advanced", Boolean.class,
            "vm.guest.network.details.enabled", "false",
            "Enable periodic collection of guest network state for running KVM user VMs.", true);
    public static final ConfigKey<String> HOST_IDS = new ConfigKey<>("Advanced", String.class,
            "vm.guest.network.details.host.ids", "",
            "Optional comma-separated host database IDs allowed for guest network collection. "
                    + "An empty value allows all hosts when enabled.", true);
    public static final ConfigKey<String> ZONE_IDS = new ConfigKey<>("Advanced", String.class,
            "vm.guest.network.details.zone.ids", "",
            "Optional comma-separated zone database IDs allowed for guest network collection. "
                    + "An empty value allows all zones when enabled.", true);
    public static final ConfigKey<Integer> INTERFACE_INTERVAL = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.interface.interval", "120",
            "Guest interface collection interval in seconds.", true);
    public static final ConfigKey<Integer> DNS_INTERVAL = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.dns.interval", "600",
            "Guest DNS collection interval in seconds for the DNS phase.", true);
    public static final ConfigKey<Integer> ROUTE_INTERVAL = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.route.interval", "600",
            "Guest route collection interval in seconds for the route phase.", true);
    public static final ConfigKey<Integer> JITTER_PERCENT = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.jitter.percent", "20",
            "Deterministic per-VM collection jitter percentage.", true);
    public static final ConfigKey<Integer> MAX_CONCURRENT_HOSTS = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.max.concurrent.hosts", "2",
            "Maximum hosts collected concurrently by one management server.", true);
    public static final ConfigKey<Integer> MAX_CONCURRENT_VMS_PER_HOST = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.max.concurrent.vms.per.host", "1",
            "Maximum VMs included in one host command batch.", true);
    public static final ConfigKey<Integer> MAX_VMS_PER_HOST_CYCLE = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.max.vms.per.host.cycle", "50",
            "Maximum VMs collected from one host in a collector cycle.", true);
    public static final ConfigKey<Integer> MAX_HOSTS_PER_CYCLE = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.max.hosts.per.cycle", "50",
            "Maximum hosts admitted to one management collector cycle.", true);
    public static final ConfigKey<Integer> FAILURE_BACKOFF_MAX = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.failure.backoff.max", "1800",
            "Maximum retry backoff in seconds after collection failures.", true);
    public static final ConfigKey<Integer> CAPABILITY_CACHE_TTL = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.capability.cache.ttl", "600",
            "QGA interface capability cache TTL in seconds.", true);
    public static final ConfigKey<Integer> COMMAND_TIMEOUT = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.command.timeout", "3",
            "QGA command timeout in seconds.", true);
    public static final ConfigKey<Integer> CYCLE_TIMEOUT = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.cycle.timeout", "60",
            "Maximum collector cycle wait in seconds.", true);
    public static final ConfigKey<Boolean> EXEC_FALLBACK_ENABLED = new ConfigKey<>("Advanced", Boolean.class,
            "vm.guest.network.details.exec.fallback.enabled", "false",
            "Enable fixed allowlisted guest-exec fallback for route and DNS collection.", true);
    public static final ConfigKey<Integer> EXEC_OUTPUT_LIMIT_BYTES = new ConfigKey<>("Advanced", Integer.class,
            "vm.guest.network.details.exec.output.limit.bytes", "1048576",
            "Maximum decoded stdout or stderr bytes accepted from allowlisted guest-exec.", true);

    @Inject
    private AgentManager agentManager;
    @Inject
    private VMInstanceDao vmInstanceDao;
    @Inject
    private NicDao nicDao;
    @Inject
    private VmGuestNetworkStateService stateService;
    @Inject
    private VmGuestNetworkScheduleService scheduleService;

    private final VmGuestNetworkCollectionPolicy policy;
    private final AtomicBoolean cycleRunning = new AtomicBoolean();
    private final Set<Long> activeHostIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<Long, Long> lastSelectedVmIdByHost = new java.util.concurrent.ConcurrentHashMap<>();
    private final String leaseOwner = "guest-network-" + UUID.randomUUID();
    private ScheduledExecutorService scheduler;
    private ExecutorService collectionExecutor;

    public VmGuestNetworkCollector() {
        this(new VmGuestNetworkCollectionPolicy());
    }

    VmGuestNetworkCollector(VmGuestNetworkCollectionPolicy policy) {
        this.policy = policy;
    }

    @Override
    public boolean start() {
        int concurrentHosts = Math.max(1, getMaxConcurrentHosts());
        int queueSize = Math.max(concurrentHosts, getMaxHostsPerCycle());
        collectionExecutor = new ThreadPoolExecutor(concurrentHosts, concurrentHosts, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize), new NamedThreadFactory("VmGuestNetwork-Host"),
                new ThreadPoolExecutor.AbortPolicy());
        scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("VmGuestNetwork-Scheduler"));
        scheduler.scheduleWithFixedDelay(this::runCycleSafely, 10L,
                COLLECTOR_SCAN_INTERVAL_SECONDS, TimeUnit.SECONDS);
        if (!isEnabled()) {
            LOGGER.info("Guest network collector is disabled; scheduler will remain idle until dynamically enabled");
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (collectionExecutor != null) {
            collectionExecutor.shutdownNow();
            collectionExecutor = null;
        }
        activeHostIds.clear();
        lastSelectedVmIdByHost.clear();
        cycleRunning.set(false);
        return true;
    }

    void runCycle() {
        if (!isEnabled() || !cycleRunning.compareAndSet(false, true)) {
            return;
        }
        GlobalLock scanLock = getCollectorGlobalLock();
        List<Callable<Void>> tasks = new ArrayList<>();
        try {
            if (!scanLock.lock(GLOBAL_LOCK_TIMEOUT_SECONDS)) {
                return;
            }
            try {
                reconcileTrackedVmStates();
                long now = System.currentTimeMillis();
                Map<Long, List<VMInstanceVO>> dueVmsByHost = selectDueVmsByHost(now);
                dueVmsByHost.forEach((hostId, vms) -> {
                    if (scheduleService != null) {
                        List<Long> vmIds = new ArrayList<>();
                        vms.forEach(vm -> vmIds.add(vm.getId()));
                        scheduleService.claim(vmIds, leaseOwner, new Date(now), getCycleTimeout());
                    }
                    tasks.add(() -> {
                        collectHost(hostId, vms);
                        return null;
                    });
                });
            } finally {
                scanLock.unlock();
            }
            // Never hold the management global lock during host/Agent I/O.
            executeTasks(tasks);
        } finally {
            scanLock.releaseRef();
            cycleRunning.set(false);
        }
    }

    void collectHost(long hostId, List<VMInstanceVO> vms) {
        if (!activeHostIds.add(hostId)) {
            return;
        }
        try {
            List<VMInstanceVO> orderedVms = rotateAfterLastSelectedVm(hostId, vms);
            int batchSize = Math.max(1, getMaxConcurrentVmsPerHost());
            int limit = Math.min(orderedVms.size(), Math.max(1, getMaxVmsPerHostCycle()));
            for (int offset = 0; offset < limit; offset += batchSize) {
                int toIndex = Math.min(limit, offset + batchSize);
                List<VMInstanceVO> batch = orderedVms.subList(offset, toIndex);
                collectBatch(hostId, batch);
                lastSelectedVmIdByHost.put(hostId, batch.get(batch.size() - 1).getId());
            }
        } finally {
            activeHostIds.remove(hostId);
        }
    }

    private List<VMInstanceVO> rotateAfterLastSelectedVm(long hostId, List<VMInstanceVO> vms) {
        if (vms.size() < 2) {
            return vms;
        }
        Long lastSelectedVmId = lastSelectedVmIdByHost.get(hostId);
        if (lastSelectedVmId == null) {
            return vms;
        }
        int start = 0;
        while (start < vms.size() && vms.get(start).getId() <= lastSelectedVmId) {
            start++;
        }
        if (start == 0 || start == vms.size()) {
            return vms;
        }
        List<VMInstanceVO> rotated = new ArrayList<>(vms.size());
        rotated.addAll(vms.subList(start, vms.size()));
        rotated.addAll(vms.subList(0, start));
        return rotated;
    }

    void collectBatch(long hostId, List<VMInstanceVO> vms) {
        if (vms.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, VMInstanceVO> vmsByName = new LinkedHashMap<>();
        Map<String, Map<String, String>> cloudNicIds = new LinkedHashMap<>();
        Set<String> cachedCapabilities = new LinkedHashSet<>();
        Set<String> interfacesDue = new LinkedHashSet<>();
        Set<String> routesDue = new LinkedHashSet<>();
        Set<String> dnsDue = new LinkedHashSet<>();
        Set<String> readinessDue = new LinkedHashSet<>();
        for (VMInstanceVO vm : vms) {
            Set<String> claimed = scheduleService == null
                    ? Collections.emptySet()
                    : scheduleService.getClaimedSections(vm.getId(), leaseOwner, new Date(now));
            boolean collectInterfaces = scheduleService == null
                    ? policy.isInterfaceDue(vm.getId(), now) : claimed.contains("interfaces");
            boolean collectRoutes = scheduleService == null
                    ? policy.isRouteDue(vm.getId(), now) : claimed.contains("routes");
            boolean collectDns = scheduleService == null
                    ? policy.isDnsDue(vm.getId(), now) : claimed.contains("dns");
            boolean collectReadiness = scheduleService != null && claimed.contains("readiness");
            if (!collectInterfaces && !collectRoutes && !collectDns && !collectReadiness) {
                continue;
            }
            vmsByName.put(vm.getInstanceName(), vm);
            cloudNicIds.put(vm.getInstanceName(), collectInterfaces
                    ? buildCloudNicMap(vm.getId()) : Collections.emptyMap());
            if (collectInterfaces) {
                interfacesDue.add(vm.getInstanceName());
            }
            if (collectRoutes) {
                routesDue.add(vm.getInstanceName());
            }
            if (collectDns) {
                dnsDue.add(vm.getInstanceName());
            }
            if (collectReadiness) {
                readinessDue.add(vm.getInstanceName());
            }
            if (collectInterfaces && policy.hasCachedEnabledInterfaceCapability(vm.getId(), now)) {
                cachedCapabilities.add(vm.getInstanceName());
            }
        }
        if (vmsByName.isEmpty()) {
            return;
        }

        GetVmGuestNetworkStateCommand command = new GetVmGuestNetworkStateCommand(
                new ArrayList<>(vmsByName.keySet()), cloudNicIds,
                Math.max(1, getCommandTimeout()), cachedCapabilities,
                interfacesDue, routesDue, dnsDue,
                isExecFallbackEnabled(), getExecOutputLimitBytes());
        command.setVmNamesRequiringReadiness(readinessDue);
        command.setPreferGuestToolsHelper(true);
        command.setCollectorHostId(hostId);
        Answer rawAnswer = agentManager.easySend(hostId, command);
        if (!(rawAnswer instanceof GetVmGuestNetworkStateAnswer) || !rawAnswer.getResult()) {
            vmsByName.values().forEach(vm ->
                    recordFailure(vm, null, "AGENT_UNAVAILABLE", "Agent did not return guest network state"));
            return;
        }

        GetVmGuestNetworkStateAnswer answer = (GetVmGuestNetworkStateAnswer) rawAnswer;
        for (Map.Entry<String, VMInstanceVO> entry : vmsByName.entrySet()) {
            String vmName = entry.getKey();
            VMInstanceVO vm = entry.getValue();
            VmGuestNetworkState state = answer.getStates().get(vmName);
            String error = answer.getErrors().get(vmName);
            recordCapability(vm.getId(), state, now);
            if (state == null) {
                recordFailure(vm, null, "COLLECTION_FAILED",
                        error == null ? "Agent returned no guest network state" : error);
                continue;
            }
            // An overall UNAVAILABLE status can represent a valid, structured observation
            // where only the requested section failed. Persist it so the state service can
            // retain successful sections and the scheduler can back off only failed work.
            Date observedAt = state.getObservedAt() > 0 ? new Date(state.getObservedAt()) : new Date(now);
            Map<String, VmGuestNetworkSectionStatus> collectedSectionStatuses =
                    new LinkedHashMap<>(state.getSectionStatuses());
            VmGuestNetworkStateVO previous = stateService.findByVmId(vm.getId());
            boolean fingerprintChanged = previous != null
                    && previous.getCapabilityHash() != null
                    && !previous.getCapabilityHash().equals(state.getCapabilityHash());
            try {
                stateService.persistSuccess(vm.getId(), state, observedAt);
                state.setSectionStatuses(collectedSectionStatuses);
                recordSectionSchedules(vm.getId(), state, observedAt, now);
                if (fingerprintChanged && scheduleService != null) {
                    scheduleService.invalidateFailedSections(vm.getId(), observedAt);
                }
            } catch (RuntimeException e) {
                LOGGER.warn("Unable to persist guest network state for VM [{}]", vm.getId(), e);
                recordFailure(vm, state, "PERSISTENCE_FAILED",
                        e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            }
        }
    }

    Map<Long, List<VMInstanceVO>> selectDueVmsByHost(long now) {
        List<VMInstanceVO> running = vmInstanceDao.listByTypeAndState(
                VirtualMachine.Type.User, VirtualMachine.State.Running);
        Map<Long, List<VMInstanceVO>> result = new LinkedHashMap<>();
        if (running == null) {
            return result;
        }
        Set<Long> allowedHostIds = parseScopeIds(getHostIdScope(), HOST_IDS.key());
        Set<Long> allowedZoneIds = parseScopeIds(getZoneIdScope(), ZONE_IDS.key());
        List<VMInstanceVO> eligible = new ArrayList<>();
        running.stream()
                .filter(vm -> vm.getHostId() != null)
                .filter(vm -> HypervisorType.KVM.equals(vm.getHypervisorType()))
                .filter(vm -> isInScope(vm.getHostId(), allowedHostIds))
                .filter(vm -> isInScope(vm.getDataCenterId(), allowedZoneIds))
                .sorted(Comparator.comparingLong(VMInstanceVO::getId))
                .forEach(eligible::add);
        if (scheduleService == null) {
            eligible.stream()
                    .filter(vm -> policy.isInterfaceDue(vm.getId(), now)
                            || policy.isRouteDue(vm.getId(), now)
                            || policy.isDnsDue(vm.getId(), now))
                    .forEach(vm -> result.computeIfAbsent(
                            vm.getHostId(), ignored -> new ArrayList<>()).add(vm));
            return result.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .limit(Math.max(1, getMaxHostsPerCycle()))
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                            Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
        }
        List<Long> vmIds = new ArrayList<>();
        eligible.forEach(vm -> vmIds.add(vm.getId()));
        Map<Long, VmGuestNetworkScheduleService.DueWork> due =
                scheduleService.findDueWork(vmIds, new Date(now));
        Map<Long, Date> oldestDueByHost = new HashMap<>();
        for (VMInstanceVO vm : eligible) {
            VmGuestNetworkScheduleService.DueWork work = due.get(vm.getId());
            if (work == null) {
                continue;
            }
            result.computeIfAbsent(vm.getHostId(), ignored -> new ArrayList<>()).add(vm);
            Date current = oldestDueByHost.get(vm.getHostId());
            if (current == null || work.getOldestDueAt().before(current)) {
                oldestDueByHost.put(vm.getHostId(), work.getOldestDueAt());
            }
        }
        return result.entrySet().stream()
                .sorted((left, right) -> {
                    int dueComparison = oldestDueByHost.get(left.getKey())
                            .compareTo(oldestDueByHost.get(right.getKey()));
                    return dueComparison != 0 ? dueComparison
                            : Long.compare(left.getKey(), right.getKey());
                })
                .limit(Math.max(1, getMaxHostsPerCycle()))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));
    }

    private Set<Long> parseScopeIds(String configuredValue, String key) {
        if (configuredValue == null || configuredValue.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (String value : configuredValue.split(",")) {
            try {
                long id = Long.parseLong(value.trim());
                if (id > 0) {
                    result.add(id);
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Ignoring invalid ID [{}] in guest network collector setting [{}]", value, key);
            }
        }
        if (result.isEmpty()) {
            result.add(Long.MIN_VALUE);
            LOGGER.warn("Guest network collector setting [{}] contains no valid IDs; collection is denied", key);
        }
        return result;
    }

    private boolean isInScope(long id, Set<Long> allowedIds) {
        return allowedIds.isEmpty() || allowedIds.contains(id);
    }

    private void reconcileTrackedVmStates() {
        Date now = new Date();
        for (Long vmId : policy.trackedVmIds()) {
            VMInstanceVO vm = vmInstanceDao.findById(vmId);
            if (vm == null) {
                stateService.removeByVmId(vmId);
                policy.remove(vmId);
            } else if (vm.getState() != VirtualMachine.State.Running) {
                stateService.markStopped(vmId, now);
                policy.remove(vmId);
            }
        }
    }

    private void executeTasks(List<Callable<Void>> tasks) {
        if (tasks.isEmpty()) {
            return;
        }
        try {
            if (collectionExecutor == null) {
                for (Callable<Void> task : tasks) {
                    task.call();
                }
                return;
            }
            List<Future<Void>> futures = collectionExecutor.invokeAll(
                    tasks, Math.max(1, getCycleTimeout()), TimeUnit.SECONDS);
            for (Future<Void> future : futures) {
                if (future.isCancelled()) {
                    LOGGER.warn("Guest network host collection was cancelled by the cycle timeout");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.warn("Guest network collection cycle failed", e);
        }
    }

    private Map<String, String> buildCloudNicMap(long vmId) {
        List<NicVO> nics = nicDao.listByVmId(vmId);
        if (nics == null) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (NicVO nic : nics) {
            if (NetUtils.isValidMac(nic.getMacAddress())) {
                result.put(NetUtils.standardizeMacAddress(nic.getMacAddress()), nic.getUuid());
            }
        }
        return result;
    }

    private void recordCapability(long vmId, VmGuestNetworkState state, long now) {
        if (state == null || !state.getCapabilities().containsKey(INTERFACE_CAPABILITY)) {
            return;
        }
        policy.recordInterfaceCapability(vmId, state.getCapabilities().get(INTERFACE_CAPABILITY),
                now, getCapabilityCacheTtl());
    }

    private void recordFailure(VMInstanceVO vm, VmGuestNetworkState state, String code, String details) {
        long now = System.currentTimeMillis();
        try {
            stateService.persistFailure(vm.getId(), state, code, details, new Date(now));
        } catch (RuntimeException e) {
            LOGGER.warn("Unable to persist guest network collection failure for VM [{}]", vm.getId(), e);
        } finally {
            if (scheduleService != null) {
                scheduleService.fail(vm.getId(), code, details, new Date(now),
                        getInterfaceInterval(), getRouteInterval(), getDnsInterval(),
                        getJitterPercent(), getFailureBackoffMax());
            } else if (policy.isInterfaceDue(vm.getId(), now)) {
                policy.recordInterfaceFailure(vm.getId(), now, getInterfaceInterval(),
                        getFailureBackoffMax(), getJitterPercent());
            }
            if (scheduleService == null && policy.isRouteDue(vm.getId(), now)) {
                policy.recordRouteFailure(vm.getId(), now, getRouteInterval(),
                        getFailureBackoffMax(), getJitterPercent());
            }
            if (scheduleService == null && policy.isDnsDue(vm.getId(), now)) {
                policy.recordDnsFailure(vm.getId(), now, getDnsInterval(),
                        getFailureBackoffMax(), getJitterPercent());
            }
        }
    }

    private void recordSectionSchedules(long vmId,
            VmGuestNetworkState state, Date observedAt, long scheduleNow) {
        if (scheduleService != null) {
            scheduleService.complete(vmId, state, observedAt,
                    getInterfaceInterval(), getRouteInterval(), getDnsInterval(),
                    getJitterPercent(), getFailureBackoffMax());
            return;
        }
        long now = scheduleNow;
        Map<String, VmGuestNetworkSectionStatus> sectionStatuses = state.getSectionStatuses();
        VmGuestNetworkSectionStatus interfaceStatus = sectionStatuses.get("interfaces");
        if (interfaceStatus != null && !"NOT_DUE".equals(interfaceStatus.getStatus())) {
            if (isSuccessfulSection(interfaceStatus)) {
                policy.recordInterfaceSuccess(vmId, now, getInterfaceInterval(), getJitterPercent());
            } else {
                policy.recordInterfaceFailure(vmId, now, getInterfaceInterval(),
                        getFailureBackoffMax(), getJitterPercent());
            }
        }
        VmGuestNetworkSectionStatus routeStatus = sectionStatuses.get("routes");
        if (routeStatus != null && !"NOT_DUE".equals(routeStatus.getStatus())) {
            if (isSuccessfulSection(routeStatus)) {
                policy.recordRouteSuccess(vmId, now, getRouteInterval(), getJitterPercent());
            } else {
                policy.recordRouteFailure(vmId, now, getRouteInterval(),
                        getFailureBackoffMax(), getJitterPercent());
            }
        }
        VmGuestNetworkSectionStatus dnsStatus = sectionStatuses.get("dns");
        if (dnsStatus != null && !"NOT_DUE".equals(dnsStatus.getStatus())) {
            if (isSuccessfulSection(dnsStatus)) {
                policy.recordDnsSuccess(vmId, now, getDnsInterval(), getJitterPercent());
            } else {
                policy.recordDnsFailure(vmId, now, getDnsInterval(),
                        getFailureBackoffMax(), getJitterPercent());
            }
        }
    }

    private boolean isSuccessfulSection(VmGuestNetworkSectionStatus section) {
        return "OK".equals(section.getStatus())
                || "EMPTY".equals(section.getStatus())
                || "PARTIAL".equals(section.getStatus());
    }

    private void runCycleSafely() {
        try {
            runCycle();
        } catch (Throwable e) {
            LOGGER.warn("Unexpected guest network collector failure", e);
        }
    }

    protected boolean isEnabled() {
        return ENABLED.value();
    }

    protected String getHostIdScope() {
        return HOST_IDS.value();
    }

    protected String getZoneIdScope() {
        return ZONE_IDS.value();
    }

    protected int getInterfaceInterval() {
        return Math.max(1, INTERFACE_INTERVAL.value());
    }

    protected int getDnsInterval() {
        return Math.max(1, DNS_INTERVAL.value());
    }

    protected int getRouteInterval() {
        return Math.max(1, ROUTE_INTERVAL.value());
    }

    protected int getJitterPercent() {
        return Math.min(50, Math.max(0, JITTER_PERCENT.value()));
    }

    protected int getMaxConcurrentHosts() {
        return Math.max(1, MAX_CONCURRENT_HOSTS.value());
    }

    protected int getMaxConcurrentVmsPerHost() {
        return Math.max(1, MAX_CONCURRENT_VMS_PER_HOST.value());
    }

    protected int getMaxVmsPerHostCycle() {
        return Math.max(1, MAX_VMS_PER_HOST_CYCLE.value());
    }

    protected int getMaxHostsPerCycle() {
        return Math.max(1, MAX_HOSTS_PER_CYCLE.value());
    }

    protected int getFailureBackoffMax() {
        int longestInterval = Math.max(getInterfaceInterval(),
                Math.max(getDnsInterval(), getRouteInterval()));
        return Math.max(longestInterval, FAILURE_BACKOFF_MAX.value());
    }

    protected int getCapabilityCacheTtl() {
        return Math.max(1, CAPABILITY_CACHE_TTL.value());
    }

    protected int getCommandTimeout() {
        return Math.max(1, COMMAND_TIMEOUT.value());
    }

    protected int getCycleTimeout() {
        return Math.max(1, CYCLE_TIMEOUT.value());
    }

    protected boolean isExecFallbackEnabled() {
        return EXEC_FALLBACK_ENABLED.value();
    }

    protected int getExecOutputLimitBytes() {
        return Math.max(1024, EXEC_OUTPUT_LIMIT_BYTES.value());
    }

    protected GlobalLock getCollectorGlobalLock() {
        return GlobalLock.getInternLock(COLLECTOR_GLOBAL_LOCK);
    }

    @Override
    public String getConfigComponentName() {
        return VmGuestNetworkCollector.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                ENABLED, HOST_IDS, ZONE_IDS, INTERFACE_INTERVAL, DNS_INTERVAL, ROUTE_INTERVAL, JITTER_PERCENT,
                MAX_CONCURRENT_HOSTS, MAX_CONCURRENT_VMS_PER_HOST, MAX_VMS_PER_HOST_CYCLE,
                MAX_HOSTS_PER_CYCLE, FAILURE_BACKOFF_MAX, CAPABILITY_CACHE_TTL,
                COMMAND_TIMEOUT, CYCLE_TIMEOUT, EXEC_FALLBACK_ENABLED, EXEC_OUTPUT_LIMIT_BYTES
        };
    }
}
