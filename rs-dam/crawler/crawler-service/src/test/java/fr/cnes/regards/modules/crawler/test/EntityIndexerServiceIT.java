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
package fr.cnes.regards.modules.crawler.test;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.encryption.exception.EncryptionException;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.Polygon;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.service.handlers.SessionAgentHandlerService;
import fr.cnes.regards.framework.modules.session.agent.service.update.AgentSnapshotService;
import fr.cnes.regards.framework.modules.session.commons.dao.ISessionStepRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.modules.crawler.plugins.TestDataAccessRightPlugin;
import fr.cnes.regards.modules.crawler.plugins.TestDataSourcePlugin;
import fr.cnes.regards.modules.crawler.service.IEntityIndexerService;
import fr.cnes.regards.modules.crawler.service.session.SessionNotifierPropertyEnum;
import fr.cnes.regards.modules.dam.dao.dataaccess.IAccessGroupRepository;
import fr.cnes.regards.modules.dam.dao.dataaccess.IAccessRightRepository;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.*;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.dam.plugin.dataaccess.accessright.NewDataObjectsAccessPlugin;
import fr.cnes.regards.modules.dam.service.dataaccess.IAccessGroupService;
import fr.cnes.regards.modules.dam.service.dataaccess.IAccessRightService;
import fr.cnes.regards.modules.dam.service.datasources.IDataSourceService;
import fr.cnes.regards.modules.dam.service.entities.IDatasetService;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchType;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.model.dao.IModelRepository;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.IModelService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Test class.
 *
 * @author Sébastien Binda
 */
@DirtiesContext(hierarchyMode = HierarchyMode.EXHAUSTIVE)
@ActiveProfiles({ "indexer-service", "noscheduler" })
@TestPropertySource(locations = { "classpath:test-indexer.properties" },
    properties = { "regards.tenant=entity_indexer", "spring.jpa.properties.hibernate.default_schema=entity_indexer" })
public class EntityIndexerServiceIT extends AbstractRegardsIT {

    private static final String SESSION_OWNER = "SOURCE 1";

    private static final String SESSION = "SESSION 1";

    private static String TENANT = "entity_indexer";

    private final List<DataObject> objects = Lists.newArrayList();

    @Autowired
    private IEsRepository esRepository;

    @Autowired
    private IModelService modelService;

    @Autowired
    private IDatasetService datasetService;

    @Autowired
    private IDataSourceService datasourceService;

    @Autowired
    private IEntityIndexerService indexerService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAccessGroupService groupService;

    @Autowired
    private IAccessRightService rightsService;

    @Autowired
    private ISearchService searchService;

    @Autowired
    private IAccessRightRepository arRepo;

    @Autowired
    private IAccessGroupRepository agRepo;

    @Autowired
    private IDatasetRepository dsRepo;

    @Autowired
    private IModelRepository modelRepo;

    @Autowired
    private IPluginConfigurationRepository pluginRepo;

    @SpyBean
    private IPublisher publisher;

    @Autowired
    private SessionAgentHandlerService stepHandlerService;

    @Autowired
    private ISessionStepRepository sessionStepRepo;

    @Autowired
    private IStepPropertyUpdateRequestRepository stepRepo;

    @Autowired
    private AgentSnapshotService agentSnapshotService;

    //    @Autowired
    //    private Gson gson;

    private Dataset dataset;

    private Dataset dataset2;

    private PluginConfiguration datasource;

    private PluginConfiguration dataAccessPlugin;

    private Model model;

    private Model dsModel;

    private AccessGroup group1;

    private AccessGroup group2;

    private AccessGroup group3;

    private AccessGroup group4;

    private AccessRight ar;

    private AccessRight ar2;

    private AccessRight ar3;

    private AccessRight ar4;

    @Autowired
    private IPluginService pluginService;

    private void initIndex(String index) {
        if (esRepository.indexExists(index)) {
            esRepository.deleteIndex(index);
        }
        esRepository.createIndex(index);
        Arrays.stream(EntityType.values()).map(EntityType::toString).toArray(length -> new String[length]);
    }

