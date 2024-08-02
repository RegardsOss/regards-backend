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


package fr.cnes.regards.modules.crawler.service.job;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.service.IDatasourceIngesterService;
import fr.cnes.regards.modules.crawler.service.IEntityIndexerService;
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
    private IAuthenticationResolver authResolver;

    @Override
    public void scheduleCatalogReset() {
        // Schedule request retry job
        JobInfo jobInfo = new JobInfo(false, 1, null, authResolver.getUser(), CatalogResetJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        LOGGER.debug("Schedule {} job with id {}", CatalogResetJob.class.getName(), jobInfo.getId());
    }

    @Override
    public void resetCatalog() throws ModuleException {
        String tenant = runtimeTenantResolver.getTenant();

        // Delete given data object from Elasticsearch
        entityIndexerService.deleteIndexNRecreateEntities(tenant);

        // Clear all datasources ingestion
        List<DatasourceIngestion> datasources = datasourceIngesterService.getDatasourceIngestions();
        if ((datasources != null) && !datasources.isEmpty()) {
            datasources.forEach(ds -> datasourceIngesterService.deleteDatasourceIngestion(ds.getId()));
        }
    }

}
