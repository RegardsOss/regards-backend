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

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.service.IIngestService;
import fr.cnes.regards.modules.ingest.service.ISIPService;

/**
 * This controller manages SIP submission API.
 *
 * @author Marc Sordi
 *
 */
@RestController
@ModuleInfo(name = "SIP management module", description = "SIP submission and management", version = "2.0.0-SNAPSHOT",
        author = "CSSI", legalOwner = "CNES", documentation = "https://github.com/RegardsOss")
@RequestMapping(SIPController.TYPE_MAPPING)
public class SIPController implements IResourceController<SIPEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPController.class);

    public static final String TYPE_MAPPING = "/sips";

    public static final String IPID_PATH = "/{ipId}";

    public static final String RETRY_PATH = "/retry";

    public static final String IMPORT_PATH = "/import";

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private ISIPService sipService;

    /**
     * Service handling hypermedia resources
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Manage SIP bulk request
     *
     * @param sips {@link SIPCollection}
     * @return {@link SIPEntity} collection
     * @throws ModuleException if error occurs!
     */
    @ResourceAccess(description = "SIP collections submission (bulk request)")
    @RequestMapping(method = RequestMethod.POST, consumes = GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE)
    public ResponseEntity<Collection<SIPDto>> ingest(@RequestBody SIPCollection sips) throws ModuleException {
        Collection<SIPDto> dtos = ingestService.ingest(sips);
        HttpStatus status = computeStatus(dtos);
        return ResponseEntity.status(status).body(dtos);
    }

    /**
     * Import a SIP collection by file
     *
     * @param file
     *            model to import
     * @return nothing
     * @throws ModuleException
     *             if error occurs!
     */
    @ResourceAccess(description = "SIP collection submission using multipart request")
    @RequestMapping(method = RequestMethod.POST, value = IMPORT_PATH)
    public ResponseEntity<Collection<SIPDto>> ingestFile(@RequestParam("file") MultipartFile file)
            throws ModuleException {
        try {
            Collection<SIPDto> dtos = ingestService.ingest(file.getInputStream());
            HttpStatus status = computeStatus(dtos);
            return ResponseEntity.status(status).body(dtos);
        } catch (IOException e) {
            final String message = "Error with file stream while importing model.";
            LOGGER.error(message, e);
            throw new ModuleException(e);
        }
    }

    @ResourceAccess(description = "Search for SIPEntities with optional criterion.")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<PagedResources<Resource<SIPEntity>>> search(
            @RequestParam(name = "sipId", required = false) String sipId,
            @RequestParam(name = "owner", required = false) String owner,
            @RequestParam(name = "from", required = false) OffsetDateTime from,
            @RequestParam(name = "state", required = false) SIPState state,
            @RequestParam(name = "processing", required = false) String processing,
            @RequestParam(name = "sessionId", required = false) String sessionId, Pageable pageable,
            PagedResourcesAssembler<SIPEntity> pAssembler) {
        Page<SIPEntity> sipEntities = sipService.search(sipId, sessionId, owner, from, state, processing, pageable);
        PagedResources<Resource<SIPEntity>> resources = toPagedResources(sipEntities, pAssembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @ResourceAccess(description = "Retrieve one SIP by is ipId.")
    @RequestMapping(value = IPID_PATH, method = RequestMethod.GET)
    public ResponseEntity<Resource<SIPEntity>> getSipEntity(@PathVariable("ipId") String ipId) throws ModuleException {
        SIPEntity sip = sipService.getSIPEntity(ipId);
        return new ResponseEntity<>(toResource(sip), HttpStatus.OK);
    }

    @ResourceAccess(description = "Delete one SIP by is sipId.")
    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteSipEntityBySipId(@RequestParam("sipId") String sipId) throws ModuleException {
        sipService.deleteSIPEntitiesForSipId(sipId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Delete one SIP by is sipId.")
    @RequestMapping(value = IPID_PATH, method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteSipEntity(@PathVariable("ipId") String sipId) throws ModuleException {
        sipService.deleteSIPEntitiesByIpIds(Sets.newHashSet(sipId));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "Retry SIP ingestion by is ipId.")
    @RequestMapping(value = IPID_PATH + RETRY_PATH, method = RequestMethod.POST)
    public ResponseEntity<Void> retrySipEntityIngest(@PathVariable("ipId") String ipId) throws ModuleException {
        ingestService.retryIngest(ipId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private HttpStatus computeStatus(Collection<SIPDto> dtos) {
        Boolean hasCreated = Boolean.FALSE;
        Boolean hasRejected = Boolean.FALSE;
        for (SIPDto sipEntity : dtos) {
            switch (sipEntity.getState()) {
                case CREATED:
                    hasCreated = Boolean.TRUE;
                    break;
                case REJECTED:
                    hasRejected = Boolean.TRUE;
                    break;
                default:
                    LOGGER.warn("Unexpected SIP state");
                    break;
            }
        }
        HttpStatus status;
        if (hasCreated && hasRejected) {
            status = HttpStatus.PARTIAL_CONTENT; // 206
        } else if (hasRejected) {
            status = HttpStatus.UNPROCESSABLE_ENTITY; // 422
        } else {
            status = HttpStatus.CREATED; // 201
        }
        return status;
    }

    @Override
    public Resource<SIPEntity> toResource(SIPEntity sipEntity, Object... pExtras) {
        final Resource<SIPEntity> resource = resourceService.toResource(sipEntity);
        resourceService.addLink(resource, this.getClass(), "getSipEntity", LinkRels.SELF,
                                MethodParamFactory.build(String.class, sipEntity.getIpId()));
        try {
            if (sipService.isDeletable(sipEntity.getIpId())) {
                resourceService.addLink(resource, this.getClass(), "deleteSipEntity", LinkRels.DELETE,
                                        MethodParamFactory.build(String.class, sipEntity.getIpId()));
            }
            if (sipService.isRetryable(sipEntity.getIpId())) {
                resourceService.addLink(resource, this.getClass(), "retrySipEntityIngest", "retry",
                                        MethodParamFactory.build(String.class, sipEntity.getIpId()));
            }
        } catch (EntityNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return resource;
    }
}