    @After
    public void clear() {
        arRepo.deleteAll();
        agRepo.deleteAll();
        dsRepo.deleteAll();
        modelRepo.deleteAll();
        pluginRepo.deleteAll();
        stepRepo.deleteAll();
        sessionStepRepo.deleteAll();
    }

    @Before
    public void init() throws ModuleException {
        clear();

        runtimeTenantResolver.forceTenant(TENANT);
        Mockito.clearInvocations(publisher);
        initIndex(TENANT);
        createModels();
        datasource = createDataSource();
        dataset = createDataset("dataset1", datasource);
        group1 = createGroup("group1");
        group2 = createGroup("group2");
        group3 = createGroup("group3");
        group4 = createGroup("group4");
        objects.add(createObject("DO1", "DataObject 1"));
        objects.add(createObject("DO2", "DataObject 2"));
        objects.add(createObject("DO3", "DataObject 3"));
        dataAccessPlugin = createDataAccessPlugin();
        indexerService.createDataObjects(TENANT, datasource.getId(), OffsetDateTime.now().minusDays(1), objects, "");
        List<DataObject> otherObj = Lists.newArrayList();
        otherObj.add(createObject("DO4", "DataObject 4"));
        otherObj.add(createObject("DO5", "DataObject 5"));
        otherObj.add(createObject("DO6", "DataObject 6"));
        indexerService.createDataObjects(TENANT, datasource.getId(), OffsetDateTime.now().minusDays(10), otherObj, "");
        objects.addAll(otherObj);
        indexerService.updateEntityIntoEs(TENANT, dataset.getIpId(), OffsetDateTime.now(), false);
    }

    private AccessGroup createGroup(String name) throws EntityAlreadyExistsException {
        return groupService.createAccessGroup(new AccessGroup(name));
    }

    private void createModels() throws ModuleException {
        model = createModel(EntityType.DATA, "DO_MODEL");
        dsModel = createModel(EntityType.DATASET, "DS_MODEL");
    }

    private Dataset createDataset(String label, PluginConfiguration datasource) throws ModuleException {
        Dataset dataset = new Dataset(dsModel, TENANT, label, label);
        dataset.setSubsettingClause(ICriterion.all());
        dataset.setDataSource(datasource);
        dataset.setOpenSearchSubsettingClause("");
        // Create dataset
        return datasetService.create(dataset);
    }

    private Model createModel(EntityType type, String label) throws ModuleException {
        Model model = new Model();
        model.setName(label);
        model.setType(type);
        model.setVersion("1.0");
        model.setDescription("TEST");
        return modelService.createModel(model);
    }

    private PluginConfiguration createDataSource() throws ModuleException {
        Set<IPluginParam> param = IPluginParam.set(IPluginParam.build(TestDataSourcePlugin.MODEL,
                                                                      PluginParameterTransformer.toJson(model)));
        return datasourceService.createDataSource(PluginConfiguration.build(TestDataSourcePlugin.class, null, param));
    }

    private PluginConfiguration createDataAccessPlugin()
        throws EntityInvalidException, EntityNotFoundException, EncryptionException {
        Set<IPluginParam> param = IPluginParam.set(IPluginParam.build(TestDataAccessRightPlugin.LABEL_PARAM,
                                                                      objects.get(0).getLabel()));
        return PluginConfiguration.build(TestDataAccessRightPlugin.class, null, param);
    }

    private PluginConfiguration createNewDataAccessPlugin()
        throws EntityInvalidException, EntityNotFoundException, EncryptionException {
        Set<IPluginParam> param = IPluginParam.set(IPluginParam.build(NewDataObjectsAccessPlugin.NB_DAYS_PARAM, 5));
        return PluginConfiguration.build(NewDataObjectsAccessPlugin.class, null, param);
    }

    private PluginConfiguration createOldDataAccessPlugin()
        throws EntityInvalidException, EntityNotFoundException, EncryptionException {
        Set<IPluginParam> param = IPluginParam.set(IPluginParam.build(NewDataObjectsAccessPlugin.NB_DAYS_PARAM, 5));
        return pluginService.savePluginConfiguration(PluginConfiguration.build(NewDataObjectsAccessPlugin.class,
                                                                               null,
                                                                               param));
    }

