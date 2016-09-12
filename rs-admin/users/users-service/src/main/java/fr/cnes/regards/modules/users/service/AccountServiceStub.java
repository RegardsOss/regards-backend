package fr.cnes.regards.modules.users.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.naming.OperationNotSupportedException;

import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.users.domain.Account;
import fr.cnes.regards.modules.users.domain.AccountSetting;
import fr.cnes.regards.modules.users.domain.CodeType;

@Service
public class AccountServiceStub implements IAccountService {

    private static List<Account> accounts = new ArrayList<>();

    @Override
    public List<Account> retrieveAccountList() {
        return accounts;
    }

    @Override
    public Account createAccount(Account pNewAccount) throws AlreadyExistingException {
        if (existAccount(pNewAccount.getEmail())) {
            throw new AlreadyExistingException(pNewAccount.getEmail());
        }
        accounts.add(pNewAccount);
        return pNewAccount;
    }

    @Override
    public Account retrieveAccount(String pAccountId) {
        return accounts.stream().filter(a -> a.getEmail().equals(pAccountId)).findFirst().get();
    }

    @Override
    public void updateAccount(String pAccountId, Account pUpdatedAccount) throws OperationNotSupportedException {
        if (existAccount(pAccountId)) {
            if (pUpdatedAccount.getEmail().equals(pAccountId)) {
                accounts = accounts.stream().map(a -> a.getEmail().equals(pAccountId) ? pUpdatedAccount : a)
                        .collect(Collectors.toList());
                return;
            }
            throw new OperationNotSupportedException("Account id specified differs from updated account id");
        }
        throw new NoSuchElementException(pAccountId);
    }

    @Override
    public void removeAccount(String pAccountId) {
        accounts = accounts.stream().filter(a -> !a.getEmail().equals(pAccountId)).collect(Collectors.toList());
    }

    @Override
    public void codeForAccount(String pEmail, CodeType pType) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unlockAccount(String pAccountId, String pUnlockCode) {
        // TODO Auto-generated method stub

    }

    @Override
    public void changeAccountPassword(String pAccountId, String pResetCode, String pNewPassword) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<AccountSetting> retrieveAccountSettings() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateAccountSetting(AccountSetting pUpdatedAccountSetting) {
        // TODO Auto-generated method stub

    }

    public boolean existAccount(String pEmail) {
        return accounts.stream().filter(p -> p.getEmail().equals(pEmail)).findFirst().isPresent();
    }
}
