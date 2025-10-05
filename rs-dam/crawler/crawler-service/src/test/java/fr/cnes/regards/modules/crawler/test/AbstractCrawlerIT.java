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
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.crawler.dao.IDatasourceIngestionRepository;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.plugins.TestDataSourcePluginFailable;
import fr.cnes.regards.modules.crawler.service.service.*;
import fr.cnes.regards.modules.dam.service.datasources.IDataSourceService;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;
import fr.cnes.regards.modules.indexer.service.IndexAliasResolver;
import fr.cnes.regards.modules.indexer.service.IndexAliasService;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.service.ModelService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class AbstractCrawlerIT extends AbstractRegardsServiceIT {

    protected abstract String tenant();

    protected abstract String alias();

    protected abstract String buildingIndex();

    protected abstract String modelName();

    protected abstract String pluginBusinessId();

    @Autowired
    protected IEsRepository esRepository;

    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    protected IDataSourceService datasourceService;

    @SpyBean
    protected DatasourceIngestionService datasourceIngestionRunnerService;

    @SpyBean
    protected DatasourceIngestionBuildingIndexService dsiBuildingIndexRunnerService;

    @Autowired
    protected CrawlerCreatorService crawlerCreatorService;

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected IDatasourceIngestionRepository datasourceIngestionRepository;

    @Autowired
    protected IJobService jobService;

    @Autowired
    protected IJobInfoService jobInfoService;

    @Autowired
    protected IDatasourceIngesterService datasourceIngesterService;

    @Autowired
    protected IJobInfoRepository jobInfoRepository;

    @Autowired
    protected IndexService indexService;

    @Autowired
    protected IndexAliasResolver indexAliasResolver;

    @Autowired
    protected IndexAliasService indexAliasService;

    @BeforeEach
    void setUp() throws ModuleException {
        datasourceIngestionRepository.deleteAll(); // make sure that we start with no ingestion records
        runtimeTenantResolver.forceTenant(tenant());
        initIndex();
        createModel();
        createDataSource();
        jobInfoService.cleanDeadJobs();
        jobInfoRepository.findAll().forEach(jobInfo -> jobInfoService.stopJob(jobInfo.getId()));
        jobInfoRepository.deleteAll(); // clean previous jobs
    }

    private void initIndex() {
        indexService.deleteIndex(buildingIndex());
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !esRepository.indexExists(buildingIndex()));
        indexService.deleteIndex(tenant());
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !esRepository.indexExists(tenant()));
        indexService.deleteIndex(alias());
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !esRepository.aliasExists(alias()));
        indexService.createIndexAndAliasIfNeeded(tenant(), false);
        indexAliasResolver.resolveBuildingIndex(tenant()).ifPresent(idx -> indexAliasService.clearBuilding(alias()));
    }

    protected void initIndexBuilding() {
        indexService.createIndexAndAliasIfNeeded(buildingIndex(), true);
        indexAliasService.setBuilding(alias(), buildingIndex());
    }

    private void createModel() throws ModuleException {
        try {
            modelService.createModel(Model.build(modelName(), "description of " + modelName(), EntityType.DATA));
        } catch (EntityAlreadyExistsException e) {
            // That's perfect, the model already exists
        }
    }

    private void createDataSource() throws ModuleException {
        PluginConfiguration pluginConfig = PluginConfiguration.build(TestDataSourcePluginFailable.class, null, null);
        pluginConfig.setBusinessId(pluginBusinessId());
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

    protected DatasourceIngestion waitForCrawlingTermination(int atMostSeconds) {
        List<DatasourceIngestion> list = Awaitility.await()
                                                   .atMost(atMostSeconds, TimeUnit.SECONDS)
                                                   .pollInterval(1, TimeUnit.SECONDS)
                                                   .until(() -> {
                                                       runtimeTenantResolver.forceTenant(tenant());
                                                       return datasourceIngestionRepository.findAll();
                                                   }, ds -> !ds.isEmpty() && ds.get(0).getStatus().isFinal());
        return list.get(0);
    }

    protected List<DatasourceIngestion> waitForCrawlingTerminationAllDSI(int atMostSeconds) {
        return Awaitility.await()
                         .atMost(atMostSeconds, TimeUnit.SECONDS)
                         .pollInterval(1, TimeUnit.SECONDS)
                         .until(() -> {
                             runtimeTenantResolver.forceTenant(tenant());
                             return datasourceIngestionRepository.findAll();
                         }, ds -> !ds.isEmpty() && ds.stream().allMatch(d -> d.getStatus().isFinal()));
    }

}
