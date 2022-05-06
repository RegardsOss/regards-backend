/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.notification.NotificationLevel;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.*;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChainType;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChains;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.acquisition.service.plugin.LongLastingSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.*;
import fr.cnes.regards.modules.acquisition.service.session.SessionNotifier;
import fr.cnes.regards.modules.acquisition.service.session.SessionProductPropertyEnum;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;


/**
 * Launch a chain with very long plugin actions and stop it.
 *
 *
 * <b>Must not be transactional in order to run jobs</b>
 *
 * @author Marc SORDI
 */
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=acq_start_stop", "regards.amqp.enabled=true" }
// ,locations = { "classpath:application-local.properties" }
)
@ActiveProfiles({"testAmqp", "nohandler"})
public class StartStopChainIT extends DataproviderMultitenantServiceIT {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(StartStopChainIT.class);

    @Autowired
    private SessionNotificationHandler sessionNotificationHandler;

    @MockBean
    private INotificationClient notificationClient;

    private Path rootPath = Paths.get("src", "test", "resources", "startstop");
    private Path fakePath = rootPath.resolve("fake").toAbsolutePath();
    private Path imagePath = rootPath.resolve("images").toAbsolutePath();
    private Path blockerPath = rootPath.resolve("blocker").toAbsolutePath();

    @Override
    public void doAfter() throws InterruptedException {
        int loops = 0;
        do {
            TimeUnit.MILLISECONDS.sleep(100);
            loops++;
        } while (jobInfoRepo.countByStatusStatusIn(JobStatus.RUNNING) > 0 && loops < 600);
        this.doInit();
        LOGGER.info("|-----------------------------> TEST DONE REMAINING RUNNING JOBS = {} <-----------------------------------------|",
                    jobInfoRepo.countByStatusStatusIn(JobStatus.RUNNING));
        LOGGER.info("|-----------------------------> TEST ENDING .... <-----------------------------------------|");
        TimeUnit.SECONDS.sleep(5);
        LOGGER.info("|-----------------------------> TEST DONE .... <-----------------------------------------|");
    }

    @Override
    public void doInit() throws InterruptedException {
        processingService.getFullChains().forEach(chain -> {
            try {
                processingService.patchStateAndMode(
                        chain.getId(),
                        UpdateAcquisitionProcessingChains.build(false, AcquisitionProcessingChainMode.AUTO, UpdateAcquisitionProcessingChainType.ALL));
                processingService.deleteChain(chain.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        });
        TimeUnit.SECONDS.sleep(2);
        sessionNotificationHandler.clear();
    }

    /**
     * Create chain with slow SIP generation plugin
     */
    private AcquisitionProcessingChain createProcessingChain(String label, Class<?> sipGenPluginClass, Path searchDir, Path searchDirThumbnail) throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel(label);
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setPeriodicity("0 * * * * *");
        processingChain.setCategories(Sets.newLinkedHashSet());

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);
        fileInfo.setScanDirInfo(Sets.newHashSet(new ScanDirectoryInfo(searchDir, null)));

        PluginConfiguration scanPlugin = PluginConfiguration.build(GlobDiskScanning.class, null, null);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        if (searchDirThumbnail != null) {
            AcquisitionFileInfo fileInfo2 = new AcquisitionFileInfo();
            fileInfo2.setMandatory(Boolean.TRUE);
            fileInfo2.setComment("A comment 2");
            fileInfo2.setMimeType(MediaType.IMAGE_PNG);
            fileInfo2.setDataType(DataType.THUMBNAIL);
            fileInfo2.setScanDirInfo(Sets.newHashSet(new ScanDirectoryInfo(searchDirThumbnail, null)));

            PluginConfiguration scanPlugin2 = PluginConfiguration.build(GlobDiskScanning.class, "ScanPlugin2", null);
            scanPlugin2.setIsActive(true);
            scanPlugin2.setLabel("Scan plugin");
            fileInfo2.setScanPlugin(scanPlugin2);

            processingChain.addFileInfo(fileInfo2);
        }

        // Validation
        PluginConfiguration validationPlugin = PluginConfiguration.build(DefaultFileValidation.class, "validPlugin", new HashSet<>());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        Set<IPluginParam> parametersProduct = IPluginParam.set(IPluginParam.build(DefaultProductPlugin.FIELD_REMOVE_EXT, true));

        PluginConfiguration productPlugin = PluginConfiguration.build(DefaultProductPlugin.class, "productPlugin", parametersProduct);
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration.build(sipGenPluginClass, "sipGenPlugin", new HashSet<>());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post-processing
        // Not required

        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("AWS", "/path/to/file", new HashSet<>()));
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file", new HashSet<>()));
        processingChain.setStorages(storages);

