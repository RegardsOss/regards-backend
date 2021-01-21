/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.toponyms;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 *
 * @author Sébastien Binda
 *
 */
@RestController
@RequestMapping(ToponymsController.ROOT_MAPPING)
public class ToponymsController implements IResourceController<ToponymDTO> {

    public static final String ROOT_MAPPING = "/toponyms";

    public static final String TOPONYM_ID = "/{businessId}";

    @Autowired
    private ToponymsService service;

    @Autowired
    private IResourceService resourceService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM module for given application", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedModel<EntityModel<ToponymDTO>>> retrieve(
            @SortDefault(sort = "label", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<ToponymDTO> assembler) throws EntityNotFoundException {
        Page<ToponymDTO> toponyms = service.findAll(pageable);
        PagedModel<EntityModel<ToponymDTO>> resources = toPagedResources(toponyms, assembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @RequestMapping(value = TOPONYM_ID, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM module for given application", role = DefaultRole.PUBLIC)
    public HttpEntity<EntityModel<ToponymDTO>> retrieve(@PathVariable("businessId") String businessId)
            throws EntityNotFoundException {
        Optional<ToponymDTO> toponym = service.findOne(businessId);
        if (toponym.isPresent()) {
            return new ResponseEntity<>(toResource(toponym.get()), HttpStatus.OK);
        } else {
            throw new EntityNotFoundException(businessId, ToponymDTO.class);
        }
    }

    @Override
    public EntityModel<ToponymDTO> toResource(ToponymDTO element, Object... extras) {
        return resourceService.toResource(element);
    }

}
