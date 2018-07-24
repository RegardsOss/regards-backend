/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.job.AIPQueryFilters;
import fr.cnes.regards.modules.storage.service.IAIPService;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Léo Mieulet
 */
@RestController
@RequestMapping(AIPSessionController.TYPE_MAPPING)
public class AIPSessionController implements IResourceController<AIPSession> {

    public static final String TYPE_MAPPING = "/sessions";

    public static final String ID_PATH = "/{id}";

    @Autowired
    private IAIPService aipService;

    /**
     * Service handling hypermedia resources
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Return session and number of AIP for each session
     * @param id
     * @param from
     * @param to
     * @param pageable
     * @param pAssembler
     * @return
     */
    @ResourceAccess(description = "Search for SIPSession with optional criterion.")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<PagedResources<Resource<AIPSession>>> search(
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "from",
                    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = "to",
                    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Pageable pageable, PagedResourcesAssembler<AIPSession> pAssembler) {
        Page<AIPSession> aipSessions = aipService.searchSessions(id, from, to, pageable);
        PagedResources<Resource<AIPSession>> resources = toPagedResources(aipSessions, pAssembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Return a single AIPSession using its id (name)
     * @param id
     * @return
     */
    @ResourceAccess(description = "Get one session using its name.")
    @RequestMapping(value = ID_PATH, method = RequestMethod.GET)
    public ResponseEntity<Resource<AIPSession>> getAipSession(@PathVariable(name = "id") String id) {
        AIPSession session = aipService.getSessionWithStats(id);
        return new ResponseEntity<>(toResource(session), HttpStatus.OK);
    }

    @ResourceAccess(description = "Delete all AIP having that session name.")
    @RequestMapping(value = ID_PATH, method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteAipEntityBySessionId(@PathVariable("id") String id) throws ModuleException {
        AIPQueryFilters filter = new AIPQueryFilters();
        filter.setSession(id);
        aipService.deleteAIPsByQuery(filter);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<AIPSession> toResource(AIPSession sipSession, Object... pExtras) {
        final Resource<AIPSession> resource = resourceService.toResource(sipSession);
        resourceService.addLink(resource, this.getClass(), "getAipSession", LinkRels.SELF,
                MethodParamFactory.build(String.class, sipSession.getId()));
        // If the session has some deletable AIPS, add the delete key
        if (sipSession.getStoredAipsCount() + sipSession.getQueuedAipsCount() > 0) {
            resourceService.addLink(resource, this.getClass(), "deleteAipEntityBySessionId", LinkRels.DELETE,
                    MethodParamFactory.build(String.class, sipSession.getId()));
        }
        return resource;
    }
}
