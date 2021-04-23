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
package fr.cnes.regards.framework.modules.session.management.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.session.management.domain.Source;
import fr.cnes.regards.framework.modules.session.management.service.controllers.SourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for {@link Source}
 *
 * @author Iliana Ghazali
 **/
@RestController
@RequestMapping(SourceController.ROOT_MAPPING)
public class SourceController implements IResourceController<Source> {

    /**
     * Hypermedia resource service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Source Repository
     */
    @Autowired
    private SourceService sourceService;

    /**
     * Rest root path
     */
    public static final String ROOT_MAPPING = "/sources";

    /**
     * Delete source path
     */
    public static final String DELETE_SOURCE_MAPPING = "/{name}";

    @GetMapping
    @ResponseBody
    public ResponseEntity<PagedModel<EntityModel<Source>>> getSources(@RequestParam(required = false) String name,
            @RequestParam(required = false) String state,
            @PageableDefault(sort = "lastUpdateDate", direction = Sort.Direction.DESC, size = 20) Pageable pageable,
            PagedResourcesAssembler<Source> assembler) {
        Page<Source> sources = this.sourceService.loadSources(name, state, pageable);
        return ResponseEntity.ok(toPagedResources(sources, assembler));
    }

    @RequestMapping(value = DELETE_SOURCE_MAPPING, method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<Void> deleteSource(@PathVariable("name") final String name) {
        try {
            this.sourceService.orderDeleteSource(name);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public EntityModel<Source> toResource(Source source, Object... extras) {
        EntityModel<Source> resource = resourceService.toResource(source);
        resourceService.addLink(resource, this.getClass(), "getSources", LinkRels.LIST,
                                MethodParamFactory.build(String.class, source.getName()),
                                MethodParamFactory.build(String.class), MethodParamFactory.build(Pageable.class));
        resourceService.addLink(resource, this.getClass(), "deleteSource", LinkRels.DELETE,
                                MethodParamFactory.build(String.class, source.getName()));
        return resourceService.toResource(source);
    }
}