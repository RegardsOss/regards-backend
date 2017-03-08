/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.plugins.client.rest;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;

import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.hystrix.HystrixFeign;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;

/**
 *
 * Check that the plugin Feign Client can contact plugin rest module.
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */

public class PluginClientIT extends AbstractRegardsWebIT {

    private static final Logger LOG = LoggerFactory.getLogger(PluginClientIT.class);

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Test
    public void testRetrievePluginTypes() {
        try {
            authService.setAuthorities(DEFAULT_TENANT, IPluginClient.PLUGIN_TYPES, "Controller", RequestMethod.GET,
                                       RoleAuthority.getSysRole(""));
            jwtService.injectToken(DEFAULT_TENANT, RoleAuthority.getSysRole(""), "");
            final IPluginClient pluginClient = HystrixFeign.builder().contract(new SpringMvcContract())
                    .encoder(new GsonEncoder()).decoder(new ResponseEntityDecoder(new GsonDecoder()))
                    .target(new TokenClientProvider<>(IPluginClient.class, "http://" + serverAddress + ":" + getPort(),
                            feignSecurityManager));
            final ResponseEntity<List<Resource<String>>> pluginTypes = pluginClient.getPluginTypes();
            Assert.assertTrue(pluginTypes.getStatusCode().equals(HttpStatus.OK));
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
