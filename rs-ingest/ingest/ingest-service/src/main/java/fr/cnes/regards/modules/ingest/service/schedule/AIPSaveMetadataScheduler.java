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
package fr.cnes.regards.modules.ingest.service.schedule;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import fr.cnes.regards.framework.jpa.multitenant.event.spring.TenantConnectionReady;
import fr.cnes.regards.framework.jpa.multitenant.lock.AbstractTaskScheduler;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.domain.dump.DumpSettings;
import fr.cnes.regards.modules.ingest.domain.request.dump.AIPSaveMetadataRequest;
import fr.cnes.regards.modules.ingest.service.dump.AIPSaveMetadataService;
import fr.cnes.regards.modules.ingest.service.dump.IDumpSettingsService;
import static fr.cnes.regards.modules.ingest.service.schedule.SchedulerConstant.AIP_SAVE_METADATA_REQUESTS;

/**
 * Scheduler handle {@link AIPSaveMetadataJobTask}s
 * @author Iliana Ghazali
 */
@Component
public class AIPSaveMetadataScheduler extends AbstractTaskScheduler {

    public static final Logger LOGGER = LoggerFactory.getLogger(AIPSaveMetadataScheduler.class);

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IDumpSettingsService dumpSettingsService;

    @Autowired
    private AIPSaveMetadataService aipSaveMetadataService;

    private Map<String, ScheduledFuture> schedulersByTenant = Maps.newHashMap();


    /**
     * Create schedulers when the application context has been refreshed see {@link ApplicationStartedEvent}
     */
    @EventListener
    public void onApplicationStartedEvent(ApplicationStartedEvent event) {
        initAIPSaveMetaDataJobsSchedulers();
    }

    /**
     * Initialize scheduled {@link AIPSaveMetadataJobTask}s for all tenants to save aip metadata
     */
    public void initAIPSaveMetaDataJobsSchedulers() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                traceScheduling(tenant, AIP_SAVE_METADATA_REQUESTS);
                // check if dump is required
                DumpSettings dumpConf = dumpSettingsService.retrieve();
                if (dumpConf.isActiveModule()) {
                    schedulersByTenant.put(tenant, taskScheduler
                            .schedule(new AIPSaveMetadataJobTask(aipSaveMetadataService, tenant, runtimeTenantResolver),
                                      new CronTrigger(dumpConf.getCronTrigger())));
                }
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Create a new scheduled {@link AIPSaveMetadataJobTask} for the tenant created
     * @param event to inform of a new connection see {@link TenantConnectionReady}
     */
    @EventListener
    public void onTenantConnectionReady(TenantConnectionReady event) {
        String tenant = event.getTenant();
        runtimeTenantResolver.forceTenant(tenant);
        updateScheduler(tenant, dumpSettingsService.retrieve());
    }

    /**
     * Update the scheduler configured for the tenant
     * Cancel the previous {@link AIPSaveMetadataJobTask} (if existing) and put a new task
     * @param tenant tenant to be updated
     */
    public void updateScheduler(String tenant, DumpSettings newDumpSettings) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            runtimeTenantResolver.forceTenant(tenant);
            traceScheduling(tenant, AIP_SAVE_METADATA_REQUESTS);
            // cancel the existing scheduler, wait until the end to cancel it
            ScheduledFuture schedulerToRestart = schedulersByTenant.get(tenant);
            if (schedulerToRestart != null) {
                schedulerToRestart.cancel(false);
            }
            // update scheduler only if the module is active
            if (newDumpSettings.isActiveModule()) {
                schedulersByTenant.put(tenant, taskScheduler
                        .schedule(new AIPSaveMetadataJobTask(aipSaveMetadataService, tenant, runtimeTenantResolver),
                                  new CronTrigger(newDumpSettings.getCronTrigger())));
            }
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    public Map<String, ScheduledFuture> getSchedulersByTenant() {
        return schedulersByTenant;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
