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
package fr.cnes.regards.modules.accessrights.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
    private static final String VERIFY_EMAIL_RELATIVE_PATH = "/verifyEmail/{token}";

    /**
     * Relative path to the endpoint activating accesses (project users)
     */
    public static final String ACTIVE_ACCESS_RELATIVE_PATH = "/{access_id}/active";

    /**
     * Relative path to the endpoint deactivating accesses (project users)
     */
    public static final String INACTIVE_ACCESS_RELATIVE_PATH = "/{access_id}/inactive";

    /**
     * Service handling CRUD operation on access requests. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IProjectUserService projectUserService;

    /**
     * Workflow manager of project users. Autowired by Spring. Must not be <code>null</code>.
     */
    @Autowired
    private ProjectUserWorkflowManager projectUserWorkflowManager;

    /**
     * {@link IRegistrationService} instance
     */
    @Autowired
    private IRegistrationService registrationService;

    /**
     * Service handling CRUD operation on {@link EmailVerificationToken}s. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IEmailVerificationTokenService emailVerificationTokenService;

    /**
     * Request a new access, i.e. a new project user
     * @param accessRequestDto A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     * @throws EntityException if error occurs.
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Request for a new projectUser (Public feature).", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestAccess(@Valid @RequestBody final AccessRequestDto accessRequestDto)
            throws EntityException {
        ProjectUser user = registrationService.requestAccess(accessRequestDto, false);
        projectUserService.configureAccessGroups(user);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Request a new access, i.e. a new project user with external authentication system.
     * @param accessRequestDto A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     * @throws EntityException if error occurs.
     */
    @ResponseBody
    @RequestMapping(value = EXTERNAL_ACCESS_PATH, method = RequestMethod.POST)
    @ResourceAccess(description = "Request for a new projectUser (Public feature).", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> requestExternalAccess(@Valid @RequestBody final AccessRequestDto accessRequestDto)
            throws EntityException {
        ProjectUser projectUser = registrationService.requestAccess(accessRequestDto, true);
        projectUserService.configureAccessGroups(projectUser);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Confirm the registration by email.
     * @param token the token
     * @return void
     * @throws EntityException when no verification token associated to this account could be found
     */
    @RequestMapping(value = VERIFY_EMAIL_RELATIVE_PATH, method = RequestMethod.GET)
    @ResourceAccess(description = "Confirm the registration by email", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> verifyEmail(@PathVariable("token") final String token) throws EntityException {
        final EmailVerificationToken emailVerificationToken = emailVerificationTokenService.findByToken(token);
        projectUserWorkflowManager.verifyEmail(emailVerificationToken);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Grants access to the project user
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *                         {@link EntityNotFoundException} if project user is in illegal status for denial<br>
     */
    @RequestMapping(value = ACCEPT_ACCESS_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Accepts the access request", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> acceptAccessRequest(@PathVariable("access_id") final Long accessId)
            throws EntityException {
        final ProjectUser projectUser = projectUserService.retrieveUser(accessId);
        projectUserWorkflowManager.grantAccess(projectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Denies access to the project user
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *                         {@link EntityNotFoundException} if project user is in illegal status for denial<br>
     */
    @ResponseBody
    @RequestMapping(value = DENY_ACCESS_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Denies the access request", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> denyAccessRequest(@PathVariable("access_id") final Long accessId)
            throws EntityException {
        final ProjectUser projectUser = projectUserService.retrieveUser(accessId);
        projectUserWorkflowManager.denyAccess(projectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Activates an inactive user
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *                         {@link EntityNotFoundException} if project user is in illegal status for activation<br>
     */
    @ResponseBody
    @RequestMapping(value = ACTIVE_ACCESS_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Activates an inactive user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> activeAccess(@PathVariable("access_id") final Long accessId) throws EntityException {
        final ProjectUser projectUser = projectUserService.retrieveUser(accessId);
        projectUserWorkflowManager.activeAccess(projectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Deactivates an active user
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *                         {@link EntityNotFoundException} if project user is in illegal status for deactivation<br>
     */
    @ResponseBody
    @RequestMapping(value = INACTIVE_ACCESS_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Desactivates an active user", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> inactiveAccess(@PathVariable("access_id") final Long accessId) throws EntityException {
        final ProjectUser projectUser = projectUserService.retrieveUser(accessId);
        projectUserWorkflowManager.inactiveAccess(projectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Rejects the access request
     * @param accessId the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException if error occurs!
     */
    @ResponseBody
    @RequestMapping(value = "/{access_id}", method = RequestMethod.DELETE)
    @ResourceAccess(description = "Rejects the access request", role = DefaultRole.EXPLOIT)
    public ResponseEntity<Void> removeAccessRequest(@PathVariable("access_id") final Long accessId)
            throws EntityException {
        final ProjectUser projectUser = projectUserService.retrieveUser(accessId);
        projectUserWorkflowManager.removeAccess(projectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
