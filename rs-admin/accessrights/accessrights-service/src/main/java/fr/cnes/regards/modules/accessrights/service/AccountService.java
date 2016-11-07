/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.CodeType;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
import fr.cnes.regards.modules.core.exception.InvalidValueException;

/**
 * {@link IAccountService} implementation.
 *
 * @author Xavier-Alexandre Brochard
 * @auhtor Sébastien Binda
 */
@Service
@InstanceTransactional
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
     * CRUD repository handling {@link Account}s. Autowired by Spring.
     */
    private final IAccountRepository accountRepository;

    /**
     * Service managing {@link ProjectUser}s. Autowired by Spring.
     */
    private final IProjectUserService projectUserService;

    /**
     * Tenant resolver. Autowired by Spring.
     */
    private final ITenantResolver tenantResolver;

    /**
     * JWT Service. Autowired by Spring.
     */
    private final JWTService jwtService;

    /**
     * Creates a new instance with passed deps
     *
     * @param pAccountRepository
     *            The account repository
     * @param pProjectUserService
     *            The project user service
     * @param pTenantResolver
     *            The tenant resolver
     * @param pJwtService
     *            The jwt service
     */
    public AccountService(final IAccountRepository pAccountRepository, final IProjectUserService pProjectUserService,
            final ITenantResolver pTenantResolver, final JWTService pJwtService) {
        super();
        accountRepository = pAccountRepository;
        projectUserService = pProjectUserService;
        tenantResolver = pTenantResolver;
        jwtService = pJwtService;
    }

    @PostConstruct
    public void initialize() throws AlreadyExistingException {
        if (!this.existAccount(rootAdminUserLogin)) {
            this.createAccount(new Account(rootAdminUserLogin, rootAdminUserLogin, rootAdminUserLogin,
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
        return accountRepository.save(pNewAccount);
    }

    @Override
    public boolean existAccount(final Long pId) {
        return accountRepository.exists(pId);
    }

    @Override
    public Account retrieveAccount(final Long pAccountId) throws EntityNotFoundException {
        return accountRepository.findOne(pAccountId);
    }

    @Override
    public void updateAccount(final Long pAccountId, final Account pUpdatedAccount)
            throws EntityNotFoundException, InvalidValueException {
        if (!existAccount(pAccountId)) {
            throw new EntityNotFoundException(pAccountId.toString(), Account.class);
        }
        if (!pUpdatedAccount.getId().equals(pAccountId)) {
            throw new InvalidValueException("Account id specified differs from updated account id");
        }
        save(pUpdatedAccount);
    }

    @Override
    public void removeAccount(final Long pAccountId) throws EntityException {
        final Account account = accountRepository.findOne(pAccountId);

        // Silently fail + shortcut if the account does not exist
        if (account == null) {
            LOG.info("Tried to remove a not existing account of id " + pAccountId);
            return;
        }

        // Get all tenants
        final Set<String> tenants = tenantResolver.getAllTenants();

        // Define inject tenant consumer
        final Consumer<? super String> injectTenant = t -> {
            try {
                // TODO: User role system
                jwtService.injectToken(t, "");
                LOG.info("Injected tenant " + t);
            } catch (final JwtException e) {
                LOG.info("Could not inject tenant " + t, e);
            }
        };

        // Predicate: is there a project user associated to the account on this tenant?
        final Predicate<? super String> hasProjectUser = t -> {
            LOG.info("Evaluated predicate with tenant " + t);
            return projectUserService.existUser(account.getEmail());
        };

        try (Stream<String> stream = tenants.stream()) {
            if (stream.peek(injectTenant).anyMatch(hasProjectUser)) {
                accountRepository.delete(pAccountId);
            } else {
                throw new EntityException(
                        "Cannot remove account of id " + pAccountId + " because it is linked to project users.");
            }
        }
    }

    @Override
    public void sendAccountCode(final String pEmail, final CodeType pType) throws EntityNotFoundException {
        if (!existAccount(pEmail)) {
            throw new EntityNotFoundException(pEmail, Account.class);
        }
        final String code = generateCode(pType);
        final Account account = retrieveAccountByEmail(pEmail);
        account.setCode(code);
        save(account);
        // TODO: sendEmail(pEmail,code);
    }

    @Override
    public void unlockAccount(final Long pAccountId, final String pUnlockCode)
            throws InvalidValueException, EntityNotFoundException {
        final Account toUnlock = retrieveAccount(pAccountId);

        // Check it is effectively locked
        if (!AccountStatus.LOCKED.equals(toUnlock.getStatus())) {
            throw new InvalidValueException("Account is not locked");
        }

        // Check code
        check(toUnlock, pUnlockCode);

        // Change status
        toUnlock.setStatus(AccountStatus.ACTIVE);
        save(toUnlock);
    }

    @Override
    public void changeAccountPassword(final Long pAccountId, final String pResetCode, final String pNewPassword)
            throws EntityNotFoundException, InvalidValueException {
        final Account account = retrieveAccount(pAccountId);
        check(account, pResetCode);
        account.setPassword(pNewPassword);
        save(account);
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

    /**
     * Save the account
     *
     * @param pAccount
     *            The account to save
     */
    private void save(final Account pAccount) {
        accountRepository.save(pAccount);
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
    private void check(final Account pAccount, final String pCode) throws InvalidValueException {
        if (!pAccount.getCode().equals(pCode)) {
            throw new InvalidValueException("this is not the right code");
        }
    }

    /**
     * Generate an unlock code for the account
     *
     * @param pType
     *            The type
     * @return The code
     */
    private String generateCode(final CodeType pType) {
        return pType.toString() + "-" + UUID.randomUUID().toString();
    }
}
