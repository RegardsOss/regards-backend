/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.Gson;
import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;

/**
 * Test Ingest API through its client
 * @author Marc Sordi
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingestclientit" })
public class IngestClientIT extends AbstractRegardsWebIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestClientIT.class);

    @Value("${server.address}")
    private String serverAddress;

    private IIngestClient client;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private Gson gson;

    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Before
    public void init() {
        client = FeignClientBuilder.build(
                                          new TokenClientProvider<>(IIngestClient.class,
                                                  "http://" + serverAddress + ":" + getPort(), feignSecurityManager),
                                          gson);
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        FeignSecurityManager.asSystem();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Test
    public void ingestSIP() {
        SIPCollectionBuilder collectionBuilder = new SIPCollectionBuilder(
                IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL);

        SIPBuilder sipBuilder = new SIPBuilder("CLIENT_SIP_001");
        String filename = OffsetDateTime.now().toString();

        collectionBuilder.add(sipBuilder.buildReference(Paths.get(filename), "sdflksdlkfjlsd45fg46sdfgdf"));

        ResponseEntity<Collection<SIPDto>> entities = client.ingest(collectionBuilder.build());
        Assert.assertEquals(HttpStatus.CREATED, entities.getStatusCode());
    }
}
