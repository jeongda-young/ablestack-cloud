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
package com.cloud.dr.health;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.dr.DrConstants;
import com.cloud.dr.DrSiteHealthCheckHistoryService;
import com.cloud.dr.DrSiteService;
import com.cloud.dr.DrSiteVO;
import com.cloud.dr.cluster.DisasterRecoveryClusterService;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;

public class DrSiteHealthCheckScheduler extends ManagerBase implements Configurable {
    private static final int GLOBAL_LOCK_TIMEOUT_SECONDS = 1;
    private static final long INITIAL_DELAY_SECONDS = 60L;
    private static final long CLEANUP_BATCH_SIZE = 500L;

    public static final ConfigKey<Boolean> DrSiteHealthCheckSchedulerEnabled = new ConfigKey<>("Advanced", Boolean.class,
            "dr.site.health.check.enabled",
            "true",
            "Indicates whether Cross Hypervisor DR site health checks should run periodically.",
            false);

    public static final ConfigKey<Integer> DrSiteHealthCheckSchedulerInterval = new ConfigKey<>("Advanced", Integer.class,
            "dr.site.health.check.interval",
            "300",
            "Interval in seconds for periodic Cross Hypervisor DR site health checks.",
            false);

    public static final ConfigKey<Integer> DrSiteHealthCheckSchedulerBatchSize = new ConfigKey<>("Advanced", Integer.class,
            "dr.site.health.check.batch.size",
            "25",
            "Maximum number of Cross Hypervisor DR sites checked in one scheduler tick.",
            false);

    public static final ConfigKey<Integer> DrSiteHealthCheckHistoryRetentionDays = new ConfigKey<>("Advanced", Integer.class,
            "dr.site.health.check.history.retention.days",
            "30",
            "Number of days to retain Cross Hypervisor DR site health check history.",
            false);

    @Inject
    private DrSiteService drSiteService;
    @Inject
    private DrSiteHealthCheckHistoryService drSiteHealthCheckHistoryService;

    private ScheduledExecutorService executor;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        executor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DrSiteHealthCheckScheduler"));
        return true;
    }

    @Override
    public boolean start() {
        int interval = Math.max(60, DrSiteHealthCheckSchedulerInterval.value());
        if (executor != null) {
            executor.scheduleWithFixedDelay(new HealthCheckTask(), INITIAL_DELAY_SECONDS, interval, TimeUnit.SECONDS);
            logger.info(String.format("Started DR site health check scheduler with interval %s seconds", interval));
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
        return true;
    }

    @Override
    public String getConfigComponentName() {
        return DrSiteHealthCheckScheduler.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                DrSiteHealthCheckSchedulerEnabled,
                DrSiteHealthCheckSchedulerInterval,
                DrSiteHealthCheckSchedulerBatchSize,
                DrSiteHealthCheckHistoryRetentionDays
        };
    }

    private final class HealthCheckTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            if (!Boolean.TRUE.equals(DisasterRecoveryClusterService.DisasterRecoveryServiceEnabled.value())
                    || !Boolean.TRUE.equals(DrSiteHealthCheckSchedulerEnabled.value())) {
                return;
            }
            GlobalLock scanLock = GlobalLock.getInternLock("DrSiteHealthCheckScheduler");
            try {
                if (scanLock.lock(GLOBAL_LOCK_TIMEOUT_SECONDS)) {
                    try {
                        runHealthChecks();
                        cleanupHistory();
                    } finally {
                        scanLock.unlock();
                    }
                }
            } catch (RuntimeException e) {
                logger.warn("Failed to run DR site health check scheduler", e);
            } finally {
                scanLock.releaseRef();
            }
        }
    }

    private void runHealthChecks() {
        if (drSiteService == null) {
            return;
        }
        List<DrSiteVO> sites = drSiteService.listSites();
        if (sites == null || sites.isEmpty()) {
            return;
        }
        int batchSize = Math.max(1, DrSiteHealthCheckSchedulerBatchSize.value());
        int checked = 0;
        for (DrSiteVO site : sites) {
            if (checked >= batchSize) {
                break;
            }
            if (site == null || site.getRemoved() != null || !DrConstants.ADMIN_STATE_ENABLED.equals(site.getState())) {
                continue;
            }
            try {
                drSiteService.checkSite(site.getId(), true, DrConstants.HEALTH_TRIGGER_SCHEDULED, null);
                checked++;
            } catch (RuntimeException e) {
                logger.warn(String.format("Failed to check DR site %s in scheduler", site != null ? site.getId() : null), e);
            }
        }
    }

    private void cleanupHistory() {
        if (drSiteHealthCheckHistoryService == null) {
            return;
        }
        int retentionDays = DrSiteHealthCheckHistoryRetentionDays.value();
        if (retentionDays <= 0) {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -retentionDays);
        Date cutoff = calendar.getTime();
        int removed = drSiteHealthCheckHistoryService.cleanupOlderThan(cutoff, CLEANUP_BATCH_SIZE);
        if (removed > 0 && logger.isDebugEnabled()) {
            logger.debug(String.format("Removed %s old DR site health check history rows", removed));
        }
    }
}
