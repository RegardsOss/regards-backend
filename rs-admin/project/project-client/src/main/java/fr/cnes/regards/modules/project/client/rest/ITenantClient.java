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
package fr.cnes.regards.modules.project.client.rest;

import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.annotation.RestClient;

/**
 *
 * Tenant client
 *
 * @author Marc Sordi
 *
 */
@RestClient(name = "rs-admin-instance", contextId = "rs-admin-instance.tenant-client")
@RequestMapping(value = "/tenants", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public interface ITenantClient {

    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<Set<String>> getAllTenants();

    @RequestMapping(method = RequestMethod.GET, value = "/{pMicroserviceName}")
    ResponseEntity<Set<String>> getAllActiveTenants(@PathVariable("pMicroserviceName") String pMicroserviceName);
}
