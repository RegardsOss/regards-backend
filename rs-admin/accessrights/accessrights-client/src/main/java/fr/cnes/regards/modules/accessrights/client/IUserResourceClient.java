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
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * User resource management API client
 *
 * @author Marc Sordi
 */
@RestClient(name = "rs-admin", contextId = "rs-admin.user-resource-client")
public interface IUserResourceClient {

    /**
     * Controller base mapping
     */
    String ROOT_TYPE_MAPPING = "/users/{user_email}/resources";

    /**
     * Retrieve the {@link List} of {@link ResourcesAccess} for the account of passed <code>email</code>.
     *
     * @param pUserLogin        The account <code>email</code>
     * @param pBorrowedRoleName The borrowed {@link Role} <code>name</code> if the user is connected with a borrowed role. Optional.
     * @return the {@link List} list of resources access
     */
    @GetMapping(path = ROOT_TYPE_MAPPING,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EntityModel<ResourcesAccess>>> retrieveProjectUserResources(
        @PathVariable("user_email") final String pUserLogin,
        @RequestParam(value = "borrowedRoleName", required = false) final String pBorrowedRoleName);

    /**
     * Update the the {@link List} of <code>permissions</code>.
     *
     * @param pLogin                   The {@link ProjectUser}'s <code>login</code>
     * @param pUpdatedUserAccessRights The {@link List} of {@link ResourcesAccess} to set
     * @return void
     */
    @PutMapping(path = ROOT_TYPE_MAPPING,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Update the list of specific user accesses", role = DefaultRole.PROJECT_ADMIN)
    ResponseEntity<Void> updateProjectUserResources(@PathVariable("user_email") final String pLogin,
                                                    @Valid @RequestBody
                                                    final List<ResourcesAccess> pUpdatedUserAccessRights);

    /**
     * Remove all specific user accesses
     *
     * @param pUserLogin user email
     * @return {@link Void}
     */
    @DeleteMapping(path = ROOT_TYPE_MAPPING,
                   consumes = MediaType.APPLICATION_JSON_VALUE,
                   produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Remove all specific user accesses", role = DefaultRole.PROJECT_ADMIN)
    ResponseEntity<Void> removeProjectUserResources(@PathVariable("user_email") final String pUserLogin);
}
