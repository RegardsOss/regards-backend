/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;
import fr.cnes.regards.modules.ingest.service.ISIPSessionService;

@RestController
@RequestMapping(SIPSessionController.TYPE_MAPPING)
public class SIPSessionController implements IResourceController<SIPSession> {

    public static final String TYPE_MAPPING = "/sessions";

    public static final String ID_PATH = "/{id}";

    public static final String REQUEST_PARAM_ID = "id";

    public static final String REQUEST_PARAM_FROM = "from";

    public static final String REQUEST_PARAM_TO = "to";

    @Autowired
    private ISIPSessionService sipSessionService;

    /**
     * Service handling hypermedia resources
     */
    @Autowired
    private IResourceService resourceService;

    @ResourceAccess(description = "Search for SIPSession with optional criterion.")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<PagedResources<Resource<SIPSession>>> search(
            @RequestParam(name = REQUEST_PARAM_ID, required = false) String id,
            @RequestParam(name = REQUEST_PARAM_FROM,
                    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = REQUEST_PARAM_TO,
                    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Pageable pageable, PagedResourcesAssembler<SIPSession> pAssembler) {
        Page<SIPSession> sipSessions = sipSessionService.search(id, from, to, pageable);
        PagedResources<Resource<SIPSession>> resources = toPagedResources(sipSessions, pAssembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @ResourceAccess(description = "Delete one SIP by is ipId.")
    @RequestMapping(value = ID_PATH, method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteSipEntityBySipId(@PathVariable("id") String id) throws ModuleException {
        sipSessionService.deleteSIPSession(id).isEmpty();
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Get one SIP by is ipId.")
    @RequestMapping(value = ID_PATH, method = RequestMethod.GET)
    public ResponseEntity<Resource<SIPSession>> getSipSession(@PathVariable(name = "id") String id) {
        SIPSession session = sipSessionService.getSession(id, false);
        return new ResponseEntity<>(toResource(session), HttpStatus.OK);
    }

    @Override
    public Resource<SIPSession> toResource(SIPSession sipSession, Object... pExtras) {
        final Resource<SIPSession> resource = resourceService.toResource(sipSession);
        resourceService.addLink(resource, this.getClass(), "getSipSession", LinkRels.SELF,
                                MethodParamFactory.build(String.class, sipSession.getId()));
        resourceService.addLink(resource, this.getClass(), "deleteSipEntityBySipId", LinkRels.DELETE,
                                MethodParamFactory.build(String.class, sipSession.getId()));
        return resource;
    }

}
