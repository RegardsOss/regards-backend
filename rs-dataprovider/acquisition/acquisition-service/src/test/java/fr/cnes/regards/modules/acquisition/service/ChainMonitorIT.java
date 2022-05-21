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
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.acquisition.domain.chain.*;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChainType;
import fr.cnes.regards.modules.acquisition.domain.payload.UpdateAcquisitionProcessingChains;
import fr.cnes.regards.modules.acquisition.service.plugins.*;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.persistence.EntityManager;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=acquisition_monitor", "regards.amqp.enabled=true" },
    locations = { "classpath:application-monitor.properties" })
@ActiveProfiles({ "testAmqp", "nohandler", "disableDataProviderTask", "noscheduler", "nojobs" })
public class ChainMonitorIT extends DataproviderMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChainMonitorIT.class);

    private Path rootPath = Paths.get("src", "test", "resources", "startstop");

    private Path fakePath = rootPath.resolve("fake").toAbsolutePath();

    private Path imagePath = rootPath.resolve("images").toAbsolutePath();

    private Path blockerPath = rootPath.resolve("blocker").toAbsolutePath();

    @Autowired
    private EntityManager entityManager;

    @Override
    public void doAfter() throws InterruptedException {
        LOGGER.info("|-----------------------------> TEST ENDING .... <-----------------------------------------|");
        TimeUnit.SECONDS.sleep(5);
        LOGGER.info("|-----------------------------> TEST DONE        <-----------------------------------------|");
    }

    @Override
    public void doInit() throws InterruptedException {
        processingService.getFullChains().forEach(chain -> {
            try {
                processingService.patchStateAndMode(chain.getId(),
                                                    UpdateAcquisitionProcessingChains.build(false,
                                                                                            AcquisitionProcessingChainMode.AUTO,
                                                                                            UpdateAcquisitionProcessingChainType.ALL));
                processingService.stopAndCleanChain(chain.getId());
                processingService.deleteChain(chain.getId());
            } catch (ModuleException e) {
                Assertions.fail(e.getMessage());
            }
        });
        TimeUnit.SECONDS.sleep(2);
    }

    @Ignore(
        "Test to count sql queries during chain monitor requests - not required to run during builds (enable hibernate and sql debug logs)")
    @Test
    public void chainMonitorTest() throws ModuleException {

        Session session = (Session) entityManager.getDelegate();
        Statistics statistics = session.getSessionFactory().getStatistics();

        LOGGER.info("Queries at startup {}", statistics.getQueryExecutionCount());

        AcquisitionProcessingChain chain = createProcessingChain("test1");

        LOGGER.info("Queries after chain creation {}", statistics.getQueryExecutionCount());

        processingService.buildAcquisitionProcessingChainSummaries(null, null, null, PageRequest.of(0, 100));

        LOGGER.info("Queries after monitor-1 {}", statistics.getQueryExecutionCount());

        processingService.buildAcquisitionProcessingChainSummaries(null, null, null, PageRequest.of(0, 100));

        LOGGER.info("Queries after monitor-2 {}", statistics.getQueryExecutionCount());

        Page<AcquisitionProcessingChain> a = processingService.getFullChains(Pageable.unpaged());

        LOGGER.info("Queries after fullChains {}", statistics.getQueryExecutionCount());

        Assertions.assertTrue(true);
    }

    private AcquisitionProcessingChain createProcessingChain(String label) throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel(label);
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setPeriodicity("0 * * * * *");
        processingChain.setCategories(Sets.newLinkedHashSet());

        // Acquisition file info
        PluginConfiguration scanPlugin = PluginConfiguration.build(GlobDiskScanning.class, "Scan plugin", null);
        scanPlugin.setIsActive(true);

        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);
        fileInfo.setScanDirInfo(Sets.newHashSet(new ScanDirectoryInfo(fakePath, null)));
        fileInfo.setScanPlugin(scanPlugin);
        processingChain.addFileInfo(fileInfo);

        PluginConfiguration scanPlugin2 = PluginConfiguration.build(GlobDiskScanning.class, "Scan plugin 2", null);
        scanPlugin.setIsActive(true);
        AcquisitionFileInfo fileInfo2 = new AcquisitionFileInfo();
        fileInfo2.setMandatory(Boolean.TRUE);
        fileInfo2.setComment("A comment 2");
        fileInfo2.setMimeType(MediaType.IMAGE_PNG);
        fileInfo2.setDataType(DataType.THUMBNAIL);
        fileInfo2.setScanDirInfo(Sets.newHashSet(new ScanDirectoryInfo(imagePath, null)));
        fileInfo2.setScanPlugin(scanPlugin2);
        processingChain.addFileInfo(fileInfo2);

        PluginConfiguration scanPlugin3 = PluginConfiguration.build(GlobDiskScanning.class, "Scan plugin 3", null);
        scanPlugin.setIsActive(true);
        AcquisitionFileInfo fileInfo3 = new AcquisitionFileInfo();
        fileInfo3.setMandatory(Boolean.TRUE);
        fileInfo3.setComment("A comment 3");
        fileInfo3.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo3.setDataType(DataType.RAWDATA);
        fileInfo3.setScanDirInfo(Sets.newHashSet(new ScanDirectoryInfo(blockerPath, null)));
        fileInfo3.setScanPlugin(scanPlugin3);
        processingChain.addFileInfo(fileInfo3);

        // Validation
        PluginConfiguration validationPlugin = PluginConfiguration.build(DefaultFileValidation.class,
                                                                         "validPlugin",
                                                                         new HashSet<>());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        Set<IPluginParam> parametersProduct = IPluginParam.set(IPluginParam.build(DefaultProductPlugin.FIELD_REMOVE_EXT,
                                                                                  true));
        PluginConfiguration productPlugin = PluginConfiguration.build(DefaultProductPlugin.class,
                                                                      "productPlugin",
                                                                      parametersProduct);
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration.build(DefaultSIPGeneration.class,
                                                                     "sipGenPlugin",
                                                                     new HashSet<>());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // Post-processing blocking plugin
        PluginConfiguration cleanAndAcknowledgePlugin = PluginConfiguration.build(CleanAndAcknowledgePlugin.class,
                                                                                  "cleanAndAcknowledgePlugin",
                                                                                  null);
        cleanAndAcknowledgePlugin.setIsActive(true);
        cleanAndAcknowledgePlugin.setLabel("Clean and Acknowledge plugin");
        processingChain.setPostProcessSipPluginConf(cleanAndAcknowledgePlugin);

        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("AWS", "/path/to/file", new HashSet<>()));
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file", new HashSet<>()));
        processingChain.setStorages(storages);

        // Save processing chain
        return processingService.createChain(processingChain);
    }

    private void assertExactCount(long expected, LongSupplier objectCount) throws InterruptedException {
        assertExactCount(100, 1, expected, objectCount);
    }

    private void assertExactCount(int maxLoops, int delay, long expected, LongSupplier objectCount)
        throws InterruptedException {
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

    private void assertMinCount(int maxLoops, int delay, long expected, LongSupplier objectCount)
        throws InterruptedException {
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
