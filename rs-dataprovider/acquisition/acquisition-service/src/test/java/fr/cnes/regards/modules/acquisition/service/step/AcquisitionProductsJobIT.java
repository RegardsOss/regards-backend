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

import java.time.OffsetDateTime;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.builder.MetaFileBuilder;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain2;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.service.conf.AcquisitionProcessingChainConfiguration;
import fr.cnes.regards.modules.acquisition.service.conf.MockedFeignClientConf;
import fr.cnes.regards.modules.acquisition.service.plugins.TestScanDirectoryPlugin;

/**
 * @author Christophe Mertz
 *
 */
@ContextConfiguration(classes = { AcquisitionProcessingChainConfiguration.class, MockedFeignClientConf.class })
@ActiveProfiles({ "test", "disableDataProviderTask", "testAmqp" })
@DirtiesContext
public class AcquisitionProductsJobIT extends AcquisitionITHelper {

    @Test
    public void runActiveProcessingChain() throws ModuleException, InterruptedException {
        chain.setLastDateActivation(OffsetDateTime.now().minusDays(10));
        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration("TestScanDirectoryPlugin",
                                                                                IAcquisitionScanDirectoryPlugin.class));
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.PARAM_1_NAME,
                                          OffsetDateTime.now().minusDays(10).toString());
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.PARAM_2_NAME, chain.getLabel());

        chain.setCheckAcquisitionPluginConf(pluginService.getPluginConfiguration("BasicCheckFilePlugin",
                                                                                 ICheckFilePlugin.class));

        Assert.assertTrue(acqProcessChainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, acqProcessChainService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(2, metaFileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(7, acquisitionFileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(6, acquisitionFileService.findByStatus(AcquisitionFileState.VALID).size());
        Assert.assertEquals(1, acquisitionFileService.findByStatus(AcquisitionFileState.INVALID).size());
        Assert.assertEquals(5, productService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(1, productService.findByStatus(ProductState.FINISHED).size());
        Assert.assertEquals(4, productService.findByStatus(ProductState.COMPLETED).size());
        Assert.assertEquals(0, productService.findByStatus(ProductState.ERROR).size());

        chain = acqProcessChainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }

    @Test
    public void runActiveProcessingChainAcquireSameFilesWithSameChecksum()
            throws ModuleException, InterruptedException {
        this.chain.setPeriodicity(1L);

        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration("TestScanDirectoryPlugin",
                                                                                IAcquisitionScanDirectoryPlugin.class));
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.PARAM_1_NAME, "Hello param one");
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.PARAM_2_NAME, "Hello param two");

        Assert.assertTrue(acqProcessChainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        // Repeat the activation of the same chain
        chain = acqProcessChainService.retrieveComplete(chain.getId());

        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.PARAM_1_NAME, "Hello param one");
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.PARAM_2_NAME, "Hello param two");

        Assert.assertTrue(acqProcessChainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, acqProcessChainService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(2, metaFileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        // 6 mandatory and 1 optional
        Assert.assertEquals(7, acquisitionFileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(7, acquisitionFileService.findByStatus(AcquisitionFileState.IN_PROGRESS).size());

        chain = acqProcessChainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }

    /**
     * Acquire 3 products with an optional {@link MetaFile}. The {@link Product} should be {@link ProductState#COMPLETED}. 
     * @throws ModuleException
     * @throws InterruptedException
     */
    @Test
    public void runActiveProcessingChainOneProductWithThreeAcquisitionFilesWithOptionalMissing()
            throws ModuleException, InterruptedException {
        MetaFile secondMetaFileMandatory = metaFileRepository.save(MetaFileBuilder.build()
                .withInvalidFolder("/var/regards/data/invalid").withMediaType(MediaType.APPLICATION_JSON_VALUE)
                .withFilePattern("file pattern for the header file").comment("it is mandatory second").isMandatory()
                .get());
        metaProduct.addMetaFile(secondMetaFileMandatory);
        metaProductService.createOrUpdate(metaProduct);

        // Scan plugin
        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration(
                                                                                "TestScanProductsWithMultipleFilesPlugin",
                                                                                IAcquisitionScanDirectoryPlugin.class));

        // Check plugin
        chain.setCheckAcquisitionPluginConf(pluginService.getPluginConfiguration("CheckInPlugin",
                                                                                 ICheckFilePlugin.class));

        Assert.assertTrue(acqProcessChainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, acqProcessChainService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(3, metaFileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(6, acquisitionFileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(6, acquisitionFileService.findByStatus(AcquisitionFileState.VALID).size());
        Assert.assertEquals(0, acquisitionFileService.findByStatus(AcquisitionFileState.INVALID).size());
        Assert.assertEquals(3, productService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(3, productService.findByStatus(ProductState.COMPLETED).size());

        Assert.assertEquals(1, execProcessingChainService.retrieveAll(new PageRequest(0, 10)).getNumberOfElements());

        chain = acqProcessChainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }

    /**
     * Acquire 3 products with only mandatory {@link MetaFile}. The {@link Product} should be {@link ProductState#FINISHED}. 
     * @throws ModuleException
     * @throws InterruptedException
     */
    @Test
    public void runActiveProcessingChainOneProductWithThreeAcquisitionFiles()
            throws ModuleException, InterruptedException {
        MetaFile secondMetaFileMandatory = metaFileRepository.save(MetaFileBuilder.build()
                .withInvalidFolder("/var/regards/data/invalid").withMediaType(MediaType.APPLICATION_JSON_VALUE)
                .withFilePattern("file pattern for the header file").comment("it is mandatory second").isMandatory()
                .get());
        metaProduct.addMetaFile(secondMetaFileMandatory);
        metaProduct.removeMetaFile(metaFileOptional);
        metaProductService.createOrUpdate(metaProduct);

        // Scan plugin
        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration(
                                                                                "TestScanProductsWithMultipleFilesPlugin",
                                                                                IAcquisitionScanDirectoryPlugin.class));

        // Check plugin
        chain.setCheckAcquisitionPluginConf(pluginService.getPluginConfiguration("CheckInPlugin",
                                                                                 ICheckFilePlugin.class));

        Assert.assertTrue(acqProcessChainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, acqProcessChainService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(2, metaFileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(6, acquisitionFileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(6, acquisitionFileService.findByStatus(AcquisitionFileState.VALID).size());
        Assert.assertEquals(0, acquisitionFileService.findByStatus(AcquisitionFileState.INVALID).size());
        Assert.assertEquals(3, productService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(3, productService.findByStatus(ProductState.FINISHED).size());

        chain = acqProcessChainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }

    @Test
    public void runActiveProcessingChainProductsWithTwoAcquisitions() throws ModuleException, InterruptedException {
        MetaFile secondMetaFileMandatory = metaFileService.createOrUpdate(MetaFileBuilder.build()
                .withInvalidFolder("/var/regards/data/invalid").withMediaType(MediaType.APPLICATION_JSON_VALUE)
                .withFilePattern("file pattern for the header file").comment("it is mandatory second").isMandatory()
                .get());
        metaProduct.addMetaFile(secondMetaFileMandatory);
        metaProduct.removeMetaFile(metaFileOptional);
        metaProductService.createOrUpdate(metaProduct);

        // Scan plugin : only the data file are acquired
        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration("TestScanProductsData",
                                                                                IAcquisitionScanDirectoryPlugin.class));

        // Check plugin
        chain.setCheckAcquisitionPluginConf(pluginService.getPluginConfiguration("CheckInPlugin",
                                                                                 ICheckFilePlugin.class));

        Assert.assertTrue(acqProcessChainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, acqProcessChainService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(2, metaFileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(3, productService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(3, acquisitionFileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(3, acquisitionFileService.findByStatus(AcquisitionFileState.VALID).size());
        Assert.assertEquals(0, acquisitionFileService.findByStatus(AcquisitionFileState.INVALID).size());
        Assert.assertEquals(3, productService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        /*
         *  3 products are acquired but for each 1 mandatory file is missing 
         */
        Assert.assertEquals(3, productService.findByStatus(ProductState.ACQUIRING).size());

        for (Product product : productService.retrieveAll(new PageRequest(0, 10))) {
            Assert.assertFalse(product.isSended());
        }

        AcquisitionProcessingChain2 chainLastAcqDate = acqProcessChainService.retrieve(chain.getId());
        Assert.assertNotNull(chainLastAcqDate.getLastDateActivation());

        Thread.sleep(WAIT_TIME);

        // Scan plugin : the header files are acquired, the 3 products will be complete
        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration("TestScanProductsHeader",
                                                                                IAcquisitionScanDirectoryPlugin.class));

        // Repeat the activation of the same chain
        chain.setRunning(false);
        Assert.assertTrue(acqProcessChainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, acqProcessChainService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(2, metaFileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(6, acquisitionFileService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        Assert.assertEquals(6, acquisitionFileService.findByStatus(AcquisitionFileState.VALID).size());
        Assert.assertEquals(0, acquisitionFileService.findByStatus(AcquisitionFileState.INVALID).size());
        Assert.assertEquals(3, productService.retrieveAll(new PageRequest(0, 10)).getTotalElements());
        /**
         * the 3 products are finished
         */
        Assert.assertEquals(3, productService.findByStatus(ProductState.FINISHED).size());
    }

    @Test
    public void runActiveProcessingChainWithoutScanPlugin() throws InterruptedException {
        Assert.assertTrue(acqProcessChainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());
    }

    @Test
    public void runNoActiveChainGeneration() throws InterruptedException {
        this.chain.setActive(false);

        Assert.assertFalse(acqProcessChainService.run(chain));
    }

    @Test
    public void runActiveChainGenerationWithoutMetaProduct() throws InterruptedException {
        this.chain.setMetaProduct(null);

        Assert.assertTrue(acqProcessChainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertTrue(succeededs.isEmpty());
        Assert.assertFalse(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());
    }

    @Test
    public void runChainGenerationPeriodicity() throws InterruptedException {
        this.chain.setActive(true);
        this.chain.setLastDateActivation(OffsetDateTime.now().minusHours(1));
        this.chain.setPeriodicity(3650L);

        Assert.assertFalse(acqProcessChainService.run(chain));
    }

}
