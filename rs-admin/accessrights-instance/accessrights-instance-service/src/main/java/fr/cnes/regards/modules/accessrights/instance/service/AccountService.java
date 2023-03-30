/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.accessrights.instance.dao.AccountSpecificationsBuilder;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountAcceptedEvent;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountSearchParameters;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.instance.service.encryption.EncryptionUtils;
import fr.cnes.regards.modules.accessrights.instance.service.setting.AccountSettingsService;
import fr.cnes.regards.modules.accessrights.instance.service.workflow.AccessRightTemplateConf;
import fr.cnes.regards.modules.authentication.client.IExternalAuthenticationClient;
import fr.cnes.regards.modules.authentication.domain.dto.ServiceProviderDto;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.project.service.IProjectService;
import fr.cnes.regards.modules.templates.service.ITemplateService;
import freemarker.template.TemplateException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private MeterRegistry registry;

    @SuppressWarnings("unused")
    private Counter createdAccountCounter;

    public AccountService(IAccountRepository accountRepository,
                          ITenantResolver tenantResolver,
                          IRuntimeTenantResolver runtimeTenantResolver,
                          IEmailClient emailClient,
                          ITemplateService templateService,
                          IInstancePublisher instancePublisher,
                          AccountSettingsService accountSettingsService,
                          IExternalAuthenticationClient externalAuthenticationClient,
                          IProjectService projectService) {
        this.accountRepository = accountRepository;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.emailClient = emailClient;
        this.templateService = templateService;
        this.instancePublisher = instancePublisher;
        this.accountSettingsService = accountSettingsService;
        this.externalAuthenticationClient = externalAuthenticationClient;
        this.projectService = projectService;
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
            createAccount(account, null);
        }
        this.createdAccountCounter = registry.counter("regards.created.account");
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
    public Account createAccount(Account account, String project) throws EntityInvalidException {
        account.setId(null);
        if (account.getPassword() != null) {
            checkPassword(account);
            account.setPassword(EncryptionUtils.encryptPassword(account.getPassword()));
        }
        account.setInvalidityDate(LocalDateTime.now().plusDays(accountValidityDuration));
        if (AccountStatus.PENDING.equals(account.getStatus()) && accountSettingsService.isAutoAccept()) {
            activate(account);
        }
        if (!StringUtils.isEmpty(project)) {
            try {
                account.setProjects(new HashSet<>(Collections.singletonList(projectService.retrieveProject(project))));
            } catch (ModuleException e) {
                throw new EntityInvalidException("Invalid project name : " + project);
            }
        }
        if (StringUtils.isEmpty(account.getOrigin())) {
            account.setOrigin(Account.REGARDS_ORIGIN);
        }
        return accountRepository.save(account);
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
        return accountRepository.findOneByEmail(email)
                                .orElseThrow(() -> new EntityNotFoundException(email, Account.class));
    }

    @Override
    public Account updateAccount(Long pAccountId, Account pUpdatedAccount) throws EntityException {
        Optional<Account> accountOpt = accountRepository.findById(pAccountId);
        if (!accountOpt.isPresent()) {
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

        Optional<Account> toValidate = accountRepository.findOneByEmail(email);

        if (!toValidate.isPresent()) {
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
        return accountRepository.findOneByEmail(pEmail).isPresent();
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
        }
        try {
            FeignSecurityManager.asSystem();
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
                                               .collect(Collectors.toList()));
                }
            } finally {
                FeignSecurityManager.reset();
                runtimeTenantResolver.clearTenant();
            }
        }
        return origins;
    }

    @Override
    public void link(String email, String project) throws EntityException {
        Account account = retrieveAccountByEmail(email);
        try {
            account.getProjects().add(projectService.retrieveProject(project));
        } catch (ModuleException e) {
            throw new EntityInvalidException("Invalid project name : " + project);
        }

    }

    @Override
    public void unlink(String email, String project) throws EntityException {
        Account account = retrieveAccountByEmail(email);
        try {
            account.getProjects().remove(projectService.retrieveProject(project));
        } catch (ModuleException e) {
            throw new EntityInvalidException("Invalid project name : " + project);
        }
    }

    @Override
    public void updateOrigin(String email, String origin) throws EntityException {
        Account account = retrieveAccountByEmail(email);
        if (!StringUtils.isEmpty(origin)) {
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

        // check issues with the invalidity date
        if ((accountValidityDuration != null) && !accountValidityDuration.equals(0L)) {
            LocalDateTime now = LocalDateTime.now();
            toCheck.stream().filter(account -> account.getInvalidityDate().isBefore(now)).forEach(account -> {
                account.setStatus(AccountStatus.INACTIVE);
                LOG.info("Account {} set to {} because of its account validity date",
                         account.getEmail(),
                         AccountStatus.INACTIVE);
            });
        }

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
        accountRepository.saveAll(toCheck);
    }

}