    private DataObject createObject(String id, String label) {
        // create dataObjectFeature
        DataObjectFeature dataObjectFeature = new DataObjectFeature(OaisUniformResourceName.pseudoRandomUrn(
            OAISIdentifier.AIP,
            EntityType.DATA,
            TENANT,
            1), id, label);
        dataObjectFeature.setSessionOwner(SESSION_OWNER);
        dataObjectFeature.setSession(SESSION);

        // create dataObject with dataObjectFeature
        DataObject dataObject = new DataObject(model, dataObjectFeature);

        // for this test, lets assume that there is only 1 version of dataobjects
        dataObject.setLast(true);
        return dataObject;
    }

    @Test
    public void checkDataObjectsDynamicUpdate() throws ModuleException {
        // 1. Init method has created 1xDATASET and 6xDATA
        runtimeTenantResolver.forceTenant(TENANT);

        // Create an accessRight to the dataset with dynamic date parameter filter
        PluginConfiguration newPluginConf = createNewDataAccessPlugin();
        createOldDataAccessPlugin();

        AccessRight ar = new AccessRight(new QualityFilter(0, 0, QualityLevel.ACCEPTED),
                                         AccessLevel.CUSTOM_ACCESS,
                                         dataset,
                                         group1);
        ar.setDataAccessLevel(DataAccessLevel.INHERITED_ACCESS);
        ar.setDataAccessPlugin(newPluginConf);
        ar = rightsService.createAccessRight(ar);
        indexerService.updateEntityIntoEs(TENANT, dataset.getIpId(), OffsetDateTime.now(), false);

        @SuppressWarnings("rawtypes") final SimpleSearchKey<AbstractEntity> searchKey = Searches.onSingleEntity(
            EntityType.DATA);
        searchKey.setSearchIndex(TENANT);
        @SuppressWarnings("rawtypes") Page<AbstractEntity> results = searchService.search(searchKey,
                                                                                          100,
                                                                                          ICriterion.contains("groups",
                                                                                                              "group1",
                                                                                                              StringMatchType.KEYWORD));
        Assert.assertEquals(3, results.getTotalElements());

    }

