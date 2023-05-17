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
package fr.cnes.regards.framework.modules.session.manager.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.session.manager.domain.Source;
import fr.cnes.regards.framework.modules.session.manager.service.controllers.SourceManagerService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Controller for {@link Source}
 *
 * @author Iliana Ghazali
 **/
@RestController
@RequestMapping(SourceManagerController.ROOT_MAPPING)
public class SourceManagerController implements IResourceController<Source> {

    /**
     * Rest root path
     */
    public static final String ROOT_MAPPING = "/sources";

    /**
     * Endpoint to retrieve the list of sources names
     */
    public static final String NAME_MAPPING = "/names";

    /**
     * Delete source path
     */
    public static final String DELETE_SOURCE_MAPPING = "/{name}";

    /**
     * Hypermedia resource service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Source Repository
     */
    @Autowired
    private SourceManagerService sourceManagerService;

    @GetMapping
    @ResponseBody
    @ResourceAccess(description = "Endpoint to get sources", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<Source>>> getSources(
        @RequestParam(value = "sourceName", required = false) String sourceName,
        @RequestParam(value = "sourceState", required = false) String sourceState,
        @PageableDefault(sort = "lastUpdateDate", direction = Sort.Direction.DESC, size = 20) Pageable pageable,
        @Parameter(hidden = true) final PagedResourcesAssembler<Source> assembler) {
        Page<Source> sources = this.sourceManagerService.loadSources(sourceName, sourceState, pageable);
        return ResponseEntity.ok(toPagedResources(sources, assembler));
    }

    @GetMapping(value = NAME_MAPPING)
    @ResourceAccess(description = "Retrieve a subset of sources names", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Set<String>> getSourcesNames(@RequestParam(value = "name", required = false) String name) {
        return ResponseEntity.ok(this.sourceManagerService.retrieveSourcesNames(name));
    }

    @DeleteMapping(value = DELETE_SOURCE_MAPPING)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to delete a source", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> deleteSource(@PathVariable("name") final String name) throws EntityNotFoundException {
        this.sourceManagerService.orderDeleteSource(name);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<Source> toResource(Source source, Object... extras) {
        EntityModel<Source> resource = resourceService.toResource(source);
        resourceService.addLink(resource,
                                this.getClass(),
                                "getSources",
                                LinkRels.LIST,
                                MethodParamFactory.build(String.class, source.getName()),
                                MethodParamFactory.build(String.class),
                                MethodParamFactory.build(Pageable.class),
                                MethodParamFactory.build(PagedResourcesAssembler.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "deleteSource",
                                LinkRels.DELETE,
                                MethodParamFactory.build(String.class, source.getName()));
        return resource;
    }
}