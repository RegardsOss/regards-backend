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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.chain;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.Jason2ProductMetadataPlugin;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.RegexDiskScanning;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.ingest.client.IIngestClient;

/**
 * Test JASON2 IGDR processing chain
 *
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=jason2idgr", "jwt.secret=123456789",
        "regards.workspace=target/workspace" })
@ContextConfiguration(classes = { Jason2IgdrProcessingChainTest.AcquisitionConfiguration.class })
@ActiveProfiles({ "disableDataProviderTask" })
public class Jason2IgdrProcessingChainTest extends AbstractDaoTest {

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IDatasetClient datasetClient;

    @Autowired
    private IAcquisitionFileRepository fileRepository;

    @Autowired
    private IProductRepository productRepository;

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    static class AcquisitionConfiguration {

        @Bean
        public IIngestClient ingestClient() {
            return Mockito.mock(IIngestClient.class);
        }

        @Bean
        public IDatasetClient datasetClient() {
            return Mockito.mock(IDatasetClient.class);
        }
    }

    @Test
    public void startChain() throws ModuleException, InterruptedException {
        AcquisitionProcessingChain processingChain = createJason2IgdrChain();
        processingService.startManualChain(processingChain.getId());

        // 1 job is created for scanning, registering files and creating products
        // 1 job per product is created for SIP generation
        // Submission jobs is disabled for this test using profile

        // Wait until all files are registered as acquired
        int fileAcquired = 0;
        int loops = 10;
        do {
            Thread.sleep(1_000);
            fileAcquired = fileRepository.findByState(AcquisitionFileState.ACQUIRED).size();
            loops--;
        } while ((fileAcquired != 3) || (loops == 0));

        if ((fileAcquired != 3) && (loops == 0)) {
            Assert.fail();
        }

        // Wait until SIP are generated
        int productGenerated = 0;
        loops = 10;
        do {
            Thread.sleep(1_000);
            productGenerated = productRepository.findNoLockBySipState(ProductSIPState.GENERATED).size();
            loops--;
        } while ((productGenerated != 3) || (loops == 0));

        if ((productGenerated != 3) && (loops == 0)) {
            Assert.fail();
        }
    }

    private AcquisitionProcessingChain createJason2IgdrChain() throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("JASON2_IGDR");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");

        Dataset dataSet = initJason2IGDRDataset();
        processingChain.setDatasetIpId(dataSet.getIpId().toString());

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        fileInfo.setDataType(DataType.RAWDATA);

        // TODO invalid folder "/var/regards/data/invalid"

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(RegexDiskScanning.FIELD_DIRS,
                              Arrays.asList("src/test/resources/income/data" + "/JASON2/IGDR"))
                .addParameter(RegexDiskScanning.FIELD_REGEX,
                              "JA2_IP(N|S|R)_2P[a-zA-Z]{1}P[0-9]{3}_[0-9]{3,4}(_[0-9]{8}_[0-9]{6}){2}(.nc){0,1}")
                .getParameters();

        // Plugin and plugin interface packages
        List<String> prefixes = Arrays.asList(IScanPlugin.class.getPackage().getName(),
                                              RegexDiskScanning.class.getPackage().getName());
        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(parameters, RegexDiskScanning.class,
                                                                            prefixes);
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
        List<PluginParameter> productParameters = PluginParametersFactory.build()
                .addParameter(DefaultProductPlugin.FIELD_REMOVE_EXT, Boolean.TRUE).getParameters();
        PluginConfiguration productPlugin = PluginUtils
                .getPluginConfiguration(productParameters, DefaultProductPlugin.class, Lists.newArrayList());
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils
                .getPluginConfiguration(Lists.newArrayList(), Jason2ProductMetadataPlugin.class, Lists.newArrayList());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        // Save processing chain
        return processingService.createChain(processingChain);
    }

    private Dataset initJason2IGDRDataset() {
        Dataset dataSet = new Dataset();
        final UniformResourceName aipUrn = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET, "SSALTO",
                UUID.randomUUID(), 1);
        dataSet.setIpId(aipUrn);
        dataSet.setSipId("DA_TC_JASON2_IGDR");
        Mockito.when(datasetClient.retrieveDataset(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(new Resource<Dataset>(dataSet), HttpStatus.OK));
        return dataSet;
    }
}
