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

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.MicroConfiguration;
import fr.cnes.regards.modules.acquisition.plugins.productreader.ProductFromMetaXmlPlugin;
import fr.cnes.regards.modules.acquisition.plugins.productreader.ProductFromDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.plugins.productreader.ProductFromSagDescriptorDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.plugins.productreader.TarGzProductFromMetaXmlPlugin;
import fr.cnes.regards.modules.acquisition.plugins.productreader.TsvProductFromMetaXmlPlugin;
import fr.cnes.regards.modules.acquisition.plugins.scan.MetadataScanPlugin;
import fr.cnes.regards.modules.acquisition.plugins.scan.SagDescriptorScanPlugin;
import fr.cnes.regards.modules.acquisition.plugins.validation.HktmValidationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.validation.N0bGnssValidationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.validation.ValidationFromMetaXmlPlugin;
import fr.cnes.regards.modules.acquisition.plugins.validation.N0xValidationPlugin;

/**
 * @author oroussel
 */
@ContextConfiguration(classes = { MicroConfiguration.class })
@TestPropertySource("classpath:microscope-test.properties")
public class MicroscopePluginsTest extends AbstractRegardsServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroscopePluginsTest.class);

    // SCAN PLUGINS
    // CCC_HISTORIQUE, DETERMINED_ORBIT, DOPPLER, ORBIT_EVENTS, PVT
    private MetadataScanPlugin cccHistoriqueScanPlugin;

    private MetadataScanPlugin orbitEventsScanPlugin;

    private MetadataScanPlugin dopplerScanPlugin;

    private MetadataScanPlugin determinatedOrbitScanPlugin;

    private MetadataScanPlugin pvtScanPlugin;

    // RAW_HKTM, HKTM_APID
    private MetadataScanPlugin cccRawHktmScanPlugin;

    private MetadataScanPlugin hktmApidScanPlugin;

    // N0b_CMSM, N0c_CMSM, N0B_GNSS
    private MetadataScanPlugin n0bCmsmScanPlugin;

    private MetadataScanPlugin n0cCmsmScanPlugin;

    private SagDescriptorScanPlugin n0bGnssScanPlugin;

    // RINEX ? BDS ?

    // VALIDATION PLUGINS
    // CCC_HISTORIQUE, DETERMINED_ORBIT, DOPPLER, PVT
    // ORBIT_EVENTS (even if file name is always MIC_ORBIT_EVENTS, this name is set under nomFichierDonnee tag)
    private ValidationFromMetaXmlPlugin sagValidationPlugin;

    // RAW_HKTM, HKTM_APID
    private HktmValidationPlugin hktmValidationPlugin;

    // N0b_CMSM, N0c_CMSM
    private N0xValidationPlugin n0xValidationPlugin;

    // N0b_GNSS
    private N0xValidationPlugin n0bGnssValidationPlugin;

    // RINEX ?

    // BDS ?

    // PRODUCT NAME PLUGINS
    // CCC_RAW_HKTM, DETERMINATED_ORBIT, DOPPLER, PVT,
    private ProductFromMetaXmlPlugin productFromMetaXmlPlugin;

    // CCC_HISTORIQUE
    private TarGzProductFromMetaXmlPlugin cccHistoriqueProductPlugin;

    // HKTM_APID
    private TsvProductFromMetaXmlPlugin hktmApidProductPlugin;

    // ORBIT_EVENTS, N0b_CMSM, N0c_CMSM
    private ProductFromDirectoryPlugin productFromDirectoryPlugin;

    // N0b_GNSS
    private ProductFromSagDescriptorDirectoryPlugin n0bGnssProductPlugin;

    // RINEX ? BDS ?

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Before
    public void setUp() {
        tenantResolver.forceTenant(getDefaultTenant());

        Map<Long, Object> pluginCacheMap = new HashMap<>();
        // SCAN PLUGINS
        cccHistoriqueScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                                .addParameter(MetadataScanPlugin.FIELD_DIR,
                                                                              "src/test/resources/MICROSCOPE/CCC_HISTORIQUE")
                                                                .getParameters(), MetadataScanPlugin.class,
                                                        pluginCacheMap);
        orbitEventsScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                              .addParameter(MetadataScanPlugin.FIELD_DIR,
                                                                            "src/test/resources/MICROSCOPE/ORBIT_EVENTS")
                                                              .getParameters(), MetadataScanPlugin.class,
                                                      pluginCacheMap);

        dopplerScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                          .addParameter(MetadataScanPlugin.FIELD_DIR,
                                                                        "src/test/resources/MICROSCOPE/DOPPLER")
                                                          .getParameters(), MetadataScanPlugin.class,
                                                  pluginCacheMap);

        determinatedOrbitScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                                    .addParameter(MetadataScanPlugin.FIELD_DIR,
                                                                                  "src/test/resources/MICROSCOPE/DETERMINATED_ORBIT")
                                                                    .getParameters(), MetadataScanPlugin.class,
                                                            pluginCacheMap);

        pvtScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                      .addParameter(MetadataScanPlugin.FIELD_DIR,
                                                                    "src/test/resources/MICROSCOPE/PVT")
                                                      .getParameters(), MetadataScanPlugin.class, pluginCacheMap);

        cccRawHktmScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                             .addParameter(MetadataScanPlugin.FIELD_DIR,
                                                                           "src/test/resources/MICROSCOPE/CCC_RAW_HKTM")
                                                             .getParameters(), MetadataScanPlugin.class,
                                                     pluginCacheMap);

        hktmApidScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                           .addParameter(MetadataScanPlugin.FIELD_DIR,
                                                                         "src/test/resources/MICROSCOPE/HKTM_APID")
                                                           .getParameters(), MetadataScanPlugin.class,
                                                   pluginCacheMap);

        n0bCmsmScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                          .addParameter(MetadataScanPlugin.FIELD_DIR,
                                                                        "src/test/resources/MICROSCOPE/N0x/N0b_CMSM")
                                                          .getParameters(), MetadataScanPlugin.class,
                                                  pluginCacheMap);

        n0cCmsmScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                          .addParameter(MetadataScanPlugin.FIELD_DIR,
                                                                        "src/test/resources/MICROSCOPE/N0x/N0c_CMSM")
                                                          .getParameters(), MetadataScanPlugin.class,
                                                  pluginCacheMap);

        n0bGnssScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                          .addParameter(SagDescriptorScanPlugin.FIELD_DIR,
                                                                        "src/test/resources/MICROSCOPE/N0x/N0b_GNSS")
                                                          .getParameters(), SagDescriptorScanPlugin.class,
                                                  pluginCacheMap);

        // VALIDATION PLUGINS
        sagValidationPlugin = PluginUtils
                .getPlugin(Collections.emptyList(), ValidationFromMetaXmlPlugin.class, pluginCacheMap);

        hktmValidationPlugin = PluginUtils
                .getPlugin(Collections.emptyList(), HktmValidationPlugin.class, pluginCacheMap);

        n0xValidationPlugin = PluginUtils
                .getPlugin(Collections.emptyList(), N0xValidationPlugin.class, pluginCacheMap);

        n0bGnssValidationPlugin = PluginUtils
                .getPlugin(Collections.emptyList(), N0bGnssValidationPlugin.class, pluginCacheMap);

        // PRODUCT NAME PLUGINS
        productFromMetaXmlPlugin = PluginUtils
                .getPlugin(Collections.emptyList(), ProductFromMetaXmlPlugin.class, pluginCacheMap);

        cccHistoriqueProductPlugin = PluginUtils
                .getPlugin(Collections.emptyList(), TarGzProductFromMetaXmlPlugin.class, pluginCacheMap);

        hktmApidProductPlugin = PluginUtils
                .getPlugin(Collections.emptyList(), TsvProductFromMetaXmlPlugin.class, pluginCacheMap);

        productFromDirectoryPlugin = PluginUtils
                .getPlugin(Collections.emptyList(), ProductFromDirectoryPlugin.class, pluginCacheMap);

        n0bGnssProductPlugin = PluginUtils
                .getPlugin(Collections.emptyList(), ProductFromSagDescriptorDirectoryPlugin.class, pluginCacheMap);

    }

    @Test
    public void testCccHistorique() throws ModuleException {
        List<Path> files = cccHistoriqueScanPlugin.scan(Optional.empty());
        Assert.assertEquals(2, files.size());

        Assert.assertTrue(sagValidationPlugin.validate(files.get(0)));
        Assert.assertFalse(sagValidationPlugin.validate(files.get(1)));

        Assert.assertEquals("HISTO_MIC_2018_05_01", cccHistoriqueProductPlugin.getProductName(files.get(0)));
    }

    @Test
    public void testOrbitEvents() throws ModuleException {
        List<Path> files = orbitEventsScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(sagValidationPlugin.validate(files.get(0)));
        Assert.assertEquals("ORBITEVENTS_01_11_2016_00_06_51_076_30_11_2016_23_59_36_344",
                            productFromDirectoryPlugin.getProductName(files.get(0)));
    }

    @Test
    public void testDoppler() throws ModuleException {
        List<Path> files = dopplerScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(sagValidationPlugin.validate(files.get(0)));
        Assert.assertEquals("MIC_AUS_DOPPLER_2017_10_28_06_35_18",
                            productFromMetaXmlPlugin.getProductName(files.get(0)));
    }

    @Test
    public void testDeterminedOrbit() throws ModuleException {
        List<Path> files = determinatedOrbitScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(sagValidationPlugin.validate(files.get(0)));
        Assert.assertEquals("DETERMINATED_ORBIT_MIC_2016_10_27_00_00_00_2016_10_28_00_03_00",
                            productFromMetaXmlPlugin.getProductName(files.get(0)));
    }

    @Test
    public void testPvt() throws ModuleException {
        List<Path> files = pvtScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(sagValidationPlugin.validate(files.get(0)));
        Assert.assertEquals("MIC_GNSS_PVT_2016_10_27_00_00_00_2016_10_28_00_00_00",
                            productFromMetaXmlPlugin.getProductName(files.get(0)));
    }

    @Test
    public void testCccRawHktm() throws ModuleException {
        List<Path> files = cccRawHktmScanPlugin.scan(Optional.empty());
        Assert.assertEquals(4, files.size());

        for (Path path : files) {
            Assert.assertTrue(hktmValidationPlugin.validate(path));
        }
        // files are sorted lexicographically
        Assert.assertEquals("MIC_HKTMR_0004_2016_10_27_01_50_03",
                            productFromMetaXmlPlugin.getProductName(files.get(0)));
        Assert.assertEquals("MIC_HKTMR_0004_2016_10_27_03_46_37",
                            productFromMetaXmlPlugin.getProductName(files.get(1)));
        Assert.assertEquals("MIC_HKTMR_0004_2016_11_01_02_25_07",
                            productFromMetaXmlPlugin.getProductName(files.get(2)));
        Assert.assertEquals("MIC_HKTMR_0004_2016_11_01_05_39_37",
                            productFromMetaXmlPlugin.getProductName(files.get(3)));
    }

    @Test
    public void testHktmApid() throws ModuleException {
        List<Path> files = hktmApidScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());
        Assert.assertTrue(hktmValidationPlugin.validate(files.get(0)));
        Assert.assertEquals("MIC_CECT_HKTM_0004_20161027T012331_20161027T035100",
                            hktmApidProductPlugin.getProductName(files.get(0)));
    }

    @Test
    public void testN0bCmsm() throws ModuleException {
        List<Path> files = n0bCmsmScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(n0xValidationPlugin.validate(files.get(0)));
        Assert.assertEquals("N0BCMSM_MISSION_20161027T000000_20161028T074518",
                            productFromDirectoryPlugin.getProductName(files.get(0)));
    }

    @Test
    public void testN0cCmsm() throws ModuleException {
        List<Path> files = n0cCmsmScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(n0xValidationPlugin.validate(files.get(0)));
        Assert.assertEquals("N0CCMSM_MISSION_20161027T121959_20180131T132337",
                            productFromDirectoryPlugin.getProductName(files.get(0)));
    }

    @Test
    public void testN0bGnss() throws ModuleException {
        List<Path> files = n0bGnssScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(n0bGnssValidationPlugin.validate(files.get(0)));
        Assert.assertEquals("N0BGNSS_20161027T000000_20161027T020427",
                            n0bGnssProductPlugin.getProductName(files.get(0)));
    }
}
