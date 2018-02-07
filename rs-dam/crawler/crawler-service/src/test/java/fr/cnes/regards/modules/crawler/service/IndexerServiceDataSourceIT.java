/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.max.InternalMax;
import org.elasticsearch.search.aggregations.metrics.min.InternalMin;
import org.elasticsearch.search.aggregations.metrics.sum.InternalSum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.crawler.domain.IngestionResult;
import fr.cnes.regards.modules.crawler.test.CrawlerConfiguration;
import fr.cnes.regards.modules.datasources.domain.AbstractAttributeMapping;
import fr.cnes.regards.modules.datasources.domain.DynamicAttributeMapping;
import fr.cnes.regards.modules.datasources.domain.StaticAttributeMapping;
import fr.cnes.regards.modules.entities.dao.IAbstractEntityRepository;
import fr.cnes.regards.modules.entities.dao.IDatasetRepository;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.entities.domain.DescriptionFile;
import fr.cnes.regards.modules.entities.domain.attribute.AbstractAttribute;
import fr.cnes.regards.modules.entities.domain.event.DatasetEvent;
import fr.cnes.regards.modules.entities.domain.event.NotDatasetEntityEvent;
import fr.cnes.regards.modules.entities.gson.MultitenantFlattenedAttributeAdapterFactory;
import fr.cnes.regards.modules.entities.gson.MultitenantFlattenedAttributeAdapterFactoryEventHandler;
import fr.cnes.regards.modules.entities.service.IDatasetService;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.dao.builder.QueryBuilderCriterionVisitor;
import fr.cnes.regards.modules.indexer.domain.JoinEntitySearchKey;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.indexer.service.Searches;
import fr.cnes.regards.modules.models.dao.IAttributeModelRepository;
import fr.cnes.regards.modules.models.dao.IFragmentRepository;
import fr.cnes.regards.modules.models.dao.IModelAttrAssocRepository;
import fr.cnes.regards.modules.models.dao.IModelRepository;
import fr.cnes.regards.modules.models.domain.Model;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;
import fr.cnes.regards.modules.models.service.IAttributeModelService;
import fr.cnes.regards.modules.models.service.IModelService;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { CrawlerConfiguration.class })
@ActiveProfiles("noschedule") // Disable scheduling, this will activate IngesterService during all tests
@Ignore
public class IndexerServiceDataSourceIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(IndexerServiceDataSourceIT.class);

    @Autowired
    private MultitenantFlattenedAttributeAdapterFactoryEventHandler gsonAttributeFactoryHandler;

    @Autowired
    private MultitenantFlattenedAttributeAdapterFactory gsonAttributeFactory;

    private static final String PLUGIN_CURRENT_PACKAGE = "fr.cnes.regards.modules.datasources.plugins";

    private static final String TABLE_NAME_TEST = "T_DATA_OBJECTS";

    private static final String DATA_MODEL_FILE_NAME = "dataModel.xml";

    private static final String DATASET_MODEL_FILE_NAME = "datasetModel.xml";

    @Value("${regards.tenant}")
    private String tenant;

    @Value("${oracle.datasource.host}")
    private String dbHost;

    @Value("${oracle.datasource.port}")
    private String dbPort;

    @Value("${oracle.datasource.name}")
    private String dbName;

    @Value("${oracle.datasource.username}")
    private String dbUser;

    @Value("${oracle.datasource.password}")
    private String dbPpassword;

    @Value("${oracle.datasource.driver}")
    private String driver;

    @Value("${regards.elasticsearch.host:}")
    private String esHost;

    @Value("${regards.elasticsearch.address:}")
    private String esAddress;

    @Value("${regards.elasticsearch.tcp.port}")
    private int esPort;

    @Autowired
    private IModelService modelService;

    @Autowired
    private IModelRepository modelRepository;

    @Autowired
    private IAttributeModelRepository attrModelRepo;

    @Autowired
    private IModelAttrAssocRepository modelAttrAssocRepo;

    @Autowired
    private IFragmentRepository fragRepo;

    @Autowired
    private IDatasetService dsService;

    // @Autowired
    // private ICollectionService collService;
    //
    // @Autowired
    // private IIndexerService indexerService;

    @Autowired
    private IAttributeModelService attributeModelService;

    @Autowired
    private ISearchService searchService;

    @Autowired
    private IIngesterService ingesterService;

    @Autowired
    private ICrawlerAndIngesterService crawlerService;

    @Autowired
    private IAbstractEntityRepository<AbstractEntity> entityRepos;

    @Autowired
    private IDatasetRepository datasetRepos;

    @Autowired
    private IEsRepository esRepos;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    private List<AbstractAttributeMapping> modelAttrMapping;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @Autowired
    private IRabbitVirtualHostAdmin rabbitVhostAdmin;

    @Autowired
    private IPublisher publisher;

    private Model dataModel;

    private Model datasetModel;

    private PluginConfiguration dataSourcePluginConf;

    private Dataset dataset1;

    private Dataset dataset2;

    private Dataset dataset3;

    private PluginConfiguration dBConnectionConf;

    @Before
    public void setUp() throws Exception {

        // Simulate spring boot ApplicationStarted event to start mapping for each tenants.
        gsonAttributeFactoryHandler.onApplicationEvent(null);

        tenantResolver.forceTenant(tenant);
        if (esRepos.indexExists(tenant)) {
            esRepos.deleteAll(tenant);
        } else {
            esRepos.createIndex(tenant);
        }

        crawlerService.setConsumeOnlyMode(false);
        ingesterService.setConsumeOnlyMode(true);

        publisher.purgeQueue(DatasetEvent.class);
        publisher.purgeQueue(NotDatasetEntityEvent.class);

        datasetRepos.deleteAll();
        entityRepos.deleteAll();
        modelAttrAssocRepo.deleteAll();
        pluginConfRepo.deleteAll();
        attrModelRepo.deleteAll();
        modelRepository.deleteAll();
        fragRepo.deleteAll();
        pluginService.addPluginPackage("fr.cnes.regards.modules.datasources.plugins");

        // get the plugin configuration for computed attributes
        initPluginConfForComputedAttributes();
        // get a model for DataObject, by importing them it also register them for (de)serialization
        importModel(DATA_MODEL_FILE_NAME);
        dataModel = modelService.getModelByName("model_1");

        // get a model for Dataset
        importModel(DATASET_MODEL_FILE_NAME);
        datasetModel = modelService.getModelByName("model_ds_1");

        // Initialize the AbstractAttributeMapping
        buildModelAttributes();

        // Connection PluginConf
        dBConnectionConf = getOracleConnectionConfiguration();
        pluginService.savePluginConfiguration(dBConnectionConf);

        // final DefaultOracleConnectionPlugin dbCtx = pluginService.getPlugin(dBConnectionConf);
        // Assume.assumeTrue(dbCtx.testConnection());

        // DataSource PluginConf
        // dataSourcePluginConf = getOracleDataSource(dBConnectionConf);
        // pluginService.savePluginConfiguration(dataSourcePluginConf);
    }

    private void initPluginConfForComputedAttributes() throws ModuleException {
    }

    @After
    public void clean() {
        entityRepos.deleteAll();
        modelAttrAssocRepo.deleteAll();
        pluginConfRepo.deleteAll();
        attrModelRepo.deleteAll();
        modelRepository.deleteAll();
        fragRepo.deleteAll();
    }

    private PluginConfiguration getOracleDataSource(final PluginConfiguration pluginConf) {
        // final List<PluginParameter> parameters = PluginParametersFactory.build()
        // .addParameterPluginConfiguration(OracleDataSourceFromSingleTablePlugin.CONNECTION_PARAM, pluginConf)
        // .addParameter(OracleDataSourceFromSingleTablePlugin.TABLE_PARAM, TABLE_NAME_TEST)
        // .addParameter(PostgreDataSourceFromSingleTablePlugin.REFRESH_RATE, "1800")
        // .addParameter(OracleDataSourceFromSingleTablePlugin.MODEL_PARAM, dataSourceModelMapping)
        // .getParameters();
        //
        // return PluginUtils.getPluginConfiguration(parameters, OracleDataSourceFromSingleTablePlugin.class,
        // Arrays.asList(PLUGIN_CURRENT_PACKAGE));
        // TODO use portgres plugin
        return null;
    }

    private PluginConfiguration getOracleConnectionConfiguration() {
        // final List<PluginParameter> parameters = PluginParametersFactory.build()
        // .addParameter(DefaultOracleConnectionPlugin.USER_PARAM, dbUser)
        // .addParameter(DefaultOracleConnectionPlugin.PASSWORD_PARAM, dbPpassword)
        // .addParameter(DefaultOracleConnectionPlugin.DB_HOST_PARAM, dbHost)
        // .addParameter(DefaultOracleConnectionPlugin.DB_PORT_PARAM, dbPort)
        // .addParameter(DefaultOracleConnectionPlugin.DB_NAME_PARAM, dbName)
        // .addParameter(DefaultOracleConnectionPlugin.MAX_POOLSIZE_PARAM, "3")
        // .addParameter(DefaultOracleConnectionPlugin.MIN_POOLSIZE_PARAM, "1").getParameters();
        //
        // return PluginUtils.getPluginConfiguration(parameters, DefaultOracleConnectionPlugin.class,
        // Arrays.asList(PLUGIN_CURRENT_PACKAGE));
        // TODO use Postgres
        return null;
    }

    private void buildModelAttributes() {
        modelAttrMapping = new ArrayList<AbstractAttributeMapping>();

        modelAttrMapping.add(new StaticAttributeMapping(AbstractAttributeMapping.PRIMARY_KEY, AttributeType.INTEGER,
                "DATA_OBJECTS_ID"));

        modelAttrMapping.add(new DynamicAttributeMapping("FILE_SIZE", AttributeType.INTEGER, "FILE_SIZE"));
        modelAttrMapping.add(new DynamicAttributeMapping("FILE_TYPE", AttributeType.STRING, "FILE_TYPE"));
        modelAttrMapping.add(new DynamicAttributeMapping("FILE_NAME_ORIGINE", AttributeType.STRING, "FILE_NAME_ORIGINE"));

        modelAttrMapping.add(new DynamicAttributeMapping("DATA_SET_ID", AttributeType.INTEGER, "DATA_SET_ID"));
        modelAttrMapping.add(new DynamicAttributeMapping("DATA_TITLE", AttributeType.STRING, "DATA_TITLE"));
        modelAttrMapping.add(new DynamicAttributeMapping("DATA_AUTHOR", AttributeType.STRING, "DATA_AUTHOR"));
        modelAttrMapping.add(new DynamicAttributeMapping("DATA_AUTHOR_COMPANY", AttributeType.STRING, "DATA_AUTHOR_COMPANY"));

        modelAttrMapping.add(new DynamicAttributeMapping("START_DATE", AttributeType.DATE_ISO8601, "START_DATE"));
        modelAttrMapping.add(new DynamicAttributeMapping("STOP_DATE", AttributeType.DATE_ISO8601, "STOP_DATE"));
        modelAttrMapping.add(new DynamicAttributeMapping("DATA_CREATION_DATE", AttributeType.DATE_ISO8601,
                "DATA_CREATION_DATE"));

        modelAttrMapping.add(new DynamicAttributeMapping("MIN", "LONGITUDE", AttributeType.INTEGER, "MIN_LONGITUDE"));
        modelAttrMapping.add(new DynamicAttributeMapping("MAX", "LONGITUDE", AttributeType.INTEGER, "MAX_LONGITUDE"));
        modelAttrMapping.add(new DynamicAttributeMapping("MIN", "LATITUDE", AttributeType.INTEGER, "MIN_LATITUDE"));
        modelAttrMapping.add(new DynamicAttributeMapping("MAX", "LATITUDE", AttributeType.INTEGER, "MAX_LATITUDE"));
        modelAttrMapping.add(new DynamicAttributeMapping("MIN", "ALTITUDE", AttributeType.INTEGER, "MIN_ALTITUDE"));
        modelAttrMapping.add(new DynamicAttributeMapping("MAX", "ALTITUDE", AttributeType.INTEGER, "MAX_ALTITUDE"));
        modelAttrMapping.add(new DynamicAttributeMapping("ANSA5_REAL", AttributeType.DOUBLE, "ANSA5_REAL"));
        modelAttrMapping.add(new DynamicAttributeMapping("ANSR5_REAL", AttributeType.DOUBLE, "ANSR5_REAL"));
        modelAttrMapping.add(new DynamicAttributeMapping("ANSE5_REAL", AttributeType.DOUBLE, "ANSE5_REAL"));
        modelAttrMapping.add(new DynamicAttributeMapping("ANSE6_STRING", AttributeType.STRING, "ANSE6_STRING"));
        modelAttrMapping.add(new DynamicAttributeMapping("ANSL6_2_STRING", AttributeType.STRING, "ANSL6_2_STRING"));
        modelAttrMapping.add(new DynamicAttributeMapping("ANSR3_INT", "frag3", AttributeType.INTEGER, "ANSR3_INT"));
        modelAttrMapping.add(new DynamicAttributeMapping("ANSL3_1_INT", "frag3", AttributeType.INTEGER, "ANSL3_1_INT"));
        modelAttrMapping.add(new DynamicAttributeMapping("ANSL3_2_INT", "frag3", AttributeType.INTEGER, "ANSL3_2_INT"));

        modelAttrMapping
                .add(new StaticAttributeMapping(AbstractAttributeMapping.THUMBNAIL, AttributeType.STRING, "ANSA7_URL"));
        modelAttrMapping
                .add(new StaticAttributeMapping(AbstractAttributeMapping.RAW_DATA, AttributeType.STRING, "ANSE7_URL"));
    }

    @Requirement("REGARDS_DSL_DAM_COL_420")
    @Purpose("Requirement is for collection. Multi search field is used here on data objects but the code is the same")
    @Test
    public void test() throws Exception {
        final String tenant = tenantResolver.getTenant();

        // Creation
        long start = System.currentTimeMillis();
        final IngestionResult summary1 = crawlerService.ingest(dataSourcePluginConf);
        System.out.println("Insertion : " + (System.currentTimeMillis() - start) + " ms");

        // Update
        start = System.currentTimeMillis();
        final IngestionResult summary2 = crawlerService.ingest(dataSourcePluginConf);
        System.out.println("Update : " + (System.currentTimeMillis() - start) + " ms");
        Assert.assertEquals(summary1.getSavedObjectsCount(), summary2.getSavedObjectsCount());

        crawlerService.startWork();
        // Create 3 Datasets on all objects
        dataset1 = new Dataset(datasetModel, tenant, "dataset label 1");
        dataset1.setDataModel(dataModel.getName());
        dataset1.setSubsettingClause(ICriterion.all());
        dataset1.setLicence("licence");
        dataset1.setDataSource(dataSourcePluginConf);
        dataset1.setTags(Sets.newHashSet("BULLSHIT"));
        dataset1.setGroups(Sets.newHashSet("group0", "group11"));
        dataset1.setDescriptionFile(new DescriptionFile("http://description.for/fun"));
        dsService.create(dataset1);

        dataset2 = new Dataset(datasetModel, tenant, "dataset label 2");
        dataset2.setDataModel(dataModel.getName());
        dataset2.setSubsettingClause(ICriterion.all());
        dataset2.setTags(Sets.newHashSet("BULLSHIT"));
        dataset2.setLicence("licence");
        dataset2.setDataSource(dataSourcePluginConf);
        dataset2.setGroups(Sets.newHashSet("group12", "group11"));
        final byte[] input = Files.readAllBytes(Paths.get("src", "test", "resources", "test.pdf"));
        final MockMultipartFile pdf = new MockMultipartFile("file", "test.pdf", MediaType.APPLICATION_PDF_VALUE, input);
        dsService.create(dataset2, pdf);

        dataset3 = new Dataset(datasetModel, tenant, "dataset label 3");
        dataset3.setDataModel(dataModel.getName());
        dataset3.setSubsettingClause(ICriterion.all());
        dataset3.setLicence("licence");
        dataset3.setDataSource(dataSourcePluginConf);
        dataset3.setGroups(Sets.newHashSet("group2"));
        dsService.create(dataset3);

        crawlerService.waitForEndOfWork();
        Thread.sleep(20_000);
        // indexerService.refresh(tenant);

        // Retrieve dataset1 from ES
        dataset1 = searchService.get(dataset1.getIpId());
        Assert.assertNotNull(dataset1);

        // SearchKey<DataObject> objectSearchKey = new SearchKey<>(tenant, EntityType.DATA.toString(),
        // DataObject.class);
        final SimpleSearchKey<DataObject> objectSearchKey = Searches.onSingleEntity(tenant, EntityType.DATA);
        // check that computed attribute were correclty done
        checkDatasetComputedAttribute(dataset1, objectSearchKey, summary1.getSavedObjectsCount());
        // Search for DataObjects tagging dataset1
        Page<DataObject> objectsPage = searchService.search(objectSearchKey, IEsRepository.BULK_SIZE,
                                                            ICriterion.eq("tags", dataset1.getIpId().toString()));
        Assert.assertTrue(objectsPage.getContent().size() > 0);
        Assert.assertEquals(summary1.getSavedObjectsCount(), objectsPage.getContent().size());
        // All data are associated with the 3 datasets so they must all have the 4 groups
        for (final DataObject object : objectsPage.getContent()) {
            Assert.assertTrue(object.getGroups().contains("group0"));
            Assert.assertTrue(object.getGroups().contains("group11"));
            Assert.assertTrue(object.getGroups().contains("group12"));
            Assert.assertTrue(object.getGroups().contains("group2"));
            Assert.assertEquals(object.getDatasetModelIds().iterator().next(), datasetModel.getId());
        }

        crawlerService.startWork();
        // Delete dataset1
        dsService.delete(dataset1.getId());

        // Wait a while to permit RabbitMq sending a message to crawler service which update ES
        crawlerService.waitForEndOfWork();

        // Search again for DataObjects tagging this dataset
        objectsPage = searchService.search(objectSearchKey, IEsRepository.BULK_SIZE,
                                           ICriterion.eq("tags", dataset1.getIpId().toString()));
        Assert.assertTrue(objectsPage.getContent().isEmpty());
        // Adding some free tag
        objectsPage.getContent().forEach(object -> object.getTags().add("TOTO"));
        esRepos.saveBulk(tenant, objectsPage.getContent());

        esRepos.refresh(tenant);

        // Search for DataObjects tagging dataset2
        objectsPage = searchService.search(objectSearchKey, IEsRepository.BULK_SIZE,
                                           ICriterion.eq("tags", dataset2.getIpId().toString()));
        Assert.assertTrue(objectsPage.getContent().size() > 0);
        Assert.assertEquals(summary1.getSavedObjectsCount(), objectsPage.getContent().size());
        // dataset1 has bee removed so objects must have "group11", "group12" (from dataset2), "group2" (from dataset3)
        // but not "group0" (only on dataset1)
        for (final DataObject object : objectsPage.getContent()) {
            Assert.assertFalse(object.getGroups().contains("group0"));
            Assert.assertTrue(object.getGroups().contains("group11"));
            Assert.assertTrue(object.getGroups().contains("group12"));
            Assert.assertTrue(object.getGroups().contains("group2"));
        }

        // Search for Dataset but with criterion on DataObjects
        // SearchKey<Dataset> dsSearchKey = new SearchKey<>(tenant, EntityType.DATA.toString(), Dataset.class);
        final JoinEntitySearchKey<DataObject, Dataset> dsSearchKey = Searches
                .onSingleEntityReturningJoinEntity(tenant, EntityType.DATA, EntityType.DATASET);
        // Page<Dataset> dsPage = searchService.searchAndReturnJoinedEntities(dsSearchKey, 1, ICriterion.all());
        Page<Dataset> dsPage = searchService.search(dsSearchKey, 1, ICriterion.all());
        Assert.assertNotNull(dsPage);
        Assert.assertFalse(dsPage.getContent().isEmpty());
        Assert.assertEquals(1, dsPage.getContent().size());

        dsPage = searchService.search(dsSearchKey, dsPage.nextPageable(), ICriterion.all());
        Assert.assertNotNull(dsPage);
        Assert.assertFalse(dsPage.getContent().isEmpty());
        Assert.assertEquals(1, dsPage.getContent().size());

        // Search for Dataset but with criterion on everything
        // SearchKey<Dataset> dsSearchKey2 = new SearchKey<>(tenant, EntityType.DATA.toString(), Dataset.class);
        final JoinEntitySearchKey<AbstractEntity, Dataset> dsSearchKey2 = Searches
                .onAllEntitiesReturningJoinEntity(tenant, EntityType.DATASET);
        dsPage = searchService.search(dsSearchKey, 1, ICriterion.all());
        Assert.assertNotNull(dsPage);
        Assert.assertFalse(dsPage.getContent().isEmpty());
        Assert.assertEquals(1, dsPage.getContent().size());

        dsPage = searchService.search(dsSearchKey2, dsPage.nextPageable(), ICriterion.all());
        Assert.assertNotNull(dsPage);
        Assert.assertFalse(dsPage.getContent().isEmpty());
        Assert.assertEquals(1, dsPage.getContent().size());

        objectsPage = searchService.multiFieldsSearch(objectSearchKey, IEsRepository.BULK_SIZE, 13,
                                                      "properties.DATA_OBJECTS_ID", "properties.FILE_SIZE",
                                                      "properties.LONGITUDE.MAX", "properties.LATITUDE.MAX");
        Assert.assertFalse(objectsPage.getContent().isEmpty());
        Assert.assertEquals(3092, objectsPage.getContent().size());
    }

    private void checkDatasetComputedAttribute(final Dataset pDataset,
            final SimpleSearchKey<DataObject> pObjectSearchKey, final long objectsCreationCount) throws IOException {
        RestClient restClient;
        RestHighLevelClient client;
        try {
            restClient = RestClient
                    .builder(new HttpHost(InetAddress.getByName((!Strings.isNullOrEmpty(esHost)) ? esHost : esAddress),
                            esPort))
                    .build();
            client = new RestHighLevelClient(restClient);

        } catch (final UnknownHostException e) {
            LOGGER.error("could not get a connection to ES in the middle of the test where we know ES is available", e);
            Assert.fail();
            return;
        }
        // lets build the request so elasticsearch can calculate the few attribute we are using in test(min(START_DATE),
        // max(STOP_DATE), sum(FILE_SIZE) via aggregation, the count of element in this context is already known:
        // objectsCreationCount
        final QueryBuilderCriterionVisitor critVisitor = new QueryBuilderCriterionVisitor();
        final ICriterion crit = ICriterion.eq("tags", pDataset.getIpId().toString());
        final QueryBuilder qb = QueryBuilders.boolQuery().must(QueryBuilders.matchAllQuery())
                .filter(crit.accept(critVisitor));
        // now we have a request on the right data
        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.query(qb).size(0);
        // lets build the aggregations
        // aggregation for the min
        builder.aggregation(AggregationBuilders.min("min_start_date").field("properties.START_DATE"));
        // aggregation for the max
        builder.aggregation(AggregationBuilders.max("max_stop_date").field("properties.STOP_DATE"));
        // aggregation for the sum
        builder.aggregation(AggregationBuilders.sum("sum_file_size").field("properties.FILE_SIZE"));
        SearchRequest request = new SearchRequest(pObjectSearchKey.getSearchIndex().toLowerCase())
                .types(pObjectSearchKey.getSearchTypes()).source(builder);

        // get the results computed by ElasticSearch
        SearchResponse response = client.search(request);
        final Map<String, Aggregation> aggregations = response.getAggregations().asMap();

        // now lets actually test things
        Assert.assertEquals(objectsCreationCount, getDatasetProperty(pDataset, "count").getValue());
        Assert.assertEquals((int) ((InternalSum) aggregations.get("sum_file_size")).getValue(),
                            getDatasetProperty(pDataset, "FILE_SIZE").getValue());
        // lets convert both dates to instant, it is the simpliest way to compare them
        Assert.assertEquals(Instant.parse(((InternalMin) aggregations.get("min_start_date")).getValueAsString()),
                            ((OffsetDateTime) getDatasetProperty(pDataset, "START_DATE").getValue()).toInstant());
        Assert.assertEquals(Instant.parse(((InternalMax) aggregations.get("max_stop_date")).getValueAsString()),
                            ((OffsetDateTime) getDatasetProperty(pDataset, "STOP_DATE").getValue()).toInstant());
        restClient.close();
    }

    private AbstractAttribute<?> getDatasetProperty(final Dataset pDataset, final String pPropertyName) {
        return pDataset.getProperties().stream().filter(p -> p.getName().equals(pPropertyName)).findAny().orElse(null);
    }

    /**
     * Import model definition file from resources directory
     * @param pFilename filename
     * @return list of created model attributes
     * @throws ModuleException if error occurs
     */
    private void importModel(final String pFilename) throws ModuleException {
        try {
            final InputStream input = Files.newInputStream(Paths.get("src", "test", "resources", pFilename));
            modelService.importModel(input);

            final List<AttributeModel> attributes = attributeModelService.getAttributes(null, null);
            gsonAttributeFactory.refresh(tenant, attributes);
        } catch (final IOException e) {
            final String errorMessage = "Cannot import " + pFilename;
            throw new AssertionError(errorMessage);
        }
    }
}
