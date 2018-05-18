/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
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
 *
 * @since 1.0-SNAPSHOT
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = "/users", produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
        consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface IProjectUsersClient {

    /**
     * Retrieve the {@link List} of all {@link ProjectUser}s.
     *
     * @return
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveProjectUserList(@RequestParam("page") int pPage,
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
    ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveAccessRequestList(@RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<Resource<ProjectUser>> createUser(@Valid @RequestBody final AccessRequestDto pDto);

    /**
     * Retrieve the {@link ProjectUser} of passed <code>id</code>.
     *
     * @param pUserId
     *            The {@link ProjectUser}'s <code>id</code>
     * @return {@link PagedResources} of {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}", method = RequestMethod.GET)
    ResponseEntity<Resource<ProjectUser>> retrieveProjectUser(@PathVariable("user_id") Long pUserId);

    /**
     * Retrieve the {@link ProjectUser} of passed <code>email</code>.
     *
     * @param pUserEmail
     *            The {@link ProjectUser}'s <code>email</code>
     * @return {@link PagedResources} of {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(value = "/email/{user_email}", method = RequestMethod.GET)
    ResponseEntity<Resource<ProjectUser>> retrieveProjectUserByEmail(@PathVariable("user_email") String pUserEmail);

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
     * @return {@link PagedResources} of {@link ProjectUser}
     */
    @ResponseBody
    @RequestMapping(value = "/{user_id}", method = RequestMethod.PUT)
    ResponseEntity<Resource<ProjectUser>> updateProjectUser(@PathVariable("user_id") Long pUserId,
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
     * retrieveRoleProjectUserList
     *
     * @param pRoleId
     *            role identifier to retrieve users.
     * @param pPage
     *            page index
     * @param pSize
     *            page size
     * @return {@link PagedResources} of {@link ProjectUser}
     * @since 1.0-SNAPSHOT
     */
    @ResponseBody
    @RequestMapping(value = "/roles/{role_id}", method = RequestMethod.GET)
    public ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveRoleProjectUserList(
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
    ResponseEntity<PagedResources<Resource<ProjectUser>>> retrieveRoleProjectUsersList(@RequestParam("role_name") String pRole, @RequestParam("page") int pPage,
            @RequestParam("size") int pSize);
}
