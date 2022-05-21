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
package fr.cnes.regards.modules.ingest.client;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

/**
 * {@link IAIPRestClient} test class
 *
 * @author Sébastien Binda
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingest_rest_client" })
public class AIPRestClientIT extends AbstractRegardsWebIT {

    @Value("${server.address}")
    private String serverAddress;

    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Autowired
    private Gson gson;

    private IAIPRestClient client;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void init() throws IOException, ModuleException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        client = FeignClientBuilder.build(new TokenClientProvider<>(IAIPRestClient.class,
                                                                    "http://" + serverAddress + ":" + getPort(),
                                                                    feignSecurityManager), gson);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        FeignSecurityManager.asSystem();
    }

    @Test
    public void searchAips() {
        ResponseEntity<PagedModel<EntityModel<AIPEntity>>> response = client.searchAIPs(SearchAIPsParameters.build()
                                                                                                            .withState(
                                                                                                                AIPState.STORED),
                                                                                        0,
                                                                                        100);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
