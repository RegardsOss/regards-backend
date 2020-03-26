/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.dao.IPasswordResetTokenRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.instance.service.passwordreset.IPasswordResetService;
import fr.cnes.regards.modules.emails.client.IEmailClient;
import fr.cnes.regards.modules.templates.dao.ITemplateRepository;

/**
 * This class test that the system can invalidate an account.
 *  
 * @author Christophe Mertz
 */
@ContextConfiguration(classes = { AccountServiceIT.Config.class })
@PropertySource("classpath:application-test.properties")
public class AccountServiceIT extends AbstractRegardsIT {

    @Configuration
    public static class Config {

        @Bean
        public ITemplateRepository templateRepository() {
            return Mockito.mock(ITemplateRepository.class);
        }

//        @Bean
//        public ITemplateService templateService() {
//            return Mockito.mock(ITemplateService.class);
//        }

        @Bean
        public IEmailClient emailClient() {
            return Mockito.mock(IEmailClient.class);
        }

        @Bean
        public IProjectUsersClient projectUsersClient() {
            return Mockito.mock(IProjectUsersClient.class);
        }
    }


    private static final Logger LOG = LoggerFactory.getLogger(AccountServiceIT.class);

    private static final String EMAIL = "AcceptAccountIT@test.com";

    private static final String FIRST_NAME = "Firstname";

    private static final String LAST_NAME = "Lastname";

    private static final String PASSWORD = "password";

    @Value("${regards.accounts.password.validity.duration}")
    private Long accountPasswordValidityDuration;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAccountRepository accountRepository;

    @Autowired
    private IAccountService accountService;

    @Autowired
    private IPasswordResetService passwordResetService;

    @Autowired
    private IPasswordResetTokenRepository tokenRepository;

    private Account account;

    @Before
    public void init() throws IOException, ModuleException, URISyntaxException {
        tenantResolver.forceTenant(getDefaultTenant());
        initDb();
    }

    private void initDb() {
        clearDb();
        account = new Account(EMAIL, FIRST_NAME, LAST_NAME, PASSWORD);
        account.setInvalidityDate(LocalDateTime.now().plusDays(10));
        account.setPasswordUpdateDate(LocalDateTime.now().plusDays(10));
        accountRepository.save(account);
    }

    @Test
    @Requirement("REGARDS_DSL_SYS_SEC_410")
    @Requirement("REGARDS_DSL_SYS_SEC_310")
    @Purpose("Check that the system can invalidate an account on the basis of its account validity duration or password validity duration")
    public void testCheckAccountValidity() {

        Account accountValid = new Account("valid@c-s.fr", "Antoine", "Griezmann", PASSWORD);
        accountValid.setInvalidityDate(LocalDateTime.now().plusDays(1));
        accountValid.setPasswordUpdateDate(LocalDateTime.now().plusDays(1));
        accountValid.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(accountValid);

        Account accountInvalid = new Account("invalid@c-s.fr", "John", "Doe", PASSWORD);
        accountInvalid.setInvalidityDate(LocalDateTime.now().minusDays(1));
        accountInvalid.setPasswordUpdateDate(LocalDateTime.now().plusDays(1));
        accountInvalid.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(accountInvalid);

        Account accountPasswordInvalid = new Account("passwordInvalid@c-s.fr", "Kylian", "Mbappé", "passWord");
        accountPasswordInvalid.setInvalidityDate(LocalDateTime.now().plusDays(5));
        accountPasswordInvalid
                .setPasswordUpdateDate(LocalDateTime.now().minusDays(accountPasswordValidityDuration).minusDays(1L));
        accountPasswordInvalid.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(accountPasswordInvalid);

        // lets test now that everything is in place
        accountService.checkAccountValidity();

        accountValid = accountRepository.findById(accountValid.getId()).get();
        accountInvalid = accountRepository.findById(accountInvalid.getId()).get();
        accountPasswordInvalid = accountRepository.findById(accountPasswordInvalid.getId()).get();

        LOG.info("account :                <{}> - {} - {}", account.getStatus(), account.getInvalidityDate(),
                 account.getPasswordUpdateDate());
        LOG.info("accountValid :           <{}> - {} - {}", accountValid.getStatus(), accountValid.getInvalidityDate(),
                 accountValid.getPasswordUpdateDate());
        LOG.info("accountInvalid :         <{}> - {} - {}", accountInvalid.getStatus(),
                 accountInvalid.getInvalidityDate(), accountInvalid.getPasswordUpdateDate());
        LOG.info("accountPasswordInvalid : <{}> - {} - {}", accountPasswordInvalid.getStatus(),
                 accountPasswordInvalid.getInvalidityDate(), accountPasswordInvalid.getPasswordUpdateDate());

        final Set<Account> toCheck = Sets.newHashSet(accountValid, accountInvalid, accountPasswordInvalid);
        Assert.assertEquals(1, toCheck.stream().filter(a -> a.getStatus().equals(AccountStatus.ACTIVE)).count());
        Assert.assertEquals(1, toCheck.stream().filter(a -> a.getStatus().equals(AccountStatus.INACTIVE)).count());
        Assert.assertEquals(1, toCheck.stream().filter(a -> a.getStatus().equals(AccountStatus.INACTIVE_PASSWORD))
                .count());
        Assert.assertEquals(AccountStatus.INACTIVE, accountInvalid.getStatus());
        Assert.assertEquals(AccountStatus.INACTIVE_PASSWORD, accountPasswordInvalid.getStatus());
    }

    @Test
    public void testResetPassword() {

        Account accountPasswordInvalid = new Account("passwordInvalid@c-s.fr", "Kylian", "Mbappé", "passWord");
        accountPasswordInvalid.setInvalidityDate(LocalDateTime.now().plusDays(5));
        accountPasswordInvalid
                .setPasswordUpdateDate(LocalDateTime.now().minusDays(accountPasswordValidityDuration).minusDays(1L));
        accountPasswordInvalid.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(accountPasswordInvalid);

        LOG.info("accountPasswordInvalid : <{}> - {} - {}", accountPasswordInvalid.getStatus(),
                 accountPasswordInvalid.getInvalidityDate(), accountPasswordInvalid.getPasswordUpdateDate());

        // lets test now that everything is in place
        accountService.checkAccountValidity();
        accountPasswordInvalid = accountRepository.findById(accountPasswordInvalid.getId()).get();

        LOG.info("accountPasswordInvalid : <{}> - {} - {}", accountPasswordInvalid.getStatus(),
                 accountPasswordInvalid.getInvalidityDate(), accountPasswordInvalid.getPasswordUpdateDate());
        Assert.assertEquals(AccountStatus.INACTIVE_PASSWORD, accountPasswordInvalid.getStatus());

        try {
            final String token = "coucou";
            passwordResetService.createPasswordResetToken(accountPasswordInvalid, token);
            passwordResetService.performPasswordReset(accountPasswordInvalid.getEmail(), token, "the new password");
        } catch (EntityException e) {
            Assert.fail();
        }

        accountService.checkAccountValidity();
        accountPasswordInvalid = accountRepository.findById(accountPasswordInvalid.getId()).get();
        
        LOG.info("accountPasswordInvalid : <{}> - {} - {}", accountPasswordInvalid.getStatus(),
                 accountPasswordInvalid.getInvalidityDate(), accountPasswordInvalid.getPasswordUpdateDate());
        Assert.assertEquals(AccountStatus.ACTIVE, accountPasswordInvalid.getStatus());
    }

    @After
    public void cleanUp() {
        clearDb();
    }

    private void clearDb() {
        tokenRepository.deleteAll();
        accountRepository.deleteAll();
    }


}
