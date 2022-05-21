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
package fr.cnes.regards.modules.accessrights.rest;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.accessrights.client.IMicroserviceResourceClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.service.projectuser.QuotaHelperService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Class ResourceFeignClientIT
 * <p>
 * Test that all endpoints of the ResourceController are accessible with Feign clients
 *
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class ResourceFeignClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourceFeignClientIT.class);

    /**
     * Web server address for ResourceController
     */
    @Value("${server.address}")
    private String serverAddress;

    /**
     * Feign Client to test
     */
    private IMicroserviceResourceClient client;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @MockBean
    private QuotaHelperService quotaHelperService;

    @Autowired
    private Gson gson;

    @Before
    public void init() {
        jwtService.injectMockToken(getDefaultTenant(), DEFAULT_ROLE);
        client = FeignClientBuilder.build(new TokenClientProvider<>(IMicroserviceResourceClient.class,
                                                                    "http://" + serverAddress + ":" + getPort(),
                                                                    feignSecurityManager), gson);
        FeignSecurityManager.asSystem();
    }

    @Test
    public void registerResourcesFromFeignClient() {
        final List<ResourceMapping> resources = new ArrayList<>();
        resources.add(new ResourceMapping("/register/test", "Controller", RequestMethod.GET));
        final ResponseEntity<Void> response = client.registerMicroserviceEndpoints("rs-test", resources);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    @Test
    public void retrieveMicroserviceResourcesFromFeignClient() {
        final ResponseEntity<PagedModel<EntityModel<ResourcesAccess>>> response = client.getAllResourceAccessesByMicroservice(
            "rs-test",
            0,
            20);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
