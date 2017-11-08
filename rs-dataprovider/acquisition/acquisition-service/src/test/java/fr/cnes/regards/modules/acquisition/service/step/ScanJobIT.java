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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.SetOfMetaFileDto;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.IChainGenerationService;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.conf.ChainGenerationServiceConfiguration;
import fr.cnes.regards.modules.acquisition.service.conf.MockedFeignClientConf;
import fr.cnes.regards.modules.acquisition.service.plugins.BasicCheckFilePlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.CheckInPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.TestGenerateSipPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.TestScanDirectoryOneProductWithMultipleFilesPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.TestScanDirectoryPlugin;

/**
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ChainGenerationServiceConfiguration.class, MockedFeignClientConf.class })
@ActiveProfiles({ "test" })
@DirtiesContext
public class ScanJobIT extends AbstractAcquisitionIT {

    @Autowired
    private IChainGenerationService chainService;

    @Autowired
    private IMetaFileService metaFileService;

    @Autowired
    private IProductService productService;

    @Autowired
    private IAcquisitionFileService acquisitionFileService;

    @Autowired
    private IPluginService pluginService;

    @Test
    public void runActiveChainGeneration() throws ModuleException, InterruptedException {
        mockIngestClientResponseOK();

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

        chain.setGenerateSIPPluginConf(pluginService.getPluginConfiguration("TestGenerateSipPlugin",
                                                                            IGenerateSIPPlugin.class));
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.META_PRODUCT_PARAM, metaProductJson);
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.SESSION_PARAM, chain.getSession());
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.INGEST_PROCESSING_CHAIN_PARAM,
                                      chain.getIngestProcessingChain());

        Assert.assertTrue(chainService.run(chain));

//        waitJob(WAIT_TIME);
        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(2, metaFileService.retrieveAll().size());
        Assert.assertEquals(7, acquisitionFileService.retrieveAll().size());
        Assert.assertEquals(6, acquisitionFileService.findByStatus(AcquisitionFileStatus.VALID).size());
        Assert.assertEquals(1, acquisitionFileService.findByStatus(AcquisitionFileStatus.INVALID).size());
        Assert.assertEquals(5, productService.retrieveAll().size());
        Assert.assertEquals(5, productService.findByStatus(ProductStatus.FINISHED).size());
        Assert.assertEquals(0, productService.findByStatus(ProductStatus.ERROR).size());

        chain = chainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }
    
    @Test
    public void runActiveChainGenerationAcquireSameFilesWithSameChecksum()
            throws ModuleException, InterruptedException {
        mockIngestClientResponseOK();

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

        chain.setGenerateSIPPluginConf(pluginService.getPluginConfiguration("TestGenerateSipPlugin",
                                                                            IGenerateSIPPlugin.class));
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.META_PRODUCT_PARAM, metaProductJson);
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.SESSION_PARAM, chain.getSession());
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.INGEST_PROCESSING_CHAIN_PARAM,
                                      chain.getIngestProcessingChain());

        Assert.assertTrue(chainService.run(chain));

//        waitJob(WAIT_TIME);
        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());
        
//        resetJobQueue();

        // Repeat the activation of the same chain
        Assert.assertTrue(chainService.run(chain));

//        waitJob(WAIT_TIME);
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
    
    @Test
    public void runActiveChainGenerationOneProductWithThreeAcquisitionFiles()
            throws ModuleException, InterruptedException {
        mockIngestClientResponseOK();

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFileMandatory);

        String metaFilesJson = new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles));
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));

        // Scan plugin
        chain.setScanAcquisitionPluginConf(pluginService.getPluginConfiguration(
                                                                                "TestScanDirectoryOneProductWithMultipleFilesPlugin",
                                                                                IAcquisitionScanDirectoryPlugin.class));
        chain.addScanAcquisitionParameter(TestScanDirectoryOneProductWithMultipleFilesPlugin.META_PRODUCT_PARAM,
                                          metaProductJson);
        chain.addScanAcquisitionParameter(TestScanDirectoryOneProductWithMultipleFilesPlugin.META_FILE_PARAM,
                                          metaFilesJson);
        chain.addScanAcquisitionParameter(TestScanDirectoryOneProductWithMultipleFilesPlugin.CHAIN_GENERATION_PARAM,
                                          chain.getLabel());
        chain.addScanAcquisitionParameter(TestScanDirectoryOneProductWithMultipleFilesPlugin.LAST_ACQ_DATE_PARAM,
                                          OffsetDateTime.now().minusDays(10).toString());

        // Check plugin
        chain.setCheckAcquisitionPluginConf(pluginService.getPluginConfiguration("CheckInPlugin",
                                                                                 ICheckFilePlugin.class));
        chain.addCheckAcquisitionParameter(CheckInPlugin.CHAIN_GENERATION_PARAM, chain.getLabel());

        // Generate SIP plugin
        chain.setGenerateSIPPluginConf(pluginService.getPluginConfiguration("TestGenerateSipPlugin",
                                                                            IGenerateSIPPlugin.class));
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.META_PRODUCT_PARAM, metaProductJson);
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.INGEST_PROCESSING_CHAIN_PARAM,
                                      chain.getIngestProcessingChain());

        Assert.assertTrue(chainService.run(chain));

//        waitJob(WAIT_TIME);
        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(2, metaFileService.retrieveAll().size());
        Assert.assertEquals(6, acquisitionFileService.retrieveAll().size());
        Assert.assertEquals(6, acquisitionFileService.findByStatus(AcquisitionFileStatus.VALID).size());
        Assert.assertEquals(0, acquisitionFileService.findByStatus(AcquisitionFileStatus.INVALID).size());
        Assert.assertEquals(3, productService.retrieveAll().size());

        chain = chainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }

    @Test
    public void runActiveChainGenerationPartialContent() throws ModuleException, InterruptedException {
        mockIngestClientResponsePartialContent();

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFileOptional);
        metaFiles.add(metaFileMandatory);

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

        chain.setGenerateSIPPluginConf(pluginService.getPluginConfiguration("TestGenerateSipPlugin",
                                                                            IGenerateSIPPlugin.class));
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.META_PRODUCT_PARAM, metaProductJson);
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.SESSION_PARAM, chain.getSession());
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.INGEST_PROCESSING_CHAIN_PARAM,
                                      chain.getIngestProcessingChain());

        Assert.assertTrue(chainService.run(chain));

//        waitJob(WAIT_TIME);
        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(2, metaFileService.retrieveAll().size());
        Assert.assertEquals(7, acquisitionFileService.retrieveAll().size());
        Assert.assertEquals(6, acquisitionFileService.findByStatus(AcquisitionFileStatus.VALID).size());
        Assert.assertEquals(1, acquisitionFileService.findByStatus(AcquisitionFileStatus.INVALID).size());
        Assert.assertEquals(5, productService.retrieveAll().size());
        Assert.assertEquals(3, productService.findByStatus(ProductStatus.FINISHED).size());
        Assert.assertEquals(2, productService.findByStatus(ProductStatus.ERROR).size());

        chain = chainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }
    @Test
    public void runActiveChainGenerationUnauthorized() throws ModuleException, InterruptedException {
        mockIngestClientResponseUnauthorized();

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

        chain.setGenerateSIPPluginConf(pluginService.getPluginConfiguration("TestGenerateSipPlugin",
                                                                            IGenerateSIPPlugin.class));
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.META_PRODUCT_PARAM, metaProductJson);
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.SESSION_PARAM, chain.getSession());
        chain.addGenerateSIPParameter(TestGenerateSipPlugin.INGEST_PROCESSING_CHAIN_PARAM,
                                      chain.getIngestProcessingChain());

        Assert.assertTrue(chainService.run(chain));

        //        waitJob(WAIT_TIME);
        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(2, metaFileService.retrieveAll().size());
        Assert.assertEquals(7, acquisitionFileService.retrieveAll().size());
        Assert.assertEquals(6, acquisitionFileService.findByStatus(AcquisitionFileStatus.VALID).size());
        Assert.assertEquals(1, acquisitionFileService.findByStatus(AcquisitionFileStatus.INVALID).size());
        Assert.assertEquals(5, productService.retrieveAll().size());

        chain = chainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }

    
    @Test
    public void runActiveChainGenerationWithoutScanPlugin() throws InterruptedException {
        Assert.assertTrue(chainService.run(chain));

//        waitJob(WAIT_TIME);
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

//        waitJob(WAIT_TIME);
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
