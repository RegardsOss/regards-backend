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
package fr.cnes.regards.modules.project.client.rest;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

/**
 * Tenant connection <b>SYSTEM</b> client
 *
 * @author Marc Sordi
 */
@RestClient(name = "rs-admin-instance", contextId = "rs-admin-instance.tenant-connection-client")
public interface ITenantConnectionClient {

    String ROOT_PATH = "/connections/{microservice}";

    /**
     * Allows the system to register a tenant connection
     *
     * @param microservice     target microservice
     * @param tenantConnection connection to register
     * @return registered connection
     */
    @PostMapping(path = ROOT_PATH,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TenantConnection> addTenantConnection(@PathVariable("microservice") String microservice,
                                                         @Valid @RequestBody TenantConnection tenantConnection);

    /**
     * Allows the system to update connection state. Only tenant, state and errorCause are useful.
     *
     * @param microservice     target microservice
     * @param tenantConnection connection to update
     * @return updated connection
     */
    @PutMapping(path = ROOT_PATH,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TenantConnection> updateState(@PathVariable("microservice") String microservice,
                                                 @Valid @RequestBody TenantConnection tenantConnection);

    /**
     * Retrieve all tenant connections
     *
     * @param microservice target microservice
     * @return list of connections
     */
    @GetMapping(path = ROOT_PATH,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<TenantConnection>> getTenantConnections(@PathVariable("microservice") String microservice);

}
