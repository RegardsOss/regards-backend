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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.acquisition.builder.AcquisitionFileBuilder;
import fr.cnes.regards.modules.acquisition.builder.AcquisitionProcessingChainBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaFileBuilder;
import fr.cnes.regards.modules.acquisition.builder.MetaProductBuilder;
import fr.cnes.regards.modules.acquisition.builder.ProductBuilder;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaFileRepository;
import fr.cnes.regards.modules.acquisition.dao.IMetaProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IExecAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.dao.IProductRepository;
import fr.cnes.regards.modules.acquisition.dao.IScanDirectoryRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFileState;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain2;
import fr.cnes.regards.modules.acquisition.domain.FileAcquisitionInformations;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.domain.metadata.ScanDirectory;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionFileService;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingChainService2;
import fr.cnes.regards.modules.acquisition.service.IMetaFileService;
import fr.cnes.regards.modules.acquisition.service.IMetaProductService;
import fr.cnes.regards.modules.acquisition.service.IExecAcquisitionProcessingChainService;
import fr.cnes.regards.modules.acquisition.service.IProductService;
import fr.cnes.regards.modules.acquisition.service.IScanDirectoryService;
import fr.cnes.regards.modules.entities.client.IDatasetClient;
import fr.cnes.regards.modules.entities.domain.Dataset;
import fr.cnes.regards.modules.ingest.client.IIngestClient;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.dto.SIPDto;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * @author Christophe Mertz
 *
 */
