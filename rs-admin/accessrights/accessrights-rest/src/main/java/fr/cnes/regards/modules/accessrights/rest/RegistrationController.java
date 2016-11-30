/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken;
import fr.cnes.regards.modules.accessrights.registration.AppUrlBuilder;
import fr.cnes.regards.modules.accessrights.registration.IRegistrationService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IAccessSettingsService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.workflow.account.AccountWorkflowManager;
import fr.cnes.regards.modules.accessrights.workflow.projectuser.ProjectUserWorkflowManager;

/**
 * Endpoints to handle Users registration for a project.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@RestController
@ModuleInfo(name = "registration", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(value = "/accesses")
public class RegistrationController {

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
     * Workflow manager of account. Autowired by Spring. Must not be <code>null</code>.
     */
    @Autowired
    private AccountWorkflowManager accountWorkflowManager;

    /**
     * Service handling CRUD operation on {@link AccessSettings}. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IAccessSettingsService accessSettingsService;

    /**
     * Service handling CRUD operation on {@link AccountSettings}. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IRegistrationService registrationService;

    /**
     * Retrieve all access requests.
     *
     * @return The {@link List} of all {@link ProjectUser}s with status {@link UserStatus#WAITING_ACCESS}
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieves the list of access request", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<List<Resource<ProjectUser>>> retrieveAccessRequestList() {
        final List<ProjectUser> projectUsers = projectUserService.retrieveAccessRequestList();
        return new ResponseEntity<>(HateoasUtils.wrapList(projectUsers), HttpStatus.OK);
    }

    /**
     * Request a new access, i.e. a new project user
     *
     * @param pAccessRequest
     *            A Dto containing all information for creating a the new project user
     * @param pRequest
     *            the request
     * @return the passed Dto
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Creates a new access request", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<Resource<AccessRequestDto>> requestAccess(
            @Valid @RequestBody final AccessRequestDto pAccessRequest, final HttpServletRequest pRequest)
            throws EntityException {
        // Build the email validation link
        final String validationUrl = AppUrlBuilder.buildFrom(pRequest);
        // Create an account if needed
        accountWorkflowManager.requestAccount(pAccessRequest, validationUrl);
        // Create a project user
        projectUserWorkflowManager.requestProjectAccess(pAccessRequest);
        final Resource<AccessRequestDto> resource = new Resource<>(pAccessRequest);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    /**
     * Confirm the registration by email.
     *
     * @param pToken
     *            the token
     * @return void
     * @throws EntityException
     *             when no verification token associated to this account could be found
     */
    @RequestMapping(value = "/validateAccount/{token}", method = RequestMethod.GET)
    @ResourceAccess(description = "Confirm the registration by email", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> validateAccount(@PathVariable("token") final String pToken) throws EntityException {
        final VerificationToken verificationToken = registrationService.getVerificationToken(pToken);
        accountWorkflowManager.validateAccount(verificationToken);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Grants access to the project user
     *
     * @param pAccessId
     *            the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException
     *             <br>
     *             {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *             {@link EntityNotFoundException} if project user is in illegal status for denial<br>
     */
    @RequestMapping(value = "/{access_id}/accept", method = RequestMethod.PUT)
    @ResourceAccess(description = "Accepts the access request", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> acceptAccessRequest(@PathVariable("access_id") final Long pAccessId)
            throws EntityException {
        final ProjectUser projectUser = projectUserService.retrieveUser(pAccessId);
        projectUserWorkflowManager.grantAccess(projectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Denies access to the project user
     *
     * @param pAccessId
     *            the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException
     *             <br>
     *             {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *             {@link EntityNotFoundException} if project user is in illegal status for denial<br>
     */
    @ResponseBody
    @RequestMapping(value = "/{access_id}/deny", method = RequestMethod.PUT)
    @ResourceAccess(description = "Denies the access request", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> denyAccessRequest(@PathVariable("access_id") final Long pAccessId)
            throws EntityException {
        final ProjectUser projectUser = projectUserService.retrieveUser(pAccessId);
        projectUserWorkflowManager.denyAccess(projectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Rejects the access request
     *
     * @param pAccessId
     *            the project user id
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/{access_id}", method = RequestMethod.DELETE)
    @ResourceAccess(description = "Rejects the access request", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> removeAccessRequest(@PathVariable("access_id") final Long pAccessId)
            throws EntityException {
        final ProjectUser projectUser = projectUserService.retrieveUser(pAccessId);
        projectUserWorkflowManager.removeAccess(projectUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Retrieve the {@link AccountSettings}.
     *
     * @return The {@link AccountSettings}
     */
    @ResponseBody
    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    @ResourceAccess(description = "Retrieves the settings managing the access requests",
            role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Resource<AccessSettings>> getAccessSettings() {
        final AccessSettings accessSettings = accessSettingsService.retrieve();
        final Resource<AccessSettings> resource = new Resource<>(accessSettings);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Update the {@link AccountSettings}.
     *
     * @param pAccessSettings
     *            The {@link AccountSettings}
     * @return The updated access settings
     */
    @ResponseBody
    @RequestMapping(value = "/settings", method = RequestMethod.PUT)
    @ResourceAccess(description = "Updates the setting managing the access requests", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> updateAccessSettings(@Valid @RequestBody final AccessSettings pAccessSettings)
            throws EntityNotFoundException {
        accessSettingsService.update(pAccessSettings);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
