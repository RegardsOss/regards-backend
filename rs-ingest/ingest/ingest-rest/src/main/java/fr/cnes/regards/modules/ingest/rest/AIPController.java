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
package fr.cnes.regards.modules.ingest.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.swagger.autoconfigure.PageableQueryParam;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntityLight;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.ingest.dto.request.dissemination.AIPDisseminationRequestDto;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.AipDisseminationService;
import fr.cnes.regards.modules.ingest.service.aip.AIPStorageService;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.request.OAISDeletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * This controller manages AIP.
 *
 * @author Léo Mieulet
 * @author Marc Sordi
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(AIPStorageService.AIPS_CONTROLLER_ROOT_PATH)
public class AIPController implements IResourceController<AIPEntityLight> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPController.class);

    public static final String REQUEST_PARAM_STATE = "state";

    public static final String REQUEST_PARAM_FROM = "lastUpdate.from";

    public static final String REQUEST_PARAM_TO = "lastUpdate.to";

    public static final String REQUEST_PARAM_TAGS = "tags";

    public static final String REQUEST_PARAM_PROVIDER_ID = "providerIds";

    public static final String REQUEST_PARAM_SESSION_OWNER = "sessionOwner";

    public static final String REQUEST_PARAM_SESSION = "session";

    public static final String REQUEST_PARAM_CATEGORIES = "categories";

    public static final String REQUEST_PARAM_STORAGES = "storages";

    public static final String REQUEST_PARAM_AIP_IDS = "aipId";

    /**
     * Controller path to manage tags of multiple AIPs
     */
    public static final String TAG_MANAGEMENT_PATH = "/tags";

    public static final String SEARCH_PATH = "/search";

    /**
     * Controller path to search used tags by multiple AIPs
     */
    public static final String TAG_SEARCH_PATH = TAG_MANAGEMENT_PATH + SEARCH_PATH;

    /**
     * Controller path to manage storage of multiple AIPs
     */
    public static final String STORAGE_MANAGEMENT_PATH = "/storages";

    /**
     * Controller path to search used storages by multiple AIPs
     */
    public static final String STORAGE_SEARCH_PATH = STORAGE_MANAGEMENT_PATH + SEARCH_PATH;

    /**
     * Controller path to manage storage of multiple AIPs
     */
    public static final String CATEGORIES_MANAGEMENT_PATH = "/categories";

    /**
     * Controller path to search used storages by multiple AIPs
     */
    public static final String CATEGORIES_SEARCH_PATH = CATEGORIES_MANAGEMENT_PATH + SEARCH_PATH;

    /**
     * Controller path to update multiple AIPs using criteria and modification lists
     */
    public static final String AIP_UPDATE_PATH = "/update";

    /**
     * Controller path to delete several OAIS entities
     */
    public static final String OAIS_DELETE_PATH = "/delete";

    /**
     * Controller path to disseminate AIPs to another Regards instance
     */
    private static final String AIP_DISSEMINATION_PATH = "/dissemination";

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private OAISDeletionService oaisDeletionRequestService;

    @Autowired
    private AipDisseminationService aipDisseminationService;

    /**
     * Retrieve a page of AIPs metadata according to the given filters
     *
     * @return page of aip metadata respecting the constraints
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get AIPs", description = "Return a page of AIPs matching criterias.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All AIPs were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve all AIPs matching criterias", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<AIPEntityLight>>> searchAIPs(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criterias.",
                                                              content = @Content(schema = @Schema(implementation = SearchAIPsParameters.class)))
        @Parameter(description = "Filter criterias for AIPs") @RequestBody SearchAIPsParameters filters,
        @PageableQueryParam @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<AIPEntityLight> assembler) {

        return new ResponseEntity<>(toPagedResources(aipService.findLightByFilters(filters, pageable), assembler),
                                    HttpStatus.OK);
    }

    @PostMapping(value = AIP_DISSEMINATION_PATH)
    @Operation(summary = "Create dissemination creator job")
    @ResourceAccess(description = "Endpoint to create a dissemination job. A scheduler will soon launch these requests",
                    role = DefaultRole.ADMIN)
    public void createDisseminationRequest(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criterias.",
                                                              content = @Content(schema = @Schema(implementation = AIPDisseminationRequestDto.class)))
        @Parameter(description = "Body contains filter criterias for AIPs, and dissemination recipient list")
        @RequestBody AIPDisseminationRequestDto disseminationRequestDto) {
        aipDisseminationService.registerDisseminationCreator(disseminationRequestDto);
    }

    /**
     * Retrieve common tags according to the given filters
     *
     * @return tags
     */
    @RequestMapping(value = TAG_SEARCH_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "Search tags used by aips", role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<String>> retrieveAIPTags(@Valid @RequestBody SearchAIPsParameters filters) {
        List<String> aipTags = aipService.findTags(filters);
        return new ResponseEntity<>(aipTags, HttpStatus.OK);
    }

    /**
     * Retrieve common storage location (pluginBusinessId) according to the given filters
     *
     * @return storage location
     */
    @RequestMapping(value = STORAGE_SEARCH_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "Search tags used by aips", role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<String>> retrieveAIPStorage(@Valid @RequestBody SearchAIPsParameters filters) {
        List<String> aipTags = aipService.findStorages(filters);
        return new ResponseEntity<>(aipTags, HttpStatus.OK);
    }

    /**
     * Retrieve common categories according to the given filters
     *
     * @return categories
     */
    @RequestMapping(value = CATEGORIES_SEARCH_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "Search categories used by aips", role = DefaultRole.EXPLOIT)
    public ResponseEntity<List<String>> retrieveAIPCategories(@Valid @RequestBody SearchAIPsParameters filters) {
        List<String> aipTags = aipService.findCategories(filters);
        return new ResponseEntity<>(aipTags, HttpStatus.OK);
    }

    @RequestMapping(value = AIPStorageService.AIP_DOWNLOAD_PATH,
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResourceAccess(description = "Download AIP as JSON file", role = DefaultRole.PUBLIC)
    public void downloadAIP(@RequestParam(required = false, name = "origin") String origin,
                            @Valid @PathVariable(AIPStorageService.AIP_ID_PATH_PARAM) String aipId,
                            HttpServletResponse response) throws ModuleException, IOException {

        LOGGER.debug("Downloading AIP file for entity \"{}\"", aipId);

        try {
            aipService.downloadAIP(OaisUniformResourceName.fromString(aipId), response);
        } catch (ModuleException e) {

            // Workaround to handle conversion of ServletErrorResponse in JSON format and
            // avoid using ContentType of file set before.
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            throw e;
        }
        response.getOutputStream().flush();
        response.setStatus(HttpStatus.OK.value());
    }

    @RequestMapping(value = AIP_UPDATE_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "Update an AIP set with provided params", role = DefaultRole.ADMIN)
    public void updateAips(@Valid @RequestBody AIPUpdateParametersDto params) {
        LOGGER.debug("Received request to update AIPs");
        aipService.registerUpdatesCreator(params);
    }

    @ResourceAccess(description = "Delete OAIS entities", role = DefaultRole.ADMIN)
    @RequestMapping(value = OAIS_DELETE_PATH, method = RequestMethod.POST)
    public void delete(@Valid @RequestBody OAISDeletionPayloadDto deletionRequest) throws ModuleException {
        LOGGER.debug("Received request to delete OAIS entities");
        oaisDeletionRequestService.registerOAISDeletionCreator(deletionRequest);
    }

    @Override
    public EntityModel<AIPEntityLight> toResource(AIPEntityLight element, Object... extras) {
        EntityModel<AIPEntityLight> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "createDisseminationRequest",
                                LinkRels.NOTIFY,
                                MethodParamFactory.build(AIPDisseminationRequestDto.class));
        return resource;
    }

}
