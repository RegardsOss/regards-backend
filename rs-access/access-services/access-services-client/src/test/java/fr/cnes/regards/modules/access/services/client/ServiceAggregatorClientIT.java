package fr.cnes.regards.modules.access.services.client;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;

/**
 * Integration tests for {@link IServiceAggregatorClient}.
 *
 * @author Xavier-Alexandre Brochard
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=access" })
public class ServiceAggregatorClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ServiceAggregatorClientIT.class);

    @Value("${server.address}")
    private String serverAddress;

    /**
     * Client to test
     */
    private IServiceAggregatorClient client;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Before
    public void init() {
        client = FeignClientBuilder.build(new TokenClientProvider<>(IServiceAggregatorClient.class,
                                                                    "http://" + serverAddress + ":" + getPort(),
                                                                    feignSecurityManager));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        FeignSecurityManager.asSystem();
    }

    /**
     * Check that the attribute model Feign Client can return the list of attribute models.
     */
    @Test
    public void retrieveServices_shouldReturnServices() {
        ResponseEntity<List<EntityModel<PluginServiceDto>>> result = client.retrieveServices(Arrays.asList("coucou"),
                                                                                             Arrays.asList(ServiceScope.MANY));
        Assert.assertTrue(result.getStatusCode().equals(HttpStatus.OK));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
