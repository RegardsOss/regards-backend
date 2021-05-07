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
package fr.cnes.regards.modules.sessionmanager.rest;

import java.time.OffsetDateTime;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.sessionmanager.domain.SessionAdmin;
import fr.cnes.regards.modules.sessionmanager.domain.SessionState;
import fr.cnes.regards.modules.sessionmanager.domain.dto.UpdateSession;
import fr.cnes.regards.modules.sessionmanager.service.ISessionService;

/**
 * REST module controller for session
 * @author Léo Mieulet
 */
@RestController
@RequestMapping(SessionController.BASE_MAPPING)
public class SessionController implements IResourceController<SessionAdmin> {

    /**
     * Base mapping
     */
    public static final String BASE_MAPPING = "/sessions";

    /**
     * Endpoint to retrieve the list of session sources
     */
    public static final String SOURCE_MAPPING = "/sources";

    /**
     * Endpoint to retrieve the list of session names
     */
    public static final String NAME_MAPPING = "/names";

    /**
     * Endpoint to handle a piece of session
     */
    public static final String SESSION_MAPPING = "/{session_id}";

    @Autowired
    private IResourceService resourceService;

    @Autowired
    private ISessionService sessionService;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieve all sessions", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<SessionAdmin>>> getSessions(
            @RequestParam(value = "source", required = false) String source,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "from",
                    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(value = "to",
                    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(value = "state", required = false) SessionState state,
            @RequestParam(value = "onlyLastSession", required = false) boolean onlyLastSession,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<SessionAdmin> assembler) {
        Page<SessionAdmin> sessions = sessionService.retrieveSessions(source, name, from, to, state, onlyLastSession,
                                                                      pageable);
        PagedModel<EntityModel<SessionAdmin>> resources = toPagedResources(sessions, assembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = NAME_MAPPING)
    @ResourceAccess(description = "Retrieve a subset of session names", role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<String>> getSessionNames(@RequestParam(value = "name", required = false) String name) {
        return new ResponseEntity<>(sessionService.retrieveSessionNames(name), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = SOURCE_MAPPING)
    @ResourceAccess(description = "Retrieve a subset of session sources", role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<String>> getSessionSources(
            @RequestParam(value = "source", required = false) String source) {
        return new ResponseEntity<>(sessionService.retrieveSessionSources(source), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PATCH, value = SESSION_MAPPING)
    @ResourceAccess(description = "Update specific field of the session", role = DefaultRole.EXPLOIT)
    public ResponseEntity<EntityModel<SessionAdmin>> updateSession(@PathVariable("session_id") Long id,
            @Valid @RequestBody UpdateSession session) throws ModuleException {
        SessionAdmin updateSession = sessionService.updateSessionState(id, session.getState());
        return new ResponseEntity<>(toResource(updateSession), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = SESSION_MAPPING)
    @ResourceAccess(description = "Delete the session using its id", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> deleteSession(@PathVariable("session_id") Long id,
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force)
            throws ModuleException {
        sessionService.deleteSession(id, force);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<SessionAdmin> toResource(SessionAdmin element, Object... pExtras) {
        final EntityModel<SessionAdmin> resource = resourceService.toResource(element);
        if (element.getState() == SessionState.ERROR) {
            resourceService.addLink(resource, this.getClass(), "updateSession", LinkRels.UPDATE,
                                    MethodParamFactory.build(Long.class, element.getId()),
                                    MethodParamFactory.build(UpdateSession.class));
        }
        if (element.getState() != SessionState.DELETED) {
            resourceService.addLink(resource, this.getClass(), "deleteSession", LinkRels.DELETE,
                                    MethodParamFactory.build(Long.class, element.getId()),
                                    MethodParamFactory.build(boolean.class));
        }
        return resource;
    }
}
