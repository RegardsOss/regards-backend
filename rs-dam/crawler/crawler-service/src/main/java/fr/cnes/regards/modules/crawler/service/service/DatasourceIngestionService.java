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

import fr.cnes.regards.framework.geojson.geometry.Unlocated;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.InactiveDatasourceException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.crawler.dao.IDatasourceIngestionRepository;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionResult;
import fr.cnes.regards.modules.crawler.domain.IngestionStatus;
import fr.cnes.regards.modules.crawler.service.conf.CrawlerPropertiesConfiguration;
import fr.cnes.regards.modules.crawler.service.event.DataSourceMessageEvent;
import fr.cnes.regards.modules.crawler.service.exception.EsBulkException;
import fr.cnes.regards.modules.crawler.service.exception.FirstFindException;
import fr.cnes.regards.modules.crawler.service.exception.NotFinishedException;
import fr.cnes.regards.modules.crawler.service.service.parallel.EsBulkParallelSaver;
import fr.cnes.regards.modules.crawler.service.service.parallel.EsBulkSaveService;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.DataSourceException;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IInternalDataSourcePlugin;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.indexer.dao.BulkSaveLightResult;
import fr.cnes.regards.modules.indexer.dao.BulkSaveResult;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.dao.spatial.ProjectGeoSettings;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.IModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
public class DatasourceIngestionService implements IDatasourceIngesterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasourceIngestionService.class);

    private static final int MAX_NOTIFICATION_LENGTH = 512;

    private static final DateTimeFormatter ISO_TIME_UTC = new DateTimeFormatterBuilder().parseCaseInsensitive()
                                                                                        .append(DateTimeFormatter.ISO_LOCAL_TIME)
                                                                                        .toFormatter();

    /**
     * Only used to delete all data objects from a removed datasource
     */
    @Autowired
    private IEsRepository esRepos;

    @Autowired
    private IDatasourceIngestionRepository dsIngestionRepos;

    @Autowired
    private IPluginService pluginService;

    /**
     * Current tenant resolver
     */
    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected IEntityIndexerService entityIndexerService;

    @Autowired
    private INotificationClient notifClient;

    @Autowired
    private IModelService modelService;

    @Autowired
    private ProjectGeoSettings projectGeoSettings;

    @Autowired
    private INotificationClient notificationClient;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private CrawlerPropertiesConfiguration crawlerConf;

    @Autowired
    private EsBulkSaveService esBulkSaveService;

    @Autowired
    private IndexService indexService;

    private final ExecutorService deletionThreadPoolExecutor = Executors.newFixedThreadPool(1);

    private final List<DatasourceIdAndErrorCause> datasourcesBlockedInStarted = new ArrayList<>();

    public void updateAndCleanTenantDatasourceIngestions() {
        String currentTenant = runtimeTenantResolver.getTenant();
        // First, check if all existing datasource plugins are managed
        // Find all current datasource ingestions
        Map<String, DatasourceIngestion> dsIngestionsMap = dsIngestionRepos.findAll()
                                                                           .stream()
                                                                           .collect(Collectors.toMap(DatasourceIngestion::getId,
                                                                                                     Function.identity()));

        // Set all the data sources that couldn't previously be marked as error to this state
        List<DatasourceIdAndErrorCause> datasourcesToSetInError = new ArrayList<>(datasourcesBlockedInStarted);
        datasourcesBlockedInStarted.clear();
        datasourcesToSetInError.forEach(ds -> setError(ds.id, ds.cause, null));

        // Find all datasource plugins except inactive ones => find all ACTIVE datasource plugins
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
            } catch (RuntimeException | ModuleException e) {
                LOGGER.error("Cannot compute next ingestion planned date", e);
            }
        });
    }

    /**
     * Find all ready datasources to be ingested and mark them as "STARTED" in a transaction
     */
    public List<String> startAllReadyDatasourceIngestion() {
        List<DatasourceIngestion> allDatasourceIngestionReady = dsIngestionRepos.findAllReady(OffsetDateTime.now()
                                                                                                            .withOffsetSameInstant(
                                                                                                                ZoneOffset.UTC));
        for (DatasourceIngestion datasourceIngestion : allDatasourceIngestionReady) {
            // Reinit old DatasourceIngestion properties
            datasourceIngestion.setStackTrace(null);
            datasourceIngestion.setSavedObjectsCount(0);
            datasourceIngestion.setInErrorObjectsCount(0);
            datasourceIngestion.setStatus(IngestionStatus.STARTED);
        }
        allDatasourceIngestionReady = dsIngestionRepos.saveAll(allDatasourceIngestionReady);
        return allDatasourceIngestionReady.stream().map(DatasourceIngestion::getId).toList();
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

    public void setError(String dsIngestionId, String cause, CrawlingCursor cursorToSet) {
        try {
            LOGGER.debug("Error while processing datasource with id {}. Cause: {}. Reset cursor to {}",
                         dsIngestionId,
                         cause,
                         cursorToSet);
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
                if (cursorToSet != null) {
                    dsIngestion.setCursor(cursorToSet);
                }
                dsIngestion = dsIngestionRepos.save(dsIngestion);
                sendNotificationSummary(dsIngestion);
            } else {
                LOGGER.warn("Unable to find datasource with id {} to set error={}", dsIngestionId, cause);
            }
        } catch (Exception e) {
            LOGGER.error("Database error while attempting to set datasource with id {} ingestion to ERROR state. "
                         + "The update will be retried later. Initial error is {}.", dsIngestionId, cause, e);
            datasourcesBlockedInStarted.add(new DatasourceIdAndErrorCause(dsIngestionId, cause));
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
        deletionThreadPoolExecutor.submit(() -> {
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
    public void updatePlannedDate(DatasourceIngestion dsIngestion)
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
                case INACTIVE -> {
                    dsIngestion.setStatus(IngestionStatus.NEW);
                    dsIngestion.setNextPlannedIngestDate(OffsetDateTime.now());
                    dsIngestionRepos.save(dsIngestion);
                }
                case STARTED, NEW -> {
                    // STARTED: Already in progress
                    // NEW: dsIngestion just been created with a next planned date as now() ie launch as soon as possible
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

    private record DatasourceIdAndErrorCause(String id,
                                             String cause) {
        // NOSONAR

    }

    @Override
    public Optional<IngestionResult> ingest(String datasourceIngestionId)
        throws ModuleException, NotFinishedException, FirstFindException {
        String tenant = runtimeTenantResolver.getTenant();
        Optional<DatasourceIngestion> odsi = dsIngestionRepos.findById(datasourceIngestionId);
        if (odsi.isPresent()) {
            DatasourceIngestion dsi = odsi.get();
            PluginConfiguration pluginConf = pluginService.getPluginConfiguration(datasourceIngestionId);
            OffsetDateTime lastUpdateDate = dsi.getLastIngestDate();
            IDataSourcePlugin dsPlugin;
            try {
                dsPlugin = pluginService.getPlugin(pluginConf.getBusinessId());
            } catch (NotAvailablePluginConfigurationException e) {
                throw new InactiveDatasourceException(e);
            }

            BulkSaveLightResult saveResult;
            OffsetDateTime ingestionStart = OffsetDateTime.now();
            Long datasourceId = pluginConf.getId();
            indexService.createIndexIfNeeded(tenant);
            // i decided not to put a cache here because attribute can be updated... even if it is minor updates it can
            // be taken into account by mappings. In case crawling seem to be slower because of this we can always add one
            // but it should be reset with attribute updates
            //lets find the model attributes so that we can have mappings for this model and try to put them.
            String modelName = dsPlugin.getModelName();
            indexService.configureMappings(tenant, modelName);
            saveResult = readDatasource(new IngestionParameters(lastUpdateDate,
                                                                tenant,
                                                                dsPlugin,
                                                                datasourceId,
                                                                ingestionStart), dsi);

            // Only update dataset if new docs are indexed
            if (saveResult.getSavedDocsCount() > 0) {
                // In case Dataset associated with datasourceId already exists (or had been created between datasource creation
                // and its ingestion), we must search for it and do as it has been updated (to update all associated data
                // objects which have a lastUpdate date >= now)
                SimpleSearchKey<Dataset> searchKey = new SimpleSearchKey<>(EntityType.DATASET.toString(),
                                                                           Dataset.class);
                searchKey.setSearchIndex(tenant);
                searchKey.setCrs(projectGeoSettings.getCrs());
                Set<Dataset> datasetsToUpdate = new HashSet<>();
                esRepos.searchAll(searchKey,
                                  datasetsToUpdate::add,
                                  ICriterion.eq("plgConfDataSource.id", datasourceId));
                if (!datasetsToUpdate.isEmpty()) {
                    sendMessage("Start updating datasets associated to datasource...", datasourceIngestionId);
                    try {
                        // Update entities associated to dataset for each entity updated previously,
                        // So search for entities with last_update > (ingestionStart - 1s)
                        // And for each updated entity set last_update = OffsetDateTime.now()
                        // criteria used to detect which products have been recently updated in current crawling
                        OffsetDateTime minLastUpdateCriteria = ingestionStart.withNano(0).minusSeconds(1);
                        entityIndexerService.updateDatasets(tenant,
                                                            datasetsToUpdate,
                                                            minLastUpdateCriteria,
                                                            OffsetDateTime.now(),
                                                            true,
                                                            datasourceIngestionId,
                                                            true);
                        // skipDissociationStep is set to true because this method only upserts dataObjects,
                        // and dissociation step is needed only when dataset is updated
                    } catch (ModuleException e) {
                        sendMessage(String.format("Error updating datasets associated to datasource. Cause : %s.",
                                                  e.getMessage()), datasourceIngestionId);
                    }
                    sendMessage("...End updating datasets.", datasourceIngestionId);
                }
            } else {
                sendMessage("No new data indexed. Dataset update skipped.", datasourceIngestionId);
            }

            return Optional.of(new IngestionResult(ingestionStart,
                                                   saveResult.getSavedDocsCount(),
                                                   saveResult.getInErrorDocsCount(),
                                                   dsi.getCursor().getCurrentLastEntityDate(),
                                                   dsi.getCursor().getPreviousLastEntityDate(),
                                                   dsi.getCursor().getCurrentLastId(),
                                                   dsi.getCursor().getPreviousLastId()));
        }
        return Optional.empty();
    }

    @Override
    public List<DatasourceIngestion> getDatasourceIngestions() {
        return dsIngestionRepos.findAll(Sort.by("label"));
    }

    @Override
    public void deleteDatasourceIngestion(String id) {
        dsIngestionRepos.deleteById(id);
    }

    @Override
    public void scheduleNowDatasourceIngestion(String id) {
        DatasourceIngestion dsi = dsIngestionRepos.findById(id).orElseThrow();
        dsi.setNextPlannedIngestDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC));
        dsIngestionRepos.save(dsi);
    }

    /**
     * Send a message to IngesterService (or whoever want to listen to it) concerning given datasourceIngestionId
     */
    public void sendMessage(String message, String dsId) {
        String msg = String.format("%s: %s",
                                   ISO_TIME_UTC.format(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)),
                                   message);
        eventPublisher.publishEvent(new DataSourceMessageEvent(this, runtimeTenantResolver.getTenant(), msg, dsId));
    }

    private BulkSaveLightResult readDatasource(IngestionParameters ingestionParameters, DatasourceIngestion dsi)
        throws FirstFindException, NotFinishedException {
        String dsiId = dsi.getId();
        sendMessage("Start reading datasource and creating objects...", dsiId);
        int availableRecordsCount = 0;

        // Use a thread pool of size 1 to merge data while datasource pull other data
        sendMessage(String.format("  Finding at most %d records from datasource...", crawlerConf.getMaxBulkSize()),
                    dsiId);
        CrawlingCursor cursor = dsi.getCursor();
        if (cursor == null) {
            dsi.setCursor(new CrawlingCursor(0, crawlerConf.getMaxBulkSize()));
            // Do not apply overlap as crawling begins from scratch
        } else {
            cursor.setSize(crawlerConf.getMaxBulkSize());
            // Try Applying overlap
            cursor.tryApplyOverlap(ingestionParameters.dsPlugin().getOverlap());
        }
        boolean isFirstFind = true;

        EsBulkParallelSaver bulkManager = esBulkSaveService.createBulkParallelSaver(ingestionParameters, dsi, this);
        try {
            availableRecordsCount = doReadSyncAndIndexAsync(ingestionParameters, dsi, bulkManager);
            cursor = dsi.getCursor();
            while (cursor.hasNext()) {
                cursor.next(ingestionParameters.dsPlugin().getCrawlingCursorMode());
                isFirstFind = false;
                sendMessage(String.format("  Searching page of %d records from datasource...", cursor.getSize()),
                            dsiId);
                int lastRecordsCount = doReadSyncAndIndexAsync(ingestionParameters, dsi, bulkManager);
                availableRecordsCount += lastRecordsCount;
                sendMessage(String.format("  ...Found %d records from datasource. Total currently found=%d",
                                          lastRecordsCount,
                                          availableRecordsCount), dsi.getId());
            }
        } catch (DataSourceException | ModuleException e) { // Find from datasource (synchronous task) has failed
            // Failed at first find from datasource => "classical" ERROR
            if (isFirstFind) {
                throw new FirstFindException(e, cursor);
            }
            // An Elasticsearch (save is async) error might occur before the caught exception — the following methods will throw in that case
            BulkSaveLightResult bulkResult = bulkManager.waitAllResultsOrThrowIfAnyFail();
            // No ElasticSearch error on bulks found (no exception thrown), so we can throw a NotFinishedException with current cursor
            throw new NotFinishedException(e, bulkResult, cursor);
        }
        // An Elasticsearch error might occur after the end of the loop — the following methods will throw in that case
        BulkSaveLightResult saveResult = bulkManager.waitAllResultsOrThrowIfAnyFail();
        sendMessage(String.format("  ...Finally indexed %d objects for %d available records.",
                                  saveResult.getSavedDocsCount(),
                                  availableRecordsCount), dsiId);
        sendMessage("...End reading datasource.", dsiId);
        return saveResult;
    }

    /**
     * Read a "page" or a "slice" of data objects from datasource and then launch a thread to index them in Elasticsearch.
     *
     * @return the number of available records found in this page
     */
    private Integer doReadSyncAndIndexAsync(IngestionParameters ingestionParameters,
                                            DatasourceIngestion dsi,
                                            EsBulkParallelSaver esBulkParallelSaver)
        throws DataSourceException, ModuleException {
        if (esBulkParallelSaver.hasErrors()) {
            // Stop processing if there is an error in any task
            throw new EsBulkException();
        }
        List<DataObject> dataObjects = findAllFromDatasource(ingestionParameters, dsi.getCursor());
        esBulkParallelSaver.saveDataObjectAsync(dataObjects);
        return dataObjects.size();
    }

    /**
     * Build an URN for a {@link EntityType} of type DATA. The URN contains an UUID builds for a specific value, it used
     * {@link UUID#nameUUIDFromBytes(byte[])}.
     *
     * @param tenant       the tenant name
     * @param providerId   the original primary key value
     * @param datasourceId The data source identifier
     * @return the IpId generated from given parameters
     */
    private static OaisUniformResourceName buildIpId(String tenant, String providerId, Long datasourceId) {
        return new OaisUniformResourceName(OAISIdentifier.AIP,
                                           EntityType.DATA,
                                           tenant,
                                           UUID.nameUUIDFromBytes((datasourceId + "$$" + providerId).getBytes()),
                                           1,
                                           null,
                                           null);
    }

    /**
     * Read datasource since given date page setting ipId to each objects
     */
    private List<DataObject> findAllFromDatasource(IngestionParameters ingestionParameters, CrawlingCursor cursor)
        throws DataSourceException, ModuleException {
        // Retrieve target model
        String tenant = ingestionParameters.tenant();
        Long datasourceId = ingestionParameters.datasourceId();
        IDataSourcePlugin dsPlugin = ingestionParameters.dsPlugin();
        Model model = modelService.getModelByName(dsPlugin.getModelName());

        // Find all features
        List<DataObjectFeature> dataObjectsRetrieved;
        try {
            long start = System.currentTimeMillis();
            dataObjectsRetrieved = dsPlugin.findAll(tenant,
                                                    cursor,
                                                    ingestionParameters.lastUpdateDate(),
                                                    ingestionParameters.ingestionStart());
            LOGGER.info("Searching entities (size={}, page={}, lastUpdateDate={}) from datasource plugin took {}ms",
                        cursor.getSize(),
                        cursor.getPosition(),
                        cursor.getLastEntityDate(),
                        System.currentTimeMillis() - start);
        } catch (Exception e) {
            // Catch Exception in order to catch all exceptions (in particular runtime) from plugins. Plugins can be out of our scope.
            String message = "Error retriving features from datasource " + dsPlugin.getClass().getName();
            if (e.getMessage() != null) {
                message = message + ". Cause: " + e.getMessage();
            }
            notificationClient.notify(message,
                                      "Datasource harvesting failure",
                                      NotificationLevel.ERROR,
                                      DefaultRole.ADMIN);
            throw new DataSourceException(String.format("Cannot retrieve data from datasource %s on tenant %s",
                                                        datasourceId,
                                                        tenant), e);
        }

        // Decorate features with its related entity (i.e. DataObject)
        List<DataObject> dataObjects = new ArrayList<>();

        for (DataObjectFeature feature : dataObjectsRetrieved) {
            // Wrap each feature into its decorator
            DataObject dataObject = DataObject.wrap(model,
                                                    feature,
                                                    IInternalDataSourcePlugin.class.isAssignableFrom(dsPlugin.getClass()));
            dataObject.setDataSourceId(datasourceId);
            // Generate IpId only if datasource plugin hasn't yet generate it
            if (dataObject.getIpId().isRandomEntityId()) {
                dataObject.setIpId(buildIpId(tenant, dataObject.getProviderId(), datasourceId));
                dataObject.setVersion(dataObject.getIpId().getVersion());
            }
            // Manage geometries
            if (feature.getGeometry() != null && !(feature.getGeometry() instanceof Unlocated)) {
                // The crs is brought by project so it must be set on feature to be taken into account by geometry
                // normalization
                dataObject.getFeature().setCrs(projectGeoSettings.getCrs().toString());
            }
            dataObjects.add(dataObject);
        }

        // Build decorated page
        return dataObjects;
    }

    /**
     * Get Callable to be used by parallel tasks to create a bulk of data objects
     */
    public BulkSaveResult createOrUpdateDataObjects(IngestionParameters ingestionParameters,
                                                    String datasourceIngestionId,
                                                    List<DataObject> dataObjects) throws ModuleException {
        sendMessage(String.format("  Indexing %d objects...", dataObjects.size()), datasourceIngestionId);
        BulkSaveResult bulkSaveResult = entityIndexerService.upsertDataObjects(ingestionParameters.tenant(),
                                                                               ingestionParameters.datasourceId(),
                                                                               ingestionParameters.ingestionStart(),
                                                                               dataObjects,
                                                                               datasourceIngestionId);
        if (bulkSaveResult.getInErrorDocsCount() > 0) {
            sendMessage(String.format("  ...%d objects cannot be saved:%n%s",
                                      bulkSaveResult.getInErrorDocsCount(),
                                      bulkSaveResult.getDetailedErrorMsg().replace("\n", "\n    ")),
                        datasourceIngestionId);
        }
        sendMessage(String.format("  ...%d objects effectively indexed.", bulkSaveResult.getSavedDocsCount()),
                    datasourceIngestionId);
        return bulkSaveResult;
    }
}
