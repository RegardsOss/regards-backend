package fr.cnes.regards.modules.access.services.client;

import java.util.Arrays;

/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;

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

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;

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
                "http://" + serverAddress + ":" + getPort(), feignSecurityManager));
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        FeignSecurityManager.asSystem();
    }

    /**
     * Check that the attribute model Feign Client can return the list of attribute models.
     */
    @Test
    public void retrieveServices_shouldReturnServices() {
        ResponseEntity<List<EntityModel<PluginServiceDto>>> result = client
                .retrieveServices(Arrays.asList("coucou"), Arrays.asList(ServiceScope.MANY));
        Assert.assertTrue(result.getStatusCode().equals(HttpStatus.OK));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
