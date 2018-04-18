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
import java.util.Arrays;
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
import org.elasticsearch.search.aggregations.metrics.max.ParsedMax;
import org.elasticsearch.search.aggregations.metrics.min.ParsedMin;
import org.elasticsearch.search.aggregations.metrics.sum.ParsedSum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.flywaydb.core.Flyway;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.crawler.dao.IDatasourceIngestionRepository;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionResult;
import fr.cnes.regards.modules.crawler.plugins.TestDataSourcePlugin;
import fr.cnes.regards.modules.crawler.test.CrawlerConfiguration;
import fr.cnes.regards.modules.datasources.domain.AbstractAttributeMapping;
import fr.cnes.regards.modules.datasources.domain.plugins.IDataSourcePlugin;
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
import fr.cnes.regards.modules.models.service.IAttributeModelService;
import fr.cnes.regards.modules.models.service.IModelService;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { CrawlerConfiguration.class })
@ActiveProfiles("noschedule") // Disable scheduling, this will activate IngesterService during all tests
@TestPropertySource(locations = { "classpath:test.properties" })
public class IndexerServiceDataSourceIT {

    private final static Logger LOGGER = LoggerFactory.getLogger(IndexerServiceDataSourceIT.class);

    private static final String PLUGIN_CURRENT_PACKAGE = "fr.cnes.regards.modules.crawler.plugins";

    private static final String TABLE_NAME_TEST = "t_validation_1";

    private static final String DATA_MODEL_FILE_NAME = "validationDataModel1.xml";

    private static final String DATASET_MODEL_FILE_NAME = "validationDatasetModel1.xml";

    @Autowired
    private MultitenantFlattenedAttributeAdapterFactoryEventHandler gsonAttributeFactoryHandler;

    @Autowired
    private MultitenantFlattenedAttributeAdapterFactory gsonAttributeFactory;

    @Value("${regards.tenant}")
    private String tenant;

    @Value("${postgresql.datasource.host}")
    private String dbHost;

    @Value("${postgresql.datasource.port}")
    private String dbPort;

    @Value("${postgresql.datasource.name}")
    private String dbName;

    @Value("${postgresql.datasource.username}")
    private String dbUser;

    @Value("${postgresql.datasource.password}")
    private String dbPpassword;

    @Value("${postgresql.datasource.schema}")
    private String dbSchema;

    @Value("${regards.elasticsearch.host:}")
    private String esHost;

    @Value("${regards.elasticsearch.address:}")
    private String esAddress;

    @Value("${regards.elasticsearch.http.port}")
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
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private Flyway flyway;

    @Autowired
    private IDatasourceIngestionRepository dsIngestionRepos;

    private List<AbstractAttributeMapping> modelAttrMapping;

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

        runtimeTenantResolver.forceTenant(tenant);
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
        pluginService.addPluginPackage(IDataSourcePlugin.class.getPackage().getName());
        pluginService.addPluginPackage(PLUGIN_CURRENT_PACKAGE);

        // get a model for DataObject, by importing them it also register them for (de)serialization
        importModel(DATA_MODEL_FILE_NAME);
        dataModel = modelService.getModelByName("VALIDATION_DATA_MODEL_1");

        // get a model for Dataset
        importModel(DATASET_MODEL_FILE_NAME);
        datasetModel = modelService.getModelByName("VALIDATION_DATASET_MODEL_1");

        // DataSource PluginConf
        dataSourcePluginConf = getPostgresDataSource();
        pluginService.savePluginConfiguration(dataSourcePluginConf);
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

    private PluginConfiguration getPostgresDataSource() {
        List<PluginParameter> param = PluginParametersFactory.build()
                .addParameter(TestDataSourcePlugin.MODEL, dataModel)
                .addParameter(IDataSourcePlugin.MODEL_NAME_PARAM, dataModel.getName()).getParameters();
        return PluginUtils.getPluginConfiguration(param,
                                                  TestDataSourcePlugin.class,
                                                  Arrays.asList(PLUGIN_CURRENT_PACKAGE,
                                                                IDataSourcePlugin.class.getPackage().getName()));
    }

