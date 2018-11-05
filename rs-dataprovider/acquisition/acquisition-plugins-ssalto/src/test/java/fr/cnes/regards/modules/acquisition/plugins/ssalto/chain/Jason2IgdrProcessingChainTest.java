/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.AbstractProductMetadataPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata.Jason2ProductMetadataPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.RegexDiskScanning;

/**
 * Test JASON2 IGDR processing chain
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=jason2idgr", "jwt.secret=123456789",
        "regards.workspace=target/workspace" })
public class Jason2IgdrProcessingChainTest extends AbstractAcquisitionChainTest {

    @Override
    protected AcquisitionProcessingChain createAcquisitionChain() throws ModuleException {

        // Create a processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel("JASON2_IGDR");
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

        Set<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(RegexDiskScanning.FIELD_DIRS,
                              Arrays.asList("src/test/resources/income/data" + "/JASON2/IGDR"))
                .addParameter(RegexDiskScanning.FIELD_REGEX,
                              "JA2_IP(N|S|R)_2P[a-zA-Z]{1}P[0-9]{3}_[0-9]{3,4}(_[0-9]{8}_[0-9]{6}){2}(.nc){0,1}")
                .getParameters();

        // Plugin and plugin interface packages
        PluginConfiguration scanPlugin = PluginUtils.getPluginConfiguration(parameters, RegexDiskScanning.class);
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
        Set<PluginParameter> productParameters = PluginParametersFactory.build()
                .addParameter(DefaultProductPlugin.FIELD_REMOVE_EXT, Boolean.TRUE).getParameters();
        PluginConfiguration productPlugin = PluginUtils.getPluginConfiguration(productParameters,
                                                                               DefaultProductPlugin.class);
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginUtils.getPluginConfiguration(PluginParametersFactory.build()
                .addParameter(AbstractProductMetadataPlugin.DATASET_SIP_ID, "DA_TC_JASON2_IGDR").getParameters(),
                                                                              Jason2ProductMetadataPlugin.class);
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required

        return processingChain;
    }

    @Ignore
    @Test
    public void removeProcessingChain() throws ModuleException {
        AcquisitionProcessingChain processingChain = processingService.getChain(1L);
        processingChain.setActive(Boolean.FALSE);
        processingService.updateChain(processingChain);
        processingService.deleteChain(processingChain.getId());
    }

    @Override
    protected long getExpectedFiles() {
        return 3;
    }

    @Override
    protected long getExpectedProducts() {
        return 3;
    }

}
