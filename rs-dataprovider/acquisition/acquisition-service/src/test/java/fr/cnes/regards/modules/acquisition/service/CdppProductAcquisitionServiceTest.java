/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileInfoRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.StorageMetadataDProvider;
import fr.cnes.regards.modules.acquisition.plugins.Arcad3IsoprobeDensiteProductPlugin;
import fr.cnes.regards.modules.acquisition.service.job.AcquisitionJobPriority;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/**
 * Test {@link AcquisitionProcessingService} for {@link Product} workflow
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_cdpp_product" })
public class CdppProductAcquisitionServiceTest extends AbstractMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(CdppProductAcquisitionServiceTest.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Autowired
    private IProductService productService;

    @Autowired
    private IJobInfoService jobInfoService;

    @SuppressWarnings("unused")
    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IAcquisitionFileInfoRepository fileInfoRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IAcquisitionProcessingChainRepository acquisitionProcessingChainRepository;

    @Before
    public void before() throws ModuleException {
        acqFileRepository.deleteAll();
        fileInfoRepository.deleteAll();
        acquisitionProcessingChainRepository.deleteAll();
        for(PluginConfiguration pc: pluginService.getAllPluginConfigurations()) {
            pluginService.deletePluginConfiguration(pc.getBusinessId());
        }
    }

    public AcquisitionProcessingChain createProcessingChain() throws ModuleException {

        // Pathes
        Path basePath = Paths.get("src", "test", "resources", "data", "plugins", "cdpp");
        Path dataPath = basePath.resolve("data");
        Path browsePath = basePath.resolve("browse");

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("Product");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");

        // RAW DATA file infos
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("RAWDATA");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);

        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(GlobDiskScanning.FIELD_DIRS,
                PluginParameterTransformer.toJson(Arrays.asList(dataPath.toString()))));

        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(parameters, GlobDiskScanning.class);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin RAWDATA" + Math.random());
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // QUICKLOOK SD file infos
        fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("QUICKLOOK_SD");
        fileInfo.setMimeType(MediaType.IMAGE_PNG);
        fileInfo.setDataType(DataType.QUICKLOOK_SD);

        parameters = IPluginParam.set(IPluginParam.build(GlobDiskScanning.FIELD_DIRS,
                PluginParameterTransformer.toJson(Arrays.asList(browsePath.toString()))),
                IPluginParam.build(GlobDiskScanning.FIELD_GLOB, "*B.png"));

        scanPlugin = PluginUtils.getPluginConfiguration(parameters, GlobDiskScanning.class);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin QUICKLOOK_SD" + Math.random());
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // QUICKLOOK MD file infos
        fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("QUICKLOOK_MD");
        fileInfo.setMimeType(MediaType.IMAGE_PNG);
        fileInfo.setDataType(DataType.QUICKLOOK_MD);

        parameters = IPluginParam.set(IPluginParam.build(GlobDiskScanning.FIELD_DIRS,
                PluginParameterTransformer.toJson(Arrays.asList(browsePath.toString()))),
                IPluginParam.build(GlobDiskScanning.FIELD_GLOB, "*C.png"));

        scanPlugin = PluginUtils.getPluginConfiguration(parameters, GlobDiskScanning.class);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin QUICKLOOK_MD" + Math.random());
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                                  DefaultFileValidation.class);
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin" + Math.random());
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginUtils
                .getPluginConfiguration(Sets.newHashSet(), Arcad3IsoprobeDensiteProductPlugin.class);
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin" + Math.random());
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                              DefaultSIPGeneration.class);
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin" + Math.random());
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        // Save processing chain
        processingChain = processingService.createChain(processingChain);

        List<StorageMetadataDProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataDProvider.build("AWS", "/path/to/file"));
        storages.add(StorageMetadataDProvider.build("HELLO", "/other/path/to/file"));
        processingChain.setStorages(storages);

        // we need to set up a fake ProductAcquisitionJob to fill its attributes
        JobInfo jobInfo = new JobInfo(true);
        jobInfo.setPriority(AcquisitionJobPriority.PRODUCT_ACQUISITION_JOB_PRIORITY.getPriority());
        jobInfo.setParameters(new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER_ID, processingChain.getId()),
                new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER_SESSION, "my funky session"));
        jobInfo.setClassName(ProductAcquisitionJob.class.getName());
        jobInfo.setOwner("user 1");
        jobInfoService.createAsQueued(jobInfo);

        processingChain.setLastProductAcquisitionJobInfo(jobInfo);

        return processingService.updateChain(processingChain);
    }

    @Test
    public void acquisitionWorkflowTest() throws ModuleException {

        AcquisitionProcessingChain processingChain = createProcessingChain();
        //AcquisitionProcessingChain processingChain = processingService.getFullChains().get(0);

        processingService.scanAndRegisterFiles(processingChain);

        // Check registered files
        for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {
            Page<AcquisitionFile> inProgressFiles = acqFileRepository
                    .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS, fileInfo,
                                                        PageRequest.of(0, 1));
            Assert.assertTrue(inProgressFiles.getTotalElements() == 1);
        }

        processingService.manageRegisteredFiles(processingChain);

        // Check registered files
        for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {
            Page<AcquisitionFile> inProgressFiles = acqFileRepository
                    .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS, fileInfo,
                                                        PageRequest.of(0, 1));
            Assert.assertTrue(inProgressFiles.getTotalElements() == 0);

            Page<AcquisitionFile> validFiles = acqFileRepository
                    .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.VALID, fileInfo, PageRequest.of(0, 1));
            Assert.assertTrue(validFiles.getTotalElements() == 0);

            Page<AcquisitionFile> acquiredFiles = acqFileRepository
                    .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.ACQUIRED, fileInfo, PageRequest.of(0, 1));
            Assert.assertTrue(acquiredFiles.getTotalElements() == 1);
        }

        // Find product to schedule
        long scheduled = productService.countByProcessingChainAndSipStateIn(processingChain,
                                                                            Arrays.asList(ProductSIPState.SCHEDULED));
        Assert.assertEquals(1, scheduled);

        // Run the job synchronously
        SIPGenerationJob genJob = new SIPGenerationJob();
        beanFactory.autowireBean(genJob);

        Map<String, JobParameter> parameters = new HashMap<>();
        parameters.put(SIPGenerationJob.CHAIN_PARAMETER_ID,
                       new JobParameter(SIPGenerationJob.CHAIN_PARAMETER_ID, processingChain.getId()));
        Set<String> productNames = new HashSet<>();
        productNames.add("ISO_DENS_20330518_0533");
        parameters.put(SIPGenerationJob.PRODUCT_NAMES, new JobParameter(SIPGenerationJob.PRODUCT_NAMES, productNames));

        genJob.setParameters(parameters);
        genJob.run();

        // Find product to submitted
        long submitted = productService.countByProcessingChainAndSipStateIn(processingChain,
                                                                            Arrays.asList(ProductSIPState.SUBMITTED));
        Assert.assertTrue(submitted == 1);
    }
}
