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

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.geojson.GeoJsonType;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.modules.crawler.dao.IDatasourceIngestionRepository;
import fr.cnes.regards.modules.crawler.dao.IEntityEventRequestRepository;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.EntityEventRequest;
import fr.cnes.regards.modules.crawler.domain.EntityEventRequestStatus;
import fr.cnes.regards.modules.crawler.service.consumer.DataObjectAssocRemover;
import fr.cnes.regards.modules.crawler.service.consumer.DataObjectGroupAssocRemover;
import fr.cnes.regards.modules.crawler.service.consumer.DataObjectGroupAssocUpdater;
import fr.cnes.regards.modules.crawler.service.consumer.SaveDataObjectsCallable;
import fr.cnes.regards.modules.crawler.service.event.DataSourceMessageEvent;
import fr.cnes.regards.modules.crawler.service.job.UpdateEntityIntoEsJob;
import fr.cnes.regards.modules.crawler.service.metric.IndexerMetricService;
import fr.cnes.regards.modules.crawler.service.session.SessionNotifier;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.AccessLevel;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.plugins.IDataObjectAccessFilterPlugin;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.dam.domain.entities.metadata.DataObjectGroup;
import fr.cnes.regards.modules.dam.service.dataaccess.IAccessRightService;
import fr.cnes.regards.modules.dam.service.entities.DataObjectService;
import fr.cnes.regards.modules.dam.service.entities.ICollectionService;
import fr.cnes.regards.modules.dam.service.entities.IDatasetService;
import fr.cnes.regards.modules.dam.service.entities.IEntitiesService;
import fr.cnes.regards.modules.dam.service.entities.visitor.AttributeBuilderVisitor;
import fr.cnes.regards.modules.indexer.dao.BulkSaveResult;
import fr.cnes.regards.modules.indexer.dao.scripts.UpdateGroupsAndDatasetAssociationEsScript;
import fr.cnes.regards.modules.indexer.dao.spatial.GeoHelper;
import fr.cnes.regards.modules.indexer.dao.spatial.ProjectGeoSettings;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.builders.GeoPointBuilder;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.indexer.domain.spatial.Crs;
import fr.cnes.regards.modules.indexer.service.EsRepositoryFacade;
import fr.cnes.regards.modules.indexer.service.IndexAliasResolver;
import fr.cnes.regards.modules.indexer.service.IndexAliasService;
import fr.cnes.regards.modules.model.domain.IComputedAttribute;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.dto.properties.ObjectProperty;
import fr.cnes.regards.modules.model.service.validation.ValidationMode;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.geo.GeoPoint;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.exception.InvalidShapeException;
import org.locationtech.spatial4j.io.GeoJSONReader;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.jts.JtsShapeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author oroussel
 */
