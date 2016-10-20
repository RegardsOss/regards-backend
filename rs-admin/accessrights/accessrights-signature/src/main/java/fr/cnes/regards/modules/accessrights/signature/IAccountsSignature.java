/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.signature;

import java.util.List;

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

import fr.cnes.regards.modules.accessrights.domain.CodeType;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidEntityException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 * Define the common interface of REST clients for {@link Account}s.
 *
 * @author CS SI
 */
@RequestMapping(path = "/accounts")
public interface IAccountsSignature {

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<Account>>> retrieveAccountList();

    @RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<Account>> createAccount(@Valid @RequestBody Account pNewAccount)
            throws AlreadyExistingException, InvalidEntityException;

    @RequestMapping(value = "/{account_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Resource<Account>> retrieveAccount(@PathVariable("account_id") Long pAccountId)
            throws EntityNotFoundException;

    /**
     * Update an {@link Account} with passed values.
     *
     * @param pAccountId
     *            The <code>id</code> of the {@link Account} to update
     * @param pUpdatedAccount
     *            The new values to set
     * @throws InvalidValueException
     *             Thrown when <code>pAccountId</code> is different from the id of <code>pUpdatedAccount</code>
     * @throws EntityNotFoundException
     *             Thrown when no {@link Account} could be found with id <code>pAccountId</code>
     */
    @RequestMapping(value = "/{account_id}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateAccount(@PathVariable("account_id") Long pAccountId,
            @Valid @RequestBody Account pUpdatedAccount) throws EntityNotFoundException, InvalidValueException;

    @RequestMapping(value = "/{account_id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> removeAccount(@PathVariable("account_id") Long pAccountId);

    /**
     * Do not respect REST architecture because the request comes from a mail client, ideally should be a PUT
     *
     * @param accountId
     * @param unlockCode
     * @return
     * @throws InvalidValueException
     * @throws EntityNotFoundException
     *             Thrown when no {@link Account} could be found with id <code>pAccountId</code>
     */
    @RequestMapping(value = "/{account_id}/unlock/{unlock_code}", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> unlockAccount(@PathVariable("account_id") Long pAccountId,
            @PathVariable("unlock_code") String pUnlockCode) throws InvalidValueException, EntityNotFoundException;

    /**
     * Change the passord of an {@link Account}.
     *
     * @param pAccountId
     *            The {@link Account}'s <code>id</code>
     * @param pResetCode
     *            The reset code. Required to allow a password change
     * @param pNewPassword
     *            The new <code>password</code>
     * @throws InvalidValueException
     *             Thrown when the passed reset code is different from the one expected
     * @throws EntityNotFoundException
     *             Thrown when no {@link Account} could be found with id <code>pAccountId</code>
     */
    @RequestMapping(value = "/{account_id}/password/{reset_code}", method = RequestMethod.PUT,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> changeAccountPassword(@PathVariable("account_id") Long pAccountId,
            @PathVariable("reset_code") String pResetCode, @Valid @RequestBody String pNewPassword)
            throws InvalidValueException, EntityNotFoundException;

    @RequestMapping(value = "/code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> codeForAccount(@RequestParam("email") String pEmail, @RequestParam("type") CodeType pType);

    @RequestMapping(value = "/settings", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<List<Resource<String>>> retrieveAccountSettings();

    @RequestMapping(value = "/settings", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> updateAccountSetting(@Valid @RequestBody String pUpdatedAccountSetting)
            throws InvalidValueException;

    @RequestMapping(value = "/{account_login}/validate", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Boolean> validatePassword(@PathVariable("account_login") String pLogin,
            @RequestParam("password") String pPassword) throws EntityNotFoundException;
}
