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
package fr.cnes.regards.modules.ingest.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingService;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * Test storage submission job
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=aip_submission",
        "regards.tenant=PROJECT", "regards.tenants=PROJECT", "regards.ingest.process.new.sips.delay=5",
        "regards.ingest.process.new.aips.storage.delay=5" })
public class AipSubmissionTest extends AbstractMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AipSubmissionTest.class);

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private IIngestProcessingService processingService;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private IAIPRepository aipRepository;

    @Configuration
    static class StorageConfiguration {

        @Bean
        public IAipClient aipClient() {
            return Mockito.mock(IAipClient.class);
        }
    }

    @Before
    public void before() {
        // Enable jobs
        simulateApplicationReadyEvent();
    }

    @Requirement("REGARDS_DSL_ING_PRO_120")
    @Purpose("Manage scheduled ingestion tasks for all CREATED SIP")
    @Test
    public void submitAips() throws ModuleException, IOException, InterruptedException {

        long startTime = System.currentTimeMillis();

        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        processingService.initDefaultServiceConfiguration();

        // Mock
        Mockito.when(aipClient.store(Mockito.any())).thenReturn(new ResponseEntity<>(HttpStatus.CREATED));
        // List<RejectedAip> rejectedAips = new ArrayList<>();
        // rejectedAips.add(new RejectedAip("URN:AIP:DATA:PROJECT:ce5e4110-127a-3bea-9a28-4752549629b3:V1",
        // Arrays.asList("error mock")));
        // Mockito.when(aipClient.store(Mockito.any()))
        // .thenReturn(new ResponseEntity<>(rejectedAips, HttpStatus.UNPROCESSABLE_ENTITY));

        // Create SIP collection
        Path collection = Paths.get("src", "test", "resources", "data", "cdpp_collection.json");

        // Ingest SIPs to create AIPs
        ingestService.ingest(Files.newInputStream(collection));

        // Wait until AIP is VALID
        int validAips = 0;
        int expectedAips = 1;
        int loops = 30;
        do {
            Thread.sleep(1_000);
            validAips = aipRepository
                    .findBySipProcessingAndState(IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL, AIPState.VALID)
                    // .findBySipProcessingAndState(IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                    // SipAIPState.REJECTED)
                    .size();
            loops--;
        } while ((validAips != expectedAips) && (loops != 0));

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        LOGGER.info("Time elapsed : {}", elapsedTime);

        if (validAips != expectedAips) {
            Assert.fail();
        }
    }
}