    @Test
    public void checkDataObjectsAccessRights() throws ModuleException {

        // 1. Init method has created 1xDATASET and 6xDATA

        // -------------------------------------------------------------------------------
        // 2. Check that no DATA are associated to GROUP1 as no AccessRighrs are created.
        // -------------------------------------------------------------------------------
        runtimeTenantResolver.forceTenant(TENANT);
        @SuppressWarnings("rawtypes") final SimpleSearchKey<AbstractEntity> searchKey = Searches.onSingleEntity(
            EntityType.DATA);
        searchKey.setSearchIndex(TENANT);
        @SuppressWarnings("rawtypes") Page<AbstractEntity> results = searchService.search(searchKey,
                                                                                          100,
                                                                                          ICriterion.all());
        Assert.assertEquals(objects.size(), results.getTotalElements());
        results = searchService.search(searchKey,
                                       100,
                                       ICriterion.contains("groups", "group1", StringMatchType.KEYWORD));
        Assert.assertEquals(0, results.getTotalElements());

        // -------------------------------------------------------------------------------
        // 3. Create an accessRight without filter and check that all dataObjects are associated to the GROUP1
        // -------------------------------------------------------------------------------

        ar = new AccessRight(new QualityFilter(0, 0, QualityLevel.ACCEPTED), AccessLevel.FULL_ACCESS, dataset, group1);
        ar.setDataAccessLevel(DataAccessLevel.INHERITED_ACCESS);
        ar = rightsService.createAccessRight(ar);
        // All data should be only in group1
        indexerService.updateEntityIntoEs(TENANT, dataset.getIpId(), OffsetDateTime.now(), false);
        results = searchService.search(searchKey,
                                       100,
                                       ICriterion.contains("groups", "group1", StringMatchType.KEYWORD));
        Assert.assertEquals(objects.size(), results.getTotalElements());

        // -------------------------------------------------------------------------------
        // 4. Create a second accessRight without filter and check that all dataObjects are associated to the GROUP1 and GROUP2
        // -------------------------------------------------------------------------------
        ar2 = new AccessRight(new QualityFilter(0, 0, QualityLevel.ACCEPTED), AccessLevel.FULL_ACCESS, dataset, group2);
        ar2.setDataAccessLevel(DataAccessLevel.INHERITED_ACCESS);
        rightsService.createAccessRight(ar2);
        indexerService.updateEntityIntoEs(TENANT, dataset.getIpId(), OffsetDateTime.now(), false);
        // All data should be only in group1 and group2
        results = searchService.search(searchKey,
                                       100,
                                       ICriterion.and(ICriterion.contains("groups", "group1", StringMatchType.KEYWORD),
                                                      ICriterion.contains("groups",
                                                                          "group2",
                                                                          StringMatchType.KEYWORD)));
        Assert.assertEquals(objects.size(), results.getTotalElements());

        // -------------------------------------------------------------------------------
        // 5. Delete first accessRight and check that all dataObjects are associated no longer associated to GROUP1
        // -------------------------------------------------------------------------------
        rightsService.deleteAccessRight(ar.getId());
        ar = null;
        indexerService.updateEntityIntoEs(TENANT, dataset.getIpId(), OffsetDateTime.now(), false);
        // All data should be only in group2
        results = searchService.search(searchKey,
                                       100,
                                       ICriterion.and(ICriterion.not(ICriterion.contains("groups",
                                                                                         "group1",
                                                                                         StringMatchType.KEYWORD)),
                                                      ICriterion.contains("groups",
                                                                          "group2",
                                                                          StringMatchType.KEYWORD)));
        Assert.assertEquals(objects.size(), results.getTotalElements());

        // -------------------------------------------------------------------------------
        // 6. Create a third accessRight with filter and check that only one dataObject is associated to the new GROUP3
        // -------------------------------------------------------------------------------
        ar3 = new AccessRight(new QualityFilter(0, 0, QualityLevel.ACCEPTED),
                              AccessLevel.CUSTOM_ACCESS,
                              dataset,
                              group3);
        ar3.setDataAccessPlugin(dataAccessPlugin);
        ar3.setDataAccessLevel(DataAccessLevel.INHERITED_ACCESS);
        rightsService.createAccessRight(ar3);
        indexerService.updateEntityIntoEs(TENANT, dataset.getIpId(), OffsetDateTime.now(), false);
        // All data should be in group2 and only one (DO1) in group3
        results = searchService.search(searchKey,
                                       100,
                                       ICriterion.and(ICriterion.not(ICriterion.contains("groups",
                                                                                         "group1",
                                                                                         StringMatchType.KEYWORD)),
                                                      ICriterion.contains("groups", "group2", StringMatchType.KEYWORD),
                                                      ICriterion.contains("groups",
                                                                          "group3",
                                                                          StringMatchType.KEYWORD)));
        Assert.assertEquals(1, results.getTotalElements());
        results = searchService.search(searchKey,
                                       100,
                                       ICriterion.and(ICriterion.not(ICriterion.contains("groups",
                                                                                         "group1",
                                                                                         StringMatchType.KEYWORD)),
                                                      ICriterion.contains("groups", "group2", StringMatchType.KEYWORD),
                                                      ICriterion.not(ICriterion.contains("groups",
                                                                                         "group3",
                                                                                         StringMatchType.KEYWORD))));
        Assert.assertEquals(objects.size() - 1, results.getTotalElements());

        // -------------------------------------------------------------------------------
        // 7. Update the third accessRight filter and check that no dataObject is associated to the new GROUP3
        //    Filter doesn't match any existing data : label=unknown
        // -------------------------------------------------------------------------------
        dataAccessPlugin.getParameter(TestDataAccessRightPlugin.LABEL_PARAM).value("unknown");
        dataAccessPlugin = pluginService.updatePluginConfiguration(dataAccessPlugin);
        indexerService.updateEntityIntoEs(TENANT, dataset.getIpId(), OffsetDateTime.now(), false);
        // All data should be in group2 and only one (DO1) in group3
        results = searchService.search(searchKey,
                                       100,
                                       ICriterion.and(ICriterion.not(ICriterion.contains("groups",
                                                                                         "group1",
                                                                                         StringMatchType.KEYWORD)),
                                                      ICriterion.contains("groups", "group2", StringMatchType.KEYWORD),
                                                      ICriterion.not(ICriterion.contains("groups",
                                                                                         "group3",
                                                                                         StringMatchType.KEYWORD))));
        Assert.assertEquals(objects.size(), results.getTotalElements());
        // No data should be in group3 as "unkown" label is not a valid label of existing objects
        results = searchService.search(searchKey,
                                       100,
                                       ICriterion.contains("groups", "group3", StringMatchType.KEYWORD));
        Assert.assertEquals(0, results.getTotalElements());

        // -------------------------------------------------------------------------------
        // 8. Update the third accessRight filter and check that only one dataObject is associated to the new GROUP3
        //    Filter match only one existing data : label=DataObject 2
        // -------------------------------------------------------------------------------
        dataAccessPlugin.getParameter(TestDataAccessRightPlugin.LABEL_PARAM).value("DataObject 2");
        dataAccessPlugin = pluginService.updatePluginConfiguration(dataAccessPlugin);
        pluginService.cleanPluginCache();
        indexerService.updateEntityIntoEs(TENANT, dataset.getIpId(), OffsetDateTime.now(), false);
        results = searchService.search(searchKey,
                                       100,
                                       ICriterion.contains("groups", "group3", StringMatchType.KEYWORD));
        Assert.assertEquals(1, results.getTotalElements());

        // -------------------------------------------------------------------------------
        // 9. Create a second dataset and an accessRight to check that all datas are in new GROUP4 through new dataset
        // -------------------------------------------------------------------------------
        dataset2 = createDataset("dataset2", datasource);
        indexerService.updateEntityIntoEs(TENANT, dataset2.getIpId(), OffsetDateTime.now(), false);
        ar4 = new AccessRight(new QualityFilter(0, 0, QualityLevel.ACCEPTED),
                              AccessLevel.FULL_ACCESS,
                              dataset2,
                              group4);
        ar4.setDataAccessLevel(DataAccessLevel.INHERITED_ACCESS);
        ar4 = rightsService.createAccessRight(ar4);
        indexerService.updateEntityIntoEs(TENANT, dataset2.getIpId(), OffsetDateTime.now(), false);
        results = searchService.search(searchKey,
                                       100,
                                       ICriterion.contains("groups", "group4", StringMatchType.KEYWORD));
        Assert.assertEquals(objects.size(), results.getTotalElements());

        results = searchService.search(searchKey,
                                       100,
                                       ICriterion.and(ICriterion.contains("groups", "group4", StringMatchType.KEYWORD),
                                                      ICriterion.contains("groups", "group3", StringMatchType.KEYWORD),
                                                      ICriterion.contains("groups",
                                                                          "group2",
                                                                          StringMatchType.KEYWORD)));
        Assert.assertEquals(1, results.getTotalElements());

        // -------------------------------------------------------------------------------
        // 10. Update third access right to no access and check that objects are no longer in GROUP3
        // -------------------------------------------------------------------------------
        ar3.setAccessLevel(AccessLevel.NO_ACCESS);
        ar3 = rightsService.updateAccessRight(ar3.getId(), ar3);
        indexerService.updateEntityIntoEs(TENANT, dataset.getIpId(), OffsetDateTime.now(), false);
        results = searchService.search(searchKey,
                                       100,
                                       ICriterion.contains("groups", "group3", StringMatchType.KEYWORD));
        Assert.assertEquals(0, results.getTotalElements());

    }

