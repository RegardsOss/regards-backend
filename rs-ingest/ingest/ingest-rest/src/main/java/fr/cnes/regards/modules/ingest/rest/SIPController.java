/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
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
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.service.IIngestService;
import fr.cnes.regards.modules.ingest.service.ISIPService;
import fr.cnes.regards.modules.storage.domain.RejectedSip;

/**
 * This controller manages SIP submission API.
 *
 * @author Marc Sordi
 *
 */
@RestController
@RequestMapping(SIPController.TYPE_MAPPING)
public class SIPController implements IResourceController<SIPEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPController.class);

    public static final String TYPE_MAPPING = "/sips";

    public static final String REQUEST_PARAM_SIP_ID = "sipId";

    public static final String SIPID_PATH = "/{" + REQUEST_PARAM_SIP_ID + "}";

    public static final String RETRY_PATH = "/retry";

    public static final String IMPORT_PATH = "/import";

    public static final String REQUEST_PARAM_PROVIDER_ID = "providerId";

    public static final String REQUEST_PARAM_OWNER = "owner";

    public static final String REQUEST_PARAM_FROM = "from";

    public static final String REQUEST_PARAM_STATE = "state";

    public static final String REQUEST_PARAM_PROCESSING = "processing";

    public static final String REQUEST_PARAM_SESSION_ID = "sessionId";

    public static final String REQUEST_PARAM_FILE = "file";

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
    public ResponseEntity<Collection<SIPDto>> ingestFile(@RequestParam(name = REQUEST_PARAM_FILE) MultipartFile file)
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
            @RequestParam(name = REQUEST_PARAM_PROVIDER_ID, required = false) String providerId,
            @RequestParam(name = REQUEST_PARAM_OWNER, required = false) String owner,
            @RequestParam(name = REQUEST_PARAM_FROM,
                    required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(name = REQUEST_PARAM_STATE, required = false) List<SIPState> state,
            @RequestParam(name = REQUEST_PARAM_PROCESSING, required = false) String processing,
            @RequestParam(name = REQUEST_PARAM_SESSION_ID, required = false) String sessionId,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<SIPEntity> pAssembler) {
        Page<SIPEntity> sipEntities = sipService.search(providerId, sessionId, owner, from, state, processing,
                                                        pageable);
        PagedResources<Resource<SIPEntity>> resources = toPagedResources(sipEntities, pAssembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @ResourceAccess(description = "Retrieve one SIP by its sipId.")
    @RequestMapping(value = SIPID_PATH, method = RequestMethod.GET)
    public ResponseEntity<Resource<SIPEntity>> getSipEntity(@PathVariable(REQUEST_PARAM_SIP_ID) String sipId)
            throws ModuleException {
        SIPEntity sip = sipService.getSIPEntity(UniformResourceName.fromString(sipId));
        return new ResponseEntity<>(toResource(sip), HttpStatus.OK);
    }

    @ResourceAccess(description = "Delete one SIP by its providerId.")
    @RequestMapping(method = RequestMethod.DELETE)
    public ResponseEntity<Collection<RejectedSip>> deleteSipEntityByProviderId(
            @RequestParam("providerId") String providerId) throws ModuleException {
        return new ResponseEntity<>(sipService.deleteSIPEntitiesForProviderId(providerId), HttpStatus.OK);
    }

    @ResourceAccess(description = "Delete one SIP by its sipId.")
    @RequestMapping(value = SIPID_PATH, method = RequestMethod.DELETE)
    public ResponseEntity<Collection<RejectedSip>> deleteSipEntity(@PathVariable(REQUEST_PARAM_SIP_ID) String sipId)
            throws ModuleException {
        return new ResponseEntity<>(
                sipService.deleteSIPEntitiesBySipIds(Sets.newHashSet(UniformResourceName.fromString(sipId))),
                HttpStatus.OK);
    }

    @ResourceAccess(description = "Retry SIP ingestion by its sipId.")
    @RequestMapping(value = SIPID_PATH + RETRY_PATH, method = RequestMethod.POST)
    public ResponseEntity<Void> retrySipEntityIngest(@PathVariable(REQUEST_PARAM_SIP_ID) String sipId)
            throws ModuleException {
        ingestService.retryIngest(UniformResourceName.fromString(sipId));
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
                                MethodParamFactory.build(String.class, sipEntity.getSipId().toString()));
        try {
            if (sipService.isDeletable(sipEntity.getSipIdUrn())) {
                resourceService.addLink(resource, this.getClass(), "deleteSipEntity", LinkRels.DELETE,
                                        MethodParamFactory.build(String.class, sipEntity.getSipId().toString()));
            }
            if (ingestService.isRetryable(sipEntity.getSipIdUrn())) {
                resourceService.addLink(resource, this.getClass(), "retrySipEntityIngest", "retry",
                                        MethodParamFactory.build(String.class, sipEntity.getSipId().toString()));
            }
        } catch (EntityNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return resource;
    }
}
