/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jsoniter.property.JsoniterAttributeModelPropertyTypeFinder;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.modules.crawler.dao.IDatasourceIngestionRepository;
import fr.cnes.regards.modules.crawler.plugins.TestDataSourcePlugin;
import fr.cnes.regards.modules.crawler.test.CrawlerConfiguration;
import fr.cnes.regards.modules.dam.dao.entities.IAbstractEntityRepository;
import fr.cnes.regards.modules.dam.dao.entities.IDatasetRepository;
import fr.cnes.regards.modules.dam.domain.datasources.plugins.DataSourcePluginConstants;
import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.dam.domain.entities.event.DatasetEvent;
import fr.cnes.regards.modules.dam.domain.entities.event.NotDatasetEntityEvent;
import fr.cnes.regards.modules.dam.service.entities.IDatasetService;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.dao.spatial.ProjectGeoSettings;
import fr.cnes.regards.modules.indexer.domain.spatial.Crs;
import fr.cnes.regards.modules.indexer.service.ISearchService;
import fr.cnes.regards.modules.model.dao.IAttributeModelRepository;
import fr.cnes.regards.modules.model.dao.IFragmentRepository;
import fr.cnes.regards.modules.model.dao.IModelAttrAssocRepository;
import fr.cnes.regards.modules.model.dao.IModelRepository;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.gson.MultitenantFlattenedAttributeAdapterFactory;
import fr.cnes.regards.modules.model.gson.MultitenantFlattenedAttributeAdapterFactoryEventHandler;
import fr.cnes.regards.modules.model.service.IAttributeModelService;
import fr.cnes.regards.modules.model.service.IModelService;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { CrawlerConfiguration.class })
@ActiveProfiles("noschedule") // Disable scheduling, this will activate IngesterService during all tests
@TestPropertySource(locations = { "classpath:test.properties" },
 properties = {
        //"regards.elasticsearch.deserialize.hits.strategy=GSON"
 })
@DirtiesContext(hierarchyMode = HierarchyMode.EXHAUSTIVE, classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public abstract class AbstractIndexerServiceDataSourceIT {

    protected final static Logger LOGGER = LoggerFactory.getLogger(AbstractIndexerServiceDataSourceIT.class);

    @SuppressWarnings("unused")
    protected static final String TABLE_NAME_TEST = "t_validation_1";

    protected static final String DATA_MODEL_FILE_NAME = "validationDataModel1.xml";

    protected static final String DATASET_MODEL_FILE_NAME = "validationDatasetModel1.xml";

    @Autowired
    protected MultitenantFlattenedAttributeAdapterFactoryEventHandler gsonAttributeFactoryHandler;

    @Autowired
    protected MultitenantFlattenedAttributeAdapterFactory gsonAttributeFactory;

    @Autowired
    protected JsoniterAttributeModelPropertyTypeFinder jsoniterAttributeFactoryHandler;

    @Value("${regards.tenant}")
    protected String tenant;

    @Value("${postgresql.datasource.host}")
    protected String dbHost;

    @Value("${postgresql.datasource.port}")
    protected String dbPort;

    @Value("${postgresql.datasource.name}")
    protected String dbName;

    @Value("${postgresql.datasource.username}")
    protected String dbUser;

    @Value("${postgresql.datasource.password}")
    protected String dbPpassword;

    @Value("${postgresql.datasource.schema}")
    protected String dbSchema;

    @Value("${regards.elasticsearch.host:}")
    protected String esHost;

    @Value("${regards.elasticsearch.address:}")
    protected String esAddress;

    @Value("${regards.elasticsearch.http.port}")
    protected int esPort;

    @Autowired
    protected IModelService modelService;

    @Autowired
    protected IModelRepository modelRepository;

    @Autowired
    protected IAttributeModelRepository attrModelRepo;

    @Autowired
    protected IModelAttrAssocRepository modelAttrAssocRepo;

    @Autowired
    protected IFragmentRepository fragRepo;

    @Autowired
    protected IDatasetService dsService;

    @Autowired
    protected IAttributeModelService attributeModelService;

    @Autowired
    protected ISearchService searchService;

    @Autowired
    protected IngesterService ingesterService;

    @Autowired
    protected ICrawlerAndIngesterService crawlerService;

    @Autowired
    protected IAbstractEntityRepository<AbstractEntity<?>> entityRepos;

    @Autowired
    protected IDatasetRepository datasetRepos;

    @Autowired
    protected IEsRepository esRepos;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected IPluginService pluginService;

    @Autowired
    protected IPluginConfigurationRepository pluginConfRepo;

    @Autowired
    protected IPublisher publisher;

    @Autowired
    protected IDatasourceIngestionRepository dsIngestionRepos;

    protected Model dataModel;

    protected Model datasetModel;

    protected PluginConfiguration dataSourcePluginConf;

    protected Dataset dataset1;

    protected Dataset dataset2;

    protected Dataset dataset3;

    @Autowired
    protected ProjectGeoSettings settings;

    @Before
    public void setUp() throws Exception {
        Mockito.when(settings.getCrs()).thenReturn(Crs.WGS_84);

        // Simulate spring boot ApplicationStarted event to start mapping for each tenants.
        gsonAttributeFactoryHandler.onApplicationEvent(null);
        jsoniterAttributeFactoryHandler.onApplicationEvent(null);

        runtimeTenantResolver.forceTenant(tenant);
        if (esRepos.indexExists(tenant)) {
            esRepos.deleteIndex(tenant);
        }
        esRepos.createIndex(tenant);

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
        Set<IPluginParam> param = IPluginParam
                .set(IPluginParam.build(TestDataSourcePlugin.MODEL, PluginParameterTransformer.toJson(dataModel)),
                        IPluginParam.build(DataSourcePluginConstants.MODEL_NAME_PARAM, dataModel.getName()));
        return PluginConfiguration.build(TestDataSourcePlugin.class, null, param);
    }

    /**
     * Import model definition file from resources directory
     * @param pFilename filename
     * @throws ModuleException if error occurs
     */
    private void importModel(final String pFilename) throws ModuleException {
        try {
            InputStream input = Files
                    .newInputStream(Paths.get("src", "test", "resources", "validation", "models", pFilename));
            modelService.importModel(input);

            List<AttributeModel> attributes = attributeModelService.getAttributes(null, null, null);
            gsonAttributeFactory.refresh(tenant, attributes);
        } catch (final IOException e) {
            String errorMessage = "Cannot import " + pFilename;
            throw new AssertionError(errorMessage);
        }
    }

}