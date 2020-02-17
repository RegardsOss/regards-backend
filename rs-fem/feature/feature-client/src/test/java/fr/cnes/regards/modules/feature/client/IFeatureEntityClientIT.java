/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.client;

import java.time.OffsetDateTime;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.Gson;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.feature.dto.FeatureEntityDto;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=fem_client" })
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
public class IFeatureEntityClientIT extends AbstractRegardsWebIT {

    private static final Logger LOG = LoggerFactory.getLogger(IFeatureEntityClientIT.class);

    @Value("${server.address}")
    private String serverAddress;

    /**
     * Client to test
     */
    private IFeatureEntityClient client;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Autowired
    private Gson gson;

    @Before
    public void init() {
        jwtService.injectMockToken(getDefaultTenant(), DEFAULT_ROLE);
        client = FeignClientBuilder.build(
                                          new TokenClientProvider<>(IFeatureEntityClient.class,
                                                  "http://" + serverAddress + ":" + getPort(), feignSecurityManager),
                                          gson);
        FeignSecurityManager.asSystem();
    }

    /**
     *
     * Check that the access right Feign Client handle the pagination parameters.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testFindAll() {
        ResponseEntity<PagedModel<EntityModel<FeatureEntityDto>>> response = client.findAll("model",
                                                                                            OffsetDateTime.now(), 0, 1);
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
