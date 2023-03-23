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
package fr.cnes.regards.modules.toponyms.client;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;
import fr.cnes.regards.modules.toponyms.domain.ToponymGeoJson;
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

import java.util.List;

/**
 * @author Sébastien Binda
 */
public class ToponymsClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ToponymsClientIT.class);

    @Value("${server.address}")
    private String serverAddress;

    /**
     * Client to test
     */
    private IToponymsClient client;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Autowired
    Gson gson;

    @Before
    public void init() {
        client = FeignClientBuilder.build(new TokenClientProvider<>(IToponymsClient.class,
                                                                    "http://" + serverAddress + ":" + getPort(),
                                                                    feignSecurityManager), gson);
        runtimeTenantResolver.forceTenant("instance");
        FeignSecurityManager.asSystem();
    }

    @Test
    public void find() {
        ResponseEntity<PagedModel<EntityModel<ToponymDTO>>> result = client.find(0, 150);
        Assert.assertTrue(result.getStatusCode().equals(HttpStatus.OK));
        Assert.assertNotNull(result.getBody());
        Assert.assertEquals(result.getBody().getMetadata().getTotalElements(), 251);
        Assert.assertNotNull(result.getBody().getContent());
        Assert.assertEquals(result.getBody().getContent().size(), 150);
    }

    @Test
    public void search() {
        ResponseEntity<List<EntityModel<ToponymDTO>>> result = client.search("fr", "fr");
        Assert.assertTrue(result.getStatusCode().equals(HttpStatus.OK));
        Assert.assertNotNull(result.getBody());
        Assert.assertEquals(result.getBody().size(), 6);
    }

    @Test
    public void get() {
        String id = "France";
        ResponseEntity<EntityModel<ToponymDTO>> result = client.get(id);
        Assert.assertTrue(result.getStatusCode().equals(HttpStatus.OK));
        Assert.assertNotNull(result.getBody());
        Assert.assertNotNull(result.getBody().getContent());
        Assert.assertEquals(result.getBody().getContent().getBusinessId(), id);
        Assert.assertEquals(result.getBody().getContent().getLabelEn(), id);
        Assert.assertEquals(result.getBody().getContent().getLabelFr(), id);
    }

    @Test
    public void getSimplified() {
        String id = "France";
        ResponseEntity<EntityModel<ToponymDTO>> result = client.get(id, true);
        Assert.assertTrue(result.getStatusCode().equals(HttpStatus.OK));
        Assert.assertNotNull(result.getBody());
        Assert.assertNotNull(result.getBody().getContent());
        Assert.assertEquals(result.getBody().getContent().getBusinessId(), id);
        Assert.assertEquals(result.getBody().getContent().getLabelEn(), id);
        Assert.assertEquals(result.getBody().getContent().getLabelFr(), id);
    }

    @Test
    public void createNotVisibleToponym() {
        String polygon = "{\"type\": \"Feature\", \"properties\": {\"test\" : 42}, \"geometry\": { \"type\": \"Polygon\", \"coordinates\": [[ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ]] }}";
        ResponseEntity<EntityModel<ToponymDTO>> result = client.createNotVisibleToponym(new ToponymGeoJson(polygon,
                                                                                                           "test_user",
                                                                                                           "test_project"));
        Assert.assertTrue(result.getStatusCode().equals(HttpStatus.CREATED));
        Assert.assertNotNull(result.getBody());
        Assert.assertNotNull(result.getBody().getContent());
        Assert.assertNotNull(result.getBody().getContent().getBusinessId());
        Assert.assertNotNull(result.getBody().getContent().getGeometry());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