    /**
     * Two goals:
     * -  test that if entities are tagged with ipId we can access them by looking for ipId or virtualId
     * -  test that if entities are tagged with virtualId we can access them by looking for ipId or virtualId
     *
     * @throws ModuleException
     */
    @Test
    public void testVirtualId() throws ModuleException {
        // dataobjects have been tagged with dataset ipId (check init methods)
        // so lets try to get them with tag ipId and virtualId to compare results
        final SimpleSearchKey<AbstractEntity> searchKey = Searches.onSingleEntity(EntityType.DATA);
        searchKey.setSearchIndex(TENANT);
        Page<AbstractEntity> fromIpId = searchService.search(searchKey,
                                                             100,
                                                             ICriterion.contains("tags",
                                                                                 dataset.getIpId().toString(),
                                                                                 StringMatchType.KEYWORD));
        Page<AbstractEntity> fromVirtualId = searchService.search(searchKey,
                                                                  100,
                                                                  ICriterion.contains("tags",
                                                                                      dataset.getVirtualId().toString(),
                                                                                      StringMatchType.KEYWORD));
        Assert.assertEquals(fromIpId.getSize(), fromVirtualId.getSize());
        Assert.assertTrue("results from ipId search should contains all results from virtualId search",
                          fromIpId.getContent().containsAll(fromVirtualId.getContent()));
        Assert.assertTrue("results from virtualId search should contains all results from IpId search",
                          fromVirtualId.getContent().containsAll(fromIpId.getContent()));
        // now lets add one dataobjet tagger with one of existing virtualId
        DataObject taggedWithLatest = createObject("taggedWithLatest", "Tagged With Latest");

        DataObject taggingWith = objects.get(0);
        UniformResourceName virtualId = taggingWith.getVirtualId();
        Assert.assertNotNull(virtualId);
        taggedWithLatest.addTags(virtualId.toString());
        indexerService.createDataObjects(TENANT,
                                         datasource.getId(),
                                         OffsetDateTime.now().minusDays(1),
                                         Lists.newArrayList(taggedWithLatest),
                                         "");
        Page<AbstractEntity> taggedWithVirtualId = searchService.search(searchKey,
                                                                        100,
                                                                        ICriterion.contains("tags",
                                                                                            virtualId.toString(),
                                                                                            StringMatchType.KEYWORD));
        Assert.assertTrue("we should have the object we just created and tagged",
                          taggedWithVirtualId.getContent().contains(taggedWithLatest));
        taggedWithVirtualId = searchService.search(searchKey,
                                                   100,
                                                   ICriterion.contains("tags",
                                                                       taggingWith.getIpId().toString(),
                                                                       StringMatchType.KEYWORD));
        Assert.assertTrue("we should have the object we just created and tagged with virtualId by looking for ipId",
                          taggedWithVirtualId.getContent().contains(taggedWithLatest));

        // Ok this is most likely not the right place but lets get by virtualId here, we already have all we need
        Dataset datasetByVirtualId = searchService.get(dataset.getVirtualId());
        Assert.assertEquals(dataset, datasetByVirtualId);
    }

