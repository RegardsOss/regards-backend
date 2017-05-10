/*
 * LICENSE_PLACEHOLDER
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

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken;
import fr.cnes.regards.modules.accessrights.registration.IRegistrationService;
import fr.cnes.regards.modules.accessrights.registration.IVerificationTokenService;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;
import fr.cnes.regards.modules.accessrights.workflow.account.AccountWorkflowManager;
import fr.cnes.regards.modules.accessrights.workflow.projectuser.AccessQualification;
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
@RequestMapping(RegistrationController.REQUEST_MAPPING_ROOT)
public class RegistrationController {

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String REQUEST_MAPPING_ROOT = "/accesses";

    /**
     * Relative path to the endpoint accepting accounts
     */
    public static final String ACCEPT_ACCOUNT_RELATIVE_PATH = "/acceptAccount/{account_email}";

    /**
     * Relative path to the endpoint accepting accesses (project users)
     */
    public static final String ACCEPT_ACCESS_RELATIVE_PATH = "/{access_id}/accept";

    /**
     * Service handling CRUD operation on accounts. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IAccountService accountService;

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
     * Service handling CRUD operation on {@link AccountSettings}. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IRegistrationService registrationService;

    /**
     * Service handling CRUD operation on {@link VerificationToken}s. Autowired by Spring. Must no be <code>null</code>.
     */
    @Autowired
    private IVerificationTokenService verificationTokenService;

    /**
     * Request a new access, i.e. a new project user
     *
     * @param pDto
     *            A Dto containing all information for creating the account/project user and sending the activation link
     * @return the passed Dto
     * @throws EntityException
     *             if error occurs.
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "Request for a new projectUser (Public feature).", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestAccess(@Valid @RequestBody final AccessRequestDto pDto) throws EntityException {
        registrationService.requestAccess(pDto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Grants access to the project user
     *
     * @param pAccountEmail
     *            account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException
     *             <br>
     *             {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *             {@link EntityNotFoundException} if project user is in illegal status for denial<br>
     */
    @RequestMapping(value = ACCEPT_ACCOUNT_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Accepts the access request", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> acceptAccount(@PathVariable("account_email") final String pAccountEmail)
            throws EntityException {
        // Retrieve the account
        final Account account = accountService.retrieveAccountByEmail(pAccountEmail);

        // Accept it
        accountWorkflowManager.acceptAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
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
        final VerificationToken verificationToken = verificationTokenService.findByToken(pToken);
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
    @RequestMapping(value = ACCEPT_ACCESS_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Accepts the access request", role = DefaultRole.PROJECT_ADMIN)
    public ResponseEntity<Void> acceptAccessRequest(@PathVariable("access_id") final Long pAccessId)
            throws EntityException {
        final ProjectUser projectUser = projectUserService.retrieveUser(pAccessId);
        if (UserStatus.WAITING_ACCESS.equals(projectUser.getStatus())) {
            projectUserWorkflowManager.qualifyAccess(projectUser, AccessQualification.GRANTED);
        } else {
            projectUserWorkflowManager.grantAccess(projectUser);
        }

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
    @RequestMapping(value = DENY_ACCESS_RELATIVE_PATH, method = RequestMethod.PUT)
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
     * @throws EntityException
     *             if error occurs!
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

}
