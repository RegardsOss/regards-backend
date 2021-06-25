/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStepProperties;
import fr.cnes.regards.framework.modules.session.commons.domain.SnapshotProcess;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.domain.chain.ScanDirectoryInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.StorageMetadataProvider;
import fr.cnes.regards.modules.acquisition.plugins.Arcad3IsoprobeDensiteProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;
import fr.cnes.regards.modules.acquisition.service.session.SessionNotifier;
import fr.cnes.regards.modules.acquisition.service.session.SessionProductPropertyEnum;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.io.FileUtils;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test {@link AcquisitionProcessingService} for {@link Product} workflow
 *
 * @author Marc Sordi
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_cdpp_product",
        "regards.session.agent.snapshot.process.scheduler.bulk.delay=15000", "regards.amqp.enabled=true" })
@ActiveProfiles({ "testAmqp", "noscheduler", "disableDataProviderTask", "nomonitoring"})
public class CdppProductAcquisitionServiceTest extends DataproviderMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(CdppProductAcquisitionServiceTest.class);

    private static final Path SRC_BASE_PATH = Paths.get("src", "test", "resources", "data", "plugins", "cdpp");

    private static final Path SRC_DATA_PATH = SRC_BASE_PATH.resolve("data");

    private static final Path SRC_BROWSE_PATH = SRC_BASE_PATH.resolve("browse");

    private static final Path TARGET_BASE_PATH = Paths.get("target", "cdpp");

    private static final Path TARGET_DATA_PATH = TARGET_BASE_PATH.resolve("data");

    private static final Path TARGET_BROWSE_PATH = TARGET_BASE_PATH.resolve("browse");


    @Autowired
    private AutowireCapableBeanFactory beanFactory;


    @Before
    public void before() throws ModuleException, IOException {
        // Prepare data repositories
        if (Files.exists(TARGET_DATA_PATH)) {
            FileUtils.forceDelete(TARGET_DATA_PATH.toFile());
        }
        Files.createDirectories(TARGET_DATA_PATH);
        if (Files.exists(TARGET_BROWSE_PATH)) {
            FileUtils.forceDelete(TARGET_BROWSE_PATH.toFile());
        }
        Files.createDirectories(TARGET_BROWSE_PATH);
    }

    @SuppressWarnings("deprecation")
    public AcquisitionProcessingChain createProcessingChain() throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("CDPPChain");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setPeriodicity("0 * * * * *");
        processingChain.setCategories(Sets.newLinkedHashSet());

        // RAW DATA file infos
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("RAWDATA");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);
        fileInfo.setScanDirInfo(Sets.newHashSet(new ScanDirectoryInfo(TARGET_DATA_PATH, null)));

        PluginConfiguration scanPlugin = PluginConfiguration.build(GlobDiskScanning.class, null, null);
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
        fileInfo.setScanDirInfo(Sets.newHashSet(new ScanDirectoryInfo(TARGET_BROWSE_PATH, null)));

        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(GlobDiskScanning.FIELD_GLOB, "*B.png"));

        scanPlugin = PluginConfiguration.build(GlobDiskScanning.class, null, parameters);
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
        fileInfo.setScanDirInfo(Sets.newHashSet(new ScanDirectoryInfo(TARGET_BROWSE_PATH, null)));

        parameters = IPluginParam.set(IPluginParam.build(GlobDiskScanning.FIELD_GLOB, "*C.png"));

        scanPlugin = PluginConfiguration.build(GlobDiskScanning.class, null, parameters);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin QUICKLOOK_MD" + Math.random());
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginConfiguration
                .build(DefaultFileValidation.class, null, new HashSet<IPluginParam>());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin" + Math.random());
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginConfiguration
                .build(Arcad3IsoprobeDensiteProductPlugin.class, null, new HashSet<IPluginParam>());
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin" + Math.random());
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration
                .build(DefaultSIPGeneration.class, null, new HashSet<IPluginParam>());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin" + Math.random());
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("AWS", "/path/to/file", new HashSet<>()));
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file", new HashSet<>()));
        processingChain.setStorages(storages);

        // Save processing chain
        return processingService.createChain(processingChain);
    }

    @Test
    public void acquisitionWorkflowTest() throws ModuleException, IOException, InterruptedException {
        // Prepare data
        FileUtils.copyDirectory(SRC_DATA_PATH.toFile(), TARGET_DATA_PATH.toFile(), false);
        FileUtils.copyDirectory(SRC_BROWSE_PATH.toFile(), TARGET_BROWSE_PATH.toFile(), false);

        AcquisitionProcessingChain processingChain = createProcessingChain();
        String session1 = "session1";
        doAcquire(processingChain, session1, true);

        // Reset last modification date
        Set<AcquisitionFileInfo> fileInfoSet = processingChain.getFileInfos();
        for (AcquisitionFileInfo fileInfo : fileInfoSet) {
            Set<ScanDirectoryInfo> dirInfoSet = fileInfo.getScanDirInfo();
            for (ScanDirectoryInfo dirInfo : dirInfoSet) {
                dirInfo.setLastModificationDate(null);
            }
        }

        String session2 = "session2";
        doAcquire(processingChain, session2, true);

        // wait the registration of all StepPropertyUpdateRequests
        // Session1 : 1requestRunning + 3filesAcquired + 1 productComplete + 1 productGenerated + (-) 1prodductComplete + (-) 1requestRunning
        //       After session2 pass, product change of session from 1 to 2. So +1 (-) productGenerated
        // ---> 9 steps
        waitStepRegistration("session1", 9);
        // Session2 : 1requestRunning + 3filesAcquired + 1 productComplete + 1 productGenerated + (-) 1prodductComplete + (-) 1requestRunning
        // ---> 8 steps
        waitStepRegistration("session2", 8);
        // launch the generation of sessionStep from StepPropertyUpdateRequests
        this.agentService
                .generateSessionStep(new SnapshotProcess(processingChain.getLabel(), null, null), OffsetDateTime.now());
        // check result
        assertSessionStep(processingChain.getLabel(), session1, 3L, 0L, null, 0L);
        assertSessionStep(processingChain.getLabel(), session2, 3L, 0L, null, 1L);
    }

    @Test
    public void twoPhaseAcquisitionTest() throws ModuleException, IOException, InterruptedException {
        // Prepare data (only data)
        FileUtils.copyDirectory(SRC_DATA_PATH.toFile(), TARGET_DATA_PATH.toFile(), false);

        AcquisitionProcessingChain processingChain = createProcessingChain();
        String session1 = "session1";
        doAcquire(processingChain, session1, false);

        // Prepare data (browse data)
        FileUtils.copyDirectory(SRC_BROWSE_PATH.toFile(), TARGET_BROWSE_PATH.toFile(), false);

        // Reload chain from database
        processingChain = acquisitionProcessingChainRepository.findCompleteById(processingChain.getId());
        String session2 = "session2";
        doAcquire(processingChain, session2, false);

        // wait the registration of all StepPropertyUpdateRequests
        waitStepRegistration("session1", 4);
        waitStepRegistration("session2", 8);
        // launch the generation of sessionStep from StepPropertyUpdateRequests
        this.agentService
                .generateSessionStep(new SnapshotProcess(processingChain.getLabel(), null, null), OffsetDateTime.now());
        // check result
        assertSessionStep(processingChain.getLabel(), session1, 0L, null, 0L, null);
        assertSessionStep(processingChain.getLabel(), session2, 3L, 0L, null, 1L);
    }

    private void waitStepRegistration(String session, int nbSteps) throws InterruptedException {
        long now = System.currentTimeMillis(), end = now + 60000L;
        int count;
        logger.info("Waiting for StepPropertyUpdateRequests creation for session {}", session);
        do {
            count = this.stepRepo.findBySession(session).size();
            now = System.currentTimeMillis();
            if (count != nbSteps) {
                Thread.sleep(30000L);
            }
        } while (count != nbSteps && now < end);

        if(count!= nbSteps) {
            Assert.fail("Unexpected number of step events created. Check the workflow through events collected in "
                                + "t_step_property_update_request");
        }
    }

    private void assertSessionStep(String sessionOwner, String session, Long acquiredFiles, Long completed,
            Long incompleted, Long generated) throws InterruptedException {
        // assert all properties required are present and have the correct count
        SessionStepProperties sessionSteps = this.sessionStepRepo
                .findBySourceAndSessionAndStepId(sessionOwner, session, SessionNotifier.GLOBAL_SESSION_STEP).get()
                .getProperties();
        String generatedValue = sessionSteps.get(SessionProductPropertyEnum.GENERATED_PRODUCTS.getName());
        String completedValue = sessionSteps.get(SessionProductPropertyEnum.PROPERTY_COMPLETED.getName());
        String incompleteValue = sessionSteps.get(SessionProductPropertyEnum.PROPERTY_INCOMPLETE.getName());
        String acqFilesValue = sessionSteps.get(SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED.getName());

        Assert.assertTrue(String.format("Wrong property count expected %s but was %s", generated, generatedValue),
                          (generated == null && generatedValue == null) || (Objects.requireNonNull(generated).toString()
                                  .equals(generatedValue)));
        Assert.assertTrue(String.format("Wrong property count expected %s but was %s", completed, completedValue),
                          (completed == null && completedValue == null) || (Objects.requireNonNull(completed).toString()
                                  .equals(completedValue)));
        Assert.assertTrue(String.format("Wrong property count expected %s but was %s", incompleted, incompleteValue),
                          (incompleted == null && incompleteValue == null) || (Objects.requireNonNull(incompleted)
                                  .toString().equals(incompleteValue)));
        Assert.assertTrue(String.format("Wrong property count expected %s but was %s", acquiredFiles, acqFilesValue),
                          (acquiredFiles == null && acqFilesValue == null) || (Objects.requireNonNull(acquiredFiles)
                                  .toString().equals(acqFilesValue)));

    }

    public void doAcquire(AcquisitionProcessingChain processingChain, String session, boolean assertResult)
            throws ModuleException, InterruptedException {

        processingService.scanAndRegisterFiles(processingChain, session);

        // Check registered files
        if (assertResult) {
            for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {
                Page<AcquisitionFile> inProgressFiles = acqFileRepository
                        .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS, fileInfo,
                                                            PageRequest.of(0, 1));
                Assert.assertTrue(inProgressFiles.getTotalElements() == 1);
            }
        }

        processingService.manageRegisteredFiles(processingChain, session);
        Thread.sleep(2000L);
        productService.manageUpdatedProducts(processingChain);
        Thread.sleep(2000L);
        // Check registered files
        if (assertResult) {
            for (AcquisitionFileInfo fileInfo : processingChain.getFileInfos()) {
                Page<AcquisitionFile> inProgressFiles = acqFileRepository
                        .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.IN_PROGRESS, fileInfo,
                                                            PageRequest.of(0, 1));
                Assert.assertTrue(inProgressFiles.getTotalElements() == 0);

                Page<AcquisitionFile> validFiles = acqFileRepository
                        .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.VALID, fileInfo, PageRequest.of(0, 1));
                Assert.assertTrue(validFiles.getTotalElements() == 0);

                Page<AcquisitionFile> acquiredFiles = acqFileRepository
                        .findByStateAndFileInfoOrderByIdAsc(AcquisitionFileState.ACQUIRED, fileInfo,
                                                            PageRequest.of(0, 1));
                Assert.assertTrue(acquiredFiles.getTotalElements() == 1);
            }
        }
    }
}
