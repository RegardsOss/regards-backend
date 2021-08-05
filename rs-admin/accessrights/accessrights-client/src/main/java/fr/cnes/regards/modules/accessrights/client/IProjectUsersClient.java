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

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUserSearchParameters;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
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
 *
 * Feign client for rs-admin ProjectUsers controller.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
@RestClient(name = TARGET_NAME, contextId = "rs-admin.project-user-client")
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
public interface IProjectUsersClient {

    String TARGET_NAME = "rs-admin";

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     *
     * @param page       page index
     * @param size       page size
     * @param parameters search parameters as request params
     * @return a {@link List} of {@link ProjectUser}
     */
    @GetMapping
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveProjectUserList(
            @RequestParam ProjectUserSearchParameters parameters, @RequestParam("page") int page, @RequestParam("size") int size
    );

    /**
     * Retrieve all users with a pending access request.
     *
     * @param page page index
     * @param size page size
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @GetMapping("/pendingaccesses")
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveAccessRequestList(@RequestParam("page") int page, @RequestParam("size") int size);

    @PostMapping
    ResponseEntity<EntityModel<ProjectUser>> createUser(@Valid @RequestBody final AccessRequestDto pDto);

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId The {@link ProjectUser}'s <code>id</code>
     * @return {@link ProjectUser}
     */
    @GetMapping("/{user_id}")
    ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUser(@PathVariable("user_id") Long pUserId);

    /**
     * Retrieve the {@link ProjectUser} of passed <code>email</code>.
     *
     * @param pUserEmail The {@link ProjectUser}'s <code>email</code>
     * @return {@link ProjectUser}
     */
    @GetMapping("/email/{user_email}")
    ResponseEntity<EntityModel<ProjectUser>> retrieveProjectUserByEmail(@PathVariable("user_email") String pUserEmail);

    @GetMapping("/email/{user_email}/admin")
    ResponseEntity<Boolean> isAdmin(@PathVariable("user_email") String userEmail);

    /**
     * Update the {@link ProjectUser} of id <code>pUserId</code>.
     *
     * @param pUserId             The {@link ProjectUser} <code>id</code>
     * @param pUpdatedProjectUser The new {@link ProjectUser}
     * @return{@link ProjectUser}
     */
    @PutMapping("/{user_id}")
    ResponseEntity<EntityModel<ProjectUser>> updateProjectUser(@PathVariable("user_id") Long pUserId, @RequestBody ProjectUser pUpdatedProjectUser);

    /**
     * Delete the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId The {@link ProjectUser}'s <code>id</code>
     * @return void
     */
    @DeleteMapping("/{user_id}")
    ResponseEntity<Void> removeProjectUser(@PathVariable("user_id") Long pUserId);

    /**
     * Retrieve the {@link ProjectUser} of current authenticated user
     * @return a {@link ProjectUser}
     */
    @GetMapping("/myuser")
    ResponseEntity<EntityModel<ProjectUser>> retrieveCurrentProjectUser();

    /**
     * Update the {@link ProjectUser} of current projet user authenticated.
     *
     * @param updatedProjectUser The new {@link ProjectUser}
     * @return a {@link ProjectUser}
     */
    @PutMapping("/myuser")
    ResponseEntity<EntityModel<ProjectUser>> updateCurrentProjectUser(@RequestBody ProjectUser updatedProjectUser);

    /**
     * retrieveRoleProjectUserList
     *
     * @param pRoleId role identifier to retrieve users.
     * @param page    page index
     * @param size    page size
     * @return {@link PagedModel} of {@link ProjectUser}
     */
    @GetMapping("/roles/{role_id}")
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUserList(
            @PathVariable("role_id") final Long pRoleId, @RequestParam("page") int page, @RequestParam("size") int size
    );

    /**
     * Retrieve pages of project user which role, represented by its name, is the one provided
     *
     * @param pRole role name
     * @param page  page index
     * @param size  page size
     * @return page of project user which role, represented by its name, is the one provided
     */
    @GetMapping("/roles")
    ResponseEntity<PagedModel<EntityModel<ProjectUser>>> retrieveRoleProjectUsersList(
            @RequestParam("role_name") String pRole, @RequestParam("page") int page, @RequestParam("size") int size
    );

    @PostMapping("/email/{email}/groups")
    ResponseEntity<Void> linkAccessGroups(@PathVariable("email") String email, @RequestBody List<String> groups);

}
