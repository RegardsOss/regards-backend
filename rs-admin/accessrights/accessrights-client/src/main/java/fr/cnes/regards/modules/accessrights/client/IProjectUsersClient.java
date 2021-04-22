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

import static fr.cnes.regards.modules.accessrights.client.IProjectUsersClient.TARGET_NAME;

import java.util.List;

import javax.validation.Valid;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;

/**
 *
 * Class IProjectUsersClient
 *
 * Feign client for rs-admin ProjectUsers controller.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
@RestClient(name = TARGET_NAME, contextId = "rs-admin.project-user-client")
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
public interface IProjectUsersClient {

    String TARGET_NAME = "rs-admin";

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveProjectUserList(@RequestParam(value = "status", required = false) String pStatus,
                                                                                 @RequestParam(value = "partialEmail", required = false) String pEmailStart,
                                                                                 @RequestParam("page") int pPage,
                                                                                 @RequestParam("size") int pSize);

    /**
     * Retrieve all users with a pending access requests.
     *
     * @param pPage
     * @param pSize
     *
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @ResponseBody
    @RequestMapping(value = "/pendingaccesses", method = RequestMethod.GET)
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveAccessRequestList(@RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<EntityModel<ProjectUser>> createUser(@Valid @RequestBody final AccessRequestDto pDto);

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}", method = RequestMethod.GET)
    ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUser(@PathVariable("user_id") Long pUserId);

    /**
     * Retrieve the {@link ProjectUser} of passed <code>email</code>.
     *
     * @param pUserEmail
     *            The {@link ProjectUser}'s <code>email</code>
     * @return {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(value = "/email/{user_email}", method = RequestMethod.GET)
    ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUserByEmail(@PathVariable("user_email") String pUserEmail);

    @ResponseBody
    @RequestMapping(value = "/email/{user_email}/admin", method = RequestMethod.GET)
    ResponseEntity<Boolean> isAdmin(@PathVariable("user_email") String userEmail);

    /**
     * Update the {@link ProjectUser} of id <code>pUserId</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser} <code>id</code>
     * @param pUpdatedProjectUser
     *            The new {@link ProjectUser}
     * @return{@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}", method = RequestMethod.PUT)
    ResponseEntity<EntityModel<ProjectUser>> updateProjectUser(@PathVariable("user_id") Long pUserId,
            @RequestBody ProjectUser pUpdatedProjectUser);

    /**
     * Delete the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return void
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}", method = RequestMethod.DELETE)
    ResponseEntity<Void> removeProjectUser(@PathVariable("user_id") Long pUserId);

    /**
     * Retrieve the {@link ProjectUser} of current authenticated user
     * @return a {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(value = "/myuser", method = RequestMethod.GET)
    ResponseEntity<EntityModel<ProjectUser>> retrieveCurrentProjectUser();

    /**
     * Update the {@link ProjectUser} of current projet user authenticated.
     * @param updatedProjectUser The new {@link ProjectUser}
     * @return a {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(value = "/myuser", method = RequestMethod.PUT)
    ResponseEntity<EntityModel<ProjectUser>> updateCurrentProjectUser(
        @RequestBody ProjectUser updatedProjectUser);

    /**
     * retrieveRoleProjectUserList
     *
     * @param pRoleId
     *            role identifier to retrieve users.
     * @param pPage
     *            page index
     * @param pSize
     *            page size
     * @return {@link PagedModel} of {@link ProjectUser}
    
     */
    @ResponseBody
    @RequestMapping(value = "/roles/{role_id}", method = RequestMethod.GET)
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUserList(
            @PathVariable("role_id") final Long pRoleId, @RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    /**
     * Retrieve pages of project user which role, represented by its name, is the one provided
     *
     * @param pRole role name
     * @param pPage
     * @param pSize
     * @return page of project user which role, represented by its name, is the one provided
     */
    @RequestMapping(value = "/roles", method = RequestMethod.GET)
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUsersList(
            @RequestParam("role_name") String pRole, @RequestParam("page") int pPage, @RequestParam("size") int pSize);
}
