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
package fr.cnes.regards.modules.search.rest;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.search.domain.SearchValidationMappings;
import fr.cnes.regards.modules.search.dto.GeometryRequestParameter;
import fr.cnes.regards.modules.search.service.IBusinessSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller to validate search request parameters
 *
 * @author Sébastien Binda
 **/
@RestController
@RequestMapping(path = SearchValidationMappings.TYPE_MAPPING)
public class SearchValidationController {

    private final IBusinessSearchService businessSearchService;

    public SearchValidationController(IBusinessSearchService businessSearchService) {
        this.businessSearchService = businessSearchService;
    }

    @Operation(summary = "Validate Geometry search parameter",
               description = "Validate that the given WKT Geometry is valid.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "The given geometry is valid."),
                            @ApiResponse(responseCode = "422", description = "The given geometry is not valid."),
                            @ApiResponse(responseCode = "403",
                                         description = "The endpoint is not accessible for the user.",
                                         content = { @Content(mediaType = "application/html") }) })
    @PostMapping(path = SearchValidationMappings.GEO_VALIDATION_MAPPING)
    @ResponseBody
    @ResourceAccess(description = "Validate that the given WKT Geometry is valid", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> isValidGeometry(@RequestBody GeometryRequestParameter geometryRequestParameter) {
        if (businessSearchService.isValidGeometry(geometryRequestParameter.getWktString())) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.unprocessableEntity().build();
    }
}
