/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.crawler.test;

import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.crawler.dao.IDatasourceIngestionRepository;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionStatus;
import fr.cnes.regards.modules.crawler.plugins.TestDataSourcePluginFailable;
import fr.cnes.regards.modules.crawler.service.service.DatasourceIngestionService;
import fr.cnes.regards.modules.crawler.service.service.IngesterService;
import fr.cnes.regards.modules.dam.service.datasources.IDataSourceService;
import fr.cnes.regards.modules.indexer.dao.CreateIndexConfiguration;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.ModelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author tguillou
 */
@ActiveProfiles({ "indexer-service", "noscheduler", "nojobs" })
@TestPropertySource(locations = { "classpath:test-crawler-it.properties" },
                    properties = { "regards.tenant=crawler_it",
                                   "spring.jpa.properties.hibernate.default_schema=crawler_it" })
class CrawlerIT extends AbstractRegardsServiceIT {

    private static final String TENANT = "crawler_it";

    private static final String INDEX = TENANT;

    private static final String MODEL_NAME = "model";

    private static final String PLUGIN_BUSINESS_ID = "test-datasource-failable-crawler-it";

    @Autowired
    private IEsRepository esRepository;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IDataSourceService datasourceService;

    @SpyBean
    private DatasourceIngestionService datasourceIngestionService;

    @Autowired
    private IngesterService ingesterService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private IDatasourceIngestionRepository datasourceIngestionRepository;

    private void initIndex() {
        if (esRepository.indexExists(CrawlerIT.INDEX)) {
            esRepository.deleteIndex(CrawlerIT.INDEX);
        }
        esRepository.createIndex(INDEX, CreateIndexConfiguration.DEFAULT);
    }

    @BeforeEach
    void setUp() throws ModuleException {
        datasourceIngestionRepository.deleteAll(); // make sure that we start with no ingestion records
        runtimeTenantResolver.forceTenant(TENANT);
        initIndex();
        createModel();
        createDataSource();
    }

    private void createModel() throws ModuleException {
        try {
            modelService.createModel(Model.build(MODEL_NAME, "description of " + MODEL_NAME, EntityType.DATA));
        } catch (EntityAlreadyExistsException e) {
            // That's perfect, the model already exists
        }
    }

    private void createDataSource() throws ModuleException {
        PluginConfiguration pluginConfig = PluginConfiguration.build(TestDataSourcePluginFailable.class, null, null);
        pluginConfig.setBusinessId(PLUGIN_BUSINESS_ID);
        try {
            datasourceService.createDataSource(pluginConfig);
        } catch (EntityInvalidException e) {
            if (e.getMessage().contains("already exists")) {
                // That's perfect, the datasource already exists
            } else {
                throw e; // Unexpected error
            }
        }
    }

