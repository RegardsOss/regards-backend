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

package fr.cnes.regards.modules.acquisition.step;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameterType;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.ChainGenerationBuilder;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFileBuilder;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectoryBuilder;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.MetaProductDto;
import fr.cnes.regards.modules.acquisition.domain.metadata.dto.SetOfMetaFileDto;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.service.AcquisitionFileServiceIT;
import fr.cnes.regards.modules.acquisition.service.AcquisitionServiceConfiguration;
import fr.cnes.regards.modules.acquisition.service.IChainGenerationService;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;
import fr.cnes.regards.modules.acquisition.service.IMetaProductService;
import fr.cnes.regards.modules.acquisition.service.IScanDirectoryService;

/**
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AcquisitionServiceConfiguration.class })
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)

public class ScanJobIT extends AbstractRegardsServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionFileServiceIT.class);

    private static final String TENANT = "PROJECT";

    private static final String CHAINE_LABEL = "chaine label";

    private static final String DATASET_NAME = "dataset name";

    private static final String META_PRODUCT_NAME = "meta product name";

    private static final String DEFAULT_USER = "John Doe";

    public final static String META_PRODUCT_PARAM = "meta-produt";

    public final static String META_FILE_PARAM = "meta-file";

    @Autowired
    private IChainGenerationService chainService;

    @Autowired
    private IMetaProductService metaProductService;

    @Autowired
    private IMetaFileService metaFileService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IScanDirectoryService scandirService;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private JWTService jwtService;

    @Autowired
    IPluginConfigurationRepository pluginConfigurationRepository;

    @Autowired
    private IMetaProductRepository metaProductRepository;;

    @Autowired
    private IPluginService pluginService;

    private ChainGeneration chain;

    private MetaFile metaFile;

    private MetaProduct metaProduct;

    @Before
    public void setUp() throws Exception {
        tenantResolver.forceTenant(TENANT);
        jwtService.injectToken(TENANT, DEFAULT_ROLE, DEFAULT_USER);
    }

    @Before
    public void init() {

        cleanDb();

        this.metaProduct = metaProductService.save(MetaProductBuilder.build(META_PRODUCT_NAME).get());

        this.chain = ChainGenerationBuilder.build(CHAINE_LABEL).isActive().withDataSet(DATASET_NAME).get();
        this.chain.setMetaProduct(metaProduct);

        // Create 2 ScanDirectory
        ScanDirectory scanDir1 = scandirService.save(ScanDirectoryBuilder.build("/var/regards/data/input1")
                .withDateAcquisition(OffsetDateTime.now().minusDays(5)).get());
        ScanDirectory scanDir2 = scandirService.save(ScanDirectoryBuilder.build("/var/regards/data/input2")
                .withDateAcquisition(OffsetDateTime.now().minusMinutes(15)).get());

        metaFile = metaFileService.save(MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withFileType(MediaType.APPLICATION_JSON_VALUE).withFilePattern("file pattern")
                .comment("test scan directory comment").isMandatory().addScanDirectory(scanDir1)
                .addScanDirectory(scanDir2).get());
    }

    @Test
    public void runActiveChainGeneration() throws ModuleException {
        LOGGER.info("start");

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFile);

        String metaFilesJson = new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles));
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));

        PluginConfiguration plgConf = getPluginConfiguration("TestScanDirectoryPlugin",
                                                             IAcquisitionScanDirectoryPlugin.class);
        chain.setScanAcquisitionPluginConf(plgConf.getId());
        chain.addScanAcquisitionParameter(META_PRODUCT_PARAM, metaProductJson);
        chain.addScanAcquisitionParameter(META_FILE_PARAM, metaFilesJson);

        boolean res = chainService.run(chain);
        Assert.assertTrue(res);

        // tester que le job s'exécute et qu'il fait ce qui est attendu

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
            Assert.fail();
        }
    }

    @Test
    public void runActiveChainGenerationWithoutScanPlugin() {
        boolean res = chainService.run(chain);
        Assert.assertTrue(res);

        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
            Assert.fail();
        }
    }

    @Test
    public void runNoActiveChainGeneration() {
        this.chain.setActive(false);
        boolean res = chainService.run(chain);
        Assert.assertFalse(res);
    }

    @Test
    public void runChainGenerationPeriodicity() {
        this.chain.setActive(true);
        this.chain.setLastDateActivation(OffsetDateTime.now().minusHours(1));
        this.chain.setPeriodicity(3650L);
        boolean res = chainService.run(chain);
        Assert.assertFalse(res);
    }

    @After
    public void cleanDb() {
        jobInfoRepository.deleteAll();
        metaProductRepository.deleteAll();
    }

    // TODO CMZ à mettre dans PluginService
    protected PluginConfiguration getPluginConfiguration(String pluginId, Class<?> interfacePluginType)
            throws ModuleException {

        // Test if a configuration exists for this pluginId
        List<PluginConfiguration> pluginConfigurations = pluginService
                .getPluginConfigurationsByType(interfacePluginType);

        if (!pluginConfigurations.isEmpty()) {
            PluginConfiguration plgConf = loadPluginConfiguration(pluginId, pluginConfigurations);
            if (plgConf != null) {
                return plgConf;
            }
        }

        // Get the PluginMetadata
        List<PluginMetaData> metaDatas = pluginService.getPluginsByType(interfacePluginType);

        PluginConfiguration pluginConfiguration = new PluginConfiguration(metaDatas.get(0),
                "Automatic plugin configuration for plugin id : " + pluginId);
        pluginConfiguration.setPluginId(pluginId);

        List<PluginParameter> plgParams = new ArrayList<>();
        for (PluginParameterType param : metaDatas.get(0).getParameters()) {
            PluginParameter plgParam = new PluginParameter(param.getName(), param.getDefaultValue());
            plgParam.setName(param.getName());
            plgParam.setValue(param.getDefaultValue());
            plgParam.setIsDynamic(param.isOptional());
            plgParams.add(plgParam);
        }
        pluginConfiguration.setParameters(plgParams);

        return pluginService.savePluginConfiguration(pluginConfiguration);
    }

    /**
     * Return a {@link PluginConfiguration} for a pluginId
     *  
     * @param pluginId the pluginid to search
     * 
     * @return the found {@link PluginConfiguration}
     */
    private PluginConfiguration loadPluginConfiguration(String pluginId, List<PluginConfiguration> pluginConfs) {
        PluginConfiguration foundPlgConf = null;
        boolean exist = false;

        for (PluginConfiguration aPluginConf : pluginConfs) {
            if (!exist) {
                exist = aPluginConf.getPluginId().equals(pluginId);
                if (exist) {
                    foundPlgConf = aPluginConf;
                }
            }
        }

        return foundPlgConf;
    }

}
