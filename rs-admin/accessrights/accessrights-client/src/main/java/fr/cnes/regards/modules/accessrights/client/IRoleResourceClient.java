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
package fr.cnes.regards.modules.accessrights.client;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 * Role resource management API client
 *
 * @author Marc Sordi
 *
 */
@RestClient(name = "rs-administration", contextId = "rs-administration.role-resource-client")
@RequestMapping(value = IRoleResourceClient.TYPE_MAPPING, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public interface IRoleResourceClient {

    /**
     * Controller base mapping
     */
    String TYPE_MAPPING = "/roles/{role_name}/resources";

    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<EntityModel<ResourcesAccess>>> getRoleResources(
            @PathVariable("role_name") final String pRoleName);

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<EntityModel<ResourcesAccess>> addRoleResource(@PathVariable("role_name") final String pRoleName,
            @RequestBody @Valid final ResourcesAccess pNewResourcesAccess);

    @RequestMapping(method = RequestMethod.DELETE, value = "/{resources_access_id}")
    ResponseEntity<Void> deleteRoleResource(@PathVariable("role_name") final String pRoleName,
            @PathVariable("resources_access_id") final Long pResourcesAccessId);

}
