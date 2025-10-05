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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.domain.IngestionStatus;
import fr.cnes.regards.modules.crawler.plugins.TestDataSourcePluginFailable;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author tguillou
 */
@ActiveProfiles({ "crawler-it", "noscheduler", "testAmqp" })
@TestPropertySource(locations = { "classpath:test-crawler-it.properties" }, properties = { "regards.tenant=crawler_it",
                                                                                           // need to enable amqp to stop job event handler
                                                                                           "regards.amqp.enabled=true",
                                                                                           "regards.elasticsearch.threadpool.size=10",
                                                                                           // due to multiple autoconfiguration, multiple mock or no-mock are created (OpenSearchService for example).
                                                                                           // This option allow beans to override previous bean.
                                                                                           "spring.main.allow-bean-definition-overriding=true",
                                                                                           "spring.jpa.properties.hibernate.default_schema=crawler_it" })
class CrawlerIT extends AbstractCrawlerIT {

    @Override
    protected String tenant() {
        return "crawler_it";
    }

    @Override
    protected String alias() {
        return "crawler_it_alias";
    }

    @Override
    protected String buildingIndex() {
        return "crawler_it_1";
    }

    @Override
    protected String modelName() {
        return "model";
    }

    @Override
    protected String pluginBusinessId() {
        return "test-datasource-failable-crawler-it";
    }

