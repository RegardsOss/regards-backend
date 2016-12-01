/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.CodeType;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.registration.ValidationUrlBuilder;
import fr.cnes.regards.modules.accessrights.service.account.IAccountService;
import fr.cnes.regards.modules.accessrights.service.account.IAccountSettingsService;
import fr.cnes.regards.modules.accessrights.workflow.account.IAccountTransitions;

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
@RequestMapping(path = "/accounts")
public class AccountsController implements IResourceController<Account> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountsController.class);

    @Autowired
    private IAccountService accountService;

    /**
     * Resource service to manage visibles hateoas links
     */
    @Autowired
    private IResourceService resourceService;

    @Autowired
    private IAccountTransitions accountWorkflowManager;

    @Autowired
    private IAccountSettingsService accountSettingsService;

    /**
     * Root admin user login
     */
    @Value("${regards.accounts.root.user.login}")
    private String rootAdminUserLogin;

    /**
     * Retrieve the list of all {@link Account}s.
     *
     * @return The accounts list
     */
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the list of account in the instance", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<List<Resource<Account>>> retrieveAccountList() {
        return ResponseEntity.ok(toResources(accountService.retrieveAccountList()));

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
    public ResponseEntity<Resource<Account>> createAccount(@Valid @RequestBody final Account pNewAccount,
            final HttpServletRequest pRequest) throws EntityException {
        // Manually create the expected DTO in order to use the same common interface of account creation
        final AccessRequestDto dto = new AccessRequestDto(pNewAccount);

        // Build the email validation link
        final String validationUrl = ValidationUrlBuilder.buildFrom(pRequest);
        final Account created = accountWorkflowManager.requestAccount(dto, validationUrl);
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
    @RequestMapping(value = "/{account_id}", method = RequestMethod.GET)
    @ResourceAccess(description = "retrieve the account account_id", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<Account>> retrieveAccount(@PathVariable("account_id") final Long pAccountId)
            throws EntityNotFoundException {
        return ResponseEntity.ok(toResource(accountService.retrieveAccount(pAccountId)));
    }

    /**
     *
     * Retrieve an account by his unique email
     *
     * @param pAccountEmail
     *            email of the account to retrieve
     * @return Account
     * @throws EntityNotFoundException
     */
    @ResponseBody
    @RequestMapping(value = "/account/{account_email}", method = RequestMethod.GET)
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
    @RequestMapping(value = "/{account_id}", method = RequestMethod.PUT)
    @ResourceAccess(description = "update the account account_id according to the body specified",
            role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Resource<Account>> updateAccount(@PathVariable("account_id") final Long pAccountId,
            @Valid @RequestBody final Account pUpdatedAccount) throws EntityException {
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
    @RequestMapping(value = "/{account_id}", method = RequestMethod.DELETE)
    @ResourceAccess(description = "remove the account account_id", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> removeAccount(@PathVariable("account_id") final Long pAccountId)
            throws ModuleException {
        final Account account = accountService.retrieveAccount(pAccountId);
        accountWorkflowManager.deleteAccount(account);
        return ResponseEntity.noContent().build();
    }

    /**
     * Do not respect REST architecture because the request comes from a mail client, ideally should be a PUT
     *
     * @param pAccountId
     *            The account id
     * @param pUnlockCode
     *            the unlock code
     * @return void
     * @throws ModuleException
     */
    @ResponseBody
    @RequestMapping(value = "/{account_id}/unlock/{unlock_code}", method = RequestMethod.GET)
    @ResourceAccess(description = "unlock the account account_id according to the code unlock_code",
            role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> unlockAccount(@PathVariable("account_id") final Long pAccountId,
            @PathVariable("unlock_code") final String pUnlockCode) throws ModuleException {
        final Account account = accountService.retrieveAccount(pAccountId);
        accountWorkflowManager.unlockAccount(account, pUnlockCode);
        return ResponseEntity.noContent().build();
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
     * @return void
     * @throws EntityException
     */
    @ResponseBody
    @RequestMapping(value = "/{account_id}/password/{reset_code}", method = RequestMethod.PUT)
    @ResourceAccess(description = "change the passsword of account account_id according to the code reset_code",
            role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> changeAccountPassword(@PathVariable("account_id") final Long pAccountId,
            @PathVariable("reset_code") final String pResetCode, @Valid @RequestBody final String pNewPassword)
            throws EntityException {
        accountService.changeAccountPassword(pAccountId, pResetCode, pNewPassword);
        return ResponseEntity.noContent().build();
    }

    /**
     * Send to the user an email containing a code:<br>
     * - to reset password<br>
     * - to unlock the account
     *
     * @param pEmail
     *            The {@link Account}'s <code>email</code>
     * @param pType
     *            The {@link CodeType} to send
     * @return void
     * @throws EntityNotFoundException
     */
    @ResponseBody
    @RequestMapping(value = "/code", method = RequestMethod.GET)
    @ResourceAccess(description = "send a code of type type to the email specified", role = DefaultRole.INSTANCE_ADMIN)
    public ResponseEntity<Void> sendAccountCode(@RequestParam("email") final String pEmail,
            @RequestParam("type") final CodeType pType) throws EntityNotFoundException {
        accountService.sendAccountCode(pEmail, pType);
        return ResponseEntity.noContent().build();
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
     * @param pUpdatedAccountSetting
     *            The {@link AccountSettings}
     * @return The updated {@link AccountSettings}
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
     * @throws EntityNotFoundException
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

    @Override
    public Resource<Account> toResource(final Account pElement, final Object... pExtras) {
        Resource<Account> resource = null;
        if ((pElement != null) && (pElement.getId() != null)) {
            resource = resourceService.toResource(pElement);
            resourceService.addLink(resource, this.getClass(), "retrieveAccount", LinkRels.SELF,
                                    MethodParamFactory.build(Long.class, pElement.getId()));
            resourceService.addLink(resource, this.getClass(), "updateAccount", LinkRels.UPDATE,
                                    MethodParamFactory.build(Long.class, pElement.getId()),
                                    MethodParamFactory.build(Account.class));
            if (!pElement.getEmail().equals(rootAdminUserLogin)) {
                resourceService.addLink(resource, this.getClass(), "removeAccount", LinkRels.DELETE,
                                        MethodParamFactory.build(Long.class, pElement.getId()));
            }
            resourceService.addLink(resource, this.getClass(), "retrieveAccountList", LinkRels.LIST);
            resourceService.addLink(resource, this.getClass(), "retrieveAccountSettings", "getAccountSettings");
            resourceService.addLink(resource, this.getClass(), "updateAccountSetting", "setAccountSetting",
                                    MethodParamFactory.build(AccountSettings.class));
        }
        return resource;
    }

}
