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
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.MicroConfiguration;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.plugins.product.BdsProductPlugin;
import fr.cnes.regards.modules.acquisition.plugins.product.ProductFromMetaXmlPlugin;
import fr.cnes.regards.modules.acquisition.plugins.product.ProductFromDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.plugins.product.ProductFromSagDescriptorPathPlugin;
import fr.cnes.regards.modules.acquisition.plugins.product.RinexProductPlugin;
import fr.cnes.regards.modules.acquisition.plugins.product.TarGzProductFromMetaXmlPlugin;
import fr.cnes.regards.modules.acquisition.plugins.product.TsvProductFromMetaXmlPlugin;
import fr.cnes.regards.modules.acquisition.plugins.scan.MetadataScanPlugin;
import fr.cnes.regards.modules.acquisition.plugins.scan.SagDescriptorScanPlugin;
import fr.cnes.regards.modules.acquisition.plugins.scan.TarGzScanPlugin;
import fr.cnes.regards.modules.acquisition.plugins.sip.BdsSipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.sip.CccHistoriqueSipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.sip.CccRawHktmSipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.sip.DopplerSipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.sip.FromMetadataSipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.sip.HktmApidSipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.sip.N0bCmsmSipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.sip.N0bGnssSipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.sip.N0cCmsmSipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.sip.RinexSipGenerationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.validation.HktmValidationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.validation.N0bGnssValidationPlugin;
import fr.cnes.regards.modules.acquisition.plugins.validation.ValidationFromMd5TxtPlugin;
import fr.cnes.regards.modules.acquisition.plugins.validation.ValidationFromMetaXmlPlugin;
import fr.cnes.regards.modules.acquisition.plugins.validation.N0xValidationPlugin;
import fr.cnes.regards.modules.ingest.domain.SIP;

/**
 * @author Olivier Rousselot
 */
