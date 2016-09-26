/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.rest;

import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;
import javax.validation.ValidationException;

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

import fr.cnes.regards.microservices.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.CodeType;
import fr.cnes.regards.modules.accessRights.domain.HateoasDTO;
import fr.cnes.regards.modules.accessRights.service.IAccountService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@RestController
@ModuleInfo(name = "users", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/accounts")
public class AccountsController {

    @Autowired
    private IAccountService accountService_;

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Data Not Found")
    public void dataNotFound() {
    }

    @ExceptionHandler(AlreadyExistingException.class)
    @ResponseStatus(value = HttpStatus.CONFLICT)
    public void dataAlreadyExisting() {
    }

    @ExceptionHandler(OperationNotSupportedException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "operation not supported")
    public void operationNotSupported() {
    }

    @ExceptionHandler(InvalidValueException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "invalid Value")
    public void invalidValue() {
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY, reason = "data does not respect validation constrains")
    public void validation() {

    }

    @ResourceAccess(description = "retrieve the list of account in the instance", name = "")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<HateoasDTO<List<Account>>> retrieveAccountList() {
        List<Account> accounts = accountService_.retrieveAccountList();

        return new ResponseEntity<>(new HateoasDTO<>(accounts), HttpStatus.OK);
    }

    @ResourceAccess(description = "create an new account", name = "")
    @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<HateoasDTO<Account>> createAccount(@Valid @RequestBody Account pNewAccount)
            throws AlreadyExistingException {
        Account created = accountService_.createAccount(pNewAccount);
        return new ResponseEntity<>(new HateoasDTO<>(created), HttpStatus.CREATED);
    }

    @ResourceAccess(description = "retrieve the account account_id", name = "")
    @RequestMapping(value = "/{account_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<HateoasDTO<Account>> retrieveAccount(@PathVariable("account_id") Long accountId) {
        Account account = accountService_.retrieveAccount(accountId);
        return new ResponseEntity<>(new HateoasDTO<>(account), HttpStatus.OK);
    }

    @ResourceAccess(description = "update the account account_id according to the body specified", name = "")
    @RequestMapping(value = "/{account_id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateAccount(@PathVariable("account_id") Long accountId,
            @Valid @RequestBody Account pUpdatedAccount) throws OperationNotSupportedException {
        accountService_.updateAccount(accountId, pUpdatedAccount);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "remove the account account_id", name = "")
    @RequestMapping(value = "/{account_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> removeAccount(@PathVariable("account_id") Long accountId) {
        accountService_.removeAccount(accountId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Do not respect REST architecture because the request comes from a mail client, ideally should be a PUT
     *
     * @param accountId
     * @param unlockCode
     * @return
     * @throws InvalidValueException
     */
    @ResourceAccess(description = "unlock the account account_id according to the code unlock_code", name = "")
    @RequestMapping(value = "/{account_id}/unlock/{unlock_code}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> unlockAccount(@PathVariable("account_id") Long accountId,
            @PathVariable("unlock_code") String unlockCode) throws InvalidValueException {
        accountService_.unlockAccount(accountId, unlockCode);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "change the passsword of account account_id according to the code reset_code", name = "")
    @RequestMapping(value = "/{account_id}/password/{reset_code}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> changeAccountPassword(@PathVariable("account_id") Long accountId,
            @PathVariable("reset_code") String resetCode, @Valid @RequestBody String pNewPassword)
            throws InvalidValueException {
        accountService_.changeAccountPassword(accountId, resetCode, pNewPassword);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "send a code of type type to the email specified", name = "")
    @RequestMapping(value = "/code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> codeForAccount(@RequestParam("email") String email,
            @RequestParam("type") CodeType type) {
        accountService_.codeForAccount(email, type);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @ResourceAccess(description = "retrieve the list of setting managing the accounts", name = "")
    @RequestMapping(value = "/settings", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<HateoasDTO<List<String>>> retrieveAccountSettings() {
        List<String> accountSettings = accountService_.retrieveAccountSettings();
        return new ResponseEntity<>(new HateoasDTO<>(accountSettings), HttpStatus.OK);
    }

    @ResourceAccess(description = "update the setting managing the account", name = "")
    @RequestMapping(value = "/settings", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody HttpEntity<Void> updateAccountSetting(@Valid @RequestBody String pUpdatedAccountSetting)
            throws InvalidValueException {
        accountService_.updateAccountSetting(pUpdatedAccountSetting);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
