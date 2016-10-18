/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.domain.CodeType;
import fr.cnes.regards.modules.accessRights.domain.instance.Account;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@Service
public class AccountService implements IAccountService {

    @Override
    public List<Account> retrieveAccountList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account createAccount(final Account pNewAccount) throws AlreadyExistingException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> retrieveAccountSettings() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateAccountSetting(final String pUpdatedAccountSetting) throws InvalidValueException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean existAccount(final Long pId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Account retrieveAccount(final Long pAccountId) throws EntityNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateAccount(final Long pAccountId, final Account pUpdatedAccount)
            throws InvalidValueException, EntityNotFoundException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAccount(final Long pAccountId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void codeForAccount(final String pAccountEmail, final CodeType pType) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unlockAccount(final Long pAccountId, final String pUnlockCode) throws InvalidValueException {
        // TODO Auto-generated method stub

    }

    @Override
    public void changeAccountPassword(final Long pAccountId, final String pResetCode, final String pNewPassword)
            throws InvalidValueException {
        // TODO Auto-generated method stub

    }

    @Override
    public Account retrieveAccountByEmail(final String pEmail) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean validatePassword(final String pLogin, final String pPassword) throws EntityNotFoundException {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean existAccount(final String pLogin) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Account retrieveAccountByLogin(final String pLogin) throws EntityNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

}
