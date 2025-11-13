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

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.service.job.CatalogResetJob;
import fr.cnes.regards.modules.indexer.service.IndexAliasResolver;
import fr.cnes.regards.modules.indexer.service.IndexAliasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Reset the catalog from the crawler service
 *
 * @author Iliana Ghazali
 */

@Service
@MultitenantTransactional
public class CatalogResetService implements ICatalogResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogResetService.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IEntityIndexerService entityIndexerService;

    @Autowired
    private IDatasourceIngesterService datasourceIngesterService;

    @Autowired
    private DatasourceIngestionService dsIngestionService;

    @Autowired
    private IndexService indexService;

    @Autowired
    private IndexAliasService indexAliasService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Override
    public void scheduleCatalogReset() {
        // Schedule request reset job
        JobInfo jobInfo = new JobInfo(false, 1, null, authResolver.getUser(), CatalogResetJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("Schedule {} job with id {}", CatalogResetJob.class.getName(), jobInfo.getId());
    }

    @SuppressWarnings("java:S2221") // Intentionally catching Exception to handle any unexpected failure
    @Override
    public void resetCatalog() throws ModuleException {
        String tenant = runtimeTenantResolver.getTenant();
        // Clear all building datasources ingestion
        clearBuildingDatasourceIngestions();
        try {
            entityIndexerService.createBuildingIndexAndCreateEntities(tenant);
        } catch (Exception e) {
            //If there was an exception during this step, then we need to revert the building index creation
            indexService.deleteBuildingIndexIfAlreadyExists(tenant);
            LOGGER.error("[REINDEX] Reset catalog failed for tenant {} : {}", tenant, e.getMessage(), e);
            String aliasName = IndexAliasResolver.resolveAliasName(tenant);
            indexAliasService.setBuilding(aliasName, null);
            clearBuildingDatasourceIngestions();
        }
        dsIngestionService.updateAndCleanTenantDatasourceIngestions();
    }

    /**
     * Clear all the datasource ingestions for building index
     */
    private void clearBuildingDatasourceIngestions() {
        List<DatasourceIngestion> datasources = datasourceIngesterService.getDatasourceIngestions();
        datasources.forEach(ds -> {
            LOGGER.info("Datasource Ingestion id : {}", ds.getId());
            if (ds.isBuilding()) {
                LOGGER.info("Datasource Ingestion id {} is for building index, let's delete it", ds.getId());
                datasourceIngesterService.deleteDatasourceIngestion(ds.getId());
            }
        });
    }

}