    @Test
    public void testGetBoundingBoxFromGeometry() throws ModuleException {
        final SimpleSearchKey<AbstractEntity> searchKey = Searches.onSingleEntity(EntityType.DATA);

        searchKey.setSearchIndex(TENANT);
        DataObject taggedWithLatest = createObject("taggedWithLatest", "Tagged With Latest");

        DataObject taggingWith = objects.get(0);
        UniformResourceName virtualId = taggingWith.getVirtualId();
        Assert.assertNotNull(virtualId);
        taggedWithLatest.addTags(virtualId.toString());

        //KSFO airport geometry [-165,70],[-150,70],[-150,66],[-124.99995,66],[-124.99995,70],[-112.00005,70],[-112.00005,75],
        //          [-97.5,75],[-97.5,80],[-90,80],[-90,86],[-45,86],[-45,86.16666],[-15,86.16666],[-15,88],[120,88],
        //          [120,86.5],[180,86.5],[180,90],[-180,90],[-180,86.5],[-142.5,86.5],[-142.5,80],[-156.25005,80],
        //          [-156.25005,77],[-165,77],[-165,70]

        Polygon geometry = IGeometry.simplePolygon(-122.40305900573729,
                                                   37.63571307432668,
                                                   -122.402286529541,
                                                   37.63109096309235,
                                                   -122.39636421203612,
                                                   37.613619505104865,
                                                   -122.3818588256836,
                                                   37.604304248565484,
                                                   -122.37876892089842,
                                                   37.604508244779446,
                                                   -122.37791061401367,
                                                   37.605188228119054,
                                                   -122.37859725952147,
                                                   37.60600419992163,
                                                   -122.37181663513184,
                                                   37.614639352839696,
                                                   -122.3587703704834,
                                                   37.60940398608524,
                                                   -122.35490798950194,
                                                   37.61511527699598,
                                                   -122.36383438110352,
                                                   37.618650618224926,
                                                   -122.36443519592285,
                                                   37.619534427267155,
                                                   -122.36615180969237,
                                                   37.62062217781951,
                                                   -122.36743927001952,
                                                   37.62021427322739,
                                                   -122.36855506896973,
                                                   37.621437980289684,
                                                   -122.3646068572998,
                                                   37.62694441280498,
                                                   -122.3649501800537,
                                                   37.627828123247475,
                                                   -122.36692428588866,
                                                   37.62884777608664,
                                                   -122.36855506896973,
                                                   37.627556213461176,
                                                   -122.37190246582031,
                                                   37.628643846637885,
                                                   -122.37250328063965,
                                                   37.62789610053861,
                                                   -122.37979888916016,
                                                   37.63047919153271,
                                                   -122.38520622253417,
                                                   37.62857587003062,
                                                   -122.38863945007323,
                                                   37.63000336572688,
                                                   -122.38906860351562,
                                                   37.63347002683941,
                                                   -122.38572120666504,
                                                   37.635645104185514,
                                                   -122.38821029663086,
                                                   37.63931540283398,
                                                   -122.40022659301758,
                                                   37.63856776411051);

        taggedWithLatest.getFeature().setCrs("WGS_84");
        taggedWithLatest.setGeometry(geometry);
        indexerService.createDataObjects(TENANT,
                                         datasource.getId(),
                                         OffsetDateTime.now().minusDays(1),
                                         Lists.newArrayList(taggedWithLatest),
                                         "");

        Page<AbstractEntity> taggedWithVirtualId = searchService.search(searchKey,
                                                                        100,
                                                                        ICriterion.contains("tags",
                                                                                            virtualId.toString(),
                                                                                            StringMatchType.KEYWORD));

        DataObject dataObject = (DataObject) taggedWithVirtualId.getContent().get(0);

        Assert.assertEquals(dataObject.getNwPoint().getLon(), -122.40305900573729, 0.0000001);
        Assert.assertEquals(dataObject.getNwPoint().getLat(), 37.63931540283398, 0.0000001);
        Assert.assertEquals(dataObject.getSePoint().getLon(), -122.35490798950194, 0.0000001);
        Assert.assertEquals(dataObject.getSePoint().getLat(), 37.604304248565484, 0.0000001);
    }

