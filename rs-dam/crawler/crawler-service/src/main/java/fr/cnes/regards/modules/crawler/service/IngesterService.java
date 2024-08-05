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
package fr.cnes.regards.modules.crawler.service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.module.rest.exception.InactiveDatasourceException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.crawler.dao.IDatasourceIngestionRepository;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionResult;
import fr.cnes.regards.modules.crawler.domain.IngestionStatus;
import fr.cnes.regards.modules.crawler.service.event.DataSourceMessageEvent;
import fr.cnes.regards.modules.crawler.service.exception.FirstFindException;
import fr.cnes.regards.modules.crawler.service.exception.NotFinishedException;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import fr.cnes.regards.modules.model.gson.ModelJsonReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Component used to schedule new {@link DatasourceIngestion} to ingest features in ES catalog for each tenants
 *
 * @author oroussel
 * @author Sébastien Binda
 */
@Component
public class IngesterService implements IHandler<PluginConfEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngesterService.class);

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
    private IDatasourceIngesterService datasourceIngester;

    /**
     * Boolean indicating whether or not crawler service is in "consume only" mode (to be used by tests only)
     */
    private boolean consumeOnlyMode = false;

    private CrawlerService crawlerService;

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
                this.manage();

            }
        } catch (RuntimeException t) {
            LOGGER.error("Cannot manage plugin conf event message", t);
        }
    }

    /**
     * By default, launched 1 mn after last one. BUT this method is also executed each time a datasource is created
     * Initial delay of 5 mn to avoid been launched too soon.
     */
    @Scheduled(initialDelayString = "${regards.ingester.rate.init.ms:300000}",
               fixedDelayString = "${regards.ingester.rate.ms:60000}")
    public void manage() {
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
                // First, update all DatasourceIngestions of all tenants (to reflect all datasource plugin configurations
                // states and to update nextPlannedIngestDate)
                for (String tenant : tenantResolver.getAllActiveTenants()) {
                    runtimeTenantResolver.forceTenant(tenant);
                    dsIngestionService.updateAndCleanTenantDatasourceIngestions();
                }
                // Then ingest...
                boolean atLeastOneIngestionDone;
                do {
                    atLeastOneIngestionDone = false;
                    for (String tenant : tenantResolver.getAllActiveTenants()) {
                        runtimeTenantResolver.forceTenant(tenant);
                        // Pick an available dsIngestion marking it as STARTED if present
                        Optional<String> dsIngestionOpt = dsIngestionService.pickAndStartDatasourceIngestion();
                        if (dsIngestionOpt.isPresent()) {
                            String dsId = dsIngestionOpt.get();
                            atLeastOneIngestionDone = true;
                            try {
                                Optional<IngestionResult> summary = ingest(dsId);
                                summary.ifPresent(ingestionResult -> dsIngestionService.updateIngesterResult(dsId,
                                                                                                             ingestionResult));
                            } catch (Exception e) {
                                // Catch all other possible exceptions to set ingestion to error status
                                setDatasourceIngestInError(dsId, e);
                            }
                        }
                    }
                    // At least one ingestion has to be done while looping through all tenants
                } while (atLeastOneIngestionDone);
                // set doItAgain to false in all cases and redo if asked to (this means a datasource has been created
                // or updated while manage() method was currently executing
            } while (doItAgain.getAndSet(false));
        } finally { // In all cases, set managing to
            managing.set(false);
        }
        LOGGER.info("...IngesterService.manage() ended.");
    }

    private Optional<IngestionResult> ingest(String dsId) {
        Optional<IngestionResult> summary = Optional.empty();
        try {
            summary = datasourceIngester.ingest(dsId);
        } catch (InactiveDatasourceException ide) {
            LOGGER.warn(ide.getMessage());
            dsIngestionService.setInactive(dsId, ide.getMessage());
        } catch (ModuleException | FirstFindException e) {
            // ModuleException can only be thrown before we start reading the datasource so it's simply an error
            setDatasourceIngestInError(dsId, e);
        } catch (NotFinishedException nfe) {
            LOGGER.error(nfe.getMessage(), nfe);
            dsIngestionService.setNotFinished(dsId, nfe);
        }
        return summary;
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
                        String stackTrace = datasourceIngestion.getStackTrace() != null ? String.format("%s%n%s",
                                                                                                        datasourceIngestion.getStackTrace(),
                                                                                                        errorMessage) : errorMessage;
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

    private void setDatasourceIngestInError(String dsId, Exception e) {
        LOGGER.error(e.getMessage(), e);
        try (StringWriter sw = new StringWriter()) {
            e.printStackTrace(new PrintWriter(sw));
            dsIngestionService.setError(dsId, sw.toString());
        } catch (IOException e1) {
            LOGGER.error(e.getMessage(), e);
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
