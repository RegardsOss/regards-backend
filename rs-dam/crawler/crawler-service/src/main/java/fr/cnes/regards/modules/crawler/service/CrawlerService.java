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

import fr.cnes.regards.framework.module.rest.exception.InactiveDatasourceException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.crawler.dao.IDatasourceIngestionRepository;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionResult;
import fr.cnes.regards.modules.crawler.service.conf.CrawlerPropertiesConfiguration;
import fr.cnes.regards.modules.crawler.service.event.DataSourceMessageEvent;
import fr.cnes.regards.modules.crawler.service.exception.FirstFindException;
import fr.cnes.regards.modules.crawler.service.exception.NotFinishedException;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.DataSourceException;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IDataSourcePlugin;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.IInternalDataSourcePlugin;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.event.NotDatasetEntityEvent;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.indexer.dao.BulkSaveLightResult;
import fr.cnes.regards.modules.indexer.dao.BulkSaveResult;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.dao.spatial.ProjectGeoSettings;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.IModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

record CrawlingParameters(OffsetDateTime lastUpdateDate, String tenant, IDataSourcePlugin dsPlugin,
                                  Long datasourceId, OffsetDateTime ingestionStart) {

}

/**
 * Crawler service for other entity than Dataset. <b>This service need @EnableSchedule at Configuration</b>
 * This service is the primary to autowire (by IngesterService) in order to ingest datasources
 *
 * @author oroussel
 */
