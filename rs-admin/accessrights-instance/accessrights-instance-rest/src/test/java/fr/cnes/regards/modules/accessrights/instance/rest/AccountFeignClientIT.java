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
package fr.cnes.regards.modules.accessrights.instance.rest;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.accessrights.instance.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.instance.dao.IAccountRepository;
import fr.cnes.regards.modules.accessrights.instance.dao.IPasswordResetTokenRepository;
import fr.cnes.regards.modules.accessrights.instance.dao.accountunlock.IAccountUnlockTokenRepository;
import fr.cnes.regards.modules.accessrights.instance.dao.emailverification.IEmailVerificationTokenRepository;
import fr.cnes.regards.modules.accessrights.instance.domain.Account;
import fr.cnes.regards.modules.accessrights.instance.domain.AccountNPassword;
import fr.cnes.regards.modules.accessrights.instance.domain.accountunlock.PerformUnlockAccountDto;
import fr.cnes.regards.modules.accessrights.instance.domain.accountunlock.RequestAccountUnlockDto;
import fr.cnes.regards.modules.authentication.client.IExternalAuthenticationClient;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
class AccountFeignClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountFeignClientIT.class);

    private static final String MAIL_TEST = "feign@user.com";

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private IAccountRepository accountRepository;

    @Autowired
    private IEmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private IPasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private IAccountUnlockTokenRepository accountUnlockTokenRepository;

    @MockBean
    private IExternalAuthenticationClient externalAuthenticationClient;

    private IAccountsClient accountsClient;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Autowired
    private Gson gson;

    @BeforeEach
    void init() {
        accountsClient = FeignClientBuilder.build(new TokenClientProvider<>(IAccountsClient.class,
                                                                            "http://" + serverAddress + ":" + getPort(),
                                                                            feignSecurityManager), gson);
        FeignSecurityManager.asSystem();

        emailVerificationTokenRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        accountUnlockTokenRepository.deleteAll();
        accountRepository.deleteAll();
    }

    /**
     * Check that the accounts Feign Client can retrieve all accounts.
     */
    @Test
    void retrieveAccountListFromFeignClient() {
        final ResponseEntity<PagedModel<EntityModel<Account>>> accounts = accountsClient.retrieveAccountList(null,
                                                                                                             0,
                                                                                                             10);
        Assert.assertEquals(HttpStatus.OK, accounts.getStatusCode());
    }

    /**
     * Check that the accounts Feign Client can create an account.
     */
    @Test
    void createAccountFromFeignClient() {
        final Account account = new Account(MAIL_TEST, "feign", "feign", "password");
        AccountNPassword accountNPassword = new AccountNPassword(account, account.getPassword());
        accountNPassword.setOriginUrl("originUrl");
        accountNPassword.setRequestLink("requestLink");
        final ResponseEntity<EntityModel<Account>> response = accountsClient.createAccount(accountNPassword);
        Assert.assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    /**
     * Check that the accounts Feign Client can update an account.
     */
    @Test
    void updateAccountFromFeignClient() {
        final Account account = new Account("feign@user.com", "feign", "feign", "password");
        account.setId(150L);
        final ResponseEntity<EntityModel<Account>> response = accountsClient.updateAccount(150L, account);
        Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * Check that the accounts Feign Client can update an account.
     */
    @Test
    void removeAccountFromFeignClient() {
        final ResponseEntity<Void> response = accountsClient.removeAccount(150L);
        Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * Check that the accounts Feign Client can retrieve an account.
     */
    @Test
    void retrieveAccountFromFeignClient() {
        final ResponseEntity<EntityModel<Account>> response = accountsClient.retrieveAccount(150L);
        Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * Check that the accounts Feign Client can retrieve an account.
     */
    @Test
    void retrieveAccountByEmailFromFeignClient() {
        final ResponseEntity<EntityModel<Account>> response = accountsClient.retrieveAccountByEmail("email@unkown.fr");
        Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * Check that the accounts Feign Client can request unlocking an account.
     */
    @Test
    void requestUnlockAccountFromFeignClient() {
        final ResponseEntity<Void> response = accountsClient.requestUnlockAccount("email@unkown.fr",
                                                                                  new RequestAccountUnlockDto(
                                                                                      "originUrl",
                                                                                      "requestLink"));
        Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * Check that the accounts Feign Client can unlock an account.
     */
    @Test
    void performAccountUnlockFromFeignClient() throws JwtException {
        jwtService.injectToken(getDefaultTenant(), DefaultRole.REGISTERED_USER.toString(), "", "");
        PerformUnlockAccountDto unlock = new PerformUnlockAccountDto();
        unlock.setToken("token");
        final ResponseEntity<Void> response = accountsClient.performUnlockAccount("email@unkown.fr", unlock);
        Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    /**
     * Check that the accounts Feign Client can retrieve an account.
     */
    @Test
    void validatePasswordFromFeignClient() {
        final ResponseEntity<Boolean> response = accountsClient.validatePassword("email@unkown.fr", "password");
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assert.assertNotNull(response.getBody());
        Assert.assertFalse(response.getBody());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
