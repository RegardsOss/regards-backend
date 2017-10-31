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
package fr.cnes.regards.modules.ingest.service.chain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.SIPCollection;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPCollectionBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.service.IIngestService;
import fr.cnes.regards.modules.ingest.service.plugin.AIPGenerationTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.AIPTaggingTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultSingleAIPGeneration;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultSipValidation;
import fr.cnes.regards.modules.ingest.service.plugin.PreprocessingTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.ValidationTestPlugin;

/**
 * Test class to verify {@link IngestProcessingJob}.
 * @author Sébastien Binda
 */
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:test.properties")
@ContextConfiguration(classes = { IngestProcessingJobTest.IngestConfiguration.class })
public class IngestProcessingJobTest extends AbstractDaoTransactionalTest {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(IngestProcessingJobTest.class);

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.modules" })
    static class IngestConfiguration {
    }

    @Autowired
    private IIngestProcessingChainRepository processingChainRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IPluginConfigurationRepository pluginConfRepo;

    @Autowired
    private IIngestService ingestService;

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Autowired
    private ProcessingChainTestErrorSimulator stepErrorSimulator;

    private Long sipIdTest;

    private Long sipRefIdTest;

    private Long sipIdDefaultChainTest;

    public static final String SIP_ID_TEST = "SIP_001";

    public static final String SIP_DEFAULT_CHAIN_ID_TEST = "SIP_002";

    public static final String SIP_REF_ID_TEST = "SIP_003";

    public static final String DEFAULT_PROCESSING_CHAIN_TEST = "defaultProcessingChain";

    public static final String PROCESSING_CHAIN_TEST = "fullProcessingChain";

    @Before
    public void init() throws ModuleException {

        pluginConfRepo.deleteAll();
        sipRepository.deleteAll();

        initFullPRocessingChain();
        initDefaultProcessingChain();

        // Init a SIP in database with state CREATED and managed with default chain
        SIPCollectionBuilder colBuilder = new SIPCollectionBuilder(DEFAULT_PROCESSING_CHAIN_TEST, "sessionId");
        SIPCollection collection = colBuilder.build();
        SIPBuilder builder = new SIPBuilder(SIP_DEFAULT_CHAIN_ID_TEST);
        collection.add(builder.build());
        Collection<SIPEntity> results = ingestService.ingest(collection);
        sipIdDefaultChainTest = results.stream().findFirst().get().getId();

        // Init a SIP in database with state CREATED
        colBuilder = new SIPCollectionBuilder(PROCESSING_CHAIN_TEST, "sessionId");
        collection = colBuilder.build();
        builder = new SIPBuilder(SIP_ID_TEST);
        collection.add(builder.build());
        results = ingestService.ingest(collection);
        sipIdTest = results.stream().findFirst().get().getId();

        // Init a SIP with reference in database with state CREATED
        colBuilder = new SIPCollectionBuilder(PROCESSING_CHAIN_TEST, "sessionId");
        collection = colBuilder.build();
        builder = new SIPBuilder(SIP_REF_ID_TEST);
        collection.add(builder.buildReference(Paths.get("src/test/resources/file_ref.xml"),
                                              "1e2d4ab665784e43243b9b07724cd483"));
        results = ingestService.ingest(collection);
        sipRefIdTest = results.stream().findFirst().get().getId();
    }

    private void initFullPRocessingChain() throws ModuleException {
        PluginMetaData preProcessingPluginMeta = PluginUtils.createPluginMetaData(PreprocessingTestPlugin.class);
        PluginConfiguration preProcessingPlugin = new PluginConfiguration(preProcessingPluginMeta,
                "preProcessingPlugin");
        pluginService.savePluginConfiguration(preProcessingPlugin);

        PluginMetaData validationPluginMeta = PluginUtils.createPluginMetaData(ValidationTestPlugin.class);
        PluginConfiguration validationPlugin = new PluginConfiguration(validationPluginMeta, "validationPlugin");
        pluginService.savePluginConfiguration(validationPlugin);

        PluginMetaData generationPluginMeta = PluginUtils.createPluginMetaData(AIPGenerationTestPlugin.class);
        PluginConfiguration generationPlugin = new PluginConfiguration(generationPluginMeta, "generationPlugin");
        pluginService.savePluginConfiguration(generationPlugin);

        PluginMetaData taggingPluginMeta = PluginUtils.createPluginMetaData(AIPTaggingTestPlugin.class);
        PluginConfiguration taggingPlugin = new PluginConfiguration(taggingPluginMeta, "taggingPlugin");
        pluginService.savePluginConfiguration(taggingPlugin);

        IngestProcessingChain fullChain = new IngestProcessingChain(PROCESSING_CHAIN_TEST,
                "Full test Ingestion processing chain", validationPlugin, generationPlugin);
        fullChain.setPreProcessingPlugin(preProcessingPlugin);
        fullChain.setGenerationPlugin(generationPlugin);
        fullChain.setTagPlugin(taggingPlugin);
        processingChainRepository.save(fullChain);
    }

