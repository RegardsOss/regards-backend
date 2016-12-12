/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.hystrix.HystrixFeign;
import fr.cnes.regards.client.core.TokenClientProvider;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.accessrights.client.IAccountsClient;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.domain.AccountStatus;
import fr.cnes.regards.modules.accessrights.domain.CodeType;
import fr.cnes.regards.modules.accessrights.domain.instance.Account;

public class AccountFeignClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountFeignClientIT.class);

    @Value("${server.address}")
    private String serverAddress;

    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private IAccountRepository accountRepo;

    private IAccountsClient accountsClient;

    private static final String MAIL_TEST = "feign@user.com";

    @Before
    public void init() throws JwtException {
        jwtService.injectToken(DEFAULT_TENANT, DefaultRole.INSTANCE_ADMIN.toString());
        accountsClient = HystrixFeign.builder().contract(new SpringMvcContract()).encoder(new GsonEncoder())
                .decoder(new ResponseEntityDecoder(new GsonDecoder())).decode404()
                .target(new TokenClientProvider<>(IAccountsClient.class, "http://" + serverAddress + ":" + serverPort));

        final Optional<Account> account = accountRepo.findOneByEmail(MAIL_TEST);
        account.ifPresent(accountRepo::delete);
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveAccountListFromFeignClient() {
        try {
            final ResponseEntity<PagedResources<Resource<Account>>> accounts = accountsClient.retrieveAccountList(0,
                                                                                                                  10);
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
    public void changeAccountPasswordFromFeignClient() {
        try {
            final ResponseEntity<Void> response = accountsClient.changeAccountPassword(new Long(150), "reset_code",
                                                                                       "new_password");
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
    public void sendAccountCodeFromFeignClient() {
        try {
            jwtService.injectToken(DEFAULT_TENANT, DefaultRole.REGISTERED_USER.toString());
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
            final ResponseEntity<AccountStatus> response = accountsClient.validatePassword("email@unkown.fr",
                                                                                           "password");
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
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
