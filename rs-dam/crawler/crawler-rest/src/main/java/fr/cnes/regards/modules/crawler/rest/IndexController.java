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
import fr.cnes.regards.modules.crawler.service.service.CatalogResetService;
import fr.cnes.regards.modules.crawler.service.service.IEntityIndexerService;
import fr.cnes.regards.modules.indexer.service.IndexAliasResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Controller for managing Elasticsearch indexes.
 * Provides endpoints to recreate, update, and check the
 * state of catalog indexes for the current tenant.
 */
@Tag(name = "Elasticsearch index controller")
@RestController
@RequestMapping(IndexController.TYPE_MAPPING)
public class IndexController {

    public static final String TYPE_MAPPING = "/index";

    public static final String INDEX_BUILDING = "/building";

    public static final String UPDATE_DATASETS = "/update/datasets";

    public static final String UPDATE_COLLECTIONS = "/update/collections";

    @Autowired
    protected IEntityIndexerService entityIndexerService;

    @Autowired
    private CatalogResetService catalogResetService;

    @Autowired
    private IndexAliasResolver indexAliasResolver;

    /**
     * Current tenant resolver
     */
    @Autowired
    protected IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Reindex the whole catalog for the current tenant by creating a new index.
     * The current index is still alive while the new index is still building, so it can still be requested.
     * The current index is deleted only when the new index is completed.
     *
     * @return void
     */
    @Operation(summary = "Reindex the whole catalog",
               description = "Deletes and recreates the full catalog index for the current tenant.")
    @ResourceAccess(description = "Endpoint to reindex the whole catalog.", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Void> recreateIndex() {
        catalogResetService.scheduleCatalogReset();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Update all indexed datasets
     *
     * @return void
     */
    @Operation(summary = "Update all datasets",
               description = "Triggers reindexing of all datasets for the current tenant.")
    @ResourceAccess(description = "Endpoint to update all datasets indexed.", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(path = TYPE_MAPPING + UPDATE_DATASETS, method = RequestMethod.POST)
    public ResponseEntity<Void> updateDatasets() throws ModuleException {
        String tenant = runtimeTenantResolver.getTenant();
        entityIndexerService.updateAllDatasets(tenant, OffsetDateTime.now(), false);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Update all indexed collections
     *
     * @return void
     */
    @Operation(summary = "Update all collections",
               description = "Triggers reindexing of all collections for the current tenant.")
    @ResourceAccess(description = "Endpoint to update all collections indexed.", role = DefaultRole.PROJECT_ADMIN)
    @RequestMapping(path = TYPE_MAPPING + UPDATE_COLLECTIONS, method = RequestMethod.POST)
    public ResponseEntity<Void> updateCollections() throws ModuleException {
        String tenant = runtimeTenantResolver.getTenant();
        entityIndexerService.updateAllCollections(tenant, OffsetDateTime.now());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Return true if there is a building index for the tenant
     */
    @Operation(summary = "Check if a building index exists",
               description = "Returns true if a building index exists for the current tenant, false otherwise.")
    @GetMapping(INDEX_BUILDING)
    @ResourceAccess(description = "Endpoint to indicate whether a building index exists", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Boolean> hasBuilding() {
        String tenant = runtimeTenantResolver.getTenant();
        return ResponseEntity.ok(indexAliasResolver.resolveBuildingIndex(tenant).isPresent());
    }
}
