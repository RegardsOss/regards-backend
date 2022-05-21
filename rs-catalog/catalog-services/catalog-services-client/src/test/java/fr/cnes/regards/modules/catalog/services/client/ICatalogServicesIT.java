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
package fr.cnes.regards.modules.catalog.services.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

/**
 * Integration Test for {@link ICatalogServicesClient}
 *
 * <p>
 *
 * @author Xavier-Alexandre Brochard
 * @DirtiesContext is mandatory, we have issue with context cleaning because of MockMvc
 */
@TestPropertySource("classpath:test.properties")
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
public class ICatalogServicesIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ICatalogServicesIT.class);

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private IPluginConfigurationRepository repos;

    /**
     * Client to test
     */
    private ICatalogServicesClient client;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Autowired
    private Gson gson;

    @Before
    public void init() {
        jwtService.injectMockToken(getDefaultTenant(), getDefaultRole());
        client = FeignClientBuilder.build(new TokenClientProvider<>(ICatalogServicesClient.class,
                                                                    "http://" + serverAddress + ":" + getPort(),
                                                                    feignSecurityManager), gson);
        FeignSecurityManager.asSystem();
        repos.deleteAll();
    }

    @Test
    @Requirement("REGARDS_DSL_ACC_ARC_130")
    @Purpose("Check that we can retrieve IHM Service augmented with their meta information")
    public void retrieveServicesWithMeta() {
        ResponseEntity<List<EntityModel<PluginConfigurationDto>>> result = client.retrieveServices(null, null);
        Assert.assertTrue(result.getStatusCode().equals(HttpStatus.OK));
    }

    protected Map<String, List<String>> getHeadersToApply() {
        Map<String, List<String>> headers = Maps.newHashMap();
        headers.put(HttpConstants.CONTENT_TYPE, Lists.newArrayList("application/json"));
        headers.put(HttpConstants.ACCEPT,
                    Lists.newArrayList(MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE));

        return headers;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