    private void initDefaultProcessingChain() throws ModuleException {
        PluginMetaData defaultValidationPluginMeta = PluginUtils.createPluginMetaData(DefaultSipValidation.class);
        PluginConfiguration defaultValidationPlugin = new PluginConfiguration(defaultValidationPluginMeta,
                "DefaultValidationPlugin");
        pluginService.savePluginConfiguration(defaultValidationPlugin);

        PluginMetaData defaultGenerationPluginMeta = PluginUtils.createPluginMetaData(DefaultSingleAIPGeneration.class);
        PluginConfiguration defaultGenerationPlugin = new PluginConfiguration(defaultGenerationPluginMeta,
                "DefaultGenerationPlugin");
        pluginService.savePluginConfiguration(defaultGenerationPlugin);

        IngestProcessingChain defaultChain = new IngestProcessingChain(DEFAULT_PROCESSING_CHAIN_TEST,
                "Default Ingestion processing chain", defaultValidationPlugin, defaultGenerationPlugin);
        processingChainRepository.save(defaultChain);
    }

    @Purpose("Test default process chain to ingest a new SIP provided by value")
    @Test
    public void testDefaultProcessingChain() {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(IngestProcessingJob.CHAIN_NAME_PARAMETER, DEFAULT_PROCESSING_CHAIN_TEST));
        parameters.add(new JobParameter(IngestProcessingJob.SIP_PARAMETER, sipIdDefaultChainTest));

