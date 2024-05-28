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
package fr.cnes.regards.modules.search.rest;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.search.dto.availability.FilesAvailabilityRequestDto;
import fr.cnes.regards.modules.search.dto.availability.FilesAvailabilityResponseDto;
import fr.cnes.regards.modules.search.dto.availability.ProductFilesStatusDto;
import fr.cnes.regards.modules.search.service.availability.FileAvailabilityException;
import fr.cnes.regards.modules.search.service.availability.FileAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to get availability of all files of product(s).
 *
 * @author Thomas GUILLOU
 **/
@RestController
@RequestMapping(FileAvailabilityController.AVAILABILITY_ROOT_PATH)
public class FileAvailabilityController {

    public static final String AVAILABILITY_ROOT_PATH = "/availability";

    public static final String AVAILABILITY_OF_PRODUCTS_PATH = "/files";

    public static final String AVAILABILITY_OF_PRODUCT_PATH = "/files/{productId}";

    private final FileAvailabilityService fileAvailabilityService;

    public FileAvailabilityController(FileAvailabilityService fileAvailabilityService) {
        this.fileAvailabilityService = fileAvailabilityService;
    }

    @PostMapping(AVAILABILITY_OF_PRODUCTS_PATH)
    @Operation(summary = "Get file availability of all files of all input products",
               description = "Return a list of availability status of all files of all input products."
                             + " List of all files must be less than a configured number of elements."
                             + " Products not found will be excluded of the response.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Availability status well calculated"),
                            @ApiResponse(responseCode = "403", description = "Forbidden access to endpoint"),
                            @ApiResponse(responseCode = "400", description = "Too much products or files in input") })
    @ResourceAccess(description = "Get file availability of all files of all input products",
                    role = DefaultRole.EXPLOIT)
    public ResponseEntity<FilesAvailabilityResponseDto> filesAvailability(
        @RequestBody @Valid FilesAvailabilityRequestDto filesAvailabilityRequestDto) throws ModuleException {
        try {
            return new ResponseEntity<>(fileAvailabilityService.checkAvailability(filesAvailabilityRequestDto.getProductIds()),
                                        HttpStatus.OK);
        } catch (FileAvailabilityException e) { // NOSONAR
            throw e.convertToResponseStatusException();
        }
    }

    @GetMapping(AVAILABILITY_OF_PRODUCT_PATH)
    @Operation(summary = "Get file availability of all files of input product",
               description = "Return a list of availability status of all files of the input product."
                             + " If the product is not found, the result will be empty.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Availability status well calculated"),
                            @ApiResponse(responseCode = "403",
                                         description = "Forbidden access to files, product or endpoint"),
                            @ApiResponse(responseCode = "404", description = "Input product not found") })
    @ResourceAccess(description = "Get file availability of all files of input product", role = DefaultRole.EXPLOIT)
    public ResponseEntity<ProductFilesStatusDto> fileAvailability(@PathVariable String productId)
        throws ModuleException {
        try {
            return new ResponseEntity<>(fileAvailabilityService.checkAvailability(productId), HttpStatus.OK);
        } catch (FileAvailabilityException e) { // NOSONAR
            throw e.convertToResponseStatusException();
        }
    }
}
