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
package fr.cnes.regards.modules.crawler.service;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.crawler.domain.DatasourceIngestion;
import fr.cnes.regards.modules.crawler.service.exception.FirstFindException;
import fr.cnes.regards.modules.crawler.service.exception.NotFinishedException;
import fr.cnes.regards.modules.dam.domain.entities.DataObject;
import fr.cnes.regards.modules.indexer.domain.SimpleSearchKey;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class IndexerServiceDataSourceDeleteIT extends AbstractIndexerServiceDataSourceIT {

    @Test
    public void testDeleteByDatasource() throws ModuleException, NotFinishedException, FirstFindException {
        String tenant = runtimeTenantResolver.getTenant();

        // Creation
        DatasourceIngestion dsi = new DatasourceIngestion(dataSourcePluginConf.getBusinessId());
        dsi.setLabel("Label");
        dsIngestionRepos.save(dsi);

        // Ingest datas
        crawlerService.ingest(dsi.getId()).orElseThrow();

        // Check ingested datas
        Long datasourceId = dataSourcePluginConf.getId();
        SimpleSearchKey<DataObject> key = new SimpleSearchKey<>(EntityType.DATA.toString(), DataObject.class);
        key.setSearchIndex(tenant);
        Assert.assertEquals(4, esRepos.count(key, ICriterion.all()).intValue());

        // Delete all from this datasource
        long nbDeleted = esRepos.deleteByDatasource(tenant, datasourceId);
        Assert.assertEquals(4, nbDeleted);
        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(tenant);
            return esRepos.count(key, ICriterion.all()) == 0;
        });
        Assert.assertEquals(0, esRepos.count(key, ICriterion.all()).intValue());
    }

}