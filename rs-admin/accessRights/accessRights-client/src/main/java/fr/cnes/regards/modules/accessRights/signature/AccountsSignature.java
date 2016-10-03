/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.signature;

import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.CodeType;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;
import fr.cnes.regards.security.utils.endpoint.annotation.ResourceAccess;

public interface AccountsSignature {

    @ResourceAccess(description = "retrieve the list of account in the instance", name = "")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<Account>>> retrieveAccountList();

    @ResourceAccess(description = "create an new account", name = "")
    @RequestMapping(value = "", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<Account>> createAccount(@Valid @RequestBody Account pNewAccount)
            throws AlreadyExistingException;

    @ResourceAccess(description = "retrieve the account account_id", name = "")
    @RequestMapping(value = "/{account_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<Account>> retrieveAccount(@PathVariable("account_id") Long accountId);

    @ResourceAccess(description = "update the account account_id according to the body specified", name = "")
    @RequestMapping(value = "/{account_id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateAccount(@PathVariable("account_id") Long accountId,
            @Valid @RequestBody Account pUpdatedAccount) throws OperationNotSupportedException;

    @ResourceAccess(description = "remove the account account_id", name = "")
    @RequestMapping(value = "/{account_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeAccount(@PathVariable("account_id") Long accountId);

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
    @ResponseBody
    HttpEntity<Void> unlockAccount(@PathVariable("account_id") Long accountId,
            @PathVariable("unlock_code") String unlockCode) throws InvalidValueException;

    @ResourceAccess(description = "change the passsword of account account_id according to the code reset_code", name = "")
    @RequestMapping(value = "/{account_id}/password/{reset_code}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> changeAccountPassword(@PathVariable("account_id") Long accountId,
            @PathVariable("reset_code") String resetCode, @Valid @RequestBody String pNewPassword)
            throws InvalidValueException;

    @ResourceAccess(description = "send a code of type type to the email specified", name = "")
    @RequestMapping(value = "/code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> codeForAccount(@RequestParam("email") String email, @RequestParam("type") CodeType type);

    @ResourceAccess(description = "retrieve the list of setting managing the accounts", name = "")
    @RequestMapping(value = "/settings", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<String>>> retrieveAccountSettings();

    @ResourceAccess(description = "update the setting managing the account", name = "")
    @RequestMapping(value = "/settings", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateAccountSetting(@Valid @RequestBody String pUpdatedAccountSetting)
            throws InvalidValueException;

    @ResourceAccess(description = "Validate the account password", name = "")
    @RequestMapping(value = "/{account_login}/validate", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Boolean> validatePassword(@PathVariable("account_login") String pLogin,
            @RequestParam("password") String pPassword) throws NoSuchElementException;
}
