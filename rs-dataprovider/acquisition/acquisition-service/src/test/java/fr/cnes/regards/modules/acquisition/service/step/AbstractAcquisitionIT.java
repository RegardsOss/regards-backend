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

package fr.cnes.regards.modules.acquisition.service.step;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.configuration.IRabbitVirtualHostAdmin;
import fr.cnes.regards.framework.amqp.configuration.RegardsAmqpAdmin;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginParameterRepository;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.modules.acquisition.builder.AcquisitionFileBuilder;
import fr.cnes.regards.modules.acquisition.builder.ChainGenerationBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaFileBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.builder.ProductBuilder;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IChainGenerationRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileStatus;
import fr.cnes.regards.modules.acquisition.domain.ChainGeneration;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductStatus;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectoryBuilder;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.IChainGenerationService;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;
import fr.cnes.regards.modules.acquisition.service.IMetaProductService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.IScanDirectoryService;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.ingest.client.IIngestClient;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * @author Christophe Mertz
 *
 */
public abstract class AbstractAcquisitionIT extends AbstractRegardsServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAcquisitionIT.class);

    protected static final String FIRST_PRODUCT = "PAUB_MESURE_TC_20130701_103715";

    protected static final String SECOND_PRODUCT = "PAUB_MESURE_TC_20130701_105909";

    @Value("${regards.tenant}")
    protected String tenant;

    protected static final String CHAINE_LABEL = "the chain label";

    protected static final String DATASET_IP_ID = "URN:DATASET:the dataset internal identifier";

    protected static final String META_PRODUCT_NAME = "the meta product name";

    protected static final String DEFAULT_USER = "John Doe";

    protected static final long WAIT_TIME = 5_000;

    protected static final String DEFAULT_TENANT = "PROJECT";

    protected static final String DEFAULT_ROLE = "ROLE_DEFAULT";

    @Autowired
    protected IChainGenerationService chainService;

    @Autowired
    protected IMetaProductService metaProductService;

    @Autowired
    protected IJobInfoRepository jobInfoRepository;

    @Autowired
    protected IMetaFileService metaFileService;

    @Autowired
    protected IScanDirectoryService scandirService;

    @Autowired
    protected IAcquisitionFileService acquisitionFileService;

    @Autowired
    protected IProductService productService;

    @Autowired
    protected IPluginService pluginService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IAuthenticationResolver authenticationResolver;

    @Autowired
    private IRabbitVirtualHostAdmin rabbitVhostAdmin;

    @Autowired
    private RegardsAmqpAdmin amqpAdmin;

    @Autowired
    protected IMetaProductRepository metaProductRepository;

    @Autowired
    protected IProductRepository productRepository;

    @Autowired
    protected IScanDirectoryRepository scanDirectoryRepository;

    @Autowired
    protected IChainGenerationRepository chainGenerationRepository;

    @Autowired
    protected IAcquisitionFileRepository acquisitionFileRepository;

    @Autowired
    protected IMetaFileRepository metaFileRepository;

    @Autowired
    protected IPluginParameterRepository pluginParameterRepository;

    @Autowired
    protected IPluginConfigurationRepository pluginConfigurationRepository;

    @Autowired
    private IIngestClient ingestClient;

    @Autowired
    private IDatasetClient datasetClient;

    @Autowired
    protected ISubscriber subscriber;

    protected ChainGeneration chain;

    protected MetaFile metaFileOptional;

    protected MetaFile metaFileMandatory;

    protected MetaProduct metaProduct;

    protected Set<UUID> runnings;

    protected Set<UUID> succeededs;

    protected Set<UUID> aborteds;

    protected Set<UUID> faileds;

    @Rule
    public TestName name = new TestName();

    @Before
    public void setUp() throws Exception {
        LOGGER.info("Start the test : {}", name.getMethodName());
        tenantResolver.forceTenant(tenant);

        cleanDb();

        initAmqp();

        initData();

        Mockito.when(authenticationResolver.getUser()).thenReturn(DEFAULT_USER);

        Dataset dataSet = new Dataset();
        dataSet.setLabel("dataset-hello-CSSI");
        Mockito.when(datasetClient.retrieveDataset(Mockito.anyString()))
                .thenReturn(new ResponseEntity<>(new Resource<Dataset>(dataSet), HttpStatus.OK));
    }

    public void initAmqp() {
        Assume.assumeTrue(rabbitVhostAdmin.brokerRunning());
        rabbitVhostAdmin.bind(tenantResolver.getTenant());
        rabbitVhostAdmin.unbind();

        subscriber.subscribeTo(JobEvent.class, new ScanJobHandler());

        runnings = Collections.synchronizedSet(new HashSet<>());
        succeededs = Collections.synchronizedSet(new HashSet<>());
        aborteds = Collections.synchronizedSet(new HashSet<>());
        faileds = Collections.synchronizedSet(new HashSet<>());
    }

    public void initData() {
        // Create 2 ScanDirectory
        ScanDirectory scanDir1 = scandirService.save(ScanDirectoryBuilder.build("/var/regards/data/input1").get());
        ScanDirectory scanDir2 = scandirService.save(ScanDirectoryBuilder.build("/var/regards/data/input2").get());
        ScanDirectory scanDir3 = scandirService.save(ScanDirectoryBuilder.build("/var/regards/data/input3").get());

        metaFileOptional = metaFileService.save(MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withFileType(MediaType.APPLICATION_JSON_VALUE).withFilePattern("file pattern optional")
                .comment("it is optional").addScanDirectory(scanDir1).addScanDirectory(scanDir2).get());
        metaFileMandatory = metaFileService.save(MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                .withFileType(MediaType.APPLICATION_JSON_VALUE).withFilePattern("one other file pattern mandatory")
                .comment("it is mandatory").isMandatory().addScanDirectory(scanDir3).get());

        // Create a ChainGeneration and a MetaProduct
        metaProduct = metaProductService.save(MetaProductBuilder.build(META_PRODUCT_NAME).addMetaFile(metaFileOptional)
                .addMetaFile(metaFileMandatory).withIngestProcessingChain("ingest-processing-chain-id").get());
        chain = chainService.save(ChainGenerationBuilder.build(CHAINE_LABEL + "-" + this.name.getMethodName())
                .isActive().withDataSet(DATASET_IP_ID).withMetaProduct(metaProduct).periodicity(1L).get());
    }

    private void purgeAMQPqueues() {
        rabbitVhostAdmin.bind(DEFAULT_TENANT);
        try {
            amqpAdmin.purgeQueue(JobEvent.class, ScanJobHandler.class, true);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        rabbitVhostAdmin.unbind();
    }

    @After
    public void cleanDb() {
        purgeAMQPqueues();
        subscriber.unsubscribeFrom(JobEvent.class);

        scanDirectoryRepository.deleteAll();
        productRepository.deleteAll();
        acquisitionFileRepository.deleteAll();
        chainGenerationRepository.deleteAll();
        metaProductRepository.deleteAll();
        metaFileRepository.deleteAll();

        pluginParameterRepository.deleteAll();
        pluginConfigurationRepository.deleteAll();

        jobInfoRepository.deleteAll();

        LOGGER.info("Clean the DB : {}", name.getMethodName());
    }

    protected void mockIngestClientResponseOK() {
        Collection<SIPEntity> sips = new ArrayList<>();
        SIPEntity sipEntity = new SIPEntity();
        sipEntity.setState(SIPState.CREATED);
        sips.add(sipEntity);

        Mockito.when(ingestClient.ingest(Mockito.any()))
                .thenReturn(new ResponseEntity<Collection<SIPEntity>>(sips, HttpStatus.CREATED));
    }

    protected void mockIngestClientResponseUnauthorized() {
        Collection<SIPEntity> sips = new ArrayList<>();
        SIPEntity sipEntity = new SIPEntity();
        sipEntity.setState(SIPState.REJECTED);
        sips.add(sipEntity);

        Mockito.when(ingestClient.ingest(Mockito.any()))
                .thenReturn(new ResponseEntity<Collection<SIPEntity>>(sips, HttpStatus.UNAUTHORIZED));
    }

    protected void mockIngestClientResponsePartialContent(String... sipIds) {
        Collection<SIPEntity> sips = new ArrayList<>();

        for (String sipId : sipIds) {
            SIPEntity sipEntity = new SIPEntity();
            sipEntity.setReasonForRejection("bad SIP format");
            sipEntity.setState(SIPState.REJECTED);
            sipEntity.setSipId(sipId);
            sips.add(sipEntity);
        }

        Mockito.when(ingestClient.ingest(Mockito.any()))
                .thenReturn(new ResponseEntity<Collection<SIPEntity>>(sips, HttpStatus.PARTIAL_CONTENT));
    }

    protected void waitJob(long millSecs) {
        try {
            Thread.sleep(millSecs);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
            Assert.fail();
        }
    }

    protected void waitJobEvent() throws InterruptedException {
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
            LOGGER.debug("wait end jobs - r={}-s={}-a={}-f={} - {}", runnings.size(), succeededs.size(), aborteds.size(),
                        faileds.size(), name.getMethodName());
        }
    }

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

    protected Product createProduct(String productName, String session, MetaProduct metaProduct, boolean sended,
            ProductStatus status, String... fileNames) {
        Product product = ProductBuilder.build(productName).withStatus(status).withMetaProduct(metaProduct)
                .isSended(sended).withSession(session).withIngestProcessingChain(metaProduct.getIngestChain()).get();

        for (String acqf : fileNames) {
            product.addAcquisitionFile(acquisitionFileService.save(AcquisitionFileBuilder.build(acqf)
                    .withStatus(AcquisitionFileStatus.VALID.toString()).withMetaFile(metaFileMandatory).get()));
        }

        product.setSip(createSIP(productName));

        return productService.save(product);
    }

    protected SIP createSIP(String productName) {
        SIPBuilder sipBuilder = new SIPBuilder(productName);
        sipBuilder.getPDIBuilder().addContextInformation("attribut-name",
                                                         productName + "-" + LocalDateTime.now().toString());
        return sipBuilder.build();
    }

}
