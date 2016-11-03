package fr.cnes.regards.modules.plugins.rest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.plugins.service.IPluginService;

@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class PluginControllerIT extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginControllerIT.class);

    /**
     * Utility service for handling JWT. Autowired by Spring.
     */
    @Autowired
    private JWTService jwtService;

    /**
     * Method authorization service.Autowired by Spring.
     */
    @Autowired
    private MethodAuthorizationService authService;

    /**
     * The jwt string
     */
    private String jwt;

    /**
     * 
     */
    @Autowired
    private IPluginService pluginService;

    /**
     * Do some setup before each test
     */
    @Before
    public void init() {
        final String tenant = "test-1";
        jwt = jwtService.generateToken(tenant, "cmz@c-s.fr", "CMZ", "ADMIN");
        authService.setAuthorities(tenant, "/plugins", RequestMethod.GET, "ADMIN");
        authService.setAuthorities(tenant, "/plugintypes", RequestMethod.GET, "ADMIN");
        authService.setAuthorities(tenant, "/plugins/{pluginId}/config", RequestMethod.GET, "ADMIN");
        authService.setAuthorities(tenant, "/plugins/{pluginId}/config/{configId}", RequestMethod.GET, "ADMIN");
    }

    // @Test
    // public void getAllPluginsRest() {
    // final List<ResultMatcher> expectations = new ArrayList<>(1);
    // expectations.add(status().isOk());
    // performGet("/plugins", jwt, expectations, "unable to load all plugins");
    // }

    @Test
    public void getAllPluginTypesRest() {
        final List<ResultMatcher> expectations = new ArrayList<>(3);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType("application/json;charset=UTF-8"));
        expectations
                .add(MockMvcResultMatchers.jsonPath("$.*", Matchers.hasSize(pluginService.getPluginTypes().size())));
        performGet("/plugintypes", jwt, expectations, "unable to load all plugin types");
    }

    @Test
    public void getPluginConfigurationsByTypeWithPluginId() {
        final List<ResultMatcher> expectations = new ArrayList<>(2);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType("application/json;charset=UTF-8"));
        expectations.add(MockMvcResultMatchers
                         .jsonPath("$.*", Matchers.hasSize(pluginService.getPluginConfigurationsByType("aParameterPlugin").size())));
        performGet("/plugins/aParameterPlugin/config", jwt, expectations,
                   "unable to load all plugin configuration of a specific type");
    }

    @Test
    public void getPluginConfiguration() {
        String configId = "33";
        final List<ResultMatcher> expectations = new ArrayList<>(3);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType("application/json;charset=UTF-8"));
        // pluginService.getPluginConfigurationsByType(pPluginId)
        expectations.add(MockMvcResultMatchers
                .jsonPath("$.*", Matchers.hasSize(pluginService.getPluginConfigurationsByType("aParameterPlugin").size())));
        performGet("/plugins/aParameterPlugin/config/" + configId, jwt, expectations,
                   "unable to load a plugin configuration");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
