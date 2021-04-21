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
package fr.cnes.regards.modules.project.rest;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.project.service.ITenantService;

/**
 * Specific controller to retrieve tenants from other microservices
 * @author Marc Sordi
 */
@RestController
@RequestMapping(TenantController.BASE_PATH)
public class TenantController {

    /**
     * Main path of this controller endpoints
     */
    public static final String BASE_PATH = "/tenants";

    /**
     * Additional path for microservice
     */
    public static final String MICROSERVICE_PATH = "/{microserviceName}";

    /**
     * Administration project service
     */
    @Autowired
    private ITenantService tenantService;

    @ResourceAccess(description = "List all tenants", role = DefaultRole.INSTANCE_ADMIN)
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Set<String>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    @ResourceAccess(description = "List all fully configured tenants", role = DefaultRole.INSTANCE_ADMIN)
    @RequestMapping(method = RequestMethod.GET, value = MICROSERVICE_PATH)
    public ResponseEntity<Set<String>> getAllActiveTenants(@PathVariable("microserviceName") String microserviceName) {
        return ResponseEntity.ok(tenantService.getAllActiveTenants(microserviceName));
    }
}
