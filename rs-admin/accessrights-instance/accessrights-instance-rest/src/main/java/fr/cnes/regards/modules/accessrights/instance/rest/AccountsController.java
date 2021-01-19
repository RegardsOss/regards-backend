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
package fr.cnes.regards.modules.accessrights.instance.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.EntityTransitionForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.rest.utils.Validity;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.instance.domain.accountunlock.PerformUnlockAccountDto;
import fr.cnes.regards.modules.accessrights.instance.domain.accountunlock.RequestAccountUnlockDto;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.PerformChangePasswordDto;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.PerformResetPasswordDto;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.RequestResetPasswordDto;
import fr.cnes.regards.modules.accessrights.instance.service.IAccountService;
import fr.cnes.regards.modules.accessrights.instance.service.encryption.EncryptionUtils;
import fr.cnes.regards.modules.accessrights.instance.service.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.instance.service.passwordreset.OnPasswordResetEvent;
import fr.cnes.regards.modules.accessrights.instance.service.workflow.state.IAccountTransitions;

/**
 * Endpoints to manage REGARDS Accounts. Accounts are transverse to all projects and so are persisted in an instance
 * database
 * @author Sébastien Binda
 * @author Christophe Mertz

 */
@RestController
@RequestMapping(AccountsController.TYPE_MAPPING)
public class AccountsController implements IResourceController<Account> {

    public static final String PATH_PASSWORD = "/password"; // NOSONAR: not a password

    /**
     * Controller path for account email validation
     */
    public static final String PATH_ACCOUNT_EMAIL_VALIDATE = "/{account_email}/validate";

    public static final String PATH_ACCOUNT_EMAIL_RESET_PASSWORD = "/{account_email}/resetPassword"; // NOSONAR: not a
    // password

    /**
     * Controller path for account email unlock
     */
    public static final String PATH_ACCOUNT_EMAIL_UNLOCK_ACCOUNT = "/{account_email}/unlockAccount";

    /**
     * Controller path using account email as path variable
     */
    public static final String PATH_ACCOUNT_ACCOUNT_EMAIL = "/account/{account_email}";

    /**
     * Controller path using account id as path variable
     */
    public static final String PATH_ACCOUNT_ID = "/{account_id}";

    public static final String PATH_ACTIVE_ACCOUNT = "/{account_email}/active";

    public static final String PATH_INACTIVE_ACCOUNT = "/{account_email}/inactive";

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String TYPE_MAPPING = "/accounts";

    /**
     * Path for account acceptance
     */
    public static final String ACCEPT_ACCOUNT_RELATIVE_PATH = "/{account_email}/accept";

    /**
     * Path for account refusal
     */
    public static final String REFUSE_ACCOUNT_RELATIVE_PATH = "/{account_email}/refuse";

    private static final String PATH_ACCOUNT_EMAIL_CHANGE_PASSWORD = "/{account_email}/changePassword";

