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
package fr.cnes.regards.modules.acquisition.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.utils.RsRuntimeException;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.MicroConfiguration;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.plugins.product.CmsmScientificProductPlugin;
import fr.cnes.regards.modules.acquisition.plugins.scan.ZipScanPlugin;
import fr.cnes.regards.modules.acquisition.plugins.sip.CmsmScientificSipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.validation.CmsmScientificValidationPlugin;
import fr.cnes.regards.modules.ingest.domain.SIP;

/**
 * Specific test for CMSM scientific (data file is huge so it depends on its existence)
 * @author Olivier Rousselot
 */
@ContextConfiguration(classes = { MicroConfiguration.class })
@TestPropertySource("classpath:microscope-test.properties")
public class CmsmScientificTest extends AbstractRegardsServiceIT {

    private static final String CMSM_SCIENTIFIC_ROOT_PATH =
            System.getProperty("user.home") + "/REGARDS/MICROSCOPE/CMSM_SCIENTIFIQUE";

    //////////////////////////
    // SCAN PLUGINS         //
    //////////////////////////
    // CMSM SCIENTIFIQUE
    private ZipScanPlugin cmsmScientificScanPlugin;

    ////////////////////////
    // VALIDATION PLUGINS //
    ////////////////////////
    private CmsmScientificValidationPlugin cmsmScientificValidationPlugin;

    //////////////////////////
    // PRODUCT NAME PLUGINS //
    //////////////////////////
    private CmsmScientificProductPlugin cmsmScientificProductPlugin;

    ///////////////////////////
    // SIP GENERATION PLUGIN //
    ///////////////////////////
    private CmsmScientificSipGenerationPlugin cmsmScientificSipGenerationPlugin;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @BeforeClass
    public static void canLaunchTest() {
        Assume.assumeTrue(new File(CMSM_SCIENTIFIC_ROOT_PATH, "N2b_01_L_SCA_G_0050_CN2_MRO_RN2_M.zip").exists());
    }

    @Before
    public void setUp() {
        tenantResolver.forceTenant(getDefaultTenant());

        Map<Long, Object> pluginCacheMap = new HashMap<>();
        // SCAN PLUGINS
        cmsmScientificScanPlugin = PluginUtils.getPlugin(
                PluginParametersFactory.build().addParameter(ZipScanPlugin.FIELD_DIR, CMSM_SCIENTIFIC_ROOT_PATH)
                        .getParameters(), ZipScanPlugin.class, pluginCacheMap);

        // VALIDATION PLUGINS
        cmsmScientificValidationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), CmsmScientificValidationPlugin.class, pluginCacheMap);

        // PRODUCT PLUGINS
        cmsmScientificProductPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), CmsmScientificProductPlugin.class, pluginCacheMap);

        // SIP GENERATION PLUGIN
        cmsmScientificSipGenerationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), CmsmScientificSipGenerationPlugin.class, pluginCacheMap);
    }

    @Test
    public void testWithValidation() throws ModuleException {
        List<Path> files = cmsmScientificScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(cmsmScientificValidationPlugin.validate(files.get(0)));
        String productName = cmsmScientificProductPlugin.getProductName(files.get(0));
        Assert.assertEquals("N2b_01_L_SCA_G_0050_CN2_MRO_RN2_M", productName);
    }

    @Test
    public void testWithoutValidation() throws ModuleException {
        List<Path> files = cmsmScientificScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        String productName = cmsmScientificProductPlugin.getProductName(files.get(0));
        Assert.assertEquals("N2b_01_L_SCA_G_0050_CN2_MRO_RN2_M", productName);
        Product product = createProduct(files.get(0), productName);

        SIP sip = cmsmScientificSipGenerationPlugin.generate(product);
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.SESSION_NB));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.PHASE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.TECHNO));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.SESSION_TYPE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.SESSION_SUB_TYPE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.SPIN));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.CAL_PARAM));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.RECORD_FILE_NAME));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.RECORD_VERSION));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.ENV_CONSTRAINT));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.ACTIVE_SU));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.PID_VERSION));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.COMMENT));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.ORBITS_COUNT));
    }

    private static Product createProduct(Path filePath, String productName) {
        Product product = new Product();
        product.setProductName(productName);
        AcquisitionFile af = new AcquisitionFile();
        af.setFilePath(filePath);
        af.setState(AcquisitionFileState.ACQUIRED);
        af.setChecksumAlgorithm(Microscope.CHECKSUM_ALGO);
        af.setChecksum("60a3573cc7d978e537c6497605464525");
        product.addAcquisitionFile(af);
        return product;
    }
}
