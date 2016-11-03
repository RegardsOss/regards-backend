/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.CodeType;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 * {@link IAccountService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
public class AccountService implements IAccountService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);

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
     * CRUD repository handling {@link Account}s
     */
    @Autowired
    private IAccountRepository accountRepository;

    @Autowired
    private IProjectUserService projectUserService;

    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private JWTService jwtService;

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
            throws InvalidValueException, EntityNotFoundException {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAccount(final Long pAccountId) {
        final Account account = accountRepository.findOne(pAccountId);
        final Set<String> tenants = tenantResolver.getAllTenants();

        // Define inject tenant consumer
        final Consumer<? super String> injectTenant = t -> {
            try {
                // TODO: User role system
                jwtService.injectToken(t, "");
            } catch (final JwtException e) {
                LOG.info("Could not inject tenant " + t, e);
            }
        };

        // Predicate: is there a project user associated to the account on this tenant?
        final Predicate<? super String> hasProjectUser = t -> {
            boolean result = false;
            try {
                result = projectUserService.retrieveOneByEmail(account.getEmail()) != null;
            } catch (final EntityNotFoundException e) {
                LOG.info("No user with email " + account.getEmail() + "for tenant " + t, e);
            }
            return result;
        };

        try (Stream<String> stream = tenants.stream()) {
            if (stream.peek(injectTenant).anyMatch(hasProjectUser)) {
                accountRepository.delete(pAccountId);
            }
        }
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
    public Account retrieveAccountByLogin(final String pLogin) throws EntityNotFoundException {
        // TODO Auto-generated method stub
        return null;
    }

}
