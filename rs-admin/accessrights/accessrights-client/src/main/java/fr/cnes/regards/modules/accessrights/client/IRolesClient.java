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
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

/**
 * Class IRolesClient
 * <p>
 * Feign client for rs-admin Roles controller.
 *
 * @author CS
 */
@RestClient(name = "rs-admin", contextId = "rs-admin.roles-client")
public interface IRolesClient { // NOSONAR

    /**
     * Root mapping for requests of this rest controller
     */
    String ROOT_TYPE_MAPPING = "/roles";

    /**
     * Mapping for managing a role mapping for requests of this rest controller
     */
    String ROLE_MAPPING = "/{role_name}";

    String ROLE_DESCENDANTS = ROLE_MAPPING + "/descendants";

    String SHOULD_ACCESS_TO_RESOURCE = "/include" + ROLE_MAPPING;

    /**
     * Mapping for retrieving borrowable role of the current user
     */
    String BORROWABLE_MAPPING = "/borrowables";

    /**
     * Mapping for retrieving all roles that can access the specified resource
     */
    String ROLE_WITH_RESOURCE_MAPPING = "/resources/{resourceId}";

    /**
     * Define the endpoint for retrieving the list of all roles.
     *
     * @return A {@link List} of roles as {@link Role} wrapped in an {@link ResponseEntity}
     */
    @GetMapping(path = ROOT_TYPE_MAPPING, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EntityModel<Role>>> getAllRoles();

    /**
     * Define the endpoint for retrieving the list of borrowable Roles for the current user.
     *
     * @return list of borrowable roles for current authenticated user
     */
    @GetMapping(path = ROOT_TYPE_MAPPING + BORROWABLE_MAPPING, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EntityModel<Role>>> getBorrowableRoles();

    /**
     * Define the endpoint for retrieving the list of roles that can access the specified resource.
     *
     * @param pResourceId
     * @return list of borrowable roles for current authenticated user
     */
    @GetMapping(path = ROOT_TYPE_MAPPING + ROLE_WITH_RESOURCE_MAPPING, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<EntityModel<Role>>> getRolesAccesingResource(
        @PathVariable("resourceId") final Long pResourceId);

    /**
     * Define the endpoint for creating a new {@link Role}.
     *
     * @param pNewRole The new {@link Role} values
     * @return The created {@link Role}
     */
    @PostMapping(value = ROOT_TYPE_MAPPING, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<Role>> createRole(@RequestBody final Role pNewRole);

    /**
     * Define the endpoint for retrieving the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleName The {@link Role}'s <code>name</code>
     * @return The {@link Role} wrapped in an {@link ResponseEntity}
     */
    @GetMapping(path = ROOT_TYPE_MAPPING + ROLE_MAPPING, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<Role>> retrieveRole(@PathVariable("role_name") final String pRoleName);

    /**
     * Define the endpoint for retrieving the descendant {@link Role}s of passed role through its name
     *
     * @return the ascendants wrapped into a {@link ResponseEntity}
     */
    @GetMapping(path = ROOT_TYPE_MAPPING + ROLE_DESCENDANTS, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Set<Role>> retrieveRoleDescendants(@PathVariable("role_name") String roleName);

    /**
     * Define the endpoint for updating the {@link Role} of id <code>pRoleId</code>.
     *
     * @param pRoleName    The {@link Role}
     * @param pUpdatedRole The new {@link Role}
     * @return Updated {@link Role}
     */
    @PutMapping(path = ROOT_TYPE_MAPPING + ROLE_MAPPING, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<Role>> updateRole(@PathVariable("role_name") final String pRoleName,
                                                 @Valid @RequestBody final Role pUpdatedRole);

    /**
     * Define the endpoint for deleting the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleName The {@link Role}'s <code>name</code>
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     */
    @DeleteMapping(path = ROOT_TYPE_MAPPING + ROLE_MAPPING, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> removeRole(@PathVariable("role_name") final String pRoleName);

    /**
     * Define the endpoint to determine if the provided ${@link Role} is inferior to the one brought by the current request
     *
     * @param roleName that should be inferior
     * @return true when the current role should have access to something requiring at least the provided role
     * @throws EntityNotFoundException if some role does not exists
     */
    @GetMapping(path = ROOT_TYPE_MAPPING + SHOULD_ACCESS_TO_RESOURCE, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Boolean> shouldAccessToResourceRequiring(@PathVariable("role_name") String roleName)
        throws EntityNotFoundException;
}