/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.CodeType;

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
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Ignore
    @Test
    public void retrieveAccountListFromFeignClient() {
        try {
            final ResponseEntity<PagedResources<Resource<Account>>> accounts = accountsClient
                    .retrieveAccountList(0, 10);
            Assert.assertTrue(accounts.getStatusCode().equals(HttpStatus.OK));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the accounts Feign Client can create an account.
     *
     * @since 1.0-SNAPSHOT
     */
    @Ignore
    @Test
    public void createAccountFromFeignClient() {
        try {
            final Account account = new Account(MAIL_TEST, "feign", "feign", "password");
            final ResponseEntity<Resource<Account>> response = accountsClient.createAccount(account);
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.CREATED));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the accounts Feign Client can update an account.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Ignore
    public void updateAccountFromFeignClient() {
        try {
            final Account account = new Account("feign@user.com", "feign", "feign", "password");
            account.setId(new Long(150));
            final ResponseEntity<Resource<Account>> response = accountsClient.updateAccount(new Long(150), account);
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the accounts Feign Client can update an account.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Ignore
    public void removeAccountFromFeignClient() {
        try {
            final ResponseEntity<Void> response = accountsClient.removeAccount(new Long(150));
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve an account.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Ignore
    public void retrieveAccountFromFeignClient() {
        try {
            final ResponseEntity<Resource<Account>> response = accountsClient.retrieveAccount(new Long(150));
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve an account.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Ignore
    public void retrieveAccountByEmailFromFeignClient() {
        try {
            final ResponseEntity<Resource<Account>> response = accountsClient.retrieveAccounByEmail("email@unkown.fr");
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve an account.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Ignore
    public void unlockAccountFromFeignClient() {
        try {
            final ResponseEntity<Void> response = accountsClient.unlockAccount(new Long(150), "unlock_code");
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve an account.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Ignore
    public void sendAccountCodeFromFeignClient() {
        try {
            jwtService.injectToken(DEFAULT_TENANT, DefaultRole.REGISTERED_USER.toString(), "");
            final ResponseEntity<Void> response = accountsClient.sendAccountCode("email@unkown.fr", CodeType.UNLOCK);
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve an account.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void validatePasswordFromFeignClient() {
        try {
            final ResponseEntity<Boolean> response = accountsClient.validatePassword("email@unkown.fr", "password");
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
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
