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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.modules.acquisition.domain.chain.StorageMetadataProvider;
import fr.cnes.regards.modules.acquisition.service.session.SessionNotifier;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

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
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMonitor;
import fr.cnes.regards.modules.acquisition.service.job.AcquisitionJobPriority;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;

/**
 * Test {@link AcquisitionProcessingService} for {@link Product} workflow
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_product" })
public class ProductAcquisitionServiceTest extends AbstractMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAcquisitionServiceTest.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Autowired
    private IProductService productService;

    @SuppressWarnings("unused")
    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IAcquisitionFileService fileService;

    @Autowired
    private IAcquisitionFileInfoRepository fileInfoRepository;

    @Autowired
    private IAcquisitionProcessingChainRepository acquisitionProcessingChainRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IJobInfoService jobInfoService;

    @SpyBean
    private IPublisher publisher;

    @Before
    public void before() throws ModuleException {
        acqFileRepository.deleteAll();
        fileInfoRepository.deleteAll();
        acquisitionProcessingChainRepository.deleteAll();
        for (PluginConfiguration pc : pluginService.getAllPluginConfigurations()) {
            pluginService.deletePluginConfiguration(pc.getBusinessId());
        }
    }

    public AcquisitionProcessingChain createProcessingChain(Path searchDir) throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("Product");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(GlobDiskScanning.FIELD_DIRS,
                                        PluginParameterTransformer.toJson(Arrays.asList(searchDir.toString()))));

        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(parameters, GlobDiskScanning.class);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                                  DefaultFileValidation.class);
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                               DefaultProductPlugin.class);
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                              DefaultSIPGeneration.class);
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("AWS", "/path/to/file"));
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file"));
        processingChain.setStorages(storages);

        // Save processing chain
        processingChain = processingService.createChain(processingChain);

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

    //    @Test
    //    public void scanTest() throws ModuleException {
    //
    //        AcquisitionProcessingChain processingChain = createProcessingChain(Paths.get("src", "test", "resources", "data",
    //                                                                                     "plugins", "scan"));
    //        processingService.scanAndRegisterFiles(processingChain);
    //
    //        processingService.scanAndRegisterFiles(processingChain);
    //
    //        processingService.scanAndRegisterFiles(processingChain);
    //    }

    @Test
    public void acquisitionWorkflowTest() throws ModuleException {

        AcquisitionProcessingChain processingChain = createProcessingChain(Paths.get("src", "test", "resources", "data",
                                                                                     "plugins", "scan"));
        AcquisitionFileInfo fileInfo = processingChain.getFileInfos().iterator().next();

        processingService.scanAndRegisterFiles(processingChain);

        // Check registered files
        Page<AcquisitionFile> inProgressFiles = acqFileRepository
                .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS, fileInfo, PageRequest.of(0, 1));
        Assert.assertTrue(inProgressFiles.getTotalElements() == 4);

        processingService.manageRegisteredFiles(processingChain);

        // Check registered files
        inProgressFiles = acqFileRepository
                .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS,
                                                    processingChain.getFileInfos().iterator().next(),
                                                    PageRequest.of(0, 1));
        Assert.assertTrue(inProgressFiles.getTotalElements() == 0);

        Page<AcquisitionFile> validFiles = acqFileRepository
                .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.VALID, fileInfo, PageRequest.of(0, 1));
        Assert.assertTrue(validFiles.getTotalElements() == 0);

        Page<AcquisitionFile> acquiredFiles = acqFileRepository
                .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.ACQUIRED, fileInfo, PageRequest.of(0, 1));
        Assert.assertTrue(acquiredFiles.getTotalElements() == 4);

        // Find product to schedule
        long scheduled = productService.countByProcessingChainAndSipStateIn(processingChain,
                                                                            Arrays.asList(ProductSIPState.SCHEDULED));
        Assert.assertTrue(scheduled == 4);

        //        // Test job algo synchronously
        //        for (Product product : products) {
        //
        //            SIPGenerationJob genJob = new SIPGenerationJob();
        //            beanFactory.autowireBean(genJob);
        //
        //            Map<String, JobParameter> parameters = new HashMap<>();
        //            parameters.put(SIPGenerationJob.CHAIN_PARAMETER_ID,
        //                           new JobParameter(SIPGenerationJob.CHAIN_PARAMETER_ID, processingChain.getId()));
        //            parameters.put(SIPGenerationJob.PRODUCT_ID, new JobParameter(SIPGenerationJob.PRODUCT_ID, product.getId()));
        //
        //            genJob.setParameters(parameters);
        //            genJob.run();
        //        }

        Assert.assertTrue(fileService.countByChain(processingChain) == 4);
        Assert.assertTrue(fileService.countByChainAndStateIn(processingChain,
                                                             Arrays.asList(AcquisitionFileState.ACQUIRED)) == 4);
        Assert.assertTrue(fileService.countByChainAndStateIn(processingChain,
                                                             Arrays.asList(AcquisitionFileState.ERROR)) == 0);

        Page<AcquisitionProcessingChainMonitor> monitor = processingService
                .buildAcquisitionProcessingChainSummaries(null, null, null, PageRequest.of(0, 10));
        Assert.assertTrue(!monitor.getContent().isEmpty());
        Assert.assertEquals(true, monitor.getContent().get(0).isActive());

        // Check product
        Assert.assertEquals(0, productService.countByProcessingChainAndSipStateIn(processingChain, Arrays
                .asList(ProductSIPState.GENERATION_ERROR, ProductSIPState.NOT_SCHEDULED_INVALID)));
        Assert.assertEquals(4, productService.countByChain(processingChain));
        Assert.assertEquals(4,
                            productService.countByProcessingChainAndSipStateIn(processingChain, Arrays
                                    .asList(ProductSIPState.NOT_SCHEDULED, ProductSIPState.SCHEDULED,
                                            ProductSIPState.SCHEDULED_INTERRUPTED)));

        // Check files
        Assert.assertEquals(0, fileService
                .countByChainAndStateIn(processingChain,
                                        Arrays.asList(AcquisitionFileState.ERROR, AcquisitionFileState.INVALID)));
        Assert.assertEquals(4, fileService.countByChain(processingChain));
        Assert.assertEquals(0, fileService
                .countByChainAndStateIn(processingChain,
                                        Arrays.asList(AcquisitionFileState.IN_PROGRESS, AcquisitionFileState.VALID)));

        // Let's test SessionNotifier
        ArgumentCaptor<ISubscribable> grantedInfo = ArgumentCaptor.forClass(ISubscribable.class);
        Mockito.verify(publisher, Mockito.times(16)).publish(grantedInfo.capture());
        // Capture how many notif of each type have been sent
        Map<String, Integer> callByProperty = new HashMap<>();
        for (ISubscribable event: grantedInfo.getAllValues()) {
            // We ignore all others types of events
            if (event instanceof SessionMonitoringEvent) {
                SessionMonitoringEvent monitoringEvent = (SessionMonitoringEvent) event;
                if (callByProperty.containsKey(monitoringEvent.getProperty())) {
                    callByProperty.put(monitoringEvent.getProperty(), callByProperty.get(monitoringEvent.getProperty()) + 1);
                } else {
                    callByProperty.put(monitoringEvent.getProperty(), 1);
                }
            }
        }
        Assert.assertEquals(4, (int) callByProperty.get(SessionNotifier.PROPERTY_GENERATED));
    }

    //    @Test
    //    public void testScan() throws ModuleException {
    //        runtimeTenantResolver.forceTenant(getDefaultTenant());
    //
    //        AcquisitionProcessingChain processingChain = createProcessingChain(Paths
    //                .get("/home/msordi/git/rs-e2e/data/cdpp/dataobjects/DA_TC_ARC_ISO_DENSITE/results/data2"));
    //
    //        long startTime = System.currentTimeMillis();
    //        processingService.scanAndRegisterFiles(processingChain);
    //
    //        LOGGER.info("Scan action took {} milliseconds", System.currentTimeMillis() - startTime);
    //
    //        processingService.manageRegisteredFiles(processingChain);
    //
    //        LOGGER.info("Manage action took {} milliseconds", System.currentTimeMillis() - startTime);
    //    }
}
