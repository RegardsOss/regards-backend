/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.rest;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileCopyRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileRequestStatus;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.domain.dto.request.FileRequestInfoDTO;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import fr.cnes.regards.modules.storage.service.file.request.FileDeletionRequestService;
import fr.cnes.regards.modules.storage.service.file.request.RequestStatusService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationService;

/**
 * REST Controller to handle requests on {@link FileRequestInfoDTO}s.<br/>
 * Those requests are DTO from {@link FileStorageRequest} {@link FileCopyRequest}, {@link FileCacheRequest} or {@link FileDeletionRequestService}.
 *
 * @author Sébastien Binda
 *
 */
@RestController
@RequestMapping(FileRequestsController.REQUESTS_PATH)
public class FileRequestsController implements IResourceController<FileRequestInfoDTO> {

    public static final String REQUESTS_PATH = "/requests";

    public static final String STORAGE_PATH = "/{storage}";

    public static final String TYPE_PATH = "/{type}";

    public static final String STOP_PATH = "/stop";

    public static final String STATUS_PARAM = "status";

    @Autowired
    private StorageLocationService service;

    @Autowired
    private RequestStatusService reqService;

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    @RequestMapping(method = RequestMethod.GET, path = STORAGE_PATH + TYPE_PATH)
    @ResourceAccess(description = "Retrieve list of all known storage locations", role = DefaultRole.ADMIN)
    public ResponseEntity<PagedModel<EntityModel<FileRequestInfoDTO>>> search(
            @PathVariable(name = "storage") String storageLocationId, @PathVariable(name = "type") FileRequestType type,
            @RequestParam(name = STATUS_PARAM, required = false) FileRequestStatus status, Pageable page,
            PagedResourcesAssembler<FileRequestInfoDTO> assembler) {
        return new ResponseEntity<>(
                toPagedResources(service.getRequestInfos(storageLocationId, type, Optional.ofNullable(status), page),
                                 assembler),
                HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, path = STORAGE_PATH + TYPE_PATH)
    @ResourceAccess(description = "Delete storage location", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable(name = "storage") String storageLocationId,
            @PathVariable(name = "type") FileRequestType type,
            @RequestParam(name = STATUS_PARAM, required = false) FileRequestStatus status, Pageable page)
            throws ModuleException {
        service.deleteRequests(storageLocationId, type, Optional.ofNullable(status));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = STOP_PATH)
    @ResourceAccess(description = "Delete storage location", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> stop() {
        reqService.stopCacheRequests();
        reqService.stopDeletionRequests();
        reqService.stopCopyRequests();
        reqService.stopStorageRequests();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<FileRequestInfoDTO> toResource(FileRequestInfoDTO element, Object... extras) {
        EntityModel<FileRequestInfoDTO> resource = resourceService.toResource(element);
        return resource;
    }

}
