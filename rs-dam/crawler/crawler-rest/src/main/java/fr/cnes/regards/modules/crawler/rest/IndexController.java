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
package fr.cnes.regards.modules.crawler.rest;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.crawler.service.IEntityIndexerService;
import fr.cnes.regards.modules.crawler.service.job.CatalogResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping(IndexController.TYPE_MAPPING)
public class IndexController {

    public static final String TYPE_MAPPING = "/index";

    public static final String UPDATE_DATASETS = "/update/datasets";

    public static final String UPDATE_COLLECTIONS = "/update/collections";

    @Autowired
    protected IEntityIndexerService entityIndexerService;

    @Autowired
    private CatalogResetService catalogResetService;

    /**
     * Current tenant resolver
     */
    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Delete a DatasourceIngestion.
     *
     * @return void
     */
    @ResourceAccess(description = "Delete and recreate curent index.", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Void> recreateIndex() {
        catalogResetService.scheduleCatalogReset();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Delete a DatasourceIngestion.
     *
     * @return void
     */
    @ResourceAccess(description = "Update all datasets indexed.", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(path = TYPE_MAPPING + UPDATE_DATASETS, method = RequestMethod.POST)
    public ResponseEntity<Void> updateDatasets() throws ModuleException {
        String tenant = runtimeTenantResolver.getTenant();
        entityIndexerService.updateAllDatasets(tenant, OffsetDateTime.now());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Delete a DatasourceIngestion.
     *
     * @return void
     */
    @ResourceAccess(description = "Update all collections indexed.", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(path = TYPE_MAPPING + UPDATE_COLLECTIONS, method = RequestMethod.POST)
    public ResponseEntity<Void> updateCollections() throws ModuleException {
        String tenant = runtimeTenantResolver.getTenant();
        entityIndexerService.updateAllCollections(tenant, OffsetDateTime.now());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
