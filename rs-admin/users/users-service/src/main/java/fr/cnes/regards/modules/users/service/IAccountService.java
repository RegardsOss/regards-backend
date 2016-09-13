package fr.cnes.regards.modules.users.service;

import java.util.List;

import javax.naming.OperationNotSupportedException;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;
import fr.cnes.regards.modules.users.domain.Account;
import fr.cnes.regards.modules.users.domain.CodeType;

public interface IAccountService {

    List<Account> retrieveAccountList();

    Account createAccount(Account pNewAccount) throws AlreadyExistingException;

    Account retrieveAccount(String pAccountId);

    void updateAccount(String pAccountId, Account pUpdatedAccount) throws OperationNotSupportedException;

    void removeAccount(String pAccountId);

    void codeForAccount(String pEmail, CodeType pType);

    void unlockAccount(String pAccountId, String pUnlockCode);

    void changeAccountPassword(String pAccountId, String pResetCode, String pNewPassword);

    List<String> retrieveAccountSettings();

    void updateAccountSetting(String pUpdatedAccountSetting) throws InvalidValueException;

    /**
     * @param pEmail
     * @return
     */
    boolean existAccount(String pEmail);

}
