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

import fr.cnes.regards.modules.ingest.dto.aip.SearchFacetsAIPsParameters;
import java.io.IOException;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;

/**
 * This controller manages AIP.
 *
 * @author Léo Mieulet
 * @author Marc Sordi
 */
@RestController
@RequestMapping(AIPController.TYPE_MAPPING)
public class AIPController implements IResourceController<AIPEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPController.class);

    public static final String TYPE_MAPPING = "/aips";

    public static final String REQUEST_PARAM_STATE = "state";

    public static final String REQUEST_PARAM_FROM = "from";

    public static final String REQUEST_PARAM_TO = "to";

    public static final String REQUEST_PARAM_TAGS = "tags";

    public static final String REQUEST_PARAM_PROVIDER_ID = "providerId";

    public static final String REQUEST_PARAM_SESSION_OWNER = "sessionOwner";

    public static final String REQUEST_PARAM_SESSION = "session";

    public static final String REQUEST_PARAM_CATEGORIES = "categories";

    public static final String REQUEST_PARAM_STORAGES = "storages";

    public static final String AIP_ID_PATH_PARAM = "aip_id";

    public static final String AIP_DOWNLOAD_PATH = "/{" + AIP_ID_PATH_PARAM + "}/download";

    /**
     * Controller path to manage tags of multiple AIPs
     */
    public static final String TAG_MANAGEMENT_PATH = "/tags";

    /**
     * Controller path to search used tags by multiple AIPs
     */
    public static final String TAG_SEARCH_PATH = TAG_MANAGEMENT_PATH + "/search";

    /**
     * Controller path to manage storage of multiple AIPs
     */
    public static final String STORAGE_MANAGEMENT_PATH = "/storages";

    /**
     * Controller path to search used storages by multiple AIPs
     */
    public static final String STORAGE_SEARCH_PATH = STORAGE_MANAGEMENT_PATH + "/search";

    /**
     * Controller path to manage storage of multiple AIPs
     */
    public static final String CATEGORIES_MANAGEMENT_PATH = "/categories";

    /**
     * Controller path to search used storages by multiple AIPs
     */
    public static final String CATEGORIES_SEARCH_PATH = CATEGORIES_MANAGEMENT_PATH + "/search";

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAIPService aipService;

    /**
     * Retrieve a page of aip metadata according to the given filters
     * @param filters
     * @param pageable
     * @param assembler
     * @return page of aip metadata respecting the constraints
     * @throws ModuleException
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "Return a page of AIPs")
    public ResponseEntity<PagedResources<Resource<AIPEntity>>> searchAIPs(SearchAIPsParameters filters,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<AIPEntity> assembler) {
        Page<AIPEntity> aips = aipService.search(filters, pageable);
        return new ResponseEntity<>(toPagedResources(aips, assembler), HttpStatus.OK);
    }

    /**
     * Retrieve common tags according to the given filters
     * @param filters
     * @return tags
     */
    @RequestMapping(value = TAG_SEARCH_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "Search tags used by aips")
    public ResponseEntity<List<String>> retrieveAIPTags(@Valid @RequestBody SearchFacetsAIPsParameters filters) {
        List<String> aipTags = aipService.searchTags(filters);
        return new ResponseEntity<>(aipTags, HttpStatus.OK);
    }

    /**
     * Retrieve common storage location (pluginBusinessId) according to the given filters
     * @param filters
     * @return storage location
     */
    @RequestMapping(value = STORAGE_SEARCH_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "Search tags used by aips")
    public ResponseEntity<List<String>> retrieveAIPStorage(@Valid @RequestBody SearchFacetsAIPsParameters filters) {
        List<String> aipTags = aipService.searchStorages(filters);
        return new ResponseEntity<>(aipTags, HttpStatus.OK);
    }

    /**
     * Retrieve common categories according to the given filters
     * @param filters
     * @return categories
     */
    @RequestMapping(value = CATEGORIES_SEARCH_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "Search categories used by aips")
    public ResponseEntity<List<String>> retrieveAIPCategories(@Valid @RequestBody SearchFacetsAIPsParameters filters) {
        List<String> aipTags = aipService.searchStorages(filters);
        return new ResponseEntity<>(aipTags, HttpStatus.OK);
    }

    @RequestMapping(value = AIP_DOWNLOAD_PATH, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResourceAccess(description = "Download AIP as JSON file")
    public void downloadAIP(@RequestParam(required = false) String origin,
            @Valid @PathVariable(AIP_ID_PATH_PARAM) UniformResourceName aipId, HttpServletResponse response)
            throws ModuleException, IOException {

        LOGGER.debug("Downloading AIP file for entity \"{}\"", aipId.toString());

        try {
            aipService.downloadAIP(aipId, response);
        } catch (ModuleException e) {
            // Workaround to handle conversion of ServletErrorResponse in JSON format and
            // avoid using ContentType of file set before.
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            throw e;
        }
        response.getOutputStream().flush();
        response.setStatus(HttpStatus.OK.value());
    }

    @Override
    public Resource<AIPEntity> toResource(AIPEntity element, Object... extras) {
        Resource<AIPEntity> resource = resourceService.toResource(element);
        return resource;
    }

}
