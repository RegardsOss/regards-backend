/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.registration.VerificationToken;

/**
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IRegistrationService {

    /**
     * Return the account linked to the passed verification token
     *
     * @param pVerificationToken
     *            the token
     * @return the account
     * @throws EntityNotFoundException
     *             if the token could not be found
     */
    Account getAccountByVerificationToken(String pVerificationToken) throws EntityNotFoundException;

    /**
     * Create a {@link VerificationToken} for the passed {@link Account}
     *
     * @param pAccount
     *            the account
     * @param pToken
     *            the token
     */
    void createVerificationToken(Account pAccount, String pToken);

    /**
     * Retrieve the verification token
     *
     * @param pVerificationToken
     *            the token
     * @return the token
     * @throws EntityNotFoundException
     *             if the token could not be foud
     */
    VerificationToken getVerificationToken(String pVerificationToken) throws EntityNotFoundException;
}
