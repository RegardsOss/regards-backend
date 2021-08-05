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

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSearchParameters;
import fr.cnes.regards.modules.accessrights.instance.domain.CodeType;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.PerformResetPasswordDto;
import fr.cnes.regards.modules.accessrights.instance.domain.passwordreset.RequestResetPasswordDto;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

/**
 * Feign client for rs-admin Accounts controller.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */
@RestClient(name = "rs-admin-instance", contextId = "rs-admin-instance.accounts-client")
@RequestMapping(path = "/accounts", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IAccountsClient {

    String ACCEPT_ACCOUNT_RELATIVE_PATH = "/{account_email}/accept";
    String REFUSE_ACCOUNT_RELATIVE_PATH = "/{account_email}/refuse";

    /**
     * Retrieve the list of all {@link Account}s.
     *
     * @return The accounts list
     */
    @GetMapping
    ResponseEntity<PagedModel<EntityModel<Account>>> retrieveAccountList(
            @RequestParam AccountSearchParameters parameters, @RequestParam("page") int page, @RequestParam("size") int size
    );

    /**
     * Create a new account in state PENDING from the passed values
     *
     * @param accountNPassword The data transfer object containing values to create the account from
     * @return the created account
     */
    @PostMapping
    ResponseEntity<EntityModel<Account>> createAccount(@Valid @RequestBody AccountNPassword accountNPassword);

    /**
     * Retrieve the {@link Account} of passed <code>id</code>.
     *
     * @param id The {@link Account}'s <code>id</code>
     * @return The account
     */
    @GetMapping("/{account_id}")
    ResponseEntity<EntityModel<Account>> retrieveAccount(@PathVariable("account_id") Long id);

    /**
     * Retrieve an account by his unique email
     *
     * @param email email of the account to retrieve
     * @return Account
     */
    @GetMapping("/account/{account_email}")
    ResponseEntity<EntityModel<Account>> retrieveAccounByEmail(@PathVariable("account_email") String email);

    /**
     * Update an {@link Account} with passed values.
     *
     * @param id             The <code>id</code> of the {@link Account} to update
     * @param updatedAccount The new values to set
     */
    @PutMapping("/{account_id}")
    ResponseEntity<EntityModel<Account>> updateAccount(@PathVariable("account_id") Long id, @Valid @RequestBody Account updatedAccount);

    /**
     * Remove on {@link Account} from db.<br>
     * Only remove if no project user for any tenant.
     *
     * @param id The account <code>id</code>
     */
    @DeleteMapping("/{account_id}")
    ResponseEntity<Void> removeAccount(@PathVariable("account_id") Long id);

    /**
     * Do not respect REST architecture because the request comes from a mail client, ideally should be a PUT
     *
     * @param id         The account id
     * @param unlockCode the unlock code
     * @return void
     */
    @GetMapping("/{account_id}/unlock/{unlock_code}")
    ResponseEntity<Void> unlockAccount(@PathVariable("account_id") Long id, @PathVariable("unlock_code") String unlockCode);

    /**
     * Send to the user an email containing a link with limited validity to reset its password.
     *
     * @param email The {@link Account}'s <code>email</code>
     * @param dto   The DTO containing<br>
     *              - The url of the app from where was issued the query<br>
     *              - The url to redirect the user to the password reset interface
     * @return void
     * @throws EntityNotFoundException
     */
    @PostMapping("/{account_email}/resetPassword")
    ResponseEntity<Void> requestResetPassword(@PathVariable("account_email") String email, @Valid @RequestBody RequestResetPasswordDto dto);

    /**
     * Change the passord of an {@link Account}.
     *
     * @param email The {@link Account}'s <code>email</code>
     * @param dto   The DTO containing : 1) the token 2) the new password
     * @return void
     */
    @PutMapping("/{account_email}/resetPassword")
    ResponseEntity<Void> performResetPassword(@PathVariable("account_email") String email, @Valid @RequestBody PerformResetPasswordDto dto);

    /**
     * Send to the user an email containing a code:<br>
     * - to reset password<br>
     * - to unlock the account
     *
     * @param email The {@link Account}'s <code>email</code>
     * @param type  The type of code
     */
    @GetMapping("/code")
    ResponseEntity<Void> sendAccountCode(@RequestParam("email") String email, @RequestParam("type") CodeType type);

    /**
     * Return <code>true</code> if the passed <code>password</code> is equal to the one set on the {@link Account} of
     * passed <code>email</code>
     *
     * @param email    The {@link Account}'s <code>email</code>
     * @param password The password to check
     * @return <code>true</code> if the password is valid, else <code>false</code>
     */
    @GetMapping("/{account_email}/validate")
    ResponseEntity<Boolean> validatePassword(@PathVariable("account_email") String email, @RequestParam("password") String password);

    /**
     * Grants access to the project user
     *
     * @param email account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @PutMapping(ACCEPT_ACCOUNT_RELATIVE_PATH)
    ResponseEntity<Void> acceptAccount(@PathVariable("account_email") String email);

    /**
     * Refuse the account request
     *
     * @param email account email
     * @return <code>void</code> wrapped in a {@link ResponseEntity}
     */
    @PutMapping(REFUSE_ACCOUNT_RELATIVE_PATH)
    ResponseEntity<Void> refuseAccount(@PathVariable("account_email") String email);

    /**
     * Link a project to an account
     *
     * @param email   email of the account to link
     * @param project name of the project to link
     */
    @PutMapping("/{account_email}/link/{project}")
    ResponseEntity<Void> link(@PathVariable("account_email") String email, @PathVariable("project") String project);

    /**
     * Unlink a project from an account
     *
     * @param email email of the account to link
     * @param project      name of the project to link
     */
    @PutMapping("/{account_email}/unlink/{project}")
    ResponseEntity<Void> unlink(@PathVariable("account_email") String email, @PathVariable("project") String project);

}
