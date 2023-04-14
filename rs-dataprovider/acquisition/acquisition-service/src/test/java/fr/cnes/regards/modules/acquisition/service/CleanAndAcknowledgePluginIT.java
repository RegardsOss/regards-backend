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

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.notification.client.INotificationClient;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.*;
import fr.cnes.regards.modules.acquisition.service.plugins.*;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;

import static org.mockito.Mockito.mock;

/**
 * @author Thomas GUILLOU
 **/
@ActiveProfiles({ "test", "nojobs", "noscheduler" })
@RunWith(SpringRunner.class)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=clean_acknowledge_test" })
public class CleanAndAcknowledgePluginIT extends AbstractRegardsTransactionalIT {

    private static final String PRODUCT_NAME_1 = "provider_id_1";

    private static final String PRODUCT_NAME_2 = "provider_id_2";

    private static final String PRODUCT_PATH_1 = "file1.extension";

    private static final String PRODUCT_PATH_2 = "file2.extension";

    private static final String FOLDER_OF_PRODUCT_2 = "folder";

    // Plugin configuration
    private static final String ACK_FOLDER_NAME = "ack_folder";

    private static final String EXTENSION_ACK = ".ack";

    private Path scanPath;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    protected IProductService productService;

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IAcquisitionFileService fileService;

    @Before
    public void before() throws URISyntaxException {
        productRepository.deleteAll();
        scanPath = Paths.get(CleanAndAcknowledgePluginIT.class.getClassLoader()
                                                              .getResource("clean-acknowledge-datas")
                                                              .toURI());
    }

    private AcquisitionProcessingChain createProcessingChain(AcquisitionFileInfo fileInfo) throws ModuleException {
        String label = "ma_processing_chain";
        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel(label);
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setPeriodicity("0 * * * * *");
        processingChain.setCategories(org.assertj.core.util.Sets.newLinkedHashSet());

        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file", new HashSet<>()));
        processingChain.setStorages(storages);

        PluginConfiguration scanPlugin = PluginConfiguration.build(GlobDiskScanning.class, null, null);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin - " + label);
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginConfiguration.build(DefaultFileValidation.class,
                                                                         null,
                                                                         new HashSet<IPluginParam>());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin " + label);
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginConfiguration.build(DefaultProductPlugin.class,
                                                                      null,
                                                                      new HashSet<IPluginParam>());
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin" + label);
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration.build(DefaultSIPGeneration.class,
                                                                     null,
                                                                     new HashSet<IPluginParam>());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin" + label);
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        // Save processing chain
        return processingService.createChain(processingChain);
    }

    private void createProductAndAcquisitionsInfos(Path scanPath) throws ModuleException {
        ScanDirectoryInfo scanDirectoryInfo = new ScanDirectoryInfo();
        scanDirectoryInfo.setScannedDirectory(scanPath);

        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setScanDirInfo(Set.of(scanDirectoryInfo));
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);

        AcquisitionProcessingChain processingChain = createProcessingChain(fileInfo);

        // Product 1
        Product product1 = new Product();
        product1.setProcessingChain(processingChain);
        product1.setIpId("IpId");
        product1.setLastUpdate(OffsetDateTime.now());
        product1.setProcessingChain(processingChain);
        product1.setProductName(PRODUCT_NAME_1);
        product1.setSession("session");
        product1.setSipState(SIPState.INGESTED);
        product1.setState(ProductState.COMPLETED);
        productService.save(product1);

        AcquisitionFile acquisitionFile1 = new AcquisitionFile();
        acquisitionFile1.setAcqDate(OffsetDateTime.now());
        acquisitionFile1.setError("");
        acquisitionFile1.setFileInfo(fileInfo);
        acquisitionFile1.setFilePath(scanPath.resolve(PRODUCT_PATH_1));
        acquisitionFile1.setProduct(product1);
        acquisitionFile1.setState(AcquisitionFileState.ACQUIRED);
        fileService.save(acquisitionFile1);

        // Product 2

        Product product2 = new Product();
        product2.setProcessingChain(processingChain);
        product2.setIpId("IpId2");
        product2.setLastUpdate(OffsetDateTime.now());
        product2.setProcessingChain(processingChain);
        product2.setProductName(PRODUCT_NAME_2);
        product2.setSession("session");
        product2.setSipState(SIPState.INGESTED);
        product2.setState(ProductState.COMPLETED);
        productService.save(product2);

