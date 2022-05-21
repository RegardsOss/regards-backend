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
import fr.cnes.regards.framework.modules.session.manager.domain.Session;
import fr.cnes.regards.framework.modules.session.manager.service.controllers.SessionManagerService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
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
 * Controller for {@link Session}
 *
 * @author Iliana Ghazali
 **/

@RestController
@RequestMapping(SessionManagerController.ROOT_MAPPING)
public class SessionManagerController implements IResourceController<Session> {

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
    public static final String ID_MAPPING = "/{id}";

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

    @GetMapping
    @ResponseBody
    @ResourceAccess(description = "Endpoint to get sessions", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<Session>>> getSessions(
        @RequestParam(value = "sessionName", required = false) String sessionName,
        @RequestParam(value = "sessionState", required = false) String sessionState,
        @RequestParam(value = "sourceName", required = false) String sourceName,
        @PageableDefault(sort = "lastUpdateDate", direction = Sort.Direction.DESC, size = 20) Pageable pageable,
        PagedResourcesAssembler<Session> assembler) {
        Page<Session> sessions = this.sessionManagerService.loadSessions(sessionName,
                                                                         sessionState,
                                                                         sourceName,
                                                                         pageable);
        return ResponseEntity.ok(toPagedResources(sessions, assembler));
    }

    @GetMapping(value = NAME_MAPPING)
    @ResourceAccess(description = "Retrieve a subset of session names", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Set<String>> getSessionNames(@RequestParam(value = "name", required = false) String name) {
        return ResponseEntity.ok(this.sessionManagerService.retrieveSessionsNames(name));
    }

    @GetMapping(value = ID_MAPPING)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve a session by id", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<Session>> getSessionById(@PathVariable("id") final long id) {
        try {
            Session session = this.sessionManagerService.getSessionById(id);
            return ResponseEntity.ok(toResource(session));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping(value = ID_MAPPING)
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
        resourceService.addLink(resource,
                                this.getClass(),
                                "getSessions",
                                LinkRels.LIST,
                                MethodParamFactory.build(String.class, session.getName()),
                                MethodParamFactory.build(String.class),
                                MethodParamFactory.build(String.class, session.getSource()),
                                MethodParamFactory.build(Pageable.class),
                                MethodParamFactory.build(PagedResourcesAssembler.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "getSessionById",
                                LinkRels.SELF,
                                MethodParamFactory.build(Long.TYPE, session.getId()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "deleteSession",
                                LinkRels.DELETE,
                                MethodParamFactory.build(Long.TYPE, session.getId()));
        return resource;
    }
}