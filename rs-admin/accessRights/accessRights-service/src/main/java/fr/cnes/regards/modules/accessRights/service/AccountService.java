/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.domain.CodeType;
import fr.cnes.regards.modules.accessRights.domain.instance.Account;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@Service
public class AccountService implements IAccountService {

    @Override
    public List<Account> retrieveAccountList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account createAccount(Account pNewAccount) throws AlreadyExistingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> retrieveAccountSettings() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateAccountSetting(String pUpdatedAccountSetting) throws InvalidValueException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean existAccount(Long pId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Account retrieveAccount(Long pAccountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateAccount(Long pAccountId, Account pUpdatedAccount) throws OperationNotSupportedException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAccount(Long pAccountId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void codeForAccount(String pAccountEmail, CodeType pType) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unlockAccount(Long pAccountId, String pUnlockCode) throws InvalidValueException {
        // TODO Auto-generated method stub

    }

    @Override
    public void changeAccountPassword(Long pAccountId, String pResetCode, String pNewPassword)
            throws InvalidValueException {
        // TODO Auto-generated method stub

    }

    @Override
    public Account retrieveAccountByEmail(String pEmail) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean validatePassword(String pLogin, String pPassword) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean existAccount(String pLogin) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Account retrieveAccountByLogin(String pLogin) {
        // TODO Auto-generated method stub
        return null;
    }

}
