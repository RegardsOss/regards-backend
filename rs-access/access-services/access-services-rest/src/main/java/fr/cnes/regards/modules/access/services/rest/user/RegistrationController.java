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
package fr.cnes.regards.modules.access.services.rest.user;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.client.IRegistrationClient;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
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

    public static final String REQUEST_MAPPING_ROOT = "/accesses";

    public static final String EXTERNAL_ACCESS_PATH = "/external";
    public static final String ACCEPT_ACCESS_RELATIVE_PATH = "/{access_id}/accept";
    public static final String DENY_ACCESS_RELATIVE_PATH = "/{access_id}/deny";
    protected static final String VERIFY_EMAIL_RELATIVE_PATH = "/verifyEmail/{token}";
    public static final String ACTIVE_ACCESS_RELATIVE_PATH = "/{access_id}/active";
    public static final String INACTIVE_ACCESS_RELATIVE_PATH = "/{access_id}/inactive";

    private final IRegistrationClient registrationClient;

    public RegistrationController(IRegistrationClient registrationClient) {
        this.registrationClient = registrationClient;
    }

    /**
     * Request a new access, i.e. a new project user
     *
     * @param accessRequestDto A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     */
    @PostMapping
    @ResourceAccess(description = "Request for a new projectUser (Public feature).", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestAccess(@Valid @RequestBody AccessRequestDto accessRequestDto) {
        ResponseEntity<EntityModel<AccessRequestDto>> response = registrationClient.requestAccess(accessRequestDto);
        HttpStatus status = response.getStatusCode().is2xxSuccessful() ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).build();
    }

    /**
     * Request a new access, i.e. a new project user with external authentication system.
     *
     * @param accessRequestDto A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     */
    @PostMapping(EXTERNAL_ACCESS_PATH)
    @ResourceAccess(description = "Request for a new projectUser (Public feature).", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> requestExternalAccess(@Valid @RequestBody AccessRequestDto accessRequestDto) {
        ResponseEntity<EntityModel<AccessRequestDto>> response = registrationClient.requestExternalAccess(accessRequestDto);
        HttpStatus status = response.getStatusCode().is2xxSuccessful() ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).build();
    }

    /**
     * Confirm the registration by email.
     *
     * @param token the token
     * @return void
     */
    @GetMapping(VERIFY_EMAIL_RELATIVE_PATH)
    @ResourceAccess(description = "Confirm the registration by email", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> verifyEmail(@PathVariable("token") String token) {
        return registrationClient.verifyEmail(token);
    }

    /**
     * Grants access to the project user
     *
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @PutMapping(ACCEPT_ACCESS_RELATIVE_PATH)
    @ResourceAccess(description = "Accepts the access request", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> acceptAccessRequest(@PathVariable("access_id") Long accessId) {
        return registrationClient.acceptAccessRequest(accessId);
    }

    /**
     * Denies access to the project user
     *
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @PutMapping(DENY_ACCESS_RELATIVE_PATH)
    @ResourceAccess(description = "Denies the access request", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> denyAccessRequest(@PathVariable("access_id") Long accessId) {
        return registrationClient.denyAccessRequest(accessId);
    }

    /**
     * Activates an inactive user
     *
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @PutMapping(ACTIVE_ACCESS_RELATIVE_PATH)
    @ResourceAccess(description = "Activates an inactive user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> activeAccess(@PathVariable("access_id") Long accessId) {
        return registrationClient.activeAccess(accessId);
    }

    /**
     * Deactivates an active user
     *
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @PutMapping(INACTIVE_ACCESS_RELATIVE_PATH)
    @ResourceAccess(description = "Deactivates an active user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> inactiveAccess(@PathVariable("access_id") Long accessId) {
        return registrationClient.inactiveAccess(accessId);
    }

    /**
     * Rejects the access request
     *
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @DeleteMapping("/{access_id}")
    @ResourceAccess(description = "Rejects the access request", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> removeAccessRequest(@PathVariable("access_id") Long accessId) {
        return registrationClient.removeAccessRequest(accessId);
    }

}