    @Requirement("REGARDS_DSL_DAM_COL_420")
    @Purpose("Requirement is for collection. Multi search field is used here on data objects but the code is the same")
    @Test
    public void test() throws Exception {
        final String tenant = runtimeTenantResolver.getTenant();

        // Creation
        long start = System.currentTimeMillis();
        DatasourceIngestion dsi = new DatasourceIngestion(dataSourcePluginConf.getId());
        dsi.setLabel("Label");
        dsIngestionRepos.save(dsi);

        final IngestionResult summary1 = crawlerService.ingest(dataSourcePluginConf, dsi);
        System.out.println("Insertion : " + (System.currentTimeMillis() - start) + " ms");

        // Update
        start = System.currentTimeMillis();
        final IngestionResult summary2 = crawlerService.ingest(dataSourcePluginConf, dsi);
        System.out.println("Update : " + (System.currentTimeMillis() - start) + " ms");
        Assert.assertEquals(summary1.getSavedObjectsCount(), summary2.getSavedObjectsCount());

        crawlerService.startWork();
        // Create 3 Datasets on all objects
        dataset1 = new Dataset(datasetModel, tenant, "dataset label 1");
        dataset1.setDataModel(dataModel.getName());
        dataset1.setSubsettingClause(ICriterion.all());
        dataset1.setOpenSearchSubsettingClause("");
        dataset1.setLicence("licence");
        dataset1.setDataSource(dataSourcePluginConf);
        dataset1.setTags(Sets.newHashSet("BULLSHIT"));
        dataset1.setGroups(Sets.newHashSet("group0", "group11"));
        dataset1.setDescriptionFile(new DescriptionFile("http://description.for/fun"));
        dsService.create(dataset1);

        dataset2 = new Dataset(datasetModel, tenant, "dataset label 2");
        dataset2.setDataModel(dataModel.getName());
        dataset2.setSubsettingClause(ICriterion.all());
        dataset2.setOpenSearchSubsettingClause("");
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
        dataset3.setOpenSearchSubsettingClause("");
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
        Page<DataObject> objectsPage = searchService
                .search(objectSearchKey, IEsRepository.BULK_SIZE, ICriterion.eq("tags", dataset1.getIpId().toString()));
        Assert.assertTrue(objectsPage.getContent().size() > 0);
        Assert.assertEquals(summary1.getSavedObjectsCount(), objectsPage.getContent().size());

        crawlerService.startWork();
        // Delete dataset1
        dsService.delete(dataset1.getId());

        // Wait a while to permit RabbitMq sending a message to crawler service which update ES
        crawlerService.waitForEndOfWork();

        // Search again for DataObjects tagging this dataset
        objectsPage = searchService
                .search(objectSearchKey, IEsRepository.BULK_SIZE, ICriterion.eq("tags", dataset1.getIpId().toString()));
        Assert.assertTrue(objectsPage.getContent().isEmpty());
        // Adding some free tag
        objectsPage.getContent().forEach(object -> object.getTags().add("TOTO"));
        esRepos.saveBulk(tenant, objectsPage.getContent());

        esRepos.refresh(tenant);

        // Search for DataObjects tagging dataset2
        objectsPage = searchService
                .search(objectSearchKey, IEsRepository.BULK_SIZE, ICriterion.eq("tags", dataset2.getIpId().toString()));
        Assert.assertTrue(objectsPage.getContent().size() > 0);
        Assert.assertEquals(summary1.getSavedObjectsCount(), objectsPage.getContent().size());

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
    }

    private void checkDatasetComputedAttribute(final Dataset pDataset,
            final SimpleSearchKey<DataObject> pObjectSearchKey, final long objectsCreationCount) throws IOException {
        RestClient restClient;
        RestHighLevelClient client;
        try {
            restClient = RestClient
                    .builder(new HttpHost(InetAddress.getByName((!Strings.isNullOrEmpty(esHost)) ? esHost : esAddress),
                                          esPort)).build();
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
        builder.aggregation(AggregationBuilders.min("min_start_date").field("properties.date"));
        // aggregation for the max
        builder.aggregation(AggregationBuilders.max("max_stop_date").field("properties.date"));
        // aggregation for the sum
        builder.aggregation(AggregationBuilders.sum("sum_values_l1").field("properties.value_l1"));
        SearchRequest request = new SearchRequest(pObjectSearchKey.getSearchIndex().toLowerCase())
                .types(pObjectSearchKey.getSearchTypes()).source(builder);

        // get the results computed by ElasticSearch
        SearchResponse response = client.search(request);
        final Map<String, Aggregation> aggregations = response.getAggregations().asMap();

        // now lets actually test things
        Assert.assertEquals(objectsCreationCount, getDatasetProperty(pDataset, "count").getValue());
        Assert.assertEquals((long) ((ParsedSum) aggregations.get("sum_values_l1")).getValue(),
                            getDatasetProperty(pDataset, "values_l1_sum").getValue());
        // lets convert both dates to instant, it is the simpliest way to compare them
        Assert.assertEquals(Instant.parse(((ParsedMin) aggregations.get("min_start_date")).getValueAsString()),
                            ((OffsetDateTime) getDatasetProperty(pDataset, "start_date").getValue()).toInstant());
        Assert.assertEquals(Instant.parse(((ParsedMax) aggregations.get("max_stop_date")).getValueAsString()),
                            ((OffsetDateTime) getDatasetProperty(pDataset, "end_date").getValue()).toInstant());
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
            final InputStream input = Files
                    .newInputStream(Paths.get("src", "test", "resources", "validation", "models", pFilename));
            modelService.importModel(input);

            final List<AttributeModel> attributes = attributeModelService.getAttributes(null, null, null);
            gsonAttributeFactory.refresh(tenant, attributes);
        } catch (final IOException e) {
            final String errorMessage = "Cannot import " + pFilename;
            throw new AssertionError(errorMessage);
        }
    }
}