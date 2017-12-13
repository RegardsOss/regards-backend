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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginParameterRepository;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.acquisition.builder.AcquisitionProcessingChainBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaFileBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IExecAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanDirectoryPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ICheckFilePlugin;
import fr.cnes.regards.modules.acquisition.plugins.IGenerateSIPPlugin;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.chain.conf.AcquisitionSsaltoProductsConfiguration;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.chain.conf.MockedFeignSsaltoClientConf;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingChainService;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.entities.domain.Dataset;

/**
 * @author Christophe Mertz
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AcquisitionSsaltoProductsConfiguration.class, MockedFeignSsaltoClientConf.class })
@ActiveProfiles({ "test", "disableDataProviderTask", "testAmqp" })
@DirtiesContext
public class AcquisitionProductsSsaltoIT {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionProductsSsaltoIT.class);

    private static final String DEFAULT_USER = "John Doe";

    private static final String BASE_DATA_DIR = "src/test/resources/income/data";

    private static final String DATASET_JASON2_IGDR = "DA_TC_JASON2_IGDR";

    private static final String DATASET_SPOT2_DORIS1B_MOE_CDDIS = "DA_TC_SPOT2_DORIS1B_MOE_CDDIS";

    @Autowired
    private IAcquisitionProcessingChainService acqProcessChainService;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAuthenticationResolver authenticationResolver;

    @Autowired
    private IRabbitVirtualHostAdmin rabbitVhostAdmin;

    @Autowired
    protected IMetaProductRepository metaProductRepository;

    @Autowired
    protected IProductRepository productRepository;

    @Autowired
    protected IScanDirectoryRepository scanDirectoryRepository;

    @Autowired
    protected IAcquisitionProcessingChainRepository processingChainRepository;

    @Autowired
    private IExecAcquisitionProcessingChainRepository execAcquisitionProcessingChainRepository;

    @Autowired
    protected IAcquisitionFileRepository acquisitionFileRepository;

    @Autowired
    protected IMetaFileRepository metaFileRepository;

    @Autowired
    protected IExecAcquisitionProcessingChainRepository execProcessingChainRepository;

    @Autowired
    protected IPluginParameterRepository pluginParameterRepository;

    @Autowired
    protected IPluginConfigurationRepository pluginConfigurationRepository;

    @Autowired
    protected IJobInfoRepository jobInfoRepository;

    @Autowired
    private IDatasetClient datasetClient;

    @Autowired
    protected ISubscriber subscriber;

    protected Set<UUID> runnings;

    protected Set<UUID> succeededs;

    protected Set<UUID> aborteds;

    protected Set<UUID> faileds;

    //    private AcquisitionProcessingChain chainJason2Igdr;
    //
    //    private AcquisitionProcessingChain chainSpot2Doris1b;

    @Rule
    public TestName name = new TestName();

    @Before
    public void setUp() throws Exception {
        cleanDb();

        initAmqp();

        initJobQueue();

        Mockito.when(authenticationResolver.getUser()).thenReturn(DEFAULT_USER);

    }

    @After
    public void cleanDb() {
        subscriber.purgeQueue(JobEvent.class, ScanJobHandler.class);
        subscriber.unsubscribeFrom(JobEvent.class);

        scanDirectoryRepository.deleteAll();
        productRepository.deleteAll();
        acquisitionFileRepository.deleteAll();
        execProcessingChainRepository.deleteAll();
        processingChainRepository.deleteAll();
        metaProductRepository.deleteAll();
        metaFileRepository.deleteAll();

        pluginParameterRepository.deleteAll();
        pluginConfigurationRepository.deleteAll();

        jobInfoRepository.deleteAll();

        LOGGER.info("Clean the DB : {}", name.getMethodName());
    }

    public void initJobQueue() {
        runnings = Collections.synchronizedSet(new HashSet<>());
        succeededs = Collections.synchronizedSet(new HashSet<>());
        aborteds = Collections.synchronizedSet(new HashSet<>());
        faileds = Collections.synchronizedSet(new HashSet<>());
    }

    public void initAmqp() {
        Assume.assumeTrue(rabbitVhostAdmin.brokerRunning());
        rabbitVhostAdmin.bind(tenantResolver.getTenant());
        rabbitVhostAdmin.unbind();

        subscriber.subscribeTo(JobEvent.class, new ScanJobHandler());
    }

    @Test
    public void runJason2IgdrProcessingChain() throws ModuleException, InterruptedException, IOException {

        AcquisitionProcessingChain chainJason2Igdr = initJason2IgdrProcessingChain();

        chainJason2Igdr.setScanAcquisitionPluginConf(pluginService
                .getPluginConfiguration("ScanDirectoryPlugin", IAcquisitionScanDirectoryPlugin.class));
        chainJason2Igdr.setCheckAcquisitionPluginConf(pluginService.getPluginConfiguration("BasicCheckFilePlugin",
                                                                                           ICheckFilePlugin.class));
        chainJason2Igdr.setGenerateSipPluginConf(pluginService.getPluginConfiguration("Jason2ProductMetadataPlugin",
                                                                                      IGenerateSIPPlugin.class));

        Assert.assertTrue(acqProcessChainService.run(chainJason2Igdr));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(3, acquisitionFileRepository.count());
        Assert.assertEquals(3, productRepository.count());
        Assert.assertEquals(1, execAcquisitionProcessingChainRepository.count());
        Assert.assertEquals(1, processingChainRepository.count());
        Assert.assertNotNull(productRepository.findAll().get(0).getSip());
    }

    private AcquisitionProcessingChain initJason2IgdrProcessingChain() throws ModuleException, IOException {
        Dataset dataSet = initJason2IGDRDataset();
        File file = new File(BASE_DATA_DIR + "/JASON2/IGDR");
        ScanDirectory scanDir = new ScanDirectory(file.getCanonicalPath());
        MetaFile metaFile = MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .withFilePattern("JA2_IP(N|S|R)_2P[a-zA-Z]{1}P[0-9]{3}_[0-9]{3,4}(_[0-9]{8}_[0-9]{6}){2}(.nc){0,1}")
                .addScanDirectory(scanDir).get();
        MetaProduct metaProduct = MetaProductBuilder.build("JASON2_IGDR").addMetaFile(metaFile)
                .withChecksumAlgorithm("MD5").withIngestProcessingChain("ingest-processing-chain-id").get();
        return acqProcessChainService.createOrUpdate(AcquisitionProcessingChainBuilder.build("JASON2_IGDR").isActive()
                .withDataSet(dataSet.getIpId().toString()).withMetaProduct(metaProduct).get());
    }

    private Dataset initJason2IGDRDataset() {
        Dataset dataSet = new Dataset();
        final UniformResourceName aipUrn = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET, "SSALTO",
                UUID.randomUUID(), 1);
        dataSet.setIpId(aipUrn);
        dataSet.setSipId(DATASET_JASON2_IGDR);
        Mockito.when(datasetClient.retrieveDataset(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(new Resource<Dataset>(dataSet), HttpStatus.OK));
        return dataSet;
    }

    @Test
    public void runSpot2Doris1bProcessingChain() throws ModuleException, InterruptedException, IOException {

        AcquisitionProcessingChain chainSpot2Doris1b = initSpot2Doris1bProcessingChain();

        chainSpot2Doris1b.setScanAcquisitionPluginConf(pluginService
                .getPluginConfiguration("ScanDirectoryPlugin", IAcquisitionScanDirectoryPlugin.class));
        chainSpot2Doris1b.setCheckAcquisitionPluginConf(pluginService.getPluginConfiguration("Spot2Doris1BCheckingPlugin",
                                                                                             ICheckFilePlugin.class));
        chainSpot2Doris1b.setGenerateSipPluginConf(pluginService
                .getPluginConfiguration("Spot2ProductMetadataPlugin", IGenerateSIPPlugin.class));

        Assert.assertTrue(acqProcessChainService.run(chainSpot2Doris1b));

        waitJobEvent();

        Assert.assertFalse(runnings.isEmpty());
        Assert.assertFalse(succeededs.isEmpty());
        Assert.assertTrue(faileds.isEmpty());
        Assert.assertTrue(aborteds.isEmpty());

        Assert.assertEquals(1, acquisitionFileRepository.count());
        Assert.assertEquals(1, productRepository.count());
        Assert.assertEquals(1, execAcquisitionProcessingChainRepository.count());
        Assert.assertEquals(1, processingChainRepository.count());
        Assert.assertNotNull(productRepository.findAll().get(0).getSip());
        Assert.assertTrue(productRepository.findAll().get(0).getProductName().startsWith("MOE_CDDIS_"));
    }

    private AcquisitionProcessingChain initSpot2Doris1bProcessingChain() throws ModuleException, IOException {
        Dataset dataSet = initSpot2Doris1bDataset();
        File file = new File(BASE_DATA_DIR + "/spot2/doris1b_moe_cddis");
        ScanDirectory scanDir = new ScanDirectory(file.getCanonicalPath());
        MetaFile metaFile = MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE).withFilePattern("DORDATA_[0-9]{6}.SP2")
                .addScanDirectory(scanDir).get();
        MetaProduct metaProduct = MetaProductBuilder.build("JASON2_IGDR").addMetaFile(metaFile)
                .withChecksumAlgorithm("MD5").withIngestProcessingChain("ingest-processing-chain-id").get();
        return acqProcessChainService.createOrUpdate(AcquisitionProcessingChainBuilder.build("JASON2_IGDR").isActive()
                .withDataSet(dataSet.getIpId().toString()).withMetaProduct(metaProduct).get());
    }

    private Dataset initSpot2Doris1bDataset() {
        Dataset dataSet = new Dataset();
        final UniformResourceName aipUrn = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET, "SSALTO",
                UUID.randomUUID(), 1);
        dataSet.setIpId(aipUrn);
        dataSet.setSipId(DATASET_SPOT2_DORIS1B_MOE_CDDIS);
        Mockito.when(datasetClient.retrieveDataset(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(new Resource<Dataset>(dataSet), HttpStatus.OK));
        return dataSet;
    }

    private void waitJobEvent() throws InterruptedException {
        boolean wait = true;

        // wait running
        while (wait) {
            Thread.sleep(1_000);
            if (runnings.size() > 0) {
                wait = false;
            }
            LOGGER.debug("wait first job running - r={}-s={}-a={}-f={} - {}", runnings.size(), succeededs.size(),
                         aborteds.size(), faileds.size(), name.getMethodName());
        }

        // wait the end of the running job
        wait = true;
        while (wait) {
            Thread.sleep(1_000);
            int received = 0;
            for (UUID uid : runnings) {
                if (succeededs.contains(uid) || aborteds.contains(uid) || faileds.contains(uid)) {
                    received++;
                }
            }
            if (runnings.size() == received) {
                wait = false;
            }
            LOGGER.debug("wait end jobs - r={}-s={}-a={}-f={} - {}", runnings.size(), succeededs.size(),
                         aborteds.size(), faileds.size(), name.getMethodName());
        }
    }

    /**
     * This handler handle's {@link JobEvent} events
     * @author Christophe Mertz
     *
     */
    protected class ScanJobHandler implements IHandler<JobEvent> {

        @Override
        public void handle(TenantWrapper<JobEvent> wrapper) {
            JobEvent event = wrapper.getContent();
            JobEventType type = event.getJobEventType();

            LOGGER.info(this.toString());

            switch (type) {
                case RUNNING:
                    runnings.add(wrapper.getContent().getJobId());
                    LOGGER.info("RUNNING for {}", wrapper.getContent().getJobId());
                    break;
                case SUCCEEDED:
                    succeededs.add(wrapper.getContent().getJobId());
                    LOGGER.info("SUCCEEDED for {}", wrapper.getContent().getJobId());
                    break;
                case ABORTED:
                    aborteds.add(wrapper.getContent().getJobId());
                    LOGGER.info("ABORTED for {}", wrapper.getContent().getJobId());
                    break;
                case FAILED:
                    faileds.add(wrapper.getContent().getJobId());
                    LOGGER.info("FAILED for {}", wrapper.getContent().getJobId());
                    break;
                default:
                    throw new IllegalArgumentException(type + " is not an handled type of JobEvent ");
            }
        }
    }

}