        // Save processing chain
        return processingService.createChain(processingChain);
    }

    /**
     * Update chain with default generation plugin to speed up generation
     */
    private void updateProcessingChain(Long processingChainId) throws ModuleException {
        AcquisitionProcessingChain processingChain = processingService.getChain(processingChainId);
        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration.build(DefaultSIPGeneration.class, "sipGenPlugin", new HashSet<>());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("Default SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);
        processingService.updateChain(processingChain);
    }

    @Test
    public void startChainWithIncompletes() throws ModuleException, InterruptedException {

        LOGGER.info("|-----------------------------> START TEST 2 <-----------------------------------------|");

        String source = "Start Stop 2";
        String session = UUID.randomUUID().toString();

        // Create a chain

        AcquisitionProcessingChain processingChain = createProcessingChain(source, DefaultSIPGeneration.class, fakePath, imagePath);
        // Start chain
        processingService.startManualChain(processingChain.getId(),  Optional.of(session), false);

        // Check all products are registered
        assertExactCount(100, () -> productRepository.count());
        LOGGER.info("-----> All {} products are registered !", 100);

        // Check that no running jobs remain
        assertExactCount(0, () -> jobInfoService.retrieveJobsCount(ProductAcquisitionJob.class.getName(), JobStatus.RUNNING));
        LOGGER.info("-----> Number of running ProductAcquisitionJob jobs {} ", 0);

        // At the end, 95 product must be valid / 5 incomplete
        assertExactCount(95, () -> productRepository.countByProcessingChainAndSipStateIn(processingChain, Collections.singletonList(ProductSIPState.SUBMITTED)));

        Assert.assertEquals(5, productRepository
                .countByProcessingChainAndStateIn(processingChain, Collections.singletonList(ProductState.ACQUIRING)));

        // Check notification for acquired files
        // --- 195 files scanned / 100 data / 95 images
        Assert.assertEquals(195,
                            sessionNotificationHandler.getPropertyCount(source, session,
                                    SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED.getName(), StepPropertyEventTypeEnum.INC));
        // Check notification for completed files
        // -- 95 products set to COMPLETED status
        Assert.assertEquals(95,
                            sessionNotificationHandler.getPropertyCount(source, session,
                                    SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_COMPLETED.getName(), StepPropertyEventTypeEnum.INC));
        // -- 95 products pass from COMPLETED to GENERATED
        Assert.assertEquals(95,
                            sessionNotificationHandler.getPropertyCount(source, session,
                                    SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_COMPLETED.getName(), StepPropertyEventTypeEnum.DEC));
        // Check notification for incomplet files
        // --- After All 5 products should be incomplets
        long inc = sessionNotificationHandler.getPropertyCount(source, session,
                SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_INCOMPLETE.getName(), StepPropertyEventTypeEnum.INC);
        long dec = sessionNotificationHandler.getPropertyCount(source, session,
                SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_INCOMPLETE.getName(), StepPropertyEventTypeEnum.DEC);
        Assert.assertEquals(5, inc - dec);

        // Check notification for generated products files
        Assert.assertEquals(95,
                            sessionNotificationHandler.getPropertyCount(source, session,
                                    SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_GENERATED_PRODUCTS.getName(), StepPropertyEventTypeEnum.INC));
        Assert.assertEquals(0,
                            sessionNotificationHandler.getPropertyCount(source, session,
                                    SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_GENERATED_PRODUCTS.getName(), StepPropertyEventTypeEnum.DEC));
        LOGGER.info("|-----------------------------> END TEST 2 <-----------------------------------------|");
    }

    @Test
    public void startAndStop() throws ModuleException, InterruptedException {

        LOGGER.info("|-----------------------------> START TEST 1 <-----------------------------------------|");

        String source = "Start Stop 1";
        String session = UUID.randomUUID().toString();

        // Create a chain
        AcquisitionProcessingChain processingChain = createProcessingChain(source, LongLastingSIPGeneration.class, fakePath, null);

        sessionNotificationHandler.clear();
        // Start chain
        processingService.startManualChain(processingChain.getId(), Optional.of(session), false);

        // Check all products are registered
        assertExactCount(100, () -> productRepository.count());

        // Check at least one SIP generation job is working
        assertMinCount(1, () -> jobInfoService.retrieveJobsCount(SIPGenerationJob.class.getName(), JobStatus.RUNNING));

        // Stop it verifying all threads are properly stopped
        processingService.stopAndCleanChain(processingChain.getId());

        // Check that no running jobs remain
        assertExactCount(0, () -> jobInfoService.retrieveJobsCount(ProductAcquisitionJob.class.getName(), JobStatus.RUNNING));
        assertExactCount(0, () -> jobInfoService.retrieveJobsCount(SIPGenerationJob.class.getName(), JobStatus.RUNNING));

        // Waiting for stop chain thread to really stop!
        TimeUnit.SECONDS.sleep(4);

        // Restart chain verifying all re-run properly
        updateProcessingChain(processingChain.getId());
        processingService.startManualChain(processingChain.getId(), Optional.of(session), false);

        // At the end, all product must be valid
        assertExactCount(100, () -> productRepository.countByProcessingChainAndSipStateIn(processingChain, Collections.singletonList(ProductSIPState.SUBMITTED)));

        // Check notification for acquired files
        Assert.assertEquals(100,
                            sessionNotificationHandler.getPropertyCount(source, session,
                                    SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_FILES_ACQUIRED.getName(), StepPropertyEventTypeEnum.INC));
        // Check notification for generated products files
        Assert.assertEquals(100,
                            sessionNotificationHandler.getPropertyCount(source, session,
                                    SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_GENERATED_PRODUCTS.getName(), StepPropertyEventTypeEnum.INC));
        Assert.assertEquals(0,
                            sessionNotificationHandler.getPropertyCount(source, session,
                                    SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_GENERATED_PRODUCTS.getName(), StepPropertyEventTypeEnum.DEC));
        // Check notification for completed files
        Assert.assertEquals(100,
                            sessionNotificationHandler.getPropertyCount(source, session,
                                    SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_COMPLETED.getName(), StepPropertyEventTypeEnum.INC));
        Assert.assertEquals(100,
                            sessionNotificationHandler.getPropertyCount(source, session,
                                    SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_COMPLETED.getName(), StepPropertyEventTypeEnum.DEC));
        // Check notification for incomplet files
        Assert.assertEquals(0,
                            sessionNotificationHandler.getPropertyCount(source, session,
                                    SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_INCOMPLETE.getName(), StepPropertyEventTypeEnum.INC));
        Assert.assertEquals(0,
                            sessionNotificationHandler.getPropertyCount(source, session,
                                    SessionNotifier.GLOBAL_SESSION_STEP, SessionProductPropertyEnum.PROPERTY_INCOMPLETE.getName(), StepPropertyEventTypeEnum.DEC));
        LOGGER.info("|-----------------------------> END TEST 1 <-----------------------------------------|");
    }

    @Test
    public void startChainWithExecutionBlocker() throws ModuleException, IOException {

        String label = "Start with blockers";
        AcquisitionProcessingChain processingChain = createProcessingChain(label, DefaultSIPGeneration.class, blockerPath, null);

        // Add blocking plugin
        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(CleanAndAcknowledgePlugin.CREATE_ACK_PARAM, true));
        PluginConfiguration cleanAndAcknowledgePlugin = PluginConfiguration.build(CleanAndAcknowledgePlugin.class, "cleanAndAcknowledgePlugin", parameters);
        cleanAndAcknowledgePlugin.setIsActive(true);
        cleanAndAcknowledgePlugin.setLabel("Clean and Acknowledge plugin");
        processingChain.setPostProcessSipPluginConf(cleanAndAcknowledgePlugin);
        processingService.updateChain(processingChain);

        String session = UUID.randomUUID().toString();
        Path ackPath = blockerPath.resolve("ack_regards");
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        // Reset
        setWritePermission(blockerPath, true);
        if (Files.exists(ackPath)) {
            setWritePermission(ackPath, true);
            Files.delete(ackPath);
        }

        // Can't start a chain if the ack directory doesn't exist and the scan directory is not writable
        setWritePermission(blockerPath, false);
        Assertions.assertThrows(EntityInvalidException.class, () -> processingService.startManualChain(processingChain.getId(), Optional.of(session), false));

        // Reset
        setWritePermission(blockerPath, true);

        // Can't start a chain if the ack directory doesn't exist and the scan directory is not writable
        Files.createDirectory(ackPath);
        setWritePermission(ackPath, false);
        Assertions.assertThrows(EntityInvalidException.class, () -> processingService.startManualChain(processingChain.getId(), Optional.of(session), false));

        Mockito.verify(notificationClient, Mockito.times(2))
                .notify(messageCaptor.capture(), anyString(), any(NotificationLevel.class), any(MimeType.class), any(DefaultRole.class));
        messageCaptor.getAllValues().forEach(message -> assertTrue(message.contains(label)));

        // Cleanup
        Files.delete(ackPath);
    }

    private void assertExactCount(long expected, LongSupplier objectCount) throws InterruptedException {
        assertExactCount(100, 1, expected, objectCount);
    }

    private void assertExactCount(int maxLoops, int delay, long expected, LongSupplier objectCount) throws InterruptedException {
        int loops = maxLoops;
        while (loops > 0 && objectCount.getAsLong() != expected) {
            loops--;
            TimeUnit.SECONDS.sleep(delay);
        }
        assertTrue(loops != 0 || objectCount.getAsLong() == expected);
    }

    private void assertMinCount(long expected, LongSupplier objectCount) throws InterruptedException {
        assertMinCount(100, 1, expected, objectCount);
    }

    private void assertMinCount(int maxLoops, int delay, long expected, LongSupplier objectCount) throws InterruptedException {
        int loops = maxLoops;
        while (loops > 0 && objectCount.getAsLong() < expected) {
            loops--;
            TimeUnit.SECONDS.sleep(delay);
        }
        assertTrue(loops != 0 || objectCount.getAsLong() >= expected);
    }

    private void setWritePermission(Path path, boolean write) {
        if (!path.toFile().setWritable(write)) {
            Assertions.fail("Unable to set test folder permissions");
        }
    }

}
