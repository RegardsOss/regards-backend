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

package fr.cnes.regards.modules.acquisition.service.step;

import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.modules.acquisition.builder.AcquisitionFileBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.builder.ProductBuilder;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.service.conf.ChainGenerationServiceConfiguration;
import fr.cnes.regards.modules.acquisition.service.conf.MockedFeignClientConf;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ChainGenerationServiceConfiguration.class, MockedFeignClientConf.class })
@ActiveProfiles({ "test" })
@DirtiesContext
public class ScheduledDataProviderTasksTest extends AbstractAcquisitionIT {

    //    private static final Logger LOG = LoggerFactory.getLogger(ScheduledDataProviderTasks.class);

    @Value("${regards.acquisition.process.new.sip.ingest.delay}")
    private String scheduledTasksDelay;

    //    @Autowired
    //    private IPluginService pluginService;
    //
    //    @Autowired
    //    private IGenerateSipStep generateSIPStepImpl;
    //
    //    @Autowired
    //    private AutowireCapableBeanFactory beanFactory;

    @Before
    public void init() {

        MetaProduct metaProduct001 = metaProductService
                .save(MetaProductBuilder.build("meta-product-name-001").addMetaFile(metaFileOptional)
                        .addMetaFile(metaFileMandatory).withIngestProcessingChain("ingest-processing-chain-001").get());
        MetaProduct metaProduct002 = metaProductService.save(MetaProductBuilder.build("meta-product-name-002")
                .addMetaFile(metaFileMandatory).withIngestProcessingChain("ingest-processing-chain-002").get());
        MetaProduct metaProduct003 = metaProductService.save(MetaProductBuilder.build("meta-product-name-003")
                .addMetaFile(metaFileMandatory).withIngestProcessingChain("ingest-processing-chain-003").get());

        // Create a Product
        createProduct("product-001", "session-001", metaProduct001, false, ProductStatus.COMPLETED, "file-001",
                      "file-002");
        createProduct("product-002", "session-001", metaProduct001, false, ProductStatus.COMPLETED, "file-003",
                      "file-004");
        createProduct("product-003", "session-002", metaProduct001, false, ProductStatus.FINISHED, "file-005",
                      "file-006");

        createProduct("product-004", "session-001", metaProduct002, false, ProductStatus.COMPLETED, "file-007",
                      "file-008");
        createProduct("product-005", "session-001", metaProduct002, false, ProductStatus.COMPLETED, "file-009",
                      "file-010");
        createProduct("product-006", "session-001", metaProduct002, false, ProductStatus.COMPLETED, "file-011",
                      "file-012");
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

        createProduct("product-099", "session-002", metaProduct003, false, ProductStatus.ACQUIRING, "file-099");
    }

    @Test
    public void scheduleDataProviderTaskNominalAllSipCreated() throws InterruptedException {
        mockIngestClientResponseOK();

        Assert.assertEquals(14, productService
                .findBySavedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(2, productService
                .findBySavedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySavedAndStatusIn(false, ProductStatus.ACQUIRING).size());

        Thread.sleep(Integer.parseInt(scheduledTasksDelay) + 1_000);

        Assert.assertEquals(0, productService
                .findBySavedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(16, productService
                .findBySavedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySavedAndStatusIn(false, ProductStatus.ACQUIRING).size());
    }

    @Test
    public void scheduleDataProviderTaskUnauthorized() throws InterruptedException {
        mockIngestClientResponseUnauthorized();

        Assert.assertEquals(14, productService
                .findBySavedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(2, productService
                .findBySavedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySavedAndStatusIn(false, ProductStatus.ACQUIRING).size());

        Thread.sleep(Integer.parseInt(scheduledTasksDelay) + 1_000);

        // Nothing should be change

        Assert.assertEquals(14, productService
                .findBySavedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(2, productService
                .findBySavedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySavedAndStatusIn(false, ProductStatus.ACQUIRING).size());
    }

    @Test
    public void scheduleDataProviderTaskNominalPartialResponseContent() throws InterruptedException {
        mockIngestClientResponsePartialContent("product-001","product-002","product-016");

        Assert.assertEquals(14, productService
                .findBySavedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(2, productService
                .findBySavedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySavedAndStatusIn(false, ProductStatus.ACQUIRING).size());

        Thread.sleep(Integer.parseInt(scheduledTasksDelay) + 1_000);

        Assert.assertEquals(3, productService
                .findBySavedAndStatusIn(false, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(13, productService
                .findBySavedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySavedAndStatusIn(false, ProductStatus.ACQUIRING).size());
    }

    private Product createProduct(String productName, String session, MetaProduct metaProduct, boolean saved,
            ProductStatus status, String... fileNames) {
        Product product = ProductBuilder.build(productName).withStatus(status).withMetaProduct(metaProduct)
                .withSaved(saved).withSession(session).withIngestProcessingChain(metaProduct.getIngestChain()).get();

        for (String acqf : fileNames) {
            product.addAcquisitionFile(acquisitionFileService.save(AcquisitionFileBuilder.build(acqf)
                    .withStatus(AcquisitionFileStatus.VALID.toString()).withMetaFile(metaFileMandatory).get()));
        }

        product.setSip(createSIP(productName));

        return productService.save(product);
    }

    private SIP createSIP(String productName) {
        SIPBuilder sipBuilder = new SIPBuilder(productName);
        sipBuilder.getPDIBuilder().addContextInformation("attribut-name",
                                                         productName + "-" + LocalDateTime.now().toString());
        return sipBuilder.build();
    }

}
