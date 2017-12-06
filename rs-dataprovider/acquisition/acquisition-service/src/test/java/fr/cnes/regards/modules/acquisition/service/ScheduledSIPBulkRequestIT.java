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

package fr.cnes.regards.modules.acquisition.service;

import java.time.OffsetDateTime;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.builder.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.builder.ProcessGenerationBuilder;
import fr.cnes.regards.modules.acquisition.domain.ProcessGeneration;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.service.conf.ChainGenerationServiceConfiguration;
import fr.cnes.regards.modules.acquisition.service.conf.MockedFeignClientConf;
import fr.cnes.regards.modules.acquisition.service.step.AcquisitionITHelper;

/**
 * @author Christophe Mertz
 *
 */
@ContextConfiguration(classes = { ChainGenerationServiceConfiguration.class, MockedFeignClientConf.class })
@ActiveProfiles({ "test", "disableDataProviderTask" })
@DirtiesContext
public class ScheduledSIPBulkRequestIT extends AcquisitionITHelper {

    @Autowired
    private IProductBulkRequestService productBulkRequestService;

    private ProcessGeneration process;

    @Before
    public void createProductsAndProcess() throws ModuleException {
        MetaProduct metaProduct001 = metaProductService
                .createOrUpdate(MetaProductBuilder.build("meta-product-name-001").addMetaFile(metaFileOptional)
                        .addMetaFile(metaFileMandatory).withIngestProcessingChain("ingest-processing-chain-001").get());
        MetaProduct metaProduct002 = metaProductService.createOrUpdate(MetaProductBuilder.build("meta-product-name-002")
                .addMetaFile(metaFileMandatory).withIngestProcessingChain("ingest-processing-chain-002").get());
        MetaProduct metaProduct003 = metaProductService.createOrUpdate(MetaProductBuilder.build("meta-product-name-003")
                .addMetaFile(metaFileMandatory).withIngestProcessingChain("ingest-processing-chain-003").get());

        // Create Products

        // ===================== session-001 ===================== 
        createProduct("product-001", "session-001", metaProduct001, false, ProductStatus.COMPLETED, "file-001",
                      "file-002");
        createProduct("product-002", "session-001", metaProduct001, false, ProductStatus.COMPLETED, "file-003",
                      "file-004");

        createProduct("product-004", "session-001", metaProduct001, false, ProductStatus.COMPLETED, "file-007",
                      "file-008");
        createProduct("product-005", "session-001", metaProduct001, false, ProductStatus.COMPLETED, "file-009",
                      "file-010");
        createProduct("product-006", "session-001", metaProduct001, false, ProductStatus.COMPLETED, "file-011",
                      "file-012");

        // ===================== session-002 =====================
        createProduct("product-003", "session-002", metaProduct002, false, ProductStatus.FINISHED, "file-005",
                      "file-006");

        createProduct("product-007", "session-002", metaProduct002, false, ProductStatus.COMPLETED, "file-013",
                      "file-014");
        createProduct("product-008", "session-002", metaProduct002, true, ProductStatus.COMPLETED, "file-015",
                      "file-016");
        createProduct("product-009", "session-002", metaProduct002, true, ProductStatus.COMPLETED, "file-017",
                      "file-018");
        createProduct("product-010", "session-002", metaProduct002, false, ProductStatus.COMPLETED, "file-019");
        createProduct("product-011", "session-002", metaProduct002, false, ProductStatus.COMPLETED, "file-020");
        createProduct("product-012", "session-002", metaProduct002, false, ProductStatus.COMPLETED, "file-021");
        createProduct("product-013", "session-002", metaProduct002, false, ProductStatus.COMPLETED, "file-022");
        createProduct("product-014", "session-002", metaProduct002, false, ProductStatus.COMPLETED, "file-023");
        createProduct("product-015", "session-002", metaProduct002, false, ProductStatus.COMPLETED, "file-024");
        createProduct("product-016", "session-002", metaProduct002, false, ProductStatus.COMPLETED, "file-025");

        createProduct("product-099", "session-003", metaProduct003, false, ProductStatus.ACQUIRING, "file-099");

        // the chain is not active to not activate it 
        chain.setActive(false);
        chain.setSession("session-001");
        chainService.createOrUpdate(chain);
        process = processGenerationService.save(ProcessGenerationBuilder.build(chain.getSession()).withChain(chain)
                .withStartDate(OffsetDateTime.now()).get());
    }

    @Test
    public void scheduleDataProviderTaskNominalAllSipCreated() throws InterruptedException {
        mockIngestClientResponseOK(Arrays.asList("product-001", "product-002", "product-003", "product-004",
                                                 "product-005", "product-006", "product-007", "product-008",
                                                 "product-009", "product-010", "product-011", "product-012",
                                                 "product-013", "product-014", "product-015", "product-016"));

        Assert.assertEquals(14, productService
                .findBySendedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(2, productService
                .findBySendedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING).size());
        Assert.assertNotNull(processGenerationService.findBySession(chain.getSession()));
        Assert.assertEquals(process, processGenerationService.findBySession(chain.getSession()));

        productBulkRequestService.runBulkRequest();

        Assert.assertEquals(1, processGenerationRepository.findAll().size());

        Assert.assertEquals(0, productService
                .findBySendedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(16, productService
                .findBySendedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING).size());

        ProcessGeneration processLoad = processGenerationService.findBySession(chain.getSession());
        Assert.assertEquals(16, processLoad.getNbSipCreated()); // 16 products created cf mock
        Assert.assertEquals(0, processLoad.getNbSipError());
        Assert.assertEquals(0, processLoad.getNbSipStored());
        Assert.assertNull(processLoad.getStopDate());
    }

    @Test
    public void scheduleDataProviderTaskUnauthorized() throws InterruptedException {
        mockIngestClientResponseUnauthorized();

        Assert.assertEquals(14, productService
                .findBySendedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(2, productService
                .findBySendedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING).size());

        productBulkRequestService.runBulkRequest();

        Assert.assertEquals(1, processGenerationRepository.findAll().size());

        // Nothing should be change
        Assert.assertEquals(14, productService
                .findBySendedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(2, productService
                .findBySendedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING).size());

    }

    @Test
    public void scheduleDataProviderTaskNominalPartialContentResponse() throws InterruptedException {
        mockIngestClientResponsePartialContent(Arrays
                .asList("product-003", "product-004", "product-005", "product-006", "product-007", "product-008",
                        "product-009", "product-010", "product-011", "product-012", "product-013", "product-014",
                        "product-015", "product-016"), Arrays.asList("product-001", "product-002"));

        Assert.assertEquals(14, productService
                .findBySendedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING).size());
        Assert.assertEquals(2, productService
                .findBySendedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());

        productBulkRequestService.runBulkRequest();

        Assert.assertEquals(1, processGenerationRepository.findAll().size());

        // 2 products in error are not sended
        Assert.assertEquals(2, productService.findBySendedAndStatusIn(false, ProductStatus.ERROR).size());
        Assert.assertEquals(1, productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING).size());
        Assert.assertEquals(14, productService
                .findBySendedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());

        ProcessGeneration processLoad = processGenerationService.findBySession(chain.getSession());
        Assert.assertEquals(14, processLoad.getNbSipCreated()); // 14 products created cf mock
        Assert.assertEquals(2, processLoad.getNbSipError());
        Assert.assertEquals(0, processLoad.getNbSipStored());
        Assert.assertNull(processLoad.getStopDate());
    }

}
