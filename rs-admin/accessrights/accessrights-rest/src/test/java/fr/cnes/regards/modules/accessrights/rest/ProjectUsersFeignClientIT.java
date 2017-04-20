/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Test project endpoint client
 *
 * @author Marc Sordi
 *
 */
public class ProjectUsersFeignClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountFeignClientIT.class);

    /**
     * Server
     */
    @Value("${server.address}")
    private String serverAddress;

    /**
     * Client to test
     */
    private IProjectUsersClient client;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Before
    public void init() {
        client = FeignClientBuilder.build(new TokenClientProvider<>(IProjectUsersClient.class,
                "http://" + serverAddress + ":" + getPort(), feignSecurityManager));
        FeignSecurityManager.asSystem();
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveProjectUserListFromFeignClient() {
        final ResponseEntity<PagedResources<Resource<ProjectUser>>> response = client.retrieveProjectUserList(0, 10);
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveAccessRequestListFromFeignClient() {
        final ResponseEntity<PagedResources<Resource<ProjectUser>>> response = client.retrieveAccessRequestList(0, 10);
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveProjectUserByEmailFromFeignClient() {
        final ResponseEntity<Resource<ProjectUser>> response = client.retrieveProjectUserByEmail("unkown@regards.de");
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveProjectUserFromFeignClient() {
        final ResponseEntity<Resource<ProjectUser>> response = client.retrieveProjectUser(1L);
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void removeProjectUserFromFeignClient() {
        final ResponseEntity<Void> response = client.removeProjectUser(new Long(150));
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
