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
package fr.cnes.regards.modules.feature.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.RecipientsSearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.domain.SearchFeatureSimpleEntityParameters;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;
import fr.cnes.regards.modules.feature.service.IFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * End point to get {@link DataObjectFeature} contain data of the last created/modified {@link FeatureEntity}
 *
 * @author Kevin Marchois
 */
@RestController
@RequestMapping(FeatureEntityController.PATH_DATA_FEATURE_OBJECT)
public class FeatureEntityController implements IResourceController<FeatureEntityDto> {

    public static final String PATH_DATA_FEATURE_OBJECT = "/admin/features";

    public static final String NOTIFY_PATH = "/notify";

    public static final String DELETE_PATH = "/delete";

    public static final String URN_PATH = "/{urn}";

    @Autowired
    private IFeatureService featureService;

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Get a {@link Page} of {@link FeatureEntityDto} matching provided {@link SearchFeatureSimpleEntityParameters}
     * filters
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get features", description = "Return a page of features matching criteria.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All features were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve features matching criteria", role = DefaultRole.EXPLOIT)
    public ResponseEntity<PagedModel<EntityModel<FeatureEntityDto>>> searchFeatures(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criteria.",
                                                              content = @Content(schema = @Schema(implementation = SearchFeatureSimpleEntityParameters.class)))
        @Parameter(description = "Filter criteria for features") @RequestBody
        SearchFeatureSimpleEntityParameters filters,
        @Parameter(description = "Sorting and page configuration")
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<FeatureEntityDto> assembler) {

        return new ResponseEntity<>(toPagedResources(featureService.findAll(filters, pageable), assembler),
                                    HttpStatus.OK);
    }

    @Operation(summary = "Retrieve one feature by its urn", description = "Retrieve one feature by its urn")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Retrieve one feature by its urn") })
    @GetMapping(path = URN_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Retrieve one feature by its urn", role = DefaultRole.EXPLOIT)
    public ResponseEntity<FeatureEntityDto> getFeature(
        @Parameter(description = "URN of the feature") @PathVariable("urn") String urn) {
        return new ResponseEntity<>(featureService.findOne(FeatureUniformResourceName.fromString(urn)), HttpStatus.OK);
    }

    /**
     * Creates job to send notification for each feature matching given search parameters
     */
    @Operation(summary = "Notify features to given recipients according to search parameters",
               description = "Notify features to given recipients according to search parameters")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Notify features to given recipients according to search parameters") })
    @PostMapping(path = NOTIFY_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Notify features to given recipients according to search parameters",
                    role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> notifyFeatures(@io.swagger.v3.oas.annotations.parameters.RequestBody(description =
                                                                                                         "Set of search criteria with the "
                                                                                                         + "recipients of direct notification.",
                                                                                                     content = @Content(
                                                                                                         schema = @Schema(
                                                                                                             implementation = SearchFeatureSimpleEntityParameters.class)))
                                               @Parameter(description = "Filter criteria for features with the list of recipients")
                                               @RequestBody RecipientsSearchFeatureSimpleEntityParameters selection) {

        featureService.scheduleNotificationsJob(selection);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    /**
     * Creates job to send deletion notification for each feature matching given search parameters
     */
    @Operation(summary = "Delete features according to search parameters",
               description = "Delete features according to search parameters")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "Delete features according to search parameters") })
    @DeleteMapping(path = DELETE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Delete features according to search parameters", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> deleteFeatures(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of search criteria.",
                                                              content = @Content(schema = @Schema(implementation = SearchFeatureSimpleEntityParameters.class)))
        @Parameter(description = "Filter criteria for features") @RequestBody
        SearchFeatureSimpleEntityParameters selection) {
        featureService.scheduleDeletionJob(selection);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }

    @Override
    public EntityModel<FeatureEntityDto> toResource(FeatureEntityDto element, Object... extras) {
        EntityModel<FeatureEntityDto> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "getFeature",
                                LinkRels.SELF,
                                MethodParamFactory.build(String.class, element.getUrn().toString()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "searchFeatures",
                                LinkRels.LIST,
                                MethodParamFactory.build(SearchFeatureSimpleEntityParameters.class),
                                MethodParamFactory.build(Pageable.class),
                                MethodParamFactory.build(PagedResourcesAssembler.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "notifyFeatures",
                                LinkRelation.of("notify"),
                                MethodParamFactory.build(RecipientsSearchFeatureSimpleEntityParameters.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "deleteFeatures",
                                LinkRelation.of("delete"),
                                MethodParamFactory.build(SearchFeatureSimpleEntityParameters.class));
        return resource;
    }
}
