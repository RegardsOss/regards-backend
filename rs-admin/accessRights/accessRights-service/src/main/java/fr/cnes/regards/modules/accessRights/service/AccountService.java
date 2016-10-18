/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.naming.OperationNotSupportedException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessRights.domain.CodeType;
import fr.cnes.regards.modules.accessRights.domain.instance.Account;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@Service
public class AccountService implements IAccountService {

    @Value("${regards.accounts.root.user.login}")
    private String rootAdminUserLogin;

    @Value("${regards.accounts.root.user.password}")
    private String rootAdminUserPassword;

    @Autowired
    private IAccountRepository accountRepository;

    @PostConstruct
    public void initialize() throws AlreadyExistingException {
        if (!this.existAccount(rootAdminUserLogin)) {
            this.createAccount(new Account(rootAdminUserLogin, rootAdminUserLogin, rootAdminUserLogin,
                    rootAdminUserPassword));
        }
    }

    @Override
    public List<Account> retrieveAccountList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Account createAccount(final Account pNewAccount) throws AlreadyExistingException {
        if (existAccount(pNewAccount.getEmail())) {
            throw new AlreadyExistingException(pNewAccount.getEmail());
        }
        return accountRepository.save(pNewAccount);
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
    public Account retrieveAccount(final Long pAccountId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateAccount(final Long pAccountId, final Account pUpdatedAccount)
            throws OperationNotSupportedException {
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
    public Account retrieveAccountByEmail(final String pEmail) throws EntityNotFoundException {
        final Account account = accountRepository.findOneByEmail(pEmail);
        if (account == null) {
            throw new EntityNotFoundException(pEmail, Account.class);
        }
        return account;

    }

    @Override
    public boolean validatePassword(final String pEmail, final String pPassword) {
        boolean valid = false;
        final Account account = accountRepository.findOneByEmail(pEmail);
        if ((account != null) && account.getPassword().equals(pPassword)) {
            valid = true;
        }
        return valid;
    }

    @Override
    public boolean existAccount(final String pEmail) {
        boolean exists = false;
        final Account account = accountRepository.findOneByEmail(pEmail);
        if (account != null) {
            exists = true;
        }
        return exists;

    }

    @Override
    public Account retrieveAccountByLogin(final String pLogin) {
        // TODO Auto-generated method stub
        return null;
    }

}
