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
package fr.cnes.regards.modules.accessrights.instance.rest;

import fr.cnes.regards.framework.hateoas.*;
import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.framework.module.rest.utils.Validity;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSearchParameters;
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
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * Endpoints to manage REGARDS Accounts. Accounts are transverse to all projects and so are persisted in an instance
 * database
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 */
@RestController
@RequestMapping(AccountsController.TYPE_MAPPING)
public class AccountsController implements IResourceController<Account> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountsController.class);

    private static final String EMAIL = "/{account_email}";

    private static final String PROJECT = "/{project}";

    /**
     * Root mapping for requests of this rest controller
     */
    public static final String TYPE_MAPPING = "/accounts";

    /**
     * Controller path using account id as path variable
     */
    public static final String ACCOUNT_ID_PATH = "/{account_id}";

    /**
     * Controller path using account email as path variable
     */
    public static final String ACCOUNT_PATH = "/account" + EMAIL;

    public static final String PASSWORD_RULES_PATH = "/password"; // NOSONAR: not a password

    public static final String RESET_PASSWORD_PATH = EMAIL + "/resetPassword"; // NOSONAR: not a password

    public static final String CHANGE_PASSWORD_PATH = EMAIL + "/changePassword";

    public static final String VALIDATE_PATH = EMAIL + "/validate";

    public static final String ACCEPT_ACCOUNT_PATH = EMAIL + "/accept";

    public static final String REFUSE_ACCOUNT_PATH = EMAIL + "/refuse";

    public static final String UNLOCK_ACCOUNT_PATH = EMAIL + "/unlockAccount";

    public static final String ACTIVE_ACCOUNT_PATH = EMAIL + "/active";

    public static final String INACTIVE_ACCOUNT_PATH = EMAIL + "/inactive";

    public static final String LINK_ACCOUNT_PATH = EMAIL + "/link" + PROJECT;

    public static final String UNLINK_ACCOUNT_PATH = EMAIL + "/unlink" + PROJECT;

    public static final String UPDATE_ORIGIN_PATH = EMAIL + "/origin/{origin}";

    public static final String ORIGINS_PATH = "/origins";

    @Value("${regards.accounts.root.user.login}")
    private String rootAdminUserLogin;

    private final IAccountService accountService;

    private final IResourceService resourceService;

    private final IAccountTransitions accountWorkflowManager;

    private final IPasswordResetService passwordResetService;

    private final ApplicationEventPublisher eventPublisher;

    public AccountsController(IAccountService accountService,
                              IResourceService resourceService,
                              IAccountTransitions accountWorkflowManager,
                              IPasswordResetService passwordResetService,
                              ApplicationEventPublisher eventPublisher) {
        this.accountService = accountService;
        this.resourceService = resourceService;
        this.accountWorkflowManager = accountWorkflowManager;
        this.passwordResetService = passwordResetService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Retrieve the list of all {@link Account}s.
     *
     * @param pageable   the pageable object used by Spring for building the page of result
     * @param assembler  injected by Spring to help assemble results as paged resources
     * @param parameters optional search parameters
     * @return The accounts list
     */
    @GetMapping
    @ResourceAccess(description = "retrieve the list of account in the instance", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<PagedModel<EntityModel<Account>>> retrieveAccountList(
        @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<Account> assembler,
        AccountSearchParameters parameters) {
        return ResponseEntity.ok(toPagedResources(accountService.retrieveAccountList(parameters, pageable), assembler));
    }

    /**
     * Create a new {@link Account} in state PENDING from the passed values
     *
     * @param accountNPassword The data transfer object containing values to create the account from
     * @return the {@link Account} created
     */
    @PostMapping
    @ResourceAccess(description = "create an new account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Account>> createAccount(@Valid @RequestBody AccountNPassword accountNPassword)
        throws EntityException {
        Account account = accountNPassword.getAccount();
        account.setPassword(accountNPassword.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(EntityModel.of(accountService.createAccount(account,
                                                                               accountNPassword.getProject())));
    }

    /**
     * Retrieve the {@link Account} of passed <code>id</code>.
     *
     * @param accountId The {@link Account}'s <code>id</code>
     * @return The {@link Account}
     */
    @GetMapping(ACCOUNT_ID_PATH)
    @ResourceAccess(description = "retrieve the account account_id", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Account>> retrieveAccount(@PathVariable("account_id") Long accountId)
        throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(accountService.retrieveAccount(accountId)));
    }

    /**
     * Retrieve an account by his unique email
     *
     * @param accountEmail email of the account to retrieve
     * @return Account
     */
    @GetMapping(ACCOUNT_PATH)
    @ResourceAccess(description = "retrieve the account with his unique email", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Account>> retrieveAccounByEmail(
        @PathVariable("account_email") String accountEmail) throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(accountService.retrieveAccountByEmail(accountEmail)));

    }

    /**
     * Update an {@link Account} with passed values.
     *
     * @param accountId      The <code>id</code> of the {@link Account} to update
     * @param updatedAccount The new values to set
     * @return the {@link Account} updated
     */
    @PutMapping(ACCOUNT_ID_PATH)
    @ResourceAccess(description = "update the account account_id according to the body specified",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Account>> updateAccount(@PathVariable("account_id") Long accountId,
                                                              @Valid @RequestBody Account updatedAccount)
        throws EntityException {
        if (updatedAccount.getPassword() != null) {
            accountService.checkPassword(updatedAccount);
        }
        return ResponseEntity.ok(toResource(accountService.updateAccount(accountId, updatedAccount)));
    }

    /**
     * Remove on {@link Account} from db.<br>
     * Only remove if no project user for any tenant.
     *
     * @param accountId The account <code>id</code>
     * @return void
     */
    @DeleteMapping(ACCOUNT_ID_PATH)
    @ResourceAccess(description = "remove the account account_id", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> removeAccount(@PathVariable("account_id") Long accountId) throws ModuleException {
        Account account = accountService.retrieveAccount(accountId);
        accountWorkflowManager.deleteAccount(account);
        return ResponseEntity.noContent().build();
    }

    /**
     * Send to the user an email containing a link with limited validity to unlock its account.
     *
     * @param accountEmail The {@link Account}'s <code>email</code>
     * @param dto          The DTO containing<br>
     *                     - The url of the app from where was issued the query<br>
     *                     - The url to redirect the user to the password reset interface
     * @return void
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} when no account with passed email could be found<br>
     *                         {@link EntityOperationForbiddenException} when the account is not in status LOCKED<br>
     */
    @PostMapping(UNLOCK_ACCOUNT_PATH)
    @ResourceAccess(description = "send a code of type type to the email specified", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestUnlockAccount(@PathVariable("account_email") String accountEmail,
                                                     @Valid @RequestBody RequestAccountUnlockDto dto)
        throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        accountWorkflowManager.requestUnlockAccount(account, dto.getOriginUrl(), dto.getRequestLink());
        return ResponseEntity.noContent().build();
    }

    /**
     * Unlock an {@link Account}.
     *
     * @param accountEmail The {@link Account}'s <code>email</code>
     * @param tokenDto     The token
     * @return a no content HTTP response
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} when no account with passed email could be found or the token could
     *                         not be found<br>
     *                         {@link EntityOperationForbiddenException} when the account is not in status LOCKED or the token is
     *                         invalid<br>
     */
    @PutMapping(UNLOCK_ACCOUNT_PATH)
    @ResourceAccess(description = "unlock the account of provided email", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> performUnlockAccount(@PathVariable("account_email") String accountEmail,
                                                     @Valid @RequestBody PerformUnlockAccountDto tokenDto)
        throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        accountWorkflowManager.performUnlockAccount(account, tokenDto.getToken());
        return ResponseEntity.noContent().build();
    }

    /**
     * Send to the user an email containing a link with limited validity to reset its password.
     *
     * @param accountEmail The {@link Account}'s <code>email</code>
     * @param dto          The DTO containing<br>
     *                     - The url of the app from where was issued the query<br>
     *                     - The url to redirect the user to the password reset interface
     * @return void
     */
    @PostMapping(RESET_PASSWORD_PATH)
    @ResourceAccess(description = "send a code of type type to the email specified", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestResetPassword(@PathVariable("account_email") String accountEmail,
                                                     @Valid @RequestBody RequestResetPasswordDto dto)
        throws EntityNotFoundException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        eventPublisher.publishEvent(new OnPasswordResetEvent(account, dto.getOriginUrl(), dto.getRequestLink()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Change the password of an {@link Account}.
     *
     * @param accountEmail      The {@link Account}'s <code>email</code>
     * @param changePasswordDto The DTO containing : 1) the token 2) the new password
     * @return void
     * @throws EntityException <br>
     *                         {@link EntityOperationForbiddenException} when the token is invalid<br>
     *                         {@link EntityNotFoundException} when no account could be found<br>
     */
    @PutMapping(CHANGE_PASSWORD_PATH)
    @ResourceAccess(description = "Change the passsword of account account_email", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> performChangePassword(@PathVariable("account_email") String accountEmail,
                                                      @Valid @RequestBody PerformChangePasswordDto changePasswordDto)
        throws EntityException {
        Account toReset = accountService.retrieveAccountByEmail(accountEmail);
        if (!accountService.validatePassword(accountEmail, changePasswordDto.getOldPassword(), false)) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        if (!accountService.validPassword(changePasswordDto.getNewPassword())) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        LOGGER.info("Changing password for user {}", accountEmail);
        accountService.changePassword(toReset.getId(),
                                      EncryptionUtils.encryptPassword(changePasswordDto.getNewPassword()));
        return ResponseEntity.noContent().build();
    }

    /**
     * Change the password of an {@link Account}.
     *
     * @param accountEmail The {@link Account}'s <code>email</code>
     * @param pDto         The DTO containing : 1) the token 2) the new password
     * @return void
     * @throws EntityException <br>
     *                         {@link EntityOperationForbiddenException} when the token is invalid<br>
     *                         {@link EntityNotFoundException} when no account could be found<br>
     */
    @PutMapping(RESET_PASSWORD_PATH)
    @ResourceAccess(description = "Change the passsword of account account_email if provided token is valid",
                    role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> performResetPassword(@PathVariable("account_email") String accountEmail,
                                                     @Valid @RequestBody final PerformResetPasswordDto pDto)
        throws EntityException {
        Account toReset = accountService.retrieveAccountByEmail(accountEmail);
        toReset.setPassword(pDto.getNewPassword());
        accountService.checkPassword(toReset);
        passwordResetService.performPasswordReset(accountEmail, pDto.getToken(), pDto.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * Return <code>true</code> if the passed <code>pPassword</code> is equal to the one set on the {@link Account} of
     * passed <code>email</code>
     *
     * @param email    The {@link Account}'s <code>email</code>
     * @param password The password to check
     * @return <code>true</code> if the password is valid, else <code>false</code>
     */
    @GetMapping(VALIDATE_PATH)
    @ResourceAccess(description = "Validate the account password", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Boolean> validatePassword(@PathVariable("account_email") String email,
                                                    @RequestParam String password) throws EntityException {
        boolean validPassword = accountService.validatePassword(email, password, true);
        return new ResponseEntity<>(validPassword, HttpStatus.OK);
    }

    /**
     * Endpoint allowing to provide a password and know if it is valid or not.
     *
     * @param password password to check
     * @return the password validity
     */
    @PostMapping(PASSWORD_RULES_PATH)
    @ResourceAccess(description = "Validate a password", role = DefaultRole.PUBLIC)
    public ResponseEntity<Validity> checkPassword(@RequestBody Password password) {
        return new ResponseEntity<>(new Validity(accountService.validPassword(password.getPassword())), HttpStatus.OK);
    }

    /**
     * Endpoint providing the rules a password has to respect in natural language
     */
    @GetMapping(PASSWORD_RULES_PATH)
    @ResourceAccess(description = "Get validation rules of password", role = DefaultRole.PUBLIC)
    public ResponseEntity<PasswordRules> getPasswordRules() {
        return new ResponseEntity<>(new PasswordRules(accountService.getPasswordRules()), HttpStatus.OK);
    }

    /**
     * Deactivates an {@link Account} in status ACTIVE.
     *
     * @param accountEmail the account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} if no account with given email could be found<br>
     *                         {@link EntityTransitionForbiddenException} if account is not in ACTIVE status<br>
     */
    @PutMapping(INACTIVE_ACCOUNT_PATH)
    @ResourceAccess(description = "Deactivates an active account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> inactiveAccount(@PathVariable("account_email") String accountEmail)
        throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        accountWorkflowManager.inactiveAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Activates an {@link Account} in status INACTIVE.
     *
     * @param accountEmail the account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityNotFoundException} if no account with given email could be found<br>
     *                         {@link EntityTransitionForbiddenException} if account is not in INACTIVE status<br>
     */
    @PutMapping(ACTIVE_ACCOUNT_PATH)
    @ResourceAccess(description = "Activates an account which has been previously deactivated",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> activeAccount(@PathVariable("account_email") String accountEmail)
        throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        accountWorkflowManager.activeAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Grants access to the project user
     *
     * @param accountEmail account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     * @throws EntityException <br>
     *                         {@link EntityTransitionForbiddenException} if no project user could be found<br>
     *                         {@link EntityNotFoundException} if project user is in illegal status for denial<br>
     */
    @PutMapping(ACCEPT_ACCOUNT_PATH)
    @ResourceAccess(description = "Accepts the access request", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> acceptAccount(@PathVariable("account_email") String accountEmail)
        throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        accountWorkflowManager.acceptAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Refuse the account request
     *
     * @param accountEmail account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @PutMapping(REFUSE_ACCOUNT_PATH)
    @ResourceAccess(description = "Refuses the access request", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> refuseAccount(@PathVariable("account_email") String accountEmail)
        throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        accountWorkflowManager.refuseAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(ORIGINS_PATH)
    @ResourceAccess(description = "List all possible origins for an account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<List<String>> getOrigins() {
        return ResponseEntity.ok(accountService.getOrigins());
    }

    @PutMapping(LINK_ACCOUNT_PATH)
    @ResourceAccess(description = "Link a project to an account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> link(@PathVariable("account_email") String accountEmail,
                                     @PathVariable("project") String project) throws EntityException {
        accountService.link(accountEmail, project);
        return ResponseEntity.ok().build();
    }

    @PutMapping(UNLINK_ACCOUNT_PATH)
    @ResourceAccess(description = "Link a project to an account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> unlink(@PathVariable("account_email") String accountEmail,
                                       @PathVariable("project") String project) throws EntityException {
        accountService.unlink(accountEmail, project);
        return ResponseEntity.ok().build();
    }

    @PutMapping(UPDATE_ORIGIN_PATH)
    @ResourceAccess(description = "Update the origin of an account identified by email",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> updateOrigin(@PathVariable("account_email") String accountEmail,
                                             @PathVariable("origin") String origin) throws EntityException {
        accountService.updateOrigin(accountEmail, origin);
        return ResponseEntity.ok().build();
    }

    @Override
    public EntityModel<Account> toResource(Account element, Object... extras) {

        EntityModel<Account> resource = null;

        if ((element != null) && (element.getId() != null)) {

            resource = resourceService.toResource(element);
            MethodParam<Long> idParam = MethodParamFactory.build(Long.class, element.getId());
            MethodParam<String> mailParam = MethodParamFactory.build(String.class, element.getEmail());

            resourceService.addLink(resource, this.getClass(), "retrieveAccount", LinkRels.SELF, idParam);
            resourceService.addLink(resource,
                                    this.getClass(),
                                    "updateAccount",
                                    LinkRels.UPDATE,
                                    idParam,
                                    MethodParamFactory.build(Account.class));

            if (AccountStatus.PENDING.equals(element.getStatus())) {
                resourceService.addLink(resource,
                                        this.getClass(),
                                        "acceptAccount",
                                        LinkRelation.of("accept"),
                                        mailParam);
                resourceService.addLink(resource,
                                        this.getClass(),
                                        "refuseAccount",
                                        LinkRelation.of("refuse"),
                                        mailParam);
            } else if (!element.getEmail().equals(rootAdminUserLogin) && accountWorkflowManager.canDelete(element)) {
                resourceService.addLink(resource, this.getClass(), "removeAccount", LinkRels.DELETE, idParam);
            }
            if (AccountStatus.ACTIVE.equals(element.getStatus())) {
                resourceService.addLink(resource,
                                        this.getClass(),
                                        "inactiveAccount",
                                        LinkRelation.of("inactive"),
                                        mailParam);
            }
            if (AccountStatus.INACTIVE.equals(element.getStatus())) {
                resourceService.addLink(resource,
                                        this.getClass(),
                                        "activeAccount",
                                        LinkRelation.of("active"),
                                        mailParam);
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
