/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.scheduling.support.CronTrigger;

import com.google.common.collect.Maps;
import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.jpa.multitenant.lock.LockingTaskExecutors;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.dump.domain.DumpSettings;
import fr.cnes.regards.framework.modules.dump.service.settings.IDumpSettingsService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;

/**
 * Abstract class to handle dump scheduling
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
    private LockingTaskExecutors lockingTaskExecutors;

    private final Map<String, ScheduledFuture> schedulersByTenant = Maps.newHashMap();

    /**
     * Create schedulers when the application context has been refreshed see {@link ApplicationStartedEvent}
     */
    @EventListener
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
     * @param event to inform of a new connection see {@link TenantConnectionReady}
     */
    @EventListener
    public void onTenantConnectionReady(TenantConnectionReady event) {
        updateScheduler(event.getTenant());
    }

    /**
     * Update the scheduler configured for the tenant
     * Cancel the previous task (if existing) and put a new task
     * @param tenant tenant to be updated
     */
    public void updateScheduler(String tenant) {
        try {
            runtimeTenantResolver.forceTenant(tenant);
            DumpSettings newDumpSettings = dumpSettingsService.retrieve();
            getLogger().info("[DUMP] Updating  {}", getNotificationTitle());
            // cancel the existing scheduler, wait until the end to cancel it
            ScheduledFuture schedulerToRestart = schedulersByTenant.get(tenant);
            if (schedulerToRestart != null) {
                schedulerToRestart.cancel(false);
            }
            // update scheduler only if the module is active
            if (newDumpSettings.isActiveModule()) {
                schedulersByTenant.put(tenant, taskScheduler.schedule(() -> {
                    runtimeTenantResolver.forceTenant(tenant);
                    traceScheduling(tenant, getType());
                    try {
                        lockingTaskExecutors.executeWithLock(getDumpTask(), new LockConfiguration(getLockName(),
                                                                                                  Instant.now()
                                                                                                          .plusSeconds(
                                                                                                                  60L)));
                    } catch (Throwable e) {
                        handleSchedulingError(getType(), getNotificationTitle(), e);
                    } finally {
                        runtimeTenantResolver.clearTenant();
                    }
                }, new CronTrigger(newDumpSettings.getCronTrigger())));
            }
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    /**
     * Update the {@link DumpSettings} and the {@link AbstractDumpScheduler} with the new configuration
     * @param newDumpSettings the new dump configuration
     */
    @RegardsTransactional
    public void updateDumpAndScheduler(DumpSettings newDumpSettings) throws ModuleException {
        // SET ID (only one id is allowed for dumpSettings)
        newDumpSettings.setId();

        // PARAMETER CHECK
        // check cron expression
        String cronTrigger = newDumpSettings.getCronTrigger();
        if (!CronSequenceGenerator.isValidExpression(cronTrigger)) {
            throw new EntityInvalidException(String.format("Cron Expression %s is not valid.", cronTrigger));
        }

        // UPDATE DUMP SETTINGS AND SCHEDULER if they were modified
        if(dumpSettingsService.update(newDumpSettings)) {
            updateScheduler(runtimeTenantResolver.getTenant());
        }
    }

    protected abstract String getLockName();

    protected abstract LockingTaskExecutor.Task getDumpTask();

    protected abstract String getNotificationTitle();

    protected abstract String getType();

    public Map<String, ScheduledFuture> getSchedulersByTenant() {
        return schedulersByTenant;
    }
}
