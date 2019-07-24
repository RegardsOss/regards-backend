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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.GeoJsonFeatureCollectionParserPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.GeoJsonSIPGeneration;

/**
 * Test {@link AcquisitionProcessingService} for {@link Product} workflow
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_geojson_product" })
public class GeojsonProductAcquisitionServiceTest extends AbstractMultitenantServiceTest {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(GeojsonProductAcquisitionServiceTest.class);

    @Autowired
    private IAcquisitionProcessingService processingService;

    @Autowired
    private IAcquisitionFileRepository acqFileRepository;

    @Autowired
    private IProductService productService;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    public AcquisitionProcessingChain createProcessingChain() throws ModuleException {

        // Pathes
        Path dataPath = Paths.get("src", "test", "resources", "data", "plugins", "geojson");

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("French departments");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");

        // RAW DATA file infos
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("GeoJson");
        fileInfo.setMimeType(MediaType.APPLICATION_JSON);
        fileInfo.setDataType(DataType.RAWDATA);

        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(GeoJsonFeatureCollectionParserPlugin.FIELD_FEATURE_ID, "nom")
                .addParameter(GeoJsonFeatureCollectionParserPlugin.FIELD_DIR, dataPath.toString()).getParameters();

        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(parameters,
                                                                            GeoJsonFeatureCollectionParserPlugin.class);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin RAWDATA" + Math.random());
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                                  DefaultFileValidation.class);
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin" + Math.random());
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                               DefaultProductPlugin.class);
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin" + Math.random());
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils.getPluginConfiguration(Sets.newHashSet(),
                                                                              GeoJsonSIPGeneration.class);
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin" + Math.random());
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        // Save processing chain
        return processingService.createChain(processingChain);
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

        // Find product scheduled
        long scheduled = productService.countByProcessingChainAndSipStateIn(processingChain,
                                                                            Arrays.asList(ProductSIPState.SCHEDULED));
        Assert.assertTrue(scheduled == 1);

        // Run the job synchronously
        SIPGenerationJob genJob = new SIPGenerationJob();
        beanFactory.autowireBean(genJob);

        Map<String, JobParameter> parameters = new HashMap<>();
        parameters.put(SIPGenerationJob.CHAIN_PARAMETER_ID,
                       new JobParameter(SIPGenerationJob.CHAIN_PARAMETER_ID, processingChain.getId()));
        Set<String> productNames = new HashSet<>();
        productNames.add("Vaucluse.json");
        parameters.put(SIPGenerationJob.PRODUCT_NAMES, new JobParameter(SIPGenerationJob.PRODUCT_NAMES, productNames));

        genJob.setParameters(parameters);
        genJob.run();

        // Find product to submitted
        long submitted = productService.countByProcessingChainAndSipStateIn(processingChain,
                                                                            Arrays.asList(ProductSIPState.SUBMITTED));
        Assert.assertTrue(submitted == 1);

    }
}
