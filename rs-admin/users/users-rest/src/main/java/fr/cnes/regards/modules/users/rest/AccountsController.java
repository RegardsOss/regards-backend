package fr.cnes.regards.modules.users.rest;

import java.util.List;
import java.util.NoSuchElementException;

import javax.annotation.PostConstruct;
import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;
import fr.cnes.regards.microservices.core.information.ModuleInfo;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.users.domain.Account;
import fr.cnes.regards.modules.users.domain.AccountSetting;
import fr.cnes.regards.modules.users.domain.CodeType;
import fr.cnes.regards.modules.users.service.IAccountService;

@RestController
@ModuleInfo(name = "users", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/accounts")
public class AccountsController {

    @Autowired
    private MethodAutorizationService authService;

    @Autowired
    private IAccountService accountService_;

    /**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        // admin can do everything!
        authService.setAutorities("/accounts@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accounts@POST", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accounts/{account_id}@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accounts/{account_id}@PUT", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accounts/{account_id}@DELETE", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accounts/code@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accounts/{account_id}/unlock/{unlock_code}@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accounts/{account_id}/password/{reset_code}@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accounts/settings@GET", new RoleAuthority("ADMIN"));
        authService.setAutorities("/accounts/settings@PUT", new RoleAuthority("ADMIN"));
        // users can just get!
        authService.setAutorities("/accounts@GET", new RoleAuthority("USER"));
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Data Not Found")
    public void dataNotFound() {
    }

    @ExceptionHandler(AlreadyExistingException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public void dataAlreadyExisting() {
    }

    @ExceptionHandler(OperationNotSupportedException.class)
    @ResponseStatus(value = HttpStatus.METHOD_NOT_ALLOWED)
    public void operationNotSupported() {
    }

    @ResourceAccess
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<List<Account>> retrieveAccountList() {
        List<Account> accounts = this.accountService_.retrieveAccountList();
        return new ResponseEntity<>(accounts, HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Account> createAccount(@Valid @RequestBody Account pNewAccount)
            throws AlreadyExistingException {
        Account created = this.accountService_.createAccount(pNewAccount);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @ResourceAccess
    @RequestMapping(value = "/{account_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Account> retrieveAccount(@PathVariable("account_id") String accountId) {
        Account account = this.accountService_.retrieveAccount(accountId);
        return new ResponseEntity<>(account, HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "/{account_id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateAccount(@PathVariable("account_id") String accountId,
            @Valid @RequestBody Account pUpdatedAccount) throws OperationNotSupportedException {
        this.accountService_.updateAccount(accountId, pUpdatedAccount);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "/{account_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> removeAccount(@PathVariable("account_id") String accountId) {
        this.accountService_.removeAccount(accountId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "/code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> codeForAccount(@RequestParam("email") String email,
            @RequestParam("type") CodeType type) {
        this.accountService_.codeForAccount(email, type);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Do not respect REST architecture because the request comes from a mail client, ideally should be a PUT
     *
     * @param accountId
     * @param unlockCode
     * @return
     */
    @ResourceAccess
    @RequestMapping(value = "/{account_id}/unlock/{unlock_code}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> unlockAccount(@RequestParam("account_id") String accountId,
            @RequestParam("unlock_code") String unlockCode) {
        this.accountService_.unlockAccount(accountId, unlockCode);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "/{account_id}/password/{reset_code}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> changeAccountPassword(@RequestParam("account_id") String accountId,
            @RequestParam("reset_code") String resetCode, @RequestBody String pNewPassword) {
        this.accountService_.changeAccountPassword(accountId, resetCode, pNewPassword);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "/settings", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<List<AccountSetting>> retrieveAccountSettings() {
        List<AccountSetting> accountSettings = this.accountService_.retrieveAccountSettings();
        return new ResponseEntity<>(accountSettings, HttpStatus.OK);
    }

    @ResourceAccess
    @RequestMapping(value = "/settings", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateAccountSetting(
            @Valid @RequestBody AccountSetting pUpdatedAccountSetting) {
        this.accountService_.updateAccountSetting(pUpdatedAccountSetting);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
