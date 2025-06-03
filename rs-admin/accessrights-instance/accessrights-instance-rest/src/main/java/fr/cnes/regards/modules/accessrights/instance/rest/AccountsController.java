/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationTokenDto;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.PerformChangePasswordDto;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.PerformResetPasswordDto;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.RequestResetPasswordDto;
import fr.cnes.regards.modules.accessrights.instance.service.IAccountService;
import fr.cnes.regards.modules.accessrights.instance.service.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.instance.service.encryption.EncryptionUtils;
import fr.cnes.regards.modules.accessrights.instance.service.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.accessrights.instance.service.passwordreset.OnPasswordResetEvent;
import fr.cnes.regards.modules.accessrights.instance.service.workflow.state.IAccountTransitions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
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

    public static final String RESEND_EMAIL_CONFIRMATION_PATH = EMAIL + "/verification/resend";

    public static final String RESET_EMAIL_CONFIRMATION_PATH = EMAIL + "/verification/reset";

    public static final String VERIFY_EMAIL_PATH = "/verifyEmail/{token}";

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

    private final IEmailVerificationTokenService emailVerificationTokenService;

    private final ApplicationEventPublisher eventPublisher;

    public AccountsController(IAccountService accountService,
                              IResourceService resourceService,
                              IAccountTransitions accountWorkflowManager,
                              IPasswordResetService passwordResetService,
                              IEmailVerificationTokenService emailVerificationTokenService,
                              ApplicationEventPublisher eventPublisher) {
        this.accountService = accountService;
        this.resourceService = resourceService;
        this.accountWorkflowManager = accountWorkflowManager;
        this.passwordResetService = passwordResetService;
        this.emailVerificationTokenService = emailVerificationTokenService;
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
    @Operation(summary = "Get Accounts", description = "Retrieve the list of accounts on the instance")
    @ApiResponse(responseCode = "200", description = "The list of accounts.")
    @ApiResponse(responseCode = "403", description = "The caller is not an instance administrator.")
    @ResourceAccess(description = "Endpoint to retrieve the list of accounts on the instance",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<PagedModel<EntityModel<Account>>> retrieveAccountList(
        @ParameterObject @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<Account> assembler,
        @ParameterObject AccountSearchParameters parameters) {
        return ResponseEntity.ok(toPagedResources(accountService.retrieveAccountList(parameters, pageable), assembler));
    }

    @PostMapping
    @Operation(summary = "Create Account", description = "Create a new account.")
    @ApiResponse(responseCode = "201", description = "The account was created.")
    @ApiResponse(responseCode = "400",
                 description = "Some parameters are invalid. If the account status is "
                               + "unspecified or set to `EMAIL_CONFIRMATION`, the body fields "
                               + "`originUrl` and `requestLink` are mandatory. If the field `project` is specified, "
                               + "it must be the name of an existing project.")
    @ApiResponse(responseCode = "403", description = "The caller is not an instance administrator.")
    @ResourceAccess(description = "Endpoint to create an new account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Account>> createAccount(
        @Parameter(description = "The account details") @Valid @RequestBody AccountNPassword accountNPassword)
        throws EntityException {
        Account account = accountNPassword.getAccount();
        account.setPassword(accountNPassword.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(EntityModel.of(accountService.createAccount(account,
                                                                               accountNPassword.getProject(),
                                                                               accountNPassword.getOriginUrl(),
                                                                               accountNPassword.getRequestLink())));
    }

    @GetMapping(ACCOUNT_ID_PATH)
    @Operation(summary = "Get Account By ID", description = "Retrieve details of the account with the specified ID.")
    @ApiResponse(responseCode = "200", description = "Account details")
    @ApiResponse(responseCode = "403", description = "The caller is not an instance administrator.")
    @ApiResponse(responseCode = "404", description = "No account exists with the specified ID.")
    @ResourceAccess(description = "Endpoint to retrieve an account by ID", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Account>> retrieveAccount(
        @Parameter(description = "ID of the account") @PathVariable("account_id") Long accountId)
        throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(accountService.retrieveAccount(accountId)));
    }

    @GetMapping(ACCOUNT_PATH)
    @Operation(summary = "Get Account By Email",
               description = "Retrieve details of the account with the specified " + "email.")
    @ApiResponse(responseCode = "200", description = "Account details")
    @ApiResponse(responseCode = "403", description = "The caller is not an instance administrator.")
    @ApiResponse(responseCode = "404", description = "No account exists with the specified email.")
    @ResourceAccess(description = "Endpoint to retrieve an account by email", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Account>> retrieveAccounByEmail(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail)
        throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(accountService.retrieveAccountByEmail(accountEmail)));

    }

    @PutMapping(ACCOUNT_ID_PATH)
    @Operation(summary = "Update Account", description = "Update details for the account with the specified ID.")
    @ApiResponse(responseCode = "200", description = "The account was successfully updated.")
    @ApiResponse(responseCode = "403", description = "The caller is not an instance administrator.")
    @ApiResponse(responseCode = "404", description = "No account exists with the specified ID.")
    @ResourceAccess(description = "Endpoint to update the account account_id according to the body specified",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<EntityModel<Account>> updateAccount(
        @Parameter(description = "ID of the account") @PathVariable("account_id") Long accountId,
        @Parameter(description = "Account details to update") @Valid @RequestBody Account updatedAccount)
        throws EntityException {
        if (updatedAccount.getPassword() != null) {
            accountService.checkPassword(updatedAccount);
        }
        return ResponseEntity.ok(toResource(accountService.updateAccount(accountId, updatedAccount)));
    }

    @DeleteMapping(ACCOUNT_ID_PATH)
    @Operation(summary = "Delete Account",
               description = "Delete the account with the specified ID.  \n*Note*: It is not possible to delete "
                             + "an account that is referenced at least one project.")
    @ApiResponse(responseCode = "204", description = "The account was successfully deleted.")
    @ApiResponse(responseCode = "403",
                 description = "The caller is not an instance administrator, or the account is "
                               + "referenced by at least one project.")
    @ApiResponse(responseCode = "404", description = "No account exists with the specified ID.")
    @ResourceAccess(description = "Endpoint to remove the account account_id", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> removeAccount(
        @Parameter(description = "ID of the account") @PathVariable("account_id") Long accountId)
        throws ModuleException {
        Account account = accountService.retrieveAccount(accountId);
        accountWorkflowManager.deleteAccount(account);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(RESET_EMAIL_CONFIRMATION_PATH)
    @Operation(summary = "Reverify Email",
               description = "Resets the account with the specified email "
                             + "to the `EMAIL_VERIFICATION` state, with "
                             + "the provided token details.  \n"
                             + "This endpoint is meant for migration purpose only.")
    @ApiResponse(responseCode = "204", description = "The account state was successfully reset.")
    @ApiResponse(responseCode = "403", description = "The caller is not an instance administrator.")
    @ApiResponse(responseCode = "404", description = "No account exists with the specified email.")
    @ApiResponse(responseCode = "409", description = "The account is already in `EMAIL_VERIFICATION` state.")
    @ResourceAccess(description = "Endpoint to reset the account to EMAIL_VERIFICATION state, with the provided reset "
                                  + "tokens details. This endpoint is for migration purpose only.",
                    role = DefaultRole.INSTANCE_ADMIN)
    public void resetEmailVerificationStatus(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail,
        @Valid @RequestBody EmailVerificationTokenDto token) throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        if (AccountStatus.EMAIL_VERIFICATION.equals(account.getStatus())) {
            throw new EntityAlreadyExistsException("The account with email "
                                                   + accountEmail
                                                   + " is already in "
                                                   + "EMAIL_VERIFICATION state");
        }
        emailVerificationTokenService.importToken(account, token);
        account.setStatus(AccountStatus.EMAIL_VERIFICATION);
        accountService.updateAccount(account.getId(), account);
    }

    @GetMapping(RESEND_EMAIL_CONFIRMATION_PATH)
    @Operation(summary = "Send Verification Email.",
               description = "Send a new verification email for an account. Such an email is automatically sent upon "
                             + "account creation, but an administrator may resend it if the validation token has expired.")
    @ApiResponse(responseCode = "204", description = "The email was successfully sent.")
    @ApiResponse(responseCode = "403",
                 description = "The caller is not an instance administrator, or the user has "
                               + "already confirmed their email.")
    @ApiResponse(responseCode = "404", description = "No user with the specified email exists.")
    @ResourceAccess(description = "Endpoint to resend a new verification email for an account waiting to be confirmed",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> resendVerificationEmail(@PathVariable("account_email") String email)
        throws EntityNotFoundException, EntityOperationForbiddenException {
        Account account = accountService.retrieveAccountByEmail(email);
        accountService.resendVerificationEmail(account);
        return ResponseEntity.ok().build();
    }

    @GetMapping(VERIFY_EMAIL_PATH)
    @Operation(summary = "Verify Email",
               description = "Confirm an account email using the specified email " + "verification token.")
    @ApiResponse(responseCode = "204", description = "The email was successfully confirmed.")
    @ApiResponse(responseCode = "403", description = "The token has expired.")
    @ApiResponse(responseCode = "404",
                 description = "The token is unknown (this may happen when the user has "
                               + "already confirmed their email with this token)")
    @ResourceAccess(description = "Endpoint to confirm an account email using a verification token",
                    role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> verifyEmail(
        @Parameter(description = "The verification token that was generated for the verification email")
        @PathVariable("token") String token) throws EntityException {
        EmailVerificationToken emailVerificationToken = emailVerificationTokenService.findByToken(token);
        accountWorkflowManager.verifyEmail(emailVerificationToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping(UNLOCK_ACCOUNT_PATH)
    @Operation(summary = "Send Unlock Account Email",
               description = "Send to the user an email containing a link with limited validity to unlock their account.")
    @ApiResponse(responseCode = "204", description = "The request was successfully taken into account.")
    @ApiResponse(responseCode = "403", description = "The account is not locked.")
    @ApiResponse(responseCode = "404", description = "No user with the specified email exists.")
    @ResourceAccess(description = "Endpoint to send to the user an email containing a link with limited validity to "
                                  + "unlock their account", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestUnlockAccount(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail,
        @Parameter(description = "Description of the unlock account request") @Valid @RequestBody
        RequestAccountUnlockDto dto) throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        accountWorkflowManager.requestUnlockAccount(account, dto.getOriginUrl(), dto.getRequestLink());
        return ResponseEntity.noContent().build();
    }

    @PutMapping(UNLOCK_ACCOUNT_PATH)
    @Operation(summary = "Unlock Account",
               description = "Unlock a locked account using an unlock token that was received by email.")
    @ApiResponse(responseCode = "204", description = "The account was successfully unlocked.")
    @ApiResponse(responseCode = "403", description = "The account is not locked, or the token has expired.")
    @ApiResponse(responseCode = "404",
                 description = "No user with the specified email exists, or the token is invalid.")
    @ResourceAccess(description = "Endpoint to unlock the account with provided email", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> performUnlockAccount(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail,
        @Parameter(description = "An object containing the unlock token") @Valid @RequestBody
        PerformUnlockAccountDto tokenDto) throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        accountWorkflowManager.performUnlockAccount(account, tokenDto.getToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(RESET_PASSWORD_PATH)
    @Operation(summary = "Send Password Reset Email",
               description = "Send to the user an email containing a link with limited validity to reset its password.")
    @ApiResponse(responseCode = "204", description = "The request was successfully taken into account.")
    @ApiResponse(responseCode = "404", description = "No user with the specified email exists.")
    @ResourceAccess(description = "Endpoint to send to the user an email containing a link with limited validity to "
                                  + "reset their password", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> requestResetPassword(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail,
        @Parameter(description = "Description of the email reset request") @Valid @RequestBody
        RequestResetPasswordDto dto) throws EntityNotFoundException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        eventPublisher.publishEvent(new OnPasswordResetEvent(account, dto.getOriginUrl(), dto.getRequestLink()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping(CHANGE_PASSWORD_PATH)
    @Operation(summary = "Change Password", description = "Changes the user password, given their current password.")
    @ApiResponse(responseCode = "204", description = "The password was successfully changed.")
    @ApiResponse(responseCode = "400", description = "The new password does not meet the password requirements.")
    @ApiResponse(responseCode = "404",
                 description = "No user with the specified email exists, or the current password is invalid.")
    @ResourceAccess(description = "Endpoint to change the password of account", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> performChangePassword(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail,
        @Parameter(description = "The old and the new passwords") @Valid @RequestBody
        PerformChangePasswordDto changePasswordDto) throws EntityException {
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

    @PutMapping(RESET_PASSWORD_PATH)
    @Operation(summary = "Reset Password",
               description = "Change an account password using a reset token that was received by email.")
    @ApiResponse(responseCode = "204", description = "The password was successfully changed.")
    @ApiResponse(responseCode = "403", description = "The token does not match the account, or the token has expired.")
    @ApiResponse(responseCode = "404",
                 description = "No user with the specified email exists, or the token is invalid.")
    @ResourceAccess(description = "Endpoint to change the password of account account_email if provided token is "
                                  + "valid", role = DefaultRole.PUBLIC)
    public ResponseEntity<Void> performResetPassword(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail,
        @Parameter(description = "The token and the new password") @Valid @RequestBody
        final PerformResetPasswordDto pDto) throws EntityException {
        Account toReset = accountService.retrieveAccountByEmail(accountEmail);
        toReset.setPassword(pDto.getNewPassword());
        accountService.checkPassword(toReset);
        passwordResetService.performPasswordReset(accountEmail, pDto.getToken(), pDto.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping(VALIDATE_PATH)
    @Operation(summary = "Verify Password",
               description = "Verify that the provided password is valid for the account"
                             + " with the specified email.")
    @ApiResponse(responseCode = "200", description = "Whether the provided password matches the account password.")
    @ApiResponse(responseCode = "403", description = "The caller is not an instance administrator.")
    @ApiResponse(responseCode = "404", description = "No account exists for the provided email.")
    @ResourceAccess(description = "Endpoint to validate the account password", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Boolean> validatePassword(
        @Parameter(description = "The account email") @PathVariable("account_email") String email,
        @Parameter(description = "The candidate password") @RequestParam String password) throws EntityException {
        boolean validPassword = accountService.validatePassword(email, password, true);
        return new ResponseEntity<>(validPassword, HttpStatus.OK);
    }

    @PostMapping(PASSWORD_RULES_PATH)
    @Operation(summary = "Check Password Validity",
               description = "Verify that the specified password meets the password requirements.")
    @ApiResponse(responseCode = "200", description = "Whether the provided password meets the requirements")
    @ResourceAccess(description = "Endpoint to validate a password", role = DefaultRole.PUBLIC)
    public ResponseEntity<Validity> checkPassword(
        @Parameter(description = "An object containing the password") @RequestBody Password password) {
        return new ResponseEntity<>(new Validity(accountService.validPassword(password.getPassword())), HttpStatus.OK);
    }

    @GetMapping(PASSWORD_RULES_PATH)
    @Operation(summary = "Get Password Rules",
               description = "Retrieve the list of password rules applicable to all new passwords set by users.")
    @ApiResponse(responseCode = "200", description = "The list of password rules in user-displayable form")
    @ResourceAccess(description = "Endpoint to get password validation rules", role = DefaultRole.PUBLIC)
    public ResponseEntity<PasswordRules> getPasswordRules() {
        return new ResponseEntity<>(new PasswordRules(accountService.getPasswordRules()), HttpStatus.OK);
    }

    @PutMapping(INACTIVE_ACCOUNT_PATH)
    @Operation(summary = "Deactivate Account", description = "Deactivate an account in status ACTIVE.")
    @ApiResponse(responseCode = "200", description = "The account was successfully deactivated.")
    @ApiResponse(responseCode = "403",
                 description = "The caller is not an instance administrator, or the account is "
                               + "not currently active.")
    @ApiResponse(responseCode = "404", description = "No account exists for the provided email.")
    @ResourceAccess(description = "Endpoint to deactivate an active account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> inactiveAccount(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail)
        throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        accountWorkflowManager.inactiveAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping(ACTIVE_ACCOUNT_PATH)
    @Operation(summary = "Activate Account", description = "Activate an account in status INACTIVE.")
    @ApiResponse(responseCode = "200", description = "The account was successfully activated.")
    @ApiResponse(responseCode = "403",
                 description = "The caller is not an instance administrator, or the account is "
                               + "not currently inactive.")
    @ApiResponse(responseCode = "404", description = "No account exists for the provided email.")
    @ResourceAccess(description = "Endpoint to activate an account which has been previously deactivated",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> activeAccount(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail)
        throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        accountWorkflowManager.activeAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping(ACCEPT_ACCOUNT_PATH)
    @Operation(summary = "Accept Account", description = "Accept an access request for an account in PENDING status.")
    @ApiResponse(responseCode = "200", description = "The account was successfully accepted.")
    @ApiResponse(responseCode = "403",
                 description = "The caller is not an instance administrator, or the account is "
                               + "not currently in PENDING status.")
    @ApiResponse(responseCode = "404", description = "No account exists for the provided email.")
    @ResourceAccess(description = "Endpoint to accept an access request", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> acceptAccount(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail)
        throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        accountWorkflowManager.acceptAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping(REFUSE_ACCOUNT_PATH)
    @Operation(summary = "Refuse Account", description = "Refuse an access request for an account in PENDING status.")
    @ApiResponse(responseCode = "200", description = "The account was successfully refused.")
    @ApiResponse(responseCode = "403",
                 description = "The caller is not an instance administrator, or the account is "
                               + "not currently in PENDING status.")
    @ApiResponse(responseCode = "404", description = "No account exists for the provided email.")
    @ResourceAccess(description = "Endpoint to refuse an access request", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> refuseAccount(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail)
        throws EntityException {
        Account account = accountService.retrieveAccountByEmail(accountEmail);
        accountWorkflowManager.refuseAccount(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(ORIGINS_PATH)
    @Operation(summary = "List Origins", description = "Return all possible origins for an account")
    @ApiResponse(responseCode = "200", description = "The list of all possible origins.")
    @ApiResponse(responseCode = "403", description = "The caller is not an instance administrator.")
    @ResourceAccess(description = "Endpoint to list all possible origins for an account",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<List<String>> getOrigins() {
        return ResponseEntity.ok(accountService.getOrigins());
    }

    @PutMapping(LINK_ACCOUNT_PATH)
    @Operation(summary = "Link to Project", description = "Link an account to a project.")
    @ApiResponse(responseCode = "200", description = "The account was successfully linked to the project.")
    @ApiResponse(responseCode = "400", description = "No project exists with the provided name.")
    @ApiResponse(responseCode = "403", description = "The caller is not an instance administrator.")
    @ApiResponse(responseCode = "404", description = "No account exists for the provided email.")
    @ResourceAccess(description = "Endpoint to link a project to an account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> link(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail,
        @Parameter(description = "The project name") @PathVariable("project") String project) throws EntityException {
        accountService.link(accountEmail, project);
        return ResponseEntity.ok().build();
    }

    @PutMapping(UNLINK_ACCOUNT_PATH)
    @Operation(summary = "Unlink from Project", description = "Unlink an account from a project.")
    @ApiResponse(responseCode = "200", description = "The account was successfully linked to the project.")
    @ApiResponse(responseCode = "400", description = "No project exists with the provided name.")
    @ApiResponse(responseCode = "403", description = "The caller is not an instance administrator.")
    @ApiResponse(responseCode = "404", description = "No account exists for the provided email.")
    @ResourceAccess(description = "Endpoint to unlink an account from a project", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> unlink(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail,
        @Parameter(description = "The project name") @PathVariable("project") String project) throws EntityException {
        accountService.unlink(accountEmail, project);
        return ResponseEntity.ok().build();
    }

    @PutMapping(UPDATE_ORIGIN_PATH)
    @Operation(summary = "Update Origin", description = "Updates the origin of an account.")
    @ApiResponse(responseCode = "200", description = "The account origin was successfully updated.")
    @ApiResponse(responseCode = "403", description = "The caller is not an instance administrator.")
    @ApiResponse(responseCode = "404", description = "No account exists for the provided email.")
    @ResourceAccess(description = "Update the origin of an account identified by email",
                    role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> updateOrigin(
        @Parameter(description = "The account email") @PathVariable("account_email") String accountEmail,
        @Parameter(description = "The new origin. If this string is empty, the origin is not updated.")
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
    public static class Password {

        @Schema(description = "The password")
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
    public static class PasswordRules {

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
