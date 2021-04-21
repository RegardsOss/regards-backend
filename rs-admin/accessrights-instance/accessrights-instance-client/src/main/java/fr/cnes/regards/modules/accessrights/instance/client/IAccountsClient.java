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
package fr.cnes.regards.modules.accessrights.instance.client;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.CodeType;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.PerformResetPasswordDto;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.RequestResetPasswordDto;

/**
 * Feign client for rs-admin Accounts controller.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard

 */
@RestClient(name = "rs-admin-instance", contextId = "rs-admin-instance.accounts-client")
@RequestMapping(path = "/accounts", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public interface IAccountsClient {

    /**
     * Path for account acceptance
     */
    String ACCEPT_ACCOUNT_RELATIVE_PATH = "/{account_email}/accept";

    /**
     * Path for account refusal
     */
    String REFUSE_ACCOUNT_RELATIVE_PATH = "/{account_email}/refuse";

    /**
     * Retrieve the list of all {@link Account}s.
     *
     * @return The accounts list
     */
    @RequestMapping(method = RequestMethod.GET)
    ResponseEntity<PagedModel<EntityModel<Account>>> retrieveAccountList(@RequestParam("page") int pPage,
            @RequestParam("size") int pSize);

    /**
     * Create a new account in state PENDING from the passed values
     *
     * @param newAccountWithPassword
     *            The data transfer object containing values to create the account from
     * @return the created account
     */
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<EntityModel<Account>> createAccount(@Valid @RequestBody AccountNPassword newAccountWithPassword);

    /**
     * Retrieve the {@link Account} of passed <code>id</code>.
     *
     * @param pAccountId
     *            The {@link Account}'s <code>id</code>
     * @return The account
     */
    @RequestMapping(value = "/{account_id}", method = RequestMethod.GET)
    ResponseEntity<EntityModel<Account>> retrieveAccount(@PathVariable("account_id") Long pAccountId);

    /**
     *
     * Retrieve an account by his unique email
     *
     * @param pAccountEmail
     *            email of the account to retrieve
     * @return Account
     */
    @RequestMapping(value = "/account/{account_email}", method = RequestMethod.GET)
    ResponseEntity<EntityModel<Account>> retrieveAccounByEmail(@PathVariable("account_email") String pAccountEmail);

    /**
     * Update an {@link Account} with passed values.
     *
     * @param pAccountId
     *            The <code>id</code> of the {@link Account} to update
     * @param pUpdatedAccount
     *            The new values to set
     */
    @RequestMapping(value = "/{account_id}", method = RequestMethod.PUT)
    ResponseEntity<EntityModel<Account>> updateAccount(@PathVariable("account_id") Long pAccountId,
            @Valid @RequestBody Account pUpdatedAccount);

    /**
     * Remove on {@link Account} from db.<br>
     * Only remove if no project user for any tenant.
     *
     * @param pAccountId
     *            The account <code>id</code>
     */
    @RequestMapping(value = "/{account_id}", method = RequestMethod.DELETE)
    ResponseEntity<Void> removeAccount(@PathVariable("account_id") Long pAccountId);

    /**
     * Do not respect REST architecture because the request comes from a mail client, ideally should be a PUT
     *
     * @param pAccountId
     *            The account id
     * @param pUnlockCode
     *            the unlock code
     * @return void
     */
    @RequestMapping(value = "/{account_id}/unlock/{unlock_code}", method = RequestMethod.GET)
    ResponseEntity<Void> unlockAccount(@PathVariable("account_id") Long pAccountId,
            @PathVariable("unlock_code") String pUnlockCode);

    /**
     * Send to the user an email containing a link with limited validity to reset its password.
     *
     * @param pAccountEmail
     *            The {@link Account}'s <code>email</code>
     * @param pDto
     *            The DTO containing<br>
     *            - The url of the app from where was issued the query<br>
     *            - The url to redirect the user to the password reset interface
     * @return void
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{account_email}/resetPassword", method = RequestMethod.POST)
    ResponseEntity<Void> requestResetPassword(@PathVariable("account_email") final String pAccountEmail,
            @Valid @RequestBody final RequestResetPasswordDto pDto);

    /**
     * Change the passord of an {@link Account}.
     *
     * @param pAccountEmail
     *            The {@link Account}'s <code>email</code>
     * @param pDto
     *            The DTO containing : 1) the token 2) the new password
     * @return void
     */
    @RequestMapping(value = "/{account_email}/resetPassword", method = RequestMethod.PUT)
    ResponseEntity<Void> performResetPassword(@PathVariable("account_email") final String pAccountEmail,
            @Valid @RequestBody final PerformResetPasswordDto pDto);

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
    @RequestMapping(value = "/code", method = RequestMethod.GET)
    ResponseEntity<Void> sendAccountCode(@RequestParam("email") String pEmail, @RequestParam("type") CodeType pType);

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
    @RequestMapping(value = "/{account_email}/validate", method = RequestMethod.GET)
    ResponseEntity<Boolean> validatePassword(@PathVariable("account_email") String pEmail,
            @RequestParam("password") String pPassword);

    /**
     * Grants access to the project user
     *
     * @param pAccountEmail
     *            account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(value = ACCEPT_ACCOUNT_RELATIVE_PATH, method = RequestMethod.PUT)
    ResponseEntity<Void> acceptAccount(@PathVariable("account_email") final String pAccountEmail);

    /**
     * Refuse the account request
     *
     * @param pAccountEmail
     *            account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @RequestMapping(value = REFUSE_ACCOUNT_RELATIVE_PATH, method = RequestMethod.PUT)
    ResponseEntity<Void> refuseAccount(@PathVariable("account_email") final String pAccountEmail);
}
