/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.catalog.plugin.client;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class CatalogPluginClientIT extends AbstractRegardsWebIT {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogPluginClientIT.class);

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Test
    public void testRetrievePluginTypes() {
        final ICatalogPluginClient pluginClient = FeignClientBuilder.build(new TokenClientProvider<>(
                ICatalogPluginClient.class, "http://" + serverAddress + ":" + getPort(), feignSecurityManager));
        FeignSecurityManager.asSystem();
        final ResponseEntity<List<Resource<String>>> pluginTypes = pluginClient.getPluginTypes();
        Assert.assertTrue(pluginTypes.getStatusCode().equals(HttpStatus.OK));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
