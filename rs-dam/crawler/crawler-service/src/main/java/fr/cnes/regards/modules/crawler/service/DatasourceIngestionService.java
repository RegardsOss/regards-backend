/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.crawler.dao.IDatasourceIngestionRepository;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionResult;
import fr.cnes.regards.modules.crawler.domain.IngestionStatus;
import fr.cnes.regards.modules.crawler.service.exception.NotFinishedException;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import fr.cnes.regards.modules.indexer.dao.BulkSaveLightResult;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service to handle {@link DatasourceIngestion}
 *
 * @author oroussel
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class DatasourceIngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceIngestionService.class);

    private static final int MAX_NOTIFICATION_LENGTH = 512;

    /**
     * Only used to delete all data objects from a removed datasource
     */
    @Autowired
    private IEsRepository esRepos;

    @Autowired
    private IDatasourceIngestionRepository dsIngestionRepos;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private INotificationClient notifClient;

    private final ExecutorService threadPoolExecutor = Executors.newFixedThreadPool(1);

    public void updateAndCleanTenantDatasourceIngestions() {
        String currentTenant = runtimeTenantResolver.getTenant();
        // First, check if all existing datasource plugins are managed
        // Find all current datasource ingestions
        Map<String, DatasourceIngestion> dsIngestionsMap = dsIngestionRepos.findAll()
                                                                           .stream()
                                                                           .collect(Collectors.toMap(DatasourceIngestion::getId,
                                                                                                     Function.identity()));

        // Find all datasource plugins less inactive ones => find all ACTIVE datasource plugins
        List<PluginConfiguration> pluginConfs = pluginService.getPluginConfigurationsByType(IDataSourcePlugin.class)
                                                             .stream()
                                                             .filter(PluginConfiguration::isActive)
                                                             .toList();

        // Add DatasourceIngestion for unmanaged datasource with immediate next planned ingestion date
        pluginConfs.stream()
                   .filter(pluginConf -> !dsIngestionRepos.existsById(pluginConf.getBusinessId()))
                   .map(pluginConf -> dsIngestionRepos.save(new DatasourceIngestion(pluginConf,
                                                                                    OffsetDateTime.now()
                                                                                                  .withOffsetSameInstant(
                                                                                                      ZoneOffset.UTC))))
                   .forEach(dsIngestion -> dsIngestionsMap.put(dsIngestion.getId(), dsIngestion));
        // Remove DatasourceIngestion for removed datasources and plan data objects deletion from Elasticsearch
        dsIngestionsMap.keySet()
                       .stream()
                       .filter(id -> !pluginService.exists(id))
                       .map(id -> this.planDatasourceDataObjectsDeletion(currentTenant, id))
                       .forEach(dsIngestionRepos::deleteById);
        // For previously ingested datasources, compute next planned ingestion date
        pluginConfs.forEach(pluginConf -> {
            try {
                updatePlannedDate(dsIngestionsMap.get(pluginConf.getBusinessId()));
            } catch (RuntimeException | ModuleException | NotAvailablePluginConfigurationException e) {
                LOGGER.error("Cannot compute next ingestion planned date", e);
            }
        });
    }

    /**
     * Find the next ready data source to be ingested and mark it at "STARTED" in a transaction
     */
    public Optional<String> pickAndStartDatasourceIngestion() {
        Optional<String> startedDatasourceIngestionId = Optional.empty();
        Optional<DatasourceIngestion> dsIngestionOpt = dsIngestionRepos.findNextReady(OffsetDateTime.now()
                                                                                                    .withOffsetSameInstant(
                                                                                                        ZoneOffset.UTC));
        if (dsIngestionOpt.isPresent()) {
            DatasourceIngestion dsIngestion = dsIngestionOpt.get();
            // Reinit old DatasourceIngestion properties
            dsIngestion.setStackTrace(null);
            dsIngestion.setSavedObjectsCount(0);
            dsIngestion.setInErrorObjectsCount(0);
            dsIngestion.setStatus(IngestionStatus.STARTED);
            dsIngestion = dsIngestionRepos.save(dsIngestion);
            startedDatasourceIngestionId = Optional.of(dsIngestion.getId());
        }
        return startedDatasourceIngestionId;
    }

    /**
     * Launch ingestion associated to the given {@link DatasourceIngestion}
     */
    public void updateIngesterResult(String dsIngestionId, IngestionResult summary) {
        Optional<DatasourceIngestion> oDsIngestion = dsIngestionRepos.findById(dsIngestionId);
        if (oDsIngestion.isPresent()) {
            DatasourceIngestion dsIngestion = oDsIngestion.get();
            // dsIngestion.stackTrace has been updated by handleMessageEvent transactional method
            if (summary.getInErrorObjectsCount() > 0) {
                dsIngestion.setStatus(IngestionStatus.FINISHED_WITH_WARNINGS);
            } else {
                dsIngestion.setStatus(IngestionStatus.FINISHED);
            }
            dsIngestion.setSavedObjectsCount(summary.getSavedObjectsCount());
            dsIngestion.setInErrorObjectsCount(summary.getInErrorObjectsCount());
            dsIngestion.setLastIngestDate(summary.getDate());
            // To avoid redoing an ingestion in this "do...while" (must be at next call to manage)
            dsIngestion.setNextPlannedIngestDate(null);
            // To avoid redoing an ingestion from beginning in case where plugin are date optimized (or id optimized)
            if (summary.getLastEntityDate() != null) {
                dsIngestion.setLastEntityDate(summary.getLastEntityDate(), summary.getPenultimateLastEntityDate());
            }
            if (summary.getLastId() != null) {
                dsIngestion.setLastId(summary.getLastId(), summary.getPreviousLastId());
            }
            // Save ingestion status
            sendNotificationSummary(dsIngestionRepos.save(dsIngestion));
        } else {
            LOGGER.warn("Unable to find datasource with id {} to set indexation results", dsIngestionId);
        }
    }

    public void setInactive(String datasourceId, String cause) {
        Optional<DatasourceIngestion> oDsIngestion = dsIngestionRepos.findById(datasourceId);
        if (oDsIngestion.isPresent()) {
            DatasourceIngestion dsIngestion = oDsIngestion.get();
            dsIngestion.setStatus(IngestionStatus.INACTIVE);
            dsIngestion.setStackTrace(cause);
            dsIngestion.setNextPlannedIngestDate(null);
            sendNotificationSummary(dsIngestionRepos.save(dsIngestion));
        } else {
            LOGGER.warn("Unable to find datasource with id {} to set status to inactive", datasourceId);
        }
    }

    public void setError(String dsIngestionId, String cause) {
        Optional<DatasourceIngestion> oDsIngestion = dsIngestionRepos.findById(dsIngestionId);
        if (oDsIngestion.isPresent()) {
            DatasourceIngestion dsIngestion = oDsIngestion.get();
            // Set Status to Error... (and status date)
            dsIngestion.setStatus(IngestionStatus.ERROR);
            // and log stack trace into database
            String stackTrace = dsIngestion.getStackTrace() == null ?
                cause :
                dsIngestion.getStackTrace() + "\n" + cause;
            dsIngestion.setStackTrace(stackTrace);
            dsIngestion.setNextPlannedIngestDate(null);
            sendNotificationSummary(dsIngestionRepos.save(dsIngestion));
        } else {
            LOGGER.warn("Unable to find datasource with id {} to set error={}", dsIngestionId, cause);
        }
    }

    public void setNotFinished(String dsIngestionId, NotFinishedException notFinishedException) {
        CrawlingCursor errorCursor = notFinishedException.getErrorCursor();
        BulkSaveLightResult partialSaveResult = notFinishedException.getSaveResult();
        Throwable notFinishedCause = notFinishedException.getCause();
        setNotFinished(dsIngestionId, errorCursor, partialSaveResult, notFinishedCause);
    }

    private void setNotFinished(String dsIngestionId,
                                CrawlingCursor errorCursor,
                                BulkSaveLightResult partialSaveResult,
                                Throwable notFinishedCause) {
        Optional<DatasourceIngestion> oDsIngestion = dsIngestionRepos.findById(dsIngestionId);
        if (oDsIngestion.isPresent()) {
            DatasourceIngestion dsIngestion = oDsIngestion.get();
            dsIngestion.setStatus(IngestionStatus.NOT_FINISHED);
            dsIngestion.setCursor(errorCursor);
            // and log stack trace into database
            StringWriter sw = new StringWriter();
            notFinishedCause.printStackTrace(new PrintWriter(sw));
            String stackTrace = dsIngestion.getStackTrace() == null ?
                sw.toString() :
                dsIngestion.getStackTrace() + "\n" + sw;
            dsIngestion.setStackTrace(stackTrace);
            dsIngestion.setSavedObjectsCount(partialSaveResult.getSavedDocsCount());
            dsIngestion.setInErrorObjectsCount(partialSaveResult.getInErrorDocsCount());
            dsIngestion.setNextPlannedIngestDate(null);
            sendNotificationSummary(dsIngestionRepos.save(dsIngestion));
        } else {
            LOGGER.warn("Unable to find datasource with id {} to set status to not finished", dsIngestionId);
        }
    }

    public void addMessageToStackTrace(String dsId, String newMessage) {
        Optional<DatasourceIngestion> dsiOpt = dsIngestionRepos.findById(dsId);
        if (dsiOpt.isPresent()) {
            DatasourceIngestion dsi = dsiOpt.get();
            // Limit stack trace size in database
            if (dsi.getStackTrace() != null && dsi.getStackTrace().length() < 10_000) {
                dsi.setStackTrace(dsi.getStackTrace() == null ? newMessage : dsi.getStackTrace() + "\n" + newMessage);
            } else {
                dsi.setStackTrace(newMessage);
            }
            dsIngestionRepos.save(dsi);
        }
    }

    /**
     * Create a task to launch datasource data objects deletion later (use a thread pool of size 1)
     */
    private String planDatasourceDataObjectsDeletion(String tenant, String dataSourceId) {
        threadPoolExecutor.submit(() -> {
            try {
                LOGGER.info("Removing all data objects associated to data source {}...", dataSourceId);
                long deletedCount = esRepos.deleteByQuery(tenant,
                                                          ICriterion.eq("dataSourceId",
                                                                        dataSourceId,
                                                                        StringMatchType.KEYWORD));
                LOGGER.info("...{} data objects removed.", deletedCount);
            } catch (RsRuntimeException e) {
                LOGGER.error("...Cannot remove data objects associated to data source", e);
            }
        });
        return dataSourceId;
    }

    /**
     * Compute next ingestion planned date if needed in its own transaction to prevent making
     * updateAndCleanTenantDatasourceIngestions failing and rollbacking its transaction
     *
     * @throws NotAvailablePluginConfigurationException from {@link fr.cnes.regards.framework.modules.plugins.service.PluginService#getPlugin(String, IPluginParam...)}
     * @throws ModuleException                          from {@link fr.cnes.regards.framework.modules.plugins.service.PluginService#getPlugin(String, IPluginParam...)}
     */
    @Transactional(noRollbackFor = { ModuleException.class, NotAvailablePluginConfigurationException.class })
    private void updatePlannedDate(DatasourceIngestion dsIngestion)
        throws ModuleException, NotAvailablePluginConfigurationException {
        int refreshRate = ((IDataSourcePlugin) pluginService.getPlugin(dsIngestion.getId())).getRefreshRate();
        // Take into account ONLY data source with null nextPlannedIngestDate
        if (dsIngestion.getNextPlannedIngestDate() == null) {
            switch (dsIngestion.getStatus()) {
                case ERROR, NOT_FINISHED -> {
                    // ERROR: last ingest in error, do not launch as soon as possible, if it is the only ingestion, user
                    // may not have time to see the error
                    // NOT_FINISHED: last ingest hasn't finished because of Datasource or Elasticsearch, no need to
                    // relaunch now, it will probably fails again
                    OffsetDateTime nextPlannedIngestDate = OffsetDateTime.now()
                                                                         .withOffsetSameInstant(ZoneOffset.UTC)
                                                                         .plus(refreshRate, ChronoUnit.SECONDS);
                    dsIngestion.setNextPlannedIngestDate(nextPlannedIngestDate);
                    dsIngestionRepos.save(dsIngestion);
                }
                case FINISHED, FINISHED_WITH_WARNINGS -> { // last ingest + refreshRate
                    dsIngestion.setNextPlannedIngestDate(dsIngestion.getLastIngestDate()
                                                                    .plus(refreshRate, ChronoUnit.SECONDS));
                    dsIngestionRepos.save(dsIngestion);
                }
                // STARTED: Already in progress
                // NEW: dsIngestion just been created with a next planned date as now() ie launch as soon as possible
                case STARTED, NEW -> {
                }
                default -> {
                }
            }
        }
    }

    private void sendNotificationSummary(DatasourceIngestion dsIngestion) {
        // Send admin notification for ingestion ends if something as been done
        if ((dsIngestion.getSavedObjectsCount() != 0) || (dsIngestion.getInErrorObjectsCount() != 0)) {
            String title = String.format("%s indexation ends.", dsIngestion.getLabel());
            String stackTrace = dsIngestion.getStackTrace();
            if ((dsIngestion.getStackTrace() != null) && (stackTrace.length() > MAX_NOTIFICATION_LENGTH)) {
                stackTrace = dsIngestion.getStackTrace()
                                        .substring(0,
                                                   Math.min(dsIngestion.getStackTrace().length(),
                                                            MAX_NOTIFICATION_LENGTH)) + " ... [truncated]";
            }
            switch (dsIngestion.getStatus()) {
                case ERROR -> notifClient.notify(String.format("Indexation error. Cause : %s", stackTrace),
                                                 title,
                                                 NotificationLevel.ERROR,
                                                 DefaultRole.PROJECT_ADMIN);
                case FINISHED_WITH_WARNINGS -> notifClient.notify(String.format(
                    "Indexation ends with %s new indexed objects and %s errors.",
                    dsIngestion.getSavedObjectsCount(),
                    dsIngestion.getInErrorObjectsCount()), title, NotificationLevel.WARNING, DefaultRole.PROJECT_ADMIN);
                case NOT_FINISHED -> notifClient.notify(String.format("""
                                                                          Indexation ends with %s new indexed objects and %s errors but is not completely terminated.
                                                                                     Something went wrong concerning datasource or Elasticsearch.
                                                                          Associated datasets haven't been updated, ingestion may be manually re-scheduled
                                                                          to be launched as soon as possible or will continue at its planned date
                                                                          """,
                                                                      dsIngestion.getSavedObjectsCount(),
                                                                      dsIngestion.getInErrorObjectsCount()),
                                                        title,
                                                        NotificationLevel.WARNING,
                                                        DefaultRole.PROJECT_ADMIN);
                default -> notifClient.notify(String.format(
                    "Indexation finished. %s new objects indexed. %s objects in error.",
                    dsIngestion.getSavedObjectsCount(),
                    dsIngestion.getInErrorObjectsCount()), title, NotificationLevel.INFO, DefaultRole.PROJECT_ADMIN);
            }
        }
    }
}
