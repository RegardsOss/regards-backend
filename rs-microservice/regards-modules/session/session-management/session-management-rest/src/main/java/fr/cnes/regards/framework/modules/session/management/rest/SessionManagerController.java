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
import fr.cnes.regards.framework.modules.session.management.service.controllers.SessionManagerService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import java.util.Set;
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
 * Controller for {@link Session}
 *
 * @author Iliana Ghazali
 **/

@RestController
@RequestMapping(SessionManagerController.ROOT_MAPPING)
public class SessionManagerController implements IResourceController<Session> {

    /**
     * Hypermedia resource service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Source Repository
     */
    @Autowired
    private SessionManagerService sessionManagerService;

    /**
     * Rest root path
     */
    public static final String ROOT_MAPPING = "/sessions";

    /**
     * Endpoint to retrieve the list of session names
     */
    public static final String NAME_MAPPING = "/names";

    /**
     * Delete session path
     */
    public static final String DELETE_SESSION_MAPPING = "/{id}";

    @GetMapping
    @ResponseBody
    @ResourceAccess(description = "Endpoint to get sessions", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<Session>>> getSessions(@RequestParam(required = false) String name,
            @RequestParam(required = false) String state, @RequestParam(required = false) String source,
            @PageableDefault(sort = "lastUpdateDate" , direction = Sort.Direction.DESC, size = 20) Pageable pageable,
            PagedResourcesAssembler<Session> assembler) {
        Page<Session> sessions = this.sessionManagerService.loadSessions(name, state, source, pageable);
        return ResponseEntity.ok(toPagedResources(sessions, assembler));
    }

    @RequestMapping(method = RequestMethod.GET, value = NAME_MAPPING)
    @ResourceAccess(description = "Retrieve a subset of session names", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Set<String>> getSessionNames(@RequestParam(value = "name", required = false) String name) {
        return ResponseEntity.ok(this.sessionManagerService.retrieveSessionsNames(name));
    }

    @RequestMapping(value = DELETE_SESSION_MAPPING, method = RequestMethod.DELETE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to delete a session", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> deleteSession(@PathVariable("id") final long id) {
        try {
            this.sessionManagerService.orderDeleteSession(id);
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
                                MethodParamFactory.build(Pageable.class),
                                MethodParamFactory.build(PagedResourcesAssembler.class));
        resourceService.addLink(resource, this.getClass(), "deleteSession", LinkRels.DELETE,
                                MethodParamFactory.build(Long.TYPE, session.getId()));
        return resource;
    }
}