    @Autowired
    private IAccountService accountService;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAuthenticationResolver authResolver;

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
     * @param pageable the pageable object used by Spring for building the page of result
     * @param assembler injected by Spring to help assemble results as paged resources
     * @param status the account status to filter results on
     * @return The accounts list
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of account in the instance", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<PagedModel<EntityModel<Account>>> retrieveAccountList(
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<Account> assembler,
            @RequestParam(value = "status", required = false) AccountStatus status) {
        if (status != null) {
            return ResponseEntity.ok(toPagedResources(accountService.retrieveAccountList(status, pageable), assembler));
        } else {
            return ResponseEntity.ok(toPagedResources(accountService.retrieveAccountList(pageable), assembler));
        }
    }

    /**
     * Create a new {@link Account} in state PENDING from the passed values
     * @param newAccountWithPassword The data transfer object containing values to create the account from
     * @return the {@link Account} created
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "create an new account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Account>> createAccount(
            @Valid @RequestBody AccountNPassword newAccountWithPassword) throws EntityException {
        Account newAccount = newAccountWithPassword.getAccount();
        newAccount.setPassword(newAccountWithPassword.getPassword());
        accountService.checkPassword(newAccount);
        return new ResponseEntity<>(new EntityModel<>(accountService.createAccount(newAccount)), HttpStatus.CREATED);
    }

    /**
     * Retrieve the {@link Account} of passed <code>id</code>.
     * @param accountId The {@link Account}'s <code>id</code>
     * @return The {@link Account}
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_ID, method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the account account_id", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Account>> retrieveAccount(@PathVariable("account_id") Long accountId)
            throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(accountService.retrieveAccount(accountId)));
    }

    /**
     * Retrieve an account by his unique email
     * @param accountEmail email of the account to retrieve
     * @return Account
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_ACCOUNT_EMAIL, method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the account with his unique email", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Account>> retrieveAccounByEmail(
            @PathVariable("account_email") String accountEmail) throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(accountService.retrieveAccountByEmail(accountEmail)));

    }

    /**
     * Update an {@link Account} with passed values.
     * @param accountId The <code>id</code> of the {@link Account} to update
     * @param updatedAccount The new values to set
     * @return the {@link Account} updated
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_ID, method = RequestMethod.PUT)
    @ResourceAccess(description = "update the account account_id according to the body specified",
            role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Account>> updateAccount(@PathVariable("account_id") Long accountId,
            @Valid @RequestBody Account updatedAccount) throws EntityException {
        if (updatedAccount.getPassword() != null) {
            accountService.checkPassword(updatedAccount);
        }
        return ResponseEntity.ok(toResource(accountService.updateAccount(accountId, updatedAccount)));
    }

    /**
     * Remove on {@link Account} from db.<br>
     * Only remove if no project user for any tenant.
     * @param accountId The account <code>id</code>
     * @return void
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_ID, method = RequestMethod.DELETE)
    @ResourceAccess(description = "remove the account account_id", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> removeAccount(@PathVariable("account_id") Long accountId) throws ModuleException {
        Account account = accountService.retrieveAccount(accountId);
        accountWorkflowManager.deleteAccount(account);
        return ResponseEntity.noContent().build();
    }

    /**
     * Send to the user an email containing a link with limited validity to unlock its account.
     * @param accountEmail The {@link Account}'s <code>email</code>
     * @param dto The DTO containing<br>
     * - The url of the app from where was issued the query<br>
     * - The url to redirect the user to the password reset interface
     * @return void
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} when no account with passed email could be found<br>
     *                         {@link EntityOperationForbiddenException} when the account is not in status LOCKED<br>
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_EMAIL_UNLOCK_ACCOUNT, method = RequestMethod.POST)
    @ResourceAccess(description = "send a code of type type to the email specified", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestUnlockAccount(@PathVariable("account_email") String accountEmail,
            @Valid @RequestBody RequestAccountUnlockDto dto) throws EntityException {
        // Retrieve the account
        Account account = accountService.retrieveAccountByEmail(accountEmail);

        // Request account unlock
        accountWorkflowManager.requestUnlockAccount(account, dto.getOriginUrl(), dto.getRequestLink());
        return ResponseEntity.noContent().build();
    }

    /**
     * Unlock an {@link Account}.
     * @param accountEmail The {@link Account}'s <code>email</code>
     * @param tokenDto The token
     * @return a no content HTTP response
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} when no account with passed email could be found or the token could
     *                         not be found<br>
     *                         {@link EntityOperationForbiddenException} when the account is not in status LOCKED or the token is
     *                         invalid<br>
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_EMAIL_UNLOCK_ACCOUNT, method = RequestMethod.PUT)
    @ResourceAccess(description = "unlock the account of provided email", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> performUnlockAccount(@PathVariable("account_email") String accountEmail,
            @Valid @RequestBody PerformUnlockAccountDto tokenDto) throws EntityException {
        // Retrieve the account
        Account account = accountService.retrieveAccountByEmail(accountEmail);

        // Perform account unlock
        accountWorkflowManager.performUnlockAccount(account, tokenDto.getToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * Send to the user an email containing a link with limited validity to reset its password.
     * @param accountEmail The {@link Account}'s <code>email</code>
     * @param dto The DTO containing<br>
     * - The url of the app from where was issued the query<br>
     * - The url to redirect the user to the password reset interface
     * @return void
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_EMAIL_RESET_PASSWORD, method = RequestMethod.POST)
    @ResourceAccess(description = "send a code of type type to the email specified", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestResetPassword(@PathVariable("account_email") String accountEmail,
            @Valid @RequestBody RequestResetPasswordDto dto) throws EntityNotFoundException {
        // Retrieve the account
        Account account = accountService.retrieveAccountByEmail(accountEmail);

        // Publish an application event
        eventPublisher.publishEvent(new OnPasswordResetEvent(account, dto.getOriginUrl(), dto.getRequestLink()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Change the password of an {@link Account}.
     * @param accountEmail The {@link Account}'s <code>email</code>
     * @param changePasswordDto The DTO containing : 1) the token 2) the new password
     * @return void
     * @throws EntityException <br>
     *                         {@link EntityOperationForbiddenException} when the token is invalid<br>
     *                         {@link EntityNotFoundException} when no account could be found<br>
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_EMAIL_CHANGE_PASSWORD, method = RequestMethod.PUT)
    @ResourceAccess(description = "Change the passsword of account account_email", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> performChangePassword(@PathVariable("account_email") String accountEmail,
            @Valid @RequestBody PerformChangePasswordDto changePasswordDto) throws EntityException {
        final Account toReset = accountService.retrieveAccountByEmail(accountEmail);
        if (!authResolver.getUser().equals(accountEmail)
                && !accountService.validatePassword(accountEmail, changePasswordDto.getOldPassword(), false)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        accountService.validPassword(changePasswordDto.getNewPassword());
        accountService.changePassword(toReset.getId(),
                                      EncryptionUtils.encryptPassword(changePasswordDto.getNewPassword()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Change the password of an {@link Account}.
     * @param accountEmail The {@link Account}'s <code>email</code>
     * @param pDto The DTO containing : 1) the token 2) the new password
     * @return void
     * @throws EntityException <br>
     *                         {@link EntityOperationForbiddenException} when the token is invalid<br>
     *                         {@link EntityNotFoundException} when no account could be found<br>
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_EMAIL_RESET_PASSWORD, method = RequestMethod.PUT)
    @ResourceAccess(description = "Change the passsword of account account_email if provided token is valid",
            role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> performResetPassword(@PathVariable("account_email") String accountEmail,
            @Valid @RequestBody final PerformResetPasswordDto pDto) throws EntityException {
        Account toReset = accountService.retrieveAccountByEmail(accountEmail);
        toReset.setPassword(pDto.getNewPassword());
        accountService.checkPassword(toReset);
        passwordResetService.performPasswordReset(accountEmail, pDto.getToken(), pDto.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * Return <code>true</code> if the passed <code>pPassword</code> is equal to the one set on the {@link Account} of
     * passed <code>email</code>
     * @param email The {@link Account}'s <code>email</code>
     * @param password The password to check
     * @return <code>true</code> if the password is valid, else <code>false</code>
     */
    @ResponseBody
    @RequestMapping(value = PATH_ACCOUNT_EMAIL_VALIDATE, method = RequestMethod.GET)
    @ResourceAccess(description = "Validate the account password", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Boolean> validatePassword(@PathVariable("account_email") String email,
            @RequestParam("password") String password) throws EntityException {
        // Validate password for given user and password
        boolean validPassword = accountService.validatePassword(email, password, true);
        // Return password validity
        return new ResponseEntity<>(validPassword, HttpStatus.OK);
    }

    /**
     * Endpoint allowing to provide a password and know if it is valid or not.
     * @param password password to check
     * @return the password validity
     */
    @ResponseBody
    @RequestMapping(value = PATH_PASSWORD, method = RequestMethod.POST)
    @ResourceAccess(description = "Validate a password", role = DefaultRole.PUBLIC)
    public ResponseEntity<Validity> checkPassword(@RequestBody Password password) {
        // JSON object is not reflected by a POJO because a POJO for ONE attribute would be overkill
        return new ResponseEntity<>(new Validity(accountService.validPassword(password.getPassword())), HttpStatus.OK);
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
     * @param accountEmail the account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} if no account with given email could be found<br>
     *                         {@link EntityTransitionForbiddenException} if account is not in ACTIVE status<br>
     */
    @RequestMapping(value = PATH_INACTIVE_ACCOUNT, method = RequestMethod.PUT)
    @ResourceAccess(description = "Deactivates an active account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> inactiveAccount(@PathVariable("account_email") String accountEmail)
            throws EntityException {
        // Retrieve the account
        final Account account = accountService.retrieveAccountByEmail(accountEmail);

        // Dactivate it
        accountWorkflowManager.inactiveAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Activates an {@link Account} in status INACTIVE.
     * @param accountEmail the account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} if no account with given email could be found<br>
     *                         {@link EntityTransitionForbiddenException} if account is not in INACTIVE status<br>
     */
    @RequestMapping(value = PATH_ACTIVE_ACCOUNT, method = RequestMethod.PUT)
    @ResourceAccess(description = "Activates an account which has been previously deactivated",
            role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> activeAccount(@PathVariable("account_email") String accountEmail)
            throws EntityException {
        // Retrieve the account
        final Account account = accountService.retrieveAccountByEmail(accountEmail);

        // Activate it
        accountWorkflowManager.activeAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Grants access to the project user
     * @param accountEmail account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *                         {@link EntityNotFoundException} if project user is in illegal status for denial<br>
     */
    @RequestMapping(value = ACCEPT_ACCOUNT_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Accepts the access request", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> acceptAccount(@PathVariable("account_email") String accountEmail)
            throws EntityException {
        // Retrieve the account
        final Account account = accountService.retrieveAccountByEmail(accountEmail);

        // Accept it
        accountWorkflowManager.acceptAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Refuse the account request
     * @param accountEmail account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(value = REFUSE_ACCOUNT_RELATIVE_PATH, method = RequestMethod.PUT)
    @ResourceAccess(description = "Refuses the access request", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> refuseAccount(@PathVariable("account_email") String accountEmail)
            throws EntityException {
        // Retrieve the account
        final Account account = accountService.retrieveAccountByEmail(accountEmail);

        // Accept it
        accountWorkflowManager.refuseAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<Account> toResource(Account element, final Object... extras) {
        EntityModel<Account> resource = null;
        if (element != null && element.getId() != null) {
            resource = resourceService.toResource(element);
            // Self retrieve link
            resourceService.addLink(resource, this.getClass(), "retrieveAccount", LinkRels.SELF,
                                    MethodParamFactory.build(Long.class, element.getId()));
            // Update link
            resourceService.addLink(resource, this.getClass(), "updateAccount", LinkRels.UPDATE,
                                    MethodParamFactory.build(Long.class, element.getId()),
                                    MethodParamFactory.build(Account.class));

            // Delete link only if the account is not admin and the account is deletable (not linked to exisisting
            // users)
            if (!AccountStatus.PENDING.equals(element.getStatus()) && !element.getEmail().equals(rootAdminUserLogin)
                    && accountWorkflowManager.canDelete(element)) {
                resourceService.addLink(resource, this.getClass(), "removeAccount", LinkRels.DELETE,
                                        MethodParamFactory.build(Long.class, element.getId()));
            }
            // Accept link, only if the account is in PENDING state
            if (AccountStatus.PENDING.equals(element.getStatus())) {
                resourceService.addLink(resource, this.getClass(), "acceptAccount", LinkRelation.of("accept"),
                                        MethodParamFactory.build(String.class, element.getEmail()));
            }
            // Refuse link, only if the account is in PENDING state
            if (AccountStatus.PENDING.equals(element.getStatus())) {
                resourceService.addLink(resource, this.getClass(), "refuseAccount", LinkRelation.of("refuse"),
                                        MethodParamFactory.build(String.class, element.getEmail()));
            }
            // Inactive link, only if the account is in ACTIVE state
            if (AccountStatus.ACTIVE.equals(element.getStatus())) {
                resourceService.addLink(resource, this.getClass(), "inactiveAccount", LinkRelation.of("inactive"),
                                        MethodParamFactory.build(String.class, element.getEmail()));
            }
            // Active link, only if the account is in INACTIVE state
            if (AccountStatus.INACTIVE.equals(element.getStatus())) {
                resourceService.addLink(resource, this.getClass(), "activeAccount", LinkRelation.of("active"),
                                        MethodParamFactory.build(String.class, element.getEmail()));
            }
        }
        return resource;
    }

    /**
     * DTO to wrap password
     */
    static class Password {

        private String password; //NOSONAR

        /**
         * Default constructor
         */
        public Password() {
        }

        /**
         * Constructor setting the parameter as attribute
         */
        public Password(String password) {
            this.password = password;
        }

        /**
         * @return the password
         */
        public String getPassword() {
            return password;
        }

        /**
         * Set the password
         */
        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * DTO to wrap password rules into an object
     */
    private static class PasswordRules {

        /**
         * The rules
         */
        private String rules;

        /**
         * Constructor setting the parameter as attribute
         */
        public PasswordRules(String passwordRules) {
            rules = passwordRules;
        }

        /**
         * @return the rules
         */
        @SuppressWarnings("unused")
        public String getRules() {
            return rules;
        }

        /**
         * Set the rules
         */
        @SuppressWarnings("unused")
        public void setRules(String rules) {
            this.rules = rules;
        }
    }
}
