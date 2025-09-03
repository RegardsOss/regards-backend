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
package fr.cnes.regards.modules.crawler.service.service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.crawler.dao.IDatasourceIngestionRepository;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionStatus;
import fr.cnes.regards.modules.crawler.service.event.DataSourceMessageEvent;
import fr.cnes.regards.modules.crawler.service.job.CrawlOneDatasourceJob;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import fr.cnes.regards.modules.model.gson.ModelJsonReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Component used to schedule new {@link DatasourceIngestion} to ingest features in ES catalog for each tenants
 *
 * @author oroussel
 * @author Sébastien Binda
 */
@Component
public class CrawlerCreatorService implements IHandler<PluginConfEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerCreatorService.class);

    /**
     * An atomic boolean used to determine whether manage() method is currently executing (and avoid launching it
     * in parallel)
     */
    public static final AtomicBoolean managing = new AtomicBoolean(false);

    public static final AtomicBoolean startup = new AtomicBoolean(true);

    /**
     * An atomic boolean permitting to take into account a new data source creation or update while managing current ones
     * (or inverse)
     */
    private static final AtomicBoolean doItAgain = new AtomicBoolean(false);

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private DatasourceIngestionService dsIngestionService;

    @Autowired
    private IDatasourceIngestionRepository datasourceIngestionRepository;

    @Autowired
    private IJobInfoService jobInfoService;

    /**
     * Boolean indicating whether or not crawler service is in "consume only" mode (to be used by tests only)
     */
    private boolean consumeOnlyMode = false;

    @EventListener
    public void handleApplicationReadyEvent(ModelJsonReadyEvent event) {
        subscriber.subscribeTo(PluginConfEvent.class, this);
        // Clean started process if any. There should be no started crawling process at startup as the dam
        // service is not scalable.
        try {
            forceRunningDataSourcesToErrorStatus();
        } finally {
            startup.set(false);
        }
    }

    /**
     * Receiving a message from crawler
     */
    @EventListener
    public void handleMessageEvent(DataSourceMessageEvent event) {
        runtimeTenantResolver.forceTenant(event.getTenant());
        dsIngestionService.addMessageToStackTrace(event.getDataSourceId(), event.getMessage());
    }

    @Override
    public void handle(String tenant, PluginConfEvent event) {
        try {
            runtimeTenantResolver.forceTenant(tenant);
            // If it concerns a data source, manage it
            if (event.getPluginTypes().contains(IDataSourcePlugin.class.getName()) && !this.consumeOnlyMode) {
                this.manageCrawlingForAllTenants();
            }
        } catch (RuntimeException t) {
            LOGGER.error("Cannot manage plugin conf event message", t);
        }
    }

    @SuppressWarnings("java:S1181") // guarding against plugin errors requires catching all Throwables
    public void manageCrawlingForAllTenants() {
        if (startup.get()) {
            // Service is starting. Wait ...
            return;
        }
        LOGGER.info("IngesterService.manage() called...");
        // if this method is called while currently been executed, doItAgain is set to true and nothing else is done
        if (managing.getAndSet(true)) {
            doItAgain.set(true);
            return;
        }
        try {
            do {
                runDatasourceIngestionsForAllTenants();
                // set doItAgain to false in all cases and redo if asked to (this means a datasource has been created
                // or updated while manage() method was currently executing
            } while (doItAgain.getAndSet(false));
        } finally { // In all cases, set managing to
            managing.set(false);
        }
        LOGGER.info("...IngesterService.manage() ended.");
    }

    private void runDatasourceIngestionsForAllTenants() {
        // First, update all DatasourceIngestions of all tenants (to reflect all datasource plugin configurations
        // states and to update nextPlannedIngestDate)
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            dsIngestionService.updateAndCleanTenantDatasourceIngestions();
        }
        // Then ingest...
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            // Start all ready datasourceIngestion : marking its as STARTED
            List<String> startedDatasourceIngestionIds = dsIngestionService.startAllReadyDatasourceIngestion();
            startedDatasourceIngestionIds.forEach(this::createCrawlJob);
        }
    }

    private void createCrawlJob(String dsId) {
        LOGGER.info("Creating crawl job for datasource with id {}", dsId);
        jobInfoService.createAsQueued(new JobInfo(false,
                                                  0,
                                                  CrawlOneDatasourceJob.buildJobParameters(dsId),
                                                  null,
                                                  CrawlOneDatasourceJob.class.getName()));
    }

    /**
     * Used at service startup to update started datasource to error status.
     */
    private void forceRunningDataSourcesToErrorStatus() {
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                List<DatasourceIngestion> datasources = datasourceIngestionRepository.findAll();
                datasources.forEach(datasourceIngestion -> {
                    if (datasourceIngestion.getStatus() == IngestionStatus.STARTED) {
                        String errorMessage = String.format(
                            "Datasource %s was in started state at service startup. Updating "
                            + "state to error. This datasource crawling will be restarted as "
                            + "soon as possible.",
                            datasourceIngestion.getLabel());
                        LOGGER.error(errorMessage);
                        // Force status to error
                        datasourceIngestion.setStatus(IngestionStatus.ERROR);
                        // Add startup restart message
                        String stackTrace = datasourceIngestion.getStackTrace() != null ?
                            String.format("%s%n%s", datasourceIngestion.getStackTrace(), errorMessage) :
                            errorMessage;
                        datasourceIngestion.setStackTrace(stackTrace);
                        // Update next planed date to now in order to force crawling restart.
                        datasourceIngestion.setNextPlannedIngestDate(OffsetDateTime.now());
                    }
                });
                datasourceIngestionRepository.saveAll(datasources);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    /**
     * Set or unset "consume only" mode where messages are polled but nothing is done
     *
     * @param b true or false (it's a boolean, what do you expect ?)
     */
    public void setConsumeOnlyMode(boolean b) {
        consumeOnlyMode = b;
    }

    /**
     * Ensure that ingestion is not running and prevent it from being run until {@link #releaseIngestionLock()} is called
     *
     * @return true if ingestion is not running and is now locked, false otherwise
     */
    public boolean lockIngestion() {
        return !managing.getAndSet(true);
    }

    /**
     * Release ingestion lock so it can be run again
     */
    public void releaseIngestionLock() {
        if (!managing.getAndSet(false)) {
            LOGGER.error("Error while trying to release ingestion lock in IngesterService : ingestion is not currently"
                         + " locked");
        }
    }

}
