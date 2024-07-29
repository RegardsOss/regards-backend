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
package fr.cnes.regards.modules.accessrights.rest;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.service.projectuser.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.projectuser.workflow.state.ProjectUserWorkflowManager;
import fr.cnes.regards.modules.accessrights.service.registration.IRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints to handle Users registration for a project.
 *
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

    private static final String VERIFY_EMAIL_RELATIVE_PATH = "/verifyEmail/{token}";

    public static final String ACTIVE_ACCESS_RELATIVE_PATH = "/{access_id}/active";

    public static final String INACTIVE_ACCESS_RELATIVE_PATH = "/{access_id}/inactive";

    private final IProjectUserService projectUserService;

    private final ProjectUserWorkflowManager projectUserWorkflowManager;

    private final IRegistrationService registrationService;

    private final IEmailVerificationTokenService emailVerificationTokenService;

    public RegistrationController(IProjectUserService projectUserService,
                                  ProjectUserWorkflowManager projectUserWorkflowManager,
                                  IRegistrationService registrationService,
                                  IEmailVerificationTokenService emailVerificationTokenService) {
        this.projectUserService = projectUserService;
        this.projectUserWorkflowManager = projectUserWorkflowManager;
        this.registrationService = registrationService;
        this.emailVerificationTokenService = emailVerificationTokenService;
    }

    /**
     * Request a new access, i.e. a new project user
     *
     * @param accessRequestDto A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     * @throws EntityException if error occurs.
     */
    @PostMapping
    @ResourceAccess(description = "Request for a new projectUser (Public feature).", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestAccess(@Valid @RequestBody AccessRequestDto accessRequestDto)
        throws EntityException {
        // FIXME use a different dto. We use a new dto here to remove the role parameter. 
        // Users should not be able to specify the role of the new user to create. The role of user is managed by project administrators.
        AccessRequestDto mangledDto = new AccessRequestDto(accessRequestDto.getEmail(),
                                                           accessRequestDto.getFirstName(),
                                                           accessRequestDto.getLastName(),
                                                           null,
                                                           accessRequestDto.getMetadata(),
                                                           accessRequestDto.getPassword(),
                                                           accessRequestDto.getOriginUrl(),
                                                           accessRequestDto.getRequestLink(),
                                                           accessRequestDto.getOrigin(),
                                                           accessRequestDto.getAccessGroups(),
                                                           null);
        registrationService.requestAccess(mangledDto, false);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Request a new access, i.e. a new project user with external authentication system.
     *
     * @param accessRequestDto A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     * @throws EntityException if error occurs.
     */
    @PostMapping(EXTERNAL_ACCESS_PATH)
    @ResourceAccess(description = "Request for a new projectUser (Public feature).", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> requestExternalAccess(@Valid @RequestBody AccessRequestDto accessRequestDto)
        throws EntityException {
        registrationService.requestAccess(accessRequestDto, true);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Confirm the registration by email.
     *
     * @param token the token
     * @return void
     * @throws EntityException when no verification token associated to this account could be found
     */
    @GetMapping(VERIFY_EMAIL_RELATIVE_PATH)
    @ResourceAccess(description = "Confirm the registration by email", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> verifyEmail(@PathVariable("token") String token) throws EntityException {
        EmailVerificationToken emailVerificationToken = emailVerificationTokenService.findByToken(token);
        projectUserWorkflowManager.verifyEmail(emailVerificationToken);
        return ResponseEntity.ok().build();
    }

    /**
     * Grants access to the project user
     *
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *                         {@link EntityNotFoundException} if project user is in illegal status for denial<br>
     */
    @PutMapping(ACCEPT_ACCESS_RELATIVE_PATH)
    @ResourceAccess(description = "Accepts the access request", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> acceptAccessRequest(@PathVariable("access_id") Long accessId) throws EntityException {
        final ProjectUser projectUser = projectUserService.retrieveUser(accessId);
        projectUserWorkflowManager.grantAccess(projectUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Denies access to the project user
     *
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *                         {@link EntityNotFoundException} if project user is in illegal status for denial<br>
     */
    @PutMapping(DENY_ACCESS_RELATIVE_PATH)
    @ResourceAccess(description = "Denies the access request", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> denyAccessRequest(@PathVariable("access_id") Long accessId) throws EntityException {
        ProjectUser projectUser = projectUserService.retrieveUser(accessId);
        projectUserWorkflowManager.denyAccess(projectUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Activates an inactive user
     *
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *                         {@link EntityNotFoundException} if project user is in illegal status for activation<br>
     */
    @PutMapping(ACTIVE_ACCESS_RELATIVE_PATH)
    @ResourceAccess(description = "Activates an inactive user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> activeAccess(@PathVariable("access_id") Long accessId) throws EntityException {
        ProjectUser projectUser = projectUserService.retrieveUser(accessId);
        projectUserWorkflowManager.activeAccess(projectUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Deactivates an active user
     *
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *                         {@link EntityNotFoundException} if project user is in illegal status for deactivation<br>
     */
    @PutMapping(INACTIVE_ACCESS_RELATIVE_PATH)
    @ResourceAccess(description = "Deactivates an active user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> inactiveAccess(@PathVariable("access_id") Long accessId) throws EntityException {
        ProjectUser projectUser = projectUserService.retrieveUser(accessId);
        projectUserWorkflowManager.inactiveAccess(projectUser);
        return ResponseEntity.ok().build();
    }

    /**
     * Rejects the access request
     *
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException if error occurs!
     */
    @DeleteMapping("/{access_id}")
    @ResourceAccess(description = "Rejects the access request", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> removeAccessRequest(@PathVariable("access_id") Long accessId) throws EntityException {
        ProjectUser projectUser = projectUserService.retrieveUser(accessId);
        projectUserWorkflowManager.removeAccess(projectUser);
        return ResponseEntity.ok().build();
    }

}
