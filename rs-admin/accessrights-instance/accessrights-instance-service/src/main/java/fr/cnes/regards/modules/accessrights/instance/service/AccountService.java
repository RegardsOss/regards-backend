/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.module.rest.exception.*;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framwork.logger.LogConstants;
import fr.cnes.regards.modules.accessrights.instance.dao.AccountSpecificationsBuilder;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountAcceptedEvent;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSearchParameters;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.instance.domain.emailverification.EmailVerificationToken;
import fr.cnes.regards.modules.accessrights.instance.service.emailverification.IEmailVerificationTokenService;
import fr.cnes.regards.modules.accessrights.instance.service.encryption.EncryptionUtils;
import fr.cnes.regards.modules.accessrights.instance.service.setting.AccountSettingsService;
import fr.cnes.regards.modules.accessrights.instance.service.workflow.AccessRightTemplateConf;
import fr.cnes.regards.modules.authentication.client.IExternalAuthenticationClient;
import fr.cnes.regards.modules.authentication.domain.dto.ServiceProviderDto;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.project.service.IProjectService;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import freemarker.template.TemplateException;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
public class AccountService implements IAccountService, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(AccountService.class);

    /**
     * Regex that the password should respect. Provided by property file.
     */
    @Value("${regards.accounts.password.regex}")
    private String passwordRegex;

    /**
     * Associated Pattern
     */
    private Pattern passwordRegexPattern;

    /**
     * Description of the regex to respect in natural language. Provided by property file. Parsed according to "\n" to transform it into a list
     */
    @Value("${regards.accounts.password.rules}")
    private String passwordRules;

    /**
     * In days. Provided by property file.
     */
    @Value("${regards.accounts.password.validity.duration}")
    private Long accountPasswordValidityDuration;

    /**
     * In days. Provided by property file.
     */
    @Value("${regards.accounts.validity.duration}")
    private Long accountValidityDuration;

    /**
     * Root admin user login. Provided by property file.
     */
    @Value("${regards.accounts.root.user.login}")
    private String rootAdminUserLogin;

    /**
     * Root admin user password. Provided by property file.
     */
    @Value("${regards.accounts.root.user.password}")
    private String rootAdminUserPassword;

    /**
     * threshold of failed authentication above which an account should be locked. Provided by property file.
     */
    @Value("${regards.accounts.failed.authentication.max}")
    private Long thresholdFailedAuthentication;

    private final IAccountRepository accountRepository;

    private final ITenantResolver tenantResolver;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IEmailClient emailClient;

    private final ITemplateService templateService;

    private final IInstancePublisher instancePublisher;

    private final AccountSettingsService accountSettingsService;

    private final IExternalAuthenticationClient externalAuthenticationClient;

    private final IProjectService projectService;

    private final IEmailVerificationTokenService emailVerificationTokenService;

    public AccountService(IAccountRepository accountRepository,
                          ITenantResolver tenantResolver,
                          IRuntimeTenantResolver runtimeTenantResolver,
                          IEmailClient emailClient,
                          ITemplateService templateService,
                          IInstancePublisher instancePublisher,
                          AccountSettingsService accountSettingsService,
                          IExternalAuthenticationClient externalAuthenticationClient,
                          IProjectService projectService,
                          IEmailVerificationTokenService emailVerificationTokenService) {
        this.accountRepository = accountRepository;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.emailClient = emailClient;
        this.templateService = templateService;
        this.instancePublisher = instancePublisher;
        this.accountSettingsService = accountSettingsService;
        this.externalAuthenticationClient = externalAuthenticationClient;
        this.projectService = projectService;
        this.emailVerificationTokenService = emailVerificationTokenService;
    }

    @Override
    public void afterPropertiesSet() throws EntityInvalidException {
        passwordRegexPattern = Pattern.compile(this.passwordRegex);
        if (!this.existAccount(rootAdminUserLogin)) {
            Account account = new Account(rootAdminUserLogin,
                                          rootAdminUserLogin,
                                          rootAdminUserLogin,
                                          rootAdminUserPassword);
            account.setStatus(AccountStatus.ACTIVE);
            account.setAuthenticationFailedCounter(0L);
            createAccount(account, null, null, null);
        }
    }

    @Override
    public Page<Account> retrieveAccountList(AccountSearchParameters parameters, Pageable pageable) {
        return accountRepository.findAll(new AccountSpecificationsBuilder().withParameters(parameters).build(),
                                         pageable);
    }

    @Override
    public boolean existAccount(Long pId) {
        return accountRepository.existsById(pId);
    }

    @Override
    @SuppressWarnings("java:S1166") // We translate an internal exception to a more explicit exception
    public Account createAccount(Account account,
                                 @Nullable String project,
                                 @Nullable String originUrl,
                                 @Nullable String requestLink) throws EntityInvalidException {
        account.setId(null);
        if (account.getPassword() != null) {
            checkPassword(account);
            account.setPassword(EncryptionUtils.encryptPassword(account.getPassword()));
        }
        account.setInvalidityDate(LocalDateTime.now().plusDays(accountValidityDuration));
        if (AccountStatus.PENDING.equals(account.getStatus()) && accountSettingsService.isAutoAccept()) {
            activate(account);
        }
        if (StringUtils.hasText(project)) {
            try {
                account.setProjects(new HashSet<>(Collections.singletonList(projectService.retrieveProject(project))));
            } catch (ModuleException e) {
                throw new EntityInvalidException("Invalid project name : " + project); // NOSONAR Duplicated strings
            }
        }
        if (!StringUtils.hasText(account.getOrigin())) {
            account.setOrigin(Account.REGARDS_ORIGIN);
        }
        Account newAccount = accountRepository.save(account);
        LOG.info(LogConstants.SECURITY_MARKER, "Create new account : " + newAccount.toString());
        // Send email only once account is saved (the token that will be created has a reference to the account)
        if (AccountStatus.EMAIL_VERIFICATION.equals(newAccount.getStatus())) {
            sendVerificationEmail(newAccount, originUrl, requestLink);
        }
        return newAccount;
    }

    /**
     * Sends an email to the account address with a link to confirm it. This
     * method creates the validation token. For resending an email (when the validation
     * token already exists but may have expired), use {@link #resendVerificationEmail(Account)}.
     */
    private void sendVerificationEmail(Account account, @Nullable String originUrl, @Nullable String requestLink)
        throws EntityInvalidException {
        if (!StringUtils.hasText(originUrl)) {
            throw new EntityInvalidException("Missing originUrl");
        }
        if (!StringUtils.hasText(requestLink)) {
            throw new EntityInvalidException("Missing requestLink");
        }
        EmailVerificationToken token = emailVerificationTokenService.create(account, originUrl, requestLink);
        doSendVerificationEmail(token);
    }

    @Override
    @SuppressWarnings("java:S1166") // We translate the database-specific exception to a more explicit exception
    public void resendVerificationEmail(Account account) throws EntityOperationForbiddenException {
        EmailVerificationToken token = null;
        try {
            token = emailVerificationTokenService.findByAccount(account);
        } catch (EntityNotFoundException e) {
            throw new EntityOperationForbiddenException("User has already confirmed their email address.");
        }
        token.renew();
        doSendVerificationEmail(token);
    }

    /**
     * Sends a verification email using an existing verification token.
     * This is the common part to the initial email and subsequent email resends.
     */
    private void doSendVerificationEmail(EmailVerificationToken token) {
        Account account = token.getAccount();
        // Create a hash map in order to store the data to inject in the mail
        final Map<String, String> data = new HashMap<>();
        data.put("name", account.getFirstName());
        data.put("requestLink", token.getRequestLink());
        data.put("originUrl", token.getOriginUrl());
        data.put("token", token.getToken());
        data.put("accountEmail", account.getEmail());

        String message;
        try {
            message = templateService.render(AccessRightTemplateConf.ACCOUNT_CONFIRMATION_TEMPLATE_NAME, data);
        } catch (final TemplateException e) {
            LOG.warn("Template could not be found, defaulting on simpler message", e);
            String linkUrlTemplate;
            if (token.getRequestLink().contains("?")) {
                linkUrlTemplate = "%s&origin_url=%s&token=%s&account_email=%s";
            } else {
                linkUrlTemplate = "%s?origin_url=%s&token=%s&account_email=%s";
            }
            message = "Please click on the following link to validate your account: " + String.format(linkUrlTemplate,
                                                                                                      token.getRequestLink(),
                                                                                                      token.getOriginUrl(),
                                                                                                      token,
                                                                                                      account.getEmail());
        }

        // Send it
        try {
            FeignSecurityManager.asInstance();
            emailClient.sendEmail(message, "[REGARDS] Email Confirmation", null, account.getEmail());
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    public void activate(Account account) {
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);
        instancePublisher.publish(new AccountAcceptedEvent(account));
    }

    @Override
    public Account retrieveAccount(Long pAccountId) throws EntityNotFoundException {
        return accountRepository.findById(pAccountId)
                                .orElseThrow(() -> new EntityNotFoundException(pAccountId, Account.class));
    }

    @Override
    public Account retrieveAccountByEmail(String email) throws EntityNotFoundException {
        return accountRepository.findOneByEmailIgnoreCase(email)
                                .orElseThrow(() -> new EntityNotFoundException(email, Account.class));
    }

    @Override
    public Account updateAccount(Long pAccountId, Account pUpdatedAccount) throws EntityException {
        Optional<Account> accountOpt = accountRepository.findById(pAccountId);
        if (accountOpt.isEmpty()) {
            throw new EntityNotFoundException(pAccountId.toString(), Account.class);
        }
        if (!pUpdatedAccount.getId().equals(pAccountId)) {
            throw new EntityInconsistentIdentifierException(pAccountId, pUpdatedAccount.getId(), Account.class);
        }
        Account account = accountOpt.get();
        account.setFirstName(pUpdatedAccount.getFirstName());
        account.setLastName(pUpdatedAccount.getLastName());
        account.setStatus(pUpdatedAccount.getStatus());
        return accountRepository.save(account);
    }

    @Override
    public boolean validatePassword(String email, String password, boolean checkAccountValidity)
        throws EntityNotFoundException {

        Optional<Account> toValidate = accountRepository.findOneByEmailIgnoreCase(email);
        if (toValidate.isEmpty()) {
            return false;
        }

        Account accountToValidate = toValidate.get();

        // Check password validity and account active status.
        boolean activeAccount = !checkAccountValidity || accountToValidate.getStatus().equals(AccountStatus.ACTIVE);
        boolean validPassword = accountToValidate.getPassword().equals(EncryptionUtils.encryptPassword(password));

        // If password is invalid and we are not trying to connect with one of instance account
        if (!validPassword && !runtimeTenantResolver.isInstance()) {
            // Increment password error counter and update account
            accountToValidate.setAuthenticationFailedCounter(accountToValidate.getAuthenticationFailedCounter() + 1);
            // If max error reached, lock account
            if (accountToValidate.getAuthenticationFailedCounter() > thresholdFailedAuthentication) {
                accountToValidate.setStatus(AccountStatus.LOCKED);
                try {
                    updateAccount(accountToValidate.getId(), accountToValidate);
                } catch (EntityException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        } else {
            resetAuthenticationFailedCounter(accountToValidate.getId());
        }
        return activeAccount && validPassword;
    }

    @Override
    public boolean existAccount(String pEmail) {
        return accountRepository.findOneByEmailIgnoreCase(pEmail).isPresent();
    }

    @Override
    public void checkPassword(Account pAccount) throws EntityInvalidException {
        if (!pAccount.isExternal() && !validPassword(pAccount.getPassword())) {
            throw new EntityInvalidException("The provided password doesn't match the configured pattern : "
                                             + passwordRegex);
        }
    }

    @Override
    public boolean validPassword(String password) {
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
    public void changePassword(Long pId, String pEncryptPassword) throws EntityNotFoundException {
        Account toChange = retrieveAccount(pId);
        toChange.setPassword(pEncryptPassword);
        toChange.setPasswordUpdateDate(LocalDateTime.now());
        resetAuthenticationFailedCounter(toChange);
        toChange.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(toChange);
        final Map<String, String> data = new HashMap<>();
        data.put("name", toChange.getFirstName());
        String message;
        try {
            message = templateService.render(AccessRightTemplateConf.PASSWORD_CHANGED_TEMPLATE_NAME, data);
        } catch (TemplateException e) {
            message = "Password successfully changed";
            LOG.error("Failed to use email template for password change. The email was sent with a basic message.", e);
        }
        try {
            FeignSecurityManager.asInstance();
            emailClient.sendEmail(message, "[REGARDS] Password changed", null, toChange.getEmail());
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    public void resetAuthenticationFailedCounter(Long id) throws EntityNotFoundException {
        Account account = retrieveAccount(id);
        resetAuthenticationFailedCounter(account);
        accountRepository.save(account);
    }

    @Override
    public List<String> getOrigins() {
        List<String> origins = new ArrayList<>();
        origins.add(Account.REGARDS_ORIGIN);
        // remark: getAllTenants() is called on a mandatory µS rs-authentication, so all tenants connections will
        // always be active, no need to check this condition.
        for (String tenant : tenantResolver.getAllTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                FeignSecurityManager.asSystem();
                PagedModel<EntityModel<ServiceProviderDto>> requestBody = externalAuthenticationClient.getServiceProviders()
                                                                                                      .getBody();
                if (requestBody != null) {
                    origins.addAll(HateoasUtils.unwrapCollection(requestBody.getContent())
                                               .stream()
                                               .map(ServiceProviderDto::getName)
                                               .toList());
                }
            } finally {
                FeignSecurityManager.reset();
                runtimeTenantResolver.clearTenant();
            }
        }
        return origins;
    }

    @Override
    @SuppressWarnings("java:S1166") // We translate an internal exception to a more explicit exception
    public void link(String email, String project) throws EntityException {
        Account account = retrieveAccountByEmail(email);
        try {
            account.getProjects().add(projectService.retrieveProject(project));
        } catch (ModuleException e) {
            throw new EntityInvalidException("Invalid project name : " + project); // NOSONAR Duplicated string
        }

    }

    @Override
    @SuppressWarnings("java:S1166") // We translate an internal exception to a more explicit exception
    public void unlink(String email, String project) throws EntityException {
        Account account = retrieveAccountByEmail(email);
        try {
            account.getProjects().remove(projectService.retrieveProject(project));
        } catch (ModuleException e) {
            throw new EntityInvalidException("Invalid project name : " + project); // NOSONAR Duplicated strings
        }
    }

    @Override
    public void updateOrigin(String email, String origin) throws EntityException {
        Account account = retrieveAccountByEmail(email);
        if (StringUtils.hasText(origin)) {
            account.setOrigin(origin);
        }
    }

    /**
     * Reset the authentication failed counter of an Account without explicitly saving changes into db.
     *
     * @param account Account which authentication failed counter is to reset
     */
    private void resetAuthenticationFailedCounter(Account account) {
        account.setAuthenticationFailedCounter(0L);
    }

    @Scheduled(cron = "${regards.accounts.validity.check.cron}")
    @Override
    public void checkAccountValidity() {

        LOG.info("Start checking accounts inactivity");

        Set<Account> toCheck = accountRepository.findAllByStatusNot(AccountStatus.INACTIVE)
                                                .stream()
                                                .filter(account -> !rootAdminUserLogin.equals(account.getEmail()))
                                                .collect(Collectors.toSet());

        // check issues with the password
        if ((accountPasswordValidityDuration != null) && !accountPasswordValidityDuration.equals(0L)) {
            LocalDateTime minValidityDate = LocalDateTime.now().minusDays(accountPasswordValidityDuration);
            // get all account that are not already locked, those already locked would not be re-locked anyway
            toCheck.stream()
                   .filter(account -> !account.isExternal()
                                      && account.getPasswordUpdateDate() != null
                                      && account.getPasswordUpdateDate().isBefore(minValidityDate))
                   .forEach(account -> {
                       account.setStatus(AccountStatus.INACTIVE_PASSWORD);
                       LOG.info("Account {} set to {} because of its password validity date",
                                account.getEmail(),
                                AccountStatus.INACTIVE_PASSWORD);
                   });
        }

        // check issues with the invalidity date (account invalidity has higher priority than password invalidity so
        // this check must be done last)
        if ((accountValidityDuration != null) && !accountValidityDuration.equals(0L)) {
            LocalDateTime now = LocalDateTime.now();
            toCheck.stream().filter(account -> account.getInvalidityDate().isBefore(now)).forEach(account -> {
                account.setStatus(AccountStatus.INACTIVE);
                LOG.info("Account {} set to {} because of its account validity date",
                         account.getEmail(),
                         AccountStatus.INACTIVE);
            });
        }

        accountRepository.saveAll(toCheck);
    }

}
