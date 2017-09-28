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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IChainGenerationRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
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
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
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
    private IAcquisitionFileService acquisitionFileService;

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
    private IScanDirectoryRepository scanDirectoryRepository;

    @Autowired
    private IChainGenerationRepository chainGenerationRepository;

    @Autowired
    private IAcquisitionFileRepository acquisitionFileRepository;

    @Autowired
    private IMetaFileRepository metaFileRepository;

    private ChainGeneration chain;

    private MetaFile metaFile;

    private MetaProduct metaProduct;

    private static Set<UUID> runnings = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> succeededs = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> aborteds = Collections.synchronizedSet(new HashSet<>());

    private static Set<UUID> faileds = Collections.synchronizedSet(new HashSet<>());

    @Autowired
    private ISubscriber subscriber;

    @Before
    public void setUp() throws Exception {
        cleanDb();

        Assume.assumeTrue(rabbitVhostAdmin.brokerRunning());
        rabbitVhostAdmin.addVhost(tenant);

        tenantResolver.forceTenant(tenant);
        Mockito.when(authenticationResolver.getUser()).thenReturn(DEFAULT_USER);

        subscriber.subscribeTo(JobEvent.class, new ScanJobHandler());

        runnings.clear();
        succeededs.clear();
        aborteds.clear();
        faileds.clear();
    }

    @Before
    public void init() {

        // Create a ChainGeneration and a MetaProduct
        this.metaProduct = metaProductService.save(MetaProductBuilder.build(META_PRODUCT_NAME).get());
        this.chain = chainService.save(ChainGenerationBuilder.build(CHAINE_LABEL).isActive().withDataSet(DATASET_NAME)
                .withMetaProduct(metaProduct).get());

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
        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFile);

        String metaFilesJson = new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles));
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));

        PluginConfiguration plgConf = pluginService.getPluginConfiguration("TestScanDirectoryPlugin",
                                                                           IAcquisitionScanDirectoryPlugin.class);
        chain.setScanAcquisitionPluginConf(plgConf.getId());
        chain.addScanAcquisitionParameter(META_PRODUCT_PARAM, metaProductJson);
        chain.addScanAcquisitionParameter(META_FILE_PARAM, metaFilesJson);

        Assert.assertTrue(chainService.run(chain));

        waitJob(5_000);

        Assert.assertTrue(!runnings.isEmpty());
        Assert.assertTrue(!succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(1, metaFileService.retrieveAll().size());
        Assert.assertEquals(2, acquisitionFileService.retrieveAll().size());

        chain = chainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }
    
    @Test
    public void runActiveChainGenerationAcquireSameFilesWithSameChecksum() throws ModuleException, InterruptedException {
        this.chain.setPeriodicity(1L);

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFile);

        String metaFilesJson = new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles));
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));

        PluginConfiguration plgConf = pluginService.getPluginConfiguration("TestScanDirectoryPlugin",
                                                                           IAcquisitionScanDirectoryPlugin.class);
        chain.setScanAcquisitionPluginConf(plgConf.getId());
        chain.addScanAcquisitionParameter(META_PRODUCT_PARAM, metaProductJson);
        chain.addScanAcquisitionParameter(META_FILE_PARAM, metaFilesJson);

        // Activate the chain
        Assert.assertTrue(chainService.run(chain));

        waitJob(5_000);

        Assert.assertTrue(!runnings.isEmpty());
        Assert.assertTrue(!succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        // Repeat the activation of the same chain
        Assert.assertTrue(chainService.run(chain));

        waitJob(2_000);

        Assert.assertTrue(!runnings.isEmpty());
        Assert.assertTrue(!succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(1, metaFileService.retrieveAll().size());
        Assert.assertEquals(2, acquisitionFileService.retrieveAll().size());

        chain = chainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }
    
    @Test
    public void runActiveChainGenerationAcquireSameFilesWithDifferentChecksum()
            throws ModuleException, InterruptedException {
        this.chain.setPeriodicity(1L);

        Set<MetaFile> metaFiles = new HashSet<>();
        metaFiles.add(metaFile);

        String metaFilesJson = new Gson().toJson(SetOfMetaFileDto.fromSetOfMetaFile(metaFiles));
        String metaProductJson = new Gson().toJson(MetaProductDto.fromMetaProduct(metaProduct));

        PluginConfiguration plgConf = pluginService.getPluginConfiguration("TestScanDirectoryPlugin",
                                                                           IAcquisitionScanDirectoryPlugin.class);
        chain.setScanAcquisitionPluginConf(plgConf.getId());
        chain.addScanAcquisitionParameter(META_PRODUCT_PARAM, metaProductJson);
        chain.addScanAcquisitionParameter(META_FILE_PARAM, metaFilesJson);

        // Activate the chain
        Assert.assertTrue(chainService.run(chain));

        waitJob(5_000);

        Assert.assertTrue(!runnings.isEmpty());
        Assert.assertTrue(!succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        // Repeat the activation of the same chain with an other Plugin
        plgConf = pluginService.getPluginConfiguration("TestScanDirectorySameFileCheckSumDifferentPlugin",
                                                       IAcquisitionScanDirectoryPlugin.class);
        chain.setScanAcquisitionPluginConf(plgConf.getId());
        Assert.assertTrue(chainService.run(chain));

        waitJob(2_000);

        Assert.assertTrue(!runnings.isEmpty());
        Assert.assertTrue(!succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, chainService.retrieveAll().size());
        Assert.assertEquals(1, metaFileService.retrieveAll().size());
        Assert.assertEquals(4, acquisitionFileService.retrieveAll().size());

        chain = chainService.retrieve(chain.getId());
        Assert.assertNotNull(chain.getLastDateActivation());
    }

    @Test
    public void runActiveChainGenerationWithoutScanPlugin() throws InterruptedException {
        Assert.assertTrue(chainService.run(chain));

        waitJob(1_000);

        Assert.assertTrue(!runnings.isEmpty());
        Assert.assertTrue(succeededs.isEmpty());
        Assert.assertTrue(!faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());
    }

    @Test
    public void runNoActiveChainGeneration() throws InterruptedException {
        this.chain.setActive(false);

        Assert.assertFalse(chainService.run(chain));

        waitJob(1_000);

        Assert.assertTrue(runnings.isEmpty());
        Assert.assertTrue(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());
    }

    @Test
    public void runChainGenerationPeriodicity() throws InterruptedException {
        this.chain.setActive(true);
        this.chain.setLastDateActivation(OffsetDateTime.now().minusHours(1));
        this.chain.setPeriodicity(3650L);

        Assert.assertFalse(chainService.run(chain));

        waitJob(1_000);

        Assert.assertTrue(runnings.isEmpty());
        Assert.assertTrue(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());
    }

    @After
    public void cleanDb() {
        jobInfoRepository.deleteAll();
        scanDirectoryRepository.deleteAll();
        productRepository.deleteAll();
        acquisitionFileRepository.deleteAll();
        chainGenerationRepository.deleteAll();
        metaProductRepository.deleteAll();
        metaFileRepository.deleteAll();
    }

    private void waitJob(long millSecs) {
        try {
            Thread.sleep(millSecs);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
            Assert.fail();
        }
    }

    private class ScanJobHandler implements IHandler<JobEvent> {

        @Override
        public void handle(TenantWrapper<JobEvent> wrapper) {
            JobEvent event = wrapper.getContent();
            JobEventType type = event.getJobEventType();
            switch (type) {
                case RUNNING:
                    runnings.add(wrapper.getContent().getJobId());
                    LOGGER.info("RUNNING for " + wrapper.getContent().getJobId());
                    break;
                case SUCCEEDED:
                    succeededs.add(wrapper.getContent().getJobId());
                    LOGGER.info("SUCCEEDED for " + wrapper.getContent().getJobId());
                    break;
                case ABORTED:
                    aborteds.add(wrapper.getContent().getJobId());
                    LOGGER.info("ABORTED for " + wrapper.getContent().getJobId());
                    break;
                case FAILED:
                    faileds.add(wrapper.getContent().getJobId());
                    LOGGER.info("FAILED for " + wrapper.getContent().getJobId());
                    break;
                default:
                    throw new IllegalArgumentException(type + " is not an handled type of JobEvent ");
            }
        }
    }

}
