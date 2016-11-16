/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.InvalidValueException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.CodeType;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * {@link IAccountState} implementation.
 *
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 */
@Service
@InstanceTransactional
public class AccountService implements IAccountService {

    /**
     * Root admin user login
     */
    @Value("${regards.accounts.root.user.login}")
    private String rootAdminUserLogin;

    /**
     * Root admin user password
     */
    @Value("${regards.accounts.root.user.password}")
    private String rootAdminUserPassword;

    /**
     * CRUD repository handling {@link Account}s. Autowired by Spring.
     */
    private final IAccountRepository accountRepository;

    /**
     * Factory class for retrieving an account's state from its status
     */
    private final AccountStateFactory accountStateFactory;

    /**
     * Creates a new instance with passed deps
     *
     * @param pAccountRepository
     *            The account repository
     * @param pAccountStateFactory
     *            The account state factory
     */
    public AccountService(final IAccountRepository pAccountRepository, final AccountStateFactory pAccountStateFactory) {
        super();
        accountRepository = pAccountRepository;
        accountStateFactory = pAccountStateFactory;
    }

    @PostConstruct
    public void initialize() throws AlreadyExistingException {
        if (!this.existAccount(rootAdminUserLogin)) {
            createAccount(new Account(rootAdminUserLogin, rootAdminUserLogin, rootAdminUserLogin,
                    rootAdminUserPassword));
        }
    }

    @Override
    public List<Account> retrieveAccountList() {
        return accountRepository.findAll();
    }

    @Override
    public Account createAccount(final Account pNewAccount) throws AlreadyExistingException {
        if (existAccount(pNewAccount.getEmail())) {
            throw new AlreadyExistingException(pNewAccount.getEmail());
        }
        final Account toCreate = new Account(pNewAccount.getEmail(), pNewAccount.getFirstName(),
                pNewAccount.getLastName(), pNewAccount.getPassword());
        return accountRepository.save(toCreate);
    }

    @Override
    public boolean existAccount(final Long pId) {
        return accountRepository.exists(pId);
    }

    @Override
    public Account retrieveAccount(final Long pAccountId) throws ModuleEntityNotFoundException {
        final Optional<Account> account = Optional.ofNullable(accountRepository.findOne(pAccountId));
        return account.orElseThrow(() -> new ModuleEntityNotFoundException(pAccountId.toString(), Account.class));
    }

    @Override
    public void updateAccount(final Long pAccountId, final Account pUpdatedAccount)
            throws ModuleEntityNotFoundException, InvalidValueException {
        if (!pUpdatedAccount.getId().equals(pAccountId)) {
            throw new InvalidValueException("Account id specified differs from updated account id");
        }
        if (!existAccount(pAccountId)) {
            throw new ModuleEntityNotFoundException(pAccountId.toString(), Account.class);
        }
        accountRepository.save(pUpdatedAccount);
    }

    @Override
    public void sendAccountCode(final String pEmail, final CodeType pType) throws ModuleEntityNotFoundException {
        if (!existAccount(pEmail)) {
            throw new ModuleEntityNotFoundException(pEmail, Account.class);
        }
        final Account account = retrieveAccountByEmail(pEmail);
        // TODO: sendEmail(pEmail,account.getCode();
    }

    @Override
    public void changeAccountPassword(final Long pAccountId, final String pResetCode, final String pNewPassword)
            throws ModuleEntityNotFoundException, InvalidValueException {
        final Account account = retrieveAccount(pAccountId);
        checkCode(account, pResetCode);
        account.setPassword(pNewPassword);
        accountRepository.save(account);
    }

    @Override
    public Account retrieveAccountByEmail(final String pEmail) throws ModuleEntityNotFoundException {
        final Account account = accountRepository.findOneByEmail(pEmail);
        if (account == null) {
            throw new ModuleEntityNotFoundException(pEmail, Account.class);
        }
        return account;
    }

    @Override
    public boolean validatePassword(final String pEmail, final String pPassword) throws ModuleEntityNotFoundException {
        final Optional<Account> account = Optional.ofNullable(accountRepository.findOneByEmail(pEmail));
        return account.orElseThrow(() -> new ModuleEntityNotFoundException(pEmail, Account.class)).getPassword()
                .equals(pPassword);
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

    /**
     * Check the passed code matches the code set on the passed account.
     *
     * @param pAccount
     *            The account
     * @param pCode
     *            The code to check
     * @throws InvalidValueException
     *             Thrown if the passed code differs from the one set on the account
     */
    private void checkCode(final Account pAccount, final String pCode) throws InvalidValueException {
        if (!pAccount.getCode().equals(pCode)) {
            throw new InvalidValueException("this is not the right code");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.account.IAccountState#makeAdminDecision(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void makeAdminDecision(final Account pAccount) throws IllegalActionForAccountStatusException {
        accountStateFactory.createState(pAccount).emailValidation(pAccount);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountState#emailValidation(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void emailValidation(final Account pAccount) throws IllegalActionForAccountStatusException {
        accountStateFactory.createState(pAccount).emailValidation(pAccount);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountState#lockAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void lockAccount(final Account pAccount) throws IllegalActionForAccountStatusException {
        accountStateFactory.createState(pAccount).lockAccount(pAccount);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountState#unlockAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void unlockAccount(final Account pAccount, final String pUnlockCode)
            throws IllegalActionForAccountStatusException, InvalidValueException {
        accountStateFactory.createState(pAccount).unlockAccount(pAccount, pUnlockCode);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountState#inactiveAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void inactiveAccount(final Account pAccount) throws IllegalActionForAccountStatusException {
        accountStateFactory.createState(pAccount).inactiveAccount(pAccount);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountState#activeAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public void activeAccount(final Account pAccount) throws IllegalActionForAccountStatusException {
        accountStateFactory.createState(pAccount).activeAccount(pAccount);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.account.IAccountState#delete(fr.cnes.regards.modules.accessrights.
     * domain.instance.Account)
     */
    @Override
    public void delete(final Account pAccount) throws ModuleException {
        accountStateFactory.createState(pAccount).delete(pAccount);
    }

}
