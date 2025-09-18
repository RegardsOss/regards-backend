/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.swagger.autoconfigure.PageableQueryParam;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.dto.FeatureEntityRawDto;
import fr.cnes.regards.modules.feature.service.IFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SlicedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.SlicedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * End point to search {@link FeatureEntityRawDto} which are representations of a
 * {@link fr.cnes.regards.modules.feature.domain.FeatureEntity FeatureEntity} with the feature field serialized as a
 * JSON String.
 *
 * @author Thibaud Michaudel
 */
@RestController
@RequestMapping(FeatureEntityRawController.PATH_DATA_FEATURE_RAW_OBJECT)
public class FeatureEntityRawController implements IResourceController<FeatureEntityRawDto> {

    public static final String PATH_DATA_FEATURE_RAW_OBJECT = "/admin/features/raw";

    private final IFeatureService featureService;

    private final IResourceService resourceService;

    /**
     * Controller path to retrieve a slice of features
     */
    private static final String SLICE_PATH = "/slice";

    public FeatureEntityRawController(IFeatureService featureService, IResourceService resourceService) {
        this.featureService = featureService;
        this.resourceService = resourceService;
    }

    /**
     * Get a {@link Page} of {@link FeatureEntityRawDto} matching provided {@link SearchFeatureSimpleEntityParameters}
     * filters
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get features", description = "Return a page of features matching criteria.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All features were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve features matching criteria", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<FeatureEntityRawDto>>> searchFeaturesRaw(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criteria.",
                                                              content = @Content(schema = @Schema(implementation = SearchFeatureSimpleEntityParameters.class)))
        @Parameter(description = "Filter criteria for features") @RequestBody
        SearchFeatureSimpleEntityParameters filters,
        @PageableQueryParam @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<FeatureEntityRawDto> assembler) {

        return new ResponseEntity<>(toPagedResources(featureService.findAllRaw(filters, pageable), assembler),
                                    HttpStatus.OK);
    }

    /**
     * Get a {@link Slice} of {@link FeatureEntityRawDto} matching provided {@link SearchFeatureSimpleEntityParameters}
     * filters
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, path = SLICE_PATH)
    @Operation(summary = "Get features", description = "Return a slice of features matching criteria.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All features were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve features matching criteria", role = DefaultRole.EXPLOIT)
    public ResponseEntity<SlicedModel<EntityModel<FeatureEntityRawDto>>> searchFeaturesSlice(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criteria.",
                                                              content = @Content(schema = @Schema(implementation = SearchFeatureSimpleEntityParameters.class)))
        @Parameter(description = "Filter criteria for features") @RequestBody
        SearchFeatureSimpleEntityParameters filters,
        @PageableQueryParam @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) SlicedResourcesAssembler<FeatureEntityRawDto> assembler) {

        Slice<FeatureEntityRawDto> slice = featureService.findAllRawSlice(filters, pageable);
        return new ResponseEntity<>(toSlicedResources(slice, assembler), HttpStatus.OK);
    }

    @Override
    public EntityModel<FeatureEntityRawDto> toResource(FeatureEntityRawDto element, Object... extras) {
        return resourceService.toResource(element);
    }
}
