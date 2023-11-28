/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.OAISDataObjectLocation;
import fr.cnes.regards.framework.oais.RepresentationInformation;
import fr.cnes.regards.framework.oais.Syntax;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.*;
import fr.cnes.regards.modules.acquisition.exception.SIPGenerationException;
import fr.cnes.regards.modules.acquisition.service.job.AcquisitionJobPriority;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;
import fr.cnes.regards.modules.acquisition.service.job.SIPGenerationJob;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultFileValidation;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultProductPlugin;
import fr.cnes.regards.modules.acquisition.service.plugins.DefaultSIPGeneration;
import fr.cnes.regards.modules.acquisition.service.plugins.GlobDiskScanning;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Test {@link AcquisitionProcessingService} for {@link AcquisitionProcessingChain} workflow
 *
 * @author Marc Sordi
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acquisition_chains" })
public class AcquisitionProcessingServiceIT extends AbstractMultitenantServiceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionProcessingServiceIT.class);

    private static final String ACQUISITION_PROCESSSING_CHAIN_LABEL = "Processing chain 1";

    @Autowired
    private IAcquisitionProcessingService acquisitionProcessingService;

    @Autowired
    private ProductService productService;

    @Autowired
    private Validator validator;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Before
    public void initialize() throws ModuleException {
        acquisitionProcessingService.getFullChains(PageRequest.of(0, 100)).getContent().forEach(c -> {
            try {
                c.setActive(false);
                acquisitionProcessingService.updateChain(c);
                acquisitionProcessingService.deleteChain(c.getId());
            } catch (ModuleException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    @Requirement("REGARDS_DSL_ING_PRO_020")
    @Requirement("REGARDS_DSL_ING_PRO_030")
    @Purpose("Create an acquisition chain")
    public void createChain() throws ModuleException {
        // Given : save processing chain
        acquisitionProcessingService.createChain(createAcquisitionProcessingChain());

        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // When : test loading chain by mode
        List<AcquisitionProcessingChain> automaticChains = acquisitionProcessingService.findAllBootableAutomaticChains();

        List<AcquisitionProcessingChain> manualChains = acquisitionProcessingService.findByModeAndActiveTrueAndLockedFalse(
            AcquisitionProcessingChainMode.MANUAL);
        // Then
        Assert.assertTrue(automaticChains.isEmpty());
        Assert.assertTrue(!manualChains.isEmpty() && (manualChains.size() == 1));
    }

    @Test
    public void deleteProducts() throws ModuleException {
        // Given
        AcquisitionProcessingChain chain = acquisitionProcessingService.createChain(createAcquisitionProcessingChain());
        // When : add a product
        createProduct(chain);
        // Then
        Assert.assertTrue("There should be product associated to the chain", productService.countByChain(chain) > 0);

        // When
        productService.deleteByProcessingChain(chain);
        // Then
        Assert.assertFalse("There should not be any product associated to the chain",
                           productService.countByChain(chain) > 0);
    }

    @Test
    public void deleteProductsWithSession() throws ModuleException {
        // Given
        AcquisitionProcessingChain chain = acquisitionProcessingService.createChain(createAcquisitionProcessingChain());
        // When : add a product
        createProduct(chain);
        // Then
        Assert.assertTrue("There should be product associated to the chain", productService.countByChain(chain) > 0);

        // When
        productService.deleteBySession(chain, "plop");
        // Then
        Assert.assertTrue("There should be product associated to the chain", productService.countByChain(chain) > 0);

        // When
        productService.deleteBySession(chain, "session");
        // Then
        Assert.assertFalse("There should not be any product associated to the chain",
                           productService.countByChain(chain) > 0);
    }

    @Test
    @Purpose(
        "The product has to be stored by reference, thus test if the store path is saved in the content information of the product")
    public void createProductsByReference() throws ModuleException {
        // Given
        String refStorageIf = "reference-loc";
        // create chain
        AcquisitionProcessingChain chain = createAcquisitionProcessingChain();
        // set products to be stored by reference and remove one storage from chain (only one storage is allowed in this case)
        chain.setProductsStored(false);
        chain.setReferenceLocation(refStorageIf);
        chain = acquisitionProcessingService.createChain(chain);

        // When : create product
        Product product = createProduct(chain);
        // Then : test result
        Product productCreated = productService.retrieve(product.getProductName());
        Assert.assertEquals("Location should be equal to the pluginId provided in acquisition storage",
                            refStorageIf,
                            productCreated.getSip()
                                          .getProperties()
                                          .getContentInformations()
                                          .get(0)
                                          .getDataObject()
                                          .getLocations()
                                          .iterator()
                                          .next()
                                          .getStorage());
    }

    @Test
    public void test_acquisition_processing_chain_in_running_but_inactive_job() throws ModuleException {
        // Given
        AcquisitionProcessingChain acquisitionProcessingChain = createAcquisitionProcessingChain();

        Product product = createProduct(acquisitionProcessingService.createChain(acquisitionProcessingChain));

        OffsetDateTime inactiveTime_heartbeat = OffsetDateTime.now().minus(5, ChronoUnit.MINUTES);

        JobInfo productAcquisitionJob = new JobInfo();
        productAcquisitionJob.setPriority(AcquisitionJobPriority.PRODUCT_ACQUISITION_JOB_PRIORITY);
        productAcquisitionJob.setParameters(new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER_ID,
                                                             acquisitionProcessingChain.getId()),
                                            new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER_SESSION,
                                                             "my test session"));
        productAcquisitionJob.setClassName(ProductAcquisitionJob.class.getName());
        productAcquisitionJob.setOwner("user 1");
        productAcquisitionJob.getStatus().setStatus(JobStatus.RUNNING);
        productAcquisitionJob.setLastHeartbeatDate(inactiveTime_heartbeat);
        jobInfoRepository.save(productAcquisitionJob);

        acquisitionProcessingChain.setLastProductAcquisitionJobInfo(productAcquisitionJob);
        acquisitionProcessingService.updateChain(acquisitionProcessingChain);

        final JobInfo sipGenerationJob = new JobInfo();
        sipGenerationJob.setPriority(AcquisitionJobPriority.SIP_GENERATION_JOB_PRIORITY);
        sipGenerationJob.setParameters(new JobParameter(SIPGenerationJob.CHAIN_PARAMETER_ID,
                                                        acquisitionProcessingChain.getId()),
                                       new JobParameter(SIPGenerationJob.PRODUCT_NAMES,
                                                        Set.of(product.getProductName())));
        sipGenerationJob.setClassName(SIPGenerationJob.class.getName());
        sipGenerationJob.setOwner("user 1");
        sipGenerationJob.getStatus().setStatus(JobStatus.RUNNING);
        sipGenerationJob.setLastHeartbeatDate(inactiveTime_heartbeat);
        jobInfoRepository.save(sipGenerationJob);

        checkAcquisitionProcessingChainActive(true);

        // When
        jobInfoService.cleanDeadJobs();

        // Then
        checkAcquisitionProcessingChainActive(false);
    }

    private void checkAcquisitionProcessingChainActive(boolean active) {
        Page<AcquisitionProcessingChainMonitor> acquisitionProcessingChainMonitorPage = acquisitionProcessingService.buildAcquisitionProcessingChainSummaries(
            ACQUISITION_PROCESSSING_CHAIN_LABEL,
            Boolean.TRUE,
            AcquisitionProcessingChainMode.MANUAL,
            PageRequest.of(0, 10));

        Assert.assertEquals(1, acquisitionProcessingChainMonitorPage.getContent().size());
        if (active) {
            Assert.assertTrue(acquisitionProcessingChainMonitorPage.getContent().get(0).isActive());
        } else {
            Assert.assertFalse(acquisitionProcessingChainMonitorPage.getContent().get(0).isActive());
        }

    }

    private AcquisitionProcessingChain createAcquisitionProcessingChain() {
        // Create a acquisition processing chain
        AcquisitionProcessingChain processingChain = new AcquisitionProcessingChain();
        processingChain.setLabel(ACQUISITION_PROCESSSING_CHAIN_LABEL);
        processingChain.setActive(Boolean.TRUE);
        processingChain.setMode(AcquisitionProcessingChainMode.MANUAL);
        processingChain.setIngestChain("DefaultIngestChain");
        processingChain.setPeriodicity("0 * * * * *");
        processingChain.setCategories(Sets.newLinkedHashSet());

        // Create an acquisition file info
        AcquisitionFileInfo fileInfo = new AcquisitionFileInfo();
        fileInfo.setMandatory(Boolean.TRUE);
        fileInfo.setComment("A comment");
        fileInfo.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        fileInfo.setDataType(DataType.RAWDATA);
        fileInfo.setScanDirInfo(Sets.newHashSet(new ScanDirectoryInfo(Paths.get("src/resources/doesnotexist"), null)));

        PluginConfiguration scanPlugin = PluginConfiguration.build(GlobDiskScanning.class, null, null);
        scanPlugin.setIsActive(true);
        scanPlugin.setLabel("Scan plugin");
        fileInfo.setScanPlugin(scanPlugin);

        processingChain.addFileInfo(fileInfo);

        // Validation
        PluginConfiguration validationPlugin = PluginConfiguration.build(DefaultFileValidation.class,
                                                                         null,
                                                                         new HashSet<IPluginParam>());
        validationPlugin.setIsActive(true);
        validationPlugin.setLabel("Validation plugin");
        processingChain.setValidationPluginConf(validationPlugin);

        // Product
        PluginConfiguration productPlugin = PluginConfiguration.build(DefaultProductPlugin.class,
                                                                      null,
                                                                      new HashSet<IPluginParam>());
        productPlugin.setIsActive(true);
        productPlugin.setLabel("Product plugin");
        processingChain.setProductPluginConf(productPlugin);

        // SIP generation
        PluginConfiguration sipGenPlugin = PluginConfiguration.build(DefaultSIPGeneration.class,
                                                                     null,
                                                                     new HashSet<IPluginParam>());
        sipGenPlugin.setIsActive(true);
        sipGenPlugin.setLabel("SIP generation plugin");
        processingChain.setGenerateSipPluginConf(sipGenPlugin);

        // SIP post processing
        // Not required
        List<StorageMetadataProvider> storages = new ArrayList<>();
        storages.add(StorageMetadataProvider.build("AWS", "/path/to/file", new HashSet<>()));
        storages.add(StorageMetadataProvider.build("HELLO", "/other/path/to/file", new HashSet<>()));
        processingChain.setStorages(storages);

        // Validate
        Errors errors = new MapBindingResult(new HashMap<>(), "apc");
        validator.validate(processingChain, errors);
        if (errors.hasErrors()) {
            errors.getAllErrors().forEach(error -> LOGGER.error(error.getDefaultMessage()));
            Assert.fail("Acquisition processing chain should be valid");
        }
        return processingChain;
    }

    public Product createProduct(AcquisitionProcessingChain chain) throws SIPGenerationException {
        // HANDLE PRODUCT
        Product product = new Product();
        product.setIpId("productIpId");
        product.setProcessingChain(chain);
        product.setProductName("ProductName");
        product.setSession("session");
        product.setSip(SIP.build(EntityType.DATA, "providerId"));
        product.setSipState(SIPState.STORED);
        product.setState(ProductState.COMPLETED);

        // HANDLE SIP
        SIP sip = product.getSip();
        // add content information to the sip
        ContentInformation ci = new ContentInformation();
        RepresentationInformation ri = new RepresentationInformation();
        Syntax syntax = new Syntax();
        syntax.setMimeType(MediaType.APPLICATION_OCTET_STREAM);
        ri.setSyntax(syntax);
        ci.setRepresentationInformation(ri);
        ci.withDataObject(DataType.RAWDATA,
                          "filename",
                          "MD5",
                          UUID.randomUUID().toString(),
                          10L,
                          OAISDataObjectLocation.build("file://ARCHIVE1/NODE1/sample1.dat/"));
        sip.getProperties().getContentInformations().add(ci);

        // SUBMIT PRODUCT
        return productService.saveAndSubmitSIP(product, chain);
    }
}