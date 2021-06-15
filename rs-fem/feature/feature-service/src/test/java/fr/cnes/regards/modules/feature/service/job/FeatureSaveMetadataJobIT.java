/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.feature.service.job;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureSaveMetadataRequest;
import fr.cnes.regards.modules.feature.dto.event.out.RequestState;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceTest;
import fr.cnes.regards.modules.feature.service.dump.FeatureSaveMetadataService;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Test for {@link FeatureSaveMetadataJob}
 * @author Iliana Ghazali
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_savemetadata_job_it",
        "regards.amqp.enabled=true" })
@ActiveProfiles(value = { "testAmqp", "noFemHandler", "noscheduler" })
public class FeatureSaveMetadataJobIT extends AbstractFeatureMultitenantServiceTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private Path dumpLocation;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IJobService jobService;

    @Autowired
    private FeatureSaveMetadataService saveMetadataService;

    @Autowired
    private IWorkspaceService workspaceService;

    @Override
    public void doInit() {
        simulateApplicationReadyEvent();
        // Re-set tenant because above simulation clear it!
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // dump location is in microservice workspace by default
        try {
            this.dumpLocation = workspaceService.getMicroserviceWorkspace();
        } catch (IOException e) {
            LOGGER.error("[FeatureSaveMetadataJobIT] Not able to get microservice workspace");
        }
    }

    @Test
    @Purpose("Check if the dump of features is successfully created")
    public void checkDumpSuccess() throws ExecutionException, InterruptedException, EntityException {
        // add feature to db
        int nbFeatures = 6;
        initData(nbFeatures);

        // dump all features created
        JobInfo jobInfo = runSaveMetadataJob();
        Assert.assertNotEquals(null, jobInfo);
        UUID jobInfoId = jobInfo.getId();
        Thread.sleep(1000);

        // CHECK RESULTS
        // Check job info is successful
        Optional<JobInfo> errorJobInfoOpt = jobInfoRepository.findById(jobInfoId);
        Assert.assertTrue(errorJobInfoOpt.isPresent());
        Assert.assertEquals(JobStatus.SUCCEEDED, errorJobInfoOpt.get().getStatus().getStatus());

        // Check all requests were deleted
        List<FeatureSaveMetadataRequest> errorRequests = featureSaveMetadataRequestRepository.findAll();
        Assert.assertEquals(0, errorRequests.size());

        // Check folder target/workspace/<microservice>/ exists and contains 1 dump
        Assert.assertTrue("The dump location does not exist or does not contain one zip",
                          Files.exists(this.dumpLocation) && this.dumpLocation.toFile().listFiles().length == 1);
    }

    @Test
    @Purpose("Check if the dump of features is not created (in error)")
    public void checkDumpError() throws InterruptedException {
        // add features to db
        int nbFeatures = 3;
        initData(nbFeatures);

        // change provider id to create duplicated jsonNames
        updateFeatureProviderIdVersion();

        // dump all features created
        try {
            runSaveMetadataJob();
            Assert.fail();
        } catch (ExecutionException | InterruptedException | EntityException e) {
            Assert.assertTrue("DuplicateUniqueNameException was expected",
                              e.getMessage().contains("DuplicateUniqueNameException"));
        }
        Thread.sleep(1000);

        // Check job info in ERROR
        Pageable pageToRequest = PageRequest.of(0, 100);
        Page<JobInfo> errorJobInfo = jobInfoRepository
                .findByClassNameAndStatusStatusIn(FeatureSaveMetadataJob.class.getName(), JobStatus.values(),
                                                  pageToRequest);
        Assert.assertEquals(1L, errorJobInfo.getTotalElements());
        Assert.assertEquals(JobStatus.FAILED, errorJobInfo.getContent().get(0).getStatus().getStatus());

        // Check request in ERROR
        List<FeatureSaveMetadataRequest> errorRequests = featureSaveMetadataRequestRepository.findAll();
        Assert.assertEquals(1, errorRequests.size());
        Assert.assertEquals(RequestState.ERROR, errorRequests.get(0).getState());

        // Check folder target/workspace/<microservice>/ does not contain dump
        Assert.assertEquals("Dump folder should be empty", 0, this.dumpLocation.toFile().listFiles().length);
    }

    private JobInfo runSaveMetadataJob() throws ExecutionException, InterruptedException, EntityException {
        // Run Job and wait for end
        JobInfo saveMetadataJobInfo = saveMetadataService.scheduleJobs();
        if (saveMetadataJobInfo != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(saveMetadataJobInfo, tenant).get();
        }
        return saveMetadataJobInfo;

    }

    public void updateFeatureProviderIdVersion() {
        abstractFeatureRequestRepo.deleteAll();
        List<FeatureEntity> features = featureRepo.findAll();
        for (FeatureEntity feature : features) {
            feature.setProviderId("Provider_1");
            feature.setVersion(1);
        }
        featureRepo.saveAll(features);
    }

    @Override
    protected void doAfter() throws IOException {
        abstractFeatureRequestRepo.deleteAll();
        featureRepo.deleteAll();
        jobInfoRepository.deleteAll();
        //clear dump location
        FileUtils.deleteDirectory(this.dumpLocation.toFile());
    }
}