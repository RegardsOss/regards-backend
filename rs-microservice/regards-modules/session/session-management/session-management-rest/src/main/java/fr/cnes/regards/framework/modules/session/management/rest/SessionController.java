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
import fr.cnes.regards.framework.modules.session.management.domain.Session;
import fr.cnes.regards.framework.modules.session.management.service.controllers.SessionService;
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
 * @author Iliana Ghazali
 **/

@RestController
@RequestMapping(SessionController.ROOT_MAPPING)
public class SessionController implements IResourceController<Session> {

    /**
     * Hypermedia resource service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Source Repository
     */
    @Autowired
    private SessionService sessionService;

    /**
     * Rest root path
     */
    public static final String ROOT_MAPPING = "/sessions";

    /**
     * Delete session path
     */
    public static final String DELETE_SESSION_MAPPING = "/{id}";

    @GetMapping
    @ResponseBody
    public ResponseEntity<PagedModel<EntityModel<Session>>> getSessions(@RequestParam(required = false) String name,
            @RequestParam(required = false) String state, @RequestParam(required = false) String source,
            @PageableDefault(sort = "lastUpdateDate", direction = Sort.Direction.DESC, size = 20) Pageable pageable,
            PagedResourcesAssembler<Session> assembler) {
        Page<Session> sessions = this.sessionService.loadSessions(name, state, source, pageable);
        return ResponseEntity.ok(toPagedResources(sessions, assembler));
    }

    @RequestMapping(value = DELETE_SESSION_MAPPING, method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<Void> deleteSession(@PathVariable("id") final long id) {
        try {
            this.sessionService.orderDeleteSession(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public EntityModel<Session> toResource(Session session, Object... extras) {
        EntityModel<Session> resource = resourceService.toResource(session);
        resourceService.addLink(resource, this.getClass(), "getSessions", LinkRels.LIST,
                                MethodParamFactory.build(String.class, session.getName()),
                                MethodParamFactory.build(String.class),
                                MethodParamFactory.build(String.class, session.getSource()),
                                MethodParamFactory.build(Pageable.class));
        resourceService.addLink(resource, this.getClass(), "deleteSession", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, session.getId()));
        return resourceService.toResource(session);
    }
}
