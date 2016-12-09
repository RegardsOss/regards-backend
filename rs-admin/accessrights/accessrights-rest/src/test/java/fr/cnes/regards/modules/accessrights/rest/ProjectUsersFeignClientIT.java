/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
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
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

@EnableFeignClients(clients = { IProjectUsersClient.class })
public class ProjectUsersFeignClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountFeignClientIT.class);

    @Value("${server.address}")
    private String serverAddress;

    @Value("${server.port}")
    private String serverPort;

    /**
     * Client to test
     */
    private IProjectUsersClient client;

    @Before
    public void init() throws JwtException {
        jwtService.injectToken(DEFAULT_TENANT, DefaultRole.PROJECT_ADMIN.toString());
        client = HystrixFeign.builder().contract(new SpringMvcContract()).encoder(new GsonEncoder())
                .decoder(new ResponseEntityDecoder(new GsonDecoder())).decode404().target(new TokenClientProvider<>(
                        IProjectUsersClient.class, "http://" + serverAddress + ":" + serverPort));
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveProjectUserListFromFeignClient() {
        try {
            final ResponseEntity<PagedResources<Resource<ProjectUser>>> response = client.retrieveProjectUserList(0,
                                                                                                                  10);
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveAccessRequestListFromFeignClient() {
        try {
            final ResponseEntity<PagedResources<Resource<ProjectUser>>> response = client.retrieveAccessRequestList(0,
                                                                                                                    10);
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveProjectUserFromFeignClient() {
        try {
            final ResponseEntity<Resource<ProjectUser>> response = client.retrieveProjectUser("unkown@regards.de");
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void removeProjectUserFromFeignClient() {
        try {
            final ResponseEntity<Void> response = client.removeProjectUser(new Long(150));
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
