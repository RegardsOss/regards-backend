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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.builder.ChainGenerationBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.plugins.IPostProcessSipPlugin;
import fr.cnes.regards.modules.acquisition.service.conf.ChainGenerationServiceConfiguration;
import fr.cnes.regards.modules.acquisition.service.step.AbstractAcquisitionIT;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;

/**
 * @author Christophe Mertz
 *
 */
@ContextConfiguration(classes = { ChainGenerationServiceConfiguration.class })
@ActiveProfiles({ "test", "disableDataProviderTask", "testAmqp" })
@DirtiesContext
public class ProductSipEventHandlerIT extends AbstractAcquisitionIT {

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    private MetaProduct metaProduct001;

    private MetaProduct metaProduct002;

    private MetaProduct metaProduct003;

    private ChainGeneration chainForProcessing;

    @Before
    public void init() {

        metaProduct001 = metaProductService
                .save(MetaProductBuilder.build("meta-product-name-001").addMetaFile(metaFileOptional)
                        .addMetaFile(metaFileMandatory).withIngestProcessingChain("ingest-processing-chain-001").get());
        metaProduct002 = metaProductService.save(MetaProductBuilder.build("meta-product-name-002")
                .addMetaFile(metaFileMandatory).withIngestProcessingChain("ingest-processing-chain-002").get());
        metaProduct003 = metaProductService.save(MetaProductBuilder.build("meta-product-name-003")
                .addMetaFile(metaFileMandatory).withIngestProcessingChain("ingest-processing-chain-003").get());

        // Create a Product
        createProduct("product-001", "session-001", metaProduct001, true, ProductStatus.COMPLETED, "file-001",
                      "file-002");
        createProduct("product-002", "session-001", metaProduct001, true, ProductStatus.COMPLETED, "file-003",
                      "file-004");
        createProduct("product-003", "session-002", metaProduct001, true, ProductStatus.FINISHED, "file-005",
                      "file-006");

        createProduct("product-004", "session-001", metaProduct002, true, ProductStatus.COMPLETED, "file-007",
                      "file-008");
        createProduct("product-005", "session-001", metaProduct002, true, ProductStatus.COMPLETED, "file-009",
                      "file-010");
        createProduct("product-006", "session-001", metaProduct002, true, ProductStatus.COMPLETED, "file-011",
                      "file-012");
        createProduct("product-007", "session-002", metaProduct002, true, ProductStatus.COMPLETED, "file-013",
                      "file-014");
        createProduct("product-008", "session-002", metaProduct002, true, ProductStatus.COMPLETED, "file-015",
                      "file-016");
        createProduct("product-009", "session-002", metaProduct002, true, ProductStatus.COMPLETED, "file-017",
                      "file-018");
        createProduct("product-010", "session-002", metaProduct002, true, ProductStatus.FINISHED, "file-019");
        createProduct("product-011", "session-002", metaProduct002, true, ProductStatus.FINISHED, "file-020");
        createProduct("product-012", "session-002", metaProduct002, true, ProductStatus.COMPLETED, "file-021");
        createProduct("product-013", "session-002", metaProduct002, true, ProductStatus.COMPLETED, "file-022");
        createProduct("product-014", "session-002", metaProduct002, true, ProductStatus.COMPLETED, "file-023");
        createProduct("product-015", "session-002", metaProduct002, true, ProductStatus.COMPLETED, "file-024");
        createProduct("product-016", "session-002", metaProduct002, true, ProductStatus.COMPLETED, "file-025");

        createProduct("product-099", "session-002", metaProduct003, false, ProductStatus.ACQUIRING, "file-099");

        // Create a first generation chain
        chainForProcessing = chainService.save(ChainGenerationBuilder.build(CHAINE_LABEL + "for processing").isActive()
                .withDataSet(DATASET_IP_ID).withSession("session-001").get());

        // Set the MetaProduct to the ChainGeneration
        chainForProcessing.setMetaProduct(metaProduct001);
        chainForProcessing = chainService.save(chainForProcessing);
    }

    @Test
    public void receivedOneSipStoreEvent() throws InterruptedException, ModuleException {
        Assert.assertEquals(16, productService
                .findBySendedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING).size());

        chainForProcessing.setPostProcessSipPluginConf(pluginService
                .getPluginConfiguration("CleanOriginalFilePostPlugin", IPostProcessSipPlugin.class));

        chainService.save(chainForProcessing);

        runtimeTenantResolver.forceTenant(tenant);
        Product p1 = productService.retrieve("product-001");
        SIPEntity sip = new SIPEntity();
        sip.setIpId(p1.getProductName());
        sip.setState(SIPState.STORED);
        publisher.publish(new SIPEvent(sip));

        waitJobEvent();

        Assert.assertEquals(15, productService
                .findBySendedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySendedAndStatusIn(true, ProductStatus.SAVED).size());
        Assert.assertEquals(1, productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING).size());
    }

    @Test
    public void receivedSipStoreEventFailedNoProcessSipPluginDefined() throws InterruptedException {
        Assert.assertEquals(16, productService
                .findBySendedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING).size());

        runtimeTenantResolver.forceTenant(tenant);

        Product p1 = productService.retrieve("product-001");
        SIPEntity sip = new SIPEntity();
        sip.setIpId(p1.getProductName());
        sip.setState(SIPState.STORED);
        publisher.publish(new SIPEvent(sip));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertTrue(succeededs.isEmpty());
        Assert.assertEquals(1, faileds.size());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(16, productService
                .findBySendedAndStatusIn(true, ProductStatus.COMPLETED, ProductStatus.FINISHED).size());
        Assert.assertEquals(1, productService.findBySendedAndStatusIn(false, ProductStatus.ACQUIRING).size());
    }

}