        AcquisitionFile acquisitionFile2 = new AcquisitionFile();
        acquisitionFile2.setAcqDate(OffsetDateTime.now());
        acquisitionFile2.setError("");
        acquisitionFile2.setFileInfo(fileInfo);
        acquisitionFile2.setFilePath(scanPath.resolve(FOLDER_OF_PRODUCT_2).resolve(PRODUCT_PATH_2));
        acquisitionFile2.setProduct(product2);
        acquisitionFile2.setState(AcquisitionFileState.ACQUIRED);
        fileService.save(acquisitionFile2);
    }

    private CleanAndAcknowledgePlugin instantiatePlugin(boolean storeAckIntoRootDirectory) {
        CleanAndAcknowledgePlugin cleanAndAcknowledgePlugin = new CleanAndAcknowledgePlugin();
        cleanAndAcknowledgePlugin.createAck = true;
        cleanAndAcknowledgePlugin.extensionAck = EXTENSION_ACK;
        cleanAndAcknowledgePlugin.folderAck = ACK_FOLDER_NAME;
        cleanAndAcknowledgePlugin.storeAckIntoRootDirectory = storeAckIntoRootDirectory;
        // useless parameters
        cleanAndAcknowledgePlugin.cleanFile = false;
        cleanAndAcknowledgePlugin.recursiveCheck = false;
        ReflectionTestUtils.setField(cleanAndAcknowledgePlugin, "notificationClient", mock(INotificationClient.class));
        return cleanAndAcknowledgePlugin;
    }

    @Test
    public void testWithStoreAckIntoRootDirectory() throws ModuleException, IOException {
        // GIVEN
        cleanAckDirectories();
        createProductAndAcquisitionsInfos(scanPath);

        CleanAndAcknowledgePlugin plugin = instantiatePlugin(true);

        // WHEN
        // keep same method than PostAcquisitionJob.run() to get a product
        Optional<Product> oProduct1 = productService.searchProduct(PRODUCT_NAME_1);
        Assertions.assertTrue(oProduct1.isPresent());
        plugin.postProcess(oProduct1.get());

        // THEN
        assertFileExistsResolvingPath(ACK_FOLDER_NAME, PRODUCT_PATH_1 + EXTENSION_ACK);

        // WHEN
        // keep same method than PostAcquisitionJob.run() to get a product
        Optional<Product> oProduct2 = productService.searchProduct(PRODUCT_NAME_2);
        Assertions.assertTrue(oProduct2.isPresent());
        plugin.postProcess(oProduct2.get());

        assertFileExistsResolvingPath(ACK_FOLDER_NAME, PRODUCT_PATH_2 + EXTENSION_ACK);
    }

    @Test
    public void testWithAckInFolder() throws ModuleException, IOException {
        // GIVEN
        cleanAckDirectories();
        createProductAndAcquisitionsInfos(scanPath);

        CleanAndAcknowledgePlugin plugin = instantiatePlugin(false);

        // WHEN
        // keep same method than PostAcquisitionJob.run() to get a product
        Optional<Product> oProduct1 = productService.searchProduct(PRODUCT_NAME_1);
        Assertions.assertTrue(oProduct1.isPresent());
        plugin.postProcess(oProduct1.get());

        // THEN
        assertFileExistsResolvingPath(ACK_FOLDER_NAME, PRODUCT_PATH_1 + EXTENSION_ACK);

        // WHEN
        // keep same method than PostAcquisitionJob.run() to get a product
        Optional<Product> oProduct2 = productService.searchProduct(PRODUCT_NAME_2);
        Assertions.assertTrue(oProduct2.isPresent());
        plugin.postProcess(oProduct2.get());

        assertFileExistsResolvingPath(FOLDER_OF_PRODUCT_2, ACK_FOLDER_NAME, PRODUCT_PATH_2 + EXTENSION_ACK);
    }

    private void cleanAckDirectories() throws IOException {
        Path ackFolderPath1 = scanPath.resolve(ACK_FOLDER_NAME);
        Path ackFolderPath2 = scanPath.resolve(FOLDER_OF_PRODUCT_2).resolve(ACK_FOLDER_NAME);
        FileUtils.deleteDirectory(ackFolderPath1.toFile());
        Assertions.assertFalse(Files.exists(ackFolderPath1));
        FileUtils.deleteDirectory(ackFolderPath2.toFile());
        Assertions.assertFalse(Files.exists(ackFolderPath2));
    }

    private void assertFileExistsResolvingPath(String... paths) {
        Path completePath = scanPath;
        for (String path : paths) {
            completePath = completePath.resolve(path);
        }
        Assertions.assertTrue(Files.exists(completePath));
    }
}