    @Test
    @Purpose("Test the deletion of an object and the correct sending of StepPropertyUpdateRequestEvents")
    public void deleteDatasetTest() {
        // check object to delete exists
        UniformResourceName objectId = objects.get(0).getIpId();
        Assert.assertNotNull("Object should exist", searchService.get(objectId));

        // check the stepEvents indexation were correctly sent
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        List<StepPropertyUpdateRequestEvent> stepEvents = argumentCaptor.getAllValues()
                                                                        .stream()
                                                                        .filter(event -> event instanceof StepPropertyUpdateRequestEvent)
                                                                        .map(event -> (StepPropertyUpdateRequestEvent) event)
                                                                        .collect(Collectors.toList());
        Assert.assertEquals(
            "Unexpected number of step events created. Check the workflow of " + "StepPropertyUpdateRequestEvent sent.",
            2,
            stepEvents.size());
        checkStepEvent(stepEvents.get(0),
                       SessionNotifierPropertyEnum.PROPERTY_AIP_INDEXED.getName(),
                       "3",
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION);
        checkStepEvent(stepEvents.get(1),
                       SessionNotifierPropertyEnum.PROPERTY_AIP_INDEXED.getName(),
                       "3",
                       StepPropertyEventTypeEnum.INC,
                       SESSION_OWNER,
                       SESSION);
        Mockito.clearInvocations(publisher);

        // delete object
        indexerService.deleteDataObject(TENANT, objectId.toString());

        // check the deletion of the first object and event sent
        Assert.assertNull("Object should have been deleted", searchService.get(objectId));
        argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        stepEvents = argumentCaptor.getAllValues()
                                   .stream()
                                   .filter(event -> event instanceof StepPropertyUpdateRequestEvent)
                                   .map(event -> (StepPropertyUpdateRequestEvent) event)
                                   .collect(Collectors.toList());
        Assert.assertEquals(
            "Unexpected number of step events created. Check the workflow of " + "StepPropertyUpdateRequestEvent sent.",
            1,
            stepEvents.size());
        checkStepEvent(stepEvents.get(0),
                       SessionNotifierPropertyEnum.PROPERTY_AIP_INDEXED.getName(),
                       "1",
                       StepPropertyEventTypeEnum.DEC,
                       SESSION_OWNER,
                       SESSION);
    }

