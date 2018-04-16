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
package fr.cnes.regards.modules.accessrights.client;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 *
 * Class IRolesClient
 *
 * Feign client for rs-admin Roles controller.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = IRolesClient.TYPE_MAPPING, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IRolesClient { // NOSONAR

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String TYPE_MAPPING = "/roles";

    /**
     * Mapping for managing a role mapping for requests of this rest controller
     */
    public static final String ROLE_MAPPING = "/{role_name}";

    public static final String ROLE_DESCENDANTS = ROLE_MAPPING + "/descendants";

    /**
     * Mapping for retrieving borrowable role of the current user
     */
    public static final String BORROWABLE_MAPPING = "/borrowables";

    /**
     * Mapping for retrieving all roles that can access the specified resource
     */
    public static final String ROLE_WITH_RESOURCE_MAPPING = "/resources/{resourceId}";

    /**
     * Define the endpoint for retrieving the list of all roles.
     *
     * @return A {@link List} of roles as {@link Role} wrapped in an {@link ResponseEntity}
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Resource<Role>>> getAllRoles();

    /**
     * Define the endpoint for retrieving the list of borrowable Roles for the current user.
     *
     * @return list of borrowable roles for current authenticated user
     */
    @RequestMapping(method = RequestMethod.GET, path = BORROWABLE_MAPPING)
    public ResponseEntity<List<Resource<Role>>> getBorrowableRoles();

    /**
     * Define the endpoint for retrieving the list of roles that can access the specified resource.
     *
     * @param pResourceId
     * @return list of borrowable roles for current authenticated user
     */
    @RequestMapping(method = RequestMethod.GET, path = ROLE_WITH_RESOURCE_MAPPING)
    public ResponseEntity<List<Resource<Role>>> getRolesAccesingResource(
            @PathVariable("resourceId") final Long pResourceId);

    /**
     * Define the endpoint for creating a new {@link Role}.
     *
     * @param pNewRole
     *            The new {@link Role} values
     * @return The created {@link Role}
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Resource<Role>> createRole(@RequestBody final Role pNewRole);

    /**
     * Define the endpoint for retrieving the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleName
     *            The {@link Role}'s <code>name</code>
     * @return The {@link Role} wrapped in an {@link ResponseEntity}
     */
    @RequestMapping(method = RequestMethod.GET, value = ROLE_MAPPING)
    public ResponseEntity<Resource<Role>> retrieveRole(@PathVariable("role_name") final String pRoleName);

    /**
     * Define the endpoint for retrieving the descendant {@link Role}s of passed role through its name
     * @return the ascendants wrapped into a {@link ResponseEntity}
     */
    @RequestMapping(method = RequestMethod.GET, path = ROLE_DESCENDANTS)
    public ResponseEntity<Set<Role>> retrieveRoleDescendants(@PathVariable("role_name") String roleName);

    /**
     * Define the endpoint for updating the {@link Role} of id <code>pRoleId</code>.
     *
     * @param pRoleName
     *            The {@link Role}
     * @param pUpdatedRole
     *            The new {@link Role}
     * @return Updated {@link Role}
     */
    @RequestMapping(method = RequestMethod.PUT, value = ROLE_MAPPING)
    public ResponseEntity<Resource<Role>> updateRole(@PathVariable("role_name") final String pRoleName,
            @Valid @RequestBody final Role pUpdatedRole);

    /**
     * Define the endpoint for deleting the {@link Role} of passed <code>id</code>.
     *
     * @param pRoleName
     *            The {@link Role}'s <code>name</code>
     * @return {@link Void} wrapped in an {@link ResponseEntity}
     */
    @RequestMapping(method = RequestMethod.DELETE, value = ROLE_MAPPING)
    public ResponseEntity<Void> removeRole(@PathVariable("role_name") final String pRoleName);

}