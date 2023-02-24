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
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserSearchParameters;
import fr.cnes.regards.modules.accessrights.domain.projects.SearchProjectUserParameters;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import static fr.cnes.regards.modules.accessrights.client.IProjectUsersClient.TARGET_NAME;

/**
 * Class IProjectUsersClient
 * <p>
 * Feign client for rs-admin ProjectUsers controller.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
@RestClient(name = TARGET_NAME, contextId = "rs-admin.project-user-client")
public interface IProjectUsersClient {

    String ROOT_PATH = "/users";

    String SEARCH_PATH = "/search";

    String TARGET_NAME = "rs-admin";

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     *
     * @param parameters search parameters as request params
     * @return a {@link List} of {@link ProjectUser}
     */
    @PostMapping(value = ROOT_PATH + SEARCH_PATH,
                 produces = MediaType.APPLICATION_JSON_VALUE,
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveProjectUserList(
        @RequestBody ProjectUserSearchParameters parameters, @SpringQueryMap Pageable pageable);

    @PostMapping(value = ROOT_PATH + SEARCH_PATH,
                 produces = MediaType.APPLICATION_JSON_VALUE,
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveProjectUserList(
        @RequestBody SearchProjectUserParameters filters, @SpringQueryMap Pageable pageable);

    /**
     * Retrieve all users with a pending access request.
     *
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @GetMapping(value = ROOT_PATH + "/pendingaccesses",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveAccessRequestList(@SpringQueryMap Pageable pageable);

    @PostMapping(value = ROOT_PATH,
                 produces = MediaType.APPLICATION_JSON_VALUE,
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<ProjectUser>> createUser(@Valid @RequestBody final AccessRequestDto pDto);

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId The {@link ProjectUser}'s <code>id</code>
     * @return {@link ProjectUser}
     */
    @GetMapping(value = ROOT_PATH + "/{user_id}",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUser(@PathVariable("user_id") Long pUserId);

    /**
     * Retrieve the {@link ProjectUser} of passed <code>email</code>.
     *
     * @param pUserEmail The {@link ProjectUser}'s <code>email</code>
     * @return {@link ProjectUser}
     */
    @GetMapping(value = ROOT_PATH + "/email/{user_email}",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUserByEmail(@PathVariable("user_email") String pUserEmail);

    @GetMapping(value = ROOT_PATH + "/email/{user_email}/admin",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Boolean> isAdmin(@PathVariable("user_email") String userEmail);

    /**
     * Update the {@link ProjectUser} of id <code>pUserId</code>.
     *
     * @param pUserId             The {@link ProjectUser} <code>id</code>
     * @param pUpdatedProjectUser The new {@link ProjectUser}
     * @return {@link ProjectUser}
     */
    @PutMapping(value = ROOT_PATH + "/{user_id}",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<ProjectUser>> updateProjectUser(@PathVariable("user_id") Long pUserId,
                                                               @RequestBody ProjectUser pUpdatedProjectUser);

    /**
     * Delete the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId The {@link ProjectUser}'s <code>id</code>
     * @return void
     */
    @DeleteMapping(value = ROOT_PATH + "/{user_id}",
                   produces = MediaType.APPLICATION_JSON_VALUE,
                   consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> removeProjectUser(@PathVariable("user_id") Long pUserId);

    /**
     * Retrieve the {@link ProjectUser} of current authenticated user
     *
     * @return a {@link ProjectUser}
     */
    @GetMapping(value = ROOT_PATH + "/myuser",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<ProjectUser>> retrieveCurrentProjectUser();

    /**
     * Update the {@link ProjectUser} of current projet user authenticated.
     *
     * @param updatedProjectUser The new {@link ProjectUser}
     * @return a {@link ProjectUser}
     */
    @PutMapping(value = ROOT_PATH + "/myuser",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<EntityModel<ProjectUser>> updateCurrentProjectUser(@RequestBody ProjectUser updatedProjectUser);

    /**
     * retrieveRoleProjectUserList
     *
     * @param pRoleId role identifier to retrieve users.
     * @return {@link PagedModel} of {@link ProjectUser}
     */
    @GetMapping(value = ROOT_PATH + "/roles/{role_id}",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUserList(
        @PathVariable("role_id") final Long pRoleId, @SpringQueryMap Pageable pageable);

    /**
     * Retrieve pages of project user which role, represented by its name, is the one provided
     *
     * @param pRole role name
     * @return page of project user which role, represented by its name, is the one provided
     */
    @GetMapping(value = ROOT_PATH + "/roles",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUsersList(
        @RequestParam("role_name") String pRole, @SpringQueryMap Pageable pageable);

    @PostMapping(value = ROOT_PATH + "/email/{email}/groups",
                 produces = MediaType.APPLICATION_JSON_VALUE,
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> linkAccessGroups(@PathVariable("email") String email, @RequestBody List<String> groups);

    @PutMapping(value = ROOT_PATH + "/email/{email}/origin/{origin}",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> updateOrigin(@PathVariable("email") String email, @PathVariable("origin") String origin);

    @GetMapping(value = ROOT_PATH + "/email/{email}/verification/resend",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> sendVerificationEmail(@PathVariable("email") String email);

}
