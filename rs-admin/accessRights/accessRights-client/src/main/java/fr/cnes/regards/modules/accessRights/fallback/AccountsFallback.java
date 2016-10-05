/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.fallback;

import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.OperationNotSupportedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.accessRights.client.AccountsClient;
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.CodeType;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@Component
public class AccountsFallback implements AccountsClient {

    private static final Logger LOG = LoggerFactory.getLogger(AccountsFallback.class);

    private static final String fallBackErrorMessage_ = "RS-ADMIN /accounts request error. Fallback.";

    @Override
    public HttpEntity<List<Resource<Account>>> retrieveAccountList() {
        LOG.error(fallBackErrorMessage_);
        return null;
    }

    @Override
    public HttpEntity<Resource<Account>> createAccount(Account pNewAccount) throws AlreadyExistingException {
        LOG.error(fallBackErrorMessage_);
        return null;
    }

    @Override
    public HttpEntity<Resource<Account>> retrieveAccount(Long pAccountId) {
        LOG.error(fallBackErrorMessage_);
        return null;
    }

    @Override
    public HttpEntity<Void> updateAccount(Long pAccountId, Account pUpdatedAccount)
            throws OperationNotSupportedException {
        LOG.error(fallBackErrorMessage_);
        return null;
    }

    @Override
    public HttpEntity<Void> removeAccount(Long pAccountId) {
        LOG.error(fallBackErrorMessage_);
        return null;
    }

    @Override
    public HttpEntity<Void> unlockAccount(Long pAccountId, String pUnlockCode) throws InvalidValueException {
        LOG.error(fallBackErrorMessage_);
        return null;
    }

    @Override
    public HttpEntity<Void> changeAccountPassword(Long pAccountId, String pResetCode, String pNewPassword)
            throws InvalidValueException {
        LOG.error(fallBackErrorMessage_);
        return null;
    }

    @Override
    public HttpEntity<Void> codeForAccount(String pEmail, CodeType pType) {
        LOG.error(fallBackErrorMessage_);
        return null;
    }

    @Override
    public HttpEntity<List<Resource<String>>> retrieveAccountSettings() {
        LOG.error(fallBackErrorMessage_);
        return null;
    }

    @Override
    public HttpEntity<Void> updateAccountSetting(String pUpdatedAccountSetting) throws InvalidValueException {
        LOG.error(fallBackErrorMessage_);
        return null;
    }

    @Override
    public HttpEntity<Boolean> validatePassword(String pLogin, String pPassword) throws NoSuchElementException {
        LOG.error(fallBackErrorMessage_);
        return null;
    }

}
