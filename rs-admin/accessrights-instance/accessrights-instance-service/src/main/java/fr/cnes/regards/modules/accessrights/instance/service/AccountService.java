/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.instance.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.instance.service.encryption.EncryptionUtils;

/**
 * {@link IAccountService} implementation.
 * @author Xavier-Alexandre Brochard
 * @author Sébastien Binda
 * @author Christophe Mertz
 * @author Sylvain Vissiere-Guerinet
 */
@Service
@InstanceTransactional
@EnableScheduling
public class AccountService implements IAccountService {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);

    /**
     * Regex that the password should respect. Provided by property file.
     */
    private final String passwordRegex;

    /**
     * Associated Pattern
     */
    private final Pattern passwordRegexPattern;

    /**
     * Description of the regex to respect in natural language. Provided by property file. Parsed according to "\n" to transform it into a list
     */
    private final String passwordRules;

    /**
     * In days. Provided by property file.
     */
    private final Long accountPasswordValidityDuration;

    /**
     * In days. Provided by property file.
     */
    private final Long accountValidityDuration;

    /**
     * Root admin user login. Provided by property file.
     */
    private final String rootAdminUserLogin;

    /**
     * Root admin user password. Provided by property file.
     */
    private final String rootAdminUserPassword;

    /**
     * threshold of failed authentication above which an account should be locked. Provided by property file.
     */
    private final Long thresholdFailedAuthentication;

    /**
     * CRUD repository handling {@link Account}s. Autowired by Spring.
     */
    private final IAccountRepository accountRepository;

    /**
     * Instance tenant name
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Constructor
     * @param accountRepository account repository
     * @param passwordRegex password regex
     * @param passwordRules password rules
     * @param accountPasswordValidityDuration account password validity duration
     * @param accountValidityDuration account validity duration
     * @param rootAdminUserLogin root admin user login
     * @param rootAdminUserPassword root admin user password
     * @param thresholdFailedAuthentication threshold faild autentication
     * @param pRuntimeTenantResolver runtime tenant resolver
     */
    public AccountService(final IAccountRepository accountRepository, //NOSONAR
            @Value("${regards.accounts.password.regex}") final String passwordRegex,
            @Value("${regards.accounts.password.rules}") final String passwordRules,
            @Value("${regards.accounts.password.validity.duration}") final Long accountPasswordValidityDuration,
            @Value("${regards.accounts.validity.duration}") final Long accountValidityDuration,
            @Value("${regards.accounts.root.user.login}") final String rootAdminUserLogin,
            @Value("${regards.accounts.root.user.password}") final String rootAdminUserPassword,
            @Value("${regards.accounts.failed.authentication.max}") final Long thresholdFailedAuthentication,
            @Autowired final IRuntimeTenantResolver pRuntimeTenantResolver) {
        super();
        this.accountRepository = accountRepository;
        this.passwordRegex = passwordRegex;
        this.passwordRegexPattern = Pattern.compile(this.passwordRegex);
        this.passwordRules = passwordRules;
        this.accountPasswordValidityDuration = accountPasswordValidityDuration;
        this.accountValidityDuration = accountValidityDuration;
        this.rootAdminUserLogin = rootAdminUserLogin;
        this.rootAdminUserPassword = rootAdminUserPassword;
        this.thresholdFailedAuthentication = thresholdFailedAuthentication;
        this.runtimeTenantResolver = pRuntimeTenantResolver;
    }

    /**
     * Call after Spring successfully build the bean
     */
    @PostConstruct
    public void initialize() {
        if (!this.existAccount(rootAdminUserLogin)) {
            final Account account = new Account(rootAdminUserLogin, rootAdminUserLogin, rootAdminUserLogin,
                    rootAdminUserPassword);
            account.setStatus(AccountStatus.ACTIVE);
            account.setAuthenticationFailedCounter(0L);
            account.setExternal(false);
            createAccount(account);
        }
    }

    @Override
    public Page<Account> retrieveAccountList(final Pageable pPageable) {
        return accountRepository.findAll(pPageable);
    }

    @Override
    public Page<Account> retrieveAccountList(final AccountStatus pStatus, final Pageable pPageable) {
        return accountRepository.findAllByStatus(pStatus, pPageable);
    }

    @Override
    public boolean existAccount(final Long pId) {
        return accountRepository.exists(pId);
    }

    @Override
    public Account createAccount(final Account account) {
        account.setId(null);
        if (account.getPassword() != null) {
            account.setPassword(EncryptionUtils.encryptPassword(account.getPassword()));
        }
        account.setInvalidityDate(LocalDateTime.now().plusDays(accountValidityDuration));
        return accountRepository.save(account);
    }

    @Override
    public Account retrieveAccount(final Long pAccountId) throws EntityNotFoundException {
        final Optional<Account> account = Optional.ofNullable(accountRepository.findOne(pAccountId));
        return account.orElseThrow(() -> new EntityNotFoundException(pAccountId.toString(), Account.class));
    }

    @Override
    public Account updateAccount(final Long pAccountId, final Account pUpdatedAccount) throws EntityException {
        final Account account = accountRepository.findOne(pAccountId);
        if (account == null) {
            throw new EntityNotFoundException(pAccountId.toString(), Account.class);
        }
        if (!pUpdatedAccount.getId().equals(pAccountId)) {
            throw new EntityInconsistentIdentifierException(pAccountId, pUpdatedAccount.getId(), Account.class);
        }
        account.setFirstName(pUpdatedAccount.getFirstName());
        account.setLastName(pUpdatedAccount.getLastName());
        account.setStatus(pUpdatedAccount.getStatus());
        return accountRepository.save(account);
    }

    @Override
    public Account retrieveAccountByEmail(final String pEmail) throws EntityNotFoundException {
        return accountRepository.findOneByEmail(pEmail)
                .orElseThrow(() -> new EntityNotFoundException(pEmail, Account.class));
    }

    @Override
    public boolean validatePassword(final String email, final String password, boolean checkAccountValidity)
            throws EntityNotFoundException {

        final Optional<Account> toValidate = accountRepository.findOneByEmail(email);

        if (!toValidate.isPresent()) {
            return false;
        }

        Account accountToValidate = toValidate.get();

        // Check password validity and account active status.
        final boolean activeAccount = checkAccountValidity ? accountToValidate.getStatus().equals(AccountStatus.ACTIVE)
                : true;
        final boolean validPassword = accountToValidate.getPassword().equals(EncryptionUtils.encryptPassword(password));

        // If password is invalid
        if (!validPassword && !runtimeTenantResolver.isInstance()) {
            // Increment password error counter and update account
            accountToValidate.setAuthenticationFailedCounter(accountToValidate.getAuthenticationFailedCounter() + 1);
            // If max error reached, lock account
            if (accountToValidate.getAuthenticationFailedCounter() > thresholdFailedAuthentication) {
                accountToValidate.setStatus(AccountStatus.LOCKED);
                try {
                    updateAccount(accountToValidate.getId(), accountToValidate);
                } catch (final EntityException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        } else {
            resetAuthenticationFailedCounter(accountToValidate.getId());
        }
        return activeAccount && validPassword;
    }

    @Override
    public boolean existAccount(final String pEmail) {
        accountRepository.findAll();
        return accountRepository.findOneByEmail(pEmail).isPresent();
    }

    @Override
    public void checkPassword(final Account pAccount) throws EntityInvalidException {
        if (!pAccount.getExternal() && !validPassword(pAccount.getPassword())) {
            throw new EntityInvalidException(
                    "The provided password doesn't match the configured pattern : " + passwordRegex);
        }
    }

    @Override
    public boolean validPassword(final String password) {
        if (password == null) {
            return false;
        }
        return this.passwordRegexPattern.matcher(password).matches();
    }

    @Override
    public String getPasswordRules() {
        return passwordRules;
    }

    @Override
    public void changePassword(final Long pId, final String pEncryptPassword) throws EntityNotFoundException {
        final Account toChange = retrieveAccount(pId);
        toChange.setPassword(pEncryptPassword);
        toChange.setPasswordUpdateDate(LocalDateTime.now());
        resetAuthenticationFailedCounter(toChange);
        toChange.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(toChange);
    }

    @Override
    public void resetAuthenticationFailedCounter(Long id) throws EntityNotFoundException {
        Account account = retrieveAccount(id);
        resetAuthenticationFailedCounter(account);
        accountRepository.save(account);
    }

    /**
     * Reset the authentication failed counter of an Account without explicitly saving changes into db.
     * @param account Account which authentication failed counter is to reset
     */
    private void resetAuthenticationFailedCounter(Account account) {
        account.setAuthenticationFailedCounter(0L);
    }

    @Scheduled(cron = "${regards.accounts.validity.check.cron}")
    @Override
    public void checkAccountValidity() {
        LOG.info("Start checking accounts inactivity");
        final Set<Account> toCheck = accountRepository.findAllByStatusNot(AccountStatus.INACTIVE);

        // Account#equals being on email, we create a fake Account with the INSTANCE_ADMIN login and we remove it from the database result.
        toCheck.remove(new Account(rootAdminUserLogin, "", "", rootAdminUserPassword));
        // lets check issues with the invalidity date
        if ((accountValidityDuration != null) && !accountValidityDuration.equals(0L)) {
            final LocalDateTime now = LocalDateTime.now();
            toCheck.stream().filter(a -> a.getInvalidityDate().isBefore(now))
                    .peek(a -> a.setStatus(AccountStatus.INACTIVE))
                    .peek(a -> LOG.info("Account {} set to {} because of its account validity date", a.getEmail(),
                                        AccountStatus.INACTIVE))
                    .forEach(accountRepository::save);
        }

        // lets check issues with the password
        if ((accountPasswordValidityDuration != null) && !accountPasswordValidityDuration.equals(0L)) {
            final LocalDateTime minValidityDate = LocalDateTime.now().minusDays(accountPasswordValidityDuration);
            // get all account that are not already locked, those already locked would not be re-locked anyway
            toCheck.stream()
                    .filter(a -> a.getExternal().equals(false) && (a.getPasswordUpdateDate() != null)
                            && a.getPasswordUpdateDate().isBefore(minValidityDate))
                    .peek(a -> a.setStatus(AccountStatus.INACTIVE_PASSWORD))
                    .peek(a -> LOG.info("Account {} set to {} because of its password validity date", a.getEmail(),
                                        AccountStatus.INACTIVE_PASSWORD))
                    .forEach(accountRepository::save);
        }
    }

}
