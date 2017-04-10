/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.account;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

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
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * {@link IAccountService} implementation.
 *
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
     * Encryption algorithm
     */
    private static final String SHA_512 = "SHA-512";

    /**
     * Regex that the password should respect. Provided by property file.
     */
    private final String passwordRegex;

    /**
     * Description of the regex to respect in natural language. Provided by property file.
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
     * Create new service with passed deps
     *
     * @param pAccountRepository
     *            the account repo
     */
    public AccountService(final IAccountRepository pAccountRepository,
            @Value("${regards.accounts.password.regex}") final String passwordRegex,
            @Value("${regards.accounts.password.rules}") final String passwordRules,
            @Value("${regards.accounts.password.validity.duration}") final Long accountPasswordValidityDuration,
            @Value("${regards.accounts.validity.duration}") final Long accountValidityDuration,
            @Value("${regards.accounts.root.user.login}") final String rootAdminUserLogin,
            @Value("${regards.accounts.root.user.password}") final String rootAdminUserPassword,
            @Value("${regards.accounts.failed.authentication.max}") final Long thresholdFailedAuthentication) {
        super();
        accountRepository = pAccountRepository;
        this.passwordRegex = passwordRegex;
        this.passwordRules = passwordRules;
        this.accountPasswordValidityDuration = accountPasswordValidityDuration;
        this.accountValidityDuration = accountValidityDuration;
        this.rootAdminUserLogin = rootAdminUserLogin;
        this.rootAdminUserPassword = rootAdminUserPassword;
        this.thresholdFailedAuthentication = thresholdFailedAuthentication;
    }

    @PostConstruct
    public void initialize() {
        if (!this.existAccount(rootAdminUserLogin)) {
            final Account account = new Account(rootAdminUserLogin, rootAdminUserLogin, rootAdminUserLogin,
                    rootAdminUserPassword);
            account.setStatus(AccountStatus.ACTIVE);
            account.setAuthenticationFailedCounter(0L);
            account.setExternal(false);
            accountRepository.save(account);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#retrieveAccountList()
     */
    @Override
    public Page<Account> retrieveAccountList(final Pageable pPageable) {
        return accountRepository.findAll(pPageable);
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
     * @see fr.cnes.regards.modules.accessrights.service.account.IAccountService#createAccount(fr.cnes.regards.modules.
     * accessrights.domain.instance.Account)
     */
    @Override
    public Account createAccount(final Account pAccount) {
        pAccount.setId(null);
        pAccount.setPassword(encryptPassword(pAccount.getPassword()));
        pAccount.setInvalidityDate(LocalDateTime.now().plusDays(accountValidityDuration));
        return accountRepository.save(pAccount);
    }

    /**
     * helper method to centralize the password encryption process.
     *
     * @return encrypted password
     */
    @Override
    public String encryptPassword(final String pPassword) {
        try {
            final MessageDigest md = MessageDigest.getInstance(SHA_512);
            return new String(md.digest(pPassword.getBytes()));
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);// NOSONAR: this is only a developpement exception and should never happens
        }
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
    public boolean validatePassword(final String pEmail, final String pPassword) throws EntityException {
        final Account toValidate = accountRepository.findOneByEmail(pEmail)
                .orElseThrow(() -> new EntityNotFoundException(pEmail, Account.class));
        final Boolean result = toValidate.getStatus().equals(AccountStatus.ACTIVE)
                && toValidate.getPassword().equals(encryptPassword(pPassword));
        if (!result) {
            toValidate.setAuthenticationFailedCounter(toValidate.getAuthenticationFailedCounter() + 1);
            if (toValidate.getAuthenticationFailedCounter() > thresholdFailedAuthentication) {
                toValidate.setStatus(AccountStatus.LOCKED);
                updateAccount(toValidate.getId(), toValidate);
            }
        }
        return result;
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

    @Override
    public void checkPassword(final Account pAccount) throws EntityInvalidException {
        if (!pAccount.getExternal() && !validPassword(pAccount.getPassword())) {
            throw new EntityInvalidException(
                    "The provided password doesn't match the configured pattern : " + passwordRegex);
        }
    }

    @Override
    public boolean validPassword(final String pPassword) {
        final Pattern p = Pattern.compile(passwordRegex);
        return p.matcher(pPassword).matches();
    }

    @Override
    public String getPasswordRules() {
        return passwordRules;
    }

    @Override
    public void changePassword(final Long pId, final String pEncryptPassword) throws EntityNotFoundException {
        final Account toChange = retrieveAccount(pId);
        toChange.setPassword(pEncryptPassword);
        toChange.setAuthenticationFailedCounter(0L);
        toChange.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(toChange);
    }

    @Scheduled(cron = "${regards.accounts.validity.check.cron}")
    @Override
    public void checkAccountValidity() {
        final Set<Account> toCheck = accountRepository.findAllByStatusNot(AccountStatus.INACTIVE);
        // lets check issues with the invalidity date
        if ((accountValidityDuration != null) && !accountValidityDuration.equals(0L)) {
            final LocalDateTime now = LocalDateTime.now();
            toCheck.stream().filter(a -> a.getInvalidityDate().isBefore(now))
                    .peek(a -> a.setStatus(AccountStatus.INACTIVE)).forEach(accountRepository::save);
        }
        // lets check issues with the password
        if ((accountPasswordValidityDuration != null) && !accountPasswordValidityDuration.equals(0L)) {
            final LocalDateTime minValidityDate = LocalDateTime.now().minusDays(accountPasswordValidityDuration);
            // get all account that are not already locked, those already locked would not be re-locked anyway
            toCheck.stream()
                    .filter(a -> a.getExternal().equals(false) && (a.getPasswordUpdateDate() != null)
                            && a.getPasswordUpdateDate().isBefore(minValidityDate))
                    .peek(a -> a.setStatus(AccountStatus.INACTIVE)).forEach(accountRepository::save);
        }
    }

}
