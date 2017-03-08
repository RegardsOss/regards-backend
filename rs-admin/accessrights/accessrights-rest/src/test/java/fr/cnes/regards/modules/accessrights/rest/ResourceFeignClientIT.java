/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;

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
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
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
     * Feign Client to test
     */
    private IResourcesClient client;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Before
    public void init() {
        client = FeignClientBuilder.build(new TokenClientProvider<>(IResourcesClient.class,
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
    public void retrieveResourcesListFromFeignClient() {
        final ResponseEntity<PagedResources<Resource<ResourcesAccess>>> response = client.retrieveResourcesAccesses(0,
                                                                                                                    20);
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
    }

    @Test
    public void registerResourcesFromFeignClient() {
        final List<ResourceMapping> resources = new ArrayList<>();
        resources.add(new ResourceMapping("/register/test", RequestMethod.GET));
        final ResponseEntity<Void> response = client.registerMicroserviceEndpoints("rs-test", resources);
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
    }

    @Test
    public void retrieveMicroserviceResourcesFromFeignClient() {
        final ResponseEntity<PagedResources<Resource<ResourcesAccess>>> response = client
                .retrieveResourcesAccesses("rs-test", 0, 20);
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
