/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.fallback;

import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.OperationNotSupportedException;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;

import fr.cnes.regards.modules.accessRights.client.AccountsClient;
import fr.cnes.regards.modules.accessRights.domain.Account;
import fr.cnes.regards.modules.accessRights.domain.CodeType;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

public class AccountsFallback implements AccountsClient {

    @Override
    public HttpEntity<List<Resource<Account>>> retrieveAccountList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Resource<Account>> createAccount(Account pNewAccount) throws AlreadyExistingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Resource<Account>> retrieveAccount(Long pAccountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> updateAccount(Long pAccountId, Account pUpdatedAccount)
            throws OperationNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> removeAccount(Long pAccountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> unlockAccount(Long pAccountId, String pUnlockCode) throws InvalidValueException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> changeAccountPassword(Long pAccountId, String pResetCode, String pNewPassword)
            throws InvalidValueException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> codeForAccount(String pEmail, CodeType pType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<List<Resource<String>>> retrieveAccountSettings() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Void> updateAccountSetting(String pUpdatedAccountSetting) throws InvalidValueException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpEntity<Boolean> validatePassword(String pLogin, String pPassword) throws NoSuchElementException {
        // TODO Auto-generated method stub
        return null;
    }

}
