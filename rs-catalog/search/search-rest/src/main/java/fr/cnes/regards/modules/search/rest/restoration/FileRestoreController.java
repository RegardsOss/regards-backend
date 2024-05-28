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
package fr.cnes.regards.modules.search.rest.restoration;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.search.dto.restoration.FilesRestoreRequestDto;
import fr.cnes.regards.modules.search.service.restoration.FileRestoreException;
import fr.cnes.regards.modules.search.service.restoration.FileRestoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller to ask the restoration of all files of product(s).
 *
 * @author Stephane Cortine
 */
@RestController
@RequestMapping(FileRestoreController.ROOT_PATH)
public class FileRestoreController {

    public static final String ROOT_PATH = "/restore";

    public static final String PRODUCT_ID_RELATIVE_PATH = "/{product_id}";

    private FileRestoreService fileRestoreService;

    public FileRestoreController(FileRestoreService fileRestoreService) {
        this.fileRestoreService = fileRestoreService;
    }

    @Operation(summary = "Ask the restoration of all files of products.",
               description = "Ask the restoration of all files of products thanks to product identifiers (URN)."
                             + " List of all files must be less than a configured number of elements."
                             + " Products not found will be excluded of the response.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "The restoration is being realized"),
                            @ApiResponse(responseCode = "403", description = "Forbidden access to endpoint"),
                            @ApiResponse(responseCode = "400", description = "Too much products or files in input") })
    @PostMapping
    @ResourceAccess(description = "Endpoint to ask the restoration of all files of products.",
                    role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> restoreProducts(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Submission request dto to be processed.",
                                                              content = @Content(schema = @Schema(implementation = FilesRestoreRequestDto.class)))
        @RequestBody @Valid FilesRestoreRequestDto filesRestoreRequestDto) throws ModuleException {
        try {
            fileRestoreService.restore(filesRestoreRequestDto.getProductIds());
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (FileRestoreException e) { // NOSONAR
            throw e.convertToResponseStatusException();
        }
    }

    @Operation(summary = "Ask the restoration of all files of one product.",
               description = "Ask the restoration of all files of one product thanks to its product identifier (URN).")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "The restoration is being realized"),
                            @ApiResponse(responseCode = "403",
                                         description = "Forbidden access to files, product or endpoint"),
                            @ApiResponse(responseCode = "404", description = "Input product not found") })
    @GetMapping(PRODUCT_ID_RELATIVE_PATH)
    @ResourceAccess(description = "Endpoint to ask the restoration of all files of one product.",
                    role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> restoreProduct(@PathVariable("product_id") String productId) throws ModuleException {
        try {
            fileRestoreService.restore(productId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (FileRestoreException e) { // NOSONAR
            throw e.convertToResponseStatusException();
        }
    }

}
