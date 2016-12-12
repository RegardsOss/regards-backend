/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
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
import fr.cnes.regards.modules.accessrights.client.IResourcesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 *
 * Class ResourceFeignClientIT
 *
 * Test that all endpoints of the ResourceController are accessible with Feign clients
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@EnableFeignClients(clients = { IResourcesClient.class })
public class ResourceFeignClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(AccountFeignClientIT.class);

    /**
     * Web server address for ResourceController
     */
    @Value("${server.address}")
    private String serverAddress;

    /**
     * Web server port for ResourceController
     */
    @Value("${server.port}")
    private String serverPort;

    /**
     * Feign Client to test
     */
    private IResourcesClient client;

    /**
     *
     * Initialize feign client
     *
     * @throws JwtException
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void init() throws JwtException {
        jwtService.injectToken(DEFAULT_TENANT, DefaultRole.PROJECT_ADMIN.toString());
        client = HystrixFeign.builder().contract(new SpringMvcContract()).encoder(new GsonEncoder())
                .decoder(new ResponseEntityDecoder(new GsonDecoder())).decode404().target(new TokenClientProvider<>(
                        IResourcesClient.class, "http://" + serverAddress + ":" + serverPort));
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
            jwtService.injectToken(DEFAULT_TENANT, DefaultRole.PUBLIC.toString());
            final ResponseEntity<List<Resource<ResourcesAccess>>> response = client.retrieveResourcesAccesses();
            Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
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
