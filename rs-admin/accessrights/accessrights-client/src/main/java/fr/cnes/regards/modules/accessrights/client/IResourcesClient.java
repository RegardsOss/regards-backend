/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.validation.Valid;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

@RestClient(name = "rs-admin", contextId = "rs-admin.resources-client")
@RequestMapping(value = IResourcesClient.TYPE_MAPPING, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IResourcesClient {

    /**
     * Root mapping for requests of this rest controller
     */
    String TYPE_MAPPING = "/resources";

    /**
     * Single resource mapping
     */
    String RESOURCE_MAPPING = "/{resource_id}";

    /**
     * Retrieve resource accesses available to the user
     *
     * @param pPageable
     *            pagination information
     * @param pPagedResourcesAssembler
     *            page assembler
     * @return list of user resource accesses
     */
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<PagedResources<Resource<ResourcesAccess>>> getAllResourceAccesses(@RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    /**
     *
     * Retrieve the ResourceAccess with given id {@link Long} exists.
     *
     * @param pResourceId
     *            resource id
     * @return {@link ResourcesAccess}
     */
    @RequestMapping(method = RequestMethod.GET, value = RESOURCE_MAPPING)
    ResponseEntity<Resource<ResourcesAccess>> getResourceAccess(@PathVariable("resource_id") final Long pResourceId);

    /**
     *
     * Update given resource access informations
     *
     * @param pResourceId
     *            Resource access identifier
     * @param pResourceAccessToUpdate
     *            Resource access to update
     * @return updated ResourcesAccess
     */
    @RequestMapping(method = RequestMethod.PUT, value = RESOURCE_MAPPING)
    ResponseEntity<Resource<ResourcesAccess>> updateResourceAccess(@PathVariable("resource_id") final Long pResourceId,
            @Valid @RequestBody final ResourcesAccess pResourceAccessToUpdate);

}