@Service
// Transactionnal is handle by hand on the right method, do not specify Multitenant or InstanceTransactionnal
public class CrawlerService extends AbstractCrawlerService<NotDatasetEntityEvent>
    implements ICrawlerAndIngesterService {

    private static final DateTimeFormatter ISO_TIME_UTC = new DateTimeFormatterBuilder().parseCaseInsensitive()
                                                                                        .append(DateTimeFormatter.ISO_LOCAL_TIME)
                                                                                        .toFormatter();


    @Autowired
    private IModelService modelService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IEsRepository esRepos;

    @Autowired
    private IDatasourceIngestionRepository dsIngestionRepos;

    @Autowired
    private ProjectGeoSettings projectGeoSettings;

    @Autowired
    private INotificationClient notificationClient;

    /**
     * Self proxy
     */
    @Autowired
    @Lazy
    private ICrawlerAndIngesterService self;

    @Autowired
    private IDatasourceIngestionRepository datasourceIngestionRepo;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private CrawlerPropertiesConfiguration crawlerConf;

    @Autowired
    private IndexService indexService;

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

    @Override
    @Async(CrawlerTaskExecutorConfiguration.CRAWLER_EXECUTOR_BEAN)
    public void crawl() {
        super.crawl(self::doPoll);
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
            // If index doesn't exist, just create all data objects
            boolean mergeNeeded = indexService.createIndexIfNeeded(tenant);
            // i decided not to put a cache here because attribute can be updated... even if it is minor updates it can
            // be taken into account by mappings. In case crawling seem to be slower because of this we can always add one
            // but it should be reset with attribute updates
            //lets find the model attributes so that we can have mappings for this model and try to put them.
            String modelName = dsPlugin.getModelName();
            indexService.configureMappings(tenant, modelName);
            // If index already exist, check if index already contains data objects (if not, no need to merge)
            if (mergeNeeded) {
                SimpleSearchKey<DataObject> searchKey = new SimpleSearchKey<>(EntityType.DATA.toString(),
                                                                              DataObject.class);
                mergeNeeded = esRepos.count(searchKey, ICriterion.all()) != 0;
            }
            saveResult = readDatasource(new CrawlingParameters(lastUpdateDate, tenant, dsPlugin, datasourceId, ingestionStart), dsi, mergeNeeded);
            // In case Dataset associated with datasourceId already exists (or had been created between datasource creation
            // and its ingestion), we must search for it and do as it has been updated (to update all associated data
            // objects which have a lastUpdate date >= now)
            SimpleSearchKey<Dataset> searchKey = new SimpleSearchKey<>(EntityType.DATASET.toString(), Dataset.class);
            searchKey.setSearchIndex(tenant);
            searchKey.setCrs(projectGeoSettings.getCrs());
            Set<Dataset> datasetsToUpdate = new HashSet<>();
            esRepos.searchAll(searchKey, datasetsToUpdate::add, ICriterion.eq("plgConfDataSource.id", datasourceId));
            if (!datasetsToUpdate.isEmpty()) {
                sendMessage("Start updating datasets associated to datasource...", datasourceIngestionId);
                try {
                    entityIndexerService.updateDatasets(tenant,
                                                        datasetsToUpdate,
                                                        lastUpdateDate,
                                                        ingestionStart,
                                                        true,
                                                        datasourceIngestionId);
                } catch (ModuleException e) {
                    sendMessage(String.format("Error updating datasets associated to datasource. Cause : %s.",
                                              e.getMessage()), datasourceIngestionId);
                }
                sendMessage("...End updating datasets.", datasourceIngestionId);
            }

            return Optional.of(new IngestionResult(ingestionStart,
                                                   saveResult.getSavedDocsCount(),
                                                   saveResult.getInErrorDocsCount(), dsi.getCursor().getLastEntityDate()));
        }
        return Optional.empty();
    }

    private BulkSaveLightResult readDatasource(CrawlingParameters crawlingParameters, DatasourceIngestion dsi,
                                               boolean mergeNeeded) throws FirstFindException, NotFinishedException {
        String dsiId = dsi.getId();
        sendMessage(String.format("Start reading datasource and %screating objects...", mergeNeeded ? "merging/" : ""),
                    dsiId);
        int availableRecordsCount = 0;
        BulkSaveLightResult saveResult = new BulkSaveLightResult();
        // Use a thread pool of size 1 to merge data while datasource pull other data
        sendMessage(String.format("  Finding at most %d records from datasource...", crawlerConf.getMaxBulkSize()),
                    dsiId);
        CrawlingCursor cursor = dsi.getCursor();
        if (cursor == null) {
            dsi.setCursor(new CrawlingCursor(0, crawlerConf.getMaxBulkSize()));
        } else {
            cursor.setSize(crawlerConf.getMaxBulkSize());
        }
        boolean isFirstFind = true;
        try {
            availableRecordsCount = doReadAndIndex(crawlingParameters,
                                                   dsi,
                                                   mergeNeeded,
                                                   availableRecordsCount,
                                                   saveResult);
            cursor = dsi.getCursor();
            while (cursor.hasNext()) {
                cursor.next();
                isFirstFind =false;
                sendMessage(String.format("  Searching page of %d records from datasource...", cursor.getSize()),
                            dsiId);
                availableRecordsCount = doReadAndIndex(crawlingParameters,
                                                       dsi,
                                                       mergeNeeded,
                                                       availableRecordsCount,
                                                       saveResult);
            }
        } catch (DataSourceException | ModuleException e) { // Find from datasource has failed
            // Failed at first find from datasource => "classical" ERROR
            if (isFirstFind) {
                throw new FirstFindException(e);
            }
            throw new NotFinishedException(e, saveResult, cursor);
        }

        sendMessage(String.format("  ...Finally indexed %d%s objects for %d available records.",
                                  saveResult.getSavedDocsCount(),
                                  mergeNeeded ? "" : " distinct",
                                  availableRecordsCount), dsiId);
        sendMessage("...End reading datasource.", dsiId);
        return saveResult;
    }

    private int doReadAndIndex(CrawlingParameters crawlingParameters, DatasourceIngestion dsi,
                               boolean mergeNeeded,
                               int availableRecordsCount,
                               BulkSaveLightResult saveResult) throws DataSourceException, ModuleException {
        String dsiId = dsi.getId();
        List<DataObject> dataObjects = findAllFromDatasource(crawlingParameters, dsi.getCursor());
        availableRecordsCount += dataObjects.size();
        sendMessage(String.format("  ...Found %d records from datasource. Total currently found=%d",
                                  dataObjects.size(),
                                  availableRecordsCount), dsiId);
        saveResult.append(createOrMergeDataObjects(crawlingParameters, dsiId, dataObjects, mergeNeeded));
        return availableRecordsCount;
    }

    private BulkSaveResult createOrMergeDataObjects(CrawlingParameters crawlingParameters,
                                                    String dsiId,
                                                    final List<DataObject> dataObjects,
                                                    boolean mergeNeeded) throws ModuleException {
        if (mergeNeeded) {
            return mergeDataObjects(crawlingParameters, dsiId, dataObjects);
        } else {
            return createDataObjects(crawlingParameters, dsiId, dataObjects);
        }
    }

    /**
     * Get Callable to be used by parallel tasks to create a bulk of data objects
     */
    private BulkSaveResult createDataObjects(CrawlingParameters crawlingParameters,
                                             String datasourceIngestionId,
                                             List<DataObject> dataObjects) throws ModuleException {

        sendMessage(String.format("  Indexing %d objects...", dataObjects.size()), datasourceIngestionId);
        BulkSaveResult bulkSaveResult = entityIndexerService.createDataObjects(crawlingParameters.tenant(),
                                                                               crawlingParameters.datasourceId(),
                                                                               crawlingParameters.ingestionStart(),
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

    /**
     * Get Callable to be used by parallel tasks to merge a bulk of data objects
     */
    private BulkSaveResult mergeDataObjects(CrawlingParameters crawlingParameters,
                                            String datasourceIngestionId,
                                            List<DataObject> list) throws ModuleException {
        sendMessage(String.format("  Indexing %d objects...", list.size()), datasourceIngestionId);
        BulkSaveResult bulkSaveResult = entityIndexerService.mergeDataObjects(crawlingParameters.tenant(),
                                                                              crawlingParameters.datasourceId(),
                                                                              crawlingParameters.ingestionStart(),
                                                                              list,
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

    /**
     * Read datasource since given date page setting ipId to each objects
     */
    private List<DataObject> findAllFromDatasource(CrawlingParameters crawlingParameters, CrawlingCursor cursor) throws DataSourceException, ModuleException {
        // Retrieve target model
        String tenant = crawlingParameters.tenant();
        Long datasourceId = crawlingParameters.datasourceId();
        IDataSourcePlugin dsPlugin = crawlingParameters.dsPlugin();
        Model model = modelService.getModelByName(dsPlugin.getModelName());

        // Find all features
        List<DataObjectFeature> dataObjectsRetrieved;
        try {
            long start = System.currentTimeMillis();
            dataObjectsRetrieved = dsPlugin.findAll(tenant, cursor, crawlingParameters.lastUpdateDate());
            LOGGER.info(
                "Searching entities (size={}, page={}, lastUpdateDate={}) from datasource plugin took {}ms",
                cursor.getSize(),
                cursor.getPosition(),
                cursor.getPreviousLastEntityDate(),
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
            throw new DataSourceException("Cannot retrieve data from datasource", e);
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
            if (feature.getGeometry() != null) {
                // The crs is brought by project so it must be set on feature to be taken into account by geometry
                // normalization
                dataObject.getFeature().setCrs(projectGeoSettings.getCrs().toString());
            }
            dataObjects.add(dataObject);
        }

        // Build decorated page
        return dataObjects;
    }

    @Override
    public List<DatasourceIngestion> getDatasourceIngestions() {
        return datasourceIngestionRepo.findAll(Sort.by("label"));
    }

    @Override
    public void deleteDatasourceIngestion(String id) {
        datasourceIngestionRepo.deleteById(id);
    }

    @Override
    public void scheduleNowDatasourceIngestion(String id) {
        DatasourceIngestion dsi = datasourceIngestionRepo.findById(id).orElseThrow();
        dsi.setNextPlannedIngestDate(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC));
        datasourceIngestionRepo.save(dsi);
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
}