    @Test
    void nominalCrawlingTest() {
        // GIVEN a datasource that has 20 objects to save
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionRunnerService);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should be FINISHED without any problem
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(1000);
        assertEquals(20, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.FINISHED, datasourceIngestion.getStatus());
        SimpleSearchKey<DataObject> searchKey = new SimpleSearchKey<>(EntityType.DATA.toString(), DataObject.class);
        searchKey.setSearchIndex(alias());
        List<DataObject> content = esRepository.search(searchKey, 25, ICriterion.all()).getContent();
        // THEN 20 data object has been crawled
        Assertions.assertEquals(20, content.size());
    }

    @Test
    void nominalComplexCrawlingTest() {
        // GIVEN a datasource that has 20 objects to save
        TestDataSourcePluginFailable.configureIngestion(5, 250, datasourceIngestionRunnerService);
        // Even if task is long, it should be taken into account
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(15, 30, 45, 60);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should be FINISHED without any problem
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(20);
        assertEquals(250, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.FINISHED, datasourceIngestion.getStatus());
    }

    @Test
    void simpleErrorCrawlingTest() {
        // GIVEN a datasource that fails on the 2nd findAll call
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionRunnerService);
        TestDataSourcePluginFailable.configureFailAtFindAllCall(2);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as NOT_FINISHED, with 5 objects saved (first bulk of 5 objects is ok)
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(10);
        assertEquals(5, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        assertEquals(1, datasourceIngestion.getCursor().getPosition());
        // job should be marked as SUCCEEDED because the error is managed by the ingestion process.
        // Only not managed error lead to job error
        JobInfo job = jobInfoRepository.findCompleteById(datasourceIngestion.getJobId());
        assertEquals(JobStatus.SUCCEEDED, job.getStatus().getStatus());
    }

    @Test
    void simpleErrorCrawlingTest2() {
        // GIVEN a datasource that fails on the 3rd findAll call
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionRunnerService);
        TestDataSourcePluginFailable.configureFailAtFindAllCall(3);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as NOT_FINISHED, with 10 objects saved (first 2 bulks of 5 objects are ok)
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(10);
        assertEquals(10, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        assertEquals(2, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void complexErrorCrawlingTestWithLongTask() {
        // GIVEN a datasource that has 20 objects to save, but fails on the 4th findAll call, with a long task
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionRunnerService);
        TestDataSourcePluginFailable.configureFailAtFindAllCall(4);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(3); // Even if task is long, it should be taken into account
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should save 15 objects, first 3 bulks of 5 objects are ok even if the 3rd one is long
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(15);
        assertEquals(15, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        assertEquals(3, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void complexErrorCrawlingTestFailAtFirstSave() {
        // GIVEN a datasource that fails on the 1st save call, but takes long to fail
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionRunnerService);
        TestDataSourcePluginFailable.configureFailAtSaveCalls(1);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(1);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(10);
        assertEquals(0, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.ERROR, datasourceIngestion.getStatus());
        // THEN cursor position is the same as the first call because nothing has been saved
        assertEquals(0, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void simpleErrorCrawlingTestFailAtFirstFindAll() {
        // GIVEN a datasource that fails on the 1st save call, but takes long to fail
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionRunnerService);
        TestDataSourcePluginFailable.configureFailAtFindAllCall(1);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        jobService.manage();
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(10);
        // THEN the ingestion should be marked as error (first save failed)
        assertEquals(0, datasourceIngestion.getSavedObjectsCount());

        // THEN cursor position is the same as the first call because nothing has been saved
        // THEN cursor position is the same as the first call because nothing has been saved
        assertEquals(0, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void complexErrorCrawlingTestFailAtFirstFindAllWithSaveLong() {
        // GIVEN a datasource that fails on the 1st save call, but takes long to fail
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionRunnerService);
        TestDataSourcePluginFailable.configureFailAtFindAllCall(1);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(1);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed, even if other bulk are done)
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(10);
        assertEquals(0, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.ERROR, datasourceIngestion.getStatus());
        // THEN cursor position is the same as the first call because nothing has been saved
        assertEquals(0, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void complexErrorCrawlingTestFailAtFirstSaveWithManyThreads() {
        // GIVEN a datasource that fails on the 1st save call, but takes long to fail
        TestDataSourcePluginFailable.configureIngestion(5, 500, datasourceIngestionRunnerService);
        TestDataSourcePluginFailable.configureFailAtSaveCalls(1);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(1);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(20);
        assertEquals(0, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.ERROR, datasourceIngestion.getStatus());
        // THEN cursor position is the same as the first call because nothing has been saved
        assertEquals(0, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void complexErrorCrawlingTestFailSaveWithManyThreads() throws InterruptedException {
        // GIVEN a datasource that fails on the 11th save call, but takes long to fail, with many threads
        TestDataSourcePluginFailable.configureIngestion(5, 500, datasourceIngestionRunnerService);
        // GIVEN 10 first saves are ok, 11th fails
        TestDataSourcePluginFailable.configureFailAtSaveCalls(11);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(11);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(20);
        assertEquals(50, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        // THEN cursor position must be set to error cursor position, to restart ingestion from it the next ingestion
        assertEquals(10, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void complexErrorCrawlingTestFailSaveWithManyThreadsAndManyErrors() {
        // GIVEN a datasource that fails on many bulk, and some are long. The first error must be kept
        TestDataSourcePluginFailable.configureIngestion(5, 500, datasourceIngestionRunnerService);
        // GIVEN 10 first saves are ok
        TestDataSourcePluginFailable.configureFailAtSaveCalls(31, 40, 41, 42);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(31, 40);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(300);
        assertEquals(150, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        // THEN cursor position must be set to error cursor position, to restart ingestion from it the next ingestion
        assertEquals(30, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void simpleErrorCrawlingTestFailSaveWithoutLongTask() {
        // GIVEN fail
        TestDataSourcePluginFailable.configureIngestion(5, 500, datasourceIngestionRunnerService);
        TestDataSourcePluginFailable.configureFailAtSaveCalls(2, 3);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(99); // no long task for this test
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(20);
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
        TestDataSourcePluginFailable.configureIngestion(5, 500, datasourceIngestionRunnerService);
        // GIVEN 20 first saves are ok
        TestDataSourcePluginFailable.configureFailAtSaveCalls(21, 40, 42);
        TestDataSourcePluginFailable.configureFailAtFindAllCall(42);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(1, 2, 3, 21, 40);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error (first save failed)
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(30);
        assertEquals(100, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        // THEN cursor position must be set to error cursor position, to restart ingestion from it the next ingestion
        assertEquals(20, datasourceIngestion.getCursor().getPosition());
        // THEN Make sure that only 42 findAll has been launched (42nd has failed, and stop ingestion immediately).
        assertEquals(42, TestDataSourcePluginFailable.getFindAllCpt());
    }

    @Test
    void nominalTestIntermediateResults() {
        // GIVEN a datasource
        TestDataSourcePluginFailable.configureIngestion(5, 100, datasourceIngestionRunnerService);
        // GIVEN 20 bulk : 10 first bulk are fast, 10 next bulk are slow
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(11,
                                                                  12,
                                                                  13,
                                                                  14,
                                                                  15,
                                                                  16,
                                                                  17,
                                                                  18,
                                                                  19,
                                                                  20); // all bulk after the 10th must be long
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should store results at each bulk
        Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(500, TimeUnit.MILLISECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(tenant());
            List<DatasourceIngestion> dsList = datasourceIngestionRepository.findAll();
            if (!dsList.isEmpty()) {
                return dsList.get(0).getSavedObjectsCount().equals(50);
            } else {
                return false;
            }
        });
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(10);
        assertEquals(100, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.FINISHED, datasourceIngestion.getStatus());
        // THEN cursor position must be set to last cursor position
        assertEquals(20, datasourceIngestion.getCursor().getPosition());
    }

    @Test
    void testIntermediateResultsWellStoredWhenError() throws ModuleException, InterruptedException {
        // GIVEN a datasource
        TestDataSourcePluginFailable.configureIngestion(5, 100, datasourceIngestionRunnerService);
        // GIVEN 20 bulk : 10 first bulk are fast, 10 next bulk are slow, but 16th failed
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(11,
                                                                  12,
                                                                  13,
                                                                  14,
                                                                  15,
                                                                  16,
                                                                  17,
                                                                  18,
                                                                  19,
                                                                  20); // all bulk after the 10th must be long
        TestDataSourcePluginFailable.configureFailAtFindAllCall(16);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // THEN the ingestion should be marked as error
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(10);
        assertEquals(75, datasourceIngestion.getSavedObjectsCount()); // only 75 features are crawled
        assertEquals(IngestionStatus.NOT_FINISHED, datasourceIngestion.getStatus());
        // THEN cursor position must be set to the error position
        assertEquals(15, datasourceIngestion.getCursor().getPosition());
        // WHEN relaunch the ingestion
        System.out.println("Relaunch the crawler");
        datasourceIngesterService.scheduleNowDatasourceIngestion(datasourceIngestion.getId());
        crawlerCreatorService.manageCrawlingForAllTenants();
        datasourceIngestion = waitForCrawlingTermination(10);
        // THEN the ingestion should be marked as FINISHED
        assertEquals(20, datasourceIngestion.getSavedObjectsCount()); // Cause of the behaviour of the test plugin
        // there is only 20 and not 25 dataObject : the bulk 16 failed, and plugin mock is configured to manage 20 bulk.
        // So it restart to bulk 17 (and not 16), then 18, 19 and 20 : 4 bulk so 20 dataojbect
        assertEquals(IngestionStatus.FINISHED, datasourceIngestion.getStatus());
        // THEN cursor position must be set to the last position
        assertEquals(19,
                     datasourceIngestion.getCursor()
                                        .getPosition()); // same reason, bulk 20 correspond to cursor 19 because of bulk 16 failed (cursor hasn't been incremented)
    }

    @Test
    void nominalRestart() throws ModuleException {
        // GIVEN a datasource that successfully finish
        TestDataSourcePluginFailable.configureIngestion(5, 100, datasourceIngestionRunnerService);
        TestDataSourcePluginFailable.configureActivateDifferentDate();
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        DatasourceIngestion datasourceIngestion = waitForCrawlingTermination(20);
        assertEquals(100, datasourceIngestion.getSavedObjectsCount());
        assertEquals(IngestionStatus.FINISHED, datasourceIngestion.getStatus());
        // THEN cursor position must be set to the last cursor position ( 0 because the date determinate the position)
        assertEquals(0, datasourceIngestion.getCursor().getPosition());
        // TestDatasourcePlugin is configured to increment by 1 hour per bulk managed, so 20 hours.
        assertEquals(TestDataSourcePluginFailable.REFERENCE_DATE.plusHours(20).atZoneSameInstant(ZoneOffset.UTC),
                     datasourceIngestion.getCursor().getLastEntityDate().atZoneSameInstant(ZoneOffset.UTC));
        // Wrong restart because date is after the last entity date
        try {
            datasourceIngesterService.scheduleNowDatasourceIngestionFromDate(datasourceIngestion.getId(),
                                                                             TestDataSourcePluginFailable.REFERENCE_DATE.plusHours(
                                                                                 30));
            Assertions.fail("Should fail here");
        } catch (ModuleException e) {
            Assertions.assertEquals("The date to crawl must be before the last entity date.", e.getMessage());
        }
        // Restart ingestion from the 10th bulk
        datasourceIngesterService.scheduleNowDatasourceIngestionFromDate(datasourceIngestion.getId(),
                                                                         TestDataSourcePluginFailable.REFERENCE_DATE.plusHours(
                                                                             10));
        TestDataSourcePluginFailable.resetBulkCpt(10);
        datasourceIngestion = datasourceIngestionRepository.findAll().get(0);
        Assertions.assertEquals(TestDataSourcePluginFailable.REFERENCE_DATE.plusHours(10),
                                datasourceIngestion.getCursor().getLastEntityDate());
        // WHEN
        crawlerCreatorService.manageCrawlingForAllTenants();
        datasourceIngestion = waitForCrawlingTermination(10);
        // THEN
        assertEquals(50, datasourceIngestion.getSavedObjectsCount()); // bulk 10 to 20 managed = 50 dataobjects
        assertEquals(IngestionStatus.FINISHED, datasourceIngestion.getStatus());
        // THEN cursor position must be set to the last cursor position ( 0 because the date determinate the position)
        assertEquals(0, datasourceIngestion.getCursor().getPosition());
        assertEquals(TestDataSourcePluginFailable.REFERENCE_DATE.plusHours(20).atZoneSameInstant(ZoneOffset.UTC),
                     datasourceIngestion.getCursor().getLastEntityDate().atZoneSameInstant(ZoneOffset.UTC));
    }

    @Test
    void testJobWellAborted() throws InterruptedException {
        // WARNING : an optimisticLockException can be raised, because the job can update the ingestion status after the ingestion deletion,
        // but it's not a problem
        // GIVEN a datasource
        TestDataSourcePluginFailable.configureIngestion(5, 10, datasourceIngestionRunnerService);
        // GIVEN find all slow to let us time to kill the job (2 find all to do)
        TestDataSourcePluginFailable.setFindAllTimingMs(5000);
        TestDataSourcePluginFailable.configureLongTaskAtSaveCalls(1, 2);
        // WHEN launch the ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        List<DatasourceIngestion> allDatasources = datasourceIngestionRepository.findAll();
        Assertions.assertEquals(1, allDatasources.size());
        DatasourceIngestion datasourceIngestion = allDatasources.get(0);
        // WHEN delete ingestion
        datasourceIngestionRunnerService.deleteDatasourceIngestion(pluginBusinessId());
        // THEN the job is ABORTED
        Awaitility.await().pollInterval(500, TimeUnit.MILLISECONDS).atMost(4, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(tenant());
            // jobStatus should be ABORTED, but it can be SUCCEEDED if the job has been able to finish before been killed
            // (this appends when datasource ingestion is remove from BD, but the job has just started,
            // and so job finish instantly after, before receiving ABORTION event)
            return jobInfoService.retrieveJob(datasourceIngestion.getJobId()).getStatus().getStatus().isFinished();
        });
        allDatasources = datasourceIngestionRepository.findAll();
        SimpleSearchKey<DataObject> searchKey = new SimpleSearchKey<>(EntityType.DATA.toString(), DataObject.class);
        searchKey.setSearchIndex(alias());
        List<DataObject> content = esRepository.search(searchKey, 10, ICriterion.all()).getContent();
        // THEN Nothing have been crawled because job has been killed
        Assertions.assertEquals(0, content.size());
        Assertions.assertEquals(0, allDatasources.size());
    }

}
