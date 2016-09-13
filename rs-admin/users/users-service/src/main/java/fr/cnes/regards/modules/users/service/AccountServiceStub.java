package fr.cnes.regards.modules.users.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;
import fr.cnes.regards.modules.users.domain.Account;
import fr.cnes.regards.modules.users.domain.CodeType;

@Service
public class AccountServiceStub implements IAccountService {

    private static List<Account> accounts = new ArrayList<>();

    @Value("${regards.instance.account_acceptance}")
    private String accountSetting;

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
        String code = generateCode(pType);
        // TODO: sendEmail(pEmail,code);
    }

    private String generateCode(CodeType pType) {
        return UUID.randomUUID().toString();
    }

    @Override
    public void unlockAccount(String pAccountId, String pUnlockCode) {
        Account toUnlock = this.retrieveAccount(pAccountId);
        // TODO: check unlockCode
        toUnlock.unlock();

    }

    @Override
    public void changeAccountPassword(String pAccountId, String pResetCode, String pNewPassword) {
        Account account = this.retrieveAccount(pAccountId);
        // TODO: check resetCode
        account.setPassword(pNewPassword);
    }

    @Override
    public List<String> retrieveAccountSettings() {
        List<String> accountSettings = new ArrayList<>();
        accountSettings.add(this.accountSetting);
        return accountSettings;
    }

    @Override
    public void updateAccountSetting(String pUpdatedAccountSetting) throws InvalidValueException {
        if (pUpdatedAccountSetting.toLowerCase().equals("manual") || pUpdatedAccountSetting.equals("auto-accept")) {
            this.accountSetting = pUpdatedAccountSetting.toLowerCase();
            return;
        }
        throw new InvalidValueException("Only value accepted : manual or auto-accept");
    }

    public boolean existAccount(String pEmail) {
        return accounts.stream().filter(p -> p.getEmail().equals(pEmail)).findFirst().isPresent();
    }
}
