/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.rest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;
import javax.validation.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.CodeType;
import fr.cnes.regards.modules.accessRights.service.IAccountService;
import fr.cnes.regards.modules.accessRights.signature.AccountsSignature;
import fr.cnes.regards.modules.core.annotation.ModuleInfo;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@RestController
@ModuleInfo(name = "users", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/accounts")
public class AccountsController implements AccountsSignature {

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

    @Override
    public HttpEntity<List<Resource<Account>>> retrieveAccountList() {
        List<Account> accounts = accountService_.retrieveAccountList();
        List<Resource<Account>> resources = accounts.stream().map(a -> new Resource<>(a)).collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public HttpEntity<Resource<Account>> createAccount(@Valid @RequestBody Account pNewAccount)
            throws AlreadyExistingException {
        Account created = accountService_.createAccount(pNewAccount);
        Resource<Account> resource = new Resource<>(created);
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @Override
    public HttpEntity<Resource<Account>> retrieveAccount(@PathVariable("account_id") Long accountId) {
        Account account = accountService_.retrieveAccount(accountId);
        Resource<Account> resource = new Resource<>(account);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @Override
    public HttpEntity<Void> updateAccount(@PathVariable("account_id") Long accountId,
            @Valid @RequestBody Account pUpdatedAccount) throws OperationNotSupportedException {
        accountService_.updateAccount(accountId, pUpdatedAccount);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public HttpEntity<Void> removeAccount(@PathVariable("account_id") Long accountId) {
        accountService_.removeAccount(accountId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public HttpEntity<Void> unlockAccount(@PathVariable("account_id") Long accountId,
            @PathVariable("unlock_code") String unlockCode) throws InvalidValueException {
        accountService_.unlockAccount(accountId, unlockCode);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public HttpEntity<Void> changeAccountPassword(@PathVariable("account_id") Long accountId,
            @PathVariable("reset_code") String resetCode, @Valid @RequestBody String pNewPassword)
            throws InvalidValueException {
        accountService_.changeAccountPassword(accountId, resetCode, pNewPassword);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public HttpEntity<Void> codeForAccount(@RequestParam("email") String email, @RequestParam("type") CodeType type) {
        accountService_.codeForAccount(email, type);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public HttpEntity<List<Resource<String>>> retrieveAccountSettings() {
        List<String> accountSettings = accountService_.retrieveAccountSettings();
        List<Resource<String>> resources = accountSettings.stream().map(a -> new Resource<>(a))
                .collect(Collectors.toList());
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    @Override
    public HttpEntity<Void> updateAccountSetting(@Valid @RequestBody String pUpdatedAccountSetting)
            throws InvalidValueException {
        accountService_.updateAccountSetting(pUpdatedAccountSetting);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public HttpEntity<Boolean> validatePassword(@PathVariable("account_login") String pLogin,
            @RequestParam("password") String pPassword) throws NoSuchElementException {
        Boolean valid = accountService_.validatePassword(pLogin, pPassword);
        return new ResponseEntity<>(valid, HttpStatus.OK);
    }
}