@ContextConfiguration(classes = { MicroConfiguration.class })
@TestPropertySource("classpath:microscope-test.properties")
public class MicroscopePluginsTest extends AbstractRegardsServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroscopePluginsTest.class);

    //////////////////////////
    // SCAN PLUGINS         //
    //////////////////////////
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

    // RINEX, BDS
    private TarGzScanPlugin rinexScanPlugin;

    private TarGzScanPlugin bdsScanPlugin;

    //////////////////////////
    // VALIDATION PLUGINS   //
    //////////////////////////
    // CCC_HISTORIQUE, DETERMINED_ORBIT, DOPPLER, PVT
    // ORBIT_EVENTS (even if file name is always MIC_ORBIT_EVENTS, this name is set under nomFichierDonnee tag)
    private ValidationFromMetaXmlPlugin sagValidationPlugin;

    // RAW_HKTM, HKTM_APID
    private HktmValidationPlugin hktmValidationPlugin;

    // N0b_CMSM, N0c_CMSM
    private N0xValidationPlugin n0xValidationPlugin;

    // N0b_GNSS
    private N0xValidationPlugin n0bGnssValidationPlugin;

    // RINEX, BDS
    private ValidationFromMd5TxtPlugin validationFromMd5TxtPlugin;

    //////////////////////////
    // PRODUCT NAME PLUGINS //
    //////////////////////////
    // CCC_RAW_HKTM, DETERMINATED_ORBIT, DOPPLER, PVT,
    private ProductFromMetaXmlPlugin productFromMetaXmlPlugin;

    // CCC_HISTORIQUE
    private TarGzProductFromMetaXmlPlugin cccHistoriqueProductPlugin;

    // HKTM_APID
    private TsvProductFromMetaXmlPlugin hktmApidProductPlugin;

    // ORBIT_EVENTS, N0b_CMSM, N0c_CMSM
    private ProductFromDirectoryPlugin productFromDirectoryPlugin;

    // N0b_GNSS
    private ProductFromSagDescriptorPathPlugin n0bGnssProductPlugin;

    // RINEX
    private RinexProductPlugin rinexProductPlugin;

    // BDS
    private BdsProductPlugin bdsProductPlugin;

    ////////////////////////////
    // SIP GENERATION PLUGINS //
    ////////////////////////////
    // CCC_HISTORIQUE
    private CccHistoriqueSipGenerationPlugin cccHistoriqueSipGenerationPlugin;

    // DETERMINATED_ORBIT, ORBIT_EVENTS
    private FromMetadataSipGenerationPlugin fromMetadataSipGenerationPlugin;

    // DOPPLER
    private DopplerSipGenerationPlugin dopplerSipGenerationPlugin;

    // CCC_RAW_HKTM
    private CccRawHktmSipGenerationPlugin cccRawHktmSipGenerationPlugin;

    // HKTM_APID
    private HktmApidSipGenerationPlugin hktmApidSipGenerationPlugin;

    // N0b_GNSS
    private N0bGnssSipGenerationPlugin n0bGnssSipGenerationPlugin;

    // N0b_CMSM
    private N0bCmsmSipGenerationPlugin n0bCmsmSipGenerationPlugin;

    // N0c_CMSM
    private N0cCmsmSipGenerationPlugin n0cCmsmSipGenerationPlugin;

    // RINEX
    private RinexSipGenerationPlugin rinexSipGenerationPlugin;

    // BDS
    private BdsSipGenerationPlugin bdsSipGenerationPlugin;

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
                                                          .getParameters(), MetadataScanPlugin.class, pluginCacheMap);

        determinatedOrbitScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                                    .addParameter(MetadataScanPlugin.FIELD_DIR,
                                                                                  "src/test/resources/MICROSCOPE/DETERMINATED_ORBIT")
                                                                    .getParameters(), MetadataScanPlugin.class,
                                                            pluginCacheMap);

        pvtScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build().addParameter(MetadataScanPlugin.FIELD_DIR,
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
                                                           .getParameters(), MetadataScanPlugin.class, pluginCacheMap);

        n0bCmsmScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                          .addParameter(MetadataScanPlugin.FIELD_DIR,
                                                                        "src/test/resources/MICROSCOPE/N0x/N0b_CMSM")
                                                          .getParameters(), MetadataScanPlugin.class, pluginCacheMap);

        n0cCmsmScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                          .addParameter(MetadataScanPlugin.FIELD_DIR,
                                                                        "src/test/resources/MICROSCOPE/N0x/N0c_CMSM")
                                                          .getParameters(), MetadataScanPlugin.class, pluginCacheMap);

        n0bGnssScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build()
                                                          .addParameter(SagDescriptorScanPlugin.FIELD_DIR,
                                                                        "src/test/resources/MICROSCOPE/N0x/N0b_GNSS")
                                                          .getParameters(), SagDescriptorScanPlugin.class,
                                                  pluginCacheMap);

        rinexScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build().addParameter(TarGzScanPlugin.FIELD_DIR,
                                                                                             "src/test/resources/MICROSCOPE/RINEX")
                                                        .getParameters(), TarGzScanPlugin.class, pluginCacheMap);

        bdsScanPlugin = PluginUtils.getPlugin(PluginParametersFactory.build().addParameter(TarGzScanPlugin.FIELD_DIR,
                                                                                           "src/test/resources/MICROSCOPE/BDS")
                                                      .getParameters(), TarGzScanPlugin.class, pluginCacheMap);

        // VALIDATION PLUGINS
        sagValidationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), ValidationFromMetaXmlPlugin.class, pluginCacheMap);

        hktmValidationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), HktmValidationPlugin.class, pluginCacheMap);

        n0xValidationPlugin = PluginUtils.getPlugin(Collections.emptySet(), N0xValidationPlugin.class, pluginCacheMap);

        n0bGnssValidationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), N0bGnssValidationPlugin.class, pluginCacheMap);

        validationFromMd5TxtPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), ValidationFromMd5TxtPlugin.class, pluginCacheMap);

        // PRODUCT NAME PLUGINS
        productFromMetaXmlPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), ProductFromMetaXmlPlugin.class, pluginCacheMap);

        cccHistoriqueProductPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), TarGzProductFromMetaXmlPlugin.class, pluginCacheMap);

        hktmApidProductPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), TsvProductFromMetaXmlPlugin.class, pluginCacheMap);

        productFromDirectoryPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), ProductFromDirectoryPlugin.class, pluginCacheMap);

        n0bGnssProductPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), ProductFromSagDescriptorPathPlugin.class, pluginCacheMap);

        rinexProductPlugin = PluginUtils.getPlugin(Collections.emptySet(), RinexProductPlugin.class, pluginCacheMap);

        bdsProductPlugin = PluginUtils.getPlugin(Collections.emptySet(), BdsProductPlugin.class, pluginCacheMap);

        // SIP GENERATION PLUGINS
        cccHistoriqueSipGenerationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), CccHistoriqueSipGenerationPlugin.class, pluginCacheMap);

        fromMetadataSipGenerationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), FromMetadataSipGenerationPlugin.class, pluginCacheMap);

        dopplerSipGenerationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), DopplerSipGenerationPlugin.class, pluginCacheMap);

        cccRawHktmSipGenerationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), CccRawHktmSipGenerationPlugin.class, pluginCacheMap);

        hktmApidSipGenerationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), HktmApidSipGenerationPlugin.class, pluginCacheMap);

        n0bGnssSipGenerationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), N0bGnssSipGenerationPlugin.class, pluginCacheMap);

        n0bCmsmSipGenerationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), N0bCmsmSipGenerationPlugin.class, pluginCacheMap);

        n0cCmsmSipGenerationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), N0cCmsmSipGenerationPlugin.class, pluginCacheMap);

        rinexSipGenerationPlugin = PluginUtils
                .getPlugin(Collections.emptySet(), RinexSipGenerationPlugin.class, pluginCacheMap);

        bdsSipGenerationPlugin =  PluginUtils
                .getPlugin(Collections.emptySet(), BdsSipGenerationPlugin.class, pluginCacheMap);
    }

    @Test
    public void testBds() throws ModuleException {
        List<Path> files = bdsScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(validationFromMd5TxtPlugin.validate(files.get(0)));
        String productName = bdsProductPlugin.getProductName(files.get(0));
        Assert.assertEquals("BDS_3.3.0.3patchDM10688", productName);
        Product product = createProduct(files.get(0), productName, true);

        SIP sip1 = bdsSipGenerationPlugin.generate(product);
        Assert.assertTrue(sip1.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip1.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
        Assert.assertTrue(sip1.getProperties().getDescriptiveInformation().containsKey(Microscope.VERSION));
    }

    @Test
    public void testCccHistorique() throws ModuleException {
        List<Path> files = cccHistoriqueScanPlugin.scan(Optional.empty());
        Assert.assertEquals(2, files.size());

        Assert.assertTrue(sagValidationPlugin.validate(files.get(0)));
        Assert.assertFalse(sagValidationPlugin.validate(files.get(1)));

        String productName = cccHistoriqueProductPlugin.getProductName(files.get(0));
        Assert.assertEquals("HISTO_MIC_2018_05_01", productName);
        Product product = createProduct(files.get(0), productName);

        SIP sip = cccHistoriqueSipGenerationPlugin.generate(product);
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));

    }

    @Test
    public void testOrbitEvents() throws ModuleException {
        List<Path> files = orbitEventsScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(sagValidationPlugin.validate(files.get(0)));
        String productName = productFromDirectoryPlugin.getProductName(files.get(0));
        Assert.assertEquals("ORBITEVENTS_01_11_2016_00_06_51_076_30_11_2016_23_59_36_344", productName);
        Product product = createProduct(files.get(0), productName);

        SIP sip = fromMetadataSipGenerationPlugin.generate(product);
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
    }

    @Test
    public void testDoppler() throws ModuleException {
        List<Path> files = dopplerScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(sagValidationPlugin.validate(files.get(0)));
        String productName = productFromMetaXmlPlugin.getProductName(files.get(0));
        Assert.assertEquals("MIC_AUS_DOPPLER_2017_10_28_06_35_18", productName);
        Product product = createProduct(files.get(0), productName);

        SIP sip = dopplerSipGenerationPlugin.generate(product);
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.STATION));
    }

    @Test
    public void testDeterminatedOrbit() throws ModuleException {
        List<Path> files = determinatedOrbitScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(sagValidationPlugin.validate(files.get(0)));
        String productName = productFromMetaXmlPlugin.getProductName(files.get(0));
        Assert.assertEquals("DETERMINATED_ORBIT_MIC_2016_10_27_00_00_00_2016_10_28_00_03_00", productName);
        Product product = createProduct(files.get(0), productName);

        SIP sip = fromMetadataSipGenerationPlugin.generate(product);
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
    }

    @Test
    public void testPvt() throws ModuleException {
        List<Path> files = pvtScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(sagValidationPlugin.validate(files.get(0)));
        String productName = productFromMetaXmlPlugin.getProductName(files.get(0));
        Assert.assertEquals("MIC_GNSS_PVT_2016_10_27_00_00_00_2016_10_28_00_00_00", productName);

        Product product = createProduct(files.get(0), productName);

        SIP sip = fromMetadataSipGenerationPlugin.generate(product);
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
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
        String productName = productFromMetaXmlPlugin.getProductName(files.get(3));
        Assert.assertEquals("MIC_HKTMR_0004_2016_11_01_05_39_37", productName);

        Product product = createProduct(files.get(0), productName);

        SIP sip = cccRawHktmSipGenerationPlugin.generate(product);
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.APID));
    }

    @Test
    public void testHktmApid() throws ModuleException {
        List<Path> files = hktmApidScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());
        Assert.assertTrue(hktmValidationPlugin.validate(files.get(0)));
        String productName = hktmApidProductPlugin.getProductName(files.get(0));
        Assert.assertEquals("MIC_CECT_HKTM_0004_20161027T012331_20161027T035100", productName);

        Product product = createProduct(files.get(0), productName);

        SIP sip = hktmApidSipGenerationPlugin.generate(product);
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.APID));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.APID_MNEMONIC));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.TM_TYPE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.BDS_VERSION));

    }

    @Test
    public void testN0bCmsm() throws ModuleException {
        List<Path> files = n0bCmsmScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(n0xValidationPlugin.validate(files.get(0)));
        String productName = productFromDirectoryPlugin.getProductName(files.get(0));
        Assert.assertEquals("N0BCMSM_MISSION_20161027T000000_20161028T074518", productName);
        Product product = createProduct(files.get(0), productName);

        SIP sip = n0bCmsmSipGenerationPlugin.generate(product);
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.SCOPE));

    }

    @Test
    public void testN0cCmsm() throws ModuleException {
        List<Path> files = n0cCmsmScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(n0xValidationPlugin.validate(files.get(0)));
        String productName = productFromDirectoryPlugin.getProductName(files.get(0));
        Assert.assertEquals("N0CCMSM_MISSION_20161027T121959_20180131T132337", productName);
        Product product = createProduct(files.get(0), productName);

        SIP sip = n0cCmsmSipGenerationPlugin.generate(product);
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.SCOPE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.SESSION));
    }

    @Test
    public void testN0bGnss() throws ModuleException {
        List<Path> files = n0bGnssScanPlugin.scan(Optional.empty());
        Assert.assertEquals(1, files.size());

        Assert.assertTrue(n0bGnssValidationPlugin.validate(files.get(0)));
        String productName = n0bGnssProductPlugin.getProductName(files.get(0));
        Assert.assertEquals("N0BGNSS_20161027T000000_20161027T020427", productName);
        Product product = createProduct(files.get(0), productName);

        SIP sip = n0bGnssSipGenerationPlugin.generate(product);
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
    }

    @Test
    public void testRinex() throws ModuleException {
        List<Path> files = rinexScanPlugin.scan(Optional.empty());
        Assert.assertEquals(2, files.size());

        Assert.assertTrue(validationFromMd5TxtPlugin.validate(files.get(0)));
        Assert.assertTrue(validationFromMd5TxtPlugin.validate(files.get(1)));

        String product1Name = rinexProductPlugin.getProductName(files.get(0));
        Assert.assertEquals("RINEX_0120", product1Name);
        String product2Name = rinexProductPlugin.getProductName(files.get(1));
        Assert.assertEquals("RINEX_0122", product2Name);

        Product product1 = createProduct(files.get(0), product1Name, true);

        SIP sip1 = rinexSipGenerationPlugin.generate(product1);
        Assert.assertTrue(sip1.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip1.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
        Assert.assertTrue(sip1.getProperties().getDescriptiveInformation().containsKey(Microscope.SESSION));

        Product product2 = createProduct(files.get(1), product2Name, true);

        SIP sip2 = rinexSipGenerationPlugin.generate(product2);
        Assert.assertTrue(sip2.getProperties().getDescriptiveInformation().containsKey(Microscope.START_DATE));
        Assert.assertTrue(sip2.getProperties().getDescriptiveInformation().containsKey(Microscope.END_DATE));
        Assert.assertTrue(sip2.getProperties().getDescriptiveInformation().containsKey(Microscope.SESSION));
    }

    private static Product createProduct(Path filePath, String productName) {
        return createProduct(filePath, productName, false);
    }

    private static Product createProduct(Path filePath, String productName, boolean isProductFile) {
        Product product = new Product();
        product.setProductName(productName);
        AcquisitionFile af = new AcquisitionFile();
        af.setFilePath(filePath);
        af.setState(AcquisitionFileState.ACQUIRED);
        if (isProductFile) {
            af.setChecksumAlgorithm(Microscope.CHECKSUM_ALGO);
            try {
                af.setChecksum(ChecksumUtils.computeHexChecksum(new FileInputStream(filePath.toFile()),
                                                                Microscope.CHECKSUM_ALGO));
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        product.addAcquisitionFile(af);
        return product;
    }

}