public class AcquisitionITHelper extends AbstractRegardsIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionITHelper.class);

    protected static final String FIRST_PRODUCT = "PAUB_MESURE_TC_20130701_103715";

    protected static final String SECOND_PRODUCT = "PAUB_MESURE_TC_20130701_105909";

    @Value("${regards.tenant}")
    protected String tenant;

    protected static final String CHAIN_LABEL = "the chain label";

    protected static final String DATASET_IP_ID = "URN:DATASET:the dataset internal identifier";

    protected static final String META_PRODUCT_NAME = "the meta product name";

    protected static final String DEFAULT_USER = "John Doe";

    protected static final long WAIT_TIME = 5_000;

    protected static final String DEFAULT_TENANT = "PROJECT";

    protected static final String DEFAULT_ROLE = "ROLE_DEFAULT";

    @Autowired
    protected IAcquisitionProcessingChainService2 acqProcessChainService;

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
    protected IExecAcquisitionProcessingChainService execProcessingChainService;

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
    private IIngestClient ingestClient;

    @Autowired
    private IDatasetClient datasetClient;

    @Autowired
    protected ISubscriber subscriber;

    protected AcquisitionProcessingChain2 chain;

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
        final UniformResourceName aipUrn = new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET, "SSALTO",
                UUID.randomUUID(), 1);
        dataSet.setIpId(aipUrn);
        dataSet.setSipId("dataset-hello-CSSI");

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

    public void initData() throws ModuleException {
        // Create 2 ScanDirectory
        ScanDirectory scanDir1 = scandirService.save(new ScanDirectory("/var/regards/data/input1"));
        ScanDirectory scanDir2 = scandirService.save(new ScanDirectory("/var/regards/data/input2"));
        ScanDirectory scanDir3 = scandirService.save(new ScanDirectory("/var/regards/data/input3"));

        // Create 2 MetaFile
        metaFileOptional = metaFileRepository
                .save(MetaFileBuilder.build().withInvalidFolder("/var/regards/data/invalid")
                        .withMediaType(MediaType.APPLICATION_JSON_UTF8_VALUE).withFilePattern("file pattern optional")
                        .comment("it is optional").addScanDirectory(scanDir1).addScanDirectory(scanDir2).get());
        metaFileMandatory = metaFileRepository.save(MetaFileBuilder.build()
                .withInvalidFolder("/var/regards/data/invalid").withMediaType(MediaType.APPLICATION_PDF_VALUE)
                .withFilePattern("one other file pattern mandatory").comment("it is mandatory").isMandatory()
                .addScanDirectory(scanDir3).get());

        // Create a AcquisitionProcessingChain and a MetaProduct
        metaProduct = metaProductRepository
                .save(MetaProductBuilder.build(META_PRODUCT_NAME).addMetaFile(metaFileOptional)
                        .addMetaFile(metaFileMandatory).withIngestProcessingChain("ingest-processing-chain-id").get());
        chain = processingChainRepository
                .save(AcquisitionProcessingChainBuilder.build(CHAIN_LABEL + "-" + this.name.getMethodName()).isActive()
                        .withDataSet(DATASET_IP_ID).withMetaProduct(metaProduct).periodicity(1L).get());
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

    /**
     * IngestClient's response is {@link HttpStatus#CREATED}
     */
    protected void mockIngestClientResponseOK(List<String> sipIdsCreated) {
        Collection<SIPDto> sips = new ArrayList<>();

        for (String sipId : sipIdsCreated) {
            SIPDto sipEntity = new SIPDto();
            sipEntity.setState(SIPState.CREATED);
            sipEntity.setIpId(sipId);
            sips.add(sipEntity);
        }

        Mockito.when(ingestClient.ingest(Mockito.any()))
                .thenReturn(new ResponseEntity<Collection<SIPDto>>(sips, HttpStatus.CREATED));
    }

    /**
     * IngestClient's response is {@link HttpStatus#UNAUTHORIZED}
     */
    protected void mockIngestClientResponseUnauthorized() {
        Collection<SIPDto> sips = new ArrayList<>();
        SIPDto sipEntity = new SIPDto();
        sipEntity.setState(SIPState.REJECTED);
        sips.add(sipEntity);

        Mockito.when(ingestClient.ingest(Mockito.any()))
                .thenReturn(new ResponseEntity<Collection<SIPDto>>(sips, HttpStatus.UNAUTHORIZED));
    }

    /**
     * IngestClient's response is {@link HttpStatus#PARTIAL_CONTENT}. The {@link SIP} id parameters are rejected.
     * @param sipIds {@link SIP} id that are rejected
     */
    protected void mockIngestClientResponsePartialContent(List<String> sipIdsCreated, List<String> sipIdsError) {
        Collection<SIPDto> sips = new ArrayList<>();

        for (String sipId : sipIdsCreated) {
            SIPDto sipEntity = new SIPDto();
            sipEntity.setState(SIPState.CREATED);
            sipEntity.setIpId(sipId);
            sips.add(sipEntity);
        }

        for (String sipId : sipIdsError) {
            SIPDto sipEntity = new SIPDto();
            sipEntity.setRejectionCauses(Arrays.asList("bad SIP format"));
            sipEntity.setState(SIPState.REJECTED);
            sipEntity.setIpId(sipId);
            sips.add(sipEntity);
        }

        Mockito.when(ingestClient.ingest(Mockito.any()))
                .thenReturn(new ResponseEntity<Collection<SIPDto>>(sips, HttpStatus.PARTIAL_CONTENT));
    }

    protected void waitTimer(long millSecs) {
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
            LOGGER.debug("wait end jobs - r={}-s={}-a={}-f={} - {}", runnings.size(), succeededs.size(),
                         aborteds.size(), faileds.size(), name.getMethodName());
        }
    }

    /**
     * Create a {@link Product} and  persists it
     * @param productName
     * @param session
     * @param metaProduct
     * @param sended
     * @param status
     * @param fileNames
     * @return the {@link Product} created
     */
    protected Product createProduct(String productName, String session, MetaProduct metaProduct, boolean sended,
            ProductState status, String... fileNames) {
        Product product = ProductBuilder.build(productName).withStatus(status).withMetaProduct(metaProduct)
                .isSended(sended).withSession(session).withIngestProcessingChain(metaProduct.getIngestChain()).get();

        for (String acqf : fileNames) {
            product.addAcquisitionFile(acquisitionFileService.save(AcquisitionFileBuilder.build(acqf)
                    .withStatus(AcquisitionFileState.VALID.toString()).withMetaFile(metaFileMandatory).get()));
        }

        product.setSip(createSIP(productName));

        return productService.save(product);
    }

    /**
     * Create a {@link Product} and  persists it
     * @param productName
     * @param session
     * @param metaProduct
     * @param sended
     * @param status
     * @param fileName
     * @param fileAcqInf
     * @return the {@link Product} created
     */
    protected Product createProduct(String productName, String session, MetaProduct metaProduct, boolean sended,
            ProductState status, String fileName, FileAcquisitionInformations fileAcqInf) {
        Product product = ProductBuilder.build(productName).withStatus(status).withMetaProduct(metaProduct)
                .isSended(sended).withSession(session).withIngestProcessingChain(metaProduct.getIngestChain()).get();

        product.addAcquisitionFile(acquisitionFileService
                .save(AcquisitionFileBuilder.build(fileName).withStatus(AcquisitionFileState.VALID.toString())
                        .withMetaFile(metaFileMandatory).withFileAcquisitionInformations(fileAcqInf).get()));

        product.setSip(createSIP(productName));

        return productService.save(product);
    }

    /**
     * Create a {@link SIP}
     * @param productName
     * @return the {@link SIP} created
     */
    protected SIP createSIP(String productName) {
        SIPBuilder sipBuilder = new SIPBuilder(productName);
        sipBuilder.getPDIBuilder().addContextInformation("attribut-name",
                                                         productName + "-" + LocalDateTime.now().toString());
        return sipBuilder.build();
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
