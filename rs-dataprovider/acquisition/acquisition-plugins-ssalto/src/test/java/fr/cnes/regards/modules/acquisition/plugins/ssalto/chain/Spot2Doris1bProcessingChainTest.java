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

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.plugins.IScanPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.AbstractProductMetadataPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.Spot2ProductMetadataPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.RegexDiskScanning;

/**
 * Test JASON2 IGDR processing chain
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=spot2doris1b",
        "jwt.secret=123456789", "regards.workspace=target/workspace" })
public class Spot2Doris1bProcessingChainTest extends AbstractAcquisitionChainTest {

    @Autowired
    private IJobService jobService;

    @Override
    public void startChain() throws ModuleException, InterruptedException {
        super.startChain();
    }

    @Ignore
    @Test
    public void stopChain() throws ModuleException {
        // Enable listener
        jobService.onApplicationEvent(null);

        Long id = 1L;
        processingService.startManualChain(id);
        AcquisitionProcessingChain processingChain = processingService.stopAndCleanChain(id);
        assertNotNull(processingChain);
    }

    @Override
    protected AcquisitionProcessingChain createAcquisitionChain() throws ModuleException {
        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("SPOT2_DORIS1B");
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);

        // TODO invalid folder "/var/regards/data/invalid"

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(RegexDiskScanning.FIELD_DIRS,
                              Arrays.asList("src/test/resources/income/data" + "/spot2/doris1b_moe_cddis"))
                .addParameter(RegexDiskScanning.FIELD_REGEX, "DORDATA_[0-9]{6}.SP2").getParameters();

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
                .addParameter(DefaultProductPlugin.FIELD_PREFIX, "MOE_CDDIS_").getParameters();
        PluginConfiguration productPlugin = PluginUtils
                .getPluginConfiguration(productParameters, DefaultProductPlugin.class, Lists.newArrayList());
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils.getPluginConfiguration(PluginParametersFactory.build()
                .addParameter(AbstractProductMetadataPlugin.DATASET_SIP_ID, "DA_TC_SPOT2_DORIS1B_MOE_CDDIS")
                .getParameters(), Spot2ProductMetadataPlugin.class, Lists.newArrayList());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        return processingChain;
    }

    @Override
    protected int getExpectedFiles() {
        return 1;
    }

    @Override
    protected int getExpectedProducts() {
        return 1;
    }
}
