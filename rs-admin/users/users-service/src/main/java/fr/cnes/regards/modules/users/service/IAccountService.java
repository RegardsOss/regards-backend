package fr.cnes.regards.modules.users.service;

import java.util.List;

import fr.cnes.regards.modules.users.domain.Account;
import fr.cnes.regards.modules.users.domain.AccountSetting;
import fr.cnes.regards.modules.users.domain.CodeType;

public interface IAccountService {

    List<Account> retrieveAccountList();

    Account createAccount(Account pNewAccount);

    Account retrieveAccount(String pAccountId);

    void updateAccount(String pAccountId, Account pUpdatedAccount);

    void removeAccount(String pAccountId);

    void codeForAccount(String pEmail, CodeType pType);

    void unlockAccount(String pAccountId, String pUnlockCode);

    void changeAccountPassword(String pAccountId, String pResetCode, String pNewPassword);

    List<AccountSetting> retrieveAccountSettings();

    void updateAccountSetting(AccountSetting pUpdatedAccountSetting);

}
