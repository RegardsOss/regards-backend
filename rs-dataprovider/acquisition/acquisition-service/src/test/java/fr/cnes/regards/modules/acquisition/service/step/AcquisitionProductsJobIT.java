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
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.builder.MetaFileBuilder;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.SetOfMetaFileDto;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.service.conf.ChainGenerationServiceConfiguration;
import fr.cnes.regards.modules.acquisition.service.conf.MockedFeignClientConf;
import fr.cnes.regards.modules.acquisition.service.plugins.BasicCheckFilePlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.CheckInPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.TestScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.TestScanProductsData;
import fr.cnes.regards.modules.acquisition.service.plugins.TestScanProductsHeader;
import fr.cnes.regards.modules.acquisition.service.plugins.TestScanProductsWithMultipleFilesPlugin;

/**
 * @author Christophe Mertz
 *
 */
@ContextConfiguration(classes = { ChainGenerationServiceConfiguration.class, MockedFeignClientConf.class })
@ActiveProfiles({ "test", "disableDataProviderTask", "testAmqp" })
@DirtiesContext
public class AcquisitionProductsJobIT extends AbstractAcquisitionIT {

    @Test
    public void runActiveChainGeneration() throws ModuleException, InterruptedException {
        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFileMandatory);
        metaFiles.add(metaFileOptional);

