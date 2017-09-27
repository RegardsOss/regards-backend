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
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IChainGenerationRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
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
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanPlugin;
import fr.cnes.regards.modules.acquisition.service.AcquisitionFileServiceIT;
import fr.cnes.regards.modules.acquisition.service.IChainGenerationService;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;
import fr.cnes.regards.modules.acquisition.service.IMetaProductService;
import fr.cnes.regards.modules.acquisition.service.IScanDirectoryService;

/**
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { ChainGenerationServiceConfiguration.class })
public class ScanJobIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionFileServiceIT.class);

    @Value("${regards.tenant}")
    private String tenant;

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
    private IScanDirectoryService scandirService;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAuthenticationResolver authenticationResolver;

    @Autowired
    private IRabbitVirtualHostAdmin rabbitVhostAdmin;

    @Autowired
    private IMetaProductRepository metaProductRepository;

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IMetaFileRepository metaFileRepository;

    @Autowired
    private IScanDirectoryRepository scanDirectoryRepository;

    @Autowired
    private IChainGenerationRepository chainGenerationRepository;

    @Autowired
    private IAcquisitionFileRepository acquisitionFileRepository;

    private ChainGeneration chain;

    private MetaFile metaFile;

    private MetaProduct metaProduct;

    @Before
    public void setUp() throws Exception {
        cleanDb();

        Assume.assumeTrue(rabbitVhostAdmin.brokerRunning());
        rabbitVhostAdmin.addVhost(tenant);

        tenantResolver.forceTenant(tenant);
        Mockito.when(authenticationResolver.getUser()).thenReturn(DEFAULT_USER);
    }

    @Before
    public void init() {

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
    public void runActiveChainGeneration() throws ModuleException, InterruptedException {
        LOGGER.info("start");

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFile);

        String metaFilesJson = new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles));
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));

        PluginConfiguration plgConf = pluginService.getPluginConfiguration("TestScanDirectoryPlugin",
                                                                           IAcquisitionScanDirectoryPlugin.class);
        chain.setScanAcquisitionPluginConf(plgConf.getId());
        chain.addScanAcquisitionParameter(META_PRODUCT_PARAM, metaProductJson);
        chain.addScanAcquisitionParameter(META_FILE_PARAM, metaFilesJson);

        boolean res = chainService.run(chain);
        Assert.assertTrue(res);

        // tester que le job s'exécute et qu'il fait ce qui est attendu

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
            Assert.fail();
        }
    }

    @Test
    public void runActiveChainGenerationWithoutJob() throws ModuleException, InterruptedException {
        LOGGER.info("start");

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFile);

        String metaFilesJson = new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles));
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));
        PluginParametersFactory factory = PluginParametersFactory.build()
                .addParameterDynamic(META_PRODUCT_PARAM, metaProductJson)
                .addParameterDynamic(META_FILE_PARAM, metaFilesJson);
        PluginConfiguration plgConf = pluginService.getPluginConfiguration("TestScanDirectoryPlugin",
                                                                           IAcquisitionScanDirectoryPlugin.class);

        IAcquisitionScanPlugin scanPlugin = pluginService
                .getPlugin(plgConf.getId(),
                           factory.getParameters().toArray(new PluginParameter[factory.getParameters().size()]));

        Set<AcquisitionFile> acquistionFiles = scanPlugin.getAcquisitionFiles();
        
        Assert.assertNotNull(acquistionFiles);
        Assert.assertEquals(2,acquistionFiles.size());
    }

    @Test
    public void runActiveChainGenerationWithoutScanPlugin() throws InterruptedException {
        boolean res = chainService.run(chain);
        Assert.assertTrue(res);

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
            Assert.fail();
        }
    }

    @Test
    public void runNoActiveChainGeneration() throws InterruptedException {
        this.chain.setActive(false);
        boolean res = chainService.run(chain);
        Assert.assertFalse(res);
    }

    @Test
    public void runChainGenerationPeriodicity() throws InterruptedException {
        this.chain.setActive(true);
        this.chain.setLastDateActivation(OffsetDateTime.now().minusHours(1));
        this.chain.setPeriodicity(3650L);
        boolean res = chainService.run(chain);
        Assert.assertFalse(res);
    }

    @After
    public void cleanDb() {
        jobInfoRepository.deleteAll();
        scanDirectoryRepository.deleteAll();
        productRepository.deleteAll();
        acquisitionFileRepository.deleteAll();
        metaProductRepository.deleteAll();
    }

}
