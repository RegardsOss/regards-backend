/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;

/**
 * Class ResourcesController
 *
 * Common Resources RestController. This Controller manage the endpoint to retrieve all Resources of a microservice.
 * @author CS
 */
@RestController
@RequestMapping("/security")
public class SecurityResourcesController {

    /**
     * Authorization service
     */
    private final MethodAuthorizationService service;

    /**
     * Constructor
     * @param pService MethodeAutorizationService autowired by spring
     */
    public SecurityResourcesController(final MethodAuthorizationService pService) {
        service = pService;
    }

    /**
     * Retrieve all enpoints annoted with @ResourceAccess
     * @return List<ResourceMapping>
     */
    @RequestMapping(value = "resources", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<ResourceMapping>> getAllResources() {
        return new ResponseEntity<>(service.getResources(), HttpStatus.OK);
    }
}
