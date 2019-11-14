/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.domain.dto.RequestInfoDto;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionRequestDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIPCollection;
import fr.cnes.regards.modules.ingest.dto.sip.SearchSIPsParameters;
import fr.cnes.regards.modules.ingest.service.IIngestService;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;

/**
 * REST API for managing SIP
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

    public static final String DELETE_PATH = "/delete";

    public static final String REQUEST_PARAM_PROVIDER_ID = "providerId";

    public static final String REQUEST_PARAM_FROM = "from";

    public static final String REQUEST_PARAM_STATE = "state";

    public static final String REQUEST_PARAM_PROCESSING = "processing";

    public static final String REQUEST_PARAM_SESSION_OWNER = "sessionOwner";

    public static final String REQUEST_PARAM_SESSION = "session";

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
     * @return {@link RequestInfoDto}
     * @throws ModuleException if error occurs!
     */
    @ResourceAccess(description = "SIP collections submission (bulk request)")
    @RequestMapping(method = RequestMethod.POST, consumes = GeoJsonMediaType.APPLICATION_GEOJSON_UTF8_VALUE)
    public ResponseEntity<RequestInfoDto> ingest(@RequestBody SIPCollection sips) throws ModuleException {
        RequestInfoDto requestInfo = ingestService.handleSIPCollection(sips);
        return ResponseEntity.status(computeStatus(requestInfo)).body(requestInfo);
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
    public ResponseEntity<RequestInfoDto> ingestFile(@RequestParam(name = REQUEST_PARAM_FILE) MultipartFile file)
            throws ModuleException {
        try {
            RequestInfoDto requestInfo = ingestService.handleSIPCollection(file.getInputStream());
            return ResponseEntity.status(computeStatus(requestInfo)).body(requestInfo);
        } catch (IOException e) {
            final String message = "Error with file stream while importing model.";
            LOGGER.error(message, e);
            throw new ModuleException(e);
        }
    }

    /**
     * Compute {@link HttpStatus} according to information return by the service
     */
    private HttpStatus computeStatus(RequestInfoDto info) {
        Boolean hasGranted = !info.getGranted().isEmpty();
        Boolean hasDenied = !info.getDenied().isEmpty();

        HttpStatus status;
        if (hasGranted && hasDenied) {
            status = HttpStatus.PARTIAL_CONTENT; // 206
        } else if (hasDenied) {
            status = HttpStatus.UNPROCESSABLE_ENTITY; // 422
        } else {
            status = HttpStatus.CREATED; // 201
        }
        return status;
    }

    @ResourceAccess(description = "Search for SIPEntities with optional criterion.")
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<PagedResources<Resource<SIPEntity>>> search(@RequestBody SearchSIPsParameters params,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<SIPEntity> pAssembler) {
        Page<SIPEntity> sipEntities = sipService.search(params, pageable);
        PagedResources<Resource<SIPEntity>> resources = toPagedResources(sipEntities, pAssembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @ResourceAccess(description = "Retrieve one SIP by its sipId.")
    @RequestMapping(value = SIPID_PATH, method = RequestMethod.GET)
    public ResponseEntity<Resource<SIPEntity>> getSipEntity(@PathVariable(REQUEST_PARAM_SIP_ID) String sipId)
            throws ModuleException {
        SIPEntity sip = sipService.getEntity(sipId);
        return new ResponseEntity<>(toResource(sip), HttpStatus.OK);
    }

    @ResourceAccess(description = "Delete by session")
    @RequestMapping(value = DELETE_PATH, method = RequestMethod.POST)
    public ResponseEntity<OAISDeletionRequestDto> deleteBySession(
            @Valid @RequestBody OAISDeletionRequestDto deletionRequest) throws ModuleException {
        OAISDeletionRequestDto response = ingestService.registerOAISDeletionRequest(deletionRequest);
        return ResponseEntity.ok(response);
    }

    @ResourceAccess(description = "Retry SIP ingestion by its sipId.")
    @RequestMapping(value = SIPID_PATH + RETRY_PATH, method = RequestMethod.POST)
    public ResponseEntity<Void> retrySipEntityIngest(@PathVariable(REQUEST_PARAM_SIP_ID) String sipId)
            throws ModuleException {
        // TODO
        // ingestService.retryIngest(UniformResourceName.fromString(sipId));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<SIPEntity> toResource(SIPEntity sipEntity, Object... pExtras) {
        final Resource<SIPEntity> resource = resourceService.toResource(sipEntity);
        resourceService.addLink(resource, this.getClass(), "getSipEntity", LinkRels.SELF,
                                MethodParamFactory.build(String.class, sipEntity.getSipId().toString()));
        // FIXME
        //        try {
        //            if (sipService.isDeletable(sipEntity.getSipIdUrn())) {
        //                resourceService.addLink(resource, this.getClass(), "deleteSipEntity", LinkRels.DELETE,
        //                                        MethodParamFactory.build(String.class, sipEntity.getSipId().toString()));
        //            }
        //            if (ingestService.isRetryable(sipEntity.getSipIdUrn())) {
        //                resourceService.addLink(resource, this.getClass(), "retrySipEntityIngest", "retry",
        //                                        MethodParamFactory.build(String.class, sipEntity.getSipId().toString()));
        //            }
        //        } catch (EntityNotFoundException e) {
        //            LOGGER.error(e.getMessage(), e);
        //        }

        return resource;
    }
}
