/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;

/**
 *
 * Tenant connection client
 *
 * @author Marc Sordi
 *
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = "/connections/{microservice}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ITenantConnectionClient {

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<TenantConnection> addTenantConnection(@PathVariable("microservice") String microservice,
            @Valid @RequestBody TenantConnection tenantConnection);

    @RequestMapping(method = RequestMethod.PUT, value = "/{tenant}/enable")
    public ResponseEntity<TenantConnection> enableTenantConnection(@PathVariable("microservice") String microservice,
            @PathVariable("tenant") String tenant);

    @RequestMapping(method = RequestMethod.PUT, value = "/{tenant}/disable")
    public ResponseEntity<TenantConnection> disableTenantConnection(@PathVariable("microservice") String microservice,
            @PathVariable("tenant") String tenant);

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<TenantConnection>> getTenantConnections(
            @PathVariable("microservice") String microservice);

}
