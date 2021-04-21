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
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 *
 * Feign client for rs-admin Accesses Rest controller. Thanks to Feign facilities, no exception handling is required.
 *
 * @author CS

 */
@RestClient(name = "rs-admin", contextId = "rs-admin.registration-client")
@RequestMapping(value = "/accesses", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public interface IRegistrationClient {

    /**
     * Retrieve all access requests.
     *
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<List<EntityModel<ProjectUser>>> retrieveAccessRequestList(@RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    /**
     * Request a new access, i.e. a new project user
     *
     * @param pAccessRequest
     *            A Dto containing all information for creating a the new project user
     * @return the passed Dto
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<EntityModel<AccessRequestDto>> requestAccess(@Valid @RequestBody AccessRequestDto pAccessRequest);

    /**
     * Request a new access, i.e. a new project user with external authentication system.
     *
     * @param pAccessRequest
     *            A Dto containing all information for creating a the new project user
     * @return the passed Dto
     */
    @ResponseBody
    @RequestMapping(value = "/external", method = RequestMethod.POST)
    ResponseEntity<EntityModel<AccessRequestDto>> requestExternalAccess(
            @Valid @RequestBody AccessRequestDto pAccessRequest);

    /**
     * Confirm the registration by email.
     * @param token the token
     * @return void
     */
    @RequestMapping(value = "/verifyEmail/{token}", method = RequestMethod.GET)
    public ResponseEntity<Void> verifyEmail(@PathVariable("token") final String token);

    /**
     * Grants access to the project user
     *
     * @param pAccessId
     *            the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(value = "/{access_id}/accept", method = RequestMethod.PUT)
    ResponseEntity<Void> acceptAccessRequest(@PathVariable("access_id") Long pAccessId);

    /**
     * Denies access to the project user
     *
     * @param pAccessId
     *            the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{access_id}/deny", method = RequestMethod.PUT)
    ResponseEntity<Void> denyAccessRequest(@PathVariable("access_id") Long pAccessId);

    /**
     * Activates an inactive user
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{access_id}/active", method = RequestMethod.PUT)
    ResponseEntity<Void> activeAccess(@PathVariable("access_id") final Long accessId);

    /**
     * Deactivates an active user
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{access_id}/inactive", method = RequestMethod.PUT)
    public ResponseEntity<Void> inactiveAccess(@PathVariable("access_id") final Long accessId);

    /**
     * Rejects the access request
     *
     * @param pAccessId
     *            the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{access_id}", method = RequestMethod.DELETE)
    ResponseEntity<Void> removeAccessRequest(@PathVariable("access_id") Long pAccessId);

}