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

import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.plugins.TestDataSourcePluginFailable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;

import static fr.cnes.regards.modules.crawler.domain.IngestionStatus.ERROR;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author mnguyen0
 */
@ActiveProfiles({ "reindex-it", "noscheduler", "testAmqp" })
@TestPropertySource(locations = { "classpath:test-reindex-it.properties" }, properties = { "regards.tenant=reindex_it",
                                                                                           // need to enable amqp to stop job event handler
                                                                                           "regards.amqp.enabled=true",
                                                                                           "regards.elasticsearch.threadpool.size=10",
                                                                                           // due to multiple autoconfiguration, multiple mock or no-mock are created (OpenSearchService for example).
                                                                                           // This option allow beans to override previous bean.
                                                                                           "spring.main.allow-bean-definition-overriding=true",
                                                                                           "spring.jpa.properties.hibernate.default_schema=reindex_it" })
class ReindexIT extends AbstractCrawlerIT {

    @Override
    protected String tenant() {
        return "reindex_it";
    }

    @Override
    protected String alias() {
        return "reindex_it_alias";
    }

    @Override
    protected String buildingIndex() {
        return "reindex_it_1";
    }

    @Override
    protected String modelName() {
        return "model";
    }

    @Override
    protected String pluginBusinessId() {
        return "test-datasource-failable-reindex-it";
    }

    @Test
    void nominal_building_datasource_crawling_test() throws InterruptedException {
        // Given
        initIndexBuilding();
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionRunnerService);
        // When - First autoCrawler scheduler pass - First ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // We simulate that the manage building datasource scheduler passes - it should not have effect
        dsiBuildingIndexRunnerService.manageBuildingDatasourceIngestions(tenant());

        // Then
        List<DatasourceIngestion> datasourceIngestions = waitForCrawlingTerminationAllDSI(1000);
        Assertions.assertEquals(2, datasourceIngestions.size());

        DatasourceIngestion dsiBuilding = datasourceIngestions.stream()
                                                              .filter(DatasourceIngestion::isBuilding)
                                                              .toList()
                                                              .get(0);

        assertNotNull(dsiBuilding.getNextPlannedIngestDate());
        assertTrue(dsiBuilding.getNextPlannedIngestDate().isBefore(OffsetDateTime.now()));

        // When - Second autoCrawler scheduler pass - Second ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // We simulate that the manage building datasource scheduler passes - it should not have effect
        dsiBuildingIndexRunnerService.manageBuildingDatasourceIngestions(tenant());

        // Then
        datasourceIngestions = waitForCrawlingTerminationAllDSI(1000);
        Assertions.assertEquals(2, datasourceIngestions.size());
        dsiBuilding = datasourceIngestions.stream().filter(DatasourceIngestion::isBuilding).toList().get(0);
        assertNull(dsiBuilding.getNextPlannedIngestDate());

        // When - Third autoCrawler scheduler pass - Update NextPlannedDate
        crawlerCreatorService.manageCrawlingForAllTenants();
        datasourceIngestions = waitForCrawlingTerminationAllDSI(1000);
        Assertions.assertEquals(2, datasourceIngestions.size());
        dsiBuilding = datasourceIngestions.stream().filter(DatasourceIngestion::isBuilding).toList().get(0);
        assertTrue(dsiBuilding.getNextPlannedIngestDate().isAfter(OffsetDateTime.now()));

        //When - building dsi scheduler simulation
        dsiBuildingIndexRunnerService.manageBuildingDatasourceIngestions(tenant());
        // Then
        datasourceIngestions = waitForCrawlingTerminationAllDSI(1000);
        Assertions.assertEquals(1, datasourceIngestions.size());
        List<DatasourceIngestion> dsis = datasourceIngestions.stream().filter(DatasourceIngestion::isBuilding).toList();
        assertTrue(dsis.isEmpty());

