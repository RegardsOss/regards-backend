package fr.cnes.regards.modules.order.rest;

import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.order.dao.IBasketRepository;
import fr.cnes.regards.modules.order.domain.basket.Basket;
import fr.cnes.regards.modules.search.client.ICatalogClient;

/**
 * @author oroussel
 */
public class OrderControllerIT extends AbstractRegardsIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderControllerIT.class);

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IBasketRepository basketRepos;

    @Configuration
    static class Conf {

        @Bean
        public ICatalogClient catalogClient() {
            return Mockito.mock(ICatalogClient.class);
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Before
    public void init() {
        tenantResolver.forceTenant(DEFAULT_TENANT);

        basketRepos.deleteAll();
    }

    @Test
    public void testCreate() {
        Basket basket = new Basket();
        basket.setEmail(DEFAULT_USER_EMAIL);
        basketRepos.save(basket);

        // Test POST without argument
        performDefaultPost("/orders", null, Lists.newArrayList(MockMvcResultMatchers.status().isCreated()), "error");
    }

    @Test
    public void testCreateNOK() {
        // All baskets have been deleted so order creation must fail
        // Test POST without argument
        performDefaultPost("/orders", null, Lists.newArrayList(MockMvcResultMatchers.status().isNotFound()), "error");
    }
}
