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
package fr.cnes.regards.modules.access.services.rest.user;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.client.IRegistrationClient;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Endpoints to handle Users registration for a project.
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard

 */
@RestController
@RequestMapping(RegistrationController.REQUEST_MAPPING_ROOT)
public class RegistrationController {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String REQUEST_MAPPING_ROOT = "/accesses";

    /**
     * Relative path to the endpoint to request an access wit external account
     */
    public static final String EXTERNAL_ACCESS_PATH = "/external";

    /**
     * Relative path to the endpoint accepting accesses (project users)
     */
    public static final String ACCEPT_ACCESS_RELATIVE_PATH = "/{access_id}/accept";

    /**
     * Relative path to the endpoint denying accesses (project users)
     */
    public static final String DENY_ACCESS_RELATIVE_PATH = "/{access_id}/deny";

    /**
     * Relative path to the endpoint for verifying the email
     */
    protected static final String VERIFY_EMAIL_RELATIVE_PATH = "/verifyEmail/{token}";

    /**
     * Relative path to the endpoint activating accesses (project users)
     */
    public static final String ACTIVE_ACCESS_RELATIVE_PATH = "/{access_id}/active";

    /**
     * Relative path to the endpoint deactivating accesses (project users)
     */
    public static final String INACTIVE_ACCESS_RELATIVE_PATH = "/{access_id}/inactive";

    @Autowired
    private IRegistrationClient registrationClient;

    /**
     * Request a new access, i.e. a new project user
     * @param accessRequestDto A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Request for a new projectUser (Public feature).", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestAccess(@Valid @RequestBody final AccessRequestDto accessRequestDto) {
        ResponseEntity<EntityModel<AccessRequestDto>> response =
            registrationClient.requestAccess(accessRequestDto);
        return response.getStatusCode().is2xxSuccessful()
        ? new ResponseEntity<>(HttpStatus.CREATED)
        : new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Request a new access, i.e. a new project user with external authentication system.
     * @param accessRequestDto A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     */
    @ResponseBody
    @RequestMapping(value = EXTERNAL_ACCESS_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "Request for a new projectUser (Public feature).", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> requestExternalAccess(@Valid @RequestBody final AccessRequestDto accessRequestDto) {
        ResponseEntity<EntityModel<AccessRequestDto>> response =
            registrationClient.requestAccess(accessRequestDto);
        return response.getStatusCode().is2xxSuccessful()
            ? new ResponseEntity<>(HttpStatus.CREATED)
            : new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Confirm the registration by email.
     * @param token the token
     * @return void
     */
    @RequestMapping(value = VERIFY_EMAIL_RELATIVE_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "Confirm the registration by email", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> verifyEmail(@PathVariable("token") final String token) {
        return registrationClient.verifyEmail(token);
    }

    /**
     * Grants access to the project user
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(value = ACCEPT_ACCESS_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Accepts the access request", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> acceptAccessRequest(@PathVariable("access_id") final Long accessId) {
        return registrationClient.acceptAccessRequest(accessId);
    }

    /**
     * Denies access to the project user
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = DENY_ACCESS_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Denies the access request", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> denyAccessRequest(@PathVariable("access_id") final Long accessId) {
        return registrationClient.denyAccessRequest(accessId);
    }

    /**
     * Activates an inactive user
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = ACTIVE_ACCESS_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Activates an inactive user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> activeAccess(@PathVariable("access_id") final Long accessId) {
        return registrationClient.activeAccess(accessId);
    }

    /**
     * Deactivates an active user
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = INACTIVE_ACCESS_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Desactivates an active user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> inactiveAccess(@PathVariable("access_id") final Long accessId) {
        return registrationClient.inactiveAccess(accessId);
    }

    /**
     * Rejects the access request
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{access_id}", method = RequestMethod.DELETE)
    @ResourceAccess(description = "Rejects the access request", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> removeAccessRequest(@PathVariable("access_id") final Long accessId) {
        return registrationClient.removeAccessRequest(accessId);
    }

}
