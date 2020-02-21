package com.example.prout;
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

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Controler de mes fesses
 * @author Marc Sordi
 */
@RestController
@RequestMapping("/hello")
public class HelloController {

    /**
     * Un end point qui dit bonjour mais qui est bien inutile
     * @param inutile une param inutile
     * @return un Hello qui vient du coeur
     */
    @GetMapping
    @Operation(summary = "un end point qui dit bonjour mais qui est bien inutile",
            description = "un end point qui dit bonjour mais qui est bien inutile")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "un Hello qui vient du coeur") })
    public ResponseEntity<String> sayHello(@Parameter(description = "un param inutile") @RequestParam(value = "inutile",
            required = false) String inutile) {
        return ResponseEntity.ok("Hello!");
    }
}
