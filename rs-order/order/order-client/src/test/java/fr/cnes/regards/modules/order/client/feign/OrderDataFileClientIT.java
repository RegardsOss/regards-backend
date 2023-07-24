/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.order.client.feign;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.order.client.env.config.OrderClientTestConfiguration;
import fr.cnes.regards.modules.order.client.env.utils.OrderTestUtilsService;
import fr.cnes.regards.modules.order.domain.Order;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

/**
 * Test for {@link IOrderDataFileClient}
 *
 * @author Iliana Ghazali
 **/

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=order_data_client_it" })
@ActiveProfiles("testFeign")
@ContextConfiguration(classes = OrderClientTestConfiguration.class)
public class OrderDataFileClientIT extends AbstractRegardsWebIT {

    // CLIENTS

    private IOrderDataFileClient availableClient; // class under test

    // SERVICES

    @Autowired
    private OrderTestUtilsService orderTestService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Autowired
    private Gson gson;

    @Value("${server.address}")
    private String serverAddress;

    @Before
    public void init() {
        orderTestService.cleanRepositories();
        initFeignClient();
    }

    public void initFeignClient() {
        availableClient = FeignClientBuilder.build(new TokenClientProvider<>(IOrderDataFileClient.class,
                                                                             "http://"
                                                                             + serverAddress
                                                                             + ":"
                                                                             + getPort(),
                                                                             feignSecurityManager), gson);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        FeignSecurityManager.asSystem();
    }

    @Test
    public void givenOrder_whenPending_thenDownloadAccepted() {
        // GIVEN
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Order order = orderTestService.createOrder();
        List<OrderDataFile> pendingFiles = orderTestService.getPendingDataFiles(order.getId());
        Assert.assertEquals("Unexpected created data files.", 2, pendingFiles.size());

        // WHEN
        ResponseEntity<InputStreamResource> response = availableClient.downloadFile(pendingFiles.get(0).getId());

        // THEN
        // Only test that download endpoint is accessible
        Assert.assertEquals("Unexpected response status.", HttpStatus.ACCEPTED, response.getStatusCode());
    }

}
