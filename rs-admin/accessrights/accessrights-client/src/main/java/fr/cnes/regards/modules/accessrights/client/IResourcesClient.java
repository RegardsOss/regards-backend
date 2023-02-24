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
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestClient(name = "rs-admin", contextId = "rs-admin.resources-client")
public interface IResourcesClient {

    /**
     * Root mapping for requests of this rest controller
     */
    String ROOT_TYPE_MAPPING = "/resources";

    /**
     * Single resource mapping
     */
    String RESOURCE_MAPPING = "/{resource_id}";

    /**
     * Retrieve resource accesses available to the user
     *
     * @param pPageable                pagination information
     * @param pPagedResourcesAssembler page assembler
     * @return list of user resource accesses
     */
    @GetMapping(value = ROOT_TYPE_MAPPING,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<EntityModel<ResourcesAccess>>> getAllResourceAccesses(@RequestParam("page") int pPage,
                                                                                    @RequestParam("size") int pSize);

    /**
     * Retrieve the ResourceAccess with given id {@link Long} exists.
     *
     * @param pResourceId resource id
     * @return {@link ResourcesAccess}
     */
    @GetMapping(value = ROOT_TYPE_MAPPING + RESOURCE_MAPPING,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<ResourcesAccess>> getResourceAccess(@PathVariable("resource_id") final Long pResourceId);

    /**
     * Update given resource access informations
     *
     * @param pResourceId             Resource access identifier
     * @param pResourceAccessToUpdate Resource access to update
     * @return updated ResourcesAccess
     */
    @PutMapping(value = ROOT_TYPE_MAPPING + RESOURCE_MAPPING,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<ResourcesAccess>> updateResourceAccess(
        @PathVariable("resource_id") final Long pResourceId,
        @Valid @RequestBody final ResourcesAccess pResourceAccessToUpdate);

}
