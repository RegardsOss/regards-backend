/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.CodeType;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.account.IAccountSettingsService;
import fr.cnes.regards.modules.accessrights.service.account.IAccountTransitions;

/**
 *
 * Class AccountsController
 *
 * Endpoints to manage REGARDS Accounts. Accounts are transverse to all projects and so are persisted in an instance
 * database
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RestController
@ModuleInfo(name = "users", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping(path = "/accounts")
public class AccountsController {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountsController.class);

    @Autowired
    private IAccountService accountService;

    @Autowired
    private IAccountTransitions accountWorkflowManager;

    @Autowired
    private IAccountSettingsService accountSettingsService;

    /**
     * Retrieve the list of all {@link Account}s.
     *
     * @return The accounts list
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of account in the instance", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<List<Resource<Account>>> retrieveAccountList() {
        final List<Account> accounts = accountService.retrieveAccountList();
        final List<Resource<Account>> resources = accounts.stream().map(a -> new Resource<>(a))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Create a new account in state PENDING from the passed values
     *
     * @param pNewAccount
     *            The data transfer object containing values to create the account from
     * @return the created account
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @ResourceAccess(description = "create an new account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<Account>> createAccount(@Valid @RequestBody final Account pNewAccount)
            throws EntityException {
        final AccessRequestDTO dto = new AccessRequestDTO(pNewAccount);
        final Account created = accountWorkflowManager.requestAccount(dto);
        final Resource<Account> resource = new Resource<>(created);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    /**
     * Retrieve the {@link Account} of passed <code>id</code>.
     *
     * @param pAccountId
     *            The {@link Account}'s <code>id</code>
     * @return The account
     */
    @ResponseBody
    @RequestMapping(value = "/{account_id}", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the account account_id", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<Account>> retrieveAccount(@PathVariable("account_id") final Long accountId)
            throws EntityNotFoundException {
        final Account account = accountService.retrieveAccount(accountId);
        final Resource<Account> resource = new Resource<>(account);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     *
     * Retrieve an account by his unique email
     *
     * @param pAccountEmail
     *            email of the account to retrieve
     * @return Account
     */
    @ResponseBody
    @RequestMapping(value = "/account/{account_email}", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the account with his unique email", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<Account>> retrieveAccounByEmail(
            @PathVariable("account_email") final String pAccountEmail) {
        ResponseEntity<Resource<Account>> response;
        try {
            final Account account = accountService.retrieveAccountByEmail(pAccountEmail);
            final Resource<Account> resource = new Resource<>(account);
            response = new ResponseEntity<>(resource, HttpStatus.OK);
        } catch (final EntityNotFoundException e) {
            LOG.info("Not found", e);
            response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return response;

    }

    /**
     * Update an {@link Account} with passed values.
     *
     * @param pAccountId
     *            The <code>id</code> of the {@link Account} to update
     * @param pUpdatedAccount
     *            The new values to set
     */
    @ResponseBody
    @RequestMapping(value = "/{account_id}", method = RequestMethod.PUT)
    @ResourceAccess(description = "update the account account_id according to the body specified",
            role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> updateAccount(@PathVariable("account_id") final Long accountId,
            @Valid @RequestBody final Account pUpdatedAccount) throws EntityException {
        accountService.updateAccount(accountId, pUpdatedAccount);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Remove on {@link Account} from db.<br>
     * Only remove if no project user for any tenant.
     *
     * @param pAccountId
     *            The account <code>id</code>
     */
    @ResponseBody
    @RequestMapping(value = "/{account_id}", method = RequestMethod.DELETE)
    @ResourceAccess(description = "remove the account account_id", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> removeAccount(@PathVariable("account_id") final Long pAccountId)
            throws ModuleException {
        final Account account = accountService.retrieveAccount(pAccountId);
        accountWorkflowManager.delete(account);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Do not respect REST architecture because the request comes from a mail client, ideally should be a PUT
     *
     * @param pAccountId
     *            The account id
     * @param pUnlockCode
     *            the unlock code
     * @return void
     */
    @ResponseBody
    @RequestMapping(value = "/{account_id}/unlock/{unlock_code}", method = RequestMethod.GET)
    @ResourceAccess(description = "unlock the account account_id according to the code unlock_code",
            role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> unlockAccount(@PathVariable("account_id") final Long pAccountId,
            @PathVariable("unlock_code") final String pUnlockCode) throws ModuleException {
        final Account account = accountService.retrieveAccount(pAccountId);
        accountWorkflowManager.unlockAccount(account, pUnlockCode);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Change the passord of an {@link Account}.
     *
     * @param pAccountId
     *            The {@link Account}'s <code>id</code>
     * @param pResetCode
     *            The reset code. Required to allow a password change
     * @param pNewPassword
     *            The new <code>password</code>
     */
    @ResponseBody
    @RequestMapping(value = "/{account_id}/password/{reset_code}", method = RequestMethod.PUT)
    @ResourceAccess(description = "change the passsword of account account_id according to the code reset_code",
            role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> changeAccountPassword(@PathVariable("account_id") final Long accountId,
            @PathVariable("reset_code") final String resetCode, @Valid @RequestBody final String pNewPassword)
            throws EntityException {
        accountService.changeAccountPassword(accountId, resetCode, pNewPassword);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Send to the user an email containing a code:<br>
     * - to reset password<br>
     * - to unlock the account
     *
     * @param pEmail
     *            The {@link Account}'s <code>email</code>
     * @param pType
     *            The type of code
     */
    @ResponseBody
    @RequestMapping(value = "/code", method = RequestMethod.GET)
    @ResourceAccess(description = "send a code of type type to the email specified", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> sendAccountCode(@RequestParam("email") final String email,
            @RequestParam("type") final CodeType type) throws EntityNotFoundException {
        accountService.sendAccountCode(email, type);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Retrieve the {@link AccountSettings} for the instance.
     *
     * @return The {@link AccountSettings} wrapped in a {@link Resource} and a {@link ResponseEntity}
     */
    @ResponseBody
    @RequestMapping(value = "/settings", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of setting managing the accounts",
            role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<AccountSettings>> retrieveAccountSettings() {
        final AccountSettings settings = accountSettingsService.retrieve();
        return new ResponseEntity<>(new Resource<>(settings), HttpStatus.OK);
    }

    /**
     * Update the {@link AccountSettings} for the instance.
     *
     * @param pSettings
     *            The {@link AccountSettings}
     * @return The updated account settings
     */
    @ResponseBody
    @RequestMapping(value = "/settings", method = RequestMethod.PUT)
    @ResourceAccess(description = "update the setting managing the account", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> updateAccountSetting(@Valid @RequestBody final AccountSettings pUpdatedAccountSetting) {
        accountSettingsService.update(pUpdatedAccountSetting);
        return new ResponseEntity<>(HttpStatus.OK);
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
     */
    @ResponseBody
    @RequestMapping(value = "/{account_email}/validate", method = RequestMethod.GET)
    @ResourceAccess(description = "Validate the account password", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<AccountStatus> validatePassword(@PathVariable("account_email") final String pEmail,
            @RequestParam("password") final String pPassword) throws EntityNotFoundException {
        if (accountService.validatePassword(pEmail, pPassword)) {
            return new ResponseEntity<>(AccountStatus.ACTIVE, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(AccountStatus.INACTIVE, HttpStatus.OK);
        }
    }

}
