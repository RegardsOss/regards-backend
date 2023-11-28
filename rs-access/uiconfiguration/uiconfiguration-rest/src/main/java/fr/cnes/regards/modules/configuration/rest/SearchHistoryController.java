/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.configuration.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.SearchHistoryDto;
import fr.cnes.regards.modules.configuration.service.SearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import javax.validation.Valid;

/**
 * Controller defining REST entry point for search history management.
 *
 * @author Théo Lasserre
 */
@Tag(name = "Search history controller")
@RestController
@RequestMapping(SearchHistoryController.SEARCH_HISTORY_PATH)
public class SearchHistoryController implements IResourceController<SearchHistoryDto> {

    /**
     * Controller base path
     */
    public static final String SEARCH_HISTORY_PATH = "/history";

    private SearchHistoryService searchHistoryService;

    /**
     * {@link IResourceService} instance
     */
    private IResourceService resourceService;

    public SearchHistoryController(IResourceService resourceService, SearchHistoryService searchHistoryService) {
        this.resourceService = resourceService;
        this.searchHistoryService = searchHistoryService;
    }

    /**
     * Define the endpoint for retrieving the search history of a given user and a given module
     */
    @Operation(summary = "Retrieve search history page",
               description = "Retrieve search history page using account email and module id")
    @ApiResponses(value = { @ApiResponse(responseCode = "201",
                                         description = "Search history page was successfully retrieved. Returns "
                                                       + "SearchHistoryDto page"),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         content = { @Content(mediaType = "application/html") }) })
    @GetMapping
    @ResourceAccess(description = "Retrieve the search history of a given user and a given module",
                    role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<PagedModel<EntityModel<SearchHistoryDto>>> retrieveSearchHistory(
        @RequestParam(value = "accountEmail") final String accountEmail,
        @RequestParam(value = "moduleId") final Long moduleId,
        @Parameter(hidden = true) final PagedResourcesAssembler<SearchHistoryDto> assembler,
        @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        final Page<SearchHistoryDto> searchHistoryPage = searchHistoryService.retrieveSearchHistory(accountEmail,
                                                                                                    moduleId,
                                                                                                    pageable);
        return new ResponseEntity<>(toPagedResources(searchHistoryPage, assembler), HttpStatus.OK);
    }

    /**
     * Define the endpoint for creating a search history element
     */
    @Operation(summary = "Create a search history element.",
               description = "Create and save a search history from a search history dto, "
                             + "an account email and a module id")
    @ApiResponses(value = { @ApiResponse(responseCode = "201",
                                         description = "The searh history element was successfully saved. Returns "
                                                       + "SearchHistoryDto."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         content = { @Content(mediaType = "application/html") }),
                            @ApiResponse(responseCode = "422",
                                         description = "The search history dto syntax is incorrect.",
                                         content = { @Content(mediaType = "application/json") }) })
    @PostMapping
    @ResponseBody
    @ResourceAccess(description = "Create a new search history element", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<SearchHistoryDto>> createSearchHistory(
        @Valid @RequestBody final SearchHistoryDto searchHistoryDto,
        @RequestParam(value = "accountEmail") final String accountEmail,
        @RequestParam(value = "moduleId") Long moduleId) throws EntityException {
        final SearchHistoryDto searchHistory = searchHistoryService.addSearchHistory(searchHistoryDto,
                                                                                     accountEmail,
                                                                                     moduleId);
        return new ResponseEntity<>(toResource(searchHistory), HttpStatus.OK);
    }

    @Operation(summary = "Update a search history element.",
               description = "Update search history element using its id.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201",
                                         description = "The searh history element was successfully updated"),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         content = { @Content(mediaType = "application/html") }) })
    @PutMapping
    @ResponseBody
    @ResourceAccess(description = "Update a search history element", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<Void>> updateSearchHistory(
        @PathVariable("searchHistoryId") final Long searchHistoryId,
        @Valid @RequestBody final String searchHistoryConfig) throws EntityException {
        searchHistoryService.updateSearchHistory(searchHistoryId, searchHistoryConfig);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "Delete a search history element.",
               description = "Delete search history element using its id.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201",
                                         description = "The searh history element was successfully deleted"),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         content = { @Content(mediaType = "application/html") }) })
    @DeleteMapping("/{searchHistoryId}")
    @ResponseBody
    @ResourceAccess(description = "Delete a search history element", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<Void>> deleteSearchHistory(
        @PathVariable("searchHistoryId") final Long searchHistoryId) throws EntityException {
        searchHistoryService.deleteSearchHistory(searchHistoryId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<SearchHistoryDto> toResource(SearchHistoryDto element, Object... extras) {
        EntityModel<SearchHistoryDto> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrieveSearchHistory",
                                LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "deleteSearchHistory",
                                LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, element.getId()));
        return resource;
    }

    @Override
    public PagedModel<EntityModel<SearchHistoryDto>> toPagedResources(Page<SearchHistoryDto> SearchHistoryDto,
                                                                      PagedResourcesAssembler<SearchHistoryDto> assembler,
                                                                      Object... extras) {
        final PagedModel<EntityModel<SearchHistoryDto>> pagedResources = assembler.toModel(SearchHistoryDto);
        pagedResources.forEach(resource -> resource.add(toResource(resource.getContent()).getLinks()));
        return pagedResources;
    }
}
