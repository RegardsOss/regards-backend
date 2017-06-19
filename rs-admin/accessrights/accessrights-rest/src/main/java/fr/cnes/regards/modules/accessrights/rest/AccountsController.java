/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.rest.utils.Validity;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.accountunlock.PerformUnlockAccountDto;
import fr.cnes.regards.modules.accessrights.domain.accountunlock.RequestAccountUnlockDto;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.passwordreset.PerformResetPasswordDto;
import fr.cnes.regards.modules.accessrights.domain.passwordreset.RequestResetPasswordDto;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.account.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.service.account.passwordreset.OnPasswordResetEvent;
import fr.cnes.regards.modules.accessrights.service.account.workflow.state.IAccountTransitions;

/**
 * Endpoints to manage REGARDS Accounts. Accounts are transverse to all projects and so are persisted in an instance
 * database
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@RestController
@ModuleInfo(name = "users", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(AccountsController.TYPE_MAPPING)
public class AccountsController implements IResourceController<Account> {

    public static final String PATH_PASSWORD = "/password"; // NOSONAR: not a password

    public static final String PATH_ACCOUNT_EMAIL_VALIDATE = "/{account_email}/validate";

    public static final String PATH_ACCOUNT_EMAIL_RESET_PASSWORD = "/{account_email}/resetPassword"; // NOSONAR: not a
                                                                                                     // password

    public static final String PATH_ACCOUNT_EMAIL_UNLOCK_ACCOUNT = "/{account_email}/unlockAccount";

    public static final String PATH_ACCOUNT_ACCOUNT_EMAIL = "/account/{account_email}";

    public static final String PATH_ACCOUNT_ID = "/{account_id}";

    public static final String PATH_ACTIVE_ACCOUNT = "/{account_email}/active";

    public static final String PATH_INACTIVE_ACCOUNT = "/{account_email}/inactive";

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String TYPE_MAPPING = "/accounts";

    @Autowired
    private IAccountService accountService;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAccountTransitions accountWorkflowManager;

    /**
     * The service exposing password reset functionalities
     */
    @Autowired
    private IPasswordResetService passwordResetService;

    /**
     * Root admin user login
     */
    @Value("${regards.accounts.root.user.login}")
    private String rootAdminUserLogin;

    /**
     * Use this to publish events
     */
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * Retrieve the list of all {@link Account}s.
     *
     * @param pPageable
     *            the pageable object used by Spring for building the page of result
     * @param pAssembler
     *            injected by Spring to help assemble results as paged resources
     * @param pStatus
     *            the account status to filter results on
     * @return The accounts list
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of account in the instance", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<PagedResources<Resource<Account>>> retrieveAccountList(final Pageable pPageable,
            final PagedResourcesAssembler<Account> pAssembler,
            @RequestParam(value = "status", required = false) final AccountStatus pStatus) {
        if (pStatus != null) {
            return ResponseEntity
                    .ok(toPagedResources(accountService.retrieveAccountList(pStatus, pPageable), pAssembler));
        } else {
            return ResponseEntity.ok(toPagedResources(accountService.retrieveAccountList(pPageable), pAssembler));
        }
    }

    /**
     * Create a new {@link Account} in state PENDING from the passed values
     *
     * @param pNewAccount
     *            The data transfer object containing values to create the account from
     * @return the {@link Account} created
     * @throws EntityException
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "create an new account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<Account>> createAccount(@Valid @RequestBody final Account pNewAccount)
            throws EntityException {
        accountService.checkPassword(pNewAccount);
        final Account created = accountService.createAccount(pNewAccount);
        final Resource<Account> resource = new Resource<>(created);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    /**
     * Retrieve the {@link Account} of passed <code>id</code>.
     *
     * @param pAccountId
     *            The {@link Account}'s <code>id</code>
     * @return The {@link Account}
     * @throws EntityNotFoundException
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_ID, method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the account account_id", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<Account>> retrieveAccount(@PathVariable("account_id") final Long pAccountId)
            throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(accountService.retrieveAccount(pAccountId)));
    }

    /**
     * Retrieve an account by his unique email
     *
     * @param pAccountEmail
     *            email of the account to retrieve
     * @return Account
     * @throws EntityNotFoundException
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_ACCOUNT_EMAIL, method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the account with his unique email", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<Account>> retrieveAccounByEmail(
            @PathVariable("account_email") final String pAccountEmail) throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(accountService.retrieveAccountByEmail(pAccountEmail)));

    }

    /**
     * Update an {@link Account} with passed values.
     *
     * @param pAccountId
     *            The <code>id</code> of the {@link Account} to update
     * @param pUpdatedAccount
     *            The new values to set
     * @return the {@link Account} updated
     * @throws EntityException
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_ID, method = RequestMethod.PUT)
    @ResourceAccess(description = "update the account account_id according to the body specified",
            role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<Account>> updateAccount(@PathVariable("account_id") final Long pAccountId,
            @Valid @RequestBody final Account pUpdatedAccount) throws EntityException {
        if (pUpdatedAccount.getPassword() != null) {
            accountService.checkPassword(pUpdatedAccount);
        }
        return ResponseEntity.ok(toResource(accountService.updateAccount(pAccountId, pUpdatedAccount)));
    }

    /**
     * Remove on {@link Account} from db.<br>
     * Only remove if no project user for any tenant.
     *
     * @param pAccountId
     *            The account <code>id</code>
     * @return void
     * @throws ModuleException
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_ID, method = RequestMethod.DELETE)
    @ResourceAccess(description = "remove the account account_id", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> removeAccount(@PathVariable("account_id") final Long pAccountId)
            throws ModuleException {
        final Account account = accountService.retrieveAccount(pAccountId);
        accountWorkflowManager.deleteAccount(account);
        return ResponseEntity.noContent().build();
    }

    /**
     * Send to the user an email containing a link with limited validity to unlock its account.
     *
     * @param pAccountEmail
     *            The {@link Account}'s <code>email</code>
     * @param pDto
     *            The DTO containing<br>
     *            - The url of the app from where was issued the query<br>
     *            - The url to redirect the user to the password reset interface
     * @return void
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} when no account with passed email could be found<br>
     *             {@link EntityOperationForbiddenException} when the account is not in status LOCKED<br>
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_EMAIL_UNLOCK_ACCOUNT, method = RequestMethod.POST)
    @ResourceAccess(description = "send a code of type type to the email specified", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestUnlockAccount(@PathVariable("account_email") final String pAccountEmail,
            @Valid @RequestBody final RequestAccountUnlockDto pDto) throws EntityException {
        // Retrieve the account
        final Account account = accountService.retrieveAccountByEmail(pAccountEmail);

        // Request account unlock
        accountWorkflowManager.requestUnlockAccount(account, pDto.getOriginUrl(), pDto.getRequestLink());
        return ResponseEntity.noContent().build();
    }

    /**
     * Unlock an {@link Account}.
     *
     * @param pAccountEmail
     *            The {@link Account}'s <code>email</code>
     * @param pTokenDto
     *            The token
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} when no account with passed email could be found or the token could
     *             not be found<br>
     *             {@link EntityOperationForbiddenException} when the account is not in status LOCKED or the token is
     *             invalid<br>
     * @return a no content HTTP response
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_EMAIL_UNLOCK_ACCOUNT, method = RequestMethod.PUT)
    @ResourceAccess(description = "unlock the account of provided email", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> performUnlockAccount(@PathVariable("account_email") final String pAccountEmail,
            @Valid @RequestBody final PerformUnlockAccountDto pTokenDto) throws EntityException {
        // Retrieve the account
        final Account account = accountService.retrieveAccountByEmail(pAccountEmail);

        // Perform account unlock
        accountWorkflowManager.performUnlockAccount(account, pTokenDto.getToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * Send to the user an email containing a link with limited validity to reset its password.
     *
     * @param pAccountEmail
     *            The {@link Account}'s <code>email</code>
     * @param pDto
     *            The DTO containing<br>
     *            - The url of the app from where was issued the query<br>
     *            - The url to redirect the user to the password reset interface
     * @return void
     * @throws EntityNotFoundException
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_EMAIL_RESET_PASSWORD, method = RequestMethod.POST)
    @ResourceAccess(description = "send a code of type type to the email specified", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestResetPassword(@PathVariable("account_email") final String pAccountEmail,
            @Valid @RequestBody final RequestResetPasswordDto pDto) throws EntityNotFoundException {
        // Retrieve the account
        final Account account = accountService.retrieveAccountByEmail(pAccountEmail);

        // Publish an application event
        eventPublisher.publishEvent(new OnPasswordResetEvent(account, pDto.getOriginUrl(), pDto.getRequestLink()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Change the passord of an {@link Account}.
     *
     * @param pAccountEmail
     *            The {@link Account}'s <code>email</code>
     * @param pDto
     *            The DTO containing : 1) the token 2) the new password
     * @return void
     * @throws EntityException
     *             <br>
     *             {@link EntityOperationForbiddenException} when the token is invalid<br>
     *             {@link EntityNotFoundException} when no account could be found<br>
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_EMAIL_RESET_PASSWORD, method = RequestMethod.PUT)
    @ResourceAccess(description = "change the passsword of account account_email if provided token is valid",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> performResetPassword(@PathVariable("account_email") final String pAccountEmail,
            @Valid @RequestBody final PerformResetPasswordDto pDto) throws EntityException {
        final Account toReset = accountService.retrieveAccountByEmail(pAccountEmail);
        toReset.setPassword(pDto.getNewPassword());
        accountService.checkPassword(toReset);
        passwordResetService.performPasswordReset(pAccountEmail, pDto.getToken(), pDto.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * Return <code>true</code> if the passed <code>pPassword</code> is equal to the one set on the {@link Account} of
     * passed <code>email</code>
     *
     * @param pEmail
     *            The {@link Account}'s <code>email</code>
     * @param pPassword
     *            The password to check
     * @return <code>true</code> if the password is valid, else <code>false</code>
     * @throws EntityException
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_EMAIL_VALIDATE, method = RequestMethod.GET)
    @ResourceAccess(description = "Validate the account password", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Boolean> validatePassword(@PathVariable("account_email") final String pEmail,
            @RequestParam("password") final String pPassword) throws EntityException {

        // Validate password for given user and password
        final boolean validPassword = accountService.validatePassword(pEmail, pPassword);

        // Return password validity
        return new ResponseEntity<>(validPassword, HttpStatus.OK);
    }

    /**
     * Endpoint allowing to provide a password and know if it is valid or not.
     *
     * @param pPassword
     *            password to check
     * @return the password validity
     */
    @ResponseBody
    @RequestMapping(value = PATH_PASSWORD, method = RequestMethod.POST)
    @ResourceAccess(description = "Validate a password", role = DefaultRole.PUBLIC)
    public ResponseEntity<Validity> checkPassword(@RequestBody final Password pPassword) {
        // JSON object is not reflected by a POJO because a POJO for ONE attribute would be overkill
        return new ResponseEntity<>(new Validity(accountService.validPassword(pPassword.getPassword())), HttpStatus.OK);
    }

    /**
     * Endpoint providing the rules a password has to respect in natural language
     */
    @ResponseBody
    @RequestMapping(value = PATH_PASSWORD, method = RequestMethod.GET)
    @ResourceAccess(description = "Get validation rules of password", role = DefaultRole.PUBLIC)
    public ResponseEntity<PasswordRules> getPasswordRules() {
        // JSON object is not reflected by a POJO because a POJO for ONE attribute would be overkill
        return new ResponseEntity<>(new PasswordRules(accountService.getPasswordRules()), HttpStatus.OK);
    }

    /**
     * Deactivates an {@link Account} in status ACTIVE.
     *
     * @param pAccountEmail the account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if no account with given email could be found<br>
     *             {@link EntityTransitionForbiddenException} if account is not in ACTIVE status<br>
     */
    @RequestMapping(value = PATH_INACTIVE_ACCOUNT, method = RequestMethod.PUT)
    @ResourceAccess(description = "Deactivates an active account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> inactiveAccount(@PathVariable("account_email") final String pAccountEmail)
            throws EntityException {
        // Retrieve the account
        final Account account = accountService.retrieveAccountByEmail(pAccountEmail);

        // Dactivate it
        accountWorkflowManager.inactiveAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Activates an {@link Account} in status INACTIVE.
     *
     * @param pAccountEmail the account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException
     *             <br>
     *             {@link EntityNotFoundException} if no account with given email could be found<br>
     *             {@link EntityTransitionForbiddenException} if account is not in INACTIVE status<br>
     */
    @RequestMapping(value = PATH_ACTIVE_ACCOUNT, method = RequestMethod.PUT)
    @ResourceAccess(description = "Activates an account which has been previously deactivated",
            role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> activeAccount(@PathVariable("account_email") final String pAccountEmail)
            throws EntityException {
        // Retrieve the account
        final Account account = accountService.retrieveAccountByEmail(pAccountEmail);

        // Activate it
        accountWorkflowManager.activeAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<Account> toResource(final Account pElement, final Object... pExtras) {
        Resource<Account> resource = null;
        if ((pElement != null) && (pElement.getId() != null)) {
            resource = resourceService.toResource(pElement);
            // Self retrieve link
            resourceService.addLink(resource, this.getClass(), "retrieveAccount", LinkRels.SELF,
                                    MethodParamFactory.build(Long.class, pElement.getId()));
            // Update link
            resourceService.addLink(resource, this.getClass(), "updateAccount", LinkRels.UPDATE,
                                    MethodParamFactory.build(Long.class, pElement.getId()),
                                    MethodParamFactory.build(Account.class));

            // Delete link only if the account is not admin and the account is deletable (not linked to exisisting
            // users)
            if (!AccountStatus.PENDING.equals(pElement.getStatus()) && !pElement.getEmail().equals(rootAdminUserLogin)
                    && accountWorkflowManager.canDelete(pElement)) {
                resourceService.addLink(resource, this.getClass(), "removeAccount", LinkRels.DELETE,
                                        MethodParamFactory.build(Long.class, pElement.getId()));
            }
            // Accept link, only if the account is in PENDING state
            if (AccountStatus.PENDING.equals(pElement.getStatus())) {
                resourceService.addLink(resource, RegistrationController.class, "acceptAccount", "accept",
                                        MethodParamFactory.build(String.class, pElement.getEmail()));
            }
            // Refuse link, only if the account is in PENDING state
            if (AccountStatus.PENDING.equals(pElement.getStatus())) {
                resourceService.addLink(resource, RegistrationController.class, "refuseAccount", "refuse",
                                        MethodParamFactory.build(String.class, pElement.getEmail()));
            }
            // Inactive link, only if the account is in ACTIVE state
            if (AccountStatus.ACTIVE.equals(pElement.getStatus())) {
                resourceService.addLink(resource, this.getClass(), "inactiveAccount", "inactive",
                                        MethodParamFactory.build(String.class, pElement.getEmail()));
            }
            // Active link, only if the account is in INACTIVE state
            if (AccountStatus.INACTIVE.equals(pElement.getStatus())) {
                resourceService.addLink(resource, this.getClass(), "activeAccount", "active",
                                        MethodParamFactory.build(String.class, pElement.getEmail()));
            }
        }
        return resource;
    }

    static class Password {

        private String password; //NOSONAR

        public Password() {
        }

        public Password(String password) {
            this.password = password;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @SuppressWarnings("unused")

    private static class PasswordRules {

        private String rules;

        public PasswordRules(String passwordRules) {
            rules = passwordRules;
        }

        public String getRules() {
            return rules;
        }

        public void setRules(String rules) {
            this.rules = rules;
        }
    }
}
