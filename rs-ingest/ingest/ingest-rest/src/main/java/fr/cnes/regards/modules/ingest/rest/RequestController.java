/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.dto.request.RequestDto;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;

/**
 * This controller manages Requests.
 *
 * @author Léo Mieulet
 */
@RestController
@RequestMapping(RequestController.TYPE_MAPPING)
public class RequestController implements IResourceController<RequestDto> {

    public static final String TYPE_MAPPING = "/requests";

    @Autowired
    private IRequestService requestService;

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Retrieve a page of ingest requests according to the given filters
     * @param filters request filters
     * @param pageable
     * @param assembler
     * @return page of aip metadata respecting the constraints
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Return a page of Requests")
    public ResponseEntity<PagedResources<Resource<RequestDto>>> searchRequest(
            @RequestBody SearchRequestsParameters filters,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<RequestDto> assembler) throws ModuleException {
        Page<RequestDto> requests = requestService.findRequests(filters, pageable);
        return new ResponseEntity<>(toPagedResources(requests, assembler), HttpStatus.OK);
    }

    @Override
    public Resource<RequestDto> toResource(RequestDto element, Object... extras) {
        Resource<RequestDto> resource = resourceService.toResource(element);
        return resource;
    }
}
