/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.fallback;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.domain.CodeType;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 *
 * Class AccountsFallback
 *
 * Fallback for Accounts Feign client. This implementation is used in case of error during feign client calls.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Component
public class AccountsFallback implements IAccountsClient {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountsFallback.class);

    /**
     * Common error message to log
     */
    private static final String fallBackErrorMessage = "RS-ADMIN /accounts request error. Fallback.";

    @Override
    public ResponseEntity<List<Resource<Account>>> retrieveAccountList() {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Resource<Account>> createAccount(final Account pNewAccount) throws AlreadyExistingException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Resource<Account>> retrieveAccount(final Long pAccountId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Void> updateAccount(final Long pAccountId, final Account pUpdatedAccount)
            throws EntityNotFoundException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Void> removeAccount(final Long pAccountId) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Void> unlockAccount(final Long pAccountId, final String pUnlockCode)
            throws InvalidValueException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Void> changeAccountPassword(final Long pAccountId, final String pResetCode,
            final String pNewPassword) throws InvalidValueException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Void> codeForAccount(final String pEmail, final CodeType pType) {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<List<Resource<String>>> retrieveAccountSettings() {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Void> updateAccountSetting(final String pUpdatedAccountSetting) throws InvalidValueException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

    @Override
    public ResponseEntity<Boolean> validatePassword(final String pLogin, final String pPassword)
            throws EntityNotFoundException {
        LOG.error(fallBackErrorMessage);
        return null;
    }

}
