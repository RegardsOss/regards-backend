/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.fallback;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.InvalidEntityException;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.domain.CodeType;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.instance.AccountSettings;

/**
 *
 * Class AccountsFallback
 *
 * Fallback for Accounts Feign client. This implementation is used in case of error during feign client calls.
 *
 * @author Sébastien Binda
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

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccountsSignature#retrieveAccountList()
     */
    @Override
    public ResponseEntity<List<Resource<Account>>> retrieveAccountList() {
        LOG.error(fallBackErrorMessage);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccountsSignature#createAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public ResponseEntity<Resource<Account>> createAccount(final Account pNewAccount)
            throws AlreadyExistingException, InvalidEntityException {
        LOG.error(fallBackErrorMessage);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccountsSignature#retrieveAccount(java.lang.Long)
     */
    @Override
    public ResponseEntity<Resource<Account>> retrieveAccount(final Long pAccountId)
            throws ModuleEntityNotFoundException {
        LOG.error(fallBackErrorMessage);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccountsSignature#updateAccount(java.lang.Long,
     * fr.cnes.regards.modules.accessrights.domain.instance.Account)
     */
    @Override
    public ResponseEntity<Void> updateAccount(final Long pAccountId, final Account pUpdatedAccount)
            throws ModuleEntityNotFoundException, InvalidValueException {
        LOG.error(fallBackErrorMessage);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccountsSignature#removeAccount(java.lang.Long)
     */
    @Override
    public ResponseEntity<Void> removeAccount(final Long pAccountId) throws EntityException {
        LOG.error(fallBackErrorMessage);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccountsSignature#unlockAccount(java.lang.Long,
     * java.lang.String)
     */
    @Override
    public ResponseEntity<Void> unlockAccount(final Long pAccountId, final String pUnlockCode)
            throws InvalidValueException, ModuleEntityNotFoundException {
        LOG.error(fallBackErrorMessage);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccountsSignature#changeAccountPassword(java.lang.Long,
     * java.lang.String, java.lang.String)
     */
    @Override
    public ResponseEntity<Void> changeAccountPassword(final Long pAccountId, final String pResetCode,
            final String pNewPassword) throws InvalidValueException, ModuleEntityNotFoundException {
        LOG.error(fallBackErrorMessage);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccountsSignature#sendAccountCode(java.lang.String,
     * fr.cnes.regards.modules.accessrights.domain.CodeType)
     */
    @Override
    public ResponseEntity<Void> sendAccountCode(final String pEmail, final CodeType pType)
            throws ModuleEntityNotFoundException {
        LOG.error(fallBackErrorMessage);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccountsSignature#retrieveAccountSettings()
     */
    @Override
    public ResponseEntity<Resource<AccountSettings>> retrieveAccountSettings() {
        LOG.error(fallBackErrorMessage);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.signature.IAccountsSignature#updateAccountSetting(fr.cnes.regards.modules.
     * accessrights.domain.instance.AccountSettings)
     */
    @Override
    public ResponseEntity<Void> updateAccountSetting(final AccountSettings pUpdatedAccountSetting) {
        LOG.error(fallBackErrorMessage);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccountsSignature#validatePassword(java.lang.String,
     * java.lang.String)
     */
    @Override
    public ResponseEntity<Void> validatePassword(final String pLogin, final String pPassword)
            throws ModuleEntityNotFoundException {
        LOG.error(fallBackErrorMessage);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.signature.IAccountsSignature#retrieveAccounByEmail(java.lang.String)
     */
    @Override
    public ResponseEntity<Resource<Account>> retrieveAccounByEmail(String pAccountEmail) {
        LOG.error(fallBackErrorMessage);
        return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
    }

}