    @Test
    @Purpose("Check a reset event was sent following a deleteIndexNRecreateEntities")
    public void deleteIndexNRecreateEntitiesTest() throws ModuleException {
        // --- PREPARE TEST ---
        // Generate the process of creating sessionSteps manually
        // first save steps to database by capturing them
        ArgumentCaptor<ISubscribable> argumentCaptor = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        List<StepPropertyUpdateRequestEvent> stepEventsInit = argumentCaptor.getAllValues()
                                                                            .stream()
                                                                            .filter(event -> event instanceof StepPropertyUpdateRequestEvent)
                                                                            .map(event -> (StepPropertyUpdateRequestEvent) event)
                                                                            .collect(Collectors.toList());
        Set<String> sources = stepHandlerService.createStepRequests(stepEventsInit);
        stepHandlerService.createMissingSnapshotProcesses(sources);
        // then generate sessionSteps from steps
        agentSnapshotService.generateSessionStep(new SnapshotProcess(SESSION_OWNER, null, null), OffsetDateTime.now());

        // --- DELETE AND RECREATE ENTITIES ---
        Mockito.clearInvocations(publisher);
        indexerService.deleteIndexNRecreateEntities(TENANT);
        // check step events were sent to reset all session step properties following the deletion of entities
        ArgumentCaptor<List<? extends ISubscribable>> argumentCaptor2 = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher, Mockito.atLeastOnce()).publish(argumentCaptor2.capture());
        List<List<StepPropertyUpdateRequestEvent>> stepEvents = argumentCaptor2.getAllValues()
                                                                               .stream()
                                                                               .filter(eventList -> !eventList.isEmpty()
                                                                                   && eventList.get(0) instanceof StepPropertyUpdateRequestEvent)
                                                                               .map(eventList -> (List<StepPropertyUpdateRequestEvent>) eventList)
                                                                               .collect(Collectors.toList());
        Assert.assertTrue("Step list should contains one event. Check if stepEventList was correctly sent",
                          !stepEvents.isEmpty() && stepEvents.get(0).size() == 1);
        checkStepEvent(stepEvents.get(0).get(0),
                       SessionNotifierPropertyEnum.RESET.getName(),
                       "0",
                       StepPropertyEventTypeEnum.VALUE,
                       SESSION_OWNER,
                       SESSION);
    }

    /**
     * Method to check properties of StepPropertyUpdateRequestEvents
     */
    private void checkStepEvent(StepPropertyUpdateRequestEvent step,
                                String expectedProperty,
                                String expectedValue,
                                StepPropertyEventTypeEnum expectedType,
                                String expectedSessionOwner,
                                String expectedSession) {
        StepProperty stepProperty = step.getStepProperty();
        Assert.assertEquals("This property was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedProperty,
                            stepProperty.getStepPropertyInfo().getProperty());
        Assert.assertEquals("This value was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedValue,
                            stepProperty.getStepPropertyInfo().getValue());
        Assert.assertEquals("This type was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedType,
                            step.getType());
        Assert.assertEquals("This sessionOwner was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedSessionOwner,
                            stepProperty.getSource());
        Assert.assertEquals("This session was not expected. Check the StepPropertyUpdateRequestEvent workflow.",
                            expectedSession,
                            stepProperty.getSession());
    }
}