@Service
public class EntityIndexerService implements IEntityIndexerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityIndexerService.class);

    private static final DateTimeFormatter ISO_TIME_UTC = new DateTimeFormatterBuilder().parseCaseInsensitive()
                                                                                        .append(DateTimeFormatter.ISO_LOCAL_TIME)
                                                                                        .toFormatter();

    private static final Type GROUPS_TYPE_GSON = new TypeToken<List<Map<String, Object>>>() {

    }.getType();

    /**
     * Current tenant resolver
     */
    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected EsRepositoryFacade esRepoFacade;

    @Autowired
    protected IPluginService pluginService;

    @Autowired
    private IEntitiesService entitiesService;

    @Autowired
    private IAccessRightService accessRightService;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private DataObjectService dataObjectService;

    @Autowired
    private IDatasetService datasetService;

    @Autowired
    private ICollectionService collectionService;

    @Autowired
    @Lazy
    private IEntityIndexerService self;

    /**
     * The spring application name ~= microservice type
     */
    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private INotificationClient notifClient;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private IDatasourceIngestionRepository dsIngestionRepository;

    @Autowired
    private ProjectGeoSettings projectGeoSettings;

    @Autowired
    private SessionNotifier sessionNotifier;

    @Autowired
    private Gson gson;

    @Autowired
    private ISessionStepRepository sessionStepRepository;

    @Autowired
    private IndexService indexService;

    @Autowired
    private IndexAliasResolver indexAliasResolver;

    @Autowired
    private IEntityEventRequestRepository entityEventRequestRepository;

    @Autowired
    private JobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IndexAliasService indexAliasService;

    @Autowired
    public IndexerMetricService indexerMetricService;

    @Value("${regards.crawler.max.bulk.size:10000}")
    private Integer maxBulkSize;

    @Value("${regards.crawler.max.session.step.size:10000}")
    private int sessionStepBulkSize;

    private static List<String> toErrors(Errors errorsObject) {
        List<String> errors = new ArrayList<>(errorsObject.getErrorCount());
        for (ObjectError objError : errorsObject.getAllErrors()) {
            if (objError instanceof FieldError fieldError) {
                String buf = "Field error in object "
                             + objError.getObjectName()
                             + " on field '"
                             + fieldError.getField()
                             + "', rejected value '"
                             + fieldError.getRejectedValue()
                             + "': "
                             + fieldError.getField()
                             + " "
                             + objError.getDefaultMessage();
                errors.add(buf);
            } else {
                errors.add(objError.toString());
            }
        }
        return errors;
    }

    @Override
    public void updateEntityIntoEs(String tenant,
                                   UniformResourceName ipId,
                                   OffsetDateTime updateDate,
                                   boolean forceAssociatedEntitiesUpdate) throws ModuleException {
        this.updateEntityIntoEs(tenant, ipId, null, updateDate, forceAssociatedEntitiesUpdate, null, false, false);
        // If there is a building index, we have to update the entity into this buildingt index too
        if (indexAliasResolver.resolveBuildingIndex(tenant).isPresent()) {
            this.updateEntityIntoEs(tenant, ipId, null, updateDate, forceAssociatedEntitiesUpdate, null, true, false);
        }
    }

    /**
     * Load given entity from database and update Elasticsearch
     *
     * @param tenant                        concerned tenant (also index intoES)
     * @param ipId                          concerned entity IpId
     * @param minLastUpdateCriteria         for dataset entity, if this date is provided, only more recent data objects must be taken
     *                                      into account
     * @param updateDate                    update date saved inside data objects
     * @param forceAssociatedEntitiesUpdate for dataset entity, force associated entities (ie data objects) update
     * @param datasourceIngestion           the {@link DatasourceIngestion} linked to this update (can be null)
     * @param skipDissociationStep          if true, skip the dissociation step (step needed only if dataset has been updated)
     *                                      the dissociation step is the step where all data objects which do not match the dataset subsetting clause anymore are detached to the dataset
     */
    @Override
    public void updateEntityIntoEs(String tenant,
                                   UniformResourceName ipId,
                                   OffsetDateTime minLastUpdateCriteria,
                                   OffsetDateTime updateDate,
                                   boolean forceAssociatedEntitiesUpdate,
                                   DatasourceIngestion datasourceIngestion,
                                   boolean buildingIndex,
                                   boolean skipDissociationStep) throws ModuleException {
        LOGGER.info("Updating {}", ipId.toString());
        String indexOrAlias = getIndexOrAliasName(tenant, buildingIndex);

        String realIndex = buildingIndex ? indexOrAlias : indexAliasResolver.resolveCurrentIndex(tenant);
        runtimeTenantResolver.forceTenant(tenant);
        AbstractEntity<?> entity = entitiesService.loadWithRelations(ipId);
        // If entity does no more exist in database, it must be deleted from ES
        if (entity == null) {
            LOGGER.debug("Entity is null !!");
            if (ipId.getEntityType() == EntityType.DATASET) {
                sendDataSourceMessage(String.format("    Dataset with IP_ID %s no more exists...", ipId),
                                      datasourceIngestion);
                manageDatasetDelete(realIndex, ipId.toString(), datasourceIngestion);
            }
            esRepoFacade.deleteFromIndexOrAlias(realIndex, ipId.getEntityType().toString(), ipId.toString());
            sendDataSourceMessage(String.format("    ...Dataset with IP_ID %s de-indexed.", ipId), datasourceIngestion);
        } else { // entity has been created or updated, it must be saved into ES
            indexService.createIndexAndAliasIfNeeded(realIndex, buildingIndex);
            indexService.configureMappings(realIndex, entity.getModel().getName());
            ICriterion savedSubsettingClause = null;
            // Remove parameters of dataset datasource to avoid expose security values
            if (entity instanceof Dataset dataset) {
                savedSubsettingClause = dataset.getSubsettingClause();
                prepareDatasetForEs(dataset);
                // update dataset groups
                for (Entry<String, DataObjectGroup> entry : dataset.getMetadata().getDataObjectsGroups().entrySet()) {
                    // remove group if no access
                    if (!entry.getValue().getDatasetAccess()) {
                        dataset.getGroups().remove(entry.getKey());
                    } else { // add (or let) group if FULL_ACCESS or RESTRICTED_ACCESS
                        dataset.getGroups().add(entry.getKey());
                    }
                }
            }
            // Lets handle virtual_id here
            if (entity.isLast()) {
                entity.setVirtualId();
            } else {
                entity.removeVirtualId();
            }
            // Then save entity
            LOGGER.debug("Saving entity {}", entity);
            // If minLastUpdateCriteria is provided, this means that update comes from an ingestion, in this case all data
            // objects must be updated.
            // If lastUpdatedDate isn't provided it means update come from a change into dataset
            // (cf. DatasetCrawlerService.handle(...)) and so it is necessary to check if differences exist between
            // previous version of dataset and current one.
            // It may also mean that it comes from a first ingestion. In this case, minLastUpdateCriteria is null but all
            // data objects must be updated
            boolean needAssociatedDataObjectsUpdate = (minLastUpdateCriteria != null) || forceAssociatedEntitiesUpdate;
            // A dataset change may need associated data objects update
            if (!needAssociatedDataObjectsUpdate && (entity instanceof Dataset dataset)) {
                needAssociatedDataObjectsUpdate = needAssociatedDataObjectsUpdate(dataset,
                                                                                  esRepoFacade.get(indexOrAlias,
                                                                                                   dataset));
            }
            boolean created = esRepoFacade.saveToIndexOrAlias(indexOrAlias, entity);
            LOGGER.debug("Elasticsearch saving result : {}", created);
            if ((entity instanceof Dataset) && needAssociatedDataObjectsUpdate) {
                // Subsetting clause is needed by many things
                LOGGER.info("Running dataset entity {} - {} data objects association calculation into elasticsearch. "
                            + "Cause : force calculation = {}, calculation needed cause of dataset update = {}",
                            entity.getLabel(),
                            entity.getId(),
                            forceAssociatedEntitiesUpdate,
                            needAssociatedDataObjectsUpdate);
                ((Dataset) entity).setSubsettingClause(savedSubsettingClause);
                updateAssociatedDataObjectsOfDataset((Dataset) entity,
                                                     minLastUpdateCriteria,
                                                     updateDate,
                                                     datasourceIngestion,
                                                     buildingIndex,
                                                     skipDissociationStep);
            } else {
                LOGGER.info("Avoid dataset entity {} - {} data objects association calculation into elasticsearch. "
                            + "Cause : force calculation = {}, calculation needed cause of dataset update = {}",
                            entity.getLabel(),
                            entity.getId(),
                            forceAssociatedEntitiesUpdate,
                            needAssociatedDataObjectsUpdate);
            }
        }
        LOGGER.info(ipId + " managed into Elasticsearch");
    }

    private void prepareDatasetForEs(Dataset dataset) throws ModuleException {
        // entity must be detached else Hibernate tries to commit update (datasource is cascade.DETACHED)
        em.detach(dataset);
        if (dataset.getPlgConfDataSource() != null) {
            dataset.getPlgConfDataSource().getParameters().clear();
        }
        // Subsetting clause must not be jsonify into Elasticsearch
        dataset.setSubsettingClause(null);
        // Retrieve dataset metadata information for indexer
        dataset.setMetadata(accessRightService.retrieveDatasetMetadata(dataset.getIpId()));
    }

    /**
     * Compare new dataset and current indexed one and determine if an update of all associated data objects is needed
     * or no
     */
    private boolean needAssociatedDataObjectsUpdate(Dataset newDataset, Dataset curDataset) {
        if (curDataset == null) {
            return true;
        }

        if (newDataset == null) {
            return false;
        }

        boolean need = false;
        if (newDataset.getOpenSearchSubsettingClause() != null) {
            need = !newDataset.getOpenSearchSubsettingClause().equals(curDataset.getOpenSearchSubsettingClause());
        } else if (curDataset.getOpenSearchSubsettingClause() != null) {
            need = true;
        }

        Map<String, DataObjectGroup> currentMetadata = curDataset.getMetadata() != null ?
            curDataset.getMetadata().getDataObjectsGroups() :
            null;
        Map<String, DataObjectGroup> newMetadata = newDataset.getMetadata() != null ?
            newDataset.getMetadata().getDataObjectsGroups() :
            null;
        if (currentMetadata != null) {
            need = need || !currentMetadata.equals(newMetadata);
        } else {
            need = true;
        }

        return need;
    }

    /**
     * Search and update associated dataset data objects (ie remove dataset IpId from tags)
     *
     * @param indexOrAlias concerned alias or index name
     * @param ipId         dataset identifier
     */
    private void manageDatasetDelete(String indexOrAlias, String ipId, DatasourceIngestion datasourceIngestion)
        throws ModuleException {
        // Search all DataObjects tagging this Dataset (only DataObjects because all other entities are already managed
        // with the system Postgres/RabbitMQ)
        sendDataSourceMessage(String.format("      Searching for all data objects tagging dataset IP_ID %s", ipId),
                              datasourceIngestion);
        AtomicInteger objectsCount = new AtomicInteger(0);
        ICriterion taggingObjectsCrit = ICriterion.eq("tags", ipId, StringMatchType.KEYWORD);

        Set<DataObject> toSaveObjects = new HashSet<>();
        OffsetDateTime updateDate = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC);

        // Function to update an object (tags, groups, lastUpdate, ...)
        Consumer<DataObject> updateDataObject = object -> {
            object.removeTags(Collections.singletonList(ipId));
            // reset datasetModelIds
            object.getDatasetModelNames().clear();
            // Remove dataset ipId from metadata.groups dans modelNames
            object.getMetadata().removeDatasetIpId(ipId);
            // update groups
            object.setGroups(object.getMetadata().getGroups());
            // update modelNames
            object.setDatasetModelNames(object.getMetadata().getModelNames());
            object.setLastUpdate(updateDate);
            toSaveObjects.add(object);
            if (toSaveObjects.size() == maxBulkSize) {
                try {
                    esRepoFacade.saveBulkToIndexOrAlias(indexOrAlias, toSaveObjects);
                    objectsCount.addAndGet(toSaveObjects.size());
                    toSaveObjects.clear();
                } catch (ElasticsearchException e) {
                    LOGGER.error("{}", e.getMessage(), e);
                }
            }
        };
        // Apply updateTag function to all tagging objects
        SimpleSearchKey<DataObject> searchKey = new SimpleSearchKey<>(EntityType.DATA.toString(), DataObject.class);
        addProjectInfos(indexOrAlias, searchKey);
        try {
            esRepoFacade.searchAll(searchKey, updateDataObject, taggingObjectsCrit);
            // Bulk save remaining objects to save
            if (!toSaveObjects.isEmpty()) {
                esRepoFacade.saveBulkToIndexOrAlias(indexOrAlias, toSaveObjects);
                objectsCount.addAndGet(toSaveObjects.size());
            }
        } catch (ElasticsearchException e) {
            throw new RsRuntimeException(e);
        }
        sendDataSourceMessage(String.format("      ...Removed dataset IP_ID from %d data objects tags.",
                                            objectsCount.get()), datasourceIngestion);

    }

    /**
     * Search and update associated dataset data objects (ie add dataset IpId into tags)
     *
     * @param dataset               concerned dataset
     * @param datasourceIngestion   the {@link DatasourceIngestion} linked to this update (can be null)
     * @param minLastUpdateCriteria Take into account only more recent minLastUpdateCriteria than provided
     * @param updateDate            update date saved inside data objects
     * @param skipDissociationStep  if true, skip the dissociation step (step needed only if dataset has been updated)
     */
    private void updateAssociatedDataObjectsOfDataset(Dataset dataset,
                                                      OffsetDateTime minLastUpdateCriteria,
                                                      OffsetDateTime updateDate,
                                                      DatasourceIngestion datasourceIngestion,
                                                      boolean buildingIndex,
                                                      boolean skipDissociationStep) throws ModuleException {
        String datasourceIngestionId = datasourceIngestion != null ? datasourceIngestion.getId() : null;
        String crawlerName = datasourceIngestion != null ? datasourceIngestion.getLabel() : null;
        String tenant = runtimeTenantResolver.getTenant();
        String indexOrAlias = getIndexOrAliasName(tenant, buildingIndex);
        sendDataSourceMessage(String.format(
            "      Updating dataset %s indexation and all its associated data objects...",
            dataset.getLabel()), datasourceIngestionId);
        sendDataSourceMessage(String.format("        Searching for dataset %s associated data objects...",
                                            dataset.getLabel()), datasourceIngestionId);
        SimpleSearchKey<DataObject> searchKey = new SimpleSearchKey<>(EntityType.DATA.toString(), DataObject.class);
        addProjectInfos(indexOrAlias, searchKey);

        ExecutorService executor = Executors.newFixedThreadPool(1);

        // Create a callable which bulk save into ES a set of data objects
        SaveDataObjectsCallable saveDataObjectsCallable = new SaveDataObjectsCallable(runtimeTenantResolver,
                                                                                      indexAliasResolver,
                                                                                      esRepoFacade,
                                                                                      tenant,
                                                                                      indexOrAlias,
                                                                                      dataset.getId());
        // Remove association between dataobjects and dataset for all dataobjects which does not match the dataset filter anymore.
        if (!skipDissociationStep) {
            try {
                Timer.Sample timer = indexerMetricService.startNewTimer();
                removeOldDatasetDataObjectsAssoc(dataset,
                                                 updateDate,
                                                 searchKey,
                                                 executor,
                                                 saveDataObjectsCallable,
                                                 datasourceIngestionId);
                indexerMetricService.stopTimer(timer,
                                               IndexerMetricService.INDEXER_REMOVE_DATA_OBJECTS_ASSOC,
                                               crawlerName);

            } catch (ModuleException e) {
                LOGGER.error(e.getMessage(), e);
                sendDataSourceMessage(String.format("Error removing all dataset objects. Cause: %s.", e.getMessage()),
                                      datasourceIngestionId);
            }
        }
        // Associate dataset to all dataobjets. Associate groups of dataset to the dataobjets through metadata
        try {
            Timer.Sample timer = indexerMetricService.startNewTimer();
            addOrUpdateDatasetDataObjectsAssoc(dataset, minLastUpdateCriteria, updateDate, searchKey);
            indexerMetricService.stopTimer(timer, IndexerMetricService.INDEXER_MANAGE_ACCESS_RIGHTS, crawlerName);
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            sendDataSourceMessage(String.format("Error updating new dataset objects. Cause: %s.", e.getMessage()),
                                  datasourceIngestionId);
        }

        // Update dataset access groups for dynamic plugin access rights
        try {
            Timer.Sample timer = indexerMetricService.startNewTimer();
            manageDatasetUpdateFilteredAccessRights(indexOrAlias,
                                                    dataset,
                                                    updateDate,
                                                    executor,
                                                    saveDataObjectsCallable,
                                                    datasourceIngestionId);
            indexerMetricService.stopTimer(timer,
                                           IndexerMetricService.INDEXER_MANAGE_ACCESS_RIGHT_WITH_FILTER,
                                           crawlerName);
        } catch (ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            sendDataSourceMessage(String.format("Error updating dataset access rights. Cause: %s.", e.getMessage()),
                                  datasourceIngestionId);
        }

        // To remove thread used by executor
        executor.shutdown();

        Timer.Sample timer = indexerMetricService.startNewTimer();
        computeComputedAttributes(dataset, datasourceIngestionId, tenant);
        indexerMetricService.stopTimer(timer, IndexerMetricService.INDEXER_COMPUTED_ATTRIBUTES, crawlerName);

        prepareDatasetForEs(dataset);
        esRepoFacade.saveToIndexOrAlias(indexOrAlias, dataset);
        LOGGER.info("Dataset {} updated", dataset.getId());
        sendDataSourceMessage("      ...Dataset indexation updated.", datasourceIngestionId);
    }

    private void addProjectInfos(String aliasOrIndex, SimpleSearchKey<DataObject> searchKey) {
        searchKey.setSearchIndex(aliasOrIndex);
        searchKey.setCrs(projectGeoSettings.getCrs());
    }

    /**
     * Handle Access rights filter for the given dataset. An Access right filter is an accessRight with a {@link IDataObjectAccessFilterPlugin}.
     */
    private void manageDatasetUpdateFilteredAccessRights(String alias,
                                                         Dataset dataset,
                                                         OffsetDateTime updateDate,
                                                         ExecutorService executor,
                                                         SaveDataObjectsCallable saveDataObjectsCallable,
                                                         String dsiId) throws ModuleException {
        SimpleSearchKey<DataObject> searchKey = new SimpleSearchKey<>(EntityType.DATA.toString(), DataObject.class);
        addProjectInfos(alias, searchKey);
        // handle association between dataobjects and groups for all access rights set by plugin
        for (DataObjectGroup group : dataset.getMetadata().getDataObjectsGroups().values()) {
            // If access to the dataset is allowed and a plugin access filter is set on dataobject metadata, calculate which dataObjects are in the given group
            if (group.getDatasetAccess() && (group.getMetaDataObjectAccessFilterPluginBusinessId() != null)) {
                try {
                    IDataObjectAccessFilterPlugin plugin = pluginService.getPlugin(group.getMetaDataObjectAccessFilterPluginBusinessId());
                    ICriterion searchFilter = plugin.getSearchFilter();
                    if (searchFilter != null) {
                        removeOldDataObjectsGroupAssoc(dataset,
                                                       updateDate,
                                                       searchKey,
                                                       executor,
                                                       saveDataObjectsCallable,
                                                       dsiId,
                                                       group.getGroupName(),
                                                       searchFilter);
                        // Handle specific dataobject groups by access filter plugin
                        addOrUpdateDataObectGroupAssoc(dataset,
                                                       updateDate,
                                                       searchKey,
                                                       executor,
                                                       saveDataObjectsCallable,
                                                       dsiId,
                                                       group.getGroupName(),
                                                       searchFilter);
                    }
                } catch (ModuleException e) {
                    // Plugin conf doesn't exist anymore, so remove all group assoc
                    removeOldDataObjectsGroupAssoc(dataset,
                                                   updateDate,
                                                   searchKey,
                                                   executor,
                                                   saveDataObjectsCallable,
                                                   dsiId,
                                                   group.getGroupName(),
                                                   ICriterion.all());
                }
            }
        }
    }

    /**
     * Manage computed attributes computation
     *
     * @param dataset concerned dataset
     * @param dsiId   can be null (in this case, no notification is sent)
     */
    @Override
    public void computeComputedAttributes(Dataset dataset, String dsiId, String tenant) {
        // lets compute computed attributes from the dataset model
        Set<IComputedAttribute<Dataset, ?>> computationPlugins = entitiesService.getComputationPlugins(dataset);
        LOGGER.info("Starting parallel computing of {} attributes (dataset {})...",
                    computationPlugins.size(),
                    dataset.getId());

        sendDataSourceMessage(String.format("        Starting computing of %d attributes...",
                                            computationPlugins.size()), dsiId);
        computationPlugins.parallelStream().forEach(p -> {
            runtimeTenantResolver.forceTenant(tenant);
            p.compute(dataset);
        });
        // Once computations has been done, associated attributes are created or updated
        createComputedAttributes(dataset, computationPlugins);

        List<IComputedAttribute<Dataset, ?>> ll = new ArrayList<>(computationPlugins);
        ll.forEach(comAtt -> LOGGER.info("attribute {} is computed", comAtt.getAttributeToCompute().getName()));

        LOGGER.info("...computing OK");
        sendDataSourceMessage(String.format("        ...Computing ended.", computationPlugins.size()), dsiId);
    }

    /**
     * Associate all DATA entities matching the subsetting clause to the given DATASET entity and dataset groups.<br/>
     * Only groups with no {@link AccessLevel#CUSTOM_ACCESS} are associated the dataobjects in this method.<br/>
     * The association is done by the es script {@see indexer-dao/src/main/resources/es-scripts/updateGroupsAndDatasetAssociations.painless} .<br/>
     * To handle the groups with {@link AccessLevel#CUSTOM_ACCESS} see {@link EntityIndexerService#addOrUpdateDataObectGroupAssoc}.<br/>
     * <b>NOTE</b> : The subsetting clause to find DATA entities is computed by adding dataset subsetting clause and "lastUpdate > minLastUpdateCriteria parameter".<br/>
     *
     * @param dataset               {@link Dataset} to associate to DATA entities
     * @param minLastUpdateCriteria {@link OffsetDateTime}. If not null, add a datatime criterion in the subsesstin clause
     *                              to find only DATA with a minLastUpdateCriteria greter than this parameter
     * @param updateDate            {@link OffsetDateTime} of the current update process
     * @param searchKey             {@link SimpleSearchKey} used to run elasticsearch searh of DATA entities to update
     */
    private void addOrUpdateDatasetDataObjectsAssoc(Dataset dataset,
                                                    OffsetDateTime minLastUpdateCriteria,
                                                    OffsetDateTime updateDate,
                                                    SimpleSearchKey<DataObject> searchKey) throws ModuleException {
        ICriterion subsettingCrit = dataset.getSubsettingClause();
        // Add lastUpdate restriction if a date is provided
        if (minLastUpdateCriteria != null) {
            subsettingCrit = ICriterion.and(subsettingCrit,
                                            ICriterion.gt(StaticProperties.LAST_UPDATE_PATH, minLastUpdateCriteria));
        }
        // set current groups with no plugin access filter from groupsMap on metadata for this datasetIpId
        // Computation of group access with plugins are done in another step.
        // This step only associate group to dataobjets of dataset with no filter. All objets of the dataset have the same groups.
        List<DatasetAccessWrite> groups = new ArrayList<>();
        for (DataObjectGroup group : dataset.getMetadata().getDataObjectsGroups().values()) {
            // Ignore any accessGroup not giving the access to the dataset
            if (group.getMetaDataObjectAccessFilterPluginBusinessId() == null
                && group.getDatasetAccess()
                && group.getDataObjectAccess()) {
                groups.add(new DatasetAccessWrite(group.getGroupName(),
                                                  dataset.getIpId().toString(),
                                                  group.getDataFileAccess()));
            }
        }

        Map<String, Object> params = new HashMap<>();
        params.put("datasetIpId", dataset.getIpId().toString());
        params.put("datasetModelName", dataset.getModel().getName());
        params.put("groups", gson.fromJson(gson.toJson(groups), GROUPS_TYPE_GSON));
        params.put("updateDate", updateDate);

        esRepoFacade.updateByQueryInOneIndex(searchKey,
                                             subsettingCrit,
                                             UpdateGroupsAndDatasetAssociationEsScript.ID,
                                             params);
    }

    /**
     * Associates all DATA entities matching the subsetting clause to the given DATASET groups with {@link AccessLevel#CUSTOM_ACCESS}.<br/>
     * The association is done by the {@link DataObjectGroupAssocUpdater} consumer.<br/>
     *
     * @param dataset                 {@link Dataset} to associate to DATA entities
     * @param updateDate              {@link OffsetDateTime} of the current update process
     * @param searchKey               {@link SimpleSearchKey} used to run elasticsearch searh of DATA entities to update
     * @param executor                {@link ExecutorService}
     * @param saveDataObjectsCallable {@link SaveDataObjectsCallable} used to save data
     * @param dsiId                   {@link DatasourceIngestion} identifier
     * @param groupName               Name of the group to associate to DATA entities.
     * @param groupSubsettingClause   {@link ICriterion} group subsetting clause. Caculate by {@link IDataObjectAccessFilterPlugin} plugin.
     */
    private void addOrUpdateDataObectGroupAssoc(Dataset dataset,
                                                OffsetDateTime updateDate,
                                                SimpleSearchKey<DataObject> searchKey,
                                                ExecutorService executor,
                                                SaveDataObjectsCallable saveDataObjectsCallable,
                                                String dsiId,
                                                String groupName,
                                                ICriterion groupSubsettingClause) throws ModuleException {
        // A set used to accumulate data objects to save into ES
        HashSet<DataObject> toSaveObjects = new HashSet<>();
        sendDataSourceMessage(String.format("          Adding or Updating data objects group <%s> association...",
                                            groupName), dsiId);
        ICriterion subsettingCrit = dataset.getSubsettingClause();
        // First : Retrieve objects associated  matching the groupSubsettingClause
        subsettingCrit = ICriterion.and(subsettingCrit, groupSubsettingClause);
        // For each objet remove group if not associated throught an other dataset
        DataObjectGroupAssocUpdater dataObjectAssocUpdater = new DataObjectGroupAssocUpdater(dataset,
                                                                                             updateDate,
                                                                                             toSaveObjects,
                                                                                             saveDataObjectsCallable,
                                                                                             executor,
                                                                                             groupName,
                                                                                             maxBulkSize);
        try {
            esRepoFacade.searchAll(searchKey, dataObjectAssocUpdater, subsettingCrit);
            // Saving remaining objects...
            dataObjectAssocUpdater.finalSave();
            sendDataSourceMessage(String.format("          ...%d data objects group <%s> association saved.",
                                                dataObjectAssocUpdater.getObjectsCount(),
                                                groupName), dsiId);
        } catch (ElasticsearchException e) {
            throw new ModuleException(e);
        }

    }

    /**
     * Remove association between DATASET entity and DATA entities that are no more into the subsetting clause.<br/>
     * Only groups with no {@link AccessLevel#CUSTOM_ACCESS} are dissociated the the dataobjects in this method.<br/>
     * The dissociation is done by the {@link DataObjectAssocRemover} consumer.<br/>
     * To handle the groups with {@link AccessLevel#CUSTOM_ACCESS} see {@link EntityIndexerService#removeOldDataObjectsGroupAssoc}.<br/>
     *
     * @param dataset                 {@link Dataset} to associate to DATA entities
     * @param updateDate              {@link OffsetDateTime} of the current update process
     * @param searchKey               {@link SimpleSearchKey} used to run elasticsearch searh of DATA entities to update
     * @param executor                {@link ExecutorService}
     * @param saveDataObjectsCallable {@link SaveDataObjectsCallable} used to save data
     * @param dsiId                   {@link DatasourceIngestion} identifier
     */
    private void removeOldDatasetDataObjectsAssoc(Dataset dataset,
                                                  OffsetDateTime updateDate,
                                                  SimpleSearchKey<DataObject> searchKey,
                                                  ExecutorService executor,
                                                  SaveDataObjectsCallable saveDataObjectsCallable,
                                                  String dsiId) throws ModuleException {

        // A set used to accumulate data objects to save into ES
        HashSet<DataObject> toSaveObjects = new HashSet<>();
        sendDataSourceMessage("          Removing old dataset data objects association...", dsiId);
        // First : remove association between dataset and data objects for data objects that are no more associated to
        // new subsetting clause so search data objects that are tagged with dataset IPID and with NOT(user subsetting
        // clause)
        ICriterion oldAssociatedObjectsCrit = ICriterion.and(ICriterion.eq("tags",
                                                                           dataset.getIpId().toString(),
                                                                           StringMatchType.KEYWORD),
                                                             ICriterion.not(dataset.getUserSubsettingClause()));
        // Create a Consumer to be executed on each data object of dataset subsetting criteria results
        DataObjectAssocRemover dataObjectAssocRemover = new DataObjectAssocRemover(dataset,
                                                                                   updateDate,
                                                                                   toSaveObjects,
                                                                                   saveDataObjectsCallable,
                                                                                   executor,
                                                                                   maxBulkSize);
        try {
            esRepoFacade.searchAll(searchKey, dataObjectAssocRemover, oldAssociatedObjectsCrit);
            // Saving remaining objects...
            dataObjectAssocRemover.finalSave();
            sendDataSourceMessage(String.format("          ...%d data objects dataset association removed.",
                                                dataObjectAssocRemover.getObjectsCount()), dsiId);
        } catch (ElasticsearchException e) {
            throw new ModuleException(e);
        }
    }

    /**
     * Remove association between DATASET entity and DATA entities that are no more into the subsetting clause.<br/>
     * Only groups with {@link AccessLevel#CUSTOM_ACCESS} are dissociated the the dataobjects in this method.<br/>
     * The dissociation is done by the {@link DataObjectGroupAssocRemover} consumer.<br/>
     * To handle the groups with {@link AccessLevel#CUSTOM_ACCESS} see {@link EntityIndexerService#removeOldDataObjectsGroupAssoc}.<br/>
     *
     * @param dataset                 {@link Dataset} to associate to DATA entities
     * @param updateDate              {@link OffsetDateTime} of the current update process
     * @param searchKey               {@link SimpleSearchKey} used to run elasticsearch searh of DATA entities to update
     * @param executor                {@link ExecutorService}
     * @param saveDataObjectsCallable {@link SaveDataObjectsCallable} used to save data
     * @param dsiId                   {@link DatasourceIngestion} identifier
     */
    private void removeOldDataObjectsGroupAssoc(Dataset dataset,
                                                OffsetDateTime updateDate,
                                                SimpleSearchKey<DataObject> searchKey,
                                                ExecutorService executor,
                                                SaveDataObjectsCallable saveDataObjectsCallable,
                                                String dsiId,
                                                String groupName,
                                                ICriterion groupSubsettingClause) throws ModuleException {
        // A set used to accumulate data objects to save into ES
        HashSet<DataObject> toSaveObjects = new HashSet<>();
        sendDataSourceMessage(String.format("          Removing old data objects group <%s> association...", groupName),
                              dsiId);
        // First : Retrieve objects associated to given group and not matching the groupSubsettingClause
        ICriterion oldAssociatedObjectsCrit = ICriterion.and(ICriterion.eq("tags",
                                                                           dataset.getIpId().toString(),
                                                                           StringMatchType.KEYWORD),
                                                             ICriterion.contains("groups",
                                                                                 groupName,
                                                                                 StringMatchType.KEYWORD),
                                                             ICriterion.not(groupSubsettingClause));
        // For each objet remove group if not associated throught an other dataset
        DataObjectGroupAssocRemover dataObjectAssocRemover = new DataObjectGroupAssocRemover(dataset,
                                                                                             updateDate,
                                                                                             toSaveObjects,
                                                                                             saveDataObjectsCallable,
                                                                                             executor,
                                                                                             groupName,
                                                                                             maxBulkSize);
        try {
            esRepoFacade.searchAll(searchKey, dataObjectAssocRemover, oldAssociatedObjectsCrit);
        } catch (ElasticsearchException e) {
            throw new ModuleException(e);
        }
        // Saving remaining objects...
        dataObjectAssocRemover.finalSave();
        sendDataSourceMessage(String.format("          ...%d data objects group <%s> association removed.",
                                            dataObjectAssocRemover.getObjectsCount(),
                                            groupName), dsiId);
    }

    /**
     * Create computed attributes from computation
     */
    private void createComputedAttributes(Dataset dataset, Set<IComputedAttribute<Dataset, ?>> computationPlugins) {
        // for each computation plugin lets add the computed attribute
        for (IComputedAttribute<Dataset, ?> plugin : computationPlugins) {
            IProperty<?> attributeToAdd = plugin.accept(new AttributeBuilderVisitor());
            if (attributeToAdd instanceof ObjectProperty attrInFragment) {
                // the attribute is inside a fragment so lets find the right one to add the attribute inside it
                Optional<IProperty<?>> candidate = dataset.getProperties()
                                                          .stream()
                                                          .filter(attr -> (attr instanceof ObjectProperty)
                                                                          && attr.getName()
                                                                                 .equals(attrInFragment.getName()))
                                                          .findFirst();
                if (candidate.isPresent()) {
                    Set<IProperty<?>> properties = ((ObjectProperty) candidate.get()).getValue();
                    // the fragment is already here, lets remove the old properties if they exist
                    properties.removeAll(attrInFragment.getValue());
                    // and now set the new ones
                    properties.addAll(attrInFragment.getValue());
                } else {
                    // the fragment is not here so lets create it by adding it
                    dataset.addProperty(attrInFragment);
                }
            } else {
                // the attribute is not inside a fragment so lets remove the old one if it exists and add the new one to
                // the root
                dataset.removeProperty(attributeToAdd);
                dataset.addProperty(attributeToAdd);
            }
        }
    }

    @Override
    @MultitenantTransactional
    public void updateDatasets(String tenant,
                               Collection<Dataset> datasets,
                               OffsetDateTime minLastUpdateCriteria,
                               OffsetDateTime updateDate,
                               boolean forceDataObjectsUpdate,
                               DatasourceIngestion datasourceIngestion,
                               boolean buildingIndex,
                               boolean skipDissociationStep) throws ModuleException {
        for (Dataset dataset : datasets) {
            LOGGER.info("Updating dataset {} ...", dataset.getLabel());
            sendDataSourceMessage(String.format("  Updating dataset %s...", dataset.getLabel()), datasourceIngestion);
            updateEntityIntoEs(tenant,
                               dataset.getIpId(),
                               minLastUpdateCriteria,
                               updateDate,
                               forceDataObjectsUpdate,
                               datasourceIngestion,
                               buildingIndex,
                               skipDissociationStep);
            sendDataSourceMessage(String.format("  ...Dataset %s updated.", dataset.getLabel()), datasourceIngestion);
            LOGGER.info("Dataset {} updated.", dataset.getLabel());
        }
    }

    @Override
    public void createNotificationForAdmin(String tenant, String title, String message, NotificationLevel level) {
        notifClient.notify(message, title, level, DefaultRole.PROJECT_ADMIN);
    }

    @Override
    public void createBuildingIndexAndCreateEntities(String tenant) throws ModuleException {
        String aliasName = IndexAliasResolver.resolveAliasName(tenant);
        indexService.deleteBuildingIndexIfAlreadyExists(tenant);

        String newIndexName = indexAliasResolver.resolveNextIndexName(tenant);
        boolean isNewIndex = indexService.createIndexAndAliasIfNeeded(newIndexName, true);
        indexAliasService.setBuilding(aliasName, newIndexName);

        OffsetDateTime updateDate = OffsetDateTime.now();

        if (indexAliasResolver.resolveBuildingIndex(tenant).isPresent()) {
            self.updateDatasets(tenant, datasetService.findAll(), null, updateDate, true, null, true, isNewIndex);
        }

        updateAllCollections(tenant, updateDate);
    }

    @Override
    @MultitenantTransactional
    public Page<EntityEventRequest> scheduleUpdateEntityIntoEsJob(Pageable page) {
        Page<EntityEventRequest> requestsPage = entityEventRequestRepository.findByStatusToDoOrderByUrnAsc(page);
        if (!requestsPage.hasContent()) {
            return requestsPage;
        }
        List<EntityEventRequest> requests = requestsPage.getContent();
        // distinct requests in TO_DO states
        List<EntityEventRequest> distinctToDoRequests = new ArrayList<>();

        List<EntityEventRequest> requestsToSchedule = new ArrayList<>();
        List<EntityEventRequest> requestsToRemove = new ArrayList<>();

        // Get a single request for each distinct urn, keep the duplicated ones to remove them
        for (EntityEventRequest request : requests) {
            if (distinctToDoRequests.stream().noneMatch(r -> r.getUrn().equals(request.getUrn()))) {
                distinctToDoRequests.add(request);
            } else {
                // It's a duplicate
                requestsToRemove.add(request);
            }
        }

        // Get existing requests in SCHEDULED, RUNNING or FAILED status
        List<EntityEventRequest> allExistingRequests = entityEventRequestRepository.findByUrnInAndStatusNotToDo(
            distinctToDoRequests.stream().map(EntityEventRequest::getUrn).toList());
        Map<String, List<EntityEventRequest>> existingRequestsByUrn = allExistingRequests.stream()
                                                                                         .collect(Collectors.groupingBy(
                                                                                             EntityEventRequest::getUrn));

        // Remove the requests for which a job with the same urn is already scheduled and ignore the ones for which a
        // job is already running, they can't be run now but still need to be run later. Also remove the failed
        // requests with the same urn.
        for (EntityEventRequest request : distinctToDoRequests) {
            List<EntityEventRequest> existingRequests = existingRequestsByUrn.getOrDefault(request.getUrn(),
                                                                                           new ArrayList<>());
            // Remove all failed request as one for the same urn will be scheduled
            List<EntityEventRequest> existingFailedRequests = existingRequests.stream()
                                                                              .filter(r -> r.getStatus()
                                                                                           == EntityEventRequestStatus.FAILED)
                                                                              .toList();
            requestsToRemove.addAll(existingFailedRequests);
            if (existingRequests.stream().anyMatch(r -> r.getStatus() == EntityEventRequestStatus.SCHEDULED)) {
                // If there is a SCHEDULED request we can remove this one
                requestsToRemove.add(request);
            } else if (existingRequests.stream().noneMatch(r -> r.getStatus() == EntityEventRequestStatus.RUNNING)) {
                // If there is no RUNNING request we can schedule this one (this mean there is either no existing
                // requests or only FAILED ones)
                requestsToSchedule.add(request);
            }
            // Else we can't schedule the request for now as there is one job already running. It will be scheduled
            // in a later iteration of the scheduler.
        }

        // Schedule jobs
        for (EntityEventRequest request : requestsToSchedule) {
            Set<JobParameter> parameters = new HashSet<>();
            parameters.add(new JobParameter(UpdateEntityIntoEsJob.URN_PARAMETER, request.getUrn()));
            parameters.add(new JobParameter(UpdateEntityIntoEsJob.USER_TO_NOTIFY_PARAMETER, request.getUserToNotify()));
            parameters.add(new JobParameter(UpdateEntityIntoEsJob.ROLE_TO_NOTIFY_PARAMETER, request.getRoleToNotify()));
            parameters.add(new JobParameter(UpdateEntityIntoEsJob.REQUEST_ID_PARAMETER, request.getId()));
            jobInfoService.createAsQueued(new JobInfo(false,
                                                      0,
                                                      parameters,
                                                      authResolver.getUser(),
                                                      UpdateEntityIntoEsJob.class.getName()));
            entityEventRequestRepository.scheduleRequest(request.getId());
        }

        // Remove duplicates and scheduled requests
        entityEventRequestRepository.deleteAllInBatch(requestsToRemove);

        return requestsPage;
    }

    @MultitenantTransactional
    @Override
    public void saveEntityUpdateRequests(List<EntityEventRequest> entityEventRequests) {
        entityEventRequestRepository.saveAll(entityEventRequests);
    }

    @MultitenantTransactional
    @Override
    public void retryEntityUpdateRequests(Long requestId) {
        entityEventRequestRepository.retryRequest(requestId);
    }

    @MultitenantTransactional
    @Override
    public void deleteEntityRequest(Long requestId) {
        entityEventRequestRepository.deleteById(requestId);
    }

    @MultitenantTransactional
    @Override
    public void runEntityRequest(Long requestId) {
        entityEventRequestRepository.runRequest(requestId);
    }

    @MultitenantTransactional
    @Override
    public void failedEntityUpdateRequests(Long requestId) {
        entityEventRequestRepository.requestFailed(requestId);
    }

    /**
     * Validate given DataObject. If no error, add it to given set else log validation errors
     */
    private void validateDataObject(Set<DataObject> toSaveObjects,
                                    DataObject dataObject,
                                    BulkSaveResult bulkSaveResult,
                                    StringBuilder buf,
                                    Long datasourceId) {
        Errors errorsObject = new MapBindingResult(new HashMap<>(), dataObject.getIpId().toString());
        List<String> errors = null;
        // If some validation errors occur, don't index data object
        try {
            dataObjectService.validate(dataObject, errorsObject, ValidationMode.CREATION);
        } catch (EntityInvalidException e) {
            // If such an exception has been thrown, it contains all errors ( as a List<String>) else errors are
            // described into errorsObject
            errors = e.getMessages();
        }
        // No exception thrown but still validation errors
        if ((errors == null) && errorsObject.hasErrors()) {
            errors = toErrors(errorsObject);
        }
        // No error => dataObject is valid
        if (errors == null) {
            // Check if there is no error already existing on that dataObject (ie from geo manipulation)
            boolean noExistingError = bulkSaveResult.getInErrorDocCause(dataObject.getDocId()) == null;
            if (noExistingError) {
                toSaveObjects.add(dataObject);
            }
        } else {
            // Validation error
            String msg = "Data object with id '"
                         + dataObject.getDocId()
                         + "' not indexed due to "
                         + errors.size()
                         + " validation error(s):\n"
                         + errors.stream().collect(Collectors.joining("\n"));
            // Log error msg
            LOGGER.warn(msg);
            // Append error msg to buffer
            buf.append("\n").append(msg);
            // Add data object in error into summary result
            bulkSaveResult.addInErrorDoc(dataObject.getDocId(),
                                         new EntityInvalidException(msg),
                                         dataObject.getFeature().getSession(),
                                         dataObject.getFeature().getSessionOwner());
        }
    }

    @Override
    public BulkSaveResult upsertDataObjects(String tenant,
                                            Long datasourceId,
                                            OffsetDateTime creationDate,
                                            List<DataObject> objects,
                                            String datasourceIngestionId,
                                            boolean buildingIndex) throws ModuleException {
        StringBuilder buf = new StringBuilder();
        BulkSaveResult bulkSaveResult = new BulkSaveResult();
        // For all objects, it is necessary to set datasourceId, creation date AND to validate them
        Set<DataObject> toSaveObjects = new HashSet<>();
        for (DataObject dataObject : objects) {
            // Lets handle virtual_id here
            if (dataObject.isLast()) {
                dataObject.setVirtualId();
            } else {
                dataObject.removeVirtualId();
            }
            dataObject.setDataSourceId(datasourceId);
            dataObject.setCreationDate(creationDate);
            dataObject.setLastUpdate(creationDate);
            if (Strings.isNullOrEmpty(dataObject.getLabel())) {
                dataObject.setLabel(dataObject.getIpId().toString());
            }
            normalizeAndReprojectGeometry(dataObject, bulkSaveResult, buf);
            // Validate data object
            validateDataObject(toSaveObjects, dataObject, bulkSaveResult, buf, datasourceId);
        }

        String indexOrAlias = getIndexOrAliasName(tenant, buildingIndex);
        try {
            esRepoFacade.upsertToIndexOrAlias(indexOrAlias, bulkSaveResult, toSaveObjects, buf);
        } catch (ElasticsearchException e) {
            String errorMsg = String.format("Upsert FAILED (tenant=%s, index or alias=%s, "
                                            + "total to save=%d, saved docs=%d, docs in errors=%d): ",
                                            tenant,
                                            indexOrAlias,
                                            toSaveObjects.size(),
                                            bulkSaveResult.getSavedDocsCount(),
                                            bulkSaveResult.getInErrorDocsCount());

            LOGGER.error(errorMsg);
            throw new ModuleException(errorMsg, e);
        } finally {
            publishEventsAndManageErrors(tenant, datasourceIngestionId, buf, bulkSaveResult);
        }

        return bulkSaveResult;
    }

    /**
     * Update the dataObject if it contains a geometry
     * Compute two different geometry :
     * - a normalized version of the geometry in the same CRS
     * - a WGS 84 projected version of the normalized geometry
     * This normalization can produce errors if the geometry is not valid
     */
    private void normalizeAndReprojectGeometry(DataObject dataObject,
                                               BulkSaveResult bulkSaveResult,
                                               StringBuilder errorBuffer) {
        DataObjectFeature feature = dataObject.getFeature();
        // This geometry has been set by plugin, IT IS NOT NORMALIZED
        IGeometry geometry = feature.getGeometry();
        if (geometry != null && !Objects.equals(geometry.getType(), GeoJsonType.UNLOCATED)) {
            // Always normalize geometry in its origin CRS
            try {
                feature.setNormalizedGeometry(GeoHelper.normalize(geometry));
                // Then manage projected (or not) geometry into WGS84
                if (!feature.getCrs().get().equals(Crs.WGS_84.toString())) {
                    try {
                        // Transform to Wgs84...(not normalized one from its origin CRS)
                        IGeometry wgs84Geometry = GeoHelper.transform(geometry,
                                                                      Crs.valueOf(feature.getCrs().get()),
                                                                      Crs.WGS_84);
                        // ...and save it onto DataObject after having normalized it
                        dataObject.setWgs84(GeoHelper.normalize(wgs84Geometry));
                    } catch (IllegalArgumentException e) {
                        throw new RsRuntimeException(String.format("Given Crs '%s' is not allowed.",
                                                                   feature.getCrs().get()), e);
                    }
                } else { // Even if Crs is WGS84, don't forget to normalize geometry (already done into feature)
                    dataObject.setWgs84(feature.getNormalizedGeometry());
                }
                String json = gson.toJson(geometry);

                GeoJSONReader reader = makeGeoJsonReader(makeFactory(projectGeoSettings.getShouldManagePolesOnGeometries()));
                Shape read = reader.read(putTypeInFirstPosition(json));

                GeoPoint nwPoint = new GeoPointBuilder(read.getBoundingBox().getMaxY(),
                                                       read.getBoundingBox().getMinX()).build();
                dataObject.setNwPoint(nwPoint);
                GeoPoint sePoint = new GeoPointBuilder(read.getBoundingBox().getMinY(),
                                                       read.getBoundingBox().getMaxX()).build();
                dataObject.setSePoint(sePoint);

            } catch (InvalidShapeException e) {
                // Validation error
                normalizeGeometryError(dataObject,
                                       bulkSaveResult,
                                       errorBuffer,
                                       feature,
                                       "Failed to normalize the feature geometry : %s.\nFeature label = %s, ProviderId = %s\n",
                                       e.getMessage());
            } catch (ParseException e) {
                // Validation error
                normalizeGeometryError(dataObject,
                                       bulkSaveResult,
                                       errorBuffer,
                                       feature,
                                       "Failed to generate bbox from geometry : %s.\nFeature label = %s, ProviderId = %s\n",
                                       e.getMessage());
            } catch (IOException e) {
                // Validation error
                normalizeGeometryError(dataObject,
                                       bulkSaveResult,
                                       errorBuffer,
                                       feature,
                                       "Failed to generate bbox from geometry : %s.\nFeature label = %s, ProviderId = %s\n",
                                       e.getMessage());
            }
        }
    }

    /**
     * Without this dirty hack, GeoJSONReader can not read geometries where the type appears after the coordinates.
     * https://github.com/locationtech/spatial4j/issues/156
     */
    private String putTypeInFirstPosition(String json) {
        String type = json.replaceFirst("(.*)(\"type\"\\s*:\\s*\"[^\"]*?\")(.*)", "$2");
        return "{" + type + "," + json.replaceFirst("\\{", "");
    }

    private void normalizeGeometryError(DataObject dataObject,
                                        BulkSaveResult bulkSaveResult,
                                        StringBuilder errorBuffer,
                                        DataObjectFeature feature,
                                        String s,
                                        String message) {
        String msg = String.format(s, message, feature.getLabel(), feature.getProviderId());
        // Log error msg
        LOGGER.warn(msg);
        errorBuffer.append(msg);
        // Add data object in error into summary result
        bulkSaveResult.addInErrorDoc(dataObject.getDocId(),
                                     new EntityInvalidException(msg),
                                     dataObject.getFeature().getSession(),
                                     dataObject.getFeature().getSessionOwner());
    }

    private Function<JtsSpatialContextFactory, JtsSpatialContextFactory> makeFactory(boolean geo) {
        return factory -> {
            factory.geo = geo;
            factory.shapeFactoryClass = JtsShapeFactory.class;
            return factory;
        };
    }

    private GeoJSONReader makeGeoJsonReader(Function<JtsSpatialContextFactory, JtsSpatialContextFactory> makeFactory) {
        JtsSpatialContextFactory factory = makeFactory.apply(new JtsSpatialContextFactory());
        return new GeoJSONReader(new JtsSpatialContext(factory), factory);
    }

    /**
     * Publish events concerning data objects indexation status (indexed or in error), notify admin and update detailed
     * save bulk result message in case of errors
     */
    private void publishEventsAndManageErrors(String tenant,
                                              String datasourceIngestionId,
                                              StringBuilder buf,
                                              BulkSaveResult bulkSaveResult) {
        if (bulkSaveResult.getSavedDocsCount() != 0) {
            // Session needs to know when an internal DataObject is indexed (if DataObject is not internal, it doesn't
            // care)
            bulkSaveResult.getSavedDocPerSessionOwner()
                          .forEach((sessionOwner, savedDocPerSession) -> savedDocPerSession.forEach((session, savedDocCount) -> sessionNotifier.notifyIndexedSuccess(
                              sessionOwner,
                              session,
                              savedDocCount)));
        }
        if (bulkSaveResult.getInErrorDocsCount() > 0) {
            // Session needs to know when an internal DataObject is cannot be indexed (if DataObject is not internal, it doesn't
            // care)
            bulkSaveResult.getInErrorDocPerSessionOwner()
                          .forEach((sessionOwner, inErrorDocPerSession) -> inErrorDocPerSession.forEach((session, inErrorDocCount) -> sessionNotifier.notifyIndexedError(
                              sessionOwner,
                              session,
                              inErrorDocCount)));
        }
        // If there are errors, notify Admin
        if (buf.length() > 0) {
            // Also add detailed message to datasource ingestion
            Optional<DatasourceIngestion> oDsIngestion = dsIngestionRepository.findById(datasourceIngestionId);
            if (oDsIngestion.isPresent()) {
                DatasourceIngestion dsIngestion = oDsIngestion.get();
                String notifTitle = String.format("'%s' Datasource ingestion error", dsIngestion.getLabel());
                self.createNotificationForAdmin(tenant, notifTitle, buf.toString(), NotificationLevel.ERROR);
                bulkSaveResult.setDetailedErrorMsg(buf.toString());
            }
        }
    }

    @Override
    public boolean deleteDataObject(String index, String ipId) {
        // get object deleted
        DataObject obj = esRepoFacade.get(index, EntityType.DATA.toString(), ipId, DataObject.class);
        // decrement the related session
        if (obj != null && obj.getFeature() != null) {
            sessionNotifier.notifyIndexDeletion(obj.getFeature().getSessionOwner(), obj.getFeature().getSession());
        }
        // delete object
        return esRepoFacade.deleteFromIndexOrAlias(index, EntityType.DATA.toString(), ipId);
    }

    @Override
    public void deleteDataObjectsFromDatasource(String tenant, Long datasourceId) {
        esRepoFacade.deleteByDatasourceInAliasAndBuildingIndex(tenant, datasourceId);
    }

    @Override
    public void deleteDataObjectsAndUpdate(String tenant, Set<String> ipIds) {
        esRepoFacade.runOnCurrentIndexAndBuildingIndex(tenant,
                                                       index -> deleteDataObjectsAndUpdateInOneIndex(tenant,
                                                                                                     index,
                                                                                                     ipIds));
    }

    private void deleteDataObjectsAndUpdateInOneIndex(String tenant, String index, Set<String> ipIds) {
        Set<UniformResourceName> allDatasetUrns = new HashSet<>();

        // Warning: deletion by document ID using an alias is only supported for aliases mapped to a single index
        String alias = IndexAliasResolver.resolveAliasName(tenant);

        LOGGER.info("Deleting {} data object(s) for index {}", ipIds.size(), alias);
        // Delete data and collect datasets to update
        for (String ipId : ipIds) {
            try {
                Set<String> tags = deleteDataObjectReturningTags(index, ipId);
                // Extract datasets from tags
                allDatasetUrns.addAll(extractDatasetsFromTags(tags));
            } catch (RsRuntimeException e) {
                LOGGER.error("Cannot delete feature ({})", ipId, e);
            }
        }

        if (!allDatasetUrns.isEmpty()) {
            // Make change available
            esRepoFacade.refreshIndex(index);
            updateDatasetComputedProperties(tenant, index, allDatasetUrns);
        }
    }

    /**
     * Delete given data object from Elasticsearch
     *
     * @param index concerned elastic index
     * @param ipId  id of Data object
     * @return if data object properly deleted, return tags else null
     */
    private Set<String> deleteDataObjectReturningTags(String index, String ipId) {
        // get object deleted
        LOGGER.debug("[DELETE] Loading data to delete : {}", ipId);
        DataObject obj = esRepoFacade.getWithIndex(index, EntityType.DATA.toString(), ipId, DataObject.class);
        // decrement the related session
        final DataObjectFeature feature = obj == null ? null : obj.getFeature();
        if (feature != null) {
            sessionNotifier.notifyIndexDeletion(feature.getSessionOwner(), feature.getSession());
        }
        // delete object
        LOGGER.debug("[DELETE] Deleting data {}", ipId);
        return esRepoFacade.deleteFromIndexOrAlias(index, EntityType.DATA.toString(), ipId) && obj != null ?
            obj.getTags() :
            null;
    }

    private Set<UniformResourceName> extractDatasetsFromTags(Set<String> tags) {
        Set<UniformResourceName> datasetURNs;
        if (tags != null) {
            datasetURNs = new HashSet<>();
            for (String tag : tags) {
                try {
                    UniformResourceName urn = UniformResourceName.fromString(tag);
                    if (EntityType.DATASET.equals(urn.getEntityType())) {
                        datasetURNs.add(urn);
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.debug("Skipping tag {} : not in URN format", tag);
                }
            }
        } else {
            datasetURNs = Collections.EMPTY_SET;
        }
        return datasetURNs;
    }

    /**
     * Update computed properties on specified datasets
     *
     * @param index       concerned index
     * @param datasetURNs list of dataset to update
     */
    private void updateDatasetComputedProperties(String tenant, String index, Set<UniformResourceName> datasetURNs) {
        // Update datasets
        if (!datasetURNs.isEmpty()) {
            try {
                // Load datasets
                List<Dataset> datasets = datasetService.loadAllWithRelations(datasetURNs.toArray(new UniformResourceName[0]));
                datasets.forEach(dataset -> {
                    try {
                        computeComputedAttributes(dataset, null, tenant);
                        prepareDatasetForEs(dataset);
                        esRepoFacade.saveToIndexOrAlias(index, dataset);
                        LOGGER.info("Dataset {} computed properties updated", dataset.getId());
                    } catch (ModuleException e) {
                        LOGGER.error("Dataset {} computed properties cannot be updated!", dataset.getId(), e);
                    }
                });
            } catch (ModuleException e) {
                LOGGER.error("Cannot update datasets computed properties", e);
            }
        }
    }

    @Override
    public void updateAllDatasets(String tenant, OffsetDateTime updateDate, boolean skipDissociationStep)
        throws ModuleException {
        self.updateDatasets(tenant,
                            datasetService.findAll(),
                            null,
                            updateDate,
                            true,
                            null,
                            false,
                            skipDissociationStep);

        // If there is a building index, we have to update the datasets into this building index too
        if (indexAliasResolver.resolveBuildingIndex(tenant).isPresent()) {
            self.updateDatasets(tenant,
                                datasetService.findAll(),
                                null,
                                updateDate,
                                true,
                                null,
                                true,
                                skipDissociationStep);
        }

    }

    @Override
    public void updateAllCollections(String tenant, OffsetDateTime updateDate) throws ModuleException {
        for (fr.cnes.regards.modules.dam.domain.entities.Collection col : collectionService.findAll()) {
            updateEntityIntoEs(tenant, col.getIpId(), null, updateDate, false, null, false, false);

            // If there is a building index, we have to update the collections into this buildingt index too
            if (indexAliasResolver.resolveBuildingIndex(tenant).isPresent()) {
                updateEntityIntoEs(tenant, col.getIpId(), null, updateDate, false, null, true, false);

            }
        }
    }

    /**
     * Send a message to IngesterService (or whoever want to listen to it) concerning given datasourceIngestionId
     *
     * @param dsId {@link DatasourceIngestion} id
     */
    public void sendDataSourceMessage(String message, String dsId) {
        if (dsId != null) {
            String msg = String.format("%s: %s",
                                       ISO_TIME_UTC.format(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC)),
                                       message);
            eventPublisher.publishEvent(new DataSourceMessageEvent(this, runtimeTenantResolver.getTenant(), msg, dsId));
        }
    }

    /**
     * Send a message to IngesterService (or whoever want to listen to it) concerning given datasourceIngestion
     *
     * @param datasourceIngestion {@link DatasourceIngestion} id
     */
    public void sendDataSourceMessage(String message, DatasourceIngestion datasourceIngestion) {
        if (datasourceIngestion != null) {
            sendDataSourceMessage(message, datasourceIngestion.getId());
        }
    }

    private record DatasetAccessWrite(String groupName,
                                      String dataset,
                                      boolean dataAccessRight) {

    }

    /**
     * Resolves the Elasticsearch index or alias name if the request is is a for a building index or not
     */
    private String getIndexOrAliasName(String tenant, boolean buildingIndex) {
        return buildingIndex ?
            indexAliasResolver.resolveBuildingIndex(tenant)
                              .orElseThrow(() -> new IllegalStateException("No building index found for tenant "
                                                                           + tenant)) :
            IndexAliasResolver.resolveAliasName(tenant);
    }

}
