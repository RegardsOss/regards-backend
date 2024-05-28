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
package fr.cnes.regards.modules.accessrights.client;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

/**
 * Role resource management API client
 *
 * @author Marc Sordi
 */
@RestClient(name = "rs-admin", contextId = "rs-admin.role-resource-client")
public interface IRoleResourceClient {

    /**
     * Controller base mapping
     */
    String ROOT_TYPE_MAPPING = "/roles/{role_name}/resources";

    @GetMapping(value = ROOT_TYPE_MAPPING,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EntityModel<ResourcesAccess>>> getRoleResources(
        @PathVariable("role_name") final String roleName);

    @GetMapping(value = ROOT_TYPE_MAPPING + "/{microservice}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EntityModel<ResourcesAccess>>> getRoleResourcesForMicroservice(
        @PathVariable("role_name") final String roleName, @PathVariable("microservice") final String microserviceName);

    @PostMapping(value = ROOT_TYPE_MAPPING,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<ResourcesAccess>> addRoleResource(@PathVariable("role_name") final String roleName,
                                                                 @RequestBody @Valid
                                                                 final ResourcesAccess newResourcesAccess);

    @DeleteMapping(value = ROOT_TYPE_MAPPING + "/{resources_access_id}",
                   consumes = MediaType.APPLICATION_JSON_VALUE,
                   produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> deleteRoleResource(@PathVariable("role_name") final String roleName,
                                            @PathVariable("resources_access_id") final Long resourcesAccessId);

}