        String metaFilesJson = new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles));
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));

        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration("TestScanDirectoryPlugin",
                                                                                IAcquisitionScanDirectoryPlugin.class));
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.META_PRODUCT_PARAM, metaProductJson);
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.META_FILE_PARAM, metaFilesJson);
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.CHAIN_GENERATION_PARAM, chain.getLabel());
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.LAST_ACQ_DATE_PARAM,
                                          OffsetDateTime.now().minusDays(10).toString());

        chain.setCheckAcquisitionPluginConf(pluginService.getPluginConfiguration("BasicCheckFilePlugin",
                                                                                 ICheckFilePlugin.class));
        chain.addCheckAcquisitionParameter(BasicCheckFilePlugin.META_PRODUCT_PARAM, metaProductJson);
        chain.addCheckAcquisitionParameter(BasicCheckFilePlugin.META_FILE_PARAM, metaFilesJson);
        chain.addCheckAcquisitionParameter(BasicCheckFilePlugin.CHAIN_GENERATION_PARAM, chain.getLabel());

        Assert.assertTrue(chainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        // 5 products are ready to be send to ingest
        Assert.assertEquals(5, faileds.size()); // because there is no GenerateSIPPluginis defined for the chain
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(2, metaFileService.retrieveAll().size());
        Assert.assertEquals(7, acquisitionFileService.retrieveAll().size());
        Assert.assertEquals(6, acquisitionFileService.findByStatus(AcquisitionFileStatus.VALID).size());
        Assert.assertEquals(1, acquisitionFileService.findByStatus(AcquisitionFileStatus.INVALID).size());
        Assert.assertEquals(5, productService.retrieveAll().size());
        Assert.assertEquals(1, productService.findByStatus(ProductStatus.FINISHED).size());
        Assert.assertEquals(4, productService.findByStatus(ProductStatus.COMPLETED).size());
        Assert.assertEquals(0, productService.findByStatus(ProductStatus.ERROR).size());

        chain = chainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }

    @Test
    public void runActiveChainGenerationAcquireSameFilesWithSameChecksum()
            throws ModuleException, InterruptedException {
        this.chain.setPeriodicity(1L);

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFileMandatory);
        metaFiles.add(metaFileOptional);

        String metaFilesJson = new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles));
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));

        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration("TestScanDirectoryPlugin",
                                                                                IAcquisitionScanDirectoryPlugin.class));
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.META_PRODUCT_PARAM, metaProductJson);
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.META_FILE_PARAM, metaFilesJson);
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.CHAIN_GENERATION_PARAM, chain.getLabel());
        chain.addScanAcquisitionParameter(TestScanDirectoryPlugin.LAST_ACQ_DATE_PARAM,
                                          OffsetDateTime.now().minusDays(10).toString());

        Assert.assertTrue(chainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        // Repeat the activation of the same chain
        chain.setRunning(false);
        Assert.assertTrue(chainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(2, metaFileService.retrieveAll().size());
        // 6 mandatory and 1 optional
        Assert.assertEquals(7, acquisitionFileService.retrieveAll().size());
        Assert.assertEquals(7, acquisitionFileService.findByStatus(AcquisitionFileStatus.IN_PROGRESS).size());

        chain = chainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }

    /**
     * Acquire 3 products with an optional {@link MetaFile}. The {@link Product} should be {@link ProductStatus#COMPLETED}. 
     * @throws ModuleException
     * @throws InterruptedException
     */
    @Test
    public void runActiveChainGenerationOneProductWithThreeAcquisitionFilesWithOptionalMissing()
            throws ModuleException, InterruptedException {
        MetaFile secondMetaFileMandatory = metaFileService.save(MetaFileBuilder.build()
                .withInvalidFolder("/var/regards/data/invalid").withFileType(MediaType.APPLICATION_JSON_VALUE)
                .withFilePattern("file pattern for the header file").comment("it is mandatory second").isMandatory()
                .get());
        metaProduct.addMetaFile(secondMetaFileMandatory);

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFileMandatory);
        metaFiles.add(secondMetaFileMandatory);
        metaFiles.add(metaFileOptional);

        String metaFilesJson = new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles));
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));

        // Scan plugin
        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration(
                                                                                "TestScanProductsWithMultipleFilesPlugin",
                                                                                IAcquisitionScanDirectoryPlugin.class));
        chain.addScanAcquisitionParameter(TestScanProductsWithMultipleFilesPlugin.META_PRODUCT_PARAM, metaProductJson);
        chain.addScanAcquisitionParameter(TestScanProductsWithMultipleFilesPlugin.META_FILE_PARAM, metaFilesJson);
        chain.addScanAcquisitionParameter(TestScanProductsWithMultipleFilesPlugin.CHAIN_GENERATION_PARAM,
                                          chain.getLabel());
        chain.addScanAcquisitionParameter(TestScanProductsWithMultipleFilesPlugin.LAST_ACQ_DATE_PARAM,
                                          OffsetDateTime.now().minusDays(10).toString());

        // Check plugin
        chain.setCheckAcquisitionPluginConf(pluginService.getPluginConfiguration("CheckInPlugin",
                                                                                 ICheckFilePlugin.class));
        chain.addCheckAcquisitionParameter(CheckInPlugin.CHAIN_GENERATION_PARAM, chain.getLabel());

        Assert.assertTrue(chainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertEquals(3, faileds.size()); // because there is no GenerateSIPPlugin defined for the chain
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(3, metaFileService.retrieveAll().size());
        Assert.assertEquals(6, acquisitionFileService.retrieveAll().size());
        Assert.assertEquals(6, acquisitionFileService.findByStatus(AcquisitionFileStatus.VALID).size());
        Assert.assertEquals(0, acquisitionFileService.findByStatus(AcquisitionFileStatus.INVALID).size());
        Assert.assertEquals(3, productService.retrieveAll().size());
        Assert.assertEquals(3, productService.findByStatus(ProductStatus.COMPLETED).size());

        chain = chainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }

    /**
     * Acquire 3 products with only mandatory {@link MetaFile}. The {@link Product} should be {@link ProductStatus#FINISHED}. 
     * @throws ModuleException
     * @throws InterruptedException
     */
    @Test
    public void runActiveChainGenerationOneProductWithThreeAcquisitionFiles()
            throws ModuleException, InterruptedException {
        MetaFile secondMetaFileMandatory = metaFileService.save(MetaFileBuilder.build()
                .withInvalidFolder("/var/regards/data/invalid").withFileType(MediaType.APPLICATION_JSON_VALUE)
                .withFilePattern("file pattern for the header file").comment("it is mandatory second").isMandatory()
                .get());
        metaProduct.addMetaFile(secondMetaFileMandatory);
        metaProduct.removeMetaFile(metaFileOptional);

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFileMandatory);
        metaFiles.add(secondMetaFileMandatory);

        String metaFilesJson = new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles));
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));

        // Scan plugin
        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration(
                                                                                "TestScanProductsWithMultipleFilesPlugin",
                                                                                IAcquisitionScanDirectoryPlugin.class));
        chain.addScanAcquisitionParameter(TestScanProductsWithMultipleFilesPlugin.META_PRODUCT_PARAM, metaProductJson);
        chain.addScanAcquisitionParameter(TestScanProductsWithMultipleFilesPlugin.META_FILE_PARAM, metaFilesJson);
        chain.addScanAcquisitionParameter(TestScanProductsWithMultipleFilesPlugin.CHAIN_GENERATION_PARAM,
                                          chain.getLabel());
        chain.addScanAcquisitionParameter(TestScanProductsWithMultipleFilesPlugin.LAST_ACQ_DATE_PARAM,
                                          OffsetDateTime.now().minusDays(10).toString());

        // Check plugin
        chain.setCheckAcquisitionPluginConf(pluginService.getPluginConfiguration("CheckInPlugin",
                                                                                 ICheckFilePlugin.class));
        chain.addCheckAcquisitionParameter(CheckInPlugin.CHAIN_GENERATION_PARAM, chain.getLabel());

        Assert.assertTrue(chainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertEquals(3, faileds.size()); // because there is no GenerateSIPPlugin defined for the chain
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(3, metaFileService.retrieveAll().size());
        Assert.assertEquals(6, acquisitionFileService.retrieveAll().size());
        Assert.assertEquals(6, acquisitionFileService.findByStatus(AcquisitionFileStatus.VALID).size());
        Assert.assertEquals(0, acquisitionFileService.findByStatus(AcquisitionFileStatus.INVALID).size());
        Assert.assertEquals(3, productService.retrieveAll().size());
        Assert.assertEquals(3, productService.findByStatus(ProductStatus.FINISHED).size());

        chain = chainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }

    @Test
    public void runActiveChainGenerationProductsWithTwoAcquisitions() throws ModuleException, InterruptedException {
        MetaFile secondMetaFileMandatory = metaFileService.save(MetaFileBuilder.build()
                .withInvalidFolder("/var/regards/data/invalid").withFileType(MediaType.APPLICATION_JSON_VALUE)
                .withFilePattern("file pattern for the header file").comment("it is mandatory second").isMandatory()
                .get());
        metaProduct.addMetaFile(secondMetaFileMandatory);
        metaProduct.removeMetaFile(metaFileOptional);

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFileMandatory);
        metaFiles.add(secondMetaFileMandatory);

        String metaFilesJson = new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles));
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));

        // Scan plugin
        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration("TestScanProductsData",
                                                                                IAcquisitionScanDirectoryPlugin.class));
        chain.addScanAcquisitionParameter(TestScanProductsData.META_PRODUCT_PARAM, metaProductJson);
        chain.addScanAcquisitionParameter(TestScanProductsData.META_FILE_PARAM, metaFilesJson);
        chain.addScanAcquisitionParameter(TestScanProductsData.CHAIN_GENERATION_PARAM, chain.getLabel());
        chain.addScanAcquisitionParameter(TestScanProductsData.LAST_ACQ_DATE_PARAM,
                                          OffsetDateTime.now().minusDays(10).toString());

        // Check plugin
        chain.setCheckAcquisitionPluginConf(pluginService.getPluginConfiguration("CheckInPlugin",
                                                                                 ICheckFilePlugin.class));
        chain.addCheckAcquisitionParameter(CheckInPlugin.CHAIN_GENERATION_PARAM, chain.getLabel());

        Assert.assertTrue(chainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(3, metaFileService.retrieveAll().size());
        Assert.assertEquals(3, acquisitionFileService.retrieveAll().size());
        Assert.assertEquals(3, acquisitionFileService.findByStatus(AcquisitionFileStatus.VALID).size());
        Assert.assertEquals(0, acquisitionFileService.findByStatus(AcquisitionFileStatus.INVALID).size());
        Assert.assertEquals(3, productService.retrieveAll().size());
        /*
         *  3 products are acquired but for each 1 mandatory file is missing 
         */
        Assert.assertEquals(3, productService.findByStatus(ProductStatus.ACQUIRING).size());

        for (Product product : productService.retrieveAll()) {
            Assert.assertFalse(product.isSended());
        }

        ChainGeneration chainLastAcqDate = chainService.retrieve(chain.getId());
        Assert.assertNotNull(chainLastAcqDate.getLastDateActivation());

        Thread.sleep(5_000);

        // Scan plugin : in this step we acquire the 3 mandatory missing files
        metaProductJson = new Gson()
                .toJson(MetaProductDto.fromMetaProduct(metaProductService.retrieveComplete(metaProduct.getId())));
        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration("TestScanProductsHeader",
                                                                                IAcquisitionScanDirectoryPlugin.class));
        chain.addScanAcquisitionParameter(TestScanProductsHeader.META_PRODUCT_PARAM, metaProductJson);
        chain.addScanAcquisitionParameter(TestScanProductsHeader.META_FILE_PARAM, metaFilesJson);
        chain.addScanAcquisitionParameter(TestScanProductsHeader.CHAIN_GENERATION_PARAM, chain.getLabel());
        chain.addScanAcquisitionParameter(TestScanProductsHeader.LAST_ACQ_DATE_PARAM,
                                          chainLastAcqDate.getLastDateActivation().toString());

        chain.setRunning(false);
        Assert.assertTrue(chainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertEquals(3, faileds.size());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(3, metaFileService.retrieveAll().size());
        Assert.assertEquals(6, acquisitionFileService.retrieveAll().size());
        Assert.assertEquals(6, acquisitionFileService.findByStatus(AcquisitionFileStatus.VALID).size());
        Assert.assertEquals(0, acquisitionFileService.findByStatus(AcquisitionFileStatus.INVALID).size());
        Assert.assertEquals(3, productService.retrieveAll().size());
        /**
         * the 3 products are finished
         */
        Assert.assertEquals(3, productService.findByStatus(ProductStatus.FINISHED).size());
    }

    @Test
    public void runActiveChainGenerationWithoutScanPlugin() throws InterruptedException {
        Assert.assertTrue(chainService.run(chain));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertTrue(succeededs.isEmpty());
        Assert.assertFalse(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());
    }

    @Test
    public void runNoActiveChainGeneration() throws InterruptedException {
        this.chain.setActive(false);

        Assert.assertFalse(chainService.run(chain));
    }

    @Test
    public void runActiveChainGenerationWithoutMetaProduct() throws InterruptedException {
        this.chain.setMetaProduct(null);

        Assert.assertTrue(chainService.run(chain));

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

        Assert.assertFalse(chainService.run(chain));
    }

}
