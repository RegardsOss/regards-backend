/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessRights.service.stubs;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import fr.cnes.regards.modules.accessRights.domain.AccountStatus;
import fr.cnes.regards.modules.accessRights.domain.CodeType;
import fr.cnes.regards.modules.accessRights.domain.instance.Account;
import fr.cnes.regards.modules.accessRights.service.IAccountService;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

@Service
@Profile("test")
@Primary
public class AccountServiceStub implements IAccountService {

    private static List<Account> accounts = new ArrayList<>();

    @PostConstruct
    public void init() {
        accounts.add(new Account(0L, "instance_admin@cnes.fr", "firstName", "lastName", "instance_admin@cnes.fr",
                "password", AccountStatus.ACCEPTED, "code"));
        accounts.add(new Account(1L, "toto@toto.toto", "Toto", "toto", "toto@toto.toto", "mdp", AccountStatus.PENDING,
                "anotherCode"));
    }

    @Value("${regards.instance.account_acceptance}")
    private String accountSetting;

    @Override
    public List<Account> retrieveAccountList() {
        return accounts;
    }

    @Override
    public Account createAccount(final Account pNewAccount) throws AlreadyExistingException {
        if (existAccount(pNewAccount)) {
            throw new AlreadyExistingException(pNewAccount.getId() + "");
        }
        accounts.add(pNewAccount);
        return pNewAccount;
    }

    @Override
    public Account retrieveAccount(final Long pAccountId) throws EntityNotFoundException {
        return accounts.stream().filter(a -> a.getId() == pAccountId).findFirst()
                .orElseThrow(() -> new EntityNotFoundException(pAccountId.toString(), Account.class));
    }

    @Override
    public void updateAccount(final Long pAccountId, final Account pUpdatedAccount)
            throws InvalidValueException, EntityNotFoundException {
        if (existAccount(pAccountId)) {
            if (pUpdatedAccount.getId() == pAccountId) {
                accounts = accounts.stream().map(a -> a.getEmail().equals(pAccountId) ? pUpdatedAccount : a)
                        .collect(Collectors.toList());
                return;
            }
            throw new InvalidValueException("Account id specified differs from updated account id");
        }
        throw new EntityNotFoundException(pAccountId.toString(), Account.class);
    }

    @Override
    public void removeAccount(final Long pAccountId) {
        accounts = accounts.stream().filter(a -> a.getId() != pAccountId).collect(Collectors.toList());
    }

    @Override
    public void codeForAccount(final String pAccountEmail, final CodeType pType) {
        final String code = generateCode(pType);
        final Account account = this.retrieveAccountByEmail(pAccountEmail);
        account.setCode(code);
        // TODO: sendEmail(pEmail,code);
    }

    private String generateCode(final CodeType pType) {
        return UUID.randomUUID().toString();
    }

    @Override
    public void unlockAccount(final Long pAccountId, final String pUnlockCode)
            throws InvalidValueException, EntityNotFoundException {
        final Account toUnlock = this.retrieveAccount(pAccountId);
        check(toUnlock, pUnlockCode);
        toUnlock.unlock();

    }

    @Override
    public void changeAccountPassword(final Long pAccountId, final String pResetCode, final String pNewPassword)
            throws InvalidValueException, EntityNotFoundException {
        final Account account = this.retrieveAccount(pAccountId);
        check(account, pResetCode);
        account.setPassword(pNewPassword);
    }

    @Override
    public List<String> retrieveAccountSettings() {
        final List<String> accountSettings = new ArrayList<>();
        accountSettings.add(this.accountSetting);
        return accountSettings;
    }

    @Override
    public void updateAccountSetting(final String pUpdatedAccountSetting) throws InvalidValueException {
        if (pUpdatedAccountSetting.toLowerCase().equals("manual") || pUpdatedAccountSetting.equals("auto-accept")) {
            this.accountSetting = pUpdatedAccountSetting.toLowerCase();
            return;
        }
        throw new InvalidValueException("Only value accepted : manual or auto-accept");
    }

    @Override
    public boolean existAccount(final Long id) {
        return accounts.stream().filter(p -> p.getId() == id).findFirst().isPresent();
    }

    /**
     * @param pNewAccount
     * @return
     */
    private boolean existAccount(final Account pNewAccount) {
        return accounts.stream().filter(p -> p.equals(pNewAccount)).findFirst().isPresent();
    }

    /**
     * @param pLogin
     * @return
     */
    @Override
    public boolean existAccount(final String pLogin) {
        return accounts.stream().filter(p -> p.getLogin().equals(pLogin)).findFirst().isPresent();
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessRights.service.IAccountService#retrieveAccount(java.lang.String)
     */
    @Override
    public Account retrieveAccountByEmail(final String pEmail) {
        return accounts.stream().filter(p -> p.getEmail().equals(pEmail)).findFirst().get();
    }

    private void check(final Account account, final String pCode) throws InvalidValueException {
        if (!account.getCode().equals(pCode)) {
            throw new InvalidValueException("this is not the right code");
        }
    }

    @Override
    public boolean validatePassword(final String pLogin, final String pPassword) throws EntityNotFoundException {
        final Account account = retrieveAccountByLogin(pLogin);
        return account.getPassword().equals(pPassword);
    }

    @Override
    public Account retrieveAccountByLogin(final String pLogin) throws EntityNotFoundException {
        return accounts.stream().filter(p -> p.getLogin().equals(pLogin)).findFirst()
                .orElseThrow(() -> new EntityNotFoundException(pLogin, Account.class));
    }
}
