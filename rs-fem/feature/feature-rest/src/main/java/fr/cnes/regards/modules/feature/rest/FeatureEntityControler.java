/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.dam.domain.entities.feature.DataObjectFeature;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;
import fr.cnes.regards.modules.feature.dto.FeaturesSelectionDTO;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.feature.service.IDataObjectFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * End point to get {@link DataObjectFeature} contain data of the last created/modified {@link FeatureEntity}
 * @author Kevin Marchois
 *
 */
@RestController
@RequestMapping(FeatureEntityControler.PATH_DATA_FEATURE_OBJECT)
public class FeatureEntityControler implements IResourceController<FeatureEntityDto> {

    public static final String PATH_DATA_FEATURE_OBJECT = "/admin/features";

    @Autowired
    private IDataObjectFeatureService dataObjectFeature;

    /**
     * {@link IResourceService} instance
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Get a {@link Page} of {@link FeatureEntityDto} it will contain data of the last created {@link FeatureEntity}
     * @param model model of wanted {@link Feature}
     * @param lastUpdateDate las modification date that we want {@link Feature}
     * @return {@link RequestInfo} a {@link Page} of {@link FeatureEntityDto}
     */
    @Operation(summary = "Get features according last update date and model",
            description = "Get features according last update date and model")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get features according last update date and model") })
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Get features according last update date")
    public ResponseEntity<PagedModel<EntityModel<FeatureEntityDto>>> getFeatures(
            @Parameter(description = "Features selection filters") FeaturesSelectionDTO selection, Pageable page,
            PagedResourcesAssembler<FeatureEntityDto> assembler) {
        return new ResponseEntity<>(toPagedResources(dataObjectFeature.findAll(selection, page), assembler),
                HttpStatus.OK);
    }

    @Override
    public EntityModel<FeatureEntityDto> toResource(FeatureEntityDto element, Object... extras) {
        EntityModel<FeatureEntityDto> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "getFeatures", LinkRels.SELF,
                                MethodParamFactory.build(FeaturesSelectionDTO.class),
                                MethodParamFactory.build(Pageable.class),
                                MethodParamFactory.build(PagedResourcesAssembler.class));
        return resource;
    }
}