        // Simulate a full process without error
        JobInfo toTest = new JobInfo(0, parameters, "owner", IngestProcessingJob.class.getName());
        runJob(toTest);
        // Assert that SIP is in AIP_CREATED state
        SIPEntity resultSip = sipRepository.findOne(sipIdDefaultChainTest);
        Assert.assertTrue("SIP should be the one generated in the test initialization.",
                          SIP_DEFAULT_CHAIN_ID_TEST.equals(resultSip.getSip().getId()));
        Assert.assertTrue("State of SIP should be AIP_CREATED After a successfull process",
                          SIPState.AIP_CREATED.equals(resultSip.getState()));
    }

    @Purpose("Test fully configured process chain to ingest a new SIP provided by value")
    @Test
    public void testProcessingChain() throws JobParameterMissingException, JobParameterInvalidException {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(IngestProcessingJob.CHAIN_NAME_PARAMETER, PROCESSING_CHAIN_TEST));
        parameters.add(new JobParameter(IngestProcessingJob.SIP_PARAMETER, sipIdTest));

        // Simulate a full process without error
        JobInfo toTest = new JobInfo(1, parameters, "owner", IngestProcessingJob.class.getName());
        runJob(toTest);
        // Assert that SIP is in AIP_CREATED state
        SIPEntity resultSip = sipRepository.findOne(sipIdTest);
        Assert.assertTrue("SIP should be the one generated in the test initialization.",
                          SIP_ID_TEST.equals(resultSip.getSip().getId()));
        Assert.assertTrue("State of SIP should be AIP_CREATED After a successfull process",
                          SIPState.AIP_CREATED.equals(resultSip.getState()));

        // Simulate an error during PreprocessingStep
        stepErrorSimulator.setSimulateErrorForStep(PreprocessingTestPlugin.class);
        toTest = new JobInfo(1, parameters, "owner", IngestProcessingJob.class.getName());
        try {
            runJob(toTest);
            Assert.fail("A runtime exception should thrown here");
        } catch (RuntimeException e) {
            LOG.info(e.getMessage());
        }
        // Assert that SIP is in INVALID state
        Assert.assertTrue("State of SIP should be INVALID after a error during PreprocessingTestPlugin",
                          SIPState.INVALID.equals(resultSip.getState()));

        // Simulate an error during ValidationStep
        stepErrorSimulator.setSimulateErrorForStep(ValidationTestPlugin.class);
        toTest = new JobInfo(1, parameters, "owner", IngestProcessingJob.class.getName());
        try {
            runJob(toTest);
            Assert.fail("A runtime exception should thrown here");
        } catch (RuntimeException e) {
            LOG.info(e.getMessage());
        }
        // Assert that SIP is in INVALID state
        Assert.assertTrue("State of SIP should be INVALID after a error during ValidationStep",
                          SIPState.INVALID.equals(resultSip.getState()));
        // Assert that SIP is in INVALID state

        // Simulate an error during GenerationStep
        stepErrorSimulator.setSimulateErrorForStep(AIPGenerationTestPlugin.class);
        toTest = new JobInfo(1, parameters, "owner", IngestProcessingJob.class.getName());
        try {
            runJob(toTest);
            Assert.fail("A runtime exception should thrown here");
        } catch (RuntimeException e) {
            LOG.info(e.getMessage());
        }
        // Assert that SIP is in AIP_GEN_ERROR state
        Assert.assertTrue("State of SIP should be AIP_GEN_ERROR after a error during GenerationStep",
                          SIPState.AIP_GEN_ERROR.equals(resultSip.getState()));

        // Simulate an error during TaggingStep
        stepErrorSimulator.setSimulateErrorForStep(AIPTaggingTestPlugin.class);
        toTest = new JobInfo(1, parameters, "owner", IngestProcessingJob.class.getName());
        try {
            runJob(toTest);
            Assert.fail("A runtime exception should thrown here");
        } catch (RuntimeException e) {
            LOG.info(e.getMessage());
        }
        // Assert that SIP is in AIP_GEN_ERROR state
        Assert.assertTrue("State of SIP should be AIP_GEN_ERROR after a error during GenerationStep",
                          SIPState.AIP_GEN_ERROR.equals(resultSip.getState()));

        // Simulate an error during StoreStep
        // TODO
        // Assert that SIP is in AIP_GEN_ERROR state

    }

    @Purpose("Test fully configured process chain to ingest a new SIP provided by reference")
    @Test
    public void testProcessingChainByRef() throws JobParameterMissingException, JobParameterInvalidException {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(IngestProcessingJob.CHAIN_NAME_PARAMETER, PROCESSING_CHAIN_TEST));
        parameters.add(new JobParameter(IngestProcessingJob.SIP_PARAMETER, sipRefIdTest));

        // Simulate a full process without error
        JobInfo toTest = new JobInfo(0, parameters, "owner", IngestProcessingJob.class.getName());
        runJob(toTest);
        // Assert that SIP is in AIP_CREATED state
        SIPEntity resultSip = sipRepository.findOne(sipRefIdTest);
        Assert.assertTrue("SIP should be the one generated in the test initialization.",
                          SIP_REF_ID_TEST.equals(resultSip.getSip().getId()));
        Assert.assertTrue("State of SIP should be AIP_CREATED After a successfull process",
                          SIPState.AIP_CREATED.equals(resultSip.getState()));

    }

    protected IJob<?> runJob(JobInfo jobInfo) {
        try {
            IJob<?> job = (IJob<?>) Class.forName(jobInfo.getClassName()).newInstance();
            beanFactory.autowireBean(job);
            job.setParameters(jobInfo.getParametersAsMap());
            if (job.needWorkspace()) {
                job.setWorkspace(Files.createTempDirectory(jobInfo.getId().toString()));
            }
            jobInfo.setJob(job);
            jobInfo.getStatus().setStartDate(OffsetDateTime.now());
            job.run();
            return job;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
            LOG.error("Unable to instantiate job", e);
            Assert.fail("Unable to instantiate job");
        } catch (JobParameterMissingException e) {
            LOG.error("Missing parameter", e);
            Assert.fail("Missing parameter");
        } catch (JobParameterInvalidException e) {
            LOG.error("Invalid parameter", e);
            Assert.fail("Invalid parameter");
        }
        return null;
    }

}