    @Test
    void nominalCrawlingTest() {
        // GIVEN a datasource that has 20 objects to save
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionService);
        // WHEN launch the ingestion
        ingesterService.manageCrawlingForAllTenants();
        // THEN the ingestion should be FINISHED without any problem
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        assertEquals(20, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.FINISHED, datasourceIngestion.getStatus());
    }

    @Test
    void simpleErrorCrawlingTest() {
        // GIVEN a datasource that fails on the 2nd findAll call
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionService);
        TestDataSourcePluginFailable.configureFailAtFindAllCall(2);
        // WHEN launch the ingestion
        ingesterService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as NOT_FINISHED, with 5 objects saved (first bulk of 5 objects is ok)
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        assertEquals(5, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        assertEquals(1, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void simpleErrorCrawlingTest2() {
        // GIVEN a datasource that fails on the 3rd findAll call
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionService);
        TestDataSourcePluginFailable.configureFailAtFindAllCall(3);
        // WHEN launch the ingestion
        ingesterService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as NOT_FINISHED, with 10 objects saved (first 2 bulks of 5 objects are ok)
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        assertEquals(10, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        assertEquals(2, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void complexErrorCrawlingTestWithLongTask() {
        // GIVEN a datasource that has 20 objects to save, but fails on the 4th findAll call, with a long task
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionService);
        TestDataSourcePluginFailable.configureFailAtFindAllCall(4);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(3); // Even if task is long, it should be taken into account
        // WHEN launch the ingestion
        ingesterService.manageCrawlingForAllTenants();
        // THEN the ingestion should saved 15 objects, first 3 bulks of 5 objects are ok even if the 3rd one is long
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        assertEquals(15, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        assertEquals(3, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void complexErrorCrawlingTestFailAtFirstSave() {
        // GIVEN a datasource that fails on the 1st save call, but takes long to fail
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionService);
        TestDataSourcePluginFailable.configureFailAtSaveCalls(1);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(1);
        // WHEN launch the ingestion
        ingesterService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        assertEquals(0, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.ERROR, datasourceIngestion.getStatus());
        // THEN cursor position is the same as the first call because nothing has been saved
        assertEquals(0, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void simpleErrorCrawlingTestFailAtFirstFindAll() {
        // GIVEN a datasource that fails on the 1st save call, but takes long to fail
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionService);
        TestDataSourcePluginFailable.configureFailAtFindAllCall(1);
        // WHEN launch the ingestion
        ingesterService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        assertEquals(0, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.ERROR, datasourceIngestion.getStatus());
        // THEN cursor position is the same as the first call because nothing has been saved
        // THEN cursor position is the same as the first call because nothing has been saved
        assertEquals(0, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void complexErrorCrawlingTestFailAtFirstFindAllWithSaveLong() {
        // GIVEN a datasource that fails on the 1st save call, but takes long to fail
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionService);
        TestDataSourcePluginFailable.configureFailAtFindAllCall(1);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(1);
        // WHEN launch the ingestion
        ingesterService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed, even if other bulk are done)
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        assertEquals(0, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.ERROR, datasourceIngestion.getStatus());
        // THEN cursor position is the same as the first call because nothing has been saved
        assertEquals(0, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void complexErrorCrawlingTestFailAtFirstSaveWithManyThreads() {
        // GIVEN a datasource that fails on the 1st save call, but takes long to fail
        TestDataSourcePluginFailable.configureIngestion(5, 500, datasourceIngestionService);
        TestDataSourcePluginFailable.configureFailAtSaveCalls(1);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(1);
        // WHEN launch the ingestion
        ingesterService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        assertEquals(0, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.ERROR, datasourceIngestion.getStatus());
        // THEN cursor position is the same as the first call because nothing has been saved
        assertEquals(0, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void complexErrorCrawlingTestFailSaveWithManyThreads() {
        // GIVEN a datasource that fails on the 11th save call, but takes long to fail, with many threads
        TestDataSourcePluginFailable.configureIngestion(5, 500, datasourceIngestionService);
        // GIVEN 10 first saves are ok, 11th fails
        TestDataSourcePluginFailable.configureFailAtSaveCalls(11);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(11);
        // WHEN launch the ingestion
        ingesterService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        assertEquals(50, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        // THEN cursor position must be set to error cursor position, to restart ingestion from it the next ingestion
        assertEquals(10, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void complexErrorCrawlingTestFailSaveWithManyThreadsAndManyErrors() {
        // GIVEN a datasource that fails on many bulk, and some are long. The first error must be kept
        TestDataSourcePluginFailable.configureIngestion(5, 500, datasourceIngestionService);
        // GIVEN 10 first saves are ok
        TestDataSourcePluginFailable.configureFailAtSaveCalls(31, 40, 41, 42);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(31, 40);
        // WHEN launch the ingestion
        ingesterService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        assertEquals(150, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        // THEN cursor position must be set to error cursor position, to restart ingestion from it the next ingestion
        assertEquals(30, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void simpleErrorCrawlingTestFailSaveWithoutLongTask() {
        // GIVEN fail
        TestDataSourcePluginFailable.configureIngestion(5, 500, datasourceIngestionService);
        TestDataSourcePluginFailable.configureFailAtSaveCalls(2, 3);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(99); // no long task for this test
        // WHEN launch the ingestion
        ingesterService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        assertEquals(5, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        // THEN cursor position must be set to error cursor position, to restart ingestion from it the next ingestion
        assertEquals(1, datasourceIngestion.getCursor().getPosition());
        // THEN the findAll method should not have been called more than 100 times, because ingestion stop at first error
        // Cannot set exact number because this depends on the thread speed, should be 3,4, more is unlikely
        assertTrue(TestDataSourcePluginFailable.getFindAllCpt() < 100);
    }

    @Test
    void complexErrorCrawlingTestFailSaveWithManyThreadsAndManyErrorsHorrible() {
        // GIVEN a datasource that fails on many bulk, and some are long. The first error must be kept
        // Bulk 21 and 40 will fail and are long task, so bulk 42 will fail firstly, but cursor must be reset to 21 (first error in the list)
        TestDataSourcePluginFailable.configureIngestion(5, 500, datasourceIngestionService);
        // GIVEN 20 first saves are ok
        TestDataSourcePluginFailable.configureFailAtSaveCalls(21, 40, 42);
        TestDataSourcePluginFailable.configureFailAtFindAllCall(42);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(1, 2, 3, 21, 40);
        // WHEN launch the ingestion
        ingesterService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        assertEquals(100, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        // THEN cursor position must be set to error cursor position, to restart ingestion from it the next ingestion
        assertEquals(20, datasourceIngestion.getCursor().getPosition());
        // THEN Make sure that only 42 findAll has been launched (42nd has failed, and stop ingestion immediately).
        assertEquals(42, TestDataSourcePluginFailable.getFindAllCpt());
    }
}
