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
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.CodeType;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * {@link IAccountService} implementation.
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
     * Creates a new instance with passed deps
     *
     * @param pAccountRepository
     *            The account repository
     */
    public AccountService(final IAccountRepository pAccountRepository) {
        super();
        accountRepository = pAccountRepository;
    }

    @PostConstruct
    public void initialize() {
        if (!this.existAccount(rootAdminUserLogin)) {
            accountRepository.save(new Account(rootAdminUserLogin, rootAdminUserLogin, rootAdminUserLogin,
                    rootAdminUserPassword));
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#retrieveAccountList()
     */
    @Override
    public List<Account> retrieveAccountList() {
        return accountRepository.findAll();
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#existAccount(java.lang.Long)
     */
    @Override
    public boolean existAccount(final Long pId) {
        return accountRepository.exists(pId);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#retrieveAccount(java.lang.Long)
     */
    @Override
    public Account retrieveAccount(final Long pAccountId) throws EntityNotFoundException {
        final Optional<Account> account = Optional.ofNullable(accountRepository.findOne(pAccountId));
        return account.orElseThrow(() -> new EntityNotFoundException(pAccountId.toString(), Account.class));
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#updateAccount(java.lang.Long,
     * fr.cnes.regards.modules.accessrights.domain.instance.Account)
     */
    @Override
    public void updateAccount(final Long pAccountId, final Account pUpdatedAccount) throws EntityException {
        if (!pUpdatedAccount.getId().equals(pAccountId)) {
            throw new EntityInconsistentIdentifierException(pAccountId, pUpdatedAccount.getId(), Account.class);
        }
        if (!existAccount(pAccountId)) {
            throw new EntityNotFoundException(pAccountId.toString(), Account.class);
        }
        accountRepository.save(pUpdatedAccount);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#sendAccountCode(java.lang.String,
     * fr.cnes.regards.modules.accessrights.domain.CodeType)
     */
    @Override
    public void sendAccountCode(final String pEmail, final CodeType pType) throws EntityNotFoundException {
        if (!existAccount(pEmail)) {
            throw new EntityNotFoundException(pEmail, Account.class);
        }
        final Account account = retrieveAccountByEmail(pEmail);
        // TODO: sendEmail(pEmail,account.getCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#changeAccountPassword(java.lang.Long,
     * java.lang.String, java.lang.String)
     */
    @Override
    public void changeAccountPassword(final Long pAccountId, final String pResetCode, final String pNewPassword)
            throws EntityException {
        final Account account = retrieveAccount(pAccountId);
        checkCode(account, pResetCode);
        account.setPassword(pNewPassword);
        accountRepository.save(account);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.accessrights.service.account.IAccountService#retrieveAccountByEmail(java.lang.String)
     */
    @Override
    public Account retrieveAccountByEmail(final String pEmail) throws EntityNotFoundException {
        return accountRepository.findOneByEmail(pEmail)
                .orElseThrow(() -> new EntityNotFoundException(pEmail, Account.class));
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#validatePassword(java.lang.String,
     * java.lang.String)
     */
    @Override
    public boolean validatePassword(final String pEmail, final String pPassword) throws EntityNotFoundException {
        return accountRepository.findOneByEmail(pEmail)
                .orElseThrow(() -> new EntityNotFoundException(pEmail, Account.class)).getPassword().equals(pPassword);
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#existAccount(java.lang.String)
     */
    @Override
    public boolean existAccount(final String pEmail) {
        return accountRepository.findOneByEmail(pEmail).isPresent();
    }

    /**
     * Check the passed code matches the code set on the passed account.
     *
     * @param pAccount
     *            The account
     * @param pCode
     *            The code to check
     * @throws EntityOperationForbiddenException
     *             Thrown if the passed code differs from the one set on the account
     */
    private void checkCode(final Account pAccount, final String pCode) throws EntityOperationForbiddenException {
        if (!pAccount.getCode().equals(pCode)) {
            throw new EntityOperationForbiddenException(pAccount.getId().toString(), Account.class, "Incorrect code");
        }
    }

}
