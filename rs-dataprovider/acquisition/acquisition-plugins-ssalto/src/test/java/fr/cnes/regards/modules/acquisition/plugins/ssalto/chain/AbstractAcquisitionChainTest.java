/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.chain;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import fr.cnes.regards.modules.ingest.client.IIngestClient;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * Test acquisition chain processing
 *
 * @author Marc Sordi
 *
 */
@ContextConfiguration(classes = { AbstractAcquisitionChainTest.AcquisitionConfiguration.class })
public abstract class AbstractAcquisitionChainTest extends AbstractDaoTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAcquisitionChainTest.class);

    @Autowired
    protected IAcquisitionProcessingService processingService;

    @Autowired
    protected IAcquisitionFileRepository fileRepository;

    @Autowired
    protected IProductRepository productRepository;

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    static class AcquisitionConfiguration {

        @Bean
        public IIngestClient ingestClient() {
            return new IngestClientMock();
        }
    }

    /**
     * @return an {@link AcquisitionProcessingChain} to test
     * @throws ModuleException
     */
    protected abstract AcquisitionProcessingChain createAcquisitionChain() throws ModuleException;

    /**
     * @return expected number of file acquired
     */
    protected abstract int getExpectedFiles();

    /**
     * @return expected number of product created
     */
    protected abstract int getExpectedProducts();

    @Requirement("REGARDS_DSL_ING_SSALTO_010")
    @Purpose("A plugin can generate a SIP from a data file respecting a pattern")
    @Test
    public void startChain() throws ModuleException, InterruptedException {
        AcquisitionProcessingChain processingChain = processingService.createChain(createAcquisitionChain());

        processingService.startManualChain(processingChain.getId());

        // 1 job is created for scanning, registering files and creating products
        // 1 job per product is created for SIP generation
        // 1 job is for submission to ingest

        // Wait until all files are registered as acquired
        int fileAcquired = 0;
        int expectedFileAcquired = getExpectedFiles();
        int loops = 10;
        do {
            Thread.sleep(1_000);
            fileAcquired = fileRepository.findByState(AcquisitionFileState.ACQUIRED).size();
            loops--;
        } while ((fileAcquired != expectedFileAcquired) && (loops != 0));

        if (fileAcquired != expectedFileAcquired) {
            Assert.fail();
        }

        // Wait until SIP are generated
        int productGenerated = 0;
        int expectedProducts = getExpectedProducts();
        loops = 10;
        do {
            Thread.sleep(1_000);
            productGenerated = productRepository.findBySipState(ProductSIPState.GENERATED).size();
            loops--;
        } while ((productGenerated != expectedProducts) && (loops != 0));

        if (productGenerated != expectedProducts) {
            Assert.fail();
        }

        // Wait until SIP are submitted to INGEST (mock!)
        int productSubmitted = 0;
        loops = 10;
        do {
            Thread.sleep(1_000);
            productSubmitted = productRepository.findBySipState(SIPState.VALID).size();
            loops--;
        } while ((productSubmitted != expectedProducts) && (loops != 0));

        if (productSubmitted != expectedProducts) {
            Assert.fail();
        }
    }
}
