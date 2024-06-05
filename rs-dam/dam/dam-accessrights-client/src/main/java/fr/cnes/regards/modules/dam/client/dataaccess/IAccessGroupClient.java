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
package fr.cnes.regards.modules.dam.client.dataaccess;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@RestClient(name = "rs-dam", contextId = "rs-dam.access-group.client")
public interface IAccessGroupClient {

    String ROOT_PATH_ACCESS_GROUPS = "/accessgroups";

    String PATH_ACCESS_GROUPS_NAME = "/{name}";

    /**
     * Retrieve access group
     *
     * @param isPublic whether we are also looking for public groups
     * @param page     which page
     * @param size     which page size
     * @return a page of access group
     */
    @GetMapping(path = ROOT_PATH_ACCESS_GROUPS,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<EntityModel<AccessGroup>>> retrieveAccessGroupsList(
        @RequestParam(name = "public", required = false) Boolean isPublic,
        @RequestParam("page") int page,
        @RequestParam("size") int size);

    @PostMapping(path = ROOT_PATH_ACCESS_GROUPS,
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<AccessGroup>> createAccessGroup(@Valid @RequestBody AccessGroup toBeCreated);

    /**
     * Retrieve an access group by its name
     *
     * @return the retrieved access group
     */
    @GetMapping(path = ROOT_PATH_ACCESS_GROUPS + PATH_ACCESS_GROUPS_NAME,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<AccessGroup>> retrieveAccessGroup(@Valid @PathVariable("name") String groupName);

    /**
     * Delete an access group by its name
     */
    @DeleteMapping(path = ROOT_PATH_ACCESS_GROUPS + PATH_ACCESS_GROUPS_NAME,
                   consumes = MediaType.APPLICATION_JSON_VALUE,
                   produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> deleteAccessGroup(@Valid @PathVariable("name") String groupName);

}
