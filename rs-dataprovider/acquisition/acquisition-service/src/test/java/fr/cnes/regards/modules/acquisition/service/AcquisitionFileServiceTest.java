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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * Test {@link AcquisitionFileService}
 *
 * @author Sébastien Binda
 *
 */
@RunWith(SpringRunner.class)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_product", "jwt.secret=123456789",
        "regards.workspace=target/workspace" })
@ContextConfiguration(classes = { ProductAcquisitionServiceTest.AcquisitionConfiguration.class })
@MultitenantTransactional
public class AcquisitionFileServiceTest extends AbstractDaoTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductAcquisitionServiceTest.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IPluginService pluginService;

    @SuppressWarnings("unused")
    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Autowired
    private IProductService productService;

    @SuppressWarnings("unused")
    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private IAcquisitionFileService fileService;

    private AcquisitionProcessingChain processingChain;

    @Before
    public void init() throws ModuleException {
        createFiles();
    }

    public AcquisitionProcessingChain createProcessingChain() throws ModuleException {

        // Create a processing chain
        processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("Product");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setDatasetIpId("DATASET_IP_ID");

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        fileInfo.setDataType(DataType.RAWDATA);

        // Search directory
        Path searchDir = Paths.get("src", "test", "resources", "data", "plugins", "scan");

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(GlobDiskScanning.FIELD_DIRS, Arrays.asList(searchDir.toString())).getParameters();

        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(parameters, GlobDiskScanning.class,
                                                                            Lists.newArrayList());
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultFileValidation.class, Lists.newArrayList());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultProductPlugin.class, Lists.newArrayList());
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), DefaultSIPGeneration.class, Lists.newArrayList());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        pluginService.addPluginPackage(IScanPlugin.class.getPackage().getName());
        pluginService.addPluginPackage(GlobDiskScanning.class.getPackage().getName());

        // Save processing chain
        return processingService.createChain(processingChain);
    }

    public void createFiles() throws ModuleException {

        AcquisitionProcessingChain processingChain = createProcessingChain();
        Product product = new Product();
        product.setIpId("IpId");
        product.setLastUpdate(OffsetDateTime.now());
        product.setProcessingChain(processingChain);
        product.setProductName("product name");
        product.setSession("session");
        product.setSipState(SIPState.INDEXED);
        product.setState(ProductState.COMPLETED);
        product = productService.save(product);

        // Add acquisition files
        for (AcquisitionFileState fileState : AcquisitionFileState.values()) {
            AcquisitionFile file = new AcquisitionFile();
            file.setAcqDate(OffsetDateTime.now());
            file.setChecksum("123445");
            file.setChecksumAlgorithm("MD5");
            file.setError("");
            file.setFileInfo(processingChain.getFileInfos().get(0));
            file.setFilePath(Paths.get("/tmp/file"));
            file.setProduct(product);
            file.setState(fileState);
            fileService.save(file);
        }
    }

    @Test
    public void testCountFilesforProcessingChain() {
        Assert.assertTrue(fileService.countByChain(processingChain) == AcquisitionFileState.values().length);
        for (AcquisitionFileState fileState : AcquisitionFileState.values()) {
            Assert.assertTrue(fileService.countByChainAndStateIn(processingChain, Arrays.asList(fileState)) == 1);
        }

    }

}