        assertEquals(buildingIndex(), indexAliasResolver.resolveCurrentIndex(tenant()));
        assertTrue(indexAliasResolver.resolveBuildingIndex(tenant()).isEmpty());
    }

    @Test
    void error_building_datasource_crawling_test() throws InterruptedException {
        // Given
        initIndexBuilding();
        TestDataSourcePluginFailable.configureIngestion(5, 20, datasourceIngestionRunnerService);
        // When - First autoCrawler scheduler pass - First ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // We simulate that the manage building datasource scheduler passes - it should not have effect
        dsiBuildingIndexRunnerService.manageBuildingDatasourceIngestions(tenant());

        // Then
        List<DatasourceIngestion> datasourceIngestions = waitForCrawlingTerminationAllDSI(1000);
        Assertions.assertEquals(2, datasourceIngestions.size());

        DatasourceIngestion dsiBuilding = datasourceIngestions.stream()
                                                              .filter(DatasourceIngestion::isBuilding)
                                                              .toList()
                                                              .get(0);

        assertNotNull(dsiBuilding.getNextPlannedIngestDate());
        assertTrue(dsiBuilding.getNextPlannedIngestDate().isBefore(OffsetDateTime.now()));

        // When - Second autoCrawler scheduler pass - Second ingestion
        crawlerCreatorService.manageCrawlingForAllTenants();
        // We simulate that the manage building datasource scheduler passes - it should not have effect
        dsiBuildingIndexRunnerService.manageBuildingDatasourceIngestions(tenant());

        // Then
        datasourceIngestions = waitForCrawlingTerminationAllDSI(1000);
        Assertions.assertEquals(2, datasourceIngestions.size());
        dsiBuilding = datasourceIngestions.stream().filter(DatasourceIngestion::isBuilding).toList().get(0);
        assertNull(dsiBuilding.getNextPlannedIngestDate());

        // When - Third autoCrawler scheduler pass - Update NextPlannedDate
        crawlerCreatorService.manageCrawlingForAllTenants();
        datasourceIngestions = waitForCrawlingTerminationAllDSI(1000);
        Assertions.assertEquals(2, datasourceIngestions.size());
        dsiBuilding = datasourceIngestions.stream().filter(DatasourceIngestion::isBuilding).toList().get(0);
        assertTrue(dsiBuilding.getNextPlannedIngestDate().isAfter(OffsetDateTime.now()));

        //When - the datasource ingestion for building index is in error, then passage of building dsi scheduler
        dsiBuilding.setStatus(ERROR);
        datasourceIngestionRepository.save(dsiBuilding);
        dsiBuildingIndexRunnerService.manageBuildingDatasourceIngestions(tenant());
        // Then
        datasourceIngestions = waitForCrawlingTerminationAllDSI(1000);
        Assertions.assertEquals(2, datasourceIngestions.size());
        dsiBuilding = datasourceIngestions.stream().filter(DatasourceIngestion::isBuilding).toList().get(0);
        assertTrue(dsiBuilding.getNextPlannedIngestDate().isAfter(OffsetDateTime.now()));
        assertNull(dsiBuilding.getLastIngestDate());

        // When - Simulate rerunning the datasource ingestion in error
        dsiBuilding.setNextPlannedIngestDate(OffsetDateTime.now());
        datasourceIngestionRepository.save(dsiBuilding);
        crawlerCreatorService.manageCrawlingForAllTenants();

        // Then
        datasourceIngestions = waitForCrawlingTerminationAllDSI(1000);
        Assertions.assertEquals(2, datasourceIngestions.size());
        dsiBuilding = datasourceIngestions.stream().filter(DatasourceIngestion::isBuilding).toList().get(0);
        assertTrue(dsiBuilding.getNextPlannedIngestDate().isBefore(OffsetDateTime.now()));

        // When
        crawlerCreatorService.manageCrawlingForAllTenants();

        // Then
        datasourceIngestions = waitForCrawlingTerminationAllDSI(1000);
        Assertions.assertEquals(2, datasourceIngestions.size());
        dsiBuilding = datasourceIngestions.stream().filter(DatasourceIngestion::isBuilding).toList().get(0);
        assertNull(dsiBuilding.getNextPlannedIngestDate());

        // When
        crawlerCreatorService.manageCrawlingForAllTenants();

        // Then
        datasourceIngestions = waitForCrawlingTerminationAllDSI(1000);
        Assertions.assertEquals(2, datasourceIngestions.size());
        dsiBuilding = datasourceIngestions.stream().filter(DatasourceIngestion::isBuilding).toList().get(0);
        assertTrue(dsiBuilding.getNextPlannedIngestDate().isAfter(OffsetDateTime.now()));

        // When
        dsiBuildingIndexRunnerService.manageBuildingDatasourceIngestions(tenant());
        datasourceIngestions = waitForCrawlingTerminationAllDSI(1000);
        Assertions.assertEquals(1, datasourceIngestions.size());
        List<DatasourceIngestion> dsis = datasourceIngestions.stream().filter(DatasourceIngestion::isBuilding).toList();
        assertTrue(dsis.isEmpty());

        assertEquals(buildingIndex(), indexAliasResolver.resolveCurrentIndex(tenant()));
        assertTrue(indexAliasResolver.resolveBuildingIndex(tenant()).isEmpty());
    }

}