/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnes.regards.framework.modules.dump.service.scheduler;

import com.google.common.collect.Maps;
import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.ILockingTaskExecutors;
import fr.cnes.regards.framework.modules.dump.service.settings.DumpSettingsService;
import fr.cnes.regards.framework.modules.dump.service.settings.IDumpSettingsService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * Abstract class to handle dump scheduling
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public abstract class AbstractDumpScheduler extends AbstractTaskScheduler {

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IDumpSettingsService dumpSettingsService;

    @Autowired
    protected ILockingTaskExecutors lockingTaskExecutors;

    private final Map<String, ScheduledFuture> schedulersByTenant = Maps.newHashMap();

    /**
     * Create schedulers when the application context has been refreshed see {@link ApplicationStartedEvent}
     * Needs to be run after settings initialization (see {@link DumpSettingsService}, hence lower order
     */
    @EventListener
    @Order(10)
    public void onApplicationStartedEvent(ApplicationStartedEvent event) {
        initSchedulers();
    }

    /**
     * Initialize scheduled for all tenants to save feature metadata
     */
    public void initSchedulers() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            updateScheduler(tenant);
        }
    }

    /**
     * Create a new scheduled task for the tenant created
     * Needs to be run after settings initialization (see {@link DumpSettingsService}, hence lower order
     *
     * @param event to inform of a new connection see {@link TenantConnectionReady}
     */
    @EventListener
    @Order(10)
    public void onTenantConnectionReady(TenantConnectionReady event) {
        updateScheduler(event.getTenant());
    }

    /**
     * Update the scheduler configured for the tenant
     * Cancel the previous task (if existing) and put a new task
     *
     * @param tenant tenant to be updated
     */
    public void updateScheduler(String tenant) {
        try {
            runtimeTenantResolver.forceTenant(tenant);
            getLogger().info("[DUMP] Updating  {}", getNotificationTitle());
            // cancel the existing scheduler, wait until the end to cancel it
            ScheduledFuture schedulerToRestart = schedulersByTenant.get(tenant);
            if (schedulerToRestart != null) {
                schedulerToRestart.cancel(false);
            }
            // update scheduler only if the module is active
            if (dumpSettingsService.getDumpParameters().isActiveModule()) {
                schedulersByTenant.put(tenant, taskScheduler.schedule(() -> {
                    runtimeTenantResolver.forceTenant(tenant);
                    traceScheduling(tenant, getType());
                    try {
                        lockingTaskExecutors.executeWithLock(getDumpTask(),
                                                             new LockConfiguration(Instant.now(),
                                                                                   getLockName(),
                                                                                   Duration.ofSeconds(60L),
                                                                                   Duration.ZERO));
                    } catch (Throwable e) {
                        handleSchedulingError(getType(), getNotificationTitle(), e);
                    } finally {
                        runtimeTenantResolver.clearTenant();
                    }
                }, new CronTrigger(dumpSettingsService.getDumpParameters().getCronTrigger())));
            }
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    public void update() {
        updateScheduler(runtimeTenantResolver.getTenant());
    }

    protected abstract String getLockName();

    protected abstract LockingTaskExecutor.Task getDumpTask();

    protected abstract String getNotificationTitle();

    protected abstract String getType();

    public Map<String, ScheduledFuture> getSchedulersByTenant() {
        return schedulersByTenant;
    }
}
