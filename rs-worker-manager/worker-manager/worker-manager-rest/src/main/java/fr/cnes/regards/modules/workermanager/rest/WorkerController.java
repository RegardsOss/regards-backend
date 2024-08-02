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
package fr.cnes.regards.modules.workermanager.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.workermanager.dto.WorkerTypeAlive;
import fr.cnes.regards.modules.workermanager.service.cache.WorkerCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Théo Lasserre
 */
@Tag(name = "Worker manager")
@RestController
public class WorkerController implements IResourceController<WorkerTypeAlive> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerController.class);

    public static final String TYPE_MAPPING = "/workers";

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private WorkerCacheService workerCacheService;

    @RequestMapping(path = TYPE_MAPPING, method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve worker types with its number of alive instances",
                    role = DefaultRole.EXPLOIT)
    @Operation(summary = "Retrieve worker types",
               description = "Retrieve worker types with its number of alive instances.")
    public ResponseEntity<List<WorkerTypeAlive>> retrieveWorkerList(
        @Parameter(description = "Filter Workers on Request Content Types they handle")
        @RequestParam(value = "contentTypes", required = false) List<String> contentTypes) {
        List<WorkerTypeAlive> workers = workerCacheService.getWorkersInstance(contentTypes);
        return new ResponseEntity<>(workers, HttpStatus.OK);
    }

    @Override
    public EntityModel<WorkerTypeAlive> toResource(WorkerTypeAlive element, Object... extras) {
        return resourceService.toResource(element);
    }
}
