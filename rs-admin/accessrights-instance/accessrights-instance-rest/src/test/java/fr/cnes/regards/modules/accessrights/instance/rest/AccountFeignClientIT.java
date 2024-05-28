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
package fr.cnes.regards.modules.accessrights.instance.rest;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.CodeType;
import fr.cnes.regards.modules.authentication.client.IExternalAuthenticationClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class AccountFeignClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountFeignClientIT.class);

    private static final String MAIL_TEST = "feign@user.com";

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private IAccountRepository accountRepo;

    @MockBean
    private IExternalAuthenticationClient externalAuthenticationClient;

    private IAccountsClient accountsClient;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Before
    public void init() {
        accountsClient = FeignClientBuilder.build(new TokenClientProvider<>(IAccountsClient.class,
                                                                            "http://" + serverAddress + ":" + getPort(),
                                                                            feignSecurityManager));
        FeignSecurityManager.asSystem();

        final Optional<Account> account = accountRepo.findOneByEmail(MAIL_TEST);
        account.ifPresent(accountRepo::delete);
    }

    /**
     * Check that the accounts Feign Client can retrieve all accounts.
     */
    @Ignore
    @Test
    public void retrieveAccountListFromFeignClient() {
        try {
            final ResponseEntity<PagedModel<EntityModel<Account>>> accounts = accountsClient.retrieveAccountList(null,
                                                                                                                 0,
                                                                                                                 10);
            Assert.assertEquals(accounts.getStatusCode(), HttpStatus.OK);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check that the accounts Feign Client can create an account.
     */
    @Ignore
    @Test
    public void createAccountFromFeignClient() {
        try {
            final Account account = new Account(MAIL_TEST, "feign", "feign", "password");
            AccountNPassword accountNPassword = new AccountNPassword(account, account.getPassword());
            final ResponseEntity<EntityModel<Account>> response = accountsClient.createAccount(accountNPassword);
            Assert.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check that the accounts Feign Client can update an account.
     */
    @Test
    @Ignore
    public void updateAccountFromFeignClient() {
        try {
            final Account account = new Account("feign@user.com", "feign", "feign", "password");
            account.setId(150L);
            final ResponseEntity<EntityModel<Account>> response = accountsClient.updateAccount(150L, account);
            Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check that the accounts Feign Client can update an account.
     */
    @Test
    @Ignore
    public void removeAccountFromFeignClient() {
        try {
            final ResponseEntity<Void> response = accountsClient.removeAccount(150L);
            Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check that the accounts Feign Client can retrieve an account.
     */
    @Test
    @Ignore
    public void retrieveAccountFromFeignClient() {
        try {
            final ResponseEntity<EntityModel<Account>> response = accountsClient.retrieveAccount(150L);
            Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check that the accounts Feign Client can retrieve an account.
     */
    @Test
    @Ignore
    public void retrieveAccountByEmailFromFeignClient() {
        try {
            final ResponseEntity<EntityModel<Account>> response = accountsClient.retrieveAccountByEmail(
                "email@unkown.fr");
            Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check that the accounts Feign Client can retrieve an account.
     */
    @Test
    @Ignore
    public void unlockAccountFromFeignClient() {
        try {
            final ResponseEntity<Void> response = accountsClient.unlockAccount(150L, "unlock_code");
            Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check that the accounts Feign Client can retrieve an account.
     */
    @Test
    @Ignore
    public void sendAccountCodeFromFeignClient() {
        try {
            jwtService.injectToken(getDefaultTenant(), DefaultRole.REGISTERED_USER.toString(), "", "");
            final ResponseEntity<Void> response = accountsClient.sendAccountCode("email@unkown.fr", CodeType.UNLOCK);
            Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check that the accounts Feign Client can retrieve an account.
     */
    @Test
    public void validatePasswordFromFeignClient() {
        try {
            final ResponseEntity<Boolean> response = accountsClient.validatePassword("email@unkown.fr", "password");
            Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
            Assert.assertFalse(response.getBody());
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
